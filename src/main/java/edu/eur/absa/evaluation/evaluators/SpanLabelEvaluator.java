package edu.eur.absa.evaluation.evaluators;

import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeSet;

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
	
	public SpanLabelEvaluator(String spanType, String spanLabel){
		this.spanLabel = spanLabel;
		this.spanType = spanType;
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
	@Override
	public EvaluationResults evaluate(HashSet<DataEntity> testSet, HashMap<DataEntity, HashSet<Prediction>> predictions,HashMap<DataEntity, HashSet<String>> features){
		int truePos=0;
		int falsePos=0;
		int falseNeg=0;
		
		
		
		for (DataEntity parentAnnotatable : predictions.keySet()){
			Span parentSpan = (Span)parentAnnotatable;
			Dataset dataset = parentSpan.getDataset();
			HashSet<Prediction> preds = predictions.get(parentSpan);
			HashSet<Object> predictedLabels = new HashSet<>();
			for (Prediction p : preds){
				predictedLabels.add(p.getAnnotation(spanLabel));
			}
			
			
			//get all spans within the textual unit of the parentSpan with type = spanType
			TreeSet<Span> golds = dataset.getSpans(parentSpan.getTextualUnit(), spanType);
			//only get spans out of this set that are covered by the parentSpan (e.g., only opinions under this sentence)
			golds = parentSpan.getCoveredSpans(golds);
			
			HashSet<Object> goldLabels = new HashSet<>();
			for (Span s : golds){
				goldLabels.add(s.getAnnotation(spanLabel));
			}
			
//			Main.debug("\n"+predictedLabels.toString());
//			Main.debug(goldLabels.toString());
			
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
