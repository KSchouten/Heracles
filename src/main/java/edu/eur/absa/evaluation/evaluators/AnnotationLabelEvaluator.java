package edu.eur.absa.evaluation.evaluators;

import java.util.HashMap;
import java.util.HashSet;

import edu.eur.absa.algorithm.Prediction;
import edu.eur.absa.Framework;
import edu.eur.absa.evaluation.results.ClassificationResults;
import edu.eur.absa.evaluation.results.EvaluationResults;
import edu.eur.absa.evaluation.results.GroupedClassificationResults;
import edu.eur.absa.model.Span;
import edu.eur.absa.model.DataEntity;

public class AnnotationLabelEvaluator implements Evaluator {

	private String spanType;
	private String annotationType;
	private boolean failureAnalysis = false;
	private boolean groupBy = false;
	
	
	public AnnotationLabelEvaluator(String spanType, String annotationType){
		this.spanType = spanType;
		this.annotationType = annotationType;
	}
	
	public AnnotationLabelEvaluator(String spanType, String annotationType, boolean failureAnalysis){
		this.spanType = spanType;
		this.annotationType = annotationType;
		this.failureAnalysis = failureAnalysis;
	}
	
	public AnnotationLabelEvaluator(String spanType, String annotationType, boolean failureAnalysis, boolean groupBy){
		this.spanType = spanType;
		this.annotationType = annotationType;
		this.failureAnalysis = failureAnalysis;
		this.groupBy = groupBy;
	}
	
	
	
	
	@Override
	public EvaluationResults evaluate(HashSet<? extends DataEntity> testSet,
			HashMap<? extends DataEntity, HashSet<Prediction>> predictions, HashMap<? extends DataEntity, HashSet<String>> features) {
		HashMap<String, Integer> truePos = new HashMap<>();
		HashMap<String, Integer> falsePos = new HashMap<>();
		HashMap<String, Integer> falseNeg = new HashMap<>();
		
//		int truePos=0;
//		int falsePos=0;
//		int falseNeg=0;
		
		for (DataEntity parentAnn : testSet){
			Span parentSpan = (Span)parentAnn;
			HashSet<Span> analysisSpans = new HashSet<>();
			if (parentSpan.getType().equals(spanType)){
				analysisSpans.add(parentSpan);
			} else {
				analysisSpans.addAll(parentSpan.getCoveredSpans(parentSpan.getDataset().getSpans(spanType)));
			}
			
			for (Span span : analysisSpans){
				boolean annotated = span.hasAnnotation(annotationType);
				String group = "";
				
					
				if (predictions.containsKey(span)){
					
					HashSet<Prediction> preds = predictions.get(span);
					//only a single prediction is performed for this type of problem
					
					Prediction singlePrediction = preds.iterator().next();
					boolean predicted = singlePrediction.hasAnnotation(annotationType); 
					if (groupBy){
						group = singlePrediction.getAnnotation("group");
					}
					
//					Framework.debug("Prediction found..."+singlePrediction.getAnnotations());
//					Framework.debug("Gold values: "+span.getAnnotations());
					boolean triggerFailureAnalysis = true;
					if (predicted && annotated){
						//check the values
						Object predObj = singlePrediction.getAnnotation(annotationType);
						Object annotObj = span.getAnnotation(annotationType);
						if (predObj.equals(annotObj)){
							truePos.put(group, truePos.getOrDefault(group,0)+1);
							if (groupBy)
								truePos.put("All", truePos.getOrDefault("All",0)+1);
							triggerFailureAnalysis = false;
						} else {
							falsePos.put(group, falsePos.getOrDefault(group,0)+1);
							falseNeg.put(group, falseNeg.getOrDefault(group,0)+1);
							if (groupBy){
								falsePos.put("All", falsePos.getOrDefault("All",0)+1);
								falseNeg.put("All", falseNeg.getOrDefault("All",0)+1);
							}
						}
					} else if (predicted && !annotated){
						falsePos.put(group, falsePos.getOrDefault(group,0)+1);
						if (groupBy){
							falsePos.put("All", falsePos.getOrDefault("All",0)+1);
						}
					} else if (!predicted && annotated){
						falseNeg.put(group, falseNeg.getOrDefault(group,0)+1);
						if (groupBy){
							falseNeg.put("All", falseNeg.getOrDefault("All",0)+1);
						}
	//					Main.debug("False neg:\n"+parentSpan.getAnnotations()+"\n"+singlePrediction.getAnnotations());
						
					}
					
					if (failureAnalysis && triggerFailureAnalysis){
						Framework.log("Predicted: "+singlePrediction.getAnnotation(annotationType));
						Framework.log("Gold: "+span.getAnnotation(annotationType));
						Framework.log(span.getTextualUnit().getAnnotation("text"));
						Framework.log(""+span.getTextualUnit());
						
						Span sentence = span.getTextualUnit().getCoveredSpans(span.getDataset().getSpans("sentence", span.first())).first();
						Framework.log(sentence.toString());
						Framework.log(span.toString());
						Framework.log(features.get(span)+"");
					}
					
				} else {
					//no prediction, check if it's a false neg
					if (annotated){
						falseNeg.put(group, falseNeg.getOrDefault(group,0)+1);
						if (groupBy){
							falseNeg.put("All", falseNeg.getOrDefault("All",0)+1);
						}
	//					Main.debug("No prediction found for: "+parentSpan);
//						if (failureAnalysis){
//							Framework.log("Predicted: Nothing!");
//							Framework.log("Gold: "+span.getAnnotations().get(annotationType));
//							Framework.log(span.getTextualUnit().getAnnotations().get("text"));
//							Framework.log(span.toString());
//							Framework.log(features.get(span).toString());
//						}
					}
					
					
					
				}
	//				Framework.log("TP: "+truePos+"\tFP: "+falsePos+"\tFN: "+falseNeg);
				
			}
		}
		if (!groupBy){
			return new ClassificationResults(getLabel(), truePos.getOrDefault("",0), falsePos.getOrDefault("",0), falseNeg.getOrDefault("",0));
		} else {
			return new GroupedClassificationResults(getLabel(), truePos, falsePos, falseNeg);
		}
		
	}

	@Override
	public String getLabel() {
		return "Classification results of annotation label '"+annotationType+"':";
	}

}
