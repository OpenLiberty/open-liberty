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

import com.ibm.ws.cdi12.suite.ShutDownSharedServer;
import com.ibm.ws.fat.util.LoggingTest;

/**
 * This test requires the ejb-3.2 feature. I started with it merged into EjbTimerTest, but
 * that test depends on ejbLite-3.2, and/or there's something funny about the way it uses
 * SHARED_SERVER... either way, EjbTimerTest hard to add new tests to.
 */
public class EJB32Test extends LoggingTest {

    @ClassRule
    public static ShutDownSharedServer SHARED_SERVER = new ShutDownSharedServer("cdi12EJB32FullServer");

    @Test
    public void testRemoteEJBsWorkWithCDI() throws Exception {
        verifyResponse("/ejbMisc/AServlet", "observed=true");
    }

    @Override
    protected ShutDownSharedServer getSharedServer() {
        return SHARED_SERVER;
    }
}
