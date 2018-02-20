package edu.eur.absa;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * The Framework class sets up some application wide static variables and static methods that can be used 
 * throughout the framework
 * @author Kim Schouten
 *
 */
public class Framework {

	/**
	 * Setting up some paths for the framework to use
	 */
	public static final String PATH = System.getProperty("user.dir")+"/";
	public static final String RESOURCES_PATH = PATH+"src/main/resources/";
	public static final String OUTPUT_PATH = PATH + "output/";
	public static final String DATA_PATH = RESOURCES_PATH + "data/";
	public static final String EXTERNALDATA_PATH = RESOURCES_PATH + "externalData/";
	public static final String RAWDATA_PATH = RESOURCES_PATH + "data/raw/";
	public static final String LIB_PATH = RESOURCES_PATH + "lib/";
	
	//Debug, error, and log can be used to pipe all system output through one place.
	//If you every decide you need to send your output somewhere else, you can simply
	//update these methods.
	
	public static void debug(String message){
		System.out.println(message);
	}
	public static void error(String message){
		System.err.println(message);
	}
	public static void log(String message){
//		Logger.getGlobal().info(message);
		System.out.println(message);
	}
	
	/**
	 * Console output is saved to a file instead of just displayed below. 
	 * The console box has a limited amount of text it can display, so if you have much data, 
	 * it makes sense to save it to a file instead. This can also be helpful to keep track of your experiment.
	 */
	public static void fileInsteadOfConsole(){
		try {
			PrintStream out = new PrintStream(new FileOutputStream(new File(OUTPUT_PATH + System.currentTimeMillis() + "_console.txt")));
			System.setOut(out);
			
		} catch (IOException e){
			e.printStackTrace();
		}		
	}
	
	public static void supressJenaMessages() {
		Logger root = (Logger)LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
		root.setLevel(Level.INFO);
	}
}
