/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.wsoc.configurator;

import java.util.Arrays;
import java.util.List;

import junit.framework.Assert;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import test.common.SharedOutputManager;


public class ConfiguratorTests {
    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    /**
     * Capture stdout/stderr output to the manager.
     * 
     * @throws Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.captureStreams();

    }

    /**
     * Final teardown work when class is exiting.
     * 
     * @throws Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        // Make stdout and stderr "normal"
        outputMgr.restoreStreams();
    }

    /**
     * Individual teardown after each test.
     * 
     * @throws Exception
     */
    @After
    public void tearDown() throws Exception {
        // Clear the output generated after each method invocation
        outputMgr.resetStreams();
    }

    /**
     * Test some basic protocol matching...
     */
    @Test
    public void testURIs() {

        // Client ones have already been trimmed when processing headers...

        DefaultServerEndpointConfigurator dsec = new DefaultServerEndpointConfigurator();
        List<String> supported = Arrays.asList("");
        List<String> requested = Arrays.asList("ONE", "TWO", "THREE");
        String protocol = dsec.getNegotiatedSubprotocol(supported, requested);
        Assert.assertEquals("", protocol);

        supported = Arrays.asList("ONE", "TWO", "THREE");
        requested = Arrays.asList("");
        protocol = dsec.getNegotiatedSubprotocol(supported, requested);
        Assert.assertEquals("", protocol);

        supported = Arrays.asList("ONE", "TWO", "THREE");
        requested = Arrays.asList("ONE");
        protocol = dsec.getNegotiatedSubprotocol(supported, requested);
        Assert.assertEquals("ONE", protocol);

        supported = Arrays.asList("ONE", "TWO", "THREE");
        requested = Arrays.asList("ONE", "TWO", "THREE");
        protocol = dsec.getNegotiatedSubprotocol(supported, requested);
        Assert.assertEquals("ONE", protocol);

        supported = Arrays.asList("ONE", "TWO", "THREE");
        requested = Arrays.asList("THREE", "TWO", "ONE");
        protocol = dsec.getNegotiatedSubprotocol(supported, requested);
        Assert.assertEquals("THREE", protocol);

        supported = Arrays.asList("FIRST", "ONE", "TWO", "THREE");
        requested = Arrays.asList("ONE");
        protocol = dsec.getNegotiatedSubprotocol(supported, requested);
        Assert.assertEquals("ONE", protocol);

    }
}
