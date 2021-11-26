package leekscript.compiler.expression;

import com.alibaba.fastjson.JSON;

import leekscript.common.Type;
import leekscript.compiler.JavaWriter;
import leekscript.compiler.WordCompiler;
import leekscript.compiler.bloc.MainLeekBlock;

public class LeekString extends AbstractExpression {

	private final String mString;

	public LeekString(String str) {
		mString = str;
	}

	@Override
	public int getNature() {
		return STRING;
	}

	@Override
	public Type getType() {
		return Type.STRING;
	}

	@Override
	public String getString() {
		return JSON.toJSONString(mString);
	}

	@Override
	public boolean validExpression(WordCompiler compiler, MainLeekBlock mainblock) throws LeekExpressionException {
		// Pour une chaine de caractères pas de problèmes
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
					if (mainblock.getCompiler().getCurrentAI().getVersion() >= 2) {
						if (len > i && mString.charAt(i + 1) == '\\') {
							str += "\\\\";
							i++;
						} else {
							str += "\\\\";
						}
					} else {
						// LeekScript 1.0 had a bug with "\\" strings producing 4 \
						str += "\\\\";
					}
				}
			}
			else str += mString.charAt(i);
		}
		writer.addCode("\"" + str + "\"");
	}

	@Override
	public void analyze(WordCompiler compiler) {

	}
}
