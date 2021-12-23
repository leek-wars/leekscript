package leekscript.compiler.bloc;

import java.util.ArrayList;
import java.util.List;

import leekscript.compiler.AIFile;
import leekscript.compiler.IAWord;
import leekscript.compiler.JavaWriter;
import leekscript.compiler.expression.LeekVariable;
import leekscript.compiler.expression.LeekVariable.VariableType;
import leekscript.compiler.instruction.ClassDeclarationInstruction;

public class ClassMethodBlock extends AbstractLeekBlock {

	private final ClassDeclarationInstruction clazz;
	private final boolean isStatic;
	private final ArrayList<IAWord> mParameters = new ArrayList<>();
	private int mId = 0;

	public ClassMethodBlock(ClassDeclarationInstruction clazz, boolean isStatic, AbstractLeekBlock parent, MainLeekBlock main, int line, AIFile<?> ai) {
		super(parent, main, line, ai);
		this.clazz = clazz;
		this.isStatic = isStatic;
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
		}
		return str + "}";
	}

	public void addParameter(IAWord token) {
		mParameters.add(token);
		addVariable(new LeekVariable(token, VariableType.ARGUMENT));
	}

	public LeekVariable getVariable(String variable, boolean includeClassMembers) {
		// Arguments
		// for (var parameter : mParameters) {
		// 	if (parameter.getWord().equals(variable)) {
		// 		return new LeekVariable(parameter, VariableType.ARGUMENT);
		// 	}
		// }

		var v = super.getVariable(variable, includeClassMembers);
		if (v != null) return v;

		// Search in fields
		if (variable.equals("class")) return new LeekVariable(new IAWord("class"), VariableType.THIS_CLASS);
		if (!isStatic) {
			if (variable.equals("this")) return new LeekVariable(new IAWord("this"), VariableType.THIS);
			if (includeClassMembers) {
				v = clazz.getMember(variable);
				if (v != null) return v;
			}
		}
		if (includeClassMembers) {
			v = clazz.getStaticMember(variable);
			if (v != null) return v;
		}
		return null;
	}

	@Override
	public String getCode() {
		String str = "(";
		for (int i = 0; i < mParameters.size(); i++) {
			if (i != 0)
				str += ", ";
			str += mParameters.get(i);
		}
		return str + ") {\n" + super.getCode() + "}\n";
	}

	@Override
	public void checkEndBlock() {

	}

	@Override
	public void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer) {

		writer.addLine("", mLine, mAI);

		super.writeJavaCode(mainblock, writer);
		if (mEndInstruction == 0) {
			writer.addLine("return null;");
		}
	}

	public ClassDeclarationInstruction getClassDeclaration() {
		return this.clazz;
	}

	public List<IAWord> getParameters() {
		return mParameters;
	}
}
