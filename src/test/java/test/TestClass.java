package test;


import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;

import leekscript.common.Error;

@ExtendWith(SummaryExtension.class)
public class TestClass extends TestCommon {


		@Test
	public void testClass_toBoolean() throws Exception {
		section("Class toBoolean");
		code_v2_("class A {} return !!A").equals("true");
		code_v2_("class A {} if (A) { return 12 } return null").equals("12");
	}

	@Test
	public void testClass_and_variable() throws Exception {
		section("Class and variable");
		code_v2_("class A {} var A = 12;").error(Error.VARIABLE_NAME_UNAVAILABLE);
		code_v2_("var A = 12; class A {}").error(Error.VARIABLE_NAME_UNAVAILABLE);
		code_v2_("class A {} function f(A) { A = 1 return A } return f(2)").equals("1");
		code_v2_("class A {} function f() { var A = 1 return A } return f()").equals("1");
		code_v2_("class A {} A = 12").error(Error.CANT_ASSIGN_VALUE);
	}

	@Test
	public void testClass_name() throws Exception {
		section("Class.name");
		code_v2_("class A { public m() { return name }} return new A().m()").error(Error.UNKNOWN_VARIABLE_OR_FUNCTION);
		code_v2_("class A { public m() { return class.name }} return new A().m()").equals("\"A\"");
		code_v2_("class A { public static m() { return name }} return A.m()").error(Error.UNKNOWN_VARIABLE_OR_FUNCTION);
		code_v2_("class A { public static m() { return class.name }} return A.m()").equals("\"A\"");
	}

	@Test
	public void testMethod_call_with_nullable_argument() throws Exception {
		section("Method call with nullable argument");
		// Calling a method without this. prefix with a nullable parameter
		code_v2_("class A { private Array<integer> list = [] private add(integer | null v) { if (v != null) { push(list, v) } } public run() { add(null) add(3) return list } } return new A().run()").equals("[3]");
		// Same with this. prefix
		code_v2_("class A { private Array<integer> list = [] private add(integer | null v) { if (v != null) { push(list, v) } } public run() { this.add(null) this.add(3) return list } } return new A().run()").equals("[3]");
		// Nullable field passed to method without this.
		code_v2_("class A { private integer? val = null private get(integer? v) { return v } public run() { return get(val) } } return new A().run()").equals("null");
		code_v2_("class A { private integer? val = null private get(integer? v) { return v } public run() { val = 42 return get(val) } } return new A().run()").equals("42");
		// Class type nullable parameter
		code_v2_("class B { public integer x = 0 } class A { private check(B | null b) { return b != null ? b.x : -1 } public run() { return check(null) + ',' + check(new B()) } } return new A().run()").equals("\"-1,0\"");
	}

	@Test
	public void testNonMinusexistent_field_access() throws Exception {
		section("Non-existent field access");
		// this.field where field doesn't exist: error in strict, warning in non-strict
		code_strict_v2_("class A { public m() { return this.x } } return new A().m()").error(Error.CLASS_MEMBER_DOES_NOT_EXIST);
		code_v2_("class A { public m() { return this.x } } return new A().m()").warning(Error.CLASS_MEMBER_DOES_NOT_EXIST);
		// Existing field should still work
		code_v2_("class A { public integer x = 42 public m() { return this.x } } return new A().m()").equals("42");
		// Implicit this field access to non-existent field
		code_strict_v2_("class A { public m() { return x } } return new A().m()").error(Error.UNKNOWN_VARIABLE_OR_FUNCTION);
		// Non-existent field on typed variable
		code_strict_v2_("class Foo { } Foo f = new Foo() return f.x").error(Error.CLASS_MEMBER_DOES_NOT_EXIST);
		code_v2_("class Foo { } Foo f = new Foo() return f.x").warning(Error.CLASS_MEMBER_DOES_NOT_EXIST);
	}

}
