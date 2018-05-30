package edu.eur.absa.algorithm.ontology;

import java.io.File;

import edu.eur.absa.Framework;
import edu.eur.absa.algorithm.AbstractAlgorithm;
import edu.eur.absa.algorithm.Experiment;
import edu.eur.absa.data.DatasetJSONReader;
import edu.eur.absa.data.DatasetJSONWriter;
import edu.eur.absa.data.SemEval2015Task12ABSAReader;
import edu.eur.absa.external.ReasoningOntology;
import edu.eur.absa.model.Dataset;
import edu.eur.absa.nlp.OntologyLookup;

public class ESWC2018 {

	public static void main(String[] args) throws Exception{
		// Note that this will use the file instead of the console out so you won't see much 
		Framework.fileInsteadOfConsole();
		// Supress all the nonsense that Jena by default dumps in the console.
		Framework.suppressJenaMessages();
		
		//If the raw SemEval XML data have not yet been processed (gone through NLP pipeline)
		// you need to uncomment this
//		SemEval2015Task12ABSAReader.main(null);
		
		/*
		 * Read SemEval 2015 ABSA and SemEval 2016 ABSA data (restaurants only)
		 */
		
		String ontology = "ontology.owl";
		
		Dataset train2015 =  (new DatasetJSONReader()).read(new File(Framework.DATA_PATH+"SemEval2015Restaurants-Train.json"));
		train2015.process(new OntologyLookup(null, ReasoningOntology.getOntology(Framework.EXTERNALDATA_PATH + ontology)), "review");
		Dataset test2015 =  (new DatasetJSONReader()).read(new File(Framework.DATA_PATH+"SemEval2015Restaurants-Test.json"));
		test2015.process(new OntologyLookup(null, ReasoningOntology.getOntology(Framework.EXTERNALDATA_PATH + ontology)), "review");
		
		Dataset train2016 =  (new DatasetJSONReader()).read(new File(Framework.DATA_PATH+"SemEval2016SB1Restaurants-Train.json"));
		train2016.process(new OntologyLookup(null, ReasoningOntology.getOntology(Framework.EXTERNALDATA_PATH + ontology)), "review");
		Dataset test2016 =  (new DatasetJSONReader()).read(new File(Framework.DATA_PATH+"SemEval2016SB1Restaurants-Test.json"));
		test2016.process(new OntologyLookup(null, ReasoningOntology.getOntology(Framework.EXTERNALDATA_PATH + ontology)), "review");
		
		ontology = "ontology.owl-Expanded.owl";
		int nrc = AspectSentimentSVMAlgorithm.RESTAURANTS;
		
		
		/*
		 * Defining the four algorithms from the paper
		 */
		AbstractAlgorithm OntBoW = new OntologySentimentAlgorithm("Ont+BoW","review")
			.setBinaryProperties("use_sentence","use_bow_backup")
			.setProperty("ont", ontology)
			.setProperty("ont_ns", "http://www.kimschouten.com/sentiment/restaurant");
		
		AbstractAlgorithm Ont = new OntologySentimentAlgorithm("Ont","review")
			.setBinaryProperties("use_sentence")
			.setProperty("ont", ontology)
			.setProperty("ont_ns", "http://www.kimschouten.com/sentiment/restaurant");
		
		AbstractAlgorithm BoW = new AspectSentimentSVMAlgorithm("BoW", "review", false, nrc)
			.setBinaryProperties("use_stanford_sentence_sentiment", "use_review","predict_neutral", "use_category","use_hyperparameter_optimization","Xignore_validation_data")
			.setProperty("ont", ontology)	
			.setProperty("ont_ns", "http://www.kimschouten.com/sentiment/restaurant");	
				
		AbstractAlgorithm BoWOnt = new AspectSentimentSVMAlgorithm("BoW+Ont", "review", false, nrc)
			.setBinaryProperties("use_stanford_sentence_sentiment", "use_review","predict_neutral", 
					"use_category","use_hyperparameter_optimization","Xignore_validation_data",
					"use_ontology")
			.setProperty("ont", ontology)
			.setProperty("ont_ns", "http://www.kimschouten.com/sentiment/restaurant");
		
//		runExperimentTable1(Ont,BoW,OntBoW,BoWOnt,train2015);
//		runExperimentTable2(Ont,BoW,OntBoW,BoWOnt,train2016);
		runExperimentTable3(Ont,train2015,test2015,train2016,test2016);
//		runExperimentFigure5(Ont,BoW,OntBoW,BoWOnt,train2015,test2015);
//		runExperimentFigure6(Ont,BoW,OntBoW,BoWOnt,train2016,test2016);
//		runExperimentTable4(Ont,BoW,OntBoW,BoWOnt,train2015,test2015,train2016,test2016);
	}

	/**
	 * Experiment to get the results from Table 1
	 */
	public static void runExperimentTable1(AbstractAlgorithm Ont, 
			AbstractAlgorithm BoW, 
			AbstractAlgorithm OntBoW,
			AbstractAlgorithm BoWOnt,
			Dataset train2015) throws InstantiationException, IllegalAccessException {

		Framework.log("****************************");
		Framework.log("*** Results for Table 1  ***");
		Framework.log("****************************");
		
		Experiment.createNewExperiment()
			.addAlgorithms(Ont)//, BoW, OntBoW, BoWOnt)
			.setDataset(train2015)	
			.setCrossValidation(1, 10, 0.8, 0.2)
			.run();
	}
	
	/**
	 * Experiment to get the results from Table 2
	 */
	public static void runExperimentTable2(AbstractAlgorithm Ont, 
			AbstractAlgorithm BoW, 
			AbstractAlgorithm OntBoW,
			AbstractAlgorithm BoWOnt,
			Dataset train2016) throws InstantiationException, IllegalAccessException {

		Framework.log("****************************");
		Framework.log("*** Results for Table 2  ***");
		Framework.log("****************************");
		
		Experiment.createNewExperiment()
			.addAlgorithms(Ont, BoW, OntBoW, BoWOnt)
			.setDataset(train2016)	
			.setCrossValidation(1, 10, 0.8, 0.2)
			.run();
	}
		
	/**
	 * Experiments to get the results from Table 3 (only the Ont+Bow results)
	 * Left column first, then right colomn
	 */
	public static void runExperimentTable3(AbstractAlgorithm Ont,
			Dataset train2015,
			Dataset test2015,
			Dataset train2016,
			Dataset test2016) throws InstantiationException, IllegalAccessException {
		
		Framework.log("****************************");
		Framework.log("*** Results for Table 3  ***");
		Framework.log("****************************");
		
		Experiment.createNewExperiment()
			.addAlgorithms(Ont)
			.setTrainingAndTestSet(train2015, test2015, true, 0.8, 0.2)
			.run();
		Experiment.createNewExperiment()
			.addAlgorithms(Ont)
			.setTrainingAndTestSet(train2016, test2016, true, 0.8, 0.2)
			.run();
	}
	
	/**
	 * Experiment to get the results from Figure 5
	 */
	public static void runExperimentFigure5(AbstractAlgorithm Ont, 
			AbstractAlgorithm BoW, 
			AbstractAlgorithm OntBoW,
			AbstractAlgorithm BoWOnt,
			Dataset train2015,
			Dataset test2015) throws InstantiationException, IllegalAccessException {
		
		Framework.log("****************************");
		Framework.log("*** Results for Figure 5 ***");
		Framework.log("****************************");
		
		for (double trainingDataSize = 1.0; trainingDataSize >= 0.0; trainingDataSize -= 0.1 ){
			Framework.log("==========================================================================");
			Framework.log("=== Training Data Size : " + trainingDataSize);
			Framework.log("==========================================================================");
			Experiment.createNewExperiment()
				.addAlgorithms(Ont, BoW, OntBoW, BoWOnt) //MOnt, MOntBow,baseline, BowMOnt)
			.setTrainingAndTestSet(train2015, test2015, false, 0.8*trainingDataSize, 0.2*trainingDataSize)
			.run();
		}
	}
		
	/**
	 * Experiment to get the results from Figure 6
	 */
	public static void runExperimentFigure6(AbstractAlgorithm Ont, 
			AbstractAlgorithm BoW, 
			AbstractAlgorithm OntBoW,
			AbstractAlgorithm BoWOnt,
			Dataset train2016,
			Dataset test2016) throws InstantiationException, IllegalAccessException {
		
		Framework.log("****************************");
		Framework.log("*** Results for Figure 6 ***");
		Framework.log("****************************");
		
		for (double trainingDataSize = 1.0; trainingDataSize >= 0.0; trainingDataSize -= 0.1 ){
			Framework.log("==========================================================================");
			Framework.log("=== Training Data Size : " + trainingDataSize);
			Framework.log("==========================================================================");
			Experiment.createNewExperiment()
				.addAlgorithms(Ont, BoW, OntBoW, BoWOnt) //MOnt, MOntBow,baseline, BowMOnt)
			.setTrainingAndTestSet(train2016, test2016, false, 0.8*trainingDataSize, 0.2*trainingDataSize)
			.run();
		}
	}
		
	/**
	 * Experiments to get the results from Table 4
	 * Left part first, then right part
	 */
	public static void runExperimentTable4(AbstractAlgorithm Ont, 
			AbstractAlgorithm BoW, 
			AbstractAlgorithm OntBoW,
			AbstractAlgorithm BoWOnt,
			Dataset train2015,
			Dataset test2015,
			Dataset train2016,
			Dataset test2016) throws InstantiationException, IllegalAccessException {
		
		Framework.log("****************************");
		Framework.log("*** Results for Table 4  ***");
		Framework.log("****************************");
		
		Experiment.createNewExperiment()
			.addAlgorithms(Ont, BoW)
			.setTrainingAndTestSet(train2015, test2015, true, 0.8, 0.2)
			.run();
		Experiment.createNewExperiment()
			.addAlgorithms(Ont, BoW)
			.setTrainingAndTestSet(train2016, test2016, true, 0.8, 0.2)
			.run();
	}
		
	
	

}
