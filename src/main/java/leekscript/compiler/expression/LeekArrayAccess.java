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
import leekscript.common.ArrayType;
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
	private Type type;

	public LeekArrayAccess(Token openingBracket) {
		openingBracket.setExpression(this);
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
	}

	@Override
	public void writeJavaCode(MainLeekBlock mainblock, JavaWriter writer, boolean parenthesis) {
		if (colon != null) {
			if (mCase != null && endIndex != null) {
				writer.addCode("range(");
			} else if (mCase != null) {
				writer.addCode("range_start(");
			} else if (endIndex != null) {
				writer.addCode("range_end(");
			} else {
				writer.addCode("range_all(");
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
		} else if (mTabular instanceof LeekVariable v && v.getVariableType() == VariableType.THIS && mCase instanceof LeekString) {
			writer.addCode(((LeekString) mCase).getText());
		} else if (mTabular.getType() instanceof LegacyArrayType) {
			mTabular.writeJavaCode(mainblock, writer, true);
			writer.addCode(".get(");
			mCase.writeJavaCode(mainblock, writer, false);
			writer.addCode(")");
		} else if (mTabular.getType() instanceof ArrayType) {
			if (type != Type.ANY) {
				if (parenthesis) writer.addCode("(");
				if (type.isPrimitive()) {
					writer.addCode("(" + type.getJavaPrimitiveName(mainblock.getVersion()) + ") ");
				}
				writer.addCode("(" + type.getJavaName(mainblock.getVersion()) + ") ");
			}
			mTabular.writeJavaCode(mainblock, writer, true);
			writer.addCode(".get(");
			mCase.writeJavaCode(mainblock, writer, false);
			writer.addCode(")");
			if (type != Type.ANY) {
				if (parenthesis) writer.addCode(")");
			}
		} else if (mTabular.getType() instanceof MapType) {
			if (type != Type.ANY) {
				if (parenthesis) writer.addCode("(");
				if (type.isPrimitive()) {
					writer.addCode("(" + type.getJavaPrimitiveName(mainblock.getVersion()) + ") ");
				}
				writer.addCode("(" + type.getJavaName(mainblock.getVersion()) + ") ");
			}
			mTabular.writeJavaCode(mainblock, writer, true);
			writer.addCode(".get(");
			mCase.writeJavaCode(mainblock, writer, false);
			writer.addCode(")");
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

		if (expr.getType() != Type.ANY && mainblock.isStrict()) {
			if (parenthesis) writer.addCode("(");
			if (expr.getType().isPrimitive()) {
				writer.addCode("(" + expr.getType().getJavaPrimitiveName(mainblock.getVersion()) + ") ");
			}
			writer.addCode("(" + expr.getType().getJavaName(mainblock.getVersion()) + ") ");
		}
		if (mTabular.getType() instanceof ArrayType) {
			mTabular.writeJavaCode(mainblock, writer, true);
			if (mainblock.isStrict()) {
				writer.addCode(".put(");
			} else {
				writer.addCode(".putv4(");
			}
			mCase.writeJavaCode(mainblock, writer, false);
			writer.addCode(", ");
			expr.writeJavaCode(mainblock, writer, false);
			writer.addCode(")");
		} else if (mTabular.getType() instanceof MapType) {
			mTabular.writeJavaCode(mainblock, writer, true);
			writer.addCode(".set(");
			mCase.writeJavaCode(mainblock, writer, false);
			writer.addCode(", ");
			expr.writeJavaCode(mainblock, writer, false);
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
		if (expr.getType() != Type.ANY && mainblock.isStrict()) {
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
		if (mTabular.getType() instanceof ArrayType) {
			mTabular.writeJavaCode(mainblock, writer, true);
			if (mainblock.isStrict()) {
				writer.addCode(".put(");
			} else {
				writer.addCode(".putv4(");
			}
			mCase.writeJavaCode(mainblock, writer, false);
			writer.addCode(", ");
			expr.writeJavaCode(mainblock, writer, false);
			writer.addCode(")");
		} else if (mTabular.getType() instanceof MapType) {
			mTabular.writeJavaCode(mainblock, writer, true);
			if (mainblock.isStrict()) {
				writer.addCode(".set(");
			} else {
				writer.addCode(".setv4(");
			}
			mCase.writeJavaCode(mainblock, writer, false);
			writer.addCode(", ");
			expr.writeJavaCode(mainblock, writer, false);
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
		// assert(mLeftValue && !mTabular.nullable());

		writer.addCode("put_add_eq(");
		mTabular.writeJavaCode(mainblock, writer, false);
		writer.addCode(", ");
		mCase.writeJavaCode(mainblock, writer, false);
		writer.addCode(", ");
		expr.writeJavaCode(mainblock, writer, false);
		writer.addCode(", " + mainblock.getWordCompiler().getCurrentClassVariable() + ")");
	}

	@Override
	public void compileSubEq(MainLeekBlock mainblock, JavaWriter writer, Expression expr, boolean parenthesis) {
		// assert(mLeftValue && !mTabular.nullable());

		writer.addCode("put_sub_eq(");
		mTabular.writeJavaCode(mainblock, writer, false);
		writer.addCode(", ");
		mCase.writeJavaCode(mainblock, writer, false);
		writer.addCode(", ");
		expr.writeJavaCode(mainblock, writer, false);
		writer.addCode(", " + mainblock.getWordCompiler().getCurrentClassVariable() + ")");
	}

	@Override
	public void compileMulEq(MainLeekBlock mainblock, JavaWriter writer, Expression expr, Type type, boolean parenthesis) {
		// assert(mLeftValue && !mTabular.nullable());

		writer.addCode("put_mul_eq(");
		mTabular.writeJavaCode(mainblock, writer, false);
		writer.addCode(", ");
		mCase.writeJavaCode(mainblock, writer, false);
		writer.addCode(", ");
		expr.writeJavaCode(mainblock, writer, false);
		writer.addCode(", " + mainblock.getWordCompiler().getCurrentClassVariable() + ")");
	}

	@Override
	public void compileModEq(MainLeekBlock mainblock, JavaWriter writer, Expression expr, boolean parenthesis) {
		// assert(mLeftValue && !mTabular.nullable());

		writer.addCode("put_mod_eq(");
		mTabular.writeJavaCode(mainblock, writer, false);
		writer.addCode(", ");
		mCase.writeJavaCode(mainblock, writer, false);
		writer.addCode(", ");
		expr.writeJavaCode(mainblock, writer, false);
		writer.addCode(", " + mainblock.getWordCompiler().getCurrentClassVariable() + ")");
	}

	@Override
	public void compileDivEq(MainLeekBlock mainblock, JavaWriter writer, Expression expr, boolean parenthesis) {
		// assert(mLeftValue && !mTabular.nullable());

		writer.addCode("put_div_eq(");
		mTabular.writeJavaCode(mainblock, writer, false);
		writer.addCode(", ");
		mCase.writeJavaCode(mainblock, writer, false);
		writer.addCode(", ");
		expr.writeJavaCode(mainblock, writer, false);
		writer.addCode(", " + mainblock.getWordCompiler().getCurrentClassVariable() + ")");
	}

	@Override
	public void compileIntDivEq(MainLeekBlock mainblock, JavaWriter writer, Expression expr, boolean parenthesis) {
		// assert(mLeftValue && !mTabular.nullable());

		writer.addCode("put_intdiv_eq(");
		mTabular.writeJavaCode(mainblock, writer, false);
		writer.addCode(", ");
		mCase.writeJavaCode(mainblock, writer, false);
		writer.addCode(", ");
		expr.writeJavaCode(mainblock, writer, false);
		writer.addCode(", " + mainblock.getWordCompiler().getCurrentClassVariable() + ")");
	}

	@Override
	public void compilePowEq(MainLeekBlock mainblock, JavaWriter writer, Expression expr, Type t, boolean parenthesis) {
		// assert(mLeftValue && !mTabular.nullable());

		writer.addCode("put_pow_eq(");
		mTabular.writeJavaCode(mainblock, writer, false);
		writer.addCode(", ");
		mCase.writeJavaCode(mainblock, writer, false);
		writer.addCode(", ");
		expr.writeJavaCode(mainblock, writer, false);
		writer.addCode(", " + mainblock.getWordCompiler().getCurrentClassVariable() + ")");
	}

	@Override
	public void compileBitOrEq(MainLeekBlock mainblock, JavaWriter writer, Expression expr, boolean parenthesis) {
		// assert(mLeftValue && !mTabular.nullable());

		writer.addCode("put_bor_eq(");
		mTabular.writeJavaCode(mainblock, writer, false);
		writer.addCode(", ");
		mCase.writeJavaCode(mainblock, writer, false);
		writer.addCode(", ");
		expr.writeJavaCode(mainblock, writer, false);
		writer.addCode(", " + mainblock.getWordCompiler().getCurrentClassVariable() + ")");
	}

	@Override
	public void compileBitAndEq(MainLeekBlock mainblock, JavaWriter writer, Expression expr, boolean parenthesis) {
		// assert(mLeftValue && !mTabular.nullable());

		writer.addCode("put_band_eq(");
		mTabular.writeJavaCode(mainblock, writer, false);
		writer.addCode(", ");
		mCase.writeJavaCode(mainblock, writer, false);
		writer.addCode(", ");
		expr.writeJavaCode(mainblock, writer, false);
		writer.addCode(", " + mainblock.getWordCompiler().getCurrentClassVariable() + ")");
	}

	@Override
	public void compileBitXorEq(MainLeekBlock mainblock, JavaWriter writer, Expression expr, boolean parenthesis) {
		// assert(mLeftValue && !mTabular.nullable());

		writer.addCode("put_bxor_eq(");
		mTabular.writeJavaCode(mainblock, writer, false);
		writer.addCode(", ");
		mCase.writeJavaCode(mainblock, writer, false);
		writer.addCode(", ");
		expr.writeJavaCode(mainblock, writer, false);
		writer.addCode(", " + mainblock.getWordCompiler().getCurrentClassVariable() + ")");
	}

	@Override
	public void compileShiftLeftEq(MainLeekBlock mainblock, JavaWriter writer, Expression expr, boolean parenthesis) {
		// assert(mLeftValue && !mTabular.nullable());

		writer.addCode("put_shl_eq(");
		mTabular.writeJavaCode(mainblock, writer, false);
		writer.addCode(", ");
		mCase.writeJavaCode(mainblock, writer, false);
		writer.addCode(", ");
		expr.writeJavaCode(mainblock, writer, false);
		writer.addCode(", " + mainblock.getWordCompiler().getCurrentClassVariable() + ")");
	}

	@Override
	public void compileShiftRightEq(MainLeekBlock mainblock, JavaWriter writer, Expression expr, boolean parenthesis) {
		// assert(mLeftValue && !mTabular.nullable());

		writer.addCode("put_shr_eq(");
		mTabular.writeJavaCode(mainblock, writer, false);
		writer.addCode(", ");
		mCase.writeJavaCode(mainblock, writer, false);
		writer.addCode(", ");
		expr.writeJavaCode(mainblock, writer, false);
		writer.addCode(", " + mainblock.getWordCompiler().getCurrentClassVariable() + ")");
	}

	@Override
	public void compileShiftUnsignedRightEq(MainLeekBlock mainblock, JavaWriter writer, Expression expr, boolean parenthesis) {
		// assert(mLeftValue && !mTabular.nullable());

		writer.addCode("put_ushr_eq(");
		mTabular.writeJavaCode(mainblock, writer, false);
		writer.addCode(", ");
		mCase.writeJavaCode(mainblock, writer, false);
		writer.addCode(", ");
		expr.writeJavaCode(mainblock, writer, false);
		writer.addCode(", " + mainblock.getWordCompiler().getCurrentClassVariable() + ")");
	}

	public void setLeftValue(boolean b) {
		mLeftValue = b;
	}

	@Override
	public boolean isLeftValue() {
		return true;
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
