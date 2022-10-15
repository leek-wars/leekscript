package leekscript.compiler.bloc;

import java.util.ArrayList;

import leekscript.common.Type;
import leekscript.common.Error;
import leekscript.compiler.AnalyzeError;
import leekscript.compiler.Token;
import leekscript.compiler.JavaWriter;
import leekscript.compiler.Location;
import leekscript.compiler.WordCompiler;
import leekscript.compiler.AnalyzeError.AnalyzeErrorLevel;
import leekscript.compiler.expression.LeekExpressionException;
import leekscript.compiler.expression.LeekVariable;
import leekscript.compiler.expression.LeekVariable.VariableType;
import leekscript.compiler.instruction.LeekVariableDeclarationInstruction;
import leekscript.runner.CallableVersion;

public class FunctionBlock extends AbstractLeekBlock {

	private Token token;
	private int mId;
	private final ArrayList<String> mParameters = new ArrayList<String>();
	private final ArrayList<LeekVariableDeclarationInstruction> mParameterDeclarations = new ArrayList<>();
	private final ArrayList<Boolean> mReferences = new ArrayList<Boolean>();
	private final ArrayList<Type> mTypes = new ArrayList<>();
	private CallableVersion[] versions;

	public FunctionBlock(AbstractLeekBlock parent, MainLeekBlock main, Token token) {
		super(parent, main);
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

	public void addParameter(WordCompiler compiler, Token token, boolean is_reference, Type type) {

		for (var parameter : mParameters) {
			if (parameter.equals(token.getWord())) {
				compiler.addError(new AnalyzeError(token, AnalyzeErrorLevel.ERROR, Error.PARAMETER_NAME_UNAVAILABLE));
			}
		}

		mParameters.add(token.getWord());
		mReferences.add(is_reference);
		mTypes.add(type);
		var declaration = new LeekVariableDeclarationInstruction(compiler, token, this);
		mParameterDeclarations.add(declaration);
		addVariable(new LeekVariable(token, VariableType.ARGUMENT, declaration));
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
	public void preAnalyze(WordCompiler compiler) {

		var types = new Type[mTypes.size()];
		for (int t = 0; t < types.length; ++t) types[t] = mTypes.get(t);
		this.versions = new CallableVersion[] { new CallableVersion(Type.ANY, types) };

		var initialFunction = compiler.getCurrentFunction();
		compiler.setCurrentFunction(this);
		super.preAnalyze(compiler);
		compiler.setCurrentFunction(initialFunction);
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
				if (mainblock.getCompiler().getCurrentAI().getVersion() <= 1) {
					sb.append("final var u_").append(parameter).append(" = new Wrapper(");
					if (mReferences.get(i)) {
						sb.append("(p_").append(parameter).append(" instanceof Box) ? (Box) p_").append(parameter).append(" : new Box(" + writer.getAIThis() + ", ").append("p_").append(parameter).append("));");
					} else {
						sb.append("new Box(").append(writer.getAIThis()).append(", copy(p_").append(parameter).append(")));");
					}
				} else {
					sb.append("final var u_").append(parameter).append(" = new Box(").append(writer.getAIThis()).append(", p_").append(parameter).append(");");
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
						sb.append("p_").append(parameter).append(";");
					}
				}
			}
		}
		writer.addLine(sb.toString(), getLocation());
		writer.addCounter(1);
		super.writeJavaCode(mainblock, writer);
		if (mEndInstruction == 0)
			writer.addLine("return null;");
		writer.addLine("}");
	}

	public void compileAnonymousFunction(MainLeekBlock mainblock, JavaWriter writer) {
		writer.addCode("new FunctionLeekValue(" + this.countParameters() + ") { public Object run(AI ai, ObjectLeekValue thiz, Object... values) throws LeekRunException {");
		writer.addCode("return f_" + this + "(");
		for (int a = 0; a < this.countParameters(); ++a) {
			if (a > 0) writer.addCode(", ");
			writer.addCode("values.length > " + a + " ? values[" + a + "] : null");
		}
		writer.addLine(");");
		writer.addLine("}}");
	}

	public boolean isReference(int i) {
		return mReferences.get(i);
	}

	public void declare(WordCompiler compiler) {
		// On ajoute la fonction
		compiler.getCurrentBlock().addVariable(new LeekVariable(token, VariableType.FUNCTION, Type.FUNCTION, this));
	}

	public String toString() {
		return token.getWord();
	}

	public CallableVersion[] getVersions() {
		return versions;
	}

	@Override
	public Location getLocation() {
		return token.getLocation();
	}

	@Override
	public int getNature() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Type getType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean validExpression(WordCompiler compiler, MainLeekBlock mainblock) throws LeekExpressionException {
		// TODO Auto-generated method stub
		return false;
	}
}
