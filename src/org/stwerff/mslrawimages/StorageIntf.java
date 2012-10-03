package org.stwerff.mslrawimages;

import java.io.Reader;
import java.io.Writer;

public interface StorageIntf {
	public String createFilename(int sol);
	public Reader getReader(String filename);
	public Writer getWriter(String filename,int count);
	public void finalize();
}
