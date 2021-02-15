package leekscript.compiler.expression;

import leekscript.compiler.AnalyzeError;
import leekscript.compiler.IAWord;
import leekscript.compiler.JavaWriter;
import leekscript.compiler.WordCompiler;
import leekscript.compiler.AnalyzeError.AnalyzeErrorLevel;
import leekscript.compiler.bloc.MainLeekBlock;
import leekscript.compiler.exceptions.LeekCompilerException;
import leekscript.compiler.expression.LeekVariable.VariableType;

public class LeekObjectAccess extends AbstractExpression {

	private AbstractExpression object;
	private IAWord field;

	public LeekObjectAccess(AbstractExpression object, IAWord field) {
		this.object = object;
		this.field = field;
	}

	@Override
	public int getType() {
		return OBJECT_ACCESS;
	}

	@Override
	public String getString() {
		return "<method_call>";
	}

	public AbstractExpression getObject() {
		return object;
	}

	@Override
	public void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer) {
		object.writeJavaCode(mainblock, writer);
		writer.addCode(".getField(mUAI, \"" + field.getWord() + "\")");
	}

	@Override
	public boolean validExpression(WordCompiler compiler, MainLeekBlock mainblock) throws LeekExpressionException {

		return true;
	}

	@Override
	public boolean isLeftValue() {
		return true;
	}

	@Override
	public void analyze(WordCompiler compiler) {
		object.analyze(compiler);
		if (object instanceof LeekVariable) {
			var v = (LeekVariable) object;
			if (v.getName().equals("this")) {
				// this, check field exists in class
				var clazz = compiler.getCurrentClass();
				if (clazz != null && !clazz.hasMember(field)) {
					compiler.addError(new AnalyzeError(field, AnalyzeErrorLevel.ERROR, LeekCompilerException.CLASS_MEMBER_DOES_NOT_EXIST));
				}
			} else if (v.getVariableType() == VariableType.CLASS && v.getClassDeclaration() != null) {
				if (!v.getClassDeclaration().hasStaticMember(field)) {
					compiler.addError(new AnalyzeError(field, AnalyzeErrorLevel.ERROR, LeekCompilerException.CLASS_STATIC_MEMBER_DOES_NOT_EXIST, new String[] { v.getClassDeclaration().getName(), field.getWord() }));
				}
			}
		}
	}

	public String getField() {
		return field.getWord();
	}
}