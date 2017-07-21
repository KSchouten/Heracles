package edu.eur.absa.nlp.tools;

import java.util.HashMap;
import java.util.Properties;

import edu.eur.absa.Framework;
import edu.eur.absa.model.Dataset;
import edu.eur.absa.model.Span;
import edu.eur.absa.model.Word;
import edu.eur.absa.nlp.AbstractNLPComponent;
import edu.eur.absa.nlp.NLPTask;
import edu.eur.absa.nlp.tools.CoreNLPHelper;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.CharacterOffsetBeginAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.EnhancedDependenciesAnnotation;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.EnhancedPlusPlusDependenciesAnnotation;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations.TreeAnnotation;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.TypesafeMap;

public class CoreNLPAnnotationViewer extends AbstractNLPComponent {

	public CoreNLPAnnotationViewer() {
	}

	@Override
	public void validatedProcess(Dataset dataset, String spanTypeOfSentenceUnit) {
		Properties prop1 = new Properties();
		//do whatever Stanford task that you want to view the labels for
		prop1.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner, parse, dcoref, mention");
		prop1.setProperty("coref.algorithm","neural");
		
		StanfordCoreNLP pipeline = new StanfordCoreNLP(prop1, false);
		
		for (Span span : dataset.getSpans(spanTypeOfSentenceUnit)){

			
			HashMap<Integer, Word> wordIndex = new HashMap<>();
//			Annotation a = CoreNLPHelper.reconstructStanfordAnnotations(span, wordIndex);
			Annotation a = new Annotation((String)span.getAnnotation("text"));
			
			Framework.debug(span.toString());
			
			pipeline.annotate(a);
			//check doc level annotations
			Framework.debug("\nDoc level annotations:");
			for (Class<?> ann : a.keySet()){
				Framework.debug(ann + "\t" + a.get((Class<? extends TypesafeMap.Key<?>>)ann));
			}
			
			for (CoreMap sentence : a.get(SentencesAnnotation.class)){
				Framework.debug("\nSentence level annotations:");
				for (Class<?> ann : sentence.keySet()){
					Framework.debug(ann + "\t" + sentence.get((Class<? extends TypesafeMap.Key<?>>)ann));
				}	
				
				for (CoreLabel token: sentence.get(TokensAnnotation.class)) {
					Framework.debug("\nToken level annotations:");
					for (Class<?> ann : token.keySet()){
						Framework.debug(ann + "\t" + token.get((Class<? extends TypesafeMap.Key<?>>)ann));
					}
					
				}
				
				//per sentence, get the dependencies
//				SemanticGraph dependencies = sentence.get(EnhancedPlusPlusDependenciesAnnotation.class);
//				SemanticGraph dependencies = sentence.get(EnhancedDependenciesAnnotation.class);
//				dependencies.prettyPrint();
			}
			
		}

	}

}
