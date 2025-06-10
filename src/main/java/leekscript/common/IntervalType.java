package leekscript.common;

import leekscript.compiler.JavaWriter;

public class IntervalType extends Type {

	private final Type type;

	public IntervalType(Type type) {
		this(type, false);
	}

	// TODO add BIG_INT
	public IntervalType(Type type, boolean infinite) {
		super(infinite ? "Interval<infinite>" : type == Type.VOID ? "Interval<empty>" : type == Type.ANY ? "Interval" : "Interval<" + type.toString() + ">", "t", "IntervalLeekValue", "IntervalLeekValue", type == Type.INT ? "new IntegerIntervalLeekValue()" : "new RealIntervalLeekValue()");
		this.type = type;
	}

	@Override
	public CastType accepts(Type type) {
		if (type instanceof IntervalType at) {
			var cast = this.type.accepts(at.type);
			if (cast.ordinal() >= CastType.SAFE_DOWNCAST.ordinal()) return CastType.UNSAFE_DOWNCAST;
			return cast;
		}
		return super.accepts(type);
	}

	@Override
	public Type key() {
		return Type.INT;
	}

	@Override
	public Type element() {
		return type;
	}

	public boolean canBeIterable() {
		return true;
	}

	public boolean isIterable() {
		return true;
	}

	@Override
	public String getJavaPrimitiveName(int version) {
		return type == Type.ANY ? "IntervalLeekValue" : type == Type.INT ? "IntegerIntervalLeekValue" : "RealIntervalLeekValue";
	}

	@Override
	public String getJavaName(int version) {
		return type == Type.ANY ? "IntervalLeekValue" : type == Type.INT ? "IntegerIntervalLeekValue" : "RealIntervalLeekValue";
	}

	@Override
	public String getDefaultValue(JavaWriter writer, int version) {
		return type == Type.INT ? "new IntegerIntervalLeekValue(" + writer.getAIThis() + ")" : "new RealIntervalLeekValue(" + writer.getAIThis() + ")";
	}
}
