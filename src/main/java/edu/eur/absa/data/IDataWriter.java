package edu.eur.absa.data;

import java.io.File;
import java.io.IOException;

import edu.eur.absa.model.Dataset;

public interface IDataWriter {

	public void write(Dataset dataset, File file) throws IOException;
}
