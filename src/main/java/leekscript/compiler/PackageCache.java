package leekscript.compiler;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Compilation Java d'un programme v5 à packages : le package est l'unité de recompilation et de
 * cache. Les classes Java d'un package (une par module) forment un groupe, compilé d'un bloc ; le
 * fichier d'entrée et les modules hors de tout package forment le groupe du programme.
 *
 * <p>Un groupe compilé se range sous l'empreinte de son Java généré, puis sous celle de l'interface
 * binaire (cf {@link ClassFileSummary}) des classes des autres groupes que son bytecode lit :
 * {@code p/<Java>/<interfaces lues>/}. Il resservira à tout programme qui génère le même Java en
 * face des mêmes interfaces : modifier le corps d'une fonction ne recompile que son package, changer
 * une signature recompile aussi les packages qui la lisent, et l'IA et sa suite de tests partagent
 * les packages qu'elles importent toutes deux. Une entrée de cache n'est jamais réécrite.
 *
 * <p>Chaque version d'un package laisse une entrée : celles qui n'ont servi à aucune compilation
 * depuis {@link #UNUSED_DAYS} jours sont supprimées (cf {@link #sweep}). Un programme dont une
 * entrée a disparu se recompile, comme si son cache disque était vide.
 */
public class PackageCache {

	/** Groupe du programme : le fichier d'entrée et ses modules hors package. */
	public static final String PROGRAM_GROUP = "";

	public record Unit(String className, String javaCode) {}

	/** Compile des sources Java, les dossiers de `classpath` en plus de celui du compilateur : nom de classe -> bytecode. */
	public interface Javac {
		Map<String, byte[]> compile(List<Unit> units, List<File> classpath) throws LeekScriptException;
	}

	public static class Result {
		/** Toutes les classes du programme : nom -> bytecode. */
		public final Map<String, byte[]> classes = new HashMap<>();
		/** Les dossiers de cache des groupes du programme (vide sans cache disque). */
		public final List<File> directories = new ArrayList<>();
		/** Les classes de premier niveau passées à javac cette fois-ci. */
		public final List<String> compiled = new ArrayList<>();
	}

	private static class Group {
		final List<Unit> units = new ArrayList<>();
		String sourceHash;
		// Entrées de cache de ce Java, une par jeu d'interfaces lues
		List<File> candidates = List.of();
		Map<String, byte[]> classes;
		// Interface binaire de chaque classe de premier niveau du groupe
		Map<String, String> api;
		File directory;
	}

	private static final String API_FILE = "api";
	private static final String READS_FILE = "reads";
	private static final String DONE_FILE = "ok";
	private static final String SOURCES_DIRECTORY = "java";

	/** Une entrée qui n'a servi à aucune compilation depuis ce nombre de jours est supprimée. */
	public static final int UNUSED_DAYS = 30;
	private static final long DAY = 24 * 3600 * 1000L;
	// Le ménage suit une compilation qui a écrit une entrée, une fois par heure au plus par dossier
	private static final long SWEEP_INTERVAL = 3600 * 1000L;
	private static final Map<String, Long> lastSweeps = new java.util.concurrent.ConcurrentHashMap<>();

	private final File root;
	private final boolean useDisk;
	private final Map<String, Group> groups = new LinkedHashMap<>();
	// Classe de premier niveau -> son groupe
	private final Map<String, Group> owners = new HashMap<>();

	/** `root` : dossier des entrées de cache ; `useDisk` faux : tout se compile, rien ne s'écrit. */
	public PackageCache(File root, boolean useDisk) {
		this.root = root;
		this.useDisk = useDisk;
	}

	public void add(String group, String className, String javaCode) {
		var g = groups.computeIfAbsent(group, k -> new Group());
		g.units.add(new Unit(className, javaCode));
		owners.put(className, g);
	}

	public Result compile(Javac javac) throws LeekScriptException {
		var result = new Result();
		try {
			// Les groupes dont ce Java n'a jamais été compilé
			var stale = new ArrayList<Group>();
			for (var group : groups.values()) {
				group.units.sort(java.util.Comparator.comparing(Unit::className));
				var digest = digest();
				for (var unit : group.units) {
					update(digest, unit.className());
					update(digest, unit.javaCode());
				}
				group.sourceHash = hex(digest);
				group.candidates = candidates(group);
				if (group.candidates.isEmpty()) {
					stale.add(group);
				} else {
					// L'interface d'une classe ne tient qu'à son Java : toutes les entrées ont la même
					group.api = readMap(new File(group.candidates.get(0), API_FILE));
				}
			}
			compile(javac, stale, result);
			// Ceux dont le Java n'a pas changé, mais qui lisent une interface qui a changé
			stale = new ArrayList<>();
			for (var group : groups.values()) {
				if (group.classes != null) continue;
				for (var candidate : group.candidates) {
					if (readMap(new File(candidate, READS_FILE)).equals(reads(candidate))) {
						group.directory = candidate;
						break;
					}
				}
				if (group.directory == null) stale.add(group);
			}
			compile(javac, stale, result);
			// Une entrée retenue peut avoir disparu entre-temps (ménage d'un autre fil) : son groupe
			// se recompile
			var lost = new ArrayList<Group>();
			for (var group : groups.values()) {
				if (group.classes != null) continue;
				try {
					group.classes = readClasses(group.directory);
					markUsed(group.directory);
				} catch (IOException e) {
					group.directory = null;
					lost.add(group);
				}
			}
			compile(javac, lost, result);
			for (var group : groups.values()) {
				result.classes.putAll(group.classes);
				if (group.directory != null) result.directories.add(group.directory);
			}
			if (useDisk && !result.compiled.isEmpty()) sweepSometimes();
		} catch (IOException e) {
			throw new LeekScriptException(leekscript.common.Error.CANNOT_WRITE_AI, e.getMessage());
		}
		return result;
	}

	private void compile(Javac javac, List<Group> stale, Result result) throws LeekScriptException, IOException {
		if (stale.isEmpty()) return;
		var units = new ArrayList<Unit>();
		for (var group : stale) units.addAll(group.units);
		// Les autres groupes se lisent par leurs classes : n'importe laquelle de leurs entrées, leur
		// interface est la même
		var classpath = new ArrayList<File>();
		for (var group : groups.values()) {
			if (stale.contains(group)) continue;
			classpath.add(group.directory != null ? group.directory : group.candidates.get(0));
		}
		var compiled = javac.compile(units, classpath);
		for (var unit : units) result.compiled.add(unit.className());
		for (var group : stale) {
			group.classes = new HashMap<>();
			group.api = new TreeMap<>();
		}
		var summaries = new HashMap<String, ClassFileSummary>();
		for (var entry : compiled.entrySet()) {
			owners.get(topLevel(entry.getKey())).classes.put(entry.getKey(), entry.getValue());
			summaries.put(entry.getKey(), new ClassFileSummary(entry.getValue()));
		}
		for (var group : stale) {
			for (var unit : group.units) {
				var digest = digest();
				var names = new ArrayList<>(group.classes.keySet());
				java.util.Collections.sort(names);
				for (var name : names) {
					if (!topLevel(name).equals(unit.className()) || isAnonymous(name)) continue;
					for (var line : summaries.get(name).api) update(digest, line);
				}
				group.api.put(unit.className(), hex(digest));
			}
		}
		if (!useDisk) return;
		for (var group : stale) {
			var reads = new TreeMap<String, String>();
			for (var name : group.classes.keySet()) {
				for (var referenced : summaries.get(name).referencedClasses) {
					var owner = owners.get(topLevel(referenced));
					if (owner != null && owner != group) reads.put(topLevel(referenced), owner.api.get(topLevel(referenced)));
				}
			}
			group.directory = store(group, reads);
		}
	}

	/** Les interfaces que lit l'entrée `candidate`, telles qu'elles sont dans ce programme. */
	private Map<String, String> reads(File candidate) throws IOException {
		var now = new TreeMap<String, String>();
		for (var name : readMap(new File(candidate, READS_FILE)).keySet()) {
			var owner = owners.get(name);
			now.put(name, owner == null ? "" : owner.api.get(name));
		}
		return now;
	}

	private List<File> candidates(Group group) {
		var found = new ArrayList<File>();
		if (!useDisk) return found;
		var entries = new File(root, group.sourceHash).listFiles();
		if (entries == null) return found;
		java.util.Arrays.sort(entries);
		for (var entry : entries) {
			// Une entrée illisible ne sert pas : le groupe se recompilera
			if (new File(entry, DONE_FILE).exists() && new File(entry, API_FILE).canRead() && new File(entry, READS_FILE).canRead()) found.add(entry);
		}
		return found;
	}

	/** Écrit l'entrée de cache du groupe ; déjà là (un autre fil l'a écrite) : la même, on la garde. */
	private File store(Group group, Map<String, String> reads) throws IOException {
		var digest = digest();
		for (var read : reads.entrySet()) update(digest, read.getKey() + " " + read.getValue());
		var directory = new File(new File(root, group.sourceHash), hex(digest));
		if (new File(directory, DONE_FILE).exists()) {
			markUsed(directory);
			return directory;
		}
		Files.createDirectories(directory.getParentFile().toPath());
		var temporary = Files.createTempDirectory(directory.getParentFile().toPath(), "tmp");
		for (var clazz : group.classes.entrySet()) {
			Files.write(temporary.resolve(clazz.getKey() + ".class"), clazz.getValue());
		}
		// Le Java, pour le lire : à l'écart des classes, où javac le recompilerait à la place de les lire
		var sources = Files.createDirectory(temporary.resolve(SOURCES_DIRECTORY));
		for (var unit : group.units) {
			Files.writeString(sources.resolve(unit.className() + ".java"), unit.javaCode());
		}
		writeMap(temporary.resolve(API_FILE), group.api);
		writeMap(temporary.resolve(READS_FILE), reads);
		Files.writeString(temporary.resolve(DONE_FILE), "");
		try {
			Files.move(temporary, directory.toPath(), StandardCopyOption.ATOMIC_MOVE);
		} catch (IOException e) {
			if (!new File(directory, DONE_FILE).exists()) throw e;
			deleteDirectory(temporary);
		}
		return directory;
	}

	/** Date la dernière utilisation d'une entrée (son fichier témoin), une fois par jour au plus. */
	private static void markUsed(File directory) {
		var done = new File(directory, DONE_FILE);
		long now = System.currentTimeMillis();
		if (now - done.lastModified() > DAY) done.setLastModified(now);
	}

	private void sweepSometimes() {
		long now = System.currentTimeMillis();
		var last = lastSweeps.put(root.getPath(), now);
		if (last != null && now - last < SWEEP_INTERVAL) {
			lastSweeps.put(root.getPath(), last);
			return;
		}
		sweep(root, now);
	}

	/**
	 * Supprime de `root` les entrées qui n'ont servi à aucune compilation depuis UNUSED_DAYS jours
	 * (à la date `now`), et les dossiers temporaires qu'une écriture interrompue a laissés. Rend le
	 * nombre d'entrées supprimées. Le témoin part en premier : l'entrée cesse d'être proposée avant
	 * de perdre ses classes.
	 */
	public static int sweep(File root, long now) {
		int removed = 0;
		var sources = root.listFiles(File::isDirectory);
		if (sources == null) return 0;
		for (var source : sources) {
			var entries = source.listFiles(File::isDirectory);
			if (entries == null) continue;
			for (var entry : entries) {
				var done = new File(entry, DONE_FILE);
				boolean unused = done.exists() ? now - done.lastModified() > UNUSED_DAYS * DAY : now - entry.lastModified() > DAY;
				if (!unused) continue;
				done.delete();
				deleteDirectory(entry.toPath());
				removed++;
			}
			source.delete(); // sans effet tant qu'il reste une entrée
		}
		return removed;
	}

	private static void deleteDirectory(Path directory) {
		var files = directory.toFile().listFiles();
		if (files != null) {
			for (var file : files) {
				if (file.isDirectory()) deleteDirectory(file.toPath());
				else file.delete();
			}
		}
		directory.toFile().delete();
	}

	private static Map<String, byte[]> readClasses(File directory) throws IOException {
		var classes = new HashMap<String, byte[]>();
		var files = directory.listFiles((dir, name) -> name.endsWith(".class"));
		if (files == null) throw new IOException("entrée de cache illisible : " + directory);
		for (var file : files) {
			classes.put(file.getName().substring(0, file.getName().length() - ".class".length()), Files.readAllBytes(file.toPath()));
		}
		return classes;
	}

	private static Map<String, String> readMap(File file) throws IOException {
		var map = new TreeMap<String, String>();
		for (var line : Files.readAllLines(file.toPath())) {
			int space = line.indexOf(' ');
			if (space > 0) map.put(line.substring(0, space), line.substring(space + 1));
		}
		return map;
	}

	private static void writeMap(Path file, Map<String, String> map) throws IOException {
		var sb = new StringBuilder();
		for (var entry : new TreeMap<>(map).entrySet()) sb.append(entry.getKey()).append(' ').append(entry.getValue()).append('\n');
		Files.writeString(file, sb.toString());
	}

	/** `AI_x$u_Foo$1` -> `AI_x` : la classe de premier niveau, celle d'une unité Java. */
	private static String topLevel(String className) {
		int inner = className.indexOf('$');
		return inner < 0 ? className : className.substring(0, inner);
	}

	// Classe anonyme ou locale (`Foo$1`) : personne d'autre ne la nomme, et leur numérotation
	// bouge avec le corps des méthodes
	private static boolean isAnonymous(String className) {
		int last = className.lastIndexOf('$');
		return last >= 0 && last + 1 < className.length() && Character.isDigit(className.charAt(last + 1));
	}

	private static MessageDigest digest() {
		try {
			return MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

	private static void update(MessageDigest digest, String text) {
		digest.update(text.getBytes(StandardCharsets.UTF_8));
		digest.update((byte) 0);
	}

	private static String hex(MessageDigest digest) {
		var bytes = digest.digest();
		var sb = new StringBuilder();
		for (int i = 0; i < 12; i++) sb.append(String.format("%02x", bytes[i]));
		return sb.toString();
	}
}
