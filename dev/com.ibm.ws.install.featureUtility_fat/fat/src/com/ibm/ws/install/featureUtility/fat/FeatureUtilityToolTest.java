/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.install.featureUtility.fat;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.zip.ZipFile;

import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.impl.LibertyFileManager;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import test.utils.TestUtils;

public abstract class FeatureUtilityToolTest {

    private static final Class<?> c = FeatureUtilityToolTest.class;
    public static LibertyServer server;
    private static String installRoot;
    static String minifiedRoot;
    protected static List<String> cleanFiles;
    protected static List<String> cleanDirectories;
    private static Logger logger = Logger.getLogger("com.ibm.ws.install.featureUtility_fat");
    protected static String serverName = "featureUtility_fat";
    private static String zipFileDestination;
    private static String unzipDestination;

    private static Properties wlpVersionProps;
    private static String originalWlpVersion;
    private static String originalWlpEdition;
    private static String originalWlpInstallType;
    static boolean isClosedLiberty = false;
    private static String pathToAutoFVTTestFiles = "lib/LibertyFATTestFiles/";

    protected static void setupEnv() throws Exception {
        final String methodName = "setup";
        server = LibertyServerFactory.getLibertyServer("com.ibm.ws.install.featureUtility_fat");

        // jbe/build/dev/image/output/wlp
        installRoot = server.getInstallRoot();
        Log.info(c, methodName, "install root: " + installRoot);

        isClosedLiberty = isClosedLibertyWlp();
        Log.info(c, methodName, "Closed liberty wlp?: "+ isClosedLiberty);


        zipFileDestination = installRoot + "/../temp/wlp.zip";
        unzipDestination = installRoot + "/../featureUtility_fat_wlp/";

        // zip up installRoot
        minifiedRoot = exportWlp(installRoot, installRoot + "/../temp/wlp.zip", installRoot + "/../featureUtility_fat_wlp/");
        Log.info(c, methodName, "minified root: " + minifiedRoot);

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
        LibertyFileManager.copyFileIntoLiberty(server.getMachine(), minifiedRoot + "/" + extendedPath, (pathToAutoFVTTestFiles + "/" + fileName));
    }

    private static boolean isClosedLibertyWlp(){
        return new File(installRoot + "/lib/versions/WebSphereApplicationServer.properties").exists();
    }
//

    /**
     *
     * @return previous wlp version. ex: returns 19.0.0.11 if the current version is 19.0.0.12
     */
    protected static String getPreviousWlpVersion(){
        String version = originalWlpVersion;
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
            Log.info(c, "getWlpVersion", "com.ibm.websphere.productVersion : " + originalWlpVersion);
            Log.info(c, "getWlpVersion",
                    "com.ibm.websphere.productId : " + wlpVersionProps.getProperty("com.ibm.websphere.productId"));
            Log.info(c, "getWlpVersion", "com.ibm.websphere.productEdition : " + originalWlpEdition);
            Log.info(c, "getWlpVersion", "com.ibm.websphere.productInstallType : " + originalWlpInstallType);
        } finally {
                try {
                    assert fIn != null;
                    fIn.close();
                } catch (IOException e) {
                    // ignore we are trying to close.
                }
            }
        }

    protected static void replaceWlpProperties(String version) throws Exception {
        OutputStream os = null;
        try {
            RemoteFile rf = new RemoteFile(server.getMachine(), minifiedRoot+ "/lib/versions/openliberty.properties");
//            RemoteFile rf = server.getFileFromLibertyInstallRoot("lib/versions/openliberty.properties");
            os = rf.openForWriting(false);
            wlpVersionProps.setProperty("com.ibm.websphere.productVersion", version);
            Log.info(c, "replaceWlpProperties", "Set the version to : " + version);
            wlpVersionProps.store(os, null);
            os.close();
        } finally {
            try {
                os.close();
            } catch (IOException e) {
                // ignore we are trying to close.
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
//        envProps.put("JVM_ARGS", "-Drepository.description.url=" + TestUtils.repositoryDescriptionUrl);
//        envProps.put("INSTALL_LOG_LEVEL", "FINE");
//        if (debug)
//            envProps.put("JVM_ARGS", "-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=7777,timeout=10000 -Drepository.description.url="
//                                     + TestUtils.repositoryDescriptionUrl);
//        else
//            envProps.put("JVM_ARGS", "-Drepository.description.url=" + TestUtils.repositoryDescriptionUrl);
        return runFeatureUtility(testcase, params, envProps);
    }

    protected ProgramOutput runFeatureUtility(String testcase, String[] params, Properties envProps) throws Exception {
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
}
