package main;

import java.io.IOException;

public class Runner {
	
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
		} catch (Exception e) {
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
