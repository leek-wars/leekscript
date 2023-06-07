package leekscript.compiler.expression;

import leekscript.common.Type;
import leekscript.compiler.Complete;
import leekscript.compiler.Hover;
import leekscript.compiler.JavaWriter;
import leekscript.compiler.Location;
import leekscript.compiler.Token;
import leekscript.compiler.WordCompiler;
import leekscript.compiler.bloc.MainLeekBlock;
import leekscript.compiler.exceptions.LeekCompilerException;

public abstract class Expression {

	public final static int EXPRESSION = 1;
	public final static int NUMBER = 2;
	public final static int STRING = 3;
	public final static int BOOLEAN = 4;
	public final static int TABULAR_VALUE = 5;
	public final static int VARIABLE = 6;
	public final static int NULL = 7;
	public final static int FUNCTION = 8;
	public final static int GLOBAL = 9;
	public final static int ARRAY = 10;
	public final static int OBJECT = 11;
	public final static int OBJECT_ACCESS = 12;
	public final static int TYPE = 13;

	public int operations = 0;

	public abstract int getNature();

	public abstract Type getType();

	public abstract void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer);

	public void compileL(MainLeekBlock mainblock, JavaWriter writer) {
		writeJavaCode(mainblock, writer);
	}

	public void compileSet(MainLeekBlock mainblock, JavaWriter writer, Expression expr) {
		writeJavaCode(mainblock, writer);
		writer.addCode(" = ");
		writer.compileConvert(mainblock, 0, expr, this.getType());
	}

	public void compileSetCopy(MainLeekBlock mainblock, JavaWriter writer, Expression expr) {
		writeJavaCode(mainblock, writer);
		writer.addCode(" = ");
		writer.compileClone(mainblock, expr);
	}

	public void compileIncrement(MainLeekBlock mainblock, JavaWriter writer) {
		writeJavaCode(mainblock, writer);
		writer.addCode("++");
	}

	public void compileDecrement(MainLeekBlock mainblock, JavaWriter writer) {
		writeJavaCode(mainblock, writer);
		writer.addCode("--");
	}

	public void compilePreIncrement(MainLeekBlock mainblock, JavaWriter writer) {
		writer.addCode("++");
		writeJavaCode(mainblock, writer);
	}

	public void compilePreDecrement(MainLeekBlock mainblock, JavaWriter writer) {
		writer.addCode("--");
		writeJavaCode(mainblock, writer);
	}

	public void compileAddEq(MainLeekBlock mainblock, JavaWriter writer, Expression expr, Type t) {
		writeJavaCode(mainblock, writer);
		writer.addCode(" += ");
		expr.writeJavaCode(mainblock, writer);
	}

	public void compileSubEq(MainLeekBlock mainblock, JavaWriter writer, Expression expr) {
		writeJavaCode(mainblock, writer);
		writer.addCode(" -= ");
		expr.writeJavaCode(mainblock, writer);
	}

	public void compileMulEq(MainLeekBlock mainblock, JavaWriter writer, Expression expr, Type type) {
		writeJavaCode(mainblock, writer);
		writer.addCode(" *= ");
		expr.writeJavaCode(mainblock, writer);
	}

	public void compileDivEq(MainLeekBlock mainblock, JavaWriter writer, Expression expr) {
		writeJavaCode(mainblock, writer);
		writer.addCode(" /= ");
		expr.writeJavaCode(mainblock, writer);
	}

	public void compileIntDivEq(MainLeekBlock mainblock, JavaWriter writer, Expression expr) {
		writeJavaCode(mainblock, writer);
		writer.addCode(" /= ");
		expr.writeJavaCode(mainblock, writer);
	}

	public void compilePowEq(MainLeekBlock mainblock, JavaWriter writer, Expression expr, Type t) {
		writeJavaCode(mainblock, writer);
		writer.addCode(" **= ");
		expr.writeJavaCode(mainblock, writer);
	}

	public void compileModEq(MainLeekBlock mainblock, JavaWriter writer, Expression expr) {
		writeJavaCode(mainblock, writer);
		writer.addCode(" %= ");
		expr.writeJavaCode(mainblock, writer);
	}

	public void compileBitOrEq(MainLeekBlock mainblock, JavaWriter writer, Expression expr) {
		writeJavaCode(mainblock, writer);
		writer.addCode(" |= ");
		expr.writeJavaCode(mainblock, writer);
	}

	public void compileBitAndEq(MainLeekBlock mainblock, JavaWriter writer, Expression expr) {
		writeJavaCode(mainblock, writer);
		writer.addCode(" &= ");
		expr.writeJavaCode(mainblock, writer);
	}

	public void compileBitXorEq(MainLeekBlock mainblock, JavaWriter writer, Expression expr) {
		writeJavaCode(mainblock, writer);
		writer.addCode(" ^= ");
		expr.writeJavaCode(mainblock, writer);
	}

	public void compileShiftLeftEq(MainLeekBlock mainblock, JavaWriter writer, Expression expr) {
		writeJavaCode(mainblock, writer);
		writer.addCode(" <<= ");
		expr.writeJavaCode(mainblock, writer);
	}

	public void compileShiftRightEq(MainLeekBlock mainblock, JavaWriter writer, Expression expr) {
		writeJavaCode(mainblock, writer);
		writer.addCode(" >>= ");
		expr.writeJavaCode(mainblock, writer);
	}

	public void compileShiftUnsignedRightEq(MainLeekBlock mainblock, JavaWriter writer, Expression expr) {
		writeJavaCode(mainblock, writer);
		writer.addCode(" >>>= ");
		expr.writeJavaCode(mainblock, writer);
	}

	public abstract boolean validExpression(WordCompiler compiler, MainLeekBlock mainblock) throws LeekExpressionException;

	public Expression trim() {
		return this;
	}

	public boolean isLeftValue() {
		return false;
	}

	public boolean nullable() {
		return true;
	}

	public void preAnalyze(WordCompiler compiler) throws LeekCompilerException {}

	public abstract void analyze(WordCompiler compiler) throws LeekCompilerException;

	public int getOperations() {
		return operations;
	}

	public abstract Location getLocation();

	public Hover hover(Token token) {
		return new Hover(getType(), getLocation());
	}

	public Complete complete(Token token) {
		return new Complete(this.getType());
	}

	public boolean isFinal() {
		return false;
	}

	public LeekVariable getVariable() {
		return null;
	}
}
