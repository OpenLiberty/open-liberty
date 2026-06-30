/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.fat;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.jaxrs.fat.securitylimits.SecurityLimitsTestServlet;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * FAT test class for CXF security limit changes:
 * <ul>
 *   <li>CXF #3159 – attachment-headers-max-count: limits the number of MIME
 *       headers allowed per attachment part (default 500).</li>
 *   <li>CXF #3177 – maxFormParameterCount default: applies a default limit of
 *       500 form parameters when no explicit value is configured.</li>
 * </ul>
 * Restricted to CXF-backed runtimes (jaxrs-2.0 / jaxrs-2.1); the properties
 * under test are CXF-specific and have no effect in RESTEasy (EE9+).
 */
@RunWith(FATRunner.class)
@SkipForRepeat({SkipForRepeat.EE9_FEATURES, SkipForRepeat.EE10_FEATURES, SkipForRepeat.EE11_FEATURES})
public class SecurityLimitsTest extends FATServletClient {

    private static final String war = "securitylimits";

    @Server("com.ibm.ws.jaxrs.fat.securitylimits")
    @TestServlet(servlet = SecurityLimitsTestServlet.class, contextRoot = war)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(server, war,
                                      "com.ibm.ws.jaxrs.fat.securitylimits");
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("CWWKW0101W", "CWWKW0102W", "SRVE8052E", "SRVE0276E",
                          "SRVE0777E", "SRVE0315E");
    }
}
