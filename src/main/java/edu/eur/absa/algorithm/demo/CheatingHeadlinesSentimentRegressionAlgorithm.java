package edu.eur.absa.algorithm.demo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;

import edu.eur.absa.algorithm.AbstractAlgorithm;
import edu.eur.absa.algorithm.Prediction;
import edu.eur.absa.evaluation.evaluators.AnnotationLabelEvaluator;
import edu.eur.absa.evaluation.evaluators.AnnotationValueEvaluator;
import edu.eur.absa.model.DataEntity;
import edu.eur.absa.model.Span;

public class CheatingHeadlinesSentimentRegressionAlgorithm extends
		AbstractAlgorithm {

	public CheatingHeadlinesSentimentRegressionAlgorithm() {
		super("CheatingHeadlinesSentimentRegressionAlgorithm","headline");
		evaluators.add(new AnnotationValueEvaluator<Double>("headline","sentiment",false));
//		evaluators.add(new AnnotationLabelEvaluator("headline","sentiment"));
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
		Random r = new Random();
		for (DataEntity a : getTestAnnotatables()){
			Prediction p = new Prediction(a);
			double random = (2*r.nextDouble())-1;
			double randomSmall = (r.nextDouble())-0.5;
			p.putAnnotation("sentiment",((Double)a.getAnnotation("sentiment")).doubleValue()+random);
//			p.getAnnotations().put("sentiment",random);
//			p.getAnnotations().put("sentiment",0.000001);
			
			if (!predictions.containsKey(a))
				predictions.put(a, new HashSet<Prediction>());
			predictions.get(a).add(p);
		}

	}

}
