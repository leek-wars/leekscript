package leekscript.compiler.instruction;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import leekscript.compiler.AIFile;
import leekscript.compiler.JavaWriter;
import leekscript.compiler.Location;
import leekscript.compiler.Token;
import leekscript.compiler.WordCompiler;
import leekscript.compiler.bloc.MainLeekBlock;
import leekscript.compiler.exceptions.LeekCompilerException;
import leekscript.compiler.expression.Expression;
import leekscript.compiler.expression.LeekVariable;
import leekscript.compiler.expression.LeekVariable.VariableType;
import leekscript.common.EnumType;
import leekscript.common.EnumValueType;
import leekscript.common.Type;

public class EnumDeclarationInstruction extends LeekInstruction {

	private final Token token;
	private final MainLeekBlock mainBlock;
	private final LinkedHashMap<String, LeekVariable> constants = new LinkedHashMap<>();
	private final LinkedHashMap<String, Expression> values = new LinkedHashMap<>();
	private final ArrayList<String> constantOrder = new ArrayList<>();
	public Type enumType;
	public Type enumValueType;

	public EnumDeclarationInstruction(Token token, int line, AIFile ai, MainLeekBlock block) {
		this.token = token;
		this.mainBlock = block;
		this.enumType = new EnumType(this);
		this.enumValueType = new EnumValueType(this);
	}

	public String getName() {
		return token.getWord();
	}

	public void addConstant(Token nameToken) {
		addConstant(nameToken, null);
	}

	public void addConstant(Token nameToken, Expression value) {
		String name = nameToken.getWord();
		// Duplicate constants are reported at parse time in WordCompiler.enumDeclaration
		if (constants.containsKey(name)) return;
		constantOrder.add(name);
		constants.put(name, new LeekVariable(nameToken, VariableType.STATIC_FIELD, enumType, true));
		values.put(name, value);
	}

	public LinkedHashMap<String, LeekVariable> getConstants() {
		return constants;
	}

	public Expression getValue(String name) {
		return values.get(name);
	}

	public ArrayList<String> getConstantOrder() {
		return constantOrder;
	}

	public LeekVariable getConstant(String name) {
		return constants.get(name);
	}

	@Override
	public String getCode() {
		String r = "enum " + token.getWord() + " {\n";
		for (String name : constantOrder) {
			r += "\t" + name;
			var value = values.get(name);
			if (value != null) {
				r += " = " + value.toString();
			}
			r += ",\n";
		}
		r += "}";
		return r;
	}

	@Override
	public int getEndBlock() {
		return 0;
	}

	@Override
	public boolean putCounterBefore() {
		return false;
	}

	public void declare(WordCompiler compiler) {
		compiler.getCurrentBlock().addVariable(new LeekVariable(token, VariableType.ENUM, this.enumValueType, this));
	}

	public void preAnalyze(WordCompiler compiler) throws LeekCompilerException {
		// Pre-analyze enum constant value expressions, if any
		for (var entry : values.entrySet()) {
			var expr = entry.getValue();
			if (expr != null) {
				expr.preAnalyze(compiler);
			}
		}
	}

	public void analyze(WordCompiler compiler) throws LeekCompilerException {
		// Analyze enum constant value expressions
		for (var entry : values.entrySet()) {
			var expr = entry.getValue();
			if (expr != null) {
				expr.analyze(compiler);
			}
		}
	}

	@Override
	public Location getLocation() {
		return token.getLocation();
	}

	@Override
	public int getNature() {
		return 0;
	}

	@Override
	public Type getType() {
		return this.enumType;
	}

	@Override
	public String toString() {
		return getCode();
	}

	@Override
	public boolean validExpression(WordCompiler compiler, MainLeekBlock mainblock) {
		return false;
	}

	public Type getEnumValueType() {
		return enumValueType;
	}

	public void declareJava(MainLeekBlock mainblock, JavaWriter writer) {
		String enumName = "u_" + token.getWord();
		writer.addLine("public EnumLeekValue " + enumName + " = new EnumLeekValue(this, \"" + token.getWord() + "\");");
	}

	public void createJava(MainLeekBlock mainblock, JavaWriter writer) {
		String enumName = "u_" + token.getWord();
		for (String name : constantOrder) {
			var value = values.get(name);
			writer.addCode(enumName + ".addConstant(\"" + name + "\", ");
			if (value != null) {
				value.writeJavaCode(mainblock, writer, false);
			} else {
				writer.addCode("\"" + name + "\"");
			}
			writer.addLine(");");
		}
	}

	@Override
	public void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer, boolean parenthesis) {
	}
}
