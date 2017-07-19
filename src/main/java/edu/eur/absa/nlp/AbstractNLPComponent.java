package edu.eur.absa.nlp;

import java.util.HashSet;

import edu.eur.absa.Framework;
import edu.eur.absa.model.Dataset;

public abstract class AbstractNLPComponent {

	protected NLPTask thisTask; 
	protected HashSet<NLPTask> prerequisites = new HashSet<>();
	/**
	 * You can set this to true in constructor to be able to overwrite a previous run.
	 * Do not use this for elementary components such as the Tokenizer.  
	 */
	protected boolean overwritePreviousRun = false;
	
	/**
	 * The process() method after validating that this component can actually be run
	 * @param dataset
	 * @param spanType
	 */
	public abstract void validatedProcess(Dataset dataset, String spanType);
	/**
	 * This method first checks if this component can be executed, if not then stop. Otherwise, refer to the
	 * validatedProcess() method of the actual component.
	 * @param dataset
	 * @param spanType
	 */
	public void process(Dataset dataset, String spanType){
		//check if task has already been done
		if (!overwritePreviousRun && dataset.getPerformedNLPTasks().contains(getTask())){
			Framework.error("This task has already been performed: " + thisTask);
			return;
		}
		//check if prerequisites are satisfied
		if (!dataset.getPerformedNLPTasks().containsAll(prerequisites)){
			HashSet<NLPTask> missingTasks = new HashSet<>();
			missingTasks.addAll(prerequisites);
			missingTasks.removeAll(dataset.getPerformedNLPTasks());
			Framework.error("This dataset does not meet the requirements to perform "+thisTask+"! Missing tasks: " + missingTasks);
			return;
		}
		validatedProcess(dataset, spanType);
	}
	
	public NLPTask getTask() {
		return thisTask;
	}

	public HashSet<NLPTask> getPrerequisiteTasks() {
		return prerequisites;
	}
}
