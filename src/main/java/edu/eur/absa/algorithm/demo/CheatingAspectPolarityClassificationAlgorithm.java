package edu.eur.absa.algorithm.demo;

import java.util.ArrayList;
import java.util.HashSet;

import edu.eur.absa.algorithm.AbstractAlgorithm;
import edu.eur.absa.algorithm.Prediction;
import edu.eur.absa.evaluation.evaluators.AnnotationLabelEvaluator;
import edu.eur.absa.evaluation.evaluators.SpanLabelEvaluator;
import edu.eur.absa.model.DataEntity;
import edu.eur.absa.model.Span;

public class CheatingAspectPolarityClassificationAlgorithm extends AbstractAlgorithm {

	public CheatingAspectPolarityClassificationAlgorithm() {
		super("CheatingAspectPolarityClassificationAlgorithm","opinion");
		evaluators.add(new AnnotationLabelEvaluator("opinion","polarity"));
	}

	@Override
	protected void cleanAlgorithm() {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void preprocess() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void train() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void predict() {
		for (DataEntity a : getTestAnnotatables()){
			Prediction p = new Prediction(a);
			p.putAnnotation("polarity",a.getAnnotation("polarity"));
			
			if (!predictions.containsKey(a))
				predictions.put(a, new HashSet<Prediction>());
			predictions.get(a).add(p);
		}
		
	}

}
