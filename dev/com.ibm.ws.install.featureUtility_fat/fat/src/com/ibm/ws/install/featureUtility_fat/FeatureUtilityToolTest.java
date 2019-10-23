package com.ibm.ws.install.featureUtility_fat;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.ws.install.internal.InstallUtils;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

public abstract class FeatureUtilityToolTest {

    private static final Class<?> c = FeatureUtilityToolTest.class;
    public static LibertyServer server;
    public static String installRoot;
    protected static List<String> cleanFiles;
    protected static List<String> cleanDirectories;
    private static Logger logger;
    private static Properties wlpVersionProps;
    private static String originalWlpVersion;
    private static String originalWlpEdition;
    private static String originalWlpInstallType;
    static String os = System.getProperty("os.name").toLowerCase();
    static boolean isWindows = os.indexOf("win") >= 0;

    protected static void setupEnv() throws Exception {
        final String methodName = "setup";
        logger = Logger.getLogger("com.ibm.ws.install.featureUtility_fat");
        server = LibertyServerFactory.getLibertyServer("com.ibm.ws.install.featureUtility_fat");
        installRoot = server.getInstallRoot();
        info(c, methodName, "install root: " + installRoot);
        
        setOriginalWlpVersionVariables();
        cleanDirectories = new ArrayList<String>();
        cleanFiles = new ArrayList<String>();

    }

    // TODO discuss if we need this. may be useful for certain test cases like
    // mirror repo
    protected static void setEnvironmentVariable() {
//        ProcessBuilder pb = isWindows ? new ProcessBuilder("CMD", "/C", "SET") : new ProcessBuilder("")

    }

    protected static void removeEnvironmentVariable() {

    }

    private static void setOriginalWlpVersionVariables() throws IOException {
        File wlpVersionPropFile = new File(installRoot + "/lib/versions/openliberty.properties");
        wlpVersionPropFile.setReadable(true);

        FileInputStream fIn = null;
        wlpVersionProps = new Properties();
        try {
            fIn = new FileInputStream(wlpVersionPropFile);
            wlpVersionProps.load(fIn);
            originalWlpVersion = wlpVersionProps.getProperty("com.ibm.websphere.productVersion");
            originalWlpEdition = wlpVersionProps.getProperty("com.ibm.websphere.productEdition");
            originalWlpInstallType = wlpVersionProps.getProperty("com.ibm.websphere.productInstallType");
            info(c, "getWlpVersion", "com.ibm.websphere.productVersion : " + originalWlpVersion);
            info(c, "getWlpVersion",
                    "com.ibm.websphere.productId : " + wlpVersionProps.getProperty("com.ibm.websphere.productId"));
            info(c, "getWlpVersion", "com.ibm.websphere.productEdition : " + originalWlpEdition);
            info(c, "getWlpVersion", "com.ibm.websphere.productInstallType : " + originalWlpInstallType);
        } finally {
            InstallUtils.close(fIn);
        }
        
    }

    protected static void replaceWlpProperties(String version, String edition, String installType) throws Exception {
        OutputStream os = null;
        try {
            RemoteFile rf = server.getFileFromLibertyInstallRoot("lib/versions/WebSphereApplicationServer.properties");
            os = rf.openForWriting(false);
            wlpVersionProps.setProperty("com.ibm.websphere.productVersion", version);
            wlpVersionProps.setProperty("com.ibm.websphere.productEdition", edition);
            wlpVersionProps.setProperty("com.ibm.websphere.productInstallType", installType);
            info(c, "replaceWlpProperties", "Set the version to : " + version);
            info(c, "replaceWlpProperties", "Set the edition to : " + edition);
            info(c, "replaceWlpProperties", "Set the installType to : " + installType);
            wlpVersionProps.store(os, null);
            os.close();
        } finally {
            InstallUtils.close(os);
        }
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
        return runCommand(testcase, "featureUtility", params, envProps);
    }

    protected ProgramOutput runCommand(String testcase, String command, String[] params, Properties envProps)
            throws Exception {
        String args = "";
        for (String param : params) {
            args = args + " " + param;
        }
//        Log.info(c, testcase, "repository.description.url: " + TestUtils.repositoryDescriptionUrl);
        info(c, testcase, "command: " + installRoot + "/bin/" + command + " " + args);
        ProgramOutput po = server.getMachine().execute(installRoot + "/bin/" + command, params, installRoot, envProps);
        info(c, testcase, po.getStdout());
        info(c, testcase, command + " command exit code: " + po.getReturnCode());
        return po;
    }

//    protected File getFeatureRepo() {
//        File featureRepo;
//
//        return null;
//
//    }


    protected static void info(Class<?> c, String method, String data) {
        logger.logp(Level.INFO, c.getCanonicalName(), method, data);
    }

    protected static void fine(Class<?> c, String method, String data) {
        logger.logp(Level.FINE, c.getCanonicalName(), method, data);
    }

    protected static void severe(Class<?> c, String method, String data) {
        logger.logp(Level.SEVERE, c.getCanonicalName(), method, data);
    }

    protected static void entering(Class<?> c, String method) {
        logger.entering(c.getCanonicalName(), method);
    }

    protected static void entering(Class<?> c, String method, Object value) {
        logger.entering(c.getCanonicalName(), method, value);
    }

    protected static void entering(Class<?> c, String method, Object[] values) {
        logger.entering(c.getCanonicalName(), method, values);
    }

    protected static void exiting(Class<?> c, String method) {
        logger.entering(c.getCanonicalName(), method);
    }

}
