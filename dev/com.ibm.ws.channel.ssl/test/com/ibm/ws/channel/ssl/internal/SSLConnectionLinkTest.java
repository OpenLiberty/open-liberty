/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.channel.ssl.internal;

import static org.junit.Assert.fail;

import java.util.Collections;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Test;

import com.ibm.websphere.channelfw.ChannelData;
import com.ibm.ws.bytebuffer.internal.RefCountWsByteBufferImpl;
import com.ibm.ws.bytebuffer.internal.WsByteBufferImpl;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.channelfw.DiscriminationProcess;
import com.ibm.wsspi.tcpchannel.TCPReadRequestContext;

/**
 *
 */
public class SSLConnectionLinkTest {

    private final Mockery mockery = new Mockery();

    /**
     * WsByteBufferImpl's must only be released once. A customer found that during discriminator
     * failure scenarios, we end up releasing the same buffer twice, resulting in a RuntimeException.
     * This test verifies that the discriminator failure case will only close each buffer once;
     */
    @Test
    public void testDetermineNextChannel_DiscriminationFailure() throws Exception {
        final WsByteBuffer readInterfaceBuffer = new WsByteBufferImpl();
        final WsByteBuffer[] readInterfaceAllBuffers = new WsByteBuffer[] {
                                                                           new WsByteBufferImpl(),
                                                                           new WsByteBufferImpl(),
                                                                           readInterfaceBuffer,
                                                                           new WsByteBufferImpl(),
                                                                           new WsByteBufferImpl()
        };
        for (WsByteBuffer buf : readInterfaceAllBuffers) {
            RefCountWsByteBufferImpl rcwbbi = new RefCountWsByteBufferImpl();
            rcwbbi.intReferenceCount = 0;
            ((WsByteBufferImpl) buf).setWsBBRefRoot(rcwbbi);
        }
        final ChannelData mockChannelData = mockery.mock(ChannelData.class);

        final SSLConnectionLink link;
        final SSLReadServiceContext readServiceContext;
        final DiscriminationProcess mockDiscrimProcess;
        final TCPReadRequestContext mockDeviceReadInterface;

        mockery.checking(new Expectations() {
            {

                allowing(mockChannelData).getName();
                will(returnValue("myChannel"));
                allowing(mockChannelData).isInbound();
                will(returnValue(Boolean.TRUE));
                allowing(mockChannelData).getDiscriminatorWeight();
                will(returnValue(0));
                allowing(mockChannelData).getPropertyBag();
                will(returnValue(Collections.emptyMap()));

            }
        });

        final SSLChannel channel = new SSLChannel(mockChannelData);
        mockDiscrimProcess = mockery.mock(DiscriminationProcess.class);
        channel.setDiscriminationProcess(mockDiscrimProcess);
        link = new SSLConnectionLink(channel);
        readServiceContext = new SSLReadServiceContext();
        mockDeviceReadInterface = mockery.mock(TCPReadRequestContext.class);
        link.deviceReadInterface = mockDeviceReadInterface;
        readServiceContext.setNetBuffer(readInterfaceBuffer);
        readServiceContext.setBuffers(readInterfaceAllBuffers);
        link.initInterfaces(null, readServiceContext, null);

        mockery.checking(new Expectations() {
            {
                allowing(mockDiscrimProcess).discriminate(null, readInterfaceAllBuffers, link);
                will(returnValue(DiscriminationProcess.FAILURE));

                allowing(mockDeviceReadInterface).getBuffers();
                will(returnValue(readInterfaceAllBuffers));
            }
        });

        // this method will throw a RuntimeException if the WsByteBuffer has already been released
        try {
            link.determineNextChannel();
        } catch (Throwable t) {
            t.printStackTrace();
            fail("Caught unexpected exception in discriminator failure scenario of determineNextChannel()");
        }

        // Now ensure that if we release all of the byte buffers at this point, that they should all throw the
        // RuntimeException
        for (int i = 0; i < readInterfaceAllBuffers.length; i++) {
            try {
                readInterfaceAllBuffers[i].release();
                fail("Expected RuntimeException not thrown when releasing a buffer that should have already been released");
            } catch (RuntimeException ex) {
                // expected

            }
        }
        mockery.assertIsSatisfied();
    }
}
