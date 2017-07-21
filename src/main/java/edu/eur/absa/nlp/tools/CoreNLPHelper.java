package edu.eur.absa.nlp.tools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import edu.eur.absa.model.Span;
import edu.eur.absa.model.Word;
import edu.stanford.nlp.ling.CoreAnnotations.DocIDAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.IndexAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.NormalizedNamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.OriginalTextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentenceIndexAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokenBeginAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokenEndAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.ValueAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.WordPositionAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.CharacterOffsetBeginAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.CharacterOffsetEndAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.ArrayCoreMap;
import edu.stanford.nlp.util.CoreMap;

public class CoreNLPHelper {

	public CoreNLPHelper() {
		// TODO Auto-generated constructor stub
	}
	
	public static Annotation reconstructStanfordAnnotations(Span sentenceSpan, HashMap<Integer, Word> wordIndex){
		return reconstructStanfordAnnotations(sentenceSpan, wordIndex, false);
	}
	public static Annotation reconstructStanfordAnnotations(Span sentenceSpan, HashMap<Integer, Word> wordIndex, boolean useWordOrderInsteadOfOffset){
		String originalText = sentenceSpan.getAnnotation("text", String.class); 
		Annotation a = new Annotation(originalText);
		a.set(TextAnnotation.class, originalText);
		
		//a.set(DocIDAnnotation.class, "document");
		
		List<CoreMap> sentenceAnnotations = new ArrayList<CoreMap>();
		a.set(SentencesAnnotation.class, sentenceAnnotations);
		List<CoreLabel> tokenAnnotations = new ArrayList<CoreLabel>();
		a.set(TokensAnnotation.class, tokenAnnotations);
		
		ArrayCoreMap sentenceAnnotation = new ArrayCoreMap();
		sentenceAnnotations.add(sentenceAnnotation);
		
//		int startOffset = sentenceSpan.first().getStartOffset();
		
		for (Word w : sentenceSpan){
			CoreLabel c = new CoreLabel();
			c.set(TextAnnotation.class, w.getWord());
			c.set(OriginalTextAnnotation.class, w.getWord());
			c.set(ValueAnnotation.class, w.getWord());
			c.set(CharacterOffsetBeginAnnotation.class, w.getStartOffset());
			c.set(CharacterOffsetEndAnnotation.class, w.getEndOffset());
			
			
			c.set(IndexAnnotation.class, w.getOrder()+1);
//			c.setIndex(w.getOrder());
			
			c.set(SentenceIndexAnnotation.class, 0);
//			c.setSentIndex(0);
			
			c.set(DocIDAnnotation.class, "document");
			c.setDocID("document");
			
			if (w.hasAnnotation("pos"))
				c.set(PartOfSpeechAnnotation.class, w.getAnnotation("pos",String.class));
			
			if (w.hasAnnotation("lemma"))
				c.set(LemmaAnnotation.class, w.getAnnotation("lemma", String.class));
			
			if (w.hasAnnotation("nerLabel"))
				c.set(NamedEntityTagAnnotation.class, w.getAnnotation("nerLabel", String.class));
			
			if (w.hasAnnotation("nerValue"))
				c.set(NormalizedNamedEntityTagAnnotation.class, w.getAnnotation("nerValue", String.class));
			
			tokenAnnotations.add(c);
			if (useWordOrderInsteadOfOffset){
				wordIndex.put(w.getOrder(), w);
			} else {
				wordIndex.put(w.getStartOffset(), w);
			}
		}
		//essential sentence annotation: TokensAnnotation
		sentenceAnnotation.set(TokensAnnotation.class, tokenAnnotations);
		//essential sentence annotation: TextAnnotation
		sentenceAnnotation.set(TextAnnotation.class, originalText);
		//essential sentence annotation: SentenceIndexAnnotation
		sentenceAnnotation.set(SentenceIndexAnnotation.class, 0);
		
		sentenceAnnotation.set(CharacterOffsetBeginAnnotation.class, 0);
		sentenceAnnotation.set(CharacterOffsetEndAnnotation.class, sentenceSpan.last().getEndOffset());
		sentenceAnnotation.set(TokenBeginAnnotation.class, 0);
		sentenceAnnotation.set(TokenEndAnnotation.class, sentenceSpan.last().getOrder());
		
		return a;
	}

}
