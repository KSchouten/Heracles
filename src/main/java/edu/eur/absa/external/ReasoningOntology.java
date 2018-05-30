package edu.eur.absa.external;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.SimpleSelector;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.reasoner.ReasonerRegistry;
import org.apache.jena.util.FileManager;
import org.apache.jena.util.iterator.ExtendedIterator;

import edu.eur.absa.Framework;
import edu.eur.absa.model.Dataset;
import edu.eur.absa.model.Span;
import edu.eur.absa.model.Word;

public class ReasoningOntology implements IOntology {

	private static String singletonOntologyFile="";
	private static ReasoningOntology singletonOntology;
	
	public final String NS = "http://www.kimschouten.com/sentiment/restaurant"; 
	public final String URI_ActionMention = NS+"#ActionMention";
	public final String URI_EntityMention = NS+"#EntityMention";
	public final String URI_PropertyMention = NS+"#PropertyMention";
//	public final String URI_NamedEntityMention = NS+"NamedEntityMention";
//	public final String URI_Statement = NS+"#Statement";
	public final String URI_Sentiment = NS+"#Sentiment";
	public final String URI_Positive = NS+"#Positive";
	public final String URI_Negative = NS+"#Negative";
//	public final String URI_Decrease = NS+"#Decrease";
//	public final String URI_Increase = NS+"#Increase";
//	public final String URI_NegativeEntityMention = NS+"#NegativeEntityMention";
//	public final String URI_PositiveEntityMention = NS+"#PositiveEntityMention";
//	public final String URI_TransitiveMention = NS+"TransitiveChangeMention";
	public final String URI_Mention = NS+"#Mention";
	
	private final int SAVE_MAX_COUNTER = 10;
	private int saveCounter = 0;
	
	private OntModel ontology = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM_RULE_INF);
	
	private OntModel data;// = FileManager.get().loadModel("file:data/rdfsDemoData.rdf");
//	private InfModel ontology;// = ModelFactory.createRDFSModel(schema, data);
	
	private HashMap<String, HashSet<String>> superclasses = new HashMap<>();
	private HashMap<String, String> antonyms = new HashMap<>();
	
	private ReasoningOntology(String ontologyFile){
//		data = ModelFactory.createOntologyModel();
		
		// use the FileManager to find the input file
		 InputStream in = FileManager.get().open( ontologyFile );
		if (in == null) {
		    throw new IllegalArgumentException(
		                                 "File: " + ontologyFile + " not found");
		}

		
		// read the RDF/XML file
		ontology.read(in, null);
		data = ontology;
//		data.read(in, null);
		updateInfModel();
		
	}
	
	private void updateInfModel() {
//		ontology = ModelFactory.createInfModel(
//				ReasonerRegistry.getOWLMiniReasoner(), data);
	}

	public static ReasoningOntology getOntology(String ontologyFile) {
		if (singletonOntologyFile.equalsIgnoreCase(ontologyFile)) {
			return singletonOntology;
		} else {
			singletonOntologyFile = ontologyFile;
			singletonOntology = new ReasoningOntology(ontologyFile);
			return singletonOntology;
		}
	}
	
	public void save(String ontologyFile){
		save(ontologyFile, false);
	}
	
	public void save(String ontologyFile, boolean saveNow){
		if (!saveNow && this.saveCounter < this.SAVE_MAX_COUNTER){
			saveCounter++;
			return;
		}
		
		saveCounter = 0;
		
		try {
			ontology.write(new FileOutputStream(new File(Framework.EXTERNALDATA_PATH+ontologyFile)), "RDF/XML",null);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
//	public String addIndividual(String lemma, String classURI, String... additionalClasses){
//		Individual indiv = data.createIndividual(NS+"I"+lemma, ontology.getResource(classURI));
//		for (String addClass :additionalClasses){
//			System.out.println(addClass + "\t" + data.getOntClass(addClass));
//			indiv.addOntClass(data.getOntClass(addClass));
//		}
//		this.updateInfModel();
//		return indiv.getURI();
//	}
	
	public String addClass(String lemmaURI, String... classURIs){
		HashSet<String> existingURIs = getLexicalizedConcepts(URI_Mention, lemmaURI.toLowerCase());
		if (existingURIs.size() > 0){
			return existingURIs.iterator().next();
		}
		
		String URI = NS + "#" + lemmaURI.replaceAll(" ", "");
//		if (ontology.getResource(URI) == null){
//		OntClass newClass = (OntClass) ontology.getResource(URI);
		OntClass newClass = data.createClass(URI);
		newClass.addProperty(ontology.getProperty(NS+"#lex"), lemmaURI.toLowerCase());
		for (String classURI : classURIs){
			newClass.addSuperClass(ontology.getResource(classURI));
		}
		Framework.log("Add Class: "+URI);
		this.updateInfModel();
		return newClass.getURI();
//		} else {
//			return URI;
//		}
	}
	
	
	
	
//	public void addProperty(String individualURI, String propertyURI, String objectURI){
//		ontology.getIndividual(individualURI).addProperty(ontology.getProperty(propertyURI), ontology.getResource(objectURI));
//	}
	
	public HashSet<String> getLexicalizedConcepts(String superclassURI, String lemma){
		return getLexicalizedConcepts(superclassURI, NS+"#lex", lemma);
	}
	
	public HashSet<String> getLexicalizedConcepts(String superclassURI, String annotationType, String lemma){
		Literal literal;
		if (lemma == null){
			literal = null;
		} else {
			literal = ontology.createLiteral(lemma);
		}
		StmtIterator iter = ontology.listStatements(new SimpleSelector(null, ontology.getProperty(annotationType),literal));
		
		
		HashSet<String> ontoConcepts=new HashSet<String>();
//		System.out.println(literal);
		while (iter.hasNext()) {
			
			Statement stmt = iter.nextStatement();
			Resource  subject   = stmt.getSubject();     // get the subject
			StmtIterator iter2 = ontology.listStatements(new SimpleSelector(
					subject, 
					ontology.getProperty("http://www.w3.org/2000/01/rdf-schema#subClassOf"),
//					ontology.getProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"),
					ontology.getResource(superclassURI)));
			if (iter2.hasNext())
				ontoConcepts.add(subject.getURI());
			
//			System.out.println(subject.toString());
		}
		return ontoConcepts;
	}
	
	/**
	 * Get (the) one subclass of ActionMention with this lemma. If there are more, just one of them is returned.
	 * @param lemma
	 * @return
	 */
	public String getLexicalizedAction(String lemma){
		HashSet<String> res = getLexicalizedConcepts(this.URI_ActionMention,lemma);
		if (res.isEmpty()){
			return null;
		} else {
			return res.iterator().next();	
		}
		
	}
	/**
	 * Get (the) one subclass of EntityMention with this lemma. If there are more, just one of them is returned.
	 * @param lemma
	 * @return
	 */
	public String getLexicalizedEntity(String lemma){
		HashSet<String> res = getLexicalizedConcepts(this.URI_EntityMention,lemma);
		if (res.isEmpty()){
			return null;
		} else {
			return res.iterator().next();	
		}
	}
	
	/**
	 * Get (the) one subclass of EntityMention with this lemma. If there are more, just one of them is returned.
	 * @param lemma
	 * @return
	 */
	public String getLexicalizedProperty(String lemma){
		HashSet<String> res = getLexicalizedConcepts(this.URI_PropertyMention,lemma);
		if (res.isEmpty()){
			return null;
		} else {
			return res.iterator().next();	
		}
	}
	
	public HashSet<String> test(String literal){
		
		StmtIterator iter = ontology.listStatements(new SimpleSelector(null, ontology.getProperty("lex"),ontology.createLiteral(literal)));
		
		
		HashSet<String> ontoConcepts=new HashSet<String>();
//		System.out.println(literal);
		while (iter.hasNext()) {
			
			Statement stmt = iter.nextStatement();
			Resource  subject   = stmt.getSubject();     // get the subject
//			StmtIterator iter2 = ontology.listStatements(new SimpleSelector(
//					subject, 
//					ontology.getProperty("http://www.w3.org/2000/01/rdf-schema#subClassOf"),
//					ontology.getResource(NS+"#Mention")));
//			if (iter2.hasNext())
				ontoConcepts.add(subject.getURI());
			
//			System.out.println(subject.toString());
		}
		return ontoConcepts;
	}
	
	public HashMap<String, HashSet<String>> getConceptRelations(String conceptURI){
		HashMap<String, HashSet<String>> relations = new HashMap<>();
		
		Resource concept = ontology.getResource(conceptURI);
		if (concept == null){
			return relations;
		}
		// get all statements where the given concept is the subject
		StmtIterator iter = ontology.listStatements(new SimpleSelector(concept,	null,(RDFNode)null));
		while (iter.hasNext()) {
		    Statement stmt      = iter.nextStatement();  // get next statement
		    Resource  subject   = stmt.getSubject();     // get the subject
		    Property  predicate = stmt.getPredicate();   // get the predicate
		    RDFNode   object    = stmt.getObject();      // get the object
			
		    if (!relations.containsKey(predicate.toString())){
		    	relations.put(predicate.toString(), new HashSet<String>());
		    }
		    relations.get(predicate.toString()).add(object.toString());		    
		}
		
		// get all statements where the given concept is the object
				iter = ontology.listStatements(new SimpleSelector(null,	null,(RDFNode)concept));
				while (iter.hasNext()) {
				    Statement stmt      = iter.nextStatement();  // get next statement
				    Resource  subject   = stmt.getSubject();     // get the subject
				    Property  predicate = stmt.getPredicate();   // get the predicate
				    RDFNode   object    = stmt.getObject();      // get the object
					
				
				    if (!relations.containsKey("inverse::"+predicate.toString())){
				    	relations.put("inverse::"+predicate.toString(), new HashSet<String>());
				    }
				    relations.get("inverse::"+predicate.toString()).add(subject.toString());		    
				}
		
		return relations;
	}
	
	/**
	 * Get all (inferred) superclasses of a target lexicalization
	 * @param targetURI
	 * @return
	 */
	public HashSet<String> getSuperclasses(String classURI){
		if (!superclasses.containsKey(classURI)){
			superclasses.put(classURI, new HashSet<>());
			superclasses.get(classURI).addAll(getObjects(classURI, "http://www.w3.org/2000/01/rdf-schema#subClassOf"));
		}
		if (superclasses.get(classURI) == null){
			Framework.error("No superclasses for: "+classURI);
		}
		return superclasses.get(classURI);
	}
	
	public String getAntonym(String classURI){
		if (antonyms.containsKey(classURI)) {
			return antonyms.get(classURI);
		} else {
			HashSet<String> antonymURIs = getConceptRelations(classURI).get("http://www.w3.org/2002/07/owl#disjointWith");
			String antonymURI = null;
			if (antonymURIs != null) {
				antonymURI = antonymURIs.iterator().next();
			}
			antonyms.put(classURI, antonymURI);
			return antonymURI;
		}
		
	}
	
	public HashSet<String> getClasses(String indivURI){
		return getObjects(indivURI, "http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
	}	
	
	public HashSet<String> getSubclasses(String classURI){
		return getSubjects(classURI, "http://www.w3.org/2000/01/rdf-schema#subClassOf");
	}
	
	public HashSet<String> getObjects(String subjectURI, String predicateURI){
		StmtIterator iter = ontology.listStatements(new SimpleSelector(ontology.getResource(subjectURI), ontology.getProperty(predicateURI),(RDFNode)null));
		HashSet<String> targetTypes = new HashSet<String>();
		while (iter.hasNext()) {
			Statement stmt      = iter.nextStatement();  // get next statement
			RDFNode object    = stmt.getObject();      // get the object
			if (object.isResource()){
				targetTypes.add(object.asResource().getURI());
			} else if (object.isLiteral()){
				targetTypes.add(object.asLiteral().getLexicalForm());
			}
		}
		targetTypes.remove(null);
		return targetTypes;
	}
	public HashSet<String> getSubjects(String objectURI, String predicateURI){
		StmtIterator iter = ontology.listStatements(new SimpleSelector(null,ontology.getProperty(predicateURI),ontology.getResource(objectURI)));
		HashSet<String> targetTypes = new HashSet<String>();
		while (iter.hasNext()) {
			Statement stmt      = iter.nextStatement();  // get next statement
			Resource subject = stmt.getSubject();
			targetTypes.add(subject.asResource().getURI());
		}
		targetTypes.remove(null);
		return targetTypes;
	}
	
	
	public HashMap<String, String> lexToURI(){
		StmtIterator iter = ontology.listStatements(new SimpleSelector(null, ontology.getProperty(NS+"#lex"),(Literal)null));
		
		
		HashMap<String, String> ontoConcepts=new HashMap<String,String>();
		
		while (iter.hasNext()) {
			
			Statement stmt = iter.nextStatement();
			Resource  subject   = stmt.getSubject();     // get the subject
			RDFNode lex = stmt.getObject();
			StmtIterator iter2 = ontology.listStatements(new SimpleSelector(
					subject, 
					ontology.getProperty("http://www.w3.org/2000/01/rdf-schema#subClassOf"),
					ontology.getResource(this.URI_Mention)));
			if (iter2.hasNext()){
				ontoConcepts.put(lex.toString(), subject.getURI());
			} else {
				System.out.println("No subclass of Mention: "+subject.toString());
				
			}
//			
		}
		return ontoConcepts;
	}
	
	public HashSet<String> getLexicalizations(String uri){
		return getObjects(uri, NS+"#lex");
	}

	public String getNS(){
		return NS;
	}
	
	public HashMap<String, String> getMultiWordConcepts(String sentenceText, String superclassURI){
		HashSet<String> foundClasses = getSubclasses(superclassURI);
		HashMap<String, String> foundTargets = new HashMap<>();
		for (String uri : foundClasses) {
			for (String lex : getLexicalizations(uri)){
				//System.out.println("Lexicalization found for: " + subject.getURI());
	//			if (lex.contains(" ")){
	//				System.out.println("Found a multi word concept: " + subject.getURI() + "\t" + sentenceText);
	//			}
				if (lex.contains(" ") && sentenceText.contains(lex)){
					//found one
					foundTargets.put(lex, uri);
	//				System.out.println("Found a multi word concept in this sentence: " + subject.getURI());
				}
			}
		}
		return foundTargets;
	}
	
	
	
	
	/**
	 * Since adding Individuals to the ontology 'on-the-fly' is way too slow to be useful in practice, this method
	 * can be used to pre-instantiate an ontology based on the used dataset, so all individuals can be found already
	 * @param data The Dataset to use
	 */
	public void expandOntology(Dataset data){
		for (Span textualSpan : data.getSpans(data.getTextualUnitSpanType())){
			
			for (Word word : textualSpan){
				
			}
			
		}
	}
	
}
