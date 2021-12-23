package leekscript.compiler.expression;

import leekscript.compiler.AnalyzeError;
import leekscript.compiler.IAWord;
import leekscript.compiler.JavaWriter;
import leekscript.compiler.WordCompiler;
import leekscript.compiler.AnalyzeError.AnalyzeErrorLevel;
import leekscript.compiler.bloc.ClassMethodBlock;
import leekscript.compiler.bloc.MainLeekBlock;
import leekscript.compiler.expression.LeekVariable.VariableType;
import leekscript.common.Error;
import leekscript.common.Type;

public class LeekObjectAccess extends AbstractExpression {

	private AbstractExpression object;
	private IAWord field;

	public LeekObjectAccess(AbstractExpression object, IAWord field) {
		this.object = object;
		this.field = field;
	}

	@Override
	public int getNature() {
		return OBJECT_ACCESS;
	}

	@Override
	public Type getType() {
		return Type.ANY;
	}

	@Override
	public String getString() {
		return object.getString() + "." + field.getWord();
	}

	public AbstractExpression getObject() {
		return object;
	}

	@Override
	public boolean validExpression(WordCompiler compiler, MainLeekBlock mainblock) throws LeekExpressionException {
		return object.validExpression(compiler, mainblock);
	}

	@Override
	public boolean isLeftValue() {
		return true;
	}

	@Override
	public boolean nullable() {
		return object.nullable();
	}

	@Override
	public void analyze(WordCompiler compiler) {
		// System.out.println("oa " + getString());
		object.analyze(compiler);
		operations = 1 + object.operations;

		if (field.getWord().equals("class")) {
			return; // .class is available everywhere
		}

		if (object instanceof LeekVariable) {
			var v = (LeekVariable) object;
			if (v.getName().equals("this")) {
				// this, check field exists in class
				var clazz = compiler.getCurrentClass();
				if (clazz != null && !clazz.hasMember(field.getWord())) {
					compiler.addError(new AnalyzeError(field, AnalyzeErrorLevel.ERROR, Error.CLASS_MEMBER_DOES_NOT_EXIST, new String[] { clazz.getName(), field.getWord() }));
				}
			} else if (v.getVariableType() == VariableType.CLASS || v.getVariableType() == VariableType.THIS_CLASS) {
				var clazz = v.getVariableType() == VariableType.CLASS ? v.getClassDeclaration() : compiler.getCurrentClass();
				operations -= 1;
				if (field.getWord().equals("name") || field.getWord().equals("super") || field.getWord().equals("fields") || field.getWord().equals("staticFields") || field.getWord().equals("methods") || field.getWord().equals("staticMethods")) {
					return; // OK
				}
				if (clazz.hasStaticMember(field.getWord())) {
					return; // OK
				}
				if (clazz.hasMethod(field.getWord())) {
					return; // OK
				}
				compiler.addError(new AnalyzeError(field, AnalyzeErrorLevel.ERROR, Error.CLASS_STATIC_MEMBER_DOES_NOT_EXIST, new String[] { clazz.getName(), field.getWord() }));
			}
		}
	}

	public String getField() {
		return field.getWord();
	}

	public IAWord getFieldToken() {
		return field;
	}

	@Override
	public void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer) {
		if (mainblock.getWordCompiler().getVersion() >= 3 && field.getWord().equals("class")) {
			writer.addCode("getClass(");
			object.writeJavaCode(mainblock, writer);
			writer.addCode(")");
		} else {
			writer.addCode("getField(");
			object.writeJavaCode(mainblock, writer);
			var from_class = writer.currentBlock instanceof ClassMethodBlock ? "u_class" : "null";
			writer.addCode(", \"" + field.getWord() + "\", " + from_class + ")");
		}
	}

	@Override
	public void compileL(MainLeekBlock mainblock, JavaWriter writer) {
		assert (object.isLeftValue() && !object.nullable());

		if (mainblock.getWordCompiler().getVersion() >= 3 && field.getWord().equals("class")) {
			writer.addCode("getClass(");
			object.writeJavaCode(mainblock, writer);
			writer.addCode(")");
		} else {
			writer.addCode("getField(");
			object.writeJavaCode(mainblock, writer);
			var from_class = writer.currentBlock instanceof ClassMethodBlock ? "u_class" : "null";
			writer.addCode(", \"" + field.getWord() + "\", " + from_class + ")");
		}
	}

	@Override
	public void compileSet(MainLeekBlock mainblock, JavaWriter writer, AbstractExpression expr) {
		assert (object.isLeftValue() && !object.nullable());

		writer.addCode("setField(");
		object.writeJavaCode(mainblock, writer);
		writer.addCode(", \"" + field.getWord() + "\", ");
		expr.writeJavaCode(mainblock, writer);
		writer.addCode(")");
	}

	@Override
	public void compileIncrement(MainLeekBlock mainblock, JavaWriter writer) {
		assert (object.isLeftValue() && !object.nullable());

		writer.addCode("field_inc(");
		object.writeJavaCode(mainblock, writer);
		writer.addCode(", \"" + field.getWord() + "\")");
	}

	@Override
	public void compilePreIncrement(MainLeekBlock mainblock, JavaWriter writer) {
		assert (object.isLeftValue() && !object.nullable());

		writer.addCode("field_pre_inc(");
		object.writeJavaCode(mainblock, writer);
		writer.addCode(", \"" + field.getWord() + "\")");
	}

	@Override
	public void compileDecrement(MainLeekBlock mainblock, JavaWriter writer) {
		assert (object.isLeftValue() && !object.nullable());

		writer.addCode("field_dec(");
		object.writeJavaCode(mainblock, writer);
		writer.addCode(", \"" + field.getWord() + "\")");
	}

	@Override
	public void compilePreDecrement(MainLeekBlock mainblock, JavaWriter writer) {
		assert (object.isLeftValue() && !object.nullable());

		writer.addCode("field_pre_dec(");
		object.writeJavaCode(mainblock, writer);
		writer.addCode(", \"" + field.getWord() + "\")");
	}

	@Override
	public void compileAddEq(MainLeekBlock mainblock, JavaWriter writer, AbstractExpression expr) {
		assert (object.isLeftValue() && !object.nullable());

		writer.addCode("field_add_eq(");
		object.writeJavaCode(mainblock, writer);
		writer.addCode(", \"" + field.getWord() + "\", ");
		expr.writeJavaCode(mainblock, writer);
		writer.addCode(")");
	}

	@Override

	public void compileSubEq(MainLeekBlock mainblock, JavaWriter writer, AbstractExpression expr) {
		assert (object.isLeftValue() && !object.nullable());

		writer.addCode("field_sub_eq(");
		object.writeJavaCode(mainblock, writer);
		writer.addCode(", \"" + field.getWord() + "\", ");
		expr.writeJavaCode(mainblock, writer);
		writer.addCode(")");
	}

	@Override

	public void compileMulEq(MainLeekBlock mainblock, JavaWriter writer, AbstractExpression expr) {
		assert (object.isLeftValue() && !object.nullable());

		writer.addCode("field_mul_eq(");
		object.writeJavaCode(mainblock, writer);
		writer.addCode(", \"" + field.getWord() + "\", ");
		expr.writeJavaCode(mainblock, writer);
		writer.addCode(")");
	}

	@Override

	public void compilePowEq(MainLeekBlock mainblock, JavaWriter writer, AbstractExpression expr) {
		assert (object.isLeftValue() && !object.nullable());

		writer.addCode("field_pow_eq(");
		object.writeJavaCode(mainblock, writer);
		writer.addCode(", \"" + field.getWord() + "\", ");
		expr.writeJavaCode(mainblock, writer);
		writer.addCode(")");
	}

	@Override

	public void compileDivEq(MainLeekBlock mainblock, JavaWriter writer, AbstractExpression expr) {
		assert (object.isLeftValue() && !object.nullable());

		writer.addCode("field_div_eq(");
		object.writeJavaCode(mainblock, writer);
		writer.addCode(", \"" + field.getWord() + "\", ");
		expr.writeJavaCode(mainblock, writer);
		writer.addCode(")");
	}

	@Override

	public void compileModEq(MainLeekBlock mainblock, JavaWriter writer, AbstractExpression expr) {
		assert (object.isLeftValue() && !object.nullable());

		writer.addCode("field_mod_eq(");
		object.writeJavaCode(mainblock, writer);
		writer.addCode(", \"" + field.getWord() + "\", ");
		expr.writeJavaCode(mainblock, writer);
		writer.addCode(")");
	}

	@Override

	public void compileBitOrEq(MainLeekBlock mainblock, JavaWriter writer, AbstractExpression expr) {
		assert (object.isLeftValue() && !object.nullable());

		writer.addCode("field_bor_eq(");
		object.writeJavaCode(mainblock, writer);
		writer.addCode(", \"" + field.getWord() + "\", ");
		expr.writeJavaCode(mainblock, writer);
		writer.addCode(")");
	}

	@Override

	public void compileBitAndEq(MainLeekBlock mainblock, JavaWriter writer, AbstractExpression expr) {
		assert (object.isLeftValue() && !object.nullable());

		writer.addCode("field_band_eq(");
		object.writeJavaCode(mainblock, writer);
		writer.addCode(", \"" + field.getWord() + "\", ");
		expr.writeJavaCode(mainblock, writer);
		writer.addCode(")");
	}

	@Override

	public void compileBitXorEq(MainLeekBlock mainblock, JavaWriter writer, AbstractExpression expr) {
		assert (object.isLeftValue() && !object.nullable());

		writer.addCode("field_bxor_eq(");
		object.writeJavaCode(mainblock, writer);
		writer.addCode(", \"" + field.getWord() + "\", ");
		expr.writeJavaCode(mainblock, writer);
		writer.addCode(")");
	}

	@Override

	public void compileShiftLeftEq(MainLeekBlock mainblock, JavaWriter writer, AbstractExpression expr) {
		assert (object.isLeftValue() && !object.nullable());

		writer.addCode("field_shl_eq(");
		object.writeJavaCode(mainblock, writer);
		writer.addCode(", \"" + field.getWord() + "\", ");
		expr.writeJavaCode(mainblock, writer);
		writer.addCode(")");
	}


	@Override

	public void compileShiftRightEq(MainLeekBlock mainblock, JavaWriter writer, AbstractExpression expr) {
		assert (object.isLeftValue() && !object.nullable());

		writer.addCode("field_shr_eq(");
		object.writeJavaCode(mainblock, writer);
		writer.addCode(", \"" + field.getWord() + "\", ");
		expr.writeJavaCode(mainblock, writer);
		writer.addCode(")");
	}


	@Override

	public void compileShiftUnsignedRightEq(MainLeekBlock mainblock, JavaWriter writer, AbstractExpression expr) {
		assert (object.isLeftValue() && !object.nullable());

		writer.addCode("field_ushr_eq(");
		object.writeJavaCode(mainblock, writer);
		writer.addCode(", \"" + field.getWord() + "\", ");
		expr.writeJavaCode(mainblock, writer);
		writer.addCode(")");
	}
}
