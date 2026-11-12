package leekscript.compiler.bloc;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import leekscript.compiler.AIFile;

/**
 * Un module v5 = un fichier source. Contrairement à {@code include()} qui fusionne
 * tout à plat dans le {@link MainLeekBlock}, un module a ses symboles privés par
 * défaut : seuls les symboles marqués {@code export} sont visibles via
 * {@code import { ... } from "..."}.
 *
 * <p>Le module principal (l'entrypoint et tout ce qu'il {@code include()}) a
 * {@link #isMain} = true et un préfixe de codegen vide : les noms émis restent
 * identiques au bit près à v1–v4. Les modules importés reçoivent un préfixe
 * {@code $m<nom>_<empreinte>$} tiré du fichier, pas de l'ordre des imports : ajouter
 * ou retirer un import ne renomme rien dans le Java des autres modules, et un module
 * porte le même nom quel que soit le point d'entrée qui l'importe. Le {@code $} n'est
 * pas un caractère d'identifiant LeekScript, donc aucun nom utilisateur ne peut entrer
 * en collision avec un nom qualifié.
 *
 * <p>Pilier 3 (modules), phase 3b. Slice 1 : fonctions uniquement. Les classes,
 * les globales et {@code import * as Ns} viennent ensuite.
 */
public class Module {

	private final AIFile file;
	private final boolean main;
	private final String tag;
	// Préfixe de codegen précalculé (immuable) : prefix() est appelé sur chaque
	// résolution de nom, autant ne pas réallouer la chaîne à chaque fois.
	private final String prefix;

	// Noms de fonctions exportées par ce module (sous-ensemble de ses fonctions).
	private final Set<String> exportedFunctions = new HashSet<>();
	// Imports de fonctions : nom local -> module source (le nom importé est, pour
	// l'instant, identique au nom exporté ; l'aliasing `as` viendra plus tard).
	private final Map<String, Module> importedFunctions = new HashMap<>();
	// Idem pour les variables/globales de niveau module (export var / export global).
	private final Set<String> exportedVars = new HashSet<>();
	private final Map<String, Module> importedVars = new HashMap<>();
	// Idem pour les classes (export class).
	private final Set<String> exportedClasses = new HashSet<>();
	private final Map<String, Module> importedClasses = new HashMap<>();
	// Imports namespace `import * as Ns from "..."` : alias local -> module source.
	private final Map<String, Module> namespaceImports = new HashMap<>();

	/** @param tag nom stable du module ({@link #tagOf}), ignoré pour le module principal */
	public Module(AIFile file, String tag, boolean main) {
		this.file = file;
		this.main = main;
		this.tag = tag;
		this.prefix = main ? "" : PREFIX_START + tag + PREFIX_END;
	}

	private static final int MAX_TAG_NAME = 24;

	/**
	 * Nom stable d'un module : le nom du fichier (pour lire le Java généré) suivi de l'empreinte de
	 * son identité (pour l'unicité : deux fichiers peuvent porter le même nom). Uniquement des
	 * caractères d'identifiant Java, jamais de `$`.
	 */
	public static String tagOf(AIFile file) {
		var path = file.getPath();
		var name = path.substring(Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\')) + 1);
		int dot = name.lastIndexOf('.');
		if (dot > 0) name = name.substring(0, dot);
		var tag = new StringBuilder();
		for (int i = 0; i < name.length() && tag.length() < MAX_TAG_NAME; i++) {
			char c = name.charAt(i);
			tag.append(c < 128 && Character.isLetterOrDigit(c) ? c : '_');
		}
		return tag.append('_').append(file.getKeyHash()).toString();
	}

	// Le préfixe ne peut pas heurter un nom LeekScript : `$` n'y est pas un caractère d'identifiant
	private static final String PREFIX_START = "$m";
	private static final char PREFIX_END = '$';

	/** Retire le préfixe de module d'un nom du code généré (`$mhitmap_1x2y3z$HitMap` -> `HitMap`). */
	public static String stripPrefix(String name) {
		if (!name.startsWith(PREFIX_START)) return name;
		int end = name.indexOf(PREFIX_END, PREFIX_START.length());
		return end < 0 ? name : name.substring(end + 1);
	}

	public AIFile getFile() {
		return file;
	}

	/** Nom stable du module ({@link #tagOf}) : dans son préfixe, et dans le nom de sa classe Java. */
	public String tag() {
		return tag;
	}

	public boolean isMain() {
		return main;
	}

	/** Préfixe des symboles du module dans les tables du compilateur, qui tiennent ceux de tous
	 * les modules. Vide pour le module principal. */
	public String prefix() {
		return prefix;
	}

	// Dans le Java généré, le préfixe ne sert que tant que les modules partagent une classe Java :
	// un module qui a la sienne y nomme ses symboles sans préfixe (`f_area`, `u_Vec`).
	private boolean ownJavaClass = false;

	public void setOwnJavaClass() {
		ownJavaClass = true;
	}

	// Module d'un vrai package (dossier à `package.leek`), pas du package du programme
	// (son identité : l'empreinte de sa racine ; null dans le package du programme)
	private String packageId = null;

	public void setPackage(String packageId) {
		this.packageId = packageId;
	}

	public String getPackage() {
		return packageId;
	}

	public boolean isInPackage() {
		return packageId != null;
	}

	/** Préfixe des symboles du module dans le Java généré (`f_`, `g_`, `u_`, `ufunction_`…). */
	public String javaPrefix() {
		return ownJavaClass ? "" : prefix;
	}

	public Set<String> getExportedFunctions() {
		return exportedFunctions;
	}

	public Set<String> getExportedVars() {
		return exportedVars;
	}

	public Set<String> getExportedClasses() {
		return exportedClasses;
	}

	public void addExportedFunction(String name) {
		exportedFunctions.add(name);
	}

	public boolean isExportedFunction(String name) {
		return exportedFunctions.contains(name);
	}

	public void addImportedFunction(String localName, Module source) {
		importedFunctions.put(localName, source);
	}

	/** Module source si {@code name} est importé dans ce module, sinon null. */
	public Module getImportedFunctionSource(String name) {
		return importedFunctions.get(name);
	}

	public void addExportedVar(String name) {
		exportedVars.add(name);
	}

	public boolean isExportedVar(String name) {
		return exportedVars.contains(name);
	}

	public void addImportedVar(String localName, Module source) {
		importedVars.put(localName, source);
	}

	/** Module source si la variable {@code name} est importée dans ce module, sinon null. */
	public Module getImportedVarSource(String name) {
		return importedVars.get(name);
	}

	public void addExportedClass(String name) {
		exportedClasses.add(name);
	}

	public boolean isExportedClass(String name) {
		return exportedClasses.contains(name);
	}

	public void addImportedClass(String localName, Module source) {
		importedClasses.put(localName, source);
	}

	/** Module source si la classe {@code name} est importée dans ce module, sinon null. */
	public Module getImportedClassSource(String name) {
		return importedClasses.get(name);
	}

	/**
	 * Lie `name` à l'export de `source` du même nom ; false si `source` ne l'exporte pas (encore).
	 * `reexport` (`export { name } from "…"`) : le nom devient aussi un export de ce module.
	 */
	public boolean bindImport(String name, Module source, boolean reexport) {
		if (source.isExportedFunction(name)) {
			addImportedFunction(name, source);
			if (reexport) addExportedFunction(name);
		} else if (source.isExportedVar(name)) {
			addImportedVar(name, source);
			if (reexport) addExportedVar(name);
		} else if (source.isExportedClass(name)) {
			addImportedClass(name, source);
			if (reexport) addExportedClass(name);
		} else {
			return false;
		}
		return true;
	}

	// Un module qui ré-exporte un nom l'importe lui-même : on suit les imports jusqu'au module
	// qui le déclare. Borné : deux modules qui se ré-exportent l'un l'autre ne bouclent pas.
	private static final int MAX_REEXPORTS = 64;

	private Module origin(String name, java.util.function.Function<Module, Map<String, Module>> imports) {
		var module = this;
		for (int i = 0; i < MAX_REEXPORTS; i++) {
			var source = imports.apply(module).get(name);
			if (source == null) return module;
			module = source;
		}
		return module;
	}

	/** Module qui déclare la fonction `name` importée ici, en suivant les ré-exports. */
	public Module functionOrigin(String name) {
		return origin(name, m -> m.importedFunctions);
	}

	/** Module qui déclare la variable `name` importée ici, en suivant les ré-exports. */
	public Module varOrigin(String name) {
		return origin(name, m -> m.importedVars);
	}

	/** Module qui déclare la classe `name` importée ici, en suivant les ré-exports. */
	public Module classOrigin(String name) {
		return origin(name, m -> m.importedClasses);
	}

	// Noms de premier niveau du module : un nom n'a qu'une liaison, soit déclaré ici, soit importé
	// d'un seul module (`import { f }` et `export { f }` du même module désignent la même liaison).
	private final Set<String> declaredNames = new HashSet<>();
	private final Map<String, Module> importedNames = new HashMap<>();

	/** Déclare `name` dans ce module ; false si le nom est déjà importé. */
	public boolean declare(String name) {
		declaredNames.add(name);
		return !importedNames.containsKey(name) && !namespaceImports.containsKey(name);
	}

	/** Réserve `name` pour un import nommé depuis `source` ; false si le nom est déjà pris. */
	public boolean reserveImport(String name, Module source) {
		return !declaredNames.contains(name) && !namespaceImports.containsKey(name) && reserve(importedNames, name, source);
	}

	/** `import * as alias` de `source` ; false, sans rien lier, si le nom est déjà pris. */
	public boolean addNamespaceImport(String alias, Module source) {
		return !declaredNames.contains(alias) && !importedNames.containsKey(alias) && reserve(namespaceImports, alias, source);
	}

	private static boolean reserve(Map<String, Module> reserved, String name, Module source) {
		var previous = reserved.putIfAbsent(name, source);
		return previous == null || previous == source;
	}

	/** Module aliasé par {@code alias} via `import * as alias`, sinon null. */
	public Module getNamespaceImport(String alias) {
		return namespaceImports.get(alias);
	}
}
