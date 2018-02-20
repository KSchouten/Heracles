package edu.eur.absa.data;

import java.io.File;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.TreeSet;

import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Elements;
import edu.eur.absa.Framework;
import edu.eur.absa.model.Dataset;
import edu.eur.absa.model.Span;
import edu.eur.absa.model.Word;
import edu.eur.absa.nlp.CoreNLPDependencyParser;
import edu.eur.absa.nlp.CoreNLPLemmatizer;
import edu.eur.absa.nlp.CoreNLPNamedEntityRecognizer;
import edu.eur.absa.nlp.CoreNLPParser;
import edu.eur.absa.nlp.CoreNLPPosTagger;
import edu.eur.absa.nlp.CoreNLPSentimentAnnotator;
import edu.eur.absa.nlp.CoreNLPTokenizer;
import edu.eur.absa.nlp.LowercaseChanger;
import edu.eur.absa.nlp.NLPTask;

/**
 * A reader for the SemEval2014 Task 4 ABSA data set. This is a data set in XML format.
 * Data for this reader can be found at: http://alt.qcri.org/semeval2014/task4/index.php?id=data-and-tools
 * @author Kim Schouten
 *
 */
public class SemEval2014Task4ABSAReader implements IDataReader {

	/**
	 * This is simply a test method
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception{
		processAllData();
//		showStatistics((new DatasetJSONReader()).read(new File(Framework.DATA_PATH+"SemEval2014Restaurants-Train.json")));
//		showStatistics((new DatasetJSONReader()).read(new File(Framework.DATA_PATH+"SemEval2014Restaurants-Test.json")));
	}
	
	/**
	 * This method will process all raw SemEval2014 data. 
	 * Note that data files are not included in the repository due to licensing and file size.
	 * 
	 * @throws Exception
	 */
	public static void processAllData() throws Exception {
		Dataset allRest = (new SemEval2014Task4ABSAReader()).read(new File(Framework.RAWDATA_PATH + "ABSA-14_Restaurants_All.xml"));
		(new DatasetJSONWriter()).write(allRest, new File(Framework.DATA_PATH+"SemEval2014Restaurants-All.json"));
		(new DatasetJSONWriter(true)).write(allRest, new File(Framework.DATA_PATH+"SemEval2014Restaurants-All.pretty.json"));
		System.exit(0);
		
		Dataset trainRest = (new SemEval2014Task4ABSAReader()).read(new File(Framework.RAWDATA_PATH + "ABSA-14_Restaurants_Train_Data.xml"));
		(new DatasetJSONWriter()).write(trainRest, new File(Framework.DATA_PATH+"SemEval2014Restaurants-Train.json"));
		(new DatasetJSONWriter(true)).write(trainRest, new File(Framework.DATA_PATH+"SemEval2014Restaurants-Train.pretty.json"));
		
		Dataset testRest = (new SemEval2014Task4ABSAReader()).read(new File(Framework.RAWDATA_PATH + "ABSA-14_Restaurants_Test_Gold.xml"));
		(new DatasetJSONWriter()).write(testRest, new File(Framework.DATA_PATH+"SemEval2014Restaurants-Test.json"));
		(new DatasetJSONWriter(true)).write(testRest, new File(Framework.DATA_PATH+"SemEval2014Restaurants-Test.pretty.json"));
		
		Dataset trainLapt = (new SemEval2014Task4ABSAReader()).read(new File(Framework.RAWDATA_PATH + "ABSA-14_Laptops_Train_Data.xml"));
		(new DatasetJSONWriter()).write(trainLapt, new File(Framework.DATA_PATH+"SemEval2014Laptops-Train.json"));
		(new DatasetJSONWriter(true)).write(trainLapt, new File(Framework.DATA_PATH+"SemEval2014Laptops-Train.pretty.json"));
		
		Dataset testLapt = (new SemEval2014Task4ABSAReader()).read(new File(Framework.RAWDATA_PATH + "ABSA-14_Laptops_Test_Gold.xml"));
		(new DatasetJSONWriter()).write(testLapt, new File(Framework.DATA_PATH+"SemEval2014Laptops-Test.json"));
		(new DatasetJSONWriter(true)).write(testLapt, new File(Framework.DATA_PATH+"SemEval2014Laptops-Test.pretty.json"));

		
	}
	
	@Override
	public Dataset read(File file) throws Exception {
		//sentence is the textual unit, as sentences are not grouped into reviews in this dataset
		Dataset dataset = new Dataset(file.getName(),"sentence");
		
		HashMap<Element, Span> aspectTermElementsWithSentenceSpans = new HashMap<>();
		HashMap<Element, Span> aspectCategoryElementsWithSentenceSpans = new HashMap<>();
		
		Document document = new Builder().build(file);
		//root is the <sentences> element
		Element root = document.getRootElement();
		Elements sentenceElements = root.getChildElements();
		for (int j = 0; j < sentenceElements.size(); j++){
			Framework.log("Reading sentence "+(j+1)+" out of "+sentenceElements.size());
			Element sentenceElement = sentenceElements.get(j);
			String sentenceId = sentenceElement.getAttributeValue("id");
			String text = sentenceElement.getChildElements("text").get(0).getValue();
			Span sentenceSpan = new Span("sentence", dataset);
			sentenceSpan.putAnnotation("id", sentenceId);
			sentenceSpan.putAnnotation("text", text.replaceAll("(\\w)(-|/)(\\w)", "$1 $3"));
			
			boolean hasAspectTerms = sentenceElement.getChildElements("aspectTerms").size()>0;
			if (hasAspectTerms){
				Elements aspectTermElements = sentenceElement.getChildElements("aspectTerms").get(0).getChildElements();
				for (int k = 0; k < aspectTermElements.size(); k++){
					Element aspectTermElement = aspectTermElements.get(k);
					aspectTermElementsWithSentenceSpans.put(aspectTermElement, sentenceSpan);
				}
			}
			boolean hasAspectCategories = sentenceElement.getChildElements("aspectCategories").size()>0;
			if (hasAspectCategories){
				Elements aspectCategoryElements = sentenceElement.getChildElements("aspectCategories").get(0).getChildElements();
				for (int k = 0; k < aspectCategoryElements.size(); k++){
					Element aspectCategoryElement = aspectCategoryElements.get(k);
					aspectCategoryElementsWithSentenceSpans.put(aspectCategoryElement, sentenceSpan);
				}
			}
		}
		
		dataset.getPerformedNLPTasks().add(NLPTask.SENTENCE_SPLITTING);
		dataset.process(new CoreNLPTokenizer(), "sentence")
			.process(new CoreNLPPosTagger(), "sentence")
			.process(new CoreNLPLemmatizer(), "sentence")
			//.process(new OntologyLookup(null, new ReasoningOntology(Framework.EXTERNALDATA_PATH + "RestaurantSentiment.owl")), "sentence")
			.process(new CoreNLPPosTagger(), "sentence")
			.process(new CoreNLPLemmatizer(), "sentence")
			//.process(new CoreNLPNamedEntityRecognizer(), "sentence")
			//.process(new CoreNLPParser(), "sentence")
			//.process(new CoreNLPDependencyParser(), "sentence")
			//.process(new CoreNLPSentimentAnnotator(), "sentence")
			;
		
		//only thing left is to get the aspecTerms and make them span the correct words
		for (Element aspectTermElement : aspectTermElementsWithSentenceSpans.keySet()){
			Span sentenceSpan = aspectTermElementsWithSentenceSpans.get(aspectTermElement);
			
			String term = aspectTermElement.getAttributeValue("term");
			String polarity = aspectTermElement.getAttributeValue("polarity");
			String from = aspectTermElement.getAttributeValue("from");
			String to = aspectTermElement.getAttributeValue("to");
			
			Span termSpan = new Span("aspectTerm", sentenceSpan.getTextualUnit());
			termSpan.putAnnotation("polarity", polarity);
			//these are mostly useless after the span has been determined, but
			// we'll keep them around so we can reconstruct the (annotated) data
			// in the same format
			termSpan.putAnnotation("term", term);
			termSpan.putAnnotation("from", from);
			termSpan.putAnnotation("to", to);

			int opinionStartOffset = Integer.parseInt(from);
			int opinionEndOffset = Integer.parseInt(to);
			for (Word word : sentenceSpan){
				if (word.getStartOffset() >= opinionStartOffset && word.getEndOffset() <= opinionEndOffset){
					termSpan.add(word);
				} else if (word.getStartOffset() >= opinionStartOffset && word.getStartOffset() < opinionEndOffset ||
						word.getEndOffset() > opinionStartOffset && word.getEndOffset() <= opinionEndOffset) {
					//this part is necessary to include long words where only part of it is annotated
					// as an aspectTerm
					termSpan.add(word);
				}
			}
			
			if (termSpan.isEmpty()){
				
				
				
				//adding words failed somehow
				Framework.debug("No words found to add to AspectTerm");
				Framework.debug(termSpan.showAnnotations());
				Framework.debug(opinionStartOffset + "\t" + opinionEndOffset);
				for (Word word : sentenceSpan){
					Framework.debug(word.getWord() + "\t" + word.getStartOffset() + "\t" + word.getEndOffset());
					if (word.getStartOffset() >= opinionStartOffset && word.getEndOffset() <= opinionEndOffset){
						termSpan.add(word);
					}
				}
			}
				
			
			
		}
		for (Element aspectCategoryElement : aspectCategoryElementsWithSentenceSpans.keySet()){
			Span sentenceSpan = aspectCategoryElementsWithSentenceSpans.get(aspectCategoryElement);
			String category = aspectCategoryElement.getAttributeValue("category");
			String polarity = aspectCategoryElement.getAttributeValue("polarity");
			Span categorySpan = new Span("aspectCategory", sentenceSpan);
			categorySpan.putAnnotation("category", category);
			if (polarity != null){
				categorySpan.putAnnotation("polarity", polarity);
			}
			//also record an augmented category field where we omit the anecdotes/miscellaneous category
			// as is done for the icwe2014 paper
			if (!category.equalsIgnoreCase("anecdotes/miscellaneous")){
				categorySpan.putAnnotation("category-nomisc", category);
			}
			categorySpan.addAll(sentenceSpan);
		}
		
		
		return dataset;
	}
	
	public static void showStatistics(Dataset dataset){
		
		
		TreeSet<Span> aspectTerms = dataset.getSpans("aspectTerm");
		TreeSet<Span> aspectCategories= dataset.getSpans("aspectCategory");
		HashMap<String, Integer> sentimentFreqsTerms = new HashMap<>();
		HashMap<String, Integer> sentimentFreqsCategories = new HashMap<>();
		HashMap<String, Integer> categoryFreqs = new HashMap<>();
		for (Span aspectTerm : aspectTerms){
			String sent = aspectTerm.getAnnotation("polarity");
			sentimentFreqsTerms.put(sent, sentimentFreqsTerms.getOrDefault(sent, 0)+1);
		}
		for (Span aspectCategory : aspectCategories){
			String sent = aspectCategory.getAnnotation("polarity");
			sentimentFreqsCategories.put(sent, sentimentFreqsCategories.getOrDefault(sent, 0)+1);
			String cat = aspectCategory.getAnnotationEntryText("category");
			categoryFreqs.put(cat, categoryFreqs.getOrDefault(cat, 0)+1);
		}	
		
		
		TreeSet<Span> sentences = dataset.getSpans("sentence");
		HashMap<Integer, Integer> termsPerSentence = new HashMap<>();
		HashMap<Integer, Integer> categoriesPerSentence = new HashMap<>();
		
		int deviatingTermOpinions = 0;
		int deviatingCategoryOpinions = 0;
		for (Span sentence : sentences){
			//count aspectTerm data
			TreeSet<Span> termsInSentence = dataset.getSpans(sentence, "aspectTerm");
			termsPerSentence.put(termsInSentence.size(), termsPerSentence.getOrDefault(termsInSentence.size(),0)+1);
			if (!termsInSentence.isEmpty()){
				HashMap<String, Integer> sentSentFreqs = new HashMap<>();
				for (Span aspectTerm : termsInSentence){
					String sent = aspectTerm.getAnnotation("polarity");
					sentSentFreqs.put(sent, sentSentFreqs.getOrDefault(sent, 0)+1);
				}
				TreeMap<Integer, String> sortedPolarities = new TreeMap<>();
				for (String pol : sentSentFreqs.keySet()){
					sortedPolarities.put(sentSentFreqs.get(pol), pol);
				}
				deviatingTermOpinions += termsInSentence.size() - sortedPolarities.lastKey();
			}
			//count aspectCategory data
			TreeSet<Span> categoriesInSentence = dataset.getSpans(sentence, "aspectCategory");
			categoriesPerSentence.put(categoriesInSentence.size(), categoriesPerSentence.getOrDefault(categoriesInSentence.size(),0)+1);
			if (!categoriesInSentence.isEmpty()){
				HashMap<String, Integer> sentSentFreqs = new HashMap<>();
				for (Span aspectCategory : categoriesInSentence){
					String sent = aspectCategory.getAnnotation("polarity");
					sentSentFreqs.put(sent, sentSentFreqs.getOrDefault(sent, 0)+1);
				}
				TreeMap<Integer, String> sortedPolarities = new TreeMap<>();
				for (String pol : sentSentFreqs.keySet()){
					sortedPolarities.put(sentSentFreqs.get(pol), pol);
				}
				
				deviatingCategoryOpinions += categoriesInSentence.size() - sortedPolarities.lastKey();
			}
		}
		
		
				
		// generate report
		Framework.log("\nNumber of sentences:\t"+sentences.size());
		Framework.log("\nNumber of aspect terms:\t"+aspectTerms.size());
		Framework.log("\nNumber of aspect categories:\t"+aspectCategories.size());
		
		Framework.log("\nSentiment Frequencies For AspectTerms");
		for (String sent : sentimentFreqsTerms.keySet()){
			Framework.log(sent + "\t" + sentimentFreqsTerms.get(sent) + "\t" + ((double)sentimentFreqsTerms.get(sent) / aspectTerms.size()));
		}
		Framework.log("\nSentiment Frequencies For AspectCategories");
		for (String sent : sentimentFreqsCategories.keySet()){
			Framework.log(sent + "\t" + sentimentFreqsCategories.get(sent) + "\t" + ((double)sentimentFreqsCategories.get(sent) / aspectCategories.size()));
		}
		Framework.log("\nAspect Category Frequencies");
		for (String cat : categoryFreqs.keySet()){
			Framework.log(cat + "\t" + categoryFreqs.get(cat) + "\t" + ((double)categoryFreqs.get(cat) / aspectCategories.size()));
		}
		Framework.log("\nTerms per sentence");
		for (Integer i : termsPerSentence.keySet()){
			Framework.log(i + "\t" + termsPerSentence.get(i) + "\t" + ((double)termsPerSentence.get(i) / sentences.size()));
		}
		Framework.log("\nCategories per sentence");
		for (Integer i : categoriesPerSentence.keySet()){
			Framework.log(i + "\t" + categoriesPerSentence.get(i) + "\t" + ((double)categoriesPerSentence.get(i) / sentences.size()));
		}
		
		Framework.log("\nAspectTerms deviating from sentence majority sentiment:\t"+deviatingTermOpinions);
		Framework.log("\nAspectCategories deviating from sentence majority sentiment:\t"+deviatingTermOpinions);
		
		
	}

}
