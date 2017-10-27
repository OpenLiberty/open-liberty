/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi12.fat.tests;

import org.junit.AfterClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.ibm.ws.cdi12.suite.ShutDownSharedServer;
import com.ibm.ws.fat.util.LoggingTest;

/**
 * All CDI tests with all applicable server features enabled.
 */
public class MultiModuleAppTest extends LoggingTest {

    @ClassRule
    public static ShutDownSharedServer SHARED_SERVER = new ShutDownSharedServer("cdi12MultiModuleServer");

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
    public void testMultiModuleApps() throws Exception {
        //part of multiModuleApp1
        this.verifyResponse(
                            "/multiModuleAppWeb1/",
                            "Test Sucessful!");
        //part of multiModuleApp1
        this.verifyResponse(
                            "/multiModuleAppWeb2/",
                            "Test Sucessful!");
        //part of multiModuleApp2
        this.verifyResponse(
                            "/multiModuleAppWeb3/",
                            "Test Sucessful!");
        //part of multiModuleApp2
        this.verifyResponse(
                            "/multiModuleAppWeb4/",
                            "Test Sucessful!");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (SHARED_SERVER != null && SHARED_SERVER.getLibertyServer().isStarted()) {
            SHARED_SERVER.getLibertyServer().stopServer("SRVE9967W");
        }
    }
}
