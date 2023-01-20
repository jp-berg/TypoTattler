package main;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

public class Input {
	private Scanner scanner;
	
	public static final List<String> yesNo = List.of("Yes", "No");
	public static final List<String> yesNoCancel = List.of("Yes", "No", "Cancel"); 
	
	
	Input(InputStream source){
		 scanner = new Scanner(source);
	}
	
	Input(){
		this(System.in);
	}
	
	public int readInt() {
		scanner.nextLine();
		String tmp;
		int res = 0;
		do {
			try {
				tmp = scanner.nextLine();
				res = Integer.parseInt(tmp);
			} catch(NumberFormatException e) {
				System.err.println("Please enter a number.");
				continue;
			}
			break;
		}while(true);
			
		return res;
	}
	
	public int readInt(List<Integer> answers) {
		int answer;
		answer = this.readInt();
		while(!answers.contains(answer)) {
			System.err.println("Answer not available.");
			answer = this.readInt();
		}
		return answer;
	}
	
	public int readInt(int start, int end) {
		int answer;
		answer = this.readInt();
		while((start > answer && answer > end)) {
			System.err.println("Answer out of bounds.");
			answer = this.readInt();
		}
		return answer;
	}
	
	public int readInt(String promt, List<Integer> answers) {
		System.out.println(promt);
		return this.readInt(answers);
	}
	
	public int readInt(String promt, int start, int end) {
		System.out.println(promt);
		return this.readInt(start, end);
	}
	
	public Character getC() {
		Character res = scanner.next().charAt(0);
		return Character.toLowerCase(res);
	}
	
	public Character getC(String promt) {
		System.out.print(promt);
		return this.getC();
	}
	
	public Character getC(List<Character> answers) {
		Character answer;
		answer = this.getC();
		while(!answers.contains(answer)) {
			System.err.println("Answer not available.");
			answer = this.getC();
		}
		return answer;
	}
	
	public Character getC(String promt, List<Character> answers) {
		System.out.print(promt);
		return this.getC(answers);
	}
	
	private static String string2option(String s) {
		return "(" + s.substring(0, 1).toUpperCase() + ")" + s.substring(1);
	}
	
	public Character getChar(String promt, List<String> options) {
		System.out.print(promt);
		return getChar(options);
	}
	
	public Character getChar(String ...options) {
		return getChar(Arrays.asList(options));
	}
	
	public static String concatOptions(List<String> options) {
		var sb = new StringBuilder();
		sb.append("[");
		for(var s: options) {
			sb.append(string2option(s));
			sb.append("/");
		}
		sb.setCharAt(sb.length()-1, ']');
		return sb.toString();
	}
	
	public static List<Character> gatherFirstLetters(List<String> options){
		var l = options.stream()
				.map(s -> s.toLowerCase())
				.map(c -> c.charAt(0))
				.distinct()
				.sorted()
				.collect(Collectors.toCollection(ArrayList::new));
		
		if(l.size() != options.size()) {
			throw new IllegalArgumentException(
					"Two options start with the same letter");
		}
		return l;
	}
	
	public Character getChar(List<String> options) {
		if(options == null) throw new NullPointerException("list is null");
		if(options.isEmpty()) throw new IllegalArgumentException("list is empty");
		
		return getC(concatOptions(options), gatherFirstLetters(options));
	}
	
	public String getS() {
		String res = scanner.next();
		return res;
	}
	
	public String getS(String promt) {
		System.out.print(promt);
		return this.getS();
	}
	
	@Override
	public void finalize() {
		scanner.close();
	}
}
