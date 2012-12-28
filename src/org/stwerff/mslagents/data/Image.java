package org.stwerff.mslagents.data;

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
	String lmst;
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
		String filename = this.name;
		try {
			char cmp = filename.charAt(16);
			if (cmp == '_') {
				cmp = filename.charAt(17);
			}
			if (cmp == 'U' || cmp == 'I' || cmp == 'T' || cmp == 'Q') {
				return "thumbnail";
			}
			if (cmp == 'D') {
				return "downscaled";
			}
			if (cmp == 'C' || cmp == 'R' || cmp == 'S') {
				return "subframe";
			}
			if (cmp == 'F' || cmp == 'B' || cmp == 'E' || cmp == 'K') {
				return "full";
			}
		} catch (Exception e) {
			System.out.println("Strange filename found:'" + filename + "'");
		}
		return "unknown";
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
	public String getLmst() {
		return lmst;
	}
	public void setLmst(String lmst) {
		this.lmst = lmst;
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
