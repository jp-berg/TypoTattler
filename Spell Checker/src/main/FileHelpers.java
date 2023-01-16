package main;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class FileHelpers {

	private static final Pattern detectNameCounter = Pattern.compile("\\(\\d+\\) *?$"); // ( + any number + ) + any whitespace + end of string

	public static String incrName(String name) {
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

	public static Path avoidNameCollision(Path path) {
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
		String xdgpath = System.getenv(envName);
		Path path;
		if(xdgpath == null || xdgpath.isBlank() || xdgpath.isEmpty() || !xdgpath.startsWith("/")) {
			 path = Path.of(System.getProperty("user.home"), fallback, appname);
		} else {
			path = Path.of(xdgpath).resolve(appname);
		}
		return path;
	}

	public static String expandUser(String path) {
		if(path.startsWith("~/")) path = path.replace("~", System.getProperty("user.home"));
		return path;
	}

	public static Path expandUser(Path path) {
		return Paths.get(expandUser(path.toString()));
	}
}
