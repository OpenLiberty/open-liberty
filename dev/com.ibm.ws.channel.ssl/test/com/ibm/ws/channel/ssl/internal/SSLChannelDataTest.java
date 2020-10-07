/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.channel.ssl.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.websphere.channelfw.ChannelData;
import com.ibm.websphere.channelfw.FlowType;
import com.ibm.wsspi.channelfw.exception.ChannelException;

import test.common.SharedOutputManager;

/**
 * Test channel config object.
 */
public class SSLChannelDataTest {
    private static SharedOutputManager outputMgr;
    private static Mockery mocker = new Mockery();
    protected static final ChannelData fakeData = mocker.mock(ChannelData.class);

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
     * Test configuration values.
     */
    @Test
    public void testConfig() {
        try {
            final Map<Object, Object> configMap = new HashMap<Object, Object>();
            configMap.put("encryptBuffersDirect", "true");
            configMap.put("decryptBuffersDirect", "true");
            configMap.put("SSLSessionCacheSize", "10");
            configMap.put("SSLSessionTimeout", "1000");
            configMap.put("testvalue", "false");
            mocker.checking(new Expectations() {
                {
                    allowing(fakeData).getName();
                    will(returnValue("testname"));
                    allowing(fakeData).getDiscriminatorWeight();
                    will(returnValue(Integer.valueOf(10)));
                    allowing(fakeData).isInbound();
                    will(returnValue(Boolean.TRUE));
                    allowing(fakeData).getPropertyBag();
                    will(returnValue(configMap));
                }
            });
            SSLChannelData config = new SSLChannelData(fakeData);
            assertTrue(config.isInbound());
            assertEquals("testname", config.getName());
            assertTrue(10 == config.getWeight());
            assertTrue(config.getDecryptBuffersDirect());
            assertTrue(config.getEncryptBuffersDirect());
            assertTrue(10 == config.getSSLSessionCacheSize());
            assertTrue(1 == config.getSSLSessionTimeout());
            assertEquals(FlowType.INBOUND, config.getFlowType());
            assertTrue(config.getBooleanProperty("encryptBuffersDirect"));
            assertFalse(config.getBooleanProperty("testvalue"));

            // test invalid config now
            configMap.clear();
            configMap.put("encryptBuffersDirect", "blue");
            mocker.checking(new Expectations() {
                {
                    allowing(fakeData).getName();
                    will(returnValue("testname2"));
                    allowing(fakeData).getDiscriminatorWeight();
                    will(returnValue(Integer.valueOf(10)));
                    allowing(fakeData).isInbound();
                    will(returnValue(Boolean.TRUE));
                    allowing(fakeData).getPropertyBag();
                    will(returnValue(configMap));
                }
            });
            try {
                new SSLChannelData(fakeData);
                Assert.fail();
            } catch (ChannelException ce) {
                // expected failure
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable("testConfig", t);
        }
    }
}
