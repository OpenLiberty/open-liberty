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

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import test.utils.TestUtils;

public abstract class FeatureUtilityToolTest {

    private static final Class<?> c = FeatureUtilityToolTest.class;
    public static LibertyServer server;
    public static String installRoot;
    protected static List<String> cleanFiles;
    protected static List<String> cleanDirectories;
    private static Logger logger = Logger.getLogger("com.ibm.ws.install.featureUtility_fat");
    private static Properties wlpVersionProps;
    private static String originalWlpVersion;
    private static String originalWlpEdition;
    private static String originalWlpInstallType;
    static String os = System.getProperty("os.name").toLowerCase();
    static boolean isWindows = os.indexOf("win") >= 0;

    protected static void setupEnv() throws Exception {
        final String methodName = "setup";
        server = LibertyServerFactory.getLibertyServer("com.ibm.ws.install.featureUtility_fat");


        // jbe/build/dev/image/output/wlp
        installRoot = server.getInstallRoot();

        Log.info(c, methodName, "install root: " + installRoot);

        // extract the kernel into our installRoot
        // kernelZipDir : jbe/build/dev/build.image/build/libs/distributions/
        String kernelZipDir = installRoot + "../../../build.image/build/libs/distributions/";
        String dest = kernelZipDir + "/openliberty-kernel";

        Log.info(c, methodName,"kernel zip dir: " + new File(kernelZipDir).getAbsolutePath());

        extractOpenLibertyKernelZip(kernelZipDir, kernelZipDir + dest);

        // change installroot to unzipped kernel
        installRoot =  dest;

//        Log.info(c, methodName, "install root: " + installRoot);
        
        setOriginalWlpVersionVariables();
        cleanDirectories = new ArrayList<String>();
        cleanFiles = new ArrayList<String>();

    }

    public static void extractOpenLibertyKernelZip(String kernelZipDir, String destinationDir) throws Exception {
        String methodName = "extractOpenLibertyKernelZip";

        if(!new File(kernelZipDir).exists()){
            throw new Exception(kernelZipDir + " doesnt exist!");
        }

        File dir = new File(kernelZipDir);

        // for debugging purposes - see what files are in kernelZipDir
        Log.info(c, methodName, Objects.requireNonNull(dir.listFiles()).toString());


        // find the files that match openliberty-kernel-*.zip
        final String id = "XXX"; // needs to be final so the anonymous class can use it
        File[] matchingFiles = dir.listFiles(new FileFilter() {
            public boolean accept(File pathname) {
                return (pathname.getName().contains("openliberty-kernel") && pathname.getName().endsWith("zip"));
            }
        });

        Log.info(c, methodName, "Found the following OL kernels: " + matchingFiles.toString());
        if(matchingFiles.length == 0){
            throw new Exception(kernelZipDir + " doesnt contain any OL kernels...");
        }

        // take the first one we find for sake of simplicity.
        File kernelZip = matchingFiles[0];

        // unzip into destinationDir now
        // first delete destinationDir
        File destFile = new File(destinationDir);
        if(destFile.exists()){
            destFile.delete();
        }

        unzipInstallRoot(kernelZip.getAbsolutePath(), destinationDir);
        Log.exiting(c, methodName, "Unzipped the OL kernel.");
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
            RemoteFile rf = server.getFileFromLibertyInstallRoot("lib/versions/openliberty.properties");
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
        return runCommand(testcase, "featureUtility", params, envProps);
    }

    protected ProgramOutput runCommand(String testcase, String command, String[] params, Properties envProps)
            throws Exception {
        String args = "";
        for (String param : params) {
            args = args + " " + param;
        }
//        Log.info(c, testcase, "repository.description.url: " + TestUtils.repositoryDescriptionUrl);
        Log.info(c, testcase, "command: " + installRoot + "/bin/" + command + " " + args);
        ProgramOutput po = server.getMachine().execute(installRoot + "/bin/" + command, params, installRoot, envProps);
        Log.info(c, testcase, po.getStdout());
        Log.info(c, testcase, command + " command exit code: " + po.getReturnCode());
        return po;
    }

//    protected File getFeatureRepo() {
//        File featureRepo;
//
//        return null;
//
//    }

    /**
     * Zips the liberty install root into /tmp/*testClass*
     *
     */
    protected static File zipInstallRoot(String oldInstallRoot, String destinationFile) throws IOException {
        String sourceFile = oldInstallRoot;

        FileOutputStream fos = new FileOutputStream(destinationFile);
        ZipOutputStream zipOut = new ZipOutputStream(fos);
        File fileToZip = new File(sourceFile);

        TestUtils.zipFile(fileToZip, fileToZip.getName(), zipOut);
        zipOut.close();
        fos.close();

        return new File(destinationFile);
    }

    protected static File unzipInstallRoot(String zipFile, String destinationToUnzip) throws IOException {
        File destination = new File(destinationToUnzip);
        TestUtils.unzipFile(zipFile,destination);
        return destination;
    }

    protected static void deleteFiles(String methodName, String featureName, String[] filePathsToClear) throws Exception {
        Log.info(c, methodName, "If Exists, Deleting files for " + featureName);
        for (String filePath : filePathsToClear) {
            if (server.fileExistsInLibertyInstallRoot(filePath)) {
                server.deleteFileFromLibertyInstallRoot(filePath);
            }
        }
        server.deleteDirectoryFromLibertyInstallRoot("lafiles/" + featureName);
        Log.info(c, methodName, "Finished deleting files associated with " + featureName);
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
