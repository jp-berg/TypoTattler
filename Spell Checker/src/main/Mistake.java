package main;
public class Mistake implements Comparable<Mistake>{
	public String wrongword;
	public Parser origin;
	public int lineno;
	public String[] suggestions;
	public boolean uppercase;
	public Mistake next = null;
	public boolean valid = true;
	
	Mistake(Parser origin, int lineno, String wrongword){
		this.wrongword = wrongword;
		this.origin = origin;
		this.lineno = lineno;
		suggestions = null;
		uppercase = Character.isUpperCase(wrongword.charAt(0));
	}
	
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
	
	private void passOnSuggestions() {
		this.getSuggestions();
		Mistake m = this;
		while(m.next != null) {
			m = m.next;
			m.suggestions = this.suggestions;
		}
	}
	
	public boolean printSuggestions() {
		if(this.getSuggestions()) {
			StringBuilder sb = new StringBuilder(suggestions.length*wrongword.length()+ 20);
			int i;
			for(i = 0; i < suggestions.length -1;i++) {
				sb.append(String.format("(%d) - %s | ", i +1, suggestions[i]));
			}
			sb.append(String.format("(%d) - %s", i +1, suggestions[i]));
			System.out.println(sb);
			return true;
		}
		System.out.println("No suggestions available.");
		return false;
		
	}
	
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
