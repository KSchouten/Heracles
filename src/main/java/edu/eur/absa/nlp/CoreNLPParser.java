package edu.eur.absa.nlp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import org.ejml.simple.SimpleMatrix;

import edu.eur.absa.Framework;
import edu.eur.absa.model.Dataset;
import edu.eur.absa.model.Relation;
import edu.eur.absa.model.Span;
import edu.eur.absa.model.Word;
import edu.eur.absa.model.exceptions.IllegalSpanException;
import edu.eur.absa.nlp.tools.CoreNLPHelper;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.neural.rnn.RNNCoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.trees.HeadFinder;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations.TreeAnnotation;
import edu.stanford.nlp.trees.UniversalSemanticHeadFinder;
import edu.stanford.nlp.util.CoreMap;

public class CoreNLPParser extends AbstractNLPComponent {

	private static HeadFinder headFinder = new UniversalSemanticHeadFinder();
	
	public CoreNLPParser() {
		thisTask = NLPTask.PARSING;
		prerequisites.add(NLPTask.TOKENIZATION);
		prerequisites.add(NLPTask.SENTENCE_SPLITTING);
	}

	@Override
	public void validatedProcess(Dataset dataset, String spanTypeOfSentenceUnit) {
		Properties prop1 = new Properties();
		prop1.setProperty("annotators", "parse");
		StanfordCoreNLP pipeline = new StanfordCoreNLP(prop1, false);
		
		for (Span span : dataset.getSpans(spanTypeOfSentenceUnit)){

			
			HashMap<Integer, Word> wordIndex = new HashMap<>();
			Annotation a = CoreNLPHelper.reconstructStanfordAnnotations(span, wordIndex);
//			Annotation a = new Annotation((String)span.getAnnotations().get("text"));
			
			if (a == null){
				System.out.println(a);
			}
			pipeline.annotate(a);
			for (CoreMap sentence : a.get(SentencesAnnotation.class)){
				//per sentence, check the syntax tree
				Tree tree = sentence.get(TreeAnnotation.class);
//				tree.percolateHeadAnnotations(headFinder);
//				tree.indentedListPrint();
				
				try {
					analyzeTree(tree, span, wordIndex);
				} catch (IllegalSpanException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
			
		}

	}
	
	private static void analyzeTree(Tree tree, Span parent, HashMap<Integer, Word> wordIndex) throws IllegalSpanException{
		for (Tree t : tree.getChildrenAsList()){
			String type = "";
			if (t.isLeaf())
				type = "{Leaf}";
			if (t.isPreTerminal())
				type = "{Preterminal}";
			if (t.isPhrasal()){
				if (t.isPrePreTerminal()){
					type = "{pre-Preterminal}";
				} else {
					type = "{Phrasal}";
				}
			}
//			Main.debug(t.label().toString() + "\t" + t.yieldWords().toString() + "\t"+type);
			//if this node is preterminal, then this is basically the POS tag, with one child: the word leaf with the word text
			//this is not useful for the syntax tree and only bloats the thing in memory and on file
			if (t.isPreTerminal())
				continue;
			
			
			
			
			Tree headLeaf = t.headTerminal(headFinder);
//			Main.debug(headLeaf.label().toString() + "\t" + headLeaf.yieldWords().toString());
						
			ArrayList<edu.stanford.nlp.ling.Word> stanfordWords = t.yieldWords();
			Word begin = wordIndex.get( stanfordWords.get(0).beginPosition() );
			Word end = wordIndex.get( stanfordWords.get(stanfordWords.size()-1).beginPosition() );
			
			Span span = new Span("syntacticPhrase", begin, end);
			span.putAnnotation("pos", t.label().toString());
			
			String fullText = (String)parent.getTextualUnit().getAnnotation("text");
			if (end.getEndOffset()>fullText.length()){
				Framework.debug(fullText.length() + "\t" + fullText);
				Framework.debug(begin.getWord() + "\t" + begin.getStartOffset() + "\t" + end.getWord() + "\t" + end.getEndOffset());
				Framework.debug(t.label().toString() + "\t" + t.yieldWords().toString() + "\t"+type);
				
			}
			String spanText = fullText.substring(begin.getStartOffset(), end.getEndOffset());
			span.putAnnotation("text", spanText);
			
			Word syntacticHead = wordIndex.get( headLeaf.yieldWords().get(0).beginPosition());
			
			new Relation("syntacticHead", syntacticHead, span);
			
//			Main.debug(spanText);
			
//			SyntacticConstituent constit = new SyntacticConstituent(t.getSpan().getSource(), t.getSpan().getTarget(), t.label().toString(), parent);
//			Span span = new Span()
//			SimpleMatrix sm = RNNCoreAnnotations.getPredictions(tree);
//			for (int i = 0; i < 5; i++){
//				constit.addSentimentScore(sm.get(i, 0));
//			}
			
			
//			System.out.print(t.label());
//			System.out.print("\t" + t.numChildren());
//			System.out.print("\t" + t.getSpan());
//			System.out.print("\t" + t.labels());
//			System.out.println();
//			System.out.println("Predictions: " + RNNCoreAnnotations.getPredictions(tree));
//			System.out.println("Predicted Class: " + RNNCoreAnnotations.getPredictedClass(tree));
			
//			System.out.println("Node Vector: " + RNNCoreAnnotations.getNodeVector(tree));
//			t.indentedListPrint();
//			System.out.println("");
			if (t.isPhrasal() && !t.isPrePreTerminal()){
				analyzeTree(t, span, wordIndex);
			}
		}
	}

}
