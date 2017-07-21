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
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.ArrayCoreMap;
import edu.stanford.nlp.util.CoreMap;

public class CoreNLPPosTagger extends AbstractNLPComponent{

	
	private boolean useOntologyLookupForNNP;
	
	public CoreNLPPosTagger(){
		thisTask = NLPTask.POS_TAGGING;
		prerequisites.add(NLPTask.TOKENIZATION);
		prerequisites.add(NLPTask.SENTENCE_SPLITTING);
		this.overwritePreviousRun = true;
	}
	
	public CoreNLPPosTagger(boolean useOntologyLookupForNNP){
		this();
		this.useOntologyLookupForNNP = useOntologyLookupForNNP;
	}
	
	
	
	/**
	 * Process the Dataset in chunks, as defined by the <code>spanType</code> parameter.
	 * The Spans denoted by spanType must each contain Words belonging to a single sentence.
	 * 
	 */
	@Override
	public void validatedProcess(Dataset dataset, String spanTypeOfSentenceUnit){
//		if (dataset.getPerformedNLPTasks().contains(getTask())){
//			Framework.error("This dataset has already been tagged with POS.");
//			return;
//		}
		//check if prerequisites are satisfied
		if (!dataset.getPerformedNLPTasks().containsAll(prerequisites)){
			HashSet<NLPTask> missingTasks = new HashSet<>();
			missingTasks.addAll(prerequisites);
			missingTasks.removeAll(dataset.getPerformedNLPTasks());
			Framework.error("This dataset does not meet the requirements to use this component! Missing tasks: " + missingTasks);
			return;
		}
		
		Properties prop1 = new Properties();
		prop1.setProperty("annotators", "pos");
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
					String tempPos = token.get(PartOfSpeechAnnotation.class);
					if (w.hasAnnotation("URI")){
						w.putAnnotation("pos", "NNP");
					} else {
						w.putAnnotation("pos", tempPos);
					}
//					System.out.println(w.getAnnotations());
				}
			

				
				
			}
		}		
	}
	
//	private Annotation reconstructStanfordAnnotations(Span sentenceSpan, HashMap<Integer, Word> wordIndex){
//		String originalText = sentenceSpan.getAnnotations().get("text", String.class); 
//		Annotation a = new Annotation(originalText);
//		List<CoreMap> sentenceAnnotations = new ArrayList<CoreMap>();
//		a.set(SentencesAnnotation.class, sentenceAnnotations);
//		ArrayCoreMap sentenceAnnotation = new ArrayCoreMap();
//		sentenceAnnotations.add(sentenceAnnotation);
//		List<CoreLabel> tokenAnnotations = new ArrayList<CoreLabel>();
//		for (Word w : sentenceSpan){
//			CoreLabel c = new CoreLabel();
//			c.set(TextAnnotation.class, w.getWord());
//			c.set(CharacterOffsetBeginAnnotation.class, w.getStartOffset());
//			c.set(CharacterOffsetEndAnnotation.class, w.getEndOffset());
//			tokenAnnotations.add(c);
//			wordIndex.put(w.getStartOffset(), w);
//		}
//		sentenceAnnotation.set(TokensAnnotation.class, tokenAnnotations);
//		return a;
//	}
	

	
	
	
	



}
