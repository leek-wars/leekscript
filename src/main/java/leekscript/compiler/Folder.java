package leekscript.compiler;

import java.io.FileNotFoundException;
import java.util.HashMap;

import leekscript.compiler.resolver.FileSystem;

public class Folder {

	protected FileSystem fs;
	private final int id;
	private final int owner;
	private String name;
	private Folder parent = null;
	private Folder root = null;
	private HashMap<String, AIFile> files = new HashMap<>();
	protected HashMap<String, Folder> folders = new HashMap<>();
	private long timestamp;

	public Folder(int owner, FileSystem fs) {
		this.fs = fs;
		this.id = 0;
		this.owner = owner;
		this.name = ".";
		this.parent = this;
		this.root = this;
		this.timestamp = 0;
	}

	public Folder(int id, int owner, String name, Folder parent, Folder root, FileSystem fs, long timestamp) {
		this.fs = fs;
		this.id = id;
		this.owner = owner;
		this.name = name;
		this.parent = parent;
		this.root = root;
		this.timestamp = timestamp;
	}

	public void setParent(Folder folder) {
		this.parent = folder;
	}

	public void setRoot(Folder root) {
		this.root = root;
	}

	public AIFile resolve(String path) throws FileNotFoundException {

		// System.out.println("Resolve " + path);

		// Chemin qui commence par / : on repart de la racine
		if (path.startsWith("/")) {
			return root.resolve(path.substring(1));
		}

		// Chemin qui commence par ./ : ignoré
		if (path.startsWith("./")) {
			return resolve(path.substring(2));
		}

		// Chemin qui commence par ../ : on cherche dans le parent
		if (path.startsWith("../")) {
			return this.parent.resolve(path.substring(3));
		}

		// Découpage des sous-dossiers
		// Certaines anciennes IA ont des / dans le nom, ils ont été remplacés par \/
		// On ne fait donc pas un split classique, les \/ sont ignorés
		for (int i = 1; i < path.length(); ++i) {
			if (path.charAt(i) == '/' && path.charAt(i - 1) != '\\') {

				var subFolder = getFolder(path.substring(0, i));
				if (subFolder == null) {
					throw new FileNotFoundException();
				}
				return subFolder.resolve(path.substring(i + 1));
			}
		}

		String name = path.replaceAll("\\\\/", "/"); // On convertit les \/ en / à nouveau

		// Recherche dans le cache
		var ai = files.get(name);
		if (ai != null) {
			// Fichier pas modifié ?
			if (fs.getAITimestamp(ai) <= ai.getTimestamp()) {
				// System.out.println("AI " + ai.getName() + " en cache " + ai.getTimestamp());
				return ai;
			}
			// System.out.println("AI " + ai.getName() + " expiré");
		}

		// Recherche d'un nouveau fichier
		ai = fs.findFile(name, this); // Throw si pas trouvé

		this.files.put(name, ai);
		return ai;
	}

	public Folder getFolder(String name) {

		var folder = this.folders.get(name);
		if (folder != null) {
			// Dossier pas modifié (supprimé) ?
			if (fs.getFolderTimestamp(folder) <= folder.getTimestamp()) {
				// System.out.println("Dossier " + folder.getName() + " en cache " + folder.getTimestamp());
				return folder;
			}
			// System.out.println("Dossier " + folder.getName() + " expiré");
		}

		// Recherche du dossier
		folder = fs.findFolder(name, this); // Throw si pas trouvé

		this.folders.put(name, folder);
		return folder;
	}

	public long getTimestamp() {
		return this.timestamp;
	}

	public AIFile getFileById(int id) {
		return null;
	}

	public boolean isLoaded(int id) {
		return false;
	}

	public Folder getFolderById(int folder) {

		return null;
	}

	public void addFile(AIFile file) {
		files.put(file.getName(), file);
	}

	public int getId() {
		return id;
	}

	public int getOwner() {
		return owner;
	}

	public void removeFile(AIFile ai) {
		files.remove(ai.getName());
	}

	public void removeFolder(Folder folder) {
		folders.remove(folder.getName());
	}

	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return name;
	}

	public Folder getParent() {
		return parent;
	}
}
