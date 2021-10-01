/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.cdi12.fat.test;


import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
@SkipForRepeat("EE9_FEATURES") //The fact this doesn't work on EE9 is probably a bug and needs investigating
public class DependentIntoJaxTest extends AbstractTest {

    @Server("com.ibm.ws.jaxrs20.cdi12.fat.basic")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        appname = "DependentIntoJax";
        ShrinkHelper.defaultDropinApp(server, appname, "com.ibm.ws.jaxrs20.cdi12.fat.dependentintojax");
        server.startServer();
    }

    @Before
    public void preTest() {
        serverRef = server;
    }

    @Test
    public void testDependentIntoJaxCleansUp() throws Exception {
        server.restartServer();
        runGetMethod("/rest/testDepPreDestroy", 200, "preDestroy was called " + 0 + " times", true);
        runGetMethod("/rest/testDepPreDestroy", 200, "preDestroy was called " + 1 + " times", true);
    }

}
