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

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 * Tests the scenario where a bean is in a JAR file that is nested in a RAR file.
 */
public class JarInRarTest {

    private static LibertyServer server;

    @BeforeClass
    public static void beforeClass() {
        server = LibertyServerFactory.getStartedLibertyServer("cdi12JarInRar");
    }

    @AfterClass
    public static void afterClass() throws Exception {
        if (server != null) {
            server.stopServer();
        }
    }

    @Test
    public void testBeanFromJarInRarInjectedIntoEJB() throws Exception {
        List<String> msgs = server.findStringsInLogs("MySingletonStartupBean - init - Buenos Dias me Amigo");
        assertEquals("Did not find expected injection message from EJB", 1, msgs.size());
    }
}
