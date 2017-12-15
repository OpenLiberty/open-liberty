/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.tck;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import com.ibm.websphere.simplicity.PortType;
import componenttest.topology.impl.LibertyServer;

/**
 * A set of mostly static utility functions.
 */
public class Utils {

	public static File home;
	public static String wlp;
	public static File tckRunnerDir;
	public static boolean init;
	public static String[] mvnCliRaw;
	public static String[] mvnCliRoot;
	public static String[] mvnCliTckRoot;
	static List<String> jarsFromWlp = new ArrayList<String>(3);

	/**
	 * Initialise shared values for a particular server. This enables us to set up
	 * things like the Liberty install directory and jar locations once.
	 *
	 * @param server
	 * @throws Exception
	 */
	public static void init(LibertyServer server) throws Exception {
		wlp = server.getInstallRoot();
		home = new File(System.getProperty("user.dir"));
		tckRunnerDir = new File("publish/tckRunner");

		mvnCliRaw = new String[] { "mvn", "clean", "test", "-Dwlp=" + wlp, "-Dtck_server=" + server.getServerName(),
				"-Dtck_port=" + server.getPort(PortType.WC_defaulthost),
				"-DtargetDirectory=" + home.getAbsolutePath() + "/results/tck" };

		// These are jars that we try to resolve independent of Liberty version numbers. 
		// On finding a match path, we export it as an ENV variable which is then
		// included as the <systemPath> element for the mvn coordinates of the dependency
		// in tck/pom.xml
		jarsFromWlp.add("com.ibm.websphere.org.eclipse.microprofile.faulttolerance");
		jarsFromWlp.add("com.ibm.ws.microprofile.faulttolerance");
		jarsFromWlp.add("com.ibm.ws.microprofile.faulttolerance.cdi");
		jarsFromWlp.add("com.ibm.websphere.org.eclipse.microprofile.config");
		jarsFromWlp.add("com.ibm.ws.microprofile.config");
		jarsFromWlp.add("com.ibm.ws.microprofile.config.cdi");

		mvnCliRoot = concatStringArray(mvnCliRaw, getJarCliEnvVars(server, jarsFromWlp));

		// The cmd below is a base for running the TCK as a whole for this project.
		// It is possible to use other Testng control xml files that do useful inclusions
		// and exclusions and there are some examples (see *.xml) in the tck directory
		mvnCliTckRoot = concatStringArray(mvnCliRoot, new String[] { "-DsuiteXmlFile=tck-suite.xml" });
		init = true;
	}

	/**
	 * Resolves a set of "-Djarname=path" type strings to add to the CLI. The path
	 * is resolved to existing jar names that match the jarName but also include
	 * version numbers etc.
	 *
	 * @param server
	 * @param nonVersionedJars
	 * @return an array of String that can be added to a ProcessRunner command
	 */
	private static String[] getJarCliEnvVars(LibertyServer server, List<String> nonVersionedJars) {

		Map<String, String> actualJarFiles = resolveJarPaths(nonVersionedJars, server);
		String[] addon = new String[] {};

		Set<String> jarsSet = actualJarFiles.keySet();
		for (Iterator<String> iterator = jarsSet.iterator(); iterator.hasNext();) {
			String jarKey = iterator.next();
			String jarPathName = actualJarFiles.get(jarKey);
			addon = concatStringArray(addon, new String[] { "-D" + jarKey + "=" + jarPathName });
		}
		return addon;
	}

	/**
	 * Smashes two String arrays into a returned third. Useful for appending to the
	 * CLI used for ProcessBuilder. This is not a very efficient way to do this but
	 * it is easy to read/write and it is not a performance sensitive area.
	 *
	 * @param a
	 * @param b
	 * @return cat a b
	 */
	public static String[] concatStringArray(String[] a, String[] b) {
		Stream<String> streamA = Arrays.stream(a);
		Stream<String> streamB = Arrays.stream(b);
		return Stream.concat(streamA, streamB).toArray(String[]::new);
	}

	/**
	 * Run a command using a ProcessBuilder.
	 *
	 * @param cmd
	 * @param workingDirectory
	 * @param outputFile
	 * @return The return code of the process. (TCKs return 0 if all tests pass and
	 *         !=0 otherwise).
	 * @throws Exception
	 */
	public static int runCmd(String[] cmd, File workingDirectory, File outputFile) throws Exception {
		ProcessBuilder pb = new ProcessBuilder(cmd);
		pb.directory(workingDirectory);
		pb.redirectOutput(outputFile);
		log("Running command " + Arrays.asList(cmd));
		Process p = pb.start();
		int exitCode = p.waitFor();
		return exitCode;
	}

	/**
	 * Find a set of jars in a LibertyServer
	 *
	 * @param jars
	 * @param server
	 * @return a Map that has the jars list parameter as the keySet and the resolved
	 *         paths as entries.
	 */
	public static Map<String, String> resolveJarPaths(List<String> jars, LibertyServer server) {
		HashMap<String, String> result = new HashMap<String, String>(jars.size());
		for (Iterator<String> iterator = jars.iterator(); iterator.hasNext();) {
			String jarName = iterator.next();
			String jarPath = resolveJarPath(jarName, server);
			result.put(jarName, jarPath);
		}
		return result;
	}

	/**
	 * Return a full path for a jar file name substr. This function enables the
	 * Liberty build version (which is often included in jar names) to increment and
	 * the FAT bucket to find the jar under the new version.
	 *
	 * @param jarName
	 * @param server
	 * @return
	 */
	public static String resolveJarPath(String jarName, LibertyServer server) {
		String wlp = server.getInstallRoot();
		String jarPath = genericResolveJarPath(jarName, wlp);
		return jarPath;
	}

	/**
	 * This is more easily unit testable and reusable version of the internals of
	 * resolveJarPath
	 *
	 * @param jarName
	 * @param wlpPathName
	 * @return the path to the jar
	 */
	public static String genericResolveJarPath(String jarName, String wlpPathName) {
		String dev = wlpPathName + "/dev/";
		String api = dev + "api/";
		String spi = dev + "spi/";
		String apiStable = api + "stable/";
		String lib = wlpPathName + "/lib/";

		ArrayList<String> places = new ArrayList<String>();
		places.add(apiStable);
		places.add(lib);

		String jarPath = null;
		for (Iterator<String> iterator = places.iterator(); iterator.hasNext();) {
			String dir = iterator.next();
			log("JAR: dir=" + dir);
			jarPath = jarPathInDir(jarName, dir);
			if (jarPath != null) {
				log("JAR: dir match=" + dir + jarPath);
				jarPath = dir + jarPath;
				break;
			}
		}
		return jarPath;
	}

	/**
	 * Looks for a path in a directory from a sub set of the filename
	 *
	 * @param jarNameFragment
	 * @param dir
	 * @return
	 */
	public static String jarPathInDir(String jarNameFragment, String dir) {
		String result = null;
		File dirFileObj = new File(dir);
		String[] files = dirFileObj.list();
		log("looking for jar " + jarNameFragment + " in dir " + dir);

		// Looking for (for example):
		// /libertyGit/open-liberty/dev/build.image/wlp/lib
		// $ find . -name "*tolerance*.jar"
		// ./com.ibm.ws.microprofile.faulttolerance.cdi_1.0.18.jar
		// ./com.ibm.ws.microprofile.faulttolerance_1.0.18.jar
		// ./com.ibm.ws.microprofile.faulttolerance.spi_1.0.18.jar
		for (int i = 0; i < files.length; i++) {
			if (files[i].contains(jarNameFragment)) {
				result = files[i];
				log("dir " + dir + " contains " + jarNameFragment + " as " + result);
				// Do we have a match ignoring version numbers
				String r = result.replaceAll(".jar", "");
				r = r.replaceAll("[0-9]", "");
				r = r.replaceAll("\\.", "");
				r = r.replaceAll("\\_", "");
				log("r testing " + r);
				if (r.equals(jarNameFragment.replaceAll("\\.", ""))) {
					log(jarNameFragment + " jar found in dir " + dir + result);
					return result;
				} else {
					log("close match failed for " + jarNameFragment + " in " + dir + result);
				}
			}
		}
		log("returning NOT FOUND for " + jarNameFragment);
		return null;
	}

	/**
	 * A simple log abstraction to enable easy grepping of messages
	 * 
	 * @param string
	 */
	public static void log(String string) {
		System.out.println("TCK: " + string);
	}

	/**
	 * A fairly standard fileTreeWalker Visitor that copies files to a new directory.
	 * Used for copying testng results to the simplicity junit directory
	 *
	 */
	public static class CopyFileVisitor extends SimpleFileVisitor<Path> {
		private Path src, tgt;

		public CopyFileVisitor(Path src, Path tgt) {
			this.src = src;
			this.tgt = tgt;
		}

		/* (non-Javadoc)
		 * @see java.nio.file.SimpleFileVisitor#visitFile(java.lang.Object, java.nio.file.attribute.BasicFileAttributes)
		 */
		public FileVisitResult visitFile(Path path, BasicFileAttributes attr) {
			Path dest = tgt.resolve(src.relativize(path));
			try {
				Files.copy(path, dest, StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
			return FileVisitResult.CONTINUE;
		}

		/* (non-Javadoc)
		 * @see java.nio.file.SimpleFileVisitor#preVisitDirectory(java.lang.Object, java.nio.file.attribute.BasicFileAttributes)
		 */
		public FileVisitResult preVisitDirectory(Path path, BasicFileAttributes fileAttributes) {
			Path dest = tgt.resolve(src.relativize(path));
			try {
				Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
			return FileVisitResult.CONTINUE;
		}

	}

}