package leekscript.compiler.expression;

import java.util.LinkedHashMap;

import leekscript.common.Type;
import leekscript.compiler.JavaWriter;
import leekscript.compiler.WordCompiler;
import leekscript.compiler.bloc.MainLeekBlock;

public class LeekObject extends AbstractExpression {

	private final LinkedHashMap<String, AbstractExpression> mValues = new LinkedHashMap<>();

	public void addEntry(String key, AbstractExpression value) {
		mValues.put(key, value);
	}

	@Override
	public int getNature() {
		return OBJECT;
	}

	@Override
	public Type getType() {
		return Type.OBJECT;
	}

	@Override
	public String getString() {
		String str = "{";
		int i = 0;
		for (var entry : mValues.entrySet()) {
			if (i++ > 0) str += ", ";
			str += entry.getKey() + ": ";
			str += entry.getValue().getString();
		}
		return str + "}";
	}

	@Override
	public boolean validExpression(WordCompiler compiler, MainLeekBlock mainblock) throws LeekExpressionException {
		for (var entry : mValues.entrySet()) {
			entry.getValue().validExpression(compiler, mainblock);
		}
		return true;
	}

	@Override
	public void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer) {
		writer.addCode("new ObjectLeekValue(" + writer.getAIThis() + ", new String[] { ");
		int i = 0;
		for (var entry : mValues.entrySet()) {
			if (i++ != 0) writer.addCode(", ");
			writer.addCode("\"" + entry.getKey() + "\"");
		}
		writer.addCode(" }, new Object[] { ");
		i = 0;
		for (var entry : mValues.entrySet()) {
			if (i++ != 0) writer.addCode(", ");
			entry.getValue().writeJavaCode(mainblock, writer);
		}
		writer.addCode(" })");
	}

	@Override
	public void analyze(WordCompiler compiler) {
		operations = 0;
		for (var value : mValues.entrySet()) {
			value.getValue().analyze(compiler);
			operations += value.getValue().getOperations();
		}
	}
}
