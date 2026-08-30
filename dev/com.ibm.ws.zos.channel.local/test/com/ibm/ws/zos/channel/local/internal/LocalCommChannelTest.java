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

import com.ibm.websphere.channelfw.ChannelData;
import com.ibm.ws.zos.channel.local.queuing.NativeWorkRequest;
import com.ibm.wsspi.channelfw.ConnectionLink;
import com.ibm.wsspi.channelfw.VirtualConnection;

/**
 * Unit tests for LocalCommchannel.
 */
public class LocalCommChannelTest extends LocalCommTestCommon {

    /**
     * Override-able LocalCommChannel test class.
     */
    public static class TestLocalCommChannel extends LocalCommChannel {
        public TestLocalCommChannel(ChannelData inputConfig) {
            super(inputConfig);
        }
    }

    /**
     * Test new connection path
     */
    @Test
    public void testNewConnection() throws Exception {
        mockupLocalCommSideObjects();

        // Create an instance of TestLocalCommChannel and override getConnectionLink().
        TestLocalCommChannel testLCChannel = new TestLocalCommChannel(null) {

            @Override
            public ConnectionLink getConnectionLink(VirtualConnection vc) {
                if (vc == null) {
                    throw new IllegalStateException("Invalid Virtual Connection. It must not be NULL.");
                }
                return mockedLocalCommConnLink;
            }
        };

        // Set up Expectations for successful newConnection() call.
        NativeWorkRequest nativeWorkRequest = new NativeWorkRequest(null);
        mockery.checking(new Expectations() {
            {
                // After obtaining a virtual connection from channel framework a connectionLink by a call to
                // LocalCommChannel.getConnectionLink(). Make sure the method to set the native work request
                // on the connection link is called.
                allowing(mockedLocalCommConnLink).setConnectWorkRequest(with(any(NativeWorkRequest.class)));

                // Make sure ready() is called on the connection link.
                allowing(mockedLocalCommConnLink).ready(with(any(VirtualConnection.class)));
            }
        });

        // Call newConnection().
        testLCChannel.newConnection(nativeWorkRequest);

    }
}
