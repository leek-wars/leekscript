package leekscript.compiler.bloc;

import java.util.ArrayList;

import leekscript.compiler.AIFile;
import leekscript.compiler.IAWord;
import leekscript.compiler.JavaWriter;
import leekscript.compiler.WordCompiler;
import leekscript.compiler.expression.LeekVariable;
import leekscript.compiler.expression.LeekVariable.VariableType;
import leekscript.compiler.instruction.LeekVariableDeclarationInstruction;

public class AnonymousFunctionBlock extends AbstractLeekBlock {

	private final ArrayList<String> mParameters = new ArrayList<String>();
	private final ArrayList<LeekVariableDeclarationInstruction> mParameterDeclarations = new ArrayList<>();
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

	public void addParameter(WordCompiler compiler, IAWord token, boolean is_reference) {
		mParameters.add(token.getWord());
		mReferences.add(is_reference);
		var declaration = new LeekVariableDeclarationInstruction(compiler, token, token.getLine(), token.getAI(), this);
		mParameterDeclarations.add(declaration);
		addVariable(new LeekVariable(token, VariableType.ARGUMENT, declaration));
	}

	public boolean hasParameter(String name) {
		for (var parameter : mParameters) {
			if (parameter.equals(name)) return true;
		}
		return false;
	}

	@Override
	public String getCode() {
		String str = "function(";
		for (int i = 0; i < mParameters.size(); i++) {
			if (i != 0)
				str += ", ";
			str += mParameters.get(i);
		}
		return str + ") {\n" + super.getCode() + "}\n";
	}

	@Override
	public void analyze(WordCompiler compiler) {
		var initialFunction = compiler.getCurrentFunction();
		compiler.setCurrentFunction(this);
		super.analyze(compiler);
		compiler.setCurrentFunction(initialFunction);
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

	@Override
	public int getOperations() {
		// TODO Auto-generated method stub
		return 0;
	}
}
