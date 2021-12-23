package leekscript.runner;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import leekscript.AILog;
import leekscript.functions.Functions;
import leekscript.functions.VariableOperations;
import leekscript.runner.values.ArrayLeekValue;
import leekscript.runner.values.FunctionLeekValue;
import leekscript.common.Error;
import leekscript.common.Type;

public enum LeekFunctions implements ILeekFunction {

	// Fonctions mathématiques
	// abs(new CallableVersion[] {
	// 	new CallableVersion(Type.INT, new Type[] { Type.INT }),
	// 	new CallableVersion(Type.REAL, new Type[] { Type.REAL })
	// }),
	abs(1, new int[] { AI.NUMBER }) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			if (parameters[0] instanceof Integer) {
				return Math.abs((Integer) parameters[0]);
			}
			return Math.abs(ai.real(parameters[0]));
		}
	},

	min(2, new int[] { AI.NUMBER, AI.NUMBER }) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			if (parameters[0] instanceof Integer && parameters[1] instanceof Integer) {
				var v1 = ((Integer) parameters[0]).intValue();
				var v2 = ((Integer) parameters[1]).intValue();
				return Math.min(v1, v2);
			}
			var v1 = ((Number) parameters[0]).doubleValue();
			var v2 = ((Number) parameters[1]).doubleValue();
			return Math.min(v1, v2);
		}
	},

	max(2, new int[] { AI.NUMBER, AI.NUMBER }) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			if (parameters[0] instanceof Integer && parameters[1] instanceof Integer) {
				var v1 = ((Integer) parameters[0]).intValue();
				var v2 = ((Integer) parameters[1]).intValue();
				return Math.max(v1, v2);
			}
			double v1 = ((Number) parameters[0]).doubleValue();
			double v2 = ((Number) parameters[1]).doubleValue();
			return Math.max(v1, v2);
		}
	},

	// cos(new CallableVersion[] {
	// 	new CallableVersion(Type.REAL, new Type[] { Type.REAL })
	// }),
	cos(1, new int[] { AI.NUMBER }) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			double v = ai.real(parameters[0]);
			return Math.cos(v);
		}
	},

	sin(1, new int[] { AI.NUMBER }) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			double v = ai.real(parameters[0]);
			return Math.sin(v);
		}
	},
	tan(1, new int[] { AI.NUMBER }) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			double v = ai.real(parameters[0]);
			return Math.tan(v);
		}
	},
	toRadians(1, new int[] { AI.NUMBER }) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			double v = ai.real(parameters[0]);
			return v * Math.PI / 180;
		}
	},
	toDegrees(1, new int[] { AI.NUMBER }) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			double v = ai.real(parameters[0]);
			return v * 180 / Math.PI;
		}
	},

	// acos(new CallableVersion[] {
	// 	new CallableVersion(Type.REAL, new Type[] { Type.REAL })
	// }),
	acos(1, new int[] { AI.NUMBER }) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			double v = ai.real(parameters[0]);
			return Math.acos(v);
		}
	},

	// asin(new CallableVersion[] {
	// 	new CallableVersion(Type.REAL, new Type[] { Type.REAL })
	// }),
	asin(1, new int[] { AI.NUMBER }) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			double v = ai.real(parameters[0]);
			return Math.asin(v);
		}
	},

	// atan(new CallableVersion[] {
	// 	new CallableVersion(Type.REAL, new Type[] { Type.REAL })
	// }),
	atan(1, new int[] { AI.NUMBER }) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			double v = ai.real(parameters[0]);
			return Math.atan(v);
		}
	},

	atan2(2, new int[] { AI.NUMBER, AI.NUMBER }) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			double y = ai.real(parameters[0]);
			double x = ai.real(parameters[1]);
			return Math.atan2(y, x);
		}
	},

	// ceil(new CallableVersion[] {
	// 	new CallableVersion(Type.INT, new Type[] { Type.INT }),
	// 	new CallableVersion(Type.INT, new Type[] { Type.REAL }),
	// }),
	ceil(1, new int[] { AI.NUMBER }) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			double v = ai.real(parameters[0]);
			return (int) Math.ceil(v);
		}
	},

	// floor(new CallableVersion[] {
	// 	new CallableVersion(Type.INT, new Type[] { Type.INT }),
	// 	new CallableVersion(Type.INT, new Type[] { Type.REAL }),
	// }),
	floor(1, new int[] { AI.NUMBER }) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			double v = ai.real(parameters[0]);
			return (int) Math.floor(v);
		}
	},

	// round(new CallableVersion[] {
	// 	new CallableVersion(Type.INT, new Type[] { Type.INT }),
	// 	new CallableVersion(Type.INT, new Type[] { Type.REAL }),
	// }),
	round(1, new int[] { AI.NUMBER }) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			double v = ai.real(parameters[0]);
			return (int) Math.round(v);
		}
	},

	sqrt(1, new int[] { AI.NUMBER }) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			double v = ai.real(parameters[0]);
			return Math.sqrt(v);
		}
	},
	cbrt(1, new int[] { AI.NUMBER }) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			double v = ai.real(parameters[0]);
			return Math.cbrt(v);
		}
	},
	log(1, new int[] { AI.NUMBER }) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			double v = ai.real(parameters[0]);
			return Math.log(v);
		}
	},
	log2(1, new int[] { AI.NUMBER }) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			double v = ai.real(parameters[0]);
			return Math.log(v) / Math.log(2);
		}
	},
	log10(1, new int[] { AI.NUMBER }) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			double v = ai.real(parameters[0]);
			return Math.log10(v);
		}
	},
	exp(1, new int[] { AI.NUMBER }) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			double v = ai.real(parameters[0]);
			return Math.exp(v);
		}
	},

	pow(2, new int[] { AI.NUMBER, AI.NUMBER }) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			double x = ai.real(parameters[0]);
			double y = ai.real(parameters[1]);
			return Math.pow(x, y);
		}
	},

	rand(0) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ai.getRandom().getDouble();
		}
	},
	randInt(2, new int[] { AI.NUMBER, AI.NUMBER }) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			int nb = ai.integer(parameters[0]);
			int nb1 = ai.integer(parameters[1]);
			if (nb > nb1)
				return ai.getRandom().getInt(nb1, nb - 1);
			else
				return ai.getRandom().getInt(nb, nb1 - 1);
		}
	},
	randFloat(2, new int[] { AI.NUMBER, AI.NUMBER }) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			double a = ai.real(parameters[0]);
			double b = ai.real(parameters[1]);
			return a + ai.getRandom().getDouble() * (b - a);
		}
	},
	hypot(2, new int[] { AI.NUMBER, AI.NUMBER }) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			double x = ai.real(parameters[0]);
			double y = ai.real(parameters[1]);
			return Math.hypot(x, y);
		}
	},
	signum(1, new int[] { AI.NUMBER }) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			double v = ((Number) parameters[0]).doubleValue();
			return (int) Math.signum(v);
		}
	},
	string(1) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ai.string(parameters[0]);
		}
	},

	// Fonctions string
	charAt(2, new int[] { AI.STRING, AI.NUMBER }) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			int pos = ai.integer(parameters[1]);
			String str = (String) parameters[0];
			if (pos < 0 || pos >= str.length())
				return null;
			return String.valueOf(str.charAt(pos));
		}
	},
	length(1, new int[] { AI.STRING }) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			String v = LeekValueManager.getString(ai, parameters[0]);
			return v.length();
		}
	},
	substring(2, 3, new int[] { AI.STRING, AI.NUMBER, -1 }) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			if (AI.verifyParameters(new int[] { AI.STRING, AI.NUMBER, AI.NUMBER }, parameters)) {
				String string = LeekValueManager.getString(ai, parameters[0]);
				int index = ai.integer(parameters[1]);
				int length = ai.integer(parameters[2]);
				if (string.length() <= index || index < 0 || index + length > string.length() || length < 0) {
					return null;
				}
				return string.substring(index, index + length);
			} else {
				String string = LeekValueManager.getString(ai, parameters[0]);
				int index = ai.integer(parameters[1]);
				if (string.length() <= index || index < 0) {
					return null;
				}
				return string.substring(index);
			}
		}

		@Override
		public void addOperations(AI ai, ILeekFunction function, Object parameters[], Object retour) throws LeekRunException {
			String s = LeekValueManager.getString(ai, retour);
			ai.ops(hasVariableOperations() ? mVariableOperations.getOperations(s.length()) : 1);
		}
	},
	replace(3, new int[] { AI.STRING, AI.STRING, AI.STRING }) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			String s = LeekValueManager.getString(ai, parameters[0]);
			String pattern = LeekValueManager.getString(ai, parameters[1]);
			String replacement = LeekValueManager.getString(ai, parameters[2]);
			return s.replaceAll(Pattern.quote(pattern),	Matcher.quoteReplacement(replacement));
		}

		@Override
		public void addOperations(AI ai, ILeekFunction function, Object parameters[], Object retour) throws LeekRunException {
			String s = LeekValueManager.getString(ai, parameters[0]);
			ai.ops(hasVariableOperations() ? mVariableOperations.getOperations(s.length()) : 1);
		}
	},
	indexOf(2, 3, new int[] { AI.STRING, AI.STRING, -1 }) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			String s = (String) parameters[0];
			String needle = (String) parameters[1];
			int from = ai.integer(parameters[2]);
			if (AI.verifyParameters(new int[] { AI.STRING, AI.STRING, AI.NUMBER }, parameters)) {
				return s.indexOf(needle, from);
			} else {
				return s.indexOf(needle);
			}
		}

		@Override
		public void addOperations(AI ai, ILeekFunction function, Object parameters[], Object retour) throws LeekRunException {
			String s = LeekValueManager.getString(ai, parameters[0]);
			ai.ops(hasVariableOperations() ? mVariableOperations.getOperations(s.length()) : 1);
		}
	},
	split(2, 3, new int[] { AI.STRING, AI.STRING, -1 }) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			String s = LeekValueManager.getString(ai, parameters[0]);
			String delimiter = LeekValueManager.getString(ai, parameters[1]);
			if (AI.verifyParameters(new int[] { AI.STRING, AI.STRING, AI.NUMBER }, parameters)) {
				int limit = ai.integer(parameters[2]);
				String[] elements = s.split(delimiter, limit);
				ArrayLeekValue array = new ArrayLeekValue();
				for (short i = 0; i < elements.length; i++) {
					array.push(ai, elements[i]);
				}
				return array;
			} else {
				String[] elements = s.split(Pattern.quote(delimiter));
				ArrayLeekValue array = new ArrayLeekValue();
				for (short i = 0; i < elements.length; i++) {
					array.push(ai, elements[i]);
				}
				return array;
			}
		}

		@Override
		public void addOperations(AI ai, ILeekFunction function, Object parameters[], Object retour) throws LeekRunException {
			String s = LeekValueManager.getString(ai, parameters[0]);
			String delimiter = LeekValueManager.getString(ai, parameters[1]);
			ai.ops(hasVariableOperations() ? mVariableOperations.getOperations((int) (s.length() * Math.log(delimiter.length() + 1))) : 1);
			if (retour instanceof ArrayLeekValue) {
				ai.ops(((ArrayLeekValue) retour).size());
			}
		}
	},
	toLower(1, new int[] { AI.STRING }) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			String s = LeekValueManager.getString(ai, parameters[0]);
			return s.toLowerCase();
		}

		@Override
		public void addOperations(AI ai, ILeekFunction function, Object parameters[], Object retour) throws LeekRunException {
			ai.ops(hasVariableOperations() ? mVariableOperations.getOperations(((String) retour).length()) : 1);
		}
	},
	toUpper(1, new int[] { AI.STRING }) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			String s = LeekValueManager.getString(ai, parameters[0]);
			return s.toUpperCase();
		}

		@Override
		public void addOperations(AI ai, ILeekFunction function, Object parameters[], Object retour) throws LeekRunException {
			ai.ops(hasVariableOperations() ? mVariableOperations.getOperations(((String) retour).length()) : 1);
		}
	},
	startsWith(2, new int[] { AI.STRING, AI.STRING }) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			String s = LeekValueManager.getString(ai, parameters[0]);
			String prefix = LeekValueManager.getString(ai, parameters[1]);
			return s.startsWith(prefix);
		}

		@Override
		public void addOperations(AI ai, ILeekFunction function, Object parameters[], Object retour) throws LeekRunException {
			String s = LeekValueManager.getString(ai, parameters[0]);
			String prefix = LeekValueManager.getString(ai, parameters[1]);
			if (s.length() > prefix.length()) {
				ai.ops(hasVariableOperations() ? mVariableOperations.getOperations((prefix.length())) : 1);
			} else
				ai.ops(1);
		}
	},
	endsWith(2, new int[] { AI.STRING, AI.STRING }) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			String s = LeekValueManager.getString(ai, parameters[0]);
			String suffix = LeekValueManager.getString(ai, parameters[1]);
			return s.endsWith(suffix);
		}

		@Override
		public void addOperations(AI ai, ILeekFunction function, Object parameters[], Object retour) throws LeekRunException {
			String s = LeekValueManager.getString(ai, parameters[0]);
			String suffix = LeekValueManager.getString(ai, parameters[1]);
			if (s.length() > suffix.length()) {
				ai.ops(hasVariableOperations() ? mVariableOperations.getOperations((suffix.length())) : 1);
			} else
				ai.ops(1);
		}
	},
	contains(2, new int[] { AI.STRING, AI.STRING }) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			String haystack = LeekValueManager.getString(ai, parameters[0]);
			String needle = LeekValueManager.getString(ai, parameters[1]);
			return haystack.contains(needle);
		}

		@Override
		public void addOperations(AI ai, ILeekFunction function, Object parameters[], Object retour) throws LeekRunException {
			String haystack = LeekValueManager.getString(ai, parameters[0]);
			String needle = LeekValueManager.getString(ai, parameters[1]);
			ai.ops(hasVariableOperations() ? mVariableOperations.getOperations((int) (haystack.length() * Math.log(needle.length() + 1))) : 1);
		}
	},
	number(1) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			var v = parameters[0];
			if (v instanceof Number)
				return v;
			if (v instanceof String) {
				var s = (String) v;
				try {
					if (s.contains(".")) {
						return Double.parseDouble(s);
					} else {
						return Integer.parseInt(s);
					}
				} catch (Exception e) {}
			}
			return null;
		}
	},
	// Fonctions array
	remove(2, new int[] { AI.ARRAY, AI.NUMBER }) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			var array = (ArrayLeekValue) parameters[0];
			var index = ((Number) parameters[1]).intValue();
			return array.remove(ai, index);
		}

		@Override
		public void addOperations(AI ai, ILeekFunction function, Object parameters[], Object retour) throws LeekRunException {
			var array = (ArrayLeekValue) parameters[0];
			var index = ((Number) parameters[1]).intValue();
			if (index >= 0 && index < array.size()) {
				ai.ops(hasVariableOperations() ? mVariableOperations.getOperations(index + 1) : 1);
			} else
				ai.ops(1);
		}
	},

	// count(new CallableVersion[] { new CallableVersion(Type.ANY, new Type[] { Type.ARRAY }) }),
	count(1, new int[] { AI.ARRAY }) {
		@Override
		public Object run(AI leekIA, ILeekFunction function, Object... parameters) throws LeekRunException {
			var array = (ArrayLeekValue) parameters[0];
			return array.size();
		}
	},

	join(2, new int[] { AI.ARRAY, AI.STRING }) {
		@Override
		public Object run(AI leekIA, ILeekFunction function, Object... parameters) throws LeekRunException {
			var array = (ArrayLeekValue) parameters[0];
			var delimiter = (String) parameters[1];
			return array.join(leekIA, delimiter);
		}

		@Override
		public void addOperations(AI leekIA, ILeekFunction function, Object parameters[], Object retour) throws LeekRunException {
			var r = (String) retour;
			leekIA.ops(hasVariableOperations() ? mVariableOperations.getOperations(r.length() + 1) : 1);
		}
	},
	insert(3, new int[] { AI.ARRAY, -1, AI.NUMBER }) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			var array = (ArrayLeekValue) parameters[0];
			var index = ((Number) parameters[2]).intValue();
			if (ai.getVersion() == 1) {
				array.insert(ai, LeekOperations.clone(ai, parameters[1]), index);
			} else {
				array.insert(ai, parameters[1], index);
			}
			return null;
		}

		@Override
		public void addOperations(AI ai, ILeekFunction function, Object parameters[], Object retour) throws LeekRunException {
			var array = (ArrayLeekValue) parameters[0];
			var index = ((Number) parameters[2]).intValue();
			ai.ops(1 + (array.size() - index) * 4);
		}
	},
	push(2, new int[] { AI.ARRAY, -1 }) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			var array = (ArrayLeekValue) parameters[0];
			if (ai.getVersion() >= 2) {
				array.push(ai, parameters[1]);
			} else {
				array.push(ai, LeekOperations.clone(ai, parameters[1]));
			}
			return null;
		}
	},
	unshift(2, new int[] { AI.ARRAY, -1 }) {
		@Override
		public Object run(AI leekIA, ILeekFunction function, Object... parameters) throws LeekRunException {
			var array = (ArrayLeekValue) parameters[0];
			if (leekIA.getVersion() == 1) {
				var value = LeekOperations.clone(leekIA, parameters[1]);
				array.insert(leekIA, value, 0);
			} else {
				array.insert(leekIA, parameters[1], 0);
			}
			return null;
		}
	},
	shift(1, new int[] { AI.ARRAY }) {
		@Override
		public Object run(AI leekIA, ILeekFunction function, Object... parameters) throws LeekRunException {
			var array = (ArrayLeekValue) parameters[0];
			return array.remove(leekIA, 0);
		}
	},
	pop(1, new int[] { AI.ARRAY }) {
		@Override
		public Object run(AI leekIA, ILeekFunction function, Object... parameters) throws LeekRunException {
			var array = (ArrayLeekValue) parameters[0];
			return array.remove(leekIA, array.size() - 1);
		}
	},
	removeElement(2, new int[] { AI.ARRAY, -1 }) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			var array = (ArrayLeekValue) parameters[0];
			array.removeObject(ai, parameters[1]);
			return null;
		}

		@Override
		public void addOperations(AI ai, ILeekFunction function, Object parameters[], Object retour) throws LeekRunException {
			var array = (ArrayLeekValue) parameters[0];
			ai.ops(hasVariableOperations() ? mVariableOperations.getOperations(array.size() + 1) : 1);
		}
	},
	removeKey(2, new int[] { AI.ARRAY, -1 }) {
		@Override
		public Object run(AI leekIA, ILeekFunction function, Object... parameters) throws LeekRunException {
			var array = (ArrayLeekValue) parameters[0];
			array.removeByKey(leekIA, parameters[1]);
			return null;
		}
	},
	sort(1, 2, new int[] { AI.ARRAY, -1 }) {
		@Override
		public Object run(AI leekIA, ILeekFunction function, Object... parameters) throws LeekRunException {
			var array = (ArrayLeekValue) parameters[0];
			int type = LeekValueComparator.SortComparator.SORT_ASC;
			if (leekIA.bool(parameters[1]))
				type = LeekValueComparator.SortComparator.SORT_DESC;
			array.sort(leekIA, type);
			return null;
		}

		@Override
		public void addOperations(AI ai, ILeekFunction function, Object parameters[], Object retour) throws LeekRunException {
			var array = (ArrayLeekValue) parameters[0];
			ai.ops(hasVariableOperations() ? mVariableOperations.getOperations(array.size() + 1) : 1);
		}
	},
	assocSort(1, 2, new int[] { AI.ARRAY, -1 }) {
		@Override
		public Object run(AI leekIA, ILeekFunction function, Object... parameters) throws LeekRunException {
			var array = (ArrayLeekValue) parameters[0];
			int type = PhpArray.ASC_A;
			if (leekIA.bool(parameters[1]))
				type = PhpArray.DESC_A;
			// try {
			array.sort(leekIA, type);
			// } catch (Exception e) {

			// 	e.printStackTrace(System.out);
			// }
			return null;
		}

		@Override
		public void addOperations(AI ai, ILeekFunction function, Object parameters[], Object retour) throws LeekRunException {
			var array = (ArrayLeekValue) parameters[0];
			ai.ops(hasVariableOperations() ? mVariableOperations.getOperations(array.size() + 1) : 1);
		}
	},
	keySort(1, 2, new int[] { AI.ARRAY, -1 }) {
		@Override
		public Object run(AI leekIA, ILeekFunction function, Object... parameters) throws LeekRunException {
			var array = (ArrayLeekValue) parameters[0];
			int type = PhpArray.ASC_K;
			if (leekIA.bool(parameters[1]))
				type = PhpArray.DESC_K;
			array.sort(leekIA, type);
			return null;
		}

		@Override
		public void addOperations(AI ai, ILeekFunction function, Object parameters[], Object retour) throws LeekRunException {
			var array = (ArrayLeekValue) parameters[0];
			ai.ops(hasVariableOperations() ? mVariableOperations.getOperations(array.size() + 1) : 1);
		}
	},
	shuffle(1, new int[] { AI.ARRAY }) {
		@Override
		public Object run(AI leekIA, ILeekFunction function, Object... parameters) throws LeekRunException {
			var array = (ArrayLeekValue) parameters[0];
			array.shuffle(leekIA);
			return null;
		}

		@Override
		public void addOperations(AI leekIA, ILeekFunction function, Object parameters[], Object retour) throws LeekRunException {
			var array = (ArrayLeekValue) parameters[0];
			leekIA.ops(hasVariableOperations() ? mVariableOperations.getOperations(array.size() + 1) : 1);
		}
	},
	search(2, 3, new int[] { AI.ARRAY, -1, -1 }) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			var array = (ArrayLeekValue) parameters[0];
			if (AI.verifyParameters(new int[] { AI.ARRAY, -1, AI.NUMBER }, parameters)) {
				var index = ai.integer(parameters[2]);
				return array.search(ai, parameters[1], index);
			} else {
				return array.search(ai, parameters[1], 0);
			}
		}

		@Override
		public void addOperations(AI leekIA, ILeekFunction function, Object parameters[], Object retour) throws LeekRunException {
			var array = (ArrayLeekValue) parameters[0];
			leekIA.ops(hasVariableOperations() ? mVariableOperations.getOperations(array.size() + 1) : 1);
		}
	},
	inArray(2, new int[] { AI.ARRAY, -1 }) {
		@Override
		public Object run(AI leekIA, ILeekFunction function, Object... parameters) throws LeekRunException {
			var array = (ArrayLeekValue) parameters[0];
			return array.contains(leekIA, parameters[1]);
		}

		@Override
		public void addOperations(AI ai, ILeekFunction function, Object parameters[], Object retour) throws LeekRunException {
			var array = (ArrayLeekValue) parameters[0];
			ai.ops(hasVariableOperations() ? mVariableOperations.getOperations(array.size() + 1) : 1);
		}
	},
	reverse(1, new int[] { AI.ARRAY }) {
		@Override
		public Object run(AI leekIA, ILeekFunction function, Object... parameters) throws LeekRunException {
			var array = (ArrayLeekValue) parameters[0];
			array.reverse(leekIA);
			return null;
		}

		@Override
		public void addOperations(AI ai, ILeekFunction function, Object parameters[], Object retour) throws LeekRunException {
			var array = (ArrayLeekValue) parameters[0];
			ai.ops(hasVariableOperations() ? mVariableOperations.getOperations(array.size() + 1) : 1);
		}
	},
	arrayMin(1, new int[] { AI.ARRAY }) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			var array = (ArrayLeekValue) parameters[0];
			if (array.size() > 0) {
				var comp = new LeekValueComparator.SortComparator(ai, LeekValueComparator.SortComparator.SORT_ASC);
				var iterator = array.iterator();
				Object min_c = iterator.next().getValue();
				while (iterator.hasNext()) {
					var value = iterator.next().getValue();
					if (comp.compare(value, min_c) == -1) {
						min_c = value;
					}
				}
				return LeekOperations.clone(ai, min_c);
			}
			return null;
		}
	},
	arrayMax(1, new int[] { AI.ARRAY }) {
		@Override
		public Object run(AI leekIA, ILeekFunction function, Object... parameters) throws LeekRunException {
			var array = (ArrayLeekValue) parameters[0];
			Object min_c = null;
			var mincomp = new LeekValueComparator.SortComparator(leekIA, LeekValueComparator.SortComparator.SORT_ASC);
			for (var val : array) {
				if (min_c == null)
					min_c = val.getValue();
				else if (mincomp.compare(val.getValue(), min_c) == 1)
					min_c = val.getValue();
			}
			if (min_c == null)
				return null;
			else
				return LeekOperations.clone(leekIA, min_c);
		}

		@Override
		public void addOperations(AI ai, ILeekFunction function, Object parameters[], Object retour) throws LeekRunException {
			var array = (ArrayLeekValue) parameters[0];
			ai.ops(hasVariableOperations() ? mVariableOperations.getOperations(array.size() + 1) : 1);
		}
	},
	sum(1, new int[] { AI.ARRAY }) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			var array = (ArrayLeekValue) parameters[0];
			double somme = 0;
			for (var val : array) {
				somme += ai.real(val.getValue());
			}
			return somme;
		}

		@Override
		public void addOperations(AI ai, ILeekFunction function, Object parameters[], Object retour) throws LeekRunException {
			var array = (ArrayLeekValue) parameters[0];
			ai.ops(hasVariableOperations() ? mVariableOperations.getOperations(array.size() + 1) : 1);
		}
	},
	average(1, new int[] { AI.ARRAY }) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			var array = (ArrayLeekValue) parameters[0];
			double average = 0;
			for (var val : array) {
				average += ai.real(val.getValue());
			}
			if (average == 0)
				return 0.0;
			return average / array.size();
		}

		@Override
		public void addOperations(AI ai, ILeekFunction function, Object parameters[], Object retour) throws LeekRunException {
			var array = (ArrayLeekValue) parameters[0];
			ai.ops(hasVariableOperations() ? mVariableOperations.getOperations(array.size() + 1) : 1);
		}
	},
	fill(2, 3, new int[] { AI.ARRAY, -1, -1 }) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			var array = (ArrayLeekValue) parameters[0];
			int size = array.size();
			if (AI.isType(parameters[2], AI.NUMBER))
				size = ai.integer(parameters[2]);
			for (int i = 0; i < size; i++) {
				array.put(ai, i, parameters[1]);
				ai.ops(3);
			}
			return null;
		}

		@Override
		public void addOperations(AI leekIA, ILeekFunction function, Object parameters[], Object retour) throws LeekRunException {}
	},
	isEmpty(1, new int[] { AI.ARRAY }) {
		@Override
		public Object run(AI leekIA, ILeekFunction function, Object... parameters) throws LeekRunException {
			var array = (ArrayLeekValue) parameters[0];
			return array.size() == 0;
		}
	},
	subArray(3, new int[] { AI.ARRAY, AI.NUMBER, AI.NUMBER }) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			var array = (ArrayLeekValue) parameters[0];
			int start = ai.integer(parameters[1]);
			int end = ai.integer(parameters[2]);
			if (start < 0 || end < start || end >= array.size())
				return null;
			ArrayLeekValue retour = new ArrayLeekValue();
			int i = 0;
			for (var val : array) {
				if (i >= start && i <= end) {
					retour.push(ai, LeekOperations.clone(ai, val.getValue()));
					ai.ops(1);
				}
				i++;
			}
			return retour;
		}

		@Override
		public void addOperations(AI leekIA, ILeekFunction function, Object parameters[], Object retour) throws LeekRunException {}
	},
	pushAll(2, new int[] { AI.ARRAY, AI.ARRAY }) {
		@Override
		public Object run(AI leekIA, ILeekFunction function, Object... parameters) throws LeekRunException {
			var array = (ArrayLeekValue) parameters[0];
			var array2 = (ArrayLeekValue) parameters[1];
			for (var value : array2) {
				if (leekIA.getVersion() == 1) {
					array.push(leekIA, LeekOperations.clone(leekIA, value.getValue()));
				} else {
					array.push(leekIA, value.getValue());
				}
				leekIA.ops(1);
			}
			return null;
		}

		@Override
		public void addOperations(AI leekIA, ILeekFunction function, Object parameters[], Object retour) throws LeekRunException {}
	},
	assocReverse(1, new int[] { AI.ARRAY }) {
		@Override
		public Object run(AI leekIA, ILeekFunction function, Object... parameters) throws LeekRunException {
			var array = (ArrayLeekValue) parameters[0];
			array.assocReverse();
			return null;
		}

		@Override
		public void addOperations(AI ai, ILeekFunction function, Object parameters[], Object retour) throws LeekRunException {
			ai.ops(hasVariableOperations() ? mVariableOperations.getOperations(((ArrayLeekValue) parameters[0]).size() + 1) : 1);
		}
	},
	arrayMap(2, new int[] { AI.ARRAY, AI.FUNCTION }) {
		@Override
		public Object run(AI leekIA, ILeekFunction function, Object... parameters) throws LeekRunException {
			var array = (ArrayLeekValue) parameters[0];
			var fun = (FunctionLeekValue) parameters[1];
			if (leekIA.getVersion() >= 2) {
				return leekIA.arrayMap(array, fun);
			} else {
				return leekIA.arrayMapV1(array, fun);
			}
		}
	},
	arrayFilter(2, new int[] { AI.ARRAY, AI.FUNCTION }) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			if (ai.getVersion() >= 2) {
				return ai.arrayFilter((ArrayLeekValue) parameters[0], (FunctionLeekValue) parameters[1]);
			} else {
				return ai.arrayFilterV1((ArrayLeekValue) parameters[0], (FunctionLeekValue) parameters[1]);
			}
		}
	},
	arrayFlatten(1, 2, new int[] { AI.ARRAY, -1 }) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			int maxDepth = AI.isType(parameters[1], AI.NUMBER) ? ai.integer(parameters[1]) : 1;
			ArrayLeekValue retour = new ArrayLeekValue();
			ai.arrayFlatten((ArrayLeekValue) parameters[0], retour, maxDepth);
			return retour;
		}

		@Override
		public void addOperations(AI leekIA, ILeekFunction function, Object parameters[], Object retour) throws LeekRunException {
			leekIA.ops(hasVariableOperations() ? mVariableOperations.getOperations(((ArrayLeekValue) retour).size() + 1) : 1);
		}
	},
	arrayFoldLeft(2, 3, new int[] { AI.ARRAY, AI.FUNCTION, -1 }) {
		@Override
		public Object run(AI leekIA, ILeekFunction function, Object... parameters) throws LeekRunException {
			return leekIA.arrayFoldLeft((ArrayLeekValue) parameters[0], (FunctionLeekValue) parameters[1], parameters[2]);
		}
	},
	arrayFoldRight(2, 3, new int[] { AI.ARRAY, AI.FUNCTION, -1 }) {
		@Override
		public Object run(AI leekIA, ILeekFunction function, Object... parameters) throws LeekRunException {
			var array = (ArrayLeekValue) parameters[0];
			return leekIA.arrayFoldRight(array, (FunctionLeekValue) parameters[1], parameters[2]);
		}
	},
	arrayPartition(2, new int[] { AI.ARRAY, AI.FUNCTION }) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			if (ai.getVersion() >= 2) {
				return ai.arrayPartition((ArrayLeekValue) parameters[0], (FunctionLeekValue) parameters[1]);
			} else {
				return ai.arrayPartitionV1((ArrayLeekValue) parameters[0], (FunctionLeekValue) parameters[1]);
			}
		}
	},
	arrayIter(2, new int[] { AI.ARRAY, AI.FUNCTION }) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			if (ai.getVersion() >= 2) {
				return ai.arrayIter((ArrayLeekValue) parameters[0], (FunctionLeekValue) parameters[1]);
			} else {
				return ai.arrayIterV1((ArrayLeekValue) parameters[0], (FunctionLeekValue) parameters[1]);
			}
		}
	},
	arrayConcat(2, new int[] { AI.ARRAY, AI.ARRAY }) {
		@Override
		public Object run(AI leekIA, ILeekFunction function, Object... parameters) throws LeekRunException {
			leekIA.ops(1);
			return leekIA.add(parameters[0], parameters[1]);
		}
	},
	arraySort(2, new int[] { AI.ARRAY, AI.FUNCTION }) {
		@Override
		public Object run(AI leekIA, ILeekFunction function, Object... parameters) throws LeekRunException {
			return leekIA.arraySort((ArrayLeekValue) parameters[0], (FunctionLeekValue) parameters[1]);
		}
	},

	// debug(new CallableVersion[] { new CallableVersion(Type.NULL, new Type[] { Type.ANY }) }),
	// debugW(new CallableVersion[] { new CallableVersion(Type.NULL, new Type[] { Type.ANY }) }),
	// debugE(new CallableVersion[] { new CallableVersion(Type.NULL, new Type[] { Type.ANY }) }),
	debug(1) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			String message = LeekValueManager.getString(ai, parameters[0]);
			ai.getLogs().addLog(AILog.STANDARD, message);
			ai.ops(message.length());
			return null;
		}
	},
	debugW(1) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			String message = LeekValueManager.getString(ai, parameters[0]);
			ai.getLogs().addLog(AILog.WARNING, message);
			ai.ops(message.length());
			return null;
		}
	},
	debugE(1) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			String message = LeekValueManager.getString(ai, parameters[0]);
			ai.getLogs().addLog(AILog.ERROR, message);
			ai.ops(message.length());
			return null;
		}
	},

	debugC(2) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			String message = LeekValueManager.getString(ai, parameters[0]);
			int color = ai.integer(parameters[1]);
			ai.getLogs().addLog(AILog.STANDARD, message, color);
			ai.ops(message.length());
			return null;
		}
	},
	jsonEncode(1) {
		@Override
		public Object run(AI leekIA, ILeekFunction function, Object... parameters) throws LeekRunException {
			return leekIA.jsonEncode(leekIA, parameters[0]);
		}
	},
	jsonDecode(1, new int[] { AI.STRING }) {
		@Override
		public Object run(AI leekIA, ILeekFunction function, Object... parameters) throws LeekRunException {
			return leekIA.jsonDecode((String) parameters[0]);
		}
	},
	getInstructionsCount(0) {
		@Override
		public Object run(AI leekIA, ILeekFunction function, Object... parameters) throws LeekRunException {
			return 0;
		}
	},
	color(3) {
		@Override
		public Object run(AI leekIA, ILeekFunction function, Object... parameters) throws LeekRunException {
			leekIA.addSystemLog(AILog.WARNING, Error.DEPRECATED_FUNCTION, new String[] { "color", "getColor" });
			return leekIA.color(parameters[0], parameters[1], parameters[2]);
		}
	},
	getColor(3) {
		@Override
		public Object run(AI leekIA, ILeekFunction function, Object... parameters) throws LeekRunException {
			return leekIA.color(parameters[0], parameters[1], parameters[2]);
		}
	},
	getRed(1, new int[] { AI.NUMBER }) {
		@Override
		public Object run(AI leekIA, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((((Number) parameters[0]).intValue()) >> 16) & 255;
		}
	},
	getGreen(1, new int[] { AI.NUMBER }) {
		@Override
		public Object run(AI leekIA, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((((Number) parameters[0]).intValue()) >> 8) & 255;
		}
	},
	getBlue(1, new int[] { AI.NUMBER }) {
		@Override
		public Object run(AI leekIA, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ((Number) parameters[0]).intValue() & 255;
		}
	},

	typeOf(1) {
		@Override
		public Object run(AI leekIA, ILeekFunction function, Object... parameters) throws LeekRunException {
			return LeekValueManager.getType(parameters[0]);
		}
	},
	trim(1, new int[] { AI.STRING }) {
		@Override
		public Object run(AI leekIA, ILeekFunction function, Object... parameters) throws LeekRunException {
			var s = (String) parameters[0];
			return s.trim();
		}
	},

	// getOperations(new CallableVersion[] { new CallableVersion(Type.INT, new Type[0]) }),
	getOperations(0) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			return ai.getOperations();
		}
	},

	clone(1, 2) {
		@Override
		public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
			// Clone one level by default
			int level = parameters[1] == null ? 1 : Math.max(0, ai.integer(parameters[1]));
			return LeekOperations.clone(ai, parameters[0], level);
		}
	}
	;

	private static String extraFunctions = null;

	private int mArguments;
	private int mArgumentsMin;
	private int mOperations = 1;
	protected VariableOperations mVariableOperations = null;
	private int[] parameters;
	private Type return_type;
	private CallableVersion[] versions;
	private boolean direct = false;

	LeekFunctions(int arguments) {
		mArgumentsMin = arguments;
		mArguments = arguments;
		// this.parameters = new int[0];
	}

	LeekFunctions(int arguments, int[] parameters) {
		mArgumentsMin = arguments;
		mArguments = arguments;
		this.parameters = parameters;
	}

	LeekFunctions(int arguments, int arguments_max, int[] parameters) {
		mArgumentsMin = arguments;
		mArguments = arguments_max;
		this.parameters = parameters;
	}

	LeekFunctions(int arguments, int arguments_max) {
		mArgumentsMin = arguments;
		mArguments = arguments_max;
		// this.parameters = new int[0];
	}

	LeekFunctions(CallableVersion[] versions) {
		this.versions = versions;
		this.direct = true;
		// this.parameters = new int[0];
		mArguments = this.versions[0].arguments.length;
	}

	LeekFunctions(Type return_type, CallableVersion[] versions, int[] parameters) {
		this.return_type = return_type;
		this.versions = versions;
		this.direct = true;
		this.parameters = parameters;
	}

	public boolean isDirect() {
		return direct;
	}

	public int[] getParameters() {
		return parameters;
	}

	public Type getReturnType() {
		return return_type;
	}

	public CallableVersion[] getVersions() {
		return versions;
	}

	public static void setExtraFunctions(String extraFunctions) {
		LeekFunctions.extraFunctions = extraFunctions;
	}

	public static int isFunction(String name) {
		ILeekFunction f = getValue(name);
		if (f == null)
			return -1;
		return f.getArguments();
	}

	public static boolean isExtraFunction(String name) {
		ILeekFunction f = getValue(name);
		return f != null && f.isExtra();
	}

	public static String getNamespace(String name) {
		return isExtraFunction(name) ? extraFunctions : "LeekFunctions";
	}

	@Override
	public int getArguments() {
		return mArguments;
	}

	@Override
	public String getNamespace() {
		return "LeekFunctions";
	}


	@Override
	public int getArgumentsMin() {
		return mArgumentsMin;
	}

	@Override
	public boolean isExtra() {
		return false;
	}

	public int getOperations() {
		return mOperations;
	}

	public boolean hasVariableOperations() {
		if (mVariableOperations == null) {
			mVariableOperations = Functions.getVariableOperations(name());
		}
		return mVariableOperations != null;
	}

	public static ILeekFunction getValue(String name) {
		try {
			return LeekFunctions.valueOf(name);
		} catch (Exception e) {}
		if (extraFunctions != null) {
			try {
				Class<?> extra = Class.forName(extraFunctions);
				for (Object func : extra.getEnumConstants()) {
					if (("" + func).equals(name)) {
						return (ILeekFunction) func;
					}
				}
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}

		}
		return null;
	}

	public static Object[] getExtraFunctions() {
		if (extraFunctions != null) {
			try {
				Class<?> extra = Class.forName(extraFunctions);
				return extra.getEnumConstants();
			} catch (ClassNotFoundException e) {
				return new Object[] {};
			}

		}
		return new Object[] {};
	}

	/*
	 * Lancer la fonction
	 */
	public Object run(AI ai, ILeekFunction function, Object... parameters) throws LeekRunException {
		return null;
	}

	public int cost() {
		return 1;
	}

	public void setOperations(int operations) {
		mOperations = operations;
	}

	public void addOperations(AI leekIA, ILeekFunction function, Object parameters[], Object retour) throws LeekRunException {
		leekIA.ops(getOperations());
	}
}
