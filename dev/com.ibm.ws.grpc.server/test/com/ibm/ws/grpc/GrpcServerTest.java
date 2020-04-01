/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.grpc;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.After;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.rules.TestRule;

import com.ibm.ws.http2.Http2Consumers;

import io.grpc.Server;
import io.grpc.internal.ServerListener;
import io.grpc.internal.ServerTransport;
import io.grpc.internal.ServerTransportListener;
import test.common.SharedOutputManager;

/**
 * Basic unittests for grpcServer feature
 */
public class GrpcServerTest {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    @Rule
    public TestRule rule = outputMgr;

    @Rule
    public TestName name = new TestName();

    @After
    public void tearDown() {
    }
    
    @Test
    public void testActiveGrpcServers() {
    	LibertyGrpcServerBuilder serverBuilder = new LibertyGrpcServerBuilder();
    	Server server1 = serverBuilder.build();
    	Server server2 = serverBuilder.build();
    	Assert.assertEquals(0, ActiveGrpcServers.getServerList().size());
    	ActiveGrpcServers.addServer("server1", server1);
    	ActiveGrpcServers.addServer("server2", server2);
    	Assert.assertEquals(server1, ActiveGrpcServers.getServer("server1"));
    	Assert.assertEquals(server2, ActiveGrpcServers.getServer("server2"));
    	
    	ActiveGrpcServers.removeServer("server1");
    	Assert.assertNull(ActiveGrpcServers.getServer("server1"));
    	
    	ActiveGrpcServers.removeAllServers();
    	Assert.assertEquals(0, ActiveGrpcServers.getServerList().size());
    }
    
    @Test
    public void testGrpcConnectionHandlerSupportedContentTypes() {
    	GrpcConnectionHandler handler = new GrpcConnectionHandler(null);
		Set<String> expected = new HashSet<>(Arrays.asList("application/grpc"));
    	Assert.assertEquals(expected, handler.getSupportedContentTypes());
    }
    
    @Test
    public void testHttp2GrpcConsumers() {
		GrpcConnectionHandler handler1 = new GrpcConnectionHandler(null);
		GrpcConnectionHandler handler2 = new GrpcConnectionHandler(null);
		GrpcConnectionHandler handler3 = new GrpcConnectionHandler(null);
		Http2Consumers.addHandler(handler1);
		Http2Consumers.addHandler(handler2);
		Http2Consumers.addHandler(handler3);
		Assert.assertTrue(Http2Consumers.getHandlers().contains(handler1));
		Assert.assertTrue(Http2Consumers.getHandlers().contains(handler2));
		Assert.assertTrue(Http2Consumers.getHandlers().contains(handler3));

		Http2Consumers.removeHandler(handler1);
		Assert.assertFalse(Http2Consumers.getHandlers().contains(handler1));
		Http2Consumers.removeHandler(handler2);
		Assert.assertFalse(Http2Consumers.getHandlers().contains(handler2));
		Http2Consumers.removeHandler(handler3);
		Assert.assertNull(Http2Consumers.getHandlers());
    }
    
    @Test
    public void testMultipleServerStart() throws IOException {
    	LibertyGrpcServer server1 = new LibertyGrpcServer();
    	LibertyGrpcServer server2 = new LibertyGrpcServer();
    	LibertyGrpcServer server3 = new LibertyGrpcServer();
    	TestServerListener listener = new TestServerListener();
    	server1.start(listener);
    	server2.start(listener);
    	server3.start(listener);
		Assert.assertEquals(3, Http2Consumers.getHandlers().size());
		
		server1.shutdown();
		server2.shutdown();
		Assert.assertEquals(1, Http2Consumers.getHandlers().size());
		
		server1.start(listener);
		Assert.assertEquals(2, Http2Consumers.getHandlers().size());
    }
    
    private final class TestServerListener implements ServerListener {

		@Override
		public ServerTransportListener transportCreated(ServerTransport transport) {
			return null;
		}

		@Override
		public void serverShutdown() {
		}
    }
}
