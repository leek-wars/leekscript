package leekscript.compiler.bloc;

import java.util.ArrayList;

import leekscript.compiler.AIFile;
import leekscript.compiler.JavaWriter;

public class AnonymousFunctionBlock extends AbstractLeekBlock {

	private final ArrayList<String> mParameters = new ArrayList<String>();
	private final ArrayList<Boolean> mReferences = new ArrayList<Boolean>();
	private int mId = 0;

	public AnonymousFunctionBlock(AbstractLeekBlock parent, MainLeekBlock main, int line, AIFile<?> ai) {
		super(parent, main, line, ai);
	}

	public void setId(int id) {
		mId = id;
	}

	public int getId() {
		return mId;
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

	public void addParameter(String parameter, boolean is_reference) {
		mParameters.add(parameter);
		mReferences.add(is_reference);
		addVariable(parameter);
	}

	@Override
	public String getCode() {
		String str = "function anonymous" + mId + "(";
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
		sb.append("new LeekAnonymousFunction() {");
		sb.append("public AbstractLeekValue run(AI mUAI, AbstractLeekValue thiz, AbstractLeekValue... values) throws LeekRunException {");

		for (int i = 0; i < mParameters.size(); i++) {
			sb.append("final VariableLeekValue user_").append(mParameters.get(i)).append(" = ");
			if (mReferences.get(i)) {
				sb.append("(values[").append(i).append("] instanceof VariableLeekValue)?(VariableLeekValue)values[").append(i).append("]:");
			}
			sb.append("new VariableLeekValue(mUAI, values[").append(i).append("].getValue());");
		}
		writer.addLine(sb.toString(), mLine, mAI);
		writer.addCounter(1);
		super.writeJavaCode(mainblock, writer);
		if (mEndInstruction == 0)
			writer.addLine("return LeekValueManager.NULL;");
		writer.addCode("}}");
	}

	public boolean isReference(int i) {
		return mReferences.get(i);
	}
}
