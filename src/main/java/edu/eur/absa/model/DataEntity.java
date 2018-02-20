package edu.eur.absa.model;

import java.util.HashMap;
import java.util.Set;

/**
 * To be implemented by anything that can have annotations. For instance, <code>Word</code>, <code>Span</code>, and 
 * <code>SpanRelation</code> all implement <code>Annotatable</code>.
 *  
 * @author Kim Schouten
 *
 */
public abstract class DataEntity implements Comparable<DataEntity> {
	
	protected int id;
	protected Span textualUnit;
	protected Dataset dataset;
	protected HashMap<String,Object> annotations = new HashMap<>();
	protected Relations relations = null;
	
	/**
	 * Return the textual unit this <code>Span</code> appears in. By definition, all <code>Word</code>s within a <code>Span</code> must originate from the same 
	 * textual unit. Textual unit can be a document, or basically any piece of independent text. Word order is counted per textual unit.
	 * @return
	 */
	public Span getTextualUnit(){
		return textualUnit;
	}
	
	public Dataset getDataset(){
		return dataset;
	}
	
	public int getId(){
		return id;
	}
	
	/**
	 * Get the set of annotations belonging to this Span. This method is deprecated in favor
	 * of using the built-in methods in DataEntity to access and update annotations
	 * @return
	 */
//	@Deprecated
//	public Annotations getAnnotations() {
//		return new Annotations(annotations, dataset);
//	}
	
	public String showAnnotations(){
		return annotations.toString();
	}
	
	public int compareTo(DataEntity anotherAnn) {
		if (this.id > anotherAnn.getId())
			return 1;
		if (this.id < anotherAnn.getId())
			return -1;
		return 0;
	}

	public Relations getRelations() {
		if (relations == null){
			relations = new Relations();
		}
		return relations;
	}
	
	/**
	 * For now it seems that just checking global id's is the most convenient and fast way of determining
	 * whether two DataEntities are the same object.
	 * Downside is that it is possible to create an exact duplicate of an already existing DataEntity, since, being
	 * a new object, it will get its own id value.
	 * @param anotherObject
	 * @return
	 */
	public boolean equals(DataEntity anotherObject){
		return (this.id == anotherObject.id);
	}
	
	@Override
	public boolean equals(Object o){
		if (o instanceof DataEntity){
			return equals((DataEntity)o);
		} else {
			return false;
		}
	}
	
	//Annotations methods
	
	@SuppressWarnings("unchecked")
	public <T> T getAnnotation(String annotationType, Class<T> dataType){
		Object value = annotations.get(annotationType);
		if (value == null)
			return null;
		if (dataType.isInstance(value)){
			return (T)value;
		} else {
			throw new ClassCastException("The annotation you requested is not of the type you specified");
		}
		
	}
	
	@SuppressWarnings("unchecked")
	public <T> T getAnnotation(String annotationType){
		Class<T> classT = (Class<T>) dataset.getAnnotationDataTypes().get(annotationType);
		return getAnnotation(annotationType, classT);
	}
	
	public String getAnnotationEntryText(String annotationType){
		return annotationType+": "+getAnnotation(annotationType).toString();
	}
	
	public Object putAnnotation(String annotationType, Object value){
		if (!dataset.getAnnotationDataTypes().containsKey(annotationType))
			dataset.getAnnotationDataTypes().put(annotationType, value.getClass());
		return annotations.put(annotationType, dataset.getAnnotationDataTypes().get(annotationType).cast(value));
	}
	
	public boolean hasAnnotation(String annotationType){
		return annotations.containsKey(annotationType);
	}
	
	public Set<String> getAnnotationTypes(){
		return annotations.keySet();
	}
	
	public void moveToDifferentDataset(Dataset newDataset){
		dataset = newDataset;
		id = dataset.getNextId(this);
	}
}
