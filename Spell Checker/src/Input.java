import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Input {
	private Scanner scanner;
	public final ArrayList<Character> yesno = new ArrayList<Character>(List.of('y', 'n'));
	
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
	
	public int readInt(ArrayList<Integer> answers) {
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
	
	public int readInt(String promt, ArrayList<Integer> answers) {
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
	
	public Character getC(ArrayList<Character> answers) {
		Character answer;
		answer = this.getC();
		while(!answers.contains(answer)) {
			System.err.println("Answer not available.");
			answer = this.getC();
		}
		return answer;
	}
	
	public Character getC(String promt, ArrayList<Character> answers) {
		System.out.print(promt);
		return this.getC(answers);
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
