package edu.eur.absa.model;

/**
 * A directed relation between two <code>Annotatable</code>s. Can be used to directly link <code>Word</code>s to their containing 
 * sentence or document or other textual unit that you need often so as to prevent much searching in the spans set.
 * @author Kim Schouten
 *
 */
public class Relation extends DataEntity {

	private String type;
	private DataEntity parent;
	private DataEntity child;

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

		
	public DataEntity getParent(){
		return parent;
	}
	public DataEntity getChild(){
		return child;
	}
	public String getType(){
		return type;
	}


	public String toString(){		
		return parent.toString() + " -> " + 
				getType() + " -> " + 
				child.toString() + " :: " + 
				showAnnotations();
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
