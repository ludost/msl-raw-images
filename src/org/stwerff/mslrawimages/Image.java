package org.stwerff.mslrawimages;

import com.fasterxml.jackson.databind.ObjectMapper;

public class Image {
	static final ObjectMapper om = new ObjectMapper();
	
	String name;
	String url;
	String thumbnailUrl;
	String type;
	long unixTimeStamp;
	String fileTimeStamp;
	String lastModified;
	String camera;
	int sol;

	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public String getThumbnailUrl() {
		return thumbnailUrl;
	}
	public void setThumbnailUrl(String thumbnailUrl) {
		this.thumbnailUrl = thumbnailUrl;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public long getUnixTimeStamp() {
		return unixTimeStamp;
	}
	public void setUnixTimeStamp(long unixTimeStamp) {
		this.unixTimeStamp = unixTimeStamp;
	}
	public String getFileTimeStamp() {
		return fileTimeStamp;
	}
	public void setFileTimeStamp(String fileTimeStamp) {
		this.fileTimeStamp = fileTimeStamp;
	}
	public String getLastModified() {
		return lastModified;
	}
	public void setLastModified(String lastModified) {
		this.lastModified = lastModified;
	}
	public String getCamera() {
		return camera;
	}
	public void setCamera(String camera) {
		this.camera = camera;
	}
	public int getSol() {
		return sol;
	}
	public void setSol(int sol) {
		this.sol = sol;
	}
	public String toString(){
		try {
			return om.writeValueAsString(this);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return super.toString();
	}
}
