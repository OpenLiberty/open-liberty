package com.ibm.ws.wlp.mavenFeatures.model;

/**
 * Exception that occurred when generating a Maven repository from Liberty features
 */
public class MavenRepoGeneratorException extends Exception {

	private static final long serialVersionUID = 1288388481240271518L;

	public MavenRepoGeneratorException(String string, Exception e) {
		super(string, e);
	}

	public MavenRepoGeneratorException(String string) {
		super(string);
	}
	
}
