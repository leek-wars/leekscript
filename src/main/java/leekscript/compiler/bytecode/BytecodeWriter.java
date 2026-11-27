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
	public sealed interface Slot permits LocalVar, FieldVar {
		Class<?> type();
		String name();
	}

	/** Variable locale : son emplacement, son type Java et son nom LeekScript (pour le débogage). */
	public record LocalVar(int slot, Class<?> type, String name) implements Slot {}

	/** Champ d'instance de la classe générée (globale…). */
	public record FieldVar(String name, Class<?> type) implements Slot {}

	public final Options options;
	public final int version;
	/** Classe générée (receveur de `this`) et la classe du runtime qu'elle étend, où se résolvent ses méthodes. */
	public final ClassDesc thisDesc;
	public final Class<?> thisClass;

	private CodeBuilder cb;
	private int nextSlot;
	private final ArrayList<Integer> scopes = new ArrayList<>();
	private final IdentityHashMap<Object, LocalVar> locals = new IdentityHashMap<>();
	private final IdentityHashMap<JExpr, Class<?>> types = new IdentityHashMap<>();

	/** Cibles de break / continue des boucles englobantes. */
	public record Loop(Label breakLabel, Label continueLabel) {}
	private final ArrayList<Loop> loops = new ArrayList<>();

	// Enveloppes génériques des fonctions système (JavaWriter.generateGenericFunction), écrites après runIA
	private final java.util.LinkedHashMap<String, List<leekscript.runner.CallableVersion>> genericFunctions = new java.util.LinkedHashMap<>();
	private final java.util.HashSet<String> strictGenericFunctions = new java.util.HashSet<>();

	// Lignes : repère Java -> ligne LeekScript, et fichiers dans l'ordre de leur première ligne
	private final TreeMap<Integer, LineMapping> lines = new TreeMap<>();
	private final ArrayList<AIFile> files = new ArrayList<>();
	private int nextLine = 1;

	/** Type Java du retour de la méthode en cours. */
	private Class<?> returnType;
	/** Faux après un saut inconditionnel ou un return : le code qui suit est mort, javac ne l'émet pas. */
	private boolean alive = true;

	public BytecodeWriter(Options options, int version, ClassDesc thisDesc, Class<?> thisClass) {
		this.options = options;
		this.version = version;
		this.thisDesc = thisDesc;
		this.thisClass = thisClass;
	}

	public boolean isOperationsEnabled() {
		return options.enableOperations();
	}

	private java.lang.classfile.ClassBuilder classBuilder;

	public void setClassBuilder(java.lang.classfile.ClassBuilder classBuilder) {
		this.classBuilder = classBuilder;
	}

	/**
	 * Écrit une méthode d'instance de la classe générée : `body` émet son code (les paramètres
	 * occupent les emplacements qui suivent `this`). Une fin de méthode atteignable lève
	 * Unsupported, comme javac refuserait une méthode sans return.
	 */
	public void writeMethod(String name, int flags, List<Class<?>> parameters, Class<?> returnType, Runnable body) {
		var descs = new ClassDesc[parameters.size()];
		int slot = 1;
		for (int i = 0; i < descs.length; i++) {
			descs[i] = JTypes.desc(parameters.get(i));
			slot += kind(parameters.get(i)).slotSize();
		}
		int firstSlot = slot;
		classBuilder.withMethodBody(name, MethodTypeDesc.of(JTypes.desc(returnType), descs), flags, code -> {
			begin(code, firstSlot, returnType);
			body.run();
			if (alive) {
				if (returnType != void.class) throw new Unsupported("fin de " + name + " atteignable");
				code.return_();
			}
		});
	}

	/** Emplacement du paramètre `index` (0 = premier après this) d'une méthode de paramètres `parameters`. */
	public static int parameterSlot(List<Class<?>> parameters, int index) {
		int slot = 1;
		for (int i = 0; i < index; i++) slot += kind(parameters.get(i)).slotSize();
		return slot;
	}

	/** Une variable LeekScript lue directement dans un emplacement existant (paramètre). */
	public LocalVar alias(Object declaration, int slot, Class<?> type, String name) {
		var v = new LocalVar(slot, type, name);
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
		this.returnType = returnType;
		this.alive = true;
		this.nextSlot = firstSlot;
		this.scopes.clear();
		this.loops.clear();
	}

	public CodeBuilder code() {
		return cb;
	}

	public Class<?> returnType() {
		return returnType;
	}

	public boolean isAlive() {
		return alive;
	}

	public Label newLabel() {
		return cb.newLabel();
	}

	/** Pose une étiquette d'instruction : le code qui la suit redevient atteignable. */
	public void bind(Label label) {
		cb.labelBinding(label);
		alive = true;
	}

	public void goto_(Label label) {
		cb.goto_(label);
		alive = false;
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
		scopes.add(nextSlot);
	}

	public void closeScope() {
		nextSlot = scopes.remove(scopes.size() - 1);
	}

	public LocalVar declare(Object declaration, Class<?> type, String name) {
		if (type == void.class || JTypes.isNull(type)) throw new Unsupported("variable de type " + type);
		var v = new LocalVar(nextSlot, type, name);
		nextSlot += TypeKind.from(JTypes.desc(type)).slotSize();
		locals.put(declaration, v);
		return v;
	}

	private final java.util.HashMap<String, FieldVar> fields = new java.util.HashMap<>();

	/** Déclare un champ d'instance de la classe générée. */
	public FieldVar field(String name, Class<?> type, int flags) {
		var f = new FieldVar(name, type);
		if (fields.putIfAbsent(name, f) != null) throw new Unsupported("champ en double " + name);
		classBuilder.withField(name, JTypes.desc(type), flags);
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

	public FieldVar field(String name) {
		var f = fields.get(name);
		if (f == null) throw new Unsupported("champ inconnu " + name);
		return f;
	}

	public LocalVar local(Object declaration) {
		var v = locals.get(declaration);
		if (v == null) throw new Unsupported("variable locale inconnue");
		return v;
	}

	public void pushLoop(Loop loop) {
		loops.add(loop);
	}

	public void popLoop() {
		loops.remove(loops.size() - 1);
	}

	public Loop currentLoop() {
		if (loops.isEmpty()) throw new Unsupported("break hors boucle");
		return loops.get(loops.size() - 1);
	}

	// ---- Lignes ----

	/** Repère de ligne pour l'instruction qui commence, relié à la ligne LeekScript de `location`. */
	public void line(Location location) {
		if (location == null || location.getFile() == null) return;
		int line = nextLine++;
		var file = location.getFile();
		int index = files.indexOf(file);
		if (index < 0) {
			index = files.size();
			files.add(file);
		}
		lines.put(line, new LineMapping(location.getStartLine(), index));
		cb.lineNumber(line);
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
		var owner = c.receiver() == null ? thisClass : typeOf(c.receiver());
		if (owner.isPrimitive() || JTypes.isNull(owner)) throw new Unsupported("appel sur " + owner);
		return Resolver.method(owner, c.name(), argTypes(c.args()), classLiterals(c.args()), false);
	}

	private Resolver.Resolved resolve(JExpr.StaticCall c) {
		return Resolver.method(c.owner(), c.name(), argTypes(c.args()), classLiterals(c.args()), true);
	}

	private Class<?> computeType(JExpr e) {
		return switch (e) {
			case JExpr.Lit l -> l.type();
			case JExpr.Read l -> l.var().type();
			case JExpr.This t -> thisClass;
			case JExpr.Call c -> resolve(c).returnType();
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
				return thisClass;
			}
			case JExpr.Call c -> {
				var m = resolve(c);
				Class<?> owner;
				ClassDesc ownerDesc;
				boolean isStatic = Modifier.isStatic(m.executable().getModifiers());
				if (c.receiver() == null) {
					owner = thisClass;
					ownerDesc = thisDesc;
					if (!isStatic) cb.aload(0);
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
				cb.aload(0);
				for (int i = 0; i < c.args().size(); i++) emitAs(c.args().get(i), c.parameters().get(i));
				var params = new ClassDesc[c.parameters().size()];
				for (int i = 0; i < params.length; i++) params[i] = JTypes.desc(c.parameters().get(i));
				cb.invokevirtual(thisDesc, c.name(), MethodTypeDesc.of(JTypes.desc(c.returnType()), params));
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

	private void load(Slot v) {
		switch (v) {
			case LocalVar l -> cb.loadLocal(kind(l.type()), l.slot());
			case FieldVar f -> {
				cb.aload(0);
				cb.getfield(thisDesc, f.name(), JTypes.desc(f.type()));
			}
		}
	}

	/** Avant la valeur d'une écriture : le receveur d'un champ. */
	private void storePrepare(Slot v) {
		if (v instanceof FieldVar) cb.aload(0);
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
				cb.putfield(thisDesc, f.name(), JTypes.desc(f.type()));
			}
		}
	}

	/** Laisse `(T) (x op e)` sur la pile. */
	private void compound(JExpr.Compound a) {
		var v = a.var();
		var binary = new JExpr.Binary(a.op(), new JExpr.Read(v), a.value());
		var t = emitBinary(binary);
		if (t != v.type()) primitiveConversion(t, v.type());
	}

	/** `x++` & co ; `used` : la valeur de l'expression reste sur la pile. */
	private Class<?> increment(JExpr.Inc i, boolean used) {
		var v = i.var();
		var k = kind(v.type());
		if (k == TypeKind.INT && v instanceof LocalVar l) {
			if (used && !i.prefix()) cb.iload(l.slot());
			cb.iinc(l.slot(), i.increment() ? 1 : -1);
			if (used && i.prefix()) cb.iload(l.slot());
			return v.type();
		}
		storePrepare(v);
		load(v);
		if (used && !i.prefix()) {
			// L'ancienne valeur, sous le receveur d'un champ
			if (v instanceof FieldVar) { if (k.slotSize() == 2) cb.dup2_x1(); else cb.dup_x1(); }
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
		convert(stack, stat, target);
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
		var t = typeOf(b);
		switch (b.op()) {
			case "<", ">", "<=", ">=", "==", "!=", "&&", "||" -> {
				return emitBoolean(b);
			}
			case "<<", ">>", ">>>" -> {
				emitAs(b.left(), t);
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
		emitAs(b.left(), operand);
		emitAs(b.right(), operand);
		var k = kind(t);
		if (k == TypeKind.BOOLEAN) k = TypeKind.INT;
		cb.with(OperatorInstruction.of(arithmetic(b.op(), k)));
		return t;
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
		alive = false;
	}


	// ---- Conversions LeekScript, portées de JavaWriter (mêmes noms, même Java) ----

	/** Classe Java du type LeekScript, telle que JavaWriter l'écrit (getJavaPrimitiveName). */
	public Class<?> primitiveClass(Type type) {
		return JTypes.forName(type.getJavaPrimitiveName(version));
	}

	/** Classe Java boxée du type LeekScript (getJavaName). */
	public Class<?> javaClass(Type type) {
		return JTypes.forName(type.getJavaName(version));
	}

	private static Type codegenType(Expression expression) {
		return expression.hasObjectJavaResult() ? Type.ANY : expression.getType();
	}

	/** L'IA, telle que JavaWriter.getAIThis() la désigne. */
	public JExpr aiThis() {
		return new JExpr.This();
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
		if (mainblock.getWordCompiler().getCurrentClass() != null) throw new Unsupported("string() dans une classe");
		return JExpr.call("string", expr(mainblock, expression));
	}

	/** La classe courante passée aux accès dynamiques (getCurrentClassVariable) : null hors d'une classe. */
	public JExpr classVariable(MainLeekBlock mainblock) {
		if (mainblock.getWordCompiler().getCurrentClass() != null) throw new Unsupported("classe courante");
		return JExpr.nul();
	}

	/** openCastObject : `realBox(x)` vers Double, un cast sinon. */
	public JExpr castObject(String javaType, JExpr e) {
		if (javaType.equals("Double")) return JExpr.call("realBox", e);
		return new JExpr.Cast(JTypes.forName(javaType), e);
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
		if (type instanceof CompoundType) throw new Unsupported("conversion vers " + type);
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
		if (m.matches()) return new JExpr.New(JTypes.forName(m.group(1)), List.of(aiThis()));
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
