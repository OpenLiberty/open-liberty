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
package com.ibm.ws.javamail.fat;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;

import org.junit.Test;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

@Mode(TestMode.FULL)
public class MutuallyExclusive {

    private static LibertyServer server = LibertyServerFactory.getLibertyServer("exclusiveFeature");
    private final Class<?> c = MutuallyExclusive.class;

/*
 * Test to make sure you can't configure JavaMail-1.5 and JavaMail-1.6 at the same time.
 */
    @Test
    public void testMutuallyExclusive() throws Exception {

        Log.info(c, "testMutuallyExclusive", "begin");
        ArrayList<String> foundStrings = null;
        boolean found = false;
        try {
            // preClean = true, clean=true, validate apps=false, expectStartFailure=true
            server.startServerAndValidate(true, true, false, true);
            foundStrings = (ArrayList<String>) server.findStringsInLogs("CWWKF0033E");
            if (foundStrings != null && !foundStrings.isEmpty())
                found = true;
            assertTrue("Failure: server started successfully with javaMail-1.5 and javaMail-1.6 configured", found);
        } catch (Exception e) {
            throw e;
        } finally {
            server.stopServer();
        }

    }
}
