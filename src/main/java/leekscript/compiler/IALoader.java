package leekscript.compiler;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;

import leekscript.ErrorManager;
import leekscript.runner.AI;

public class IALoader {

	@SuppressWarnings("resource")
	public static AI loadAI(String file, String classname) {

		File f = new File(file);
		if (!f.exists())
			return null;

		URLClassLoader loader = null;
		try {
			// Not closed because we need to load internal classes, like Class$1.class etc.
			loader = new URLClassLoader(new URL[] { f.toURI().toURL() }, new ClassLoader() {});

			if (loader != null) {
				Class<?> c = loader.loadClass(classname);
				return (AI) c.newInstance();
			}
		} catch (Exception e) {
			ErrorManager.exception(e);
		}
		return null;
	}
}
