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
	
	public static String expandUser(String path) {
		if(path.startsWith("~/")) path = path.replace("~", System.getProperty("user.home"));
		return path;
	}
	
	public static Path expandUser(Path path) {
		return Paths.get(expandUser(path.toString()));
	}
	

}
