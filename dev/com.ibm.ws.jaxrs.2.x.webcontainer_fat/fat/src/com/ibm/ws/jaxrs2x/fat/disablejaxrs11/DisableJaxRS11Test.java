/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jaxrs2x.fat.disablejaxrs11;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(FATRunner.class)
public class DisableJaxRS11Test {

    @Server("jaxrs2x.service.DisableJaxRS11")
    public static LibertyServer server;

    @Test
    public void testDiableJaxRS11() {
        try {
            server.startServer(true);
            // (WI 234120) To match the message below in any language, we can only check for the message ID and untranslated arguments.
            // CWWKF0031I: The server skipped loading feature jaxrs-1.1 because equivalent functionality already exists
            String log = server.waitForStringInLog("CWWKF0031I.*jaxrs-1.1", 5000);
            Log.info(this.getClass(), "testDiableJaxRS11", log);

            assertTrue("fail to disable jaxrs-1.1", log.matches(".*CWWKF0031I.*jaxrs-1.1.*"));

            server.stopServer();
        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }

}