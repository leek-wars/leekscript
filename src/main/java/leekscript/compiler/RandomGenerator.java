package leekscript.compiler;

public interface RandomGenerator {
	public void seed(long seed);
	public int getInt(int min, int max);
	public double getDouble();
}
