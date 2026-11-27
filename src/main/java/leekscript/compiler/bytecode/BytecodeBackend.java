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
import leekscript.runner.Session;

/**
 * Backend bytecode : la classe de l'IA écrite directement avec l'API Class-File du JDK, sans
 * passer par le Java et javac. Il ne traite encore qu'une partie du langage ; pour le reste,
 * {@link #compile} rend null et le programme se compile par javac comme avant.
 *
 * Activé par la propriété système `leekscript.bytecode` : `true` (repli silencieux sur javac),
 * ou `check` (une erreur interne du backend, autre qu'une construction non gérée, est levée).
 */
public final class BytecodeBackend {

	private BytecodeBackend() {}

	private static final String MODE = System.getProperty("leekscript.bytecode", "");

	/** Choix du backend pour le fil courant, au-dessus de la propriété (mode différentiel des tests). */
	public static final ThreadLocal<Boolean> override = new ThreadLocal<>();

	public static boolean enabled() {
		var forced = override.get();
		if (forced != null) return forced;
		return MODE.equals("true") || MODE.equals("check");
	}

	/** Les classes compilées et le fichier `.lines` qui relie leurs lignes au LeekScript. */
	public record Result(Map<String, byte[]> classes, String lines) {}

	// Statistiques des compilations (tests, bancs) : nombre de programmes émis, et des replis par raison
	public static final LongAdder compiled = new LongAdder();
	public static final ConcurrentHashMap<String, LongAdder> fallbacks = new ConcurrentHashMap<>();
	/** Un programme par raison de repli (le plus court), pour choisir et tester la construction suivante. */
	public static final ConcurrentHashMap<String, String> examples = new ConcurrentHashMap<>();

	private static void fallback(String reason, AIFile file) {
		fallbacks.computeIfAbsent(reason, k -> new LongAdder()).increment();
		var code = file.getCode();
		if (code != null) examples.merge(reason, code, (a, b) -> b.length() < a.length() ? b : a);
	}

	/** Le bytecode du programme, ou null s'il contient une construction que le backend ne sait pas émettre. */
	public static Result compile(MainLeekBlock main, AIFile file, String className, Options options) {
		try {
			var result = build(main, file, className, options);
			compiled.increment();
			return result;
		} catch (Unsupported e) {
			fallback(e.getMessage(), file);
			return null;
		} catch (RuntimeException | LinkageError e) {
			// Méthode de plus de 64 Ko : javac la refusera aussi, avec l'erreur CODE_TOO_LARGE habituelle
			if (e instanceof IllegalArgumentException && String.valueOf(e.getMessage()).startsWith("Code length")) {
				fallback("méthode de plus de 64 Ko", file);
				return null;
			}
			fallback("ERREUR " + e, file);
			if (MODE.equals("check")) throw new IllegalStateException("backend bytecode : " + file.getPath(), e);
			return null;
		}
	}

	private static Result build(MainLeekBlock main, AIFile file, String className, Options options) {
		var root = JTypes.forName(file.getRootClass());
		var thisDesc = ClassDesc.of(className);
		var rootDesc = JTypes.desc(root);
		int version = main.getWordCompiler().getVersion();
		var writer = new BytecodeWriter(options, version, thisDesc, root);

		var resolver = ClassHierarchyResolver.of(List.of(), Map.of(thisDesc, rootDesc))
			.orElse(ClassHierarchyResolver.defaultResolver())
			.orElse(ClassHierarchyResolver.ofClassLoading(BytecodeBackend.class.getClassLoader()));
		var classFile = ClassFile.of(ClassFile.ClassHierarchyResolverOption.of(resolver));

		byte[] bytes = classFile.build(thisDesc, clb -> {
			clb.withFlags(AccessFlag.PUBLIC, AccessFlag.SUPER);
			clb.withSuperclass(rootDesc);

			clb.withMethodBody(ConstantDescs.INIT_NAME, MethodTypeDesc.of(ConstantDescs.CD_void), ClassFile.ACC_PUBLIC, cb -> {
				cb.aload(0);
				cb.loadConstant(main.getInstructionCount());
				cb.loadConstant(version);
				cb.invokespecial(rootDesc, ConstantDescs.INIT_NAME, MethodTypeDesc.of(ConstantDescs.CD_void, ConstantDescs.CD_int, ConstantDescs.CD_int));
				cb.return_();
			});

			clb.withMethodBody("runIA", MethodTypeDesc.of(ConstantDescs.CD_Object, JTypes.desc(Session.class)), ClassFile.ACC_PUBLIC, cb -> {
				writer.begin(cb, 2, Object.class);
				main.writeBytecode(writer, options);
				if (writer.isAlive()) throw new Unsupported("fin de runIA atteignable");
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
		});

		var errors = classFile.verify(bytes);
		if (!errors.isEmpty()) throw new IllegalStateException("bytecode invalide : " + errors.get(0).getMessage());

		var lines = new StringBuilder();
		for (var e : writer.getLines().entrySet()) {
			lines.append(e.getKey()).append(' ').append(e.getValue().getAI()).append(' ').append(e.getValue().getLeekScriptLine()).append('\n');
		}
		return new Result(Map.of(className, bytes), lines.toString());
	}

	private static void newArray(CodeBuilder cb, int size, ClassDesc component) {
		cb.loadConstant(size);
		cb.anewarray(component);
	}
}
