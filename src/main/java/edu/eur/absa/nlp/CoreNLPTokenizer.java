package edu.eur.absa.nlp;


import java.util.List;
import java.util.Properties;

import edu.eur.absa.Framework;
import edu.eur.absa.model.Dataset;
import edu.eur.absa.model.Span;
import edu.eur.absa.model.Word;
import edu.stanford.nlp.ling.CoreAnnotations.CharacterOffsetBeginAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.OriginalTextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;


public class CoreNLPTokenizer extends AbstractNLPComponent{

	private boolean processInOrder = false;

	public CoreNLPTokenizer(){
		thisTask = NLPTask.TOKENIZATION;
	}
	
	public CoreNLPTokenizer(boolean processInOrder){
		this();
		this.processInOrder = processInOrder;
	}
	
	/**
	 * Process the Dataset in chunks, as defined by the <code>spanType</code> parameter.
	 * The Spans denoted by spanType must provide an annotation of type "text".
	 * This spanType does not have to be textual unit.
	 */
	@Override
	public void validatedProcess(Dataset dataset, String spanTypeOfTextUnit){
		if (dataset.getPerformedNLPTasks().contains(getTask())){
			Framework.error("This dataset has already been tokenized.");
			return;
		}
		
		Properties prop1 = new Properties();
		prop1.setProperty("annotators", "tokenize");
		//prop1.setProperty("options", "splitHyphenated=true");
		StanfordCoreNLP pipeline = new StanfordCoreNLP(prop1, false);
		
		
		for (Span span : dataset.getSpans(spanTypeOfTextUnit)){
			Span textualUnit = span.getTextualUnit();
			String originalText = span.getAnnotation("text", String.class); 
			Annotation a = new Annotation(originalText);
			pipeline.annotate(a);
			List<CoreLabel> tokenAnnotations = a.get(TokensAnnotation.class);
			Word previousWord = null;
			if (!textualUnit.isEmpty())
				previousWord = textualUnit.last();
				
			for (CoreLabel token : tokenAnnotations){
				String word = token.get(OriginalTextAnnotation.class);
				int startOffset = token.get(CharacterOffsetBeginAnnotation.class);
//				int endOffset = token.get(CharacterOffsetEndAnnotation.class);
//				System.out.println(word + "\t" + startOffset + "\t" + endOffset);
				if (previousWord == null){
					previousWord = new Word(word, startOffset, textualUnit, dataset);
				} else {
					previousWord = new Word(word, startOffset, previousWord);
				}
				//and add the new word to the sentence span. If span=textualSpan than this has no effect
				if (!textualUnit.equals(span))
					span.add(previousWord);
			}
		}		
	}

}
