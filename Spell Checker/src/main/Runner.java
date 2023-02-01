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

import java.io.IOException;

/**
 * Runner for the TypoTattler. Entry point for the program.
 * @author Jan Philipp Berg
 * @vers 0.2
 *
 */
public class Runner {

	/** A message showing detailed instructions on how to use the program: {@value}*/
	private final static String HELPMESSAGE = 
			"""
					SYNOPSIS
							typotattler FILE [DICTIONARY]

					DESCRIPTION
							typotattler disassembles FILE into its individual words and checks them against the default wordlist or against a DICTIONARY if provided.
							File should be a normal textfile. DICTIONARY should be a textfile consisting of one individual word per line. Operation is facilitated via
							the keys shown in USAGE.

					USAGE
							n - Display the next valid mistake and (if not already shown) the full line the mistake belongs to.
							p - Display the previous valid mistake and (if not already shown) the full line the mistake belongs to.
							r - Provide a revision for the current (and optionally all other matching) mistakes.
							s - Get a list of words from DICTIONARY that are close to the current mistake. 
								Select a replacement by choosing the corresponding number.
							a - Add the mistake to the user DICTIONARY. All future occurrences of the mistake in this FILE (now marked as invalid)
								and others will not be marked as mistake again.
							i - Ignore this and all future occurrences of this mistake in this FILE (marks them as invalid). 
								Resets when reloading the FILE.
							c - Print the previous, the current and the next line corresponding to the current mistake.
							l - Go to the first valid mistake in the line referenced by the number or in the lines following after.
							o - Get a quick overview over the key commands described here.
							e - Exit the program and save the FILE if it was modified.
					""" + System.lineSeparator();


	/**
	 * Main method for the TypoTattler. Responsible for basic command line argument checking,
	 * creating a new instance and launching the {@link main.TypoTattler#mainloop}.
	 * @param args command line arguments
	 */
	public static void main(String[] args) {

		if(args.length > 2) {
			System.err.print("Too many arguments");
			return;
		}

		if(args.length == 0 || args[0].equals("--help")) {
			System.out.print(HELPMESSAGE);
			return;
		}

		TypoTattler tt;
		try {
			tt = new TypoTattler(args);
		} catch (IOException | IllegalArgumentException e) {
			System.err.print("Error: " + e.getMessage());
			System.err.println(". Exiting...");
			return;
		}

		try {
			tt.mainloop();
		} catch (IOException e) {
			System.err.println("Error while closing the scanner responsible for handling the user interaction");
			System.err.println(e.getMessage());
			e.printStackTrace();
		}

		System.out.println("Exiting...");
	}

}
