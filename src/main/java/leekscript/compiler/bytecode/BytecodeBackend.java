package leekscript.compiler.bytecode;

import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassHierarchyResolver;
import java.lang.classfile.CodeBuilder;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.lang.reflect.AccessFlag;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

import leekscript.compiler.AIFile;
import leekscript.compiler.Options;
import leekscript.compiler.bloc.MainLeekBlock;

/**
 * Backend bytecode : la classe de l'IA écrite directement avec l'API Class-File du JDK, sans
 * passer par le Java et javac. Il ne traite encore qu'une partie du langage ; pour le reste,
 * {@link #compile} rend null et le programme se compile par javac comme avant.
 *
 * Activé par la propriété système `leekscript.bytecode` : `true` (repli silencieux sur javac),
 * `check` (une erreur interne du backend, autre qu'une construction non gérée, est levée), ou
 * `compare` : javac compile aussi le programme, et ses classes ne servent que si l'empreinte des deux
 * diffère (cf {@link BytecodeCompare}) ; l'écart, comme une erreur interne, va à {@link #reporter}.
 */
public final class BytecodeBackend {

	private BytecodeBackend() {}

	private static final String MODE = System.getProperty("leekscript.bytecode", "");

	/** Choix du backend pour le fil courant, au-dessus de la propriété (mode différentiel des tests). */
	public static final ThreadLocal<Boolean> override = new ThreadLocal<>();

	public static boolean enabled() {
		var forced = override.get();
		if (forced != null) return forced;
		return MODE.equals("true") || MODE.equals("check") || MODE.equals("compare");
	}

	/** Mode `compare` : chaque programme compilé aussi par javac, les deux comparés. */
	public static boolean comparing() {
		return MODE.equals("compare") && override.get() == null;
	}

	/**
	 * Où va un écart avec javac ou une erreur interne du backend en mode `compare` (le programme se
	 * joue alors avec les classes de javac) : le générateur y branche son ErrorManager.
	 */
	public static volatile java.util.function.BiConsumer<AIFile, String> reporter = (file, problem) ->
		System.err.println("[bytecode] " + (file != null ? file.getPath() : "?") + " : " + problem);

	/** Mode `compare` : programmes comparés, et ceux dont l'empreinte diffère de javac. */
	public static final LongAdder compared = new LongAdder(), mismatched = new LongAdder();

	/** Mode `compare` : l'écart entre les classes de javac et celles du backend, signalé ; vrai s'il y en a un. */
	public static boolean differs(AIFile file, Map<String, byte[]> javac, Result bytecode) {
		compared.increment();
		String difference;
		try {
			difference = BytecodeCompare.difference(javac, bytecode.classes());
		} catch (RuntimeException e) {
			difference = "comparaison impossible : " + e;
		}
		if (difference == null) return false;
		mismatched.increment();
		report(file, "classes différentes de javac, " + difference);
		return true;
	}

	private static void report(AIFile file, String problem) {
		try {
			reporter.accept(file, problem);
		} catch (RuntimeException e) {
			System.err.println("[bytecode] signalement impossible : " + e);
		}
	}

	/**
	 * Hiérarchie des classes du runtime (AI, valeurs…), lue une fois pour toutes les compilations.
	 * Son cache est partagé par les compilations parallèles : `cached()` seul le tiendrait dans une HashMap.
	 */
	private static final ClassHierarchyResolver RUNTIME_HIERARCHY = ClassHierarchyResolver.defaultResolver()
		.orElse(ClassHierarchyResolver.ofClassLoading(BytecodeBackend.class.getClassLoader())).cached(ConcurrentHashMap::new);

	/** Les classes compilées et le fichier `.lines` qui relie leurs lignes au LeekScript. */
	/** `loader` : les classes déjà définies et vérifiées par la JVM, à réutiliser (null en mode check). */
	public record Result(Map<String, byte[]> classes, String lines, ClassLoader loader) {}

	// Statistiques des compilations (tests, bancs) : nombre de programmes émis, et des replis par raison
	public static final LongAdder compiled = new LongAdder();
	/** Dont les programmes v5 à modules (une classe par module). */
	public static final LongAdder compiledModules = new LongAdder();
	/** Temps passé (ns) : construction des classes, vérification. */
	public static final LongAdder buildNanos = new LongAdder(), verifyNanos = new LongAdder();
	public static final ConcurrentHashMap<String, LongAdder> fallbacks = new ConcurrentHashMap<>();
	/** Un programme par raison de repli (le plus court), pour choisir et tester la construction suivante. */
	public static final ConcurrentHashMap<String, String> examples = new ConcurrentHashMap<>();

	// Des raisons en nombre borné (un message peut porter un nom du programme) ; les exemples, qui
	// gardent le code d'une IA, seulement pour les tests et les bancs (mode check)
	private static final int MAX_REASONS = 256;

	private static void fallback(String reason, AIFile file) {
		var counter = fallbacks.get(reason);
		if (counter == null) {
			if (fallbacks.size() >= MAX_REASONS) reason = "(autres raisons)";
			counter = fallbacks.computeIfAbsent(reason, k -> new LongAdder());
		}
		counter.increment();
		var code = file.getCode();
		if (code != null && MODE.equals("check")) examples.merge(reason, code, (a, b) -> b.length() < a.length() ? b : a);
	}

	/** Le bytecode du programme, ou null s'il contient une construction que le backend ne sait pas émettre. */
	public static Result compile(MainLeekBlock main, AIFile file, String className, Options options) {
		try {
			// Une valeur fonction demandée à une classe de module déjà écrite : on recommence en la
			// déclarant d'avance (une par reprise, autant que le programme en a)
			var predeclared = new java.util.LinkedHashSet<leekscript.compiler.bloc.FunctionBlock>();
			while (true) {
				try {
					var result = build(main, file, className, options, predeclared);
					compiled.increment();
					return result;
				} catch (BytecodeWriter.Retry retry) {
					if (!predeclared.add(retry.function)) throw new IllegalStateException("valeur fonction redemandée " + retry.function.getName());
				}
			}
		} catch (Unsupported e) {
			fallback(e.getMessage(), file);
			return null;
		} catch (StackOverflowError e) {
			// Le backend empile plus de cadres par nœud que JavaWriter : une expression très imbriquée
			// que javac compile encore se compile par lui
			fallback("pile du backend", file);
			return null;
		} catch (RuntimeException | LinkageError e) {
			// Limite de la JVM (méthode de plus de 64 Ko, plus de 255 emplacements de paramètres, plus de
			// 65 535 constantes) : javac la refusera aussi, avec l'erreur CODE_TOO_LARGE habituelle
			var limit = jvmLimit(e);
			if (limit != null) {
				fallback(limit, file);
				return null;
			}
			fallback("ERREUR " + e, file);
			if (MODE.equals("check")) throw new IllegalStateException("backend bytecode : " + file.getPath(), e);
			if (comparing()) report(file, "erreur interne, " + e);
			return null;
		} finally {
			// (la dernière position émise retiendrait l'AIFile, son source et son Java, dans le fil)
			BytecodeWriter.CURRENT_LOCATION.remove();
		}
	}

	private static void appendLines(StringBuilder lines, BytecodeWriter writer) {
		for (var e : writer.getLines().entrySet()) {
			lines.append(e.getKey()).append(' ').append(e.getValue().getAI()).append(' ').append(e.getValue().getLeekScriptLine()).append('\n');
		}
	}

	private static String jvmLimit(Throwable e) {
		for (var t = e; t != null; t = t.getCause()) {
			var message = String.valueOf(t.getMessage());
			if (message.startsWith("Code length")) return "méthode de plus de 64 Ko";
			if (message.contains("Too many arguments in method signature")) return "plus de 255 emplacements de paramètres";
			if (message.contains("is not a valid index")) return "plus de 65 535 constantes";
		}
		return null;
	}

	private static Result build(MainLeekBlock main, AIFile file, String className, Options options, java.util.Set<leekscript.compiler.bloc.FunctionBlock> predeclared) {
		var root = JTypes.forName(file.getRootClass());
		var thisDesc = ClassDesc.of(className);
		var rootDesc = JTypes.desc(root);
		int version = main.getWordCompiler().getVersion();
		// Hiérarchie des classes générées, remplie au fil de l'écriture (stack maps, vérification)
		var hierarchy = new ConcurrentHashMap<ClassDesc, ClassDesc>();
		hierarchy.put(thisDesc, rootDesc);
		ClassHierarchyResolver generated = desc -> {
			var parent = hierarchy.get(desc);
			return parent == null ? null : ClassHierarchyResolver.ClassHierarchyInfo.ofClass(parent);
		};
		var resolver = generated.orElse(RUNTIME_HIERARCHY);
		var classFile = ClassFile.of(ClassFile.ClassHierarchyResolverOption.of(resolver));
		var writer = new BytecodeWriter(options, version, new BytecodeWriter.ClassContext(thisDesc, root, root, null), classFile, hierarchy);
		writer.setPredeclaredFunctionValues(predeclared);

		long t0 = System.nanoTime();
		byte[] bytes = classFile.build(thisDesc, clb -> {
			clb.withFlags(AccessFlag.PUBLIC, AccessFlag.SUPER);
			clb.withSuperclass(rootDesc);

			writer.setClassBuilder(clb);
			// Champs, fonctions et runIA de l'IA
			main.writeBytecode(writer, options);

			// Le constructeur : super(instructions, version), puis les initialiseurs des champs
			writer.writeMethod(ConstantDescs.INIT_NAME, ClassFile.ACC_PUBLIC, List.of(), void.class, () -> {
				var cb = writer.code();
				cb.aload(0);
				cb.loadConstant(main.getInstructionCount());
				cb.loadConstant(version);
				cb.invokespecial(rootDesc, ConstantDescs.INIT_NAME, MethodTypeDesc.of(ConstantDescs.CD_void, ConstantDescs.CD_int, ConstantDescs.CD_int));
				for (var initializer : writer.getFieldInitializers()) writer.emitStatement(initializer);
				for (var initializer : writer.takeLazyInitializers()) writer.emitStatement(initializer);
				for (var statement : writer.getConstructorStatements()) writer.emitStatement(statement);
			});

			GenericFunctions.write(clb, writer);

			clb.withMethodBody("getAIString", MethodTypeDesc.of(ConstantDescs.CD_String), ClassFile.ACC_PROTECTED, cb -> {
				cb.loadConstant(main.getAIName());
				cb.areturn();
			});

			var files = writer.getFiles();
			clb.withMethodBody("getErrorFiles", MethodTypeDesc.of(ConstantDescs.CD_String.arrayType()), ClassFile.ACC_PROTECTED, cb -> {
				newArray(cb, files.size(), ConstantDescs.CD_String);
				for (int i = 0; i < files.size(); i++) {
					cb.dup();
					cb.loadConstant(i);
					cb.loadConstant(files.get(i).getPath().replace("\\/", "/"));
					cb.aastore();
				}
				cb.areturn();
			});
			clb.withMethodBody("getErrorFilesID", MethodTypeDesc.of(ConstantDescs.CD_int.arrayType()), ClassFile.ACC_PROTECTED, cb -> {
				cb.loadConstant(files.size());
				cb.newarray(java.lang.classfile.TypeKind.INT);
				for (int i = 0; i < files.size(); i++) {
					cb.dup();
					cb.loadConstant(i);
					cb.loadConstant(files.get(i).getId());
					cb.iastore();
				}
				cb.areturn();
			});
			writer.writeAccessors(clb);
			writer.writeStaticInit(clb);
			writer.finishClass(clb, thisDesc);
		});
		writer.markBuilt();
		// v5 : les classes des modules importés
		main.writeModulesBytecode(writer, options);
		if (writer.isMultiClass()) compiledModules.increment();

		long t1 = System.nanoTime();
		buildNanos.add(t1 - t0);
		var classes = new java.util.LinkedHashMap<String, byte[]>();
		classes.put(className, bytes);
		classes.putAll(writer.getClasses());
		ClassLoader verified = null;
		if (MODE.equals("check")) {
			// Vérificateur de l'API Class-File : des messages précis, mais lent
			for (var entry : classes.entrySet()) {
				var errors = classFile.verify(entry.getValue());
				if (!errors.isEmpty()) throw new IllegalStateException("bytecode invalide (" + entry.getKey() + ") : " + errors.get(0).getMessage());
			}
		} else {
			// Celui de la JVM, natif : chaque classe définie dans un chargeur jetable puis liée, pour
			// qu'une VerifyError fasse repasser le programme par javac au lieu d'éclater en combat
			var loader = leekscript.compiler.JavaCompiler.inMemoryLoader(classes);
			verified = loader;
			for (var name : classes.keySet()) {
				try {
					// Initialisée pour être liée (vérifiée) : un initialiseur statique ne fait que créer des ContentType
					Class.forName(name, true, loader);
				} catch (ClassNotFoundException | LinkageError e) {
					throw new IllegalStateException("bytecode refusé par la JVM (" + name + ") : " + e);
				}
			}
		}
		verifyNanos.add(System.nanoTime() - t1);

		// Les lignes de la classe de l'IA, puis celles de chaque module sous le nom de sa classe (comme JavaWriter)
		var lines = new StringBuilder();
		appendLines(lines, writer);
		for (var module : writer.getModuleWriters()) {
			lines.append(leekscript.runner.AI.LINES_CLASS_MARK).append(' ').append(module.aiContext.desc.displayName()).append('\n');
			appendLines(lines, module);
		}
		return new Result(classes, lines.toString(), verified);
	}

	private static void newArray(CodeBuilder cb, int size, ClassDesc component) {
		cb.loadConstant(size);
		cb.anewarray(component);
	}
}
