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

public class Parser implements Iterator<Mistake>{
	public Path filepath;
	public Checker checker;
	public ArrayList<String> lines;
	private ArrayList<Mistake> mistakes;
	private int lineno = 0, mistakeno = -1;
	public int currentline = -1;
	private final String noPunctuationRegex 
						= "(?!\\b'\\b)\\p{Punct}|\\p{Space}|\\p{Cntrl}|\\p{Digit}";//{Punct} without apostrophe
	private final Pattern noPunctuation = Pattern.compile(noPunctuationRegex); 
	
	//https://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html
	
	public Parser(Path path, Checker checker) throws IOException {
		lines = new ArrayList<>(Files.readAllLines(path));
		mistakes = new ArrayList<Mistake>(lines.size());
		ArrayList<Mistake> tmp;
		HashMap<String, Mistake> s2m = new HashMap<>(lines.size());
		
		for(String line: lines) {
			tmp = noPunctuation.splitAsStream(line)
					.filter(s -> s.length() > 0)
					.filter(s -> checker.ismistake(s))
					.map(s -> new Mistake(this, lineno, s))
					.map(m -> chainSameMistakes(m, s2m))
					.collect(Collectors.toCollection(ArrayList::new));
			
			mistakes.addAll(tmp);
			lineno++;
		}
		mistakes.trimToSize();
		filepath = path;
		this.checker = checker;
		
		
	}
	
	public Parser(Path path) throws IOException {
		this(path, new Checker());
	}
	
	private Mistake chainSameMistakes(Mistake m, HashMap<String, Mistake> s2m) {
		String s = m.wrongword.toLowerCase();
		if(s2m.putIfAbsent(s, m) != null) {
			s2m.get(s).next = m;
			s2m.replace(s, m);
		}
		
		return m;
	}

	@Override
	public boolean hasNext() {
		mistakeno++;
		while(mistakeno < mistakes.size() && !mistakes.get(mistakeno).valid) mistakeno++;
		return mistakeno < mistakes.size();
	}

	@Override
	public Mistake next() {
		return mistakes.get(mistakeno);
	}
	
	public boolean hasPrevious() {
		mistakeno--;
		while(mistakeno > 0 && !mistakes.get(mistakeno).valid) mistakeno--;
		return mistakeno > 0;
	}
	
	public Mistake previous() {
		return mistakes.get(mistakeno--);
	}
	
	public String replace(Mistake m, String replacement) {
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
	 
	public List<String> replaceAll(Mistake m, String replacement) {
		Mistake current = m;
		var failedList = new ArrayList<String>();
		String notFoundIn = null;
		int lastLine = -1;
		
		do {
			if(current.lineno != lastLine) { //The same mistake can occur multiple times in the same line, but will be corrected the first time it is encountered					
				System.out.println(lastLine);
				notFoundIn = this.replace(current, replacement);
				if(notFoundIn != null) {
					failedList.add("" + current.lineno + ": " + notFoundIn);
				}
			}
			
			lastLine = current.lineno;
			current = current.next;
		
		}while(current != null);
		
		if(failedList.isEmpty()) return null;
		return failedList;
		
	}
	
	public String context(Mistake mistake) {
		final int i = mistake.lineno;
		String res = lines.get(i);
		if(i-1 >= 0) res = lines.get(i-1) + System.lineSeparator() + res;
		if(i+1 < lines.size()) res = res + System.lineSeparator() + lines.get(i+1);
		return res;
	}
	
	public boolean writetodisk() {
		 return writetodisk(filepath);
	}
	
	public boolean writetodisk(Path path) {
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
	
	public void ignore(Mistake m) {
		checker.add(m.wrongword);
		m.invalidateAll();
		
	}
	
	public void toLine(int l) {
		l = (l > lines.size())? lines.size() : l; 
		if(l < 0) {
			mistakeno = 0;
			return;
		}
		mistakeno = binsearchMistakes(l);	
	}
	
	private int binsearchMistakes(int l) {
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
