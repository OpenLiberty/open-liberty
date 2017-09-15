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
public class ConvertersTest extends AbstractConfigApiTest {

    @ClassRule
    public static SharedServer SHARED_SERVER = new SharedServer("ConvertersServer");

    public ConvertersTest() {
        super("/converters/");
    }

    @Override
    protected SharedServer getSharedServer() {
        return SHARED_SERVER;
    }

    @Rule
    public TestName testName = new TestName();

    /**
     * Test that a simple converter can be registered for a user type
     *
     * @throws Exception
     */
    @Test
    public void testConverters() throws Exception {
        test(testName.getMethodName());
    }

    /**
     * Test support for duplicate converters
     *
     * @throws Exception
     */
    @Test
    public void testMultipleSameTypeConverters() throws Exception {
        test(testName.getMethodName());
    }

    /**
     * Test support for different type and subclass converters
     *
     * @throws Exception
     */
    @Test
    public void testConverterSubclass() throws Exception {
        test(testName.getMethodName());
    }

    /**
     * Test support for discovered converters
     *
     * @throws Exception
     */
    @Test
    public void testDiscoveredConverters() throws Exception {
        test(testName.getMethodName());
    }

    /**
     * Test what happens when a converter raises an exception
     *
     * @throws Exception
     */
    @Test
    @ExpectedFFDC({ "java.lang.IllegalArgumentException", "com.ibm.ws.microprofile.config.interfaces.ConversionException" })
    public void testConverterExceptions() throws Exception {
        test(testName.getMethodName());
    }

}
