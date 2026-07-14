package leekscript.compiler.expression;

import leekscript.util.Json;

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
		return Json.toJson(mString);
	}

	@Override
	public boolean validExpression(WordCompiler compiler, MainLeekBlock mainblock) throws LeekExpressionException {
		// Pour une chaine de caractères pas de problèmes
		return true;
	}

	@Override
	public void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer, boolean parenthesis) {
		String str = "";
		int len = mString.length() - 1;
		boolean v2plus = mainblock.getCompiler().getCurrentAI().getVersion() >= 2;
		boolean v4plus = mainblock.getCompiler().getCurrentAI().getVersion() >= 4;
		for (int i = 0; i < mString.length(); i++) {
			if (mString.charAt(i) == '\n') str += "\\n";
			else if (mString.charAt(i) == '"') str += "\\\"";
			else if (mString.charAt(i) == '\\') {
				if (len > i && mString.charAt(i + 1) == 'n') str += "\\";
				else if (len > i && mString.charAt(i + 1) == 't') str += "\\";
				// v2+ : `\"` est une vraie séquence d'échappement — on émet juste `"`.
				// (Le lexer skipe déjà le `\"` sans fermer la chaîne ; sans ce cas le
				// runtime gardait le backslash littéralement, donnant `length("a\"b") == 4`
				// au lieu de 3.) Comportement v1 préservé.
				else if (v2plus && len > i && mString.charAt(i + 1) == '"') {
					str += "\\\"";
					i++;
				}
				// v4+ : `\$` échappe l'interpolation — on émet un `$` littéral.
				else if (v4plus && len > i && mString.charAt(i + 1) == '$') {
					str += "$";
					i++;
				}
				else {
					if (v2plus) {
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

	public String getText() {
		return mString;
	}
}
