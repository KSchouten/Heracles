package edu.eur.absa.external;

import java.util.HashMap;
import java.util.HashSet;

public interface IOntology {

	public HashMap<String, String> lexToURI();
	
	public HashSet<String> test(String literal);
	
	public HashMap<String, HashSet<String>> getConceptRelations(String conceptURI);
}
 