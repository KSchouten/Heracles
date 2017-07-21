package edu.eur.absa.nlp;

import java.util.HashMap;
import java.util.Properties;

import edu.eur.absa.Framework;
import edu.eur.absa.model.DataEntity;
import edu.eur.absa.model.Dataset;
import edu.eur.absa.model.Relation;
import edu.eur.absa.model.Span;
import edu.eur.absa.model.Word;
import edu.eur.absa.nlp.tools.CoreNLPHelper;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.EnhancedDependenciesAnnotation;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.EnhancedPlusPlusDependenciesAnnotation;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TypedDependency;
import edu.stanford.nlp.trees.TreeCoreAnnotations.TreeAnnotation;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.TypesafeMap;

public class CoreNLPDependencyParser extends AbstractNLPComponent {

	public CoreNLPDependencyParser() {
		thisTask = NLPTask.DEP_PARSING;
		prerequisites.add(NLPTask.TOKENIZATION);
		prerequisites.add(NLPTask.SENTENCE_SPLITTING);
		prerequisites.add(NLPTask.POS_TAGGING);
	}

	@Override
	public void validatedProcess(Dataset dataset, String spanTypeOfSentenceUnit) {
		Properties prop1 = new Properties();
		prop1.setProperty("annotators", "depparse");
		
		StanfordCoreNLP pipeline = new StanfordCoreNLP(prop1, false);
		
		for (Span span : dataset.getSpans(spanTypeOfSentenceUnit)){

			
			HashMap<Integer, Word> wordIndex = new HashMap<>();
			Annotation a = CoreNLPHelper.reconstructStanfordAnnotations(span, wordIndex);
			
			
//			Main.debug(span.toString());
			
			pipeline.annotate(a);
						
			for (CoreMap sentence : a.get(SentencesAnnotation.class)){
								
				//per sentence, get the dependencies
				SemanticGraph dependencies = sentence.get(EnhancedPlusPlusDependenciesAnnotation.class);
				
				for (TypedDependency td : dependencies.typedDependencies()){
//					Main.debug(td.toString());
					String relationType = td.reln().getLongName();
					Word dep = wordIndex.get(td.dep().beginPosition());
					DataEntity gov = wordIndex.get(td.gov().beginPosition());
					if (gov == null){
						//this is the root, link to sentence
						gov = span;
					}
					if (dep == null || gov == null){
						Framework.debug(td.toString());
						Framework.debug(td.dep().beginPosition() + "\t" + td.gov().beginPosition());
						Framework.debug(wordIndex.toString());
					}
					Relation rel = new Relation("deps", gov, dep);
					rel.putAnnotation("relationLongName", td.reln().getLongName());
					if (td.reln().getParent() != null)
						rel.putAnnotation("relationParentShortName", td.reln().getParent().getShortName());
					rel.putAnnotation("relationShortName", td.reln().getShortName());
//					rel.putAnnotation("relationSpecific", td.reln().getSpecific());
					dep.getRelations().addRelationToParent(rel);
					gov.getRelations().addRelationToChild(rel);
					
				}				
//				dependencies.prettyPrint();
			}
			
		}

	}

}
