import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;


public class TypoTattler {
	
	private String correction;
	private Mistake current = null;
	private boolean writetodisk = false;
	private char c = 'n', c2 = 'y';
	private int suggestion;
	private ArrayList<Character> answers;
	private Parser p;
	private Input in;
	private Path dict, toEdit;
	private boolean valid = false;
	private final String OPTIONS = "(N)ext/(P)revious/(R)evise/(S)uggestions/(A)dd to dictionary/(I)gnore all/(C)ontext/Go to (L)ine/(O)ption overview/(E)xit\n";
	private final String extendedhelpmessage = 
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
			""";
	
	public TypoTattler(String[] args) {
		
		if(args.length < 1 && args.length > 2) {
			System.err.println("Unexpected number of arguments. Exiting..."); 
			return; 
		 }
		
		if(args.length == 0 || args[0].equals("--help")) {
			System.out.print(extendedhelpmessage);
			return;
		}
		if(args[0].startsWith("~")) args[0] = args[0].replace("~", System.getProperty("user.home"));
		toEdit = Paths.get(args[0]);
		if(!Files.exists(toEdit)) {
			System.err.printf("%s is not a valid file. Exiting...\n", toEdit.toString());
			return;
		}
		
		dict = null;
		if(args.length == 2) {
			dict = Paths.get(args[1]);
			if(!Files.exists(dict)) {
				System.err.println("Dictionary does not exist. Fallback to default.");
				dict = null;
			} else {
				try {
					Checker checker = new Checker(dict);
					p = new Parser(toEdit, checker);
				} catch (IOException e) {
					System.err.println("Invalid dictionary file. Fallback to default.");
					dict = null;
					p = null;
				}
			}
		}
		
		if(dict == null) {
			try {
				p = new Parser(toEdit);
			} catch(IOException e) {
				System.err.println("Invalid standard dictionary file. Exiting...");
				return;
			}
		}
		
		in = new Input();
		answers = new ArrayList<Character>(List.of('e', 'n', 'r', 'a', 'i', 'c', 's', 'l', 'o', 'p'));
		answers.sort(Character::compare);
		
		valid = true;
		
	}
	
	public void mainloop() {
		if(!valid) return;
		System.out.println(OPTIONS);
			
			loop:
			while(true) {
				
				switch (c) {
				case 'e':
					if(in.getC("Exit? (Y/N)", in.yesno) == 'y') break loop;
					break;
				
				case 'h':
					System.out.println(OPTIONS);
					break;
					
				case 'a':
					c2 = 'y';
					while(!p.checker.addToUsrDict(current) && c2 != 'n') {
						c2 = in.getC("Could not save to file. Retry? (Y/N)", in.yesno);
					};
					c = 'n';
					//fallthrough
				
				case 'i':
					p.ignore(current);
					//fallthrough	
					
				case 'n':
					if(p.hasNext()) {
						current = p.next();
						System.out.println(current);
					}else{
						System.out.print("End of file.");
						c = 'e';
						continue;
					}
					break;
				
				case 'p':
					if(p.hasPrevious()) {
						current = p.previous();
						System.out.println(current);
					} else {
						System.out.println("Reached start of file.");
						c = 'n';
						continue;
					}
					break;
					
				case 'r':
					correction = in.getS("Revision: ");
					if(p.checker.ismistake(correction)) {
						c = in.getC("Word not in dictionary. Replace anyways? (Y/N)", in.yesno);
						if(c == 'n') {
								c = 'r';
								continue;
						}
					}
					this.replace(current, correction);
					continue;
					
				case 's':
					current.getSuggestions();
					System.out.print("(0) - Cancel || ");
					current.printSuggestions();
					suggestion = in.readInt(0 , current.suggestions.length);
					if(suggestion != 0) {
						this.replace(current, current.suggestions[suggestion-1]);
						continue;
					}
					
					System.out.print("Cancelled - New command:");
					break;
					
				
				case 'c':
					System.out.println(p.context(current));
					break;
					
				
				case 'o':
					System.out.println(OPTIONS);
					break;
					
				case 'l':
					int i = in.readInt("Go to line: ", 0, p.lines.size());
					p.toLine(i);
					c = 'n';
					continue;
				}
				
				c = in.getC(answers);	
			}
			
			if(writetodisk) {
				w2d();
			}
			
			System.out.println("Exiting...");
		}
	
	private void replace(Mistake m, String replacement) {
		c = 'n';
		if(current.next != null) {
			c = in.getC(current.wrongword + " was found more than once. Replace all? (Y/N)", in.yesno);
		}
		
		if(c == 'y') {
			p.replaceAll(current, replacement);
			c = 'n';
		}else {
			p.replace(current, replacement);
		}
		writetodisk = true;
	}
	
	private void w2d() {
		
		boolean correctpath = true;
		String tmpstr = toEdit.getFileName().toString();
		Path tmp = Paths.get(toEdit.getParent().toString(), 
				tmpstr.substring(0, tmpstr.lastIndexOf('.'))
				+"-copy.txt");
		c = in.getC(String.format("File was modified. It will be saved as '%s' (Y/N)", tmp), in.yesno);
		if(c == 'y') {
			correctpath = p.writetodisk(tmp);
		}
		if(c == 'n' || !correctpath) {
			
			do {
					if(!correctpath) System.out.print("Incorrect path. ");
					tmpstr  = in.getS("Please enter a new path:");
					if(!tmpstr.endsWith(".txt")) tmpstr += ".txt";
					if(tmpstr.startsWith("~")) tmpstr = tmpstr.replace("~", System.getProperty("user.home"));
					tmp = Paths.get(tmpstr);
					correctpath = p.writetodisk(tmp);
			}while(!correctpath);
		}
	}

}
