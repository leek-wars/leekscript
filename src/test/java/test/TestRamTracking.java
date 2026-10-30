package test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import org.junit.jupiter.api.Test;

import leekscript.common.Error;
import leekscript.runner.AI;
import leekscript.runner.LeekRunException;
import leekscript.runner.Session;
import leekscript.runner.values.LegacyArrayLeekValue;
import leekscript.runner.values.SetLeekValue;

/**
 * RAM suivie par AI.allocateRAM / RamUsage.free : libérations dans le désordre (le dernier du
 * tableau vient boucher le trou) et au-delà de la capacité initiale du tableau, sans perte ni
 * double comptage. Le tableau est privé : son invariant (case i ↔ slot i, rien au-delà du
 * compte) est relu par réflexion, un emplacement mal recalé ne se voyant pas dans le compte.
 * Un tableau v1-v3 refusé faute de RAM reste intact. Le code compilé se partage entre les entités qui l'exécutent.
 */
public class TestRamTracking {

	@Test
	public void freeInAnyOrder() throws Exception {
		var ai = newAI(4);
		long ram = ai.getUsedRAM();
		var referents = new ArrayList<Object>();
		var usages = new ArrayList<AI.RamUsage>();
		for (int i = 0; i < 5000; i++) {
			var o = new Object();
			referents.add(o); // référents gardés vivants : seul free() rend la RAM
			usages.add(ai.allocateRAM(o, 3));
		}
		assertEquals(ram + 3 * 5000, ai.getUsedRAM());
		checkSlots(ai);

		Collections.shuffle(usages, new Random(42));
		for (int i = 0; i < 2500; i++) usages.get(i).free(ai);
		assertEquals(ram + 3 * 2500, ai.getUsedRAM());
		checkSlots(ai);

		usages.get(0).free(ai); // déjà libéré : rien de plus
		assertEquals(ram + 3 * 2500, ai.getUsedRAM());

		for (int i = 2500; i < 5000; i++) usages.get(i).free(ai);
		assertEquals(ram, ai.getUsedRAM());
		checkSlots(ai);
	}

	@Test
	public void legacyArrayIntactWhenOutOfMemory() throws Exception {
		var ai = newAI(3);
		var array = new LegacyArrayLeekValue(ai);
		// La limite tombe au milieu du 6e élément
		ai.setMaxRAM((int) ai.getUsedRAM() + 5 * LegacyArrayLeekValue.ELEMENT_RAM + 1);
		for (int i = 0; i < 5; i++) array.push(ai, (long) i);
		var e = assertThrows(LeekRunException.class, () -> array.push(ai, 5l));
		assertEquals(Error.OUT_OF_MEMORY, e.getError());
		// Le refus n'a pas ajouté d'élément (sa RAM reste comptée, comme en v4), le tableau reste utilisable
		assertEquals(5, array.size());
		long ram = ai.getUsedRAM();
		array.removeIndex(ai, 0);
		assertEquals(4, array.size());
		assertEquals(ram - LegacyArrayLeekValue.ELEMENT_RAM, ai.getUsedRAM());
	}

	@Test
	public void setOperationsChargeTheirResult() throws Exception {
		var ai = newAI(4);
		var ramField = SetLeekValue.class.getDeclaredField("ram");
		ramField.setAccessible(true);
		var a = new SetLeekValue(ai, new Object[] { 1l, 2l, 3l });
		var b = new SetLeekValue(ai, new Object[] { 3l, 4l });
		long ram = ai.getUsedRAM();
		var results = new SetLeekValue[] { a.setUnion(ai, b), a.setIntersection(ai, b), a.setDifference(ai, b), a.setDisjunction(ai, b) };
		// Libérer les résultats rend toute leur RAM : rien n'est resté compté sur `a`
		for (var result : results) ((AI.RamUsage) ramField.get(result)).free(ai);
		assertEquals(ram, ai.getUsedRAM());
	}

	@Test
	public void sharedCodeRAM() throws Exception {
		var ai = newAI(4);
		long base = ai.getUsedRAM();
		ai.chargeCodeRAM(4000);
		ai.shareCodeRAM(4);
		assertEquals(250, ai.getCodeRAM());
		assertEquals(base + 250, ai.getUsedRAM());
	}

	private static AI newAI(int version) throws Exception {
		return new AI(0, version) {
			@Override
			public Object runIA(Session session) {
				return null;
			}
		};
	}

	private static void checkSlots(AI ai) throws Exception {
		var arrayField = AI.class.getDeclaredField("ramUsages");
		var countField = AI.class.getDeclaredField("ramUsagesCount");
		var slotField = AI.RamUsage.class.getDeclaredField("slot");
		arrayField.setAccessible(true);
		countField.setAccessible(true);
		slotField.setAccessible(true);
		var array = (Object[]) arrayField.get(ai);
		int count = countField.getInt(ai);
		for (int i = 0; i < count; i++) assertEquals(i, slotField.getInt(array[i]), "case " + i);
		for (int i = count; i < array.length; i++) assertNull(array[i], "case " + i);
	}
}
