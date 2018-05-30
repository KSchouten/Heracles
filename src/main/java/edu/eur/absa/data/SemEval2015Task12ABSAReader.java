package edu.eur.absa.data;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
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
 * A reader for the SemEval2015 Task 12 ABSA data sets, works also for the SemEval2016 Task 5 ABSA Subtask 1 data (same format).
 * This is a good example of an XML reader.
 * Data for this reader can be found at: 
 * - http://alt.qcri.org/semeval2015/task12/index.php?id=data-and-tools
 * - http://alt.qcri.org/semeval2016/task5/index.php?id=important-dates (subtask 1 only)
 * @author Kim Schouten
 *
 */
public class SemEval2015Task12ABSAReader implements IDataReader {

	/**
	 * This is simply a test method
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception{
		processAllData();
		
//		showStatistics((new DatasetJSONReader()).read(new File(Framework.DATA_PATH+"SemEval2016SB1Laptops-Train.json")));
//
//		showStatistics((new DatasetJSONReader()).read(new File(Framework.DATA_PATH+"SemEval2016SB1Laptops-Test.json")));
		
	}
	
	public static void processAllData() throws Exception{
		Dataset test = (new SemEval2015Task12ABSAReader()).read(new File(Framework.RAWDATA_PATH + "ABSA-15_Restaurants_Test_Gold.xml"));
		(new DatasetJSONWriter()).write(test, new File(Framework.DATA_PATH+"SemEval2015Restaurants-Test.json"));
		(new DatasetJSONWriter(true)).write(test, new File(Framework.DATA_PATH+"SemEval2015Restaurants-Test.pretty.json"));
		
		Dataset train = (new SemEval2015Task12ABSAReader()).read(new File(Framework.RAWDATA_PATH + "ABSA-15_Restaurants_Train_Data.xml"));
		(new DatasetJSONWriter()).write(train, new File(Framework.DATA_PATH+"SemEval2015Restaurants-Train.json"));
		(new DatasetJSONWriter(true)).write(train, new File(Framework.DATA_PATH+"SemEval2015Restaurants-Train.pretty.json"));
		
		Dataset train2016 = (new SemEval2015Task12ABSAReader()).read(new File(Framework.RAWDATA_PATH + "ABSA-16_SB1_Restaurants_Train_Data.xml"));
		(new DatasetJSONWriter()).write(train2016, new File(Framework.DATA_PATH+"SemEval2016SB1Restaurants-Train.json"));
		(new DatasetJSONWriter(true)).write(train2016, new File(Framework.DATA_PATH+"SemEval2016SB1Restaurants-Train.pretty.json"));

		Dataset test2016 = (new SemEval2015Task12ABSAReader()).read(new File(Framework.RAWDATA_PATH + "ABSA-16_SB1_Restaurants_Test_Gold.xml"));
		(new DatasetJSONWriter()).write(test2016, new File(Framework.DATA_PATH+"SemEval2016SB1Restaurants-Test.json"));
		(new DatasetJSONWriter(true)).write(test2016, new File(Framework.DATA_PATH+"SemEval2016SB1Restaurants-Test.pretty.json"));

		
//		Dataset check = (new DatasetJSONReader()).read(new File(Framework.DATA_PATH+"SemEval2016SB1Restaurants-Training.json"));
//		(new DatasetJSONWriter(true)).write(check, new File(Framework.DATA_PATH+"Check.json"));
		
		
	}
	
	@Override
	public Dataset read(File file) throws Exception {
		//review is the textual unit, as sentences within a single review are semantically connected
		Dataset dataset = new Dataset(file.getName(),"review");
		
		HashMap<Element, Span> opinionElementsWithSentenceSpans = new HashMap<>();
		
		Document document = new Builder().build(file);
		//root is the <reviews> element
		Element root = document.getRootElement();
		Elements reviewElements = root.getChildElements();
		for (int i = 0; i < reviewElements.size(); i++){
			Element reviewElement = reviewElements.get(i);
			String reviewId = reviewElement.getAttributeValue("rid");
			Span reviewSpan = new Span("review", dataset);
			reviewSpan.putAnnotation("id", reviewId);
			String reviewText = "";
			Elements sentenceElements = reviewElement.getChildElements("sentences").get(0).getChildElements();
			for (int j = 0; j < sentenceElements.size(); j++){
				Element sentenceElement = sentenceElements.get(j);
				String sentenceId = sentenceElement.getAttributeValue("id");
				String text = sentenceElement.getChildElements("text").get(0).getValue();
				Span sentenceSpan = new Span("sentence", reviewSpan);
				sentenceSpan.putAnnotation("id", sentenceId);
				sentenceSpan.putAnnotation("text", text);
				reviewText += text;
				boolean hasOpinions = sentenceElement.getChildElements("Opinions").size()>0;
				if (hasOpinions){
					Elements opinionElements = sentenceElement.getChildElements("Opinions").get(0).getChildElements();
					for (int k = 0; k < opinionElements.size(); k++){
						Element opinionElement = opinionElements.get(k);
						opinionElementsWithSentenceSpans.put(opinionElement, sentenceSpan);
					}
				} else {
					//Framework.log("No opinions? "+sentenceId+"\n"+sentenceElement.toXML());
				}
			}
			reviewSpan.putAnnotation("text", reviewText);
		}
		dataset.getPerformedNLPTasks().add(NLPTask.SENTENCE_SPLITTING);
		dataset.process(new CoreNLPTokenizer(), "sentence")
			//.process(new LowercaseChanger(), "review")
			.process(new CoreNLPPosTagger(), "sentence")
			.process(new CoreNLPLemmatizer(), "sentence")
			//.process(new OntologyLookup(null, new ReasoningOntology(Framework.EXTERNALDATA_PATH + "RestaurantSentiment.owl")), "sentence")
			.process(new CoreNLPPosTagger(), "sentence")
			.process(new CoreNLPLemmatizer(), "sentence")
//			.process(new CoreNLPNamedEntityRecognizer(), "sentence")
			.process(new CoreNLPParser(), "sentence")
			.process(new CoreNLPDependencyParser(), "sentence")
			.process(new CoreNLPSentimentAnnotator(), "sentence")
			;
		
		//only thing left is to get the opinions and make them span the correct words
		for (Element opinionElement : opinionElementsWithSentenceSpans.keySet()){
			Span sentenceSpan = opinionElementsWithSentenceSpans.get(opinionElement);
			
			String target = opinionElement.getAttributeValue("target");
			String category = opinionElement.getAttributeValue("category");
			String polarity = opinionElement.getAttributeValue("polarity");
			String from = opinionElement.getAttributeValue("from");
			String to = opinionElement.getAttributeValue("to");
			
			Span opinionSpan = new Span("opinion", sentenceSpan.getTextualUnit());
			opinionSpan.putAnnotation("category", category);
			opinionSpan.putAnnotation("polarity", polarity);
			if (target != null && !target.equalsIgnoreCase("NULL")){
				//these are mostly useless after the span has been determined, but
				// we'll keep them around so we can reconstruct the (annotated) data
				// in the same format
				opinionSpan.putAnnotation("target", target);
				opinionSpan.putAnnotation("from", from);
				opinionSpan.putAnnotation("to", to);

				int sentenceOffset = sentenceSpan.first().getStartOffset();
				int opinionStartOffset = sentenceOffset + Integer.parseInt(from);
				int opinionEndOffset = sentenceOffset + Integer.parseInt(to);
				for (Word word : sentenceSpan){
					if (word.getStartOffset() >= opinionStartOffset && word.getEndOffset() <= opinionEndOffset){
						opinionSpan.add(word);
					}
				}
				
				if (opinionSpan.isEmpty()){
					//adding words failed somehow
					Framework.debug(opinionSpan.showAnnotations());
					Framework.debug(opinionStartOffset + "\t" + opinionEndOffset);
					for (Word word : sentenceSpan){
						Framework.debug(word.getWord() + "\t" + word.getStartOffset() + "\t" + word.getEndOffset());
						if (word.getStartOffset() >= opinionStartOffset && word.getEndOffset() <= opinionEndOffset){
							opinionSpan.add(word);
						}
					}
				}
				
			} else {
				opinionSpan.addAll(sentenceSpan);
			}
			
		}
		
		return dataset;
	}
	
	public static void showStatistics(Dataset dataset){
		
		
		TreeSet<Span> opinions = dataset.getSpans("opinion");
		HashMap<String, Integer> sentimentFreqs = new HashMap<>();
		HashMap<String, Integer> categoryFreqs = new HashMap<>();
		for (Span opinion : opinions){
			String sent = opinion.getAnnotation("polarity");
			sentimentFreqs.put(sent, sentimentFreqs.getOrDefault(sent, 0)+1);
			String cat = opinion.getAnnotationEntryText("category");
			categoryFreqs.put(cat, categoryFreqs.getOrDefault(cat, 0)+1);
		}
		
		TreeSet<Span> sentences = dataset.getSpans("sentence");
		HashMap<Integer, Integer> opinionsPerSentence = new HashMap<>();
		int deviatingOpinionsS = 0;
		for (Span sentence : sentences){
			TreeSet<Span> opinionsInSentence = sentence.getCoveredSpans(opinions);
			opinionsPerSentence.put(opinionsInSentence.size(), opinionsPerSentence.getOrDefault(opinionsInSentence.size(),0)+1);
			if (!opinionsInSentence.isEmpty()){
				HashMap<String, Integer> sentSentFreqs = new HashMap<>();
				for (Span opinion : opinionsInSentence){
					String sent = opinion.getAnnotation("polarity");
					sentSentFreqs.put(sent, sentSentFreqs.getOrDefault(sent, 0)+1);
				}
				TreeMap<Integer, String> sortedPolarities = new TreeMap<>();
				for (String pol : sentSentFreqs.keySet()){
					sortedPolarities.put(sentSentFreqs.get(pol), pol);
				}
				
				deviatingOpinionsS += opinionsInSentence.size() - sortedPolarities.lastKey();
			}
		}
		
		TreeSet<Span> reviews = dataset.getSpans("review");
		HashMap<Integer, Integer> opinionsPerReview = new HashMap<>();
		int deviatingOpinionsR = 0;
		for (Span review : reviews){
			TreeSet<Span> opinionsInReview = review.getCoveredSpans(opinions);
			opinionsPerReview.put(opinionsInReview.size(), opinionsPerReview.getOrDefault(opinionsInReview.size(),0)+1);
			if (!opinionsInReview.isEmpty()){
				HashMap<String, Integer> sentSentFreqs = new HashMap<>();
				for (Span opinion : opinionsInReview){
					String sent = opinion.getAnnotation("polarity");
					sentSentFreqs.put(sent, sentSentFreqs.getOrDefault(sent, 0)+1);
				}
				TreeMap<Integer, String> sortedPolarities = new TreeMap<>();
				for (String pol : sentSentFreqs.keySet()){
					sortedPolarities.put(sentSentFreqs.get(pol), pol);
				}
//				Framework.log(sentSentFreqs.toString() + "\n" + sortedPolarities.toString());
				deviatingOpinionsR += opinionsInReview.size() - sortedPolarities.lastKey();
			}
		}
		
		// generate report
		Framework.log("\nNumber of reviews:\t"+reviews.size());
		Framework.log("\nNumber of sentences:\t"+sentences.size());
		Framework.log("\nNumber of opinions:\t"+opinions.size());
		
		Framework.log("\nSentiment Frequencies");
		for (String sent : sentimentFreqs.keySet()){
			Framework.log(sent + "\t" + sentimentFreqs.get(sent) + "\t" + ((double)sentimentFreqs.get(sent) / opinions.size()));
		}
		Framework.log("\nAspect Category Frequencies");
		for (String cat : categoryFreqs.keySet()){
			Framework.log(cat + "\t" + categoryFreqs.get(cat) + "\t" + ((double)categoryFreqs.get(cat) / opinions.size()));
		}
		Framework.log("\nOpinions per sentence");
		for (Integer i : opinionsPerSentence.keySet()){
			Framework.log(i + "\t" + opinionsPerSentence.get(i) + "\t" + ((double)opinionsPerSentence.get(i) / sentences.size()));
		}
		Framework.log("\nOpinions deviating from sentence majority sentiment:\t"+deviatingOpinionsS);
		Framework.log("\nOpinions per review");
		for (Integer i : opinionsPerReview.keySet()){
			Framework.log(i + "\t" + opinionsPerReview.get(i) + "\t" + ((double)opinionsPerReview.get(i) / reviews.size()));
		}
		Framework.log("\nOpinions deviating from review majority sentiment:\t"+deviatingOpinionsR);
	}

}
