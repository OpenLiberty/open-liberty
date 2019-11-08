package com.ibm.ws.install.featureUtility_fat;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({

                InstallFeatureTest.class, InstallServerTest.class

})
public class FATSuite {

    /**
     * Start of FAT suite processing.
     *
     * @throws Exception
     */
    @BeforeClass
    public static void beforeSuite() throws Exception {
        // TODO
    }
}
