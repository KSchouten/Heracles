package edu.eur.absa.evaluation.evaluators;

import java.util.HashMap;
import java.util.HashSet;

import edu.eur.absa.algorithm.Prediction;
import edu.eur.absa.evaluation.results.EvaluationResults;
import edu.eur.absa.model.DataEntity;
import edu.eur.absa.model.Span;

public interface Evaluator {

	public EvaluationResults evaluate(HashSet<? extends DataEntity> testSet,
			HashMap<? extends DataEntity, HashSet<Prediction>> predictions, HashMap<? extends DataEntity, HashSet<String>> features);

	
	
	public String getLabel();
}
