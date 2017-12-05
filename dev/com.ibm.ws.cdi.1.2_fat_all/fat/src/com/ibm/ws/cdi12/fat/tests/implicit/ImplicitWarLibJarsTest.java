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
package com.ibm.ws.cdi12.fat.tests.implicit;

import org.junit.AfterClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.ibm.ws.fat.util.LoggingTest;
import com.ibm.ws.fat.util.SharedServer;

import componenttest.rules.ServerRules;
import componenttest.rules.TestRules;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 * Test that library jars inside a war can be implicit bean archives.
 */
public class ImplicitWarLibJarsTest extends LoggingTest {

    private static final LibertyServer server = LibertyServerFactory.getLibertyServer("cdi12ImplicitServer");

    @ClassRule
    public static final TestRule startAndStopServerRule = ServerRules.startAndStopAutomatically(server);

    @Rule
    public final TestRule runAll = TestRules.runAllUsingTestNames(server).usingApp("implicitBeanDiscovery").andServlet("");

    /**
     * Test that we can inject beans from an explicit bean archive (sanity check).
     */
    @Test
    public void testExplicitBeanArchive() {}

    /**
     * Test that we can inject annotated beans from an archive with {@code bean-discovery-mode=annotated}.
     */
    @Test
    public void testAnnotatedBeanDiscoveryMode() {}

    /**
     * Test that we can inject annotated beans from an archive with no beans.xml.
     */
    @Test
    public void testNoBeansXml() {}

    /** {@inheritDoc} */
    @Override
    protected SharedServer getSharedServer() {
        // TODO Auto-generated method stub
        return null;
    }

    @AfterClass
    public static void afterClass() throws Exception {
        if (server != null) {
            server.stopServer();
        }
    }

}
