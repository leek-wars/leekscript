package leekscript.compiler.instruction;

import java.util.HashMap;

import leekscript.common.Type;
import leekscript.compiler.JavaWriter;
import leekscript.compiler.Location;
import leekscript.compiler.WordCompiler;
import leekscript.compiler.bloc.MainLeekBlock;
import leekscript.compiler.bloc.Module;
import leekscript.compiler.expression.LeekExpressionException;
import leekscript.compiler.expression.LeekVariable;
import leekscript.compiler.expression.LeekVariable.VariableType;

/**
 * Borne le code de haut niveau d'un module importé (v5) dans les instructions du bloc principal :
 * le marqueur d'ouverture analyse la suite dans le module (ses imports, ses déclarations privées)
 * avec ses propres variables locales, celui de fermeture rétablit l'appelant. En Java, la paire
 * émet un bloc `{ … }` : deux modules peuvent déclarer chacun un `var x`.
 */
public class ModuleScopeInstruction extends LeekInstruction {

	private final Module module;
	private final ModuleScopeInstruction opening;
	private final Location location;
	// Rétablis par le marqueur de fermeture
	private Module callerModule;
	private HashMap<String, LeekVariable> callerVariables;

	private ModuleScopeInstruction(Module module, ModuleScopeInstruction opening, Location location) {
		this.module = module;
		this.opening = opening;
		this.location = location;
	}

	public static ModuleScopeInstruction open(Module module, Location location) {
		return new ModuleScopeInstruction(module, null, location);
	}

	public ModuleScopeInstruction close() {
		return new ModuleScopeInstruction(module, this, location);
	}

	private void enterOrExit(WordCompiler compiler) {
		var main = compiler.getMainBlock();
		if (opening == null) {
			callerModule = compiler.getModule();
			compiler.setModule(module);
			// Les locales de l'appelant ne sont pas visibles du module ; le reste (classes,
			// fonctions, globales) l'est comme avant, filtré par LeekVariable.preAnalyze.
			callerVariables = main.getVariables();
			var variables = new HashMap<String, LeekVariable>();
			if (callerVariables != null) {
				for (var entry : callerVariables.entrySet()) {
					if (entry.getValue().getVariableType() != VariableType.LOCAL) variables.put(entry.getKey(), entry.getValue());
				}
			}
			main.setVariables(variables);
		} else {
			compiler.setModule(opening.callerModule);
			main.setVariables(opening.callerVariables);
		}
	}

	@Override
	public void preAnalyze(WordCompiler compiler) {
		enterOrExit(compiler);
	}

	@Override
	public void analyze(WordCompiler compiler) {
		enterOrExit(compiler);
	}

	@Override
	public void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer, boolean parenthesis) {
		writer.addLine(opening == null ? "{" : "}");
	}

	@Override
	public String getCode() {
		return "";
	}

	@Override
	public int getEndBlock() {
		return 0;
	}

	@Override
	public boolean putCounterBefore() {
		return false;
	}

	@Override
	public Location getLocation() {
		return location;
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
		return "";
	}

	@Override
	public boolean validExpression(WordCompiler compiler, MainLeekBlock mainblock) throws LeekExpressionException {
		return true;
	}
}
