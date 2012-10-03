package org.stwerff.mslrawimages;

import java.io.BufferedReader;
import java.io.Reader;
import java.io.Writer;
import java.nio.channels.Channels;
import java.util.logging.Logger;

import com.google.appengine.api.files.AppEngineFile;
import com.google.appengine.api.files.FileReadChannel;
import com.google.appengine.api.files.FileService;
import com.google.appengine.api.files.FileServiceFactory;
import com.google.appengine.api.files.FileWriteChannel;
import com.google.appengine.api.files.GSFileOptions.GSFileOptionsBuilder;

public class GoogleCloudStorage implements StorageIntf {
	FileService fileService = FileServiceFactory.getFileService();
	FileWriteChannel writeChannel = null;
	Writer writer=null;
	AppEngineFile writableFile = null;
	private static final Logger log = Logger.getLogger("msl-raw-images");
	
	public String createFilename(int sol){
		return "/gs/msl-raw-images/sol_"+sol+".json";
	}
	
	@Override
	public Reader getReader(String filename) {
		boolean lockForRead = false;
		AppEngineFile readableFile = new AppEngineFile(filename);
		try {
			FileReadChannel readChannel = fileService.openReadChannel(readableFile, lockForRead);
			BufferedReader reader =
			        new BufferedReader(Channels.newReader(readChannel, "UTF8"));
			return reader;
		} catch (Exception e){
			log.severe("Couldn't read file:"+filename+ " -> "+e.getMessage());
		}
		return null;
	}

	@Override
	public Writer getWriter(String filename,int count) {
		GSFileOptionsBuilder optionsBuilder = new GSFileOptionsBuilder()
		  .setBucket("msl-raw-images")
		  .setCacheControl("public, max-age=600")
		  .setKey(filename)
		  .setAcl("public-read")
		  .setMimeType("application/json");
		if (count>0)optionsBuilder.addUserMetadata("imageCount",new Integer(count).toString());
		try {
			writableFile = fileService.createNewGSFile(optionsBuilder.build());
			boolean lockForWrite = false; // Do you want to exclusively lock this object?
			writeChannel = fileService.openWriteChannel(writableFile, lockForWrite);
			writer = Channels.newWriter(writeChannel, "UTF8");
			return writer;
		} catch (Exception e) {
			log.severe("Couldn't write file:"+filename+ " -> "+e.getLocalizedMessage()+e.toString());
		}
		return null;
	}
	public void finalize(){
		try {
			writer.flush();
			writeChannel.close();
			boolean lockForWrite = true; // Do you want to exclusively lock this object?
			writeChannel = fileService.openWriteChannel(writableFile, lockForWrite);
			writeChannel.closeFinally();
		} catch (Exception e) {
			log.severe("Couldn't finalize file -> "+e.getLocalizedMessage()+e.toString());
		}
	}
}
