package leekscript.runner.classes;

import leekscript.runner.AI;

public class ColorClass {

	public static long color(AI ai, long red, long green, long blue) {
		return ((red & 255) << 16) | ((green & 255) << 8) | (blue & 255);
	}

	public static long getColor(AI ai, long red, long green, long blue) {
		return ((red & 255) << 16) | ((green & 255) << 8) | (blue & 255);
	}

	public static long getRed(AI ai, long color) {
		return (color >> 16) & 255;
	}

	public static long getGreen(AI ai, long color) {
		return (color >> 8) & 255;
	}

	public static long getBlue(AI ai, long color) {
		return color & 255;
	}
}
