/* LICENSING MISSING */
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

/**
 * Class providing methods to tell if a word is a misspelling according to the underlying
 * dictionary and to which words from the dictionary that misspelling is similar.
 * @author Jan Philipp Berg
 * @vers 0.2
 *
 */
public class Checker {
	
	/** Guess for the number of elements will contain, set to {@value}. */
	private static final int initialdictlen = 150000;
	/** Contains all the 'known' words */
	private HashSet<String> dict = new HashSet<String>(initialdictlen);
	/** User entries into the dictionary */
	private File usrdict = null;
	/** Length of the longest word in the dictionary. Used to create {@link #dlMatrix}. */
	private int maxwordlength = -1;
	/** The matrix used to calculate the edit-distance between two words*/
	private int[][] dlMatrix;
	
/**
 * Checks if word is the longest word in the dictionary.
 * @param word to check
 * @return true if word is the longest encountered yet, false otherwise
 */
	private boolean isMax(String word) {
		if(maxwordlength < word.length()) {
			maxwordlength = word.length();
			return true;
		}
			return false;
	}	

	/**
	 * Merges a file into {@link #dict} and updates {@link #maxwordlength}.
	 * @param in a reader for the file containing the words to be merged into {@link #dict}
	 * @throws IOException if the file cannot be read
	 */
private void file2dict(Reader in) throws IOException {
	try (BufferedReader reader = new BufferedReader(in)){
		String word = null;
		while((word = reader.readLine())!= null) {
			this.dict.add(word.toLowerCase());
			isMax(word);
		}
		dlMatrix = new int[maxwordlength][maxwordlength];
	} catch (IOException e) {
		throw new FileNotFoundException("Cannot read dictionary file");
	}
}

/**
 * Wrapper for {@link #file2dict(Reader)}.
 * @param file contains the words to be merged into {@link #dict}.
 * @throws IOException if the file cannot be read
 */
private void file2dict(File file) throws IOException {
	FileReader fr;
	try {
		fr = new FileReader(file);
	} catch (IOException e) {
		throw new FileNotFoundException("Invalid dictionary file: " + file.getPath().toString());
	}
	file2dict(fr);
}

/**
 * Responsible for initializing {@link #usrdict}. The method will create the file, if it
 * does not exists or merge the contained words into {@link #dict} if it does.
 * @throws IOException Either if the file cannot be created or cannot be read
 */
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

/**
 * Constructor that initializes the class from a provided dictionary. 
 * @param dictpath the path to the dictionary
 * @throws IOException if there is a problem with reading dictpath or with creating/accessing the user dictionary
 */
public Checker(Path dictpath) throws IOException {
	requireNonNull(dictpath);
	
	File dictfile = dictpath.toFile();
	file2dict(dictfile);
	loadUserDict();
}

/**
 * Constructor that initializes the class from the dictionary embedded into the project.
 * @throws IOException if there is a problem with accessing the embedded dictionary or with creating/accessing the user dictionary
 * @see {@link resources.american-english-huge}
 */
public Checker() throws IOException{
	String embeddedDict = "/resources/american-english-huge";
	var tmp = getClass().getResourceAsStream(embeddedDict);
	if(tmp == null) throw new FileNotFoundException("Embedded dictionary not found: " + embeddedDict);
	var isr = new InputStreamReader(tmp);
	file2dict(isr);
	loadUserDict();
}

/**
 * Checks if a String is a mistake according to the Strings saved in {@link #dict}.
 * @param word the String to check
 * @return true if {@link #dict} contains word, false otherwise
 */
public boolean isMistake(String word) {
	requireNonNull(word);
	return !this.dict.contains(word.toLowerCase());
}

/**
 * Wrapper for {@link #isMistake(Mistake)}
 * @param mistake the mistake to check
 * @return true if {@link #dict} does not contain {@link Mistake.wrongword}
 */
public boolean isMistake(Mistake mistake) {
	return this.isMistake(mistake.wrongword);
}

/**
 * Wrapper for {@link #addToUsrDict(String)}
 * @param m the word to be added to the user dictionary
 * @return true if the word was appended, false if {@link #usrdict} was not writeable
 */
public boolean addToUsrDict(Mistake m) {
	return addToUsrDict(m.wrongword);
}

/**
 * Appends word to {@link #usrdict}
 * @param word the word to be added to the user dictionary
 * @return true if the word was appended, false if {@link #usrdict} was not writeable
 */
public boolean addToUsrDict(String word) {
	requireNonNull(word);
	try(BufferedWriter bw = new BufferedWriter(new FileWriter(usrdict, true))){
		bw.append(word.toLowerCase() + System.lineSeparator());
	} catch(IOException e) {
		return false;
	}
	return true;
}

/**
 * Adds word to {@link #dict}. Resizes {@link #dlMatrix} if necessary.
 * @param word the word to be added to the dictionary
 */
public void add(String word) {
	requireNonNull(word);
	this.dict.add(word.toLowerCase());
	if(this.isMax(word)) {
		dlMatrix = new int[maxwordlength][maxwordlength];
	}
}

/**
 * Provides a list of Strings from {@link #dict} that are most similar to s.
 * @param s the String that will be checked for similarities with the words from {@link #dict}
 * @return a list of Strings similar to s (to a maximum of 10), sorted by ther
 */
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

/**
 * Wrapper for {@link #DLdist(char[], char[])}
 * @param s1 the first word
 * @param s2 the second word
 * @return the Damerau-Levenshtein distance between s1, s2
 */
private int DLdist(String s1, String s2) {
	return DLdist(s1.toCharArray(), s2.toCharArray());
}

//https://en.wikipedia.org/wiki/Damerau%E2%80%93Levenshtein_distance

/**
 * Computes the Damerau-Levenshtein distance between the words c1 and c2 and shows how
 * closely related the two words are (e.g. how many single-character-editing steps are
 * needed to turn c1 into c2 and vice versa).
 * The algorithm is a translation from the corresponding Wikipedia-article.
 * @param c1
 * @param c2
 * @return
 * @see <a href="https://en.wikipedia.org/wiki/Damerau%E2%80%93Levenshtein_distance">
	 * Damerau–Levenshtein distance</a>
 */
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
