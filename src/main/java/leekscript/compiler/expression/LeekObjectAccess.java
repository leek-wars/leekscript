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

		if (object instanceof LeekVariable) {
			var v = (LeekVariable) object;
			if (v.getName().equals("this")) {
				// this, check field exists in class
				var clazz = compiler.getCurrentClass();
				if (clazz != null && !clazz.hasMember(field.getWord())) {
					compiler.addError(new AnalyzeError(field, AnalyzeErrorLevel.ERROR, Error.CLASS_MEMBER_DOES_NOT_EXIST, new String[] { clazz.getName(), field.getWord() }));
				}
			} else if (v.getVariableType() == VariableType.CLASS && v.getClassDeclaration() != null) {
				if (!v.getClassDeclaration().hasStaticMember(field.getWord())) {
					compiler.addError(new AnalyzeError(field, AnalyzeErrorLevel.ERROR, Error.CLASS_STATIC_MEMBER_DOES_NOT_EXIST, new String[] { v.getClassDeclaration().getName(), field.getWord() }));
				}
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
		writer.addCode("getField(");
		object.writeJavaCode(mainblock, writer);
		writer.addCode(", \"" + field.getWord() + "\")");
	}

	@Override
	public void compileL(MainLeekBlock mainblock, JavaWriter writer) {
		assert (object.isLeftValue() && !object.nullable());

		writer.addCode("getField(");
		object.compileL(mainblock, writer);
		writer.addCode(", \"" + field.getWord() + "\")");
	}

	@Override
	public void compileSet(MainLeekBlock mainblock, JavaWriter writer, AbstractExpression expr) {
		assert (object.isLeftValue() && !object.nullable());

		writer.addCode("setField(");
		object.compileL(mainblock, writer);
		writer.addCode(", \"" + field.getWord() + "\", ");
		expr.writeJavaCode(mainblock, writer);
		writer.addCode(")");
	}

	@Override
	public void compileIncrement(MainLeekBlock mainblock, JavaWriter writer) {
		assert (object.isLeftValue() && !object.nullable());

		object.compileL(mainblock, writer);
		writer.addCode(".getFieldL(\"" + field.getWord() + "\").increment()");
	}

	@Override
	public void compileAddEq(MainLeekBlock mainblock, JavaWriter writer, AbstractExpression expr) {
		assert (object.isLeftValue() && !object.nullable());

		writer.addCode("field_add_eq(");
		object.compileL(mainblock, writer);
		writer.addCode(", \"" + field.getWord() + "\", ");
		expr.writeJavaCode(mainblock, writer);
		writer.addCode(")");
	}

	@Override

	public void compileSubEq(MainLeekBlock mainblock, JavaWriter writer, AbstractExpression expr) {
		assert (object.isLeftValue() && !object.nullable());

		writer.addCode("field_sub_eq(");
		object.compileL(mainblock, writer);
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

	public void compileBitOrEq(MainLeekBlock mainblock, JavaWriter writer, AbstractExpression expr) {
		assert (object.isLeftValue() && !object.nullable());

		writer.addCode("field_bor_eq(");
		object.writeJavaCode(mainblock, writer);
		writer.addCode(", \"" + field.getWord() + "\", ");
		expr.writeJavaCode(mainblock, writer);
		writer.addCode(")");
	}
}
