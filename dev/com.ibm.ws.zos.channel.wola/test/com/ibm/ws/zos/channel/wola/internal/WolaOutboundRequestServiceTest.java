/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.channel.wola.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ibm.ws.zos.channel.local.LocalCommServiceContext;
import com.ibm.ws.zos.channel.wola.internal.msg.WolaMessage;

/**
 *
 */
public class WolaOutboundRequestServiceTest {

    /**
     * Mock environment.
     */
    private Mockery mockery = null;

    /**
     * Create the mockery environment for each test. Setting up a new mockery
     * environment for each test helps isolate Expectation sets, making it easier to
     * debug when some Expectation fails and all the Expectations are dumped
     * to the error log.
     */
    @Before
    public void before() {
        // Needs to be ClassImposteriser in order to mock WolaConnLink.
        mockery = new JUnit4Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
            }
        };
    }

    /**
     * There are alternative ways to do this.
     * 1) Use @RunWith(JMock.class) (deprecated)
     * 2) Declare the field: @Rule public final JUnitRuleMockery context = new JUnitRuleMockery();
     * (this version of Junit is not in our codebase).
     *
     * Doing it the manual way for now.
     */
    @After
    public void after() {
        mockery.assertIsSatisfied();
    }

    /**
     *
     */
    @Test
    public void testSendRequest() throws Exception {

        final WolaConnLink mockedWolaConnLink = mockery.mock(WolaConnLink.class);
        final LocalCommServiceContext mockedLocalCommServiceContext = mockery.mock(LocalCommServiceContext.class);

        // Setup expectations for sendRequest.
        mockery.checking(new Expectations() {
            {
                oneOf(mockedWolaConnLink).getDeviceLinkChannelAccessor();
                will(returnValue(mockedLocalCommServiceContext));

                // sendRequest writing the request.
                oneOf(mockedLocalCommServiceContext).syncWrite(with(any(ByteBuffer.class)));
            }
        });

        WolaMessage request = new WolaMessage();
        assertEquals(0, request.getRequestId());
        new WolaOutboundRequestService(mockedWolaConnLink).sendRequest(request);

        // The requestId is assigned by sendRequest.
        assertEquals(1, request.getRequestId());
    }

    /**
     *
     */
    @Test
    public void testSendRequestMultiple() throws Exception {

        final WolaConnLink mockedWolaConnLink = mockery.mock(WolaConnLink.class);
        final LocalCommServiceContext mockedLocalCommServiceContext = mockery.mock(LocalCommServiceContext.class);

        // Setup expectations for sendRequest.
        mockery.checking(new Expectations() {
            {
                allowing(mockedWolaConnLink).getDeviceLinkChannelAccessor();
                will(returnValue(mockedLocalCommServiceContext));

                // sendRequest writing the request.
                allowing(mockedLocalCommServiceContext).syncWrite(with(any(ByteBuffer.class)));
            }
        });

        WolaMessage request = new WolaMessage();
        assertEquals(0, request.getRequestId());

        WolaOutboundRequestService wolaOutboundRequestService = new WolaOutboundRequestService(mockedWolaConnLink);
        wolaOutboundRequestService.sendRequest(request);

        // The requestId is assigned by sendRequest.
        assertEquals(1, request.getRequestId());

        // 2nd request should get different requestId
        // (ignore the fact we're using the same WolaMessage object - it's irrelevant).
        wolaOutboundRequestService.sendRequest(request);
        assertEquals(2, request.getRequestId());
    }

    /**
     *
     */
    @Test
    public void testPostResponse() throws Exception {

        final WolaConnLink mockedWolaConnLink = mockery.mock(WolaConnLink.class);
        final LocalCommServiceContext mockedLocalCommServiceContext = mockery.mock(LocalCommServiceContext.class);

        // Setup expectations for sendRequest.
        mockery.checking(new Expectations() {
            {
                allowing(mockedWolaConnLink).getDeviceLinkChannelAccessor();
                will(returnValue(mockedLocalCommServiceContext));

                // sendRequest writing the request.
                allowing(mockedLocalCommServiceContext).syncWrite(with(any(ByteBuffer.class)));
            }
        });

        WolaMessage request1 = new WolaMessage();

        WolaOutboundRequestService wolaOutboundRequestService = new WolaOutboundRequestService(mockedWolaConnLink);
        Future<WolaMessage> future1 = wolaOutboundRequestService.sendRequest(request1);

        // The requestId is assigned by sendRequest.
        assertEquals(1, request1.getRequestId());

        WolaMessage request2 = new WolaMessage();
        Future<WolaMessage> future2 = wolaOutboundRequestService.sendRequest(request2);

        // The requestId is assigned by sendRequest.
        assertEquals(2, request2.getRequestId());

        // Test postResponse using the same request object (it already has the
        // requestId set and that's all that matters for this test).
        assertEquals(future1, wolaOutboundRequestService.postResponse(request1));
        assertEquals(future2, wolaOutboundRequestService.postResponse(request2));

        // Verify response futures are removed from the map
        assertNull(wolaOutboundRequestService.postResponse(request1));
        assertNull(wolaOutboundRequestService.postResponse(request2));

        // Test postresponse with a non-existent requestId.
        assertNull(wolaOutboundRequestService.postResponse(new WolaMessage().setRequestId(3)));
    }

    /**
     *
     */
    @Test
    public void testPostAll() throws Exception {

        final WolaConnLink mockedWolaConnLink = mockery.mock(WolaConnLink.class);
        final LocalCommServiceContext mockedLocalCommServiceContext = mockery.mock(LocalCommServiceContext.class);

        // Setup expectations for sendRequest.
        mockery.checking(new Expectations() {
            {
                allowing(mockedWolaConnLink).getDeviceLinkChannelAccessor();
                will(returnValue(mockedLocalCommServiceContext));

                // sendRequest writing the request.
                allowing(mockedLocalCommServiceContext).syncWrite(with(any(ByteBuffer.class)));
            }
        });

        WolaMessage request1 = new WolaMessage();
        WolaMessage request2 = new WolaMessage();

        WolaOutboundRequestService wolaOutboundRequestService = new WolaOutboundRequestService(mockedWolaConnLink);
        Future<WolaMessage> future1 = wolaOutboundRequestService.sendRequest(request1);
        Future<WolaMessage> future2 = wolaOutboundRequestService.sendRequest(request2);

        IOException ex = new IOException();
        wolaOutboundRequestService.postAll(ex);

        // Verify all response futures have been removed from the map
        assertNull(wolaOutboundRequestService.postResponse(request1));
        assertNull(wolaOutboundRequestService.postResponse(request2));

        Throwable initCause1 = null;
        Throwable initCause2 = null;

        // Calling get on the future should result in an ExecutionException
        // with the IOException as its cause
        try {
            future1.get();
        } catch (ExecutionException ee) {
            initCause1 = ee.getCause();
        }

        try {
            future2.get();
        } catch (ExecutionException ee) {
            initCause2 = ee.getCause();
        }

        assertEquals(initCause1, ex);
        assertEquals(initCause2, ex);
    }

    /**
     *
     */
    @Test(expected = IOException.class)
    public void testSendRequestWithException() throws Exception {

        final WolaConnLink mockedWolaConnLink = mockery.mock(WolaConnLink.class);
        final LocalCommServiceContext mockedLocalCommServiceContext = mockery.mock(LocalCommServiceContext.class);

        // Setup expectations for sendRequest.
        mockery.checking(new Expectations() {
            {
                allowing(mockedWolaConnLink).getDeviceLinkChannelAccessor();
                will(returnValue(mockedLocalCommServiceContext));

                // sendRequest writing the request.
                allowing(mockedLocalCommServiceContext).syncWrite(with(any(ByteBuffer.class)));
                will(throwException(new IOException()));
            }
        });

        WolaOutboundRequestService wolaOutboundRequestService = new WolaOutboundRequestService(mockedWolaConnLink);
        wolaOutboundRequestService.sendRequest(new WolaMessage()); // this should blow up
    }

}
