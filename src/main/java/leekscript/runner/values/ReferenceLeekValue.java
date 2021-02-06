package leekscript.runner.values;

import leekscript.runner.AI;
import leekscript.runner.LeekOperations;
import leekscript.runner.LeekRunException;

public class ReferenceLeekValue extends AbstractLeekValue {

	private AbstractLeekValue mValue;

	public ReferenceLeekValue(AI uai, AbstractLeekValue value) throws LeekRunException {
		if (!(value instanceof ReferenceLeekValue))
			mValue = value;
		else
			mValue = LeekOperations.clone(uai, value.getValue());
	}

	@Override
	public int getSize() throws LeekRunException {
		return mValue.getSize();
	}

	@Override
	public int getInt(AI ai) throws LeekRunException {
		return mValue.getInt(ai);
	}

	@Override
	public double getDouble(AI ai) throws LeekRunException {
		return mValue.getDouble(ai);
	}

	@Override
	public String getString(AI ai) throws LeekRunException {
		return mValue.getString(ai);
	}

	@Override
	public boolean getBoolean() {
		return mValue.getBoolean();
	}

	@Override
	public ArrayLeekValue getArray() {
		return mValue.getArray();
	}

	@Override
	public boolean isArray() {
		return mValue.isArray();
	}

	@Override
	public boolean isNull() {
		return mValue.isNull();
	}

	// Fonctions spéciales L-Values
	@Override
	public AbstractLeekValue getValue() {
		return mValue.getValue();
	}

	@Override
	public AbstractLeekValue increment(AI ai) throws LeekRunException {
		return mValue.increment(ai);
	}

	@Override
	public AbstractLeekValue decrement(AI ai) throws LeekRunException {
		return mValue.decrement(ai);
	}

	@Override
	public AbstractLeekValue pre_increment(AI ai) throws LeekRunException {
		return mValue.pre_increment(ai);
	}

	@Override
	public AbstractLeekValue pre_decrement(AI ai) throws LeekRunException {
		return mValue.pre_decrement(ai);
	}

	@Override
	public AbstractLeekValue opposite(AI ai) throws LeekRunException {
		return mValue.opposite(ai);
	}

	@Override
	public boolean equals(AI ai, AbstractLeekValue comp) throws LeekRunException {
		return mValue.equals(ai, comp);
	}

	@Override
	public boolean less(AI ai, AbstractLeekValue comp) throws LeekRunException {
		return mValue.less(ai, comp);
	}

	@Override
	public boolean more(AI ai, AbstractLeekValue comp) throws LeekRunException {
		return mValue.more(ai, comp);
	}

	@Override
	public AbstractLeekValue add(AI ai, AbstractLeekValue val) throws LeekRunException {

		return mValue = mValue.add(ai, val);
	}

	@Override
	public AbstractLeekValue minus(AI ai, AbstractLeekValue val) throws LeekRunException {

		return mValue = mValue.minus(ai, val);
	}

	@Override
	public AbstractLeekValue multiply(AI ai, AbstractLeekValue val) throws LeekRunException {

		return mValue = mValue.multiply(ai, val);
	}

	@Override
	public AbstractLeekValue power(AI ai, AbstractLeekValue val) throws LeekRunException {

		return mValue = mValue.power(ai, val);
	}

	@Override
	public AbstractLeekValue band(AI ai, AbstractLeekValue val) throws LeekRunException {

		return mValue = mValue.band(ai, val);
	}

	@Override
	public AbstractLeekValue bor(AI ai, AbstractLeekValue val) throws LeekRunException {

		return mValue = mValue.bor(ai, val);
	}

	@Override
	public AbstractLeekValue bxor(AI ai, AbstractLeekValue val) throws LeekRunException {

		return mValue = mValue.bxor(ai, val);
	}

	@Override
	public AbstractLeekValue bleft(AI ai, AbstractLeekValue val) throws LeekRunException {

		return mValue = mValue.bleft(ai, val);
	}

	@Override
	public AbstractLeekValue bright(AI ai, AbstractLeekValue val) throws LeekRunException {

		return mValue = mValue.bright(ai, val);
	}

	@Override
	public AbstractLeekValue brotate(AI ai, AbstractLeekValue val) throws LeekRunException {

		return mValue = mValue.brotate(ai, val);
	}

	@Override
	public AbstractLeekValue divide(AI ai, AbstractLeekValue val) throws LeekRunException {

		return mValue = mValue.divide(ai, val);
	}

	@Override
	public AbstractLeekValue modulus(AI ai, AbstractLeekValue val) throws LeekRunException {

		return mValue = mValue.modulus(ai, val);
	}

	@Override
	public int getType() {
		return mValue.getType();
	}

	@Override
	public boolean isReference() {
		return true;
	}

	@Override
	public AbstractLeekValue executeFunction(AI ai, AbstractLeekValue... values) throws LeekRunException {
		return mValue.executeFunction(ai, values);
	}

	@Override
	public int getArgumentsCount(AI ai) throws LeekRunException {
		return mValue.getArgumentsCount(ai);
	}

	@Override
	public Object toJSON(AI ai) throws LeekRunException {
		return mValue.toJSON(ai);
	}
}
