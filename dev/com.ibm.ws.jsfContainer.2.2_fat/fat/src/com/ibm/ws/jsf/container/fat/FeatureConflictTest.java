/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsf.container.fat;

import static org.junit.Assert.assertNotNull;

import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
public class FeatureConflictTest extends FATServletClient {

    @Server("jsf.container.2.2_fat.featureconflict")
    public static LibertyServer server;

    @AfterClass
    public static void testCleanup() throws Exception {
        server.stopServer("CWWKF0033E");
    }

    /**
     * Verify that the jsf-2.2 and jsfContainer-2.2 features cannot be loaded at the same time
     */
    @Test
    @ExpectedFFDC("java.lang.IllegalArgumentException")
    public void testFeatureConflict() throws Exception {
        server.startServer();
        assertNotNull(server.waitForStringInLog(".* CWWKF0033E: " +
                                                ".* com.ibm.websphere.appserver.jsfProvider-2.2.0.[MyFaces|Container]" +
                                                ".* com.ibm.websphere.appserver.jsfProvider-2.2.0.[MyFaces|Container].*"));
    }
}
