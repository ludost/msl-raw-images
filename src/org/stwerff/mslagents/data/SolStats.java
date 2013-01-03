package org.stwerff.mslagents.data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class SolStats implements Serializable {
	private static final long serialVersionUID = 5820895314015075546L;
	
	int sol;
	int nofImages;
	int nofFull;
	int nofDownScaled;
	int nofSubframe;
	int nofThumbnail;
	int nofUnknown;
	boolean incompleteHeads=false;
	Map<String,Boolean> incomplete;
	
	public SolStats(int sol){
		this.sol=sol;
		this.incomplete = new HashMap<String,Boolean>();
	}
	public int getSol() {
		return sol;
	}
	public void setSol(int sol) {
		this.sol = sol;
	}
	public void addGenImage(String type){
		if ("full".equals(type)) addFull();
		else if ("downscaled".equals(type)) addDownScaled();
		else if ("subframe".equals(type)) addSubframe();
		else if ("thumbnail".equals(type)) addThumbnail();
		else if ("unknown".equals(type)) addUnknown();
		else System.err.println("Unknown type: '"+type+"' found! sol:"+this.sol);
	}
	public void addImage(){
		this.nofImages++;
	}
	public int getNofImages() {
		return nofImages;
	}
	public void setNofImages(int nofImages) {
		this.nofImages = nofImages;
	}
	public void addFull(){
		this.nofFull++;
	}
	public int getNofFull() {
		return nofFull;
	}
	public void setNofFull(int nofFull) {
		this.nofFull = nofFull;
	}
	public void addDownScaled(){
		this.nofDownScaled++;
	}
	public int getNofDownScaled() {
		return nofDownScaled;
	}
	public void setNofDownScaled(int nofDownScaled) {
		this.nofDownScaled = nofDownScaled;
	}
	public void addSubframe(){
		this.nofSubframe++;
	}
	public int getNofSubframe() {
		return nofSubframe;
	}
	public void setNofSubframe(int nofSubframe) {
		this.nofSubframe = nofSubframe;
	}
	public void addThumbnail(){
		this.nofThumbnail++;
	}
	public int getNofThumbnail() {
		return nofThumbnail;
	}
	public void setNofThumbnail(int nofThumbnail) {
		this.nofThumbnail = nofThumbnail;
	}
	public void addUnknown(){
		this.nofUnknown++;
	}
	public int getNofUnknown() {
		return nofUnknown;
	}
	public void setNofUnknown(int nofUnknown) {
		this.nofUnknown = nofUnknown;
	}
	public boolean getStrange(){
		return (this.nofFull+this.nofSubframe+this.nofDownScaled+this.nofThumbnail+this.nofUnknown != this.nofImages);
	}
	public void setIncompleteHeads(boolean incompleteHeads){
		incomplete.put("heads", incompleteHeads);
	}
	public Map<String,Boolean> getIncomplete(){
		return incomplete;
	}
	public void setIncomplete(Map<String,Boolean> incomplete){
		this.incomplete=incomplete;
	}
	@JsonIgnore
	public boolean isIncomplete(String type) {
		if (this.incomplete == null) this.incomplete = new HashMap<String,Boolean>();
		if (this.incomplete.get(type) == null){
			if ("heads".equals(type)){
				setIncompleteHeads(this.incompleteHeads);
				return this.incompleteHeads;
			}
			return true;
		}
		return this.incomplete.get(type);
	}
	@JsonIgnore
	public void setIncomplete(String type, boolean incomplete) {
		if (this.incomplete == null) this.incomplete = new HashMap<String,Boolean>();
		this.incomplete.put(type,incomplete);
	}
	
}
