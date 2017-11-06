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
package com.ibm.ws.clientcontainer.fat;

import org.junit.Test;

import componenttest.custom.junit.runner.Mode;

@Mode(Mode.TestMode.FULL)
public class AppServerTest extends AbstractTest {
    boolean isClient = false;

    /*
     * Check if appClientSupport-1.0 is loaded fine in server.xml.
     */
    @Test
    public void testAppServerStart() throws Exception {
        while (server.isStarted());
        server.copyFileToLibertyServerRoot("servers/HelloAppServer/server.xml");
        startProcess(isClient);
        assertStartMessages(isClient);
        stopProcess(isClient);
    }

    /*
     * Check if javaee-7.0 is loaded fine in server.xml.
     */
    @Test
    public void testAppServerStartWithJavaEE() throws Exception {
        while (server.isStarted());
        server.copyFileToLibertyServerRoot("servers/HelloAppServerWithJavaEE/server.xml");
        startProcess(isClient);
        assertStartMessages(isClient);
        stopProcess(isClient);
    }
}
