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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import test.common.SharedOutputManager;

import com.ibm.websphere.channelfw.FlowType;
import com.ibm.ws.channelfw.internal.ChannelFrameworkImpl;
import com.ibm.ws.channelfw.testsuite.channels.connector.MainChannelFactory;
import com.ibm.ws.channelfw.testsuite.channels.protocol.ProtocolDummyFactory;
import com.ibm.wsspi.channelfw.ChannelFactory;
import com.ibm.wsspi.channelfw.exception.ChainException;
import com.ibm.wsspi.channelfw.exception.ChannelException;

/**
 * Test the channel factory life cycle methods
 */
public class ChannelFactoryLifeCycleTest {
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
     * Test non-persistent creation.
     */
    @Test
    public void testNonPersistCreation() {
        try {
            ChannelFrameworkImpl framework = new ChannelFrameworkImpl();
            try {
                ChannelFactory cf = framework.getChannelFactoryInternal(MainChannelFactory.class, false);
                assertNotNull(cf);
                // check to ensure this one isn't initialized
                assertEquals(MainChannelFactory.UNINIT, ((MainChannelFactory) cf).state);
                // second factory creation
                ChannelFactory cf2 = framework.getChannelFactoryInternal(MainChannelFactory.class, false);
                assertNotNull(cf2);
                // check to ensure this one isn't initialized
                assertEquals(MainChannelFactory.UNINIT, ((MainChannelFactory) cf2).state);
                // check to ensure first object was not persisted
                assertFalse(cf.equals(cf2));
            } catch (ChannelException e) {
                e.printStackTrace();
                fail("channel exception: " + e.getMessage());
            } finally {
                try {
                    framework.clear();
                } catch (Exception e) {
                    // nothing
                }
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable("testNonPersistCreation", t);
        }
    }

    /**
     * Test one chain lifecycle.
     */
    @Test
    public void testSingleLifecycle() {
        try {
            ChannelFrameworkImpl framework = new ChannelFrameworkImpl();
            try {
                framework.addChannel("main", MainChannelFactory.class, null);
                framework.addChannel("proto", ProtocolDummyFactory.class, null);
                framework.addChain("chain1", FlowType.INBOUND, new String[] { "main", "proto" });

                framework.initChain("chain1");
                ChannelFactory cf = framework.getChannelFactoryInternal(MainChannelFactory.class, false);
                assertNotNull(cf);
                assertEquals(MainChannelFactory.INIT, ((MainChannelFactory) cf).state);
                ChannelFactory cf2 = framework.getChannelFactoryInternal(MainChannelFactory.class, false);
                assertNotNull(cf2);
                assertEquals(MainChannelFactory.INIT, ((MainChannelFactory) cf2).state);
                assertEquals(cf, cf2);
                framework.destroyChain("chain1");
                assertEquals(MainChannelFactory.DEST, ((MainChannelFactory) cf).state);

                // check to ensure its not in the table
                cf2 = framework.getChannelFactoryInternal(MainChannelFactory.class, false);
                assertNotNull(cf2);
                assertEquals(MainChannelFactory.UNINIT, ((MainChannelFactory) cf2).state);
                // check to ensure first object was not persisted
                assertFalse(cf.equals(cf2));
            } catch (ChainException e) {
                e.printStackTrace();
                fail("chain exception: " + e.getMessage());
            } catch (ChannelException e) {
                e.printStackTrace();
                fail("channel exception: " + e.getMessage());
            } finally {
                try {
                    framework.clear();
                } catch (Exception e) {
                    // nothing
                }
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable("testSingleLifecycle", t);
        }
    }

    /**
     * Test two chain lifecycles.
     */
    @Test
    public void testDualLifecycle() {
        try {
            ChannelFrameworkImpl framework = new ChannelFrameworkImpl();
            try {
                framework.addChannel("main", MainChannelFactory.class, null);
                framework.addChannel("proto", ProtocolDummyFactory.class, null);
                framework.addChain("chain1", FlowType.INBOUND, new String[] { "main", "proto" });
                framework.initChain("chain1");
                framework.addChannel("main2", MainChannelFactory.class, null);
                framework.addChannel("proto2", ProtocolDummyFactory.class, null);
                framework.addChain("chain2", FlowType.INBOUND, new String[] { "main2", "proto2" });
                framework.initChain("chain2");

                ChannelFactory cf = framework.getChannelFactoryInternal(MainChannelFactory.class, false);
                assertNotNull(cf);
                assertEquals(MainChannelFactory.INIT, ((MainChannelFactory) cf).state);
                ChannelFactory cf2 = framework.getChannelFactoryInternal(MainChannelFactory.class, false);
                assertNotNull(cf2);
                assertEquals(MainChannelFactory.INIT, ((MainChannelFactory) cf2).state);
                assertEquals(cf, cf2);
                framework.destroyChain("chain1");

                // check to ensure this wasn't destroyed
                assertEquals(MainChannelFactory.INIT, ((MainChannelFactory) cf).state);
                // test destroy #2
                framework.destroyChain("chain2");

                // check to ensure this one isn't initialized
                assertEquals(MainChannelFactory.DEST, ((MainChannelFactory) cf).state);

                // check to ensure its not in the table
                cf2 = framework.getChannelFactoryInternal(MainChannelFactory.class, false);
                assertNotNull(cf2);
                assertEquals(MainChannelFactory.UNINIT, ((MainChannelFactory) cf2).state);
                // check to ensure first object was not persisted
                assertFalse(cf.equals(cf2));
            } catch (ChainException e) {
                e.printStackTrace();
                fail("chain exception: " + e.getMessage());
            } catch (ChannelException e) {
                e.printStackTrace();
                fail("channel exception: " + e.getMessage());
            } finally {
                try {
                    framework.clear();
                } catch (Exception e) {
                    // nothing
                }
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable("testDualLifeCycle", t);
        }
    }

}
