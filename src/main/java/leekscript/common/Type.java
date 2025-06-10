package leekscript.common;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import leekscript.runner.values.BigIntegerValue;
import java.util.ArrayList;
import java.util.stream.Collectors;

import leekscript.compiler.Complete;
import leekscript.compiler.JavaWriter;
import leekscript.compiler.instruction.ClassDeclarationInstruction;

public class Type {

	// public static final Map<HashSet<Type>, Type> compoundTypes = new HashMap<>();
	// public static final Map<Type, Type> arrayTypes = new HashMap<>();
	// public static final Map<Map.Entry<Type, Type>, Type> mapTypes = new HashMap<>();
	// public static final Map<Map.Entry<FunctionType, Type>, FunctionType> addArgumentTypes = new HashMap<>();
	// public static final Map<Map.Entry<Type, Type[]>, Type> functionTypes = new HashMap<>();

	public static final Type ERROR = new ErrorType();
	public static final Type WARNING = new WarningType();
	public static final Type VOID = new Type("void", "v", "Object", "Object", "null");
	public static final Type ANY = new Type("any", "x", "Object", "Object", "null");
	public static final Type NULL = new Type("null", "u", "Object", "Object", "null");
	public static final Type BOOL = new Type("boolean", "b", "boolean", "Boolean", "false");
	public static final Type INT = new Type("integer", "i", "long", "Long", "0l");
	public static final Type REAL = new Type("real", "r", "double", "Double", "0.0");
	public static final Type STRING = new Type("string", "s", "String", "String", "\"\"");
	public static final Type BIG_INT = new BigIntegerType();
	public static final Type OBJECT = new ObjectType();
	public static final FunctionType FUNCTION = new FunctionType(Type.ANY);
	public static final Type MAP = map(Type.ANY, Type.ANY);
	public static final Type EMPTY_MAP = map(Type.VOID, Type.VOID);
	public static final Type CLASS = new ClassValueType(null);
	// public static final Type LEGACY_ARRAY = new LegacyArrayType();
	public static final Type ARRAY = array(Type.ANY);
	public static final Type EMPTY_ARRAY = array(Type.VOID);
	public static final Type SET = set(Type.ANY);
	public static final Type SET_REAL = set(Type.REAL);
	public static final Type SET_INT = set(Type.INT);
	public static final Type EMPTY_SET = set(Type.VOID);
	public static final Type INTERVAL = new IntervalType(Type.ANY);
	public static final Type EMPTY_INTERVAL = new IntervalType(Type.VOID);
	public static final Type REAL_INTERVAL = new IntervalType(Type.REAL);
	public static final Type INTEGER_INTERVAL = new IntervalType(Type.INT);
	public static final Type ARRAY_INT = array(Type.INT);
	public static final Type ARRAY_REAL = array(Type.REAL);
	public static final Type ARRAY_STRING = array(Type.STRING);
	public static final Type INT_OR_NULL = compound(Type.INT, Type.NULL);
	public static final Type BOOL_OR_NULL = compound(Type.BOOL, Type.NULL);
	public static final Type INT_OR_BOOL = compound(Type.INT, Type.BOOL);
	public static final Type ARRAY_OR_NULL = compound(Type.ARRAY, Type.NULL);
	public static final Type ARRAY_INT_OR_NULL = compound(Type.ARRAY_INT, Type.NULL);
	public static final Type STRING_OR_NULL = compound(Type.STRING, Type.NULL);
	public static final Type INT_OR_REAL = compound(Type.INT, Type.REAL);
	public static final Type MAP_INT_STRING = map(Type.INT, Type.STRING);
	public static final Type MAP_STRING_STRING = map(Type.STRING, Type.STRING);

	public static enum CastType {
		EQUALS,
		UPCAST,
		SAFE_DOWNCAST,
		UNSAFE_DOWNCAST,
		INCOMPATIBLE
	}

	public String name;
	public String signature;
	private String javaPrimitiveName;
	private String javaName;
	private String defaultValue;

	public Type(String name, String signature, String javaPrimitiveName, String javaName, String defaultValue) {
		this.name = name;
		this.signature = signature;
		this.javaPrimitiveName = javaPrimitiveName;
		this.javaName = javaName;
		this.defaultValue = defaultValue;
	}

	public CastType accepts(Type type) {
		if (type == this) return CastType.EQUALS;
		if (this == ANY) return CastType.UPCAST;
		if (type == ANY) return CastType.UNSAFE_DOWNCAST;

		if (type instanceof CompoundType) {
			var best = CastType.INCOMPATIBLE;
			var worst = CastType.EQUALS;
			// boolean ok = false;
			// boolean nok = false;
			for (var t : ((CompoundType) type).getTypes()) {
				var r = this.accepts(t);
				if (r.ordinal() > worst.ordinal()) worst = r;
				if (r.ordinal() < best.ordinal()) best = r;
				// if (r != CastType.INCOMPATIBLE) ok = true;
				// if (r != CastType.EQUALS) nok = true;
			}
			// System.out.println("best = " + best + " worst = " + worst);
			// if (best == CastType.INCOMPATIBLE && ok) return CastType.UNSAFE_DOWNCAST;
			// if (best == CastType.EQUALS && nok) return CastType.UNSAFE_DOWNCAST;
			// Si un est compatible, le tout est compatible
			if (worst == CastType.INCOMPATIBLE && best != CastType.INCOMPATIBLE) return CastType.UNSAFE_DOWNCAST;
			// Sinon on prend le pire
			return worst;
		}

		if (this == REAL) {
			if (type == INT || type == BIG_INT) {
				return CastType.SAFE_DOWNCAST;
			}
		}
		if (this == INT) {
			if (type == REAL || type == BIG_INT) {
				return CastType.SAFE_DOWNCAST;
			}
		}
		if (this == BIG_INT) {
			if (type == REAL || type == INT) {
				return CastType.SAFE_DOWNCAST;
			}
		}
		if (this == FUNCTION) {
			if (type instanceof ClassValueType) {
				return CastType.UPCAST;
			}
		}
		return CastType.INCOMPATIBLE;
	}

	public boolean isPrimitiveNumber() {
		return this == INT || this == REAL;
	}

	public boolean isNumber() {
		return this == INT || this == REAL || this == BIG_INT;
	}

	public String toString() {
		return getCode();
	}

	public String getSignature() {
		return signature;
	}

	public String getJavaPrimitiveName(int version) {
		return javaPrimitiveName;
	}

	public String getJavaName(int version) {
		return javaName;
	}

	public String getDefaultValue(JavaWriter writer, int version) {
		return defaultValue;
	}

	public Type union(Type type) {
		if (this == Type.VOID) return type;
		if (type == Type.VOID) return this;
		return Type.ANY;
	}

	public static Type union(List<Type> types) {
		var r = types.get(0);
		for (var t = 1; t < types.size(); ++t) {
			r = r.union(types.get(t));
		}
		return r;
	}

	public Object toJSON() {
		return this.toString();
	}

	public boolean isArray() {
		return false;
	}

	public boolean isArrayOrNull() {
		return false;
	}

	public boolean isMap() {
		return false;
	}

	public static Type compound(HashSet<Type> types) {
		if (types.size() == 0) return Type.VOID;
		var all = new HashSet<Type>();
		for (var t : types) {
			if (t instanceof CompoundType) {
				all.addAll(((CompoundType) t).getTypes());
			} else if (t != Type.VOID) {
				all.add(t);
			}
		}
		if (all.size() == 1) return all.iterator().next();
		// var cached = compoundTypes.get(all);
		// if (cached != null) return cached;
		var type = new CompoundType(all);
		// compoundTypes.put(all, type);
		return type;
	}

	public static Type compound(Type type1, Type type2) {
		if (type1 == Type.VOID) return type2;
		if (type2 == Type.VOID) return type1;
		if (type1 == type2) return type1;
		var all = new HashSet<Type>();
		all.add(type1);
		all.add(type2);
		return compound(all);
	}

	public static Type compound(Type... types) {
		return compound(new HashSet<Type>(Arrays.asList(types)));
	}

	public static Type versions(HashSet<Type> types) {
		var all = new HashSet<Type>();
		for (var t : types) {
			if (t instanceof VersionsType) {
				all.addAll(((VersionsType) t).getTypes());
			} else if (t != Type.VOID) {
				all.add(t);
			}
		}
		if (all.size() == 1) return all.iterator().next();
		return new VersionsType(all);
	}

	public static Type versions(Type... types) {
		return versions(new HashSet<Type>(Arrays.asList(types)));
	}

	public static Type array(Type type) {
		// var cached = arrayTypes.get(type);
		// if (cached != null) return cached;
		var array = new ArrayType(type);
		// arrayTypes.put(type, array);
		return array;
	}

	public static Type map(Type key, Type value) {
		// var entry = new AbstractMap.SimpleEntry<Type, Type>(key, value);
		// var cached = mapTypes.get(entry);
		// if (cached != null) return cached;
		var map = new MapType(key, value);
		// mapTypes.put(entry, map);
		return map;
	}

	public static Type set(Type type) {
		return new SetType(type);
	}

	public Type key() {
		if (this == ANY) {
			return Type.ANY;
		}
		return Type.NULL;
	}

	public Type element() {
		if (this == ANY) {
			return Type.ANY;
		}
		return Type.NULL;
	}

	public Type elementAccess(int version, boolean strict) {
		if (this == ANY) {
			return Type.ANY;
		}
		return Type.NULL;
	}

	public Type elementAccess(int version, boolean strict, String key) {
		if (this == ANY) {
			return Type.ANY;
		}
		return Type.NULL;
	}

	public boolean isIterable() {
		return false;
	}

	public boolean canBeIterable() {
		if (this == ANY) {
			return true;
		}
		return false;
	}

	public boolean isIndexable() {
		return false;
	}

	public boolean canBeIndexable() {
		if (this == ANY) {
			return true;
		}
		return false;
	}

	public boolean canBeCallable() {
		if (this == ANY) {
			return true;
		}
		return false;
	}

	public boolean isCallable() {
		return false;
	}

	public Type member(String member) {
		if (this == OBJECT) {
			return Type.ANY;
		}
		if (this == ANY) {
			return Type.WARNING;
		}
		return Type.ERROR;
	}

	public ClassDeclarationInstruction getClassDeclaration() {
		return null;
	}

	public static FunctionType add_argument(FunctionType current, Type argument) {
		// var entry = new AbstractMap.SimpleEntry<FunctionType, Type>(current, argument);
		// var cached = addArgumentTypes.get(entry);
		// if (cached != null) return cached;
		var newArguments = new ArrayList<Type>(current.getArguments());
		newArguments.add(argument);
		var type = new FunctionType(current.getReturnType(), newArguments);
		// addArgumentTypes.put(entry, type);
		return type;
	}

	public static Type function(Type return_type, Type[] arguments) {
		// var entry = new AbstractMap.SimpleEntry<Type, Type[]>(return_type, arguments);
		// var cached = functionTypes.get(entry);
		// if (cached != null) return cached;
		var type = new FunctionType(return_type, arguments);
		// functionTypes.put(entry, type);
		return type;
	}

	public int getMinArguments() {
		return Integer.MIN_VALUE;
	}

	public int getMaxArguments() {
		return Integer.MAX_VALUE;
	}

	public CastType acceptsArguments(List<Type> types) {
		if (this == Type.ANY) {
			return CastType.UPCAST;
		}
		return CastType.INCOMPATIBLE;
	}

	public Type getArgument(int a) {
		if (this == ANY) {
			return Type.ANY;
		}
		return Type.NULL;
	}

	public Type getArgument(int argumentCount, int a) {
		if (this == ANY) {
			return Type.ANY;
		}
		return Type.NULL;
	}

	public Type returnType() {
		if (this == ANY) {
			return Type.ANY;
		}
		return Type.NULL;
	}

	public Type assertNotNull() {
		return this;
	}

	public Type add(Type type) {

		if (this instanceof CompoundType ct) {
			return Type.compound(ct.getTypes().stream().map(t -> t.add(type)).collect(Collectors.toCollection(HashSet::new)));
		}
		if (type instanceof CompoundType ct) {
			return Type.compound(ct.getTypes().stream().map(t -> this.add(t)).collect(Collectors.toCollection(HashSet::new)));
		}

		if (this == Type.BIG_INT && (type.isNumber() || type == Type.BOOL || type == Type.NULL)
				|| (this.isNumber() || this == Type.BOOL || this == Type.NULL) && type == Type.BIG_INT) return Type.BIG_INT;
		if ((this == Type.INT || this == Type.BOOL || this == Type.NULL) && (type == Type.INT || type == Type.BOOL || type == Type.NULL)) return Type.INT;
		if ((this.isNumber() || this == Type.BOOL || this == Type.NULL) && (type.isNumber() || type == Type.BOOL || type == Type.NULL)) return Type.REAL;

		if (this == Type.STRING || type == Type.STRING) return Type.STRING;

		if (this instanceof ArrayType a1 && type instanceof ArrayType a2) {
			return Type.array(Type.compound(a1.element(), a2.element()));
		}
		if (this instanceof ArrayType a1) {
			return Type.array(Type.compound(a1.element(), type));
		}

		if (this instanceof MapType m1 && type instanceof MapType m2) {
			return Type.map(Type.compound(m1.key(), m2.key()), Type.compound(m1.element(), m2.element()));
		}

		return Type.ANY;
	}

	public Type sub(Type type) {

		if (this instanceof CompoundType ct) {
			return Type.compound(ct.getTypes().stream().map(t -> t.sub(type)).collect(Collectors.toCollection(HashSet::new)));
		}
		if (type instanceof CompoundType ct) {
			return Type.compound(ct.getTypes().stream().map(t -> this.sub(t)).collect(Collectors.toCollection(HashSet::new)));
		}

		if (this == Type.BIG_INT && (type.isNumber() || type == Type.BOOL || type == Type.NULL)
				|| (this.isNumber() || this == Type.BOOL || this == Type.NULL) && type == Type.BIG_INT) return Type.BIG_INT;
		if ((this == Type.INT || this == Type.BOOL || this == Type.NULL) && (type == Type.INT || type == Type.BOOL || type == Type.NULL)) return Type.INT;
		if ((this.isNumber() || this == Type.BOOL || this == Type.NULL) && (type.isNumber() || type == Type.BOOL || type == Type.NULL)) return Type.REAL;

		return Type.ANY;
	}

	public Type mul(Type type) {

		if (this instanceof CompoundType ct) {
			return Type.compound(ct.getTypes().stream().map(t -> t.mul(type)).collect(Collectors.toCollection(HashSet::new)));
		}
		if (type instanceof CompoundType ct) {
			return Type.compound(ct.getTypes().stream().map(t -> this.mul(t)).collect(Collectors.toCollection(HashSet::new)));
		}

		if (this == Type.BIG_INT && (type.isNumber() || type == Type.BOOL || type == Type.NULL)
				|| (this.isNumber() || this == Type.BOOL || this == Type.NULL) && type == Type.BIG_INT) return Type.BIG_INT;
		if ((this == Type.INT || this == Type.BOOL || this == Type.NULL) && (type == Type.INT || type == Type.BOOL || type == Type.NULL)) return Type.INT;
		if ((this.isNumber() || this == Type.BOOL || this == Type.NULL) && (type.isNumber() || type == Type.BOOL || type == Type.NULL)) return Type.REAL;

		return Type.ANY;
	}

	public Type div(Type type) {

		if (this instanceof CompoundType ct) {
			return Type.compound(ct.getTypes().stream().map(t -> t.div(type)).collect(Collectors.toCollection(HashSet::new)));
		}
		if (type instanceof CompoundType ct) {
			return Type.compound(ct.getTypes().stream().map(t -> this.div(t)).collect(Collectors.toCollection(HashSet::new)));
		}
		if (this == Type.BIG_INT && type == Type.NULL) return Type.REAL;
		if (this == Type.BIG_INT && (type.isNumber() || type == Type.BOOL) 
				|| type == Type.BIG_INT && (this.isNumber() || this == Type.BOOL)) return Type.ANY;
		if ((this.isNumber() || this == Type.BOOL) && (type.isNumber() || type == Type.BOOL)) return Type.REAL;

		return Type.ANY;
	}
	
	public Type mod(Type type) {

		if (this instanceof CompoundType ct) {
			return Type.compound(ct.getTypes().stream().map(t -> t.mod(type)).collect(Collectors.toCollection(HashSet::new)));
		}
		if (type instanceof CompoundType ct) {
			return Type.compound(ct.getTypes().stream().map(t -> this.mod(t)).collect(Collectors.toCollection(HashSet::new)));
		}
		if (this == Type.BIG_INT && type == Type.NULL) return Type.REAL;
		if (this == Type.BIG_INT && (type.isNumber() || type == Type.BOOL) 
				|| type == Type.BIG_INT && (this.isNumber() || this == Type.BOOL)) return Type.ANY;
		if ((this.isNumber() || this == Type.BOOL) && (type.isNumber() || type == Type.BOOL)) return Type.INT;

		return Type.ANY;
	}
	
	public Type intdiv(Type type) {

		if (this instanceof CompoundType ct) {
			return Type.compound(ct.getTypes().stream().map(t -> t.intdiv(type)).collect(Collectors.toCollection(HashSet::new)));
		}
		if (type instanceof CompoundType ct) {
			return Type.compound(ct.getTypes().stream().map(t -> this.intdiv(t)).collect(Collectors.toCollection(HashSet::new)));
		}
		if (this == Type.BIG_INT && type == Type.NULL) return Type.REAL;
		if (this == Type.BIG_INT && (type.isNumber() || type == Type.BOOL) 
				|| type == Type.BIG_INT && (this.isNumber() || this == Type.BOOL)) return Type.ANY;
		if ((this.isNumber() || this == Type.BOOL) && (type.isNumber() || type == Type.BOOL)) return Type.INT;

		return Type.ANY;
	}

	public Type pow(Type type) {

		if (this instanceof CompoundType ct) {
			return Type.compound(ct.getTypes().stream().map(t -> t.pow(type)).collect(Collectors.toCollection(HashSet::new)));
		}
		if (type instanceof CompoundType ct) {
			return Type.compound(ct.getTypes().stream().map(t -> this.pow(t)).collect(Collectors.toCollection(HashSet::new)));
		}

		if (this == Type.BIG_INT && (type.isNumber() || type == Type.BOOL || type == Type.NULL)
				|| (this.isNumber() || this == Type.BOOL || this == Type.NULL) && type == Type.BIG_INT) return Type.ANY;
		if ((this == Type.INT || this == Type.BOOL || this == Type.NULL) && (type == Type.INT || type == Type.BOOL || type == Type.NULL)) return Type.INT;
		if ((this.isNumber() || this == Type.BOOL || this == Type.NULL) && (type.isNumber() || type == Type.BOOL || type == Type.NULL)) return Type.REAL;

		return Type.ANY;
	}
	
	public Type shift(Type type) {
		if (this instanceof CompoundType ct) {
			return Type.compound(ct.getTypes().stream().map(t -> t.shift(type)).collect(Collectors.toCollection(HashSet::new)));
		}
		if (type instanceof CompoundType ct) {
			return Type.compound(ct.getTypes().stream().map(t -> this.shift(t)).collect(Collectors.toCollection(HashSet::new)));
		}
		
		if (this == Type.BIG_INT) {
			return Type.BIG_INT;
		}else if (this == Type.INT) {
			return Type.INT;
		}
		return Type.ANY;
	}
	
	public Type binop(Type type) {
		if (this instanceof CompoundType ct) {
			return Type.compound(ct.getTypes().stream().map(t -> t.binop(type)).collect(Collectors.toCollection(HashSet::new)));
		}
		if (type instanceof CompoundType ct) {
			return Type.compound(ct.getTypes().stream().map(t -> this.binop(t)).collect(Collectors.toCollection(HashSet::new)));
		}
		
		if (this == Type.BIG_INT || type == Type.BIG_INT) {
			return Type.BIG_INT;
		} else if (this == Type.INT && type == Type.INT) {
			return Type.INT;
		}
		return Type.ANY;
	}

	public boolean isWarning() {
		return false;
	}

	public static Type replaceErrors(Type type) {
		if (type == Type.ERROR) return Type.NULL;
		if (type == Type.WARNING) return Type.ANY;
		if (type instanceof CompoundType ct) {
			return Type.compound(ct.getTypes().stream().map(t -> replaceErrors(t)).collect(Collectors.toCollection(HashSet::new)));
		}
		return type;
	}

	public boolean canBeNull() {
		if (this == Type.ANY || this == Type.NULL) {
			return true;
		}
		return false;
	}

	static Class<?> findClosestCommonSuper(Class<?> a, Class<?> b) {
		while (!a.isAssignableFrom(b))
			a = a.getSuperclass();
		return a;
	}

	public Complete complete() {
		var complete = new Complete(this);
		return complete;
	}

	public String getCode() {
		return name;
	}

	public boolean isCompoundNumber() {
		return false;
	}

	public boolean isPrimitive() {
		return this == Type.INT || this == Type.BOOL || this == Type.REAL;
	}

	@Override
	public int hashCode() {
		return this.signature.hashCode();
	}

	public List<Type> getArguments() {
		return new ArrayList<>();
	}

	public List<Type> getArguments(int argumentsCount) {
		return new ArrayList<>();
	}
}
