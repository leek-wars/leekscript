package test;

import leekscript.runner.LeekConstants;

public class TestGeneral extends TestCommon {

	public void run() {

		section("typeOf()");
		// Test nombre
		code("return typeOf(255)").equals(String.valueOf(LeekConstants.TYPE_NUMBER.getIntValue()));
		code("return typeOf(255.8)").equals(String.valueOf(LeekConstants.TYPE_NUMBER.getIntValue()));
		// Test string
		code("return typeOf('coucou')").equals(String.valueOf(LeekConstants.TYPE_STRING.getIntValue()));
		// Test boolean
		code("return typeOf(false)").equals(String.valueOf(LeekConstants.TYPE_BOOLEAN.getIntValue()));
		// Test array
		code("return typeOf([1,false])").equals(String.valueOf(LeekConstants.TYPE_ARRAY.getIntValue()));
		// Test fonction
		code("return typeOf(function(){ return null; })").equals(String.valueOf(LeekConstants.TYPE_FUNCTION.getIntValue()));
		// Test null
		code("return typeOf(null)").equals(String.valueOf(LeekConstants.TYPE_NULL.getIntValue()));
		// Test piège
		code("return typeOf(function(){ return 4; }())").equals(String.valueOf(LeekConstants.TYPE_NUMBER.getIntValue()));

		section("color()");
		code("return color(255,0,255)").equals(String.valueOf(0xFF00FF));
		code("return color(255,255,0)").equals(String.valueOf(0xFFFF00));
		code("return color(0,255,255)").equals(String.valueOf(0x00FFFF));

		// Red
		code("return getRed(" + 0xAE0000 + ")").equals("174");
		// Green
		code("return getGreen(" + 0xAF00 + ")").equals("175");
		// Blue
		code("return getBlue(" + 0xAD + ")").equals("173");
	}
}
