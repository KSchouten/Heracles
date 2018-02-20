package edu.eur.absa.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.TreeMap;
import java.util.TreeSet;

import edu.eur.absa.Framework;
import edu.eur.absa.nlp.NLPTask;
import edu.eur.absa.nlp.AbstractNLPComponent;

/**
 * <code>Dataset</code> is the container for all <code>Span</code>s that are processed together. Contains no textual information apart from that, but instead gives the tools for data set management.
 * It allows you to use an arbitrary <code>spanType</code> and divide the data set into training/validation/test based on that.
 * 
 * @author Kim Schouten
 *
 */
public class Dataset {

	//it is useful to have all span in one place
	private TreeSet<Span> spans = new TreeSet<>();
	private HashMap<String, TreeSet<Span>> spansByType = new HashMap<>();
	private HashMap<Span, TreeSet<Span>> spansByTextualUnit = new HashMap<>();
	//relations by type
	private HashMap<String, TreeSet<Relation>> relationsByType = new HashMap<>();
	//all annotatables by ID
	private HashMap<Integer, DataEntity> annotatablesById = new HashMap<>();
	
	
	
//	private HashMap<String, HashSet<Relation>> spanRelationsByType = new HashMap<>();
//	private HashMap<Span, HashSet<Relation>> spanRelationsByParent = new HashMap<>();
//	private HashMap<Span, HashSet<Relation>> spanRelationsByChild = new HashMap<>();
	
	private HashSet<NLPTask> performedNLPTasks = new HashSet<>();
	private String textualUnitSpanType;
	private HashMap<String, Class<?>> annotationDataTypes = new HashMap<>();
	
	private String filename;
	//global annotatable identifier
	private int nextId = 0;
	
	public Dataset(String filename, String textualUnitSpanType){
		this.textualUnitSpanType = textualUnitSpanType;
	}
	
	
	
	/**
	 * Divide the data into subsets, using the given <code>spanType</code> as the level to divide on. This means all <code>Span</code>s of the given type
	 * will be gathered and randomly distributed over the folds while adhering to the given <code>subSetProportions</code>.
	 * This does not imply that an <code>Algorithm</code> works on that particular level of analysis, just that the data is divided using these. 
	 * For instance, dividing on the "sentence" spans, means that sentences will be randomly assigned to any of the subsets, independent from where adjacent sentences 
	 * will be assigned. However, the <code>Algorithm</code> might try to predict the sentiment for each of the "aspect" spans contained within a sentence.
	 * The boolean useAllData is used to determine whether any leftover Spans will be put in the last subset (when true) or simply left out (when false). Default is true.
	 * @param spanType
	 * @param subSetProportions These should sum up to one.
	 * @return The data split into subsets, put together in a list. Last one in the list is the test set.
	 */
	public ArrayList<HashSet<Span>> createSubSets(String spanType, double... subSetProportions){
		return Dataset.createSubSets(getSpans(spanType), subSetProportions);
	}
	
	public ArrayList<HashSet<Span>> createSubSets(String spanType, boolean useAllData, double... subSetProportions){
		return createSubSets(getSpans(spanType), useAllData, subSetProportions);
	}
	
	public static ArrayList<HashSet<Span>> createSubSets(TreeSet<Span> spansToDivide, double... subSetProportions){
		return createSubSets(spansToDivide, true, subSetProportions);
	}
	
	public static ArrayList<HashSet<Span>> createSubSets(TreeSet<Span> spansToDivide, boolean useAllData, double... subSetProportions){
		if (spansToDivide == null || spansToDivide.isEmpty()){
			Framework.error("Cannot divide dataset with a span type that does not exist");
		}
		
		//prepare the list of subsets
		ArrayList<HashSet<Span>> subsets = new ArrayList<>();
		//put the set of spans (of the given type) in random order
		TreeMap<Double,Span> randomizedSpans = new TreeMap<>();
		
		Random r = new Random();
		
		for (Span s : spansToDivide){
			while (randomizedSpans.putIfAbsent(r.nextDouble(), s) != null);
		}
		//this code is obsolete in favor of just putting everything in an ArrayList, which gives the same functionality
		//and probably uses less memory than an additional TreeMap.
//		TreeMap<Integer,Span> randomlyOrderedSpans = new TreeMap<>();
//		int nextInt = 0;
//		for (Double d : randomizedSpans.keySet()){
//			randomlyOrderedSpans.put(nextInt, randomizedSpans.get(d));
//			nextInt++;
//		}
		
		ArrayList<Span> randomizedList = new ArrayList<>();
		randomizedList.addAll(randomizedSpans.values());
		
		int nextInt = 0;
		for (double subsetProportion : subSetProportions){
			HashSet<Span> subset = new HashSet<Span>();
			int numberOfSpans = (int) (spansToDivide.size() * subsetProportion);
//			subset.addAll(randomlyOrderedSpans.subMap(nextInt, nextInt+numberOfSpans).values());
			subset.addAll(randomizedList.subList(nextInt, nextInt+numberOfSpans));
			nextInt += numberOfSpans;
			subsets.add(subset);
		}
//		subsets.get(subsets.size()-1).addAll(randomlyOrderedSpans.tailMap(nextInt).values());
		if (useAllData)
			subsets.get(subsets.size()-1).addAll(randomizedList.subList(nextInt, randomizedList.size()));
		
		Framework.debug("Total size: "+spansToDivide.size());
		for (HashSet<Span> subset : subsets){
			Framework.debug("Subset: "+subset.size());
		}
		return subsets;
	}
	
	
	public void addSpan(Span span){
		//maybe this works too:
		String spanType = span.getType();
		//rest of code
		if (!spansByType.containsKey(spanType))
			spansByType.put(spanType, new TreeSet<>());
		spansByType.get(spanType).add(span);
		
		if (!spansByTextualUnit.containsKey(span.getTextualUnit()))
			spansByTextualUnit.put(span.getTextualUnit(), new TreeSet<Span>());
		spansByTextualUnit.get(span.getTextualUnit()).add(span);
		
		spans.add(span);
		
		
	}
	
	public void addRelation(Relation rel){
		if (!relationsByType.containsKey(rel.getType())){
			relationsByType.put(rel.getType(), new TreeSet<Relation>());
		}
		relationsByType.get(rel.getType()).add(rel);
	}
	
	/**
	 * 
	 * @return An existing TreeSet (do not change it)
	 */
	public TreeSet<Span> getSpans(){
		return spans;
	}
	/**
	 * 
	 * @param spanType
	 * @return An existing TreeSet (do not change it)
	 */
	public TreeSet<Span> getSpans(String spanType){
		return spansByType.get(spanType);
	}
	/**
	 * 
	 * @param textualUnit
	 * @return An existing TreeSet (do not change it)
	 */
	public TreeSet<Span> getSpans(Span textualUnit){
		return spansByTextualUnit.get(textualUnit);
	}
	/**
	 * 
	 * @param textualUnit
	 * @param spanType
	 * @return A new <code>TreeSet</code> with the selected Spans 
	 */
	public TreeSet<Span> getSpans(Span textualUnit, String spanType){
		TreeSet<Span> spans = new TreeSet<>();
		spans.addAll(getSpans(textualUnit));
		spans.retainAll(getSpans(spanType));
		return spans;
	}
	
	public TreeSet<Span> getSpans(String spanType, Word containingWord){
		TreeSet<Span> spans = getSpans(containingWord.getTextualUnit(), spanType);
		TreeSet<Span> spansWithWord = new TreeSet<>();
		for (Span span : spans){
			if (span.contains(containingWord)){
				spansWithWord.add(span);
			}
		}
		return spansWithWord;
	}
	
	public TreeSet<Span> getSpans(Word containingWord){
	
		TreeSet<Span> spans = getSpans(containingWord.getTextualUnit());
		TreeSet<Span> spansWithWord = new TreeSet<>();
		for (Span span : spans){
			if (span.contains(containingWord)){
				spansWithWord.add(span);
			}
		}
		return spansWithWord;
	}
	
	public HashSet<Span> getSubSpans(HashSet<Span> originalData, String subSpanType){
		HashSet<Span> subSpanData = new HashSet<Span>();
//		Framework.log("Subspantype:"+subSpanType);
		for (Span span : originalData){
			subSpanData.addAll(
					span.getCoveredSpans(
							getSpans(span.getTextualUnit(), subSpanType)
					)
			);
		}
		return subSpanData;
	}
	
	/**
	 * 
	 * @param relationType
	 * @return An existing TreeSet (do not change it)
	 */
	public TreeSet<Relation> getRelations(String relationType){
		return relationsByType.get(relationType);
	}
	
	
	public DataEntity getAnnotatable(int id){
		return annotatablesById.get(id);
	}
	
	public Iterator<DataEntity> getDataEntityIterator(){
		return annotatablesById.values().iterator();
	}
	
	
	
	public Dataset process(AbstractNLPComponent nlp, String spanType){
		nlp.process(this, spanType);
		performedNLPTasks.add(nlp.getTask());
		return this;
	}
	
	public HashSet<NLPTask> getPerformedNLPTasks(){
		return performedNLPTasks;
	}
	
	public String getTextualUnitSpanType(){
		return textualUnitSpanType;
	}
	
	public HashMap<String, Class<?>> getAnnotationDataTypes(){
		return annotationDataTypes;
	}
	
	public String getFilename(){
		return this.filename;
	}
	
	/**
	 * Get the next unique identifier and also register this Annotatable with that id in this Dataset
	 * @param a
	 * @return
	 */
	public int getNextId(DataEntity a){
		nextId++;
		annotatablesById.put(nextId, a);
		return nextId;
	}
	public int getCurrentId(){
		return nextId;
	}
	public void removeAnnotatable(DataEntity a){
		annotatablesById.remove(a.getId());
	}
	/**
	 * Use this method to add the contents of another Dataset object into this Dataset
	 * @param moreData The Dataset whose content will be added to this Dataset
	 */
	public void mergeDataset(Dataset moreData) throws Exception{
		//first, check compatibility
		HashSet<NLPTask> moreDataNLPTasks = new HashSet<>();
		moreDataNLPTasks.addAll(moreData.performedNLPTasks);
		for (NLPTask t : performedNLPTasks){
			if (!moreDataNLPTasks.contains(t)){
				throw new Exception("Datasets are not compatible: different NLP tasks have been performed on both Datasets\n"+this.performedNLPTasks+"\n"+moreData.performedNLPTasks);
			}
			moreDataNLPTasks.remove(t);
		}
		if (moreDataNLPTasks.size()>0){
			throw new Exception("Datasets are not compatible: different NLP tasks have been performed on both Datasets\n"+this.performedNLPTasks+"\n"+moreData.performedNLPTasks);
		}
		
		if (!textualUnitSpanType.equalsIgnoreCase(moreData.textualUnitSpanType)){
			throw new Exception("Datasets are not compatible: different textual unit span types are in use");
		}
		HashMap<String, Class<?>> moreDataAnnotationDataTypes = new HashMap<>();
		moreDataAnnotationDataTypes.putAll(moreData.annotationDataTypes);
		for (String annotationType : annotationDataTypes.keySet()){
			if (!moreDataAnnotationDataTypes.containsKey(annotationType) ||
					!moreDataAnnotationDataTypes.get(annotationType).equals(annotationDataTypes.get(annotationType))){
				throw new Exception("Datasets are not compatible: different sets of annotations are in use");
			}
			moreDataAnnotationDataTypes.remove(annotationType);
		}
		if (moreDataAnnotationDataTypes.size() > 0){
			throw new Exception("Datasets are not compatible: different sets of annotations are in use");
		}
		//ok, both Datasets are compatible, add everything to this Dataset
		Framework.log("Merging two Datasets....");
		for (DataEntity d : moreData.annotatablesById.values()){
			//this will also update the id's and dataset references of each DataEntity
			d.moveToDifferentDataset(this); 
			//special references for Spans and Relations for indexing purposes
			if (d instanceof Span){
				this.addSpan((Span)d);
			}
			if (d instanceof Relation){
				this.addRelation((Relation)d);
			}
		}
		Framework.log("Merging two Datasets....Done!");
	}
}
