package leekscript.runner;

public class RamUsage {
	private int value;

	public RamUsage(int value) {
		this.value = value;
	}
	
	public void add(int ram) {
		value += ram;
	}

	public void remove(int ram) {
		value -= ram;
	}
	
	public int getValue() {
		return value;
	}
}
