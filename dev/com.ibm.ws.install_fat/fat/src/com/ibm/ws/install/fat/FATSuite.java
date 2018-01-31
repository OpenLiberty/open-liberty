/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2013
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.install.fat;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import test.utils.TestCertTrust;
import test.utils.TestUtils;

@RunWith(Suite.class)
@SuiteClasses({
//		Disabled DirectorTest - see WI 247066
//		DirectorTest will be deleted once WI 247736 is completed
//                DirectorTest.class,
                InstallKernelTest.class,
                FeatureManagerToolTest.class,
                FindCommandTest.class
                // Minify Test will be renabled/moved via issue 8372
//               ,               MinifyTest.class
})
public class FATSuite {

    /**
     * Start of FAT suite processing.
     *
     * @throws Exception
     */
    @BeforeClass
    public static void beforeSuite() throws Exception {
        TestCertTrust.trustAll();
        TestUtils.setupWlpDirs();
        TestUtils.testRepositoryConnection();
    }

    /**
     * End of FAT suite processing.
     *
     * @throws Exception
     */
    @AfterClass
    public static void afterSuite() throws Exception {
        TestUtils.removeWlpDirs();
    }

}
