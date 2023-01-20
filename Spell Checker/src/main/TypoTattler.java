package main;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;


public class TypoTattler {
	
	private Parser p;
	private final Input in = new Input();
	private Path toEdit;
	private boolean writeToDisk = false;
	
	public TypoTattler(String[] args) throws IOException {
		Path dict = null;
		Checker checker = null;
		if(args.length < 1 && args.length > 2) {
			throw new IllegalArgumentException("Unexpected number of arguments"); 
		 }
		
		args[0] = FileHelpers.expandUser(args[0]);
		toEdit = Paths.get(args[0]);
		
		if(args.length == 2) {
			args[1]  = FileHelpers.expandUser(args[1]);
			dict = Paths.get(args[1]);
			try {
				checker = new Checker(dict);
			} catch (IOException e) {
				System.err.printf("Invalid dictionary file: %s"+ System.lineSeparator()
				+ "Falling back to installed dictionary", dict);
				dict = null;
			}
		}
		
		final String WORDPATH1 = "/user/share/dict/words";
		if(dict == null) {
			dict = Path.of(WORDPATH1);
			try {
				checker = new Checker(dict);
			} catch(IOException e) { dict = null;}
		}
		
		final String WORDPATH2 = "/user/dict/words";
		if(dict == null) {
			dict = Path.of(WORDPATH2);
			try {
				checker = new Checker(dict);
			} catch(IOException e) {
				String errmsg = 
						"""
			'words'-file not accessible under '%s' or '%s'. 
			Please check if files are available and have the right permissions.
			Some IDE-launched JVMs may have trouble accessing those files.
			
						""";
				System.err.printf(errmsg, WORDPATH1, WORDPATH2);
				dict = null;
			}
		}
		
		if(dict == null) {
			System.err.println("Falling back to embedded dictionary");
			String location = "/resources/american-english-huge";
			try {
				checker = new Checker(location);
			} catch(IOException e) {
				throw new FileNotFoundException("Invalid standard dictionary file: " + location);
			}
		}
		
		p = new Parser(toEdit, checker);	
	}
	
	public void mainloop() {
		String correction;
		int suggestion;
		Mistake current = null;
		char c = 'n';
		final List<String> options = List.<String>of("next", "previous", "suggestions", 
				"revision", "add to dictionary", "ignore all", "context", "go to line", 
				"option overview", "exit");
		final String OPTIONS = Input.concatOptions(options);
		final List<Character> possibleAnswers = Input.gatherFirstLetters(options);
			
		System.out.println(OPTIONS);
			loop:
			while(true) {
				
				switch (c) {
				case 'e':
					if(in.getChar("Exit?", Input.yesNo) == 'y') break loop;
					break;
					
				case 'a':
					c = 'y';
					while(!p.checker.addToUsrDict(current) && c != 'n') {
						c = in.getChar("Could not save to file. Retry?", Input.yesNoCancel);
						if(c == 'c') break;
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
						c = in.getChar("Word not in dictionary. Replace anyways?", Input.yesNoCancel);
						if(c == 'n') {
								c = 'r';
								continue;
						}
						if(c == 'c') break;
					}
					c = this.replace(current, correction);
					continue;
					
				case 's':
					current.getSuggestions();
					System.out.print("(0) - Cancel || ");
					current.printSuggestions();
					suggestion = in.readInt(0 , current.suggestions.length);
					if(suggestion != 0) {
						c = this.replace(current, current.suggestions[suggestion-1]);
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
				
				case '0': break;
				}
				
				c = in.getC(possibleAnswers);	
			}
			
			if(writeToDisk) {
				w2d();
			}
		}
	
	private static final String errmsg_replace = """
			Error: Mismatch between the line number recorded for the mistake and the actual line. The mistake
			'%s' was not found in the recorded line in the following cases:
			""" + System.lineSeparator();
	
	private char replace(Mistake current, String replacement) {
		char c = 'n';
		List<String> failed = null;
		
		if(current.next != null) {
			c = in.getChar(current.wrongword + " was found more than once. Replace all? ", Input.yesNoCancel);
		}
		
		if(c == 'y') {
			failed = p.replaceAll(current, replacement);
		}else if (c == 'n'){
			String tmp = p.replace(current, replacement);
			if(tmp != null) failed = List.of(tmp);
		}else {
			c = '0';
		}
		
		if(failed != null) {
			System.err.println(errmsg_replace);
			for(var element: failed) {
				System.err.println(element + System.lineSeparator());
			}
			c = 'r';
		} else {
			writeToDisk = true;
			c = 'n';
		}
		System.out.println("NEXT!!!" + c);
		return c;
	}
	
	private void w2d() {
		
		boolean correctpath = true;
		Path path = FileHelpers.avoidNameCollision(toEdit);
		String tmpstr = String.format("File was modified. It will be saved as '%s'", path);
		char c = in.getChar(tmpstr, Input.yesNo);
		
		if(c == 'y') {
			correctpath = p.writetodisk(path);
		}
		if(c == 'n' || !correctpath) {
			
			do {
					if(!correctpath) System.out.printf("Invalid path: %s" + System.lineSeparator(), path);
					
					tmpstr  = in.getS("Please enter a new path:");
					tmpstr = FileHelpers.expandUser(tmpstr);
					path = Paths.get(tmpstr);
					
					if(Files.exists(path)) {
						Path tmpPath = FileHelpers.avoidNameCollision(path);
						String promt = String.format("File '%s' already exists. ", tmpstr);
						var options = List.of("Enter new name",
								"Rename to '" + tmpPath.getFileName() + "'", 
								"Overwrite");
						c = in.getChar(promt, options);
						
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
