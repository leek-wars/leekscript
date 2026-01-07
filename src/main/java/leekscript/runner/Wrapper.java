
package leekscript.runner;

import leekscript.runner.values.Box;

public class Wrapper<T> {

	private Box<T> variable;

	public Wrapper(Box variable) {
		this.variable = variable;
	}

	public Wrapper(Box variable, int ops) throws LeekRunException {
		this.variable = variable;
		this.variable.getAI().ops(ops);
	}

	public Wrapper(Wrapper wrapper, int ops) throws LeekRunException {
		this.variable = wrapper.variable;
		this.variable.getAI().ops(ops);
	}

	public Object setBox(Box variable) {
		this.variable = variable;
		return this.variable.get();
	}

	public Object set(Object value) throws LeekRunException {
		this.variable.set(value);
		return value;
	}

	public Object setBoxOrValue(Object value) throws LeekRunException {
		if (value instanceof Box box) {
			this.variable = box;
			return this.variable.get();
		} else {
			return this.variable.set(value);
		}
	}

	public Box<T> getVariable() {
		return variable;
	}

	public T get() {
		return variable.get();
	}

	public Object increment() throws LeekRunException {
		return variable.increment();
	}

	public Object pre_increment() throws LeekRunException {
		return variable.pre_increment();
	}

	public Object decrement() throws LeekRunException {
		return variable.decrement();
	}

	public Object pre_decrement() throws LeekRunException {
		return variable.pre_decrement();
	}

	public Object add_eq(Object x) throws LeekRunException {
		return variable.add_eq(x);
	}

	public Object sub_eq(Object x) throws LeekRunException {
		return variable.sub_eq(x);
	}

	public Object mul_eq(Object x) throws LeekRunException {
		return variable.mul_eq(x);
	}

	public Number div_eq(Object x) throws LeekRunException {
		return variable.div_eq(x);
	}

	public Object div_v1_eq(Object x) throws LeekRunException {
		return variable.div_v1_eq(x);
	}

	public Object mod_eq(Object x) throws LeekRunException {
		return variable.mod_eq(x);
	}

	public Object pow_eq(Object x) throws LeekRunException {
		return variable.pow_eq(x);
	}

	public Number band_eq(Object x) throws LeekRunException {
		return variable.band_eq(x);
	}

	public Number bor_eq(Object x) throws LeekRunException {
		return variable.bor_eq(x);
	}

	public Number bxor_eq(Object x) throws LeekRunException {
		return variable.bxor_eq(x);
	}

	public Number shl_eq(Object x) throws LeekRunException {
		return variable.shl_eq(x);
	}

	public Number shr_eq(Object x) throws LeekRunException {
		return variable.shr_eq(x);
	}

	public Number ushr_eq(Object x) throws LeekRunException {
		return variable.ushr_eq(x);
	}

	@Override
	public String toString() {
		return "Wrapper(" + variable.get() + ")";
	}
}