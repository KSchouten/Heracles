package edu.eur.absa.model.exceptions;

public class IllegalSpanException extends ModelException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1554596948793171313L;

	public IllegalSpanException() {
		super("This is not a valid Span, as the last word is not to the right of the first word in the text.");
	}
	
	public IllegalSpanException(String message){
		super(message);
	}

}
