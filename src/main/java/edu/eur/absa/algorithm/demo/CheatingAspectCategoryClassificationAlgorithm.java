package edu.eur.absa.algorithm.demo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.TreeSet;

import edu.eur.absa.algorithm.AbstractAlgorithm;
import edu.eur.absa.algorithm.Prediction;
import edu.eur.absa.Framework;
import edu.eur.absa.evaluation.evaluators.AnnotationLabelEvaluator;
import edu.eur.absa.evaluation.evaluators.SpanLabelEvaluator;
import edu.eur.absa.model.Dataset;
import edu.eur.absa.model.Span;

public class CheatingAspectCategoryClassificationAlgorithm extends AbstractAlgorithm {

	
	public CheatingAspectCategoryClassificationAlgorithm() {
		super("CheatingAspectCategoryClassificationAlgorithm","sentence");
		evaluators.add(new SpanLabelEvaluator("opinion","category"));
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
		for (Span sentenceSpan : getTestData()){
			Dataset dataset = sentenceSpan.getDataset();
			Span textualUnit = sentenceSpan.getTextualUnit();
			TreeSet<Span> opinions = dataset.getSpans(textualUnit, "opinion");
			HashSet<Object> assignedLabels = new HashSet<>();
			TreeSet<Span> opinionsPerSentence = sentenceSpan.getCoveredSpans(opinions);
//			Main.debug("Number of opinions: "+opinionsPerSentence.size() + "\t\t\t" + sentenceSpan.getAnnotations().get("categories"));
			for (Span opinionSpan : opinionsPerSentence){
				if (!predictions.containsKey(sentenceSpan))
					predictions.put(sentenceSpan, new HashSet<Prediction>());
				
				Object goldLabelValue = opinionSpan.getAnnotation("category");
				if (!assignedLabels.contains(goldLabelValue)){
					Prediction p = new Prediction(sentenceSpan, "opinion", sentenceSpan.first().getOrder(), sentenceSpan.last().getOrder());
					p.putAnnotation("category", goldLabelValue);
					predictions.get(sentenceSpan).add(p);
					assignedLabels.add(goldLabelValue);
//					Main.debug("Assigned: "+goldLabelValue);
				} else {
//					Main.debug("Not assigned: "+goldLabelValue);
				}
			}
		}
		
			
		
		
	}



}
