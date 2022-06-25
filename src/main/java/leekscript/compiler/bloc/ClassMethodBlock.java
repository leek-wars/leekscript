package leekscript.compiler.bloc;

import java.util.ArrayList;
import java.util.List;

import leekscript.common.Type;
import leekscript.compiler.JavaWriter;
import leekscript.compiler.Location;
import leekscript.compiler.Token;
import leekscript.compiler.WordCompiler;
import leekscript.compiler.expression.LeekExpressionException;
import leekscript.compiler.expression.LeekVariable;
import leekscript.compiler.expression.LeekVariable.VariableType;
import leekscript.compiler.instruction.ClassDeclarationInstruction;
import leekscript.compiler.instruction.LeekVariableDeclarationInstruction;

public class ClassMethodBlock extends AbstractLeekBlock {

	private final ClassDeclarationInstruction clazz;
	private final boolean isStatic;
	private final ArrayList<Token> mParameters = new ArrayList<>();
	private final ArrayList<LeekVariableDeclarationInstruction> mParameterDeclarations = new ArrayList<>();
	private int mId = 0;
	private final Token token;

	public ClassMethodBlock(ClassDeclarationInstruction clazz, boolean isStatic, AbstractLeekBlock parent, MainLeekBlock main, Token token) {
		super(parent, main);
		this.clazz = clazz;
		this.isStatic = isStatic;
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

	public String referenceArray() {
		String str = "{";
		for (int i = 0; i < mParameters.size(); i++) {
			if (i != 0)
				str += ",";
		}
		return str + "}";
	}

	public void addParameter(WordCompiler compiler, Token token) {
		mParameters.add(token);
		var declaration = new LeekVariableDeclarationInstruction(compiler, token, this);
		mParameterDeclarations.add(declaration);
		addVariable(new LeekVariable(token, VariableType.ARGUMENT, declaration));
	}

	@Override
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
		if (variable.equals("class")) return new LeekVariable(new Token("class"), VariableType.THIS_CLASS);
		if (!isStatic) {
			if (variable.equals("this")) return new LeekVariable(new Token("this"), VariableType.THIS);
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

		writer.addLine("", getLocation());

		super.writeJavaCode(mainblock, writer);
		if (mEndInstruction == 0) {
			writer.addLine("return null;");
		}
	}

	public ClassDeclarationInstruction getClassDeclaration() {
		return this.clazz;
	}

	public List<Token> getParameters() {
		return mParameters;
	}

	public ArrayList<LeekVariableDeclarationInstruction> getParametersDeclarations() {
		return mParameterDeclarations;
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
