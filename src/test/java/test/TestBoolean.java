package test;


import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;

import leekscript.common.Error;

@ExtendWith(SummaryExtension.class)
public class TestBoolean extends TestCommon {


		@Test
	public void testBoolean() throws Exception {
		section("Boolean");
		code_v1_2("return true").equals("true");
		code_v1_2("return True").equals("true");
		code_v1_2("return TRUE").equals("true");
		code_v3_("return true").equals("true");
		code_v3_("return True").error(Error.UNKNOWN_VARIABLE_OR_FUNCTION);
		code_v3_("return TRUE").error(Error.UNKNOWN_VARIABLE_OR_FUNCTION);

		code_v1_2("return false").equals("false");
		code_v1_2("return False").equals("false");
		code_v1_2("return FALSE").equals("false");
		code_v3_("return false").equals("false");
		code_v3_("return False").error(Error.UNKNOWN_VARIABLE_OR_FUNCTION);
		code_v3_("return FALSE").error(Error.UNKNOWN_VARIABLE_OR_FUNCTION);
	}

	@Test
	public void testKeywordOperatorCaseSensitivity() throws Exception {
		section("Logical operator keywords: case-insensitive in v1-2, case-sensitive in v3+");
		// Pendant du test des littéraux True/TRUE ci-dessus, mais pour les OPÉRATEURS.
		// v1-2 : lexer case-insensitive, `AND` majuscule est l'opérateur logique.
		code_v1_2("return true AND false").equals("false");
		// v3+ : seules les minuscules sont des mots-clés ; `and` marche...
		code_v3("return true and false").equals("false");
		// ...mais `AND` majuscule est un identifiant, donc `return true AND false` casse
		// (le `return` termine, `AND false` devient une instruction inatteignable).
		code_v3("return true AND false").error(Error.CANT_ADD_INSTRUCTION_AFTER_BREAK);
	}

	@Test
	public void testBoolean_operator_Not() throws Exception {
		section("Boolean.operator !");
		code_v1_2("return not true").equals("false");
		code_v1_2("return not false").equals("true");
		code_v1_2("return Not true").equals("false");
		code_v1_2("return Not false").equals("true");
		code_v1_2("return NOT true").equals("false");
		code_v1_2("return NOT false").equals("true");
		code_v3_("return not true").equals("false");
		code_v3_("return not false").equals("true");
		code_v3_("return Not true").error(Error.CANT_ADD_INSTRUCTION_AFTER_BREAK);
		code_v3_("return Not false").error(Error.CANT_ADD_INSTRUCTION_AFTER_BREAK);
		code_v3_("return NOT true").error(Error.CANT_ADD_INSTRUCTION_AFTER_BREAK);
		code_v3_("return NOT false").error(Error.CANT_ADD_INSTRUCTION_AFTER_BREAK);
	}

	@Test
	public void testBoolean_operator_xor() throws Exception {
		section("Boolean.operator xor");
		code("return true xor true").debug().equals("false");
		code("return true xor false").equals("true");
		code("return false xor true").equals("true");
		code("return false xor false").equals("false");
	}

}
