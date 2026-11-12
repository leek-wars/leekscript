package leekscript.compiler;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Ce que le cache des packages (cf {@link PackageCache}) lit d'un fichier .class sans le charger :
 * les classes que son bytecode nomme, et son interface binaire, c'est-à-dire ce qu'une autre classe
 * peut en lire (parent, champs et méthodes non privés, avec leurs descripteurs). Tant que
 * l'interface binaire d'une classe ne bouge pas, le bytecode des classes qui la lisent reste valide.
 */
public class ClassFileSummary {

	private static final int ACC_PRIVATE = 0x0002;
	private static final int ACC_SYNTHETIC = 0x1000;

	/** Classes nommées par le bytecode (noms internes, `a/b/C$D`). */
	public final Set<String> referencedClasses = new TreeSet<>();
	/** Une ligne par élément de l'interface binaire, dans l'ordre du fichier. */
	public final List<String> api = new ArrayList<>();
	/** La classe parente (nom interne), null pour Object. */
	public final String superClass;

	public ClassFileSummary(byte[] bytes) throws IOException {
		var in = new DataInputStream(new ByteArrayInputStream(bytes));
		in.readInt(); // magic
		in.readInt(); // version
		int count = in.readUnsignedShort();
		var utf8 = new String[count];
		var classIndex = new int[count];
		var constants = new Object[count];
		for (int i = 1; i < count; i++) {
			int tag = in.readUnsignedByte();
			switch (tag) {
				case 1 -> utf8[i] = in.readUTF();
				case 3 -> constants[i] = in.readInt();
				case 4 -> constants[i] = in.readFloat();
				case 5 -> { constants[i] = in.readLong(); i++; }
				case 6 -> { constants[i] = in.readDouble(); i++; }
				case 7 -> classIndex[i] = in.readUnsignedShort();
				case 8 -> constants[i] = new int[] { in.readUnsignedShort() }; // chaîne : index de son texte
				case 16, 19, 20 -> in.readUnsignedShort();
				case 15 -> { in.readUnsignedByte(); in.readUnsignedShort(); }
				case 9, 10, 11, 12, 17, 18 -> in.readInt();
				default -> throw new IOException("constante inconnue " + tag);
			}
		}
		for (int i = 1; i < count; i++) {
			if (classIndex[i] != 0) referencedClasses.add(utf8[classIndex[i]]);
		}
		int access = in.readUnsignedShort();
		var self = utf8[classIndex[in.readUnsignedShort()]];
		int parent = in.readUnsignedShort();
		superClass = parent == 0 ? null : utf8[classIndex[parent]];
		var header = new StringBuilder("class " + access + " " + self + " : " + (superClass == null ? "" : superClass));
		int interfaces = in.readUnsignedShort();
		for (int i = 0; i < interfaces; i++) header.append(' ').append(utf8[classIndex[in.readUnsignedShort()]]);
		api.add(header.toString());
		for (var kind : new String[] { "field", "method" }) {
			int members = in.readUnsignedShort();
			for (int m = 0; m < members; m++) {
				int memberAccess = in.readUnsignedShort();
				var line = new StringBuilder(kind + " " + memberAccess + " " + utf8[in.readUnsignedShort()] + " " + utf8[in.readUnsignedShort()]);
				int attributes = in.readUnsignedShort();
				for (int a = 0; a < attributes; a++) {
					var name = utf8[in.readUnsignedShort()];
					int length = in.readInt();
					if (name.equals("ConstantValue")) {
						// javac recopie la valeur d'une constante chez ceux qui la lisent
						var value = constants[in.readUnsignedShort()];
						line.append(" = ").append(value instanceof int[] text ? utf8[text[0]] : String.valueOf(value));
					} else {
						in.skipBytes(length);
					}
				}
				if ((memberAccess & (ACC_PRIVATE | ACC_SYNTHETIC)) == 0) api.add(line.toString());
			}
		}
	}
}
