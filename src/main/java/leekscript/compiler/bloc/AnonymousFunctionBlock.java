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
import leekscript.compiler.expression.LeekType;
import leekscript.compiler.expression.LeekVariable;
import leekscript.compiler.expression.LeekVariable.VariableType;
import leekscript.compiler.instruction.LeekVariableDeclarationInstruction;
import leekscript.compiler.bytecode.BytecodeWriter;
import leekscript.compiler.bytecode.JExpr;
import leekscript.compiler.bytecode.Unsupported;
import java.util.List;

public class AnonymousFunctionBlock extends AbstractLeekBlock {

	private final ArrayList<String> mParameters = new ArrayList<String>();
	private final ArrayList<LeekVariableDeclarationInstruction> mParameterDeclarations = new ArrayList<>();
	private final ArrayList<Boolean> mReferences = new ArrayList<Boolean>();
	private final ArrayList<LeekType> mTypes = new ArrayList<>();
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

	public void addParameter(WordCompiler compiler, Token token, boolean is_reference, LeekType leekType) throws LeekCompilerException {

		for (var parameter : mParameters) {
			if (parameter.equals(token.getWord())) {
				compiler.addError(new AnalyzeError(token, AnalyzeErrorLevel.ERROR, Error.PARAMETER_NAME_UNAVAILABLE));
			}
		}

		mParameters.add(token.getWord());
		mReferences.add(is_reference);
		mTypes.add(leekType);
		var type = leekType == null ? Type.ANY : leekType.getType();
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
		// Le corps s'exécute plus tard : les affinages de champs posés autour ne valent pas dedans
		compiler.getMainBlock().invalidateFieldNarrowings();
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
	public void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer, boolean parenthesis) {
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
		super.writeJavaCode(mainblock, writer, false);
		if (mEndInstruction == 0) {
			writer.addLine("return " + type.returnType().getDefaultValue(writer, mainblock.getVersion()) + ";");
		}
		writer.addCode("}");
		mainblock.getWordCompiler().setCurrentFunction(previousFunction);
	}


	/** getArgument : `(values.length > i ? (T) values[i] : <défaut>)`. */
	private JExpr bytecodeArgument(MainLeekBlock mainblock, BytecodeWriter writer, int i) {
		var values = new JExpr.Read(new BytecodeWriter.LocalVar(3, Object[].class, "values"));
		var declared = mParameterDeclarations.get(i).getType();
		JExpr given = new JExpr.ArrayGet(values, JExpr.of(i));
		if (declared != Type.ANY) given = new JExpr.Cast(writer.typeNamed(declared.getJavaName(mainblock.getVersion())), given);
		return new JExpr.Cond(new JExpr.Binary(">", new JExpr.ArrayLength(values), JExpr.of(i)), given, writer.defaultValue(this.type.getArgument(i)));
	}

	/** Le corps de `run(AI ai, Object thiz, Object... values)` pour le backend bytecode. */
	public void writeBytecodeBody(MainLeekBlock mainblock, BytecodeWriter writer) {
		if (mainblock.getWordCompiler().getVersion() < 2) throw new Unsupported("fonction anonyme v1");
		var previousFunction = mainblock.getWordCompiler().getCurrentFunction();
		mainblock.getWordCompiler().setCurrentFunction(this);
		writer.line(getLocation());
		for (int i = 0; i < mParameters.size(); i++) {
			var declaration = mParameterDeclarations.get(i);
			if (mReferences.get(i)) throw new Unsupported("paramètre par référence");
			var argument = bytecodeArgument(mainblock, writer, i);
			if (declaration.isCaptured()) {
				var javaType = writer.boxGeneric(leekscript.runner.Wrapper.class, declaration.getType().getJavaName(mainblock.getVersion()));
				var wrapper = writer.declare(declaration, leekscript.runner.Wrapper.class, mParameters.get(i), javaType);
				var box = new JExpr.New(leekscript.runner.values.Box.class, List.of(writer.aiThis(), argument));
				writer.emitStatement(new JExpr.Assign(wrapper, new JExpr.New(leekscript.runner.Wrapper.class, List.of(box))));
			} else {
				var declared = declaration.getType();
				var generic = declared != Type.ANY ? writer.genericNamed(declared.getJavaName(mainblock.getVersion())) : null;
				var local = writer.declare(declaration, writer.typeOf(argument), mParameters.get(i), generic);
				writer.emitStatement(new JExpr.Assign(local, argument));
			}
		}
		writer.pendingLine();
		writer.counter(1);
		writeBlockBytecode(mainblock, writer);
		if (mEndInstruction == 0 && writer.isAlive()) writer.emitReturn(writer.defaultValue(type.returnType()), writer.returnType());
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
