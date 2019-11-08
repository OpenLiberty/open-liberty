package com.ibm.ws.install.featureUtility_fat;

import static org.junit.Assert.assertEquals;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.websphere.simplicity.ProgramOutput;


public class InstallTest extends FeatureUtilityToolTest {

    private static final Class<?> c = FeatureUtilityToolTest.class;

    @BeforeClass
    public static void beforeClassSetup() throws Exception {
        final String methodName = "setup";
        entering(c, methodName);
        setupEnv();
        exiting(c, methodName);
    }
    
    @AfterClass
    public static void cleanUp() {
        // TODO
    }

    /**
     * Test the install of jsp-2.3 from maven central.
     * 
     * @throws Exception
     */
    @Test
    public void testInstallFromMavenCentral() throws Exception {
        final String METHOD_NAME = "testInstallFromMavenCentral";
        entering(c, METHOD_NAME);

        String[] param1s = { "installFeature", "jsp-2.3" };

        ProgramOutput po;
        po = runFeatureUtility(METHOD_NAME, param1s);
        assertEquals("Exit code should be 0", po.getReturnCode(), 0);
        exiting(c, METHOD_NAME);

    }

    /**
     * Test install of mpHealth-2.0 from a local maven based feature repo
     * 
     * @throws Exception
     */
    @Test
    public void testInstallFromLocalRepo() throws Exception {
        // TODO: implement from
        final String METHOD_NAME = "testInstallFromLocalRepo";
        entering(c, METHOD_NAME);

        String[] param1s = { "installFeature", "mpHealth-2.0" };

        ProgramOutput po;
        po = runFeatureUtility(METHOD_NAME, param1s);
        assertEquals("Exit code should be 0", po.getReturnCode(), 0);
        exiting(c, METHOD_NAME);
    }

    @Test
    public void testInstallWithMirrorRepository() throws Exception {
        // TODO
        final String METHOD_NAME = "testInstallWithMirrorRepository";
        entering(c, METHOD_NAME);

        String[] param1s = { "installFeature", "webProfile-8.0" };


        ProgramOutput po;
        po = runFeatureUtility(METHOD_NAME, param1s);
        assertEquals("Exit code should be 0", po.getReturnCode(), 0);
        exiting(c, METHOD_NAME);
    }

}
