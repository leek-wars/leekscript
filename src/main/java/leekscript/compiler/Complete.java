package leekscript.compiler;

import java.util.ArrayList;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

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

		public JSONObject toJSON() {
			var o = new JSONObject();
			o.put("category", category.ordinal());
			o.put("name", name);
			o.put("type", type.toJSON());
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

	public JSON toJSON() {
		JSONObject o = new JSONObject();
		o.put("type", type.toJSON());
		var array = new JSONArray();
		for (var completion : completions) {
			array.add(completion.toJSON());
		}
		o.put("items", array);
		return o;
	}

	@Override
	public String toString() {
		return "type=" + type + " completions=" + completions.size();
	}
}
