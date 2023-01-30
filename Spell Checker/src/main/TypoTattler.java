package main;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import static java.util.Objects.requireNonNull;


public class TypoTattler {
	
	private Parser p;
	private final Input in;
	private Path toEdit;
	private boolean writeToDisk = false;
	
	private Checker initChecker() throws IOException {

		Checker checker = null;
		final Path WORDPATH1 = Path.of("/usr/share/dict/words");
		final Path WORDPATH2 = Path.of("/usr/dict/words");

		try {
			checker = new Checker(WORDPATH1);
		} catch(FileNotFoundException e) {
			checker = null;
		} 

		if(checker == null) {
			try {
				checker = new Checker(WORDPATH2);
			} catch(FileNotFoundException e) {
				
				String errmsg = String.format(
				"""
				words'-file not accessible under '%s' or '%s'. 
				Please check if files are available and have the right permissions.
				Some IDE-launched JVMs may have trouble accessing those files.
				""", WORDPATH1, WORDPATH2);
				
				System.err.print(errmsg);
				checker = null;
			}
		}

		if(checker == null) {
			System.err.println("Falling back to embedded dictionary");
			checker = new Checker();
		}

		return checker;
	}
	
	public TypoTattler(String[] args) throws IOException {
		requireNonNull(args);
		if(args.length < 1 && args.length > 2) {
			throw new IllegalArgumentException("Unexpected number of arguments"); 
		 }
		
		args[0] = FileHelpers.expandUser(args[0]);
		toEdit = Paths.get(args[0]);
		Checker checker = null;
		
		if(args.length == 2) {
			args[1]  = FileHelpers.expandUser(args[1]);
			Path dict = Paths.get(args[1]);
			try {
				checker = new Checker(dict);
			} catch (IOException e) {
				System.err.println(e.getMessage());
				System.err.println("Falling back to installed dictionary");
				checker = null;
			}
		}
		
		if(checker == null) checker = initChecker();
		
		p = new Parser(toEdit, checker);
		in = new Input();
	}
	
	private Mistake current = null;
	private boolean noExit = true; //only modified by {@link main.TypoTattler#exit}
	
	public void mainloop() throws IOException {
		String correction;
		int suggestion;
		char c = 'n';
		final List<String> options = List.<String>of("next", "previous", "suggestions", 
				"revision", "add to dictionary", "ignore all", "context", "go to line", 
				"option overview", "exit");
		final String OPTIONS = Input.concatOptions(options);
		final List<Character> possibleAnswers = Input.gatherFirstLetters(options);
		
		final String CANCELTEXT = "Cancelled - New command:";
			
		System.out.println(OPTIONS);
		
			while(noExit) {
				
				switch (c) {
				case 'e':
					exit();
					break;
					
				case 'a':
					c = 'y';
					while(!p.checker.addToUsrDict(current) && c != 'n') {
						c = in.getChar("Could not save to file. Retry?", Input.yesNoCancel);
						if(c == 'c') {System.out.println(CANCELTEXT); break;}
					};
					c = 'n';
					//fallthrough
				
				case 'i':
					ignore();
					break;
					
				case 'n':
					next();
					break;
				
				case 'p':
					previous();
					break;
					
				case 'r':
					correction = in.getS("Revision: ");
					if(p.checker.ismistake(correction)) {
						c = in.getChar("Word not in dictionary. Replace anyways?", Input.yesNoCancel);
						if(c == 'n') {
								c = 'r';
								continue;
						}
						if(c == 'c') {System.out.println(CANCELTEXT); break;}
					}
					c = this.replace(current, correction);
					if(c != '0') continue;
					System.out.println(CANCELTEXT);
					break;
					
				case 's':
					current.getSuggestions();
					System.out.print("(0) - Cancel || ");
					current.printSuggestions();
					suggestion = in.readInt(0 , current.suggestions.length);
					if(suggestion != 0) {
						c = this.replace(current, current.suggestions[suggestion-1]);
						if(c != '0') continue;
					}
					
					System.out.print(CANCELTEXT);
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
				
				if(noExit) c = in.getC(possibleAnswers);	
			}
		}
	
	private void next() throws IOException {
		if(p.hasNext()) {
			current = p.next();
			System.out.println(current);
		}else{
			System.out.print("End of file.");
			exit();
		}
	}
	
	private void previous() throws IOException {
		if(p.hasPrevious()) {
			current = p.previous();
			System.out.println(current);
		} else {
			System.out.println("Reached start of file.");
		}
	}
	
	private void ignore() throws IOException {
		p.ignore(current);
		next();
	}
	
	private void exit() throws IOException {
		if(in.getChar("Exit?", Input.yesNo) == 'y') {
			noExit = false;
			if(writeToDisk) w2d();
			in.close();
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
						String prompt = String.format("File '%s' already exists. ", tmpstr);
						var options = List.of("Enter new name",
								"Rename to '" + tmpPath.getFileName() + "'", 
								"Overwrite");
						c = in.getChar(prompt, options);
						
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
