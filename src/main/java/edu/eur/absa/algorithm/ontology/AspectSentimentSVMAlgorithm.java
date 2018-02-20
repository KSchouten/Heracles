package edu.eur.absa.algorithm.ontology;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeSet;

import weka.attributeSelection.ASEvaluation;
import weka.attributeSelection.ASSearch;
import weka.attributeSelection.AttributeSetEvaluator;
import weka.attributeSelection.BestFirst;
import weka.attributeSelection.CfsSubsetEval;
import weka.attributeSelection.InfoGainAttributeEval;
import weka.attributeSelection.PrincipalComponents;
import weka.classifiers.Evaluation;
import weka.classifiers.evaluation.NominalPrediction;
import weka.classifiers.functions.SMO;
import weka.classifiers.functions.SMOreg;
import weka.classifiers.functions.supportVector.RBFKernel;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SelectedTag;
import weka.core.SparseInstance;
import weka.core.converters.CSVLoader;
import weka.filters.supervised.attribute.AttributeSelection;
import edu.eur.absa.Framework;
import edu.eur.absa.algorithm.AbstractAlgorithm;
import edu.eur.absa.algorithm.Prediction;
import edu.eur.absa.evaluation.evaluators.AnnotationLabelEvaluator;
import edu.eur.absa.evaluation.evaluators.AnnotationValueEvaluator;
import edu.eur.absa.external.NRCReviewSentimentLexicon;
import edu.eur.absa.external.ReasoningOntology;
import edu.eur.absa.model.DataEntity;
import edu.eur.absa.model.Dataset;
import edu.eur.absa.model.Relation;
import edu.eur.absa.model.Span;
import edu.eur.absa.model.Word;

public class AspectSentimentSVMAlgorithm extends AbstractAlgorithm {

	private Instances allWekaData;
	private HashMap<Span, Instance> wekaInstances = new HashMap<>();
	private SMO model;
	private AttributeSelection featureSelector;
	private Evaluation eval;
//	private StockMarketLexicon sentLex = StockMarketLexicon.getInstance();
//	private FinanceOntology ont =new FinanceOntology(Main.EXTERNALDATA_PATH + "finance.owl");
	private ReasoningOntology ont; 
//	private ReasoningOntology ont =new ReasoningOntology(Main.EXTERNALDATA_PATH + "RestaurantSentimentExpanded.owl");
	
	
	private NRCReviewSentimentLexicon revSentUnigrams;
	private NRCReviewSentimentLexicon revSentBigrams;
	
	public static final int RESTAURANTS = 0;
	public static final int LAPTOPS = 1;
	
	private int indivId = 0;
	private ArrayList<String> classLabels = new ArrayList<String>();
	
	private OntologySentimentAlgorithm ontAlg = new OntologySentimentAlgorithm("OntologySentimentAlgorithm","review");
	
	public AspectSentimentSVMAlgorithm(String analysisSpan, boolean failureAnalysis) {
		this(analysisSpan, failureAnalysis, RESTAURANTS);
		
		
	}
	
	public AspectSentimentSVMAlgorithm(String analysisSpan, boolean failureAnalysis, int reviewType) {
		this("AspectSentimentSVMAlgorithm",analysisSpan, failureAnalysis, reviewType);
	
	}
	
	public AspectSentimentSVMAlgorithm(String label, String analysisSpan, boolean failureAnalysis, int reviewType) {
		super(label,analysisSpan);
		evaluators.add(new AnnotationLabelEvaluator("opinion","polarity",failureAnalysis));
		
		if (reviewType == RESTAURANTS){
			revSentUnigrams = new NRCReviewSentimentLexicon(NRCReviewSentimentLexicon.RESTAURANTS_UNIGRAM);
			revSentBigrams = new NRCReviewSentimentLexicon(NRCReviewSentimentLexicon.RESTAURANTS_BIGRAM);
		}
		if (reviewType == LAPTOPS){
			revSentUnigrams = new NRCReviewSentimentLexicon(NRCReviewSentimentLexicon.LAPTOPS_UNIGRAM);
			revSentBigrams = new NRCReviewSentimentLexicon(NRCReviewSentimentLexicon.LAPTOPS_BIGRAM);
		}
		
	}
	
	protected void setDataSubsets(ArrayList<HashSet<Span>> dataSubSets, Dataset dataset){
		super.setDataSubSets(dataSubSets, dataset);
		ontAlg.setBinaryProperties("use_sentence");
		if (hasProperty("ont")){
			ontAlg.setProperty("ont", getProperty("ont"));
		} else {
			ontAlg.setProperty("ont", "RestaurantSentimentExpanded.owl");
		}
		ontAlg.setProperty("ont_ns", "http://www.kimschouten.com/sentiment/restaurant");
		ontAlg.setDataSubSets(dataSubSets, dataset);
	}

	@Override
	protected void cleanAlgorithm() {
		Framework.log(getLabel()+" - Cleaning");
		model = null;
		eval = null;
		featureSelector = null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void preprocess() {
		Framework.log(getLabel() + " - Preprocessing ...");
		
		if (hasProperty("use_ontology")){
//			 ont =new ReasoningOntology(Main.EXTERNALDATA_PATH + "RestaurantSentiment.owl");
//			 ont =new ReasoningOntology(Framework.EXTERNALDATA_PATH + "RestaurantSentimentExpanded.owl");
			ont = new ReasoningOntology(Framework.EXTERNALDATA_PATH + getProperty("ont"));
		}
		
		//collect all attributes and their values for all the spans
		HashMap<String,Attribute> listOfAttributes = new HashMap<>();
		HashMap<Span,HashMap<Attribute, Double>> allInstanceValues = new HashMap<>();
		HashMap<Span,String> targetValues = new HashMap<>();
		//add span level features
		if (hasProperty("use_stanford_sentence_sentiment"))
			listOfAttributes.put("stanfordSentiment", new Attribute("stanfordSentiment"));
		if (hasProperty("use_stanford_opinion_sentiment"))
			listOfAttributes.put("stanfordOpinionSentiment", new Attribute("stanfordOpinionSentiment"));		
		String[] wordFeatureArray = new String[0];
		if (hasProperty("use_word_annotations")){
			wordFeatureArray = getProperty("use_word_annotations").split(" ");
			if (wordFeatureArray.length == 1 && wordFeatureArray[0].equals(""))
				wordFeatureArray = new String[]{};
		} else {
			wordFeatureArray = new String[]{"lemma"};
		}
			

		
		
		
		for (Span reviewSpan : getCombinedData()){
//			this.extractStatements(span);
			TreeSet<Span> opinionsForReview = reviewSpan.getDataset().getSpans(reviewSpan, "opinion");
			
			for (Span span : opinionsForReview){
				//Framework.log("Opinion span: "+span.toString());
				HashMap<Attribute, Double> instanceValues = new HashMap<>();
				allInstanceValues.put(span,instanceValues);
				if (span.hasAnnotation("polarity"))
					targetValues.put(span, span.getAnnotation("polarity", String.class));
	
				
				
				TreeSet<Word> scope;
				Span sentence = null;
				try {
				sentence = getSentence(span);
				} catch (Exception e) {
					e.printStackTrace();
					Framework.log(span.toString());
					Framework.log(span.first().getId() + "\t" + span.last().getId());
					for (Word w: span) {
						Framework.log(w.toString() + "\t" + w.getId());
					}
					Framework.log(dataset.getSpans(span.first()).toString());
					TreeSet<Span> sentences = dataset.getSpans(span.getTextualUnit(), "sentence"); 
					for (Span s : sentences) {
						Framework.log(""+s.contains(span.first()));
						Framework.log(""+s.contains(span.last()));
						Framework.log(s.toString());
						
						for (Word w : s) {
							Framework.log(w.toString() + "\t" + w.getId());
						}
					}
				}
				TreeSet<Span> opinionsPerSentence = sentence.getCoveredSpans(opinionsForReview);
				
				if (hasProperty("use_sentence")){
					scope = sentence.getWords();
//					scope = span.getDataset().getSpans("sentence", span.first()).first();
	//				scope = span.getTextualUnit();
				} else if (hasProperty("use_review")){
					scope = span.getTextualUnit().getWords();
				} else if (hasProperty("use_word_window")) {
					if (span.hasAnnotation("target") && opinionsPerSentence.size() > 1){
						scope = new TreeSet<Word>();
						scope.addAll(span.getWords());
						int window = 4;
						Word firstWord = span.first();
						Word lastWord = span.last();
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
					scope = getDepWindow(span.getWords(),6);
				} else {
					scope = span.getWords();
				}
				
				
				
				
	//			if (hasProperty("use_dep_flow")){
	//				instanceValues.put(listOfAttributes.get("depFlowCompanySentiment"), companyRelations(source));
	//			}
				String category = span.getAnnotation("category", String.class);
				if (category == null){
					Framework.error(this.label + "\tCategory is null!");
					Framework.error(span.toString());
					for (Word w : span){
						Framework.error(w.toString());
					}
//					System.exit(-1);
				}
				
				if (hasProperty("use_category")){
					
					listOfAttributes.putIfAbsent(category, new Attribute(category));
					instanceValues.put(listOfAttributes.get(category), 1.0);
				}
				
				
				HashSet<String> featureURIs = new HashSet<String>();
				for (Word word : scope){
					//Annotations ann = word.getAnnotations();
					
					for (String attr : wordFeatureArray){
						if (attr.equals("lemma") && hasProperty("use_ner") 
								&& word.hasAnnotation("nerLabel") 
								&& !word.getAnnotation("nerLabel",String.class).equalsIgnoreCase("O")){
							//if using NER labels instead of lemmas for NEs
							if (!listOfAttributes.containsKey(word.getAnnotationEntryText("nerLabel"))){
								Framework.debug(word.getAnnotationEntryText("nerLabel"));
								listOfAttributes.put(word.getAnnotationEntryText("nerLabel"), new Attribute(word.getAnnotationEntryText("nerLabel")));
							}
							
							
						} else {
							//if not using NER, or the NER label is O (so no NE) then use the lemma
							//lemma but not a ner or we don't use ners
							String attribute = word.getAnnotationEntryText(attr);
							
							
							if (!listOfAttributes.containsKey(attribute)){
	//							Main.debug(ann.getEntryText(attr));
								listOfAttributes.put(attribute, new Attribute(attribute));
							}
							
	//						
	//						if (!listOfAttributes.containsKey(ann.getEntryText(attr))){
	////							Main.debug(ann.getEntryText(attr));
	//							listOfAttributes.put(ann.getEntryText(attr), new Attribute(ann.getEntryText(attr)));
	//						}		
						}
						
	//					instanceValues.put(listOfAttributes.get(ann.getEntryText(attr)), 1.0);
						
						double instanceValue = 0.0;
						double dicts = 0.0;
						if (hasProperty("use_stanford_sentiment")){
							double sentiment = getSentimentScore(word.getAnnotation("phraseSentiment", ArrayList.class));
							instanceValue += sentiment;
	//						instanceValues.put(listOfAttributes.get(ann.getEntryText(attr)), sentiment);
							dicts++;
	//						Main.debug(word.getWord() + "\t" + sentiment);
						}
						if (hasProperty("use_nrc_sentiment")){
	//						useFeature("NRC Sentiment: " + word.getLemma(), orderedFeatures, revSentUnigrams.getScore(word.getCorrectedText()));
							double sentimentWeight = revSentUnigrams.getScore(word.getWord());
							instanceValue += sentimentWeight;
							dicts++;
						}
						if (dicts > 0)
							instanceValue /= dicts;
						
						if (!hasProperty("use_nrc_sentiment") && !hasProperty("use_stanford_sentiment")){
							//presence only
							instanceValue = 1.0;
							//frequency
//							instanceValue = instanceValues.getOrDefault(listOfAttributes.get(ann.getEntryText(attr)),0.0)+1.0;
							
						}
						
						
						instanceValues.put(listOfAttributes.get(word.getAnnotationEntryText(attr)), instanceValue);
						
					}
					
//					if (hasProperty("use_ontology") && word.getAnnotations().containsKey("URI")
//							&& sentence.contains(word)){
////							&& (!span.getAnnotations().containsKey("target") || sentence.contains(word))){
//	//					for (String URI : ont.getLexicalizedConcepts(ont.URI_Mention, word.getAnnotations().get("lemma"))){
//	//						listOfAttributes.putIfAbsent(URI, new Attribute(URI));
//	//						instanceValues.put(listOfAttributes.get(URI), 1.0);
//	//					}
//						String URI = word.getAnnotations().get("URI");
//						HashSet<String> superclasses = new HashSet<String>();
//						//get all superclasses of this word
//						superclasses.addAll(ont.getSuperclasses(URI));
//						//get all URIs of concepts directly associated with the aspect category of the current opinion
//						HashSet<String> categoryURIs = ont.getLexicalizedConcepts(ont.URI_Mention, ont.getNS()+"#aspect", category); 
//						
//						//for entities, only keep superclasses that are also a category concept
//						// before (without the if), generic properties were always filtered out because
//						// they do not belong to any category and thus superclasses would always be empty
//						if (superclasses.contains(ont.URI_EntityMention))
//							superclasses.retainAll(categoryURIs);
//						if (!superclasses.isEmpty() 
////								&& (!span.getAnnotations().containsKey("target") || sentence.contains(word))){
//								&& (sentence.contains(word))){
//							featureURIs.addAll(ont.getSuperclasses(URI));
////							for (String classURI : ont.getSuperclasses(URI)){
////								listOfAttributes.putIfAbsent(classURI, new Attribute(classURI));
////								instanceValues.put(listOfAttributes.get(classURI), 1.0);
////							}
//						
//	//					if (word.hasNextWord() && word.getNextWord().getAnnotations().containsKey("URI")){
//	//						HashSet<String> superclasses = ont.getSuperclasses(word.getNextWord().getAnnotations().get("URI"));
//	//						if (ont.getSuperclasses(URI).contains(ont.URI_PropertyMention) &&
//	//								superclasses.contains(ont.URI_EntityMention)){
//	//							//superclasses.addAll(ont.getSuperclasses(URI));
//	//							Main.debug("Onto Bigram:" + word.getWord() + " " + word.getNextWord().getWord());
//	//							String word1 = word.getWord().substring(0, 1).toUpperCase()+word.getWord().substring(1);
//	//							String word2 = word.getNextWord().getAnnotations().get("lemma");
//	//							word2 = word2.substring(0,1).toUpperCase()+word2.substring(1);
//	//							String newClassURI = ont.addClass(word1 + " " + word2, URI, word.getNextWord().getAnnotations().get("URI"));
//	//							Main.debug(newClassURI);
//	//							listOfAttributes.putIfAbsent(newClassURI, new Attribute(newClassURI));
//	//							instanceValues.put(listOfAttributes.get(newClassURI), 1.0);
//	//						}
//	//						
//	//					}
//							boolean negate = false;
//							HashSet<Word> relatedWords = new HashSet<>();
//							for (Relation rel : word.getRelations().getRelationsToChildren("deps")){
//								if (rel.getChild() instanceof Word)
//									relatedWords.add((Word)rel.getChild());
//								if (rel.getAnnotations().get("relationShortName").equals("neg")){
//									negate = true;
//								}
//							}
//							for (Relation rel : word.getRelations().getRelationsToParents("deps")){
//								if (rel.getParent() instanceof Word)
//									relatedWords.add((Word)rel.getParent());
//							}
//							HashSet<String> thisWordSuperclasses = ont.getSuperclasses(word.getAnnotations().get("URI"));
//							
//							if (!negate){
//								//check for negation in preceding words
//								int maxLookingBack = 3;
//								Word currentWord = word;
//								while (maxLookingBack > 0 && currentWord.hasPreviousWord()){
//									currentWord = currentWord.getPreviousWord();
//									maxLookingBack--;
//									if (currentWord.getWord().equals("not") || currentWord.getWord().equals("never")){
//										negate = true;
//									}
//									
//								}
//							}
//							//negation
//							if (negate){
//								for (String classURI : ont.getSuperclasses(URI)){
//									//instanceValues.put(listOfAttributes.get(classURI), -1.0);
//									
//									//get disjoint class as antonym
//									HashSet<String> antonymURIs = ont.getConceptRelations(classURI).get("http://www.w3.org/2002/07/owl#disjointWith");
//									if (antonymURIs != null){
//										//there exists an antonym
////										instanceValues.remove(listOfAttributes.get(classURI));
//										featureURIs.remove(listOfAttributes.get(classURI));
//										String antonymURI = antonymURIs.iterator().next();
//										featureURIs.add(antonymURI);
////										listOfAttributes.putIfAbsent(antonymURI, new Attribute(antonymURI));
////										instanceValues.put(listOfAttributes.get(antonymURI), 1.0);
//									}
//									
//								}
//							}
//							
//							for (Word relWord : relatedWords){
//								
//								if (relWord.getAnnotations().containsKey("URI")){
//									HashSet<String> classes = ont.getSuperclasses(relWord.getAnnotations().get("URI"));
//									if (thisWordSuperclasses.contains(ont.URI_PropertyMention) &&
//											classes.contains(ont.URI_EntityMention) && 
//											!thisWordSuperclasses.contains(ont.URI_Sentiment) &&
//											!classes.contains(ont.URI_Sentiment)){
//										//Main.debug("Onto Dep-Bigram:" + word.getWord() + " " + relWord.getWord());
//										String word1 = word.getWord().substring(0, 1).toUpperCase()+word.getWord().substring(1);
//										String word2 = relWord.getAnnotations().get("lemma");
//										word2 = word2.substring(0,1).toUpperCase()+word2.substring(1);
//										
//										String newClassURI = ont.addClass(word1 + " " + word2, URI, relWord.getAnnotations().get("URI"));
////										Framework.debug(newClassURI);
//										featureURIs.addAll(ont.getSuperclasses(newClassURI));
////										for (String superclassURI : ont.getSuperclasses(newClassURI)){
////											listOfAttributes.putIfAbsent(superclassURI, new Attribute(superclassURI));
////											instanceValues.put(listOfAttributes.get(superclassURI), 1.0);
////										}
//	//									ont.save("RestaurantSentimentExpanded.owl");
//									}
//								}
//							}
//						} else {
//							//this word has an ontology concept but is not linked to the current aspect category
//							//let's try removing those words from the feature set (we are already not including
//							//those ontology concepts)
//							for (String attr : wordFeatureArray){
//								String attribute = ann.getEntryText(attr);
//								instanceValues.remove(listOfAttributes.get(attribute));
//							}
//							
//							
//						}
						
						
//					}
					
					
					if (hasProperty("use_bigrams")){
						for (Word w : span){
							if (w.hasPreviousWord()){
								String attribute = w.getPreviousWord().getAnnotation("lemma") + " " + 
									w.getAnnotation("lemma");
								if (!listOfAttributes.containsKey(attribute)){
									listOfAttributes.put(attribute, new Attribute(attribute));
								}
								
								instanceValues.put(listOfAttributes.get(attribute), 1.0);
							}
						}
					}
					
					
				}
				
				
			
				//only add ontology features when we have either Positive or Negative (not both)
				if (hasProperty("use_ontology")){
					featureURIs.addAll(ontAlg.findURIs(span, opinionsForReview, ont).keySet());
//					if (featureURIs.contains(ont.URI_Positive) == featureURIs.contains(ont.URI_Negative)){
//						featureURIs.remove(ont.URI_Negative);
//						featureURIs.remove(ont.URI_Positive);
//					}
//					for (String featureURI : featureURIs){
//						listOfAttributes.putIfAbsent(featureURI, new Attribute(featureURI));
//						instanceValues.put(listOfAttributes.get(featureURI), 1.0);
//					}
					
					//only use positive/negative from ontology, not rest of classes
					if (featureURIs.contains(ont.URI_Positive) && !featureURIs.contains(ont.URI_Negative)){
						listOfAttributes.putIfAbsent(ont.URI_Positive, new Attribute(ont.URI_Positive));
						instanceValues.put(listOfAttributes.get(ont.URI_Positive), 1.0);
					}
					if (featureURIs.contains(ont.URI_Negative) && !featureURIs.contains(ont.URI_Positive)){
						listOfAttributes.putIfAbsent(ont.URI_Negative, new Attribute(ont.URI_Negative));
						instanceValues.put(listOfAttributes.get(ont.URI_Negative), 1.0);
					}
				}
				
				
				
				if (hasProperty("use_stanford_sentence_sentiment")){
					double sentiment = getSentimentScore(sentence.getAnnotation("phraseSentiment", ArrayList.class));
					instanceValues.put(listOfAttributes.get("stanfordSentiment"), sentiment);
				}
				if (hasProperty("use_stanford_opinion_sentiment")){
					double sentiment = getSentimentScore(span.getAnnotation("phraseSentiment", ArrayList.class));
					instanceValues.put(listOfAttributes.get("stanfordOpinionSentiment"), sentiment);
				}
				
	
				
	////			Main.debug(span.getAnnotations().get("text"));
	//			HashMap<String, Double> ontologyFeatures = new HashMap<>();
	//			if (hasProperty("use_ontology_entities") || hasProperty("use_ontology_actions") || hasProperty("use_ontology_properties")){
	//				ontologyFeatures.putAll(getOntologySingleConcepts(span, source, hasProperty("use_ontology_entities"), hasProperty("use_ontology_actions"),hasProperty("use_ontology_properties")));
	//			}
	////			Main.debug(ontologyFeatures.toString());
	//			if (hasProperty("use_ontology_relations")){
	//				ontologyFeatures.putAll(getRelations(span, source));
	//			}
	////			Main.debug(ontologyFeatures.toString());
	//			
	//			for (String uri : ontologyFeatures.keySet()){
	//				if (!listOfAttributes.containsKey("Ont: "+uri)){
	////						Main.debug(ann.getEntryText(attr));
	//					listOfAttributes.put("Ont: "+uri, new Attribute("Ont: "+uri));
	//				}
	//				instanceValues.put(listOfAttributes.get("Ont: "+uri), ontologyFeatures.get(uri));
	//			}
				
				
			}
		}
//		ont.save("RestaurantSentimentExpanded.owl", true);
//		Main.debug(targetValues.values().toString());
		
		ArrayList<Attribute> attributes = new ArrayList<>();
		attributes.addAll(listOfAttributes.values());
		if (classLabels.isEmpty()) {
	//		classLabels.add("missing");
			classLabels.add("positive");
			classLabels.add("negative");
			if (hasProperty("predict_neutral")){
				classLabels.add("neutral");
			}
		}
		Attribute sentimentTarget = new Attribute("polarity",classLabels); 
		attributes.add(sentimentTarget);
		allWekaData = new Instances(label, attributes, 0);
		allWekaData.setClass(sentimentTarget);
		
		for (Span s : allInstanceValues.keySet()){
			HashMap<Attribute, Double> instanceData = allInstanceValues.get(s);
			Instance i = new SparseInstance(attributes.size());
			i.setDataset(allWekaData);
			
			features.put(s, new HashSet<String>());
			for (Attribute att : attributes){
				//if this attribute is present, put its value in there, otherwise, put a 0 (which will not be saved due to the
				// instance being a SparseInstance
//				if (instanceData.containsKey(att))
					i.setValue(att, instanceData.getOrDefault(att, 0.0));
					if (instanceData.containsKey(att))
						features.get(s).add(att.name()+"\t"+instanceData.get(att)+"\n");
				
			}
			
			if (targetValues.containsKey(s)){
				if (!hasProperty("predict_neutral") && targetValues.get(s).equalsIgnoreCase("neutral")){
					i.setClassValue("positive");
				} else {
					i.setClassValue(targetValues.get(s));
				}
					
			}
//			Framework.debug(""+i.toString());
			wekaInstances.put(s, i);
			allWekaData.add(i);
		}
		

		
//		System.exit(0);
	}

	@Override
	public void train() {
		Framework.log(getLabel() + " - Training ...");
		Instances trainingData = new Instances(this.allWekaData, 0);
		Instances partialTrainingData = new Instances(this.allWekaData, 0);
		Instances validationData = new Instances(this.allWekaData, 0);
		ArrayList<Span> trainingSpans = new ArrayList<Span>();
		HashSet<DataEntity> trainingSet = new HashSet<DataEntity>();
		if (hasProperty("use_feature_selection") || hasProperty("use_hyperparameter_optimization")){
			for (Span reviewSpan : this.getTrainingData().get(0)){
				for (Span s : reviewSpan.getDataset().getSpans(reviewSpan, "opinion")){
					partialTrainingData.add(this.wekaInstances.get(s));
//					trainingSpans.add(s);
				}
			}
			for (Span reviewSpan : this.getTrainingData().get(1)){
				for (Span s : reviewSpan.getDataset().getSpans(reviewSpan, "opinion")){
					validationData.add(this.wekaInstances.get(s));
				}
			}
		}
		for (Span reviewSpan : getCombinedTrainingData()){
			for (Span s : reviewSpan.getDataset().getSpans(reviewSpan, "opinion")){
				trainingData.add(this.wekaInstances.get(s));
				trainingSpans.add(s);
			}
		}
		
		trainingSet.addAll(trainingSpans);
		

		
		
		try {
			if (hasProperty("use_feature_selection")){
				//not sure if this works
				featureSelector = new AttributeSelection();
				featureSelector.setInputFormat(validationData);
				ASEvaluation featureEvaluator = new CfsSubsetEval();
				
				featureEvaluator.buildEvaluator(validationData);
				ASSearch featureSearcher = new BestFirst();
				featureSelector.setEvaluator(featureEvaluator);
				featureSelector.setSearch(featureSearcher);
				trainingData = AttributeSelection.useFilter(trainingData, featureSelector);
			}
			
			if (hasProperty("use_principal_components")){
				PrincipalComponents pc = new PrincipalComponents();
				pc.buildEvaluator(trainingData);
				trainingData = pc.transformedData(trainingData);
			}
			double bestGamma = 0;
			double bestC = 0;
			if (hasProperty("use_hyperparameter_optimization")){
				double bestF1 = 0;
				for (int gamma = -6; gamma <= 0; gamma++){
					for (int c = -1; c <= 5; c++){
						
						model = new SMO();
						model.setFilterType(new SelectedTag(SMO.FILTER_NORMALIZE, SMO.TAGS_FILTER));
						if (!hasProperty("linear"))
							model.setKernel(new RBFKernel(partialTrainingData, 25007, Math.pow(10, gamma)));
						model.setC(Math.pow(10, c));
						model.buildClassifier(partialTrainingData);
					
						eval = new Evaluation(validationData);
						eval.evaluateModel(model, validationData);
						
						double f1 = eval.weightedFMeasure();
						if (f1 > bestF1){
							bestF1 = f1;
							bestGamma = gamma;
							bestC = c;
						}
						Framework.log("Optimization round...\tC="+Math.pow(10, c)+"\tGamma="+Math.pow(10, gamma)+"\tF1:"+f1);
					}
					
				}
				Framework.log("Best performing gamma: "+Math.pow(10,bestGamma)+"\tBest performing C: "+Math.pow(10,bestC)+"\tResulting in F1="+bestF1+" on validationdata");
			} else {
				bestGamma = -4;	//-2 or -1
				bestC = 3;		//2
			}
			
			model = new SMO();
			model.setFilterType(new SelectedTag(SMO.FILTER_NORMALIZE, SMO.TAGS_FILTER));
//			model.setFilterType(new SelectedTag(SMOreg.FILTER_NONE, SMOreg.TAGS_FILTER));
			if (!hasProperty("linear"))
				model.setKernel(new RBFKernel(trainingData, 25007, Math.pow(10, bestGamma)));
			model.setC(Math.pow(10, bestC));
			
			model.buildClassifier(trainingData);
			
			eval = new Evaluation(trainingData);
			
			//get in-sample performance
			double[] inSamplePredictions = eval.evaluateModel(model, trainingData);
//			FastVector<NominalPrediction> stringPredictions = eval.predictions();
			ArrayList< weka.classifiers.evaluation.Prediction> stringPredictions = eval.predictions();
			
			for (int i = 0; i < trainingSpans.size(); i++){
				Span s = trainingSpans.get(i);
				Prediction p = new Prediction(s);
//				p.getAnnotations().put("polarity", stringPredictions.get(i).toString());
				p.putAnnotation("polarity", classLabels.get((int)inSamplePredictions[i]));
				this.predictions.put(s, p.getSingletonSet());
			}
			
			Framework.log(this.evaluate(this.getEvaluators().iterator().next(), trainingSet).getEvaluationResults());
			predictions.clear();
			
		} catch (Exception e){
			e.printStackTrace();
		}
		
		
	}

	@Override
	public void predict() {
		Framework.log(getLabel() + " - Predicting ...");
		
		Instances testData = new Instances(this.allWekaData, 0);
		ArrayList<Span> testSpans = new ArrayList<Span>();
		for (Span reviewSpan : getTestData()){
			for (Span s : reviewSpan.getDataset().getSpans(reviewSpan, "opinion")){
				testData.add(this.wekaInstances.get(s));
				testSpans.add(s);
			}
		}
		
		try {
			if (hasProperty("use_feature_selection")){
				testData = AttributeSelection.useFilter(testData, featureSelector);
			}
			
			
			eval = new Evaluation(testData);
			double[] predictions = eval.evaluateModel(model, testData);
			//FastVector<NominalPrediction> stringPredictions = eval.predictions();
			ArrayList< weka.classifiers.evaluation.Prediction> stringPredictions = eval.predictions();
			System.out.println(eval.toSummaryString("\nResults\n======\n", false));
			System.out.println(eval.toClassDetailsString());
			for (int i = 0; i < testSpans.size(); i++){
				Span s = testSpans.get(i);
				Prediction p = new Prediction(s);
//				Main.debug(""+stringPredictions.get(i).actual() + "\t" + stringPredictions.get(i).predicted() + "\t" + predictions[i]);
//				p.getAnnotations().put("polarity", stringPredictions.get(i).toString());
				p.putAnnotation("polarity", classLabels.get((int)predictions[i]));
				this.predictions.put(s, p.getSingletonSet());
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	
	
	public Span getSentence(Span opinionSpan){
		TreeSet<Span> sentences = opinionSpan.getDataset().getSpans("sentence", opinionSpan.first());
		sentences.addAll(opinionSpan.getDataset().getSpans("sentence", opinionSpan.last()));
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
	
	public double getSentimentScore(ArrayList<Double> sentimentScores){
		if (sentimentScores == null || sentimentScores.isEmpty())
			return 0.0;
		
		double score = 0;
		if (sentimentScores.size()> 0){
			for (int i = 0; i < 5; i++){
				score += sentimentScores.get(i)*(i-2);
			}
		}
		return score;
	}

	public double getDepDistanceWeight(Word w){
		double distance = (w.getAnnotation("depDistance"));
		
		distance = 1.0 / Math.max(1.0,distance);
//		distance = Math.max(distance, 0.25);
//		
//		distance = distance > 1 ? 0.0 : 1.0;
		
//		distance = Math.max(0.0,1.0 - distance / 3.0);
		
		return distance;
	}
	
	public double getWordDistanceWeight(Word source, Word w){
		double distance = Math.abs(source.getOrder() - w.getOrder());
		distance = distance > 4 ? 0.25 : 1.0;
//		distance = 1.0 / Math.max(1.0,distance);
		return distance;
	}
	
	
	
	
	
//	public double companyRelations(Word source){
//		Main.debug("\n"+source.getWord() + "\t" + source.getTextualUnit().getAnnotations().get("text"));
////		Main.debug(""+source.getRelations().getRelationsToParents("deps"));
////		Main.debug(""+source.getRelations().getRelationsToChildren("deps"));
//		double value = mapRelations(source, true, new HashSet<Word>());
//		Main.debug("Value: "+value);
//		return value;
//	}
	
//	public double mapRelations(Word currentWord, boolean directionUp, HashSet<Word> visitedWords){
//		visitedWords.add(currentWord);
//		double value = 0.0;
//		if (directionUp){
//			for (Relation rel : currentWord.getRelations().getRelationsToParents("deps")){
//				if (!rel.getAnnotations().get("relationShortName").equals("root") && !visitedWords.contains((Word)rel.getParent())){					
//					value += mapRelations((Word)rel.getParent(), true, visitedWords);
//					Main.debug(value + "\t" + rel.toString());
//				}
//			}
//		}
//		directionUp = false;
//		for (Relation rel : currentWord.getRelations().getRelationsToChildren("deps")){
//			if (!visitedWords.contains((Word)rel.getChild())){
//				value += mapRelations((Word)rel.getChild(),false, visitedWords);
//				Main.debug(value + "\t" + rel.toString());
//			}
//		}
//		
//		HashSet<String> concepts = ont.getLexicalizedConcepts(ont.URI_Mention, currentWord.getWord());
//		concepts.addAll(ont.getLexicalizedConcepts(ont.URI_Mention, currentWord.getAnnotations().get("lemma")));
//		HashSet<String> superclasses = new HashSet<>();
//		for (String uri : concepts){
//			superclasses.addAll(ont.getSuperclasses(uri));
//		}
//		if (superclasses.contains(ont.URI_Positive))
//			value = Math.abs(value)+1;
//		if (superclasses.contains(ont.URI_Negative))
//			value = -1*Math.abs(value)-1;
//		if (superclasses.contains(ont.URI_Decrease))
//			value = -1.5*(value+1);
//		if (superclasses.contains(ont.URI_Increase))
//			value = 1.5*(value+1);	
//		if (superclasses.contains(ont.URI_NegativeEntityMention))
//			value += -1;
//		if (superclasses.contains(ont.URI_PositiveEntityMention))
//			value += 1;
//		Main.debug(superclasses.toString());
//		Main.debug(value + "\t" + currentWord.getWord() + "\t" + currentWord.getAnnotations().get("lemma") + "\t" + currentWord.getAnnotations().get("pos"));
//		return value;
//	}
//	
//	
//	/**
//	 * Get the largest nounphrase in this sentence that contains as much of the company name
//	 * @param sentenceSpan
//	 * @param companyName
//	 */
//	public Span getCompanyPhrase(Span sentenceSpan, String companyName){
//		
//		//loop through sentence to find first word of company name
//		Word firstWord = null;
//		Word lastWord = null;
//				
//		for (Word word : sentenceSpan){
//			if (companyName.startsWith(word.getWord())){
//				firstWord = word;
//				lastWord = firstWord;
//				int index = word.getEndOffset()+1;
//				while (lastWord.hasNextWord() 
//						&& companyName.length()>index 
//						&& companyName.substring(index).startsWith(lastWord.getNextWord().getWord())){
//					lastWord = lastWord.getNextWord();
//				}
//				break;
//			}
//		}
//		if (firstWord == null)
//			return null;
//		Dataset dataset = sentenceSpan.getDataset();
//		TreeSet<Span> spans1 = dataset.getSpans("syntacticPhrase", firstWord);
//		TreeSet<Span> spans2 = dataset.getSpans("syntacticPhrase", lastWord);
//		TreeSet<Span> candidates = new TreeSet<Span>();
//		candidates.addAll(spans1);
//		candidates.retainAll(spans2);
//		if (candidates.size()==0){
//			return null;
//		} else if (candidates.size()==1){
//			Span c = candidates.iterator().next(); 
//			if (c.getAnnotations().get("pos").equals("NP")){
//				return c;
//			} else {
//				return null;
//			}
//		} else {
//			Span smallestNP = null;
//			for (Span c : candidates){
//				if (c.getAnnotations().get("pos").equals("NP")){
//					if (smallestNP == null || smallestNP.size() > c.size()){
//						smallestNP = c;
//					}
//				}
//			}
//			return smallestNP;
//		}
//		
//	}
//	
//	public HashMap<String, Double> getOntologySingleConcepts(Span span, Word source, boolean getEntities, boolean getActions, boolean getProperties){
//		String companyName = "";
//		if (span.getAnnotations().containsKey("company"))
//			companyName = span.getAnnotations().get("company");
//		
//		
//			
//		
//		
//		HashMap<String, Double> ontologyFeatures = new HashMap<String, Double>();
//		for (Word w : span){
//			double weight = (hasProperty("use_dep_distance")) ? getDepDistanceWeight(w) : 1.0;
////			weight = (hasProperty("use_word_distance") && source!=null) ? getWordDistanceWeight(source, w) : 1.0;
//			
//			if (!hasProperty("no_company_name") || companyName.indexOf(w.getWord()) == -1){
//				String pos = w.getAnnotations().get("pos");
//				String lemma = w.getAnnotations().get("lemma");
//				String uri = null; 
//				if (getEntities){
//					uri = ont.getLexicalizedEntity(w.getWord());
//					for (String classURI : ont.getSuperclasses(uri)){
//						ontologyFeatures.put(classURI, Math.max(weight,ontologyFeatures.getOrDefault(classURI, 0.0)));
//					}
//					if (uri == null){
//						uri = ont.getLexicalizedEntity(lemma);
//						for (String classURI : ont.getSuperclasses(uri)){
//							ontologyFeatures.put(classURI, Math.max(weight,ontologyFeatures.getOrDefault(classURI, 0.0)));
//						}
//					}
//					if (hasProperty("no_company_uri") && ontologyFeatures.containsKey(ont.URI_NamedEntityMention)){
//						ontologyFeatures.remove(uri);
//					}
//				}
//				
//				if (getActions){
//					uri = ont.getLexicalizedAction(lemma);
//					for (String classURI : ont.getSuperclasses(uri)){
//						ontologyFeatures.put(classURI, Math.max(weight,ontologyFeatures.getOrDefault(classURI, 0.0)));
//					}
//				}
//				if (getProperties){
//					uri = ont.getLexicalizedProperty(lemma);
//					for (String classURI : ont.getSuperclasses(uri)){
//						ontologyFeatures.put(classURI, Math.max(weight,ontologyFeatures.getOrDefault(classURI, 0.0)));
//					}
//				}
////				ontologyFeatures.addAll(ont.getSuperclasses(uri));
//				
//				
//			}
//		}
//		return ontologyFeatures;
//	}
//	
//	
//	public HashMap<String, Double> getRelations(Span sentenceSpan, Word source){
//		String companyName = "";
//		String companyURI = "";
//		if (sentenceSpan.getAnnotations().containsKey("company")){
//			companyName = sentenceSpan.getAnnotations().get("company");
//			companyURI = ont.getLexicalizedEntity(companyName);
//		}
//		
//		
//		HashMap<String,HashSet<Relation>> relations = new HashMap<>();
//		for (Word w : sentenceSpan){
//			if (!hasProperty("no_company_name") || companyName.indexOf(w.getWord()) == -1){
//				for (Relation rel :w.getRelations().getRelationsToChildren("deps")){
//					String depType = rel.getAnnotations().get("relationShortName");
//					relations.putIfAbsent(depType, new HashSet<>());
//					relations.get(depType).add(rel);
//				}
//				for (Relation rel :w.getRelations().getRelationsToParents("deps")){
//					String depType = rel.getAnnotations().get("relationShortName");
//					relations.putIfAbsent(depType, new HashSet<>());
//					relations.get(depType).add(rel);
//				}
//			}
//		}
//		
//		HashMap<String, Double> ontologyFeatures = new HashMap<>();
//		
//		relations.remove("root");
//		
////		for (String depType : new String[]{"dobj","nsubj"}){
//		for (String depType : relations.keySet()){
//			if (relations.containsKey(depType)){
//				for (Relation rel : relations.get(depType)){
//					Word parent = (Word)rel.getParent();
//					Word child = (Word)rel.getChild();
//					String parentPos = parent.getAnnotations().get("pos");
//					String childPos = child.getAnnotations().get("pos");
//	//				parentPos = parentPos.substring(0, Math.min(parentPos.length(), 2));
//	//				childPos = childPos.substring(0, Math.min(childPos.length(), 2));
//					String actionURI = null;
//					String entityURI = null;
//					String propertyURI= null;
//					if (parentPos.startsWith("VB") && childPos.startsWith("NN")){
//						entityURI= ont.getLexicalizedEntity(child.getAnnotations().get("lemma"));
//						if (entityURI==null)
//							entityURI= ont.getLexicalizedEntity(child.getWord());
//						actionURI = ont.getLexicalizedAction(parent.getAnnotations().get("lemma"));
//					} else if (childPos.startsWith("VB") && parentPos.startsWith("NN")){
//						actionURI = ont.getLexicalizedAction(child.getAnnotations().get("lemma"));
//						entityURI= ont.getLexicalizedEntity(parent.getAnnotations().get("lemma"));
//						if (entityURI==null)
//							entityURI= ont.getLexicalizedEntity(parent.getWord());
//					} else if (parentPos.startsWith("NN")){
//						propertyURI = ont.getLexicalizedProperty(child.getAnnotations().get("lemma"));
//						entityURI= ont.getLexicalizedEntity(parent.getAnnotations().get("lemma"));
//						if (entityURI==null)
//							entityURI= ont.getLexicalizedEntity(parent.getWord());
//					} else {
//						propertyURI = ont.getLexicalizedProperty(child.getAnnotations().get("lemma"));
//						actionURI = ont.getLexicalizedAction(parent.getAnnotations().get("lemma"));
//					}
////					double weight = (hasProperty("use_weight") && entityURI != null && entityURI.equals(companyURI)) ? 
////							Double.parseDouble(getProperty("use_weight")) : 1.0;
//					
//					double weight = (hasProperty("use_dep_distance")) ? Math.max(getDepDistanceWeight(parent),getDepDistanceWeight(child)) : 1.0;
////					weight = (hasProperty("use_word_distance") && source!=null) ? Math.max(getWordDistanceWeight(source, parent),getWordDistanceWeight(source, child)) : 1.0;
//					
//					
//					//check for transitivity
//					if (hasProperty("check_transitivity") && rel.getAnnotations().get("relationShortName").equals("nsubj") &&
//							ont.getSuperclasses(actionURI).contains(ont.URI_TransitiveMention)){
//						actionURI = null;
//					}
//					
//					
//					//we found a useful statement?
//					if (actionURI!=null && entityURI != null){
//						
//						//faster proxy
//						for (String uri : ont.getSuperclasses(actionURI)){
//							ontologyFeatures.put(uri, Math.max(weight,ontologyFeatures.getOrDefault(uri, 0.0)));
//						}
//						for (String uri : ont.getSuperclasses(entityURI)){
//							ontologyFeatures.put(uri, Math.max(weight,ontologyFeatures.getOrDefault(uri, 0.0)));
//						}
//						ontologyFeatures.put(ont.URI_Statement,1.0);
//						
//						//real ontology checking code
////							HashSet<String> actionClasses = ont.getSuperclasses(action);
////							if (actionClasses.contains(ont.URI_Increase) || actionClasses.contains(ont.URI_Decrease)){
////								String indiv = ont.addIndividual((indivId++)+"", ont.URI_Statement, action, entity);
////								ontologyFeatures.addAll(ont.getClasses(indiv));
////								Main.debug(rel.toString() + "\n" + action+"\n"+entity+"\n"+indiv+"\n"+ont.getClasses(indiv));
////							} else if (actionClasses.contains(ont.URI_Positive) || actionClasses.contains(ont.URI_Negative)){
////								ontologyFeatures.addAll(ont.getSuperclasses(entity));
////								ontologyFeatures.addAll(actionClasses);
////								ontologyFeatures.add(ont.URI_Statement);
////							}
////							if (ontologyFeatures.contains(ont.URI_NamedEntityMention)){
////								ontologyFeatures.remove(entity);
////							}
//					}
//					
//					if (propertyURI!=null && entityURI != null){ 
//						//faster proxy
//						for (String uri : ont.getSuperclasses(propertyURI)){
//							ontologyFeatures.put(uri, Math.max(weight,ontologyFeatures.getOrDefault(uri, 0.0)));
//						}
//						for (String uri : ont.getSuperclasses(entityURI)){
//							ontologyFeatures.put(uri, Math.max(weight,ontologyFeatures.getOrDefault(uri, 0.0)));
//						}
//						ontologyFeatures.put(ont.URI_Statement,1.0);
//
//						//real ontology checking code							
////							HashSet<String> actionClasses = ont.getSuperclasses(property);
////							if (actionClasses.contains(ont.URI_Increase) || actionClasses.contains(ont.URI_Decrease)){
////								String indiv = ont.addIndividual((indivId++)+"", ont.URI_Statement, property, entity);
////								ontologyFeatures.addAll(ont.getClasses(indiv));
////								Main.debug(rel.toString() + "\n" + property+"\n"+entity+"\n"+indiv+"\n"+ont.getClasses(indiv));
////							} else if (actionClasses.contains(ont.URI_Positive) || actionClasses.contains(ont.URI_Negative)){
////								ontologyFeatures.addAll(ont.getSuperclasses(entity));
////								ontologyFeatures.addAll(actionClasses);
////								ontologyFeatures.add(ont.URI_Statement);
////							}
////							if (ontologyFeatures.contains(ont.URI_NamedEntityMention)){
////								ontologyFeatures.remove(entity);
////							}
//					}
//				
//					if (propertyURI!=null && actionURI != null){
//						boolean increaseProperty = false;
//						for (String uri : ont.getSuperclasses(propertyURI)){
//							ontologyFeatures.put(uri, Math.max(weight,ontologyFeatures.getOrDefault(uri, 0.0)));
//							if (ontologyFeatures.containsKey(ont.URI_Decrease)){
//								ontologyFeatures.put(ont.URI_Negative, Math.max(weight,ontologyFeatures.getOrDefault(ont.URI_Negative, 0.0)));
//							}
//							if (ontologyFeatures.containsKey(ont.URI_Increase)){
//								increaseProperty = true;
//							}
//						}
//						for (String uri : ont.getSuperclasses(actionURI)){
//							ontologyFeatures.put(uri, Math.max(weight,ontologyFeatures.getOrDefault(uri, 0.0)));
//							if (increaseProperty){
//								if (ontologyFeatures.containsKey(ont.URI_Decrease)){
//									ontologyFeatures.put(ont.URI_Negative, Math.max(weight,ontologyFeatures.getOrDefault(ont.URI_Negative, 0.0)));
//								}
//								if (ontologyFeatures.containsKey(ont.URI_Increase)){
//									ontologyFeatures.put(ont.URI_Positive, Math.max(weight,ontologyFeatures.getOrDefault(ont.URI_Positive, 0.0)));
//								}
//							}
//								
//							
//							
//						}
//						ontologyFeatures.put(ont.URI_Statement,1.0);
//						
//					}
//					
//					
//					if (ontologyFeatures.containsKey(ont.URI_Increase)){
//						if (ontologyFeatures.containsKey(ont.URI_NegativeEntityMention)){
//							ontologyFeatures.put(ont.URI_Negative, Math.max(weight,ontologyFeatures.getOrDefault(ont.URI_Negative, 0.0)));
//						} else {
//							ontologyFeatures.put(ont.URI_Positive, Math.max(weight,ontologyFeatures.getOrDefault(ont.URI_Positive, 0.0)));
//						}
//					} else if (ontologyFeatures.containsKey(ont.URI_Decrease)){
//						if (ontologyFeatures.containsKey(ont.URI_NegativeEntityMention)){
//							ontologyFeatures.put(ont.URI_Positive, Math.max(weight,ontologyFeatures.getOrDefault(ont.URI_Positive, 0.0)));
//						} else {
//							ontologyFeatures.put(ont.URI_Negative, Math.max(weight,ontologyFeatures.getOrDefault(ont.URI_Negative, 0.0)));
//						}
//					}
////					if (ontologyFeatures.containsKey(ont.URI_Increase) 
////							&& ontologyFeatures.containsKey(ont.URI_ActionMention)
////							&& ontologyFeatures.containsKey(ont.URI_PropertyMention)){
////						ontologyFeatures.put(ont.URI_Positive, Math.max(weight,ontologyFeatures.getOrDefault(ont.URI_Positive, 0.0)));
////					}
////					if (ontologyFeatures.containsKey(ont.URI_Decrease) 
////							&& ontologyFeatures.containsKey(ont.URI_ActionMention)
////							&& ontologyFeatures.containsKey(ont.URI_PropertyMention)){
////						ontologyFeatures.put(ont.URI_Negative, Math.max(weight,ontologyFeatures.getOrDefault(ont.URI_Negative, 0.0)));
////					}
//					
//					if (hasProperty("no_company_uri") && ontologyFeatures.containsKey(ont.URI_NamedEntityMention)){
//						ontologyFeatures.remove(entityURI);
//					}
//				}
//			}
//		}
//		return ontologyFeatures;
//	}
	
	
	
	
	public void extractStatements(Span sentenceSpan){
		Dataset dataset = sentenceSpan.getDataset();
		
//		Main.debug("\n"+sentenceSpan.toString());
//		HashSet<Relation> children = sentenceSpan.getRelations().getAllRelationsToChildren();
//		Word root = null;
//		if (children.size() == 1){
//			root = (Word) children.iterator().next().getChild();
//			for (Relation r : root.getRelations().getAllRelationsToChildren()){
//				Main.debug("\t"+r);
//			}
//		}
//		Main.debug(""+root);
//		
		
		HashMap<String, HashSet<Relation>> relations = new HashMap<>();
		
		for (Relation rel : dataset.getRelations("deps")){
			String gramType = rel.getAnnotation("relationShortName");
			if (!relations.containsKey(gramType))
				relations.put(gramType, new HashSet<Relation>());
			relations.get(gramType).add(rel);
			
		}
		
		for (String gramType : relations.keySet()){
			Framework.debug(gramType+": "+relations.get(gramType).size());
		}
		for (String gramType : new String[]{"dobj"}){
			Framework.debug("RelationType: "+gramType);
			for (Relation r : relations.get(gramType)){
				DataEntity parent = r.getParent();
				TreeSet<Relation> parentPhrases = parent.getRelations().getRelationsToChildren("syntacticHead");
				
				DataEntity child = r.getChild();
				TreeSet<Relation> childPhrases = child.getRelations().getRelationsToChildren("syntacticHead");
				
				Framework.debug(""+parent.getAnnotation("lemma") + " " +
						parent.getAnnotation("pos") + " " +
						parent.getAnnotation("nerLabel") + " " +
						" -> " + child.getAnnotation("lemma") + " " +
						child.getAnnotation("pos") + " " +
						child.getAnnotation("nerLabel") + "\t" +
						((Word)parent).getTextualUnit().getAnnotation("text") + "\t" +
						((Word)parent).getTextualUnit().getAnnotationEntryText("polarity")
						);
				
//				if (parentPhrases != null){
//					for (Relation rel : parentPhrases){
//						Main.debug("Parent phrase: "+((Span)rel.getChild()).getAnnotations().get("text"));
//					}
//				}
				if (childPhrases != null){
					Span bestSpan=null;
					for (Relation rel : childPhrases){
						Span currentSpan = (Span)rel.getChild();
						if (!currentSpan.contains((Word)parent)){
							//we don't want a noun phrase so big it contains the verb
							//but it should be as big as possible
							if (bestSpan == null || bestSpan.size() < currentSpan.size()){
								bestSpan = currentSpan;
							}
						}
					}
					if (bestSpan!=null){
						Framework.debug("Child phrase: "+bestSpan.getAnnotation("text"));
					}
				}
			}
		}
		
		System.exit(0);
//		Main.debug(sentenceSpan.getTextualUnit().getRelations().getAllRelationsToChildren().toString());
//		Main.debug(sentenceSpan.getTextualUnit().getRelations().getAllRelationsToChildren().toString());
		
	}
	
	
	
}
