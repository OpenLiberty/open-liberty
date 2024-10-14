/*******************************************************************************
 * Copyright (c) 2019, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.install.featureUtility.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;
import java.util.zip.ZipFile;
import java.util.stream.*;

import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.containers.GenericContainer;

import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.impl.LibertyFileManager;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import test.utils.TestUtils;

public abstract class FeatureUtilityToolTest {

    private static final Class<?> c = FeatureUtilityToolTest.class;

    protected static String libertyVersion = "23.0.0.2";
    // ${buildDir}/publish/repo
    protected static String mavenLocalRepo1 = Paths.get("publish/repo/").toAbsolutePath().toString();
    protected static String mavenLocalRepo2 = Paths.get("publish/repo2/").toAbsolutePath().toString();
    public static LibertyServer server;
    private static String installRoot;
    static String minifiedRoot;
    static String relativeMinifiedRoot;

    protected static List<String> cleanFiles;
    protected static List<String> cleanDirectories;
    private static Logger logger = Logger.getLogger("com.ibm.ws.install.featureUtility_fat");
    protected static String serverName = "featureUtility_fat";
    private static String zipFileDestination;
    private static String unzipDestination;

    private static Properties wlpVersionProps;
    private static Properties featureUtilityProps;
    private static String originalWlpVersion;
    private static String originalWlpEdition;
    private static String originalWlpInstallType;
    private static String originalWlpPublicKeyId;
    static boolean isClosedLiberty = false;
    private static String pathToAutoFVTTestFiles = "lib/LibertyFATTestFiles/";
    public static boolean isZos = System.getProperty("os.name").toLowerCase().contains("z/os") || System.getProperty("os.name").toLowerCase().contains("os/390");
    
    protected static void setupEnv() throws Exception {

        final String methodName = "setup";
        server = LibertyServerFactory.getLibertyServer("com.ibm.ws.install.featureUtility_fat");

	// dev/build.image/wlp
        installRoot = server.getInstallRoot();
        Log.info(c, methodName, "install root: " + installRoot);

        isClosedLiberty = isClosedLibertyWlp();
        Log.info(c, methodName, "Closed liberty wlp?: "+ isClosedLiberty);


        zipFileDestination = installRoot + "/../temp/wlp.zip";
        unzipDestination = installRoot + "/../featureUtility_fat_wlp/";

        // zip up installRoot
        relativeMinifiedRoot = "/../featureUtility_fat_wlp";
        minifiedRoot = exportWlp(installRoot, installRoot + "/../temp/wlp.zip", installRoot + relativeMinifiedRoot);
        Log.info(c, methodName, "minified root: " + minifiedRoot);

        Log.info(c, methodName, "mavenLocalRepo1 : " + mavenLocalRepo1.toString());
        Log.info(c, methodName, "mavenLocalRepo2 : " + mavenLocalRepo2.toString());

        if(!new File(minifiedRoot).exists()){
            throw new Exception("The minified root does not exist!");
        }

        setOriginalWlpVersionVariables();
        cleanDirectories = new ArrayList<String>();
        cleanFiles = new ArrayList<String>();

    }

    /**
     * Zip the installRoot and unzip it somewhere else
     * @return
     */
    private static String exportWlp(String installRoot, String zipFileDestination, String unzipDestination) throws IOException {
        String methodName = "exportWlp";
        Log.entering(c, methodName);
        // zip up installRoot
        File zf = new File(zipFileDestination);
        File uz = new File(unzipDestination);
        if(zf.exists()){
            // delete
            zf.delete();
        }
        if(uz.exists()){
            TestUtils.deleteFolder(uz);
        }
        zf.getParentFile().mkdirs();
        uz.mkdirs();

        TestUtils.zipDirectory(installRoot, zipFileDestination);
        ZipFile destFile = new ZipFile(zipFileDestination);
        TestUtils.unzipFileIntoDirectory(destFile, new File(unzipDestination));


        Log.exiting(c, methodName);
        return unzipDestination + "/wlp";
    }
    
    public String unzipToInstallRootFatWlp(String zipFile) throws IOException {
    	String fileDest = installRoot + "/../featureUtility_fat_wlp_repo/";
    	File uz = new File(fileDest);
    	if(uz.exists()){
            TestUtils.deleteFolder(uz);
        }
        uz.mkdirs();
    	ZipFile destFile = new ZipFile(pathToAutoFVTTestFiles + "/" + zipFile);
        TestUtils.unzipFileIntoDirectory(destFile, new File(fileDest));
        return fileDest + destFile.getName();
    }

    /**
     * Delete the wlp.zip and new feature utility wlp folder
     */
    public static void cleanUpTempFiles() throws IOException {
        String methodName ="cleanUpTempFiles";
        if(zipFileDestination != null){
            Log.info(c, methodName, "Cleaning up " + zipFileDestination);
            TestUtils.deleteFolder(new File(zipFileDestination));
        }
        if(unzipDestination != null){
            Log.info(c, methodName, "Cleaning up: " + unzipDestination);
            TestUtils.deleteFolder(new File(unzipDestination));
        }
    }


    /**
     * Same as LibertyServer.copyFileToLibertyInstallRoot except it uses our own installRoot
     * @param extendedPath
     * @param fileName
     * @throws Exception
     */
    private static void copyFileToLibertyInstallRoot(String root, String extendedPath, String fileName) throws Exception {
        LibertyFileManager.copyFileIntoLiberty(server.getMachine(), root + "/" + extendedPath, (pathToAutoFVTTestFiles + "/" + fileName));
    }


    /**
     * Same as LibertyServer.copyFileToLibertyInstallRoot except it uses minifedRoot
     * @param extendedPath
     * @param fileName
     * @throws Exception
     */
    public static void copyFileToMinifiedRoot(String extendedPath, String fileName) throws Exception {
	LibertyFileManager.copyFileIntoLiberty(server.getMachine(), minifiedRoot + "/" + extendedPath, fileName);
    }
    

    public static void writeToProps(String remoteFileName, String property, String value) throws Exception {
        OutputStream os = null;
        featureUtilityProps = new Properties();
        try {
            RemoteFile rf = server.getMachine().getFile(remoteFileName);
            os = rf.openForWriting(true);
            featureUtilityProps.setProperty(property, value);
            Log.info(c, "writeToProps", "Set the " + property + " to : " + value);
            featureUtilityProps.store(os, null);
            os.close();
        } finally {
            try {
                os.close();
            } catch (IOException e) {
                // ignore we are trying to close.
            }
        }
		
	}

    private static boolean isClosedLibertyWlp(){
        return new File(installRoot + "/lib/versions/WebSphereApplicationServer.properties").exists();
    }
//

    /**
     *
     * @return previous wlp version. ex: returns 19.0.0.11 if the current version is 19.0.0.12
     */
    protected static String getPreviousWlpVersion() throws IOException {
        String version = getCurrentWlpVersion();
        String [] split = version.split("\\.");
        String year = split[0];
        String month = split[3];

        String newYear, newMonth;
        if(month.equals("1")){
            // subtract year and go into december
            newMonth = "12";
            newYear = String.valueOf(Integer.parseInt(year) - 1);
        } else {
            // go back 1 month
            newMonth = String.valueOf(Integer.parseInt(month) - 1);
            newYear = year;
        }
        return String.format("%s.%s.%s.%s", newYear, split[1], split[2], newMonth);

    }



    // TODO discuss if we need this. may be useful for certain test cases like
    // mirror repo
    protected static void setEnvironmentVariable() {
//        ProcessBuilder pb = isWindows ? new ProcessBuilder("CMD", "/C", "SET") : new ProcessBuilder("")

    }

    protected static void removeEnvironmentVariable() {

    }

    private static void setOriginalWlpVersionVariables() throws IOException {
        File wlpVersionPropFile = new File(minifiedRoot + "/lib/versions/openliberty.properties");
        wlpVersionPropFile.setReadable(true);

        FileInputStream fIn = null;
        wlpVersionProps = new Properties();
        try {
            fIn = new FileInputStream(wlpVersionPropFile);
            wlpVersionProps.load(fIn);
            originalWlpVersion = wlpVersionProps.getProperty("com.ibm.websphere.productVersion");
            originalWlpEdition = wlpVersionProps.getProperty("com.ibm.websphere.productEdition");
            originalWlpInstallType = wlpVersionProps.getProperty("com.ibm.websphere.productInstallType");
	    originalWlpPublicKeyId = wlpVersionProps.getProperty("com.ibm.websphere.productPublicKeyId");

            Log.info(c, "getWlpVersion", "com.ibm.websphere.productVersion : " + originalWlpVersion);
            Log.info(c, "getWlpVersion",
                    "com.ibm.websphere.productId : " + wlpVersionProps.getProperty("com.ibm.websphere.productId"));
            Log.info(c, "getWlpVersion", "com.ibm.websphere.productEdition : " + originalWlpEdition);
            Log.info(c, "getWlpVersion", "com.ibm.websphere.productInstallType : " + originalWlpInstallType);
	    Log.info(c, "getWlpVersion", "com.ibm.websphere.productPublicKeyId : " + originalWlpPublicKeyId);
        } finally {
            try {
                assert fIn != null;
                fIn.close();
            } catch (IOException e) {
                // ignore we are trying to close.
            }
        }
    }

    public static String getCurrentWlpVersion() throws IOException {
        File wlpVersionPropFile = new File(minifiedRoot + "/lib/versions/openliberty.properties");
        wlpVersionPropFile.setReadable(true);

        FileInputStream fIn = null;
        wlpVersionProps = new Properties();
        try {
            fIn = new FileInputStream(wlpVersionPropFile);
            wlpVersionProps.load(fIn);
            return wlpVersionProps.getProperty("com.ibm.websphere.productVersion");

        } finally {
            try {
                assert fIn != null;
                fIn.close();
            } catch (IOException e) {
                // ignore we are trying to close.
            }
        }
    }

    public static String getClosedLibertyWlpEdition() throws IOException {
        File wlpVersionPropFile = new File(minifiedRoot + "/lib/versions/WebSphereApplicationServer.properties");
        Log.info(c, "getWlpEdition", "wlpVersionPropFile exists : " + wlpVersionPropFile.exists());
        if(!wlpVersionPropFile.exists()) return null;

        
        wlpVersionPropFile.setReadable(true);
        Properties wlpProps = new Properties();
        String wlpEdition = null;
        try(FileInputStream fIn2 = new FileInputStream(wlpVersionPropFile)){
            wlpProps.load(fIn2);
            wlpEdition = wlpProps.getProperty("com.ibm.websphere.productEdition");
            Log.info(c, "getWlpEdition", "com.ibm.websphere.productEdition : " + wlpEdition);
        }
        return wlpEdition;
    }
    
    public static void writeToProps(String remoteFileName, Map<String, String> map) throws Exception {
        OutputStream os = null;
        featureUtilityProps = new Properties();
        try {
            RemoteFile rf = server.getMachine().getFile(remoteFileName);
            os = rf.openForWriting(false);
            Set<String> keyset = map.keySet();
            for (String key: keyset) {
            	featureUtilityProps.setProperty(key, map.get(key));
            	Log.info(c, "writeToProps", "Set the " + key + " to : " + map.get(key));
            }
            featureUtilityProps.store(os, null);
            os.close();
        } finally {
            try {
                os.close();
            } catch (IOException e) {
                // ignore we are trying to close.
            }
        }

	}


    protected static void replaceWlpProperties(String version) throws Exception {
            RemoteFile rf = server.getMachine().getFile(minifiedRoot+ "/lib/versions/openliberty.properties");
	    try (OutputStream os = rf.openForWriting(false)) {
		wlpVersionProps.setProperty("com.ibm.websphere.productVersion", version);
		Log.info(c, "replaceWlpProperties", "Set the version to : " + version);
		wlpVersionProps.store(os, null);
	    }
	    // replace cl properties version if it exits
	    rf = server.getMachine().getFile(minifiedRoot + "/lib/versions/WebSphereApplicationServer.properties");
	    if (rf.exists()) {
		Properties wlProps = new Properties();
		try (InputStream is = rf.openForReading();) {
		    wlProps.load(is);
		    wlProps.setProperty("com.ibm.websphere.productVersion", version);
		    Log.info(c, "replaceWlpProperties - closed ", "Set the version to : " + version);
		}

		try (OutputStream os = rf.openForWriting(false)) {
		    wlProps.store(os, null);
		}
	    }

    }
    protected static void resetOriginalWlpProps() throws Exception {
        replaceWlpProperties(originalWlpVersion);
    }

    protected ProgramOutput runFeatureUtility(String testcase, String[] params) throws Exception {
        return runFeatureUtility(testcase, params, false);
    }

    protected ProgramOutput runFeatureUtility(String testcase, String[] params, boolean debug) throws Exception {
        Properties envProps = new Properties();
	      //add beta property here
	      envProps.put("JVM_ARGS", "-Dcom.ibm.ws.beta.edition=true");
        return runFeatureUtility(testcase, params, envProps);
    }

    protected ProgramOutput runFeatureUtility(String testcase, String[] params, Properties envProps) throws Exception {
    		// add beta property here
	     envProps.put("JVM_ARGS", "-DfeatureUtility.beta=true");    
   	   envProps.put("JVM_ARGS", "-Dcom.ibm.ws.beta.edition=true");
	      // always run feature utility with minified root
        return runCommand(minifiedRoot, testcase, "featureUtility", params, envProps);
    }

    protected static ProgramOutput runCommand(String root, String testcase, String command, String[] params, Properties envProps)
            throws Exception {
        String args = "";
        for (String param : params) {
            args = args + " " + param;
        }
//        Log.info(c, testcase, "repository.description.url: " + TestUtils.repositoryDescriptionUrl);
        Log.info(c, testcase, "command: " + root + "/bin/" + command + " " + args);
        ProgramOutput po = server.getMachine().execute(root + "/bin/" + command, params, root, envProps);
        Log.info(c, testcase, po.getStderr());
        Log.info(c, testcase, po.getStdout());
        Log.info(c, testcase, command + " command exit code: " + po.getReturnCode());
        return po;
    }

    protected static boolean deleteFeaturesAndLafilesFolders(String methodName) throws IOException {
        // delete /lib/features and /lafiles
        boolean features = TestUtils.deleteFolder(new File(minifiedRoot + "/lib/features"));
        boolean lafiles = TestUtils.deleteFolder(new File(minifiedRoot+"/lafiles"));

        Log.info(c, methodName, "DELETED FOLDERS: /lib/features, /lafiles? VALUES: " + features + ", " + lafiles);

        return features && lafiles;
    }
    
    protected static boolean deleteProps(String methodName) throws IOException {
        // delete /lib/features and /lafiles
        boolean etc = TestUtils.deleteFolder(new File(minifiedRoot + "/etc"));
        boolean props = new File(minifiedRoot+"/lib/versions/WebSphereApplicationServer.properties").delete();

        Log.info(c, methodName, "DELETED files/folders: /etc, /lib/versions/WebSphereApplicationServer.properties? VALUES: " + etc + ", " + props);

        return etc && props;
    }
    
    protected static boolean deleteRepo(String methodName) throws IOException {
	boolean repo = TestUtils.deleteFolder(new File(mavenLocalRepo1));
	Log.info(c, methodName, "DELETED REPO : " + mavenLocalRepo1 + "?" + repo);
	repo  = TestUtils.deleteFolder(new File(mavenLocalRepo2));
	Log.info(c, methodName, "DELETED REPO : " + mavenLocalRepo2 + "?" + repo);
    	return repo;
    }

    protected static boolean deleteEtcFolder(String methodName){
        boolean etc = TestUtils.deleteFolder(new File(minifiedRoot + "/etc"));
        Log.info(c, methodName, "DELETED files/folders: /etc ? VALUE: " + etc);

        return etc;

    }
    
    protected static boolean deleteUsrExtFolder(String methodName){
        boolean usr = TestUtils.deleteFolder(new File(minifiedRoot + "/usr/extension"));
        Log.info(c, methodName, "DELETED files/folders: /usr/extension ? VALUE: " + usr);

        return usr;
    }

    protected static boolean deleteUsrToExtFolder(String methodName){
        boolean usr = TestUtils.deleteFolder(new File(minifiedRoot + "/usr/cik"));
        Log.info(c, methodName, "DELETED files/folders: /usr/cik ? VALUE: " + usr);

        return usr;
    }

    protected static void deleteFiles(String methodName, String featureName, String[] filePathsToClear)
		throws Exception {
	Log.info(c, methodName, "If Exists, Deleting files for " + featureName);
	for (String filePath : filePathsToClear) {
	    server.deleteFileFromLibertyInstallRoot(filePath);
	}
    }

    protected static void assertFilesExist(String[] filePaths)  throws Exception {
    	for (String filePath : filePaths) {
    		assertTrue(filePath + " does not exist.", new File(minifiedRoot, filePath).exists());
    	}
    }

    protected void createExtensionDirs(String extensionName) throws Exception {
        //create extensionName.properties file in wlp/etc/extensions
        String methodName = "createExtensionDirs";
        String propsName = extensionName + ".properties";

        //create /etc/extensions
        File extensionsDir = new File(minifiedRoot + "/etc/extensions");
        boolean success = extensionsDir.mkdir();
        Log.info(c, methodName, "Extension dir " + extensionsDir.getAbsolutePath() + " created= " + success);

        //create extension folder
        File extensionsInstallDir = new File(minifiedRoot + "/usr/cik/extensions", extensionName);
        success = extensionsInstallDir.mkdir();
        Log.info(c, methodName, "Extension install dir created= " + success);

        File propsFile = new File(extensionsDir, propsName);

        writeToProps(propsFile.toString(),"com.ibm.websphere.productId", extensionName );
        writeToProps(propsFile.toString(),"com.ibm.websphere.productInstall", extensionsInstallDir.getAbsolutePath() );
    }

    /*
     * Check the exit code of the command.
     * 
     * @param ProgramOutput from command utility
     * 
     * @param exitCode expected exit code of the command
     * 
     * @param errorCode expected error code
     * 
     * @param fileLists expected files to be installed
     * 
     * @return True if exit code matches expected.
     */
    protected static void checkCommandOutput(ProgramOutput po, int exitCode, String errorCode, String[] filesList)
	    throws Exception {
	String output = po.getStdout();

	    if (errorCode != null) {
		assertTrue(String.format("Should contain %s", errorCode), output.contains(errorCode));
	    }
	    if (filesList != null) {
		assertFilesExist(filesList);
	    }
	    assertEquals(String.format("Exit code should be %d", exitCode), exitCode, po.getReturnCode());

    }


    /**
     * @param METHOD_NAME
     * @throws IOException
     * @throws InterruptedException
     */
    protected void checkProxyLog(final String METHOD_NAME, GenericContainer<?> proxyContainer)
	    throws IOException, InterruptedException {
	ExecResult lsResult = proxyContainer.execInContainer("cat", "/var/log/squid/access.log");
	String stdout = lsResult.getStdout();
	Log.info(c, METHOD_NAME, "Test Failed. Proxy Log: " + stdout);
    }

    
    protected void retryFeatureUtility(String METHOD_NAME) throws Exception {
		try (Stream <Path> walk = Files.walk(Paths.get(installRoot))) {
			List<String> result = walk.filter(Files::isRegularFile).map(x -> x.toString()).collect(Collectors.toList());
			result.forEach(x -> Log.info(c, METHOD_NAME, x));
		}
		String[] param1s = { "installFeature", "jsp-2.2", "jsp-2.3", "--verbose" };
		runFeatureUtility(METHOD_NAME, param1s);
	}

	/*
	 * / Copy Maven central features and signatures to local repository
	 */
	protected static void constructLocalMavenRepo(String repoPath,Path artifactPath) throws Exception {
	Log.info(c, "constructLocalMavenRepo",
		"Creating local repository using " + artifactPath.toAbsolutePath().toString());
	
	ZipFile zipFile = new ZipFile(artifactPath.toFile());
	TestUtils.unzipFileIntoDirectory(zipFile, Paths.get(repoPath).toFile());
	Log.info(c, "constructLocalMavenRepo", "Unzipped to " + Paths.get(repoPath).toAbsolutePath().toString());
	
	}


}
