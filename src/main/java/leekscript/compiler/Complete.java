package leekscript.compiler;

import java.util.ArrayList;

import leekscript.util.Json;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import leekscript.common.Type;

public class Complete {

	public Type type;
	public ArrayList<CompleteItem> completions = new ArrayList<>();

	public enum CompleteCategory {
		METHOD,
		FIELD
	}

	public static class CompleteItem {
		public CompleteCategory category;
		public String name;
		public Type type;

		public CompleteItem(CompleteCategory category, String name, Type type) {
			this.category = category;
			this.name = name;
			this.type = type;
		}

		public ObjectNode toJSON() {
			var o = Json.createObject();
			o.putPOJO("category", category.ordinal());
			o.putPOJO("name", name);
			o.putPOJO("type", type.toJSON());
			return o;
		}
	}

	public Complete(Type type) {
		this.type = type == null ? Type.ANY : type;
	}

	public void add(CompleteCategory category, String x, Type type) {
		completions.add(new CompleteItem(category, x, type));
	}

	public void addAll(Complete complete) {
		completions.addAll(complete.completions);
	}

	public Object toJSON() {
		ObjectNode o = Json.createObject();
		o.putPOJO("type", type.toJSON());
		var array = Json.createArray();
		for (var completion : completions) {
			array.addPOJO(completion.toJSON());
		}
		o.putPOJO("items", array);
		return o;
	}

	@Override
	public String toString() {
		return "type=" + type + " completions=" + completions.size();
	}
}
