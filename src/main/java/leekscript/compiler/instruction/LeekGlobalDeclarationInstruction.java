package leekscript.compiler.instruction;

import leekscript.compiler.Token;
import leekscript.common.Type;
import leekscript.common.Type.CastType;
import leekscript.compiler.AnalyzeError;
import leekscript.compiler.JavaWriter;
import leekscript.compiler.Location;
import leekscript.compiler.NarrowingInfo;
import leekscript.compiler.WordCompiler;
import leekscript.compiler.AnalyzeError.AnalyzeErrorLevel;
import leekscript.compiler.bloc.MainLeekBlock;
import leekscript.compiler.exceptions.LeekCompilerException;
import leekscript.compiler.expression.Expression;
import leekscript.compiler.expression.LeekExpressionException;
import leekscript.compiler.expression.LeekType;
import leekscript.compiler.expression.LeekVariable;
import leekscript.compiler.expression.LeekVariable.VariableType;
import leekscript.common.Annotation;
import leekscript.common.Error;

import java.util.EnumSet;

public class LeekGlobalDeclarationInstruction extends LeekInstruction implements leekscript.compiler.Annotatable {

	private final Token token;
	private final Token variableToken;
	private Expression mValue = null;
	private Type type;
	private LeekType leekType;
	private LeekVariable variable;
	// Lazy : la grande majorité des globals n'ont aucune annotation.
	private EnumSet<Annotation> annotations = null;
	// true (global) = champ initialisé UNE fois (garde g_init, persiste sur le combat).
	// false (var top-level v5) = champ RÉ-INITIALISÉ chaque tour (pas de garde g_init).
	private boolean persistent = true;
	// Module v5 propriétaire (préfixe le nom du champ). null = module principal.
	private leekscript.compiler.bloc.Module module;

	public LeekGlobalDeclarationInstruction(Token token, Token variableToken, LeekType leekType) {
		this.token = token;
		this.variableToken = variableToken;
		this.leekType = leekType;
	}

	public void setPersistent(boolean persistent) {
		this.persistent = persistent;
	}

	public boolean isPersistent() {
		return persistent;
	}

	public leekscript.compiler.bloc.Module getModule() {
		return module;
	}

	/** Type écrit dans la déclaration (`export var integer x`). */
	public boolean isTyped() {
		return leekType != null;
	}

	public Token getNameToken() {
		return variableToken;
	}

	public void setModule(leekscript.compiler.bloc.Module module) {
		this.module = module;
	}

	/** Nom du champ Java (`g_<internalName>`), préfixé par le module pour éviter les
	 * collisions entre modules ; vide pour le module principal → byte-identique. */
	public String getInternalName() {
		return (module == null ? "" : module.prefix()) + variableToken.getWord();
	}

	@Override
	public void addAnnotation(Annotation a) {
		if (annotations == null) annotations = EnumSet.noneOf(Annotation.class);
		annotations.add(a);
	}

	public void setValue(Expression value) {
		mValue = value;
	}

	public String getName() {
		return variableToken.getWord();
	}

	@Override
	public String getCode() {
		if (mValue != null) {
			return "global " + variableToken.getWord() + " = " + mValue.toString() + ";";
		} else {
			return "global " + variableToken.getWord() + ";";
		}
	}

	@Override
	public void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer, boolean parenthesis) {
		final String name = getInternalName();
		// Persistant (global) : init une seule fois, gardée par g_init. Non persistant
		// (var top-level v5) : ré-init nue, l'assignation re-tourne à chaque runIA.
		if (persistent) writer.addCode("if (!g_init_" + name + ") { ");
		if (mainblock.getWordCompiler().getVersion() >= 2) {
			writer.addCode("g_" + name + " = ");
			if (mValue != null) {
				if (variable.getType() != Type.ANY) {
					writer.addCode("(" + variable.getType().getJavaPrimitiveName(mainblock.getVersion()) + ") ");
				}
				if (mValue.getOperations() > 0) writer.addCode("ops(");
				writer.compileConvert(mainblock, 0, mValue, variable.getType(), false);
				if (mValue.getOperations() > 0) writer.addCode(", " + mValue.getOperations() + ")");
			} else {
				writer.addCode(this.variable.getType().getDefaultValue(writer, mainblock.getVersion()));
			}
		} else {
			// Même type que la déclaration du champ (MainLeekBlock) : en strict, celui inféré de la
			// valeur, que la variable perd à sa première affectation (#5324)
			writer.addCode("g_" + name + " = new Box<" + type.getJavaName(mainblock.getVersion()) + ">(" + writer.getAIThis() + ", ");
			if (mValue != null) mValue.compileL(mainblock, writer, false);
			else writer.addCode("null");
			writer.addCode(", " + (mValue != null ? mValue.getOperations() : 0) + ")");
		}
		writer.addCode(";");
		if (persistent) writer.addCode(" g_init_" + name + " = true;");
		if (mainblock.getWordCompiler().getVersion() >= 2) writer.addCode(" ops(1);");
		writer.addLine(persistent ? " }" : "", getLocation());
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
		// On ajoute la variable
		this.variable = new LeekVariable(compiler, variableToken, VariableType.GLOBAL, leekType == null ? Type.ANY : leekType.getType());
		this.variable.setGlobalDeclaration(this);
		this.variable.setGlobalPrefix(module == null ? "" : module.prefix());
		this.type = this.variable.getType();
		if (annotations != null) {
			for (var a : annotations) this.variable.addAnnotation(a);
		}
		// Registre module-aware (clé = nom interne préfixé), pour résoudre les imports.
		compiler.getMainBlock().addGlobalVar(getInternalName(), this.variable);
		// Le module principal garde ses globales dans le bloc partagé (résolution par
		// chaîne de blocs, byte-identique) ; un module importé NON (anti-fuite + collision).
		if (module == null || module.isMain()) {
			compiler.getCurrentBlock().addVariable(this.variable);
		}
	}

	@Override
	public void preAnalyze(WordCompiler compiler) throws LeekCompilerException {
		// // On ajoute la variable
		// this.variable = new LeekVariable(compiler, variableToken, VariableType.GLOBAL, leekType == null ? Type.ANY : leekType.getType());
		// this.type = this.variable.getType();
		// compiler.getCurrentBlock().addVariable(this.variable);

		if (mValue != null) {
			mValue.preAnalyze(compiler);
		}
		if (variable != null) variable.initializedBy(mValue);
	}

	@Override
	public void analyze(WordCompiler compiler) throws LeekCompilerException {
		if (mValue != null) {
			mValue.analyze(compiler);

			// Vérification du type de l'expression
			if (this.leekType != null) {
				var cast = NarrowingInfo.warningCast(this.leekType.getType(), mValue);
				if (cast.ordinal() > CastType.UPCAST.ordinal()) {

					if (cast == CastType.INCOMPATIBLE || compiler.getMainBlock().isStrict()) {
						var level = cast == CastType.INCOMPATIBLE ? AnalyzeErrorLevel.ERROR : AnalyzeErrorLevel.WARNING;
						var error = cast == CastType.INCOMPATIBLE ? Error.ASSIGNMENT_INCOMPATIBLE_TYPE : Error.DANGEROUS_CONVERSION_VARIABLE;

						compiler.addError(new AnalyzeError(mValue.getLocation(), level, error, new String[] {
							mValue.toString(),
							mValue.getType().toString(),
							this.token.getWord(),
							this.leekType.getType().toString(),
						}));
					}
				}
			}

			// LS5+ : la variable prend le type de l'expression si pas de type manuel
			if (compiler.getMainBlock().isStrict() && this.variable != null && this.leekType == null) {
				this.type = mValue.getType();
				this.variable.setType(mValue.getType());
			}
		}
	}

	@Override
	public int getOperations() {
		return 0;
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
