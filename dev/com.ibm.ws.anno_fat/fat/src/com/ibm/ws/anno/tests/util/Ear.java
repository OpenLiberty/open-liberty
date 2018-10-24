package com.ibm.ws.anno.tests.util;

import java.util.ArrayList;

public class Ear {
	
	private ArrayList<War> wars = new ArrayList<War>();
	private String name;
	
	public Ear(String name) {
		this.setName(name);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public ArrayList<War> getWars() {
		return wars;
	}

	public void setWars(ArrayList<War> wars) {
		this.wars = wars;
	}

	public void addWar(War war) {
		wars.add(war);
	}	
}
