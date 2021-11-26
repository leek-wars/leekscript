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
					sb.append("(values[").append(i).append("] instanceof Box) ? (Box) values[").append(i).append("] : ");
				}
				sb.append("new Box(" + writer.getAIThis() + ", ");
				sb.append("values[").append(i).append("]));");
			} else {
				sb.append("var u_").append(parameter).append(" = ");

				if (mainblock.getWordCompiler().getVersion() >= 2) {
					sb.append("values[").append(i).append("]; ops(1);");
				} else {
					// In LeekScript 1.0, load the value or reference
					if (mReferences.get(i)) {
						sb.append("values[").append(i).append("] instanceof Box ? (Box) values[").append(i).append("] : new Box(" + writer.getAIThis() + ", load(values[").append(i).append("]));");
					} else {
						// sb.append("new Box(" + writer.getAIThis() + ", values[").append(i).append("] instanceof Box ? copy(load(values[").append(i).append("])) : copy(load(values[").append(i).append("])));");
						sb.append("new Box(" + writer.getAIThis() + ", values[").append(i).append("]);");
					}
				}
			}
		}
		writer.addLine(sb.toString(), mLine, mAI);
		writer.addCounter(1);
		super.writeJavaCode(mainblock, writer);
		if (mEndInstruction == 0)
			writer.addLine("return null;");
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
