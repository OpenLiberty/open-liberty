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
package com.ibm.ws.wsoc.outbound;

import java.net.URI;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import test.common.SharedOutputManager;

public class AddressTest {
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
    public void testURIs() throws Exception {

        URI theUri = new URI("ws://localhost");
        WsocAddress addr = new WsocAddress(theUri);
        Assert.assertEquals(80, addr.getRemoteAddress().getPort());
        Assert.assertEquals(false, addr.isSecure());
        Assert.assertEquals("/", addr.getPath());
        addr.validateURI();

        theUri = new URI("wss://localhost");
        addr = new WsocAddress(theUri);
        Assert.assertEquals(443, addr.getRemoteAddress().getPort());
        Assert.assertEquals(true, addr.isSecure());
        Assert.assertEquals("/", addr.getPath());
        addr.validateURI();

        theUri = new URI("wss://localhost:444");
        addr = new WsocAddress(theUri);
        Assert.assertEquals(true, addr.isSecure());
        Assert.assertEquals("/", addr.getPath());
        Assert.assertEquals(444, addr.getRemoteAddress().getPort());
        addr.validateURI();

        theUri = new URI("wss://localhost/blahblah");
        addr = new WsocAddress(theUri);
        Assert.assertEquals("/blahblah", addr.getPath());
        addr.validateURI();

        theUri = new URI("WS://localhost/");
        addr = new WsocAddress(theUri);
        Assert.assertEquals("/", addr.getPath());
        Assert.assertEquals(false, addr.isSecure());
        addr.validateURI();

        theUri = new URI("WsS://localhost/qqq");
        addr = new WsocAddress(theUri);
        Assert.assertEquals(true, addr.isSecure());
        Assert.assertEquals("/qqq", addr.getPath());
        addr.validateURI();

        /*
         * strange log message, not going to run this one.. it passes though.
         * try {
         * theUri = new URI("http://localhost/");
         * addr = new WsocAddress(theUri);
         * addr.validateURI();
         * throw new Exception("http should of been recognized as invalid scheme");
         * } catch (IllegalArgumentException e) {
         * //Expected
         * }
         */

    }
}
