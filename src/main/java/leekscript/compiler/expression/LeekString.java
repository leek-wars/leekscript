package leekscript.compiler.expression;

import leekscript.compiler.JavaWriter;
import leekscript.compiler.bloc.MainLeekBlock;

public class LeekString extends AbstractExpression {

	private final String mString;

	public LeekString(String str) {
		mString = str;
	}

	@Override
	public int getType() {
		return STRING;
	}

	@Override
	public String getString() {
		return "\"" + mString + "\"";
	}

	@Override
	public boolean validExpression(MainLeekBlock mainblock) throws LeekExpressionException {
		//Pour une chaine de caractères pas de problèmes
		return true;
	}

	@Override
	public void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer) {
		String str = "";
		int len = mString.length() - 1;
		for (int i = 0; i < mString.length(); i++) {
			if (mString.charAt(i) == '\n') str += "\\n";
			else if (mString.charAt(i) == '"') str += "\\\"";
			else if (mString.charAt(i) == '\\') {
				if (len > i && mString.charAt(i + 1) == 'n') str += "\\";
				else if (len > i && mString.charAt(i + 1) == 't') str += "\\";
				else {
					if (mainblock.getCompiler().getCurrentAI().getVersion() >= 11) {
						str += "\\";
					} else {
						// LeekScript 1.0 had a bug with "\\" strings producing 4 \
						str += "\\\\";
					}
				}
			}
			else str += mString.charAt(i);
		}
		writer.addCode("new StringLeekValue(\"" + str + "\")");
	}
}
