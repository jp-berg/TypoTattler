/* LICENSING MISSING */
package main;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static java.util.Objects.requireNonNull;

/**
 * Class containing methods aiding in the handling of files.
 * @author Jan Philipp Berg
 * @vers 0.2
 *
 */
public final class FileHelpers {

	private static final Pattern detectNameCounter = 
			Pattern.compile("\\(\\d+\\) *?$"); // ( + any number + ) + any whitespace + end of string
	
	/**
	 * 'Increments' the name of a file.
	 * Eg. 	-'foo' -> 'foo(1)'
	 * 		-'foo(1)' -> 'foo(2)'
	 * 		-'bar(21)' -> 'bar(22)'
	 * 		- etc
	 * @param name The name of the file (without the extension)
	 * @return The 'incremented' name
	 */
	public static String incrName(String name) {
		if(name == null) name = "";
		Matcher m = detectNameCounter.matcher(name);
		if(m.find()) {
			String num = name.substring(m.start() +1, name.lastIndexOf(')'));
			name = name.substring(0, m.start());
			int i = Integer.parseInt(num) +1;
			name = name + "(" + i + ")";
			
		} else {
			name = name + "(1)";
		}
		
		return name;
		
	}
	
	/**
	 * If the provided path leads to a file that already exists, this function will
	 * generate a new name for the file with {@link main.FileHelpers#incrName(String)}
	 * until it gets a path that does not conflict with any of the other files in the
	 * folder.
	 * @param path The colliding path
	 * @return A new path for the file in the same folder
	 */
	public static Path avoidNameCollision(Path path) {
		requireNonNull(path);
		String filename = path.getFileName().toString();
		String extention = "";

		int i = filename.lastIndexOf('.');
		if(i != -1) {
			extention = filename.substring(i, filename.length());
			filename = filename.substring(0, i);
		}
		
		do {
			filename = incrName(filename);
			path = path.resolveSibling(filename + extention);
		}while(Files.exists(path));
		
		return path;

	}

	private static final String FSEP = System.getProperty("file.separator");
	public static String joinStrings(String s1, String ... strings) {
		requireNonNull(s1);
		if(strings.length == 0) return s1;
		int totalLength = s1.length();
		for(var s: strings) totalLength += s.length() + 1;
		StringBuilder sb = new StringBuilder(totalLength);
		sb.append(s1);
		for(var s: strings) {
			sb.append(FSEP);
			sb.append(s);
		}
		return sb.toString();
	}

	//https://specifications.freedesktop.org/basedir-spec/basedir-spec-latest.html

	public static Path getConfigDir(String appname) {
		return getXDGDir("XDG_CONFIG_HOME", ".config", appname);
	}

	public static Path getDataDir(String appname) {
		return getXDGDir("XDG_DATA_HOME", joinStrings(".local", "share"), appname);
	}

	public static Path getStateDir(String appname) {
		return getXDGDir("XDG_STATE_HOME", joinStrings(".local", "state"), appname);
	}
	
	public static Path getCacheDir(String appname) {
		return getXDGDir("XGD_STATE_HOME", ".cache", appname);
	}

	public static Path getXDGDir(String envName, String fallback, String appname) {
		requireNonNull(envName); requireNonNull(fallback); requireNonNull(appname);
		String xdgpath = System.getenv(envName);
		Path path;
		if(xdgpath == null || xdgpath.isBlank() || xdgpath.isEmpty() || !xdgpath.startsWith("/")) {
			 path = Path.of(System.getProperty("user.home"), fallback, appname);
		} else {
			path = Path.of(xdgpath).resolve(appname);
		}
		return path;
	}

	/**
	 * Subsitutes a leading tilde with the home directory of the current user.
	 * @param path The string indicating the path
	 * @return A string with the tilde expanded to the home directory
	 */
	public static String expandUser(String path) {
		requireNonNull(path);
		if(path.startsWith("~/")) path = path.replace("~", System.getProperty("user.home"));
		return path;
	}

	/** Wrapper for {@link main.FileHelpers#expandUser(String)}
	 * @param path The path to expand
	 * @return The expanded path
	 */
	public static Path expandUser(Path path) {
		return Paths.get(expandUser(path.toString()));
	}
}
