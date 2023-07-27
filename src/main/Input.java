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
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;
import static java.util.Objects.requireNonNull;

/**
 * Class that provides methods for dialogue with the user via the command line interface.
 * @author Jan Philipp Berg
 * @vers 0.2
 *
 */
public class Input implements Closeable{

	/** the Scanner this instance of this class operates on*/
	private Scanner scanner;

	/** Facilitates asking of simple yes/no-questions*/
	public static final List<String> yesNo = List.of("Yes", "No");
	/** Facilitates asking of simple yes/no-questions with the additional option to 
	 * cancel the dialogue.
	 */
	public static final List<String> yesNoCancel = List.of("Yes", "No", "Cancel"); 

	/**
	 * Constructor that takes a source for the input-stream.
	 * @param source the input-stream this instance of {@link main.Input} should operate on.
	 */
	Input(InputStream source){
		scanner = new Scanner(source);
	}

	/** Constructor that initializes the scanner with the standard input stream.*/
	Input(){
		this(System.in);
	}

	/**
	 * Reads a line and parses an Integer from that line. Loops this process until it
	 * actually reads an Integer.
	 * @return The integer that was read from {@link #scanner}.
	 */
	public int readInt() {
		scanner.nextLine();
		String tmp;
		int res = 0;
		do {
			try {
				tmp = scanner.nextLine();
				res = Integer.parseInt(tmp);
			} catch(NumberFormatException e) {
				System.err.println("Please enter a number.");
				continue;
			}
			break;
		}while(true);

		return res;
	}

	/**
	 * Gets an Integer from {@link #readInt()} and checks if the Integer is part of answers.
	 * Loops until it finds an Integer matching the criteria.
	 * @param answers a List containing all acceptable inputs
	 * @return the input that also matched one of the options contained in answers
	 * @throws IllegalArgumentException if answers is empty
	 */
	public int readInt(List<Integer> answers) {
		requireNonNull(answers);
		if(answers.isEmpty()) {
			throw new IllegalArgumentException("List with allowed answers is empty");
		}

		int answer;
		answer = this.readInt();
		while(!answers.contains(answer)) {
			System.err.println("Answer not available.");
			answer = this.readInt();
		}
		return answer;
	}

	/**
	 * Gets an Integer from {@link #readInt()} and checks if the Integer i is bigger than
	 * start and smaller than end. Loops until it finds an Integer matching the criteria.
	 * @param start the lower bound for the input
	 * @param end the upper bound for the input
	 * @return the input that lies between start and end
	 */
	public int readInt(int start, int end) {
		if(start >= end) {
			throw new IllegalArgumentException(String.format(
					"'start' (%d) must be smaller than 'end' (%d)", start, end));
		}
		int answer;
		answer = this.readInt();
		while((start > answer || answer > end)) {
			System.err.printf("Answer has to be between %d and %d (inclusive).%s",
					start, end, System.lineSeparator());
			answer = this.readInt();
		}
		return answer;
	}

	/**
	 * Wrapper for {@link #readInt(List)}. Prints the prompt to the standard output
	 * before calling {@link #readInt(List)}.
	 * @param prompt the prompt to be printed to the standard output
	 * @param answers a List containing all acceptable inputs
	 * @return the input that also matched one of the options contained in answers
	 */
	public int readInt(String prompt, List<Integer> answers) {
		System.out.println(prompt);
		return this.readInt(answers);
	}

	/**
	 * Wrapper for {@link #readInt(int, int)}. Prints the prompt to the standard output
	 * before calling {@link #readInt(int, int)}.
	 * @param prompt the prompt to be printed to the standard output
	 * @param start the lower bound for the input
	 * @param end the upper bound for the input
	 * @return the input that lies between start and end
	 */
	public int readInt(String prompt, int start, int end) {
		System.out.println(prompt);
		return this.readInt(start, end);
	}

	/**
	 * Reads a char from the {@link #scanner} and converts it to lower case.
	 * @return The char that was read
	 */
	public Character getC() {
		Character res = scanner.next().charAt(0);
		return Character.toLowerCase(res);
	}

	/**
	 * Prints the prompt to the standard output, before invoking {@link #getC()}
	 * @param prompt the prompt to appear on the standard output
	 * @return the char that was read
	 */
	public Character getC(String prompt) {
		System.out.print(prompt);
		return this.getC();
	}

	/**
	 * Reads a char from the {@link #scanner} and converts it to lower case. It then
	 * tries to find it in answers. If there is no match it waits for the next char
	 * and compares that to answers. It repeats this process until a char matching one
	 * of those in answers has been found.
	 * @param answers the List of accepted Characters
	 * @return a char that was read from {@link #scanner} and is also contained in answers
	 * @throws IllegalArgumentException if answers is empty
	 */
	public Character getC(List<Character> answers) {
		requireNonNull(answers);
		if(answers.isEmpty()) {
			throw new IllegalArgumentException("List with allowed answers is empty");
		}

		Character answer;
		answer = this.getC();
		while(!answers.contains(answer)) {
			System.err.println("Answer not available.");
			answer = this.getC();
		}
		return answer;
	}

	/**
	 * Wrapper for {@link #getC(List)}. Prints the prompt to the standard output, before
	 * invoking {@link #getC(List)}.
	 * @param prompt the prompt to appear on the standard output
	 * @param answers the List of accepted Characters
	 * @return a char that was read from {@link #scanner} and is also contained in answers
	 */
	public Character getC(String prompt, List<Character> answers) {
		System.out.print(prompt);
		return this.getC(answers);
	}

	/**
	 * Converts a normal String to an 'option-prompt'-String, e.g.
	 *<pre>
	 *-"delete" -> "(D)elete"
	 *-"Save" -> "(S)ave"
	 *-"edit" -> "(E)dit"
	 *</pre>
	 * @param s the String to be converted
	 * @return the String s as an 'option-prompt'
	 */
	private static String stringToOption(String s) {
		return "(" + s.substring(0, 1).toUpperCase() + ")" + s.substring(1);
	}

	/**
	 * Creates an option overview String from multiple Strings, e.g.
	 * <pre>
	 * - ("add", "remove", "edit") -> "[(A)dd/(R)emove/(E)dit]"
	 * - ("Yes", "No", "Cancel") -> "[(Y)es/(N)o/(C)ancel]"
	 * </pre>
	 * @param options Strings that the option-suggestion String will be created from
	 * @return the option overview String
	 * @see #stringToOption(String)
	 */
	public static String concatOptions(List<String> options) {
		if(options == null || options.isEmpty()) return "[]";
		var sb = new StringBuilder();
		sb.append("[");
		for(var s: options) {
			sb.append(stringToOption(s));
			sb.append("/");
		}
		sb.setCharAt(sb.length()-1, ']');
		return sb.toString();
	}

	/**
	 * Gathers the first letter of every String contained in options in a List. This List
	 * will be sorted alphabetically.
	 * @param options the Strings whose first letter will be gathered
	 * @return alphabetically sorted List of every first character of every String in options
	 * @throws IllegalArgumentException if the same first letter appeared multiple times
	 */
	public static List<Character> gatherFirstLetters(List<String> options){
		if(options == null || options.isEmpty()) return Collections.emptyList();
		var l = options.stream()
				.map(s -> s.toLowerCase())
				.map(c -> c.charAt(0))
				.distinct()
				.sorted()
				.collect(Collectors.toCollection(ArrayList::new));

		if(l.size() != options.size()) {
			throw new IllegalArgumentException(
					"Two options start with the same letter");
		}
		return l;
	}

	/**
	 * Wrapper for {@link #getChar(List)}. Prints the prompt to the standard output
	 * before calling {@link #getChar(List)}.
	 * @param prompt the prompt to be printed to the standard output
	 * @param options the Strings describing the options. Every first letter must be unique.
	 * @return the char that matches one of the first letters of the Strings in options
	 * @throws IllegalArgumentException if options is empty
	 * @throws IllegalArgumentException if the same first letter appeared multiple times in options
	 */
	public Character getChar(String prompt, List<String> options) {
		requireNonNull(options);
		if(options.isEmpty()) {
			throw new IllegalArgumentException("List with proposed options is empty");
		}
		System.out.print(prompt);
		return getChar(options);
	}

	/**
	 * Wrapper for {@link #getChar(List)}.
	 * @param options the Strings describing the options. Every first letter must be unique.
	 * @return the char that matches one of the first letters of the Strings in options
	 * @throws IllegalArgumentException if options is empty
	 * @throws IllegalArgumentException if the same first letter appeared multiple times in options
	 */
	public Character getChar(String ...options) {
		return getChar(Arrays.asList(options));
	}

	/**
	 * Generates an option overview from options and uses {@link #getC(String, List)} to
	 * get a char that matches one of the first letters of the Strings in options.
	 * @param options the Strings describing the options. Every first letter must be unique.
	 * @return the char that matches one of the first letters of the Strings in options
	 * @throws IllegalArgumentException if options is empty
	 * @throws IllegalArgumentException if the same first letter appeared multiple times in options
	 */
	public Character getChar(List<String> options) {
		requireNonNull(options);
		if(options.isEmpty()) throw new IllegalArgumentException("list is empty");

		return getC(concatOptions(options), gatherFirstLetters(options));
	}

	/**
	 * Reads the next line from {@link #scanner}.
	 * @return the line read from {@link #scanner}
	 */
	public String getS() {
		String res = scanner.next();
		return res;
	}

	/**
	 * Wrapper for {@link #getS()}. Prints the prompt to the standard output
	 * before calling {@link #getS()}.
	 * @param prompt prompt the prompt to be printed to the standard output
	 * @return the line read from {@link #scanner}
	 */
	public String getS(String prompt) {
		System.out.print(prompt);
		return this.getS();
	}

	@Override
	public void close() throws IOException {
		scanner.close();

	}
}
