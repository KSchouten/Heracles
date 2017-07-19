package edu.eur.absa.model;

import java.util.HashMap;
import java.util.HashSet;

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
	protected Annotations annotations = null;
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
	 * Get the set of annotations belonging to this Span
	 * @return
	 */
	public Annotations getAnnotations() {
		if (annotations == null){
			annotations = new Annotations(dataset);
		}
		return annotations;
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
}
