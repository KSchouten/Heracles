package edu.eur.absa.nlp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.TreeSet;

import org.ejml.simple.SimpleMatrix;

import edu.eur.absa.Framework;
import edu.eur.absa.model.DataEntity;
import edu.eur.absa.model.Dataset;
import edu.eur.absa.model.Span;
import edu.eur.absa.model.Word;
import edu.eur.absa.model.exceptions.IllegalSpanException;
import edu.eur.absa.nlp.tools.CoreNLPHelper;
import edu.stanford.nlp.ling.CoreAnnotations.CharacterOffsetBeginAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.neural.rnn.RNNCoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations;
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations.SentimentAnnotatedTree;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations.TreeAnnotation;
import edu.stanford.nlp.util.CoreMap;

/**
 * Computes the sentiment values for words and phrases using the Stanford CoreNLP Sentiment annotator
 * This component depends on the Tree annotations from the CoreNLP Parser
 * TODO: Currently, the parser is simply run (again) to get these Tree annotations, but in the future it would probably
 * be better to reconstruct these objects from our own data model
 * 
 * @author Kim Schouten
 *
 */
public class CoreNLPSentimentAnnotator extends AbstractNLPComponent {

	public CoreNLPSentimentAnnotator() {
		thisTask = NLPTask.STANFORD_SENTIMENT_ANNOTATING;
		prerequisites.add(NLPTask.POS_TAGGING);
		prerequisites.add(NLPTask.PARSING);
	}

	@Override
	public void validatedProcess(Dataset dataset, String spanTypeOfSentenceUnit) {

		Properties prop1 = new Properties();
		prop1.setProperty("annotators", "parse sentiment");
		StanfordCoreNLP pipeline = new StanfordCoreNLP(prop1, false);
		
		for (Span span : dataset.getSpans(spanTypeOfSentenceUnit)){

			
			HashMap<Integer, Word> wordIndex = new HashMap<>();
			Annotation a = CoreNLPHelper.reconstructStanfordAnnotations(span, wordIndex, true);
			pipeline.annotate(a);
			
			for (CoreMap sentence : a.get(SentencesAnnotation.class)){
				Tree sentimentTree = sentence.get(SentimentAnnotatedTree.class);
				sentimentTree.setSpans();
				sentimentTree.indexLeaves();
				sentimentTree.indexSpans();
				sentimentTree.percolateHeadIndices();
//				for (CoreLabel cl : sentimentTree.taggedLabeledYield()){
//					Main.debug(""+cl.beginPosition()+"\t"+cl.get(CharacterOffsetBeginAnnotation.class));
//					Main.debug(cl.index() + "\t" + cl.keySet());
//				}
				
//				sentimentTree.indentedListPrint();
				
//				sentence.get(TreeAnnotation.class).indentedListPrint();
				
				SimpleMatrix sm = RNNCoreAnnotations.getPredictions(sentimentTree);
				assignSentiment(span, sm, "phraseSentiment");
//				Main.debug(sm.toString());
				
//				//assign begin positions to each word in the tree because those seem to be missing
//				int order = 0;
//				ArrayList<edu.stanford.nlp.ling.Word> stanfordWords = sentimentTree.yieldWords();
//				for (Word w : span){
//					stanfordWords.get(order).setBeginPosition(w.getStartOffset());
//					order++;
//				}
				
				try {
					analyzeTree(sentimentTree, span, wordIndex,0);
				} catch (IllegalSpanException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
		}
	}
	
	private static void analyzeTree(Tree tree, Span sentenceSpan, HashMap<Integer, Word> wordIndex, int coveredWords) throws IllegalSpanException{
		
		for (Tree t : tree.getChildrenAsList()){
			SimpleMatrix sm = RNNCoreAnnotations.getPredictions(tree);
			
//			List<edu.stanford.nlp.ling.CoreLabel> stanfordWords = t.taggedLabeledYield();
			ArrayList<edu.stanford.nlp.ling.Word> stanfordWords = t.yieldWords();
			
//			Main.debug("Words: "+stanfordWords.toString());
			Word begin = wordIndex.get( sentenceSpan.first().getOrder()+coveredWords );
			Word end = wordIndex.get( sentenceSpan.first().getOrder()+coveredWords+stanfordWords.size()-1 );
			
//			Main.debug("Words: "+begin.getWord() + " - " + end.getWord());
			if (t.isPreTerminal()){
				//this node has only the POS tag of the node below it, which is the lead word
				//add the sentiment values as an annotation to the leaf word
				assignSentiment(begin, sm, "phraseSentiment");
			}
			
			if (t.isPhrasal()){
				//try to match this phrase with one already in the dataset and get the Span
				//hopefully the parser tree and the sentiment tree line up...
				//when Span is found, add the sentiment values as an annotation to it
				String pos = t.label().toString().replaceAll("--?[\\d]*", "");
				Dataset dataset = sentenceSpan.getDataset();
				TreeSet<Span> candidateSpans = dataset.getSpans("syntacticPhrase", begin);
				boolean found = false;
				Span phraseSpan=null;
				for (Span candidate : candidateSpans){
					if (candidate.last().equals(end) && candidate.getAnnotation("pos").equals(pos)){
						phraseSpan = candidate;
						assignSentiment(candidate, sm, "phraseSentiment");
						found =true;
					}
				}
				
				if (!found){
//					Main.debug("No matching Span");
//					Main.debug("candidateSpans: "+candidateSpans);
//					Main.debug(pos + "\t" + begin.getWord() + "\t" + end.getWord());
				}
//				if (t.isPhrasal() && !t.isPrePreTerminal()){
//					if (phraseSpan != null){
//						analyzeTree(t, phraseSpan, wordIndex, coveredWords);
//					} else {
//						analyzeTree(t, parent, wordIndex, coveredWords);
//					}
//				}
			}
			if (t.isLeaf()){
				//this is a single word -> we can add the sentiment values as an annotation to it
				assignSentiment(begin, sm, "wordSentiment");
			}
			
			if (!t.isLeaf())
				analyzeTree(t, sentenceSpan, wordIndex, coveredWords);
			
			
			coveredWords += stanfordWords.size();
			
		}
	}

	private static void assignSentiment(DataEntity a, SimpleMatrix sm, String sentimentLabel){
		ArrayList<Double> sentimentScores = new ArrayList<>();
		for (int i = 0; i < 5; i++){
			sentimentScores.add(sm.get(i, 0));
		}
		a.putAnnotation(sentimentLabel, sentimentScores);
	}
	
}
