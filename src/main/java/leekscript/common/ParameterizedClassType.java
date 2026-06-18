package leekscript.common;

import java.util.HashMap;
import java.util.Map;

import leekscript.compiler.instruction.ClassDeclarationInstruction;

/**
 * Un {@link ClassType} raffiné par des arguments de type concrets, p. ex.
 * {@code Box<integer>} inféré depuis {@code new Box(5)}. Purement compile-time : il
 * s'efface vers EXACTEMENT le même Java que le {@code ClassType} brut (mêmes noms
 * {@code u_<Class>}, codegen inchangé), et ne fait qu'affiner le type des membres et
 * méthodes en substituant ses liaisons. Introduit en version 5.
 */
public class ParameterizedClassType extends ClassType {

	private final Map<TypeVariable, Type> bindings;

	public ParameterizedClassType(ClassDeclarationInstruction clazz, Map<TypeVariable, Type> bindings) {
		super(clazz);
		this.bindings = bindings;
	}

	public Map<TypeVariable, Type> getBindings() {
		return bindings;
	}

	// Vue de Map qui renvoie ANY pour n'importe quelle variable de type : sert à effacer
	// les variables RÉSIDUELLES (non liées par cette instanciation, p. ex. un paramètre
	// de type d'une classe parente) après substitution des liaisons concrètes.
	private static final Map<TypeVariable, Type> ERASE_RESIDUAL = new java.util.AbstractMap<>() {
		@Override public Type get(Object key) { return Type.ANY; }
		@Override public java.util.Set<Map.Entry<TypeVariable, Type>> entrySet() { return java.util.Set.of(); }
	};

	/**
	 * Substitue nos liaisons dans le type d'un membre, puis efface toute variable de type
	 * résiduelle vers sa borne (any). Un `T` brut ne doit JAMAIS fuir dans un type de
	 * membre : sinon `integer i = b.get()` (membre hérité dont le type vient d'une variable
	 * parente non liée) lèverait une erreur dure d'assignabilité au lieu de dégrader en any.
	 */
	private Type resolve(Type t) {
		// Rien à substituer pour un membre non générique (cas dominant) : pas d'alloc.
		if (t == Type.ERROR || !t.containsTypeVariable()) return t;
		var r = t.substitute(bindings);
		return r.containsTypeVariable() ? r.substitute(ERASE_RESIDUAL) : r;
	}

	@Override
	public Type member(String member) {
		return resolve(super.member(member));
	}

	@Override
	public Type elementAccess(int version, boolean strict, String key) {
		return resolve(super.elementAccess(version, strict, key));
	}

	@Override
	public Type substitute(Map<TypeVariable, Type> outer) {
		// Une variable peut apparaître dans nos liaisons (générique imbriqué dans un
		// contexte générique) : on les re-substitue. Court-circuit identité sinon.
		Map<TypeVariable, Type> next = null;
		for (var entry : bindings.entrySet()) {
			var sub = entry.getValue().substitute(outer);
			if (sub != entry.getValue()) {
				if (next == null) next = new HashMap<>(bindings);
				next.put(entry.getKey(), sub);
			}
		}
		return next == null ? this : new ParameterizedClassType(getClassDeclaration(), next);
	}

	@Override
	public boolean containsTypeVariable() {
		for (var t : bindings.values()) {
			if (t.containsTypeVariable()) return true;
		}
		return false;
	}

	@Override
	public CastType accepts(Type type) {
		// Invariance entre types paramétrés de la même classe (comme l'élément d'un
		// Array) : Box<integer> et Box<string> sont incompatibles. Un ClassType BRUT
		// (effacé, p. ex. `Box b`) reste assignable des deux côtés : on délègue alors au
		// comportement ClassType (identité de classe / héritage), permissif par effacement.
		if (type instanceof ParameterizedClassType p && p.getClassDeclaration() == this.getClassDeclaration()) {
			for (var tp : getClassDeclaration().getTypeParameters()) {
				var a = bindings.get(tp);
				var b = p.bindings.get(tp);
				if (a == null || b == null) continue;
				if (a.accepts(b) != CastType.EQUALS) return CastType.INCOMPATIBLE;
			}
			return CastType.EQUALS;
		}
		return super.accepts(type);
	}

	@Override
	public String getCode() {
		var params = getClassDeclaration().getTypeParameters();
		if (params == null) return super.getCode(); // construit sur une classe non générique : ne devrait pas arriver
		var sb = new StringBuilder(getClassDeclaration().getName());
		sb.append('<');
		for (int i = 0; i < params.size(); ++i) {
			if (i > 0) sb.append(", ");
			var t = bindings.get(params.get(i));
			sb.append(t == null ? params.get(i).getCode() : t.getCode());
		}
		sb.append('>');
		return sb.toString();
	}
}
