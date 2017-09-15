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
package com.ibm.ws.channelfw.testsuite.junit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import test.common.SharedOutputManager;

import com.ibm.websphere.channelfw.ChannelFactoryData;
import com.ibm.ws.channelfw.internal.ChannelFrameworkImpl;
import com.ibm.ws.channelfw.testsuite.channels.protocol.ProtocolDummyFactory;
import com.ibm.ws.channelfw.testsuite.channels.server.AppDummyFactory;
import com.ibm.wsspi.channelfw.exception.ChannelException;
import com.ibm.wsspi.channelfw.exception.ChannelFactoryPropertyIgnoredException;
import com.ibm.wsspi.channelfw.exception.InvalidChannelFactoryException;

/**
 * Test code for the channel factory runtime configuration.
 */
public class ChannelFactoryDataTest {
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
     * Test the full updates.
     */
    @Test
    public void testUpdateChannelFactoryProperties() {
        try {
            int numFactories = 0;
            ChannelFactoryData factoryData = null;
            Map<Object, Object> factoryProperties = new HashMap<Object, Object>();
            factoryProperties.put("foo", "bar");

            ChannelFrameworkImpl framework = new ChannelFrameworkImpl();
            try {
                try {
                    framework.updateAllChannelFactoryProperties(null, factoryProperties);
                    fail("Incorrectly allowed null factory");
                } catch (InvalidChannelFactoryException e) {
                    // expected failure
                }

                numFactories = framework.getNumChannelFactories();
                framework.updateAllChannelFactoryProperties(ProtocolDummyFactory.class, null);
                factoryData = framework.getChannelFactory(ProtocolDummyFactory.class);
                assertEquals((numFactories + 1), framework.getNumChannelFactories());
                assertNotNull(factoryData);
                assertNull(factoryData.getProperties());

                numFactories = framework.getNumChannelFactories();
                framework.updateAllChannelFactoryProperties(ProtocolDummyFactory.class, null);
                factoryData = framework.getChannelFactory(ProtocolDummyFactory.class);
                assertEquals(numFactories, framework.getNumChannelFactories());
                assertNotNull(factoryData);
                assertNull(factoryData.getProperties());

                numFactories = framework.getNumChannelFactories();
                framework.updateAllChannelFactoryProperties(AppDummyFactory.class, factoryProperties);
                factoryData = framework.getChannelFactory(AppDummyFactory.class);
                assertTrue(numFactories != framework.getNumChannelFactories());
                assertNotNull(factoryData);
                assertNotNull(factoryData.getProperties());
                assertEquals(factoryProperties, factoryData.getProperties());

                numFactories = framework.getNumChannelFactories();
                framework.updateAllChannelFactoryProperties(ProtocolDummyFactory.class, factoryProperties);
                factoryData = framework.getChannelFactory(ProtocolDummyFactory.class);
                assertEquals(numFactories, framework.getNumChannelFactories());
                assertNotNull(factoryData);
                assertNotNull(factoryData.getProperties());
                assertEquals(factoryProperties, factoryData.getProperties());
            } catch (ChannelException e) {
                e.printStackTrace();
                fail();
            } finally {
                try {
                    framework.clear();
                } catch (Exception e) {
                    // nothing
                }
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable("testUpdateChannelFactoryProperties", t);
        }
    }

    /**
     * Test a single prop update.
     */
    @Test
    public void testUpdateChannelFactoryProperty() {
        try {
            String key = "key";
            String value = "value";

            ChannelFrameworkImpl framework = new ChannelFrameworkImpl();
            try {
                try {
                    framework.updateChannelFactoryProperty(null, key, value);
                    fail("Incorrectly allowed null factory class");
                } catch (InvalidChannelFactoryException e) {
                    // expected failure
                }

                try {
                    framework.updateChannelFactoryProperty(ProtocolDummyFactory.class, null, value);
                    fail("Incorrectly allowed unknown factory class");
                } catch (ChannelFactoryPropertyIgnoredException e) {
                    // expected failure
                }

                framework.updateChannelFactoryProperty(ProtocolDummyFactory.class, key, value);
                Map<Object, Object> properties =
                                framework.getChannelFactory(ProtocolDummyFactory.class).getProperties();
                assertNotNull(properties);
                assertEquals(1, properties.size());
                assertEquals(value, properties.get(key));

                framework.updateChannelFactoryProperty(ProtocolDummyFactory.class, key, null);
                properties = framework.getChannelFactory(ProtocolDummyFactory.class).getProperties();
                assertNotNull(properties);
                assertEquals(1, properties.size());
                assertNull(properties.get(key));
            } catch (ChannelException e) {
                e.printStackTrace();
                fail();
            } finally {
                try {
                    framework.clear();
                } catch (Exception e) {
                    // nothing
                }
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable("testUpdateChannelFactoryProperty", t);
        }
    }

    /**
     * Test the getter.
     */
    @Test
    public void testGetChannelFactory() {
        try {
            ChannelFactoryData factoryData = null;
            Class<?> factoryClass = ProtocolDummyFactory.class;

            ChannelFrameworkImpl framework = new ChannelFrameworkImpl();
            try {
                try {
                    framework.getChannelFactory(null);
                    fail("Incorrectly allowed null factory");
                } catch (InvalidChannelFactoryException e) {
                    // expected failure
                }

                factoryData = framework.getChannelFactory(factoryClass);
                assertNotNull(factoryData);
                assertEquals(factoryClass.getName(), factoryData.getFactory().getName());
            } catch (ChannelException e) {
                e.printStackTrace();
                fail();
            } finally {
                try {
                    framework.clear();
                } catch (Exception e) {
                    // nothing
                }
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable("testGetChannelFactory", t);
        }
    }

}
