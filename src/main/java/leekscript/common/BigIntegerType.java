package leekscript.common;

import leekscript.compiler.JavaWriter;

/**
 * Type des entiers de taille arbitraire (`big_integer`), adossé à
 * {@link leekscript.runner.values.BigIntegerValue}.
 *
 * Conçu initialement par Batary (bat_jarry@hotmail.fr).
 */
public class BigIntegerType extends Type {

	public BigIntegerType() {
		super("big_integer", "l", "BigIntegerValue", "BigIntegerValue", "new BigIntegerValue()");
	}

	@Override
	public String getDefaultValue(JavaWriter writer, int version) {
		return "new BigIntegerValue(" + writer.getAIThis() + ", 0)";
	}
}
