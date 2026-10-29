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
 * collision-proof {@code $m<id>$} (le {@code $} n'est pas un caractère
 * d'identifiant LeekScript, donc aucun nom utilisateur ne peut entrer en
 * collision avec un nom qualifié).
 *
 * <p>Pilier 3 (modules), phase 3b. Slice 1 : fonctions uniquement. Les classes,
 * les globales et {@code import * as Ns} viennent ensuite.
 */
public class Module {

	private final AIFile file;
	private final int id;
	private final boolean main;
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

	public Module(AIFile file, int id, boolean main) {
		this.file = file;
		this.id = id;
		this.main = main;
		this.prefix = main ? "" : "$m" + id + "$";
	}

	public AIFile getFile() {
		return file;
	}

	public int getId() {
		return id;
	}

	public boolean isMain() {
		return main;
	}

	/** Préfixe de codegen, intégré dans les noms Java (`f_`, `ufunction_`, ...).
	 * Vide pour le module principal → v1–v4 byte-identique. */
	public String prefix() {
		return prefix;
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

	/** Lie `name` à l'export de `source` du même nom ; false si `source` ne l'exporte pas (encore). */
	public boolean bindImport(String name, Module source) {
		if (source.isExportedFunction(name)) {
			addImportedFunction(name, source);
		} else if (source.isExportedVar(name)) {
			addImportedVar(name, source);
		} else if (source.isExportedClass(name)) {
			addImportedClass(name, source);
		} else {
			return false;
		}
		return true;
	}

	public void addNamespaceImport(String alias, Module source) {
		namespaceImports.put(alias, source);
	}

	/** Module aliasé par {@code alias} via `import * as alias`, sinon null. */
	public Module getNamespaceImport(String alias) {
		return namespaceImports.get(alias);
	}
}
