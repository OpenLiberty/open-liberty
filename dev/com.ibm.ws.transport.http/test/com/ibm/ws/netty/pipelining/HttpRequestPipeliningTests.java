/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.netty.pipelining;

import java.beans.Transient;
import java.util.concurrent.atomic.AtomicInteger;

import com.ibm.ws.http.channel.internal.HttpChannelConfig;
import com.ibm.ws.http.netty.pipeline.inbound.LibertyHttpRequestHandler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;

import static org.junit.Assert.*;
import org.junit.Test;
import static org.mockito.Mockito.*;

public class HttpRequestPipeliningTests {

    private static final int DEFAULT_QUEUE_MAX = 50;

    private EmbeddedChannel channel;

    void teardown(){
        if(channel != null){
            try{
                channel.finishAndReleaseAll();
            }catch(Throwable t){
                //Ignore this as we are tearing down.
            }
        }
    }

    @Test 
    public void testUnlimitedRequestsDrainQueue(){
        LibertyHttpRequestHandler handler = newHandler(-1);
        DispatcherTesterHandler dispatcher = new DispatcherTesterHandler();
        channel = new EmbeddedChannel(handler, dispatcher);

        channel.writeInbound(newRequest("/request0"));
        for(int i=1; i <= DEFAULT_QUEUE_MAX; i++){
            channel.writeInbound(newRequest("/request"+i));
        }
        runPending();

        assertEquals("Only the first request should have been dispatched",1L, (long)dispatcher.requestsSeen.get());
        assertFalse("Auto read should be paused when queue is at max capacity",channel.config().isAutoRead());

        handler.processNextRequest();
        runPending();

        assertTrue("Auto read should resume after partial drain when not closing",channel.config().isAutoRead());
        assertTrue(channel.isOpen());
        teardown();

    }

    @Test 
    public void testOverflowQueueClosesConnection(){
        LibertyHttpRequestHandler handler = newHandler(-1);
        DispatcherTesterHandler dispatcher = new DispatcherTesterHandler();
        channel = new EmbeddedChannel(handler, dispatcher);

        channel.writeInbound(newRequest("/request0"));
        FullHttpRequest overflowOne = newRequest("/overflow1");
        FullHttpRequest overflowTwo = newRequest("/overflowTwo");

        for(int i = 1; i <= DEFAULT_QUEUE_MAX; i++){
            channel.writeInbound(newRequest("/request"+i));
        }
        runPending();

        //Overflow should now cause the handler to drain the queue and close
        channel.writeInbound(overflowOne);
        channel.writeInbound(overflowTwo);
        runPending();

        assertEquals(1L, (long)dispatcher.requestsSeen.get());
        assertEquals(0, overflowOne.refCnt());
        assertEquals(0, overflowTwo.refCnt());
        assertFalse("Auto-read should be paused while draining.", channel.config().isAutoRead());
        assertTrue("Channel should be open and draining.", channel.isOpen()); 

        for(int i=0; i<= DEFAULT_QUEUE_MAX; i++){
            handler.processNextRequest();
            runPending();
        }

        assertEquals("All requests must have been forwarded before close.", 1 +DEFAULT_QUEUE_MAX, dispatcher.requestsSeen.get());
        assertFalse("Channel should close after draining.", channel.isOpen());
        teardown();
    }

    @Test
    public void testKeepAliveMaxSet(){
        LibertyHttpRequestHandler handler = newHandler(3);
        DispatcherTesterHandler dispatcher = new DispatcherTesterHandler();
        channel = new EmbeddedChannel(handler, dispatcher);

        //Write more requests than the max keep alive
        FullHttpRequest request0 = newRequest("/request0");
        FullHttpRequest request1 = newRequest("/request1");
        FullHttpRequest request2 = newRequest("/request2");
        FullHttpRequest request3 = newRequest("/request3"); //Expected to be dropped
        FullHttpRequest request4 = newRequest("/request4"); //Expected to be dropped

        channel.writeInbound(request0);
        channel.writeInbound(request1);
        channel.writeInbound(request2);
        channel.writeInbound(request3);
        channel.writeInbound(request4);
        runPending();

        assertEquals(0, request3.refCnt());
        assertEquals(0, request4.refCnt());
        //Process the 3 accepted requests.
        handler.processNextRequest(); runPending();
        handler.processNextRequest(); runPending();
        handler.processNextRequest(); runPending();

        assertEquals(3, dispatcher.requestsSeen.get());
        assertFalse("Channel should be close after completing max open requests." , channel.isOpen());
        teardown();
    }

    @Test 
    public void testChannelInactiveReleasesQueue(){
        LibertyHttpRequestHandler handler = newHandler(-1);
        DispatcherTesterHandler dispatcher = new DispatcherTesterHandler();
        channel = new EmbeddedChannel(handler, dispatcher);

        FullHttpRequest request0 = newRequest("/request0");
        FullHttpRequest request1 = newRequest("/request1");
        channel.writeInbound(request0);
        channel.writeInbound(request1);
        runPending();

        //Instead of running processNextRequest, close channel to trigger inactivity
        channel.close();
        runPending();
        assertEquals(0, request1.refCnt());
        teardown();
    }

    private static class DispatcherTesterHandler extends SimpleChannelInboundHandler<FullHttpRequest>{
        final AtomicInteger requestsSeen = new AtomicInteger();
        DispatcherTesterHandler(){
            super(true);
        }

        @Override
        protected void channelRead0(ChannelHandlerContext context, FullHttpRequest message){
            requestsSeen.incrementAndGet();
        }
    }

    private static FullHttpRequest newRequest(String path){
        ByteBuf buf = Unpooled.buffer();
        buf.writeBytes(new byte[]{1,2,3,4,5,6,7,8,9});
        return new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, path, buf);
    }

    private static HttpChannelConfig configWithMaxRequests(int max){
        HttpChannelConfig config = mock(HttpChannelConfig.class);
        when(config.getMaximumPersistentRequests()).thenReturn(max);
        return config;
    }

    private LibertyHttpRequestHandler newHandler(int maxKeepAliveRequests){
        return new LibertyHttpRequestHandler(configWithMaxRequests(maxKeepAliveRequests));
    }

    private void runPending(){
        channel.runPendingTasks();
        channel.runScheduledPendingTasks();
    }

}
