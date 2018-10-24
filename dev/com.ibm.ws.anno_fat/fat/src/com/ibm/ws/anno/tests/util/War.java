package com.ibm.ws.anno.tests.util;

import java.util.ArrayList;

public class War {
	
	private ArrayList<Jar> jars = new ArrayList<Jar>();
	private ArrayList<String> packageNames = new ArrayList<String>();
	private String name;
	
	public War(String name) {
		setName(name);
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}


	public ArrayList<Jar> getJars() {
		return jars;
	}

	public void setJars(ArrayList<Jar> jars) {
		this.jars = jars;
	}
	
	public void addJar(Jar jar) {
		jars.add(jar);
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
