/**
 * Copyright (C) 2023 Jan Philipp Berg <git.7ksst@aleeas.com>
 * 
 * This file is part of TypoTattler.
 * 
 * TypoTattler is free software: you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation, either 
 * version 3 of the License, or (at your option) any later version.
 * 
 * TypoTattler is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; 
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with TypoTattler. 
 * If not, see <https://www.gnu.org/licenses/>. 
 */

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
	 * 'Increments' the name of a file, eg. 
	 * <pre>
	 *  	-'foo' -> 'foo(1)'
	 * 	-'foo(1)' -> 'foo(2)'
	 * 	-'bar(21)' -> 'bar(22)'
	 * 	- etc
	 * </pre>
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

	/** The file separator of the current platform */
	private static final String FSEP = System.getProperty("file.separator");

	/**
	 * Returns the config-file-directory according to the XDG Base Directory Specification.
	 * @param appname the name of the program
	 * @return the config-file-directory corresponding to the appname
	 * @see <a href="https://specifications.freedesktop.org/basedir-spec/basedir-spec-latest.html">
	 * XDG Base Directory Specification</a>
	 */
	public static Path getConfigDir(String appname) {
		return getXDGDir("XDG_CONFIG_HOME", ".config", appname);
	}

	/**
	 * Returns the data-file-directory according to the XDG Base Directory Specification.
	 * @param appname the name of the program
	 * @return the data-file-directory corresponding to the appname
	 * @see <a href="https://specifications.freedesktop.org/basedir-spec/basedir-spec-latest.html">
	 * XDG Base Directory Specification</a>
	 */
	public static Path getDataDir(String appname) {
		return getXDGDir("XDG_DATA_HOME", String.join(FSEP, ".local", "share"), appname);
	}

	/**
	 * Returns the state-file-directory according to the XDG Base Directory Specification.
	 * @param appname the name of the program
	 * @return the state-file-directory corresponding to the appname
	 * @see <a href="https://specifications.freedesktop.org/basedir-spec/basedir-spec-latest.html">
	 * XDG Base Directory Specification</a>
	 */
	public static Path getStateDir(String appname) {
		return getXDGDir("XDG_STATE_HOME", String.join(FSEP, ".local", "state"), appname);
	}

	/**
	 * Returns the cache-file-directory according to the XDG Base Directory Specification.
	 * @param appname the name of the program
	 * @return the cache-file-directory corresponding to the appname
	 * @see <a href="https://specifications.freedesktop.org/basedir-spec/basedir-spec-latest.html">
	 * XDG Base Directory Specification</a>
	 */
	public static Path getCacheDir(String appname) {
		return getXDGDir("XGD_STATE_HOME", ".cache", appname);
	}

	/**
	 * Creates a XDG-Base-Directory-Specification-conforming path to an app-specific directory.
	 * @param envName the name of the environment-variable that may store information over the desired XDG-path
	 * @param fallback the path that should be used, if envName is not set or empty
	 * @param appname the name of the program
	 * @return the XDG-Base-Directory-Specification-conforming path to an app-specific directory
	 * @see <a href="https://specifications.freedesktop.org/basedir-spec/basedir-spec-latest.html">
	 * XDG Base Directory Specification</a>
	 */
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
	 * Substitutes a leading tilde with the home directory of the current user.
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
