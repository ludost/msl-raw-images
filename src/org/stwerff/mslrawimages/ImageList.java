package org.stwerff.mslrawimages;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.appengine.api.datastore.QueryResultIterator;
import com.google.code.twig.annotation.AnnotationObjectDatastore;
import com.google.code.twig.annotation.Id;

public class ImageList implements Serializable {
	private static final long serialVersionUID = 8291748219696403349L;
	private static final Logger log = Logger.getLogger("msl-raw-images");
	static final ObjectMapper om = new ObjectMapper();

	@Id String uuid = "";
	String json = "";
	public String getUuid() {
		return uuid;
	}
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}
	public String getJson() {
		return json;
	}
	public void setJson(String json) {
		this.json = json;
	}
	
	public void store(){
		AnnotationObjectDatastore datastore  = new AnnotationObjectDatastore();
		try {
			datastore.store(this);
		} catch (Exception e){
			log.warning("Couldn't open list:"+uuid+" -> "+e.getLocalizedMessage());
		}
	}
	public String toString(){
		try {
			return this.json;
		} catch (Exception e) {
			log.severe("Couldn't serialize ImageList:"+e.getLocalizedMessage());
		}
		return super.toString();
	}
	public JsonNode toNode(){
		try {
			return om.readTree(this.json);
		} catch (Exception e) {
			log.severe("Couldn't serialize ImageList:"+e.getLocalizedMessage());
		}
		return null;
	}
	public static ImageList getListByUuid(String uuid){
		AnnotationObjectDatastore datastore  = new AnnotationObjectDatastore();
		try {
			return datastore.load(ImageList.class,uuid);
		} catch (Exception e){
			log.warning("Couldn't open list:"+uuid+" -> "+e.getLocalizedMessage());
		}
		return null;
	}
	public static List<ImageList> getListsByUuid(String[] ids){
		AnnotationObjectDatastore datastore  = new AnnotationObjectDatastore();
		try{
			return new ArrayList<ImageList>(datastore.loadAll(ImageList.class, Arrays.asList(ids)).values());
		} catch (Exception e){
			log.warning("Couldn't open any lists!"+e.getLocalizedMessage());
		}
		return new ArrayList<ImageList>(0);
	}
	public static List<ImageList> getAllLists(){
		AnnotationObjectDatastore datastore  = new AnnotationObjectDatastore();
		try{
			QueryResultIterator<ImageList> iter = datastore.find(ImageList.class);
			List<ImageList> result = new ArrayList<ImageList>();
			while (iter.hasNext()){
				result.add(iter.next());
			}
			return result;
		} catch (Exception e){
			log.warning("Couldn't open any lists!"+e.getLocalizedMessage());
		}
		return new ArrayList<ImageList>(0);
	}
}
