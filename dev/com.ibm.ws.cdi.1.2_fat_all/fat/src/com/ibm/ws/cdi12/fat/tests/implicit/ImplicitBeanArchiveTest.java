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
public class ImplicitBeanArchiveTest extends LoggingTest {

    private static final LibertyServer server = LibertyServerFactory.getLibertyServer("cdi12ImplicitServer");

    @ClassRule
    public static final TestRule startAndStopServerRule = ServerRules.startAndStopAutomatically(server);

    @Rule
    public final TestRule runAll = TestRules.runAllUsingTestNames(server).usingApp("implicitBeanArchive").andServlet("");

    @Test
    public void testUnannotatedBeanInAllModeBeanArchive() {
        //this one has a beans.xml with mode set to "all" so should be ok
    }

    @Test
    public void testApplicationScopedBeanInImplicitArchive() {
        //this one is an implicit bean so should be ok
    }

    @Test
    public void testConversationScopedBeanInImplicitArchive() {
        //this one is an implicit bean so should be ok
    }

    @Test
    public void testNormalScopedBeanInImplicitArchive() {
        //this one is an implicit bean so should be ok
    }

    @Test
    public void testStereotypedBeanInImplicitArchive() {
        //this one is an implicit bean so should be ok
    }

    @Test
    public void testRequestScopedBeanInImplicitArchive() {
        //this one is an implicit bean so should be ok
    }

    @Test
    public void testSessionScopedBeanInImplicitArchive() {
        //this one is an implicit bean so should be ok
    }

    @Test
    public void testUnannotatedBeanInImplicitArchive() {
        //this one is NOT an implicit bean and has no beans.xml so it should be null
    }

    @Test
    public void testDependentScopedBeanInAnnotatedModeArchive() {
        //this one is an implicit bean in an "annotated" mode archive so should be ok
    }

    @Test
    public void testUnannotatedBeanInAnnotatedModeArchive() {
        //this one is NOT an implicit bean in an "annotated" mode archive so should be null
    }

    @Test
    public void testRequestScopedBeanInNoneModeArchive() {
        //this one is an implicit bean in an "none" mode archive so should be null
    }

    @Test
    public void testClassWithInjectButNotInABeanArchive() {
        //this one is not an implicit bean and has no beans.xml so it should be null
    }

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
