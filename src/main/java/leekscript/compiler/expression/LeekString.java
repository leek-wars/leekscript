package leekscript.compiler.expression;

import com.alibaba.fastjson.JSON;

import leekscript.common.Type;
import leekscript.compiler.Hover;
import leekscript.compiler.JavaWriter;
import leekscript.compiler.Location;
import leekscript.compiler.Token;
import leekscript.compiler.WordCompiler;
import leekscript.compiler.bloc.MainLeekBlock;

public class LeekString extends Expression {

	private final Token token;
	private final String mString;

	public LeekString(Token token, String str) {
		this.token = token;
		mString = str.substring(1, str.length() - 1);
		token.setExpression(this);
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
	public String toString() {
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

	public boolean equals(Object o) {
		if (o instanceof LeekString) {
			return mString.equals(((LeekString) o).mString);
		}
		return false;
	}

	@Override
	public Location getLocation() {
		return token.getLocation();
	}

	@Override
	public Hover hover(Token token) {
		var hover = new Hover(getType(), getLocation(), toString());
		hover.setSize(mString.length());
		return hover;
	}
}
