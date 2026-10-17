package leekscript.compiler.expression;

import leekscript.common.Type;
import leekscript.compiler.Hover;
import leekscript.compiler.JavaWriter;
import leekscript.compiler.Location;
import leekscript.compiler.WordCompiler;
import leekscript.compiler.bloc.MainLeekBlock;

/**
 * Contenu d'un fichier du joueur, lu à la compilation : remplace le chemin littéral passé à une
 * fonction à argument fichier (cf. LeekFunctions.setFileArgument). Le texte est porté par un
 * champ statique de l'IA (MainLeekBlock.addFileContent), un seul par fichier.
 */
public class LeekFileContent extends Expression {

	// Au-delà, la chaîne est découpée : une constante Java ne dépasse pas 65 535 octets.
	private static final int CHUNK = 8000;
	private static final char[] HEX = "0123456789abcdef".toCharArray();

	private final LeekString path;
	private final String field;

	/** `field` : champ statique de l'IA qui porte le contenu (MainLeekBlock.addFileContent). */
	public LeekFileContent(LeekString path, String field) {
		this.path = path;
		this.field = field;
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
		return path.toString();
	}

	@Override
	public boolean validExpression(WordCompiler compiler, MainLeekBlock mainblock) {
		return true;
	}

	@Override
	public void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer, boolean parenthesis) {
		writer.addCode(field);
	}

	/** Expression Java d'une chaîne quelconque, découpée si elle dépasse la taille d'une constante. */
	public static String javaString(String content) {
		if (content.length() <= CHUNK) return javaLiteral(content);
		// String.join n'est pas replié en une seule constante par javac.
		var sb = new StringBuilder("String.join(\"\"");
		for (int i = 0; i < content.length(); i += CHUNK) {
			sb.append(", ").append(javaLiteral(content.substring(i, Math.min(content.length(), i + CHUNK))));
		}
		return sb.append(")").toString();
	}

	private static String javaLiteral(String text) {
		var sb = new StringBuilder(text.length() + 2).append('"');
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			switch (c) {
				case '"' -> sb.append("\\\"");
				case '\\' -> sb.append("\\\\");
				case '\n' -> sb.append("\\n");
				case '\r' -> sb.append("\\r");
				case '\t' -> sb.append("\\t");
				default -> {
					// Jamais de fin de ligne en échappement unicode (javac le décode avant de lire la chaîne) : traitées plus haut.
					if (c < 0x20 || c > 0x7e) {
						sb.append("\\u").append(HEX[c >> 12]).append(HEX[(c >> 8) & 0xF]).append(HEX[(c >> 4) & 0xF]).append(HEX[c & 0xF]);
					} else {
						sb.append(c);
					}
				}
			}
		}
		return sb.append('"').toString();
	}

	@Override
	public void analyze(WordCompiler compiler) {
	}

	@Override
	public Location getLocation() {
		return path.getLocation();
	}

	@Override
	public Hover hover(leekscript.compiler.Token token) {
		return path.hover(token);
	}
}
