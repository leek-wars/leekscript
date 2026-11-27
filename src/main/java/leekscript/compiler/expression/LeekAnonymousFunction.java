package leekscript.compiler.expression;

import leekscript.common.Type;
import leekscript.compiler.JavaWriter;
import leekscript.compiler.Location;
import leekscript.compiler.Token;
import leekscript.compiler.WordCompiler;
import leekscript.compiler.bloc.AnonymousFunctionBlock;
import leekscript.compiler.bloc.MainLeekBlock;
import leekscript.compiler.exceptions.LeekCompilerException;
import leekscript.compiler.bytecode.BytecodeWriter;
import leekscript.compiler.bytecode.JExpr;
import leekscript.runner.AI;
import leekscript.runner.values.FunctionLeekValue;
import java.lang.classfile.ClassFile;
import java.util.List;

public class LeekAnonymousFunction extends Expression {

	private final AnonymousFunctionBlock mBlock;
	private final Token token;

	public LeekAnonymousFunction(AnonymousFunctionBlock block, Token token) {
		mBlock = block;
		this.token = token;
	}

	@Override
	public int getNature() {
		return FUNCTION;
	}

	@Override
	public Type getType() {
		return mBlock.getType();
	}

	@Override
	public String toString() {
		return mBlock.getCode();
	}

	@Override
	public boolean validExpression(WordCompiler compiler, MainLeekBlock mainblock) throws LeekExpressionException {
		return true;
	}

	@Override
	public void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer, boolean parenthesis) {
		writer.addCode("new FunctionLeekValue(" + mBlock.countParameters() + ") {");
		mBlock.writeJavaCode(mainblock, writer, false);
		writer.addCode("}");
	}

	@Override
	public JExpr toBytecode(MainLeekBlock mainblock, BytecodeWriter writer) {
		var runParams = List.<Class<?>>of(AI.class, Object.class, Object[].class);
		return writer.anonymous(FunctionLeekValue.class, List.of(JExpr.of(mBlock.countParameters())),
			List.of(new BytecodeWriter.AnonymousMethod("run", ClassFile.ACC_PUBLIC | ClassFile.ACC_VARARGS, runParams, Object.class, () -> mBlock.writeBytecodeBody(mainblock, writer))));
	}

	@Override
	public void preAnalyze(WordCompiler compiler) throws LeekCompilerException {
		var previousFunction = compiler.getCurrentFunction();
		compiler.setCurrentFunction(mBlock);
		mBlock.preAnalyze(compiler);
		compiler.setCurrentFunction(previousFunction);
	}

	@Override
	public void analyze(WordCompiler compiler) throws LeekCompilerException {
		var previousFunction = compiler.getCurrentFunction();
		compiler.setCurrentFunction(mBlock);
		mBlock.analyze(compiler);
		compiler.setCurrentFunction(previousFunction);
	}

	@Override
	public Location getLocation() {
		return new Location(token.getLocation(), mBlock.getLocation());
	}
}
