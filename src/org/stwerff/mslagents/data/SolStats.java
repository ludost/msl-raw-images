package org.stwerff.mslagents.data;

import java.io.Serializable;

public class SolStats implements Serializable {
	private static final long serialVersionUID = 5820895314015075546L;
	
	int sol;
	int nofImages;
	int nofFull;
	int nofDownScaled;
	int nofSubframe;
	int nofThumbnail;
	boolean incompleteHeads=false;
	
	public SolStats(int sol){
		this.sol=sol;
	}
	public int getSol() {
		return sol;
	}
	public void setSol(int sol) {
		this.sol = sol;
	}
	public void addGenImage(String type){
		if ("full".equals(type)) addFull();
		if ("downscaled".equals(type)) addDownScaled();
		if ("subframe".equals(type)) addSubframe();
		if ("thumbnail".equals(type)) addThumbnail();
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
	public boolean isIncompleteHeads() {
		return incompleteHeads;
	}
	public void setIncompleteHeads(boolean incompleteHeads) {
		this.incompleteHeads = incompleteHeads;
	}
	
}
