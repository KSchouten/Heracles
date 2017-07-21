package edu.eur.absa.evaluation.evaluators;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import edu.eur.absa.algorithm.Prediction;
import edu.eur.absa.Framework;
import edu.eur.absa.evaluation.results.EvaluationResults;
import edu.eur.absa.evaluation.results.RegressionResults;
import edu.eur.absa.model.DataEntity;

public class AnnotationValueEvaluator<T extends Number> implements Evaluator {

	private String spanType;
	private String annotationType;
	private boolean failureAnalysis = false;
	
	public AnnotationValueEvaluator(String spanType, String annotationType, boolean failureAnalysis){
		this.spanType = spanType;
		this.annotationType = annotationType;
		this.failureAnalysis = failureAnalysis;
	}
	
	
	@SuppressWarnings("unchecked")
	@Override
	public EvaluationResults evaluate(HashSet<? extends DataEntity> testSet,
			HashMap<? extends DataEntity, HashSet<Prediction>> predictions, HashMap<? extends DataEntity, HashSet<String>> features) {

		ArrayList<T> predictedValues = new ArrayList<T>();
		ArrayList<T> goldValues = new ArrayList<T>();
		
		for (DataEntity testAnn : testSet){
			boolean hasGoldValue = testAnn.hasAnnotation(annotationType);
			
			if (hasGoldValue){
				T goldValue = (T) testAnn.getAnnotation(annotationType);
				if (predictions.containsKey(testAnn)){
					HashSet<Prediction> predictionSet = predictions.get(testAnn);
					Prediction pred = predictionSet.iterator().next();
					T predictionValue = (T) pred.getAnnotation(annotationType);
					
					predictedValues.add(predictionValue);
					goldValues.add(goldValue);
					
					if (failureAnalysis && Math.abs((double)predictionValue - (double)goldValue)>0.2){
						Framework.log("Predicted: "+predictionValue);
						Framework.log("Gold: "+goldValue);
						Framework.log(testAnn.toString());
						Framework.log(features.get(testAnn)+"");
					}
					
				} else if (hasGoldValue){
					predictedValues.add((T) new Double(0.0));
					goldValues.add(goldValue);
					
					if (failureAnalysis){
						Framework.log("Predicted: Nothing!");
						Framework.log("Gold: "+goldValue);
						Framework.log(testAnn.toString());
						Framework.log(features.get(testAnn).toString());
					}
				}
			} else {
				//no gold value, so no eval possible -> let's annotate the data with the predicted value for future/separate eval
				if (predictions.containsKey(testAnn)){
					HashSet<Prediction> predictionSet = predictions.get(testAnn);
					Prediction pred = predictionSet.iterator().next();
					T predictionValue = (T) pred.getAnnotation(annotationType);
				
					testAnn.putAnnotation("predicted-"+annotationType, predictionValue);
				}
			}
			
		}
		
		return new RegressionResults<T>(getLabel(), predictedValues, goldValues);
	}

	@Override
	public String getLabel() {
		return "Regression results for annotation label '"+annotationType+"':";
	}

}
