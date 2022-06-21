package leekscript.compiler.instruction;

import leekscript.compiler.JavaWriter;
import leekscript.compiler.Location;
import leekscript.compiler.bloc.MainLeekBlock;

public class BlankInstruction extends LeekInstruction {

	@Override
	public String getCode() {
		return "";
	}

	@Override
	public void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer) {
	}

	@Override
	public int getEndBlock() {
		return 0;
	}

	@Override
	public boolean putCounterBefore() {
		return false;
	}

	@Override
	public Location getLocation() {
		return null;
	}
}
