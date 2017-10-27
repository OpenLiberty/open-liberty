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

import org.junit.ClassRule;
import org.junit.Test;

import com.ibm.ws.cdi12.suite.ShutDownSharedServer;
import com.ibm.ws.fat.util.LoggingTest;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

/**
 * This test ensures that if a class in one module is masked by a class in another module, CDI doesn't break.
 * <p>
 * The test was introduced because CDI was assuming that all classes present in a module should be loaded by that modules classloader. However, they could be loaded from another
 * module which is on the application classpath.
 * <p>
 * We also test that beans in an App Client jar are not visible to other modules.
 */

public class ClassMaskingTest extends LoggingTest {

    @ClassRule
    public static ShutDownSharedServer server = new ShutDownSharedServer("cdi12ClassMasking");

    @Override
    protected ShutDownSharedServer getSharedServer() {
        return server;
    }

    //@Mode(TestMode.FULL)
    @Test
    public void testClassMasking() throws Exception {
        LibertyServer lServer = server.getLibertyServer();
        lServer.useSecondaryHTTPPort();

        HttpUtils.findStringInUrl(lServer, "/maskedClassWeb/TestServlet",
                                  "This is Type3, a managed bean in the war",
                                  "This is TestBean in the war");
    }
}
