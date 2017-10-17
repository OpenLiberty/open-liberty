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
package com.ibm.ws.cdi12.fat.tests.implicit;

import org.junit.ClassRule;
import org.junit.Test;

import com.ibm.ws.cdi12.suite.ShrinkWrapServer;
import com.ibm.ws.fat.util.LoggingTest;

import componenttest.custom.junit.runner.Mode;

public class ImplicitBeanArchiveNoAnnotationsTest extends LoggingTest {

    @ClassRule
    public static ShrinkWrapServer SHARED_SERVER = new ShrinkWrapServer("cdi12EjbDefInXmlServer");

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.fat.LoggingTest#getSharedServer()
     */
    @Override
    protected ShrinkWrapServer getSharedServer() {
        return SHARED_SERVER;
    }

    @Test
    public void testMultipleNamedEJBsInWar() throws Exception {
        this.verifyResponse("/archiveWithNoBeansXml/SimpleServlet", "PASSED");
    }

    @Mode
    @Test
    public void testConstructorInjection() throws Exception {
        this.verifyResponse("/archiveWithNoBeansXml/ConstructorInjectionServlet", "SUCCESSFUL");
    }

    @Test
    public void testMultipleNamesEjbsInEar() throws Exception {
        this.verifyResponse("/ejbArchiveWithNoAnnotations/ejbServlet", "PASSED");
    }

    @Test
    public void testEjbJarInWar() throws Exception {
        this.verifyResponse("/ejbJarInWarNoAnnotations/ejbServlet", "PASSED");
    }
}
