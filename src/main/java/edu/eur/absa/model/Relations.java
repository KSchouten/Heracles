package edu.eur.absa.model;

import java.util.HashMap;
import java.util.TreeSet;

/**
 * Model the set of relations an Annotatable can have, since this is not allowed inside an interface,
 * and we don't want to repeat this same code in every implementation of Annotatable
 * @author Kim Schouten
 *
 */
public class Relations {

	private HashMap<String, TreeSet<Relation>> relationsToParents = new HashMap<>();
	private HashMap<String, TreeSet<Relation>> relationsToChildren = new HashMap<>();
	
	public Relations() {
		
	}

	
	public TreeSet<Relation> getAllRelationsToChildren() {
		TreeSet<Relation> allRelations = new TreeSet<>();

		for (String relationType : relationsToChildren.keySet()){
			TreeSet<Relation> rels = relationsToChildren.get(relationType);
			allRelations.addAll(rels);	
		}
		
		return allRelations;
	}

	
	public TreeSet<Relation> getAllRelationsToParents() {
		TreeSet<Relation> allRelations = new TreeSet<>();

		for (String relationType : relationsToParents.keySet()){
			TreeSet<Relation> rels = relationsToParents.get(relationType);
			allRelations.addAll(rels);	
		}
		
		return allRelations;
	}

	
	public TreeSet<Relation> getRelationsToChildren(String... relationTypes) {
		
		TreeSet<Relation> allRelations = new TreeSet<>();

		for (String relType : relationTypes){
			if (relationsToChildren.containsKey(relType)){
				TreeSet<Relation> rels = relationsToChildren.get(relType);
				allRelations.addAll(rels);
			}
		}
		
		return allRelations;
		
	}

	public static TreeSet<Relation> filterRelationsOnAnnotation(TreeSet<Relation> relations, String annotationType, String... allowedAnnotationValues) {
		TreeSet<Relation> returnSet = new TreeSet<>();

		for (Relation rel : relations){
			for (String allowedAnnValue : allowedAnnotationValues){
				if (rel.getAnnotation(annotationType).equals(allowedAnnValue)){
					//match
					returnSet.add(rel);
				}
			}
		}
		
		return returnSet;
	}
	
	
	public TreeSet<Relation> getRelationsToParents(String... relationTypes) {
		TreeSet<Relation> allRelations = new TreeSet<>();

		for (String relType : relationTypes){
			if (relationsToParents.containsKey(relType)){
				TreeSet<Relation> rels = relationsToParents.get(relType);
				allRelations.addAll(rels);
			}
		}
		
		return allRelations;
	}
	
	public void addRelationToChild(Relation relationToChild){
		String relType = relationToChild.getType();
		if (!relationsToChildren.containsKey(relType))
			relationsToChildren.put(relType, new TreeSet<>());
		this.relationsToChildren.get(relType).add(relationToChild);
	}
	
	public void addRelationToParent(Relation relationToParent){
		String relType = relationToParent.getType();
		if (!relationsToParents.containsKey(relType))
			relationsToParents.put(relType, new TreeSet<>());
		this.relationsToParents.get(relType).add(relationToParent);
	}
}
