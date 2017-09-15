/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2016
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.microprofile.config.fat.tests;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.ibm.ws.fat.util.SharedServer;

import componenttest.annotation.ExpectedFFDC;

/**
 *
 */
public class ClassLoadersTest extends AbstractConfigApiTest {

    @ClassRule
    public static SharedServer SHARED_SERVER = new SharedServer("ClassLoadersServer");

    public ClassLoadersTest() {
        super("/classLoaders/");
    }

    @Override
    protected SharedServer getSharedServer() {
        return SHARED_SERVER;
    }

    @Rule
    public TestName testName = new TestName();

    /**
     *
     * @throws Exception
     */
    @Test
    public void testUserClassLoaders() throws Exception {
        test(testName.getMethodName());
    }

    /**
     *
     * @throws Exception
     */
    @Test
    @ExpectedFFDC({ "java.util.ServiceConfigurationError" })
    public void testUserLoaderErrors() throws Exception {
        test(testName.getMethodName());
    }

    /**
     *
     * @throws Exception
     */
    @Test
    public void testMultiUrlResources() throws Exception {
        test(testName.getMethodName());
    }

}
