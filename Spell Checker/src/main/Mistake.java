package main;
/*
 * LICENCING MISSING
 */

/**
 * 
 * This class represents a word that is not known to the dictionary in {@link main.Checker}.
 * @version 0.1
 * @author Jan Philipp Berg
 *
 */
public class Mistake implements Comparable<Mistake> {
	
	/** The word containing the mistake */
	public String wrongword; 
	
	/** A reference to the {@link main.Parser}, that identified the mistake.*/
	public Parser origin;
	
	/** The line number from the original text file the mistake was found at */
	public int lineno; 
	
	/** The most similar words {@link main.Checker} could find */
	public String[] suggestions; 
	
	/** true if the original word started with an uppercase */
	public boolean uppercase;
	
	/** Points to the next mistake for a word written in the same way as this word */
	public Mistake next = null;
	
	/**If the user decides that the spelling of wrongword was not an error this is set to
	 * false 
	 */
	public boolean valid = true; 
	
	
	/**
	 * Constructor. Creates a new mistake based on the {@link main.Parser}, the line
	 * number the mistake was found on and the spelling of the mistake.
	 * @param origin The {@link main.Parser} that identified the mistake
	 * @param lineno The line number in the original document the mistake was found on
	 * @param wrongword The spelling of the mistake
	 */
	Mistake(Parser origin, int lineno, String wrongword){
		this.wrongword = wrongword;
		this.origin = origin;
		this.lineno = lineno;
		suggestions = null;
		uppercase = Character.isUpperCase(wrongword.charAt(0));
	}
	
	/**
	 * Generates {@link main.Mistake#suggestions} this mistake based on similarities with other words from
	 * the dictionary. The suggestions are passed on to similar mistakes to avoid reprocessing.
	 * @return True if there were words similar to the mistake, false if no similarities
	 * to known words exist
	 */
	public boolean getSuggestions() {
		if(suggestions == null) {
			suggestions = origin.checker.guess(wrongword);
			passOnSuggestions();
		}
		if(suggestions.length == 0) return false;
		if(uppercase && Character.isUpperCase(suggestions[0].charAt(0))) {
			for(int i = 0; i < suggestions.length; i++) {
				suggestions[i] = Character.toUpperCase(suggestions[i].charAt(0))
						+ suggestions[i].substring(1, suggestions[i].length());
			}
		}
		
		return true;
	}

	/**
	 * Passes the generated suggestions to all mistakes with the same spelling.
	 */
	private void passOnSuggestions() {
		this.getSuggestions();
		Mistake m = this;
		while(m.next != null) {
			m = m.next;
			m.suggestions = this.suggestions;
		}
	}
	
	/**
	 * Prints the {@link main.Mistake#suggestions} for this mistake. If they are not
	 * generated yet it tries to generate them.
	 * @return True if there were suggestions, false if none are available for this mistake
	 */
	public boolean printSuggestions() {
		if(this.getSuggestions()) {
			StringBuilder sb = new StringBuilder(suggestions.length*wrongword.length()+ 20);
			int i;
			for(i = 0; i < suggestions.length -1;i++) {
				sb.append(String.format("(%d) - %s | ", i +1, suggestions[i]));
			}
			sb.append(String.format("(%d) - %s", i +1, suggestions[i])); /* Without '|' at the end*/
			System.out.println(sb);
			return true;
		}
		System.out.println("No suggestions available.");
		return false;
		
	}

	/**
	 * Marks this and all mistakes with the same spelling as invalid mistakes, i.e. they
	 * should be ignored.
	 */
	public void invalidateAll() {
		Mistake m = this;
		do {
			m.valid = false;
			m = m.next;
		}
		while(m != null);
	}
	
	@Override
	public String toString() {
		String s = ""; 
		if(this.lineno != origin.currentline) {
			origin.currentline = this.lineno;
			s = String.format("%d: %s", lineno +1, origin.lines.get(lineno));
		}
		s += (String.format("%s\t=> '%s'", System.lineSeparator(), wrongword));
		return s;
	}

	@Override
	public int compareTo(Mistake m) {
		return this.wrongword.compareToIgnoreCase(m.wrongword);
	}

}