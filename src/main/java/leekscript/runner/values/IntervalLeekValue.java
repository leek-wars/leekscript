package leekscript.runner.values;

import java.util.Iterator;
import java.util.Map.Entry;

import leekscript.runner.AI;
import leekscript.runner.LeekRunException;

public abstract class IntervalLeekValue implements LeekValue {

	protected final AI ai;
	protected final int id;
	protected final boolean minClosed;
	protected final boolean maxClosed;

	public IntervalLeekValue(AI ai, boolean minClosed, boolean maxClosed) {
		this.ai = ai;
		this.id = ai.getNextObjectID();
		this.minClosed = minClosed;
		this.maxClosed = maxClosed;
	}

	public abstract Iterator<Entry<Object, Object>> iterator();

	public abstract boolean intervalIsEmpty(AI ai);

	public abstract ArrayLeekValue range(AI ai, Object start, Object object, Object stride) throws LeekRunException;

}
