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

import org.junit.AfterClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.ibm.ws.cdi12.suite.ShrinkWrapServer;
import com.ibm.ws.fat.util.LoggingTest;

public class ImplicitBeanArchivesDisabledTest extends LoggingTest {

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.fat.LoggingTest#getSharedServer()
     */
    @ClassRule
    public static ShrinkWrapServer SHARED_SERVER = new ShrinkWrapServer("cdi12DisableImplicitBeanArchiveServer");

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
    public void testDisableImplicitBeanArchives() throws Exception {
        //part of multiModuleApp1
        this.verifyResponse(
                            "/implicitBeanArchiveDisabled/",
                            "Car Bike No Plane!");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (SHARED_SERVER != null && SHARED_SERVER.getLibertyServer().isStarted()) {
            /*
             * Ignore following warning as it is expected: CWOWB1009W: Implicit bean archives are disabled.
             */
            SHARED_SERVER.getLibertyServer().stopServer("CWOWB1009W");
        }
    }

}
