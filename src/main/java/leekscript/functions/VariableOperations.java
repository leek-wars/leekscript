package leekscript.functions;

import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import com.alibaba.fastjson.JSONObject;

public class VariableOperations {
	private final int[] mValues;
	private final int[] mOperations;

	public VariableOperations(JSONObject values) {
		mValues = new int[values.size()];
		mOperations = new int[values.size()];

		int id = 0;

		SortedMap<Integer, Integer> operations = new TreeMap<Integer, Integer>();

		for(Entry<String, Object> ob : values.entrySet()){
			operations.put(Integer.parseInt(ob.getKey()), Integer.parseInt((String) ob.getValue()));
		}

		for(Entry<Integer, Integer> ob : operations.entrySet()){
			mValues[id] = ob.getKey();
			mOperations[id] = ob.getValue();
			id++;
		}
	}

	public int getOperations(int value) {
		if(mOperations.length == 1) return mOperations[0];
		long v1 = -1, v2 = -1, o1 = -1, o2 = -1;
		for(int i = 0; i < mValues.length; i++){
			if(mValues[i] > value){
				if(i == 0){
					v1 = 0;
					v2 = mValues[i];
					o1 = 0;
					o2 = mOperations[i];
				}
				else{
					v1 = mValues[i - 1];
					v2 = mValues[i];
					o1 = mOperations[i - 1];
					o2 = mOperations[i];
				}
				break;
			}
		}
		if(v1 == -1){
			v1 = mValues[mValues.length - 2];
			v2 = mValues[mValues.length - 1];
			o1 = mOperations[mValues.length - 2];
			o2 = mOperations[mValues.length - 1];
		}
		if(v1 == value) return (int)o1;
		return (int)(o1 + (o2 - o1) * (value - v1) / (v2 - v1));
	}
}
