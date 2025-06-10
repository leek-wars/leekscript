package leekscript.compiler.expression;

import leekscript.compiler.AnalyzeError;
import leekscript.compiler.Complete;
import leekscript.compiler.Hover;
import leekscript.compiler.Token;
import leekscript.compiler.JavaWriter;
import leekscript.compiler.Location;
import leekscript.compiler.WordCompiler;
import leekscript.compiler.AnalyzeError.AnalyzeErrorLevel;
import leekscript.compiler.bloc.MainLeekBlock;
import leekscript.compiler.exceptions.LeekCompilerException;
import leekscript.compiler.expression.LeekVariable.VariableType;
import leekscript.common.ClassType;
import leekscript.common.ClassValueType;
import leekscript.common.Error;
import leekscript.common.FunctionType;
import leekscript.common.Type;

public class LeekObjectAccess extends Expression {

	private Expression object;
	private Token dot;
	private Token field;
	private Type type = Type.ANY;
	private boolean isFinal = false;
	private boolean isLeftValue = true;
	private LeekVariable variable;

	public LeekObjectAccess(Expression object, Token dot, Token field) {
		this.object = object;
		this.field = field;
		this.dot = dot;
		dot.setExpression(this);
		if (field != null) {
			field.setExpression(this);
		}
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
		return object.toString() + "." + (field != null ? field.getWord() : "");
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
	public void preAnalyze(WordCompiler compiler) throws LeekCompilerException {

		object.preAnalyze(compiler);

		if (field != null && field.getWord().equals("class")) {
			return; // .class is available everywhere
		}
	}

	@Override
	public void analyze(WordCompiler compiler) throws LeekCompilerException {

		// System.out.println("oa " + getString());
		object.analyze(compiler);
		operations = 1 + object.operations;

		// Expression incomplète
		if (field == null) return;

		if (field.getWord().equals("class")) {
			var clazz = object.getType().getClassDeclaration();
			type = clazz == null ? Type.CLASS : clazz.getClassValueType();
			return; // .class is available everywhere
		}
		if (object instanceof LeekVariable) {
			var v = (LeekVariable) object;
			if (v.getVariableType() == VariableType.CLASS || v.getVariableType() == VariableType.THIS_CLASS) {
				operations -= 1;
			}
		}

		boolean isSuper = object instanceof LeekVariable v && v.getVariableType() == VariableType.SUPER;
		if (object.getType() instanceof ClassType || isSuper) {
			// this, check field exists in class
			var clazz = object.getType() instanceof ClassType ct ? ct.getClassDeclaration() : compiler.getCurrentClass().getParent();
			if (clazz != null) {
				this.variable = clazz.getMember(field.getWord());
				if (this.variable == null) {
					// var level = compiler.getMainBlock().isStrict() ? AnalyzeErrorLevel.ERROR : AnalyzeErrorLevel.WARNING;
					// compiler.addError(new AnalyzeError(field, level, Error.CLASS_MEMBER_DOES_NOT_EXIST, new String[] {
					// 	field.getWord(),
					// 	object.toString(),
					// 	clazz.getName()
					// }));
				} else {
					var error = clazz.canAccessField(field.getWord(), compiler.getCurrentClass());
					if (error != null) {
						compiler.addError(new AnalyzeError(this.getLocation(), AnalyzeErrorLevel.ERROR, error, new String[] { clazz.getName(), field.getWord() }));
					}
					this.isFinal = this.variable.isFinal();
					this.isLeftValue = this.variable.isLeftValue();
					this.type = this.variable.getType();
				}
			}
		} else if (object.getType() instanceof ClassValueType cvt) {

			var clazz = cvt.getClassDeclaration();

			this.variable = clazz != null ? clazz.getStaticMember(field.getWord()) : null;
			if (this.variable != null) {
				this.type = this.variable.getType();
				this.isFinal = this.variable.isFinal();
			} else {
				this.variable = compiler.getMainBlock().getUserClass("Class").getMember(field.getWord());
				if (this.variable != null) {
					this.type = this.variable.getType();
					this.isFinal = this.variable.isFinal();
				} else {
					var m = clazz != null ? clazz.getMethod(field.getWord()) : null;
					if (m != null) {
						this.isLeftValue = false;
						for (var mm : m.entrySet()) {
							this.type = mm.getValue().block.getType();
							break;
						}
					} else {
						compiler.addError(new AnalyzeError(field, AnalyzeErrorLevel.ERROR, Error.CLASS_STATIC_MEMBER_DOES_NOT_EXIST, new String[] {
							clazz == null ? "?" : clazz.getName(),
							field.getWord()
						}));
					}
				}
			}
		}

		// get type of member
		var memberType = isSuper && object.getType().getClassDeclaration() != null ? object.getType().getClassDeclaration().getType().member(field.getWord()) : object.getType().member(field.getWord());
		if (memberType.isWarning()) {

			if (memberType == Type.ERROR || compiler.getMainBlock().isStrict()) {
				var level = memberType == Type.ERROR && compiler.getMainBlock().isStrict() ? AnalyzeErrorLevel.ERROR : AnalyzeErrorLevel.WARNING;
				var error = memberType == Type.ERROR ? Error.CLASS_MEMBER_DOES_NOT_EXIST : Error.FIELD_MAY_NOT_EXIST;

				compiler.addError(new AnalyzeError(field, level, error, new String[] {
					field.getWord(),
					object.toString(),
					object.getType().toString(),
				}));
			}
			memberType = Type.replaceErrors(memberType);
		}
		this.type = memberType;
	}

	public String getField() {
		return field == null ? "" : field.getWord();
	}

	public Token getFieldToken() {
		return field;
	}

	@Override
	public void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer) {
		if (mainblock.getWordCompiler().getVersion() >= 2 && field.getWord().equals("class")) {
			writer.addCode("classOf(");
			object.writeJavaCode(mainblock, writer);
			writer.addCode(")");
		} else {
			if (this.variable != null && this.variable.getVariableType() == VariableType.METHOD && mainblock.getWordCompiler().getCurrentClassVariable() != null) {
				writer.addCode(mainblock.getWordCompiler().getCurrentClassVariable() + ".getField(\"" + field.getWord() + "\")");
			} else if (object instanceof LeekVariable && ((LeekVariable) object).getVariableType() == VariableType.THIS) {
				writer.addCode(field.getWord());
			} else if (object.getType() instanceof ClassType && !(type instanceof FunctionType)) { // TODO : mieux détecter les méthodes
				object.writeJavaCode(mainblock, writer);
				writer.addCode(".");
				writer.addCode(field.getWord());
			} else {
				if (type != Type.ANY) {
					writer.addCode("(");
					if (type.isPrimitive()) {
						writer.addCode("(" + type.getJavaPrimitiveName(mainblock.getVersion()) + ") ");
					}
					writer.addCode("(" + type.getJavaName(mainblock.getVersion()) + ") (");
				}
				writer.addCode("getField(");
				object.writeJavaCode(mainblock, writer);
				writer.addCode(", \"" + field.getWord() + "\", " + mainblock.getWordCompiler().getCurrentClassVariable() + ")");
				if (type != Type.ANY) {
					writer.addCode("))");
				}
			}
		}
	}

	@Override
	public void compileL(MainLeekBlock mainblock, JavaWriter writer) {
		// assert (object.isLeftValue() && !object.nullable());

		if (mainblock.getWordCompiler().getVersion() >= 2 && field.getWord().equals("class")) {
			writer.addCode("classOf(");
			object.writeJavaCode(mainblock, writer);
			writer.addCode(")");
		} else {
			if (object instanceof LeekVariable && ((LeekVariable) object).getVariableType() == VariableType.THIS) {
				writer.addCode(field.getWord());
			} else if (object.getType() instanceof ClassType) {
				object.writeJavaCode(mainblock, writer);
				writer.addCode(".");
				writer.addCode(field.getWord());
			} else {
				writer.addCode("getField(");
				object.writeJavaCode(mainblock, writer);
				writer.addCode(", \"" + field.getWord() + "\", " + mainblock.getWordCompiler().getCurrentClassVariable() + ")");
			}
		}
	}

	@Override
	public void compileSet(MainLeekBlock mainblock, JavaWriter writer, Expression expr) {
		// assert (object.isLeftValue() && !object.nullable());

		if (object instanceof LeekVariable && ((LeekVariable) object).getVariableType() == VariableType.THIS) {
			writer.addCode(field.getWord() + " = ");
			writer.compileConvert(mainblock, 0, expr, type);
		} else if (object.getType() instanceof ClassType) {
			object.writeJavaCode(mainblock, writer);
			writer.addCode("." + field.getWord() + " = ");
			writer.compileConvert(mainblock, 0, expr, type);
		} else {
			writer.addCode("setField(");
			object.writeJavaCode(mainblock, writer);
			writer.addCode(", \"" + field.getWord() + "\", ");
			writer.compileConvert(mainblock, 0, expr, this.type);
			// expr.writeJavaCode(mainblock, writer);
			writer.addCode(", " + mainblock.getWordCompiler().getCurrentClassVariable() + ")");
		}
	}

	@Override
	public void compileIncrement(MainLeekBlock mainblock, JavaWriter writer) {
		// assert (object.isLeftValue() && !object.nullable());

		if (this.type != Type.ANY) {
			writer.addCode("(" + this.type.getJavaName(mainblock.getVersion()) + ") ");
		}
		writer.addCode("field_inc(");
		object.writeJavaCode(mainblock, writer);
		writer.addCode(", \"" + field.getWord() + "\", " + mainblock.getWordCompiler().getCurrentClassVariable() + ")");
	}

	@Override
	public void compilePreIncrement(MainLeekBlock mainblock, JavaWriter writer) {
		// assert (object.isLeftValue() && !object.nullable());

		if (this.type != Type.ANY) {
			writer.addCode("(" + this.type.getJavaName(mainblock.getVersion()) + ") ");
		}
		writer.addCode("field_pre_inc(");
		object.writeJavaCode(mainblock, writer);
		writer.addCode(", \"" + field.getWord() + "\", " + mainblock.getWordCompiler().getCurrentClassVariable() + ")");
	}

	@Override
	public void compileDecrement(MainLeekBlock mainblock, JavaWriter writer) {
		// assert (object.isLeftValue() && !object.nullable());

		if (this.type != Type.ANY) {
			writer.addCode("(" + this.type.getJavaName(mainblock.getVersion()) + ") ");
		}
		writer.addCode("field_dec(");
		object.writeJavaCode(mainblock, writer);
		writer.addCode(", \"" + field.getWord() + "\", " + mainblock.getWordCompiler().getCurrentClassVariable() + ")");
	}

	@Override
	public void compilePreDecrement(MainLeekBlock mainblock, JavaWriter writer) {
		// assert (object.isLeftValue() && !object.nullable());

		if (this.type != Type.ANY) {
			writer.addCode("(" + this.type.getJavaName(mainblock.getVersion()) + ") ");
		}
		writer.addCode("field_pre_dec(");
		object.writeJavaCode(mainblock, writer);
		writer.addCode(", \"" + field.getWord() + "\", " + mainblock.getWordCompiler().getCurrentClassVariable() + ")");
	}

	@Override
	public void compileAddEq(MainLeekBlock mainblock, JavaWriter writer, Expression expr, Type t) {
		compileEq(mainblock, writer, expr, "add");
	}

	@Override
	public void compileSubEq(MainLeekBlock mainblock, JavaWriter writer, Expression expr) {
		compileEq(mainblock, writer, expr, "sub");
	}

	@Override
	public void compileMulEq(MainLeekBlock mainblock, JavaWriter writer, Expression expr, Type t) {
		compileEq(mainblock, writer, expr, "mul");
	}

	@Override
	public void compilePowEq(MainLeekBlock mainblock, JavaWriter writer, Expression expr, Type t) {
		compileEq(mainblock, writer, expr, "pow");
	}

	@Override
	public void compileDivEq(MainLeekBlock mainblock, JavaWriter writer, Expression expr) {
		compileEq(mainblock, writer, expr, "div");
	}

	@Override
	public void compileIntDivEq(MainLeekBlock mainblock, JavaWriter writer, Expression expr) {
		compileEq(mainblock, writer, expr, "intdiv");
	}

	@Override
	public void compileModEq(MainLeekBlock mainblock, JavaWriter writer, Expression expr) {
		compileEq(mainblock, writer, expr, "mod");
	}

	@Override
	public void compileBitOrEq(MainLeekBlock mainblock, JavaWriter writer, Expression expr) {
		compileEq(mainblock, writer, expr, "bor");
	}

	@Override
	public void compileBitAndEq(MainLeekBlock mainblock, JavaWriter writer, Expression expr) {
		compileEq(mainblock, writer, expr, "band");
	}

	@Override
	public void compileBitXorEq(MainLeekBlock mainblock, JavaWriter writer, Expression expr) {
		compileEq(mainblock, writer, expr, "bxor");
	}

	@Override
	public void compileShiftLeftEq(MainLeekBlock mainblock, JavaWriter writer, Expression expr) {
		compileEq(mainblock, writer, expr, "shl");
	}

	@Override
	public void compileShiftRightEq(MainLeekBlock mainblock, JavaWriter writer, Expression expr) {
		compileEq(mainblock, writer, expr, "shr");
	}

	@Override
	public void compileShiftUnsignedRightEq(MainLeekBlock mainblock, JavaWriter writer, Expression expr) {
		compileEq(mainblock, writer, expr, "ushr");
	}
	
	private void compileEq(MainLeekBlock mainblock, JavaWriter writer, Expression expr, String function) {
		// assert (object.isLeftValue() && !object.nullable());
		writer.addCode("field_" + function + "_eq(");
		object.writeJavaCode(mainblock, writer);
		writer.addCode(", \"" + field.getWord() + "\", ");

		if (variable != null && variable.getType().isPrimitiveNumber() && !expr.getType().isPrimitiveNumber()) {
			// need cast
			if (type == Type.INT) {
				writer.addCode("longint(");
				expr.writeJavaCode(mainblock, writer);
				writer.addCode(")");
			} else if (type == Type.REAL) {
				writer.addCode("real(");
				expr.writeJavaCode(mainblock, writer);
				writer.addCode(")");						
			}
		} else {
			expr.writeJavaCode(mainblock, writer);
		}
		writer.addCode(", " + mainblock.getWordCompiler().getCurrentClassVariable() + ")");
	}

	@Override
	public Location getLocation() {
		if (field == null) {
			return new Location(object.getLocation(), dot.getLocation());
		}
		return new Location(object.getLocation(), field.getLocation());
	}

	@Override
	public Hover hover(Token token) {
		if (object instanceof LeekVariable) {
			var v = (LeekVariable) object;
			if (v.getVariableType() == VariableType.CLASS) {
				var clazz = v.getClassDeclaration();

				var staticMember = clazz.getStaticMember(field.getWord());
				if (staticMember != null) {
					return new Hover(staticMember.getType(), getLocation(), staticMember.getLocation());
				}
			}
		}
		var clazz = object.getType().getClassDeclaration();
		if (clazz != null && field != null) {
			var member = clazz.getMember(field.getWord());
			if (member != null) {
				return new Hover(member.getType(), getLocation(), member.getLocation());
			}
			member = clazz.getStaticMember(field.getWord());
			if (member != null) {
				return new Hover(member.getType(), getLocation(), member.getLocation());
			}
		}
		return new Hover(getType(), getLocation());
	}


	@Override
	public Complete complete(Token token) {
		// System.out.println("OA complete " + this.object.getType());
		return this.object.getType().complete();
	}

	public LeekVariable getVariable() {
		return this.variable;
	}

	public Token getDot() {
		return this.dot;
	}

	public Token getLastToken() {
		if (this.field != null) {
			return this.field;
		}
		return this.dot;
	}
}
