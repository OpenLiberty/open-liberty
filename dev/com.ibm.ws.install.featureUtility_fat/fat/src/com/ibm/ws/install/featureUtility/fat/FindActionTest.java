package com.ibm.ws.install.featureUtility.fat;

import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.log.Log;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FindActionTest extends  FeatureUtilityToolTest {

    private static final Class<?> c = FindActionTest.class;

    @BeforeClass
    public static void beforeClassSetup() throws Exception {
        final String methodName = "beforeClassSetup";
        Log.entering(c, methodName);
        setupEnv();

        // rollback wlp version 2 times (e.g 20.0.0.5 -> 20.0.0.3)
        replaceWlpProperties(getPreviousWlpVersion());
        replaceWlpProperties(getPreviousWlpVersion());
        Log.exiting(c, methodName);
    }

    @AfterClass
    public static void cleanUp() throws Exception {
        // TODO
        resetOriginalWlpProps();
        cleanUpTempFiles();
    }

    /**
     * Tests the find action when supplied no arguments.
     * i.e ./featureUtility find
     * The command should return a list of all the available features from the jsons.
     */
    @Test
    public void testFindNoArgs() throws Exception {
        final String METHOD_NAME = "testFindNoArgs";
        Log.entering(c, METHOD_NAME);

        // run the command
        String[] param1s = { "find"};
        ProgramOutput po = runFeatureUtility(METHOD_NAME, param1s);
        assertEquals("Exit code should be 0",0, po.getReturnCode());
        String output = po.getStdout();

        // check for open liberty feature
        assertTrue("Should contain mpHealth-2.0", output.contains("mpHealth-2.0"));

        // check for closed liberty feature
        assertTrue("Should contain adminCenter-1.0", output.contains("adminCenter-1.0"));

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * Tests the find action when supplied an Open Liberty feature (jsp-2.3).
     * The command should return the specified feature (jsp-2.3 in this case).
     */
    @Test
    public void testFindOpenLibertyFeature() throws Exception {
        final String METHOD_NAME = "testFindOpenLibertyFeature";
        Log.entering(c, METHOD_NAME);

        // run the command
        String[] param1s = { "find", "jsp-2.3"};
        ProgramOutput po = runFeatureUtility(METHOD_NAME, param1s);
        assertEquals("Exit code should be 0",0, po.getReturnCode());
        String output = po.getStdout();

        // check for jsp-2.3
        assertTrue("Should contain jsp-2.3", output.contains("jsp-2.3"));

        Log.exiting(c, METHOD_NAME);
    }
    /**
     * Tests the find action when supplied a closed liberty feature (adminCenter-1.0)
     * The command should return the specified feature (adminCenter-1.0 in this test case).
     */
    @Test
    public void testFindClosedLibertyFeature() throws Exception {
        final String METHOD_NAME = "testFindClosedLibertyFeature";
        Log.entering(c, METHOD_NAME);

        // run the command
        String[] param1s = { "find", "adminCenter-1.0"};
        ProgramOutput po = runFeatureUtility(METHOD_NAME, param1s);
        assertEquals("Exit code should be 0",0, po.getReturnCode());
        String output = po.getStdout();

        // check for jsp-2.3
        assertTrue("Should contain adminCenter-1.0", output.contains("adminCenter-1.0"));

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * Test the find action's ability to handle multiple strings as arguments.
     * i.e ./featureUtility find rest api
     * It should return apiDiscovery-1.0 because it's description contains those words in sequence.
     */
    @Test
    public void testFindMultipleArgs() throws Exception {
        final String METHOD_NAME = "testFindMultipleArgs";
        Log.entering(c, METHOD_NAME);

        // run the command
        String[] param1s = { "find", "rest", "api"};
        ProgramOutput po = runFeatureUtility(METHOD_NAME, param1s);
        assertEquals("Exit code should be 0",0, po.getReturnCode());
        String output = po.getStdout();

        // check for apiDiscovery-1.0
        assertTrue("Should contain apiDiscovery-1.0", output.contains("apiDiscovery-1.0"));

        Log.exiting(c, METHOD_NAME);
    }


    /**
     * Test the find commands ability to restrict hidden features from the output.
     * In this test case, we try finding the autofeature cdi1.0-ejblite3.1. The find command should not
     * list that autofeature in the output because it's a hidden feature.
     * @throws Exception
     */
    @Test
    public void testFindHiddenFeature() throws Exception {
        final String METHOD_NAME = "testFindMultipleArgs";
        Log.entering(c, METHOD_NAME);

        // run the command
        String[] param1s = { "find", "cdi1.0-ejblite3.1"};
        ProgramOutput po = runFeatureUtility(METHOD_NAME, param1s);
        assertEquals("Exit code should be 0",0, po.getReturnCode());
        String output = po.getStdout();

        // ensure that cdi1.0-ejblite3.1 is not in the output
        assertTrue("Should not contain cdi1.0-ejblite3.1", !output.contains("cdi1.0-ejblite3.1"));

        Log.exiting(c, METHOD_NAME);
    }


}
