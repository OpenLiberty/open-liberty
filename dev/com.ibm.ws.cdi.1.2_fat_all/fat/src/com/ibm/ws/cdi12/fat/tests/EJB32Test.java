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
package com.ibm.ws.cdi12.fat.tests;

import org.junit.AfterClass;
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

    @AfterClass
    public static void afterClass() throws Exception {
        SHARED_SERVER.getLibertyServer().removeDropinsApplications("ejbMisc.war");
    }

    @Override
    protected ShutDownSharedServer getSharedServer() {
        return SHARED_SERVER;
    }
}
