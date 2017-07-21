package edu.eur.absa.nlp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;

import org.jfree.util.Log;

import ch.qos.logback.classic.Logger;
import edu.eur.absa.Framework;
import edu.eur.absa.model.Dataset;
import edu.eur.absa.model.Span;
import edu.eur.absa.model.Word;
import edu.eur.absa.nlp.tools.CoreNLPHelper;
import edu.stanford.nlp.ling.CoreAnnotations.CharacterOffsetBeginAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.CharacterOffsetEndAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.NormalizedNamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.ArrayCoreMap;
import edu.stanford.nlp.util.CoreMap;

public class CoreNLPNamedEntityRecognizer extends AbstractNLPComponent{


	public CoreNLPNamedEntityRecognizer(){
		thisTask = NLPTask.NER;
		prerequisites.add(NLPTask.TOKENIZATION);
		prerequisites.add(NLPTask.SENTENCE_SPLITTING);
		prerequisites.add(NLPTask.POS_TAGGING);
		prerequisites.add(NLPTask.LEMMATIZATION);
	}
	
	/**
	 * Process the Dataset in chunks, as defined by the <code>spanType</code> parameter.
	 * The Spans denoted by spanType must each contain Words belonging to a single sentence.
	 * 
	 */
	@Override
	public void validatedProcess(Dataset dataset, String spanTypeOfSentenceUnit){
		
		
		
		Properties prop1 = new Properties();
		prop1.setProperty("annotators", "ner");
		StanfordCoreNLP pipeline = new StanfordCoreNLP(prop1, false);
		
		for (Span span : dataset.getSpans(spanTypeOfSentenceUnit)){

			
			HashMap<Integer, Word> wordIndex = new HashMap<>();
			Annotation a = CoreNLPHelper.reconstructStanfordAnnotations(span, wordIndex);
			if (a == null){
				System.out.println(a);
			}
			pipeline.annotate(a);
			List<CoreMap> sentenceAnnotations = a.get(SentencesAnnotation.class);
			for (CoreMap sentence : sentenceAnnotations){
				for (CoreLabel token: sentence.get(TokensAnnotation.class)) {
					
					Word w = wordIndex.get(token.get(CharacterOffsetBeginAnnotation.class));
					String ner = token.get(NamedEntityTagAnnotation.class);
					String nerValue = token.get(NormalizedNamedEntityTagAnnotation.class);
					if (ner!=null)
						w.putAnnotation("nerLabel", ner);
					if (nerValue!=null)
						w.putAnnotation("nerValue", nerValue);
					
//					System.out.println(w.getAnnotations());
				}
			

				
				
			}
		}		
	}


	



}
