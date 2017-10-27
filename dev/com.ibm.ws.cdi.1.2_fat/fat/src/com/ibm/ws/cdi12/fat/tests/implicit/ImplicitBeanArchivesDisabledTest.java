/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi12.fat.tests.implicit;

import org.junit.AfterClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.ibm.ws.cdi12.suite.ShutDownSharedServer;
import com.ibm.ws.fat.util.LoggingTest;

public class ImplicitBeanArchivesDisabledTest extends LoggingTest {

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.fat.LoggingTest#getSharedServer()
     */
    @ClassRule
    public static ShutDownSharedServer SHARED_SERVER = new ShutDownSharedServer("cdi12DisableImplicitBeanArchiveServer");

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.fat.LoggingTest#getSharedServer()
     */
    @Override
    protected ShutDownSharedServer getSharedServer() {
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
