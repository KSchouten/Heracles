package edu.eur.absa.algorithm.demo;

import java.io.File;

import edu.eur.absa.Framework;
import edu.eur.absa.algorithm.AbstractAlgorithm;
import edu.eur.absa.algorithm.Experiment;
import edu.eur.absa.algorithm.ontology.OntologySentimentAlgorithm;
import edu.eur.absa.data.DatasetJSONReader;
import edu.eur.absa.external.ReasoningOntology;
import edu.eur.absa.model.Dataset;
import edu.eur.absa.nlp.OntologyLookup;

public class DemoExperiment {

	public static void main(String[] args) throws Exception {
		Dataset train2015 =  (new DatasetJSONReader()).read(new File(Framework.DATA_PATH+"SemEval2015Restaurants-Train.json"));
		Dataset test2015 =  (new DatasetJSONReader()).read(new File(Framework.DATA_PATH+"SemEval2015Restaurants-Test.json"));
		
		AbstractAlgorithm cheat1 = new CheatingAspectCategoryClassificationAlgorithm();
		
		Experiment.createNewExperiment()
			.addAlgorithms(cheat1)
			.setDataset(train2015)	
			.setCrossValidation(1, 10, 0.8, 0.2)
			.run();
		
		AbstractAlgorithm cheat2 = new CheatingAspectPolarityClassificationAlgorithm();
		
		Experiment.createNewExperiment()
			.addAlgorithms(cheat2)
			.setTrainingAndTestSet(train2015, test2015, true, 0.8, 0.2)
			.run();
		
		
				
				
	}

}
