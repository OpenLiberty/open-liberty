package com.ibm.ws.logging.flush.fat.printObjectTests;

public class DummyObject {
	
	public String small;
	public String string8192;
	public String string8193;
	
	DummyObject() {
		generateSmall();
		generate8192();
		generate8193();
	}
	
	private void generateSmall() {
		small = "smallStr";
	}
	
	private void generate8192() {
		String starter = "";
    	String string = "R";
    	for(int i=0; i < 8192; i++) {
    		starter = starter + string;
    	}
    	string8192 = starter;
	}
	
	private void generate8193() {
		String starter = "";
    	String string = "R";
    	for(int i=0; i < 8193; i++) {
    		starter = starter + string;
    	}
    	string8193 = starter;
	}
	
	public String toStringSmall() {
		return small;
	}
	
	public String toString8192() {
		return string8192;
	}
	
	public String toString8193() {
		return string8193;
	}

}
