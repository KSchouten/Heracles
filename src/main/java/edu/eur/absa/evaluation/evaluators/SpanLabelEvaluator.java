package edu.eur.absa.evaluation.evaluators;

import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeSet;

import edu.eur.absa.Framework;
import edu.eur.absa.algorithm.Prediction;
import edu.eur.absa.evaluation.results.ClassificationResults;
import edu.eur.absa.evaluation.results.EvaluationResults;
import edu.eur.absa.model.DataEntity;
import edu.eur.absa.model.Dataset;
import edu.eur.absa.model.Span;

/**
 * This evaluates only a single label of predicted spans. For this purpose, multiple spans with the same label are considered
 * the same.
 * Application: evaluating the performance of aspect category prediction on SemEval2015/2016 data.
 * @author Kim Schouten
 *
 */
public class SpanLabelEvaluator implements Evaluator {

	private String spanLabel;
	private String spanType;
	private boolean failureAnalysis = false;
	
	public SpanLabelEvaluator(String spanType, String spanLabel){
		this.spanLabel = spanLabel;
		this.spanType = spanType;
	}
	
	public SpanLabelEvaluator(String spanType, String spanLabel, boolean failureAnalysis){
		this.spanLabel = spanLabel;
		this.spanType = spanType;
		this.failureAnalysis = failureAnalysis;
	}
	
//	public void evaluate(Dataset dataset, HashSet<Prediction> predictions){
//		HashMap<Span, HashSet<Prediction>> predictionsPerParentSpan = new HashMap<>();
//		for (Span goldSpans : dataset.getSpans(spanType)){
//			predictionsPerParentSpan.put(goldSpans, new HashSet<>());
//		}
//		for (Prediction pred : predictions){
//			predictionsPerParentSpan.get(pred.getParentSpan()).add(pred);
//		}
//		evaluate(dataset, predictionsPerParentSpan);
//	
//	}
	
	public String getSpanType(){
		return spanType;
	}
	public String getSpanLabel(){
		return spanLabel;
	}
	@Override
	public EvaluationResults evaluate(HashSet<? extends DataEntity> testSet, HashMap<? extends DataEntity, HashSet<Prediction>> predictions,HashMap<? extends DataEntity, HashSet<String>> features){

		int truePos=0;
		int falsePos=0;
		int falseNeg=0;
		
		
		
//		for (DataEntity parentAnnotatable : predictions.keySet()){
		for (DataEntity parentAnnotatable : testSet){
			
			
			Span parentSpan = (Span)parentAnnotatable;
//			Framework.log(parentSpan.getType());
			Dataset dataset = parentSpan.getDataset();
			HashSet<Prediction> preds = predictions.getOrDefault(parentSpan, new HashSet<Prediction>());
			HashSet<Object> predictedLabels = new HashSet<>();
			for (Prediction p : preds){
				if (p.hasAnnotation(spanLabel))
					predictedLabels.add(p.getAnnotation(spanLabel));
			}
			
			
			//get all spans within the textual unit of the parentSpan with type = spanType
			TreeSet<Span> golds = dataset.getSpans(parentSpan.getTextualUnit(), spanType);
			//only get spans out of this set that are covered by the parentSpan (e.g., only opinions under this sentence)
			golds = parentSpan.getCoveredSpans(golds);
			
			HashSet<Object> goldLabels = new HashSet<>();
			for (Span s : golds){
				if (s.hasAnnotation(spanLabel))
					goldLabels.add(s.getAnnotation(spanLabel));
			}
			
			if (failureAnalysis){
				Framework.debug("\n"+predictedLabels.toString());
				Framework.debug(goldLabels.toString());
			}
			
			//order is important, do not change without careful thinking
			HashSet<Object> workingCopy = new HashSet<Object>();
			workingCopy.addAll(predictedLabels);
			workingCopy.removeAll(goldLabels);
			falsePos += workingCopy.size();
			
			workingCopy.addAll(predictedLabels);
			workingCopy.retainAll(goldLabels);
			truePos += workingCopy.size();
			
			workingCopy.addAll(goldLabels);
			workingCopy.removeAll(predictedLabels);
			falseNeg += workingCopy.size();
		}
		return new ClassificationResults(getLabel(), truePos, falsePos, falseNeg);
	}

@Override
public String getLabel() {
	return "Classification results of span label '"+spanLabel+"':";
}
	
	
}
