package leekscript.compiler.expression;

import leekscript.common.MapType;
import leekscript.common.Type;
import leekscript.common.Type.CastType;
import leekscript.compiler.AnalyzeError;
import leekscript.compiler.JavaWriter;
import leekscript.compiler.Location;
import leekscript.compiler.Token;
import leekscript.compiler.WordCompiler;
import leekscript.compiler.AnalyzeError.AnalyzeErrorLevel;
import leekscript.compiler.bloc.MainLeekBlock;
import leekscript.compiler.exceptions.LeekCompilerException;
import leekscript.compiler.expression.LeekVariable.VariableType;
import leekscript.runner.AI;
import leekscript.common.ArrayType;
import leekscript.common.ClassType;
import leekscript.common.Error;
import leekscript.common.LegacyArrayType;

public class LeekArrayAccess extends Expression {

	private Expression mTabular;
	private Expression mCase;
	private boolean mLeftValue = false;
	private Token colon;
	private Expression endIndex;
	private Token closingBracket;
	private Token colon2;
	private Expression stride;
	// `analyze()` pose le vrai type, mais l'éditeur interroge aussi des arbres qui
	// n'ont jamais été analysés (une IA dont l'analyse a échoué ou expiré reste en
	// cache côté démon, tokens posés). `getType()` ne doit jamais rendre null : tout
	// le compilateur le déréférence sans test, comme les autres expressions qui
	// partent déjà de ANY.
	private Type type = Type.ANY;
	private boolean optional = false; // accès indexé optionnel `a?[b]`
	private boolean strict; // cf setReturnsObject

	public LeekArrayAccess(Token openingBracket) {
		openingBracket.setExpression(this);
	}

	public void setOptional(boolean optional) {
		this.optional = optional;
	}

	public boolean isOptional() {
		return optional;
	}

	public void setTabular(Expression tabular) {
		mTabular = tabular;
	}

	public void setCase(Expression caseexp) {
		mCase = caseexp;
	}

	public void setColon(Token colon) {
		this.colon = colon;
		if (this.colon != null) {
			colon.setExpression(this);
		}
	}

	public void setEndIndex(Expression endIndex) {
		this.endIndex = endIndex;
	}

	public void setColon2(Token colon2) {
		this.colon2 = colon2;
		if (this.colon2 != null) {
			this.colon2.setExpression(this);
		}
	}

	public void setStride(Expression stride) {
		this.stride = stride;
	}

	public void setClosingBracket(Token closingBracket) {
		this.closingBracket = closingBracket;
		closingBracket.setExpression(this);
	}

	public Expression getTabular() {
		return mTabular;
	}

	public Expression getCase() {
		return mCase;
	}

	@Override
	public int getNature() {
		return TABULAR_VALUE;
	}

	@Override
	public Type getType() {
		return type;
	}

	@Override
	public String toString() {
		return (mTabular == null ? "null" : mTabular.toString()) + "["
			+ (mCase != null ? mCase.toString() : "")
			+ (colon != null ? ":" : "")
			+ (endIndex != null ? endIndex.toString() : "")
			+ (colon2 != null ? ":" : "")
			+ (stride != null ? stride.toString() : "")
		+ "]";
	}

	@Override
	public boolean validExpression(WordCompiler compiler, MainLeekBlock mainblock) throws LeekExpressionException {
		// On doit vérifier qu'on a affaire : soit à une expression tabulaire, soit à une variable, soit à une globale
		// throw new LeekExpressionException(this, "Ce n'est pas un tableau valide");
		if (!mTabular.isLeftValue()) {
			mLeftValue = false;
		}

		// Sinon on valide simplement les deux expressions
		mTabular.validExpression(compiler, mainblock);
		if (mCase != null) {
			mCase.validExpression(compiler, mainblock);
		}
		if (endIndex != null) {
			endIndex.validExpression(compiler, mainblock);
		}
		if (stride != null) {
			stride.validExpression(compiler, mainblock);
		}
		return true;
	}

	@Override
	public void preAnalyze(WordCompiler compiler) throws LeekCompilerException {
		mTabular.preAnalyze(compiler);
		if (mCase != null) {
			mCase.preAnalyze(compiler);
		}
		if (endIndex != null) {
			endIndex.preAnalyze(compiler);
		}
		if (stride != null) {
			stride.preAnalyze(compiler);
		}
	}

	@Override
	public void analyze(WordCompiler compiler) throws LeekCompilerException {

		strict = compiler.getMainBlock().isStrict();
		mTabular.analyze(compiler);
		operations = mTabular.getOperations();

		// Vérification du type du tableau
		if (compiler.getMainBlock().isStrict() && !mTabular.getType().isIndexable()) {
			compiler.addError(new AnalyzeError(mTabular.getLocation(), AnalyzeErrorLevel.WARNING, Error.MAY_NOT_BE_INDEXABLE, new String[] {
				mTabular.toString(),
				mTabular.getType().toString()
			}));
		} else if (!mTabular.getType().canBeIndexable()) {
			var level = compiler.getMainBlock().isStrict() ? AnalyzeErrorLevel.ERROR : AnalyzeErrorLevel.WARNING;
			compiler.addError(new AnalyzeError(mTabular.getLocation(), level, Error.NOT_INDEXABLE, new String[] {
				mTabular.toString(),
				mTabular.getType().toString()
			}));
		}

		if (mCase != null) {
			mCase.analyze(compiler);
			operations += mCase.getOperations();
		}
		if (endIndex != null) {
			endIndex.analyze(compiler);
			operations += endIndex.getOperations();
		}
		if (stride != null) {
			stride.analyze(compiler);
			operations += stride.getOperations();
		}

		// Vérification du type de clé
		if (mCase != null) {
			var cast = mTabular.getType().key().accepts(mCase.getType());
			if (cast == CastType.INCOMPATIBLE) {
				compiler.addError(new AnalyzeError(mCase.getLocation(), AnalyzeErrorLevel.WARNING, Error.INCOMPATIBLE_TYPE, new String[] {
					mCase.getType().toString(),
					mTabular.getType().key().toString(),
				}));
			} else if (compiler.getMainBlock().isStrict() && cast.ordinal() >= CastType.UNSAFE_DOWNCAST.ordinal()) {
				compiler.addError(new AnalyzeError(mCase.getLocation(), AnalyzeErrorLevel.WARNING, Error.DANGEROUS_CONVERSION, new String[] {
					mCase.getType().toString(),
					mTabular.getType().key().toString(),
				}));
			}
		}

		if (colon != null) {
			this.type = mTabular.getType();
		} else {
			var key = mCase instanceof LeekString ls ? ls.getText() : null;
			this.type = mTabular.getType().elementAccess(compiler.getMainBlock().getVersion(), compiler.getMainBlock().isStrict(), key);
		}

		// Accès indexé optionnel `a?[b]` : identique à `a[b]` au runtime (l'indexation
		// renvoie déjà null pour un conteneur null ou un index hors bornes), mais le
		// résultat est explicitement typé `element | null`. Utile surtout en mode
		// strict, où `a[b]` est sinon typé non-null. Résultat nullable ⇒ pas une lvalue.
		if (optional) {
			this.type = Type.compound(this.type, Type.NULL);
		}
	}

	@Override
	public void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer, boolean parenthesis) {
		if (mTabular.getType() == Type.STRING) {
			// Accès indexé sur une chaîne : caractère ou sous-chaîne (comme les listes)
			if (colon != null) {
				if (mCase != null && endIndex != null) {
					writer.addCode("rangeString(");
				} else if (mCase != null) {
					writer.addCode("rangeString_start(");
				} else if (endIndex != null) {
					writer.addCode("rangeString_end(");
				} else {
					writer.addCode("rangeString_all(");
				}
				mTabular.writeJavaCode(mainblock, writer, false);
				if (mCase != null) {
					writer.addCode(", ");
					mCase.writeJavaCode(mainblock, writer, false);
				}
				if (endIndex != null) {
					writer.addCode(", ");
					endIndex.writeJavaCode(mainblock, writer, false);
				}
				if (stride != null) {
					writer.addCode(", ");
					stride.writeJavaCode(mainblock, writer, false);
				}
				writer.addCode(")");
			} else {
				writer.addCode("getString(");
				mTabular.writeJavaCode(mainblock, writer, false);
				writer.addCode(", ");
				mCase.writeJavaCode(mainblock, writer, false);
				writer.addCode(")");
			}
			return;
		}
		if (colon != null) {
			// Type ANY : la valeur peut être un tableau, un intervalle ou une chaîne
			// au runtime, on dispatche donc dynamiquement (rangeDynamic retourne une
			// chaîne pour les strings, un tableau sinon).
			var prefix = mTabular.getType() == Type.ANY ? "rangeDynamic" : "range";
			if (mCase != null && endIndex != null) {
				writer.addCode(prefix + "(");
			} else if (mCase != null) {
				writer.addCode(prefix + "_start(");
			} else if (endIndex != null) {
				writer.addCode(prefix + "_end(");
			} else {
				writer.addCode(prefix + "_all(");
			}
			mTabular.writeJavaCode(mainblock, writer, false);
			if (mCase != null) {
				writer.addCode(", ");
				mCase.writeJavaCode(mainblock, writer, false);
			}
			if (endIndex != null) {
				writer.addCode(", ");
				endIndex.writeJavaCode(mainblock, writer, false);
			}
			if (stride != null) {
				writer.addCode(", ");
				stride.writeJavaCode(mainblock, writer, false);
			}
			writer.addCode(")");
		} else if (mTabular instanceof LeekVariable v && v.getVariableType() == VariableType.THIS && mCase instanceof LeekString ls
				&& mTabular.getType() instanceof ClassType ct && ct.getClassDeclaration().hasField(ls.getText())) {
			// Champ d'instance déclaré : même code que `this.champ`. Tout autre nom passe par
			// l'accès dynamique, sinon javac le résoudrait dans la portée Java de la classe générée.
			writer.addCode(ls.getText());
		} else if (mTabular.getType() instanceof LegacyArrayType) {
			mTabular.writeJavaCode(mainblock, writer, true);
			writer.addCode(".get(");
			mCase.writeJavaCode(mainblock, writer, false);
			writer.addCode(")");
		} else if (mTabular.getType() instanceof ArrayType || mTabular.getType() instanceof MapType) {
			var close = "";
			if (type != Type.ANY) {
				if (parenthesis) writer.addCode("(");
				if (type.isPrimitive()) {
					writer.addCode("(" + type.getJavaPrimitiveName(mainblock.getVersion()) + ") ");
				}
				close = writer.openCastObject(type.getJavaName(mainblock.getVersion()));
			}
			mTabular.writeJavaCode(mainblock, writer, true);
			writer.addCode(".get(");
			mCase.writeJavaCode(mainblock, writer, false);
			writer.addCode(")" + close);
			if (type != Type.ANY) {
				if (parenthesis) writer.addCode(")");
			}
		} else {
			if (this.type != Type.ANY) {
				if (parenthesis) writer.addCode("(");
				writer.addCode("(" + this.type.getJavaName(mainblock.getVersion()) + ") ");
			}
			writer.addCode("get(");
			mTabular.writeJavaCode(mainblock, writer, false);
			writer.addCode(", ");
			mCase.writeJavaCode(mainblock, writer, false);
			writer.addCode(", ");
			writer.addCode(mainblock.getWordCompiler().getCurrentClassVariable());
			writer.addCode(")");
			if (this.type != Type.ANY) {
				if (parenthesis) writer.addCode(")");
			}
		}
	}

	@Override
	public void compileL(MainLeekBlock mainblock, JavaWriter writer, boolean parenthesis) {
		// assert(mLeftValue && !mTabular.nullable());
		writer.addCode("getBox(");
		mTabular.writeJavaCode(mainblock, writer, false);
		writer.addCode(", ");
		mCase.writeJavaCode(mainblock, writer, false);
		writer.addCode(")");
	}

	@Override
	public void compileSet(MainLeekBlock mainblock, JavaWriter writer, Expression expr, boolean parenthesis) {
		// assert(mLeftValue && !mTabular.nullable());

		// La valeur renvoyée par .set()/.put() est la valeur *coercée* stockée dans le
		// conteneur : son type est celui de l'élément, pas celui de l'expression de droite.
		// Caster le résultat vers expr.getType() génère du Java invalide quand les deux
		// diffèrent (ex : une expression real stockée dans un Map<integer, integer> :
		// MapLeekValue.set renvoie un Long, et (Double) Long ne compile pas).
		Type castType = expr.getType();
		if (mTabular.getType() instanceof ArrayType at) {
			castType = at.element();
		} else if (mTabular.getType() instanceof MapType mt) {
			castType = mt.element();
		}
		if (castType != Type.ANY && mainblock.isStrict()) {
			if (parenthesis) writer.addCode("(");
			if (castType.isPrimitive()) {
				writer.addCode("(" + castType.getJavaPrimitiveName(mainblock.getVersion()) + ") ");
			}
			writer.addCode("(" + castType.getJavaName(mainblock.getVersion()) + ") ");
		}
		if (mTabular.getType() instanceof ArrayType at) {
			mTabular.writeJavaCode(mainblock, writer, true);
			// v1-3 : le runtime est LegacyArrayLeekValue, qui n'a que put()
			// (sémantique legacy : clé associative, croissance). (#4465)
			if (mainblock.isStrict() || mainblock.getVersion() <= 3) {
				writer.addCode(".put(");
			} else {
				writer.addCode(".putv4(");
			}
			mCase.writeJavaCode(mainblock, writer, false);
			writer.addCode(", ");
			writeValueWithElementCoercion(mainblock, writer, expr, at.element());
			writer.addCode(")");
		} else if (mTabular.getType() instanceof MapType mt) {
			mTabular.writeJavaCode(mainblock, writer, true);
			writer.addCode(".set(");
			mCase.writeJavaCode(mainblock, writer, false);
			writer.addCode(", ");
			writeValueWithElementCoercion(mainblock, writer, expr, mt.element());
			writer.addCode(")");
		} else {
			if (mainblock.isStrict()) {
				writer.addCode("put(");
			} else {
				writer.addCode("putv4(");
			}
			mTabular.writeJavaCode(mainblock, writer, false);
			writer.addCode(", ");
			mCase.writeJavaCode(mainblock, writer, false);
			writer.addCode(", ");
			expr.writeJavaCode(mainblock, writer, false);
			writer.addCode(", " + mainblock.getWordCompiler().getCurrentClassVariable() + ")");
		}
		if (castType != Type.ANY && mainblock.isStrict()) {
			if (parenthesis) writer.addCode(")");
		}
	}

	@Override
	public void compileSetCopy(MainLeekBlock mainblock, JavaWriter writer, Expression expr, boolean parenthesis) {
		// assert(mLeftValue && !mTabular.nullable());

		if (expr.getType() != Type.ANY) {
			if (parenthesis) writer.addCode("(");
			writer.addCode("(" + expr.getType().getJavaName(mainblock.getVersion()) + ") ");
		}
		if (mTabular.getType() instanceof ArrayType at) {
			mTabular.writeJavaCode(mainblock, writer, true);
			// compileSetCopy n'est appelé qu'en v1 : put() legacy, pas de putv4 (#4465)
			writer.addCode(".put(");
			mCase.writeJavaCode(mainblock, writer, false);
			writer.addCode(", ");
			writeValueWithElementCoercion(mainblock, writer, expr, at.element());
			writer.addCode(")");
		} else if (mTabular.getType() instanceof MapType mt) {
			mTabular.writeJavaCode(mainblock, writer, true);
			// setv4 n'existe sur aucune classe runtime : MapLeekValue n'a que set() (#4465)
			writer.addCode(".set(");
			mCase.writeJavaCode(mainblock, writer, false);
			writer.addCode(", ");
			writeValueWithElementCoercion(mainblock, writer, expr, mt.element());
			writer.addCode(")");
		} else {
			writer.addCode("put(");
			mTabular.writeJavaCode(mainblock, writer, false);
			writer.addCode(", ");
			mCase.writeJavaCode(mainblock, writer, false);
			writer.addCode(", ");
			expr.writeJavaCode(mainblock, writer, false);
			writer.addCode(", " + mainblock.getWordCompiler().getCurrentClassVariable() + ")");
		}
		if (expr.getType() != Type.ANY) {
			if (parenthesis) writer.addCode(")");
		}
		// writer.compileClone(mainblock, expr);
	}

	private void writeValueWithElementCoercion(MainLeekBlock mainblock, JavaWriter writer, Expression expr, Type elementType) {
		if (elementType.isPrimitiveNumber()) {
			writer.compileConvert(mainblock, 0, expr, elementType, false);
		} else {
			expr.writeJavaCode(mainblock, writer, false);
		}
	}

	@Override
	public void compileIncrement(MainLeekBlock mainblock, JavaWriter writer, boolean parenthesis) {
		// assert(mLeftValue && !mTabular.nullable());

		writer.addCode("put_inc(");
		mTabular.writeJavaCode(mainblock, writer, false);
		writer.addCode(", ");
		mCase.writeJavaCode(mainblock, writer, false);
		writer.addCode(", " + mainblock.getWordCompiler().getCurrentClassVariable() + ")");
	}


	@Override
	public void compilePreIncrement(MainLeekBlock mainblock, JavaWriter writer, boolean parenthesis) {
		// assert(mLeftValue && !mTabular.nullable());

		writer.addCode("put_pre_inc(");
		mTabular.writeJavaCode(mainblock, writer, false);
		writer.addCode(", ");
		mCase.writeJavaCode(mainblock, writer, false);
		writer.addCode(", " + mainblock.getWordCompiler().getCurrentClassVariable() + ")");
	}

	@Override
	public void compileDecrement(MainLeekBlock mainblock, JavaWriter writer, boolean parenthesis) {
		// assert(mLeftValue && !mTabular.nullable());

		writer.addCode("put_dec(");
		mTabular.writeJavaCode(mainblock, writer, false);
		writer.addCode(", ");
		mCase.writeJavaCode(mainblock, writer, false);
		writer.addCode(", " + mainblock.getWordCompiler().getCurrentClassVariable() + ")");
	}

	@Override
	public void compilePreDecrement(MainLeekBlock mainblock, JavaWriter writer, boolean parenthesis) {
		// assert(mLeftValue && !mTabular.nullable());

		writer.addCode("put_pre_dec(");
		mTabular.writeJavaCode(mainblock, writer, false);
		writer.addCode(", ");
		mCase.writeJavaCode(mainblock, writer, false);
		writer.addCode(", " + mainblock.getWordCompiler().getCurrentClassVariable() + ")");
	}

	@Override
	public void compileAddEq(MainLeekBlock mainblock, JavaWriter writer, Expression expr, Type t, boolean parenthesis) {
		compilePutArithmeticOperation(mainblock, writer, expr, "put_add_eq", AI.ArithmeticOperation.ADD);
	}

	@Override
	public void compileSubEq(MainLeekBlock mainblock, JavaWriter writer, Expression expr, boolean parenthesis) {
		compilePutArithmeticOperation(mainblock, writer, expr, "put_sub_eq", AI.ArithmeticOperation.SUB);
	}

	@Override
	public void compileMulEq(MainLeekBlock mainblock, JavaWriter writer, Expression expr, Type type, boolean parenthesis) {
		compilePutArithmeticOperation(mainblock, writer, expr, "put_mul_eq", AI.ArithmeticOperation.MUL);
	}

	@Override
	public void compileModEq(MainLeekBlock mainblock, JavaWriter writer, Expression expr, boolean parenthesis) {
		compilePutArithmeticOperation(mainblock, writer, expr, "put_mod_eq", AI.ArithmeticOperation.MOD);
	}

	@Override
	public void compileDivEq(MainLeekBlock mainblock, JavaWriter writer, Expression expr, boolean parenthesis) {
		compilePutArithmeticOperation(mainblock, writer, expr, "put_div_eq", AI.ArithmeticOperation.DIV);
	}

	@Override
	public void compileIntDivEq(MainLeekBlock mainblock, JavaWriter writer, Expression expr, boolean parenthesis) {
		compilePutBitOperation(mainblock, writer, expr, "put_intdiv_eq", AI.BitOperation.INTDIV);
	}

	@Override
	public void compilePowEq(MainLeekBlock mainblock, JavaWriter writer, Expression expr, Type t, boolean parenthesis) {
		compilePutArithmeticOperation(mainblock, writer, expr, "put_pow_eq", AI.ArithmeticOperation.POW);
	}

	@Override
	public void compileBitOrEq(MainLeekBlock mainblock, JavaWriter writer, Expression expr, boolean parenthesis) {
		compilePutBitOperation(mainblock, writer, expr, "put_bor_eq", AI.BitOperation.BOR);
	}

	@Override
	public void compileBitAndEq(MainLeekBlock mainblock, JavaWriter writer, Expression expr, boolean parenthesis) {
		compilePutBitOperation(mainblock, writer, expr, "put_band_eq", AI.BitOperation.BAND);
	}

	@Override
	public void compileBitXorEq(MainLeekBlock mainblock, JavaWriter writer, Expression expr, boolean parenthesis) {
		compilePutBitOperation(mainblock, writer, expr, "put_bxor_eq", AI.BitOperation.BXOR);
	}

	@Override
	public void compileShiftLeftEq(MainLeekBlock mainblock, JavaWriter writer, Expression expr, boolean parenthesis) {
		compilePutBitOperation(mainblock, writer, expr, "put_shl_eq", AI.BitOperation.SHL);
	}

	@Override
	public void compileShiftRightEq(MainLeekBlock mainblock, JavaWriter writer, Expression expr, boolean parenthesis) {
		compilePutBitOperation(mainblock, writer, expr, "put_shr_eq", AI.BitOperation.SHR);
	}

	@Override
	public void compileShiftUnsignedRightEq(MainLeekBlock mainblock, JavaWriter writer, Expression expr, boolean parenthesis) {
		compilePutBitOperation(mainblock, writer, expr, "put_ushr_eq", AI.BitOperation.USHR);
	}

	/**
	 * `a[k] <op>= v` par le helper `helper` du runtime (put_bor_eq…). Sur une case que le
	 * contenant ne connaît pas (cf JavaWriter.containerBitConversion), par put_bit_eq qui
	 * convertit le résultat : booléenne ou réelle, ou big_integer d'une map (clé absente) face à
	 * un opérande integer. Pas une case big_integer de tableau : hors strict, toute case d'un
	 * `Array<big_integer>` est `big_integer?`, et la convertir changerait le Java de ces IA.
	 */
	private void compilePutBitOperation(MainLeekBlock mainblock, JavaWriter writer, Expression expr, String helper, AI.BitOperation operation) {
		var element = getType().assertNotNull();
		boolean converted = element == Type.BOOL || element == Type.REAL
			|| element == Type.BIG_INT && isMapCell() && expr.getType().assertNotNull() == Type.INT;
		compilePutOperation(mainblock, writer, expr, helper, "put_bit_eq", operation, converted ? element : null);
	}

	/**
	 * `a[k] <op>= v` (+=…) par le helper `helper` du runtime (put_add_eq…), ou par
	 * put_arithmetic_eq quand add() & co rendraient un autre type que celui de la case (cf
	 * JavaWriter.arithmeticConversion). Une case est prise non nullable, sauf la case
	 * big_integer d'une map (clé absente) : hors strict, toute case d'un `Array<real>` est
	 * `real?`, et `scores[k] += 10` sur une `Map<integer, real>` doit garder put_add_eq.
	 */
	private void compilePutArithmeticOperation(MainLeekBlock mainblock, JavaWriter writer, Expression expr, String helper, AI.ArithmeticOperation operation) {
		var element = getType().assertNotNull();
		var cell = element == Type.BIG_INT && isMapCell() ? getType() : element;
		compilePutOperation(mainblock, writer, expr, helper, "put_arithmetic_eq", operation, JavaWriter.arithmeticConversion(cell, expr.getType(), operation));
	}

	/** `helper(tableau, clé, valeur, classe)`, ou `converted(tableau, clé, valeur, opération, cible, classe)` vers `target`. */
	private void compilePutOperation(MainLeekBlock mainblock, JavaWriter writer, Expression expr, String helper, String converted, Enum<?> operation, Type target) {
		writer.addCode((target != null ? converted : helper) + "(");
		mTabular.writeJavaCode(mainblock, writer, false);
		writer.addCode(", ");
		mCase.writeJavaCode(mainblock, writer, false);
		writer.addCode(", ");
		expr.writeJavaCode(mainblock, writer, false);
		if (target != null) writer.addCode(JavaWriter.operationArguments(operation, target));
		writer.addCode(", " + mainblock.getWordCompiler().getCurrentClassVariable() + ")");
	}

	private boolean isMapCell() {
		var tabular = mTabular.getType();
		return tabular.isMap() || tabular.isMapOrNull();
	}

	@Override
	public void compileCoalesceEq(MainLeekBlock mainblock, JavaWriter writer, Expression expr, boolean parenthesis) {
		// a[index] ??= b
		var fromClass = mainblock.getWordCompiler().getCurrentClass();
		// `b` n'est évaluée que si l'élément est null (#5300), sauf si l'évaluer ne se voit pas
		boolean lazy = !ConstantFolder.isHarmlessValue(expr, fromClass);
		// Le test relit le tableau et l'index ; s'ils ne se relisent pas sans effet, leurs valeurs
		// sont gardées dans des locales Java, celles d'une expression switch
		boolean bind = lazy && !(ConstantFolder.isSilentRead(mTabular, fromClass) && ConstantFolder.isSilentRead(mCase, fromClass));
		Runnable tabular = () -> mTabular.writeJavaCode(mainblock, writer, false);
		Runnable key = () -> mCase.writeJavaCode(mainblock, writer, false);
		if (bind) {
			var id = mainblock.getCount();
			writer.addCode("(switch (0) { default -> { Object $t" + id + " = ");
			tabular.run();
			writer.addCode("; Object $k" + id + " = ");
			key.run();
			writer.addCode("; yield ");
			tabular = () -> writer.addCode("$t" + id);
			key = () -> writer.addCode("$k" + id);
		}
		writer.addCode("put_coalesce_eq(");
		tabular.run();
		writer.addCode(", ");
		key.run();
		writer.addCode(", ");
		if (lazy) {
			writer.addCode("put_coalesce_needed(");
			tabular.run();
			writer.addCode(", ");
			key.run();
			writer.addCode(") ? ");
		}
		expr.writeJavaCode(mainblock, writer, false);
		writer.addCode((lazy ? " : null" : "") + ", " + mainblock.getWordCompiler().getCurrentClassVariable() + ")");
		if (bind) writer.addCode("; } })");
	}

	public void setLeftValue(boolean b) {
		mLeftValue = b;
	}

	@Override
	public boolean isLeftValue() {
		// Un accès optionnel `a?[b]` n'est pas assignable (résultat nullable)
		return !optional;
	}

	/**
	 * Le Java de `t[k] = v` est-il un Object ? Hors strict seulement, où compileSet ne caste
	 * pas le résultat : put / putv4 (tableau typé ou non) rendent un Object, seul
	 * MapLeekValue.set, générique, rend le type de la valeur écrite.
	 */
	public boolean setReturnsObject() {
		return !strict && !(mTabular.getType() instanceof MapType);
	}

	@Override
	public boolean nullable() {
		return true;
	}

	@Override
	public Location getLocation() {
		return new Location(mTabular.getLocation(), closingBracket.getLocation());
	}
}
