package edu.eur.absa.evaluation.results;

import java.util.HashMap;
import java.util.HashSet;

import edu.eur.absa.Framework;

public class GroupedClassificationResults extends EvaluationResults {

	private HashMap<String, Integer> truePos;
	private HashMap<String, Integer> falsePos;
	private HashMap<String, Integer> falseNeg;
	private HashSet<String> groups;
	
	public GroupedClassificationResults(String label, HashMap<String, Integer> truePos, 
			HashMap<String, Integer> falsePos, HashMap<String, Integer> falseNeg) {
		super(label);
		this.truePos = truePos;
		this.falsePos = falsePos;
		this.falseNeg = falseNeg;
		Framework.log(label+"\n"+truePos+"\n"+falsePos+"\n"+falseNeg);
		groups = new HashSet<String>();
		groups.addAll(truePos.keySet());
		groups.addAll(falsePos.keySet());
		groups.addAll(falseNeg.keySet());
	}
	
	public int getTruePos(String group){
		return truePos.get(group);
	}
	public int getFalsePos(String group){
		if (falsePos == null){
			Framework.log(label+"\n"+truePos+"\n"+falsePos+"\n"+falseNeg);
		}
		return falsePos.get(group);
	}
	public int getFalseNeg(String group){
		return falseNeg.get(group);
	}
	
	@Override
	public String getEvaluationResults() {
		String results = "";
		for (String group : groups){
			results += "\n\nResults for \""+group+"\""+
					"\n=================================================="+
					"\n\tTrue Positives: \t"+getTruePos(group)+
					"\n\tFalse Positives:\t"+getFalsePos(group)+
					"\n\tFalse Negatives:\t"+getFalseNeg(group)+
					"\n\tPrecision:      \t"+getPrecision(group)+
					"\n\tRecall:         \t"+getRecall(group)+
					"\n\tF1:             \t"+getF1(group);
			
		}
		return results;
	}

	//maybe put these metrics in separate classes with an interface as well?
	public double getPrecision(String group){
		return (getTruePos(group) + getFalsePos(group) > 0) ? (double)getTruePos(group) / (getTruePos(group)+getFalsePos(group)) : 0;
	}
	public double getRecall(String group){
		return (getTruePos(group) + getFalseNeg(group) > 0) ? (double)getTruePos(group) / (getTruePos(group)+getFalseNeg(group)) : 0;
	}
	public double getFbeta(String group, double beta){
		return (getTruePos(group) + getFalsePos(group) + getFalseNeg(group) > 0) ? ((1 + beta*beta)*(double)getTruePos(group)) / ((1 + beta * beta)* (double)getTruePos(group) + beta*beta*(double)getFalseNeg(group) + (double)getFalsePos(group)) : 0;
	}
	public double getF1(String group){
		return getFbeta(group, 1);
	}

	@Override
	public double getMainNumber() {
		return getF1("All");
	}
	
}
