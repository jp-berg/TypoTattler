package main;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.*;
import java.util.Comparator;
import java.util.HashSet;
import static java.util.Objects.requireNonNull;

public class Checker {
	
	private final int initialdictlen = 150000;
	private HashSet<String> dict = new HashSet<String>(initialdictlen);
	private File usrdict = null;
	private int maxwordlength = -1;
	private int[][] dlMatrix;
	

	private boolean ismax(String word) {
		if(maxwordlength < word.length()) {
			maxwordlength = word.length();
			return true;
		}
			return false;
	}	
	
private void file2dict(Reader in) throws IOException {
	try (BufferedReader reader = new BufferedReader(in)){
		String word = null;
		while((word = reader.readLine())!= null) {
			this.dict.add(word.toLowerCase());
			ismax(word);
		}
		dlMatrix = new int[maxwordlength][maxwordlength];
	} catch (IOException e) {
		throw new FileNotFoundException("Cannot read dictionary file");
	}
}

private void file2dict(File file) throws IOException {
	FileReader fr;
	try {
		fr = new FileReader(file);
	} catch (IOException e) {
		throw new FileNotFoundException("Invalid dictionary file: " + file.getPath().toString());
	}
	file2dict(fr);
}

private void loadUserDict() throws IOException {
	Path dir = FileHelpers.getDataDir("TypoTattler");
	usrdict = dir.resolve("usrdict.txt").toFile();
	
	try {
		Files.createDirectories(dir);
	} catch (IOException e) {
		throw new IOException("Cannot create path to user dictionary: " + dir);
	}
	
	try {
	if(!usrdict.createNewFile()) {
		file2dict(new FileReader(usrdict));
	}
	} catch (IOException e) {
		throw new IOException("Cannot create user dictionary: " + usrdict);
	}
}

Checker(Path dictpath) throws IOException {
	requireNonNull(dictpath);
	
	File dictfile = dictpath.toFile();
	file2dict(dictfile);
	loadUserDict();
}

Checker() throws IOException{
	String embeddedDict = "/resources/american-english-huge";
	var tmp = getClass().getResourceAsStream(embeddedDict);
	if(tmp == null) throw new FileNotFoundException("Embedded dictionary not found: " + embeddedDict);
	var isr = new InputStreamReader(tmp);
	file2dict(isr);
	loadUserDict();
}

public boolean ismistake(String word) {
	requireNonNull(word);
	return !this.dict.contains(word.toLowerCase());
}

public boolean ismistake(Mistake mistake) {
	return this.ismistake(mistake.wrongword);
}

public boolean addToUsrDict(Mistake m) {
	return addToUsrDict(m.wrongword);
}

public boolean addToUsrDict(String word) {
	requireNonNull(word);
	try(BufferedWriter bw = new BufferedWriter(new FileWriter(usrdict, true))){
		bw.append(word.toLowerCase() + System.lineSeparator());
	} catch(IOException e) {
		return false;
	}
	return true;
}

public void add(String word) {
	requireNonNull(word);
	this.dict.add(word.toLowerCase());
	if(this.ismax(word)) {
		dlMatrix = new int[maxwordlength][maxwordlength];
	}
}

public String[] guess(String s){
	requireNonNull(s);
	s.toLowerCase();
	record StrComp(String string, int dldist) {};
	Comparator<StrComp> comp = Comparator.comparingInt(StrComp::dldist);
	var guesses = dict.stream()
						.filter(word -> s.length() +2 > word.length() && s.length() -2 < word.length())
						.map(word -> new StrComp(word, DLdist(word, s)))
						.filter(sc -> sc.dldist < 4)
						.sorted(comp)
						.limit(10)
						.map(word -> word.string)
						.toArray(String[]::new);
	return guesses;
}

private int DLdist(String s1, String s2) {
	return DLdist(s1.toCharArray(), s2.toCharArray());
}

private int DLdist(char[] c1, char[] c2) {
	
	final int height = c1.length +1;
	final int width = c2.length +1;
	int cost;
	
	for(int i = 0; i < height; i++) dlMatrix[i][0] = i;
	for(int i = 0; i < width; i++) dlMatrix[0][i] = i;
	
	for(int i = 1; i < height; i++) {
		for(int j = 1; j < width; j++) {
			if(c1[i -1] == c2[j -1]) {
				cost = 0;
			} else {
				cost = 1;
			}
			dlMatrix[i][j] = Math.min(Math.min(dlMatrix[i-1][j] +1,
										dlMatrix[i][j-1] +1),
										dlMatrix[i-1][j-1] + cost);
			
			  if(i > 1 && j > 1 && c1[i -1] == c2[j-2] && c1[i-2] == c2[j -1]) { 
				  dlMatrix[i][j] = Math.min(dlMatrix[i][j], dlMatrix[i-2][j-2] +1);
				  }
			
		}
	}
	return dlMatrix[c1.length][c2.length];
	
}

}
