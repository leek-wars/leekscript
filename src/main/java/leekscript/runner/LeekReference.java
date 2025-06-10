package leekscript.runner;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

public class LeekReference extends WeakReference<Object> {
	private RamUsage ram;

	public LeekReference(RamUsage ram, Object referent, ReferenceQueue<? super Object> q) {
		super(referent, q);
		this.ram = ram;
	}
	
	public void finalizeResources(AI ai) {
        ai.freeRAM(this, ram.getValue());
    }

}
