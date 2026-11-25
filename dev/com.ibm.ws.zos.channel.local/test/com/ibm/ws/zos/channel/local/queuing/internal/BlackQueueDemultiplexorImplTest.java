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
package com.ibm.ws.zos.channel.local.queuing.internal;

import java.io.IOException;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ibm.ws.zos.channel.local.LocalCommClientConnHandle;
import com.ibm.ws.zos.channel.local.queuing.BlackQueueDemultiplexor;
import com.ibm.ws.zos.channel.local.queuing.BlackQueueReadyCallback;
import com.ibm.ws.zos.channel.local.queuing.NativeWorkRequest;
import com.ibm.ws.zos.channel.local.queuing.NativeWorkRequestType;

/**
 * Unit tests.
 */
public class BlackQueueDemultiplexorImplTest {

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
     * Helper method for registering a dummy callback for DISCONNECT, to avoid
     * getting ConnectionClosedExceptions under registerCallback (which throws
     * the exception if a DISCONNECT callback is not registered for the given
     * lhdlPointer).
     *
     * @param blackQueueDemultiplexor
     * @param lhdlPointer
     */
    public static void registerDummyDisconnectCallback(BlackQueueDemultiplexor blackQueueDemultiplexor, LocalCommClientConnHandle handle) throws IOException {
        blackQueueDemultiplexor.registerCallback(new BlackQueueReadyCallback() {
            @Override
            public void blackQueueReady(NativeWorkRequest nativeWorkRequest) {
            }

            @Override
            public void cancel(Exception e) {
            }
        }, NativeWorkRequestType.REQUESTTYPE_DISCONNECT, handle);
    }

    /**
     * Verify the newConnectionCallback is called for CONNECT requests.
     */
    @Test
    public void testNewConnectionCallback() throws Exception {

        // Create the demultiplexor and register a newConnection callback.
        BlackQueueDemultiplexorImpl blackQueueDemultiplexor = new BlackQueueDemultiplexorImpl();
        final BlackQueueReadyCallback mockedCallback = mockery.mock(BlackQueueReadyCallback.class);
        blackQueueDemultiplexor.registerNewConnectionCallback(mockedCallback);

        final NativeWorkRequest mockedNativeWorkRequestConnect = mockery.mock(NativeWorkRequest.class);

        mockery.checking(new Expectations() {
            {
                // NativeWorkRequest.getRequestType is called by BlackQueueDemultiplexor.dispatch
                exactly(2).of(mockedNativeWorkRequestConnect).getRequestType();
                will(returnValue(NativeWorkRequestType.REQUESTTYPE_CONNECT));

                // Verify the callback is invoked.
                exactly(2).of(mockedCallback).blackQueueReady(mockedNativeWorkRequestConnect);
            }
        });

        // Dispatch two connects. Ensure that both are sent to the callback.
        blackQueueDemultiplexor.dispatch(mockedNativeWorkRequestConnect);
        blackQueueDemultiplexor.dispatch(mockedNativeWorkRequestConnect);
    }

    /**
     * Verify the newConnectionCallback is NOT called for non-CONNECT requests.
     */
    @Test(expected = CallbackNotFoundException.class)
    public void testNewConnectionCallbackNotCalledForNonConnect() throws Exception {

        // Create the demultiplexor and register a newConnection callback.
        BlackQueueDemultiplexorImpl blackQueueDemultiplexor = new BlackQueueDemultiplexorImpl();
        final BlackQueueReadyCallback mockedCallback = mockery.mock(BlackQueueReadyCallback.class);
        blackQueueDemultiplexor.registerNewConnectionCallback(mockedCallback);

        final NativeWorkRequest mockedNativeWorkRequest = mockery.mock(NativeWorkRequest.class);
        final LocalCommClientConnHandle handle1 = new LocalCommClientConnHandle(new byte[] { 0x00, 0x11, 0x22, 0x33 });

        mockery.checking(new Expectations() {
            {
                // NativeWorkRequest.getRequestType is called by BlackQueueDemultiplexor.dispatch
                allowing(mockedNativeWorkRequest).getRequestType();
                will(returnValue(NativeWorkRequestType.REQUESTTYPE_READREADY));

                // NativeWorkRequest.getClientConnectionHandle is called by BlackQueueDemultiplexor.dispatch
                oneOf(mockedNativeWorkRequest).getClientConnectionHandle();
                will(returnValue(handle1));

                // No expectations for the callback since it shouldn't be called.
            }
        });

        // Dispatch the request.  This should raise a CallbackNotFoundException, since we
        // didn't register one for this type of request and connection handle.
        blackQueueDemultiplexor.dispatch(mockedNativeWorkRequest);
    }

    /**
     * Verify the appropriate callback is called.
     */
    @Test
    public void testCallbacks() throws Exception {

        BlackQueueDemultiplexorImpl blackQueueDemultiplexor = new BlackQueueDemultiplexorImpl();

        // Register callback #1 (for DISCONNECT requests on LHDLPointer=1)
        final BlackQueueReadyCallback mockedCallback1 = mockery.mock(BlackQueueReadyCallback.class, "BlackQueueReadyCallback1");
        final LocalCommClientConnHandle handle1 = new LocalCommClientConnHandle(new byte[] { 0x00, 0x11, 0x22, 0x33 });
        final NativeWorkRequestType requestType1 = NativeWorkRequestType.REQUESTTYPE_DISCONNECT;
        blackQueueDemultiplexor.registerCallback(mockedCallback1, requestType1, handle1);

        // Register callback #2 (for READREADY requests on LHDLPointer=1)
        final BlackQueueReadyCallback mockedCallback2 = mockery.mock(BlackQueueReadyCallback.class, "BlackQueueReadyCallback2");
        final LocalCommClientConnHandle handle2 = handle1;
        final NativeWorkRequestType requestType2 = NativeWorkRequestType.REQUESTTYPE_READREADY;
        blackQueueDemultiplexor.registerCallback(mockedCallback2, requestType2, handle2);

        // Register callback #3 (for READ requests on LHDLPointer=3)
        final BlackQueueReadyCallback mockedCallback3 = mockery.mock(BlackQueueReadyCallback.class, "BlackQueueReadyCallback3");
        final LocalCommClientConnHandle handle3 = new LocalCommClientConnHandle(new byte[] { 0x01, 0x23, 0x45, 0x67 });
        final NativeWorkRequestType requestType3 = NativeWorkRequestType.REQUESTTYPE_READREADY;
        registerDummyDisconnectCallback(blackQueueDemultiplexor, handle3); // Need Disconnect dummy to avoid ConnectionClosedException
        blackQueueDemultiplexor.registerCallback(mockedCallback3, requestType3, handle3);

        final NativeWorkRequest mockedNativeWorkRequest1 = mockery.mock(NativeWorkRequest.class, "NativeWorkRequest1");
        final NativeWorkRequest mockedNativeWorkRequest2 = mockery.mock(NativeWorkRequest.class, "NativeWorkRequest2");
        final NativeWorkRequest mockedNativeWorkRequest3 = mockery.mock(NativeWorkRequest.class, "NativeWorkRequest3");

        mockery.checking(new Expectations() {
            {
                // Expectations for NativeWorkRequest #1.
                // NativeWorkRequest.getRequestType is called by BlackQueueDemultiplexor.dispatch
                allowing(mockedNativeWorkRequest1).getRequestType();
                will(returnValue(requestType1));

                // NativeWorkRequest.getClientConnectionHandle is called by BlackQueueDemultiplexor.dispatch
                oneOf(mockedNativeWorkRequest1).getClientConnectionHandle();
                will(returnValue(handle1));

                // The callback shall be called.  The parm is the associated NativeWorkRequest (#1).
                oneOf(mockedCallback1).blackQueueReady(mockedNativeWorkRequest1);

                // Expectations for NativeWorkRequest #2
                allowing(mockedNativeWorkRequest2).getRequestType();
                will(returnValue(requestType2));

                oneOf(mockedNativeWorkRequest2).getClientConnectionHandle();
                will(returnValue(handle2));

                // The callback shall be called.  The parm is the associated NativeWorkRequest (#2).
                oneOf(mockedCallback2).blackQueueReady(mockedNativeWorkRequest2);

                // Expectations for NativeWorkRequest #3
                allowing(mockedNativeWorkRequest3).getRequestType();
                will(returnValue(requestType3));

                oneOf(mockedNativeWorkRequest3).getClientConnectionHandle();
                will(returnValue(handle3));

                // The callback shall be called.  The parm is the associated NativeWorkRequest (#2).
                oneOf(mockedCallback3).blackQueueReady(mockedNativeWorkRequest3);
            }
        });

        // Dispatch the requests.
        blackQueueDemultiplexor.dispatch(mockedNativeWorkRequest1);
        blackQueueDemultiplexor.dispatch(mockedNativeWorkRequest2);
        blackQueueDemultiplexor.dispatch(mockedNativeWorkRequest3);
    }

    /**
     * Verify a CallbackNotFoundException is raised for non-existent callbacks.
     */
    @Test(expected = CallbackNotFoundException.class)
    public void testCallbackNotFound() throws Exception {

        BlackQueueDemultiplexorImpl blackQueueDemultiplexor = new BlackQueueDemultiplexorImpl();

        final NativeWorkRequest mockedNativeWorkRequest = mockery.mock(NativeWorkRequest.class);
        final LocalCommClientConnHandle handle1 = new LocalCommClientConnHandle(new byte[] { 0x00, 0x11, 0x22, 0x33 });

        mockery.checking(new Expectations() {
            {
                // NativeWorkRequest.getRequestType is called by BlackQueueDemultiplexor.dispatch
                allowing(mockedNativeWorkRequest).getRequestType();
                will(returnValue(NativeWorkRequestType.REQUESTTYPE_READREADY));

                // NativeWorkRequest.getClientConnectionHandle is called by BlackQueueDemultiplexor.dispatch
                oneOf(mockedNativeWorkRequest).getClientConnectionHandle();
                will(returnValue(handle1));
            }
        });

        // Dispatch the request.  This should raise a CallbackNotFoundException, since we
        // didn't register one for this type of request and connection handle.
        blackQueueDemultiplexor.dispatch(mockedNativeWorkRequest);
    }

    /**
     * Verify a CallbackNotFoundException is raised for non-existent newConnection callback.
     */
    @Test(expected = CallbackNotFoundException.class)
    public void testNewConnectionCallbackNotFound() throws Exception {

        BlackQueueDemultiplexorImpl blackQueueDemultiplexor = new BlackQueueDemultiplexorImpl();

        final NativeWorkRequest mockedNativeWorkRequest = mockery.mock(NativeWorkRequest.class);
        final LocalCommClientConnHandle handle1 = new LocalCommClientConnHandle(new byte[] { 0x00, 0x11, 0x22, 0x33 });

        mockery.checking(new Expectations() {
            {
                // NativeWorkRequest.getRequestType is called by BlackQueueDemultiplexor.dispatch
                allowing(mockedNativeWorkRequest).getRequestType();
                will(returnValue(NativeWorkRequestType.REQUESTTYPE_CONNECT));

                // NativeWorkRequest.getClientConnectionHandle is called by BlackQueueDemultiplexor.dispatch
                oneOf(mockedNativeWorkRequest).getClientConnectionHandle();
                will(returnValue(handle1));
            }
        });

        // Dispatch the request.  This should raise a CallbackNotFoundException, since we
        // didn't register a newConnection callback.
        blackQueueDemultiplexor.dispatch(mockedNativeWorkRequest);
    }

    /**
     * Verify the callback is removed from the map once it is dispatched.
     */
    @Test(expected = CallbackNotFoundException.class)
    public void testCallbackIsRemoved() throws Exception {

        BlackQueueDemultiplexorImpl blackQueueDemultiplexor = new BlackQueueDemultiplexorImpl();

        // Register callback #1 (for READ requests on LHDLPointer=1)
        final BlackQueueReadyCallback mockedCallback1 = mockery.mock(BlackQueueReadyCallback.class, "BlackQueueReadyCallback1");
        final LocalCommClientConnHandle handle1 = new LocalCommClientConnHandle(new byte[] { 0x00, 0x33, 0x66, 0x77 });
        final NativeWorkRequestType requestType1 = NativeWorkRequestType.REQUESTTYPE_READREADY;
        registerDummyDisconnectCallback(blackQueueDemultiplexor, handle1); // Need Disconnect dummy to avoid ConnectionClosedException
        blackQueueDemultiplexor.registerCallback(mockedCallback1, requestType1, handle1);

        final NativeWorkRequest mockedNativeWorkRequest1 = mockery.mock(NativeWorkRequest.class, "NativeWorkRequest1");

        mockery.checking(new Expectations() {
            {
                // Expectations for NativeWorkRequest #1.
                // NativeWorkRequest.getRequestType is called by BlackQueueDemultiplexor.dispatch
                allowing(mockedNativeWorkRequest1).getRequestType();
                will(returnValue(requestType1));

                // NativeWorkRequest.getClientConnectionHandle is called by BlackQueueDemultiplexor.dispatch
                allowing(mockedNativeWorkRequest1).getClientConnectionHandle();
                will(returnValue(handle1));

                // The callback shall be called.  The parm is the associated NativeWorkRequest (#1).
                oneOf(mockedCallback1).blackQueueReady(mockedNativeWorkRequest1);
            }
        });

        // Dispatch the requests.  The first one should work just fine and should satisfy
        // the Expectation that callback.blackQueueReady is called.  The second one should
        // raise a CallbackNotFoundException, since the callback should have been removed
        // by the first dispatch.
        blackQueueDemultiplexor.dispatch(mockedNativeWorkRequest1);
        blackQueueDemultiplexor.dispatch(mockedNativeWorkRequest1);
    }

    /**
     * Attempting to register a 2nd callback for the same requestType and lhdlPointer
     * should fail with a CallbackAlreadyRegisteredException.
     */
    @Test(expected = CallbackAlreadyRegisteredException.class)
    public void testCallbackAlreadyRegistered() throws Exception {

        BlackQueueDemultiplexorImpl blackQueueDemultiplexor = new BlackQueueDemultiplexorImpl();

        // Register callback #1 (for DISCONNECT requests on LHDLPointer=1)
        final BlackQueueReadyCallback mockedCallback1 = mockery.mock(BlackQueueReadyCallback.class, "BlackQueueReadyCallback1");
        final LocalCommClientConnHandle handle1 = new LocalCommClientConnHandle(new byte[] { 0x00, 0x22, 0x44, 0x66 });
        final NativeWorkRequestType requestType1 = NativeWorkRequestType.REQUESTTYPE_DISCONNECT;
        blackQueueDemultiplexor.registerCallback(mockedCallback1, requestType1, handle1);

        // Second registration for same requestType/lhdlPointer should fail.
        final BlackQueueReadyCallback mockedCallback2 = mockery.mock(BlackQueueReadyCallback.class, "BlackQueueReadyCallback2");
        blackQueueDemultiplexor.registerCallback(mockedCallback2, requestType1, handle1);
    }

    /**
     * Attempting to register a 2nd callback for the same requestType and lhdlPointer
     * normally fails with a CallbackAlreadyRegisteredException; however in this case
     * the exact same callback instance is being registered, so it should work fine.
     */
    @Test
    public void testRegisterSameCallback() throws Exception {

        BlackQueueDemultiplexorImpl blackQueueDemultiplexor = new BlackQueueDemultiplexorImpl();

        // Register callback #1 (for DISCONNECT requests on LHDLPointer=1)
        final BlackQueueReadyCallback mockedCallback1 = mockery.mock(BlackQueueReadyCallback.class, "BlackQueueReadyCallback1");
        final LocalCommClientConnHandle handle1 = new LocalCommClientConnHandle(new byte[] { 0x11, 0x22, 0x33, 0x44 });
        final NativeWorkRequestType requestType1 = NativeWorkRequestType.REQUESTTYPE_DISCONNECT;
        blackQueueDemultiplexor.registerCallback(mockedCallback1, requestType1, handle1);

        // Second registration for same requestType/lhdlPointer should be fine, since it
        // is the same exact callback object.
        blackQueueDemultiplexor.registerCallback(mockedCallback1, requestType1, handle1);

        final NativeWorkRequest mockedNativeWorkRequest1 = mockery.mock(NativeWorkRequest.class, "NativeWorkRequest1");

        mockery.checking(new Expectations() {
            {
                // Expectations for NativeWorkRequest #1.
                // NativeWorkRequest.getRequestType is called by BlackQueueDemultiplexor.dispatch
                allowing(mockedNativeWorkRequest1).getRequestType();
                will(returnValue(requestType1));

                // NativeWorkRequest.getClientConnectionHandle is called by BlackQueueDemultiplexor.dispatch
                allowing(mockedNativeWorkRequest1).getClientConnectionHandle();
                will(returnValue(handle1));

                // The callback shall be called.  The parm is the associated NativeWorkRequest (#1).
                oneOf(mockedCallback1).blackQueueReady(mockedNativeWorkRequest1);
            }
        });

        // Dispatch the request.
        blackQueueDemultiplexor.dispatch(mockedNativeWorkRequest1);
    }

    /**
     * Attempting to register a callback shall fail if there is not a DISCONNECT
     * callback registered for the given lhdlPointer. This is to ensure that callbacks
     * aren't registered for a connection that's already closing/closed.
     */
    @Test(expected = ConnectionClosedException.class)
    public void testRegisterCallbackFailsNoDisconnectCallback() throws Exception {

        BlackQueueDemultiplexorImpl blackQueueDemultiplexor = new BlackQueueDemultiplexorImpl();

        // Register callback #1 (for READREADY requests on LHDLPointer=1)
        final BlackQueueReadyCallback mockedCallback1 = mockery.mock(BlackQueueReadyCallback.class, "BlackQueueReadyCallback1");
        final LocalCommClientConnHandle handle1 = new LocalCommClientConnHandle(new byte[] { 0x00, 0x11, 0x22, 0x33 });
        final NativeWorkRequestType requestType1 = NativeWorkRequestType.REQUESTTYPE_READREADY;
        blackQueueDemultiplexor.registerCallback(mockedCallback1, requestType1, handle1);

        // Register should fail, since no DISCONNECT callback is registered.
        blackQueueDemultiplexor.registerCallback(mockedCallback1, requestType1, handle1);
    }

    /**
     * Verify the callbacks are all cancelled.
     */
    @Test
    public void testCancelCallbacks() throws Exception {

        BlackQueueDemultiplexorImpl blackQueueDemultiplexor = new BlackQueueDemultiplexorImpl();

        // Register callback #1 (for DISCONNECT requests on LHDLPointer=1)
        final BlackQueueReadyCallback mockedCallback1 = mockery.mock(BlackQueueReadyCallback.class, "BlackQueueReadyCallback1");
        final LocalCommClientConnHandle handle1 = new LocalCommClientConnHandle(new byte[] { 0x00, 0x11, 0x22, 0x33 });
        blackQueueDemultiplexor.registerCallback(mockedCallback1, NativeWorkRequestType.REQUESTTYPE_DISCONNECT, handle1);

        // Register callback #2 (for READREADY requests on LHDLPointer=1)
        final BlackQueueReadyCallback mockedCallback2 = mockery.mock(BlackQueueReadyCallback.class, "BlackQueueReadyCallback2");
        blackQueueDemultiplexor.registerCallback(mockedCallback2, NativeWorkRequestType.REQUESTTYPE_READREADY, handle1);

        final Exception cancelException = new Exception();

        mockery.checking(new Expectations() {
            {
                // The callbacks shall be cancelled.
                oneOf(mockedCallback1).cancel(cancelException);
                oneOf(mockedCallback2).cancel(cancelException);
            }
        });

        // Dispatch the requests.
        blackQueueDemultiplexor.cancelCallbacks(handle1, cancelException);
    }

    /**
     * Verify the callbacks for other connections are NOT cancelled when a
     * given connection's callbacks are cancelled.
     */
    @Test
    public void testDontCancelCallbacksForOtherConnections() throws Exception {

        BlackQueueDemultiplexorImpl blackQueueDemultiplexor = new BlackQueueDemultiplexorImpl();

        // Register callback #1 (for DISCONNECT requests on LHDLPointer=1)
        final BlackQueueReadyCallback mockedCallback1 = mockery.mock(BlackQueueReadyCallback.class, "BlackQueueReadyCallback1");
        final LocalCommClientConnHandle handle1 = new LocalCommClientConnHandle(new byte[] { 0x00, 0x11, 0x22, 0x33 });
        blackQueueDemultiplexor.registerCallback(mockedCallback1, NativeWorkRequestType.REQUESTTYPE_DISCONNECT, handle1);

        // Register callback #2 (for READREADY requests on LHDLPointer=1)
        final BlackQueueReadyCallback mockedCallback2 = mockery.mock(BlackQueueReadyCallback.class, "BlackQueueReadyCallback2");
        blackQueueDemultiplexor.registerCallback(mockedCallback2, NativeWorkRequestType.REQUESTTYPE_READREADY, handle1);

        final Exception cancelException = new Exception();
        final LocalCommClientConnHandle handle2 = new LocalCommClientConnHandle(new byte[] { 0x11, 0x22, 0x33, 0x44 }); // Connection to cancel.

        // Dispatch the requests.
        blackQueueDemultiplexor.cancelCallbacks(handle2, cancelException);
    }

    /**
     * Verify all 'Disconnect' callbacks are invoked.
     */
    @Test
    public void testDisconnectAll() throws Exception {

        BlackQueueDemultiplexorImpl blackQueueDemultiplexor = new BlackQueueDemultiplexorImpl();

        // Register callback #1 (for DISCONNECT requests on LHDLPointer=1)
        final BlackQueueReadyCallback mockedCallback1 = mockery.mock(BlackQueueReadyCallback.class, "BlackQueueReadyCallback1");
        final LocalCommClientConnHandle handle1 = new LocalCommClientConnHandle(new byte[] { 0x00, 0x11, 0x22, 0x33 });
        blackQueueDemultiplexor.registerCallback(mockedCallback1, NativeWorkRequestType.REQUESTTYPE_DISCONNECT, handle1);

        // Register callback #2 (for DISCONNECT requests on LHDLPointer=2)
        final BlackQueueReadyCallback mockedCallback2 = mockery.mock(BlackQueueReadyCallback.class, "BlackQueueReadyCallback2");
        final LocalCommClientConnHandle handle2 = new LocalCommClientConnHandle(new byte[] { 0x11, 0x22, 0x33, 0x44 });
        blackQueueDemultiplexor.registerCallback(mockedCallback2, NativeWorkRequestType.REQUESTTYPE_DISCONNECT, handle2);

        // Register callback #3 (for READREADY requests on LHDLPointer=1)
        // This callback should NOT be invoked.
        final BlackQueueReadyCallback mockedCallback3 = mockery.mock(BlackQueueReadyCallback.class, "BlackQueueReadyCallback3");
        blackQueueDemultiplexor.registerCallback(mockedCallback3, NativeWorkRequestType.REQUESTTYPE_READREADY, handle1);

        mockery.checking(new Expectations() {
            {
                // The 'Disconnect' callbacks shall all be invoked.
                oneOf(mockedCallback1).blackQueueReady(null);
                oneOf(mockedCallback2).blackQueueReady(null);
            }
        });

        // Do it.
        blackQueueDemultiplexor.disconnectAll();
    }

    /**
     * Verify the ffdcCallback is called for FFDC requests.
     */
    @Test
    public void testFFDCCallback() throws Exception {

        // Create the demultiplexor and register a ffdc callback.
        BlackQueueDemultiplexorImpl blackQueueDemultiplexor = new BlackQueueDemultiplexorImpl();
        final BlackQueueReadyCallback mockedCallback = mockery.mock(BlackQueueReadyCallback.class);
        blackQueueDemultiplexor.registerFfdcCallback(mockedCallback);

        final NativeWorkRequest mockedNativeWorkRequestFFDC = mockery.mock(NativeWorkRequest.class);

        mockery.checking(new Expectations() {
            {
                // NativeWorkRequest.getRequestType is called by BlackQueueDemultiplexor.dispatch
                allowing(mockedNativeWorkRequestFFDC).getRequestType();
                will(returnValue(NativeWorkRequestType.REQUESTTYPE_FFDC));

                // Verify the callback is invoked.
                exactly(2).of(mockedCallback).blackQueueReady(mockedNativeWorkRequestFFDC);
            }
        });

        // Dispatch two ffdc requests. Ensure that both are sent to the callback.
        blackQueueDemultiplexor.dispatch(mockedNativeWorkRequestFFDC);
        blackQueueDemultiplexor.dispatch(mockedNativeWorkRequestFFDC);
    }

    /**
     * Verify a CallbackNotFoundException is raised for non-existent ffdc callback.
     */
    @Test(expected = CallbackNotFoundException.class)
    public void testFFDCCallbackNotFound() throws Exception {

        BlackQueueDemultiplexorImpl blackQueueDemultiplexor = new BlackQueueDemultiplexorImpl();

        final NativeWorkRequest mockedNativeWorkRequest = mockery.mock(NativeWorkRequest.class);
        final LocalCommClientConnHandle handle1 = new LocalCommClientConnHandle(new byte[] { 0x00, 0x11, 0x22, 0x33 });

        mockery.checking(new Expectations() {
            {
                // NativeWorkRequest.getRequestType is called by BlackQueueDemultiplexor.dispatch
                allowing(mockedNativeWorkRequest).getRequestType();
                will(returnValue(NativeWorkRequestType.REQUESTTYPE_FFDC));

                // NativeWorkRequest.getClientConnectionHandle is called by BlackQueueDemultiplexor.dispatch
                oneOf(mockedNativeWorkRequest).getClientConnectionHandle();
                will(returnValue(handle1));
            }
        });

        // Dispatch the request.  This should raise a CallbackNotFoundException, since we
        // didn't register a newConnection callback.
        blackQueueDemultiplexor.dispatch(mockedNativeWorkRequest);
    }
}
