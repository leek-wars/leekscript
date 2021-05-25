package leekscript.compiler;

import java.util.HashMap;

import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;

public class SimpleFileManager extends ForwardingJavaFileManager<StandardJavaFileManager> {

    private final HashMap<String, SimpleClassFile> compiled = new HashMap<>();

    public SimpleFileManager(StandardJavaFileManager delegate) {
        super(delegate);
    }

    @Override
    public JavaFileObject getJavaFileForOutput(Location location, String className, JavaFileObject.Kind kind, FileObject sibling) {
        var result = new SimpleClassFile(className);
        compiled.put(className, result);
        return result;
    }

	public SimpleClassFile get(String name) {
		return compiled.get(name);
	}

	public void clear() {
        compiled.clear();
	}

    public HashMap<String, SimpleClassFile> getCompiled() {
        return compiled;
    }
}