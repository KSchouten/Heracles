package edu.eur.absa.nlp;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Properties;
import java.util.TreeSet;

import org.json.JSONException;

import edu.eur.absa.Framework;
import edu.eur.absa.data.DatasetJSONReader;
import edu.eur.absa.model.Dataset;
import edu.eur.absa.model.Span;
import edu.eur.absa.model.Word;
import edu.eur.absa.model.exceptions.IllegalSpanException;
import edu.eur.absa.nlp.tools.CoreNLPHelper;
import edu.stanford.nlp.coref.CorefCoreAnnotations;
import edu.stanford.nlp.coref.data.CorefChain;
import edu.stanford.nlp.coref.data.Mention;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;


/**
 * A non-finished Coreference Resolution component. It is not finished (only prints out Stanford output) since it does not seem to
 *  be very useful for the restaurant data: not enough useful relations and too many errors
 * @author Kim Schouten
 *
 */
public class CoreNLPCorefResolver extends AbstractNLPComponent {

	//test
	public static void main(String[] args) throws ClassNotFoundException, JSONException, IllegalSpanException, IOException{
		Dataset dataset =  (new DatasetJSONReader()).read(new File(Framework.DATA_PATH+"SemEval2016SB1Restaurants-Training.json"));
		dataset.process(new CoreNLPCorefResolver(), "sentence");
	}
	
	public CoreNLPCorefResolver() {
		thisTask = NLPTask.COREF_RESOLUTION;
		prerequisites.add(NLPTask.POS_TAGGING);
		prerequisites.add(NLPTask.NER);
		prerequisites.add(NLPTask.PARSING);
	}
	
	@Override
	public void validatedProcess(Dataset dataset, String spanTypeOfTextualUnit) {
		Properties prop1 = new Properties();
//		prop1.setProperty("annotators", "parse dcoref");
		prop1.setProperty("annotators", "tokenize,ssplit,pos,lemma,ner,parse,mention,coref");
		prop1.setProperty("coref.algorithm", "neural");
		StanfordCoreNLP pipeline = new StanfordCoreNLP(prop1, false);
		
//		Annotation document = new Annotation("Barack Obama was born in Hawaii.  He is the president. Obama was elected in 2008.");
		
		for (Span span : dataset.getSpans(spanTypeOfTextualUnit)){
//			TreeSet<Span> sentences = span.getDataset().getSpans(span, "sentence");
////			Framework.log("Sentences: "+sentences);
//			String reviewTextCorrected = "";
//			for (Span sentence : sentences){
//				reviewTextCorrected += " " + sentence.getAnnotations().get("text");
//			}
//			reviewTextCorrected = reviewTextCorrected.trim().replaceAll("  "," ");
			Framework.log(span.getAnnotation("text"));
			
			HashMap<Integer, Word> wordIndex = new HashMap<>();
			Annotation a = new Annotation(span.getAnnotation("text", String.class));
//			Annotation a = CoreNLPHelper.reconstructStanfordAnnotations(span, wordIndex);
			
			pipeline.annotate(a);
		
			System.out.println("coref chains");
		    for (CorefChain cc : a.get(CorefCoreAnnotations.CorefChainAnnotation.class).values()) {
		      System.out.println("\t" + cc);
		    }
//		    for (CoreMap sentence : a.get(CoreAnnotations.SentencesAnnotation.class)) {
//		      System.out.println("---");
//		      System.out.println("mentions");
//		      for (Mention m : sentence.get(CorefCoreAnnotations.CorefMentionsAnnotation.class)) {
//		        System.out.println("\t" + m);
//		       }
//		    }
		}
	}

}
