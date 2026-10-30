package leekscript.compiler.bloc;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import leekscript.common.AccessLevel;
import leekscript.common.Type;
import leekscript.compiler.AIFile;
import leekscript.compiler.Folder;
import leekscript.compiler.Package;
import leekscript.compiler.AnalyzeError;
import leekscript.compiler.AnalyzeError.AnalyzeErrorLevel;
import leekscript.compiler.IACompiler;
import leekscript.compiler.Token;
import leekscript.compiler.JavaWriter;
import leekscript.compiler.Location;
import leekscript.compiler.Options;
import leekscript.compiler.WordCompiler;
import leekscript.compiler.exceptions.LeekCompilerException;
import leekscript.compiler.expression.LeekExpressionException;
import leekscript.compiler.expression.LeekFileContent;
import leekscript.compiler.expression.LeekNumber;
import leekscript.compiler.expression.LeekVariable.VariableType;
import leekscript.compiler.instruction.ClassDeclarationInstruction;
import leekscript.compiler.instruction.ModuleScopeInstruction;
import leekscript.compiler.instruction.LeekGlobalDeclarationInstruction;
import leekscript.runner.LeekFunctions;
import leekscript.common.Error;

public class MainLeekBlock extends AbstractLeekBlock {

	// HashSet pour hasGlobal/hasDeclaredGlobal — sur Quantum (118 globals × 7000 lookups
	// pendant firstPass) ArrayList.contains était le hotspot #1 du JFR (~18% des
	// samples). On n'itère jamais mGlobales : seul mGlobalesDeclarations a besoin
	// d'un ordre stable.
	private final HashSet<String> mGlobales = new HashSet<>();
	private final ArrayList<LeekGlobalDeclarationInstruction> mGlobalesDeclarations = new ArrayList<>();
	// Registre module-aware des variables de niveau module (clé = nom interne préfixé
	// `$m<id>$<name>`) : backing LeekVariable, pour résoudre les imports cross-module.
	private final Map<String, leekscript.compiler.expression.LeekVariable> mGlobalVars = new HashMap<>();
	private final HashSet<String> mRedefinedFunctions = new HashSet<String>();
	private final HashMap<String, FunctionBlock> mFunctions = new HashMap<>();
	// #4243 : surcharges de fonctions utilisateur (v5), regroupées par nom interne SANS suffixe
	// (préfixe module + nom brut). Une entrée à >1 élément = nom surchargé → dispatch au site d'appel.
	private final HashMap<String, ArrayList<FunctionBlock>> mOverloadSets = new HashMap<>();
	private final ArrayList<AnonymousFunctionBlock> mAnonymousFunctions = new ArrayList<AnonymousFunctionBlock>();
	// HashMap : mUserFunctions n'est jamais itéré, seulement containsKey/put. Pas
	// besoin de l'ordre TreeMap. Quantum déclare ~100+ fonctions utilisateur.
	private final Map<String, Integer> mUserFunctions = new HashMap<>();
	// Fichiers lus par les fonctions à argument fichier, par chemin : émis une fois chacun en champ
	// statique, quel que soit le nombre d'appels.
	private final Map<String, String> mFileFields = new HashMap<>();
	private final List<String> mFileContents = new ArrayList<>();
	private int mFileContentsLength = 0;
	public static final int MAX_FILE_CONTENT = 64 * 1024;
	public static final int MAX_FILE_CONTENTS = 256 * 1024;
	private final Map<String, ClassDeclarationInstruction> mDefinedClasses = new TreeMap<String, ClassDeclarationInstruction>();
	private final Map<String, ClassDeclarationInstruction> mUserClasses = new TreeMap<String, ClassDeclarationInstruction>();
	private final List<ClassDeclarationInstruction> mUserClassesList = new ArrayList<>();
	private int mMinLevel = 1;
	private int mAnonymousId = 1;
	private int mFunctionId = 1;
	// Dédup des includes par (owner, chemin) et NON par référence d'AIFile : le
	// cache de fichiers du FileSystem (LRU borné côté worker/daemon) peut évincer
	// un fichier au milieu d'une compilation, et le resolve suivant fabrique alors
	// une deuxième instance pour le même chemin. Avec une dédup par référence, le
	// fichier était re-parsé et ses classes redéclarées → VARIABLE_NAME_UNAVAILABLE
	// intermittent sur une IA pourtant valide.
	private final Map<String, AIFile> mIncluded = new LinkedHashMap<>();
	private final Map<String, AIFile> mIncludedFirstPass = new HashMap<>();
	// Modules v5 (import/export). mMainModule = l'entrypoint + ses include() (préfixe
	// vide → byte-identique v1-4). mModules = les fichiers importés (un Module chacun,
	// préfixe collision-proof). Dédoublonnage d'import par AIFile.getKey() (comme les
	// includes) : le cache du FileSystem peut recréer l'AIFile d'un même fichier.
	private final Module mMainModule;
	private final Map<String, Module> mModules = new HashMap<>();
	private final Map<String, AIFile> mImportedFirstPass = new HashMap<>();
	private final Map<String, AIFile> mImported = new HashMap<>();
	private int mModuleId = 1;
	private int mCounter = 0;
	private int mCountInstruction = 0;
	private final IACompiler mCompiler;
	private String mAIName;
	private String className;
	private WordCompiler wordCompiler;

	public MainLeekBlock(IACompiler compiler, WordCompiler wordCompiler, AIFile ai) throws LeekCompilerException {
		super(null, null);
		this.mMain = this;

		// On ajoute l'IA pour pas pouvoir l'include
		mIncluded.put(ai.getKey(), ai);
		mIncludedFirstPass.put(ai.getKey(), ai);
		mAIName = ai.getPath();
		mCompiler = compiler;
		mCompiler.setCurrentAI(ai);
		mMainModule = new Module(ai, 0, true);
		mModules.put(ai.getKey(), mMainModule);
		// Un module importé peut importer l'entrée (cycle) : elle est déjà compilée
		mImportedFirstPass.put(ai.getKey(), ai);
		mImported.put(ai.getKey(), ai);

		if (ai.getVersion() >= 2) {
			var classClass = new ClassDeclarationInstruction(new Token("Class"), 0, ai, true, this, Type.CLASS).notInstantiable();
			classClass.addField(wordCompiler, new Token("name"), Type.STRING, null, AccessLevel.PUBLIC, true);
			classClass.addField(wordCompiler, new Token("super"), Type.CLASS, null, AccessLevel.PUBLIC, true);
			classClass.addField(wordCompiler, new Token("fields"), Type.ARRAY_STRING, null, AccessLevel.PUBLIC, true);
			classClass.addField(wordCompiler, new Token("staticFields"), Type.ARRAY_STRING, null, AccessLevel.PUBLIC, true);
			classClass.addField(wordCompiler, new Token("methods"), Type.ARRAY_STRING, null, AccessLevel.PUBLIC, true);
			classClass.addField(wordCompiler, new Token("staticMethods"), Type.ARRAY_STRING, null, AccessLevel.PUBLIC, true);
			addClass(classClass);
		}

		if (ai.getVersion() >= 3) {

			// MIROIR COMPILE des classes internes (typage). Toute classe/constante déclarée
			// ici DOIT l'être aussi côté runtime (AI.java) et vice-versa, sinon le code
			// généré (getField, new XxxLeekValue...) ne résout pas. Constantes Math = source
			// unique LeekConstants.MATH_NAMESPACE.
			var valueClass = new ClassDeclarationInstruction(new Token("Value"), 0, ai, true, this, Type.ANY).notInstantiable();
			valueClass.addField(wordCompiler, new Token("class"), Type.CLASS, null, AccessLevel.PUBLIC, true);
			addClass(valueClass);

			addClass(new ClassDeclarationInstruction(new Token("Null"), 0, ai, true, this, Type.NULL));
			addClass(new ClassDeclarationInstruction(new Token("Boolean"), 0, ai, true, this, Type.BOOL));

			var integerClass = new ClassDeclarationInstruction(new Token("Integer"), 0, ai, true, this, Type.INT);
			integerClass.addStaticField(wordCompiler, new Token("MIN_VALUE"), Type.INT, new LeekNumber(new Token(""), 0, Long.MIN_VALUE, Type.INT), AccessLevel.PUBLIC, true);
			integerClass.addStaticField(wordCompiler, new Token("MAX_VALUE"), Type.INT, new LeekNumber(new Token(""), 0, Long.MAX_VALUE, Type.INT), AccessLevel.PUBLIC, true);
			addClass(integerClass);

			var realClass = new ClassDeclarationInstruction(new Token("Real"), 0, ai, true, this, Type.REAL);
			realClass.addStaticField(wordCompiler, new Token("MIN_VALUE"), Type.REAL, new LeekNumber(new Token(""), Double.MIN_VALUE, 0, Type.REAL), AccessLevel.PUBLIC, true);
			realClass.addStaticField(wordCompiler, new Token("MAX_VALUE"), Type.REAL, new LeekNumber(new Token(""), Double.MAX_VALUE, 0, Type.REAL), AccessLevel.PUBLIC, true);
			addClass(realClass);

			addClass(new ClassDeclarationInstruction(new Token("BigInteger"), 0, ai, true, this, Type.BIG_INT));

			addClass(new ClassDeclarationInstruction(new Token("Number"), 0, ai, true, this, Type.INT_OR_REAL));
			addClass(new ClassDeclarationInstruction(new Token("Array"), 0, ai, true, this, Type.ARRAY, Type.EMPTY_ARRAY));
			if (ai.getVersion() >= 4) {
				addClass(new ClassDeclarationInstruction(new Token("Map"), 0, ai, true, this, Type.MAP, Type.EMPTY_MAP));
			}
			addClass(new ClassDeclarationInstruction(new Token("Interval"), 0, ai, true, this, Type.INTERVAL, Type.EMPTY_INTERVAL));
			addClass(new ClassDeclarationInstruction(new Token("Set"), 0, ai, true, this, Type.SET, Type.EMPTY_SET));
			addClass(new ClassDeclarationInstruction(new Token("String"), 0, ai, true, this, Type.STRING));
			var objectClass = new ClassDeclarationInstruction(new Token("Object"), 0, ai, true, this, Type.OBJECT);
			objectClass.addMethod(wordCompiler, new Token("keys"), new ClassMethodBlock(objectClass, false, false, wordCompiler.getCurrentBlock(), this, new Token("keys"), Type.ARRAY), AccessLevel.PUBLIC);
			addClass(objectClass);
			addClass(new ClassDeclarationInstruction(new Token("Function"), 0, ai, true, this, Type.FUNCTION).notInstantiable());
			addClass(new ClassDeclarationInstruction(new Token("JSON"), 0, ai, true, this).notInstantiable());
			addClass(new ClassDeclarationInstruction(new Token("System"), 0, ai, true, this).notInstantiable());

			// v5 (pilier 2, stdlib objet) : namespace `Math` regroupant les fonctions
			// mathématiques (`Math.sqrt(x)`, `Math.abs(x)`, `Math.rand()`...). Forme
			// canonique remplaçant les formes libres `sqrt(x)`/`abs(x)`. Pur namespace
			// (pas de type valeur), comme JSON/System. Gardé v>=5 pour ne pas réserver
			// le nom `Math` dans les versions antérieures.
			if (ai.getVersion() >= 5) {
				var mathClass = new ClassDeclarationInstruction(new Token("Math"), 0, ai, true, this).notInstantiable();
				// Constantes mathématiques en accès de champ. Source UNIQUE
				// LeekConstants.MATH_NAMESPACE, partagée avec le runtime (AI) : on ne peut
				// pas oublier/désynchroniser un côté.
				for (var c : leekscript.runner.LeekConstants.MATH_NAMESPACE) {
					mathClass.addStaticField(wordCompiler, new Token(c.name()), c.getType(), new LeekNumber(new Token(""), c.getValue(), 0, c.getType()), AccessLevel.PUBLIC, true);
				}
				addClass(mathClass);
			}
		}
	}

	@Override
	public int getCount() {
		return mCounter++;
	}

	public void addRedefinedFunction(String function) {
		mRedefinedFunctions.add(function);
	}

	public boolean hasRedefinedFunctions() {
		return !mRedefinedFunctions.isEmpty();
	}

	public boolean isRedefinedFunction(String function) {
		// isEmpty fast path : pour la plupart des IAs aucune fonction n'est redéfinie
		// (LeekExpression.preAnalyze.line 1068, uniquement déclenché par
		// `assign à une SYSTEM_FUNCTION/FUNCTION`). Le contains() était dans le top
		// 30 du JFR sur Quantum alors que Quantum n'a aucune redef.
		return !mRedefinedFunctions.isEmpty() && mRedefinedFunctions.contains(function);
	}

	public void addInstruction() {
		mCountInstruction++;
	}

	public int getInstructionsCount() {
		return mCountInstruction;
	}

	public int getMinLevel() {
		return mMinLevel;
	}

	public void setMinLevel(int min_level) {
		this.mMinLevel = min_level;
	}

	/**
	 * Cœur partagé d'include() (v≤4) et import (v5) : résout le fichier, déduplique,
	 * compile le child avec son propre WordCompiler, propage les erreurs sur l'AIFile
	 * appelant. Retourne l'AIFile résolu (même déjà dédupliqué) ou null si introuvable.
	 *
	 * @param dedup déduplication propre à la passe/au mode, par AIFile.getKey() (le cache du
	 *        FileSystem peut recréer l'AIFile d'un même fichier)
	 * @param moduleProvider fournit/crée le Module v5 posé sur le child (null pour
	 *        include = fusion à plat dans le module principal)
	 * @param firstPass true = firstPass (+ clearErrors), false = secondPass
	 */
	private AIFile compileChild(WordCompiler compiler, String path, Map<String, AIFile> dedup,
			java.util.function.Function<AIFile, Module> moduleProvider, boolean firstPass) throws LeekCompilerException {
		AIFile ai;
		try {
			ai = mCompiler.getCurrentAI().getFolder().resolve(path);
		} catch (FileNotFoundException e) {
			return null;
		}
		return compileChild(compiler, ai, dedup, moduleProvider, firstPass);
	}

	private AIFile compileChild(WordCompiler compiler, AIFile ai, Map<String, AIFile> dedup,
			java.util.function.Function<AIFile, Module> moduleProvider, boolean firstPass) throws LeekCompilerException {
		var key = ai.getKey();
		if (dedup.containsKey(key)) return ai;
		// Hack dégueu à retirer, crash du daemon dans un cas précis d'include/import infini
		if (dedup.size() > 500) {
			throw new LeekCompilerException(compiler.getTokenStream().get(), Error.UNKNOWN_ERROR);
		}
		if (firstPass) ai.clearErrors(); // includeAI/importModule (secondPass) ne clear pas
		dedup.put(key, ai);
		var previousAI = mCompiler.getCurrentAI();
		mCompiler.setCurrentAI(ai);
		WordCompiler newCompiler = new WordCompiler(ai, compiler.getVersion(), compiler.getOptions());
		newCompiler.setMainBlock(this);
		if (moduleProvider != null) newCompiler.setModule(moduleProvider.apply(ai));
		try {
			if (firstPass) newCompiler.firstPass(); else newCompiler.secondPass();
		} catch (LeekCompilerException e) {
			recordIncludeError(ai, e);
		}
		compiler.getAI().getErrors().addAll(ai.getErrors());
		mCompiler.setCurrentAI(previousAI);
		return ai;
	}

	private record PendingImport(WordCompiler compiler, Module importer, Module source, Token name, boolean reexport) {}
	private final List<PendingImport> mPendingImports = new ArrayList<>();

	public void addPendingImport(WordCompiler compiler, Module importer, Module source, Token name, boolean reexport) {
		mPendingImports.add(new PendingImport(compiler, importer, source, name, reexport));
	}

	/**
	 * Lie les noms importés d'un module qui n'avait pas fini sa première passe (cycle), une fois
	 * toutes les premières passes faites : tous les exports sont alors connus.
	 */
	public void resolvePendingImports() throws LeekCompilerException {
		// Un ré-export lié ici peut en débloquer un autre (chaîne dans un cycle) : on recommence
		// tant qu'une liaison progresse.
		boolean progress = true;
		while (progress) {
			progress = false;
			for (var iterator = mPendingImports.iterator(); iterator.hasNext();) {
				var pending = iterator.next();
				if (pending.importer().bindImport(pending.name().getWord(), pending.source(), pending.reexport())) {
					iterator.remove();
					progress = true;
				}
			}
		}
		for (var pending : mPendingImports) {
			var name = pending.name().getWord();
			var error = new AnalyzeError(pending.name(), AnalyzeErrorLevel.ERROR, Error.SYMBOL_NOT_EXPORTED, new String[] { name });
			pending.compiler().addError(error);
			// Les erreurs d'un module sont recopiées sur l'entrée à la fin de sa passe : déjà fait
			var entry = mMainModule.getFile();
			if (pending.compiler().getAI() != entry) entry.getErrors().add(error);
		}
		mPendingImports.clear();
	}

	// Packages v5 (cf Package), par dossier : lus une fois par compilation. Un dossier sans
	// `package.leek` prend le package de son parent, le programme au bout.
	private final Map<String, Package> mPackages = new HashMap<>();

	/** Clé d'un dossier, stable d'une instance de Folder à l'autre : propriétaire et chemin. */
	private static String folderKey(Folder folder) {
		var key = new StringBuilder(folder.getOwner() + ":");
		for (var f = folder; f != null && f.getParent() != f; f = f.getParent()) key.insert(key.indexOf(":") + 1, "/" + f.getName());
		return key.toString();
	}
	private Package mProgramPackage;

	public Package programPackage() {
		if (mProgramPackage == null) mProgramPackage = Package.read(null, mMainModule.getFile());
		return mProgramPackage;
	}

	public Package packageOf(AIFile file) {
		var visited = new ArrayList<String>();
		Package found = null;
		for (var folder = file.getFolder(); folder != null; folder = folder.getParent()) {
			var key = folderKey(folder);
			found = mPackages.get(key);
			if (found != null) break;
			visited.add(key);
			var root = Package.findRoot(folder, "");
			if (root != null) {
				found = Package.read(folder, root);
				break;
			}
			if (folder.getParent() == folder) break;
		}
		if (found == null) found = programPackage();
		for (var key : visited) mPackages.put(key, found);
		return found;
	}

	/** Racine (`package.leek`) de la dépendance `dependency` de `from`, null si absente. */
	private AIFile dependencyRoot(Package from, Package.Dependency dependency) {
		var base = from.isProgram() ? mMainModule.getFile().getFolder() : from.getFolder();
		return Package.findRoot(base, dependency.path());
	}

	/**
	 * Fichier visé par `import … from "path"` depuis le module en cours : `"@nom"` désigne la racine
	 * d'un package déclaré par `depends`, un chemin relatif ne sort pas du package de l'importeur.
	 * Null (erreur signalée) si l'import est refusé ou introuvable.
	 */
	public AIFile resolveImport(WordCompiler compiler, Token pathToken, String path) throws LeekCompilerException {
		var file = resolveImportUncached(compiler, pathToken, path);
		if (file != null) mResolvedImports.put(compiler.getAI().getKey() + "\n" + path, file);
		return file;
	}

	/** Fichier résolu au firstPass pour cet import, null s'il a été refusé. */
	public AIFile resolvedImport(WordCompiler compiler, String path) {
		return mResolvedImports.get(compiler.getAI().getKey() + "\n" + path);
	}

	private final Map<String, AIFile> mResolvedImports = new HashMap<>();

	private AIFile resolveImportUncached(WordCompiler compiler, Token pathToken, String path) throws LeekCompilerException {
		var from = packageOf(compiler.getAI());
		if (path.startsWith("@")) {
			var alias = path.substring(1);
			// Seule l'API d'un package (sa racine) s'importe : pas de `"@nom/fichier"`
			if (alias.contains("/")) {
				compiler.addError(new AnalyzeError(pathToken, AnalyzeErrorLevel.ERROR, Error.PACKAGE_PATH_CROSSES_BOUNDARY, new String[] { path }));
				return null;
			}
			var dependency = from.getDepends().get(alias);
			if (dependency == null) {
				compiler.addError(new AnalyzeError(pathToken, AnalyzeErrorLevel.ERROR, Error.PACKAGE_NOT_DECLARED, new String[] { alias }));
				return null;
			}
			var root = dependencyRoot(from, dependency);
			if (root == null) {
				compiler.addError(new AnalyzeError(pathToken, AnalyzeErrorLevel.ERROR, Error.PACKAGE_NOT_FOUND, new String[] { alias, dependency.path() }));
			}
			return root;
		}
		AIFile target;
		try {
			target = compiler.getAI().getFolder().resolve(path);
		} catch (FileNotFoundException e) {
			compiler.addError(new AnalyzeError(pathToken, AnalyzeErrorLevel.ERROR, Error.AI_NOT_EXISTING, new String[] { path }));
			return null;
		}
		var to = packageOf(target);
		if (!to.equals(from)) {
			var name = to.getName() != null ? to.getName().getWord() : path;
			compiler.addError(new AnalyzeError(pathToken, AnalyzeErrorLevel.ERROR, Error.PACKAGE_PATH_CROSSES_BOUNDARY, new String[] { path, name }));
			return null;
		}
		return target;
	}

	/**
	 * Graphe des `depends` depuis le package de l'entrée, avant toute compilation : dépendance
	 * introuvable, nom qui ne correspond pas, cycle (interdit entre packages).
	 */
	public void checkPackages(WordCompiler compiler) throws LeekCompilerException {
		var start = packageOf(mMainModule.getFile());
		var done = new java.util.HashSet<Package>();
		done.add(start);
		checkPackage(compiler, start, new ArrayList<>(), done);
	}

	private void checkPackage(WordCompiler compiler, Package current, List<Package> path, java.util.Set<Package> done) throws LeekCompilerException {
		path.add(current);
		for (var dependency : current.getDepends().values()) {
			var root = dependencyRoot(current, dependency);
			if (root == null) {
				compiler.addError(new AnalyzeError(dependency.alias().getLocation(), AnalyzeErrorLevel.ERROR, Error.PACKAGE_NOT_FOUND, new String[] { dependency.alias().getWord(), dependency.path() }));
				continue;
			}
			var target = packageOf(root);
			var name = target.getName();
			if (name == null || !name.getWord().equals(dependency.alias().getWord())) {
				compiler.addError(new AnalyzeError(dependency.alias().getLocation(), AnalyzeErrorLevel.ERROR, Error.PACKAGE_NAME_MISMATCH, new String[] { dependency.alias().getWord(), name == null ? "" : name.getWord() }));
				continue;
			}
			int index = path.indexOf(target);
			if (index >= 0) {
				var chain = new StringBuilder();
				for (var p : path.subList(index, path.size())) chain.append(p.getName().getWord()).append(" → ");
				chain.append(name.getWord());
				compiler.addError(new AnalyzeError(dependency.alias().getLocation(), AnalyzeErrorLevel.ERROR, Error.PACKAGE_CYCLE, new String[] { chain.toString() }));
				continue;
			}
			if (done.add(target)) checkPackage(compiler, target, path, done);
		}
		path.remove(path.size() - 1);
	}

	/**
	 * L'API d'un package (ce qu'exporte son package.leek, ré-exports compris) est entièrement
	 * typée : on compile un appelant sans lire les corps. Paramètres et retours des fonctions et
	 * méthodes publiques, champs publics, variables exportées.
	 */
	public void checkPackageApis(WordCompiler compiler) throws LeekCompilerException {
		for (var module : mModules.values()) {
			if (!Package.isRootFile(module.getFile())) continue;
			for (var name : module.getExportedFunctions()) {
				// Toutes les surcharges : getUserFunction ne rend que la première
				var overloads = getOverloads(module.functionOrigin(name).prefix() + name);
				if (overloads != null) {
					for (var function : overloads) checkTypedSignature(compiler, name, function, true);
				} else {
					var function = getUserFunction(name, module);
					if (function != null) checkTypedSignature(compiler, name, function, true);
				}
			}
			for (var name : module.getExportedClasses()) {
				var clazz = getDefinedClass(name, module);
				if (clazz != null) checkTypedClass(compiler, clazz);
			}
			for (var name : module.getExportedVars()) {
				var origin = module.varOrigin(name);
				for (var declaration : mGlobalesDeclarations) {
					if (declaration.getModule() == origin && declaration.getName().equals(name) && !declaration.isTyped()) {
						untyped(compiler, declaration.getNameToken().getLocation(), name, "type");
					}
				}
			}
		}
	}

	private void checkTypedClass(WordCompiler compiler, ClassDeclarationInstruction clazz) throws LeekCompilerException {
		var name = clazz.getName();
		for (var fields : List.of(clazz.getFields(), clazz.getStaticFields())) {
			for (var field : fields.entrySet()) {
				if (field.getValue().level == AccessLevel.PUBLIC && !field.getValue().typed) {
					untyped(compiler, clazz.getLocation(), name + "." + field.getKey(), "type");
				}
			}
		}
		for (var constructor : clazz.getConstructors().values()) {
			if (constructor.level == AccessLevel.PUBLIC && constructor.block != null) checkTypedMethod(compiler, name, constructor.block);
		}
		for (var methods : List.of(clazz.getMethods(), clazz.getStaticMethods())) {
			for (var overloads : methods.entrySet()) {
				for (var method : overloads.getValue().values()) {
					if (method.level == AccessLevel.PUBLIC) checkTypedMethod(compiler, name + "." + overloads.getKey(), method.block);
				}
			}
		}
	}

	private void checkTypedMethod(WordCompiler compiler, String name, ClassMethodBlock method) throws LeekCompilerException {
		for (int i = 0; i < method.countParameters(); i++) {
			if (!method.isParameterTyped(i)) untyped(compiler, method.getLocation(), name, method.getParameterName(i));
		}
		if (!method.isConstructor() && !method.isReturnTypeWritten()) untyped(compiler, method.getLocation(), name, "->");
	}

	private void checkTypedSignature(WordCompiler compiler, String name, FunctionBlock function, boolean withReturn) throws LeekCompilerException {
		for (int i = 0; i < function.countParameters(); i++) {
			if (!function.isParameterTyped(i)) untyped(compiler, function.getLocation(), name, function.getParameterName(i));
		}
		if (withReturn && !function.isReturnTypeWritten()) untyped(compiler, function.getLocation(), name, "->");
	}

	private void untyped(WordCompiler compiler, Location location, String name, String part) throws LeekCompilerException {
		compiler.addError(new AnalyzeError(location, AnalyzeErrorLevel.ERROR, Error.PACKAGE_EXPORT_UNTYPED, new String[] { name, part }));
	}

	public boolean includeAIFirstPass(WordCompiler compiler, String path) throws LeekCompilerException {
		return compileChild(compiler, path, mIncludedFirstPass, null, true) != null;
	}

	// On absorbe l'exception pour que les autres includes soient parsés, mais on
	// la matérialise sur l'AIFile : sinon l'erreur est invisible côté utilisateur
	// (ex: doublon de fonction → on ne voyait que l'UNKNOWN_FUNCTION trompeur en
	// aval, jamais le FUNCTION_NAME_UNAVAILABLE qui pointe la vraie cause).
	private static void recordIncludeError(AIFile ai, LeekCompilerException e) {
		ai.getErrors().add(new AnalyzeError(e.getLocation(), AnalyzeErrorLevel.ERROR,
				e.getError(), e.getParameters()));
	}

	public boolean includeAI(WordCompiler compiler, String path) throws LeekCompilerException {
		return compileChild(compiler, path, mIncluded, null, false) != null;
	}

	/**
	 * Compile (firstPass) un module importé et retourne son {@link Module}. Miroir
	 * de {@link #includeAIFirstPass}, mais le module garde ses symboles privés :
	 * le child WordCompiler reçoit son propre Module (préfixe collision-proof), au
	 * lieu de fusionner à plat dans le module principal. Retourne null si le fichier
	 * est introuvable. v5 modules, phase 3b.
	 */
	public Module importModuleFirstPass(WordCompiler compiler, AIFile file) throws LeekCompilerException {
		compileChild(compiler, file, mImportedFirstPass, this::getOrCreateModule, true);
		return getOrCreateModule(file);
	}

	/** secondPass d'un module importé. Miroir de {@link #includeAI}. */
	public Module importModule(WordCompiler compiler, AIFile file) throws LeekCompilerException {
		var module = getOrCreateModule(file);
		if (mImported.containsKey(file.getKey())) return module;
		// Le code de haut niveau du module s'ajoute aux instructions du bloc principal :
		// ces marqueurs l'analysent dans son module, avec ses propres locales
		var opening = ModuleScopeInstruction.open(module, compiler.getTokenStream().get().getLocation());
		addInstruction(compiler, opening);
		compileChild(compiler, file, mImported, this::getOrCreateModule, false);
		addInstruction(compiler, opening.close());
		return module;
	}

	public boolean hasUserFunction(String name, boolean use_declarations) {
		return hasUserFunction(name, use_declarations, mMainModule);
	}

	public boolean hasUserFunction(String name, boolean use_declarations, Module module) {
		String key = (module == null) ? name : module.prefix() + name;
		if (mFunctions.containsKey(key)) {
			return true;
		}
		if (use_declarations && mUserFunctions.containsKey(key)) {
			return true;
		}
		if (module != null && module.getImportedFunctionSource(name) != null) {
			return true;
		}
		return false;
	}

	public Module getMainModule() {
		return mMainModule;
	}

	public Module getModule(AIFile file) {
		return mModules.get(file.getKey());
	}

	/** Crée (ou retourne) le Module associé à un fichier importé. */
	public Module getOrCreateModule(AIFile file) {
		var existing = mModules.get(file.getKey());
		if (existing != null) return existing;
		var module = new Module(file, mModuleId++, false);
		mModules.put(file.getKey(), module);
		return module;
	}

	public void addFunctionDeclaration(String name, int count_param) {
		mUserFunctions.put(name, count_param);
	}

	public void addGlobalDeclaration(LeekGlobalDeclarationInstruction declaration) {
		mGlobalesDeclarations.add(declaration);
	}

	public void addAnonymousFunction(AnonymousFunctionBlock block) {
		block.setId(mAnonymousId);
		mAnonymousFunctions.add(block);
		mAnonymousId++;
	}

	public FunctionBlock getUserFunction(String name) {
		return mFunctions.get(name);
	}

	/** Résolution module-aware : imports d'abord, puis fonctions propres du module
	 * (clé préfixée). Un module non-principal n'a PAS de fallback vers le namespace
	 * global plat (c'est ce qui rend ses voisins privés). */
	public FunctionBlock getUserFunction(String name, Module module) {
		if (module != null) {
			var src = module.getImportedFunctionSource(name);
			if (src != null) return mFunctions.get(src.functionOrigin(name).prefix() + name);
			var own = mFunctions.get(module.prefix() + name);
			if (own != null) return own;
			// Un module importé n'a PAS de fallback vers le namespace global (privacy).
			if (!module.isMain()) return null;
		}
		return mFunctions.get(name);
	}

	public void addGlobalVar(String internalName, leekscript.compiler.expression.LeekVariable variable) {
		mGlobalVars.put(internalName, variable);
	}

	/** Résolution module-aware d'une variable de niveau module : imports d'abord, puis
	 * champs propres du module (clé préfixée). Un module importé n'a PAS de fallback vers
	 * le namespace global plat (privacy). Retourne la backing LeekVariable ou null. */
	public leekscript.compiler.expression.LeekVariable getGlobal(String name, Module module) {
		if (module != null) {
			var src = module.getImportedVarSource(name);
			if (src != null) return mGlobalVars.get(src.varOrigin(name).prefix() + name);
			var own = mGlobalVars.get(module.prefix() + name);
			if (own != null) return own;
			if (!module.isMain()) return null;
		}
		return mGlobalVars.get(name);
	}

	@Override
	public boolean hasGlobal(String globale) {
		return mGlobales.contains(globale);
	}

	@Override
	public boolean hasDeclaredGlobal(String globale) {
		return mGlobales.contains(globale);
	}

	@Override
	public void addGlobal(String variable) {
		mGlobales.add(variable);
	}

	public void addFunction(FunctionBlock block) {
		block.setId(mFunctionId);
		mFunctionId++;
		// #4243 : regroupement des surcharges par nom public. La 1ʳᵉ fonction d'un nom garde un suffixe
		// vide (nom interne inchangé) ; les suivantes reçoivent `$ov<n>` pour un nom Java unique.
		var set = mOverloadSets.get(block.publicInternalName());
		if (set == null) {
			set = new ArrayList<>();
			mOverloadSets.put(block.publicInternalName(), set);
		} else {
			block.setOverloadSuffix("$ov" + set.size());
		}
		set.add(block);
		mFunctions.put(block.getInternalName(), block);
	}

	/** Surcharges déclarées sous ce nom interne public (préfixe module + nom brut), ou null. */
	public ArrayList<FunctionBlock> getOverloads(String publicInternalName) {
		return mOverloadSets.get(publicInternalName);
	}

	/** Vrai si la fonction participe à un ensemble de surcharges (>1 fonction de même nom). */
	public boolean isOverloaded(FunctionBlock block) {
		var set = mOverloadSets.get(block.publicInternalName());
		return set != null && set.size() > 1;
	}

	@Override
	public String getCode() {
		String str = "";
		for (var instruction : mFunctions.values()) {
			str += instruction.getCode() + "\n";
		}
		for (var clazz : mUserClasses.values()) {
			if (clazz.internal) continue;
			str += clazz.getCode() + "\n";
		}
		return str + super.getCode();
	}

	/**
	 * Retient le contenu d'un fichier lu par une fonction à argument fichier, sous la même clé
	 * qu'un include (le chemin seul ne distingue pas deux fichiers homonymes partout). Rend le nom
	 * du champ statique qui le porte, ou null si le fichier, ou le total des fichiers lus, est trop gros.
	 */
	public String addFileContent(AIFile file) {
		var key = file.getKey();
		var field = mFileFields.get(key);
		if (field != null) return field;
		var content = file.getCode();
		if (content.length() > MAX_FILE_CONTENT || mFileContentsLength + content.length() > MAX_FILE_CONTENTS) return null;
		field = "file_" + mFileContents.size();
		mFileFields.put(key, field);
		mFileContents.add(content);
		mFileContentsLength += content.length();
		return field;
	}

	public void writeJavaCode(JavaWriter writer, String className, String AIClass, Options options) {
		this.className = className;

		writer.addLine("import leekscript.runner.*;");
		writer.addLine("import leekscript.runner.values.*;");
		writer.addLine("import leekscript.runner.classes.*;");
		writer.addLine("import leekscript.common.*;");
		if (LeekFunctions.getExtraFunctionsImport() != null) {
			writer.addLine("import " + LeekFunctions.getExtraFunctionsImport() + ";");
		}
		writer.addLine();
		writer.addLine("public class " + className + " extends " + AIClass + " {");

		// Classes
		for (var clazz : mUserClassesList) {
			if (clazz.internal) continue;
			clazz.declareJava(this, writer);
		}

		for (int i = 0; i < mFileContents.size(); i++) {
			writer.addLine("private static final String file_" + i + " = " + LeekFileContent.javaString(mFileContents.get(i)) + ";");
		}

		// Constructor
		writer.addLine("public " + className + "() throws LeekRunException {");
		writer.addLine("super(" + mInstructions.size() + ", " + mCompiler.getCurrentAI().getVersion() + ");");

		// Une méthode par classe : le constructeur seul dépassait les 64 Ko de bytecode d'une méthode
		// Java (CODE_TOO_LARGE) sur une IA à plusieurs centaines de classes et quelques milliers de méthodes.
		for (var clazz : mUserClassesList) {
			if (clazz.internal) continue;
			clazz.defineClass(this, writer);
		}
		writer.addLine("}");

		// Classes initialize functions
		for (var clazz : mUserClassesList) {
			if (clazz.internal) continue;
			clazz.writeDefineClass(this, writer);
			clazz.writeCreateStaticFields(this, writer);
			clazz.writeInitializeStaticFields(this, writer);
		}

		// Static init
		writer.addLine("public void staticInit() throws LeekRunException {");

		// Create classes static fields
		for (var clazz : mUserClassesList) {
			if (clazz.internal) continue;
			clazz.createStaticFields(this, writer);
		}

		// Initialize classes fields
		for (var clazz : mUserClassesList) {
			if (clazz.internal) continue;
			clazz.initializeStaticFields(this, writer);
		}

		writer.addLine("}"); // Fin staticInit

		// Variables globales (nom de champ préfixé par module : getInternalName)
		for (var global : mGlobalesDeclarations) {
			var fieldName = global.getInternalName();
			if (getWordCompiler().getVersion() >= 2) {
				writer.addLine("private " + global.getType().getJavaPrimitiveName(getVersion()) + " g_" + fieldName + " = " + global.getType().getDefaultValue(writer, getVersion()) + ";");
			} else {
				writer.addLine("private Box<" + global.getType().getJavaName(getVersion()) + "> g_" + fieldName + " = new Box<" + global.getType().getJavaName(getVersion()) + ">(" + writer.getAIThis() + ");");
			}
			// Garde d'init une-fois : seulement pour les champs persistants (global).
			// Un var top-level v5 (non persistant) est ré-initialisé chaque tour.
			if (global.isPersistent()) {
				writer.addLine("private boolean g_init_" + fieldName + " = false;");
			}
		}
		// Fonctions redéfinies
		for (String redefined : mRedefinedFunctions) {
			writer.addCode("private Box rfunction_");
			writer.addCode(redefined);
			writer.addLine(";");
		}
		// Fonctions
		for (var instruction : mFunctions.values()) {
			instruction.writeJavaCode(this, writer, false);
		}

		writer.addLine("public Object runIA(Session session) throws LeekRunException {");

		for (var clazz : mUserClassesList) {
			if (clazz.internal) continue;
			clazz.writeJavaCode(this, writer, false);
		}

		// Import des variables de la session
		if (options.session() != null) {
			// Reparente les valeurs de la session à l'AI courant pour que les ops
			// consommées par les accès aux collections (Map/Array/...) soient
			// comptabilisées sur cette exécution, pas sur l'AI qui les a créées.
			writer.addLine("session.rebindAll(this);");
			for (var variable : options.session().getVariables().keySet()) {
				var type = options.session().getType(variable);
				if (type == Type.ANY) {
					writer.addLine("var u_" + variable + " = session.getVariable(\"" + variable + "\");");
				} else {
					// Box typée, comme une variable capturée : `.get()` rend alors le type déclaré
					writer.addLine("@SuppressWarnings(\"unchecked\") var u_" + variable + " = (Box<" + type.getJavaName(getVersion()) + ">) (Box) session.getVariable(\"" + variable + "\");");
				}
			}
		}

		super.writeJavaCode(this, writer, false);

		writer.addLine("}");

		writer.writeErrorFunction(mCompiler, mAIName);
		printFunctionInformations(writer);

		if (mRedefinedFunctions.size() > 0) {
			writer.addCode("public void init() throws LeekRunException {\n");
			for (String redefined : mRedefinedFunctions) {
				FunctionBlock user_function = getUserFunction(redefined);
				writer.addCode("rfunction_");
				writer.addCode(redefined);
				writer.addCode(" = new Box(" + writer.getAIThis() + ", ");
				if (user_function != null) {
					user_function.compileAnonymousFunction(this, writer);
				} else {
					var system_function = LeekFunctions.getValue(redefined, writer.getOptions().useExtra());
					writer.generateAnonymousSystemFunction(system_function);
					writer.addCode(system_function.getStandardClass() + "_" + redefined);
				}
				writer.addLine(");");
			}
			writer.addCode("}");
		}

		writer.writeGenericFunctions(this);
		writer.writeAnonymousSystemFunctions(this);
		writer.writeAnonymousUserFunctions(this);
		writer.writeMatchTempFields();

		writer.addLine("}");
	}

	public void writeBeforeReturn(JavaWriter writer) {
		// Export des variables de la session
		if (writer.options.session() != null && mVariables != null) {
			for (var variable : mVariables.entrySet()) {
				if (!writer.options.session().getVariables().containsKey(variable.getKey()) && variable.getValue().getVariableType() == VariableType.LOCAL) {
					writer.options.session().setType(variable.getKey(), variable.getValue().getDeclaredType());
					writer.addLine("session.setVariable(" + writer.getAIThis() + ", \"" + variable.getKey() + "\", u_" + variable.getKey() + ");");
				}
			}
		}
	}

	public void printFunctionInformations(JavaWriter writer) {

	}

	public Set<AIFile> getIncludedAIs() {
		return new LinkedHashSet<>(mIncluded.values());
	}

	public List<ClassDeclarationInstruction> getUserClasses() {
		return mUserClassesList;
	}

	public IACompiler getCompiler() {
		return mCompiler;
	}

	public void defineClass(ClassDeclarationInstruction classDeclaration) {
		// Clé = nom interne préfixé par module (byte-identique pour le principal).
		mUserClasses.put(classDeclaration.getInternalName(), classDeclaration);
		mDefinedClasses.put(classDeclaration.getInternalName(), classDeclaration);
	}

	/** Résolution module-aware d'une classe : imports d'abord, puis classes propres du
	 * module (clé préfixée), puis classes internes (Array/Map/... visibles partout). Un
	 * module importé ne voit PAS les classes USER du principal (privacy). */
	public ClassDeclarationInstruction getDefinedClass(String name, leekscript.compiler.bloc.Module module) {
		if (module != null) {
			var src = module.getImportedClassSource(name);
			if (src != null) return mDefinedClasses.get(src.classOrigin(name).prefix() + name);
			var own = mDefinedClasses.get(module.prefix() + name);
			if (own != null) return own;
		}
		var flat = mDefinedClasses.get(name);
		if (module != null && !module.isMain() && flat != null && !flat.internal) {
			return null; // classe user du principal invisible depuis un module importé
		}
		return flat;
	}

	public void addClassList(ClassDeclarationInstruction classDeclaration) {
		mUserClassesList.add(classDeclaration);
	}

	public void addClass(ClassDeclarationInstruction classDeclaration) {
		mUserClasses.put(classDeclaration.getName(), classDeclaration);
		mUserClassesList.add(classDeclaration);
		mDefinedClasses.put(classDeclaration.getName(), classDeclaration);
	}

	public ClassDeclarationInstruction getUserClass(String name) {
		return mUserClasses.get(name);
	}

	public ClassDeclarationInstruction getDefinedClass(String name) {
		return mDefinedClasses.get(name);
	}

	public Map<String, ClassDeclarationInstruction> getDefinedClasses() {
		return mDefinedClasses;
	}

	public void preAnalyze(WordCompiler compiler) throws LeekCompilerException {
		for (var clazz : mUserClassesList) {
			clazz.declare(compiler);
		}
		for (var function : mFunctions.values()) {
			function.declare(compiler);
		}
		for (var global : mGlobalesDeclarations) {
			global.declare(compiler);
		}
		for (var clazz : mUserClassesList) {
			clazz.preAnalyze(compiler);
		}
		for (var function : mFunctions.values()) {
			function.preAnalyze(compiler);
		}
		super.preAnalyze(compiler);
	}

	public void analyze(WordCompiler compiler) throws LeekCompilerException {
		// for (var global : mGlobalesDeclarations) {
		// 	global.analyze(compiler);
		// }
		for (var clazz : mUserClassesList) {
			clazz.analyze(compiler);
		}
		for (var function : mFunctions.values()) {
			function.analyze(compiler);
		}
		super.analyze(compiler);
	}

	public String getClassName() {
		return className;
	}

	public void setWordCompiler(WordCompiler compiler) {
		this.wordCompiler = compiler;
		compiler.setMainBlock(this);
	}

	public WordCompiler getWordCompiler() {
		return this.wordCompiler;
	}

	public int getVersion() {
		return this.wordCompiler.getVersion();
	}

	@Override
	public Location getLocation() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getNature() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Type getType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean validExpression(WordCompiler compiler, MainLeekBlock mainblock) throws LeekExpressionException {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean isStrict() {
		return mCompiler.getCurrentAI().isStrict();
	}
}
