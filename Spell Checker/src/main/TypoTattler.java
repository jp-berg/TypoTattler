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


public class TypoTattler {
	
	private Parser p;
	private final Input in = new Input();
	private Path toEdit;
	
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
				case 'e':
					if(in.getC("Exit? (Y/N)", in.yesno) == 'y') break loop;
					break;
					
				case 'a':
					c = 'y';
					while(!p.checker.addToUsrDict(current) && c != 'n') {
						c = in.getC("Could not save to file. Retry? (Y/N)", in.yesno);
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
					writeToDisk = true;
					c = 'n';
					continue;
					
				case 's':
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
			
			if(writeToDisk) {
				w2d();
			}
		}
	
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
