package leekscript.compiler.bloc;

import java.util.ArrayList;

import leekscript.compiler.AIFile;
import leekscript.compiler.IAWord;
import leekscript.compiler.JavaWriter;
import leekscript.compiler.WordCompiler;
import leekscript.compiler.expression.LeekVariable;
import leekscript.compiler.expression.LeekVariable.VariableType;
import leekscript.compiler.instruction.LeekVariableDeclarationInstruction;

public class FunctionBlock extends AbstractLeekBlock {

	private IAWord token;
	private int mId;
	private final ArrayList<String> mParameters = new ArrayList<String>();
	private final ArrayList<LeekVariableDeclarationInstruction> mParameterDeclarations = new ArrayList<>();
	private final ArrayList<Boolean> mReferences = new ArrayList<Boolean>();

	public FunctionBlock(AbstractLeekBlock parent, MainLeekBlock main, int line, AIFile<?> ai, IAWord token) {
		super(parent, main, line, ai);
		this.token = token;
	}

	public int getId() {
		return mId;
	}

	public void setId(int id) {
		mId = id;
	}

	public String getName() {
		return token.getWord();
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

	public void addParameter(WordCompiler compiler, IAWord parameter, boolean is_reference) {
		mParameters.add(parameter.getWord());
		mReferences.add(is_reference);
		var declaration = new LeekVariableDeclarationInstruction(compiler, parameter, parameter.getLine(), parameter.getAI(), this);
		mParameterDeclarations.add(declaration);
		addVariable(new LeekVariable(parameter, VariableType.ARGUMENT, declaration));
	}

	@Override
	public boolean hasVariable(String variable) {
		return mVariables.containsKey(variable);
	}

	@Override
	public String getCode() {
		String str = "function " + token.getWord() + "(";
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
		sb.append("private Object f_").append(token.getWord()).append("(");
		for (int i = 0; i < mParameters.size(); i++) {
			if (i != 0)
				sb.append(", ");
			sb.append("Object p_").append(mParameters.get(i));
		}
		sb.append(") throws LeekRunException {");
		for (int i = 0; i < mParameters.size(); i++) {
			var parameter = mParameters.get(i);
			var declaration = mParameterDeclarations.get(i);
			if (declaration.isCaptured()) {
				sb.append("final var u_").append(parameter).append(" = new Wrapper(");
				if (mainblock.getCompiler().getCurrentAI().getVersion() <= 1) {
					if (mReferences.get(i)) {
						sb.append("(p_").append(parameter).append(" instanceof Box) ? (Box) p_").append(parameter).append(" : new Box(" + writer.getAIThis() + ", ").append("p_").append(parameter).append("));");
					} else {
						sb.append("new Box(").append(writer.getAIThis()).append(", copy(p_").append(parameter).append(")));");
					}
				} else {
					sb.append("new Box(").append(writer.getAIThis()).append(", p_").append(parameter).append("));");
				}
			} else {
				sb.append("var u_").append(parameter).append(" = ");
				if (mReferences.get(i)) {
					sb.append("(p_").append(parameter).append(" instanceof Box) ? (Box) p_").append(parameter).append(" : new Box(" + writer.getAIThis());
					if (mainblock.getCompiler().getCurrentAI().getVersion() <= 1) {
						sb.append(", p_").append(parameter).append(");");
					} else {
						sb.append(", copy(p_").append(parameter).append("));");
					}
				} else {
					if (mainblock.getCompiler().getCurrentAI().getVersion() <= 1) {
						sb.append("new Box(" + writer.getAIThis() + ", copy(p_").append(parameter).append("));");
					} else {
						sb.append("p_").append(parameter).append("; ops(1); ");
					}
				}
			}
		}
		writer.addLine(sb.toString(), mLine, mAI);
		writer.addCounter(1);
		super.writeJavaCode(mainblock, writer);
		if (mEndInstruction == 0)
			writer.addLine("return null;");
		writer.addLine("}");
	}

	public boolean isReference(int i) {
		return mReferences.get(i);
	}

	public void declare(WordCompiler compiler) {
		// On ajoute la fonction
		compiler.getCurrentBlock().addVariable(new LeekVariable(token, VariableType.FUNCTION));
	}

	public String toString() {
		return token.getWord();
	}
}
