package leekscript.compiler;

import javax.tools.SimpleJavaFileObject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;

/** Holds compiled byte code in a byte array */
public class SimpleClassFile extends SimpleJavaFileObject {

    private ByteArrayOutputStream out;
    private String name;

    public SimpleClassFile(String name) {
        super(URI.create(name), Kind.CLASS);
        this.name = name;
    }

    @Override
    public OutputStream openOutputStream() throws IOException {
        return out = new ByteArrayOutputStream();
    }

    public byte[] getCompiledBinaries() {
        return out.toByteArray();
    }

    public String getName() {
        return name;
    }
}