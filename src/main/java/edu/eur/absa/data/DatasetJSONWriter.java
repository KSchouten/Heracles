package edu.eur.absa.data;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.json.JSONObject;

import edu.eur.absa.Framework;
import edu.eur.absa.algorithm.AbstractAlgorithm;
import edu.eur.absa.model.DataEntity;
import edu.eur.absa.model.Dataset;
import edu.eur.absa.model.Relation;
import edu.eur.absa.model.Span;
import edu.eur.absa.model.Word;
import edu.eur.absa.nlp.NLPTask;

/**
 * This DataWriter allows you to save the fully processed Dataset to a JSON file, so you can later restore it without 
 * going through the whole time-consuming NLP again.  
 * @author Kim Schouten
 *
 */
public class DatasetJSONWriter implements IDataWriter{

	private boolean prettyJSON = false;
	
	public DatasetJSONWriter(){
	}
	
	public DatasetJSONWriter(boolean prettyJSON){
		this.prettyJSON = prettyJSON;
	}
	
	@Override
	public void write(Dataset dataset, File file) throws IOException {
		Framework.debug("DatasetJSONWriter: Start writing " + file + "...");
		JSONObject datasetJSON = new JSONObject();
		
		datasetJSON.put("textualUnitSpanType", dataset.getTextualUnitSpanType());
		for (NLPTask performedTask : dataset.getPerformedNLPTasks()){
			datasetJSON.append("performedNLPTasks", performedTask);
		}
		
		JSONObject annotationDataTypesJSON = new JSONObject();
		datasetJSON.put("annotationDataTypes", annotationDataTypesJSON);
		for (String annotationType : dataset.getAnnotationDataTypes().keySet()){
			annotationDataTypesJSON.put(annotationType, dataset.getAnnotationDataTypes().get(annotationType).getName());
		}
		
		for (int id = 1; id <= dataset.getCurrentId(); id++){
			DataEntity a = dataset.getAnnotatable(id);
			JSONObject annJSON = new JSONObject();
			if (a instanceof Word){
				Word word = (Word)a;
				annJSON.put("startOffset", word.getStartOffset());
				annJSON.put("word",word.getWord());
				annJSON.put("order", word.getOrder());
				annJSON.put("id", word.getId());
				annJSON.put("textualUnitId", word.getTextualUnit().getId());
//				wordJSON.put("annotations", word.getAnnotations());
				datasetJSON.append("words", annJSON);
				
			}
			if (a instanceof Span){
				Span span = (Span)a;
				annJSON.put("id", span.getId());
				annJSON.put("spanType", span.getType());
				if (span.size() == 0){
					Framework.debug("Span without words: (" + span.getId() + ") " + span+"\nTextual Unit: "+span.getTextualUnit());
				}
				annJSON.put("firstWordId", span.first().getId());
				annJSON.put("lastWordId", span.last().getId());
				annJSON.put("textualUnitId", span.getTextualUnit().getId());
//				spanJSON.put("annotations", span.getAnnotations());
				if (span.getTextualUnit().getId()==span.getId()){
					datasetJSON.append("textualUnits", annJSON);
				} else {
					datasetJSON.append("spans",annJSON);
				}
			}
			if (a instanceof Relation){
				Relation rel = (Relation)a;
				annJSON.put("id", rel.getId());
				annJSON.put("type", rel.getType());
				annJSON.put("parentId", rel.getParent().getId());
				annJSON.put("childId", rel.getChild().getId());
				datasetJSON.append("relations",annJSON);
			}
			JSONObject annotationsJSON = new JSONObject();
			annJSON.put("annotations", annotationsJSON);
			if (a == null){
				Framework.debug("Removed id: "+id);
			} else {
				for (String annotationKey : a.getAnnotationTypes()){
					annotationsJSON.put(annotationKey, (Object)a.getAnnotation(annotationKey));
					
				}
			}
			
		}
		
		BufferedWriter out;
		out = new BufferedWriter(new FileWriter(file));
		if (prettyJSON){
			out.write(datasetJSON.toString(2));
		} else {
			out.write(datasetJSON.toString());
		}
		out.close();
	}

	@Override
	public void write(Dataset dataset, AbstractAlgorithm alg, File file) throws IOException {
		throw new IOException("Saving predictions in this file format is not supported");
	}

	@Override
	public boolean supportsWritingPredictions() {
		return false;
	}
	
	

}
