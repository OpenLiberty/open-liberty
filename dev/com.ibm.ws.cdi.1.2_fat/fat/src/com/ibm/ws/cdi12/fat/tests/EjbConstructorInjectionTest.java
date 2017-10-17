/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2015
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.cdi12.fat.tests;

import org.junit.ClassRule;
import org.junit.Test;

import com.ibm.ws.cdi12.suite.ShrinkWrapServer;
import com.ibm.ws.fat.util.LoggingTest;

public class EjbConstructorInjectionTest extends LoggingTest {

    @ClassRule
    // Create the server and install the CDIOWB Test feature.
    public static ShrinkWrapServer SHARED_SERVER = new ShrinkWrapServer("cdi12EjbConstructorInjectionServer");

    @Override
    protected ShrinkWrapServer getSharedServer() {
        return SHARED_SERVER;
    }

    @Test
    public void testTransientReferenceOnEjbConstructor() throws Exception {
        this.verifyResponse("/ejbConstructorInjection/Servlet", new String[] { "destroy called",
                                                                               "First bean message: foo",
                                                                               "Second bean message: bar",
                                                                               "Third bean message: spam",
                                                                               "Forth bean message: eggs" });
    }

}
