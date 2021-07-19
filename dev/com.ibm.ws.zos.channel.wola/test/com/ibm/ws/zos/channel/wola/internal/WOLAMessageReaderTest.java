/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.channel.wola.internal;

import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ibm.ws.zos.channel.local.LocalCommReadCompletedCallback;
import com.ibm.ws.zos.channel.local.LocalCommServiceContext;
import com.ibm.ws.zos.channel.wola.internal.msg.WolaMessage;

/**
 * Unit tests.
 */
public class WOLAMessageReaderTest {

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
        // Needs to be ClassImposteriser in order to mock NativeRequestHandler and NativeWorkRequest.
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
     * Test the scenario where:
     * - WOLAMessageParser.parseMessage() returns a message
     * - WOLAMessageParser.getLeftovers() returns null
     *
     * parseAndDispatch shall issue an asyncRead and dispatch the message.
     */
    @Test
    public void testParseAndDispatchWithMessageNoLeftovers() throws Exception {

        final WolaConnLink mockedWOLAConnectionLink = mockery.mock(WolaConnLink.class);
        final LocalCommServiceContext mockedLocalCommServiceContext = mockery.mock(LocalCommServiceContext.class);
        final WOLAMessageParser mockedWOLAMessageParser = mockery.mock(WOLAMessageParser.class);

        mockery.checking(new Expectations() {
            {
                // parseAndDispatch calls WOLAMessageParser.parseMessage to parse the WOLA message.
                oneOf(mockedWOLAMessageParser).parseMessage();
                will(returnValue(new WolaMessage()));

                // parseAndDispatch calls WOLAMessageParser.getLeftovers to get a reference to any
                // leftover un-parsed data (an incomplete message).
                oneOf(mockedWOLAMessageParser).getLeftovers();
                will(returnValue(null));

                // Since parseMessage returned a message, we will dispatch that message on this thread.
                // parseAndDispatch forces an asyncRead to get the next message.
                oneOf(mockedLocalCommServiceContext).asyncRead(with(any(LocalCommReadCompletedCallback.class)));

                // Called under the dispatch method.
                oneOf(mockedLocalCommServiceContext).releaseDisconnectLock();
                oneOf(mockedLocalCommServiceContext).isDispatchThread();
                will(returnValue(true));

                // This is invoked by WOLARequestDispatcher.run, in order to get a ref to
                // LocalCommServiceContext and write the response.
                allowing(mockedWOLAConnectionLink).getDeviceLinkChannelAccessor();
                will(returnValue(mockedLocalCommServiceContext));

                // WOLARequestDispatcher writing the response.
                allowing(mockedLocalCommServiceContext).syncWrite(with(any(ByteBuffer.class)));
            }
        });

        new WOLAChannelFactoryProvider().activate();
        new WOLAMessageReader(mockedWOLAConnectionLink).parseAndDispatch(mockedLocalCommServiceContext, mockedWOLAMessageParser);
    }

    /**
     * Test the scenario where:
     * - WOLAMessageParser.parseMessage() returns a message
     * - WOLAMessageParser.getLeftovers() returns null
     * - Local Comm runs us on the listener thread
     *
     * parseAndDispatch shall issue an asyncRead and dispatch the message via the executor service.
     */
    @Test
    public void testParseAndDispatchWithMessageNoLeftoversNoDispatchThread() throws Exception {

        final WolaConnLink mockedWOLAConnectionLink = mockery.mock(WolaConnLink.class);
        final LocalCommServiceContext mockedLocalCommServiceContext = mockery.mock(LocalCommServiceContext.class);
        final WOLAMessageParser mockedWOLAMessageParser = mockery.mock(WOLAMessageParser.class);
        final ExecutorService mockedExecutorService = mockery.mock(ExecutorService.class);

        mockery.checking(new Expectations() {
            {
                // parseAndDispatch calls WOLAMessageParser.parseMessage to parse the WOLA message.
                oneOf(mockedWOLAMessageParser).parseMessage();
                will(returnValue(new WolaMessage()));

                // parseAndDispatch calls WOLAMessageParser.getLeftovers to get a reference to any
                // leftover un-parsed data (an incomplete message).
                oneOf(mockedWOLAMessageParser).getLeftovers();
                will(returnValue(null));

                // Since parseMessage returned a message, we will dispatch that message on this thread.
                // parseAndDispatch forces an asyncRead to get the next message.
                oneOf(mockedLocalCommServiceContext).asyncRead(with(any(LocalCommReadCompletedCallback.class)));

                // Called under the dispatch method.
                oneOf(mockedLocalCommServiceContext).releaseDisconnectLock();
                oneOf(mockedLocalCommServiceContext).isDispatchThread();
                will(returnValue(false));

                // Submit the work to the executor
                oneOf(mockedExecutorService).execute(with(any(Runnable.class)));
            }
        });

        new WOLAChannelFactoryProvider().activate();
        WOLAChannelFactoryProvider.getInstance().setExecutorService(mockedExecutorService);
        new WOLAMessageReader(mockedWOLAConnectionLink).parseAndDispatch(mockedLocalCommServiceContext, mockedWOLAMessageParser);
    }

    /**
     * Test the scenario where:
     * - WOLAMessageParser.parseMessage() returns null
     * - WOLAMessageParser.getLeftovers() returns null
     *
     * parseAndDispatch shall issue another read() for more data.
     */
    @Test
    public void testParseAndDispatchNoMessageNoLeftovers() throws Exception {

        final WolaConnLink mockedWOLAConnectionLink = mockery.mock(WolaConnLink.class);
        final LocalCommServiceContext mockedLocalCommServiceContext = mockery.mock(LocalCommServiceContext.class);
        final WOLAMessageParser mockedWOLAMessageParser = mockery.mock(WOLAMessageParser.class);

        mockery.checking(new Expectations() {
            {
                // parseAndDispatch calls WOLAMessageParser.parseMessage to parse the WOLA message.
                oneOf(mockedWOLAMessageParser).parseMessage();
                will(returnValue(null));

                // parseAndDispatch calls WOLAMessageParser.getLeftovers to get a reference to any
                // leftover un-parsed data (an incomplete message).
                oneOf(mockedWOLAMessageParser).getLeftovers();
                will(returnValue(null));

                // Since parseMessage returned null, we issue another read() to get the rest of the
                // message.
                oneOf(mockedLocalCommServiceContext).read(with(any(LocalCommReadCompletedCallback.class)));
            }
        });

        new WOLAMessageReader(mockedWOLAConnectionLink).parseAndDispatch(mockedLocalCommServiceContext, mockedWOLAMessageParser);
    }

    /**
     *
     */
    @Test
    public void testDispatchRequest() throws Exception {
        final WolaConnLink mockedWolaConnLink = mockery.mock(WolaConnLink.class);
        final LocalCommServiceContext mockedLocalCommServiceContext = mockery.mock(LocalCommServiceContext.class);

        // Setup WOLARequestDispatcher expectations, since we're dispatching a request.
        mockery.checking(new Expectations() {
            {
                // Called under the dispatch method.
                oneOf(mockedLocalCommServiceContext).releaseDisconnectLock();
                oneOf(mockedLocalCommServiceContext).isDispatchThread();
                will(returnValue(true));

                // This is invoked by WOLARequestDispatcher.run, in order to get a ref to
                // LocalCommServiceContext and write the response.
                allowing(mockedWolaConnLink).getDeviceLinkChannelAccessor();
                will(returnValue(mockedLocalCommServiceContext));

                // WOLARequestDispatcher writing the response.
                allowing(mockedLocalCommServiceContext).syncWrite(with(any(ByteBuffer.class)));
            }
        });

        new WOLAChannelFactoryProvider().activate();
        new WOLAMessageReader(mockedWolaConnLink).dispatch(new WolaMessage(), mockedLocalCommServiceContext, false);
    }

    /**
     *
     */
    @Test
    public void testDispatchResponse() throws Exception {
        final WolaConnLink mockedWolaConnLink = mockery.mock(WolaConnLink.class);
        final WolaOutboundRequestService mockedWolaOutboundRequestService = mockery.mock(WolaOutboundRequestService.class);
        final LocalCommServiceContext mockedLocalCommServiceContext = mockery.mock(LocalCommServiceContext.class);

        ByteBuffer rawData = ByteBuffer.allocate(WolaMessage.HeaderSize).putShort(WolaMessage.MessageTypeOffset, (short) WolaMessage.WOLA_MESSAGE_TYPE_RESPONSE);
        final WolaMessage response = new WolaMessage(new ByteBufferVector(rawData.array()));

        // Setup WolaOutboundRequestService expectations, since we're dispatching a response.
        mockery.checking(new Expectations() {
            {
                oneOf(mockedWolaConnLink).getOutboundRequestService();
                will(returnValue(mockedWolaOutboundRequestService));

                oneOf(mockedWolaOutboundRequestService).postResponse(response);

                oneOf(mockedLocalCommServiceContext).releaseDisconnectLock();
            }
        });

        new WOLAMessageReader(mockedWolaConnLink).dispatch(response, mockedLocalCommServiceContext, false);
    }
}
