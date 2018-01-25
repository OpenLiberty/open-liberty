package com.ibm.ws.install.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServerFactory;
import test.utils.TestUtils;

public class FindCommandTest extends ToolTest {

    private static final Class<?> c = FindCommandTest.class;
    private static List<String> cleanFiles;
    private static List<String> cleanDirectories;

    @BeforeClass
    public static void beforeClassSetup() throws Exception {
        setupEnv(LibertyServerFactory.getLibertyServer("com.ibm.ws.install_fat"));
        cleanDirectories = new ArrayList<String>();
        cleanFiles = new ArrayList<String>();
    }

    @AfterClass
    public static void cleanup() throws Exception {
        final String METHOD_NAME = "cleanup";
        entering(c, METHOD_NAME);
        for (String cFile : cleanFiles) {
            server.deleteFileFromLibertyInstallRoot(cFile);
            Log.info(c, METHOD_NAME, "delete " + cFile);
        }
        cleanDirectories.add("usr/cik");
        for (String cDir : cleanDirectories) {
            server.deleteDirectoryFromLibertyInstallRoot(cDir);
            Log.info(c, METHOD_NAME, "delete " + cDir);
        }
        ToolTest.cleanup();
        exiting(c, METHOD_NAME);
    }

    private static void entering(Class<?> c, String METHOD_NAME) {
        Log.info(c, METHOD_NAME, "---- " + METHOD_NAME + " : entering ----------------------------");
    }

    private static void exiting(Class<?> c, String METHOD_NAME) {
        Log.info(c, METHOD_NAME, "---- " + METHOD_NAME + " : exiting ----------------------------");
    }

    private ProgramOutput runFeatureManagerWithInvalidRepository(String testcase, String[] params) throws Exception {
        Properties envProps = new Properties();
        envProps.put("JVM_ARGS", "-Drepository.description.url=" + TestUtils.repositoryDescriptionUrl.replace("assetServiceLocation", "assetServiceLocation.invalid"));
        return runFeatureManager(testcase, params, envProps);
    }

    @Test
    @Ignore
    public void testFindAll() throws Exception {
        Assume.assumeTrue(TestUtils.connectedToRepo);
        String METHOD_NAME = "testFindAll";
        entering(c, METHOD_NAME);
        String[] param1s = { "find", "\"\"" };
        ProgramOutput po = runFeatureManager(METHOD_NAME, param1s);
        Log.info(FindCommandTest.class, METHOD_NAME, "StdOut: " + po.getStdout());
        if (po.getReturnCode() != 0) {
            Log.info(FindCommandTest.class, METHOD_NAME, "StdErr: " + po.getStderr());
        }
        assertEquals("Expected exit code", 0, po.getReturnCode());
        String output = po.getStdout();
        assertTrue("Should contain \"This process might take several minutes to complete.\"", output.indexOf("This process might take several minutes to complete.") >= 0);
        assertTrue("Should contain Web Cache Monitor", output.indexOf("Web Cache Monitor") >= 0);
        String split[] = output.split(" : ");
        assertTrue("Should only be more than 10", split.length > 10);
        assertTrue("list should be sorted: \"a8552.\" < \"com.ibm.feature\"", output.indexOf("a8552.") < output.indexOf("com.ibm.feature"));
        assertTrue("list should be sorted: \"com.ibm.feature\" < \"genericCoreFeature :\"", output.indexOf("com.ibm.feature") < output.indexOf("genericCoreFeature :"));
        assertTrue("list should be sorted: \"genericCoreFeature :\" < \"webCacheMonitor-1.0 :\"", output.indexOf("genericCoreFeature :") < output.indexOf("webCacheMonitor-1.0 :"));
        exiting(c, METHOD_NAME);
    }

    @Test
    public void testFindAllViewInfo() throws Exception {
        Assume.assumeTrue(TestUtils.connectedToRepo);
        String METHOD_NAME = "testFindAllViewInfo";
        entering(c, METHOD_NAME);
        String[] param1s = { "find", "\"\"", "--viewInfo" };
        ProgramOutput po = runFeatureManager(METHOD_NAME, param1s);
        Log.info(FindCommandTest.class, METHOD_NAME, "StdOut: " + po.getStdout());
        if (po.getReturnCode() != 0) {
            Log.info(FindCommandTest.class, METHOD_NAME, "StdErr: " + po.getStderr());
        }
        assertEquals("Expected exit code", 0, po.getReturnCode());
        String output = po.getStdout();
        assertTrue("Label occurs more than 1", output.indexOf("Symbolic Name: ") < output.lastIndexOf("Symbolic Name: "));
        assertTrue("Description:", output.contains("Description: "));
        assertTrue("Enabled by:", output.contains("Enabled by:"));
        exiting(c, METHOD_NAME);
    }

    // EsaResource.findMatchingEsas unable to find symbolic name
    //@Test
    public void testFindId() throws Exception {
        Assume.assumeTrue(TestUtils.connectedToRepo);
        String METHOD_NAME = "testFindId";
        entering(c, METHOD_NAME);
        String[] param1s = { "find", "com.ibm.websphere.appserver.webCacheMonitor-1.0" };
        ProgramOutput po = runFeatureManager(METHOD_NAME, param1s);
        Log.info(FindCommandTest.class, METHOD_NAME, "StdOut: " + po.getStdout());
        if (po.getReturnCode() != 0) {
            Log.info(FindCommandTest.class, METHOD_NAME, "StdErr: " + po.getStderr());
        }
        assertEquals("Expected exit code", 0, po.getReturnCode());
        String output = po.getStdout();
        assertTrue("Should contain Web Cache Monitor", output.indexOf("Web Cache Monitor") >= 0);
        String split[] = output.split(" : ");
        assertEquals("Should only be 2", 2, split.length);
        exiting(c, METHOD_NAME);
    }

    @Test
    public void testFindIgnoreCase() throws Exception {
        Assume.assumeTrue(TestUtils.connectedToRepo);
        String METHOD_NAME = "testFindIgnoreCase";
        entering(c, METHOD_NAME);
        String[] param1s = { "find", "genericcorefeature" };
        ProgramOutput po = runFeatureManager(METHOD_NAME, param1s);
        Log.info(FindCommandTest.class, METHOD_NAME, "StdOut: " + po.getStdout());
        if (po.getReturnCode() != 0) {
            Log.info(FindCommandTest.class, METHOD_NAME, "StdErr: " + po.getStderr());
        }
        assertEquals("Expected exit code", 0, po.getReturnCode());
        String output = po.getStdout();
        assertTrue("Should contain genericCoreFeatureDependancyOnEsaPass",
                   output.indexOf("genericCoreFeatureDependancyOnEsaPass : com.ibm.genericCoreFeatureDependancyOnEsaPass") >= 0);
        String split[] = output.split(" : ");
        assertTrue("Should only be more than 2", split.length >= 2);
        exiting(c, METHOD_NAME);
    }

    @Test
    public void testFindShortDesc() throws Exception {
        Assume.assumeTrue(TestUtils.connectedToRepo);
        String METHOD_NAME = "testFindShortDesc";
        entering(c, METHOD_NAME);
        String[] param1s = { "find", "\"statistics\"" };
        ProgramOutput po = runFeatureManager(METHOD_NAME, param1s);
        Log.info(FindCommandTest.class, METHOD_NAME, "StdOut: " + po.getStdout());
        if (po.getReturnCode() != 0) {
            Log.info(FindCommandTest.class, METHOD_NAME, "StdErr: " + po.getStderr());
        }
        assertEquals("Expected exit code", 0, po.getReturnCode());
        String output = po.getStdout();
        assertTrue("Should contain Web Cache Monitor", output.indexOf("Web Cache Monitor") >= 0);
        exiting(c, METHOD_NAME);
    }

    // EsaResource.findMatchingEsas unable to find short name
    //@Test
    public void testFindShortName() throws Exception {
        Assume.assumeTrue(TestUtils.connectedToRepo);
        String METHOD_NAME = "testFindShortName";
        entering(c, METHOD_NAME);
        String[] param1s = { "find", "\"webCacheMonitor-1.0\"" };
        ProgramOutput po = runFeatureManager(METHOD_NAME, param1s);
        Log.info(FindCommandTest.class, METHOD_NAME, "StdOut: " + po.getStdout());
        if (po.getReturnCode() != 0) {
            Log.info(FindCommandTest.class, METHOD_NAME, "StdErr: " + po.getStderr());
        }
        assertEquals("Expected exit code", 0, po.getReturnCode());
        String output = po.getStdout();
        assertTrue("Should contain Web Cache Monitor", output.indexOf("Web Cache Monitor") >= 0);
        String split[] = output.split(" : ");
        assertEquals("Should only be 2", 2, split.length);
        exiting(c, METHOD_NAME);
    }

    @Test
    public void testFindError() throws Exception {
        Assume.assumeTrue(TestUtils.connectedToRepo);
        String METHOD_NAME = "testFindError";
        entering(c, METHOD_NAME);
        String[] param1s = { "find", "GENERICCOREVERSIONINDEPENDENTB" };
        ProgramOutput po = runFeatureManagerWithInvalidRepository(METHOD_NAME, param1s);
        Log.info(FindCommandTest.class, METHOD_NAME, "StdOut: " + po.getStdout());
        if (po.getReturnCode() != 0) {
            Log.info(FindCommandTest.class, METHOD_NAME, "StdErr: " + po.getStderr());
        }
        assertEquals("Expected exit code", 21, po.getReturnCode());
        String output = po.getStdout();
        assertTrue("Should contain CWWKF1219E", output.indexOf("CWWKF1219E") >= 0);
        exiting(c, METHOD_NAME);
    }

    @Test
    public void testFindNoFeature() throws Exception {
        Assume.assumeTrue(TestUtils.connectedToRepo);
        String METHOD_NAME = "testFindNoFeature";
        entering(c, METHOD_NAME);
        String[] param1s = { "find", "XXXXXXX" };
        ProgramOutput po = runFeatureManager(METHOD_NAME, param1s);
        Log.info(FindCommandTest.class, METHOD_NAME, "StdOut: " + po.getStdout());
        if (po.getReturnCode() != 0) {
            Log.info(FindCommandTest.class, METHOD_NAME, "StdErr: " + po.getStderr());
        }
        assertEquals("Expected exit code", 0, po.getReturnCode());
        String output = po.getStdout();
        assertTrue("No feature was found.", output.indexOf("No feature was found.") >= 0);
        exiting(c, METHOD_NAME);
    }

    @Test
    public void testFindFeatureCollection() throws Exception {
        Assume.assumeTrue(TestUtils.connectedToRepo);
        String METHOD_NAME = "testFindFeatureCollection";
        entering(c, METHOD_NAME);
        replaceWlpProperties(null, "ND", "InstallationManager");
        try {
            String[] param1s = { "find", "\"Install Extended Package\"" };
            ProgramOutput po = runFeatureManager(METHOD_NAME, param1s);
            Log.info(FindCommandTest.class, METHOD_NAME, "StdOut: " + po.getStdout());
            if (po.getReturnCode() != 0) {
                Log.info(FindCommandTest.class, METHOD_NAME, "StdErr: " + po.getStderr());
            }
            assertEquals("Expected exit code", 0, po.getReturnCode());
            String output = po.getStdout();
            assertTrue("Should contain Liberty Profile V8.5.5.x Install Extended Package", output.indexOf("Liberty Profile V8.5.5.x Install Extended Package") >= 0);
        } finally {
            resetOriginalWlpProps();
            exiting(c, METHOD_NAME);
        }
    }

    @Mode(TestMode.FULL)
    @Test
    public void testFindDupProductPropertiesFile() throws Exception {
        Assume.assumeTrue(TestUtils.connectedToRepo);
        String METHOD_NAME = "testFindDupProductPropertiesFile";
        entering(c, METHOD_NAME);
        try {
            server.copyFileToLibertyInstallRoot("lib/versions", "dupWAS.properties");
            String[] param1s = { "find", "com.ibm.installExtendedPackage-1.0" };
            ProgramOutput po = runFeatureManager(METHOD_NAME, param1s);
            Log.info(FindCommandTest.class, METHOD_NAME, "StdOut: " + po.getStdout());
            if (po.getReturnCode() != 0) {
                Log.info(FindCommandTest.class, METHOD_NAME, "StdErr: " + po.getStderr());
            }
            assertEquals("Expected exit code", 21, po.getReturnCode());
            String output = po.getStdout();
            assertTrue("Should contain \"The value of com.ibm.websphere.productId must be unique\"",
                       output.indexOf("The value of com.ibm.websphere.productId must be unique") >= 0);
        } finally {
            server.deleteFileFromLibertyInstallRoot("lib/versions/dupWAS.properties");
        }
        exiting(c, METHOD_NAME);
    }

    @Test
    public void testFindProductPropertiesFileMissingProductVersion() throws Exception {
        Assume.assumeTrue(TestUtils.connectedToRepo);
        String METHOD_NAME = "testFindProductPropertiesFileMissingProductVersion";
        entering(c, METHOD_NAME);
        try {
            server.copyFileToLibertyInstallRoot("lib/versions", "missingProductVersionWAS.properties");
            String[] param1s = { "find", "com.ibm.installExtendedPackage-1.0" };
            ProgramOutput po = runFeatureManager(METHOD_NAME, param1s);
            Log.info(FindCommandTest.class, METHOD_NAME, "StdOut: " + po.getStdout());
            if (po.getReturnCode() != 0) {
                Log.info(FindCommandTest.class, METHOD_NAME, "StdErr: " + po.getStderr());
            }
            assertEquals("Expected exit code", 21, po.getReturnCode());
            String output = po.getStdout();
            assertTrue("Should contain \"The required property key com.ibm.websphere.productVersion is missing in file\"",
                       output.indexOf("The required property key com.ibm.websphere.productVersion is missing in file") >= 0);
        } finally {
            server.deleteFileFromLibertyInstallRoot("lib/versions/missingProductVersionWAS.properties");
        }
        exiting(c, METHOD_NAME);
    }
}
