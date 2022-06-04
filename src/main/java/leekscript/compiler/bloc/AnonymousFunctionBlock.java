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

	public String getArgument(int i) {
		return "(values.length > " + i + " ? values[" + i + "] : null)";
	}

	@Override
	public void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer) {
		var previousFunction = mainblock.getWordCompiler().getCurrentFunction();
		mainblock.getWordCompiler().setCurrentFunction(this);
		StringBuilder sb = new StringBuilder();
		sb.append("new LeekAnonymousFunction() {");
		sb.append("public Object run(ObjectLeekValue thiz, Object... values) throws LeekRunException {");

		for (int i = 0; i < mParameters.size(); i++) {
			var parameter = mParameters.get(i);
			var declaration = mParameterDeclarations.get(i);
			if (declaration.isCaptured()) {
				sb.append("final var u_").append(parameter).append(" = new Wrapper(");
				if (mReferences.get(i)) {
					sb.append("(" + getArgument(i) + " instanceof Box) ? (Box) " + getArgument(i) + " : ");
				}
				sb.append("new Box(" + writer.getAIThis() + ", ");
				sb.append(getArgument(i) + "));");
			} else {
				sb.append("var u_").append(parameter).append(" = ");

				if (mainblock.getWordCompiler().getVersion() >= 2) {
					sb.append(getArgument(i) + ";");
				} else {
					// In LeekScript 1, load the value or reference
					if (mReferences.get(i)) {
						sb.append(getArgument(i) + " instanceof Box ? (Box) " + getArgument(i) + " : new Box(" + writer.getAIThis() + ", load(" + getArgument(i) + "));");
					} else {
						sb.append("new Box(" + writer.getAIThis() + ", " + getArgument(i) + ");");
					}
				}
			}
		}
		writer.addLine(sb.toString(), mLine, mAI);
		writer.addCounter(1);
		super.writeJavaCode(mainblock, writer);
		if (mEndInstruction == 0) {
			writer.addLine("return null;");
		}
		writer.addCode("}}");
		mainblock.getWordCompiler().setCurrentFunction(previousFunction);
	}

	public boolean isReference(int i) {
		return mReferences.get(i);
	}

	@Override
	public int getOperations() {
		return 0;
	}
}
