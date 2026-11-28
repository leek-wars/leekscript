package leekscript.compiler.instruction;

import leekscript.common.CompoundType;
import leekscript.common.LanguageFeature;
import leekscript.common.Type;
import leekscript.common.Type.CastType;
import leekscript.compiler.Token;
import leekscript.compiler.AnalyzeError;
import leekscript.compiler.JavaWriter;
import leekscript.compiler.Location;
import leekscript.compiler.NarrowingInfo;
import leekscript.compiler.WordCompiler;
import leekscript.compiler.AnalyzeError.AnalyzeErrorLevel;
import leekscript.compiler.bloc.AnonymousFunctionBlock;
import leekscript.compiler.bloc.MainLeekBlock;
import leekscript.compiler.exceptions.LeekCompilerException;
import leekscript.compiler.expression.Expression;
import leekscript.compiler.expression.LeekExpressionException;
import leekscript.compiler.expression.LeekExpression;
import leekscript.compiler.expression.LeekFunctionCall;
import leekscript.compiler.expression.LeekTernaire;
import leekscript.compiler.expression.Operators;
import leekscript.common.Error;
import leekscript.compiler.bytecode.BytecodeWriter;
import leekscript.compiler.bytecode.JExpr;
import leekscript.compiler.bytecode.Unsupported;

public class LeekReturnInstruction extends LeekInstruction {

	private final Token token;
	private final Expression expression;
	private final boolean optional;
	private Type returnType;
	// Le Java de la fonction renvoie son type de retour déclaré (une fonction anonyme rend un Object)
	private boolean typedJavaReturn = false;

	public LeekReturnInstruction(Token token, Expression exp, boolean optional) {
		this.token = token;
		this.expression = exp;
		this.optional = optional;
	}

	public Expression getExpression() {
		return expression;
	}

	public boolean isOptional() {
		return optional;
	}

	@Override
	public String getCode() {
		return "return" + (optional ? " ?" : "") + (expression == null ? "" : " " + expression.toString()) + ";";
	}


	@Override
	public void preAnalyze(WordCompiler compiler) throws LeekCompilerException {
		if (expression != null) {
			expression.preAnalyze(compiler);
		}
	}

	@Override
	public void analyze(WordCompiler compiler) throws LeekCompilerException {
		this.returnType = Type.ANY;
		if (expression != null) {
			expression.analyze(compiler);
			// @tailrec : marque les appels récursifs atteignables en position terminale.
			// `return? f()` peut poursuivre l'exécution si f() est faux : non terminal.
			if (!optional) {
				markTailCalls(expression, compiler);
			}
		}

		// #43 : enregistrer ce `return` pour l'inférence du type de retour de la fonction
		// englobante (ignoré au niveau principal et dans les anonymes/méthodes : la fonction
		// courante n'y est pas un FunctionBlock).
		if (compiler.getCurrentFunction() instanceof leekscript.compiler.bloc.FunctionBlock fb) {
			fb.registerReturn(this);
		}

		// Vérification du type de retour
		var functionType = compiler.getCurrentFunction().getType();
		if (functionType != null) {
			this.returnType = functionType.returnType();
		}
		this.typedJavaReturn = !(compiler.getCurrentFunction() instanceof AnonymousFunctionBlock);

		var actualType = expression == null ? Type.VOID : expression.getType();

		var cast = returnType.accepts(actualType);
		// `return?` ne renvoie que si la valeur est vraie, donc jamais null (#5183). Le type privé
		// de null ne sert qu'à retirer un diagnostic, jamais à l'aggraver : un `integer?` renvoyé par
		// une fonction `string?` passerait de warning à erreur et refuserait une IA qui compilait.
		// Sauf vers big_integer : sa conversion, faite avant le test, journalise une erreur sur un
		// null (BigIntegerValue.valueOf), et ce warning le signale.
		if (optional && returnType != Type.BIG_INT && actualType instanceof CompoundType ct && ct.containsNull()) {
			var nonNullType = ct.assertNotNull();
			var nonNullCast = returnType.accepts(nonNullType);
			if (nonNullCast.ordinal() < cast.ordinal()) {
				actualType = nonNullType;
				cast = nonNullCast;
			}
		}
		if (expression != null) cast = NarrowingInfo.warningCast(returnType, expression, cast);
		cast = compiler.getMainBlock().strictContainerCast(cast, returnType, actualType);
		if (cast.ordinal() > CastType.UPCAST.ordinal()) {

			if (cast == CastType.INCOMPATIBLE || compiler.getMainBlock().isStrict()) {
				var level = compiler.getMainBlock().isStrict() && cast == CastType.INCOMPATIBLE ? AnalyzeErrorLevel.ERROR : AnalyzeErrorLevel.WARNING;
				var error = cast == CastType.INCOMPATIBLE ? Error.INCOMPATIBLE_TYPE : Error.DANGEROUS_CONVERSION;

				compiler.addError(new AnalyzeError(getLocation(), level, error, new String[] {
					actualType.toString(),
					compiler.getCurrentFunction().getType().returnType().toString()
				}));
			}
		}
	}

	// #43 : type de la valeur retournée (NULL pour un `return` nu), pour l'inférence du
	// type de retour de la fonction englobante (FunctionBlock.analyze).
	public Type getReturnedType() {
		return expression == null ? Type.NULL : expression.getType();
	}

	// #43 : aligne le returnType (utilisé au codegen pour convertir la valeur retournée)
	// sur le type inféré de la fonction, une fois l'inférence faite.
	public void setInferredReturnType(Type type) {
		this.returnType = type;
	}

	// @tailrec : marque récursivement comme terminaux les appels récursifs atteignables
	// en position de queue depuis cette expression de retour : l'appel lui-même, à travers
	// les parenthèses (trim, `return (f(x))`), les DEUX branches d'un ternaire
	// (`return c ? f(a) : f(b)`) et l'opérande DROIT d'un `&&`/`||` (`return c || f(x)` :
	// `f(x)` coercé en booléen est la valeur retournée quand le court-circuit ne se
	// déclenche pas). L'opérande GAUCHE d'un `&&`/`||` (`return f(x) || c`), les autres
	// opérateurs binaires/unaires (`f(n) + 1`) et la condition d'un ternaire ne propagent
	// PAS la position terminale.
	private void markTailCalls(Expression expr, WordCompiler compiler) {
		if (expr == null) return; // branche de ternaire incomplète (entrée malformée)
		var e = expr.trim();
		if (e instanceof LeekFunctionCall fc) {
			var target = fc.getResolvedCallable();
			if (target != null && target == compiler.getCurrentFunction()) {
				fc.setTailPosition(true);
			}
		} else if (e instanceof LeekTernaire t) {
			markTailCalls(t.getExpression1(), compiler);
			markTailCalls(t.getExpression2(), compiler);
		} else if (e instanceof LeekExpression le && (le.getOperator() == Operators.AND || le.getOperator() == Operators.OR)) {
			markTailCalls(le.getExpression2(), compiler);
		}
	}

	@Override
	public void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer, boolean parenthesis) {
		if (writer.currentBlock == mainblock) {
			mainblock.writeBeforeReturn(writer);
		}
		if (expression == null) {
			writer.addPosition(token);
			// `return?` nu reste conditionnel pour l'analyse (getEndBlock) : `if (true)`, comme le bouclier
			// de SwitchBlock, évite que javac rejette le code émis après (« unreachable statement »).
			writer.addCode(optional ? "if (true) return null;" : "return null;");
		} else {
			var finalExpression = expression.trim();
			if (finalExpression.getOperations() > 0) {
				writer.addCode("ops(" + finalExpression.getOperations() + "); ");
			}
			if (optional) {
				// La conversion précède le test : vers un Array/Map/Set, toArray/toMap/toSet planteraient
				// sur un null que bool() doit simplement écarter (#5183), même sous un type statique non
				// nullable (champ non initialisé, défaut `= null`). Leur variante OrNull, identique hors
				// null, reçoit l'expression telle quelle : pas d'écriture brute, que javac refuse pour les
				// expressions émises en Object (`a?.x`, `m[k] ??= v`…).
				// `$` : ne masque aucun champ (cf. LeekFunctionCall)
				String r = "$ret" + writer.nextId(mainblock);
				writer.addCode(returnType.getJavaName(mainblock.getVersion()) + " " + r + " = ");
				var converter = orNullConverter(mainblock, finalExpression.getType());
				if (converter != null) {
					writer.addCode(converter);
					finalExpression.writeJavaCode(mainblock, writer, false);
					writer.addCode(")");
				} else {
					writer.compileConvertTyped(mainblock, 0, finalExpression, returnType, false);
				}
				writer.addLine("; if (bool(" + r + ")) return " + r + ";", getLocation());
			} else {
				writer.addCode("return ");
				if (mainblock.getWordCompiler().getVersion() == 1 && !convertsV1Return(finalExpression)) {
					finalExpression.compileL(mainblock, writer, false);
				} else {
					writer.compileConvertTyped(mainblock, 0, finalExpression, returnType, false);
				}
				writer.addLine(";", getLocation());
			}
		}
	}

	@Override
	public void writeBytecode(MainLeekBlock mainblock, BytecodeWriter writer) {
		writer.line(getLocation());
		if (expression == null) {
			// (`return?` nu : `if (true) return null;`, le même bytecode)
			writer.emitReturn(JExpr.nul(), writer.returnType());
			return;
		}
		var finalExpression = expression.trim();
		if (finalExpression.getOperations() > 0) writer.emitStatement(JExpr.call("ops", JExpr.of(finalExpression.getOperations())));
		if (optional) {
			// `T $ret = <valeur>; if (bool($ret)) return $ret;`
			var converter = orNullConverter(mainblock, finalExpression.getType());
			JExpr value;
			if (converter != null) {
				var name = converter.substring(0, converter.indexOf('('));
				value = converter.contains("(0, ") ? JExpr.call(name, JExpr.of(0), writer.expr(mainblock, finalExpression)) : JExpr.call(name, writer.expr(mainblock, finalExpression));
			} else {
				value = writer.compileConvertTyped(mainblock, 0, finalExpression, returnType);
			}
			var r = writer.declare(new Object(), writer.javaClass(returnType), "$ret");
			writer.emitStatement(new JExpr.Assign(r, value));
			var skip = writer.newLabel();
			writer.jump(JExpr.call("bool", new JExpr.Read(r)), false, skip);
			writer.emitReturn(new JExpr.Read(r), writer.returnType());
			writer.bind(skip);
			return;
		}
		if (mainblock.getWordCompiler().getVersion() == 1 && !convertsV1Return(finalExpression)) {
			writer.emitReturn(finalExpression.bytecodeL(mainblock, writer), writer.returnType());
			return;
		}
		writer.emitReturn(writer.compileConvertTyped(mainblock, 0, finalExpression, returnType), writer.returnType());
	}

	/**
	 * v1 : une variable se renvoie par son Box (compileL), une opération sur Box rend un Object.
	 * Une fonction au type de retour déclaré le déclare aussi en Java, qui n'y accepte ni l'un ni
	 * l'autre (#5323) : ceux-là sont convertis, toute autre valeur s'écrit comme avant.
	 */
	private boolean convertsV1Return(Expression expression) {
		if (!typedJavaReturn || returnType.getJavaPrimitiveName(1).equals("Object")) return false;
		var type = expression.getType();
		return expression.isLeftValue() || expression.hasObjectJavaResult() || type != Type.NULL && type.getJavaPrimitiveName(1).equals("Object");
	}

	/**
	 * Ouverture de la variante OrNull du convertisseur qu'émettrait compileConvert vers le type de
	 * retour Array/Map/Set (même forme, `f(0, expression)`), ou null s'il n'en émet aucun. En v5,
	 * integer et real aussi : longintNotNull()/realNotNull() lèveraient sur le null que bool()
	 * doit écarter (#5389).
	 */
	private String orNullConverter(MainLeekBlock mainblock, Type valueType) {
		if (returnType.accepts(valueType).ordinal() <= CastType.EQUALS.ordinal()) return null;
		if (returnType.isArray()) return (mainblock.getVersion() >= 4 ? "toArrayOrNull" : "toLegacyArrayOrNull") + "(0, ";
		if (returnType.isMap()) return "toMapOrNull(0, ";
		if (returnType.isSet()) return "toSetOrNull(0, ";
		if (LanguageFeature.NULL_TO_NUMBER_ERROR.enabledIn(mainblock.getVersion()) && valueType.canBeNull()) {
			if (returnType == Type.INT) return "longintOrNull(";
			if (returnType == Type.REAL) return "realOrNull(";
		}
		return null;
	}

	@Override
	public int getEndBlock() {
		return optional ? 0 : 1;
	}

	@Override
	public boolean putCounterBefore() {
		return true;
	}

	@Override
	public int getOperations() {
		return 0;
	}

	public Location getLocation() {
		if (expression == null) {
			return token.getLocation();
		}
		return new Location(token.getLocation(), expression.getLocation());
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
