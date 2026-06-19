package test;

import org.junit.jupiter.api.Test;

import leekscript.common.Error;

/**
 * Modules (v5) : import / export. Phase 3a = syntaxe seule (parse + gate de version),
 * sans scoping ni résolution de module (ça vient en 3b).
 */
public class TestModules extends TestCommon {

	@Test
	public void exportImportSyntax() throws Exception {
		section("Modules (v5) - 3a: syntaxe export/import (parse seul)");
		// `export` préfixe une déclaration : consommé, la déclaration marche normalement
		// (pas encore de visibilité privée/exportée en 3a).
		code_v5_("export function f() { return 1 } return f()").equals("1");
		code_v5_("export global g = 5 return g").equals("5");
		code_v5_("export class C { public integer v constructor() { this.v = 7 } } return new C().v").equals("7");
		// `import { ... } from "file"` parse (3a : pas de résolution ni de liaison).
		code_v5_("import { clamp } from \"utils\" return 1").equals("1");
		code_v5_("import { a, b, c } from \"lib\" return 2").equals("2");
		// export/import sont des features v5 : erreur dure avant v5 (mots-clés réservés depuis v3).
		code_v4("export function f() { return 1 } return f()").error(Error.KEYWORD_UNEXPECTED);
		code_v4("import { x } from \"utils\" return 1").error(Error.KEYWORD_UNEXPECTED);
	}
}
