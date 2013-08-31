package com.github.rosjava.android_apps.map_manager;

public class MapListData {

	private String text = null;
	private boolean isChecked = false;
	private int id;
	
	public String getText() {
		return text;
	}
	public int getId() {
		return id;
	}
	
	public boolean isChecked() {
		return isChecked;
	}
	
	public void setText(String text) {
		this.text = text;
	}
	
	public void setId(int id) {
		this.id = id;
	}
	
	public void setChecked(boolean isChecked) {
		this.isChecked = isChecked;
	}
	
}