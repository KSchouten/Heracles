package edu.eur.absa.external;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.eur.absa.Framework;

public class NRCReviewSentimentLexicon {

	private HashMap<String, Double> dictionary;		//key = word (not lemma), giving sentiment score
	
	
	public static final int RESTAURANTS_UNIGRAM = 0;
	public static final int RESTAURANTS_BIGRAM = 1;
	public static final int LAPTOPS_UNIGRAM = 2;
	public static final int LAPTOPS_BIGRAM = 3;
	
	
	
	public NRCReviewSentimentLexicon(int version){
		String filename="";
		switch(version){
			case LAPTOPS_UNIGRAM:
				filename = "Amazon-laptops-electronics-reviews-AFFLEX-NEGLEX-unigrams.txt";
				break;
			
			case LAPTOPS_BIGRAM:
				filename = "Amazon-laptops-electronics-reviews-AFFLEX-NEGLEX-bigrams.txt";
				break;
				
			case RESTAURANTS_UNIGRAM:
				filename = "Yelp-restaurant-reviews-AFFLEX-NEGLEX-unigrams.txt";
				break;
			
			case RESTAURANTS_BIGRAM:
				filename = "Yelp-restaurant-reviews-AFFLEX-NEGLEX-bigrams.txt";
				break;
		}
	
			
		
		dictionary = new HashMap<>();

        try
        {
            BufferedReader reader =  new BufferedReader(new FileReader(Framework.EXTERNALDATA_PATH + "NRCSentimentLexicons/"+filename));
            String line;
            
            while((line = reader.readLine()) != null){		
	            String[] cats = line.split("\t");
	            String word = cats[0];
	            double score = Double.parseDouble(cats[1]);
	            
	            dictionary.put(word, score);
            }
            
            
            reader.close();
        }
        catch(IOException | NumberFormatException e)
        {
            Logger.getLogger(NRCReviewSentimentLexicon.class.getName()).log(Level.SEVERE, null, e);
        }
		
	}
	
	
	
	public double getScore(String word){
		if (dictionary.containsKey(word))
			return dictionary.get(word);
		else
			return 0;
	}
	
	
	
	
}
