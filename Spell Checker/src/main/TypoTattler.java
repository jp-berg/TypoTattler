package main;
import java.io.FileNotFoundException;
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
	private final Path DICT = Paths.get(System.getProperty("user.home"), "dict.txt");
	private final String OPTIONS = "(N)ext/(P)revious/(R)evise/(S)uggestions/(A)dd to dictionary/(I)gnore all/(C)ontext/Go to (L)ine/(O)ption overview/(E)xit\n";
	
	public static String expandUser(String path) {
		if(path.startsWith("~/")) path = path.replace("~", System.getProperty("user.home"));
		return path;
	}
	
	public TypoTattler(String[] args) throws FileNotFoundException {
		
		if(args.length < 1 && args.length > 2) {
			throw new IllegalArgumentException("Unexpected number of arguments"); 
		 }
		
		args[0] = expandUser(args[0]);
		toEdit = Paths.get(args[0]);
		if(!Files.exists(toEdit)) {
			String errormsg = String.format("%s does not exist", toEdit.toString());
			throw new FileNotFoundException(errormsg);
		}
		
		dict = null;
		if(args.length == 2) {
			args[1]  = expandUser(args[1]);
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
				Checker checker = new Checker(DICT);
				p = new Parser(toEdit, checker);
			} catch(IOException e) {
				throw new FileNotFoundException("Invalid standard dictionary file: " + DICT);
			}
		}
		
		in = new Input();
		answers = new ArrayList<Character>(List.of('e', 'n', 'r', 'a', 'i', 'c', 's', 'l', 'o', 'p'));
		answers.sort(Character::compare);
		
	}
	
	public void mainloop() {
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
