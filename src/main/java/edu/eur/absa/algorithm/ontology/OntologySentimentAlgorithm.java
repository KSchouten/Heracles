package edu.eur.absa.algorithm.ontology;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeSet;

import org.json.JSONException;

import edu.eur.absa.Framework;
import edu.eur.absa.algorithm.AbstractAlgorithm;
import edu.eur.absa.algorithm.Experiment;
import edu.eur.absa.algorithm.Prediction;
import edu.eur.absa.data.DatasetJSONReader;
import edu.eur.absa.data.DatasetJSONWriter;
import edu.eur.absa.data.SemEval2015Task12ABSAReader;
import edu.eur.absa.evaluation.evaluators.AnnotationLabelEvaluator;
import edu.eur.absa.evaluation.evaluators.Evaluator;
import edu.eur.absa.external.ReasoningOntology;
import edu.eur.absa.model.Dataset;
import edu.eur.absa.model.Relation;
import edu.eur.absa.model.Span;
import edu.eur.absa.model.Word;
import edu.eur.absa.model.exceptions.IllegalSpanException;
import edu.eur.absa.nlp.OntologyLookup;
import weka.core.Attribute;

public class OntologySentimentAlgorithm extends AbstractAlgorithm {

	private ReasoningOntology ont;// = new ReasoningOntology(Framework.EXTERNALDATA_PATH + "RestaurantSentimentExpanded.owl");
	private AbstractAlgorithm backupAlg = null;
	private boolean failureAnalysis = false;
	private HashSet<String> allCategoryURIs; 
	
	

	
	public OntologySentimentAlgorithm(String label,	String unitOfAnalysisSpanType) {
		super(label, unitOfAnalysisSpanType);
//		evaluators.add(new AnnotationLabelEvaluator("opinion","polarity",false,false));
		evaluators.add(new AnnotationLabelEvaluator("opinion","polarity",false,true));
		
		
	}

	@Override
	protected void cleanAlgorithm() {
		// TODO Auto-generated method stub

	}

	public void setDataSubSets(ArrayList<HashSet<Span>> dataSubSets, Dataset dataset){
		super.setDataSubSets(dataSubSets, dataset);
		if (hasProperty("use_bow_backup")){
			
			if (backupAlg==null){
				backupAlg = new AspectSentimentSVMAlgorithm("review",false)
						.setBinaryProperties("use_stanford_sentence_sentiment", 
								"use_review","predict_neutral", "use_category",
								"use_hyperparameter_optimization","Xignore_validation_data")
						//.setProperty("ont", getProperty("ont"))
						;
				;
				Framework.log("Using BOW backup algorithm: "+backupAlg.getLabel());		
			}
			backupAlg.setDataSubSets(dataSubSets, dataset);
			
		}
		if (hasProperty("ont")){
			ont = ReasoningOntology.getOntology(Framework.EXTERNALDATA_PATH + getProperty("ont"));
			allCategoryURIs = ont.getLexicalizedConcepts(ont.URI_Mention, ont.getNS()+"#aspect", null);
			//Framework.log("Categories: "+allCategoryURIs);
		}
	}
	
	
	@Override
	public void preprocess() {
		if (hasProperty("use_bow_backup")){
			backupAlg.preprocess();
		}
		
	}

	@Override
	public void train() {
		if (hasProperty("use_bow_backup")){
			backupAlg.train();
		}
		
	}

	@Override
	public void predict(){
		Dataset dataset;
		
		if (hasProperty("use_bow_backup"))
			backupAlg.predict();
		
		for (Span review : getTestData()){
			predictForReview(review);
//			testData(review);
		}
		ont.save(getProperty("ont")+"-Expanded.owl", true);
		
		
	}
	
	public void testData(Span review) {
		TreeSet<Span> opinionsForReview = review.getDataset().getSpans(review, "opinion");
		for (Span opinion : opinionsForReview){
			Framework.log(opinion.toString());
			Span sentence = getSentence(opinion);
			
			Prediction p = new Prediction(opinion);
			p.putAnnotation("polarity", "positive");
			String cat = "No sentiment";
			
			p.putAnnotation("group", cat);
			
			this.predictions.put(opinion, p.getSingletonSet());
		}
	}
	
	public void predictForReview(Span review){
		Framework.log(System.currentTimeMillis()+"\tStart next review");
		TreeSet<Span> opinionsForReview = review.getDataset().getSpans(review, "opinion");
		Framework.log("Need to assign sentiment to "+opinionsForReview.size()+" opinions in this review");
		for (Span opinion : opinionsForReview){
			HashMap<String, Double> foundURIs = findURIs(opinion, opinionsForReview, ont);
			//looped over all words
			
			boolean assignedPred = false;
			boolean foundPos = foundURIs.containsKey(ont.URI_Positive);
			boolean foundNeg = foundURIs.containsKey(ont.URI_Negative);
			String prediction = "positive";
			if (foundNeg && !foundPos){
				prediction = "negative";
				assignedPred = true;
			}
			if (!foundNeg && foundPos){
				prediction = "positive";
				assignedPred = true;
			}
			
			if (foundNeg && foundPos){
				if (foundURIs.get(ont.URI_Positive) < foundURIs.get(ont.URI_Negative)){
					prediction = "negative";
					assignedPred = true;
				} else if (foundURIs.get(ont.URI_Positive) > foundURIs.get(ont.URI_Negative)){
					prediction = "positive";
					assignedPred = true;
				}
			}
			
			if (hasProperty("use_only_bow") || (hasProperty("use_bow_backup") && !assignedPred)){
				Prediction backupPred = backupAlg.getPrediction(opinion).iterator().next();
				prediction = backupPred.getAnnotation("polarity");
				//Framework.log("===\nUsing BOW backup to predict: " + prediction);
			}
			
			
			if (failureAnalysis && !opinion.getAnnotation("polarity").equals(prediction) &&
					!opinion.getAnnotation("polarity").equals("neutral")){
				Framework.log("===");
				Framework.log(opinion.toString());
				Framework.log(getSentence(opinion).getAnnotation("text"));
	//			for (Word w : sentence){
	//				Framework.log(w.getLemma());
	//			}
				Framework.log("===");
				Framework.log("Found URIs: "+foundURIs);
				Framework.log("Found positive: " + foundPos);
				Framework.log("Found negative: " + foundNeg);
				Framework.log("Gold: "+opinion.getAnnotation("polarity"));
				Framework.log("Predicted: "+prediction);
			}
			
			Prediction p = new Prediction(opinion);
			p.putAnnotation("polarity", prediction);
			String cat = "No sentiment";
			if (foundPos && foundNeg)
				cat="Positive and negative";
			if (foundPos && !foundNeg)
				cat="Only positive";
			if (!foundPos && foundNeg)
				cat="Only negative";
			
			
			p.putAnnotation("group", cat);
			
			this.predictions.put(opinion, p.getSingletonSet());
		}
	}
	
	/**
	 * Find ontology concepts in this review, including their superclasses
	 * @param opinion
	 * @param opinionsForReview
	 * @param ont
	 * @return
	 */
	public HashMap<String, Double> findURIs(Span opinion, TreeSet<Span> opinionsForReview, ReasoningOntology ont){
		Framework.log(System.currentTimeMillis()+"\tStart findURIs() for next opinion"); 
		if (ont == null){
			Framework.error("Ontology is null!");
		}
		if (allCategoryURIs == null || allCategoryURIs.isEmpty()){
			allCategoryURIs = ont.getLexicalizedConcepts(ont.URI_Mention, ont.getNS()+"#aspect", null);
		}
		Dataset dataset = opinion.getDataset();
		Span sentence = getSentence(opinion);
		TreeSet<Span> opinionsPerSentence = sentence.getCoveredSpans(opinionsForReview);
		TreeSet<Word> scope = getScope(opinion, sentence, opinionsPerSentence.size());
		String category = opinion.getAnnotation("category", String.class);
		HashMap<String, Double> foundURIs = new HashMap<>();
		Framework.log(System.currentTimeMillis()+"\tScope contains "+scope.size()+" words. Start loop.");
		for (Word word : scope){
			Framework.log(System.currentTimeMillis()+"\tProcess next word in scope: "+word.getLemma());
			double wordDistance = 1;
//			if (word.getOrder() >= opinion.first().getOrder() && word.getOrder() <= opinion.last().getOrder()){
//				wordDistance = 1;
//			} else {
//				wordDistance = 1.0 / Math.min(Math.abs(opinion.first().getOrder() - word.getOrder()), Math.abs(opinion.last().getOrder() - word.getOrder()));
//			}
			
			if (word.hasAnnotation("URI")){
				String URI = word.getAnnotation("URI");
				Framework.log("Word has a URI: "+URI);
				HashSet<String> superclasses = new HashSet<String>();
				//get all superclasses of this word
				superclasses.addAll(ont.getSuperclasses(URI));
				Framework.log(System.currentTimeMillis()+"\tRetrieved superclasses");
				
				//get all URIs of concepts directly associated with the aspect category of the current opinion
				HashSet<String> categoryURIs = ont.getLexicalizedConcepts(ont.URI_Mention, ont.getNS()+"#aspect", category); 
				Framework.log(System.currentTimeMillis()+"\tRetrieved categoryURIs ");
				
				//for entities, only keep superclasses that are also a category concept
				// before (without the if), generic properties were always filtered out because
				// they do not belong to any category and thus superclasses would always be empty
//				if (superclasses.contains(ont.URI_EntityMention))
//					superclasses.retainAll(categoryURIs);
				
				//check if any of the superclasses contains a category concept
				// if so, at least one of these category concepts should match the category of the opinion
				boolean categoryFilter = true;
				boolean categorySpecific = false;
				superclasses.retainAll(allCategoryURIs);
				if (!superclasses.isEmpty()){
					superclasses.retainAll(categoryURIs);
					categoryFilter = !superclasses.isEmpty();
					categorySpecific = categoryFilter;
				}
				
				if (categorySpecific){
					wordDistance = 2;
				}
				
				//TODO: Give priority to category specific sentiment words when there are more of them
				//TODO: negators like 'too' always make things negative
				//TODO: use a distance discount for positive/negative concepts for explicit targets
				//TODO: filter on POS before matching with ontology?
				if (categoryFilter){ 
//						&& (!span.hasAnnotation("target") || sentence.contains(word))){
//						&& (sentence.contains(word))){
					Framework.log(System.currentTimeMillis()+"\tCategory filter is TRUE");
					
					HashSet<Word> relatedWords = new HashSet<>();
					boolean negate = detectNegationAndFindRelatedWords(word, relatedWords);
					HashSet<String> thisWordSuperclasses = ont.getSuperclasses(URI);
					
					Framework.log(System.currentTimeMillis()+"\tLoop over "+thisWordSuperclasses.size()+ " superclasses of current word");
					
					for (String classURI : thisWordSuperclasses){
						if (negate && false){
							Framework.log(System.currentTimeMillis()+"\tPerform negation");
							//get disjoint class as antonym
							HashSet<String> antonymURIs = ont.getConceptRelations(classURI).get("http://www.w3.org/2002/07/owl#disjointWith");
							if (antonymURIs != null){
																	
								String antonymURI = antonymURIs.iterator().next();
								foundURIs.put(antonymURI, Math.max(wordDistance, foundURIs.getOrDefault(antonymURI, 0.0)));
								
							} else {
								foundURIs.put(classURI, Math.max(wordDistance, foundURIs.getOrDefault(classURI, 0.0)));
							}
						} else {
							foundURIs.put(classURI, Math.max(wordDistance, foundURIs.getOrDefault(classURI, 0.0)));
						}
					}
					
					Framework.log(System.currentTimeMillis()+"\tLoop over "+relatedWords.size()+ " related words");
					
					boolean foundRelation = false;
					for (Word relWord : relatedWords){
						
						if (relWord.hasAnnotation("URI")){
							
							HashSet<String> classes = ont.getSuperclasses(relWord.getAnnotation("URI"));
							if (thisWordSuperclasses.contains(ont.URI_PropertyMention) &&
									classes.contains(ont.URI_EntityMention) &&
									!thisWordSuperclasses.contains(ont.URI_Sentiment) &&
									!classes.contains(ont.URI_Sentiment)){
								Framework.log(System.currentTimeMillis()+"\tRelated word has a URI of proper type:"+ relWord.getAnnotation("URI"));
								
								foundRelation = true;
								//Main.debug("Onto Dep-Bigram:" + word.getWord() + " " + relWord.getWord());
								boolean word2Negated = detectNegationAndFindRelatedWords(relWord, new HashSet<Word>());
								String word1 = word.getWord().substring(0, 1).toUpperCase()+word.getWord().substring(1);
								String word2 = relWord.getAnnotation("lemma");
								word2 = word2.substring(0,1).toUpperCase()+word2.substring(1);
								
								String newClassURI = ont.addClass(word1 + " " + word2, URI, relWord.getAnnotation("URI"));
								Framework.debug(newClassURI);
								for (String superclassURI : ont.getSuperclasses(newClassURI)){
									if (negate || word2Negated){
										//get disjoint class as antonym
										HashSet<String> antonymURIs = ont.getConceptRelations(superclassURI).get("http://www.w3.org/2002/07/owl#disjointWith");
										if (antonymURIs != null){
																				
											String antonymURI = antonymURIs.iterator().next();
											foundURIs.put(antonymURI, Math.max(wordDistance, foundURIs.getOrDefault(antonymURI, 0.0)));
										} else {
											foundURIs.put(superclassURI, Math.max(wordDistance, foundURIs.getOrDefault(superclassURI, 0.0)));
										}
									} else {
										foundURIs.put(superclassURI, Math.max(wordDistance, foundURIs.getOrDefault(superclassURI, 0.0)));
										
									}
									
									Framework.debug("\t"+superclassURI);
								}
//									ont.save("RestaurantSentimentExpanded.owl");
							}
						}
					}
					
//					if (!foundRelation && !thisWordSuperclasses.contains(ont.URI_Sentiment)){
//						for (String categoryURI : categoryURIs){
//							
//						}
//					}
					
				}
			}
			
		}
		
		return foundURIs;
		
	}
	
	
	private boolean detectNegationAndFindRelatedWords(Word word, HashSet<Word> relatedWords){
		boolean negate = false;
		for (Relation rel : word.getRelations().getRelationsToChildren("deps")){
			if (rel.getChild() instanceof Word){
				Word child = (Word)rel.getChild();
				relatedWords.add(child);
				
			}
			if (rel.getAnnotation("relationShortName").equals("neg")){
				negate = true;
			}
			
		}
		for (Relation rel : word.getRelations().getRelationsToParents("deps")){
			if (rel.getParent() instanceof Word)
				relatedWords.add((Word)rel.getParent());
		}
		
		if (!negate){
			//check for negation in preceding words
			int maxLookingBack = 3;
			Word currentWord = word;
			while (maxLookingBack > 0 && currentWord.hasPreviousWord()){
				currentWord = currentWord.getPreviousWord();
				maxLookingBack--;
				if (currentWord.getLemma().equalsIgnoreCase("not") || 
						currentWord.getLemma().equalsIgnoreCase("never")){
					negate = true;
				}
				
			}
		}
		return negate;
	}
	
	private void executeNegation(String URI, HashMap<String, Double> foundURIs){
		for (String classURI : ont.getSuperclasses(URI)){
			//instanceValues.put(listOfAttributes.get(classURI), -1.0);
			
			//get disjoint class as antonym
			HashSet<String> antonymURIs = ont.getConceptRelations(classURI).get("http://www.w3.org/2002/07/owl#disjointWith");
			if (antonymURIs != null){
				//there exists an antonym
				foundURIs.remove(classURI);
				
				String antonymURI = antonymURIs.iterator().next();
				foundURIs.put(antonymURI, 1.0);
				
			}
			
		}
	}
	
	private TreeSet<Word> getScope(Span opinion, Span sentence, int opinionsPerSentence){
		TreeSet<Word> scope;
		if (hasProperty("use_sentence")){
			scope = sentence.getWords();
//			scope = span.getDataset().getSpans("sentence", span.first()).first();
//				scope = span.getTextualUnit();
		} else if (hasProperty("use_review")){
			scope = opinion.getTextualUnit().getWords();
		} else if (hasProperty("use_word_window")) {
			if (opinion.hasAnnotation("target") && opinionsPerSentence > 1){
				scope = new TreeSet<Word>();
				scope.addAll(opinion.getWords());
				int window = 4;
				Word firstWord = opinion.first();
				Word lastWord = opinion.last();
				while (window > 0){
					boolean addedWord = false;
					if (firstWord.hasPreviousWord()){
						firstWord = firstWord.getPreviousWord();
						if (sentence.contains(firstWord)){
							scope.add(firstWord);
							addedWord = true;
						}
					}
					if (lastWord.hasNextWord()){
						lastWord = lastWord.getNextWord();
						if (sentence.contains(firstWord)){
							scope.add(lastWord);
							addedWord = true;
						}
					}
					if (addedWord){
						window--;
					} else {
						window = 0;
					}
				}
			} else {
				scope =sentence.getWords();
			}
		} else if (hasProperty("use_dep_window")){
			scope = getDepWindow(opinion.getWords(),4);
		} else {
			scope = opinion.getWords();
		}
		return scope;
	}
	
	public Span getSentence(Span opinionSpan){
//		TreeSet<Span> sentences = opinionSpan.getDataset().getSpans("sentence", opinionSpan.first());
//		if (sentences.isEmpty()){
//			Word w = opinionSpan.first();
////			//Framework.log("Span: "+opinionSpan.getDataset().getSpans(w));
////			for (Span sentence : opinionSpan.getDataset().getSpans(opinionSpan.getTextualUnit(), "sentence")){
////				Framework.log("words:"+sentence.getWords());
////				Framework.log("Contains word? "+sentence.contains(opinionSpan.first()));
////				Framework.log("Contains word? "+sentence.getWords().contains(opinionSpan.first()));
////				Framework.log("Contains word? "+((TreeSet<Word>)sentence.getWords().clone()).contains(opinionSpan.first()));
////			}
//			Framework.log(w.toString() +"\t" + w.getTextualUnit().showAnnotations() );
//		}
//		return sentences.first();
		
		TreeSet<Span> sentences = new TreeSet<Span>();
		sentences.addAll(opinionSpan.getDataset().getSpans("sentence", opinionSpan.first()));
		sentences.addAll(opinionSpan.getDataset().getSpans("sentence", opinionSpan.last()));
		if (sentences.isEmpty()) {
			Framework.log("No sentences?");
			Framework.log("Content opinionSpan: "+opinionSpan.size());
			Framework.log("Textual unit: "+opinionSpan.getTextualUnit().toString());
			TreeSet<Span> sentenceList = opinionSpan.getDataset().getSpans(
					opinionSpan.getTextualUnit(), "sentence");
			for (Span sentence : sentenceList) {
				Framework.log(sentence.contains(opinionSpan.first()) + "\t" + sentence.contains(opinionSpan.last()) + "\t" + sentence.getWords());
			}
		}
		return sentences.first();
	}
	
	public TreeSet<Word> getDepWindow(TreeSet<Word> span, int distance){
		TreeSet<Word> scope = new TreeSet<Word>();
		scope.addAll(span);
		for (Word word : span){
			for (Relation rel : word.getRelations().getRelationsToChildren("deps")){
				if (rel.getChild() instanceof Word)
					scope.add((Word)rel.getChild());
			}
			for (Relation rel : word.getRelations().getRelationsToParents("deps")){
				if (rel.getParent() instanceof Word)
					scope.add((Word)rel.getParent());
			}
		}
		if (distance > 0){
			return getDepWindow(scope, distance-1);
		} else {
			return scope;
		}
	}
	
	//old predict
	// returns a list of related words to the target, uses the ontology to filter words
	public void oldPredict() {
		Dataset dataset;
		
		HashSet<String> allCategoryURIs = ont.getLexicalizedConcepts(ont.URI_Mention, ont.getNS()+"#aspect", null); 
		
		for (Span review : getTestData()){
			
			for (Span opinion : review.getDataset().getSpans(review, "opinion")){
				dataset = opinion.getDataset();
				
				Span sentence = dataset.getSpans("sentence", opinion.first()).first();
				boolean explicitAspect = (sentence.size() > opinion.size());
				if (failureAnalysis){
					Framework.log("===");
					Framework.log(opinion.toString());
					Framework.log(sentence.getAnnotation("text"));
		//			for (Word w : sentence){
		//				Framework.log(w.getLemma());
		//			}
					Framework.log("===");
				}
				String category = opinion.getAnnotation("category");
				
	//			checkRelations(sentence);
				
				for (Word word : opinion.descendingSet()){
					boolean match = explicitAspect;
					if (word.hasAnnotation("URI")){
						//found ontology concept
						String URI = word.getAnnotation("URI");
						if (failureAnalysis)
							Framework.log(word.getLemma() + " " + URI);
						
						if (!match)
							match = conceptMatchesCategory(URI, allCategoryURIs, category);
					}
						
					if (match){
						TreeSet<Word> relatedWords = getRelatedWords(word, new TreeSet<Word>());
						TreeSet<Word> filteredRelatedWords = getMatchingRelatedWords(word, new TreeSet<Word>(), allCategoryURIs, category);
								
						
	//						for (Relation rel : word.getRelations().getRelationsToChildren("deps")){
	//							if (rel.getChild() instanceof Word)
	////								checkRelation(rel, true, URI);
	//								relatedWords.add((Word)rel.getChild());
	//						}
	//						for (Relation rel : word.getRelations().getRelationsToParents("deps")){
	//							if (rel.getParent() instanceof Word && rel.getAnnotation("relationShortName").equals("nsubj"))
	////								checkRelation(rel, false, URI);
	//								relatedWords.add((Word)rel.getParent());
	//						}
	//						superclassesURIs = ont.getSuperclasses(URI);
						
						if (failureAnalysis){
							Framework.log("Related words: "+relatedWords);
							Framework.log("Filtered related words: " + filteredRelatedWords);
						}
					}
						
					
				}
				
				
				Prediction p = new Prediction(opinion);
				p.putAnnotation("polarity", "positive");
				this.predictions.put(opinion, p.getSingletonSet());
	
			}
		}

	}
	
	private boolean conceptMatchesCategory(String URI,HashSet<String> allCategoryURIs, String category){
		boolean match = false;
		HashSet<String> superclassesURIs = (HashSet<String>) ont.getSuperclasses(URI).clone();
		HashSet<String> categoryURIs = (HashSet<String>) ont.getLexicalizedConcepts(ont.URI_Mention, ont.getNS()+"#aspect", category).clone(); 
		categoryURIs.retainAll(superclassesURIs);
		superclassesURIs.retainAll(allCategoryURIs);
		if (superclassesURIs.size() > 0){
			//this concept belongs to at least one specific aspect category
			// in that case, the current opinion's aspect category should be one of them
			if (categoryURIs.size() > 0){
				//aspect category is matching
//				Framework.log("Match!");
				match = true;
				
			} else {
//				Framework.log("...");
			}
		} else {
//			Framework.log("Match!");
			match = true;
			
		}
		return match;
	}

	private TreeSet<Word> getMatchingRelatedWords(Word word, TreeSet<Word> relatedWords, HashSet<String> allCategoryURIs, String category){
		for (Relation rel : word.getRelations().getRelationsToChildren("deps")){
			String relType = rel.getAnnotation("relationShortName");
			if (rel.getChild() instanceof Word && !relType.equals("parataxis")){
				Word child = (Word)rel.getChild();
				boolean match = false;
				if (child.hasAnnotation("URI")){
					match = conceptMatchesCategory(child.getAnnotation("URI"), allCategoryURIs, category);
				} else {
					match = true;
				}
				if (match && relatedWords.add(child))
					relatedWords.addAll(getMatchingRelatedWords(child, relatedWords, allCategoryURIs, category));
			}
		}
		for (Relation rel : word.getRelations().getRelationsToParents("deps")){
			String relType = rel.getAnnotation("relationShortName");
			if (rel.getParent() instanceof Word && (relType.equals("nsubj") || relType.startsWith("nmod"))){
				Word parent = (Word)rel.getParent();
				boolean match = false;
				if (parent.hasAnnotation("URI")){
					match = conceptMatchesCategory(parent.getAnnotation("URI"), allCategoryURIs, category);
				} else {
					match = true;
				}
				if (match && relatedWords.add(parent))
					relatedWords.addAll(getMatchingRelatedWords(parent, relatedWords, allCategoryURIs, category));
			}
		}
		return relatedWords;
	}
	
	private TreeSet<Word> getRelatedWords(Word word, TreeSet<Word> relatedWords){
		for (Relation rel : word.getRelations().getRelationsToChildren("deps")){
			String relType = rel.getAnnotation("relationShortName");
			if (rel.getChild() instanceof Word && !relType.equals("parataxis")){
				Word child = (Word)rel.getChild();
				if (relatedWords.add(child))
					relatedWords.addAll(getRelatedWords(child, relatedWords));
			}
		}
		for (Relation rel : word.getRelations().getRelationsToParents("deps")){
			String relType = rel.getAnnotation("relationShortName");
			if (rel.getParent() instanceof Word && relType.equals("nsubj")){
				Word parent = (Word)rel.getParent();
				if (relatedWords.add(parent))
					relatedWords.addAll(getRelatedWords(parent, relatedWords));
			}
		}
		return relatedWords;
	}
	
	/**
	 * If parent is true, then URI is the parentURI, and the relation relates it to a child
	 * If parent is false, then URI is the childURI, and the relation relates it to a parent
	 * @param rel
	 * @param parent
	 * @param URI
	 */
	private Word checkRelation(Relation rel, boolean parent, String URI){
		Word relWord;
		if (parent){
			relWord = (Word)rel.getChild();
		} else {
			relWord = (Word)rel.getParent();
		}
//		if (relWord.hasAnnotation("URI")){
//			String relURI = relWord.getAnnotation("URI");
//			Framework.log("\t"+rel.getAnnotation("relationShortName") + "\t" + relWord.getLemma()+ " " + relURI + "\t" + ont.getSuperclasses(relURI));
//			
//			if (parent && rel.getAnnotation("relationShortName").equals("amod")){
//				//create combo
//				
//			}
//			
//		} else {
//			Framework.log("\t"+relWord.getLemma());
//		}
		
		if (parent || rel.getAnnotation("relationShortName").equals("nsubj")){
			return relWord;
		} else { 
			return null;
		}
		
		
	}
	
	private void checkRelations(Span sentenceSpan){
		Word root = (Word)sentenceSpan.getRelations().getRelationsToChildren("deps").first().getChild();
		Framework.log("ROOT: "+root.getWord());
		getChildren(root, new TreeSet<Word>());
		
		
//		
//		Framework.log("Relations: "+sentenceSpan.getRelations().getAllRelationsToChildren());
//		Framework.log("Relations: "+sentenceSpan.getRelations().getAllRelationsToParents());
	}
	
	private int getChildren(Word parent, TreeSet<Word> visitedWords){
		//to avoid cycles in the dependency graph
		if (!visitedWords.add(parent)){
			return 0;
		}
		int children = 0;
		TreeSet<Relation> relationsToChildren = parent.getRelations().getRelationsToChildren("deps");
		TreeSet<Word> directChildren = new TreeSet<>();
		for (Relation relToChild : relationsToChildren){
			String depName = relToChild.getAnnotation("relationShortName");
			if (!depName.equals("cc") && !depName.equals("conj")){
				Word child = (Word) relToChild.getChild();
				directChildren.add(child);
				children += 1 + getChildren(child, visitedWords);
			}
		}
		if (children == directChildren.size() && children > 0){
			//then this is pre-terminal node (all its children are leafs
			String phrase = "";
			for (Word child : directChildren){
				phrase += " " +child.getWord();
			}
			phrase += " " + parent.getWord();
			//Framework.log(phrase);
		}
		return children;
	}
	
	
}
