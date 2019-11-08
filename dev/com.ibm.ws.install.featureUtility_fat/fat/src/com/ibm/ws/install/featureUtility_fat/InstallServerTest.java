package com.ibm.ws.install.featureUtility_fat;

import org.junit.AfterClass;
import org.junit.BeforeClass;

public class DownloadTest extends FeatureUtilityToolTest {
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

    // @Test
}
