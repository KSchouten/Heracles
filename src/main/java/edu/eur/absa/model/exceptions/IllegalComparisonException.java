package edu.eur.absa.model.exceptions;

public class IllegalComparisonException extends ModelException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3841789804548354194L;

	public IllegalComparisonException() {
		super("Words and Spans have to be from the same Document to be comparable, sorted, etc.");
	}

	
}
