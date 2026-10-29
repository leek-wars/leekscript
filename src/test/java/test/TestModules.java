package test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import leekscript.compiler.LeekScript;

import leekscript.common.Error;

/**
 * Modules (v5) : import / export — cas mono-fichier (parsing, gate de version,
 * export consommé). La résolution multi-fichiers (import effectif, privacy,
 * codegen) est couverte par {@link TestModulesResolution}.
 */
public class TestModules extends TestCommon {

	@Test
	public void exportSyntax() throws Exception {
		section("Modules (v5) - export préfixe une déclaration");
		// `export` préfixe une déclaration : consommé, la déclaration marche normalement.
		code_v5_("export function f() { return 1 } return f()").equals("1");
		code_v5_("export global g = 5 return g").equals("5");
		code_v5_("export class C { public integer v constructor() { this.v = 7 } } return new C().v").equals("7");
	}

	@Test
	public void moduleFieldVar() throws Exception {
		section("Modules (v5) - export var / global = champs du module");
		// `export var` = champ : visible des fonctions du module (un `var` nu ne l'est pas).
		code_v5_("export var x = 5 function f() { return x } return f()").equals("5");
		code_v5_("export var c = 0 c = c + 3 return c").equals("3");
		// `global` = champ persistant, visible des fonctions (inchangé).
		code_v5_("global g = 7 function f() { return g } return f()").equals("7");
		// Un `var` NU au top-level reste un local : invisible aux fonctions.
		code_v5_("var y = 5 function f() { return y } return f()").error(leekscript.common.Error.UNKNOWN_VARIABLE_OR_FUNCTION);
	}

	@Test
	public void versionGate() throws Exception {
		section("Modules (v5) - export/import réservés avant v5");
		// export/import sont des features v5 : erreur dure avant v5 (mots-clés réservés depuis v3).
		code_v4("export function f() { return 1 } return f()").error(Error.KEYWORD_UNEXPECTED);
		code_v4("import { x } from \"utils\" return 1").error(Error.KEYWORD_UNEXPECTED);
	}

	@Test
	public void includeRemovedInV5() throws Exception {
		section("Modules (v5) - include() supprimé au profit de import/export");
		// include() est une feature v≤4 ; en v5 c'est une erreur dure.
		code_v5("include(\"utils\") return 1").error(Error.INCLUDE_REMOVED_IN_V5);
	}

	@Test
	public void unreleasedVersionNeedsAccess() throws Exception {
		// `// @version:5` : accepté pour un propriétaire qui y a accès, sinon pragma invalide et
		// version par défaut (LS4), comme une version inconnue
		var open = new leekscript.compiler.AIFile("open", "// @version:5\nreturn 1", 0, LeekScript.LATEST_VERSION, 1, false);
		leekscript.compiler.PragmaParser.apply(open);
		assertEquals(5, open.getVersion());
		var closed = new leekscript.compiler.AIFile("closed", "// @version:5\nreturn 1", 0, LeekScript.LATEST_VERSION, NO_UNRELEASED_ACCESS, false);
		leekscript.compiler.PragmaParser.apply(closed);
		assertEquals(LeekScript.LATEST_VERSION, closed.getVersion());
		assertTrue(closed.getErrors().stream().anyMatch(e -> e.error == leekscript.common.Error.PRAGMA_INVALID_VALUE));
		// Version 5 venue d'ailleurs (base, création) sans accès : version par défaut
		var stored = new leekscript.compiler.AIFile("stored", "return 1", 0, 5, NO_UNRELEASED_ACCESS, false);
		leekscript.compiler.PragmaParser.apply(stored);
		assertEquals(LeekScript.LATEST_VERSION, stored.getVersion());
	}
}
