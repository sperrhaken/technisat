
@SuppressWarnings("serial")
public class TechnisatException extends Exception {
	public TechnisatException() {}
	
	public TechnisatException(String message) {
		super(message);
	}
	
	public TechnisatException(Throwable cause) {
		super(cause);
	}
	
	public TechnisatException(String message, Throwable cause) {
		super(message, cause);
	}
}
