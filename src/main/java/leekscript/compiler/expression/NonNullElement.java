package leekscript.compiler.expression;

import leekscript.common.Type;
import leekscript.compiler.JavaWriter;
import leekscript.compiler.Location;
import leekscript.compiler.WordCompiler;
import leekscript.compiler.bloc.MainLeekBlock;
import leekscript.compiler.exceptions.LeekCompilerException;

/**
 * v5 : valeur ajoutée par `a += x` à un tableau ou un ensemble typé d'élément non nullable,
 * écrite via nonNullElement() comme pour push (#5349). Déjà analysée : ne fait que l'envelopper.
 */
public class NonNullElement extends Expression {

	private final Expression value;
	private final Type elementType;

	public NonNullElement(Expression value, Type elementType) {
		this.value = value;
		this.elementType = elementType;
		this.operations = value.getOperations();
	}

	@Override
	public int getNature() {
		return value.getNature();
	}

	@Override
	public Type getType() {
		return value.getType();
	}

	@Override
	public boolean hasObjectJavaResult() {
		return true;
	}

	@Override
	public String toString() {
		return value.toString();
	}

	@Override
	public void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer, boolean parenthesis) {
		writer.writeElement(mainblock, elementType, value);
	}

	@Override
	public boolean validExpression(WordCompiler compiler, MainLeekBlock mainblock) throws LeekExpressionException {
		return value.validExpression(compiler, mainblock);
	}

	@Override
	public void preAnalyze(WordCompiler compiler) throws LeekCompilerException {}

	@Override
	public void analyze(WordCompiler compiler) throws LeekCompilerException {}

	@Override
	public Location getLocation() {
		return value.getLocation();
	}
}
