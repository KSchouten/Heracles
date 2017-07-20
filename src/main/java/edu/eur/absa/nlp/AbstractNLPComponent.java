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
	 * After process() has determined whether this component can be run, it will call
	 * validatedProcess(), which should be implemented in each subclass of AbstractNLPComponent.
	 * This method contains the actual work being done.
	 * @param dataset The dataset to process
	 * @param spanType The type of Span this component operates on
	 */
	public abstract void validatedProcess(Dataset dataset, String spanType);
	/**
	 * This method first checks if this component can be executed, if not then stop. Otherwise, refer to the
	 * validatedProcess() method of the actual component.
	 * @param dataset The dataset to process
	 * @param spanType The type of Span this component operates on
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
	
	/**
	 * 
	 * @return The NLPTask this component is performing
	 */
	public NLPTask getTask() {
		return thisTask;
	}

	/**
	 * 
	 * @return The set of NLPTasks that have to be performed already before this component can be used
	 */
	public HashSet<NLPTask> getPrerequisiteTasks() {
		return prerequisites;
	}
}
