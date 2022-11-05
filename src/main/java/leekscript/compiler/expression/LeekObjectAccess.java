package leekscript.compiler.expression;

import leekscript.compiler.AnalyzeError;
import leekscript.compiler.Hover;
import leekscript.compiler.Token;
import leekscript.compiler.JavaWriter;
import leekscript.compiler.Location;
import leekscript.compiler.WordCompiler;
import leekscript.compiler.AnalyzeError.AnalyzeErrorLevel;
import leekscript.compiler.bloc.MainLeekBlock;
import leekscript.compiler.expression.LeekVariable.VariableType;
import leekscript.common.Error;
import leekscript.common.Type;

public class LeekObjectAccess extends Expression {

	private Expression object;
	private Token field;
	private Type type = Type.ANY;
	private boolean isFinal = false;
	private boolean isLeftValue = true;
	private LeekVariable variable;

	public LeekObjectAccess(Expression object, Token dot, Token field) {
		this.object = object;
		this.field = field;
		dot.setExpression(this);
		field.setExpression(this);
	}

	@Override
	public int getNature() {
		return OBJECT_ACCESS;
	}

	@Override
	public Type getType() {
		return type;
	}

	@Override
	public String toString() {
		return object.toString() + "." + field.getWord();
	}

	public Expression getObject() {
		return object;
	}

	@Override
	public boolean validExpression(WordCompiler compiler, MainLeekBlock mainblock) throws LeekExpressionException {
		return object.validExpression(compiler, mainblock);
	}

	@Override
	public boolean isLeftValue() {
		return isLeftValue;
	}

	@Override
	public boolean isFinal() {
		return isFinal;
	}

	@Override
	public boolean nullable() {
		return object.nullable();
	}


	@Override
	public void preAnalyze(WordCompiler compiler) {

		object.preAnalyze(compiler);

		if (field.getWord().equals("class")) {
			return; // .class is available everywhere
		}

		if (object instanceof LeekVariable) {
			var v = (LeekVariable) object;
			if (v.getName().equals("this")) {
				// this, check field exists in class
				var clazz = compiler.getCurrentClass();
				if (clazz != null) {
					this.variable = clazz.getMember(field.getWord());
					if (this.variable == null) {
						compiler.addError(new AnalyzeError(field, AnalyzeErrorLevel.ERROR, Error.CLASS_MEMBER_DOES_NOT_EXIST, new String[] { clazz.getName(), field.getWord() }));
					} else {
						this.isFinal = this.variable.isFinal();
						this.isLeftValue = this.variable.isLeftValue();
					}
				}
			} else if (v.getVariableType() == VariableType.CLASS || v.getVariableType() == VariableType.THIS_CLASS) {
				var clazz = v.getVariableType() == VariableType.CLASS ? v.getClassDeclaration() : compiler.getCurrentClass();

				this.variable = clazz.getStaticMember(field.getWord());
				if (this.variable != null) {
					type = this.variable.getType();
					this.isFinal = this.variable.isFinal();
					return; // OK
				}
				if (clazz.hasMethod(field.getWord())) {
					this.type = Type.FUNCTION;
					this.isLeftValue = false;
					return; // OK
				}
				compiler.addError(new AnalyzeError(field, AnalyzeErrorLevel.ERROR, Error.CLASS_STATIC_MEMBER_DOES_NOT_EXIST, new String[] { clazz.getName(), field.getWord() }));
			}
		}
	}

	@Override
	public void analyze(WordCompiler compiler) {
		// System.out.println("oa " + getString());
		object.analyze(compiler);
		operations = 1 + object.operations;

		if (field.getWord().equals("class")) {
			type = Type.CLASS;
			return; // .class is available everywhere
		}
		if (object instanceof LeekVariable) {
			var v = (LeekVariable) object;
			if (v.getVariableType() == VariableType.CLASS || v.getVariableType() == VariableType.THIS_CLASS) {
				operations -= 1;
			}
		}
	}

	public String getField() {
		return field.getWord();
	}

	public Token getFieldToken() {
		return field;
	}

	@Override
	public void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer) {
		if (mainblock.getWordCompiler().getVersion() >= 3 && field.getWord().equals("class")) {
			writer.addCode("getClass(");
			object.writeJavaCode(mainblock, writer);
			writer.addCode(")");
		} else {
			if (type != Type.ANY) {
				writer.addCode("((" + type.getJavaName(mainblock.getVersion()) + ") ");
			}
			writer.addCode("getField(");
			object.writeJavaCode(mainblock, writer);
			writer.addCode(", \"" + field.getWord() + "\", " + mainblock.getWordCompiler().getCurrentClassVariable() + ")");
			if (type != Type.ANY) {
				writer.addCode(")");
			}
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
			writer.addCode(", \"" + field.getWord() + "\", " + mainblock.getWordCompiler().getCurrentClassVariable() + ")");
		}
	}

	@Override
	public void compileSet(MainLeekBlock mainblock, JavaWriter writer, Expression expr) {
		assert (object.isLeftValue() && !object.nullable());

		if (object instanceof LeekVariable && ((LeekVariable) object).getVariableType() == VariableType.THIS && writer.isInConstructor()) {
			writer.addCode("initField(");
			object.writeJavaCode(mainblock, writer);
			writer.addCode(", \"" + field.getWord() + "\", ");
			expr.writeJavaCode(mainblock, writer);
			writer.addCode(")");
		} else {
			writer.addCode("setField(");
			object.writeJavaCode(mainblock, writer);
			writer.addCode(", \"" + field.getWord() + "\", ");
			expr.writeJavaCode(mainblock, writer);
			writer.addCode(")");
		}
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
	public void compileAddEq(MainLeekBlock mainblock, JavaWriter writer, Expression expr) {
		assert (object.isLeftValue() && !object.nullable());

		writer.addCode("field_add_eq(");
		object.writeJavaCode(mainblock, writer);
		writer.addCode(", \"" + field.getWord() + "\", ");
		expr.writeJavaCode(mainblock, writer);
		writer.addCode(")");
	}

	@Override

	public void compileSubEq(MainLeekBlock mainblock, JavaWriter writer, Expression expr) {
		assert (object.isLeftValue() && !object.nullable());

		writer.addCode("field_sub_eq(");
		object.writeJavaCode(mainblock, writer);
		writer.addCode(", \"" + field.getWord() + "\", ");
		expr.writeJavaCode(mainblock, writer);
		writer.addCode(")");
	}

	@Override

	public void compileMulEq(MainLeekBlock mainblock, JavaWriter writer, Expression expr) {
		assert (object.isLeftValue() && !object.nullable());

		writer.addCode("field_mul_eq(");
		object.writeJavaCode(mainblock, writer);
		writer.addCode(", \"" + field.getWord() + "\", ");
		expr.writeJavaCode(mainblock, writer);
		writer.addCode(")");
	}

	@Override

	public void compilePowEq(MainLeekBlock mainblock, JavaWriter writer, Expression expr) {
		assert (object.isLeftValue() && !object.nullable());

		writer.addCode("field_pow_eq(");
		object.writeJavaCode(mainblock, writer);
		writer.addCode(", \"" + field.getWord() + "\", ");
		expr.writeJavaCode(mainblock, writer);
		writer.addCode(")");
	}

	@Override

	public void compileDivEq(MainLeekBlock mainblock, JavaWriter writer, Expression expr) {
		assert (object.isLeftValue() && !object.nullable());

		writer.addCode("field_div_eq(");
		object.writeJavaCode(mainblock, writer);
		writer.addCode(", \"" + field.getWord() + "\", ");
		expr.writeJavaCode(mainblock, writer);
		writer.addCode(")");
	}

	@Override

	public void compileIntDivEq(MainLeekBlock mainblock, JavaWriter writer, Expression expr) {
		assert (object.isLeftValue() && !object.nullable());

		writer.addCode("field_intdiv_eq(");
		object.writeJavaCode(mainblock, writer);
		writer.addCode(", \"" + field.getWord() + "\", ");
		expr.writeJavaCode(mainblock, writer);
		writer.addCode(")");
	}

	@Override

	public void compileModEq(MainLeekBlock mainblock, JavaWriter writer, Expression expr) {
		assert (object.isLeftValue() && !object.nullable());

		writer.addCode("field_mod_eq(");
		object.writeJavaCode(mainblock, writer);
		writer.addCode(", \"" + field.getWord() + "\", ");
		expr.writeJavaCode(mainblock, writer);
		writer.addCode(")");
	}

	@Override

	public void compileBitOrEq(MainLeekBlock mainblock, JavaWriter writer, Expression expr) {
		assert (object.isLeftValue() && !object.nullable());

		writer.addCode("field_bor_eq(");
		object.writeJavaCode(mainblock, writer);
		writer.addCode(", \"" + field.getWord() + "\", ");
		expr.writeJavaCode(mainblock, writer);
		writer.addCode(")");
	}

	@Override

	public void compileBitAndEq(MainLeekBlock mainblock, JavaWriter writer, Expression expr) {
		assert (object.isLeftValue() && !object.nullable());

		writer.addCode("field_band_eq(");
		object.writeJavaCode(mainblock, writer);
		writer.addCode(", \"" + field.getWord() + "\", ");
		expr.writeJavaCode(mainblock, writer);
		writer.addCode(")");
	}

	@Override

	public void compileBitXorEq(MainLeekBlock mainblock, JavaWriter writer, Expression expr) {
		assert (object.isLeftValue() && !object.nullable());

		writer.addCode("field_bxor_eq(");
		object.writeJavaCode(mainblock, writer);
		writer.addCode(", \"" + field.getWord() + "\", ");
		expr.writeJavaCode(mainblock, writer);
		writer.addCode(")");
	}

	@Override

	public void compileShiftLeftEq(MainLeekBlock mainblock, JavaWriter writer, Expression expr) {
		assert (object.isLeftValue() && !object.nullable());

		writer.addCode("field_shl_eq(");
		object.writeJavaCode(mainblock, writer);
		writer.addCode(", \"" + field.getWord() + "\", ");
		expr.writeJavaCode(mainblock, writer);
		writer.addCode(")");
	}


	@Override

	public void compileShiftRightEq(MainLeekBlock mainblock, JavaWriter writer, Expression expr) {
		assert (object.isLeftValue() && !object.nullable());

		writer.addCode("field_shr_eq(");
		object.writeJavaCode(mainblock, writer);
		writer.addCode(", \"" + field.getWord() + "\", ");
		expr.writeJavaCode(mainblock, writer);
		writer.addCode(")");
	}


	@Override

	public void compileShiftUnsignedRightEq(MainLeekBlock mainblock, JavaWriter writer, Expression expr) {
		assert (object.isLeftValue() && !object.nullable());

		writer.addCode("field_ushr_eq(");
		object.writeJavaCode(mainblock, writer);
		writer.addCode(", \"" + field.getWord() + "\", ");
		expr.writeJavaCode(mainblock, writer);
		writer.addCode(")");
	}

	@Override
	public Location getLocation() {
		return new Location(object.getLocation(), field.getLocation());
	}

	@Override
	public Hover hover(Token token) {
		if (object instanceof LeekVariable) {
			var v = (LeekVariable) object;
			if (v.getVariableType() == VariableType.CLASS) {
				var clazz = v.getClassDeclaration();

				var member = clazz.getMember(field.getWord());
				if (member != null) {
					return new Hover(member.getType(), getLocation(), member.getLocation());
				}
				var staticMember = clazz.getStaticMember(field.getWord());
				if (staticMember != null) {
					return new Hover(staticMember.getType(), getLocation(), staticMember.getLocation());
				}
			}
		}
		return new Hover(getType(), getLocation());
	}

	public LeekVariable getVariable() {
		return this.variable;
	}
}
