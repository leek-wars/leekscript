package leekscript.compiler;

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

	public static void apply(AIFile file) {

		String code = file.getCode();
		if (code == null || code.indexOf("//") < 0) return;

		Integer version = null;
		int versionLine = -1;
		boolean strict = false;
		int strictLine = -1;

		String[] lines = code.split("\n", -1);
		for (int i = 0; i < lines.length; i++) {
			String line = lines[i];
			Matcher m = PRAGMA_PATTERN.matcher(line);
			if (!m.matches()) continue;

			int lineNum = i + 1;
			int startCol = line.indexOf('@') + 1;
			int endCol = line.length() + 1;
			Location loc = new Location(file, lineNum, startCol, lineNum, endCol);

			String name = m.group(1);
			String value = m.group(2);

			switch (name) {
				case "version": {
					if (versionLine != -1) {
						file.getErrors().add(new AnalyzeError(loc, AnalyzeErrorLevel.ERROR, Error.PRAGMA_DUPLICATE, new String[]{"version"}));
						break;
					}
					if (value == null) {
						file.getErrors().add(new AnalyzeError(loc, AnalyzeErrorLevel.ERROR, Error.PRAGMA_INVALID_VALUE, new String[]{"version", ""}));
						break;
					}
					int v;
					try {
						v = Integer.parseInt(value);
					} catch (NumberFormatException e) {
						file.getErrors().add(new AnalyzeError(loc, AnalyzeErrorLevel.ERROR, Error.PRAGMA_INVALID_VALUE, new String[]{"version", value}));
						break;
					}
					if (v < 1 || v > LeekScript.LATEST_VERSION) {
						file.getErrors().add(new AnalyzeError(loc, AnalyzeErrorLevel.ERROR, Error.PRAGMA_INVALID_VALUE, new String[]{"version", value}));
						break;
					}
					version = v;
					versionLine = lineNum;
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
					strictLine = lineNum;
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
