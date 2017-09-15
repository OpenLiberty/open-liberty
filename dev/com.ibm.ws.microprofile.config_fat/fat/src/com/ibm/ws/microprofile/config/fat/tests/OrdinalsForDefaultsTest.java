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

/**
 *
 */
public class OrdinalsForDefaultsTest extends AbstractConfigApiTest {

    @ClassRule
    public static SharedServer SHARED_SERVER = new SharedServer("OrdForDefaultsServer");

    public OrdinalsForDefaultsTest() {
        super("/ordForDefaults/");
    }

    @Override
    protected SharedServer getSharedServer() {
        return SHARED_SERVER;
    }

    @Rule
    public TestName testName = new TestName();

    /**
     * Tests that default properties files can tolerate having the same
     * property defined in more that on micro-profile.xxx file and behaviour
     * is as expected.
     *
     * @throws Exception
     */
    @Test
    public void defaultsMixedOrdinals() throws Exception {
        test(testName.getMethodName());
    }

    @Test
    public void defaultsOrdinalFromSource() throws Exception {
        test(testName.getMethodName());
    }
}
