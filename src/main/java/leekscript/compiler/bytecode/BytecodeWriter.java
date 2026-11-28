package leekscript.compiler.bytecode;

import java.lang.classfile.CodeBuilder;
import java.lang.classfile.Label;
import java.lang.classfile.Opcode;
import java.lang.classfile.TypeKind;
import java.lang.classfile.instruction.OperatorInstruction;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import leekscript.compiler.AIFile;
import leekscript.compiler.LineMapping;
import leekscript.compiler.Location;
import leekscript.common.Type.CastType;
import leekscript.common.CompoundType;
import leekscript.common.LanguageFeature;
import leekscript.common.Type;
import leekscript.compiler.Options;
import leekscript.compiler.bloc.MainLeekBlock;
import leekscript.compiler.expression.Expression;

/**
 * Émetteur de bytecode d'une méthode, l'équivalent de {@link leekscript.compiler.JavaWriter}
 * pour le backend bytecode. Les nœuds de l'AST construisent des {@link JExpr} ; celui-ci les
 * type comme javac (JLS 15) et les émet. Les numéros de ligne sont des repères propres au
 * backend, reliés à la ligne LeekScript par le fichier `.lines`, comme ceux du Java.
 */
public class BytecodeWriter {

	/** Une variable du Java : locale de la méthode ou champ de la classe générée. */
	public sealed interface Slot permits LocalVar, FieldVar, MemberVar {
		Class<?> type();
		String name();
		/** Le type générique de la variable (`Wrapper<FunctionLeekValue<String>>`…), null s'il n'en a pas. */
		default GType generic() {
			return null;
		}
	}

	/**
	 * Variable locale : son emplacement, son type Java, son nom LeekScript, la méthode qui la
	 * déclare (null : celle en cours d'écriture) et le paramètre de type de sa boîte.
	 */
	public record LocalVar(int slot, Class<?> type, String name, Object method, GType generic) implements Slot {
		public LocalVar(int slot, Class<?> type, String name) {
			this(slot, type, name, null, null);
		}
	}

	/** Champ public `name` d'un objet quelconque (`o.f`), déclaré dans `owner`. */
	public record MemberVar(JExpr receiver, Class<?> owner, String name, Class<?> type) implements Slot {}

	/** Champ d'instance d'une classe générée (globale de l'IA, champ d'une classe utilisateur…). */
	public record FieldVar(ClassContext owner, String name, Class<?> type, GType generic) implements Slot {
		public FieldVar(ClassContext owner, String name, Class<?> type) {
			this(owner, name, type, null);
		}
	}

	/**
	 * Une classe en cours d'écriture : celle de l'IA, une classe utilisateur `u_X` ou une classe
	 * anonyme, interne à `outer` (champ `this$0`). `type` type son `this` ; `members` est la classe où
	 * se cherchent ses membres pour un nom non qualifié (la classe du runtime pour l'IA, le talon sinon).
	 */
	public static final class ClassContext {
		public final ClassDesc desc;
		public final Class<?> type;
		public final Class<?> members;
		public final ClassContext outer;
		java.lang.classfile.ClassBuilder builder;
		final java.util.HashMap<String, FieldVar> fields = new java.util.HashMap<>();
		/** Classe anonyme : les locales des méthodes englobantes qu'elle lit, chacune dans son champ `val$x`. */
		java.util.LinkedHashMap<LocalVar, FieldVar> captures = null;

		public ClassContext(ClassDesc desc, Class<?> type, Class<?> members, ClassContext outer) {
			this.desc = desc;
			this.type = type;
			this.members = members;
			this.outer = outer;
		}

		public java.lang.classfile.ClassBuilder builder() {
			return builder;
		}
	}

	/** L'état de la méthode en cours d'écriture, mis de côté le temps d'écrire une classe anonyme. */
	private static final class MethodState {
		CodeBuilder cb;
		int nextSlot;
		final ArrayList<Integer> scopes = new ArrayList<>();
		final ArrayList<Loop> loops = new ArrayList<>();
		Class<?> returnType;
		boolean alive = true;
		String name;
		MethodTypeDesc type;
		/** Repère posé sans ligne LeekScript, que la prochaine instruction reprend (cf pendingLine). */
		int pendingLine = 0;
	}

	public final Options options;
	public final int version;
	/** La classe de l'IA, et la classe en cours d'écriture. */
	public final ClassContext aiContext;
	private ClassContext context;

	private MethodState method = new MethodState();
	private CodeBuilder cb;
	private final IdentityHashMap<Object, LocalVar> locals = new IdentityHashMap<>();
	private final IdentityHashMap<JExpr, Class<?>> types = new IdentityHashMap<>();

	/** Cibles de break / continue des boucles englobantes. */
	public record Loop(Label breakLabel, Label continueLabel) {}

	// Enveloppes génériques des fonctions système (JavaWriter.generateGenericFunction), écrites après runIA
	private final java.util.LinkedHashMap<String, List<leekscript.runner.CallableVersion>> genericFunctions = new java.util.LinkedHashMap<>();
	private final java.util.HashSet<String> strictGenericFunctions = new java.util.HashSet<>();

	// Lignes : repère Java -> ligne LeekScript, et fichiers dans l'ordre de leur première ligne
	private final TreeMap<Integer, LineMapping> lines = new TreeMap<>();
	private final ArrayList<AIFile> files = new ArrayList<>();
	private int nextLine = 1;

	public BytecodeWriter(Options options, int version, ClassContext aiContext, java.lang.classfile.ClassFile classFile, Map<ClassDesc, ClassDesc> hierarchy) {
		this.classFile = classFile;
		this.hierarchy = hierarchy;
		this.options = options;
		this.version = version;
		this.aiContext = aiContext;
		this.context = aiContext;
	}

	public ClassContext context() {
		return context;
	}

	/** Le bloc de la méthode de classe en cours (JavaWriter.currentBlock). */
	public leekscript.compiler.bloc.AbstractLeekBlock currentBlock = null;

	public boolean inInstanceContext() {
		return currentBlock != null && !currentBlock.isInStaticMethod();
	}

	// Classes écrites à côté de celle de l'IA (classes utilisateur, anonymes), leur hiérarchie pour
	// les stack maps, et les attributs InnerClasses / NestMembers de chaque classe englobante
	private final java.util.LinkedHashMap<String, byte[]> classes = new java.util.LinkedHashMap<>();
	private final Map<ClassDesc, ClassDesc> hierarchy;
	private final java.lang.classfile.ClassFile classFile;
	private final java.util.HashMap<ClassDesc, ArrayList<java.lang.classfile.attribute.InnerClassInfo>> innerClasses = new java.util.HashMap<>();
	private final ArrayList<ClassDesc> nestMembers = new ArrayList<>();
	private int anonymousCount = 0;

	public Map<String, byte[]> getClasses() {
		return classes;
	}

	public void addClass(ClassDesc desc, byte[] bytes) {
		classes.put(desc.displayName(), bytes);
	}

	/** Le talon de la classe de l'IA (type du paramètre d'instance englobante des classes internes). */
	public Class<?> aiStub() {
		var stub = stubs.get(aiContext.desc.displayName());
		if (stub == null) throw new Unsupported("talon de l'IA");
		return stub;
	}

	public java.lang.classfile.ClassFile classFile() {
		return classFile;
	}

	/** Une classe interne de l'IA : sa hiérarchie, et sa place dans le nid et dans les InnerClasses de `outer`. */
	public void registerInnerClass(ClassDesc desc, ClassDesc superDesc, ClassDesc outer, String simpleName, int flags) {
		hierarchy.put(desc, superDesc);
		nestMembers.add(desc);
		var info = java.lang.classfile.attribute.InnerClassInfo.of(desc, simpleName == null ? java.util.Optional.empty() : java.util.Optional.of(outer),
			java.util.Optional.ofNullable(simpleName), flags);
		innerClasses.computeIfAbsent(outer, k -> new ArrayList<>()).add(info);
		innerClasses.computeIfAbsent(desc, k -> new ArrayList<>()).add(info);
	}

	/** Les attributs de nid et de classes internes de la classe `desc`, à la fin de son écriture. */
	public void finishClass(java.lang.classfile.ClassBuilder builder, ClassDesc desc) {
		var inner = innerClasses.get(desc);
		if (inner != null) builder.with(java.lang.classfile.attribute.InnerClassesAttribute.of(inner));
		if (desc.equals(aiContext.desc)) {
			if (!nestMembers.isEmpty()) {
				var members = new ArrayList<java.lang.constant.ClassDesc>(nestMembers);
				builder.with(java.lang.classfile.attribute.NestMembersAttribute.ofSymbols(members));
			}
		} else {
			builder.with(java.lang.classfile.attribute.NestHostAttribute.of(aiContext.desc));
		}
	}

	/** Un type générique à un paramètre, tel que javac le suit : `raw<arg>`. */
	public record GType(Class<?> raw, GType arg) {}

	private static final java.util.Set<Class<?>> GENERIC_CLASSES = java.util.Set.of(leekscript.runner.values.Box.class, leekscript.runner.Wrapper.class, leekscript.runner.values.FunctionLeekValue.class);

	/** Le type générique d'un nom écrit par JavaWriter (`FunctionLeekValue<String>`), null hors des classes suivies. */
	public GType genericNamed(String name) {
		int lt = name.indexOf('<');
		if (lt < 0) return null;
		if (!name.endsWith(">")) return null;
		var inner = name.substring(lt + 1, name.length() - 1).trim();
		if (inner.contains(",")) return null;
		var raw = runtimeType(name.substring(0, lt));
		if (!GENERIC_CLASSES.contains(raw)) return null;
		var innerGeneric = genericNamed(inner);
		return new GType(raw, innerGeneric != null ? innerGeneric : new GType(typeNamed(inner), null));
	}

	/** `Box<T>` / `Wrapper<T>` d'une variable capturée dont T s'écrit `elementJavaName`. */
	public GType boxGeneric(Class<?> box, String elementJavaName) {
		var element = genericNamed(elementJavaName);
		return new GType(box, element != null ? element : new GType(typeNamed(elementJavaName), null));
	}

	/** Le type générique statique d'une expression, quand javac le connaît mieux que son effacement. */
	public GType genericOf(JExpr e) {
		if (e == null) return null;
		return switch (e) {
			case JExpr.Read r -> r.var().generic();
			case JExpr.Assign a -> a.var().generic();
			case JExpr.Call c -> {
				var g = genericOf(c.receiver());
				yield g != null && g.arg() != null && returnsClassTypeVariable(resolve(c)) ? g.arg() : null;
			}
			default -> null;
		};
	}

	private static boolean returnsClassTypeVariable(Resolver.Resolved r) {
		return r.executable() instanceof Method m && m.getGenericReturnType() instanceof java.lang.reflect.TypeVariable<?> tv && tv.getGenericDeclaration() instanceof Class<?>;
	}

	/** Une méthode d'une classe anonyme. */
	public record AnonymousMethod(String name, int flags, List<Class<?>> parameters, Class<?> returnType, Runnable body) {}

	/**
	 * `new S(superArgs) { méthodes }` : la classe anonyme, interne à la classe en cours, est écrite à
	 * part ; rend l'expression qui l'instancie (typée S). Son constructeur reçoit l'instance
	 * englobante (`this$0`) puis les paramètres du constructeur de S choisi par les arguments.
	 */
	public JExpr anonymous(Class<?> superclass, List<JExpr> superArgs, List<AnonymousMethod> methods) {
		var superCtor = Resolver.constructor(superclass, argTypesOf(superArgs));
		var superParams = List.of(superCtor.parameters());
		var outer = context;
		var desc = ClassDesc.of(aiContext.desc.displayName() + "$" + (++anonymousCount));
		var superDesc = JTypes.desc(superclass);
		registerInnerClass(desc, superDesc, outer.desc, null, 0);
		var enclosingName = method.name;
		var enclosingType = method.type;
		var anonymous = new ClassContext(desc, superclass, superclass, outer);
		anonymous.captures = new java.util.LinkedHashMap<>();
		var bytes = classFile.build(desc, clb -> inClass(anonymous, clb, () -> {
			clb.withFlags(java.lang.classfile.ClassFile.ACC_SUPER);
			clb.withSuperclass(superDesc);
			clb.withField("this$0", outer.desc, java.lang.classfile.ClassFile.ACC_FINAL | java.lang.classfile.ClassFile.ACC_SYNTHETIC);
			// Les méthodes d'abord : elles disent quelles locales englobantes la classe capture
			for (var m : methods) writeMethod(m.name(), m.flags(), m.parameters(), m.returnType(), m.body());
			var captures = new ArrayList<>(anonymous.captures.values());
			var ctorParams = new ClassDesc[1 + superParams.size() + captures.size()];
			ctorParams[0] = outer.desc;
			for (int i = 0; i < superParams.size(); i++) ctorParams[i + 1] = JTypes.desc(superParams.get(i));
			for (int i = 0; i < captures.size(); i++) ctorParams[1 + superParams.size() + i] = JTypes.desc(captures.get(i).type());
			clb.withMethodBody(ConstantDescs.INIT_NAME, MethodTypeDesc.of(ConstantDescs.CD_void, ctorParams), 0, code -> {
				code.aload(0);
				code.aload(1);
				code.putfield(desc, "this$0", outer.desc);
				int slot = 2;
				var superDescs = new ClassDesc[superParams.size()];
				for (int i = 0; i < superParams.size(); i++) {
					superDescs[i] = JTypes.desc(superParams.get(i));
					slot += kind(superParams.get(i)).slotSize();
				}
				// Les champs des locales capturées, avant super() comme javac
				for (var capture : captures) {
					var k = kind(capture.type());
					code.aload(0);
					code.loadLocal(k, slot);
					code.putfield(desc, capture.name(), JTypes.desc(capture.type()));
					slot += k.slotSize();
				}
				code.aload(0);
				slot = 2;
				for (int i = 0; i < superParams.size(); i++) {
					var k = kind(superParams.get(i));
					code.loadLocal(k, slot);
					slot += k.slotSize();
				}
				code.invokespecial(superDesc, ConstantDescs.INIT_NAME, MethodTypeDesc.of(ConstantDescs.CD_void, superDescs));
				code.return_();
			});
			if (enclosingName != null) {
				clb.with(java.lang.classfile.attribute.EnclosingMethodAttribute.of(outer.desc, java.util.Optional.of(enclosingName), java.util.Optional.of(enclosingType)));
			}
			finishClass(clb, desc);
		}));
		classes.put(desc.displayName(), bytes);
		// L'instanciation passe aussi les locales capturées (lues dans la méthode englobante, qui peut à son tour les capturer)
		var parameters = new ArrayList<Class<?>>(superParams);
		var args = new ArrayList<JExpr>(superArgs);
		for (var capture : anonymous.captures.entrySet()) {
			parameters.add(capture.getValue().type());
			args.add(new JExpr.Read(capture.getKey()));
		}
		return new JExpr.NewInner(desc, parameters, args, superclass);
	}

	private List<Class<?>> argTypesOf(List<JExpr> args) {
		var r = new ArrayList<Class<?>>(args.size());
		for (var a : args) r.add(typeOf(a));
		return r;
	}

	/** Écrit `body` dans la classe `inner` (une méthode en cours est mise de côté puis reprise). */
	public void inClass(ClassContext inner, java.lang.classfile.ClassBuilder builder, Runnable body) {
		var savedContext = context;
		var savedMethod = method;
		var savedCb = cb;
		inner.builder = builder;
		context = inner;
		method = new MethodState();
		try {
			body.run();
		} finally {
			context = savedContext;
			method = savedMethod;
			cb = savedCb;
		}
	}

	public boolean isOperationsEnabled() {
		return options.enableOperations();
	}

	public void setClassBuilder(java.lang.classfile.ClassBuilder classBuilder) {
		context.builder = classBuilder;
	}

	/**
	 * Écrit une méthode d'instance de la classe générée : `body` émet son code (les paramètres
	 * occupent les emplacements qui suivent `this`). Une fin de méthode atteignable lève
	 * Unsupported, comme javac refuserait une méthode sans return.
	 */
	public void writeMethod(String name, int flags, List<Class<?>> parameters, Class<?> returnType, Runnable body) {
		writeMethod(name, flags, parameters, returnType, List.of(), body);
	}

	/** Idem, avec des annotations visibles à l'exécution (`@Private`, `@Protected` des méthodes de classe). */
	public void writeMethod(String name, int flags, List<Class<?>> parameters, Class<?> returnType, List<Class<?>> annotations, Runnable body) {
		var descs = new ClassDesc[parameters.size()];
		int slot = 1;
		for (int i = 0; i < descs.length; i++) {
			descs[i] = JTypes.desc(parameters.get(i));
			slot += kind(parameters.get(i)).slotSize();
		}
		int firstSlot = slot;
		var methodType = MethodTypeDesc.of(JTypes.desc(returnType), descs);
		java.util.function.Consumer<java.lang.classfile.CodeBuilder> handler = code -> {
			var saved = method;
			var savedCb = cb;
			method = new MethodState();
			try {
				begin(code, firstSlot, returnType);
				method.name = name;
				method.type = methodType;
				body.run();
				if (method.alive) {
					if (returnType != void.class) throw new Unsupported("fin de " + name + " atteignable");
					code.return_();
				}
			} finally {
				method = saved;
				cb = savedCb;
			}
		};
		if (annotations.isEmpty()) {
			context.builder.withMethodBody(name, methodType, flags, handler);
		} else {
			context.builder.withMethod(name, methodType, flags, mb -> {
				var list = new ArrayList<java.lang.classfile.Annotation>();
				for (var a : annotations) list.add(java.lang.classfile.Annotation.of(JTypes.desc(a)));
				mb.with(java.lang.classfile.attribute.RuntimeVisibleAnnotationsAttribute.of(list));
				mb.withCode(handler);
			});
		}
	}

	/** Emplacement du paramètre `index` (0 = premier après this) d'une méthode de paramètres `parameters`. */
	public static int parameterSlot(List<Class<?>> parameters, int index) {
		int slot = 1;
		for (int i = 0; i < index; i++) slot += kind(parameters.get(i)).slotSize();
		return slot;
	}

	/** Une variable LeekScript lue directement dans un emplacement existant (paramètre). */
	public LocalVar alias(Object declaration, int slot, Class<?> type, String name) {
		return alias(declaration, slot, type, name, null);
	}

	public LocalVar alias(Object declaration, int slot, Class<?> type, String name, GType generic) {
		var v = new LocalVar(slot, type, name, method, generic);
		locals.put(declaration, v);
		return v;
	}

	/** Repère de ligne sans ligne LeekScript : le code qui suit n'est rattaché à aucune (cadre ignoré). */
	public void noLine() {
		cb.lineNumber(nextLine++);
	}

	/** Début d'une méthode : `firstSlot` = premier emplacement libre après this et les paramètres. */
	public void begin(CodeBuilder cb, int firstSlot, Class<?> returnType) {
		this.cb = cb;
		method.cb = cb;
		method.returnType = returnType;
		method.alive = true;
		method.nextSlot = firstSlot;
		method.scopes.clear();
		method.loops.clear();
	}

	public CodeBuilder code() {
		return cb;
	}

	public Class<?> returnType() {
		return method.returnType;
	}

	public boolean isAlive() {
		return method.alive;
	}

	public Label newLabel() {
		return cb.newLabel();
	}

	/** Pose une étiquette d'instruction : le code qui la suit redevient atteignable. */
	public void bind(Label label) {
		cb.labelBinding(label);
		method.alive = true;
	}

	public void goto_(Label label) {
		cb.goto_(label);
		method.alive = false;
	}

	/** `ops(n);` de JavaWriter.addCounter. */
	public void counter(int count) {
		if (isOperationsEnabled()) emitStatement(JExpr.call("ops", JExpr.of(count)));
	}

	public TreeMap<Integer, LineMapping> getLines() {
		return lines;
	}

	public List<AIFile> getFiles() {
		return files;
	}

	/** Enregistre l'enveloppe générique des versions d'une fonction système et rend son nom de clé, comme JavaWriter. */
	public String genericFunction(List<leekscript.runner.CallableVersion> versions, boolean strictNull) {
		String key = versions.get(0).function.getName();
		for (var version : versions) key += "_" + version.getParametersSignature();
		if (strictNull && versions.stream().anyMatch(GenericFunctions::convertsNumberArgument)) {
			key += "_strict";
			strictGenericFunctions.add(key);
		}
		genericFunctions.put(key, versions);
		return key;
	}

	public java.util.Map<String, List<leekscript.runner.CallableVersion>> getGenericFunctions() {
		return genericFunctions;
	}

	public boolean isStrictGenericFunction(String key) {
		return strictGenericFunctions.contains(key);
	}

	// ---- Portées et variables locales ----

	public void openScope() {
		method.scopes.add(method.nextSlot);
	}

	public void closeScope() {
		method.nextSlot = method.scopes.remove(method.scopes.size() - 1);
	}

	public LocalVar declare(Object declaration, Class<?> type, String name) {
		return declare(declaration, type, name, null);
	}

	/** Une locale de type générique (`Box<T>`, `Wrapper<T>`, `FunctionLeekValue<T>`) : `generic` le décrit. */
	public LocalVar declare(Object declaration, Class<?> type, String name, GType generic) {
		if (type == void.class || JTypes.isNull(type)) throw new Unsupported("variable de type " + type);
		var v = new LocalVar(method.nextSlot, type, name, method, generic);
		method.nextSlot += TypeKind.from(JTypes.desc(type)).slotSize();
		locals.put(declaration, v);
		return v;
	}

	/** Déclare un champ d'instance de la classe en cours d'écriture. */
	public FieldVar field(String name, Class<?> type, int flags) {
		return field(name, type, flags, null);
	}

	/** Idem, avec ses annotations (`@Final`, `@Private`… des champs de classe utilisateur). */
	public FieldVar field(String name, Class<?> type, int flags, List<Class<?>> annotations) {
		return field(name, type, flags, annotations, null);
	}

	public FieldVar field(String name, Class<?> type, int flags, List<Class<?>> annotations, GType generic) {
		var f = new FieldVar(context, name, type, generic);
		if (context.fields.putIfAbsent(name, f) != null) throw new Unsupported("champ en double " + name);
		if (annotations == null || annotations.isEmpty()) {
			context.builder.withField(name, JTypes.desc(type), flags);
		} else {
			context.builder.withField(name, JTypes.desc(type), fb -> {
				fb.withFlags(flags);
				var list = new ArrayList<java.lang.classfile.Annotation>();
				for (var a : annotations) list.add(java.lang.classfile.Annotation.of(JTypes.desc(a)));
				fb.with(java.lang.classfile.attribute.RuntimeVisibleAnnotationsAttribute.of(list));
			});
		}
		return f;
	}

	// Initialiseurs des champs (`T g_x = <défaut>;`), exécutés par le constructeur après super()
	private final ArrayList<JExpr> fieldInitializers = new ArrayList<>();

	public void initializeField(FieldVar field, JExpr value) {
		fieldInitializers.add(new JExpr.Assign(field, value));
	}

	public List<JExpr> getFieldInitializers() {
		return fieldInitializers;
	}

	// Valeurs fonction des fonctions utilisateur (`ufunction_<nom>`), créées à la première référence et
	// initialisées par le constructeur de l'IA (leur classe anonyme a l'IA pour classe englobante)
	private final java.util.LinkedHashMap<String, FieldVar> functionValues = new java.util.LinkedHashMap<>();
	private final ArrayList<java.util.function.Supplier<JExpr>> lazyInitializers = new ArrayList<>();
	private final ArrayList<FieldVar> lazyFields = new ArrayList<>();

	public FieldVar functionValue(String name, java.util.function.Supplier<JExpr> value) {
		var field = functionValues.get(name);
		if (field == null) {
			field = new FieldVar(aiContext, name, leekscript.runner.values.FunctionLeekValue.class);
			aiContext.fields.put(name, field);
			aiContext.builder.withField(name, JTypes.desc(field.type()), java.lang.classfile.ClassFile.ACC_PRIVATE);
			functionValues.put(name, field);
			lazyFields.add(field);
			lazyInitializers.add(value);
		}
		return field;
	}

	/** Les initialiseurs des valeurs fonction, à écrire dans le constructeur de l'IA (après les autres champs). */
	public List<JExpr> takeLazyInitializers() {
		var result = new ArrayList<JExpr>();
		// Une valeur fonction peut en référencer d'autres : la liste grandit pendant qu'on la parcourt
		for (int i = 0; i < lazyInitializers.size(); i++) result.add(new JExpr.Assign(lazyFields.get(i), lazyInitializers.get(i).get()));
		return result;
	}

	// Corps du constructeur de l'IA après les initialiseurs (defineClass_X()…)
	private final ArrayList<JExpr> constructorStatements = new ArrayList<>();

	public void addConstructorStatement(JExpr statement) {
		constructorStatements.add(statement);
	}

	public List<JExpr> getConstructorStatements() {
		return constructorStatements;
	}

	/** L'instance de la classe `c` (la classe en cours ou une classe qui l'englobe). */
	public JExpr instance(ClassContext c) {
		return c == context ? new JExpr.This() : new JExpr.Outer(c);
	}

	/** `u_X.this` : l'instance de la classe englobante de type `type`. */
	public JExpr thisOf(Class<?> type) {
		for (var c = context; c != null; c = c.outer) {
			if (c.type == type) return instance(c);
		}
		throw new Unsupported("pas d'instance englobante " + type.getSimpleName());
	}

	/**
	 * Un champ lu par son nom simple, comme Java le trouve : dans la classe en cours ou la plus
	 * interne qui l'englobe et l'a, champ déclaré par le backend ou hérité (public ou protégé).
	 */
	public Slot fieldNamed(String name) {
		for (var c = context; c != null; c = c.outer) {
			var f = c.fields.get(name);
			if (f != null) return f;
			var reflected = findField(c.members, name);
			if (reflected != null) {
				// Un membre protégé d'une autre classe englobante : javac passerait par une méthode d'accès
				if (c != context && Modifier.isProtected(reflected.getModifiers())) throw new Unsupported("champ protégé englobant " + name);
				return new MemberVar(instance(c), reflected.getDeclaringClass(), name, reflected.getType());
			}
		}
		throw new Unsupported("champ inconnu " + name);
	}

	private static java.lang.reflect.Field findField(Class<?> owner, String name) {
		for (var k = owner; k != null; k = k.getSuperclass()) {
			for (var f : k.getDeclaredFields()) {
				int mod = f.getModifiers();
				if (f.getName().equals(name) && !Modifier.isStatic(mod) && (Modifier.isPublic(mod) || Modifier.isProtected(mod))) return f;
			}
		}
		return null;
	}

	/** Le champ public `name` de l'objet `receiver`, cherché dans son type statique (classe du runtime ou talon). */
	public MemberVar member(JExpr receiver, String name) {
		var owner = typeOf(receiver);
		try {
			var f = owner.getField(name);
			if (Modifier.isStatic(f.getModifiers())) throw new Unsupported("champ statique " + name);
			return new MemberVar(receiver, owner, name, f.getType());
		} catch (NoSuchFieldException e) {
			throw new Unsupported("champ " + owner.getSimpleName() + "." + name);
		}
	}

	/** Le champ `name` tel que Java le trouve depuis la classe en cours : la plus interne qui le déclare. */
	public FieldVar field(String name) {
		for (var c = context; c != null; c = c.outer) {
			var f = c.fields.get(name);
			if (f != null) return f;
		}
		throw new Unsupported("champ inconnu " + name);
	}

	public LocalVar local(Object declaration) {
		var v = locals.get(declaration);
		if (v == null) throw new Unsupported("variable locale inconnue");
		return v;
	}

	public void pushLoop(Loop loop) {
		method.loops.add(loop);
	}

	public void popLoop() {
		method.loops.remove(method.loops.size() - 1);
	}

	public Loop currentLoop() {
		if (method.loops.isEmpty()) throw new Unsupported("break hors boucle");
		return method.loops.get(method.loops.size() - 1);
	}

	// ---- Lignes ----

	/** Repère de ligne pour l'instruction qui commence, relié à la ligne LeekScript de `location`. */
	/** Diagnostic (cf Unsupported) : l'instruction LeekScript en cours d'émission. */
	static final ThreadLocal<Location> CURRENT_LOCATION = new ThreadLocal<>();

	public void line(Location location) {
		if (location == null || location.getFile() == null) return;
		CURRENT_LOCATION.set(location);
		boolean pending = method.pendingLine != 0;
		int line = pending ? method.pendingLine : nextLine++;
		method.pendingLine = 0;
		var file = location.getFile();
		int index = files.indexOf(file);
		if (index < 0) {
			index = files.size();
			files.add(file);
		}
		lines.put(line, new LineMapping(location.getStartLine(), index));
		if (!pending) cb.lineNumber(line);
	}

	/**
	 * Le code qui suit (le `ops(1)` d'entrée d'un corps) est sur la ligne Java de la prochaine
	 * instruction, comme JavaWriter l'écrit : il en prend la ligne LeekScript, ou aucune si le bloc
	 * se ferme avant (cf endLine).
	 */
	public void pendingLine() {
		method.pendingLine = nextLine++;
		cb.lineNumber(method.pendingLine);
	}

	/** Fin de bloc (`}`) : une ligne en attente reste sans ligne LeekScript. */
	public void endLine() {
		method.pendingLine = 0;
	}

	// ---- Typage (JLS 15) ----

	public Class<?> typeOf(JExpr e) {
		var t = types.get(e);
		if (t == null) {
			t = computeType(e);
			types.put(e, t);
		}
		return t;
	}

	private List<Class<?>> argTypes(List<JExpr> args) {
		var r = new ArrayList<Class<?>>(args.size());
		for (var a : args) r.add(typeOf(a));
		return r;
	}

	/** Les `X.class` passés en argument, pour déduire un `T` déclaré `Class<T>`. */
	private static List<Class<?>> classLiterals(List<JExpr> args) {
		List<Class<?>> r = null;
		for (int i = 0; i < args.size(); i++) {
			if (args.get(i) instanceof JExpr.ClassLit c) {
				if (r == null) r = new ArrayList<>(java.util.Collections.nCopies(args.size(), null));
				r.set(i, c.type());
			}
		}
		return r;
	}

	private Resolver.Resolved resolve(JExpr.Call c) {
		var owner = c.receiver() == null ? methodScope(c.name()).members : typeOf(c.receiver());
		if (owner.isPrimitive() || JTypes.isNull(owner)) throw new Unsupported("appel sur " + owner);
		return Resolver.method(owner, c.name(), argTypes(c.args()), classLiterals(c.args()), false);
	}

	private Resolver.Resolved resolveSuper(JExpr.SuperCall c) {
		var parent = context.members.getSuperclass();
		if (parent == null) throw new Unsupported("super sans parent");
		return Resolver.method(parent, c.name(), argTypes(c.args()), classLiterals(c.args()), false);
	}

	private Resolver.Resolved resolve(JExpr.StaticCall c) {
		return Resolver.method(c.owner(), c.name(), argTypes(c.args()), classLiterals(c.args()), true);
	}

	private Class<?> computeType(JExpr e) {
		return switch (e) {
			case JExpr.Lit l -> l.type();
			case JExpr.Read l -> l.var().type();
			case JExpr.This t -> context.type;
			case JExpr.Outer o -> o.target().type;
			case JExpr.Call c -> {
				var r = resolve(c);
				// T d'un Box<T>, Wrapper<T>, FunctionLeekValue<T> : get(), run()… rendent le type de la variable
				var g = genericOf(c.receiver());
				if (g != null && g.arg() != null && returnsClassTypeVariable(r)) yield g.arg().raw();
				yield r.returnType();
			}
			case JExpr.StaticCall c -> resolve(c).returnType();
			case JExpr.GetStatic g -> field(g).getType();
			case JExpr.Self c -> c.returnType();
			case JExpr.InstanceOf i -> {
				if (typeOf(i.expr()).isPrimitive()) throw new Unsupported("instanceof sur un primitif");
				yield boolean.class;
			}
			case JExpr.ClassLit c -> Class.class;
			case JExpr.New n -> n.type();
			case JExpr.NewArray n -> n.component().arrayType();
			case JExpr.NewInner n -> n.type();
			case JExpr.ArrayGet a -> {
				var t = typeOf(a.array());
				if (!t.isArray()) throw new Unsupported("[] sur " + t);
				yield t.getComponentType();
			}
			case JExpr.ArrayLength a -> {
				if (!typeOf(a.array()).isArray()) throw new Unsupported(".length sur " + typeOf(a.array()));
				yield int.class;
			}
			case JExpr.BindNonNull b -> {
				if (typeOf(b.expr()).isPrimitive()) throw new Unsupported("instanceof sur un primitif");
				yield boolean.class;
			}
			case JExpr.SuperCall c -> resolveSuper(c).returnType();
			case JExpr.Cast c -> c.type();
			case JExpr.Assign a -> a.var().type();
			case JExpr.Compound a -> {
				var t = a.var().type();
				var op = binaryType(new JExpr.Binary(a.op(), new JExpr.Read(a.var()), a.value()));
				if (!t.isPrimitive() || op == boolean.class && t != boolean.class) throw new Unsupported("composée sur " + t);
				yield t;
			}
			case JExpr.Inc i -> {
				if (JTypes.numeric(i.var().type()) == null || !i.var().type().isPrimitive()) throw new Unsupported("++ sur " + i.var().type());
				yield i.var().type();
			}
			case JExpr.Unary u -> {
				if (u.op().equals("!")) {
					if (JTypes.box(typeOf(u.expr())) != Boolean.class) throw new Unsupported("! sur " + typeOf(u.expr()));
					yield boolean.class;
				}
				var t = JTypes.promote(typeOf(u.expr()));
				if (t == null) throw new Unsupported("unaire " + u.op() + " sur " + typeOf(u.expr()));
				yield t;
			}
			case JExpr.Binary b -> binaryType(b);
			case JExpr.Cond c -> condType(typeOf(c.then()), typeOf(c.otherwise()));
		};
	}

	private static java.lang.reflect.Field field(JExpr.GetStatic g) {
		try {
			var f = g.owner().getField(g.name());
			if (!Modifier.isStatic(f.getModifiers())) throw new Unsupported("champ non statique " + g.name());
			return f;
		} catch (NoSuchFieldException ex) {
			throw new Unsupported("champ " + g.owner() + "." + g.name());
		}
	}

	private Class<?> binaryType(JExpr.Binary b) {
		var l = typeOf(b.left());
		var r = typeOf(b.right());
		switch (b.op()) {
			case "+", "-", "*", "/", "%" -> {
				if (l == String.class || r == String.class) throw new Unsupported("concaténation");
				var t = JTypes.promote(l, r);
				if (t == null) throw new Unsupported(b.op() + " sur " + l + ", " + r);
				return t;
			}
			case "&", "|", "^" -> {
				if (JTypes.box(l) == Boolean.class && JTypes.box(r) == Boolean.class) return boolean.class;
				var t = JTypes.promote(l, r);
				if (t != long.class && t != int.class) throw new Unsupported(b.op() + " sur " + l + ", " + r);
				return t;
			}
			case "<<", ">>", ">>>" -> {
				var t = JTypes.promote(l);
				var tr = JTypes.promote(r);
				if ((t != long.class && t != int.class) || (tr != long.class && tr != int.class)) throw new Unsupported("décalage sur " + l + ", " + r);
				return t;
			}
			case "<", ">", "<=", ">=" -> {
				if (JTypes.promote(l, r) == null) throw new Unsupported(b.op() + " sur " + l + ", " + r);
				return boolean.class;
			}
			case "==", "!=" -> {
				equalityKind(l, r);
				return boolean.class;
			}
			case "&&", "||" -> {
				if (JTypes.box(l) != Boolean.class || JTypes.box(r) != Boolean.class) throw new Unsupported(b.op() + " sur " + l + ", " + r);
				return boolean.class;
			}
		}
		throw new Unsupported("opérateur " + b.op());
	}

	/** Nature d'un `==` (JLS 15.21) : le type primitif de la comparaison, ou null pour une comparaison de références. */
	private static Class<?> equalityKind(Class<?> l, Class<?> r) {
		if (l.isPrimitive() || r.isPrimitive()) {
			if (JTypes.box(l) == Boolean.class && JTypes.box(r) == Boolean.class) return boolean.class;
			var t = JTypes.promote(l, r);
			if (t == null) throw new Unsupported("== sur " + l + ", " + r);
			return t;
		}
		if (!JTypes.subtypeRef(l, r) && !JTypes.subtypeRef(r, l) && !l.isInterface() && !r.isInterface()) {
			throw new Unsupported("== entre " + l + " et " + r);
		}
		return null;
	}

	/** Type d'un ternaire (JLS 15.25). */
	private static Class<?> condType(Class<?> a, Class<?> b) {
		if (a == b) return a;
		if (JTypes.box(a) == Boolean.class && JTypes.box(b) == Boolean.class) return boolean.class;
		var na = JTypes.numeric(a);
		var nb = JTypes.numeric(b);
		if (na != null && nb != null) {
			if (a.isPrimitive() && JTypes.unbox(b) == a) return a;
			if (b.isPrimitive() && JTypes.unbox(a) == b) return b;
			return JTypes.promote(a, b);
		}
		if (JTypes.isNull(a) && b.isPrimitive()) return JTypes.box(b);
		if (JTypes.isNull(b) && a.isPrimitive()) return JTypes.box(a);
		if (JTypes.isNull(a)) return b;
		if (JTypes.isNull(b)) return a;
		var ba = JTypes.box(a);
		var bb = JTypes.box(b);
		if (JTypes.subtypeRef(ba, bb)) return bb;
		if (JTypes.subtypeRef(bb, ba)) return ba;
		throw new Unsupported("ternaire entre " + a + " et " + b);
	}

	// ---- Émission ----

	/** Émet `e` et rend le type effacé laissé sur la pile (void pour un appel sans résultat). */
	public Class<?> emit(JExpr e) {
		switch (e) {
			case JExpr.Lit l -> {
				if (l.value() == null) cb.aconst_null();
				else if (l.value() instanceof Boolean bool) cb.loadConstant(bool ? 1 : 0);
				else cb.loadConstant((java.lang.constant.ConstantDesc) l.value());
				return l.type();
			}
			case JExpr.Read l -> {
				load(l.var());
				return l.var().type();
			}
			case JExpr.This t -> {
				cb.aload(0);
				return context.type;
			}
			case JExpr.Outer o -> {
				loadInstance(o.target());
				return o.target().type;
			}
			case JExpr.Call c -> {
				var m = resolve(c);
				Class<?> owner;
				ClassDesc ownerDesc;
				boolean isStatic = Modifier.isStatic(m.executable().getModifiers());
				if (c.receiver() == null) {
					// Appel non qualifié : qualifié par la classe où Java trouve la méthode
					var scope = methodScope(c.name());
					if (scope != context && Modifier.isProtected(m.executable().getModifiers())) {
						// Méthode protégée de l'IA appelée d'une classe interne : par une méthode d'accès, comme javac
						if (scope != aiContext || isStatic) throw new Unsupported("méthode protégée englobante " + c.name());
						loadInstance(scope);
						emitArgs(m, c.args());
						cb.invokestatic(aiContext.desc, accessor(m), accessorDesc(m), false);
						return m.erasedReturn();
					}
					owner = scope.members;
					ownerDesc = scope.desc;
					if (!isStatic) loadInstance(scope);
				} else {
					if (isStatic) throw new Unsupported("méthode statique sur une instance");
					owner = typeOf(c.receiver());
					ownerDesc = JTypes.desc(owner);
					emitAs(c.receiver(), owner);
				}
				emitArgs(m, c.args());
				var desc = methodDesc(m);
				if (isStatic) cb.invokestatic(ownerDesc, c.name(), desc, owner.isInterface());
				else if (owner.isInterface()) cb.invokeinterface(ownerDesc, c.name(), desc);
				else cb.invokevirtual(ownerDesc, c.name(), desc);
				return m.erasedReturn();
			}
			case JExpr.StaticCall c -> {
				var m = resolve(c);
				emitArgs(m, c.args());
				cb.invokestatic(JTypes.desc(c.owner()), c.name(), methodDesc(m), c.owner().isInterface());
				return m.erasedReturn();
			}
			case JExpr.Self c -> {
				loadInstance(aiContext);
				for (int i = 0; i < c.args().size(); i++) emitAs(c.args().get(i), c.parameters().get(i));
				var params = new ClassDesc[c.parameters().size()];
				for (int i = 0; i < params.length; i++) params[i] = JTypes.desc(c.parameters().get(i));
				cb.invokevirtual(aiContext.desc, c.name(), MethodTypeDesc.of(JTypes.desc(c.returnType()), params));
				return c.returnType();
			}
			case JExpr.InstanceOf i -> {
				emitValue(i.expr());
				cb.instanceOf(JTypes.desc(i.type()));
				return boolean.class;
			}
			case JExpr.ClassLit c -> {
				cb.ldc(JTypes.desc(c.type()));
				return Class.class;
			}
			case JExpr.GetStatic g -> {
				var f = field(g);
				cb.getstatic(JTypes.desc(g.owner()), g.name(), JTypes.desc(f.getType()));
				return f.getType();
			}
			case JExpr.New n -> {
				var m = Resolver.constructor(n.type(), argTypes(n.args()));
				var d = JTypes.desc(n.type());
				cb.new_(d);
				cb.dup();
				emitArgs(m, n.args());
				cb.invokespecial(d, ConstantDescs.INIT_NAME, methodDesc(m));
				return n.type();
			}
			case JExpr.ArrayLength a -> {
				emitValue(a.array());
				cb.arraylength();
				return int.class;
			}
			case JExpr.BindNonNull b -> {
				emitAs(b.expr(), Object.class);
				cb.dup();
				cb.astore(b.temp().slot());
				cb.instanceOf(ConstantDescs.CD_Object);
				return boolean.class;
			}
			case JExpr.ArrayGet a -> {
				var t = typeOf(a.array());
				emitAs(a.array(), t);
				emitAs(a.index(), int.class);
				cb.arrayLoad(kind(t.getComponentType()));
				return t.getComponentType();
			}
			case JExpr.SuperCall c -> {
				var m = resolveSuper(c);
				cb.aload(0);
				emitArgs(m, c.args());
				cb.invokespecial(JTypes.desc(context.members.getSuperclass()), c.name(), methodDesc(m));
				return m.erasedReturn();
			}
			case JExpr.NewInner n -> {
				cb.new_(n.desc());
				cb.dup();
				cb.aload(0);
				var params = new ClassDesc[n.parameters().size() + 1];
				params[0] = context.desc;
				for (int i = 0; i < n.args().size(); i++) {
					emitAs(n.args().get(i), n.parameters().get(i));
					params[i + 1] = JTypes.desc(n.parameters().get(i));
				}
				cb.invokespecial(n.desc(), ConstantDescs.INIT_NAME, MethodTypeDesc.of(ConstantDescs.CD_void, params));
				return n.type();
			}
			case JExpr.NewArray n -> {
				var component = n.component();
				cb.loadConstant(n.elements().size());
				if (component.isPrimitive()) cb.newarray(kind(component));
				else cb.anewarray(JTypes.desc(component));
				for (int i = 0; i < n.elements().size(); i++) {
					cb.dup();
					cb.loadConstant(i);
					emitAs(n.elements().get(i), component);
					cb.arrayStore(kind(component));
				}
				return component.arrayType();
			}
			case JExpr.Cast c -> {
				emitCast(c);
				return c.type();
			}
			case JExpr.Assign a -> {
				storePrepare(a.var());
				emitAs(a.value(), a.var().type());
				store(a.var(), true);
				return a.var().type();
			}
			case JExpr.Compound a -> {
				storePrepare(a.var());
				compound(a);
				store(a.var(), true);
				return a.var().type();
			}
			case JExpr.Inc i -> {
				return increment(i, true);
			}
			case JExpr.Unary u -> {
				if (u.op().equals("!")) return emitBoolean(e);
				var t = typeOf(e);
				emitAs(u.expr(), t);
				switch (kind(t)) {
					case LONG -> cb.lneg();
					case DOUBLE -> cb.dneg();
					case INT -> cb.ineg();
					case FLOAT -> cb.fneg();
					default -> throw new Unsupported("- sur " + t);
				}
				return t;
			}
			case JExpr.Binary b -> {
				return emitBinary(b);
			}
			case JExpr.Cond c -> {
				var t = typeOf(e);
				var otherwise = cb.newLabel();
				var end = cb.newLabel();
				jump(c.condition(), false, otherwise);
				emitAs(c.then(), t);
				cb.goto_(end);
				cb.labelBinding(otherwise);
				emitAs(c.otherwise(), t);
				cb.labelBinding(end);
				return t;
			}
		}
	}

	/** La locale d'une méthode englobante, lue depuis une classe anonyme : son champ `val$x`, créé à la première lecture. */
	private FieldVar captured(LocalVar l) {
		if (context.captures == null) throw new Unsupported("locale d'une autre méthode hors d'une classe anonyme");
		var f = context.captures.get(l);
		if (f == null) {
			f = new FieldVar(context, "val$" + l.name() + "$" + context.captures.size(), l.type(), l.generic());
			context.captures.put(l, f);
			context.builder.withField(f.name(), JTypes.desc(f.type()), java.lang.classfile.ClassFile.ACC_FINAL | java.lang.classfile.ClassFile.ACC_SYNTHETIC);
		}
		return f;
	}

	private boolean isForeign(LocalVar l) {
		return l.method() != null && l.method() != method;
	}

	private void load(Slot v) {
		if (v instanceof LocalVar l && isForeign(l)) {
			load(captured(l));
			return;
		}
		switch (v) {
			case LocalVar l -> cb.loadLocal(kind(l.type()), l.slot());
			case FieldVar f -> {
				loadInstance(f.owner());
				cb.getfield(f.owner().desc, f.name(), JTypes.desc(f.type()));
			}
			case MemberVar m -> {
				emitAs(m.receiver(), m.owner());
				cb.getfield(JTypes.desc(m.owner()), m.name(), JTypes.desc(m.type()));
			}
		}
	}

	/** Avant la valeur d'une écriture : le receveur d'un champ. */
	private void storePrepare(Slot v) {
		if (v instanceof LocalVar l && isForeign(l)) throw new Unsupported("écriture d'une locale capturée");
		if (v instanceof FieldVar f) loadInstance(f.owner());
		if (v instanceof MemberVar m) emitAs(m.receiver(), m.owner());
	}

	/** Lit la variable dont le receveur est déjà sur la pile (storePrepare), sans le consommer. */
	private void loadPrepared(Slot v) {
		switch (v) {
			case LocalVar l -> cb.loadLocal(kind(l.type()), l.slot());
			case FieldVar f -> {
				cb.dup();
				cb.getfield(f.owner().desc, f.name(), JTypes.desc(f.type()));
			}
			case MemberVar m -> {
				cb.dup();
				cb.getfield(JTypes.desc(m.owner()), m.name(), JTypes.desc(m.type()));
			}
		}
	}

	/** Range la valeur du sommet de pile ; `keep` : elle y reste aussi, valeur de l'expression. */
	private void store(Slot v, boolean keep) {
		var k = kind(v.type());
		switch (v) {
			case LocalVar l -> {
				if (keep) { if (k.slotSize() == 2) cb.dup2(); else cb.dup(); }
				cb.storeLocal(k, l.slot());
			}
			case FieldVar f -> {
				if (keep) { if (k.slotSize() == 2) cb.dup2_x1(); else cb.dup_x1(); }
				cb.putfield(f.owner().desc, f.name(), JTypes.desc(f.type()));
			}
			case MemberVar m -> {
				if (keep) { if (k.slotSize() == 2) cb.dup2_x1(); else cb.dup_x1(); }
				cb.putfield(JTypes.desc(m.owner()), m.name(), JTypes.desc(m.type()));
			}
		}
	}

	/** Laisse `(T) (x op e)` sur la pile. */
	private void compound(JExpr.Compound a) {
		var v = a.var();
		loadPrepared(v);
		var binary = new JExpr.Binary(a.op(), new JExpr.Read(v), a.value());
		var t = emitBinary(binary, true);
		if (t != v.type()) primitiveConversion(t, v.type());
	}

	/** `x++` & co ; `used` : la valeur de l'expression reste sur la pile. */
	private Class<?> increment(JExpr.Inc i, boolean used) {
		var v = i.var();
		var k = kind(v.type());
		if (v instanceof LocalVar l && isForeign(l)) throw new Unsupported("incrément d'une locale capturée");
		if (k == TypeKind.INT && v instanceof LocalVar l) {
			if (used && !i.prefix()) cb.iload(l.slot());
			cb.iinc(l.slot(), i.increment() ? 1 : -1);
			if (used && i.prefix()) cb.iload(l.slot());
			return v.type();
		}
		storePrepare(v);
		loadPrepared(v);
		if (used && !i.prefix()) {
			// L'ancienne valeur, sous le receveur d'un champ
			if (!(v instanceof LocalVar)) { if (k.slotSize() == 2) cb.dup2_x1(); else cb.dup_x1(); }
			else { if (k.slotSize() == 2) cb.dup2(); else cb.dup(); }
		}
		switch (k) {
			case LONG -> { cb.lconst_1(); if (i.increment()) cb.ladd(); else cb.lsub(); }
			case DOUBLE -> { cb.dconst_1(); if (i.increment()) cb.dadd(); else cb.dsub(); }
			case INT -> { cb.iconst_1(); if (i.increment()) cb.iadd(); else cb.isub(); }
			default -> throw new Unsupported("++ sur " + v.type());
		}
		store(v, used && i.prefix());
		return v.type();
	}

	private static MethodTypeDesc methodDesc(Resolver.Resolved m) {
		var params = m.parameters();
		var descs = new ClassDesc[params.length];
		for (int i = 0; i < params.length; i++) descs[i] = JTypes.desc(params[i]);
		var ret = m.executable() instanceof Method method ? JTypes.desc(method.getReturnType()) : ConstantDescs.CD_void;
		return MethodTypeDesc.of(ret, descs);
	}

	private void emitArgs(Resolver.Resolved m, List<JExpr> args) {
		var params = m.parameters();
		if (!m.varargs()) {
			for (int i = 0; i < args.size(); i++) emitAs(args.get(i), params[i]);
			return;
		}
		int fixed = params.length - 1;
		for (int i = 0; i < fixed; i++) emitAs(args.get(i), params[i]);
		var component = params[fixed].getComponentType();
		cb.loadConstant(args.size() - fixed);
		if (component.isPrimitive()) cb.newarray(kind(component));
		else cb.anewarray(JTypes.desc(component));
		for (int i = fixed; i < args.size(); i++) {
			cb.dup();
			cb.loadConstant(i - fixed);
			emitAs(args.get(i), component);
			cb.arrayStore(kind(component));
		}
	}

	public static TypeKind kind(Class<?> c) {
		return TypeKind.from(JTypes.desc(c));
	}

	/** Émet `e` converti vers `target` par conversion d'affectation (JLS 5.2) ou d'invocation (5.3). */
	public void emitAs(JExpr e, Class<?> target) {
		var stat = typeOf(e);
		var stack = emit(e);
		try {
			convert(stack, stat, target);
		} catch (Unsupported u) {
			if (Unsupported.tracing()) System.out.println("  expression : " + String.valueOf(e).substring(0, Math.min(600, String.valueOf(e).length())));
			throw u;
		}
	}

	/** Émet `e` et laisse sa valeur sur la pile avec son type statique. */
	public Class<?> emitValue(JExpr e) {
		var stat = typeOf(e);
		convert(emit(e), stat, stat);
		return stat;
	}

	/** Émet `e` comme une instruction : sa valeur, s'il en a une, est dépilée (ou jamais calculée, comme javac). */
	public void emitStatement(JExpr e) {
		switch (e) {
			case JExpr.Assign a -> {
				typeOf(e);
				storePrepare(a.var());
				emitAs(a.value(), a.var().type());
				store(a.var(), false);
				return;
			}
			case JExpr.Compound a -> {
				typeOf(e);
				storePrepare(a.var());
				compound(a);
				store(a.var(), false);
				return;
			}
			case JExpr.Inc i -> {
				typeOf(e);
				increment(i, false);
				return;
			}
			default -> {}
		}
		var stack = emit(e);
		if (stack == void.class) return;
		if (kind(stack).slotSize() == 2) cb.pop2(); else cb.pop();
	}

	private void convert(Class<?> stack, Class<?> stat, Class<?> target) {
		if (stack == void.class) throw new Unsupported("valeur void utilisée");
		// Résultat effacé d'une méthode générique : casté au type déduit, seulement si la cible l'exige
		if (!stat.isPrimitive() && stack != stat && !JTypes.isNull(stat) && !(target != null && !target.isPrimitive() && JTypes.subtypeRef(stack, target))) {
			cb.checkcast(JTypes.desc(stat));
		}
		if (target == null || target == stat) return;
		if (stat.isPrimitive() && target.isPrimitive()) {
			if (!JTypes.widensPrimitive(stat, target)) throw new Unsupported("conversion " + stat + " -> " + target);
			primitiveConversion(stat, target);
		} else if (stat.isPrimitive()) {
			var box = JTypes.box(stat);
			if (!JTypes.subtypeRef(box, target)) throw new Unsupported("boxing " + stat + " -> " + target);
			box(stat);
		} else if (target.isPrimitive()) {
			var p = JTypes.unbox(stat);
			if (p == null || !JTypes.widensPrimitive(p, target)) throw new Unsupported("unboxing " + stat + " -> " + target);
			unbox(stat);
			primitiveConversion(p, target);
		} else if (!JTypes.subtypeRef(stat, target)) {
			throw new Unsupported("conversion " + stat + " -> " + target);
		}
	}

	private void box(Class<?> primitive) {
		var box = JTypes.box(primitive);
		cb.invokestatic(JTypes.desc(box), "valueOf", MethodTypeDesc.of(JTypes.desc(box), JTypes.desc(primitive)));
	}

	private void unbox(Class<?> boxed) {
		var p = JTypes.unbox(boxed);
		cb.invokevirtual(JTypes.desc(boxed), p.getName() + "Value", MethodTypeDesc.of(JTypes.desc(p)));
	}

	private void primitiveConversion(Class<?> from, Class<?> to) {
		if (from == to) return;
		var a = kind(from);
		var b = kind(to);
		// byte, short, char : des int sur la pile
		if (a == TypeKind.BYTE || a == TypeKind.SHORT || a == TypeKind.CHAR) a = TypeKind.INT;
		if (a == b) return;
		if (b == TypeKind.BYTE || b == TypeKind.SHORT || b == TypeKind.CHAR) {
			if (a != TypeKind.INT) cb.conversion(a, TypeKind.INT);
			cb.conversion(TypeKind.INT, b);
			return;
		}
		cb.conversion(a, b);
	}

	/** Cast Java (JLS 5.5). */
	private void emitCast(JExpr.Cast c) {
		var target = c.type();
		var stat = typeOf(c.expr());
		if (stat.isPrimitive() && target.isPrimitive()) {
			emitAs(c.expr(), stat);
			if (stat == boolean.class || target == boolean.class) {
				if (stat != target) throw new Unsupported("cast " + stat + " -> " + target);
				return;
			}
			primitiveConversion(stat, target);
		} else if (stat.isPrimitive()) {
			// Boxing puis élargissement de référence
			if (!JTypes.subtypeRef(JTypes.box(stat), target)) throw new Unsupported("cast " + stat + " -> " + target);
			emitAs(c.expr(), stat);
			box(stat);
		} else if (target.isPrimitive()) {
			var p = JTypes.unbox(stat);
			if (p != null) {
				// Unboxing puis élargissement
				if (!JTypes.widensPrimitive(p, target)) throw new Unsupported("cast " + stat + " -> " + target);
				emitAs(c.expr(), target);
			} else {
				// Object, Number… : checkcast vers la boîte du primitif, puis unboxing
				var box = JTypes.box(target);
				if (!JTypes.subtypeRef(box, stat)) throw new Unsupported("cast " + stat + " -> " + target);
				emitAs(c.expr(), stat);
				cb.checkcast(JTypes.desc(box));
				unbox(box);
			}
		} else {
			// Cast de référence : checkcast sauf s'il est redondant, comme javac
			var stack = emit(c.expr());
			if (!JTypes.isNull(stat) && !JTypes.subtypeRef(stack, target)) cb.checkcast(JTypes.desc(target));
		}
	}

	private Class<?> emitBinary(JExpr.Binary b) {
		return emitBinary(b, false);
	}

	/** `leftOnStack` : l'opérande gauche (de son type statique) est déjà sur la pile. */
	private Class<?> emitBinary(JExpr.Binary b, boolean leftOnStack) {
		var t = typeOf(b);
		switch (b.op()) {
			case "<", ">", "<=", ">=", "==", "!=", "&&", "||" -> {
				return emitBoolean(b);
			}
			case "<<", ">>", ">>>" -> {
				emitLeft(b.left(), t, leftOnStack);
				var tr = JTypes.promote(typeOf(b.right()));
				emitAs(b.right(), tr);
				if (tr == long.class) cb.l2i();
				var op = switch (b.op()) {
					case "<<" -> t == long.class ? Opcode.LSHL : Opcode.ISHL;
					case ">>" -> t == long.class ? Opcode.LSHR : Opcode.ISHR;
					default -> t == long.class ? Opcode.LUSHR : Opcode.IUSHR;
				};
				cb.with(OperatorInstruction.of(op));
				return t;
			}
		}
		var operand = t == boolean.class ? boolean.class : t;
		emitLeft(b.left(), operand, leftOnStack);
		emitAs(b.right(), operand);
		var k = kind(t);
		if (k == TypeKind.BOOLEAN) k = TypeKind.INT;
		cb.with(OperatorInstruction.of(arithmetic(b.op(), k)));
		return t;
	}

	private void emitLeft(JExpr left, Class<?> target, boolean onStack) {
		if (!onStack) {
			emitAs(left, target);
			return;
		}
		var stat = typeOf(left);
		convert(stat, stat, target);
	}

	private static Opcode arithmetic(String op, TypeKind k) {
		return switch (k) {
			case LONG -> switch (op) { case "+" -> Opcode.LADD; case "-" -> Opcode.LSUB; case "*" -> Opcode.LMUL; case "/" -> Opcode.LDIV; case "%" -> Opcode.LREM; case "&" -> Opcode.LAND; case "|" -> Opcode.LOR; case "^" -> Opcode.LXOR; default -> throw new Unsupported(op); };
			case INT -> switch (op) { case "+" -> Opcode.IADD; case "-" -> Opcode.ISUB; case "*" -> Opcode.IMUL; case "/" -> Opcode.IDIV; case "%" -> Opcode.IREM; case "&" -> Opcode.IAND; case "|" -> Opcode.IOR; case "^" -> Opcode.IXOR; default -> throw new Unsupported(op); };
			case DOUBLE -> switch (op) { case "+" -> Opcode.DADD; case "-" -> Opcode.DSUB; case "*" -> Opcode.DMUL; case "/" -> Opcode.DDIV; case "%" -> Opcode.DREM; default -> throw new Unsupported(op); };
			case FLOAT -> switch (op) { case "+" -> Opcode.FADD; case "-" -> Opcode.FSUB; case "*" -> Opcode.FMUL; case "/" -> Opcode.FDIV; case "%" -> Opcode.FREM; default -> throw new Unsupported(op); };
			default -> throw new Unsupported(op + " sur " + k);
		};
	}

	/** Valeur booléenne d'une condition, comme javac : sauts puis 1 ou 0. */
	private Class<?> emitBoolean(JExpr e) {
		var no = cb.newLabel();
		var end = cb.newLabel();
		jump(e, false, no);
		cb.iconst_1();
		cb.goto_(end);
		cb.labelBinding(no);
		cb.iconst_0();
		cb.labelBinding(end);
		return boolean.class;
	}

	/** Saute à `target` quand la condition `e` vaut `when`, continue sinon. */
	public void jump(JExpr e, boolean when, Label target) {
		if (typeOf(e) != boolean.class && typeOf(e) != Boolean.class) throw new Unsupported("condition de type " + typeOf(e));
		switch (e) {
			case JExpr.Lit l when l.value() instanceof Boolean v -> {
				if (v == when) cb.goto_(target);
				return;
			}
			case JExpr.Unary u when u.op().equals("!") -> {
				jump(u.expr(), !when, target);
				return;
			}
			case JExpr.Binary b when b.op().equals("&&") || b.op().equals("||") -> {
				boolean and = b.op().equals("&&");
				if (and != when) {
					// && qui saute sur faux, || qui saute sur vrai : chaque opérande peut décider seul
					jump(b.left(), when, target);
					jump(b.right(), when, target);
				} else {
					var skip = cb.newLabel();
					jump(b.left(), !when, skip);
					jump(b.right(), when, target);
					cb.labelBinding(skip);
				}
				return;
			}
			case JExpr.Binary b when isComparison(b.op()) -> {
				compareJump(b, when, target);
				return;
			}
			default -> {}
		}
		emitAs(e, boolean.class);
		if (when) cb.ifne(target); else cb.ifeq(target);
	}

	private static boolean isComparison(String op) {
		return switch (op) { case "<", ">", "<=", ">=", "==", "!=" -> true; default -> false; };
	}

	private static String negate(String op) {
		return switch (op) { case "<" -> ">="; case ">" -> "<="; case "<=" -> ">"; case ">=" -> "<"; case "==" -> "!="; default -> "=="; };
	}

	private void compareJump(JExpr.Binary b, boolean when, Label target) {
		var l = typeOf(b.left());
		var r = typeOf(b.right());
		// Condition du saut : l'opérateur tel quel pour sauter sur vrai, sa négation pour sauter sur faux
		var op = when ? b.op() : negate(b.op());
		Class<?> t;
		if (b.op().equals("==") || b.op().equals("!=")) {
			t = equalityKind(l, r);
			if (t == null) {
				if (b.right() instanceof JExpr.Lit lit && lit.value() == null) {
					emitValue(b.left());
					if (op.equals("==")) cb.ifnull(target); else cb.ifnonnull(target);
				} else if (b.left() instanceof JExpr.Lit lit && lit.value() == null) {
					emitValue(b.right());
					if (op.equals("==")) cb.ifnull(target); else cb.ifnonnull(target);
				} else {
					emitValue(b.left());
					emitValue(b.right());
					if (op.equals("==")) cb.if_acmpeq(target); else cb.if_acmpne(target);
				}
				return;
			}
		} else {
			t = JTypes.promote(l, r);
		}
		emitAs(b.left(), t);
		emitAs(b.right(), t);
		var k = kind(t);
		if (k == TypeKind.INT || k == TypeKind.BOOLEAN) {
			switch (op) {
				case "<" -> cb.if_icmplt(target);
				case ">" -> cb.if_icmpgt(target);
				case "<=" -> cb.if_icmple(target);
				case ">=" -> cb.if_icmpge(target);
				case "==" -> cb.if_icmpeq(target);
				default -> cb.if_icmpne(target);
			}
			return;
		}
		// long : lcmp ; double/float : NaN rend toute comparaison fausse, comme javac (cmpg pour < et <=)
		boolean lessFamily = b.op().equals("<") || b.op().equals("<=");
		switch (k) {
			case LONG -> cb.lcmp();
			case DOUBLE -> { if (lessFamily) cb.dcmpg(); else cb.dcmpl(); }
			case FLOAT -> { if (lessFamily) cb.fcmpg(); else cb.fcmpl(); }
			default -> throw new Unsupported("comparaison sur " + t);
		}
		switch (op) {
			case "<" -> cb.iflt(target);
			case ">" -> cb.ifgt(target);
			case "<=" -> cb.ifle(target);
			case ">=" -> cb.ifge(target);
			case "==" -> cb.ifeq(target);
			default -> cb.ifne(target);
		}
	}

	/** Retour d'une méthode de type `returnType` avec la valeur de `e` (null : `return;`). */
	public void emitReturn(JExpr e, Class<?> returnType) {
		if (e == null) {
			cb.return_();
		} else {
			emitAs(e, returnType);
			cb.return_(kind(returnType));
		}
		method.alive = false;
	}


	// ---- Conversions LeekScript, portées de JavaWriter (mêmes noms, même Java) ----

	// Talons des classes générées (`u_X` → AI_N$u_X), pour typer comme javac
	private java.util.Map<String, Class<?>> stubs = java.util.Map.of();

	public void setStubs(java.util.Map<String, Class<?>> stubs) {
		this.stubs = stubs;
	}

	/** La classe d'un nom de type écrit par JavaWriter : talon d'une classe générée, sinon classe du runtime. */
	public Class<?> typeNamed(String name) {
		var stub = stubs.get(name);
		return stub != null ? stub : runtimeType(name);
	}

	/**
	 * Une classe du runtime par son nom simple, comme javac le résout dans la classe de l'IA : une
	 * classe membre héritée (NativeObjectLeekValue d'AI) passe avant les imports.
	 */
	public Class<?> runtimeType(String name) {
		// FunctionLeekValue<T> : effacé ; run() rend alors Object là où javac connaît T, et les
		// conversions que javac n'écrit pas (T vers un primitif…) font repasser le programme par lui
		if (name.startsWith("FunctionLeekValue<")) return leekscript.runner.values.FunctionLeekValue.class;
		if (!name.contains(".") && !name.contains("<")) {
			for (var c = aiContext.members; c != null; c = c.getSuperclass()) {
				for (var member : c.getDeclaredClasses()) {
					if (member.getSimpleName().equals(name) && (Modifier.isPublic(member.getModifiers()) || Modifier.isProtected(member.getModifiers()))) return member;
				}
			}
		}
		return JTypes.forName(name);
	}

	/** Classe Java du type LeekScript, telle que JavaWriter l'écrit (getJavaPrimitiveName). */
	public Class<?> primitiveClass(Type type) {
		return typeNamed(type.getJavaPrimitiveName(version));
	}

	/** Classe Java boxée du type LeekScript (getJavaName). */
	public Class<?> javaClass(Type type) {
		return typeNamed(type.getJavaName(version));
	}

	private static Type codegenType(Expression expression) {
		return expression.hasObjectJavaResult() ? Type.ANY : expression.getType();
	}

	/** L'IA, telle que JavaWriter.getAIThis() la désigne (`AI_N.this`). */
	public JExpr aiThis() {
		return context == aiContext ? new JExpr.This() : new JExpr.Outer(aiContext);
	}

	/** Charge l'instance de `target`, la classe en cours ou une classe qui l'englobe (`this$0`…). */
	private void loadInstance(ClassContext target) {
		cb.aload(0);
		for (var c = context; c != target; c = c.outer) {
			if (c.outer == null) throw new Unsupported("instance englobante introuvable");
			cb.getfield(c.desc, "this$0", c.outer.desc);
		}
	}

	// Méthodes d'accès aux méthodes protégées de l'IA depuis ses classes internes (`access$N`), écrites à la fin
	private final java.util.LinkedHashMap<Method, String> accessors = new java.util.LinkedHashMap<>();

	private String accessor(Resolver.Resolved m) {
		return accessors.computeIfAbsent((Method) m.executable(), k -> "access$" + accessors.size());
	}

	private MethodTypeDesc accessorDesc(Resolver.Resolved m) {
		var method = (Method) m.executable();
		var params = new ArrayList<ClassDesc>();
		params.add(aiContext.desc);
		for (var p : method.getParameterTypes()) params.add(JTypes.desc(p));
		return MethodTypeDesc.of(JTypes.desc(method.getReturnType()), params);
	}

	/** Les méthodes d'accès demandées : `static R access$N(AI_N self, params) { return self.m(params); }`. */
	public void writeAccessors(java.lang.classfile.ClassBuilder builder) {
		for (var entry : accessors.entrySet()) {
			var method = entry.getKey();
			var params = new ArrayList<ClassDesc>();
			params.add(aiContext.desc);
			for (var p : method.getParameterTypes()) params.add(JTypes.desc(p));
			var desc = MethodTypeDesc.of(JTypes.desc(method.getReturnType()), params);
			builder.withMethodBody(entry.getValue(), desc, java.lang.classfile.ClassFile.ACC_STATIC | java.lang.classfile.ClassFile.ACC_SYNTHETIC, code -> {
				code.aload(0);
				int slot = 1;
				for (var p : method.getParameterTypes()) {
					code.loadLocal(kind(p), slot);
					slot += kind(p).slotSize();
				}
				var own = new ClassDesc[method.getParameterCount()];
				for (int i = 0; i < own.length; i++) own[i] = JTypes.desc(method.getParameterTypes()[i]);
				code.invokevirtual(aiContext.desc, method.getName(), MethodTypeDesc.of(JTypes.desc(method.getReturnType()), own));
				if (method.getReturnType() == void.class) code.return_(); else code.return_(kind(method.getReturnType()));
			});
		}
	}

	/** La classe englobante (ou courante) où Java trouve la méthode `name` appelée sans receveur. */
	private ClassContext methodScope(String name) {
		for (var c = context; c != null; c = c.outer) {
			if (Resolver.hasMethod(c.members, name)) return c;
		}
		throw new Unsupported("méthode inconnue " + name);
	}

	public List<JExpr> exprs(MainLeekBlock mainblock, List<? extends Expression> list) {
		var r = new ArrayList<JExpr>(list.size());
		for (var e : list) r.add(expr(mainblock, e));
		return r;
	}

	public JExpr expr(MainLeekBlock mainblock, Expression e) {
		return e.toBytecode(mainblock, this);
	}

	public JExpr getBoolean(MainLeekBlock mainblock, Expression expression) {
		var type = codegenType(expression);
		if (type == Type.BOOL) return expr(mainblock, expression);
		if (type == Type.INT) return new JExpr.Binary("!=", expr(mainblock, expression), JExpr.of(0l));
		return JExpr.call("bool", expr(mainblock, expression));
	}

	public JExpr getString(MainLeekBlock mainblock, Expression expression) {
		if (codegenType(expression) == Type.STRING) return expr(mainblock, expression);
		// Dans une classe, `string(` nu viserait celle de NativeObjectLeekValue : celle de l'IA
		if (mainblock.getWordCompiler().getCurrentClass() != null) return new JExpr.Call(aiThis(), "string", List.of(expr(mainblock, expression)));
		return JExpr.call("string", expr(mainblock, expression));
	}

	/** La classe courante passée aux accès dynamiques (getCurrentClassVariable) : null hors d'une classe. */
	public JExpr classVariable(MainLeekBlock mainblock) {
		var name = mainblock.getWordCompiler().getCurrentClassVariable();
		return name == null ? JExpr.nul() : new JExpr.Read(fieldNamed(name));
	}

	/**
	 * Le début d'un foreach : `final var $ar = ops(<conteneur>, n); if (isIterable($ar)) {` ; rend
	 * l'emplacement de `$ar`, et `end` reçoit le saut quand le conteneur n'est pas itérable.
	 */
	public LocalVar foreachContainer(JExpr container, int operations, Label end) {
		JExpr value = JExpr.call("ops", container, JExpr.of(operations));
		var ar = declare(new Object(), typeOf(value), "$ar");
		emitStatement(new JExpr.Assign(ar, value));
		jump(JExpr.call("isIterable", new JExpr.Read(ar)), false, end);
		return ar;
	}

	/** `var $i = iterator($ar);` */
	public LocalVar foreachIterator(LocalVar ar) {
		var it = declare(new Object(), java.util.Iterator.class, "$i");
		emitStatement(new JExpr.Assign(it, JExpr.call("iterator", new JExpr.Read(ar))));
		return it;
	}

	/** `while ($i.hasNext()) { var $v = $i.next();` : le test (étiquette `test`) puis l'entrée courante. */
	public LocalVar foreachNext(LocalVar it, Label test, Label end) {
		bind(test);
		jump(new JExpr.Call(new JExpr.Read(it), "hasNext", List.of()), false, end);
		var v = declare(new Object(), java.util.Map.Entry.class, "$v");
		emitStatement(new JExpr.Assign(v, new JExpr.Cast(java.util.Map.Entry.class, new JExpr.Call(new JExpr.Read(it), "next", List.of()))));
		return v;
	}

	/** openCastObject : `realBox(x)` vers Double, un cast sinon. */
	public JExpr castObject(String javaType, JExpr e) {
		if (javaType.equals("Double")) return JExpr.call("realBox", e);
		return new JExpr.Cast(typeNamed(javaType), e);
	}

	public JExpr getInt(MainLeekBlock mainblock, Expression expression) {
		if (codegenType(expression) == Type.INT) return expr(mainblock, expression);
		return JExpr.call("longint", expr(mainblock, expression));
	}

	public JExpr compileTyped(MainLeekBlock mainblock, Expression expression) {
		return compileTyped(mainblock, expression, expression.getType());
	}

	public JExpr compileTyped(MainLeekBlock mainblock, Expression expression, Type type) {
		if (expression.hasObjectJavaResult()) return compileConvertTyped(mainblock, 0, expression, type);
		return expr(mainblock, expression);
	}

	public JExpr compileLoad(MainLeekBlock mainblock, Expression e) {
		var t = e.getType();
		if (t == Type.NULL || t == Type.BOOL || t.isNumber() || t == Type.STRING || t.isArray()) return expr(mainblock, e);
		return JExpr.call("load", expr(mainblock, e));
	}

	/** compileDefaultValue : la valeur par défaut d'un paramètre ; `null` reste null, casté pour être typé. */
	public JExpr compileDefaultValue(MainLeekBlock mainblock, int index, Expression value, Type type) {
		if (value.getType() == Type.NULL) {
			var e = expr(mainblock, value);
			return type != Type.ANY && !type.isPrimitive() ? new JExpr.Cast(primitiveClass(type), e) : e;
		}
		return compileConvertTyped(mainblock, index, value, type);
	}

	public JExpr compileConvert(MainLeekBlock mainblock, int index, Expression value, Type type) {
		return compileConvert(mainblock, index, value, type, false);
	}

	public JExpr compileConvertTyped(MainLeekBlock mainblock, int index, Expression value, Type type) {
		return compileConvert(mainblock, index, value, type, true);
	}

	private static boolean convertsAsIs(Type valueType, Type type) {
		return type != Type.BIG_INT && type.accepts(valueType) == CastType.EQUALS;
	}

	private JExpr compileConvert(MainLeekBlock mainblock, int index, Expression value, Type type, boolean typed) {
		if (type == Type.BIG_INT || value.getType() == Type.BIG_INT) throw new Unsupported("big_integer");
		if (type == Type.REAL && value.getType().isIntOrReal()) {
			return new JExpr.Call(new JExpr.Cast(Number.class, expr(mainblock, value)), "doubleValue", List.of());
		}
		if (type == Type.INT && value.getType().isIntOrReal()) {
			return new JExpr.Call(new JExpr.Cast(Number.class, expr(mainblock, value)), "longValue", List.of());
		}
		if (convertsAsIs(value.getType(), type) && !(typed && value.hasObjectJavaResult())) {
			return expr(mainblock, value);
		}
		var converter = containerConverter(type);
		if (converter != null) {
			// Contrôle du contenu (v5, valeur any) : un champ ContentType de la classe, pas encore émis
			if (LanguageFeature.TYPED_CONTENT_CHECK.enabledIn(version) && value.getType() == Type.ANY) throw new Unsupported("contrôle du contenu d'un conteneur");
			return JExpr.call(converter, JExpr.of(index), expr(mainblock, value));
		}
		if (type == Type.INT) {
			if (value.getType() == Type.REAL) return new JExpr.Cast(long.class, new JExpr.Cast(double.class, expr(mainblock, value)));
			return JExpr.call(numberConversion(mainblock, value, "longint"), expr(mainblock, value));
		}
		if (type == Type.REAL) {
			if (value.getType() == Type.INT) return new JExpr.Cast(double.class, new JExpr.Cast(long.class, expr(mainblock, value)));
			return JExpr.call(numberConversion(mainblock, value, "real"), expr(mainblock, value));
		}
		if (type == Type.BOOL) return getBoolean(mainblock, value);
		// int?, real?
		if (type instanceof CompoundType ct && ct.getTypes().size() == 2 && ct.containsNull()) {
			for (var t : ct.getTypes()) {
				if (t == Type.NULL) continue;
				if (t == Type.REAL && value.getType() == Type.INT) return new JExpr.Cast(double.class, new JExpr.Cast(long.class, expr(mainblock, value)));
				if (t == Type.INT && value.getType() == Type.REAL) return new JExpr.Cast(long.class, new JExpr.Cast(double.class, expr(mainblock, value)));
				if (t == Type.INT) return JExpr.call("longintOrNull", expr(mainblock, value));
				if (t == Type.REAL) return JExpr.call("realOrNull", expr(mainblock, value));
			}
		}
		if (type != Type.ANY) {
			var v = expr(mainblock, value);
			if (!type.isPrimitive()) v = new JExpr.Cast(Object.class, v);
			return new JExpr.Cast(primitiveClass(type), v);
		}
		return expr(mainblock, value);
	}

	private String containerConverter(Type type) {
		if (type.isArray()) return version >= 4 ? "toArray" : "toLegacyArray";
		if (type.isArrayOrNull()) return version >= 4 ? "toArrayOrNull" : "toLegacyArrayOrNull";
		if (type.isMap()) return "toMap";
		if (type.isMapOrNull()) return "toMapOrNull";
		if (type.isSet()) return "toSet";
		if (type.isSetOrNull()) return "toSetOrNull";
		return null;
	}

	private static String numberConversion(MainLeekBlock mainblock, Expression value, String function) {
		if (LanguageFeature.NULL_TO_NUMBER_ERROR.enabledIn(mainblock.getVersion()) && value.getType().canBeNull()) return function + "NotNull";
		return function;
	}

	/** Conversion du résultat Object d'une opération (add, sub…) vers `castType` (openResultConversion). */
	public JExpr resultConversion(Type castType, JExpr call) {
		var c = callConversion(castType.assertNotNull() == Type.BIG_INT ? Type.BIG_INT : castType, call);
		if (c != null) return c;
		if (castType != Type.ANY) return new JExpr.Cast(primitiveClass(castType), call);
		return call;
	}

	/** openFieldResultConversion. */
	public JExpr fieldResultConversion(Type castType, JExpr call) {
		if (castType == Type.BOOL) return JExpr.call("bool", call);
		var c = callConversion(castType, call);
		return c != null ? c : call;
	}

	private JExpr callConversion(Type castType, JExpr call) {
		if (castType == Type.INT) return JExpr.call("longint", call);
		if (castType == Type.REAL) return JExpr.call("real", call);
		if (castType == Type.BIG_INT) throw new Unsupported("big_integer");
		return null;
	}

	private static final leekscript.compiler.JavaWriter DEFAULT_PROBE = new leekscript.compiler.JavaWriter(false, "PROBE", true);
	private static final java.util.regex.Pattern NEW_DEFAULT = java.util.regex.Pattern.compile("new (\\w+)\\(PROBE\\.this\\)");

	/** La valeur par défaut d'un type, lue du Java de Type.getDefaultValue : un littéral ou `new X(ai)`. */
	public JExpr defaultValue(Type type) {
		var java = type.getDefaultValue(DEFAULT_PROBE, version);
		switch (java) {
			case "null": return JExpr.nul();
			case "0l": return JExpr.of(0l);
			case "0.0": return JExpr.of(0.0);
			case "false": return JExpr.of(false);
			case "\"\"": return JExpr.of("");
			case "new BigIntegerValue(PROBE.this, 0)": return new JExpr.New(leekscript.runner.values.BigIntegerValue.class, List.of(aiThis(), JExpr.of(0)));
		}
		var m = NEW_DEFAULT.matcher(java);
		if (m.matches()) return new JExpr.New(typeNamed(m.group(1)), List.of(aiThis()));
		throw new Unsupported("valeur par défaut " + java);
	}

	/** writeElement : une valeur insérée dans un conteneur d'élément typé, contrôlée non nulle si besoin. */
	public JExpr element(MainLeekBlock mainblock, Type elementType, Expression value) {
		if (leekscript.compiler.JavaWriter.checksNullElement(mainblock, elementType, value)) {
			var name = JTypes.javaStringValue(leekscript.compiler.expression.LeekFileContent.javaString(elementType.toString()));
			return JExpr.call("nonNullElement", expr(mainblock, value), JExpr.of(name));
		}
		return expr(mainblock, value);
	}
}
