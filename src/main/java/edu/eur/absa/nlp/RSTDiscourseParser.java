package edu.eur.absa.nlp;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeSet;

import org.clulab.discourse.rstparser.DiscourseTree;
import org.clulab.discourse.rstparser.RSTParser;
import org.clulab.discourse.rstparser.RelationDirection;
import org.clulab.processors.Document;
import org.clulab.processors.Processor;
import org.clulab.processors.Sentence;
import org.clulab.processors.corenlp.CoreNLPProcessor;
import org.clulab.processors.fastnlp.FastNLPProcessor;
import org.clulab.processors.shallownlp.ShallowNLPProcessor;
import org.json.JSONException;

import edu.eur.absa.Framework;
import edu.eur.absa.data.DatasetJSONReader;
import edu.eur.absa.data.DatasetJSONWriter;
import edu.eur.absa.model.Dataset;
import edu.eur.absa.model.Relation;
import edu.eur.absa.model.Span;
import edu.eur.absa.model.Word;
import edu.eur.absa.model.exceptions.IllegalSpanException;
//import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;

import scala.Enumeration;
import scala.Enumeration.Value;
import scala.Option;
import scala.collection.JavaConversions;

public class RSTDiscourseParser extends AbstractNLPComponent {

	//test
	public static void main(String[] args) throws ClassNotFoundException, JSONException, IllegalSpanException, IOException{
		Dataset dataset2016 =  (new DatasetJSONReader()).read(new File(Framework.DATA_PATH+"SemEval2016SB1Restaurants-Test.json"));
		dataset2016.process(new RSTDiscourseParser(), "review");
		(new DatasetJSONWriter()).write(dataset2016, new File(Framework.DATA_PATH+"SemEval2016SB1Restaurants-Test-RST.json"));
		(new DatasetJSONWriter(true)).write(dataset2016, new File(Framework.DATA_PATH+"SemEval2016SB1Restaurants-Test-RST.pretty.json"));
		
	}
		
	public RSTDiscourseParser(){
		thisTask = NLPTask.RST_PARSING;
		prerequisites.add(NLPTask.TOKENIZATION);
		prerequisites.add(NLPTask.SENTENCE_SPLITTING);
		
	}
	
	@Override
	public void validatedProcess(Dataset dataset, String spanType) {
		//spanType should be the document
		
		Processor proc = new CoreNLPProcessor(true, true, ShallowNLPProcessor.WITH_DISCOURSE(), 999);
	
		for (Span docSpan : dataset.getSpans(spanType)){
			TreeSet<Span> sentenceSpans = dataset.getSpans(docSpan, "sentence");
			ArrayList<String> textPerSentence = new ArrayList<>();
			HashMap<Integer, Span> sentencesByOrder = new HashMap<>();
			
			int i=0;
			for (Span sentenceSpan : sentenceSpans){		
				textPerSentence.add(sentenceSpan.getAnnotation("text"));
				sentencesByOrder.put(i, sentenceSpan);
				i++;
			}
			
			
			scala.collection.Iterable<String> sentences = JavaConversions.collectionAsScalaIterable(textPerSentence);

			Document annotatedDocument = proc.mkDocumentFromSentences(sentences, true, 1);
			proc.annotate(annotatedDocument);	
			
			Option<DiscourseTree> possibleTree = annotatedDocument.discourseTree();
			if (possibleTree.nonEmpty()) {
				//we have an RST tree
				
				DiscourseTree tree = possibleTree.get();
				System.out.println(tree.toString(true, true)+"\n\n=============================\n\n");
				
				processDiscourseTree(docSpan, sentencesByOrder, tree, null, "root", null);
			}
//			try {
//				(new DatasetJSONWriter(true)).write(dataset, new File(Framework.DATA_PATH+"SemEval2016SB2Restaurants-Training-RST.pretty.json"));
//			} catch(IOException e){
//				System.err.println("Could not save dataset");
//				e.printStackTrace();
//			}
//			System.exit(0);
		}
	}
	
	public void processDiscourseTree(Span document, HashMap<Integer, Span> sentencesByOrder, DiscourseTree tree, Span parentNode, String nodeType, String relationLabel){
		String relation = tree.relationLabel();
		
		String text = "";
		//nodes that span multiple sentence do not have this value set properly!
		if (tree.rawText() != null){
			text = tree.rawText();
		}
		
		int startSentence = tree.firstToken().sentence();
		int startWord = tree.firstToken().token();
		int endSentence = tree.lastToken().sentence();
		int endWord = tree.lastToken().token();
		
		
		if (startSentence > 0){
			startWord += sentencesByOrder.get(startSentence-1).last().getOrder()+1;
		}
		if (endSentence > 0){
			endWord += sentencesByOrder.get(endSentence-1).last().getOrder()+1;
		}
				
		Word start = document.getWordByOrder(startWord);
		Word end = document.getWordByOrder(endWord);
		
//		System.out.println("\n"+startSentence +"/"+startWord + " until " + endSentence + "/" + endWord + "\t" + tree.rawText());
//		System.out.println(tree.kind() + "\t" + relation + "\t" + tree.relationDirection());
//		System.out.println(start.getWord() + " until " + end.getWord());
		
		
		Span rstNode = null;
		try {
			rstNode = new Span("discoursePhrase", start, end);
			
		} catch (IllegalSpanException e){
			e.printStackTrace();
			System.exit(-1);
		}
		
		//Apparently, the parser says that all nodes are nuclei, which is obviously a bug somewhere in the library
		// so tree.kind() is always "Nucleus"
		rstNode.putAnnotation("type", nodeType);
		rstNode.putAnnotation("rawText", text);
		
		if (parentNode != null){
			//child node, link to parent span (and to nucleus if it is a satellite)
			
			
//			if (relationDirection.equals(RelationDirection.RightToLeft())){
//				//a sideways, horizontal relation (this is a satellite-nucleus relation)
//				//  that goes from nucleus (right) to satellite (left) 
//				//if there is already a child node below the parent, we are on the right and the satellite object
//				//  should have been created by now (because of depth-first traversal of discourse tree)
//				
//				//find span of satellite
//				TreeSet<Relation> existingChildren = parentNode.getRelations().getRelationsToChildren();
//				Framework.log("Children"+existingChildren);
//				if (!existingChildren.isEmpty()){
//					Span satellite = (Span) existingChildren.first().getChild();
//					Framework.log("New relation:\t"+new Relation(relationLabel, rstNode, satellite));
//				} else {
//					rstNode.putAnnotation("type", "satellite");
//				}
//			}
//			if (relationDirection.equals(RelationDirection.LeftToRight())){
//				//a sideways, horizontal relation (this is a satellite-nucleus relation)
//				//  that goes from nucleus (left) to satellite (right)
//				//if there is already a child node below the parent, we are on the right and the nucleus object
//				//  should have been created by now (because of depth-first traversal of discourse tree)
//				
//				//find span of nucleus
//				TreeSet<Relation> existingChildren = parentNode.getRelations().getRelationsToChildren();
//				Framework.log("Children: "+existingChildren);
//				if (!existingChildren.isEmpty()){
//					Span nucleus = (Span) existingChildren.first().getChild();
//					Framework.log("New relation:\t"+new Relation(relationLabel, nucleus, rstNode));
//					rstNode.putAnnotation("type", "satellite");
//				} else {
//					
//				}
//			}
			
			TreeSet<Relation> existingChildren = parentNode.getRelations().getAllRelationsToChildren();
			if (!existingChildren.isEmpty()){
				Framework.log("Children: "+existingChildren);
				Span otherNode = (Span) existingChildren.first().getChild();
				//if there is already a childnode under the parent and either that node or the current node
				//  is a satellite, we can create a sideways, labeled, relation
				//sideways relations go from the nucleus (parent) to the satellite (child)
				if (rstNode.getAnnotation("type",String.class).equalsIgnoreCase("satellite")){
					(new Relation("rstRelation", otherNode, rstNode)).putAnnotation("type", relationLabel);
				}
				if(otherNode.getAnnotation("type",String.class).equalsIgnoreCase("satellite")){
					(new Relation("rstRelation", rstNode, otherNode)).putAnnotation("type", relationLabel);
				}
			}
			
			
			//include a relation from the parent node to the child node anyways
			if (rstNode.getAnnotation("type",String.class).equalsIgnoreCase("nucleus")){
				new Relation("rstNucleus",parentNode,rstNode);	
			} else if(rstNode.getAnnotation("type",String.class).equalsIgnoreCase("satellite")) {
				new Relation("rstSatellite",parentNode,rstNode);	
			} else {
				System.err.println("Unknown RST node kind");
				System.exit(-1);
			}
			
		} else {
			//root node -> link to textual unit span
			rstNode.putAnnotation("type", "root");
		}
		
		Framework.log(rstNode.toString());
		
		if (!tree.isTerminal()){
			boolean firstChild = true;
			for (DiscourseTree childTree : tree.children()){
				String childNodeType="nucleus";
				if (!firstChild && tree.relationDirection().equals(RelationDirection.LeftToRight()) ||
						firstChild && tree.relationDirection().equals(RelationDirection.RightToLeft())){
					childNodeType = "satellite";
				}
				firstChild=false;
				processDiscourseTree(document, sentencesByOrder, childTree, rstNode, childNodeType, tree.relationLabel());
			}
		} else {
			//terminal leaf in discourse tree
			// let's add a relation between each word and its direct containing RST leaf
			// this should make it easier to go from a word to the rst tree
			for (Word w : rstNode){
				new Relation("directRSTLeaf", w, rstNode);
			}
		}
	}

}


