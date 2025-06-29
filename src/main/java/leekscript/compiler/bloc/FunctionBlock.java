package leekscript.compiler.bloc;

import java.util.ArrayList;

import leekscript.common.Type;
import leekscript.common.Error;
import leekscript.common.FunctionType;
import leekscript.compiler.AnalyzeError;
import leekscript.compiler.Token;
import leekscript.compiler.JavaWriter;
import leekscript.compiler.Location;
import leekscript.compiler.WordCompiler;
import leekscript.compiler.AnalyzeError.AnalyzeErrorLevel;
import leekscript.compiler.exceptions.LeekCompilerException;
import leekscript.compiler.expression.LeekExpressionException;
import leekscript.compiler.expression.LeekType;
import leekscript.compiler.expression.LeekVariable;
import leekscript.compiler.expression.LeekVariable.VariableType;
import leekscript.compiler.instruction.LeekVariableDeclarationInstruction;
import leekscript.runner.CallableVersion;

public class FunctionBlock extends AbstractLeekBlock {

	private Token token;
	private int mId;
	private final ArrayList<String> mParameters = new ArrayList<String>();
	private final ArrayList<LeekType> mParametersTypes = new ArrayList<LeekType>();
	private final ArrayList<LeekVariableDeclarationInstruction> mParameterDeclarations = new ArrayList<>();
	private final ArrayList<Boolean> mReferences = new ArrayList<Boolean>();
	private final ArrayList<Type> mTypes = new ArrayList<>();
	private LeekType returnType;
	private CallableVersion[] versions;
	private FunctionType type = new FunctionType(Type.ANY);

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

	public void addParameter(WordCompiler compiler, Token token, boolean is_reference, LeekType leekType) throws LeekCompilerException {

		for (var parameter : mParameters) {
			if (parameter.equals(token.getWord())) {
				compiler.addError(new AnalyzeError(token, AnalyzeErrorLevel.ERROR, Error.PARAMETER_NAME_UNAVAILABLE));
			}
		}

		var type = leekType != null ? leekType.getType() : Type.ANY;
		mParameters.add(token.getWord());
		mParametersTypes.add(leekType);
		mReferences.add(is_reference);
		mTypes.add(type);
		var declaration = new LeekVariableDeclarationInstruction(compiler, token, this, type);
		mParameterDeclarations.add(declaration);
		addVariable(new LeekVariable(token, VariableType.ARGUMENT, type, declaration));
		this.type.add_argument(type, false);
	}

	public void setReturnType(LeekType type) {
		this.type.setReturnType(type);
		this.returnType = type;
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
			if (mParametersTypes.get(i) != null) {
				str += mParametersTypes.get(i).getType().getCode() + " ";
			}
			str += mParameters.get(i);
		}
		str += ")";
		if (this.returnType != null) {
			str += "=> " + this.returnType.toString();
		}
		return str + " {\n" + super.getCode() + "}\n";
	}

	@Override
	public void preAnalyze(WordCompiler compiler) throws LeekCompilerException {

		var types = new Type[mTypes.size()];
		for (int t = 0; t < types.length; ++t) types[t] = mTypes.get(t);
		this.versions = new CallableVersion[] { new CallableVersion(this.type.getReturnType(), types) };

		var initialFunction = compiler.getCurrentFunction();
		compiler.setCurrentFunction(this);
		super.preAnalyze(compiler);
		compiler.setCurrentFunction(initialFunction);
	}

	@Override
	public void analyze(WordCompiler compiler) throws LeekCompilerException {
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
		sb.append("private " + this.type.returnType().getJavaPrimitiveName(mainblock.getVersion()) + " f_").append(token.getWord()).append("(");
		for (int i = 0; i < mParameters.size(); i++) {
			if (i != 0)
				sb.append(", ");
			sb.append(mParameterDeclarations.get(i).getType().getJavaPrimitiveName(mainblock.getVersion()));
			sb.append(" p_").append(mParameters.get(i));
		}
		sb.append(") throws LeekRunException {");
		for (int i = 0; i < mParameters.size(); i++) {
			var parameter = mParameters.get(i);
			var declaration = mParameterDeclarations.get(i);
			if (declaration.isCaptured()) {
				if (mainblock.getCompiler().getCurrentAI().getVersion() <= 1) {
					sb.append("final var u_").append(parameter).append(" = new Wrapper<" + declaration.getType().getJavaName(mainblock.getVersion()) + ">(");
					if (mReferences.get(i)) {
						sb.append("(p_").append(parameter).append(" instanceof Box) ? (Box) p_").append(parameter).append(" : new Box(" + writer.getAIThis() + ", ").append("p_").append(parameter).append("));");
					} else {
						sb.append("new Box(").append(writer.getAIThis()).append(", copy(p_").append(parameter).append(")));");
					}
				} else {
					sb.append("final var u_").append(parameter).append(" = new Box<" + declaration.getType().getJavaName(mainblock.getVersion()) + ">(").append(writer.getAIThis()).append(", p_").append(parameter).append(");");
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
		if (mEndInstruction == 0) {
			writer.addLine("return " + type.returnType().getDefaultValue(writer, mainblock.getVersion()) + ";");
		}
		writer.addLine("}");
	}

	public void compileAnonymousFunction(MainLeekBlock mainblock, JavaWriter writer) {
		writer.addCode("new FunctionLeekValue(" + this.countParameters() + ") { public Object run(AI ai, Object thiz, Object... values) throws LeekRunException {");
		writer.addCode("return f_" + this + "(");
		for (int a = 0; a < this.countParameters(); ++a) {
			if (a > 0) writer.addCode(", ");
			writer.addCode("values.length > " + a + " ? " + (type != Type.ANY ? "(" + type.getArgument(a).getJavaName(mainblock.getVersion()) + ")" : "") + " values[" + a + "] : " + this.type.getArgument(a).getDefaultValue(writer, mainblock.getVersion()));
		}
		writer.addLine(");");
		writer.addLine("}}");
	}

	public boolean isReference(int i) {
		return mReferences.get(i);
	}

	public void declare(WordCompiler compiler) {
		// On ajoute la fonction
		compiler.getCurrentBlock().addVariable(new LeekVariable(token, VariableType.FUNCTION, type, this));
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
		return type;
	}

	@Override
	public boolean validExpression(WordCompiler compiler, MainLeekBlock mainblock) throws LeekExpressionException {
		// TODO Auto-generated method stub
		return false;
	}
}
