package edu.eur.absa.data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import edu.eur.absa.Framework;
import edu.eur.absa.model.DataEntity;
import edu.eur.absa.model.Dataset;
import edu.eur.absa.model.Relation;
import edu.eur.absa.model.Span;
import edu.eur.absa.model.Word;
import edu.eur.absa.model.exceptions.IllegalSpanException;
import edu.eur.absa.nlp.NLPTask;

public class DatasetJSONReader implements IDataReader {

	@Override
	public Dataset read(File file) throws IOException, ClassNotFoundException, JSONException, IllegalSpanException {
		Framework.debug("DatasetJSONReader: Start reading " + file + "...");
		BufferedReader in = new BufferedReader(new FileReader(file));
		String line;
		String json = "";
		while ((line = in.readLine()) != null){
			json += line + "\n";
		}
		in.close();
		
		JSONObject datasetJSON = new JSONObject(json.trim());
		Dataset dataset = new Dataset(file.getName(),datasetJSON.getString("textualUnitSpanType"));
		for (Object nlpTask : datasetJSON.getJSONArray("performedNLPTasks")){
			dataset.getPerformedNLPTasks().add(NLPTask.valueOf((String)nlpTask));
		}
		
		JSONObject annotationDataTypesJSON = datasetJSON.getJSONObject("annotationDataTypes");
		for (String annotationType : annotationDataTypesJSON.keySet()){
			dataset.getAnnotationDataTypes().put(annotationType, Class.forName(annotationDataTypesJSON.getString(annotationType)));
		}
		/**
		 * Order to read:
		 * 1. Textual spans, since they can be created empty and words have a hard link to them
		 * 2. Words, link them to their textual span
		 * 3. Other spans, find their words and link to textual span
		 * 4. Relations, as all necessary Annotatables should be loaded by now
		 * 
		 * Since Annotatables get a fresh id on object creation, keep a mapping of the old ids at
		 * hand to resolve the links in the dataset
		 */
		HashMap<Integer, DataEntity> oldIds = new HashMap<>();

		for (Object textualUnit : datasetJSON.getJSONArray("textualUnits")){
			Span textualSpan = new Span(dataset.getTextualUnitSpanType(), dataset);
			JSONObject textualSpanJSON = (JSONObject)textualUnit;
			//generic Annotatable stuff to do
			processAnnotatable(textualSpan, textualSpanJSON, oldIds);
		}
		for (Object wordObj : datasetJSON.getJSONArray("words")){
			JSONObject wordJSON = (JSONObject)wordObj;
			int textualUnitId = wordJSON.getInt("textualUnitId");
			Span textualUnit = (Span)oldIds.get(textualUnitId);
			Word word;
			if (textualUnit.size()==0){
				word = new Word(wordJSON.getString("word"),wordJSON.getInt("startOffset"),textualUnit, dataset);
			} else {
				word = new Word(wordJSON.getString("word"), wordJSON.getInt("startOffset"), textualUnit.last());
			}
			//generic Annotatable stuff
			processAnnotatable(word, wordJSON, oldIds);
		}
		if (datasetJSON.has("spans")){
			for (Object spanObj : datasetJSON.getJSONArray("spans")){
				JSONObject spanJSON = (JSONObject)spanObj;
				
				Word firstWord = (Word) oldIds.get(spanJSON.getInt("firstWordId"));
				Word lastWord = (Word) oldIds.get(spanJSON.getInt("lastWordId"));
				String spanType = spanJSON.getString("spanType");
				if (firstWord == null || lastWord == null){
					Framework.log(spanJSON.toString(2));
				}
				Span span = new Span(spanType, firstWord, lastWord);
				//generic Annotatable stuff
				processAnnotatable(span, spanJSON, oldIds);
			}
		}
		if (datasetJSON.has("relations")){
			for (Object relObj : datasetJSON.getJSONArray("relations")){
				JSONObject relJSON = (JSONObject)relObj;
				DataEntity parent = oldIds.get(relJSON.getInt("parentId"));
				DataEntity child = oldIds.get(relJSON.getInt("childId"));
				String type = relJSON.getString("type");
				Relation rel = new Relation(type, parent, child);
				//generic Annotatable stuff
				processAnnotatable(rel, relJSON, oldIds);
			}
		}
		
//		for (Object textualUnit : datasetJSON.getJSONArray("textualUnits")){
//			TreeMap<Integer, Word> wordByOrder = new TreeMap<>();
//			Span textualSpan = new Span(dataset.getTextualUnitSpanType(), dataset);
//			JSONObject textualSpanJSON = (JSONObject)textualUnit;
//			for (Object wordObj : textualSpanJSON.getJSONArray("words")){
//				JSONObject wordJSON = (JSONObject)wordObj;
//				Word word;
//				if (wordByOrder.isEmpty()){
//					word = new Word(wordJSON.getString("word"),wordJSON.getInt("startOffset"),textualSpan, dataset);
//					wordByOrder.put(word.getOrder(), word);
//				} else {
//					word = new Word(wordJSON.getString("word"), wordJSON.getInt("startOffset"), wordByOrder.get(wordByOrder.lastKey()));
//					wordByOrder.put(word.getOrder(), word);
//				}
//				JSONObject annotationsJSON = wordJSON.getJSONObject("annotations");
//				for (String annotationKey : annotationsJSON.keySet()){
//					word.getAnnotations().put(annotationKey, translateJSONToProperObject(dataset, annotationKey, annotationsJSON));
//				}
//			}
//			for (Object spanObj : textualSpanJSON.getJSONArray("spans")){
//				JSONObject spanJSON = (JSONObject)spanObj;
//				Span span;
//				if (spanJSON.getString("spanType").equalsIgnoreCase(dataset.getTextualUnitSpanType())){
//					//this is the span we already created earlier to put our words in
//					span = textualSpan;
//				} else {
//					Word firstWord = wordByOrder.get(spanJSON.getInt("first"));
//					Word lastWord = wordByOrder.get(spanJSON.getInt("last"));
//					String spanType = spanJSON.getString("spanType");
//					
//					span = new Span(spanType, firstWord, lastWord);
//				}
//				//now fix the annotations for both cases
//				JSONObject annotationsJSON = spanJSON.getJSONObject("annotations");
//				for (String annotationKey : annotationsJSON.keySet()){
//					span.getAnnotations().put(annotationKey, translateJSONToProperObject(dataset, annotationKey, annotationsJSON));
//				}
//			}
//			
//		}
		
		return dataset;
	}
	
	private void processAnnotatable(DataEntity ann, JSONObject annJSON, HashMap<Integer, DataEntity> oldIds){
		oldIds.put(annJSON.getInt("id"), ann);
		JSONObject annotationsJSON = annJSON.getJSONObject("annotations");
		for (String annotationKey : annotationsJSON.keySet()){
			ann.putAnnotation(annotationKey, translateJSONToProperObject(ann.getDataset(), annotationKey, annotationsJSON));
		}
	}
	
	private Object translateJSONToProperObject(Dataset dataset, String annotationKey, JSONObject annotationsJSON){
		//horrible manual exception since 0.0 is wrongly written to JSON as 0, so reading it back in gives
		//an Integer instead of a Double so a ClassCastException is thrown
		if (dataset.getAnnotationDataTypes().get(annotationKey).equals(Double.class)){
			return annotationsJSON.getDouble(annotationKey);
		} else if (dataset.getAnnotationDataTypes().get(annotationKey).equals(HashSet.class)){ 
			//HashSets are stored as JSONArrays and are not directly castable back to HashSets unfortunately.
			JSONArray arrayData = annotationsJSON.getJSONArray(annotationKey);
			HashSet<Object> objects = new HashSet<>();
			for (Object o : arrayData){
				objects.add(o);
			}
			//now cast to the right HashSet<?>
			return dataset.getAnnotationDataTypes().get(annotationKey).cast(objects);
		} else if (dataset.getAnnotationDataTypes().get(annotationKey).equals(ArrayList.class)){
			//ArrayLists are stored as JSONArrays and are not directly castable back to ArrayLists unfortunately.
			JSONArray arrayData = annotationsJSON.getJSONArray(annotationKey);
			ArrayList<Object> objects = new ArrayList<>();
			for (Object o : arrayData){
				objects.add(o);
			}
			//now cast to the right HashSet<?>
			return dataset.getAnnotationDataTypes().get(annotationKey).cast(objects);
		} else {
			return dataset.getAnnotationDataTypes().get(annotationKey).cast(annotationsJSON.get(annotationKey));
		}
		
	}


}
