package edu.eur.absa.algorithm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import edu.eur.absa.Framework;
import edu.eur.absa.evaluation.evaluators.Evaluator;
import edu.eur.absa.evaluation.results.EvaluationResults;
import edu.eur.absa.model.DataEntity;
import edu.eur.absa.model.Dataset;
import edu.eur.absa.model.Span;

/**
 * AbstractAlgorithm is the template class that every Algorithm should subclass.
 * It comes loaded with useful features and methods that can be used in any Algorithm.
 * Following this template will ensure your algorithm can automatically be run and evaluated 
 * by the system.
 * 
 * @author Kim Schouten
 *
 */
public abstract class AbstractAlgorithm {

	/**
	 * The predictions that are created by an algorithm are stored here.
	 */
	protected HashMap<DataEntity, HashSet<Prediction>> predictions = new HashMap<>();
	/**
	 * The things that can be evaluated based on the output of the algorithm 
	 * for example, different evaluators can evaluate classification results or regression results.
	 * If your algorithm produces more than one type of annotation, you can simply add more
	 * evaluators so each type of output produced by your algorithm is properly evaluated.
	 */
	protected HashSet<Evaluator> evaluators = new HashSet<>();
	/**
	 * Features to cache, this is useful in e.g. cross-validation where an algorithm is run
	 * multiple times. Caching the textual features is a good time saver.
	 */
	protected HashMap<DataEntity, HashSet<String>> features = new HashMap<>();
	/**
	 * the training, validation, test splits of the data
	 */
	protected ArrayList<HashSet<Span>> dataSubSets = new ArrayList<>();
	/**
	 * The DataEntities that are in the test set are stored separately, this is used in the evaluation method
	 * The reason this is different from dataSubSets is that how you split your data might not be the same
	 * as what thing you are interested in and predicting.
	 */
	protected HashSet<DataEntity> testAnnotatables = new HashSet<>();
	/**
	 * A descriptive label for this algorithm which is used in printing the output.
	 */
	protected String label;
	/**
	 * Properties can be used to model different variants of the same algorithm without copying
	 * the whole algorithm class for only a small change, or changing the code all the time
	 */
	protected Properties properties = new Properties();
	/**
	 * The Span type which is used to divide the dataset for this algorithm
	 */
	protected String unitOfAnalysisSpanType;
	/**
	 * The Span which is the target for this algorithm. The training and test set should be sets of Spans of 
	 * this spanType.
	 */
	protected String targetSpanType;
	/**
	 * A reference to the dataset
	 */
	protected Dataset dataset;
	/**
	 * Every new Algorithm should at least specify a label and the unit of analysis. 
	 * By default, the targetSpanType is the same as the unitOfAnalysisSpanType
	 * @param label A descriptive label for this algorithm which is used in printing the output.
	 * @param unitOfAnalysisSpanType The Span type which is used to divide the dataset for this algorithm
	 */
	public AbstractAlgorithm(String label, String unitOfAnalysisSpanType) {
		this(label,unitOfAnalysisSpanType,unitOfAnalysisSpanType);
	}
	/**
	 * Constructor that allows targetSpanType to be different than unitOfAnalysisSpanType.
	 * The latter is used to split the dataset into training/test/etc.
	 * While the former is used to actually test against when evaluating.
	 * For instance, you can split the data by review, but since you want to evaluate whether you
	 * assigned the right aspectCategory spans to each sentence span, the targetSpanType = "sentence" 
	 * @param label
	 * @param unitOfAnalysisSpanType
	 * @param targetSpanType
	 */
	public AbstractAlgorithm(String label, String unitOfAnalysisSpanType, String targetSpanType) {
		this.label = label;
		this.unitOfAnalysisSpanType = unitOfAnalysisSpanType;
		this.targetSpanType = targetSpanType;
	}

	/**
	 * The high-level clean algorithm that cleans this algorithm so it is ready for another run
	 * (e.g., the next fold in cross-validation).
	 * Note that this method will call the cleanAlgorithm method which is in your own custom
	 * Algorithm class. Make sure you reset all essential variables in that method to avoid data leakage.
	 */
	public void clean(){
		predictions.clear();
		cleanAlgorithm();
	}
	
	/**
	 * A method to set the data subsets (e.g., training, validation, test set) for this Algorithm
	 * @param dataSubSets The data subsets for training, validation, testing, etc.
	 */
	public void setDataSubSets(ArrayList<HashSet<Span>> dataSubSets, Dataset dataset){
		this.dataset = dataset;
		this.dataSubSets = dataSubSets;
		testAnnotatables.clear();
		testAnnotatables.addAll(dataset.getSubSpans(getTestData(), this.targetSpanType));
		
//		for (DataEntity s : testAnnotatables){
//			if (s instanceof Span){
//				Framework.log(((Span)s).getType());
//			}
//		}
//		for (Span s : getTestData()){
//			testAnnotatables.add(s);
//		}
	}
	
	/**
	 * This is the local clean algorithm, as referred to by the global clean() method. 
	 * This method should be implemented in each Algorithm.
	 * The clean() method, and thus cleanAlgorithm() as well, is called first when running an Algorithm
	 */
	protected abstract void cleanAlgorithm();
	
	/**
	 * The preprocess method is called after cleaning is performed and can be used to do some costly
	 * data preprocessing that does not have to be repeated over multiple runs.
	 * For instance, you can usually already extract the textual features from all pieces of text and 
	 * store them in <code>features</code>, as these normally do not change over different runs. 
	 */
	public abstract void preprocess();
	
	/**
	 * The train method is called after preprocessing and is used to train your Algorithm.
	 * Here, you can use the gold standard annotation to tune paramters, train machine learning models, etc.
	 * This method is repeated everytime the Algorithm is run. 
	 * Variables resulting from this method, like the trained model or parameters, have to be cleaned first
	 * to ensure valid results.
	 */
	public abstract void train();
	
	// when it is possible to serialize trained algorithms, it might be useful to add a predict(HashSet<Span> newTestData) function
	/**
	 * The predict method uses the trained model or tuned parameters from the train method to make predictions 
	 * on previously unseen data. 
	 * Your implementation should create <code>Prediction</code> objects which should be stored in the 
	 * <code>prediction</code> map as the evaluation method will look there for your results.
	 */
	public abstract void predict();

	/**
	 * Specify a specific Evaluator to evaluate the predictions of this Algorithm.
	 * @param evaluator The Evaluator to be used when evaluating this Algorithm
	 * @return An EvaluationResults object that contains the performance of this Algorithm
	 */
	public EvaluationResults evaluate(Evaluator evaluator){
		return evaluate(evaluator, getTestAnnotatables());
	}
	
	/**
	 * Evaluate this Algorithm, given an Evaluator and a test set.
	 * @param evaluator The Evaluator to be used when evaluating this Algorithm
	 * @param testAnnotatables The test set to compare the predictions against
	 * @return An EvaluationResults object that contains the performance of this Algorithm
	 */
	protected EvaluationResults evaluate(Evaluator evaluator, HashSet<? extends DataEntity> testAnnotatables){
		if (!evaluators.contains(evaluator)){
			return null;
		} else {
			return evaluator.evaluate(testAnnotatables, predictions, features);
		}
	}
	
	/**
	 * Execute this algorithm from scratch, including the preprocessing step
	 * @return A textual report of the evaluation results
	 */
	public String executeAndShowResults(){
		return executeAndShowResults(true);
	}
	
	/**
	 * Execute this algorithm and specify whether the preprocessing step has to be performed
	 * @param preprocess Perform the preprocess step if true
	 * @return A textual report of the evaluation results
	 */
	public String executeAndShowResults(boolean preprocess){
		clean();
		if (preprocess){
			preprocess();
		}
		train();
		predict();
		String output = "";
		for (Evaluator eval : evaluators){
			output += evaluate(eval).toString();
		}
		return output;
	}
	
	/**
	 * Execute this algorithm from scratch, including the preprocessing and training step.
	 * @return A set of EvaluationResults object that contain the evaluation results of each task
	 */
	public HashMap<Class<? extends Evaluator>, EvaluationResults> executeAndReturnResults(){
		return executeAndReturnResults(true, true);
	}
	
	/**
	 * Execute this algorithm and specify whether the preprocessing step has to be performed
	 * @param preprocess Perform the preprocess step if true
	 * @return A set of EvaluationResults object that contain the evaluation results of each task
	 */
	public HashMap<Class<? extends Evaluator>, EvaluationResults> executeAndReturnResults(boolean preprocess){
		return executeAndReturnResults(preprocess, true);
	}
	/**
	 * Execute this algorithm and specify whether the preprocessing and/or training step has to be performed
	 * @param preprocess Perform the preprocess step if true
	 * @param train Perform the train step if true
	 * @return A set of EvaluationResults object that contain the evaluation results of each task
	 */
	public HashMap<Class<? extends Evaluator>, EvaluationResults> executeAndReturnResults(boolean preprocess, boolean train){
		clean();
		HashMap<Class<? extends Evaluator>, EvaluationResults> results = new HashMap<>();
		if (preprocess){
			preprocess();
		}
		if (train){
			train();
		}
		predict();
		String output = "";
		for (Evaluator eval : evaluators){
			results.put(eval.getClass(), evaluate(eval));
		}
		return results;
	}

	/**
	 * Within an Algorithm, it is sometimes necessary to get the actual test set
	 * @return The test set
	 */
	protected HashSet<? extends DataEntity> getTestAnnotatables(){
		return testAnnotatables;
	}
	
	/**
	 * Get the data subset that corresponds to the test set
	 * @return The test data subset
	 */
	protected HashSet<Span> getTestData(){
		return dataSubSets.get(dataSubSets.size()-1);
	}
	/**
	 * Return all spans in a single HashSet (useful for the preprocess method)
	 * The distinction between training/test/etc set is lost, so be careful to
	 * not use this in the train() method
	 * @return All data as Spans of type <code>unitOfAnalysisSpanType</code>
	 */
	protected HashSet<Span> getCombinedData(){
		HashSet<Span> allData = new HashSet<>();
		for (HashSet<Span> subset : dataSubSets){
			allData.addAll(subset);
		}
		return allData;
	}
	/**
	 * Get all training data, which is all subsets (also for validation etc.) except the test data subset
	 * @return All subsets except the test set
	 */
	protected ArrayList<HashSet<Span>> getTrainingData(){
		ArrayList<HashSet<Span>> allTrainingSubSets = new ArrayList<>();
		allTrainingSubSets.addAll(dataSubSets);
		allTrainingSubSets.remove(getTestData());
		return allTrainingSubSets;
	}
	/**
	 * Get all training data from (maybe) different subsets into one set. If there is a validation set, it is
	 * combined with the training data
	 * @return All subsets extent the test set, but flattened into a single HashSet
	 */
	protected HashSet<Span> getCombinedTrainingData(){
		HashSet<Span> trainingData = new HashSet<>();
		for (int i = 0; i < dataSubSets.size()-1; i++){
			trainingData.addAll(dataSubSets.get(i));
		}
		return trainingData;
	}
	
	/**
	 * Method to encode the predictions into the data model so the resp. DataWriter can write it back to the proper file format
	 */
	public void annotateTestData(){
		
	}

	/**
	 * 
	 * @return Get the unit of analysis span, that is used to divide the data into subsets
	 */
	public String getUnitOfAnalysisSpanType(){
		return this.unitOfAnalysisSpanType;
	}
	
	/**
	 * 
	 * @return Get the set of Evaluators used to evaluate this Algorithm
	 */
	public HashSet<Evaluator> getEvaluators(){
		return this.evaluators;
	}
	
	/**
	 * 
	 * @param s The Span for which predictions were made
	 * @return The predictions that fall within this Span.
	 */
	public HashSet<Prediction> getPrediction(DataEntity s){
		return predictions.get(s);
	}
	public Set<DataEntity> getPredictionTargets(){
		return predictions.keySet();
	}
	
	/**
	 * 
	 * @return The descriptive label of this Algorithm
	 */
	public String getLabel(){
		return label + " " + properties;
	}
	
	//Properties related methods to make it easier to use them without having to specify "properties." all the time
	/**
	 * Set a property of this Algorithm, so you can have different variants of the same Algorithms
	 * @param key The property key
	 * @param value The property value
	 * @return This Algorithm, so you chain these methods
	 */
	public AbstractAlgorithm setProperty(String key, String value){
		properties.setProperty(key, value);
		return this;
	}
	/**
	 * Set a binary property for this Algorithm, which are simply flags that are present or not
	 * Only the presence of the property key is tested, so the value is empty
	 * @param keys The property key
	 * @return This Algorithm, so you chain these methods
	 */
	public AbstractAlgorithm setBinaryProperties(String... keys){
		for (String key : keys){
			properties.setProperty(key, "");
		}
		return this;
	}
	/**
	 * Get whether a given property key is present or not. This is mostly used with
	 * binary properties.
	 * @param key The property key to check 
	 * @return True when this key is present, false otherwise
	 */
	public boolean hasProperty(String key){
		return properties.containsKey(key);
	}
	/**
	 * Get the value corresponding to a given property key. This is only useful for
	 * non-binary properties.
	 * @param key The property key to get the value of
	 * @return The value that corresponds to the given property key
	 */
	public String getProperty(String key){
		return properties.getProperty(key);
	}
	/**
	 * Get the value corresponding to a given property key. This is only useful for
	 * non-binary properties. You can supply a default value to use when the given key
	 * is not present. This is useful to prevent having to put getProperty() within
	 * an if block to check for its existence first.
	 * @param key The property key to get the value of
	 * @param defaultValue The value to use when the key is absent
	 * @return The value for the given key if present, provided default value otherwise
	 * 
	 */
	public String getPropertyOrDefault(String key, String defaultValue){
		return properties.getProperty(key, defaultValue);
	}
	
	public void copyPropertiesFrom(AbstractAlgorithm toCopyFrom){
		this.properties.putAll(toCopyFrom.properties);
	}
}
