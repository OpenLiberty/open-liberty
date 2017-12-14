/**
 *
 */
package com.ibm.ws.microprofile.tck;

import java.io.File;
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
 * A set of mostly static utility functions. This exists partly as there is more
 * than one test class in development that share these functions.
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

		jarsFromWlp.add("com.ibm.websphere.org.eclipse.microprofile.faulttolerance");
		jarsFromWlp.add("com.ibm.ws.microprofile.faulttolerance");
		jarsFromWlp.add("com.ibm.ws.microprofile.faulttolerance.cdi");
		jarsFromWlp.add("com.ibm.websphere.org.eclipse.microprofile.config");
		jarsFromWlp.add("com.ibm.ws.microprofile.config");
		jarsFromWlp.add("com.ibm.ws.microprofile.config.cdi");

		mvnCliRaw = new String[] { "mvn", "clean", "test", "-Dwlp=" + wlp, "-Dtck_server=" + server.getServerName(),
				"-Dtck_port=" + server.getPort(PortType.WC_defaulthost),
				"-DtargetDirectory=" + home.getAbsolutePath() + "/results/tck" };

		mvnCliRoot = concatStringArray(mvnCliRaw, getJarCliEnvVars(server, jarsFromWlp));

		// The cmd below is a base for running the TCK as a whole for this project.
		// It is possible to use other Testng control xml files (and even generate them
		// based on examining the TCK jar) in which case the value for suiteXmlFile
		// would
		// be different.
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
	 * @return an array of string that can be added to a ProcessRunner command
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
	 * it is easy to read/write and it is not a performance sensitive use case.
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
	 * Liberty build version which is often included in jar names to increment and
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
	 * This is more easily unit testable and reusable version of guts of
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
	 * TODO support regexes in the future?
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
			log(files[i]);
			if (files[i].contains(jarNameFragment)) {
				result = files[i];
				log("dir " + dir + " contains " + jarNameFragment + " as " + result);
				// We match on dots and letters only
				String r = result.replaceAll(".jar", "");
				r = r.replaceAll("[0-9]", "");
				r = r.replaceAll("\\.","");
				r = r.replaceAll("\\_","");
				log("r testing " + r);
				if (r.equals( jarNameFragment.replaceAll("\\.","") )) {
					log(jarNameFragment + " jar found in dir " +  dir + result);
					return result;
				}else {
					log("match failed");
				}
			}
		}
		log("JAR returning NOT FOUND" + jarNameFragment );
		return null;
	}

	/**
	 * A simple log abstraction to enable easy grepping of the logs.
	 * 
	 * @param string
	 */
	public static void log(String string) {
		System.out.println("TCK: " + string);
	}

}