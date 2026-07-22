package test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;

@ExtendWith(SummaryExtension.class)
public class TestFieldTypeRepro extends TestCommon {

	@Test
	public void testDynamicSetTypedField() throws Exception {
		section("Set de champ typé via une variable any (#4595)");
		// Le chemin dynamique doit appliquer les mêmes conversions que le chemin statique
		code_v4_("class A { integer x } A a = new A() a.x = 1.5 return a.x").equals("1");
		code_v4_("class A { integer x } var a = new A() a.x = 1.5 return a.x").equals("1");
		code_v4_("class A { integer x } var a = new A() a.x = null return a.x").equals("0");
		code_v4_("class A { integer x } var a = new A() a.x = 12 return a.x").equals("12");
		code_v4_("class A { integer x } var a = new A() a.x = true return a.x").equals("1");
		code_v4_("class A { real x } var a = new A() a.x = 3 return a.x").equals("3.0");
		code_v4_("class A { real x } var a = new A() a.x = null return a.x").equals("0.0");
		code_v4_("class A { boolean x } var a = new A() a.x = 1 return a.x").equals("true");
		code_v4_("class A { integer | null x = 8 } var a = new A() a.x = null return a.x").equals("null");
		code_v4_("class A { integer | null x } var a = new A() a.x = 2.5 return a.x").equals("2");
		// La valeur de l'expression d'assignation est la valeur convertie
		code_v4_("class A { integer x } var a = new A() return a.x = 1.5").equals("1");
		// Champ string : pas de conversion implicite depuis un integer (cast impossible),
		// l'assignation est ignorée avec une erreur, le champ garde sa valeur
		code_v4_("class A { string x = 'a' } var a = new A() a.x = 12 return a.x").equals("\"a\"");
	}

	@Test
	public void testDynamicCompoundOpsTypedField() throws Exception {
		section("Opérations composées sur champ typé via any (#4595)");
		code_v4_("class A { integer x = 3 } var a = new A() a.x += 0.5 return a.x").equals("3");
		code_v4_("class A { integer x = 3 } var a = new A() a.x /= 2 return a.x").equals("1");
		code_v4_("class A { integer x = 3 } var a = new A() a.x++ return a.x").equals("4");
		code_v4_("class A { integer x = 3 } var a = new A() return a.x++").equals("3");
		code_v4_("class A { integer x = 3 } var a = new A() return ++a.x").equals("4");
		code_v4_("class A { real x = 1.5 } var a = new A() a.x *= 2 return a.x").equals("3.0");
	}

	@Test
	public void testReproJoueur50332() throws Exception {
		section("Repro joueur 50332 : champs remplis depuis des données any");
		// getWeapon() peut renvoyer null, getChipEffects() renvoie des reals :
		// assignés à des champs integer via une variable any, ça doit convertir
		// silencieusement comme en typé, pas UNKNOWN_FIELD
		code_v4_("class E { integer minValue integer maxValue } function f() { return [2, 40.5, 60.5] } var e = new E() var d = f() e.minValue = d[1] e.maxValue = d[2] return e.minValue + e.maxValue").equals("100");
	}
}
