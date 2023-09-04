package leekscript.common;

public class IntervalType extends Type {


	public IntervalType() {
		super("interval", "t", "IntervalLeekValue", "IntervalLeekValue", "new IntervalLeekValue()");
	}

	@Override
	public Type element() {
		return Type.INT;
	}

	public boolean canBeIterable() {
		return true;
	}

	public boolean isIterable() {
		return true;
	}
}
