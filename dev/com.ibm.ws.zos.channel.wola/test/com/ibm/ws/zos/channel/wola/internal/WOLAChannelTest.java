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

import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.websphere.channelfw.ChannelData;
import com.ibm.ws.channelfw.internal.ChannelDataImpl;
import com.ibm.ws.channelfw.internal.ChannelFrameworkImpl;
import com.ibm.ws.zos.channel.local.LocalCommServiceContext;
import com.ibm.wsspi.channelfw.ChannelFrameworkFactory;

import test.common.SharedOutputManager;

/**
 * WOLA channel unit test.
 */
public class WOLAChannelTest {

    private static SharedOutputManager outputMgr;
    private static ChannelFrameworkImpl chfw;
    private static String MOCK_CHANNEL_NAME = "WOLA-MOCK-Channel";

    /**
     * Pre unit test processing.
     *
     * @throws Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // Capture stdout/stderr output to the manager.
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.captureStreams();

        // Get a reference to the channel framework.
        chfw = (ChannelFrameworkImpl) ChannelFrameworkFactory.getChannelFramework();
    }

    /**
     * Post unit test processing.
     *
     * @throws Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        // Final teardown work when class is exiting.
        outputMgr.restoreStreams();
    }

    /**
     * Individual teardown after each test.
     *
     * @throws Exception
     */
    @After
    public void tearDown() throws Exception {
        outputMgr.resetStreams();
    }

    /**
     * Test values.
     */
    @Test
    public void testBasics() {
        Map<Object, Object> props = new HashMap<Object, Object>();
        ChannelData data = new ChannelDataImpl(MOCK_CHANNEL_NAME, null, props, 0, chfw);
        WOLAChannel channel = new WOLAChannel(data);
        String name = channel.getName();
        assertTrue("The set channel name should have been: " + MOCK_CHANNEL_NAME + ". It was: " + name, name.equals(MOCK_CHANNEL_NAME));
        assertTrue("WOLA is currently the TOP of the chain. No app interface provided.", channel.getApplicationInterface() == null);
        assertTrue("WOLA is currently the top of the chain. No discriminatory type provided.", channel.getDiscriminatoryType() == null);
        assertTrue("The device interface should be the local comm's application interface: LocalCommServiceContext.class ",
                   (channel.getDeviceInterface() == LocalCommServiceContext.class));
        assertTrue("The channel's discriminator should not be null.", channel.getDiscriminator() != null);
        assertTrue("The channel's discriminator should be an instance of WOLADiscirimator.", channel.getDiscriminator() instanceof WOLADiscriminator);
    }
}
