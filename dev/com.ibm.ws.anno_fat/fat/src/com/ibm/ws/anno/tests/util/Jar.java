package com.ibm.ws.anno.tests.util;

import java.util.ArrayList;

public class Jar {
	
	private ArrayList<String> packageNames = new ArrayList<String>();
	private String name;
	
	public Jar(String name) {
		setName(name);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public ArrayList<String> getPackageNames() {
		return packageNames;
	}

	public void setPackageNames(ArrayList<String> packageNames) {
		this.packageNames = packageNames;
	}
	
	public void addPackageName(String packageName) {
		packageNames.add(packageName);
	}
	
}
