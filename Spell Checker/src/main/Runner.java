package main;

/**
 * Runner for the TypoTattler. Entry point for the program.
 * @author Jan Philipp Berg
 * @vers 0.1
 *
 */
public class Runner {

	/**
	 * Main method for the TypoTattler. Responsible for creating a new instance and
	 * launching the mainloop.
	 * @param args command line arguments
	 */
	public static void main(String[] args) {
		TypoTattler tt = new TypoTattler(args);
		tt.mainloop();
	}

}
