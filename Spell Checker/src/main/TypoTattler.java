/* LICENSING MISSING */
package main;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Class containing the setup and the main loop of the program. 
 * @author Jan Philipp Berg
 * @vers 0.2
 *
 */
public class TypoTattler {
	
	/** The parser that will analyze the file.*/
	private Parser p;
	/** The class used to communicate with the user*/
	private final Input in;
	/** The path to the file in need of correction*/
	private Path toEdit;
	
	
	/**
	 * Initializes the {@link main.Checker} if there was no dictionary provided with the
	 * command line arguments or if the provided dictionary was invalid. The method tries
	 * to find a Unix-word-file in the locations specified by {@code WORDPATH1} or 
	 * {@code WORDPATH2}. If that fails it tries to access the embedded dictionary.
	 * Installed dictionaries are preferred, because it is more likely that they reflect
	 * the users idea of what a dictionary should contain (or in what language). The embedded
	 * dictionary is a crutch in case those files are inaccessible, but it is unlikely that 
	 * this is the best choice for every user ('american-english-huge' as of version 0.2).
	 * @return an initialized dictionary
	 * @throws IOException if none of the dictionaries can be found or they are inaccessible
	 */
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
	
	/**
	 * Constructor. Validates the command line arguments and tries to
	 * handle errors.
	 * @param args The command line arguments for the program
	 * @throws IOException when there was a problem with the dicts or with the file provided for editing
	 */
	public TypoTattler(String[] args) throws IOException {
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
	
	/** The {@link main.Mistake} to be analyzed and edited by the user*/
	private Mistake current = null;
	/** Controls the loop in {@link #mainloop()}. Must only be modified by {@link #exit()}*/
	private boolean noExit = true;
	
	/**
	 * Iterates through the text {@link main.Mistake} by {@link main.Mistake} guided by
	 * user input, offering various operations for correcting them.
	 */
	public void mainloop() throws IOException {
		final List<String> options = List.<String>of("next", "previous", "suggestions", 
				"revision", "add to dictionary", "ignore all", "context", "go to line", 
				"option overview", "exit");
		final String OPTIONS = Input.concatOptions(options);
		final List<Character> possibleAnswers = Input.gatherFirstLetters(options);
		System.out.println(OPTIONS);
		next();
		
		char c;
			while(noExit) {
				c = in.getC(possibleAnswers);
				switch (c) {
				case 'e' -> exit();
				case 'a' -> addToDict();
				case 'i' -> ignore();
				case 'n' -> next();
				case 'p' -> previous();
				case 'r' -> revision();
				case 's' -> suggestion();
				case 'c' -> System.out.println(p.context(current));
				case 'o' -> System.out.println(OPTIONS);
				case 'g' -> goToLine();
				}
			}
	}
	
	/** {@value} */
	private static final String CANCELTEXT = "Cancelled - New command:";
	/** Set only by {@link #replace(Mistake, String)} it tells {@link #exit()} whether
	 * {@link #toEdit} has to be saved to disk because the file has been edited.
	 */
	private boolean writeToDisk = false;
	
	/**
	 * Gets the next valid mistake and saves it in {@link TypoTattler#current}
	 * if available, otherwise (when reaching the end of the file {@link TypoTattler#toEdit})
	 * it will state this and invoke {@link TypoTattler#exit()}.
	 * @throws IOException @see {@link TypoTattler#exit()}
	 */
	private void next() throws IOException {
		if(p.hasNext()) {
			current = p.next();
			System.out.println(current);
		}else{
			System.out.print("End of file.");
			exit();
		}
	}
	
	/**
	 * Gets the previous valid mistake and saves it in {@link TypoTattler#current}
	 * if available, otherwise (when reaching the beginning of the file {@link TypoTattler#toEdit}) 
	 * it will simply state this.
	 * @throws IOException @see {@link TypoTattler#exit()}
	 */
	private void previous() throws IOException {
		if(p.hasPrevious()) {
			current = p.previous();
			System.out.println(current);
		} else {
			System.out.println("Reached start of file.");
		}
	}
	
	/**
	 * Adds the misspelling in {@link TypoTattler#current} the dictionary of the
	 * user and invokes {@link TypoTattler#ignore()}
	 * @throws IOException @see {@link TypoTattler#exit()}
	 */
	private void addToDict() throws IOException {
		char c = 'y';
		while(!p.checker.addToUsrDict(current) && c != 'n') {
			c = in.getChar("Could not save to file. Retry?", Input.yesNoCancel);
			if(c == 'c') {
				System.out.println(CANCELTEXT);
				return;
			}
		};
		ignore();
	}
	
	/**
	 * Invalidates all {@link main.Mistake} containing the same misspelling.
	 * @throws IOException @see {@link TypoTattler#exit()}
	 */
	private void ignore() throws IOException {
		p.ignore(current);
		next();
	}
	
	/**
	 * Gets called, if the end of the file has been reached or via direct request from
	 * the user. Breaks the loop in {@link TypoTattler#mainloop()}, saves the
	 * file {@link TypoTattler#toEdit} if any changes have been made and closes
	 * {@link TypoTattler#in}. Because of the latter it must be the only exit point for
	 * the mainloop (aside from any fatal crashes).
	 * @throws IOException if an I/O-Error while closing {@link TypoTattler#in} occurs
	 */
	private void exit() throws IOException {
		if(in.getChar("Exit?", Input.yesNo) == 'y') {
			noExit = false;
			if(writeToDisk) w2d();
			in.close();
		}
	}
	
	/**
	 *Provides a dialogue in which the user can enter a correction to the misspelling in
	 *{@link TypoTattler#current}. 
	 * @throws IOException @see {@link TypoTattler#exit()}
	 */
	private void revision() throws IOException {
		String correction = in.getS("Revision: ");
		if(p.checker.ismistake(correction)) {
			char c = in.getChar("Word not in dictionary. Replace anyways? ", 
					List.of("replace", 
							"add word to dictionary and replace", 
							"no", "cancel"));
			if(c == 'n') {
				revision(); //stackoverflow unreasonable, 6476 recursions necessary in testing 
				return;
			}
			if(c == 'c') {
				System.out.println(CANCELTEXT);
				return; 
			}
		}
		this.replace(current, correction);
	}
	
	/**
	 * Presents the user with replacement options for the misspelling in {@link #current}.
	 * These options are generated by {@link main.Checker#guess(String)}. The user is
	 * able to choose a replacement or to cancel. The replacement is inserted via 
	 * {@link #replace(Mistake, String)} into the line.
	 * @throws IOException @see {@link TypoTattler#exit()}
	 */
	private void suggestion() throws IOException {
		current.getSuggestions();
		System.out.print("(0) - Cancel || ");
		current.printSuggestions();
		int suggestion = in.readInt(0 , current.suggestions.length);
		
		if(suggestion != 0) {
			this.replace(current, current.suggestions[suggestion-1]);
			return;
		}
		
		System.out.println(CANCELTEXT);
	}
	
	/**
	 * Inserts the first {@link main.Mistake} following the beginning of the line
	 * corresponding to the user-provided number into {@link #current}
	 * @throws IOException @see {@link TypoTattler#exit()}
	 */
	private void goToLine() throws IOException {
		int i = in.readInt("Go to line: ", 0, p.lines.size());
		p.toLine(i-1); //toLine() works with zero-based numbers -> i-1
		Mistake m = p.next();
		if(!m.valid) {
			next();
		}else {
			current = m;
			System.out.println(current);
		}
		
	}
	
	/** {@value} */
	private static final String errmsg_replace = 
			"""
			Error: Mismatch between the line number recorded for the mistake and the actual line. The mistake
			'%s' was not found in the recorded line in the following cases:
			""" 
			+ System.lineSeparator();

	/**
	 * Replaces the mistake with a user provided alternative. If the misspelling cannot
	 * be found in the line specified by {@link main.Mistake} the line will be printed
	 * to screen.
	 * 
	 * @param current The mistake to replace
	 * @param replacement The string to correct the spelling error in mistake
	 */
	private void replace(Mistake current, String replacement) throws IOException {
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
			System.out.println(CANCELTEXT);
			return;
		}
		
		if(failed != null) {
			System.err.println(errmsg_replace);
			for(var element: failed) {
				System.err.println(element + System.lineSeparator());
			}
			return;
		} else {
			writeToDisk = true;
			next();
			return;
		}
	}
	
	/**
	 * Writes the modified file (if desired by the user). Detects name collisions with
	 * existing files and offers new names to avoid overwriting existing files.
	 */
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
