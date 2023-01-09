/* LICENSING MISSING */
package main;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class containing the setup and the main loop of the program. 
 * @author Jan Philipp Berg
 * @vers 0.1
 *
 */
public class TypoTattler {
	
	/** The parser that will analyze the file.*/
	private Parser p;
	/** The class used to communicate with the user*/
	private final Input in = new Input();
	/** The path to the file in need of correction*/
	private Path toEdit;
	
	
	/**
	 * Constructor. Validates the command line arguments and tries to
	 * handle errors.
	 * @param args The command line arguments for the program
	 * @throws FileNotFoundException
	 */
	public TypoTattler(String[] args) throws FileNotFoundException {
		Path dict;
		if(args.length < 1 && args.length > 2) {
			throw new IllegalArgumentException("Unexpected number of arguments"); 
		 }
		
		args[0] = FileHelpers.expandUser(args[0]);
		toEdit = Paths.get(args[0]);
		if(!Files.exists(toEdit)) {
			String errormsg = String.format("%s does not exist", toEdit.toString());
			throw new FileNotFoundException(errormsg);
		}
		
		dict = null;
		if(args.length == 2) {
			args[1]  = FileHelpers.expandUser(args[1]);
			dict = Paths.get(args[1]);
			try {
				Checker checker = new Checker(dict);
				p = new Parser(toEdit, checker);
			} catch (IOException e) {
				System.err.printf("Invalid dictionary file: %s\nFallback to default.\n", dict);
				dict = null;
				p = null;
			}
		}
		
		final Path DICT = Paths.get(System.getProperty("user.home"), "dict.txt");
		if(dict == null) {
			try {
				Checker checker = new Checker(DICT);
				p = new Parser(toEdit, checker);
			} catch(IOException e) {
				throw new FileNotFoundException("Invalid standard dictionary file: " + DICT);
			}
		}
		
		
		
	}
	
	/**
	 * Iterates throught the text {@link main.Mistake} by {@link main.Mistake} guided by
	 * user input, offering various operations for correcting them.
	 */
	public void mainloop() {
		boolean writeToDisk = false;
		String correction;
		int suggestion;
		Mistake current = null;
		char c = 'n';
		final String OPTIONS = 
				"""
(N)ext/(P)revious/(R)evise/(S)uggestions/(A)dd to dictionary/(I)gnore all/(C)ontext/Go to (L)ine/(O)ption overview/(E)xit\n		
				""";
		ArrayList<Character> answers = 
				new ArrayList<Character>(
						List.of('e', 'n', 'r', 'a', 'i', 'c', 's', 'l', 'o', 'p')
						);
		answers.sort(Character::compare);
		
		System.out.println(OPTIONS);	
			loop:
			while(true) {
				
				switch (c) {
				case 'e': /* Exists the loop and the program */
					if(in.getC("Exit? (Y/N)", in.yesno) == 'y') break loop;
					break;
				
				case 'h':
					System.out.println(OPTIONS);
					break;
					
				case 'a': 
					/* Adds the spelling in current to the dictionary and ignores this and future occurrences*/
					c = 'y';
					while(!p.checker.addToUsrDict(current) && c != 'n') {
						c = in.getC("Could not save to file. Retry? (Y/N)", in.yesno);
					};
					c = 'n';
					//fallthrough
				
				case 'i': /* Ignore this and future occurences of the spelling in current*/
					p.ignore(current);
					//fallthrough	
					
				case 'n': /* Move to the next mistake*/
					if(p.hasNext()) {
						current = p.next();
						System.out.println(current);
					}else{
						System.out.print("End of file.");
						c = 'e';
						continue;
					}
					break;
				
				case 'p': /* Move to the previous mistake*/
					if(p.hasPrevious()) {
						current = p.previous();
						System.out.println(current);
					} else {
						System.out.println("Reached start of file.");
						c = 'n';
						continue;
					}
					break;
					
				case 'r': /* Get a user-provided spelling for the current mistake*/
					correction = in.getS("Revision: ");
					if(p.checker.ismistake(correction)) {
						c = in.getC("Word not in dictionary. Replace anyways? (Y/N)", in.yesno);
						if(c == 'n') {
								c = 'r';
								continue;
						}
					}
					this.replace(current, correction);
					writeToDisk = true;
					c = 'n';
					continue;
					
				case 's': /* Get possible corrections for the current mistake from the dictionary*/
					current.getSuggestions();
					System.out.print("(0) - Cancel || ");
					current.printSuggestions();
					suggestion = in.readInt(0 , current.suggestions.length);
					if(suggestion != 0) {
						this.replace(current, current.suggestions[suggestion-1]);
						writeToDisk = true;
						c = 'n';
						continue;
					}
					
					System.out.print("Cancelled - New command:");
					break;
					
				
				case 'c': /* Prints the lines surrounding the mistakte to provide context: */
					System.out.println(p.context(current));
					break;
					
				
				case 'o': /* Shows the overview over the options again */
					System.out.println(OPTIONS);
					break;
					
				case 'l': /* Moves to the first mistake following the beginning of the line */
					int i = in.readInt("Go to line: ", 0, p.lines.size());
					p.toLine(i);
					c = 'n';
					continue;
				}
				
				c = in.getC(answers);	
			}
			
			if(writeToDisk) {
				w2d();
			}
		}
	
	/**
	 * Replaces the mistake with a user provided alternative.
	 * 
	 * @param current The mistake to replace
	 * @param replacement The string to correct the spelling error in mistake
	 */
	private void replace(Mistake current, String replacement) {
		char c = 'n';
		if(current.next != null) {
			c = in.getC(current.wrongword + " was found more than once. Replace all? (Y/N)", in.yesno);
		}
		
		if(c == 'y') {
			p.replaceAll(current, replacement);
		}else {
			p.replace(current, replacement);
		}
	}
	
	/**
	 * Writes the modified file (if desired by the user). Detects name collisions with
	 * existing files and offers new names to avoid overwriting existing files.
	 */
	private void w2d() {
		
		boolean correctpath = true;
		Path path = FileHelpers.avoidNameCollision(toEdit);
		String tmpstr = String.format("File was modified. It will be saved as '%s' (Y/N)", path);
		char c = in.getC(tmpstr, in.yesno);
		
		if(c == 'y') {
			correctpath = p.writetodisk(path);
		}
		if(c == 'n' || !correctpath) {
			
			do {
					if(!correctpath) System.out.printf("Invalid path: %s\n", path);
					
					tmpstr  = in.getS("Please enter a new path:");
					tmpstr = FileHelpers.expandUser(tmpstr);
					path = Paths.get(tmpstr);
					
					if(Files.exists(path)) {
						Path tmpPath = FileHelpers.avoidNameCollision(path);
						String answer = String.format("File '%s' already exists. (E)nter new name/(R)ename to '%s'/(O)verwrite", 
								tmpstr, tmpPath.getFileName());
						c = in.getC(answer, List.of('e', 'r', 'o'));
						
						switch(c) {
						case 'e': continue;
						case 'r': path = tmpPath;break;
						case 'o':break;
						}
					}
					correctpath = p.writetodisk(path);
			}while(!correctpath);
		}
	}

}
