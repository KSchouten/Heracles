package edu.eur.absa.nlp;

import java.util.HashMap;
import java.util.HashSet;

import edu.eur.absa.Framework;
import edu.eur.absa.external.IOntology;
import edu.eur.absa.model.Dataset;
import edu.eur.absa.model.Span;
import edu.eur.absa.model.Word;

public class OntologyLookup extends AbstractNLPComponent {

	private IOntology ont;
	private String spanAnnotationType;
	
	public OntologyLookup(String spanAnnotationType, IOntology ont){
		this.ont = ont;
		this.thisTask = NLPTask.ONTOLOGY_LOOKUP;
		this.prerequisites.add(NLPTask.TOKENIZATION);
		this.spanAnnotationType = spanAnnotationType;
		this.overwritePreviousRun = true;
	}
	
	@Override
	public void validatedProcess(Dataset dataset, String textualUnitSpanType) {
		// put ontology lexes with classes in HashMap
//		Framework.log("Amazing: "+ont.getConceptRelations("http://www.kimschouten.com/sentiment/restaurant#Amazing"));
		Framework.log("Retrieving all names entities from the ontology...");
		HashMap<String, String> lemmaToURI = ont.lexToURI();
		Framework.log("Done! Retrieved "+lemmaToURI.size() + " lexicalizations.");
		//Framework.log(""+lemmaToURI);
		for (Span span : dataset.getSpans(textualUnitSpanType)){
		
			if (spanAnnotationType != null)
				span.putAnnotation("URI", lemmaToURI.get(span.getAnnotation(spanAnnotationType)));
			
			HashSet<Word> wordsToRemove = new HashSet<>();
			int wordsToSkip = 0;
			for (Word w : span){
				
				if (wordsToSkip> 0){
					wordsToSkip--;
					continue;
				}
				
				String candidateMultiWord = w.getWord();
				String candidateMultiLemma = w.getLemma();
				String URI = lemmaToURI.get(candidateMultiLemma);
				Word endWord = w;
				String multiWord=null;
				if (URI != null)
					multiWord=candidateMultiLemma;
				Word nextWord = w;
				
				boolean proceed = true;
				int counter = 0;
				while (nextWord.hasNextWord() && proceed){
					nextWord = nextWord.getNextWord();
					if (nextWord.getWord().startsWith(".") || nextWord.getWord().startsWith("'")){
						candidateMultiLemma = candidateMultiWord + nextWord.getLemma();
						candidateMultiWord += nextWord.getWord();
					} else {
						candidateMultiLemma = candidateMultiWord + " " + nextWord.getLemma();
						candidateMultiWord += " " + nextWord.getWord();
					}
					String longerURI = lemmaToURI.get(candidateMultiLemma);
					if (longerURI != null){
						URI = longerURI;
						endWord = nextWord;
						multiWord = candidateMultiLemma;
						//Framework.debug(candidateMultiLemma + "... Match: " + URI);
					} else {
//						Main.debug(candidateMultiWord + "...");
						//if we already got an expression, but the next word does not match, stop
						// actually, scrap that, since that would not work for London Stock Exchange, as it matches
						// on the location London, but there is no match for just London Stock, meaning it will never
						// find the full "London Stock Exchange"
//						if (multiWord != null)
//							proceed = false;
					}
				}
				
				
				
				//real multi-word entity
				if (URI != null && endWord != w){
//					Framework.debug("Merging words into one...");
//					Framework.debug("Multiword: "+multiWord);
//					Framework.debug("Start: "+w.getWord() + "\tEnd: "+endWord.getWord());
					int startOffset = w.getStartOffset();
					int endOffset = endWord.getEndOffset();
					nextWord = w;
					//remove all subsequent words after w that are to be included in the merged Word object
					while (nextWord.hasNextWord() && (!endWord.hasNextWord() || !nextWord.getNextWord().equals(endWord.getNextWord()))){
						nextWord = nextWord.getNextWord();
						wordsToRemove.add(nextWord);
						wordsToSkip++;
					}

					//instead of constructing a new Word object, we modify the first Word into the longer one
					// this prevents the Span from protesting since it tries to keep a consecutive list of Words
					// and adding a new Word in the middle of it will not work
					w.setWord(multiWord);
					w.setNextWord(endWord.getNextWord());

					if (endWord.hasNextWord()){
						endWord.getNextWord().setPreviousWord(w);
						
						nextWord = endWord.getNextWord();
						nextWord.resetOrder();
						while (nextWord.hasNextWord()){
							nextWord = nextWord.getNextWord();
							nextWord.resetOrder();
						}
					}
					
//					Framework.debug("Previous word: "+w.getPreviousWord());
//					Framework.debug("Next word: "+w.getNextWord());
					
				}
				//single-word entity
				if (URI != null ){
					w.putAnnotation("URI", URI);
				}
				
			}
			//this is not an ideal situation
			span.removeAll(wordsToRemove);
//			Framework.log(span.getTextualUnit().toString());
//			Framework.log(""+dataset.getSpans(span.getTextualUnit()));
//			for (Span s : dataset.getSpans(span.getTextualUnit())){
//				s.removeAll(wordsToRemove);
//			}
		}
		
		
//		for (String uri : ont.getLexicalizedConcepts(ont.URI_NamedEntityMention, null)){
//			Main.debug(uri);
//		}
//		
//		HashSet<String> companyNames = new HashSet<String>();
//		for (Span span : dataset.getSpans(spanType)){
//			companyNames.add(span.getAnnotations().get("company"));
//		}
//		for (String name : companyNames){
//			name = name.replaceAll("&", "&amp;");
//			
//			String owl = "    <!-- http://www.kimschouten.com/finance/" + name.replaceAll(" ", "") + " -->\n\n"+
//						 "    <owl:Class rdf:about=\"http://www.kimschouten.com/finance/"+ name.replaceAll(" ", "") +"\">\n"+
//						 "        <rdfs:subClassOf rdf:resource=\"http://www.kimschouten.com/finance/CompanyNameMention\"/>\n"+
//						 "        <lex>"+ name + "</lex>\n"+
//						 "        <llex>" + name.toLowerCase() + "</llex>\n"+
//						 "    </owl:Class>\n\n";
//			Main.debug(owl);
//		}
//		
		
	}

	
	
}
