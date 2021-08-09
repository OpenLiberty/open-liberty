/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http.channel.test.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import test.common.SharedOutputManager;

import com.ibm.websphere.channelfw.ChannelData;
import com.ibm.ws.channelfw.internal.ChannelDataImpl;
import com.ibm.ws.channelfw.internal.ChannelFactoryDataImpl;
import com.ibm.ws.channelfw.internal.ChannelFrameworkImpl;
import com.ibm.ws.channelfw.internal.InboundVirtualConnectionFactoryImpl;
import com.ibm.ws.http.channel.internal.CallbackIDs;
import com.ibm.ws.http.channel.internal.HttpConfigConstants;
import com.ibm.ws.http.channel.internal.HttpInternalConstants;
import com.ibm.ws.http.channel.internal.HttpObjectFactory;
import com.ibm.ws.http.channel.internal.inbound.HttpInboundChannel;
import com.ibm.ws.http.channel.internal.inbound.HttpInboundChannelFactory;
import com.ibm.ws.http.channel.internal.inbound.HttpInboundLink;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.channelfw.ChannelFrameworkFactory;
import com.ibm.wsspi.channelfw.Discriminator;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.http.channel.HttpRequestMessage;
import com.ibm.wsspi.http.channel.inbound.HttpInboundServiceContext;
import com.ibm.wsspi.http.channel.values.MethodValues;
import com.ibm.wsspi.http.channel.values.VersionValues;
import com.ibm.wsspi.tcpchannel.TCPConnectionContext;

/**
 * Simple test class to check the discriminate method of the inbound HTTP
 * channel.
 */
public class DiscrimTest {
    private static SharedOutputManager outputMgr;

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
     * Test method.
     */
    @Test
    public void testMain() {
        try {
            ChannelFrameworkImpl chfw = (ChannelFrameworkImpl) ChannelFrameworkFactory.getChannelFramework();
            HttpObjectFactory hof = new HttpObjectFactory();
            Map<Object, Object> props = new HashMap<Object, Object>();
            HttpInboundChannelFactory factory = new HttpInboundChannelFactory();
            factory.init(new ChannelFactoryDataImpl(
                            HttpInboundChannelFactory.class,
                            new Class[] { TCPConnectionContext.class },
                            HttpInboundServiceContext.class));
            ChannelData data = new ChannelDataImpl("test", null, props, 0, chfw);
            HttpInboundChannel hic = new HttpInboundChannel(data, factory, hof);
            hic.start();
            VirtualConnection vc = new InboundVirtualConnectionFactoryImpl().createConnection();

            // ************************************************************
            // Test regular text discrim path, but return a MAYBE and go
            // back through discrimination with more information
            // ************************************************************

            // test discrim with partial HTTP data
            WsByteBuffer[] list = new WsByteBuffer[1];
            list[0] = ChannelFrameworkFactory.getBufferManager().allocateDirect(1024);
            list[0].put("GET /test".getBytes());
            assertTrue(Discriminator.MAYBE == hic.discriminate(vc, list));

            // test remaining data
            list[0].put("index.html HTTP/1.0\r\n\r\n".getBytes());
            assertTrue(Discriminator.YES == hic.discriminate(vc, list));

            HttpInboundLink link = (HttpInboundLink)
                            vc.getStateMap().get(CallbackIDs.CALLBACK_HTTPICL);
            HttpInboundServiceContext hsc = (HttpInboundServiceContext)
                            link.getChannelAccessor();
            HttpRequestMessage msg = hsc.getRequest();
            assertEquals(MethodValues.GET, msg.getMethodValue());
            assertEquals("/testindex.html", msg.getRequestURI());
            assertEquals(VersionValues.V10, msg.getVersionValue());

            // *************************************************************
            // Make sure we can re-use the objects for discrim after destroy
            // *************************************************************

            link.destroy(null);
            list[0].clear();
            list[0].put("GET / HTTP/1.1\r\n".getBytes());
            assertTrue(Discriminator.YES == hic.discriminate(vc, list));
            link.destroy(new Exception(""));

            // **************************************************************
            // Now try it with binary transport mode enabled
            // **************************************************************

            // test discrim with partial HTTP data

            props.put(HttpConfigConstants.PROPNAME_BINARY_TRANSPORT, "true");
            data = new ChannelDataImpl("test", null, props, 0, chfw);
            hic = new HttpInboundChannel(data, factory, new HttpObjectFactory());
            hic.start();
            vc = new InboundVirtualConnectionFactoryImpl().createConnection();
            list[0].clear();
            list[0].put(HttpInternalConstants.BINARY_TRANSPORT_V1);
            list[0].putInt(MethodValues.GET.getOrdinal());
            list[0].putInt(15);
            list[0].put("/test".getBytes());

            assertTrue(Discriminator.MAYBE == hic.discriminate(vc, list));

            // test remaining data
            list[0].put("index.html".getBytes());
            list[0].putInt(VersionValues.V10.getOrdinal());
            assertTrue(Discriminator.YES == hic.discriminate(vc, list));

            link = (HttpInboundLink) vc.getStateMap().get(CallbackIDs.CALLBACK_HTTPICL);
            hsc = (HttpInboundServiceContext) link.getChannelAccessor();
            msg = hsc.getRequest();
            assertEquals(MethodValues.GET, msg.getMethodValue());
            assertEquals("/testindex.html", msg.getRequestURI());
            assertEquals(VersionValues.V10, msg.getVersionValue());
            link.destroy(null);
        } catch (Throwable t) {
            outputMgr.failWithThrowable("testMain", t);
        }
    }

}