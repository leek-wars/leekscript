package test;

import org.junit.jupiter.api.Test;

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
	public void versionGate() throws Exception {
		section("Modules (v5) - export/import réservés avant v5");
		// export/import sont des features v5 : erreur dure avant v5 (mots-clés réservés depuis v3).
		code_v4("export function f() { return 1 } return f()").error(Error.KEYWORD_UNEXPECTED);
		code_v4("import { x } from \"utils\" return 1").error(Error.KEYWORD_UNEXPECTED);
	}
}
