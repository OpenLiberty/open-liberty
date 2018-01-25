package com.ibm.ws.install.fat;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;

import test.utils.TestUtils;

import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.install.internal.InstallUtils;
import componenttest.topology.impl.LibertyServer;

/**
 *
 */
public abstract class ToolTest {

    private static final Class<?> c = ToolTest.class;
    protected static LibertyServer server;
    protected static String installRoot;
    protected static String tempDir;
    protected static String originalWlpVersion;
    private static String originalWlpEdition;
    private static String originalWlpInstallType;
    private static Properties wlpVersionProps;

    protected static void setupEnv(LibertyServer svr) throws Exception {
        final String METHOD_NAME = "setup";
        server = svr;
        installRoot = server.getInstallRoot();
        Log.info(c, METHOD_NAME, "installRoot: " + installRoot);

        setOriginalWlpProps();
    }

    protected static void cleanup() throws Exception {
        final String METHOD_NAME = "cleanup";
        Log.entering(c, METHOD_NAME);
        if (server.isStarted())
            server.stopServer();
        resetOriginalWlpProps();
        Log.exiting(c, METHOD_NAME);
    }

    protected static void checkInstalledFiles(String[] fileList, boolean installed) {
        if (installed) {
            ArrayList<String> missingFiles = new ArrayList<String>();
            for (String fileName : fileList) {
                if (!(new File(TestUtils.wlpDir, fileName).exists()))
                    missingFiles.add(fileName);
            }
            assertTrue("Files List : " + missingFiles.toString() + " should exist", missingFiles.isEmpty());
        } else {
            ArrayList<String> extraFiles = new ArrayList<String>();
            for (String fileName : fileList) {
                if (new File(TestUtils.wlpDir, fileName).exists())
                    extraFiles.add(fileName);
            }
            assertTrue("Files List : " + extraFiles.toString() + " should not exist", extraFiles.isEmpty());
        }
    }

    protected ProgramOutput runFeatureManager(String testcase, String[] params) throws Exception {
        Properties envProps = new Properties();
        //specify the java.io.tmpdir to tempDir
        tempDir = server.getInstallRoot() + "/../tempDir";
        File temp = new File(tempDir).getCanonicalFile();
        temp.mkdirs();
        envProps.put("JVM_ARGS", "-Djava.io.tmpdir=" + tempDir);
        Log.info(c, testcase, "java.io.tmpdir: " + tempDir);

        //specify the repository.description.url
        envProps.put("JVM_ARGS", "-Drepository.description.url=" + TestUtils.repositoryDescriptionUrl);

        //run featureManager
        ProgramOutput po = runFeatureManager(testcase, params, envProps);

        //clean tempDir
        server.deleteDirectoryFromLibertyInstallRoot(tempDir);
        Log.info(c, testcase, "delete " + tempDir);
        return po;
    }

    protected ProgramOutput runFeatureManager(String testcase, String[] params, Properties envProps) throws Exception {
        return runCommand(testcase, "featureManager", params, envProps);
    }

    protected ProgramOutput runCommand(String testcase, String command, String[] params, Properties envProps) throws Exception {
        String args = "";
        for (String param : params) {
            args = args + " " + param;
        }

        Log.info(c, testcase, "repository.description.url: " + TestUtils.repositoryDescriptionUrl);
        Log.info(c, testcase,
                 "Feature Manager command :" + server.getInstallRoot()
                                 + "/bin/" + command + " " + args);
        envProps.put("INSTALL_LOG_LEVEL", "FINE");
        ProgramOutput po = server.getMachine().execute(server.getInstallRoot() + "/bin/" + command, params,
                                                       server.getInstallRoot(), envProps);
        Log.info(c, testcase, po.getStdout());

        return po;
    }

    protected void createExtensionDirs(String extensionName, String extensionPath) throws IOException {
        //create extensionName.properties file in wlp/etc 
        String methodName = "createExtensionDirs";
        String propsName = "" + extensionName + ".properties";

        //create etc
        File etcDir = new File("" + server.getInstallRoot() + "/etc");
        if (etcDir.exists()) {
            Log.info(c, methodName, "etc dir " + etcDir.getAbsolutePath() + " already created");
        } else {
            Log.info(c, methodName, "etc dir = " + etcDir.getAbsolutePath());
            boolean success = etcDir.mkdir();
            Log.info(c, methodName, "etc dir created= " + success);
        }

        //create etc/extensions
        File extensionsDir = new File("" + server.getInstallRoot() + "/etc/extensions");
        if (extensionsDir.exists()) {
            Log.info(c, methodName, "Extension dir " + extensionsDir.getAbsolutePath() + " already created");
        } else {
            Log.info(c, methodName, "Extension dir = " + extensionsDir.getAbsolutePath());
            boolean success = extensionsDir.mkdir();
            Log.info(c, methodName, "Extension dir created= " + success);
        }

        //create extension folder
        File extensionsInstallDir = new File(extensionPath + extensionName);
        boolean success = extensionsInstallDir.mkdir();
        Log.info(c, methodName, "Extension install dir created= " + success);

        File propsFile = new File(extensionsDir, propsName);
        propsFile.createNewFile();
        //write com.ibm.websphere.productId=<extensionName> and com.ibm.websphere.productInstall=<file path to where extension where the feature will be installed to> to the file
        FileOutputStream fOut = null;
        Properties properties = new Properties();
        try {
            fOut = new FileOutputStream(propsFile);
            properties.setProperty("com.ibm.websphere.productId", extensionName);
            properties.setProperty("com.ibm.websphere.productInstall", extensionPath + extensionName);
            properties.store(fOut, null);
        } finally {
            InstallUtils.close(fOut);
        }
    }

    private static void setOriginalWlpProps() throws IOException {

        File wlpVersionPropFile = new File(server.getInstallRoot() +
                                           "/lib/versions/WebSphereApplicationServer.properties");
        wlpVersionPropFile.setReadable(true);

        FileInputStream fIn = null;
        wlpVersionProps = new Properties();
        try {
            fIn = new FileInputStream(wlpVersionPropFile);
            wlpVersionProps.load(fIn);
            originalWlpVersion = wlpVersionProps.getProperty("com.ibm.websphere.productVersion");
            originalWlpEdition = wlpVersionProps.getProperty("com.ibm.websphere.productEdition");
            originalWlpInstallType = wlpVersionProps.getProperty("com.ibm.websphere.productInstallType");
            Log.info(c, "getWlpVersion", "Original version of the wlp directory : " + originalWlpVersion);
            Log.info(c, "getWlpVersion", "com.ibm.websphere.productId : " +
                                         wlpVersionProps.getProperty("com.ibm.websphere.productId"));
            Log.info(c, "getWlpVersion", "com.ibm.websphere.productEdition : " + originalWlpEdition);
            Log.info(c, "getWlpVersion", "com.ibm.websphere.productInstallType : " + originalWlpInstallType);
        } finally {
            InstallUtils.close(fIn);
        }
    }

    protected static void replaceWlpProperties(String version, String edition, String installType) throws IOException {

        if (version == null && edition == null && installType == null)
            return;

        File wlpVersionPropFile = new File(server.getInstallRoot() +
                                           "/lib/versions/WebSphereApplicationServer.properties");
        wlpVersionPropFile.setWritable(true);

        FileOutputStream fOut = null;
        try {
            fOut = new FileOutputStream(wlpVersionPropFile);
            if (version != null) {
                wlpVersionProps.setProperty("com.ibm.websphere.productVersion", version);
                Log.info(c, "replaceWlpProperties", "Set the version of the wlp directory to : " + version);
            }
            if (edition != null) {
                wlpVersionProps.setProperty("com.ibm.websphere.productEdition", edition);
                Log.info(c, "replaceWlpProperties", "Set the edition of the wlp directory to : " + edition);
            }
            if (installType != null) {
                wlpVersionProps.setProperty("com.ibm.websphere.productInstallType", installType);
                Log.info(c, "replaceWlpProperties", "Set the installType of the wlp directory to : " + installType);
            }
            wlpVersionProps.store(fOut, null);
        } finally {
            InstallUtils.close(fOut);
        }
    }

    protected static void resetOriginalWlpProps() throws IOException {
        replaceWlpProperties(originalWlpVersion, originalWlpEdition, originalWlpInstallType);
    }

    protected static void assertFilesExist(String[] filePaths) throws Exception {
        for (String filePath : filePaths) {
            assertTrue(filePath + " does not exist.", server.fileExistsInLibertyInstallRoot(filePath));
        }
    }

    protected static void assertFilesNotExist(String[] filePaths) throws Exception {
        for (String filePath : filePaths) {
            assertFalse(filePath + " does exist.", server.fileExistsInLibertyInstallRoot(filePath));
        }
    }

    protected static void deleteFiles(String methodName, String featureName, String[] filePathsToClear) throws Exception {

        Log.info(c, methodName, "If Exists, Deleting files for " + featureName);

        for (String filePath : filePathsToClear) {
            if (server.fileExistsInLibertyInstallRoot(filePath)) {
                server.deleteFileFromLibertyInstallRoot(filePath);
            }
        }

        server.deleteDirectoryFromLibertyInstallRoot("lafiles");

        Log.info(c, methodName, "Finished deleting files associated with " + featureName);

    }
}
