package leekscript;

import com.alibaba.fastjson.JSONObject;

public class LeekAI {

	private final int mId;
	private final String mName;
	private String mCode;
	private String mCompiled;
	private int mMinLevel;
	private int mValid;
	private final int mOwner;
	private String mClassName;
	private int mInstructions;
	private long mModified;
	private boolean mLoaded = true;
	private boolean mAITournament = false;
	private int folder;
	public boolean v2;

	public LeekAI(int id, int owner, String name, long modified, String code, int min_level, int valid, int instructions, int folder, boolean v2) {
		mId = id;
		mName = name;
		mCode = code;
		mOwner = owner;
		mMinLevel = min_level;
		mValid = valid;
		mClassName = String.valueOf(id);
		mInstructions = instructions;
		mModified = modified;
		this.folder = folder;
		this.v2 = v2;
	}

	public boolean getAITournament() {
		return mAITournament;
	}

	public void setAITournament(boolean aiTournament) {
		mAITournament = aiTournament;
	}

	public long getModified() {
		return mModified;
	}

	public void setModified(long m) {
		mModified = m;
	}

	public String getClassName() {
		return mClassName;
	}

	public void setClassName(String classname) {
		mClassName = classname;
	}

	public int getId() {
		return mId;
	}

	public int getOwner() {
		return mOwner;
	}

	public String getName() {
		return mName;
	}

	public String getCode() {
		return mCode;
	}

	public String getCompiled() {
		return mCompiled;
	}

	public void setCompiled(String compiled) {
		mCompiled = compiled;
	}

	public int getMinLevel() {
		return mMinLevel;
	}

	public boolean setMinLevel(int min_level) {
		boolean change = mMinLevel != min_level;
		mMinLevel = min_level;
		return change;
	}

	public boolean setValid(int valid) {
		boolean change = mValid != valid;
		mValid = valid;
		return change;
	}

	public int getValid() {
		return mValid;
	}

	public int getInstructions() {
		return mInstructions;
	}

	public void setInstructions(int instructions) {
		mInstructions = instructions;
	}

	public void setCode(String code) {
		mCode = code;
	}

	public void setLoaded(boolean b) {
		mLoaded = b;
	}

	public boolean isLoaded() {
		return mLoaded;
	}

	public JSONObject getJSON() {

		JSONObject ai_obj = new JSONObject();
		ai_obj.put("id", mId);
		ai_obj.put("owner", mOwner);
		ai_obj.put("name", mName);
		ai_obj.put("modified", mModified);
		ai_obj.put("min_level", mMinLevel);
		ai_obj.put("valid", mValid);
		ai_obj.put("instructions", mInstructions);
		ai_obj.put("code", mCode);
		ai_obj.put("java", mCompiled);
		ai_obj.put("ai_tournament", mAITournament);
		ai_obj.put("folder", folder);

		return ai_obj;
	}

	public static LeekAI fromJSON(JSONObject object) {

		LeekAI retour = new LeekAI(object.getIntValue("id"), object.getIntValue("owner"),
				object.getString("name"), object.getLongValue("modified"),
				object.getString("code"), object.getIntValue("min_level"),
				object.getIntValue("valid"), object.getIntValue("instructions"), object.getIntValue("folder"), object.getBooleanValue("v2"));

		if (object.containsKey("ai_tournament") && object.getBooleanValue("ai_tournament")) {
			retour.setAITournament(true);
		}
		return retour;
	}

	public int getFolder() {
		return folder;
	}
}
