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
package com.ibm.ws.wsoc.uri.internal;

import javax.websocket.server.ServerEndpointConfig;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import test.common.SharedOutputManager;

import com.ibm.ws.wsoc.EndpointManager;

//testing uri format code in EndpointManager can be simple junit tests
public class EndpointURITest {
    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    //create ServerEndpointConfigs to fill in serverEndpointConfigMap in EndpointManager
    private static ServerEndpointConfig config1;
    private static ServerEndpointConfig config2;
    private static ServerEndpointConfig config3;
    private static ServerEndpointConfig config4;
    private static ServerEndpointConfig config5;
    private static ServerEndpointConfig config6;
    private static ServerEndpointConfig config7;
    private static ServerEndpointConfig config8;
    private static ServerEndpointConfig config9;

    private static EndpointManager endpointManager = new EndpointManager();

    /**
     * Capture stdout/stderr output to the manager.
     * 
     * @throws Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.captureStreams();

        //pass EndpointURITest class for testing purpose. For testing purpose, we don't care which class 
        //is passed to 1st param, we only care about second param which is the uri
        config1 = ServerEndpointConfig.Builder.create(EndpointURITest.class, "/l/m/n").build();
        endpointManager.addServerEndpointConfig(config1);

        config2 = ServerEndpointConfig.Builder.create(EndpointURITest.class, "/a/{var}/c").build();
        endpointManager.addServerEndpointConfig(config2);

        config3 = ServerEndpointConfig.Builder.create(EndpointURITest.class, "/a/{var}/{var}").build();
        endpointManager.addServerEndpointConfig(config3);

        config4 = ServerEndpointConfig.Builder.create(EndpointURITest.class, "/a/b/c").build();
        endpointManager.addServerEndpointConfig(config4);

        config5 = ServerEndpointConfig.Builder.create(EndpointURITest.class, "/{var}/b/d").build();
        endpointManager.addServerEndpointConfig(config5);

        config6 = ServerEndpointConfig.Builder.create(EndpointURITest.class, "/{var}/{var}/{var}").build();
        endpointManager.addServerEndpointConfig(config6);

        config7 = ServerEndpointConfig.Builder.create(EndpointURITest.class, "/a/b/{var}").build();
        endpointManager.addServerEndpointConfig(config7);

        config8 = ServerEndpointConfig.Builder.create(EndpointURITest.class, "/{var}/b/{var}").build();
        endpointManager.addServerEndpointConfig(config8);
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
     * Test configuration values.
     */
    @Test
    public void testURIs() {
        //config4 which has endpoint uri as a/b/c is the exact match for incoming uri /a/b/c
        Assert.assertEquals(config4, endpointManager.getServerEndpointConfig("/a/b/c"));
        //config7 which has endpoint uri /a/b/{var} the best match for incoming uri /a/b/d
        Assert.assertEquals(config7, endpointManager.getServerEndpointConfig("/a/b/d"));
        //config8 which has endpoint uri /{var}/b/{var} is the best match for incoming uri /x/b/c
        Assert.assertEquals(config8, endpointManager.getServerEndpointConfig("/x/b/c"));
        //there is no endpoint URI match for /x/b. Hence return value is null
        Assert.assertNull(endpointManager.getServerEndpointConfig("/x/b"));
        //there is no endpoint URI match for /{var}/{var}/{var}/{var}. Hence return value is null
        Assert.assertNull(endpointManager.getServerEndpointConfig("/{var}/{var}/{var}/{var}"));

        //add a new endpoint uri /{var}/b/c and re-test best match for /x/b/c.. Now, it should config9 not config8 
        config9 = ServerEndpointConfig.Builder.create(EndpointURITest.class, "/{var}/b/c").build();
        endpointManager.addServerEndpointConfig(config9);
        Assert.assertEquals(config9, endpointManager.getServerEndpointConfig("/x/b/c"));
    }

}
