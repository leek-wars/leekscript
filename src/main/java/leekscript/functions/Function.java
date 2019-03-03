package leekscript.functions;

public class Function {

	private final String mName;
	private final int mArgumentCount;
	private final int mOperations;

	public Function(String name, int argumentcount, String operations) {
		mName = name;
		mArgumentCount = argumentcount;
		int nb = -1;
		try {
			nb = Integer.parseInt(operations);
		} catch (Exception e) {}
		mOperations = nb;
	}

	public String getName() {
		return mName;
	}

	public int countArguments() {
		return mArgumentCount;
	}

	public int getOperations() {
		return mOperations;
	}
}
