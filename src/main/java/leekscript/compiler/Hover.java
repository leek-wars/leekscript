package leekscript.compiler;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import leekscript.common.Type;

public class Hover {

	public Type type;
	public Location location;
	public String alias;
	public Location defined;
	public int size = -1;

	public Hover(Type type, Location location) {
		this(type, location, null, null);
	}

	public Hover(Location location, String alias) {
		this(null, location, alias, null);
	}

	public Hover(Type type, Location location, String alias) {
		this(type, location, alias, null);
	}

	public Hover(Type type, Location location, Location defined) {
		this(type, location, null, defined);
	}

	public Hover(Type type, Location location, String alias, Location defined) {
		this.type = type;
		this.location = location;
		this.alias = alias;
		this.defined = defined;
	}

	public JSON toJSON() {
		JSONObject o = new JSONObject();
		if (type != null) {
			o.put("type", type.toJSON());
		}
		o.put("location", location.toJSON());
		o.put("alias", alias);
		if (defined != null) {
			o.put("defined", defined.toJSON());
		}
		if (size != -1) {
			o.put("size", size);
		}
		return o;
	}

	public void setSize(int size) {
		this.size = size;
	}
}
