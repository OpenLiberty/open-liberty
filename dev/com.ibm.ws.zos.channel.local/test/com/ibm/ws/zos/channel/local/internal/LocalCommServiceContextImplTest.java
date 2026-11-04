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
package com.ibm.ws.zos.channel.local.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.TimeUnit;

import org.jmock.Expectations;
import org.junit.Test;

import com.ibm.ws.zos.channel.local.LocalCommClientConnHandle;
import com.ibm.ws.zos.channel.local.LocalCommReadCompletedCallback;
import com.ibm.ws.zos.channel.local.LocalCommServiceContext;
import com.ibm.ws.zos.channel.local.queuing.NativeWorkRequest;
import com.ibm.ws.zos.channel.local.queuing.NativeWorkRequestType;
import com.ibm.ws.zos.channel.local.queuing.internal.BlackQueueDemultiplexorImpl;
import com.ibm.ws.zos.channel.local.queuing.internal.BlackQueueDemultiplexorImplTest;
import com.ibm.ws.zos.channel.local.queuing.internal.LocalChannelProviderImpl;
import com.ibm.ws.zos.channel.local.queuing.internal.NativeRequestHandler;

/**
 * Unit tests for LocalCommServiceContextImpl.
 */
public class LocalCommServiceContextImplTest extends LocalCommTestCommon {

    /**
     * Test read() with data immediately available. The callback should be called in line.
     */
    @Test
    public void testReadWithDataAvailable() throws Exception {

        // Set up test environment:
        mockUpNativeSideStuff();

        // 4. Create the LocalCommChannel.
        // 5. Create the LocalCommConnLink.
        // 6. Create a mocked NativeWorkRequest and set it into the localCommConnLink.
        LocalCommChannel localCommChannel = new LocalCommChannel(null);
        LocalCommConnLink localCommConnLink = (LocalCommConnLink) localCommChannel.getConnectionLink(null);
        final NativeWorkRequest mockedNativeWorkRequest = mockery.mock(NativeWorkRequest.class);
        localCommConnLink.setConnectWorkRequest(mockedNativeWorkRequest);
        final LocalCommClientConnHandle localCommClientConnHandle = new LocalCommClientConnHandle(new byte[] { 0x00, 0x11, 0x22, 0x33 });

        // Now we're ready to run the test.
        final LocalCommServiceContext localCommServiceContext = (LocalCommServiceContext) localCommConnLink.getChannelAccessor();

        // Setup a mocked callback.  All we're interested in verifying is that
        // the callback gets called with the data we expect.
        final LocalCommReadCompletedCallback mockedCallback = mockery.mock(LocalCommReadCompletedCallback.class);
        final ByteBuffer dataReturnedFromRead = ByteBuffer.allocate(1);

        // Set up Expectations.
        mockery.checking(new Expectations() {
            {
                // NativeWorkRequest.getClientConnectionHandle is called multiple times, via LocalCommConnLink.getClientConnectionHandle()
                allowing(mockedNativeWorkRequest).getClientConnectionHandle();
                will(returnValue(localCommClientConnHandle));

                // NativeRequestHandler.read(lhdlPointer) is called to read the data.
                // Here it shall return data immediately.
                oneOf(mockedNativeRequestHandler).read(mockedNativeWorkRequest, false);
                will(returnValue(dataReturnedFromRead));

                // Since read() returned data immediately, the callback shall be invoked inline.
                oneOf(mockedCallback).ready(localCommServiceContext, dataReturnedFromRead);
            }
        });

        // Must register dummy disconnect callback or else registerCallback will fail.
        BlackQueueDemultiplexorImplTest.registerDummyDisconnectCallback(localChannelProvider.getBlackQueueDemultiplexor(), localCommClientConnHandle);

        // Run the test.
        localCommServiceContext.read(mockedCallback);
    }

    /**
     * Test read() with NO data available. The request goes async, meaning a callback
     * is registered with BlackQueueDemultiplexor. The test simulates the read complete NativeWorkRequest
     * and dispatches it thru the BlackQueueDemultiplexor, which calls back into LocalCommServiceContextImpl.read,
     * which issues the read again (this time data is available) and invokes the upstream callback.
     *
     * In other words:
     * ***Thread 1***
     * 1. LocalCommServiceContext.read(upstreamCallback)
     * 2. BlackQueueDemultiplexor.registerCallback(LocalCommServiceContext.read)
     * 3. NativeRequestHandler.read() // returns null, gone async
     * ***Thread 2***
     * 4. BlackQueueDemultiplexor.dispatch(NativeWorkReqeust read complete)
     * 5. LocalCommServiceContext.read(upstreamCallback)
     * 6. NativeRequestHandler.read() // returns data
     * 7. upstreamCallback.ready()
     */
    @Test
    public void testReadAsync() throws Exception {

        // Set up test environment:
        mockUpNativeSideStuff();

        // 4. Create the LocalCommChannel.
        // 5. Create the LocalCommConnLink.
        // 6. Create a mocked NativeWorkRequest and set it into the localCommConnLink.
        LocalCommChannel localCommChannel = new LocalCommChannel(null);
        LocalCommConnLink localCommConnLink = (LocalCommConnLink) localCommChannel.getConnectionLink(null);
        final NativeWorkRequest mockedNativeWorkRequestConnect = mockery.mock(NativeWorkRequest.class, "NativeWorkRequest.CONNECT");
        localCommConnLink.setConnectWorkRequest(mockedNativeWorkRequestConnect);
        final LocalCommClientConnHandle localCommClientConnHandle = new LocalCommClientConnHandle(new byte[] { 0x00, 0x11, 0x22, 0x33 });

        // Now we're ready to run the test.
        final LocalCommServiceContext localCommServiceContext = (LocalCommServiceContext) localCommConnLink.getChannelAccessor();

        // Setup a mocked callback.  All we're interested in verifying is that
        // the callback gets called with the data we expect.
        final LocalCommReadCompletedCallback mockedCallback = mockery.mock(LocalCommReadCompletedCallback.class);
        final ByteBuffer dataReturnedFromRead = ByteBuffer.allocate(1);

        // Set up Expectations for the async read.
        mockery.checking(new Expectations() {
            {
                // NativeWorkRequest.getClientConnectionHandle is called multiple times, via LocalCommConnLink.getClientConnectionHandle()
                allowing(mockedNativeWorkRequestConnect).getClientConnectionHandle();
                will(returnValue(localCommClientConnHandle));

                // NativeRequestHandler.read(lhdlPointer) is called to read the data.
                // Here it shall return null, meaning the request went async.
                oneOf(mockedNativeRequestHandler).read(mockedNativeWorkRequestConnect, false);
                will(returnValue(null));
            }
        });

        // Must register dummy disconnect callback or else registerCallback will fail.
        BlackQueueDemultiplexorImplTest.registerDummyDisconnectCallback(localChannelProvider.getBlackQueueDemultiplexor(), localCommClientConnHandle);

        // Run the first part of the test.
        localCommServiceContext.read(mockedCallback);

        // Setup mocked NativeWorkRequest for the read complete request.
        final NativeWorkRequest mockedNativeWorkRequestReadComplete = mockery.mock(NativeWorkRequest.class, "NativeWorkRequest.READCOMPLETE");

        // Set up Expectations for the read complete.
        mockery.checking(new Expectations() {
            {
                // NativeWorkRequest.getRequestType is called multiple times by BlackQueueDemultiplexor.dispatch
                allowing(mockedNativeWorkRequestReadComplete).getRequestType();
                will(returnValue(NativeWorkRequestType.REQUESTTYPE_READREADY));

                // NativeWorkRequest.getClientConnectionHandle is called once by BlackQueueDemultiplexor.dispatch
                oneOf(mockedNativeWorkRequestReadComplete).getClientConnectionHandle();
                will(returnValue(localCommClientConnHandle));

                // BlackQueueDemultiplexor.dispatch eventually calls into LocalCommServiceContext.read again.
                // NativeWorkRequest.getClientConnectionHandle is again called multiple times but we already have that covered
                // in the above Expectations group.

                // NativeRequestHandler.read(lhdlPointer) is again called to read the data.
                // Here it shall return the data.
                oneOf(mockedNativeRequestHandler).read(mockedNativeWorkRequestConnect, false);
                will(returnValue(dataReturnedFromRead));

                // And now the upstream callback shall be called
                oneOf(mockedCallback).ready(localCommServiceContext, dataReturnedFromRead);
            }
        });

        // Dispatch the read complete request.
        ((BlackQueueDemultiplexorImpl) localChannelProvider.getBlackQueueDemultiplexor()).dispatch(mockedNativeWorkRequestReadComplete);
    }

    /**
     * Test read() with failure. The callback.error should be called in line.
     */
    @Test
    public void testReadFailure() throws Exception {

        // Set up test environment:
        mockUpNativeSideStuff();

        // 4. Create the LocalCommChannel.
        // 5. Create the LocalCommConnLink.
        // 6. Create a mocked NativeWorkRequest and set it into the localCommConnLink.
        LocalCommChannel localCommChannel = new LocalCommChannel(null);
        LocalCommConnLink localCommConnLink = (LocalCommConnLink) localCommChannel.getConnectionLink(null);
        final NativeWorkRequest mockedNativeWorkRequest = mockery.mock(NativeWorkRequest.class);
        localCommConnLink.setConnectWorkRequest(mockedNativeWorkRequest);
        final LocalCommClientConnHandle handle = new LocalCommClientConnHandle(new byte[] { 0x01, 0x23, 0x45, 0x67 });

        // Now we're ready to run the test.
        final LocalCommServiceContext localCommServiceContext = (LocalCommServiceContext) localCommConnLink.getChannelAccessor();

        // Setup a mocked callback.  All we're interested in verifying is that
        // the callback gets called with the data we expect.
        final LocalCommReadCompletedCallback mockedCallback = mockery.mock(LocalCommReadCompletedCallback.class);
        final IOException raisedException = new IOException();

        // Set up Expectations.
        mockery.checking(new Expectations() {
            {
                // NativeWorkRequest.getClientConnectionHandle is called multiple times, via LocalCommConnLink.getClientConnectionHandle()
                allowing(mockedNativeWorkRequest).getClientConnectionHandle();
                will(returnValue(handle));

                // NativeRequestHandler.read(lhdlPointer) is called to read the data.
                // Here it shall raise an exception.
                oneOf(mockedNativeRequestHandler).read(mockedNativeWorkRequest, false);
                will(throwException(raisedException));

                // Since read() threw up, callback.error shall be invoked inline.
                oneOf(mockedCallback).error(localCommServiceContext, raisedException);
            }
        });

        // Must register dummy disconnect callback or else registerCallback will fail.
        BlackQueueDemultiplexorImplTest.registerDummyDisconnectCallback(localChannelProvider.getBlackQueueDemultiplexor(), handle);

        // Run the test.
        localCommServiceContext.read(mockedCallback);
    }

    /**
     * Test syncRead() with data immediately available. The async callback (under the covers)
     * should be called in line.
     */
    @Test
    public void testSyncReadWithDataAvailable() throws Exception {

        // Set up test environment:
        mockUpNativeSideStuff();

        // 4. Create the LocalCommChannel.
        // 5. Create the LocalCommConnLink.
        // 6. Create a mocked NativeWorkRequest and set it into the localCommConnLink.
        LocalCommChannel localCommChannel = new LocalCommChannel(null);
        LocalCommConnLink localCommConnLink = (LocalCommConnLink) localCommChannel.getConnectionLink(null);
        final NativeWorkRequest mockedNativeWorkRequest = mockery.mock(NativeWorkRequest.class);
        localCommConnLink.setConnectWorkRequest(mockedNativeWorkRequest);
        final LocalCommClientConnHandle handle = new LocalCommClientConnHandle(new byte[] { 0x01, 0x23, 0x45, 0x67 });

        // Now we're ready to run the test.
        final LocalCommServiceContext localCommServiceContext = (LocalCommServiceContext) localCommConnLink.getChannelAccessor();

        final ByteBuffer dataReturnedFromRead = ByteBuffer.allocate(1);

        // Set up Expectations.
        mockery.checking(new Expectations() {
            {
                // NativeWorkRequest.getClientConnectionHandle is called multiple times, via LocalCommConnLink.getClientConnectionHandle()
                allowing(mockedNativeWorkRequest).getClientConnectionHandle();
                will(returnValue(handle));

                // NativeRequestHandler.read(lhdlPointer) is called to read the data.
                // Here it shall return data immediately.
                oneOf(mockedNativeRequestHandler).read(mockedNativeWorkRequest, false);
                will(returnValue(dataReturnedFromRead));
            }
        });

        // Must register dummy disconnect callback or else registerCallback will fail.
        BlackQueueDemultiplexorImplTest.registerDummyDisconnectCallback(localChannelProvider.getBlackQueueDemultiplexor(), handle);

        // Run the test.
        assertSame(dataReturnedFromRead, localCommServiceContext.syncRead());
    }

    /**
     * Test syncRead() with NO data available. syncRead() blocks, while under the covers, the read
     * request goes async (same path as read()). Once the read completes, syncRead() wakes up and
     * returns the data.
     *
     * The flow is:
     * ***Thread 1***
     * 1. LocalCommServiceContext.syncRead()
     * 2. LocalCommServiceContext.read(SyncReadCompletedCallback)
     * 3. BlackQueueDemultiplexor.registerCallback(LocalCommServiceContext.read)
     * 4. NativeRequestHandler.read() // returns null, gone async
     * 5. SyncReadCompletedCallback.get() // blocks, waiting for read to complete.
     * ***Thread 2***
     * 6. BlackQueueDemultiplexor.dispatch(NativeWorkReqeust read complete)
     * 7. LocalCommServiceContext.read(SyncReadCompletedCallback)
     * 8. NativeRequestHandler.read() // returns data
     * 9. SyncReadCompletedCallback.ready()
     * ***Thread 1***
     * 10. SyncReadCompletedCallback.get() // returns data.
     *
     * Note: this test issues a blocking call; hence the timeout, in the event something
     * goes wrong.
     */
    @Test(timeout = 3000)
    public void testSyncReadAsync() throws Exception {

        // Set up test environment:
        mockUpNativeSideStuff();

        // 4. Create the LocalCommChannel.
        // 5. Create the LocalCommConnLink.
        // 6. Create a mocked NativeWorkRequest and set it into the localCommConnLink.
        LocalCommChannel localCommChannel = new LocalCommChannel(null);
        LocalCommConnLink localCommConnLink = (LocalCommConnLink) localCommChannel.getConnectionLink(null);
        final NativeWorkRequest mockedNativeWorkRequestConnect = mockery.mock(NativeWorkRequest.class, "NativeWorkRequest.CONNECT");
        localCommConnLink.setConnectWorkRequest(mockedNativeWorkRequestConnect);
        final LocalCommClientConnHandle handle = new LocalCommClientConnHandle(new byte[] { 0x01, 0x23, 0x45, 0x67 });

        // Now we're ready to run the test.
        final LocalCommServiceContext localCommServiceContext = (LocalCommServiceContext) localCommConnLink.getChannelAccessor();

        final ByteBuffer dataReturnedFromRead = ByteBuffer.allocate(1);

        // Set up Expectations for the async read.
        mockery.checking(new Expectations() {
            {
                // NativeWorkRequest.getClientConnectionHandle is called multiple times, via LocalCommConnLink.getClientConnectionHandle()
                allowing(mockedNativeWorkRequestConnect).getClientConnectionHandle();
                will(returnValue(handle));

                // NativeRequestHandler.read(lhdlPointer) is called to read the data.
                // Here it shall return null, meaning the request went async.
                oneOf(mockedNativeRequestHandler).read(mockedNativeWorkRequestConnect, false);
                will(returnValue(null));
            }
        });

        // Setup mocked NativeWorkRequest for the read complete request.
        final NativeWorkRequest mockedNativeWorkRequestReadComplete = mockery.mock(NativeWorkRequest.class, "NativeWorkRequest.READCOMPLETE");

        // Set up Expectations for the read complete.
        mockery.checking(new Expectations() {
            {
                // NativeWorkRequest.getRequestType is called multiple times by BlackQueueDemultiplexor.dispatch
                allowing(mockedNativeWorkRequestReadComplete).getRequestType();
                will(returnValue(NativeWorkRequestType.REQUESTTYPE_READREADY));

                // NativeWorkRequest.getClientConnectionHandle is called once by BlackQueueDemultiplexor.dispatch
                oneOf(mockedNativeWorkRequestReadComplete).getClientConnectionHandle();
                will(returnValue(handle));

                // BlackQueueDemultiplexor.dispatch eventually calls into LocalCommServiceContext.read again.
                // NativeWorkRequest.getClientConnectionHandle is again called multiple times but we already have that covered
                // in the above Expectations group.

                // NativeRequestHandler.read(lhdlPointer) is again called to read the data.
                // Here it shall return the data.
                oneOf(mockedNativeRequestHandler).read(mockedNativeWorkRequestConnect, false);
                will(returnValue(dataReturnedFromRead));
            }
        });

        // Create and start a 2nd thread to issue the read complete callback.
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(100); // Ensure the main thread has enough time to issue the async read and block.

                    // Dispatch the read complete request.
                    ((BlackQueueDemultiplexorImpl) localChannelProvider.getBlackQueueDemultiplexor()).dispatch(mockedNativeWorkRequestReadComplete);
                } catch (Throwable t) {
                    System.out.println("!!! Black Queue Request Dispatch FAILED: " + t);
                }
            }
        }).start();

        // Must register dummy disconnect callback or else registerCallback will fail.
        BlackQueueDemultiplexorImplTest.registerDummyDisconnectCallback(localChannelProvider.getBlackQueueDemultiplexor(), handle);

        // Run the test.  This call will block until the Thread we just started gets
        // control and issues the read complete.
        assertSame(dataReturnedFromRead, localCommServiceContext.syncRead());
    }

    /**
     * Test syncRead() with failure. Verify the IOException is raised.
     */
    @Test(expected = IOException.class)
    public void testSyncReadFailure() throws Exception {

        // Set up test environment:
        mockUpNativeSideStuff();

        // 4. Create the LocalCommChannel.
        // 5. Create the LocalCommConnLink.
        // 6. Create a mocked NativeWorkRequest and set it into the localCommConnLink.
        LocalCommChannel localCommChannel = new LocalCommChannel(null);
        LocalCommConnLink localCommConnLink = (LocalCommConnLink) localCommChannel.getConnectionLink(null);
        final NativeWorkRequest mockedNativeWorkRequest = mockery.mock(NativeWorkRequest.class);
        localCommConnLink.setConnectWorkRequest(mockedNativeWorkRequest);
        final LocalCommClientConnHandle handle = new LocalCommClientConnHandle(new byte[] { 0x01, 0x23, 0x45, 0x67 });

        // Now we're ready to run the test.
        final LocalCommServiceContext localCommServiceContext = (LocalCommServiceContext) localCommConnLink.getChannelAccessor();

        final IOException raisedException = new IOException();

        // Set up Expectations.
        mockery.checking(new Expectations() {
            {
                // NativeWorkRequest.getClientConnectionHandle is called multiple times, via LocalCommConnLink.getClientConnectionHandle()
                allowing(mockedNativeWorkRequest).getClientConnectionHandle();
                will(returnValue(handle));

                // NativeRequestHandler.read(lhdlPointer) is called to read the data.
                // Here it shall raise an exception.
                oneOf(mockedNativeRequestHandler).read(mockedNativeWorkRequest, false);
                will(throwException(raisedException));
            }
        });

        // Must register dummy disconnect callback or else registerCallback will fail.
        BlackQueueDemultiplexorImplTest.registerDummyDisconnectCallback(localChannelProvider.getBlackQueueDemultiplexor(), handle);

        // Run the test.
        localCommServiceContext.syncRead();
    }

    /**
     * Test syncWrite(). Not much to verify here...
     */
    @Test
    public void testSyncWrite() throws Exception {

        // Set up test environment:
        mockUpNativeSideStuff();

        // 4. Create the LocalCommChannel.
        // 5. Create the LocalCommConnLink.
        // 6. Create a mocked NativeWorkRequest and set it into the localCommConnLink.
        LocalCommChannel localCommChannel = new LocalCommChannel(null);
        LocalCommConnLink localCommConnLink = (LocalCommConnLink) localCommChannel.getConnectionLink(null);
        final NativeWorkRequest mockedNativeWorkRequest = mockery.mock(NativeWorkRequest.class);
        localCommConnLink.setConnectWorkRequest(mockedNativeWorkRequest);

        // Now we're ready to run the test.
        final LocalCommServiceContext localCommServiceContext = (LocalCommServiceContext) localCommConnLink.getChannelAccessor();

        final ByteBuffer dataToWrite = ByteBuffer.allocate(1);

        // Set up Expectations.
        mockery.checking(new Expectations() {
            {
                // NativeRequestHandler.write(lhdlPointer, data) is called to write the data.
                oneOf(mockedNativeRequestHandler).write(mockedNativeWorkRequest, dataToWrite);
            }
        });

        // Run the test.
        localCommServiceContext.syncWrite(dataToWrite);
    }

    /**
     * Test asyncRead() with NO data available. The request goes async, meaning a callback
     * is registered with BlackQueueDemultiplexor. The test simulates the read complete NativeWorkRequest
     * and dispatches it thru the BlackQueueDemultiplexor, which calls back into LocalCommServiceContextImpl.read,
     * which issues the read again (this time data is available) and invokes the upstream callback.
     *
     * In other words:
     * ***Thread 1***
     * 1. LocalCommServiceContext.asyncRead(upstreamCallback)
     * 2. BlackQueueDemultiplexor.registerCallback(LocalCommServiceContext.read)
     * 3. NativeRequestHandler.read() // returns null, gone async
     * ***Thread 2***
     * 4. BlackQueueDemultiplexor.dispatch(NativeWorkReqeust read complete)
     * 5. LocalCommServiceContext.read(upstreamCallback)
     * 6. NativeRequestHandler.read() // returns data
     * 7. upstreamCallback.ready()
     */
    @Test
    public void testAsyncReadAsync() throws Exception {

        // Set up test environment:
        mockUpNativeSideStuff();

        // 4. Create the LocalCommChannel.
        // 5. Create the LocalCommConnLink.
        // 6. Create a mocked NativeWorkRequest and set it into the localCommConnLink.
        LocalCommChannel localCommChannel = new LocalCommChannel(null);
        LocalCommConnLink localCommConnLink = (LocalCommConnLink) localCommChannel.getConnectionLink(null);
        final NativeWorkRequest mockedNativeWorkRequestConnect = mockery.mock(NativeWorkRequest.class, "NativeWorkRequest.CONNECT");
        localCommConnLink.setConnectWorkRequest(mockedNativeWorkRequestConnect);
        final LocalCommClientConnHandle handle = new LocalCommClientConnHandle(new byte[] { 0x01, 0x23, 0x45, 0x67 });

        // Now we're ready to run the test.
        final LocalCommServiceContext localCommServiceContext = (LocalCommServiceContext) localCommConnLink.getChannelAccessor();

        // Setup a mocked callback.  All we're interested in verifying is that
        // the callback gets called with the data we expect.
        final LocalCommReadCompletedCallback mockedCallback = mockery.mock(LocalCommReadCompletedCallback.class);
        final ByteBuffer dataReturnedFromRead = ByteBuffer.allocate(1);

        // Set up Expectations for the async read.
        mockery.checking(new Expectations() {
            {
                // NativeWorkRequest.getClientConnectionHandle is called multiple times, via LocalCommConnLink.getClientConnectionHandle()
                allowing(mockedNativeWorkRequestConnect).getClientConnectionHandle();
                will(returnValue(handle));

                // NativeRequestHandler.read(lhdlPointer) is called to read the data.
                // Here it shall return null, meaning the request went async.
                oneOf(mockedNativeRequestHandler).read(mockedNativeWorkRequestConnect, true);
                will(returnValue(null));
            }
        });

        // Must register dummy disconnect callback or else registerCallback will fail.
        BlackQueueDemultiplexorImplTest.registerDummyDisconnectCallback(localChannelProvider.getBlackQueueDemultiplexor(), handle);

        // Run the first part of the test.
        localCommServiceContext.asyncRead(mockedCallback);

        // Setup mocked NativeWorkRequest for the "read ready" request.
        final NativeWorkRequest mockedNativeWorkRequestReadComplete = mockery.mock(NativeWorkRequest.class, "NativeWorkRequest.READREADY");

        // Set up Expectations for the read ready.
        mockery.checking(new Expectations() {
            {
                // NativeWorkRequest.getRequestType is called by BlackQueueDemultiplexor.dispatch
                allowing(mockedNativeWorkRequestReadComplete).getRequestType();
                will(returnValue(NativeWorkRequestType.REQUESTTYPE_READREADY));

                // NativeWorkRequest.getClientConnectionHandle is called by BlackQueueDemultiplexor.dispatch
                allowing(mockedNativeWorkRequestReadComplete).getClientConnectionHandle();
                will(returnValue(handle));

                // BlackQueueDemultiplexor.dispatch eventually calls into LocalCommServiceContext.read again.
                // LocalCommConnLink.NativeWorkRequest.getClientConnectionHandle is again called multiple times but we
                // already have that covered in the above Expectations group.

                // NativeRequestHandler.read(lhdlPointer) is again called to read the data.
                // Here it shall return the data.
                oneOf(mockedNativeRequestHandler).read(mockedNativeWorkRequestConnect, false);
                will(returnValue(dataReturnedFromRead));

                // And now the upstream callback shall be called
                oneOf(mockedCallback).ready(localCommServiceContext, dataReturnedFromRead);
            }
        });

        // Dispatch the read complete request.
        ((BlackQueueDemultiplexorImpl) localChannelProvider.getBlackQueueDemultiplexor()).dispatch(mockedNativeWorkRequestReadComplete);
    }

    /**
     * Test asyncRead() with data available. The read request returns synchronously. This is an error - the native
     * layer should always force the request async and not return any data synchronously.
     *
     * In other words:
     * ***Thread 1***
     * 1. LocalCommServiceContext.asyncRead(upstreamCallback)
     * 2. BlackQueueDemultiplexor.registerCallback(LocalCommServiceContext.read)
     * 3. NativeRequestHandler.read() // returns data
     * 4. LocalCommConnLink.LocalCommChannel.getExecutorService().execute( new Runnable() { upstreamCallback.error(IOException) } )
     *
     */
    @Test
    public void testAsyncReadSync() throws Exception {

        // Set up test environment:
        mockUpNativeSideStuff();

        // 4. Create the LocalCommChannel.
        // 5. Create the LocalCommConnLink.
        // 6. Create a mocked NativeWorkRequest and set it into the localCommConnLink.
        LocalCommChannel localCommChannel = new LocalCommChannel(null);
        LocalCommConnLink localCommConnLink = (LocalCommConnLink) localCommChannel.getConnectionLink(null);
        final NativeWorkRequest mockedNativeWorkRequestConnect = mockery.mock(NativeWorkRequest.class, "NativeWorkRequest.CONNECT");
        localCommConnLink.setConnectWorkRequest(mockedNativeWorkRequestConnect);
        final LocalCommClientConnHandle handle = new LocalCommClientConnHandle(new byte[] { 0x01, 0x23, 0x45, 0x67 });

        // Now we're ready to run the test.
        final LocalCommServiceContext localCommServiceContext = (LocalCommServiceContext) localCommConnLink.getChannelAccessor();

        // Setup a mocked callback.  All we're interested in verifying is that
        // the callback gets called with the data we expect.
        final LocalCommReadCompletedCallback mockedCallback = mockery.mock(LocalCommReadCompletedCallback.class);
        final ByteBuffer dataReturnedFromRead = ByteBuffer.allocate(1);

        // Set up Expectations for the sync read.
        mockery.checking(new Expectations() {
            {
                // NativeWorkRequest.getClientConnectionHandle is called multiple times, via LocalCommConnLink.getClientConnectionHandle()
                allowing(mockedNativeWorkRequestConnect).getClientConnectionHandle();
                will(returnValue(handle));

                // NativeRequestHandler.read(lhdlPointer) is called to read the data.
                // Here it shall return the data, meaning the request completed synchronously.
                oneOf(mockedNativeRequestHandler).read(mockedNativeWorkRequestConnect, true);
                will(returnValue(dataReturnedFromRead));

                // Since the read returned synchronously, we schedule the async callback to
                // another task via the ExecutorService.  For testing purposes, the ExecutorService
                // invokes the runnable immediately, so we can verify that the callback is indeed
                // called.
                oneOf(mockedCallback).error(with(localCommServiceContext), with(any(IOException.class)));
            }
        });

        // Must register dummy disconnect callback or else registerCallback will fail.
        BlackQueueDemultiplexorImplTest.registerDummyDisconnectCallback(localChannelProvider.getBlackQueueDemultiplexor(), handle);

        // Run the test.
        localCommServiceContext.asyncRead(mockedCallback);
    }

    /**
     * Test that we can read the client connection handle.
     */
    @Test
    public void testGetClientConnectionHandle() throws Exception {

        // Set up test environment:
        mockUpNativeSideStuff();

        // 4. Create the LocalCommChannel.
        // 5. Create the LocalCommConnLink.
        // 6. Create a mocked NativeWorkRequest and set it into the localCommConnLink.
        LocalCommChannel localCommChannel = new LocalCommChannel(null);
        LocalCommConnLink localCommConnLink = (LocalCommConnLink) localCommChannel.getConnectionLink(null);
        final NativeWorkRequest mockedNativeWorkRequestConnect = mockery.mock(NativeWorkRequest.class, "NativeWorkRequest.CONNECT");
        localCommConnLink.setConnectWorkRequest(mockedNativeWorkRequestConnect);
        final LocalCommClientConnHandle handle = new LocalCommClientConnHandle(new byte[] { 0x01, 0x23, 0x45, 0x67 });

        // Now we're ready to run the test.
        final LocalCommServiceContext localCommServiceContext = (LocalCommServiceContext) localCommConnLink.getChannelAccessor();

        // Set up Expectations when we create the connection
        mockery.checking(new Expectations() {
            {
                // NativeWorkRequest.getClientConnectionHandle is called under the covers
                allowing(mockedNativeWorkRequestConnect).getClientConnectionHandle();
                will(returnValue(handle));
            }
        });

        // Run the test.
        assertEquals(handle, localCommServiceContext.getClientConnectionHandle());
    }
}

/**
 * Test extension for LocalChannelProviderImpl, to allow the test to set
 * a mocked NativeRequestHandler into the class.
 */
class TestLocalChannelProviderImpl extends LocalChannelProviderImpl {
    public TestLocalChannelProviderImpl setNativeRequestHandler(NativeRequestHandler nativeRequestHandler) {
        this.nativeRequestHandler = nativeRequestHandler;
        return this;
    }
}

/**
 * Test dummy. This guy is used to test LocalCommServiceContext.asyncRead in the
 * scenario where data is available and the read completes synchronously. asyncRead
 * will use the ExecutorService to invoke the upstream callback asynchronously.
 */
class TestExecutorService extends AbstractExecutorService {

    /**
     * Run the runnable inline (so the unit test can assert that it was executed).
     */
    @Override
    public void execute(Runnable command) {
        command.run();
    }

    @Override
    public void shutdown() {
    }

    @Override
    public List<Runnable> shutdownNow() {
        return null;
    }

    @Override
    public boolean isShutdown() {
        return false;
    }

    @Override
    public boolean isTerminated() {
        return false;
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return false;
    }
}
