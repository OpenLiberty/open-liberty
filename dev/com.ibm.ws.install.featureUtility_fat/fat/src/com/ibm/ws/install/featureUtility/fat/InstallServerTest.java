package com.ibm.ws.install.featureUtility.fat;

import static org.junit.Assert.assertEquals;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.websphere.simplicity.log.Log;

public class InstallServerTest extends FeatureUtilityToolTest {
    private static final Class<?> c = FeatureUtilityToolTest.class;

    @BeforeClass
    public static void beforeClassSetup() throws Exception {
        final String methodName = "setup";
        Log.entering(c, methodName);
        setupEnv();
        Log.exiting(c, methodName);
    }

    @AfterClass
    public static void cleanUp() {
        // TODO
    }

    @Test
    public void testInstallFromMavenCentral() throws Exception {
        // TODO actualy use server.xml here
        final String METHOD_NAME = "testInstallFromMavenCentral";
        Log.entering(c, METHOD_NAME);

        assertEquals("TODO", 1+1, 2);
        Log.exiting(c, METHOD_NAME);

    }

}
