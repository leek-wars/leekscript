package leekscript.runner;

import java.util.Random;
import java.util.regex.Pattern;

import leekscript.LeekLog;
import leekscript.Util;
import leekscript.functions.Functions;
import leekscript.functions.VariableOperations;
import leekscript.runner.values.AbstractLeekValue;
import leekscript.runner.values.ArrayLeekValue;
import leekscript.runner.values.BooleanLeekValue;
import leekscript.runner.values.DoubleLeekValue;
import leekscript.runner.values.FunctionLeekValue;
import leekscript.runner.values.IntLeekValue;
import leekscript.runner.values.NullLeekValue;
import leekscript.runner.values.StringLeekValue;

public enum LeekFunctions {
	// Fonctions mathématiques
	abs(1) {
		@Override
		public AbstractLeekValue run(AI leekIA, LeekFunctions function, AbstractLeekValue[] parameters, int count) throws Exception {
			return LeekValueManager.getLeekDoubleValue(Math.abs(parameters[0].getDouble(leekIA)));
		}

		@Override
		public int[] parameters() {
			return new int[] { NUMBER };
		}
	},
	min(2) {
		@Override
		public AbstractLeekValue run(AI leekIA, LeekFunctions function, AbstractLeekValue[] parameters, int count) throws Exception {
			return LeekValueManager.getLeekDoubleValue(Math.min(parameters[0].getDouble(leekIA), parameters[1].getDouble(leekIA)));
		}

		@Override
		public int[] parameters() {
			return new int[] { NUMBER, NUMBER };
		}
	},
	max(2) {
		@Override
		public AbstractLeekValue run(AI leekIA, LeekFunctions function, AbstractLeekValue[] parameters, int count) throws Exception {
			return LeekValueManager.getLeekDoubleValue(Math.max(parameters[0].getDouble(leekIA), parameters[1].getDouble(leekIA)));
		}

		@Override
		public int[] parameters() {
			return new int[] { NUMBER, NUMBER };
		}
	},
	cos(1) {
		@Override
		public AbstractLeekValue run(AI leekIA, LeekFunctions function, AbstractLeekValue[] parameters, int count) throws Exception {
			return new DoubleLeekValue(Math.cos(parameters[0].getDouble(leekIA)));
		}

		@Override
		public int[] parameters() {
			return new int[] { NUMBER };
		}
	},
	sin(1) {
		@Override
		public AbstractLeekValue run(AI leekIA, LeekFunctions function, AbstractLeekValue[] parameters, int count) throws Exception {
			return new DoubleLeekValue(Math.sin(parameters[0].getDouble(leekIA)));
		}

		@Override
		public int[] parameters() {
			return new int[] { NUMBER };
		}
	},
	tan(1) {
		@Override
		public AbstractLeekValue run(AI leekIA, LeekFunctions function, AbstractLeekValue[] parameters, int count) throws Exception {
			return new DoubleLeekValue(Math.tan(parameters[0].getDouble(leekIA)));
		}

		@Override
		public int[] parameters() {
			return new int[] { NUMBER };
		}
	},
	toRadians(1) {
		@Override
		public AbstractLeekValue run(AI leekIA, LeekFunctions function, AbstractLeekValue[] parameters, int count) throws Exception {
			return new DoubleLeekValue(parameters[0].getDouble(leekIA) * Math.PI / 180);
		}

		@Override
		public int[] parameters() {
			return new int[] { NUMBER };
		}
	},
	toDegrees(1) {
		@Override
		public AbstractLeekValue run(AI leekIA, LeekFunctions function, AbstractLeekValue[] parameters, int count) throws Exception {
			return new DoubleLeekValue(parameters[0].getDouble(leekIA) * 180 / Math.PI);
		}

		@Override
		public int[] parameters() {
			return new int[] { NUMBER };
		}
	},
	acos(1) {
		@Override
		public AbstractLeekValue run(AI leekIA, LeekFunctions function, AbstractLeekValue[] parameters, int count) throws Exception {
			return new DoubleLeekValue(Math.acos(parameters[0].getDouble(leekIA)));
		}

		@Override
		public int[] parameters() {
			return new int[] { NUMBER };
		}
	},
	asin(1) {
		@Override
		public AbstractLeekValue run(AI leekIA, LeekFunctions function, AbstractLeekValue[] parameters, int count) throws Exception {
			return new DoubleLeekValue(Math.asin(parameters[0].getDouble(leekIA)));
		}

		@Override
		public int[] parameters() {
			return new int[] { NUMBER };
		}
	},
	atan(1) {
		@Override
		public AbstractLeekValue run(AI leekIA, LeekFunctions function, AbstractLeekValue[] parameters, int count) throws Exception {
			return new DoubleLeekValue(Math.atan(parameters[0].getDouble(leekIA)));
		}

		@Override
		public int[] parameters() {
			return new int[] { NUMBER };
		}
	},
	atan2(2) {
		@Override
		public AbstractLeekValue run(AI leekIA, LeekFunctions function, AbstractLeekValue[] parameters, int count) throws Exception {
			return new DoubleLeekValue(Math.atan2(parameters[0].getDouble(leekIA), parameters[1].getDouble(leekIA)));
		}

		@Override
		public int[] parameters() {
			return new int[] { NUMBER, NUMBER };
		}
	},
	ceil(1) {
		@Override
		public AbstractLeekValue run(AI leekIA, LeekFunctions function, AbstractLeekValue[] parameters, int count) throws Exception {
			return LeekValueManager.getLeekIntValue((int) Math.ceil(parameters[0].getDouble(leekIA)));
		}

		@Override
		public int[] parameters() {
			return new int[] { NUMBER };
		}
	},
	floor(1) {
		@Override
		public AbstractLeekValue run(AI leekIA, LeekFunctions function, AbstractLeekValue[] parameters, int count) throws Exception {
			return LeekValueManager.getLeekIntValue((int) Math.floor(parameters[0].getDouble(leekIA)));
		}

		@Override
		public int[] parameters() {
			return new int[] { NUMBER };
		}
	},
	round(1) {
		@Override
		public AbstractLeekValue run(AI leekIA, LeekFunctions function, AbstractLeekValue[] parameters, int count) throws Exception {
			return LeekValueManager.getLeekIntValue((int) Math.round(parameters[0].getDouble(leekIA)));
		}

		@Override
		public int[] parameters() {
			return new int[] { NUMBER };
		}
	},
	sqrt(1) {
		@Override
		public AbstractLeekValue run(AI leekIA, LeekFunctions function, AbstractLeekValue[] parameters, int count) throws Exception {
			return LeekValueManager.getLeekDoubleValue(Math.sqrt(parameters[0].getDouble(leekIA)));
		}

		@Override
		public int[] parameters() {
			return new int[] { NUMBER };
		}
	},
	cbrt(1) {
		@Override
		public AbstractLeekValue run(AI leekIA, LeekFunctions function, AbstractLeekValue[] parameters, int count) throws Exception {
			return new DoubleLeekValue(Math.cbrt(parameters[0].getDouble(leekIA)));
		}

		@Override
		public int[] parameters() {
			return new int[] { NUMBER };
		}
	},
	log(1) {
		@Override
		public AbstractLeekValue run(AI leekIA, LeekFunctions function, AbstractLeekValue[] parameters, int count) throws Exception {
			return new DoubleLeekValue(Math.log(parameters[0].getDouble(leekIA)));
		}

		@Override
		public int[] parameters() {
			return new int[] { NUMBER };
		}
	},
	log10(1) {
		@Override
		public AbstractLeekValue run(AI leekIA, LeekFunctions function, AbstractLeekValue[] parameters, int count) throws Exception {
			return new DoubleLeekValue(Math.log10(parameters[0].getDouble(leekIA)));
		}

		@Override
		public int[] parameters() {
			return new int[] { NUMBER };
		}
	},
	exp(1) {
		@Override
		public AbstractLeekValue run(AI leekIA, LeekFunctions function, AbstractLeekValue[] parameters, int count) throws Exception {
			return new DoubleLeekValue(Math.exp(parameters[0].getDouble(leekIA)));
		}

		@Override
		public int[] parameters() {
			return new int[] { NUMBER };
		}
	},
	pow(2) {
		@Override
		public AbstractLeekValue run(AI leekIA, LeekFunctions function, AbstractLeekValue[] parameters, int count) throws Exception {
			return LeekValueManager.getLeekDoubleValue(Math.pow(parameters[0].getDouble(leekIA), parameters[1].getDouble(leekIA)));
		}

		@Override
		public int[] parameters() {
			return new int[] { NUMBER, NUMBER };
		}
	},
	rand(0) {
		@Override
		public AbstractLeekValue run(AI leekIA, LeekFunctions function, AbstractLeekValue[] parameters, int count) throws Exception {
			return new DoubleLeekValue(sRandom.nextDouble());
		}
	},
	randInt(2) {
		@Override
		public AbstractLeekValue run(AI leekIA, LeekFunctions function, AbstractLeekValue[] parameters, int count) throws Exception {
			int nb = parameters[0].getInt(leekIA);
			int nb1 = parameters[1].getInt(leekIA);
			if (nb > nb1)
				return LeekValueManager.getLeekIntValue(Util.getRandom(nb1, nb - 1));
			else
				return LeekValueManager.getLeekIntValue(Util.getRandom(nb, nb1 - 1));
		}

		@Override
		public int[] parameters() {
			return new int[] { NUMBER, NUMBER };
		}
	},
	randFloat(2) {
		@Override
		public AbstractLeekValue run(AI leekIA, LeekFunctions function, AbstractLeekValue[] parameters, int count) throws Exception {
			return new DoubleLeekValue(parameters[0].getDouble(leekIA) + sRandom.nextDouble() * (parameters[1].getDouble(leekIA) - parameters[0].getDouble(leekIA)));
		}

		@Override
		public int[] parameters() {
			return new int[] { NUMBER, NUMBER };
		}
	},
	hypot(2) {
		@Override
		public AbstractLeekValue run(AI leekIA, LeekFunctions function, AbstractLeekValue[] parameters, int count) throws Exception {
			return new DoubleLeekValue(Math.hypot(parameters[0].getDouble(leekIA), parameters[1].getDouble(leekIA)));
		}

		@Override
		public int[] parameters() {
			return new int[] { NUMBER, NUMBER };
		}
	},
	signum(1) {
		@Override
		public AbstractLeekValue run(AI leekIA, LeekFunctions function, AbstractLeekValue[] parameters, int count) throws Exception {
			return LeekValueManager.getLeekIntValue((int) Math.signum(parameters[0].getDouble(leekIA)));
		}

		@Override
		public int[] parameters() {
			return new int[] { NUMBER };
		}
	},
	string(1) {
		@Override
		public AbstractLeekValue run(AI leekIA, LeekFunctions function, AbstractLeekValue[] parameters, int count) throws Exception {
			return new StringLeekValue(parameters[0].getString(leekIA));
		}
	},
	// Fonctions string
	charAt(2) {

		@Override
		public AbstractLeekValue run(AI leekIA, LeekFunctions function, AbstractLeekValue[] parameters, int count) throws Exception {
			int pos = parameters[1].getInt(leekIA);
			String str = parameters[0].getString(leekIA);
			if (pos < 0 || pos >= str.length())
				return LeekValueManager.NULL;
			return new StringLeekValue(String.valueOf(str.charAt(pos)));
		}

		@Override
		public int[] parameters() {
			return new int[] { STRING, NUMBER };
		}
	},
	length(1) {
		@Override
		public AbstractLeekValue run(AI leekIA, LeekFunctions function, AbstractLeekValue[] parameters, int count) throws Exception {
			return LeekValueManager.getLeekIntValue(parameters[0].getString(leekIA).length());
		}

		@Override
		public int[] parameters() {
			return new int[] { STRING };
		}
	},
	substring(2, 3) {
		@Override
		public AbstractLeekValue run(AI leekIA, LeekFunctions function, AbstractLeekValue[] parameters, int count) throws Exception {
			if (verifyParameters(new int[] { STRING, NUMBER, NUMBER }, parameters)) {
				String string = parameters[0].getString(leekIA);
				int index = parameters[1].getInt(leekIA);
				int length = parameters[2].getInt(leekIA);
				if (string.length() <= index || index < 0) {
					return LeekValueManager.NULL;
				}
				if (index + length > string.length()) {
					return LeekValueManager.NULL;
				}
				return new StringLeekValue(string.substring(index, index + length));
			} else {
				int index = parameters[1].getInt(leekIA);
				if (parameters[0].getString(leekIA).length() <= index || index < 0) {
					return LeekValueManager.NULL;
				}
				return new StringLeekValue(parameters[0].getString(leekIA).substring(index));
			}
		}
		@Override
		public int[] parameters() {
			return new int[] { STRING, NUMBER, -1 };
		}
		@Override
		public void addOperations(AI leekIA, LeekFunctions function, AbstractLeekValue parameters[], AbstractLeekValue retour, int count) throws Exception {
			leekIA.addOperations(hasVariableOperations() ? mVariableOperations.getOperations(
					retour.getString(leekIA).length()) : 1);
		}
	},
	replace(3) {
		@Override
		public AbstractLeekValue run(AI leekIA, LeekFunctions function, AbstractLeekValue[] parameters, int count) throws Exception {
			return new StringLeekValue(parameters[0].getString(leekIA)
					.replaceAll(Pattern.quote(parameters[1]
							.getString(leekIA)), 
							parameters[2].getString(leekIA)));
		}

		@Override
		public int[] parameters() {
			return new int[] { STRING, STRING, STRING };
		}

		@Override
		public void addOperations(AI leekIA, LeekFunctions function, AbstractLeekValue parameters[], AbstractLeekValue retour, int count) throws Exception {
			leekIA.addOperations(hasVariableOperations() ? 
					mVariableOperations.getOperations(
							parameters[0].getString(leekIA).length()) : 1);
		}
	},
	indexOf(2, 3) {

		@Override
		public AbstractLeekValue run(AI leekIA, LeekFunctions function, AbstractLeekValue[] parameters, int count) throws Exception {
			if (verifyParameters(new int[] { STRING, STRING, NUMBER }, parameters)) {
				return LeekValueManager.getLeekIntValue(parameters[0].getString(leekIA)
						.indexOf(parameters[1].getString(leekIA), parameters[2].getInt(leekIA)));
			} else {
				return LeekValueManager.getLeekIntValue(parameters[0].getString(leekIA)
						.indexOf(parameters[1].getString(leekIA)));
			}
		}

		@Override
		public int[] parameters() {
			return new int[] { STRING, STRING, -1 };
		}

		@Override
		public void addOperations(AI leekIA, LeekFunctions function, AbstractLeekValue parameters[], AbstractLeekValue retour, int count) throws Exception {
			leekIA.addOperations(hasVariableOperations() ? mVariableOperations.getOperations((parameters[0].getString(leekIA).length())) : 1);
		}
	},
	split(2, 3) {
		@Override
		public AbstractLeekValue run(AI leekIA, LeekFunctions function, AbstractLeekValue[] parameters, int count) throws Exception {
			if (verifyParameters(new int[] { STRING, STRING, NUMBER }, parameters)) {
				String[] elements = parameters[0].getString(leekIA)
						.split(parameters[1].getString(leekIA), parameters[2].getInt(leekIA));
				ArrayLeekValue array = new ArrayLeekValue();
				for (short i = 0; i < elements.length; i++) {
					array.push(leekIA, new StringLeekValue(elements[i]));
				}
				return array;
			} else {
				String[] elements = parameters[0].getString(leekIA)
						.split(Pattern.quote(parameters[1].getString(leekIA)));
				ArrayLeekValue array = new ArrayLeekValue();
				for (short i = 0; i < elements.length; i++) {
					array.push(leekIA, new StringLeekValue(elements[i]));
				}
				return array;
			}
		}
		@Override
		public int[] parameters() {
			return new int[] { STRING, STRING, -1 };
		}
		@Override
		public void addOperations(AI leekIA, LeekFunctions function, AbstractLeekValue parameters[], AbstractLeekValue retour, int count) throws Exception {
			leekIA.addOperations(
					hasVariableOperations() ?
					mVariableOperations.getOperations(
							(int) (parameters[0].getString(leekIA).length() 
									* Math.log(parameters[1].getString(leekIA).length() + 1)))
					: 1);
			if (retour.isArray())
				leekIA.addOperations(retour.getArray().size());
		}
	},
	toLower(1) {
		@Override
		public AbstractLeekValue run(AI leekIA, LeekFunctions function, AbstractLeekValue[] parameters, int count) throws Exception {
			return new StringLeekValue(parameters[0].getString(leekIA).toLowerCase());
		}

		@Override
		public int[] parameters() {
			return new int[] { STRING };
		}

		@Override
		public void addOperations(AI leekIA, LeekFunctions function, AbstractLeekValue parameters[], AbstractLeekValue retour, int count) throws Exception {
			leekIA.addOperations(hasVariableOperations() ? 
					mVariableOperations.getOperations(retour.getString(leekIA).length()) : 1);
		}
	},
	toUpper(1) {
		@Override
		public AbstractLeekValue run(AI leekIA, LeekFunctions function, AbstractLeekValue[] parameters, int count) throws Exception {
			return new StringLeekValue(parameters[0].getString(leekIA).toUpperCase());
		}

		@Override
		public int[] parameters() {
			return new int[] { STRING };
		}

		@Override
		public void addOperations(AI leekIA, LeekFunctions function, AbstractLeekValue parameters[], AbstractLeekValue retour, int count) throws Exception {
			leekIA.addOperations(hasVariableOperations() 
					? mVariableOperations.getOperations(retour.getString(leekIA).length()) : 1);
		}
	},
	startsWith(2) {
		@Override
		public AbstractLeekValue run(AI leekIA, LeekFunctions function, AbstractLeekValue[] parameters, int count) throws Exception {
			return LeekValueManager.getLeekBooleanValue(
					parameters[0].getString(leekIA)
					.startsWith(parameters[1].getString(leekIA)));
		}

		@Override
		public int[] parameters() {
			return new int[] { STRING, STRING };
		}

		@Override
		public void addOperations(AI leekIA, LeekFunctions function, AbstractLeekValue parameters[], AbstractLeekValue retour, int count) throws Exception {
			if (parameters[0].getString(leekIA).length() > parameters[1].getString(leekIA).length()) {
				leekIA.addOperations(hasVariableOperations() ? 
						mVariableOperations.getOperations((parameters[1].getString(leekIA).length())) : 1);
			} else
				leekIA.addOperations(1);
		}
	},
	endsWith(2) {
		@Override
		public AbstractLeekValue run(AI leekIA, LeekFunctions function, AbstractLeekValue[] parameters, int count) throws Exception {
			return LeekValueManager.getLeekBooleanValue(parameters[0].getString(leekIA)
					.endsWith(parameters[1].getString(leekIA)));
		}

		@Override
		public int[] parameters() {
			return new int[] { STRING, STRING };
		}

		@Override
		public void addOperations(AI leekIA, LeekFunctions function, AbstractLeekValue parameters[], AbstractLeekValue retour, int count) throws Exception {
			if (parameters[0].getString(leekIA).length() > parameters[1].getString(leekIA).length()) {
				leekIA.addOperations(hasVariableOperations() ? 
						mVariableOperations.getOperations((parameters[1].getString(leekIA).length())) : 1);
			} else
				leekIA.addOperations(1);
		}
	},
	contains(2) {
		@Override
		public AbstractLeekValue run(AI leekIA, LeekFunctions function, AbstractLeekValue[] parameters, int count) throws Exception {
			String haystack = parameters[0].getString(leekIA);
			String needle = parameters[1].getString(leekIA);
			if (needle.length() > haystack.length()) {
				return LeekValueManager.getLeekBooleanValue(false);
			}
			return LeekValueManager.getLeekBooleanValue(haystack.contains(needle));
		}
		@Override
		public int[] parameters() {
			return new int[] { STRING, STRING };
		}
		@Override
		public void addOperations(AI leekIA, LeekFunctions function, AbstractLeekValue parameters[], AbstractLeekValue retour, int count) throws Exception {
			leekIA.addOperations(hasVariableOperations() ? 
					mVariableOperations.getOperations(
							(int) (parameters[0].getString(leekIA).length() 
									* Math.log(parameters[1].getString(leekIA).length() + 1))) : 1);
		}
	},
	number(1) {
		@Override
		public AbstractLeekValue run(AI leekIA, LeekFunctions function, AbstractLeekValue[] parameters, int count) throws Exception {
			if (parameters[0].getType() == AbstractLeekValue.NUMBER)
				return LeekOperations.clone(leekIA, parameters[0].getValue());
			if (parameters[0].getType() != AbstractLeekValue.STRING)
				return LeekValueManager.NULL;
			try {
				if (parameters[0].getString(leekIA).contains(".")) {
					return LeekValueManager.getLeekDoubleValue(Double.parseDouble(parameters[0].getString(leekIA)));
				} else {
					return LeekValueManager.getLeekIntValue(Integer.parseInt(parameters[0].getString(leekIA)));
				}
			} catch (Exception e) {
				return LeekValueManager.NULL;
			}
		}

		@Override
		public int[] parameters() {
			return new int[] { -1 };
		}
	},
	// Fonctions array
	remove(2) {
		@Override
		public AbstractLeekValue run(AI leekIA, LeekFunctions function, AbstractLeekValue[] parameters, int count) throws Exception {
			return parameters[0].getArray().remove(leekIA, parameters[1].getInt(leekIA));
		}

		@Override
		public int[] parameters() {
			return new int[] { ARRAY, NUMBER };
		}

		@Override
		public void addOperations(AI leekIA, LeekFunctions function, AbstractLeekValue parameters[], AbstractLeekValue retour, int count) throws Exception {
			if (parameters[1].getInt(leekIA) >= 0 && parameters[1].getInt(leekIA) < parameters[0].getArray().size()) {
				leekIA.addOperations(hasVariableOperations() ? mVariableOperations.getOperations(parameters[1].getInt(leekIA) + 1) : 1);
			} else
				leekIA.addOperations(1);
		}
	},
	count(1) {
		@Override
		public AbstractLeekValue run(AI leekIA, LeekFunctions function, AbstractLeekValue[] parameters, int count) throws Exception {
			return LeekValueManager.getLeekIntValue(parameters[0].getArray().size());
		}

		@Override
		public int[] parameters() {
			return new int[] { ARRAY };
		}
	},
	join(2) {
		@Override
		public AbstractLeekValue run(AI leekIA, LeekFunctions function, AbstractLeekValue[] parameters, int count) throws Exception {
			return new StringLeekValue(parameters[0].getArray().join(leekIA, parameters[1].getString(leekIA)));
		}

		@Override
		public int[] parameters() {
			return new int[] { ARRAY, STRING };
		}

		@Override
		public void addOperations(AI leekIA, LeekFunctions function, AbstractLeekValue parameters[], AbstractLeekValue retour, int count) throws Exception {
			leekIA.addOperations(hasVariableOperations() ? mVariableOperations.getOperations(retour.getString(leekIA).length() + 1) : 1);
		}
	},
	insert(3) {
		@Override
		public AbstractLeekValue run(AI leekIA, LeekFunctions function, AbstractLeekValue[] parameters, int count) throws Exception {
			parameters[0].getArray().insert(leekIA, LeekOperations.clone(leekIA, parameters[1]), parameters[2].getInt(leekIA));
			return LeekValueManager.NULL;
		}

		@Override
		public int[] parameters() {
			return new int[] { ARRAY, -1, NUMBER };
		}

		@Override
		public void addOperations(AI leekIA, LeekFunctions function, AbstractLeekValue parameters[], AbstractLeekValue retour, int count) throws Exception {
			if (parameters[2].getInt(leekIA) >= 0 && parameters[2].getInt(leekIA) < parameters[0].getArray().size()) {
				leekIA.addOperations(hasVariableOperations() ? mVariableOperations.getOperations(parameters[1].getInt(leekIA) + 1) : 1);
			} else
				leekIA.addOperations(1);
		}
	},
	push(2) {
		@Override
		public AbstractLeekValue run(AI leekIA, LeekFunctions function, AbstractLeekValue[] parameters, int count) throws Exception {
			parameters[0].getArray().push(leekIA, LeekOperations.clone(leekIA, parameters[1]));
			return LeekValueManager.NULL;
		}

		@Override
		public int[] parameters() {
			return new int[] { ARRAY, -1 };
		}
	},
	unshift(2) {
		@Override
		public AbstractLeekValue run(AI leekIA, LeekFunctions function, AbstractLeekValue[] parameters, int count) throws Exception {
			parameters[0].getArray().insert(leekIA, LeekOperations.clone(leekIA, parameters[1]), 0);
			return LeekValueManager.NULL;
		}

		@Override
		public int[] parameters() {
			return new int[] { ARRAY, -1 };
		}
	},
	shift(1) {
		@Override
		public AbstractLeekValue run(AI leekIA, LeekFunctions function, AbstractLeekValue[] parameters, int count) throws Exception {
			if (parameters[0].getArray().size() > 0) {
				AbstractLeekValue v = parameters[0].getArray().start().getValue();
				parameters[0].getArray().remove(leekIA, 0);
				return v;
			}
			return LeekValueManager.NULL;
		}

		@Override
		public int[] parameters() {
			return new int[] { ARRAY };
		}
	},
	pop(1) {
		@Override
		public AbstractLeekValue run(AI leekIA, LeekFunctions function, AbstractLeekValue[] parameters, int count) throws Exception {
			if (parameters[0].getArray().size() > 0) {
				AbstractLeekValue v = parameters[0].getArray().end().getValue();
				parameters[0].getArray().remove(leekIA, parameters[0].getArray().size() - 1);
				return v;
			}
			return LeekValueManager.NULL;
		}

		@Override
		public int[] parameters() {
			return new int[] { ARRAY };
		}
	},
	removeElement(2) {
		@Override
		public AbstractLeekValue run(AI leekIA, LeekFunctions function, AbstractLeekValue[] parameters, int count) throws Exception {
			parameters[0].getArray().removeObject(leekIA, parameters[1]);
			return LeekValueManager.NULL;
		}

		@Override
		public int[] parameters() {
			return new int[] { ARRAY, -1 };
		}

		@Override
		public void addOperations(AI leekIA, LeekFunctions function, AbstractLeekValue parameters[], AbstractLeekValue retour, int count) throws Exception {
			leekIA.addOperations(hasVariableOperations() ? 
					mVariableOperations.getOperations(parameters[0].getArray().size() + 1) : 1);
		}
	},
	removeByKey(2) {
		@Override
		public AbstractLeekValue run(AI leekIA, LeekFunctions function, AbstractLeekValue[] parameters, int count) throws Exception {
			parameters[0].getArray().removeByKey(leekIA, parameters[1]);
			return LeekValueManager.NULL;
		}

		@Override
		public int[] parameters() {
			return new int[] { ARRAY, -1 };
		}
	},
	removeKey(2) {
		@Override
		public AbstractLeekValue run(AI leekIA, LeekFunctions function, AbstractLeekValue[] parameters, int count) throws Exception {
			parameters[0].getArray().removeByKey(leekIA, parameters[1]);
			return LeekValueManager.NULL;
		}

		@Override
		public int[] parameters() {
			return new int[] { ARRAY, -1 };
		}
	},
	sort(1, 2) {
		@Override
		public AbstractLeekValue run(AI leekIA, LeekFunctions function, AbstractLeekValue[] parameters, int count) throws Exception {
			int type = LeekValueComparator.SortComparator.SORT_ASC;
			if (parameters[1].getBoolean())
				type = LeekValueComparator.SortComparator.SORT_DESC;
			parameters[0].getArray().sort(leekIA, type);
			return LeekValueManager.NULL;
		}

		@Override
		public int[] parameters() {
			return new int[] { ARRAY, -1 };
		}

		@Override
		public void addOperations(AI leekIA, LeekFunctions function, AbstractLeekValue parameters[], AbstractLeekValue retour, int count) throws Exception {
			leekIA.addOperations(hasVariableOperations() ? mVariableOperations.getOperations(parameters[0].getArray().size() + 1) : 1);
		}
	},
	assocSort(1, 2) {
		@Override
		public AbstractLeekValue run(AI leekIA, LeekFunctions function, AbstractLeekValue[] parameters, int count) throws Exception {
			int type = PhpArray.ASC_A;
			if (parameters[1].getBoolean())
				type = PhpArray.DESC_A;
			parameters[0].getArray().sort(leekIA, type);
			return LeekValueManager.NULL;
		}

		@Override
		public int[] parameters() {
			return new int[] { ARRAY, -1 };
		}

		@Override
		public void addOperations(AI leekIA, LeekFunctions function, AbstractLeekValue parameters[], AbstractLeekValue retour, int count) throws Exception {
			leekIA.addOperations(hasVariableOperations() ? mVariableOperations.getOperations(parameters[0].getArray().size() + 1) : 1);
		}
	},
	keySort(1, 2) {
		@Override
		public AbstractLeekValue run(AI leekIA, LeekFunctions function, AbstractLeekValue[] parameters, int count) throws Exception {

			int type = PhpArray.ASC_K;
			if (parameters[1].getBoolean())
				type = PhpArray.DESC_K;
			parameters[0].getArray().sort(leekIA, type);
			return LeekValueManager.NULL;
		}

		@Override
		public int[] parameters() {
			return new int[] { ARRAY, -1 };
		}

		@Override
		public void addOperations(AI leekIA, LeekFunctions function, AbstractLeekValue parameters[], AbstractLeekValue retour, int count) throws Exception {
			leekIA.addOperations(hasVariableOperations() ? mVariableOperations.getOperations(parameters[0].getArray().size() + 1) : 1);
		}
	},
	shuffle(1) {
		@Override
		public AbstractLeekValue run(AI leekIA, LeekFunctions function, AbstractLeekValue[] parameters, int count) throws Exception {
			parameters[0].getArray().shuffle(leekIA);
			return LeekValueManager.NULL;
		}

		@Override
		public int[] parameters() {
			return new int[] { ARRAY };
		}

		@Override
		public void addOperations(AI leekIA, LeekFunctions function, AbstractLeekValue parameters[], AbstractLeekValue retour, int count) throws Exception {
			leekIA.addOperations(hasVariableOperations() ? mVariableOperations.getOperations(parameters[0].getArray().size() + 1) : 1);
		}
	},
	search(2, 3) {
		@Override
		public AbstractLeekValue run(AI leekIA, LeekFunctions function, AbstractLeekValue[] parameters, int count) throws Exception {
			if (verifyParameters(new int[] { ARRAY, -1, NUMBER }, parameters)) {
				return parameters[0].getArray().search(leekIA, parameters[1], parameters[2].getInt(leekIA));
			} else {
				return parameters[0].getArray().search(leekIA, parameters[1], 0);
			}
		}

		@Override
		public int[] parameters() {
			return new int[] { ARRAY, -1, -1 };
		}

		@Override
		public void addOperations(AI leekIA, LeekFunctions function, AbstractLeekValue parameters[], AbstractLeekValue retour, int count) throws Exception {
			leekIA.addOperations(hasVariableOperations() ? mVariableOperations.getOperations(parameters[0].getArray().size() + 1) : 1);
		}
	},
	inArray(2) {
		@Override
		public AbstractLeekValue run(AI leekIA, LeekFunctions function, AbstractLeekValue[] parameters, int count) throws Exception {
			return LeekValueManager.getLeekBooleanValue(parameters[0].getArray().contains(leekIA, parameters[1]));
		}

		@Override
		public int[] parameters() {
			return new int[] { ARRAY, -1 };
		}

		@Override
		public void addOperations(AI leekIA, LeekFunctions function, AbstractLeekValue parameters[], AbstractLeekValue retour, int count) throws Exception {
			leekIA.addOperations(hasVariableOperations() ? mVariableOperations.getOperations(parameters[0].getArray().size() + 1) : 1);
		}
	},
	reverse(1) {
		@Override
		public AbstractLeekValue run(AI leekIA, LeekFunctions function, AbstractLeekValue[] parameters, int count) throws Exception {
			parameters[0].getArray().reverse(leekIA);
			return LeekValueManager.NULL;
		}

		@Override
		public int[] parameters() {
			return new int[] { ARRAY };
		}

		@Override
		public void addOperations(AI leekIA, LeekFunctions function, AbstractLeekValue parameters[], AbstractLeekValue retour, int count) throws Exception {
			leekIA.addOperations(hasVariableOperations() ? mVariableOperations.getOperations(parameters[0].getArray().size() + 1) : 1);
		}
	},
	arrayMin(1) {
		@Override
		public AbstractLeekValue run(AI leekIA, LeekFunctions function, AbstractLeekValue[] parameters, int count) throws Exception {
			AbstractLeekValue max_c = null;
			LeekValueComparator.SortComparator comp = new LeekValueComparator.SortComparator(leekIA, LeekValueComparator.SortComparator.SORT_ASC);
			for (AbstractLeekValue val : parameters[0].getArray()) {
				if (max_c == null)
					max_c = val.getValue();
				else if (comp.compare(val.getValue(), max_c) == -1)
					max_c = val.getValue();
			}
			if (max_c == null)
				return LeekValueManager.NULL;
			else
				return LeekOperations.clone(leekIA, max_c);
		}

		@Override
		public int[] parameters() {
			return new int[] { ARRAY };
		}
	},
	arrayMax(1) {
		@Override
		public AbstractLeekValue run(AI leekIA, LeekFunctions function, AbstractLeekValue[] parameters, int count) throws Exception {
			AbstractLeekValue min_c = null;
			LeekValueComparator.SortComparator mincomp = new LeekValueComparator.SortComparator(leekIA, LeekValueComparator.SortComparator.SORT_ASC);
			for (AbstractLeekValue val : parameters[0].getArray()) {
				if (min_c == null)
					min_c = val.getValue();
				else if (mincomp.compare(val.getValue(), min_c) == 1)
					min_c = val.getValue();
			}
			if (min_c == null)
				return LeekValueManager.NULL;
			else
				return LeekOperations.clone(leekIA, min_c);
		}

		@Override
		public int[] parameters() {
			return new int[] { ARRAY };
		}

		@Override
		public void addOperations(AI leekIA, LeekFunctions function, AbstractLeekValue parameters[], AbstractLeekValue retour, int count) throws Exception {
			leekIA.addOperations(hasVariableOperations() ? mVariableOperations.getOperations(parameters[0].getArray().size() + 1) : 1);
		}
	},
	sum(1) {
		@Override
		public AbstractLeekValue run(AI leekIA, LeekFunctions function, AbstractLeekValue[] parameters, int count) throws Exception {
			double somme = 0;
			for (AbstractLeekValue val : parameters[0].getArray()) {
				somme += val.getDouble(leekIA);
			}
			return LeekValueManager.getLeekDoubleValue(somme);
		}

		@Override
		public int[] parameters() {
			return new int[] { ARRAY };
		}

		@Override
		public void addOperations(AI leekIA, LeekFunctions function, AbstractLeekValue parameters[], AbstractLeekValue retour, int count) throws Exception {
			leekIA.addOperations(hasVariableOperations() ? mVariableOperations.getOperations(parameters[0].getArray().size() + 1) : 1);
		}
	},
	average(1) {
		@Override
		public AbstractLeekValue run(AI leekIA, LeekFunctions function, AbstractLeekValue[] parameters, int count) throws Exception {
			double average = 0;
			for (AbstractLeekValue val : parameters[0].getArray()) {
				average += val.getDouble(leekIA);
			}
			if (average == 0)
				return LeekValueManager.getLeekIntValue(0);
			return LeekValueManager.getLeekDoubleValue(average / parameters[0].getArray().size());
		}

		@Override
		public int[] parameters() {
			return new int[] { ARRAY };
		}

		@Override
		public void addOperations(AI leekIA, LeekFunctions function, AbstractLeekValue parameters[], AbstractLeekValue retour, int count) throws Exception {
			leekIA.addOperations(hasVariableOperations() ? mVariableOperations.getOperations(parameters[0].getArray().size() + 1) : 1);
		}
	},
	fill(2, 3) {
		@Override
		public AbstractLeekValue run(AI leekIA, LeekFunctions function, AbstractLeekValue[] parameters, int count) throws Exception {
			ArrayLeekValue array = parameters[0].getArray();
			int size = array.size();
			if (isType(parameters[2], NUMBER))
				size = parameters[2].getInt(leekIA);
			AbstractLeekValue copy = LeekOperations.clone(leekIA, parameters[1].getValue());
			for (int i = 0; i < size; i++) {
				array.get(leekIA, i).set(leekIA, copy);
				leekIA.addOperations(3);
			}
			return LeekValueManager.NULL;
		}

		@Override
		public int[] parameters() {
			return new int[] { ARRAY, -1, -1 };
		}

		@Override
		public void addOperations(AI leekIA, LeekFunctions function, AbstractLeekValue parameters[], AbstractLeekValue retour, int count) throws Exception {}
	},
	isEmpty(1) {
		@Override
		public AbstractLeekValue run(AI leekIA, LeekFunctions function, AbstractLeekValue[] parameters, int count) throws Exception {
			return LeekValueManager.getLeekBooleanValue(parameters[0].getArray().size() == 0);
		}

		@Override
		public int[] parameters() {
			return new int[] { ARRAY };
		}
	},
	subArray(3) {
		@Override
		public AbstractLeekValue run(AI leekIA, LeekFunctions function, AbstractLeekValue[] parameters, int count) throws Exception {
			ArrayLeekValue array = parameters[0].getArray();
			int start = parameters[1].getInt(leekIA);
			int end = parameters[2].getInt(leekIA);
			if (start < 0 || end < start || end >= array.size())
				return LeekValueManager.NULL;
			ArrayLeekValue retour = new ArrayLeekValue();
			int i = 0;
			for (AbstractLeekValue val : array) {
				if (i >= start && i <= end) {
					retour.push(leekIA, LeekOperations.clone(leekIA, val.getValue()));
					leekIA.addOperations(1);
				}
				i++;
			}
			return retour;
		}

		@Override
		public int[] parameters() {
			return new int[] { ARRAY, NUMBER, NUMBER };
		}

		@Override
		public void addOperations(AI leekIA, LeekFunctions function, AbstractLeekValue parameters[], AbstractLeekValue retour, int count) throws Exception {}
	},
	pushAll(2) {
		@Override
		public AbstractLeekValue run(AI leekIA, LeekFunctions function, AbstractLeekValue[] parameters, int count) throws Exception {
			ArrayLeekValue array = parameters[0].getArray();
			ArrayLeekValue source = LeekOperations.clone(leekIA, parameters[1]).getArray();
			for (AbstractLeekValue value : source) {
				array.push(leekIA, value.getValue());
				leekIA.addOperations(1);
			}
			return LeekValueManager.NULL;
		}

		@Override
		public int[] parameters() {
			return new int[] { ARRAY, ARRAY };
		}

		@Override
		public void addOperations(AI leekIA, LeekFunctions function, AbstractLeekValue parameters[], AbstractLeekValue retour, int count) throws Exception {}
	},
	assocReverse(1) {
		@Override
		public AbstractLeekValue run(AI leekIA, LeekFunctions function, AbstractLeekValue[] parameters, int count) throws Exception {
			parameters[0].getArray().assocReverse();
			return LeekValueManager.NULL;
		}

		@Override
		public int[] parameters() {
			return new int[] { ARRAY };
		}

		@Override
		public void addOperations(AI leekIA, LeekFunctions function, AbstractLeekValue parameters[], AbstractLeekValue retour, int count) throws Exception {
			leekIA.addOperations(hasVariableOperations() ? mVariableOperations.getOperations(parameters[0].getArray().size() + 1) : 1);
		}
	},
	arrayMap(2) {
		@Override
		public AbstractLeekValue run(AI leekIA, LeekFunctions function, AbstractLeekValue[] parameters, int count) throws Exception {
			return leekIA.arrayMap(parameters[0].getArray(), parameters[1]);
		}

		@Override
		public int[] parameters() {
			return new int[] { ARRAY, FUNCTION };
		}
	},
	arrayFilter(2) {
		@Override
		public AbstractLeekValue run(AI leekIA, LeekFunctions function, AbstractLeekValue[] parameters, int count) throws Exception {
			return leekIA.arrayFilter(parameters[0].getArray(), parameters[1]);
		}

		@Override
		public int[] parameters() {
			return new int[] { ARRAY, FUNCTION };
		}
	},
	arrayFlatten(1, 2) {
		@Override
		public AbstractLeekValue run(AI leekIA, LeekFunctions function, AbstractLeekValue[] parameters, int count) throws Exception {
			int maxDepth = isType(parameters[1], NUMBER) ? parameters[1].getInt(leekIA) : 1;
			ArrayLeekValue retour = new ArrayLeekValue();
			leekIA.arrayFlatten(parameters[0].getArray(), retour, maxDepth);
			return retour;
		}

		@Override
		public int[] parameters() {
			return new int[] { ARRAY, -1 };
		}

		@Override
		public void addOperations(AI leekIA, LeekFunctions function, AbstractLeekValue parameters[], AbstractLeekValue retour, int count) throws Exception {
			leekIA.addOperations(hasVariableOperations() ? mVariableOperations.getOperations(retour.getArray().size() + 1) : 1);
		}
	},
	arrayFoldLeft(2, 3) {
		@Override
		public AbstractLeekValue run(AI leekIA, LeekFunctions function, AbstractLeekValue[] parameters, int count) throws Exception {
			return leekIA.arrayFoldLeft(parameters[0].getArray(), parameters[1], parameters[2]);
		}

		@Override
		public int[] parameters() {
			return new int[] { ARRAY, FUNCTION, -1 };
		}
	},
	arrayFoldRight(2, 3) {
		@Override
		public AbstractLeekValue run(AI leekIA, LeekFunctions function, AbstractLeekValue[] parameters, int count) throws Exception {
			return leekIA.arrayFoldRight(parameters[0].getArray(), parameters[1], parameters[2]);
		}

		@Override
		public int[] parameters() {
			return new int[] { ARRAY, FUNCTION, -1 };
		}
	},
	arrayPartition(2) {
		@Override
		public AbstractLeekValue run(AI leekIA, LeekFunctions function, AbstractLeekValue[] parameters, int count) throws Exception {
			return leekIA.arrayPartition(parameters[0].getArray(), parameters[1]);
		}

		@Override
		public int[] parameters() {
			return new int[] { ARRAY, FUNCTION };
		}
	},
	arrayIter(2) {
		@Override
		public AbstractLeekValue run(AI leekIA, LeekFunctions function, AbstractLeekValue[] parameters, int count) throws Exception {
			return leekIA.arrayIter(parameters[0].getArray(), parameters[1]);
		}

		@Override
		public int[] parameters() {
			return new int[] { ARRAY, FUNCTION };
		}
	},
	arrayConcat(2) {
		@Override
		public AbstractLeekValue run(AI leekIA, LeekFunctions function, AbstractLeekValue[] parameters, int count) throws Exception {
			return LeekOperations.add(leekIA, parameters[0], parameters[1]);
		}

		@Override
		public int[] parameters() {
			return new int[] { ARRAY, ARRAY };
		}
	},
	arraySort(2) {
		@Override
		public AbstractLeekValue run(AI leekIA, LeekFunctions function, AbstractLeekValue[] parameters, int count) throws Exception {
			return leekIA.arraySort(parameters[0].getArray(), parameters[1]);
		}

		@Override
		public int[] parameters() {
			return new int[] { ARRAY, FUNCTION };
		}
	},
	debug(1) {
		@Override
		public AbstractLeekValue run(AI leekIA, LeekFunctions function, AbstractLeekValue[] parameters, int count) throws Exception {
			String p = parameters[0].getString(leekIA);
			leekIA.getLeekLog().addLog(LeekLog.STANDARD, p);
			leekIA.addOperations(p.length());
			return LeekValueManager.NULL;
		}
	},
	debugW(1) {
		@Override
		public AbstractLeekValue run(AI leekIA, LeekFunctions function, AbstractLeekValue[] parameters, int count) throws Exception {
			String p = parameters[0].getString(leekIA);
			leekIA.getLeekLog().addLog(LeekLog.WARNING, p);
			leekIA.addOperations(p.length());
			return LeekValueManager.NULL;
		}
	},
	debugE(1) {
		@Override
		public AbstractLeekValue run(AI leekIA, LeekFunctions function, AbstractLeekValue[] parameters, int count) throws Exception {
			String p = parameters[0].getString(leekIA);
			leekIA.getLeekLog().addLog(LeekLog.ERROR, p);
			leekIA.addOperations(p.length());
			return LeekValueManager.NULL;
		}
	},
	debugC(2) {
		@Override
		public AbstractLeekValue run(AI leekIA, LeekFunctions function, AbstractLeekValue[] parameters, int count) throws Exception {
			String message = parameters[0].getString(leekIA);
			int color = parameters[1].getInt(leekIA);
			leekIA.getLeekLog().addLog(LeekLog.STANDARD, message, color);
			leekIA.addOperations(message.length());
			return LeekValueManager.NULL;
		}
	},
	jsonEncode(1) {
		@Override
		public AbstractLeekValue run(AI leekIA, LeekFunctions function, AbstractLeekValue[] parameters, int count) throws Exception {
			return leekIA.jsonEncode(leekIA, parameters[0]);
		}
	},
	jsonDecode(1) {
		@Override
		public AbstractLeekValue run(AI leekIA, LeekFunctions function, AbstractLeekValue[] parameters, int count) throws Exception {
			return leekIA.jsonDecode(parameters[0].getString(leekIA));
		}

		@Override
		public int[] parameters() {
			return new int[] { STRING };
		}
	},
	getInstructionsCount(0) {
		@Override
		public AbstractLeekValue run(AI leekIA, LeekFunctions function, AbstractLeekValue[] parameters, int count) throws Exception {
			return LeekValueManager.getLeekIntValue(0);
		}
	},
	color(3) {
		@Override
		public AbstractLeekValue run(AI leekIA, LeekFunctions function, AbstractLeekValue[] parameters, int count) throws Exception {
			leekIA.addOperations(AI.ERROR_LOG_COST);
			leekIA.addSystemLog(LeekLog.WARNING, LeekLog.DEPRECATED_FUNCTION, new String[] { "color", "getColor" });
			return leekIA.color(parameters[0], parameters[1], parameters[2]);
		}
	},
	getColor(3) {
		@Override
		public AbstractLeekValue run(AI leekIA, LeekFunctions function, AbstractLeekValue[] parameters, int count) throws Exception {
			return leekIA.color(parameters[0], parameters[1], parameters[2]);
		}
	},
	getRed(1) {
		@Override
		public AbstractLeekValue run(AI leekIA, LeekFunctions function, AbstractLeekValue[] parameters, int count) throws Exception {
			return LeekValueManager.getLeekIntValue(((parameters[0].getInt(leekIA)) >> 16) & 255);
		}

		@Override
		public int[] parameters() {
			return new int[] { NUMBER };
		}
	},
	getGreen(1) {
		@Override
		public AbstractLeekValue run(AI leekIA, LeekFunctions function, AbstractLeekValue[] parameters, int count) throws Exception {
			return LeekValueManager.getLeekIntValue(((parameters[0].getInt(leekIA)) >> 8) & 255);
		}

		@Override
		public int[] parameters() {
			return new int[] { NUMBER };
		}
	},
	getBlue(1) {
		@Override
		public AbstractLeekValue run(AI leekIA, LeekFunctions function, AbstractLeekValue[] parameters, int count) throws Exception {
			return LeekValueManager.getLeekIntValue(parameters[0].getInt(leekIA) & 255);
		}

		@Override
		public int[] parameters() {
			return new int[] { NUMBER };
		}
	},
	
	typeOf(1) {
		@Override
		public AbstractLeekValue run(AI leekIA, LeekFunctions function, AbstractLeekValue[] parameters, int count) throws Exception {
			return LeekValueManager.getLeekIntValue(leekIA.typeOf(parameters[0]));
		}
	},
	trim(1) {
		@Override
		public AbstractLeekValue run(AI leekIA, LeekFunctions function, AbstractLeekValue[] parameters, int count) throws Exception {
			return new StringLeekValue(parameters[0].getString(leekIA).trim());
		}

		@Override
		public int[] parameters() {
			return new int[] { STRING };
		}
	},
	getOperations(0) {
		@Override
		public AbstractLeekValue run(AI leekIA, LeekFunctions function, AbstractLeekValue[] parameters, int count) throws Exception {
			return LeekValueManager.getLeekIntValue((int) leekIA.getOperations());
		}
	};

	private static Random sRandom = new Random();

	private int mArguments;
	private int mArgumentsMin;
	private Integer mOperations = null;
	protected VariableOperations mVariableOperations = null;

	public static final int DOUBLE = 1;
	public static final int INT = 2;
	public static final int BOOLEAN = 3;
	public static final int STRING = 4;
	public static final int NULL = 5;
	public static final int ARRAY = 6;
	public static final int NUMBER = 7;
	public static final int FUNCTION = 8;

	LeekFunctions(int arguments) {
		mArgumentsMin = arguments;
		mArguments = arguments;
	}

	LeekFunctions(int arguments, int arguments_max) {
		mArgumentsMin = arguments;
		mArguments = arguments_max;
	}

	public static int isFunction(String name) {
		LeekFunctions f = getValue(name);
		if (f == null)
			return -1;
		return f.mArguments;
	}

	public int getArguments() {
		return mArguments;
	}

	public int getArgumentsMin() {
		return mArgumentsMin;
	}

	public int getOperations() {
		if (mOperations == null) {
			if (!Functions.isReady()) {
				return 0;
			}
			mOperations = Functions.getOperations(this.name());
			if (mOperations <= 0) {
				mOperations = 1;
			}
		}
		return mOperations;
	}

	public boolean hasVariableOperations() {
		if (mVariableOperations == null) {
			mVariableOperations = Functions.getVariableOperations(name());
		}
		return mVariableOperations != null;
	}

	public static LeekFunctions getValue(String name) {
		for (LeekFunctions func : LeekFunctions.values()) {
			if (func.name().equals(name))
				return func;
			// return IAFunctions.valueOf(name);
		}
		return null;
	}

	/*
	 * Lancer la fonction
	 */
	public abstract AbstractLeekValue run(AI ai, LeekFunctions function, AbstractLeekValue parameters[], int count) throws Exception;

	public int[] parameters() {
		return null;
	}

	public int cost() {
		return 1;
	}

	public void addOperations(AI leekIA, LeekFunctions function, AbstractLeekValue parameters[], AbstractLeekValue retour, int count) throws Exception {
		leekIA.addOperations(getOperations());
	}

	public static AbstractLeekValue executeFunction(AI leekIA, LeekFunctions function, AbstractLeekValue parameters[], int count) throws Exception {

		// Vérification parametres
		int[] types = function.parameters();
		if (types == null || verifyParameters(types, parameters)) {
			AbstractLeekValue retour = function.run(leekIA, function, parameters, count);
			function.addOperations(leekIA, function, parameters, retour, count);
			return retour;
		} else {
			// Message d'erreur
			String ret = AbstractLeekValue.getParamString(parameters);
			leekIA.addOperations(AI.ERROR_LOG_COST);
			leekIA.addSystemLog(LeekLog.ERROR, LeekLog.UNKNOWN_FUNCTION, new String[] { function + "(" + ret + ")" });
			return LeekValueManager.NULL;
		}
		// throw new LeekRunException(LeekRunException.UNKNOWN_FUNCTION);
	}

	/*
	 * private static void parametersMatch(int[] types, IAAbstractValue[]
	 * parameters, IAFunctions function) throws IARunException {
	 * if(!verifyParameters(types, parameters)) throw new
	 * IARunException(IARunException.FUNCTION_PARAMETERS, function.name()); }
	 */

	public static int intOrNull(AI ai, AbstractLeekValue value) throws LeekRunException {
		if (isType(value, NULL))
			return -1;
		return value.getInt(ai);
	}

	public static boolean verifyParameters(int[] types, AbstractLeekValue[] parameters) {
		if (parameters == null) {
			return types.length == 0;
		}
		if (types.length != parameters.length) {
			return false;
		}
		for (int i = 0; i < types.length; i++) {
			if (types[i] == -1) {
				continue;
			}
			if (!isType(parameters[i], types[i])) {
				return false;
			}
		}
		return true;
	}

	public static boolean isType(AbstractLeekValue value, int type) {
		if (type == BOOLEAN && !(value instanceof BooleanLeekValue))
			return false;
		if (type == INT && !(value instanceof IntLeekValue))
			return false;
		if (type == DOUBLE && !(value instanceof DoubleLeekValue))
			return false;
		if (type == STRING && !(value instanceof StringLeekValue))
			return false;
		if (type == NULL && !(value instanceof NullLeekValue))
			return false;
		if (type == ARRAY && !(value instanceof ArrayLeekValue))
			return false;
		if (type == FUNCTION && !(value instanceof FunctionLeekValue))
			return false;
		if (type == NUMBER && !(value instanceof IntLeekValue) && !(value instanceof DoubleLeekValue))
			return false;
		return true;
	}
}
