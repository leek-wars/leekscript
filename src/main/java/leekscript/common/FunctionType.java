package leekscript.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FunctionType extends Type {

	private Type return_type;
	private List<Type> arguments;
	private int minArguments = 0;
	private int maxArguments = 0;

	public FunctionType(Type return_type, List<Type> arguments) {
		super("", "f", "FunctionLeekValue", "FunctionLeekValue", "null");
		this.return_type = return_type;
		this.arguments = arguments;
	}

	public FunctionType(Type return_type, Type... arguments) {
		super("", "f", "FunctionLeekValue", "FunctionLeekValue", "null");
		this.return_type = return_type;
		this.arguments = new ArrayList<Type>(Arrays.asList(arguments));
		this.minArguments = arguments.length;
		this.maxArguments = arguments.length;
		this.updateName();
	}

	public FunctionType(Type return_type, int min_arguments, Type... arguments) {
		super("", "f", "FunctionLeekValue", "FunctionLeekValue", "null");
		this.return_type = return_type;
		this.arguments = new ArrayList<Type>(Arrays.asList(arguments));
		this.minArguments = min_arguments;
		this.maxArguments = arguments.length;
		this.updateName();
	}

	public void add_argument(Type argument, boolean optional) {
		assert argument != null;
		arguments.add(argument);
		if (!optional) this.minArguments++;
		this.maxArguments++;
		this.updateName();
	}

	private void updateName() {
		this.name = "(";
		for (int a = 0; a < arguments.size(); ++a) {
			if (a > 0) this.name += ", ";
			if (a >= this.minArguments) this.name += "[";
			this.name += arguments.get(a);
			if (a >= this.minArguments) this.name += "]";
		}
		this.name += ") => " + return_type;
	}

	@Override
	public List<Type> getArguments() {
		return arguments;
	}

	public Type getReturnType() {
		return return_type;
	}

	@Override
	public boolean canBeCallable() {
		return true;
	}

	@Override
	public boolean isCallable() {
		return true;
	}

	@Override
	public CastType accepts(Type type) {
		if (type instanceof FunctionType ft) {

			if (ft.getArguments().size() < this.minArguments) return CastType.INCOMPATIBLE;

			var worst = return_type.accepts(ft.getReturnType());
			int n = Math.min(ft.getArguments().size(), this.arguments.size());
			for (int a = 0; a < n; ++a) {
				var cast = this.arguments.get(a).accepts(ft.getArgument(a));
				if (cast.ordinal() > worst.ordinal()) worst = cast;
			}

			return worst;
		}

		if (type instanceof ClassValueType ct) {

			if (ct.getClassDeclaration() == null) return CastType.UNSAFE_DOWNCAST;

			var constructorMin = Integer.MAX_VALUE;
			var constructorMax = -Integer.MAX_VALUE;
			for (var c : ct.getClassDeclaration().getConstructors().keySet()) {
				if (c < constructorMin) constructorMin = c;
				if (c > constructorMax) constructorMax = c;
			}

			if (constructorMax >= this.minArguments && constructorMin <= this.maxArguments) {
				return CastType.EQUALS;
			}
			return CastType.INCOMPATIBLE;
		}
		return super.accepts(type);
	}

	@Override
	public Type getArgument(int a) {
		return a < arguments.size() ? arguments.get(a) : Type.VOID;
	}

	@Override
	public Type getArgument(int argumentCount, int a) {
		return a < arguments.size() ? arguments.get(a) : Type.VOID;
	}

	@Override
	public int getMinArguments() {
		return minArguments;
	}

	@Override
	public int getMaxArguments() {
		return maxArguments;
	}

	@Override
	public CastType acceptsArguments(List<Type> types) {
		if (types.size() < this.minArguments || types.size() > this.maxArguments) return CastType.INCOMPATIBLE;
		var worst = CastType.EQUALS;
		for (int a = 0; a < types.size(); ++a) {
			var cast = arguments.get(a).accepts(types.get(a));
			if (cast.ordinal() > worst.ordinal()) worst = cast;
		}
		return worst;
	}

	@Override
	public Type returnType() {
		return this.return_type;
	}

	public void setReturnType(Type type) {
		this.return_type = type;
		this.updateName();
	}

	@Override
	public String getCode() {
		String r = "Function<";
		for (int a = 0; a < arguments.size(); ++a) {
			if (a > 0) r += ", ";
			// if (a >= this.minArguments) r += "[";
			r += arguments.get(a).getCode();
			// if (a >= this.minArguments) r += "]";
		}
		r += " => " + return_type.getCode() + ">";
		return r;
	}

	@Override
	public String getJavaPrimitiveName(int version) {
		return "FunctionLeekValue<" + return_type.getJavaName(version) + ">";
	}

	@Override
	public String getJavaName(int version) {
		return "FunctionLeekValue<" + return_type.getJavaName(version) + ">";
	}

	@Override
	public Type substitute(Map<String, Type> substitution) {
		var newArgs = new ArrayList<Type>();
		for (var arg : arguments) {
			newArgs.add(arg.substitute(substitution));
		}
		return Type.function(return_type.substitute(substitution), newArgs.toArray(new Type[0]));
	}

	/**
	 * Infer a substitution map for TemplateType parameters based on actual argument types.
	 * This enables simple generic function behavior like (T) => T or (T, U) => T | U.
	 */
	public Map<String, Type> inferSubstitution(List<Type> actualArguments) {
		var substitution = new HashMap<String, Type>();
		int n = Math.min(arguments.size(), actualArguments.size());
		for (int i = 0; i < n; i++) {
			unify(arguments.get(i), actualArguments.get(i), substitution);
		}
		return substitution;
	}

	public Type inferReturnType(List<Type> actualArguments) {
		return return_type.substitute(inferSubstitution(actualArguments));
	}

	private static void unify(Type formal, Type actual, Map<String, Type> substitution) {
		if (formal instanceof TemplateType tt) {
			var existing = substitution.get(tt.name);
			if (existing == null) substitution.put(tt.name, actual);
			else substitution.put(tt.name, Type.compound(existing, actual));
			return;
		}
		if (formal instanceof ArrayType fa && actual instanceof ArrayType aa) {
			unify(fa.element(), aa.element(), substitution);
			return;
		}
		if (formal instanceof SetType fs && actual instanceof SetType as) {
			unify(fs.element(), as.element(), substitution);
			return;
		}
		if (formal instanceof IntervalType fi && actual instanceof IntervalType ai) {
			unify(fi.element(), ai.element(), substitution);
			return;
		}
		if (formal instanceof MapType fm && actual instanceof MapType am) {
			unify(fm.key(), am.key(), substitution);
			unify(fm.element(), am.element(), substitution);
			return;
		}
		if (formal instanceof CompoundType fc) {
			// Best-effort: attempt to unify each branch with the actual type.
			for (var t : fc.getTypes()) unify(t, actual, substitution);
			return;
		}
		if (formal instanceof FunctionType ff && actual instanceof FunctionType af) {
			int nArgs = Math.min(ff.getArguments().size(), af.getArguments().size());
			for (int i = 0; i < nArgs; i++) {
				unify(ff.getArgument(i), af.getArgument(i), substitution);
			}
			unify(ff.getReturnType(), af.getReturnType(), substitution);
			return;
		}
		// GenericClassType inference is intentionally not implemented yet.
	}
}
