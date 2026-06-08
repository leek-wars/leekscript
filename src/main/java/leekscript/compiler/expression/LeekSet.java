package leekscript.compiler.expression;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.stream.Collectors;

import leekscript.common.Type;
import leekscript.compiler.Hover;
import leekscript.compiler.Token;
import leekscript.compiler.JavaWriter;
import leekscript.compiler.Location;
import leekscript.compiler.WordCompiler;
import leekscript.compiler.bloc.MainLeekBlock;
import leekscript.compiler.exceptions.LeekCompilerException;

public class LeekSet extends Expression {

	/**
	 * Un élément d'un littéral set : soit une valeur simple (`end == null`), soit
	 * un intervalle d'entiers `start..end` (#2335) expansé au runtime.
	 */
	private static class SetElement {
		final Expression start;
		final Expression end;
		SetElement(Expression start, Expression end) {
			this.start = start;
			this.end = end;
		}
		boolean isRange() {
			return end != null;
		}
	}

	private final ArrayList<SetElement> mElements = new ArrayList<SetElement>();
	private Token openingToken;
	private Token closingToken;

	public Type type = Type.SET;

	public LeekSet(Token openingToken) {
		this.openingToken = openingToken;
	}

	public void addValue(Expression param) {
		mElements.add(new SetElement(param, null));
	}

	public void addRange(Expression start, Expression end) {
		mElements.add(new SetElement(start, end));
	}

	public void setClosingToken(Token closingToken) {
		this.closingToken = closingToken;
		closingToken.setExpression(this);
		openingToken.setExpression(this);
	}

	@Override
	public int getNature() {
		return ARRAY;
	}

	@Override
	public Type getType() {
		return type;
	}

	@Override
	public String toString() {
		return mElements.stream().map(e -> e.isRange() ? e.start.toString() + ".." + e.end.toString() : e.start.toString()).collect(Collectors.joining(", ", "<", ">"));
	}

	@Override
	public boolean validExpression(WordCompiler compiler, MainLeekBlock mainblock) throws LeekExpressionException {
		for (var element : mElements) {
			element.start.validExpression(compiler, mainblock);
			if (element.isRange()) {
				element.end.validExpression(compiler, mainblock);
			}
		}
		return true;
	}

	@Override
	public void preAnalyze(WordCompiler compiler) throws LeekCompilerException {
		for (var element : mElements) {
			element.start.preAnalyze(compiler);
			if (element.isRange()) {
				element.end.preAnalyze(compiler);
			}
		}
	}

	@Override
	public void analyze(WordCompiler compiler) throws LeekCompilerException {
		operations = 0;

		var types = new HashSet<Type>();
		for (var element : mElements) {
			element.start.analyze(compiler);
			operations += 2 + element.start.getOperations();
			if (element.isRange()) {
				element.end.analyze(compiler);
				operations += 2 + element.end.getOperations();
				// Un intervalle `a..b` ne produit que des entiers
				types.add(Type.INT);
			} else {
				types.add(element.start.getType());
			}
		}

		this.type = Type.set(types.size() == 0 ? Type.VOID : Type.compound(types));
	}

	private boolean hasRange() {
		for (var element : mElements) {
			if (element.isRange()) return true;
		}
		return false;
	}

	@Override
	public void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer, boolean parenthesis) {
		if (!hasRange()) {
			// Cas courant : que des valeurs simples
			writer.addCode("new SetLeekValue(" + writer.getAIThis() + ", new Object[] { ");
			for (int i = 0; i < mElements.size(); i++) {
				if (i != 0) writer.addCode(", ");
				mElements.get(i).start.writeJavaCode(mainblock, writer, false);
			}
			writer.addCode(" })");
			return;
		}
		// Avec intervalle(s) : on construit le set par chaînage, chaque helper retourne
		// le set pour permettre l'imbrication. Le dernier élément est le plus externe.
		for (int i = mElements.size() - 1; i >= 0; i--) {
			writer.addCode(mElements.get(i).isRange() ? "setLiteralRange(" : "setLiteralAdd(");
		}
		writer.addCode("new SetLeekValue(" + writer.getAIThis() + ")");
		for (int i = 0; i < mElements.size(); i++) {
			var element = mElements.get(i);
			writer.addCode(", ");
			element.start.writeJavaCode(mainblock, writer, false);
			if (element.isRange()) {
				writer.addCode(", ");
				element.end.writeJavaCode(mainblock, writer, false);
			}
			writer.addCode(")");
		}
	}

	@Override
	public Location getLocation() {
		return new Location(openingToken.getLocation(), closingToken.getLocation());
	}

	@Override
	public Hover hover(Token token) {
		var hover = new Hover(getType(), getLocation(), toString());
		hover.setSize(mElements.size());
		return hover;
	}
}
