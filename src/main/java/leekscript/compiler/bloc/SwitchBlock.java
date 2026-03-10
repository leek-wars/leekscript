package leekscript.compiler.bloc;

import java.util.ArrayList;

import leekscript.common.Type;
import leekscript.compiler.JavaWriter;
import leekscript.compiler.Location;
import leekscript.compiler.Token;
import leekscript.compiler.WordCompiler;
import leekscript.compiler.exceptions.LeekCompilerException;
import leekscript.compiler.expression.Expression;
import leekscript.compiler.expression.LeekExpressionException;

public class SwitchBlock extends AbstractLeekBlock {

	private final Token token;
	private Expression mExpression;
	private final ArrayList<SwitchCase> mCases = new ArrayList<>();
	private final int mId;

	public static class SwitchCase {
		public final ArrayList<Expression> values; // empty for default
		public final AbstractLeekBlock body;
		public final boolean isDefault;

		public SwitchCase(ArrayList<Expression> values, AbstractLeekBlock body, boolean isDefault) {
			this.values = values;
			this.body = body;
			this.isDefault = isDefault;
		}
	}

	public SwitchBlock(AbstractLeekBlock parent, MainLeekBlock main, Token token) {
		super(parent, main);
		this.token = token;
		this.mId = main.getCount();
	}

	public void setExpression(Expression expression) {
		mExpression = expression;
	}

	public void addCase(SwitchCase switchCase) {
		mCases.add(switchCase);
	}

	@Override
	public String getCode() {
		StringBuilder str = new StringBuilder("switch (");
		str.append(mExpression.toString()).append(") {\n");
		for (var c : mCases) {
			if (c.isDefault) {
				str.append("default:\n");
			} else {
				for (var v : c.values) {
					str.append("case ").append(v.toString()).append(":\n");
				}
			}
			str.append(c.body.getCode());
		}
		str.append("}");
		return str.toString();
	}

	@Override
	public void preAnalyze(WordCompiler compiler) throws LeekCompilerException {
		mExpression.preAnalyze(compiler);
		for (var c : mCases) {
			for (var v : c.values) {
				v.preAnalyze(compiler);
			}
			c.body.preAnalyze(compiler);
		}
	}

	@Override
	public void analyze(WordCompiler compiler) throws LeekCompilerException {
		mExpression.analyze(compiler);
		for (var c : mCases) {
			for (var v : c.values) {
				v.analyze(compiler);
			}
			c.body.analyze(compiler);
		}
	}

	@Override
	public void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer, boolean parenthesis) {
		String swVar = "__sw_" + mId;
		String siVar = "__si_" + mId;

		writer.addLine("{");

		// Store the switch expression in a temp variable
		writer.addCode("Object " + swVar + " = ");
		mExpression.writeJavaCode(mainblock, writer, false);
		writer.addLine(";", getLocation());

		if (writer.isOperationsEnabled() && mExpression.getOperations() > 0) {
			writer.addCounter(mExpression.getOperations());
		}

		writer.addLine("int " + siVar + " = -1;");

		// Generate if/else if chain to compute the switch index
		boolean first = true;
		int index = 0;
		for (var c : mCases) {
			if (!c.isDefault) {
				if (first) {
					writer.addCode("if (");
					first = false;
				} else {
					writer.addCode("else if (");
				}
				boolean firstValue = true;
				for (var v : c.values) {
					if (!firstValue) {
						writer.addCode(" || ");
					}
					writer.addCode("eq(" + swVar + ", ");
					v.writeJavaCode(mainblock, writer, false);
					writer.addCode(")");
					firstValue = false;
				}
				writer.addLine(") " + siVar + " = " + index + ";");
			}
			index++;
		}

		// Generate Java switch on the index
		writer.addLine("switch (" + siVar + ") {");

		index = 0;
		for (var c : mCases) {
			if (c.isDefault) {
				writer.addLine("default: {");
			} else {
				writer.addLine("case " + index + ": {");
			}
			writer.addCounter(1);
			c.body.writeJavaCode(mainblock, writer, false);
			writer.addLine("}");
			index++;
		}

		writer.addLine("}"); // end switch
		writer.addLine("}"); // end block
	}

	@Override
	public boolean isBreakable() {
		return true;
	}

	@Override
	public int getEndBlock() {
		// Switch always completes if it has a default and all cases return
		boolean hasDefault = false;
		boolean allReturn = true;
		for (var c : mCases) {
			if (c.isDefault) hasDefault = true;
			if (c.body.getEndBlock() != 1) allReturn = false;
		}
		if (hasDefault && allReturn) return 1;
		return 0;
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
		return Type.VOID;
	}

	@Override
	public String toString() {
		return null;
	}

	@Override
	public boolean validExpression(WordCompiler compiler, MainLeekBlock mainblock) throws LeekExpressionException {
		return false;
	}
}
