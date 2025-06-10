package leekscript.compiler.instruction;

import leekscript.compiler.Token;
import leekscript.common.Type;
import leekscript.common.Type.CastType;
import leekscript.compiler.AnalyzeError;
import leekscript.compiler.JavaWriter;
import leekscript.compiler.Location;
import leekscript.compiler.WordCompiler;
import leekscript.compiler.AnalyzeError.AnalyzeErrorLevel;
import leekscript.compiler.bloc.MainLeekBlock;
import leekscript.compiler.exceptions.LeekCompilerException;
import leekscript.compiler.expression.Expression;
import leekscript.compiler.expression.LeekExpressionException;
import leekscript.compiler.expression.LeekType;
import leekscript.compiler.expression.LeekVariable;
import leekscript.compiler.expression.LeekVariable.VariableType;
import leekscript.common.Error;

public class LeekGlobalDeclarationInstruction extends LeekInstruction {

	private final Token token;
	private final Token variableToken;
	private Expression mValue = null;
	private Type type;
	private LeekType leekType;
	private LeekVariable variable;

	public LeekGlobalDeclarationInstruction(Token token, Token variableToken, LeekType leekType) {
		this.token = token;
		this.variableToken = variableToken;
		this.leekType = leekType;
	}

	public void setValue(Expression value) {
		mValue = value;
	}

	public String getName() {
		return variableToken.getWord();
	}

	@Override
	public String getCode() {
		if (mValue != null) {
			return "global " + variableToken.getWord() + " = " + mValue.toString() + ";";
		} else {
			return "global " + variableToken.getWord() + ";";
		}
	}

	@Override
	public void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer) {
		writer.addCode("if (!g_init_" + variableToken.getWord() + ") { ");
		if (mainblock.getWordCompiler().getVersion() >= 2) {
		if (mValue != null && mValue.getOperations() > 0) writer.addCode("ops(");
			writer.addCode("g_" + variableToken.getWord() + " = ");
			if (mValue != null) {
				if (variable.getType() != Type.ANY) {
					writer.compileConvert(mainblock, 0, mValue, variable.getType());
				} else {
					mValue.writeJavaCode(mainblock, writer);
				}
				if (mValue.getOperations() > 0) writer.addCode(", " + mValue.getOperations() + ")");
			} else {
				writer.addCode(this.variable.getType().getDefaultValue(writer, mainblock.getVersion()));
			}
		} else {
			writer.addCode("g_" + variableToken.getWord() + " = new Box<" + variable.getType().getJavaName(mainblock.getVersion()) + ">(" + writer.getAIThis() + ", ");
			if (mValue != null) mValue.compileL(mainblock, writer);
			else writer.addCode("null");
			writer.addCode(", " + (mValue != null ? mValue.getOperations() : 0) + ")");
		}
		writer.addCode("; g_init_" + variableToken.getWord() + " = true;");
		if (mainblock.getWordCompiler().getVersion() >= 2) writer.addCode(" ops(1);");
		writer.addLine(" }", getLocation());
	}

	@Override
	public int getEndBlock() {
		return 0;
	}

	@Override
	public boolean putCounterBefore() {
		return false;
	}

	public void declare(WordCompiler compiler) {
		// On ajoute la variable
		this.variable = new LeekVariable(compiler, variableToken, VariableType.GLOBAL, leekType == null ? Type.ANY : leekType.getType());
		this.type = this.variable.getType();
		compiler.getCurrentBlock().addVariable(this.variable);
	}

	@Override
	public void preAnalyze(WordCompiler compiler) throws LeekCompilerException {
		// // On ajoute la variable
		// this.variable = new LeekVariable(compiler, variableToken, VariableType.GLOBAL, leekType == null ? Type.ANY : leekType.getType());
		// this.type = this.variable.getType();
		// compiler.getCurrentBlock().addVariable(this.variable);

		if (mValue != null) {
			mValue.preAnalyze(compiler);
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

			// LS5+ : la variable prend le type de l'expression si pas de type manuel
			if (compiler.getMainBlock().isStrict() && this.variable != null && this.leekType == null) {
				this.type = mValue.getType();
				this.variable.setType(mValue.getType());
			}
		}
	}

	@Override
	public int getOperations() {
		return 0;
	}

	@Override
	public Location getLocation() {
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
}
