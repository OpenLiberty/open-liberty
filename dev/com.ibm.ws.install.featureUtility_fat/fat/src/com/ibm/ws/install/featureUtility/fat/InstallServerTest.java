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
        server.copyFileToLibertyInstallRoot("usr/temp", "../../publish/tmp/serverX.zip");
        replaceWlpProperties("19.0.0.11");
        Log.exiting(c, methodName);
    }

    @AfterClass
    public static void cleanUp() throws Exception {
        // TODO
        resetOriginalWlpProps();
    }

    @Test
    public void testInstallAutoFeatureServerXml() throws Exception {
        final String METHOD_NAME = "testInstallAutoFeatureServerXml";
        Log.entering(c, METHOD_NAME);

        // replace the server.xml
        server.copyFileToLibertyInstallRoot("usr/servers/serverX", "../../publish/tmp/autoFeatureServerXml/server.xml");

        String[] param1s = { "installServer", "serverX"};

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
