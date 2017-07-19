package edu.eur.absa.model;

import java.util.HashMap;
import java.util.HashSet;

/**
 * A directed relation between two <code>Annotatable</code>s. Can be used to directly link <code>Word</code>s to their containing 
 * sentence or document or other textual unit that you need often so as to prevent much searching in the spans set.
 * @author Kim Schouten
 *
 */
public class Relation extends DataEntity {

	private int id;
	private String type;
	private DataEntity parent;
	private DataEntity child;
	private Dataset dataset;
	private Annotations annotations = null;
	private Relations relations = new Relations();
	
	
	public Relation(String type, DataEntity parent, DataEntity child){
		this.type = type;
		this.parent = parent;
		parent.getRelations().addRelationToChild(this);
		this.child = child;
		child.getRelations().addRelationToParent(this);
		this.dataset = parent.getDataset();
		this.id = dataset.getNextId(this);
		dataset.addRelation(this);
	}

	@Override
	public Annotations getAnnotations() {
		if (annotations == null){
			annotations = new Annotations(dataset);
		}
		return annotations;
	}
	
	@Override
	public Dataset getDataset() {
		return dataset;
	}

	@Override
	public int getId() {
		return id;
	}
	
	public DataEntity getParent(){
		return parent;
	}
	public DataEntity getChild(){
		return child;
	}
	public String getType(){
		return type;
	}

	@Override
	public Relations getRelations(){
		return relations;
	}

	public String toString(){		
		return parent.toString() + " -> " + 
				getType() + " -> " + 
				child.toString() + " :: " + 
				getAnnotations().toString();
	}
	
	@Override
	public int compareTo(DataEntity anotherAnn) {
		if (this.id > anotherAnn.getId())
			return 1;
		if (this.id < anotherAnn.getId())
			return -1;
		return 0;
	}
}
