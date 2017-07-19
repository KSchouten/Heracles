package edu.eur.absa.data;

import java.io.File;

import edu.eur.absa.model.Dataset;

public interface IDataReader {

	public Dataset read(File file) throws Exception;
}
