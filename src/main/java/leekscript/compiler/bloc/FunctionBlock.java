package leekscript.compiler.bloc;

import java.util.ArrayList;

import leekscript.compiler.AIFile;
import leekscript.compiler.JavaWriter;

public class FunctionBlock extends AbstractLeekBlock {

	private String mName;
	private int mId;
	private final ArrayList<String> mParameters = new ArrayList<String>();
	private final ArrayList<Boolean> mReferences = new ArrayList<Boolean>();

	public FunctionBlock(AbstractLeekBlock parent, MainLeekBlock main, int line, AIFile<?> ai) {
		super(parent, main, line, ai);
	}

	public int getId() {
		return mId;
	}

	public void setId(int id) {
		mId = id;
	}

	public String getName() {
		return mName;
	}

	public int countParameters() {
		return mParameters.size();
	}

	public String referenceArray() {
		String str = "{";
		for (int i = 0; i < mParameters.size(); i++) {
			if (i != 0)
				str += ",";
			str += mReferences.get(i) ? "true" : "false";
		}
		return str + "}";
	}

	public void setName(String name) {
		mName = name;
	}

	public void addParameter(String parameter, boolean is_reference) {
		mParameters.add(parameter);
		mReferences.add(is_reference);
		addVariable(parameter);
	}

	@Override
	public boolean hasVariable(String variable) {
		return mVariables.contains(variable);
	}

	@Override
	public String getCode() {
		String str = "function " + mName + "(";
		for (int i = 0; i < mParameters.size(); i++) {
			if (i != 0)
				str += ", ";
			str += mParameters.get(i);
		}
		return str + "){\n" + super.getCode() + "}\n";
	}

	@Override
	public void checkEndBlock() {

	}

	@Override
	public void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer) {
		StringBuilder sb = new StringBuilder();
		sb.append("private AbstractLeekValue user_function_").append(mName).append("(");
		for (int i = 0; i < mParameters.size(); i++) {
			if (i != 0)
				sb.append(", ");
			sb.append("AbstractLeekValue param_").append(mParameters.get(i));
		}
		sb.append(") throws Exception{");
		for (int i = 0; i < mParameters.size(); i++) {
			sb.append("final VariableLeekValue user_").append(mParameters.get(i)).append(" = ");
			if (mReferences.get(i)) {
				sb.append("(param_").append(mParameters.get(i)).append(" instanceof VariableLeekValue)?(VariableLeekValue)param_").append(mParameters.get(i)).append(":");
			}
			sb.append("new VariableLeekValue(this, param_").append(mParameters.get(i)).append(".getValue());");
		}
		writer.addLine(sb.toString(), mLine, mAI);
		writer.addCounter(1);
		super.writeJavaCode(mainblock, writer);
		if (mEndInstruction == 0)
			writer.addLine("return LeekValueManager.NULL;");
		writer.addLine("}");
	}

	public boolean isReference(int i) {
		return mReferences.get(i);
	}
}
