package leekscript;

import java.util.Random;

public class Util {

	private static Random random = new Random();

	public static int getRandom(int min, int max) {
		if (max - min + 1 <= 0)
			return 0;
		return min + random.nextInt(max - min + 1);
	}
}
