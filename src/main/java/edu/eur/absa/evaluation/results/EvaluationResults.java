package edu.eur.absa.evaluation.results;

public abstract class EvaluationResults {

	protected String label;
	
	protected EvaluationResults(String label){
		this.label = label;
	}
	
	public abstract double getMainNumber();
	
	public abstract String getEvaluationResults();
	
	@Override
	public String toString(){
		return getEvaluationResults();
	}
	
	public String getLabel(){
		return label;
	}
}
