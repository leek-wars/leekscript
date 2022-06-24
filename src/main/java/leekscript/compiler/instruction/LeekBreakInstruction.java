package leekscript.compiler.instruction;

import leekscript.common.Type;
import leekscript.compiler.JavaWriter;
import leekscript.compiler.Location;
import leekscript.compiler.Token;
import leekscript.compiler.WordCompiler;
import leekscript.compiler.bloc.MainLeekBlock;
import leekscript.compiler.expression.LeekExpressionException;

public class LeekBreakInstruction extends LeekInstruction {

	private final Token token;

	public LeekBreakInstruction(Token token) {
		this.token = token;
	}

	@Override
	public String getCode() {
		return "break;";
	}

	@Override
	public void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer) {
		writer.addCounter(1);
		writer.addLine("break;", getLocation());
	}

	@Override
	public int getEndBlock() {
		return 2;
	}

	@Override
	public boolean putCounterBefore() {
		return true;
	}

	@Override
	public void analyze(WordCompiler compiler) {

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
