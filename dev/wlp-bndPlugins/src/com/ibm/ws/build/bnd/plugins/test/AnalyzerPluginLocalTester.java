package com.ibm.ws.build.bnd.plugins.test;

import java.io.File;

import aQute.bnd.osgi.Analyzer;

import com.ibm.ws.build.bnd.plugins.ImportlessPackager.ImportlessPackagerLevel0;

public class AnalyzerPluginLocalTester {

	// load a already build installer to exercise the  ImportlessPackagerLevel
	public static void main(String[] args) {
		Analyzer analyzer =  new Analyzer();
		String exclude = "org.w3c.dom,javax.xml*,javax.security.auth.x500, javax.management,javax.security.auth";
		File jarFile = new File("../ant_archiveToolingLapis.extract/build/lib/com.ibm.ws.ifix.template_1.0.jar");
		try {
			analyzer.setJar(jarFile);
			analyzer.setClasspath(new File[]{jarFile});
			analyzer.setProperty("importless.packager.excludes", exclude);
			analyzer.analyze();
			ImportlessPackagerLevel0.class.newInstance().analyzeJar(analyzer);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
