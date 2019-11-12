package com.ibm.ws.install.featureUtility.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.log.Log;

public class InstallFeatureTest extends FeatureUtilityToolTest {

    private static final Class<?> c = InstallFeatureTest.class;

    @BeforeClass
    public static void beforeClassSetup() throws Exception {
        final String methodName = "setup";
        Log.entering(c, methodName);
        setupEnv();
        replaceWlpProperties("19.0.0.11");
        Log.exiting(c, methodName);
    }
    
    @AfterClass
    public static void cleanUp() throws Exception {
        // TODO
        resetOriginalWlpProps();
    }

    /**
     * Test the install of jsp-2.3 from maven central.
     * 
     * @throws Exception
     */
    @Test
    public void testInstallFeature() throws Exception {
        final String METHOD_NAME = "testInstallFeature";
        Log.entering(c, METHOD_NAME);

        String[] param1s = { "installFeature", "jsp-2.3"};
        String [] fileLists = {"lib/features/com.ibm.websphere.appserver.jsp-2.3.mf"};
        deleteFiles(METHOD_NAME, "com.ibm.websphere.appserver.jsp-2.3", fileLists);

        ProgramOutput po = runFeatureUtility(METHOD_NAME, param1s);
        assertEquals("Exit code should be 0",0, po.getReturnCode());
        String output = po.getStdout();
        assertTrue("Should contain jsp-2.3", output.indexOf("jsp-2.3") >= 0);

        deleteFiles(METHOD_NAME, "com.ibm.websphere.appserver.jsp-2.3", fileLists);
        Log.exiting(c, METHOD_NAME);
    }

    /**
     * Test the installation of a closed liberty feature (adminCenter-1.0) in Open Liberty
     * using Feature Utility.
     * @throws Exception
     */
    @Test
    public void testClosedLibertyFeature() throws Exception {
        final String METHOD_NAME = "testClosedLibertyFeature";
        Log.entering(c, METHOD_NAME);

        String[] param1s = { "installFeature", "adminCenter-1.0"};
        ProgramOutput po = runFeatureUtility(METHOD_NAME, param1s);
        assertEquals("Exit code should be 21",21,  po.getReturnCode());
        String output = po.getStdout();
        assertTrue("Should contain CWWKF1299E", output.indexOf("CWWKF1299E") >= 0);
        Log.exiting(c, METHOD_NAME);
    }

    @Test
    public void testInstallAutoFeature() throws Exception {
        final String METHOD_NAME = "testInstallAutoFeature";
        Log.entering(c, METHOD_NAME);

        String[] param1s = { "installFeature", "jsf-2.2", "cdi-1.2"};
        String [] fileListA = {"lib/features/com.ibm.websphere.appserver.jsf-2.2.mf", "lib/features/com.ibm.websphere.appserver.cdi1.2-jsf2.2.mf"};
        String [] fileListB = {"lib/features/com.ibm.websphere.appserver.cdi-1.2.mf"};
        deleteFiles(METHOD_NAME, "com.ibm.websphere.appserver.jsf-2.2", fileListA);
        deleteFiles(METHOD_NAME, "com.ibm.websphere.appserver.cdi-1.2", fileListB);


        ProgramOutput po = runFeatureUtility(METHOD_NAME, param1s);
        assertEquals("Exit code should be 0",0, po.getReturnCode());
        String output = po.getStdout();
        assertTrue("Output should contain jsf-2.2", output.indexOf("jsf-2.2") >= 0);
        assertTrue("Output should contain cdi-1.2", output.indexOf("cdi-1.2") >= 0);
        assertTrue("The autofeature cdi1.2-jsf-2.2 should be installed" , new File(installRoot + "/lib/features/com.ibm.websphere.appserver.cdi1.2-jsf2.2.mf").exists());

        deleteFiles(METHOD_NAME, "com.ibm.websphere.appserver.jsf-2.2", fileListA);
        deleteFiles(METHOD_NAME, "com.ibm.websphere.appserver.cdi-1.2", fileListB);

        Log.exiting(c, METHOD_NAME);
    }


}
