package leekscript.compiler.expression;

import leekscript.compiler.IAWord;
import leekscript.compiler.JavaWriter;
import leekscript.compiler.WordCompiler;
import leekscript.compiler.bloc.MainLeekBlock;

public class LeekGlobal extends AbstractExpression {

	private final IAWord token;

	public LeekGlobal(IAWord token) {
		this.token = token;
	}

	@Override
	public int getType() {
		return GLOBAL;
	}

	@Override
	public String getString() {
		return token.getWord();
	}

	@Override
	public boolean validExpression(WordCompiler compiler, MainLeekBlock mainblock) throws LeekExpressionException {
		// Pour une globale, la vérification est faite avant l'ajout donc pas besoin de refaire
		return true;
	}

	@Override
	public void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer) {
		writer.addCode("globale_" + token.getWord());
	}

	@Override
	public boolean isLeftValue() {
		return true;
	}

	@Override
	public void analyze(WordCompiler compiler) {

	}
}
