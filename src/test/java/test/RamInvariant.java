package test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import leekscript.runner.AI;
import leekscript.runner.values.ArrayLeekValue;
import leekscript.runner.values.MapLeekValue;
import leekscript.runner.values.ObjectLeekValue;
import leekscript.runner.values.SetLeekValue;

/**
 * RAM comptée des conteneurs v4 encore vivants à la fin d'un cas (tableaux, maps, ensembles et
 * objets `{}`) : exactement leur coût de base plus celui de leurs éléments (1 par élément d'un
 * tableau ou d'un ensemble, 2 par entrée d'une map ou par champ d'un objet). Un écart veut dire qu'un
 * chemin ajoute ou retire sans compter, ou impute au mauvais conteneur (la source au lieu du
 * résultat) : la limite de RAM de l'IA devient fausse. Les tableaux v1-v3 et les instances de
 * classes ont leurs propres règles, vérifiées par TestRamCost.
 */
final class RamInvariant {

	private static final Field USAGES = field(AI.class, "ramUsages");
	private static final Field COUNT = field(AI.class, "ramUsagesCount");
	private static final Field ARRAY_RAM = field(ArrayLeekValue.class, "ram");
	private static final Field MAP_RAM = field(MapLeekValue.class, "ram");
	private static final Field SET_RAM = field(SetLeekValue.class, "ram");
	private static final Field OBJECT_RAM = field(ObjectLeekValue.class, "ram");
	private static final int ARRAY_BASE = baseRam(ArrayLeekValue.class);
	private static final int MAP_BASE = baseRam(MapLeekValue.class);
	private static final int SET_BASE = baseRam(SetLeekValue.class);
	private static final int OBJECT_BASE = baseRam(ObjectLeekValue.class);

	private RamInvariant() {}

	static List<String> violations(AI ai) throws ReflectiveOperationException {
		var result = new ArrayList<String>();
		var usages = (AI.RamUsage[]) USAGES.get(ai);
		int count = COUNT.getInt(ai);
		for (int i = 0; i < count; i++) {
			var usage = usages[i];
			var referent = usage.get();
			Field ram;
			int size, expected;
			if (referent instanceof ArrayLeekValue a) { ram = ARRAY_RAM; size = a.size(); expected = ARRAY_BASE + size; }
			else if (referent instanceof MapLeekValue m) { ram = MAP_RAM; size = m.size(); expected = MAP_BASE + 2 * size; }
			else if (referent instanceof SetLeekValue s) { ram = SET_RAM; size = s.size(); expected = SET_BASE + size; }
			else if (referent instanceof ObjectLeekValue o) { ram = OBJECT_RAM; size = o.size(); expected = OBJECT_BASE + 2 * size; }
			else continue;
			// Un autre compteur du même objet (champs d'une classe qui étend Array, Map ou Set) suit ses propres règles
			if (ram.get(referent) != usage) continue;
			if (usage.getValue() != expected) {
				result.add(referent.getClass().getSimpleName() + " de taille " + size + " : " + usage.getValue() + " compté, " + expected + " attendu");
			}
		}
		return result;
	}

	private static Field field(Class<?> clazz, String name) {
		try {
			var f = clazz.getDeclaredField(name);
			f.setAccessible(true);
			return f;
		} catch (NoSuchFieldException e) {
			throw new IllegalStateException(e);
		}
	}

	private static int baseRam(Class<?> clazz) {
		try {
			return field(clazz, "BASE_RAM").getInt(null);
		} catch (IllegalAccessException e) {
			throw new IllegalStateException(e);
		}
	}
}
