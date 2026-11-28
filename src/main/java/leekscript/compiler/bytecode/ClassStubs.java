package leekscript.compiler.bytecode;

import java.lang.classfile.ClassFile;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Talons des classes que le backend va écrire (classes utilisateur `AI_N$u_X`, en v5 les classes
 * des modules et leurs classes `AI_N_<module>$u_X`) : mêmes noms, mêmes champs, mêmes signatures,
 * corps vides. Chargés dans un chargeur jetable, ils servent seulement au typage et à la
 * résolution des surcharges par réflexion, comme javac lit les classes en cours de compilation.
 */
public final class ClassStubs {

	private ClassStubs() {}

	public record FieldSpec(String name, String javaType) {}

	public record MethodSpec(String name, String returnJavaType, List<String> parameterJavaTypes, int flags) {}

	/**
	 * Une classe interne à la classe racine `owner` (l'IA ou un module) : `javaName` (u_X), sa
	 * super-classe, ses champs, ses méthodes et ses constructeurs (sans le paramètre de l'instance
	 * englobante). Son nom de type Java est `owner.u_X`.
	 */
	public record ClassSpec(String owner, String javaName, String superJavaName, List<FieldSpec> fields, List<MethodSpec> methods, List<List<String>> constructors) {
		String typeName() {
			return owner + "." + javaName;
		}
	}

	/** Le chargeur des talons : il les définit à la demande, par leur nom binaire. */
	static final class StubLoader extends ClassLoader {
		private final Map<String, byte[]> classes;

		StubLoader(ClassLoader parent, Map<String, byte[]> classes) {
			super(parent);
			this.classes = classes;
		}

		@Override
		protected Class<?> findClass(String name) throws ClassNotFoundException {
			var bytes = classes.get(name);
			if (bytes == null) throw new ClassNotFoundException(name);
			return defineClass(name, bytes, 0, bytes.length);
		}
	}

	public static boolean isStub(Class<?> c) {
		return c.getClassLoader() instanceof StubLoader;
	}

	/**
	 * Les talons de `aiName` (qui étend `root`, rendu sous son nom), des classes de modules
	 * `modules` (qui étendent AIModule) et des classes internes `specs`, par nom de type Java
	 * (`AI_N.u_X`) et par nom de classe racine. `runtimeType` donne la classe du runtime d'un autre nom de type.
	 */
	public static Map<String, Class<?>> build(String aiName, Class<?> root, List<String> modules, List<ClassSpec> specs, Function<String, Class<?>> runtimeType) {
		var aiDesc = ClassDesc.of(aiName);
		var descs = new HashMap<String, ClassDesc>();
		for (var spec : specs) descs.put(spec.typeName(), ClassDesc.of(spec.owner() + "$" + spec.javaName()));
		// Un nom simple dans une classe : celui d'une classe interne de sa racine, comme en Java
		Function<ClassSpec, Function<String, ClassDesc>> descIn = spec -> name -> {
			var d = descs.get(name);
			if (d == null && name.startsWith("u_")) d = descs.get(spec.owner() + "." + name);
			return d != null ? d : JTypes.desc(runtimeType.apply(name));
		};
		var bytes = new HashMap<String, byte[]>();
		var classFile = ClassFile.of();
		bytes.put(aiName, classFile.build(aiDesc, clb -> {
			clb.withFlags(ClassFile.ACC_PUBLIC | ClassFile.ACC_SUPER);
			clb.withSuperclass(JTypes.desc(root));
		}));
		var moduleDesc = JTypes.desc(leekscript.runner.AIModule.class);
		for (var module : modules) {
			bytes.put(module, classFile.build(ClassDesc.of(module), clb -> {
				clb.withFlags(ClassFile.ACC_PUBLIC | ClassFile.ACC_FINAL | ClassFile.ACC_SUPER);
				clb.withSuperclass(moduleDesc);
				clb.withMethodBody(ConstantDescs.INIT_NAME, MethodTypeDesc.of(ConstantDescs.CD_void, JTypes.desc(leekscript.runner.AI.class)), ClassFile.ACC_PUBLIC, cb -> { cb.aconst_null(); cb.athrow(); });
			}));
		}
		for (var spec : specs) {
			var self = descs.get(spec.typeName());
			var desc = descIn.apply(spec);
			var ownerDesc = ClassDesc.of(spec.owner());
			bytes.put(self.displayName(), classFile.build(self, clb -> {
				clb.withFlags(ClassFile.ACC_PUBLIC | ClassFile.ACC_SUPER);
				clb.withSuperclass(desc.apply(spec.superJavaName()));
				for (var field : spec.fields()) clb.withField(field.name(), desc.apply(field.javaType()), ClassFile.ACC_PUBLIC);
				for (var ctor : spec.constructors()) {
					var params = new ClassDesc[ctor.size() + 1];
					params[0] = ownerDesc;
					for (int i = 0; i < ctor.size(); i++) params[i + 1] = desc.apply(ctor.get(i));
					clb.withMethodBody(ConstantDescs.INIT_NAME, MethodTypeDesc.of(ConstantDescs.CD_void, params), ClassFile.ACC_PUBLIC, cb -> { cb.aconst_null(); cb.athrow(); });
				}
				for (var method : spec.methods()) {
					var params = new ClassDesc[method.parameterJavaTypes().size()];
					for (int i = 0; i < params.length; i++) params[i] = desc.apply(method.parameterJavaTypes().get(i));
					var ret = method.returnJavaType().equals("void") ? ConstantDescs.CD_void : desc.apply(method.returnJavaType());
					clb.withMethodBody(method.name(), MethodTypeDesc.of(ret, params), method.flags(), cb -> { cb.aconst_null(); cb.athrow(); });
				}
			}));
		}
		var loader = new StubLoader(ClassStubs.class.getClassLoader(), bytes);
		var result = new HashMap<String, Class<?>>();
		try {
			result.put(aiName, Class.forName(aiName, false, loader));
			for (var module : modules) result.put(module, Class.forName(module, false, loader));
			for (var spec : specs) result.put(spec.typeName(), Class.forName(descs.get(spec.typeName()).displayName(), false, loader));
		} catch (ClassNotFoundException | LinkageError e) {
			throw new Unsupported("talon : " + e);
		}
		return result;
	}
}
