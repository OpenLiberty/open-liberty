/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2014
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.install.fat;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.install.internal.InstallUtils;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import test.utils.TestUtils;

/**
 *
 */
public class MinifyTest {
    public static final Class<?> c = MinifyTest.class;
    public static LibertyServer server;
    public static String installRoot;
    private static String originalWlpVersion;
    private static String originalWlpEdition;
    private static Properties wlpVersionProps;

    @BeforeClass
    public static void beforeClassSetup() throws Exception {
        setupEnv(LibertyServerFactory.getLibertyServer("com.ibm.ws.install_fat"));
    }

    /**
     * Setup the environment.
     *
     * @param svr
     *            The server instance.
     *
     * @throws Exception
     */
    public static void setupEnv(LibertyServer svr) throws Exception {
        final String METHOD_NAME = "setup";
        server = svr;
        installRoot = server.getInstallRoot();
        Log.info(c, METHOD_NAME, "installRoot: " + installRoot);

        setOriginalWlpVersion();
    }

    /**
     * This method removes all the testing artifacts from the server directories.
     *
     * @throws Exception
     */
    @AfterClass
    public static void cleanup() throws Exception {
        final String METHOD_NAME = "cleanup";

        Log.entering(c, METHOD_NAME);

        if (server.isStarted())
            server.stopServer();

        replaceWlpProperties(originalWlpVersion, originalWlpEdition);
        Log.exiting(c, METHOD_NAME);

        server.deleteDirectoryFromLibertyInstallRoot("usr/servers/miniwlp");
    }

    @Mode(TestMode.FULL)
    @Test
    public void testMinifyWithAddon() throws Exception {
        Assume.assumeTrue(TestUtils.connectedToRepo);
        final String METHOD_NAME = "testMinify";
        Log.entering(c, METHOD_NAME);

        String serverFile = server.getInstallRoot() + "/usr/servers/miniwlp/server.xml";
        String zipFile = server.getInstallRoot() + "/usr/servers/miniwlp/miniwlp.zip";
        String paxFile = server.getInstallRoot() + "/usr/servers/miniwlp/miniwlp.pax";
        String miniWlp = server.getInstallRoot() + "/usr/servers/miniwlp/";
        String propertiesFile = server.getInstallRoot() + "/lib/versions/WebSphereApplicationServer.properties";
        String miniWlpPropertiesFile = miniWlp + "wlp/lib/versions/WebSphereApplicationServer.properties";

        FileInputStream in = null;
        FileOutputStream out = null;
        try {
            in = new FileInputStream(propertiesFile);
            Properties props = new Properties();
            props.load(in);
            in.close();
            props.setProperty("com.ibm.websphere.productInstallType", "InstallationManager");
            props.setProperty("com.ibm.websphere.productEdition", "ND");
            out = new FileOutputStream(propertiesFile);
            props.store(out, null);
        } catch (Exception e) {
            assertTrue("Fail to set the properties", false);
            e.printStackTrace();
        } finally {
            InstallUtils.close(in);
            InstallUtils.close(out);
        }

        //Make Sure build.image\wlp\lib\extract\ exist
        File extract = new File(server.getInstallRoot() + "/lib/extract/");
        assertTrue("extract Folder Doesn't exist, please copy it manually from project " +
                   "/wlp.lib.extract/build/classes/wlp/lib/extract", extract.exists());

        //Create the server.xml file
        String fileContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                             "<server description=\"new server\">" +
                             "<featureManager>" +
                             "<feature>genericCoreFeatureDependancyOnEsaPass</feature>" +
                             "<feature>genericCoreFeatureModifyA</feature>" +
                             "</featureManager>" +
                             "</server>";
        boolean rt = makeFile(serverFile, fileContent);
        assertTrue("Fail to create server.xml file", rt);

        ProgramOutput po;

        //Install an addon
        String[] FMInstall = { "install", "com.ibm.installExtendedPackage-1.0",
                               "--acceptLicense", "--when-file-exists=replace" };
        Properties envProps = new Properties();
        envProps.put("JVM_ARGS", "-Drepository.description.url=" + TestUtils.repositoryDescriptionUrl);
        po = runCommand(METHOD_NAME, server.getInstallRoot() + "bin/",
                        server.getInstallRoot() + "/bin/featureManager", FMInstall, envProps);
        assertTrue("genericCoreFeatureDependancyOnEsaPass is not installed.",
                   isFeatureInstalled(server.getInstallRoot() + "", "genericCoreFeatureDependancyOnEsaPass"));
        assertTrue("genericCoreFeatureModifyA is not installed.",
                   isFeatureInstalled(server.getInstallRoot() + "", "genericCoreFeatureModifyA"));

        //Run server package to create a minify wlp zip file
        String[] serverPackage = { "package", "miniwlp", "--include=minify" };
        po = runCommand(METHOD_NAME, server.getInstallRoot() + "/bin/",
                        server.getInstallRoot() + "/bin/server", serverPackage, new Properties());
        String logContain = "Server miniwlp package complete in";
        assertTrue("Should contain '" + logContain + "'", po.getStdout().contains(logContain));

        //Extract the Archive file Created
        if (new File(zipFile).exists()) {
            unzip(zipFile, miniWlp);
        } else if (new File(paxFile).exists()) {//.pax archive file will created instead of .zip in Z/OS
            String[] unpax = { "-r", "<" + paxFile };
            po = runCommand(METHOD_NAME, miniWlp, "pax", unpax, new Properties());
        } else
            assertTrue("The archive miniwlp archive file doesn't exist.", false);

        //Verify that version and installType in the miniWlp properties file is still InstallationManager
        //also verify that com.ibm.installExtendedPackage-1.0 is not installed in miniwlp
        try {
            in = new FileInputStream(miniWlpPropertiesFile);
            Properties props = new Properties();
            props.load(in);
            in.close();
            assertTrue("Install Type should be Archive",
                       props.getProperty("com.ibm.websphere.productInstallType").equals("Archive"));

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            InstallUtils.close(in);
        }

        assertTrue("genericCoreFeatureDependancyOnEsaPass is not installed.",
                   isFeatureInstalled(miniWlp + "/wlp", "genericCoreFeatureDependancyOnEsaPass"));
        assertTrue("genericCoreFeatureModifyA is not installed.",
                   isFeatureInstalled(miniWlp + "/wlp", "genericCoreFeatureModifyA"));

        File file = new File(miniWlp + "/wlp/lib/assets");
        assertFalse("com.ibm.installExtendedPackage-1.0 is installed. or " + miniWlp + "/wlp/lib/assets folder still exists",
                    file.exists());

        //Run a simple test using FM in the miniwlp
        String[] FMInstall2 = { "install", "com.ibm.genericCoreFeatureH", "--acceptLicense", "--when-file-exists=replace" };
        envProps = new Properties();
        envProps.put("JVM_ARGS", "-Drepository.description.url=" + TestUtils.repositoryDescriptionUrl);
        po = runCommand(METHOD_NAME, miniWlp + "/wlp/bin/", miniWlp + "/wlp/bin/featureManager", FMInstall2, envProps);
        assertTrue("genericCoreFeatureA is not installed.", isFeatureInstalled(miniWlp + "/wlp", "genericCoreFeatureA"));
        assertTrue("genericCoreFeatureH is not installed.", isFeatureInstalled(miniWlp + "/wlp", "genericCoreFeatureH"));
    }

    /**
     * @param string
     * @param featureInstalled
     */

    /**
     * This test may failed in the local machine due to several files/folders missing in the image wlp
     * 1. /wlp/lib/extract/*
     * 2. /wlp/lib/extract/META-INF/MANIFEST.MF
     * please copy it from else where
     */
    @Mode(TestMode.FULL)
    @Test
    public void testMinify() throws Exception {
        Assume.assumeTrue(TestUtils.connectedToRepo);
        final String METHOD_NAME = "testMinify";
        Log.entering(c, METHOD_NAME);

        String serverFile = server.getInstallRoot() + "/usr/servers/miniwlp/server.xml";
        String zipFile = server.getInstallRoot() + "/usr/servers/miniwlp/miniwlp.zip";
        String paxFile = server.getInstallRoot() + "/usr/servers/miniwlp/miniwlp.pax";
        String miniWlp = server.getInstallRoot() + "/usr/servers/miniwlp/";
        String propertiesFile = server.getInstallRoot() + "/lib/versions/WebSphereApplicationServer.properties";
        String miniWlpPropertiesFile = miniWlp + "wlp/lib/versions/WebSphereApplicationServer.properties";

        FileInputStream in = null;
        FileOutputStream out = null;
        try {
            in = new FileInputStream(propertiesFile);
            Properties props = new Properties();
            props.load(in);
            in.close();
            props.setProperty("com.ibm.websphere.productInstallType", "InstallationManager");
            out = new FileOutputStream(propertiesFile);
            props.store(out, null);
        } catch (Exception e) {
            assertTrue("Fail to set the productInstallType", false);
            e.printStackTrace();
        } finally {
            InstallUtils.close(in);
            InstallUtils.close(out);
        }

        //Make Sure build.image\wlp\lib\extract\ exist
        File extract = new File(server.getInstallRoot() + "/lib/extract/");
        assertTrue("extract Folder Doesn't exist, please copy it manually from project " +
                   "/wlp.lib.extract/build/classes/wlp/lib/extract", extract.exists());

        //Create the server.xml file
        boolean rt = makeFile(serverFile, "<server/>");
        assertTrue("Fail to create server.xml file", rt);

        ProgramOutput po;

        //Run server package to create a minify wlp zip file
        String[] serverPackage = { "package", "miniwlp", "--include=minify" };
        po = runCommand(METHOD_NAME, server.getInstallRoot() + "/bin/", server.getInstallRoot() + "/bin/server", serverPackage, new Properties());
        String logContain = "Server miniwlp package complete in";
        assertTrue("Should contain '" + logContain + "'", po.getStdout().contains(logContain));

        //Extract the Archive file Created
        if (new File(zipFile).exists()) {
            unzip(zipFile, miniWlp);
        } else if (new File(paxFile).exists()) {//.pax archive file will created instead of .zip in Z/OS
            String[] unpax = { "-r", "<" + paxFile };
            po = runCommand(METHOD_NAME, miniWlp, "pax", unpax, new Properties());
        } else
            assertTrue("The archive miniwlp archive file doesn't exist.", false);

        //Verify that version and installType in the miniWlp properties file is still InstallationManager
        try {
            in = new FileInputStream(miniWlpPropertiesFile);
            Properties props = new Properties();
            props.load(in);
            in.close();
            assertTrue("Install Type should be Archive",
                       props.getProperty("com.ibm.websphere.productInstallType").equals("Archive"));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            InstallUtils.close(in);
        }

        //Run a simple test using FM in the miniwlp
        String[] FMInstall = { "install", "com.ibm.genericCoreFeatureH", "--acceptLicense", "--when-file-exists=replace" };
        Properties envProps = new Properties();
        envProps.put("JVM_ARGS", "-Drepository.description.url=" + TestUtils.repositoryDescriptionUrl);
        po = runCommand(METHOD_NAME, miniWlp + "/wlp/bin/", miniWlp + "/wlp/bin/featureManager", FMInstall, envProps);
        assertTrue("genericCoreFeatureA is not installed.", isFeatureInstalled(miniWlp + "/wlp", "com.ibm.genericCoreFeatureA"));
        assertTrue("genericCoreFeatureH is not installed.", isFeatureInstalled(miniWlp + "/wlp", "com.ibm.genericCoreFeatureH"));
    }

    //Check if a feature is installed, note this may not apply to real feature, because real feature may have
    //a different jar file name.
    //checking for both featueName or com.ibm.featureName files.
    private boolean isFeatureInstalled(String wlpDir, String featureName) {
        String laidDowns[] = {
                               "/lib/features/checksums/" + featureName + ".cs",
                               "/lib/features/" + featureName + ".mf",
                               "/lib/features/l10n/" + featureName + ".properties",
                               "/lib/" + featureName + "_1.0.0.jar"
        };
        for (String laidDown : laidDowns) {
            File file = new File(wlpDir + laidDown);
            if (!file.exists()) {
                laidDown = laidDown.replace(featureName, "com.ibm." + featureName);
                File altFile = new File(wlpDir + laidDown);
                if (!altFile.exists()) {
                    Log.info(c, "isFeatureInstalled ", file.toString() + " or " + altFile.toString() + " do not exist!");
                    return false;
                }
            }
        }
        Log.info(c, "isFeatureInstalled", "Check File laid down: " + featureName + " install successed");
        return true;
    }

    private ProgramOutput runCommand(String testcase, String workDir, String command, String[] params, Properties envProps) throws Exception {
        String args = "";
        for (String param : params) {
            args = args + " " + param;
        }
        Log.info(c, "runCommand", command + "" + args);
        envProps.put("INSTALL_LOG_LEVEL", "ALL");
        ProgramOutput po = server.getMachine().execute(command, params,
                                                       workDir, envProps);
        Log.info(c, "runCommand", po.getStdout());
        return po;
    }

    //Make a file in specific location and write contents in
    private static boolean makeFile(String location, String contents) {
        File file = new File(location);
        FileWriter fWriter;
        BufferedWriter bWriter = null;
        try {
            file.getParentFile().mkdirs();
            file.createNewFile();
            fWriter = new FileWriter(file);
            bWriter = new BufferedWriter(fWriter);
            bWriter.write(contents);
            bWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
            Log.info(c, "makeFile", "Exception:" + e.getMessage());
            return false;
        } finally {
            InstallUtils.close(bWriter);
        }
        return true;
    }

    public void unzip(String zipFilePath, String destDirectory) {
        File destDir = new File(destDirectory);
        if (!destDir.exists()) {
            destDir.mkdir();
        }
        ZipInputStream zipIn = null;
        try {
            zipIn = new ZipInputStream(new FileInputStream(zipFilePath));
            ZipEntry entry = zipIn.getNextEntry();
            // iterates over entries in the zip file
            while (entry != null) {
                String filePath = destDirectory + File.separator + entry.getName();
                if (!entry.isDirectory()) {
                    // if the entry is a file, extracts it
                    extractFile(zipIn, filePath);
                }
                zipIn.closeEntry();
                entry = zipIn.getNextEntry();
            }
            zipIn.close();
        } catch (Exception e) {
            e.printStackTrace();
            Log.info(c, "unzip", "Exception:" + e.getMessage());
        } finally {
            InstallUtils.close(zipIn);
        }
    }

    private void extractFile(ZipInputStream zipIn, String filePath) {
        makeFile(filePath, "");
        BufferedOutputStream bos = null;
        try {
            bos = new BufferedOutputStream(new FileOutputStream(filePath));
            byte[] bytesIn = new byte[4096];
            int read = 0;
            while ((read = zipIn.read(bytesIn)) != -1) {
                bos.write(bytesIn, 0, read);
            }
            bos.close();
            File file = new File(filePath);
            file.setExecutable(true, false);
            file.setWritable(true, false);
            file.setReadable(true, false);
        } catch (Exception e) {
            e.printStackTrace();
            Log.info(c, "unzip", "Exception:" + e.getMessage());
        } finally {
            InstallUtils.close(bos);
        }
    }

    private static void replaceWlpProperties(String version, String edition) throws IOException {

        File wlpVersionPropFile = new File(server.getInstallRoot() +
                                           "/lib/versions/WebSphereApplicationServer.properties");
        wlpVersionPropFile.setWritable(true);

        FileOutputStream fOut = null;
        try {
            fOut = new FileOutputStream(wlpVersionPropFile);
            wlpVersionProps.setProperty("com.ibm.websphere.productVersion", version);
            wlpVersionProps.setProperty("com.ibm.websphere.productEdition", edition);
            wlpVersionProps.store(fOut, null);
            Log.info(c, "replaceWlpProperties", "Set the version of the wlp directory to : " + version);
        } finally {
            InstallUtils.close(fOut);
        }
    }

    private static void setOriginalWlpVersion() throws IOException {

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
            Log.info(c, "setOriginalWlpVersion", "Original version of the wlp directory : " + originalWlpVersion);
            Log.info(c, "setOriginalWlpVersion", "com.ibm.websphere.productId : " +
                                                 wlpVersionProps.getProperty("com.ibm.websphere.productId"));
            Log.info(c, "setOriginalWlpVersion", "com.ibm.websphere.productEdition : " +
                                                 wlpVersionProps.getProperty("com.ibm.websphere.productEdition"));
            Log.info(c, "setOriginalWlpVersion", "com.ibm.websphere.productInstallType : " +
                                                 wlpVersionProps.getProperty("com.ibm.websphere.productInstallType"));
        } finally {
            InstallUtils.close(fIn);
        }
    }
}
