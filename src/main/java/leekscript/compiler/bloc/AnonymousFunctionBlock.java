package leekscript.compiler.bloc;

import java.util.ArrayList;

import leekscript.common.Type;
import leekscript.common.Error;
import leekscript.common.FunctionType;
import leekscript.compiler.Token;
import leekscript.compiler.AnalyzeError;
import leekscript.compiler.JavaWriter;
import leekscript.compiler.Location;
import leekscript.compiler.WordCompiler;
import leekscript.compiler.AnalyzeError.AnalyzeErrorLevel;
import leekscript.compiler.exceptions.LeekCompilerException;
import leekscript.compiler.expression.LeekExpressionException;
import leekscript.compiler.expression.LeekVariable;
import leekscript.compiler.expression.LeekVariable.VariableType;
import leekscript.compiler.instruction.LeekVariableDeclarationInstruction;

public class AnonymousFunctionBlock extends AbstractLeekBlock {

	private final ArrayList<String> mParameters = new ArrayList<String>();
	private final ArrayList<LeekVariableDeclarationInstruction> mParameterDeclarations = new ArrayList<>();
	private final ArrayList<Boolean> mReferences = new ArrayList<Boolean>();
	private final ArrayList<Type> mTypes = new ArrayList<>();
	private int mId = 0;
	private final Token token;
	private Token endToken;
	private FunctionType type = new FunctionType(Type.ANY);

	public AnonymousFunctionBlock(AbstractLeekBlock parent, MainLeekBlock main, Token token) {
		super(parent, main);
		this.token = token;
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

	public void setEndToken(Token token) {
		this.endToken = token;
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

	public void addParameter(WordCompiler compiler, Token token, boolean is_reference, Type type) throws LeekCompilerException {

		for (var parameter : mParameters) {
			if (parameter.equals(token.getWord())) {
				compiler.addError(new AnalyzeError(token, AnalyzeErrorLevel.ERROR, Error.PARAMETER_NAME_UNAVAILABLE));
			}
		}

		mParameters.add(token.getWord());
		mReferences.add(is_reference);
		mTypes.add(type);
		var declaration = new LeekVariableDeclarationInstruction(compiler, token, this, type);
		mParameterDeclarations.add(declaration);
		addVariable(new LeekVariable(token, VariableType.ARGUMENT, type, declaration));
		this.type.add_argument(type, false);
	}

	public void setReturnType(Type type) {
		this.type.setReturnType(type);
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
	public void preAnalyze(WordCompiler compiler) throws LeekCompilerException {
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

	public String getArgument(int i, JavaWriter writer, MainLeekBlock mainblock) {
		var type = mParameterDeclarations.get(i).getType();
		return "(values.length > " + i + " ? " + (type != Type.ANY ? "(" + type.getJavaName(mainblock.getVersion()) + ")" : "") + " values[" + i + "] : " + this.type.getArgument(i).getDefaultValue(writer, mainblock.getVersion()) + ")";
	}

	@Override
	public void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer) {
		var previousFunction = mainblock.getWordCompiler().getCurrentFunction();
		mainblock.getWordCompiler().setCurrentFunction(this);
		StringBuilder sb = new StringBuilder();
		sb.append("public Object run(AI ai, Object thiz, Object... values) throws LeekRunException {");

		for (int i = 0; i < mParameters.size(); i++) {
			var parameter = mParameters.get(i);
			var declaration = mParameterDeclarations.get(i);
			if (declaration.isCaptured()) {
				sb.append("final var u_").append(parameter).append(" = new Wrapper<" + declaration.getType().getJavaName(mainblock.getVersion()) + ">(");
				if (mReferences.get(i)) {
					sb.append("(" + getArgument(i, writer, mainblock) + " instanceof Box) ? (Box) " + getArgument(i, writer, mainblock) + " : ");
				}
				sb.append("new Box(" + writer.getAIThis() + ", ");
				sb.append(getArgument(i, writer, mainblock) + "));");
			} else {
				sb.append("var u_").append(parameter).append(" = ");

				if (mainblock.getWordCompiler().getVersion() >= 2) {
					sb.append(getArgument(i, writer, mainblock) + ";");
				} else {
					// In LeekScript 1, load the value or reference
					if (mReferences.get(i)) {
						sb.append(getArgument(i, writer, mainblock) + " instanceof Box ? (Box) " + getArgument(i, writer, mainblock) + " : new Box(" + writer.getAIThis() + ", load(" + getArgument(i, writer, mainblock) + "));");
					} else {
						sb.append("new Box(" + writer.getAIThis() + ", " + getArgument(i, writer, mainblock) + ");");
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
		writer.addCode("}");
		mainblock.getWordCompiler().setCurrentFunction(previousFunction);
	}

	public boolean isReference(int i) {
		return mReferences.get(i);
	}

	@Override
	public int getOperations() {
		return 0;
	}

	@Override
	public Location getLocation() {
		if (this.endToken != null) {
			return new Location(token.getLocation(), this.endToken.getLocation());
		}
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
	public String toString() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean validExpression(WordCompiler compiler, MainLeekBlock mainblock) throws LeekExpressionException {
		// TODO Auto-generated method stub
		return false;
	}
}
