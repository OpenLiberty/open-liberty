package com.ibm.ws.install.featureUtility.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.log.Log;

public class InstallServerTest extends FeatureUtilityToolTest {
    private static final Class<?> c = FeatureUtilityToolTest.class;

    @BeforeClass
    public static void beforeClassSetup() throws Exception {
        final String methodName = "setup";
        Log.entering(c, methodName);
        setupEnv();
        copyFileToMinifiedRoot("usr/temp", "../../publish/tmp/serverX.zip");
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
     * Install jsf-2.2 on its own first, then install a server.xml with jsf-2.2 and cdi-1.2. The autofeature cdi1.2-jsf2.2 should be installed along with the other features.
     * @throws Exception
     */
    @Test
    public void testInstallAutoFeatureServerXml() throws Exception {
        final String METHOD_NAME = "testInstallAutoFeatureServerXml";
        Log.entering(c, METHOD_NAME);

        String [] param1s = {"installFeature", "jsf-2.2"};
        deleteFeaturesAndLafilesFolders(METHOD_NAME);

        ProgramOutput po = runFeatureUtility(METHOD_NAME, param1s);
        assertEquals("Exit code should be 0",0, po.getReturnCode());
        String output = po.getStdout();
        assertTrue("Output should contain jsf-2.2", output.indexOf("jsf-2.2") >= 0);

        // replace the server.xml and install from server.xml now
        copyFileToMinifiedRoot("usr/servers/serverX", "../../publish/tmp/autoFeatureServerXml/server.xml");
        String[] param2s = { "installServerFeatures", "serverX"};
        deleteFeaturesAndLafilesFolders(METHOD_NAME);


        po = runFeatureUtility(METHOD_NAME, param2s);
        assertEquals("Exit code should be 0",0, po.getReturnCode());
        output = po.getStdout();
        assertTrue("Output should contain jsf-2.2", output.indexOf("jsf-2.2") >= 0);
        assertTrue("Output should contain cdi-1.2", output.indexOf("cdi-1.2") >= 0);
        assertTrue("The autofeature cdi1.2-jsf-2.2 should be installed" , new File(minifiedRoot + "/lib/features/com.ibm.websphere.appserver.cdi1.2-jsf2.2.mf").exists());

        deleteFeaturesAndLafilesFolders(METHOD_NAME);


        Log.exiting(c, METHOD_NAME);
    }


}
