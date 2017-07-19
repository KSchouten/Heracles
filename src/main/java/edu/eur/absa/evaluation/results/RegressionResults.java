package edu.eur.absa.evaluation.results;

import java.util.ArrayList;

import edu.eur.absa.Framework;


public class RegressionResults<T extends Number> extends EvaluationResults {

	private ArrayList<T> predictedValues;
	private ArrayList<T> goldValues;
	
	public RegressionResults(String label, ArrayList<T> predictedValues, ArrayList<T> goldValues) {
		super(label);
		this.predictedValues = predictedValues;
		this.goldValues = goldValues;
		if (predictedValues.size() != goldValues.size()){
			Framework.error("The number of predictions should be equal to the number of gold values.");
		}
	}

	@Override
	public String getEvaluationResults() {
		return "\n"+label
				+"\n\tCosine distance: \t"+getCosineDistance()
//				+"\n\tPredicted values: \t"+predictedValues+
//				+"\n\tGold values:      \t"+goldValues
				;
	}
	
	public double getCosineDistance(){
		double numerator = 0;
		double denomGold = 0;
		double denomPred = 0;
		
		for (int i = 0; i < predictedValues.size(); i++){
			
			numerator += predictedValues.get(i).doubleValue() * goldValues.get(i).doubleValue();
			denomGold += Math.pow(goldValues.get(i).doubleValue(),2);
			denomPred += Math.pow(predictedValues.get(i).doubleValue(),2);
		}
		double cosine = numerator / (Math.sqrt(denomGold) * Math.sqrt(denomPred));
		return cosine;
	}

	@Override
	public double getMainNumber() {
		return getCosineDistance();
	}
	
	

}
