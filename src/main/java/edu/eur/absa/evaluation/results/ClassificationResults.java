package edu.eur.absa.evaluation.results;

public class ClassificationResults extends EvaluationResults {

	private int truePos = 0;
	private int falsePos = 0;
	private int falseNeg = 0;
	
	public ClassificationResults(String label, int truePos, int falsePos, int falseNeg){
		super(label);
		this.truePos = truePos;
		this.falsePos = falsePos;
		this.falseNeg = falseNeg;
	}

	public int getTruePos(){
		return truePos;
	}
	public int getFalsePos(){
		return falsePos;
	}
	public int getFalseNeg(){
		return falseNeg;
	}
	
	@Override
	public String getEvaluationResults() {
		return "\n"+label+
				"\n\tTrue Positives: \t"+truePos+
				"\n\tFalse Positives:\t"+falsePos+
				"\n\tFalse Negatives:\t"+falseNeg+
				"\n\tPrecision:      \t"+getPrecision()+
				"\n\tRecall:         \t"+getRecall()+
				"\n\tF1:             \t"+getF1();
	}
	
	//maybe put these metrics in separate classes with an interface as well?
	public double getPrecision(){
		return (truePos + falsePos > 0) ? (double)truePos / (truePos+falsePos) : 0;
	}
	public double getRecall(){
		return (truePos + falseNeg > 0) ? (double)truePos / (truePos+falseNeg) : 0;
	}
	public double getFbeta(double beta){
		return (truePos + falsePos + falseNeg > 0) ? ((1 + beta*beta)*(double)truePos) / ((1 + beta * beta)* (double)truePos + beta*beta*(double)falseNeg + (double)falsePos) : 0;
	}
	public double getF1(){
		return getFbeta(1);
	}

	@Override
	public double getMainNumber() {
		return getF1();
	}
}
