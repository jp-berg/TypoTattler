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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.regex.*;
import java.util.stream.Collectors;
import static java.util.Objects.requireNonNull;

/**
 * Responsible for finding mistakes and presenting a navigatable interface through which
 * they (and in turn the file containing them) can be manipulated.
 * @author Jan Philipp Berg
 * @vers 0.2
 *
 */
public class Parser implements Iterator<Mistake>{

	/** File path to the file in need of correction */
	public Path filepath;

	/** The class used to identify mistakes */
	public Checker checker;

	/** The file from {@link #filepath} broken up into lines */
	public ArrayList<String> lines;

	/** A list of the mistakes found in the order they occur in the text*/
	private ArrayList<Mistake> mistakes;

	/** The total number of lines the file from {@link #filepath} contains */
	private int lineno = 0;

	/** The number of the mistake the iterator currently is at*/
	private int	mistakeno = -1;

	/** The line of the mistake the iterator currently is at*/
	public int currentline = -1;

	/** The {@link main.Parser#noPunctuation}-regex-pattern in String-form*/
	private final String noPunctuationRegex 
	= "(?!\\b'\\b)\\p{Punct}|\\p{Space}|\\p{Cntrl}|\\p{Digit}";

	/**The regex used to tokenize the file from {@link #filepath} into words.
	 * Divides happen on the POSIX character classes for whitespace-, control-, digit-
	 * and punctuation-characters. The latter excludes apostrophes between two word
	 * boundaries, eg. does not match the apostrophe in <pre>
	 * 		- "wasn't"
	 * 		- "I'd"
	 * 		- "1's"
	 * 		- "d'accord"
	 * but matches (and tokenizes) all apostrophes in
	 * 		- "Greeces' beaches" -> "Greeces", ("",) "beaches"
	 * 		- "the boys' room" -> "the", "boys", ("",) "room"
	 * 		- "the so called 'coolest dude'" -> "the", "so", "called, ("",) "coolest", "dude" (,"")
	 * 		- "its'" -> "its"
	 * </pre>
	 * (//https://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html)
	 */
	private final Pattern noPunctuation = Pattern.compile(noPunctuationRegex); 

	//https://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html

	/**
	 * Constructor. Reads the text file and breaks it down into the individual words,
	 * then checks them with {@link main.Checker} to identify unknown spellings.
	 * @param path path to the file that is supposed to be checked for mistakes
	 * @param checker the checker responsible for identifying mistakes
	 * @throws IOException the IOException from {@link java.nio.file.Files#readAllLines(Path)}
	 * @throws IllegalArgumentException if the provided file contains no lines or if no mistakes have been found
	 */
	public Parser(Path path, Checker checker) throws IOException {
		requireNonNull(path); requireNonNull(checker);
		lines = new ArrayList<>(Files.readAllLines(path));
		if(lines.size() == 0) {
			throw new IllegalArgumentException("The file to check is empty.");
		}
		mistakes = new ArrayList<Mistake>(lines.size());
		ArrayList<Mistake> tmp;
		HashMap<String, Mistake> s2m = new HashMap<>(lines.size());

		for(String line: lines) {
			tmp = noPunctuation.splitAsStream(line)
					.filter(s -> s.length() > 0)
					.filter(s -> checker.isMistake(s))
					.map(s -> new Mistake(this, lineno, s))
					.map(m -> chainSameMistakes(m, s2m))
					.collect(Collectors.toCollection(ArrayList::new));

			mistakes.addAll(tmp);
			lineno++;
		}
		if(mistakes.isEmpty()) {
			throw new IllegalArgumentException("No mistakes found.");
		}
		mistakes.trimToSize();
		filepath = path;
		this.checker = checker;


	}

	/**
	 * Constructor. Creates a new Checker using the parameterless constructor of {@link main.Checker}
	 * @see main.Parser#Parser(Path, Checker)
	 * @param path path to the file that is supposed to be checked for mistakes
	 * @throws IOException IOException the IOException from {@link java.nio.file.Files#readAllLines(Path)}
	 */
	public Parser(Path path) throws IOException {
		this(path, new Checker());
	}

	/**
	 * If this particular spelling of a word not contained in the dictionary has been
	 * encountered before they will be linked to each other to facilitate ignoring
	 * this particular spelling faster (instead of scanning all mistakes again)
	 * @param m the mistake to be chained
	 * @param s2m a map that translates this spelling to the last mistake encountered with the same spelling
	 * @return the same mistake given as a parameter (to allow method chaining)
	 */
	private Mistake chainSameMistakes(Mistake m, HashMap<String, Mistake> s2m) {
		String s = m.wrongword.toLowerCase();
		if(s2m.putIfAbsent(s, m) != null) {
			s2m.get(s).next = m;
			s2m.replace(s, m);
		}

		return m;
	}

	/**
	 * @return Returns true if there is a valid mistake still ahead of the current position of the parser, false otherwise.
	 */
	@Override
	public boolean hasNext() {
		mistakeno++;
		while(mistakeno < mistakes.size() && !mistakes.get(mistakeno).valid) mistakeno++;
		return mistakeno < mistakes.size();
	}

	/**
	 * @return the next mistake ahead of the current position of the parser
	 */
	@Override
	public Mistake next() {
		return mistakes.get(mistakeno);
	}

	/**
	 * 
	 * @return Returns true if there are valid mistakes behind the current position of the parser, false otherwise.
	 */
	public boolean hasPrevious() {
		mistakeno--;
		while(mistakeno > 0 && !mistakes.get(mistakeno).valid) mistakeno--;
		return mistakeno > 0;
	}

	/**
	 * @return the next mistake behind of the current position of the parser
	 */
	public Mistake previous() {
		return mistakes.get(mistakeno--);
	}

	/**
	 * Replaces the spelling of the mistake with a replacement in the in-memory file
	 * representation.
	 * @param m the mistake to be replaced
	 * @param replacement the corrected spelling of the mistake
	 * @return null if the mistake was found in the specified line, the line if not
	 * @throws IllegalArgumentException if the line referenced in the mistake does not contain the mistake
	 */
	public String replace(Mistake m, String replacement) {
		requireNonNull(m); requireNonNull(replacement);
		if(!m.valid) return null;
		String line = lines.get(m.lineno), word = m.wrongword;
		if(m.uppercase) replacement.toUpperCase();

		if(line.contains(word)) {
			lines.set(m.lineno, line.replace(word, replacement));
			m.valid = false;
			return null;
		}else {
			return line;
		}
	}

	/**
	 * Calls {@link main.Parser#replace(Mistake, String)} on all following mistakes with the
	 * same spelling.
	 * @param m the mistake containing the spelling to be replaced
	 * @param replacement the corrected spelling of the mistake
	 * @return null if no error occurred, List of all lines, where the mistake could not be matched to the line
	 */
	public List<String> replaceAll(Mistake m, String replacement) {
		requireNonNull(m); requireNonNull(replacement);
		Mistake current = m;
		var failedList = new ArrayList<String>();
		String notFoundIn = null;
		int lastLine = -1;

		do {
			//The same mistake can occur multiple times in the same line, 
			//but will be corrected the first time it is encountered
			if(current.lineno != lastLine) { 					
				notFoundIn = this.replace(current, replacement);
				if(notFoundIn != null) {
					failedList.add("" + current.lineno+1 + ": " + notFoundIn);
				}
			}

			lastLine = current.lineno;
			current = current.next;

		}while(current != null);

		if(failedList.isEmpty()) return null;
		return failedList;

	}

	/**
	 * Provides (if available) the lines proceeding and following the line containing the
	 * mistake and the line itself. Useful, when the line itself is not enough to decide
	 * on a replacement.
	 * @param mistake the mistake that needs to be seen with context
	 * @return (if available) the line before, the line containing and the line after the mistake
	 */
	public String context(Mistake mistake) {
		requireNonNull(mistake);
		final int i = mistake.lineno;
		String res = lines.get(i);
		if(i-1 >= 0) res = lines.get(i-1) + System.lineSeparator() + res;
		if(i+1 < lines.size()) res = res + System.lineSeparator() + lines.get(i+1);
		return res;
	}

	/**
	 * Wrapper for {@link #writeToDisk(Path)}. Passes {@link #filepath} to
	 * the function and thus overwriting the original file with the corrected version.
	 * @return true if the file was successfully written
	 */
	public boolean writeToDisk() {
		return writeToDisk(filepath);
	}

	/**
	 * Writes the transformed version of the corrected file to the location specified in path.
	 * @param path the location where the transformed version is supposed to be saved
	 * @return true if the file was successfully written
	 */
	public boolean writeToDisk(Path path) {
		requireNonNull(path);
		try(Writer w = new BufferedWriter(new FileWriter(path.toFile()))){
			for(String s: lines) {
				w.write(s);
				w.write(System.lineSeparator());
			}
		}catch(IOException e) {
			return false;
		}
		return true;
	}

	/**
	 * Adds this mistake to the temporary dictionary and invalidates all future mistakes
	 * with the same spelling.
	 * @param the mistake containing the spelling that is supposed to be ignored
	 */
	public void ignore(Mistake m) {
		requireNonNull(m);
		checker.add(m.wrongword);
		m.invalidateAll();

	}

	/**
	 * Moves the iterator to the next mistake starting from the specified line.
	 * @param l the line number the iterator is supposed to be moved to
	 */
	public void toLine(int l) {
		l = (l > lines.size())? lines.size() : l; 
		if(l < 0) {
			mistakeno = 0;
			return;
		}
		mistakeno = binSearchMistakes(l);	
	}

	/**
	 * Uses binary search to return the number of the next mistake starting from line l.
	 * @param l the line number the iterator is supposed to be moved to
	 * @return the number of the first mistake starting with line l
	 */
	private int binSearchMistakes(int l) {
		int L = 0;
		int R = mistakes.size()-1;
		int m = 0;
		int i = 0;
		while(L < R) {
			m = Math.floorDiv(R+L, 2);
			if(mistakes.get(m).lineno < l) {
				L = m+1;
			} else {
				R = m;
			}
		}

		return L;
	}



}
