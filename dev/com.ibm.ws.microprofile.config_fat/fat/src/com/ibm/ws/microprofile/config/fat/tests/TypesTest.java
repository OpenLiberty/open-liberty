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
public class TypesTest extends AbstractConfigApiTest {

    @ClassRule
    public static SharedServer SHARED_SERVER = new SharedServer("TypesServer");

    public TypesTest() {
        super("/types/");
    }

    @Override
    protected SharedServer getSharedServer() {
        return SHARED_SERVER;
    }

    @Rule
    public TestName testName = new TestName();

    /** Tests that a user can retrieve properties of type boolean */
    @Test
    public void testBooleanTypes() throws Exception {
        test(testName.getMethodName());
    }

    /** Tests that a user can retrieve properties of type Integer */
    @Test
    public void testIntegerTypes() throws Exception {
        test(testName.getMethodName());
    }
}
