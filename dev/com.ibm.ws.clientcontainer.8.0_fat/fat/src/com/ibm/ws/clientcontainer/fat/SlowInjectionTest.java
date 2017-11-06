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

import static org.junit.Assert.assertEquals;

import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TestRule;

import componenttest.custom.junit.runner.OnlyRunInJava7Rule;

//@Mode(TestMode.FULL)
public class SlowInjectionTest extends AbstractTest {

    @ClassRule
    public static TestRule java7Rule = new OnlyRunInJava7Rule();

    private final String testClientName = "com.ibm.ws.clientcontainer.fat.SlowInjectionClient";

    @Override
    protected String getClientName() {
        return testClientName;
    }

    @Test
    public void testInjectionTakesTooLong() throws Exception {
        client.copyFileToLibertyClientRoot("clients/SlowInjection/client.xml");
        startProcess();
        assertEquals("Missing expected message in client main's injection method indicating that injection is occurring", 1,
                     client.findStringsInCopiedLogs("SlowInjectionAppClient - sleeping").size());
        assertEquals("Missing expected message indicating a failure to launch", 1,
                     client.findStringsInCopiedLogs("java.lang.IllegalStateException: Failed to initialize client prior to attempted invocation.").size());
        assertEquals("Found unexpected message from client main method that should never have executed", 0,
                     client.findStringsInCopiedLogs("SlowInjectionAppClient - AppName").size());
    }

    @Test
    public void testInjectionError() throws Exception {
        client.copyFileToLibertyClientRoot("clients/ErrorInjection/client.xml");
        startProcess();
        assertEquals("Missing expected message in client main's injection method indicating that injection is occurring", 1,
                     client.findStringsInCopiedLogs("ErrorInjectionAppClient - setAppName ").size());
        assertEquals("Missing expected message indicating a failure to launch", 1,
                     client.findStringsInCopiedLogs("java.lang.IllegalStateException: Client module main failed initialization").size());
        assertEquals("Found unexpected message from client main method that should never have executed", 0,
                     client.findStringsInCopiedLogs("ErrorInjectionAppClient - main").size());
    }
}
