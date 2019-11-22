package com.ibm.ws.install.featureUtility.fat;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import javax.xml.transform.Templates;

import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.install.internal.InstallUtils;

import componenttest.topology.impl.LibertyFileManager;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.FileUtils;
import test.utils.TestUtils;

public abstract class FeatureUtilityToolTest {

    private static final Class<?> c = FeatureUtilityToolTest.class;
    public static LibertyServer server;
    private static String installRoot;
    public static String minifiedRoot;
    protected static List<String> cleanFiles;
    protected static List<String> cleanDirectories;
    private static Logger logger = Logger.getLogger("com.ibm.ws.install.featureUtility_fat");
    protected static String serverName = "featureUtility_fat";
    private static Properties wlpVersionProps;
    private static String originalWlpVersion;
    private static String originalWlpEdition;
    private static String originalWlpInstallType;
    static String os = System.getProperty("os.name").toLowerCase();
    static boolean isWindows = os.indexOf("win") >= 0;
    static boolean isClosedLiberty = false;
    public static String pathToAutoFVTTestFiles = "lib/LibertyFATTestFiles/";

    protected static void setupEnv() throws Exception {
        final String methodName = "setup";
        server = LibertyServerFactory.getLibertyServer("com.ibm.ws.install.featureUtility_fat");
        
        // jbe/build/dev/image/output/wlp
        installRoot = server.getInstallRoot();

        Log.info(c, methodName, "install root: " + installRoot);

        isClosedLiberty = isClosedLibertyWlp();
        Log.info(c, methodName, "Closed liberty wlp?: "+ isClosedLiberty);

        // minify server package
        minifiedRoot = transportWlp(installRoot, serverName);
        Log.info(c, methodName, "minified root: " + minifiedRoot);
        Log.info(c, methodName, "exists??: " + new File(minifiedRoot).exists());



        setOriginalWlpVersionVariables();
        cleanDirectories = new ArrayList<String>();
        cleanFiles = new ArrayList<String>();

    }

    /**
     * This method minifies the installRoot by creating a server with name @serverName
     * @param installRoot install root
     * @param serverName name of the server to create
     * @return path to minified wlp
     * @throws Exception
     */
    private static String transportWlp(String installRoot, String serverName) throws Exception {
        String methodName = "transportWlp";
        // first copy a blank server.xml into a server folder
        createServer(serverName);
        String noFeaturesServerXml = "../../publish/tmp/noFeaturesServerXml/server.xml";
        Log.info(c, methodName,  noFeaturesServerXml);
        copyNoFeaturesServerXmlToServer(serverName, noFeaturesServerXml);
        // minify server
        File minifiedServerPackage = minifyServer(serverName);
        Log.info(c, methodName, minifiedServerPackage.getAbsolutePath());
        Log.info(c, methodName, String.valueOf(minifiedServerPackage.exists()));
        // unzip server package
        String destination = installRoot + "/../minified/" + serverName + "/";
        File destinationFile = new File(destination);
        destinationFile.mkdirs();

        Log.info(c, methodName, "destination: " + destination);

        TestUtils.unzipFileIntoDirectory(new ZipFile(minifiedServerPackage), destinationFile);

//        unzipInstallRoot(minifiedServerPackage.getAbsolutePath(), destination);

        return destination + "wlp";

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


    private static ProgramOutput runServer(String testcase, String[] params) throws Exception {
        return runCommand(installRoot, testcase, "server", params, new Properties());

    }

    private static void createServer(String serverName) throws Exception {
        runServer("createServer", new String[]{"create", serverName});
    }

    private static void copyNoFeaturesServerXmlToServer(String serverName, String noFeaturesServerXml) throws Exception {
//        server.copyFileToLibertyInstallRoot("usr/servers/"+ serverName, noFeaturesServerXml.getAbsolutePath());
//        FileUtils.copyFile(noFeaturesServerXml, new File(installRoot + "/usr/servers/"+serverName));
        copyFileToLibertyInstallRoot(installRoot, "usr/servers/" + serverName, noFeaturesServerXml);
    }

    private static File minifyServer(String serverName) throws Exception {
        runServer("minifyServer", new String[]{"package", serverName, "--include=minify"});

        // return the zipped wlp
        return new File(installRoot+ "/usr/servers/"+serverName+"/"+serverName+".zip");
    }


//    protected static void deleteFiles(String methodName, String featureName, String[] filePathsToClear) throws Exception {
//        Log.info(c, methodName, "If Exists, Deleting files for " + featureName);
//        for (String filePath : filePathsToClear) {
//            if (server.fileExistsInLibertyInstallRoot(filePath)) {
//                server.deleteFileFromLibertyInstallRoot(filePath);
//            }
//        }
//        server.deleteDirectoryFromLibertyInstallRoot("lafiles/" + featureName);
//        Log.info(c, methodName, "Finished deleting files associated with " + featureName);
//    }

    protected static boolean deleteFeaturesAndLafilesFolders(String methodName) throws IOException {
        // delete /lib/features and /lafiles
        boolean features = TestUtils.deleteFolder(new File(minifiedRoot + "/lib/features"));
        boolean lafiles = TestUtils.deleteFolder(new File(minifiedRoot+"/lafiles"));

        Log.info(c, methodName, "DELETED FOLDERS: /lib/features, /lafiles? VALUES: " + features + ", " + lafiles);

        return features && lafiles;
    }



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
        if(month.equals("01")){
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



}
