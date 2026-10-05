package leekscript.compiler.bloc;

import leekscript.common.Type;
import leekscript.compiler.JavaWriter;
import leekscript.compiler.Location;
import leekscript.compiler.Token;
import leekscript.compiler.WordCompiler;
import leekscript.compiler.expression.LeekExpressionException;

public class ScopeBlock extends AbstractLeekBlock {

	private final Token token;

	public ScopeBlock(AbstractLeekBlock parent, MainLeekBlock main, Token token) {
		super(parent, main);
		this.token = token;
	}

	@Override
	public String getCode() {
		return "{\n" + super.getCode() + "}";
	}

	@Override
	public void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer, boolean parenthesis) {
		writer.addLine("{");
		super.writeJavaCode(mainblock, writer, false);
		writer.addLine("}");
	}

	// Un bloc nu se termine comme sa dernière instruction : return (1), break/continue (2)
	// ou un mélange (3) remontent tels quels au parent, qui refuse alors le code qui suit,
	// comme javac (« unreachable statement »). Le défaut d'AbstractLeekBlock ne garde que 1.
	@Override
	public int getEndBlock() {
		return mEndInstruction;
	}

	@Override
	public Location getLocation() {
		return token.getLocation();
	}

	@Override
	public int getNature() {
		return 0;
	}

	@Override
	public Type getType() {
		return Type.VOID;
	}

	@Override
	public String toString() {
		return getCode();
	}

	@Override
	public boolean validExpression(WordCompiler compiler, MainLeekBlock mainblock) throws LeekExpressionException {
		return false;
	}
}
