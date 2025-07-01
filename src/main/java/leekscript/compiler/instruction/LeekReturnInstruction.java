package leekscript.compiler.instruction;

import leekscript.common.Type;
import leekscript.common.Type.CastType;
import leekscript.compiler.Token;
import leekscript.compiler.AnalyzeError;
import leekscript.compiler.JavaWriter;
import leekscript.compiler.Location;
import leekscript.compiler.WordCompiler;
import leekscript.compiler.AnalyzeError.AnalyzeErrorLevel;
import leekscript.compiler.bloc.MainLeekBlock;
import leekscript.compiler.exceptions.LeekCompilerException;
import leekscript.compiler.expression.Expression;
import leekscript.compiler.expression.LeekExpressionException;
import leekscript.common.Error;

public class LeekReturnInstruction extends LeekInstruction {

	private final Token token;
	private final Expression expression;
	private final boolean optional;
	private Type returnType;

	public LeekReturnInstruction(Token token, Expression exp, boolean optional) {
		this.token = token;
		this.expression = exp;
		this.optional = optional;
	}

	@Override
	public String getCode() {
		return "return" + (optional ? " ?" : "") + (expression == null ? "" : " " + expression.toString()) + ";";
	}


	@Override
	public void preAnalyze(WordCompiler compiler) throws LeekCompilerException {
		if (expression != null) {
			expression.preAnalyze(compiler);
		}
	}

	@Override
	public void analyze(WordCompiler compiler) throws LeekCompilerException {
		this.returnType = Type.ANY;
		if (expression != null) {
			expression.analyze(compiler);
		}

		// Vérification du type de retour
		var functionType = compiler.getCurrentFunction().getType();
		if (functionType != null) {
			this.returnType = functionType.returnType();
		}

		var actualType = expression == null ? Type.VOID : expression.getType();

		var cast = returnType.accepts(actualType);
		if (cast.ordinal() > CastType.UPCAST.ordinal()) {

			if (cast == CastType.INCOMPATIBLE || compiler.getMainBlock().isStrict()) {
				var level = compiler.getMainBlock().isStrict() && cast == CastType.INCOMPATIBLE ? AnalyzeErrorLevel.ERROR : AnalyzeErrorLevel.WARNING;
				var error = cast == CastType.INCOMPATIBLE ? Error.INCOMPATIBLE_TYPE : Error.DANGEROUS_CONVERSION;

				compiler.addError(new AnalyzeError(getLocation(), level, error, new String[] {
					actualType.toString(),
					compiler.getCurrentFunction().getType().returnType().toString()
				}));
			}
		}
	}

	@Override
	public void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer) {
		if (writer.currentBlock == mainblock) {
			mainblock.writeBeforeReturn(writer);
		}
		if (expression == null) {
			writer.addPosition(token);
			writer.addCode("return null;");
		} else {
			var finalExpression = expression.trim();
			if (finalExpression.getOperations() > 0) {
				writer.addCode("ops(" + finalExpression.getOperations() + "); ");
			}
			if (optional) {
				String r = "r" + mainblock.getCount();
				writer.addCode(returnType.getJavaName(mainblock.getVersion()) + " " + r + " = ");
				writer.compileConvert(mainblock, 0, finalExpression, returnType);
				writer.addLine("; if (bool(" + r + ")) return " + r + ";", getLocation());
			} else {
				writer.addCode("return ");
				if (mainblock.getWordCompiler().getVersion() == 1) {
					finalExpression.compileL(mainblock, writer);
				} else {
					writer.compileConvert(mainblock, 0, finalExpression, returnType);
				}
				writer.addLine(";", getLocation());
			}
		}
	}

	@Override
	public int getEndBlock() {
		return optional ? 0 : 1;
	}

	@Override
	public boolean putCounterBefore() {
		return true;
	}

	@Override
	public int getOperations() {
		return 0;
	}

	public Location getLocation() {
		if (expression == null) {
			return token.getLocation();
		}
		return new Location(token.getLocation(), expression.getLocation());
	}

	@Override
	public int getNature() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Type getType() {
		// TODO Auto-generated method stub
		return null;
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
