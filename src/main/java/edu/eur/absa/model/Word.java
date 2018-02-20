package edu.eur.absa.model;

/**
 * A Word is the atomic unit of a <code>Document</code>. It contains only the most basic information about a word: 
 * its original textual representation, its order within the <code>Document</code>, including links to the previous and next <code>Word</code>s,
 * and offset information that can be useful when reconstructing the original data set with extra annotations (e.g., for submitting to competitions, benchmarks, etc.)
 * @author Kim Schouten
 *
 */
public class Word extends DataEntity {

	/**
	 * 
	 */

	private String word;
	private Word previousWord = null;
	private Word nextWord = null;
	//these values can be useful for generating acceptable output files, but are not to be used for internal logic, use wordOrder instead
	private int startOffset = 0;
	private int endOffset = 0;
	//used to denote the order in a document
	private int order = 0;
		
	private Word(String word, int startOffset, Dataset dataset){
		this.word = word;
		this.startOffset = startOffset;
		this.endOffset = startOffset+word.length();
		this.dataset = dataset;
		this.id = dataset.getNextId(this);
	}
	
	/**
	 * <p>Creates a basic <code>Word</code> object, using the length of <code>word</code> to compute the <code>endOffset</code>.
	 * It links this <code>Word</code> object to its containing <code>Dataset</code>.</p>
	 * This is the ideal way of creating the first <code>Word</code> for a new textual unit.
	 * @param word
	 * @param startOffset
	 * @param textualUnit
	 * @param dataset
	 */
	public Word(String word, int startOffset,Span textualUnit, Dataset dataset){
		this(word, startOffset, dataset);
		this.textualUnit = textualUnit;
		textualUnit.add(this);
	}
	
	/**
	 * <p>Creates a basic <code>Word</code> object, using the length of <code>word</code> to compute the <code>endOffset</code>.
	 * It links this <code>Word</code> object to <code>previousWord</code> and this <code>Word</code> object is set 
	 * as <code>nextWord</code> of <code>previousWord</code>.
	 * It also sets the <code>Document</code> to the document value of <code>previousWord</code>.</p>
	 * <p>This is the preferred way of creating <code>Word</code>s, as it automatically creates the links between <code>Word</code>s.</p>
	 * <p>Setting the <code>previousWord</code> parameter to <code>null</code> will result in a <code>NullPointerException</code>. </p>
	 * @param word
	 * @param startOffset
	 * @param previousWord
	 */
	public Word(String word, int startOffset, Word previousWord){
		this(word, startOffset, previousWord.dataset);
		this.previousWord = previousWord;
		previousWord.nextWord = this;
		order = previousWord.order+1;
		textualUnit = previousWord.getTextualUnit();
		textualUnit.add(this);
//		document = previousWord.document;
//		document.add(this);
	}
	

	
	
	
	
	public boolean hasNextWord(){
		return (nextWord != null);
	}
	public Word getNextWord(){
		return nextWord;
	}
	public boolean hasPreviousWord(){
		return (previousWord != null);
	}
	public Word getPreviousWord(){
		return previousWord;
	}
	/**
	 * This is the start offset of this word, in terms of characters, within some textual unit (e.g., sentence or document), depending on the source data. 
	 * This information can be useful to reconstruct a valid data file for evaluation/benchmark purposes
	 * @return
	 */
	public int getStartOffset(){
		return startOffset;
	}
	/**
	 * This is the end offset of this word, in terms of characters, within some textual unit (e.g., sentence or document), depending on the source data. 
	 * This information can be useful to reconstruct a valid data file for evaluation/benchmark purposes
	 * @return
	 */
	public int getEndOffset(){
		return endOffset;
	}
	/**
	 * Return the original word as first encountered in the source data. Can be useful to reconstruct a valid data file for evaluation/benchmark purposes. 
	 * In normal life, one would probably grab one of the annotation in {@link Span} <code>wordSpan</code>. 
	 * @return
	 */
	public String getWord(){
		return word;
	}
	
	/**
	 * Return the order of this <code>Word</code> in its original textual unit (e.g., sentence or document), depending on the source data.
	 * @return
	 */
	public int getOrder(){
		return order;
	}
	
	public Span getTextualUnit(){
		return textualUnit;
	}
	
	public Dataset getDataset(){
		return dataset;
	}
	
	public int getId(){
		return id;
	}
	
//	@Override
//	public boolean equals(Object anotherObject){
//		if (anotherObject instanceof Word){
//			return this.id == ((Word)anotherObject).id;
//		
//		} else {
//			return false;
//		}
//	}
	
//	@Override
//	public boolean equals(Object anotherObject){
//		if (anotherObject instanceof Word){
//			Word anotherWord = (Word)anotherObject;
//			if (this.getTextualUnit().hashCode() == anotherWord.getTextualUnit().hashCode() &&
//					this.getOrder() == anotherWord.getOrder()){
//				return true;
//			} else {
//				return false;
//			}
//		} else {
//			return false;
//		}
//	}
	
//	@Override
//	public int compareTo(Annotatable anotherWord) {
////		try {
//			if (this.textualUnit.hashCode() != anotherWord.textualUnit.hashCode())
//				return -1;
//				//throw new IllegalComparisonException();
//		
//			if (this.order > anotherWord.order)
//				return 1;
//			
//			if (this.order < anotherWord.order)
//				return -1;
//			
//			//this only happens if you compare the same two Word objects
//			return 0;
//			
////		} catch (IllegalComparisonException e){
////			e.printStackTrace();
////			return 0;
////		}
//		
//	}
	@Override
	public int compareTo(DataEntity anotherAnn) {
		if (this.id > anotherAnn.getId())
			return 1;
		if (this.id < anotherAnn.getId())
			return -1;
		return 0;
	}

	
	@Override
	public String toString(){
		return word;
	}
	
	public void setPreviousWord(Word w){
		previousWord = w;
	}
	public void setNextWord(Word w){
		nextWord = w;
	}
	public void setWord(String word){
		this.word = word;
		endOffset = startOffset + word.length();
	}
	/**
	 * If the order of words is changed (by adding or removing a word somewhere), it needs to be updated
	 *   Not only in the Word objects, but also in all of the Spans that contain this word.
	 */
	public void resetOrder(){
		this.order = previousWord.order+1;
		for (Span s : dataset.getSpans(this)){
			s.resetOrder(this);
		}
		
	}
	
	//Convenience methods, since these are often used
	//Behaviour untested when these annotations are not present
	public String getLemma(){
//		return getAnnotations().get("lemma", String.class);
		return getAnnotation("lemma", String.class);
	}
	public String getPOS(){
//		return getAnnotations().get("pos", String.class);
		return getAnnotation("pos", String.class);
	}
	
}
