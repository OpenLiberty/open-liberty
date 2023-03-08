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

import org.jmock.Expectations;
import org.junit.Test;

import com.ibm.ws.zos.channel.local.LocalCommClientConnHandle;
import com.ibm.ws.zos.channel.local.queuing.NativeWorkRequest;
import com.ibm.wsspi.channelfw.DiscriminationProcess;
import com.ibm.wsspi.channelfw.exception.DiscriminationProcessException;

/**
 * Unit tests for LocalCommConnLink.
 */
public class LocalCommConnLinkTest extends LocalCommTestCommon {

    /**
     * Tests a successful ready() call.
     *
     * @throws Exception
     */
    @Test
    public void testReadySuccess() throws Exception {
        // Mock up local comm objects.
        mockUpNativeSideStuff();
        mockupLocalCommSideObjects();

        // create a localCommChannel and set the needed data.
        LocalCommChannel localCommChannel = new LocalCommChannel(null);
        localCommChannel.setDiscriminationProcess(mockedDiscriminationProcess);

        // Get a localCommConnLink from the localCommChannel and set needed data.
        final LocalCommConnLink localCommConnLink = (LocalCommConnLink) localCommChannel.getConnectionLink(mockedVirtialConnection);
        localCommConnLink.setConnectWorkRequest(mockedNativeWorkRequest);
        localCommConnLink.setApplicationCallback(mockedAppCallback);

        final LocalCommClientConnHandle handle = new LocalCommClientConnHandle(new byte[] { 0x01, 0x23, 0x45, 0x67 });

        // Set up Expectations for successful ready() call.
        mockery.checking(new Expectations() {
            {
                // discriminate() is called on the discriminator process. It returns SUCCESS.
                allowing(mockedDiscriminationProcess).discriminate(with(mockedVirtialConnection), with(any(NativeWorkRequest.class)), with(localCommConnLink));
                will(returnValue(DiscriminationProcess.SUCCESS));

                // Discrimination succeeded. We should now be calling connectAccepted() where we register
                // a disconnect callback with the demultiplexor using the HDLPointer.
                allowing(mockedNativeWorkRequest).getClientConnectionHandle();
                will(returnValue(handle));

                // We should now be calling the native code through nativeConnectAccepted() to inform it that the connection was
                // successful.
                allowing(mockedNativeRequestHandler).nativeConnectAccepted(with(any(NativeWorkRequest.class)));

                // Last, we call ready() on the application callback (upstream channel callback).
                allowing(mockedAppCallback).ready(with(mockedVirtialConnection));
            }
        });

        // Call ready().
        localCommConnLink.ready(mockedVirtialConnection);
    }

    /**
     * Tests a failed ready() call where the call to discriminate throws an exception.
     *
     * @throws Exception
     */
    @Test
    public void testReadyWithExceptionOnDiscriminate() throws Exception {
        // Mockup local comm objects.
        mockUpNativeSideStuff();
        mockupLocalCommSideObjects();

        // create a localCommChannel and set the needed data.
        LocalCommChannel localCommChannel = new LocalCommChannel(null);
        localCommChannel.setDiscriminationProcess(mockedDiscriminationProcess);

        // Get a localCommConnLink from the localCommChannel and set neede data.
        final LocalCommConnLink localCommConnLink = (LocalCommConnLink) localCommChannel.getConnectionLink(mockedVirtialConnection);
        localCommConnLink.setConnectWorkRequest(mockedNativeWorkRequest);

        // Set up Expectations for a failed ready() call where the call to discriminate fails with an exception.
        mockery.checking(new Expectations() {
            {
                // Discriminate() is called on the discriminator process. Discriminate() should fail with a DiscriminationProcessException.
                allowing(mockedDiscriminationProcess).discriminate(with(mockedVirtialConnection), with(any(NativeWorkRequest.class)), with(localCommConnLink));
                will(throwException(new DiscriminationProcessException()));

                // Discrimination failed. We should be in the close path now. Get HDLPointer to call close on native request handler.
                allowing(mockedNativeWorkRequest).getClientConnectionHandle();

                // Close path: Call to the native request handler to dump native control blocks.
                allowing(mockedNativeRequestHandler).dumpNativeControlBlocks(with(equal(mockedNativeWorkRequest)));

                // Close path: Call to the native request handler to close the connection in native.
                allowing(mockedNativeRequestHandler).close(mockedNativeWorkRequest);

                // Close path: Get HDLPointer to cancel demultiplexor callbacks.
                allowing(mockedNativeWorkRequest).getClientConnectionHandle();

                // Close path: Detroy call on the virtual connection.
                allowing(mockedVirtialConnection).destroy();
            }
        });

        // Call ready().
        localCommConnLink.ready(mockedVirtialConnection);
    }
}
