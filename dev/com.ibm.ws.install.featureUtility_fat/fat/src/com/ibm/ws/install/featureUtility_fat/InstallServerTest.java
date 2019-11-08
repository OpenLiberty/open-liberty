package com.ibm.ws.install.featureUtility_fat;

import static org.junit.Assert.assertEquals;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.websphere.simplicity.ProgramOutput;

public class InstallServerTest extends FeatureUtilityToolTest {
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

    @Test
    public void testInstallFromMavenCentral() throws Exception {
        // TODO actualy use server.xml here
        final String METHOD_NAME = "testInstallFromMavenCentral";
        entering(c, METHOD_NAME);

        assertEquals("TODO", 1+1, 2);
        exiting(c, METHOD_NAME);

    }

}
