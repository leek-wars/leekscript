package test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import org.junit.jupiter.api.Test;

import leekscript.runner.AI;
import leekscript.runner.Session;

/**
 * RAM suivie par AI.allocateRAM / RamUsage.free : libérations dans le désordre (le dernier du
 * tableau vient boucher le trou) et au-delà de la capacité initiale du tableau, sans perte ni
 * double comptage. Le tableau est privé : son invariant (case i ↔ slot i, rien au-delà du
 * compte) est relu par réflexion, un emplacement mal recalé ne se voyant pas dans le compte.
 */
public class TestRamTracking {

	@Test
	public void freeInAnyOrder() throws Exception {
		var ai = newAI();
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
	public void shadowPeaksCountLiveContainers() throws Exception {
		var ai = newAI();
		// Au-dessus de la RAM réelle (3 000), mais la mesure ne commence qu'à la moitié de la
		// limite : sous le double du pic à 8 (~19 000)
		ai.setMaxRAM(20000);
		var countField = AI.class.getDeclaredField("ramUsagesCount");
		countField.setAccessible(true);
		long ram = ai.getUsedRAM();
		var referents = new ArrayList<Object>();
		for (int i = 0; i < 1000; i++) {
			var empty = new Object();
			var full = new Object();
			referents.add(empty);
			referents.add(full);
			ai.allocateRAM(empty);
			ai.allocateRAM(full, 3);
		}
		// La mesure ne change pas la RAM du jeu
		assertEquals(ram + 3 * 1000, ai.getUsedRAM());
		// Rien n'est libéré : le pic est l'état final, conteneurs vides compris
		long count = countField.getInt(ai);
		var peaks = ai.getShadowRAMPeaks();
		for (int i = 0; i < AI.SHADOW_RAM_BASES.length; i++) {
			assertEquals(ai.getUsedRAM() + AI.SHADOW_RAM_BASES[i] * count, peaks[i], "base " + AI.SHADOW_RAM_BASES[i]);
		}
	}

	@Test
	public void shadowPeaksIgnoredFarFromTheLimit() throws Exception {
		var ai = newAI();
		ai.setMaxRAM(Integer.MAX_VALUE);
		var referents = new ArrayList<Object>();
		for (int i = 0; i < 1000; i++) {
			var o = new Object();
			referents.add(o);
			ai.allocateRAM(o, 3);
		}
		for (long peak : ai.getShadowRAMPeaks()) assertEquals(0, peak);
	}

	private static AI newAI() throws Exception {
		return new AI(0, 4) {
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
