/*******************************************************************************
 * Copyright (c) 2015, 2026 IBM Corporation and others.
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
package com.ibm.ws.security.csiv2.fat;

import static org.junit.Assert.assertNull;

import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.security.csiv2.fat.common.CommonTest;
import com.ibm.ws.security.csiv2.fat.common.Constants;

import componenttest.annotation.MaximumJavaLevel;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

@Mode(TestMode.FULL)
// Java 2 security manager is permanently disabled in Java 24+, so any FATs that rely on it
// must be limited to a max level of Java 23 - https://openjdk.org/jeps/486
@MaximumJavaLevel(javaLevel = 23)
@org.junit.runner.RunWith(componenttest.custom.junit.runner.FATRunner.class)
public class Java2SecurityTest extends CommonTest {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        upstreamServer = setupUpstreamServer(Constants.CSIv2_JAVA2_UPSTREAM_SERVER, "client_minimal.xml");
        upstreamServer.setAllTestsRestartServer(true);
        downstreamServer = setupDownstreamServer(Constants.CSIv2_JAVA2_DOWNSTREAM_SERVER, "minimal.xml");
        downstreamServer.setAllTestsRestartServer(true);
        setupBasicAuthClient();
    }

    @Test
    public void testJava2SecurityTest_getSSLConfig() throws Exception {
        String response = driveDownstreamFlow();
        assertSubjectInResponse(response, user);

        LibertyServer upstreamLibertyServer = upstreamServer.getServer();
        assertNull("There should not be a Java2 Security exception for getSSLConfig in the upstream server.",
                   upstreamLibertyServer.verifyStringNotInLogUsingMark(".*getSSLConfig : Access denied.*", 10000));

        LibertyServer downstreamLibertyServer = downstreamServer.getServer();
        assertNull("There should not be a Java2 Security exception for getSSLConfig in the downstream server.",
                   downstreamLibertyServer.verifyStringNotInLogUsingMark(".*getSSLConfig : Access denied.*", 10000));
    }

}
