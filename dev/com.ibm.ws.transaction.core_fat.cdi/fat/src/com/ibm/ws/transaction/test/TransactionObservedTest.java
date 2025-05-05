/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package com.ibm.ws.transaction.test;

import static org.junit.Assert.assertNotNull;

import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
public class TransactionObservedTest extends FATServletClient {

    private static final String APP_NAME = "transactionobserved";
    private static final String PROP_NAME = "-DenableBuggyOldBehavior";
    private static final String CONTEXT_IS_AVAILABLE = "Context is available in AFTER_SUCCESS observer";
    private static final String CONTEXT_IS_NOT_AVAILABLE = "Context is not available in AFTER_SUCCESS observer";

    @Server("com.ibm.ws.transaction_observed")
    public static LibertyServer server;

    @BeforeClass
    public static void beforeClass() throws Exception {
        ShrinkHelper.defaultApp(server, APP_NAME, "com.ibm.ws.transactionobserved.web.*");
    }

    @After
    public void after() throws Exception {
        server.stopServer();
    }

    @Before
    public void before() throws Exception {
        final Map<String, String> jvmOptions = server.getJvmOptionsAsMap();
        jvmOptions.remove(PROP_NAME);
        server.setJvmOptions(jvmOptions);
    }

    @Test
    public void testCorrectBehaviorNoProperty() throws Exception {
        server.startServer();
        assertNotNull(server.waitForStringInLog(CONTEXT_IS_AVAILABLE));
    }

    @Test
    public void testCorrectBehaviorFalseProperty() throws Exception {
        final Map<String, String> jvmOptions = server.getJvmOptionsAsMap();
        jvmOptions.put(PROP_NAME, Boolean.FALSE.toString());
        server.setJvmOptions(jvmOptions);
        server.startServer();
        assertNotNull(server.waitForStringInLog(CONTEXT_IS_AVAILABLE));
    }

    @Test
    public void testLegacyBehavior() throws Exception {
        final Map<String, String> jvmOptions = server.getJvmOptionsAsMap();
        jvmOptions.put(PROP_NAME, Boolean.TRUE.toString());
        server.setJvmOptions(jvmOptions);
        server.startServer();
        assertNotNull(server.waitForStringInLog(CONTEXT_IS_NOT_AVAILABLE));
    }
}
