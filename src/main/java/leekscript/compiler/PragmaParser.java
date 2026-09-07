package leekscript.compiler;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import leekscript.common.Error;
import leekscript.compiler.AnalyzeError.AnalyzeErrorLevel;

/**
 * Parses LeekScript source-level pragmas of the form <code>// @name</code> or
 * <code>// @name:value</code>. Pragmas can appear anywhere in the file but a given
 * pragma may only be declared once.
 *
 * Supported pragmas:
 *   <code>// @version:N</code> — selects the LeekScript language version (1..LATEST_VERSION)
 *   <code>// @strict</code>    — enables strict mode
 */
public class PragmaParser {

	private static final Pattern PRAGMA_PATTERN = Pattern.compile(
		"^\\s*//\\s*@([A-Za-z_][A-Za-z0-9_]*)(?:\\s*:\\s*(\\S+))?\\s*$"
	);

	/** Un pragma trouvé dans le code, avec sa position (colonnes à base 1, fin exclusive). */
	private record Pragma(String name, String value, int line, int startCol, int endCol) {}

	/** Tous les pragmas du code, dans l'ordre du fichier. Seule lecture du code : apply et declaredVersion en dépendent. */
	private static List<Pragma> scan(String code) {
		var pragmas = new ArrayList<Pragma>();
		if (code == null || code.indexOf("//") < 0) return pragmas;
		String[] lines = code.split("\n", -1);
		for (int i = 0; i < lines.length; i++) {
			String line = lines[i];
			Matcher m = PRAGMA_PATTERN.matcher(line);
			if (!m.matches()) continue;
			pragmas.add(new Pragma(m.group(1), m.group(2), i + 1, line.indexOf('@') + 1, line.length() + 1));
		}
		return pragmas;
	}

	/** Valeur d'un <code>@version:N</code> si elle est un entier de 1 à LATEST_VERSION, sinon null. */
	private static Integer parseVersion(String value) {
		if (value == null) return null;
		try {
			int v = Integer.parseInt(value);
			return v >= 1 && v <= LeekScript.LATEST_VERSION ? v : null;
		} catch (NumberFormatException e) {
			return null;
		}
	}

	/**
	 * Version déclarée par le premier pragma <code>// @version:N</code> valide du fichier, ou
	 * null s'il n'en déclare aucune. Même règle qu'{@link #apply} (une valeur invalide y produit
	 * une erreur sans fixer la version, le pragma valide suivant compte), mais sans effet de
	 * bord : ni erreur ajoutée au fichier, ni version modifiée.
	 */
	public static Integer declaredVersion(AIFile file) {
		for (var pragma : scan(file.getCode())) {
			if (!pragma.name().equals("version")) continue;
			Integer v = parseVersion(pragma.value());
			if (v != null) return v;
		}
		return null;
	}

	public static void apply(AIFile file) {

		Integer version = null;
		int versionLine = -1;
		boolean strict = false;
		int strictLine = -1;

		for (var pragma : scan(file.getCode())) {
			Location loc = new Location(file, pragma.line(), pragma.startCol(), pragma.line(), pragma.endCol());
			String name = pragma.name();
			String value = pragma.value();

			switch (name) {
				case "version": {
					if (versionLine != -1) {
						file.getErrors().add(new AnalyzeError(loc, AnalyzeErrorLevel.ERROR, Error.PRAGMA_DUPLICATE, new String[]{"version"}));
						break;
					}
					Integer v = parseVersion(value);
					if (v == null) {
						file.getErrors().add(new AnalyzeError(loc, AnalyzeErrorLevel.ERROR, Error.PRAGMA_INVALID_VALUE, new String[]{"version", value == null ? "" : value}));
						break;
					}
					version = v;
					versionLine = pragma.line();
					break;
				}
				case "strict": {
					if (strictLine != -1) {
						file.getErrors().add(new AnalyzeError(loc, AnalyzeErrorLevel.ERROR, Error.PRAGMA_DUPLICATE, new String[]{"strict"}));
						break;
					}
					if (value != null) {
						file.getErrors().add(new AnalyzeError(loc, AnalyzeErrorLevel.ERROR, Error.PRAGMA_INVALID_VALUE, new String[]{"strict", value}));
						break;
					}
					strict = true;
					strictLine = pragma.line();
					break;
				}
				default:
					file.getErrors().add(new AnalyzeError(loc, AnalyzeErrorLevel.WARNING, Error.PRAGMA_UNKNOWN, new String[]{name}));
			}
		}

		if (version != null || strictLine != -1) {
			int finalVersion = version != null ? version : file.getVersion();
			boolean finalStrict = strictLine != -1 ? strict : file.isStrict();
			file.setVersion(finalVersion, finalStrict);
		}
	}
}
