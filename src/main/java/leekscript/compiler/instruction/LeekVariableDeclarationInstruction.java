package leekscript.compiler.instruction;

import leekscript.compiler.AnalyzeError;
import leekscript.compiler.Token;
import leekscript.compiler.JavaWriter;
import leekscript.compiler.Location;
import leekscript.compiler.WordCompiler;
import leekscript.compiler.AnalyzeError.AnalyzeErrorLevel;
import leekscript.compiler.bloc.AbstractLeekBlock;
import leekscript.compiler.bloc.MainLeekBlock;
import leekscript.compiler.exceptions.LeekCompilerException;
import leekscript.compiler.expression.Expression;
import leekscript.compiler.expression.LeekAnonymousFunction;
import leekscript.compiler.expression.LeekExpression;
import leekscript.compiler.expression.LeekExpressionException;
import leekscript.compiler.expression.LeekType;
import leekscript.compiler.expression.LeekVariable;
import leekscript.compiler.expression.Operators;
import leekscript.compiler.expression.LeekVariable.VariableType;
import leekscript.common.Error;
import leekscript.common.FunctionType;
import leekscript.common.Type;
import leekscript.common.Type.CastType;

public class LeekVariableDeclarationInstruction extends LeekInstruction {

	private final Token token;
	private Expression mValue = null;
	private boolean captured = false;
	private AbstractLeekBlock function;
	private boolean box = false;
	private LeekVariable variable;
	private Type type;
	private LeekType leekType;

	public LeekVariableDeclarationInstruction(WordCompiler compiler, Token token, AbstractLeekBlock function, Type type) {
		this.token = token;
		this.function = function;
		this.box = compiler.getVersion() == 1;
		this.type = type;
	}

	public LeekVariableDeclarationInstruction(WordCompiler compiler, Token token, AbstractLeekBlock function, LeekType leekType) {
		this.token = token;
		this.function = function;
		this.box = compiler.getVersion() == 1;
		this.type = leekType == null ? Type.ANY : leekType.getType();
		this.leekType = leekType;
	}

	public void setValue(Expression value) {
		mValue = value;
	}

	public String getName() {
		return token.getWord();
	}

	public Token getToken() {
		return this.token;
	}

	public boolean isBox() {
		return this.box || this.captured;
	}

	public boolean isWrapper() {
		return this.box && this.captured;
	}

	@Override
	public String getCode() {
		String r = (this.leekType != null ? this.leekType.toString() : "var") + " " + token.getWord();
		if (mValue != null) {
			r += " = " + mValue.toString();
		}
		return r + ";";
	}

	@Override
	public void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer) {
		if (this.captured) {
			if (mValue != null && mValue.trim() instanceof LeekAnonymousFunction) {
				writer.addCode("final var u_" + token.getWord() + " = new Wrapper<" + type.getJavaName(mainblock.getVersion()) + ">(new Box(" + writer.getAIThis() + ", null)); u_" + token.getWord() + ".set(");
				mValue.writeJavaCode(mainblock, writer);
				writer.addLine(");", getLocation());
			} else if (mValue instanceof LeekExpression && ((LeekExpression) mValue).getOperator() == Operators.REFERENCE) {
				var e = ((LeekExpression) mValue).getExpression2();
				if (e.isLeftValue() && !(e instanceof LeekVariable v && v.getVariableType() == VariableType.GLOBAL)) {
					writer.addCode("final var u_" + token.getWord() + " = new Wrapper<" + type.getJavaName(mainblock.getVersion()) + ">(");
					e.compileL(mainblock, writer);
					writer.addCode(", " + e.getOperations() + ")");
				} else {
					writer.addCode("final var u_" + token.getWord() + " = new Wrapper<" + type.getJavaName(mainblock.getVersion()) + ">(new Box(" + writer.getAIThis() + ", ");
					e.writeJavaCode(mainblock, writer);
					writer.addCode("), " + e.getOperations() + ")");
				}
				writer.addLine(";", getLocation());
			} else if (mainblock.getWordCompiler().getVersion() <= 1) {
				writer.addCode("final var u_" + token.getWord() + " = new Wrapper<" + type.getJavaName(mainblock.getVersion()) + ">(new Box(" + writer.getAIThis() + ", ");
				if (mValue != null) mValue.compileL(mainblock, writer);
				else writer.addCode("null");
				writer.addCode(")");
				if (mValue != null && mValue.getOperations() > 0) {
					writer.addCode(", " + mValue.getOperations());
				}
				writer.addLine(");", getLocation());
			} else {
				writer.addCode("final var u_" + token.getWord() + " = new Wrapper<" + type.getJavaName(mainblock.getVersion()) + ">(new Box(" + writer.getAIThis() + ", ");
				if (mValue != null) mValue.writeJavaCode(mainblock, writer);
				else writer.addCode("null");
				writer.addCode(")");
				if (mValue != null && mValue.getOperations() > 0) {
					writer.addCode(", " + mValue.getOperations());
				}
				writer.addLine(");", getLocation());
			}
		} else {
			if (mainblock.getWordCompiler().getVersion() <= 1) {
				if (mValue instanceof LeekExpression && ((LeekExpression) mValue).getOperator() == Operators.REFERENCE) {
					var e = ((LeekExpression) mValue).getExpression2();
					if (e.isLeftValue()) {
						writer.addCode("var u_" + token.getWord() + " = new Box<" + type.getJavaName(mainblock.getVersion()) + ">(" + writer.getAIThis() + ", ");
						e.writeJavaCode(mainblock, writer);
						if (mValue.getOperations() > 0) {
							writer.addCode(", " + mValue.getOperations());
						}
						writer.addLine(")");
					} else {
						writer.addCode("var u_" + token.getWord() + " = new Box<" + type.getJavaName(mainblock.getVersion()) + ">(" + writer.getAIThis() + ", ");
						// e.writeJavaCode(mainblock, writer);
						mValue.compileL(mainblock, writer);
						if (mValue.getOperations() > 0) {
							writer.addCode(", " + mValue.getOperations());
						}
						writer.addLine(")");
					}
					writer.addLine(";", getLocation());
				} else {
					writer.addCode("var u_" + token.getWord() + " = new Box<" + type.getJavaName(mainblock.getVersion()) + ">(" + writer.getAIThis() + ", ");
					if (mValue != null) {
						if (mValue.isLeftValue()) {
							writer.compileClone(mainblock, mValue);
					 	} else {
							mValue.compileL(mainblock, writer);
						}
						if (mValue.getOperations() > 0) {
							writer.addCode(", " + mValue.getOperations());
						}
					} else {
						writer.addCode("null");
					}
					writer.addLine(");", getLocation());
				}
			} else {
				writer.addCode(this.type.getJavaPrimitiveName(mainblock.getVersion()));
				writer.addCode(" u_" + token.getWord() + " = ");
				if (writer.isOperationsEnabled()) {
					if (this.type != Type.ANY && !this.type.isPrimitive()) {
						writer.addCode("(" + this.type.getJavaPrimitiveName(mainblock.getVersion()) + ") ");
					}
					writer.addCode("ops(");
				}
				if (mValue != null) {
					writer.compileConvert(mainblock, 0, mValue, this.type);
				} else {
					writer.addCode(this.type.getDefaultValue(writer, mainblock.getVersion()));
				}
				if (writer.isOperationsEnabled()) {
					writer.addCode(", " + (1 + (mValue == null ? 0 : mValue.getOperations())) + ")");
				}
				writer.addLine(";", getLocation());
			}
		}
	}

	@Override
	public int getEndBlock() {
		return 0;
	}

	public AbstractLeekBlock getFunction() {
		return this.function;
	}

	public boolean isCaptured() {
		return captured;
	}

	@Override
	public boolean putCounterBefore() {
		return false;
	}

	@Override
	public void preAnalyze(WordCompiler compiler) throws LeekCompilerException {
		// System.out.println("VD preAnalyze " + token.getWord());
		this.function = compiler.getCurrentFunction();
		if (mValue != null && mValue.getType() instanceof FunctionType) {
			registerVariable(compiler, this.type);
			mValue.preAnalyze(compiler);
		} else {
			if (mValue != null) mValue.preAnalyze(compiler);
			registerVariable(compiler, this.type);
		}
	}

	@Override
	public void analyze(WordCompiler compiler) throws LeekCompilerException {
		if (mValue != null) {
			mValue.analyze(compiler);

			// Vérification du type de l'expression
			if (this.leekType != null) {
				var cast = this.leekType.getType().accepts(mValue.getType());
				if (cast.ordinal() > CastType.UPCAST.ordinal()) {

					if (cast == CastType.INCOMPATIBLE || compiler.getMainBlock().isStrict()) {
						var level = cast == CastType.INCOMPATIBLE ? AnalyzeErrorLevel.ERROR : AnalyzeErrorLevel.WARNING;
						var error = cast == CastType.INCOMPATIBLE ? Error.ASSIGNMENT_INCOMPATIBLE_TYPE : Error.DANGEROUS_CONVERSION_VARIABLE;

						compiler.addError(new AnalyzeError(mValue.getLocation(), level, error, new String[] {
							mValue.toString(),
							mValue.getType().toString(),
							this.token.getWord(),
							this.leekType.getType().toString(),
						}));
					}
				}
			}

			// Mode strict : la variable prend le type de l'expression si pas de type manuel
			if (compiler.getMainBlock().isStrict() && this.variable != null && this.leekType == null) {
				this.type = mValue.getType();
				this.variable.setType(mValue.getType());
			}
		}
	}

	private void registerVariable(WordCompiler compiler, Type type) throws LeekCompilerException {
		// Variables interdites
		if (compiler.getVersion() >= 2 && token.getWord().equals("this")) {
			compiler.addError(new AnalyzeError(token, AnalyzeErrorLevel.ERROR, Error.THIS_NOT_ALLOWED_HERE));
		} else {
			// Vérification déjà existante (on vérifie les globales et fonctions seulement en 1.1 car il y a un léger bug en 1.0 avec les includes)
			if ((compiler.getVersion() >= 2 && (compiler.getMainBlock().hasGlobal(token.getWord()) || compiler.getMainBlock().hasUserFunction(token.getWord(), true))) || compiler.getCurrentBlock().hasVariable(token.getWord())) {
				compiler.addError(new AnalyzeError(token, AnalyzeErrorLevel.ERROR, Error.VARIABLE_NAME_UNAVAILABLE, new String[] {
					token.getWord()
				}));
			} else {
				// On ajoute la variable
				this.variable = new LeekVariable(token, VariableType.LOCAL, type, this);
				compiler.getCurrentBlock().addVariable(this.variable);
			}
		}
	}

	public void setCaptured() {
		this.captured = true;
	}

	public void setFunction(AbstractLeekBlock function) {
		this.function = function;
	}

	@Override
	public int getOperations() {
		return 0;
	}

	@Override
	public Location getLocation() {
		if (this.leekType != null) {
			return new Location(this.leekType.getLocation(), token.getLocation());
		}
		return token.getLocation();
	}

	@Override
	public int getNature() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Type getType() {
		return type;
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean validExpression(WordCompiler compiler, MainLeekBlock mainblock) throws LeekExpressionException {
		// TODO Auto-generated method stub
		return false;
	}

	public LeekVariable getVariable() {
		return this.variable;
	}
}
