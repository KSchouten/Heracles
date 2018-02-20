package edu.eur.absa.model;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.SortedSet;
import java.util.TreeSet;

import org.json.JSONObject;

import edu.eur.absa.Framework;
import edu.eur.absa.model.exceptions.IllegalSpanException;

/**
 * A Span is an arbitrary sequence of Words that are directly adjacent to one another. Hence, they have to originate from the same textual unit.
 * A <code>Span</code> is the vehicle to add annotations to your textual data.
 * <code>Span</code>s cannot span over multiple textual units as these are by definition independent textual units.
 * @author Kim Schouten
 *
 */
public class Span extends DataEntity implements NavigableSet<Word> {

	/**
	 * 
	 */
	private String spanType;
	
	private TreeSet<Word> words = new TreeSet<Word>();
	
	private HashMap<Integer, Word> wordsByOrder = new HashMap<>();
	/**
	 * Standard constructor to create a Span object. Note that all words that are contained in a single <code>Span</code> have to originate from the same textual unit.
	 * @param spanType A textual label denoting the kind of <code>Span</code>. Some basic ones would be "word", "sentence", "aspect", etc.
	 * @param firstWord
	 * @param lastWord
	 * @throws IllegalSpanException If the first and last word do not originate from the textual unit.
	 */
	public Span(String spanType, Word firstWord, Word lastWord) throws IllegalSpanException{
		//All words that are contained in a single span have to originate from the same textual unit
		if (!firstWord.getTextualUnit().equals(lastWord.getTextualUnit()))
			throw new IllegalSpanException();
		
		this.spanType = spanType;
		Word currentWord = firstWord;
		this.add(currentWord);
		while (currentWord.hasNextWord() && currentWord != lastWord){
			currentWord = currentWord.getNextWord();
			this.add(currentWord);
		}
		this.textualUnit = firstWord.getTextualUnit();
		this.dataset = firstWord.getDataset();
		this.id = dataset.getNextId(this);
		dataset.addSpan(this);
		
	}

	/**
	 * Special 'empty' Span constructor. This is used to create an empty Span for a new textual unit, since no Words are yet added to it. 
	 * This Span is its own textual unit so this constructor sets <code>textualUnit=this</code>. 
	 * This constructor will automatically register this span with its parent Dataset.
	 * @param spanType
	 */
	public Span(String spanType, Dataset dataset){
		this.spanType = spanType;
		this.dataset = dataset;
		this.textualUnit = this;
		this.id = dataset.getNextId(this);
		dataset.addSpan(this);
		
	}

	/**
	 * Special 'empty' Span constructor. This is used to create an empty sub-Span for an existing textual unit that has no Words yet.
	 * This can be useful if the given dataset already splits the text into sentences or other sub-blocks of an integral piece of text,
	 * but does not provide the word splits. 
	 * This constructor will automatically register this span with its parent Dataset. 
	 * @param spanType
	 */
	public Span(String spanType, Span textualUnit){
		this.spanType = spanType;
		this.dataset = textualUnit.getDataset();
		this.textualUnit = textualUnit;
		this.id = dataset.getNextId(this);
		dataset.addSpan(this);
	}
	
	
	/**
	 * Add a Word to this Span. It has to be adjacent to either the first or last word already in the Span, otherwise an 
	 * IllegalSpanException is thrown (and caught, but displayed as a warning).
	 */
	public boolean add(Word word){
		if (isEmpty() || (last().hasNextWord() && last().getNextWord().equals(word)) || (first().hasPreviousWord() && first().getPreviousWord().equals(word))){
			wordsByOrder.put(word.getOrder(), word);
			return words.add(word);
		} else {
			try {
				throw new IllegalSpanException("You cannot add a Word to a Span that is not adjacent to the first or last word already in the Span");
			} catch (IllegalSpanException e){
				e.printStackTrace();
			}
			return false;
		}
	}
	
	public boolean addAll(Span span){
		if (span.isEmpty()){
			//nothing to add
			return false;
		}
		if (isEmpty() || (last().hasNextWord() && last().getNextWord().equals(span.first())) || (first().hasPreviousWord() && first().getPreviousWord().equals(span.last()))){
			return words.addAll(span.words);
		} else {
			try {
				throw new IllegalSpanException("You cannot add the Words from one Span to another Span if the two Spans are not adjacent to each other");
			} catch (IllegalSpanException e){
				e.printStackTrace();
			}
			return false;
		}
	}
	
		
	public boolean remove(Word w){
		dataset.removeAnnotatable((Word) w);
		return words.remove(w);
	}

	
	public Word first(){
		return words.first();
	}
	public Word last(){
		return words.last();
	}
	public boolean isEmpty(){
		return words.isEmpty();
	}
	
	public int size(){
		return words.size();
	}
	
	public void resetOrder(Word word){
		wordsByOrder.remove(word);
		wordsByOrder.put(word.getOrder(), word);
	}
	
	public Word getWordByOrder(int order){
		return wordsByOrder.get(order);
	}
	
	/**
	 * Given a <code>HashSet</code> of <code>Span</code> objects, return the ones that are completely covered by this <code>Span</code> instance. 
	 * This means that every <code>Word</code> in such a <code>Span</code> would also need to be contained within this instance of <code>Span</code>.
	 * @param spans
	 * @return
	 */
	public TreeSet<Span> getCoveredSpans(TreeSet<Span> spans){
		TreeSet<Span> coveredSpans = new TreeSet<>();
		for (Span s : spans){
			if (this.contains(s.first()) && this.contains(s.last())){
				coveredSpans.add(s);
			}
		}
		return coveredSpans;
	}
	/**
	 * Given a <code>TreeSet</code> of <code>Span</code> objects, return the ones that completely cover this <code>Span</code> instance. 
	 * This means that every <code>Word</code> in this instance of <code>Span</code> would also need to be contained within such a <code>Span</code>.
	 * @param spans
	 * @return
	 */
	public TreeSet<Span> getCoveringSpans(TreeSet<Span> spans){
		TreeSet<Span> coveringSpans = new TreeSet<>();
		for (Span s : spans){
			if (s.contains(this.first()) && s.contains(this.last())){
				coveringSpans.add(s);
			}
		}
		return coveringSpans;
	}
	/**
	 * Given a <code>TreeSet</code> of <code>Span</code> objects, return the ones that have at least some overlap with this <code>Span</code> instance. 
	 * This means that at least one <code>Word</code> in such a <code>Span</code> would also need to be contained within this instance of <code>Span</code>.
	 * @param spans
	 * @return
	 */	
	public TreeSet<Span> getTouchingSpans(TreeSet<Span> spans){
		TreeSet<Span> touchingSpans = new TreeSet<>();
		for (Span s : spans){
			if (s.contains(first()) || this.contains(s.last())){
				touchingSpans.add(s);
			}
		}
		return touchingSpans;
	}
	/**
	 * Given a <code>TreeSet</code> of <code>Span</code> objects, return the ones that contain only <code>Word</code>s that have a lower word order than 
	 * any word in this instance of <code>Span</code>.
	 * The returned set of <code>Span</code>s would appear earlier, or to the left, of the text covered by the current <code>Span</code>, without any overlap. 
	 * @param spans
	 * @return
	 */		
	public TreeSet<Span> getStrictlyLeftSpans(TreeSet<Span> spans){
		TreeSet<Span> strictlyLeftSpans = new TreeSet<>();
		for (Span s : spans){
			if (first().getTextualUnit().equals(s.first().getTextualUnit()) && first().getOrder() > s.last().getOrder()){
				strictlyLeftSpans.add(s);
			}
		}
		return strictlyLeftSpans;
	}
	/**
	 * Given a <code>TreeSet</code> of <code>Span</code> objects, return the ones that contain only <code>Word</code>s that have a higher word order than 
	 * any word in this instance of <code>Span</code>. 
	 * The returned set of <code>Span</code>s would appear later, or to the right, of the text covered by the current <code>Span</code>, without any overlap.
	 * @param spans
	 * @return
	 */			
	public TreeSet<Span> getStrictlyRightSpans(TreeSet<Span> spans){
		TreeSet<Span> strictlyRightSpans = new TreeSet<>();
		for (Span s : spans){
			if (first().getTextualUnit().equals(s.first().getTextualUnit()) &&  last().getOrder() < s.first().getOrder()){
				strictlyRightSpans.add(s);
			}
		}
		return strictlyRightSpans;
	}
	/**
	 * Return the type of this <code>Span</code>: a textual label denoting the kind of <code>Span</code>. Some basic ones would be "word", "sentence", "aspect", etc.
	 * @return
	 */
	public String getType(){
		return spanType;
	}
	

	
//	/**
//	 * A better version to compare Spans than the official one, since we can use the information that a Span
//	 * always holds adjacent Words, and the annotations need to be the same. 
//	 * Hence, for the elements, we only need to check if the first and last one are the same instead of all 
//	 * in-between ones as well.
//	 * But we do need to check the annotations too, since we can have a span over the same words (esp. if emtpy), but with different
//	 * annotations or different type.
//	 * @param anotherSpan
//	 * @return
//	 */
//	public boolean equals(Span anotherSpan){
//		boolean sameType = this.getType().equalsIgnoreCase(anotherSpan.getType());
//		if (!sameType)
//			return false;
//		boolean sameTextualUnit = this.getTextualUnit()==null || anotherSpan.getTextualUnit() == null || this.getTextualUnit().hashCode() == anotherSpan.getTextualUnit().hashCode();
//		if (!sameTextualUnit)
//			return false;
//		//same elements if both are emtpy, or both have the same elements
//		boolean sameElements = (this.isEmpty() && anotherSpan.isEmpty()) || 
//				(first().equals(anotherSpan.first()) && last().equals(anotherSpan.last()));
//		if (!sameElements)
//			return false;
//		boolean sameAnnotations = this.getAnnotations().equals(anotherSpan.getAnnotations());
//		return sameAnnotations;
//	}
	

	

	
	@Override
	public String toString(){
		JSONObject spanJSON = new JSONObject();
		spanJSON.put("spanType", getType());
		spanJSON.put("id", getId());
		if (size()>0){
			spanJSON.put("first", first().getOrder());
			spanJSON.put("last", last().getOrder());
		}
		JSONObject annotationsJSON = new JSONObject();
		spanJSON.put("annotations", annotationsJSON);
		for (String annotationKey : annotations.keySet()){
			annotationsJSON.put(annotationKey, (Object)getAnnotation(annotationKey));
		}
		return spanJSON.toString()+"\n";
	}



	@Override
	public Iterator<Word> iterator() {
		return words.iterator();
	}

	@SuppressWarnings("unchecked")
	public TreeSet<Word> getWords(){
		return (TreeSet<Word>) words.clone();
		//return words;
	}

	@Override
	public Comparator<? super Word> comparator() {
		return words.comparator();
	}

	@Override
	public boolean addAll(Collection<? extends Word> c) {
		boolean changed = false;
		for (Word w : c){
			changed = changed || add(w);
		}
		return changed;
	}

	@Override
	public void clear() {
		try {
			throw new IllegalSpanException("This operation is not supported for Spans");
		} catch (IllegalSpanException e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean contains(Object o) {
		if (o instanceof Word){
//			//TODO: This is extremely strange: without the cloning, a Word is not always properly found!
			return ( words.contains((Word)o) || ((TreeSet<Word>)words.clone()).contains((Word)o));
		} else {
			Framework.log("Incorrect comparison");
			return false;
		}
		//return words.contains(o);
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return words.containsAll(c);
	}

	@Override
	public boolean remove(Object o) {
		if (o instanceof Word){
			return remove((Word)o);
		} else {
			return false;
		}
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		boolean changed = false;
		for (Object o : c){
			changed = changed || remove(o);
		}
		return changed;
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		try {
			throw new IllegalSpanException("This operation is not supported for Spans");
		} catch (IllegalSpanException e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public Object[] toArray() {
		return words.toArray();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		return words.toArray(a);
	}

	@Override
	public Word ceiling(Word e) {
		return words.ceiling(e);
	}

	@Override
	public Iterator<Word> descendingIterator() {
		return words.descendingIterator();
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public NavigableSet<Word> descendingSet(){
		return ((TreeSet<Word>)words.clone()).descendingSet();
	}

	@Override
	public Word floor(Word e) {
		return words.floor(e);
	}

	@Override
	public SortedSet<Word> headSet(Word toElement) {
		return words.headSet(toElement);
	}

	@Override
	public NavigableSet<Word> headSet(Word toElement, boolean inclusive) {
		return words.headSet(toElement, inclusive);
	}

	@Override
	public Word higher(Word e) {
		return words.higher(e);
	}

	@Override
	public Word lower(Word e) {
		return words.lower(e);
	}

	@Override
	public Word pollFirst() {
		try {
			throw new IllegalSpanException("This operation is not supported for Spans");
		} catch (IllegalSpanException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public Word pollLast() {
		try {
			throw new IllegalSpanException("This operation is not supported for Spans");
		} catch (IllegalSpanException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public SortedSet<Word> subSet(Word fromElement, Word toElement) {
		return words.subSet(fromElement, toElement);
	}

	@Override
	public NavigableSet<Word> subSet(Word fromElement, boolean fromInclusive, Word toElement, boolean toInclusive) {
		return words.subSet(fromElement, fromInclusive, toElement, toInclusive);
	}

	@Override
	public SortedSet<Word> tailSet(Word fromElement) {
		return words.tailSet(fromElement);
	}

	@Override
	public NavigableSet<Word> tailSet(Word fromElement, boolean inclusive) {
		return words.tailSet(fromElement, inclusive);
	}

}
