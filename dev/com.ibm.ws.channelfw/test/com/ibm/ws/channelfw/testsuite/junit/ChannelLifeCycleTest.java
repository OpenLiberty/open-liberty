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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.ibm.websphere.channelfw.FlowType;
import com.ibm.ws.channelfw.internal.ChannelFrameworkImpl;
import com.ibm.ws.channelfw.internal.RuntimeState;
import com.ibm.ws.channelfw.internal.chains.Chain;
import com.ibm.ws.channelfw.testsuite.channels.protocol.ProtocolDummyFactory;
import com.ibm.ws.channelfw.testsuite.channels.server.AppDummyFactory;
import com.ibm.ws.tcpchannel.internal.TCPChannelFactory;
import com.ibm.wsspi.channelfw.exception.ChainException;
import com.ibm.wsspi.channelfw.exception.ChannelException;
import com.ibm.wsspi.channelfw.exception.InvalidRuntimeStateException;

import test.common.SharedOutputManager;

/**
 * The purpose of this class is to test the channel configuration methods
 * in ChannelFrameworkImpl.
 */
public class ChannelLifeCycleTest {
    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    @Rule
    public TestRule managerRule = outputMgr;

    // Helper method to create valid tcp props to start a chain with TCP in it.
    protected Map<Object, Object> getTcpProps(String portProp, String defPort) {
        String port = System.getProperty(portProp, defPort);
        Map<Object, Object> tcpMap = new HashMap<Object, Object>();
        tcpMap.put("port", port);
        return tcpMap;
    }

    /**
     * Test initChannelInChain method.
     */
    @Test
    public void testInitChannelInChain() {
        try {
            ChannelFrameworkImpl framework = new ChannelFrameworkImpl();
            Chain chain = null;

            try {
                framework.addChannel("tcp", TCPChannelFactory.class, getTcpProps("ChannelLifeCycleTest.testInitChannelInChain", "7843"), 45);
                framework.addChannel("proto", ProtocolDummyFactory.class, null, 10);
                framework.addChain("chain1", FlowType.INBOUND, new String[] { "tcp" });
                framework.addChain("chain2", FlowType.INBOUND, new String[] { "tcp", "proto" });

                // Call initChain which calls initChannelInChain
                framework.initChain("chain1");
                chain = framework.getRunningChain("chain1");
                assertNotNull(chain);
                assertNotNull(framework.getRunningChannel("tcp", chain));
                assertEquals(RuntimeState.INITIALIZED, framework.getChannelState("tcp", chain));
                assertTrue(framework.doesChannelReferenceChain("tcp", "chain1"));

                // Move chain1 into started state.
                framework.startChain("chain1");
                framework.initChain("chain2");
                chain = framework.getRunningChain("chain2");
                assertNotNull(chain);
                assertNotNull(framework.getRunningChannel("tcp", chain));
                assertEquals(RuntimeState.STARTED, framework.getChannelState("tcp", chain));
                assertTrue(framework.doesChannelReferenceChain("tcp", "chain1"));
                assertTrue(framework.doesChannelReferenceChain("tcp", "chain2"));

                framework.destroyChain("chain2");
                framework.stopChain("chain1", 5000);
                chain = framework.getRunningChain("chain1");
                assertNotNull(chain);
                assertEquals(RuntimeState.QUIESCED, framework.getChannelState("tcp", chain));

                framework.stopChain("chain1", 0);
                chain = framework.getRunningChain("chain1");
                assertEquals(RuntimeState.INITIALIZED, framework.getChannelState("tcp", chain));

                chain = framework.getRunningChain("chain1");
                framework.destroyChain("chain1");
                assertNull(framework.getRunningChannel("tcp", chain));
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
            outputMgr.failWithThrowable("testInitChannelInChain", t);
        }
    }

    /**
     * Test startChannelInChain method.
     */
    @Test
    public void testStartChannelInChain() {
        try {
            ChannelFrameworkImpl framework = new ChannelFrameworkImpl();
            Chain chain = null;

            try {
                framework.addChannel("tcp", TCPChannelFactory.class, getTcpProps("ChannelLifeCycleTest.testStartChannelInChain", "7845"), 45);
                framework.addChannel("proto", ProtocolDummyFactory.class, null, 10);
                framework.addChain("chain1", FlowType.INBOUND, new String[] { "tcp" });
                framework.addChain("chain2", FlowType.INBOUND, new String[] { "tcp", "proto" });

                framework.initChain("chain1");
                framework.startChain("chain1");
                chain = framework.getRunningChain("chain1");
                assertNotNull(chain);
                assertNotNull(framework.getRunningChannel("tcp", chain));
                assertEquals(RuntimeState.STARTED, framework.getChannelState("tcp", chain));

                framework.initChain("chain2");
                framework.startChain("chain2");
                chain = framework.getRunningChain("chain2");
                assertNotNull(chain);
                assertNotNull(framework.getRunningChannel("tcp", chain));
                assertEquals(RuntimeState.STARTED, framework.getChannelState("tcp", chain));

                framework.stopChain("chain1", 0);
                framework.stopChain("chain2", 0);
                chain = framework.getRunningChain("chain1");
                assertEquals(RuntimeState.INITIALIZED, framework.getChannelState("tcp", chain));
                framework.startChain("chain1");
                assertEquals(RuntimeState.STARTED, framework.getChannelState("tcp", chain));

                framework.stopChain("chain1", 100);
                chain = framework.getRunningChain("chain1");
                assertEquals(RuntimeState.QUIESCED, framework.getChannelState("tcp", chain));
                framework.startChain("chain2");
                assertEquals(RuntimeState.STARTED, framework.getChannelState("tcp", chain));

                framework.stopChain("chain1", 0);
                chain = framework.getRunningChain("chain1");
                framework.destroyChain("chain1");
                assertNull(framework.getRunningChannel("tcp", chain));
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
            outputMgr.failWithThrowable("testStartChannelInChain", t);
        }
    }

    /**
     * Test stopChannelInChain method.
     */
    @Test
    public void testStopChannelInChain() {
        try {
            ChannelFrameworkImpl framework = new ChannelFrameworkImpl();
            Chain chain = null;

            try {
                framework.addChannel("tcp", TCPChannelFactory.class, getTcpProps("ChannelLifeCycleTest.testStopChannelInChain", "7846"), 45);
                framework.addChannel("proto", ProtocolDummyFactory.class, null, 10);
                framework.addChannel("app", AppDummyFactory.class, null, 10);
                framework.addChain("chain1", FlowType.INBOUND, new String[] { "tcp" });
                framework.addChain("chain2", FlowType.INBOUND, new String[] { "tcp", "proto", "app" });

                framework.initChain("chain1");
                framework.startChain("chain1");
                chain = framework.getRunningChain("chain1");
                assertEquals(RuntimeState.STARTED, framework.getChannelState("tcp", chain));
                framework.stopChain("chain1", 0);
                chain = framework.getRunningChain("chain1");
                assertNotNull(chain);
                assertNotNull(framework.getRunningChannel("tcp", chain));
                assertEquals(RuntimeState.INITIALIZED, framework.getChannelState("tcp", chain));

                framework.initChain("chain2");
                framework.startChain("chain2");
                framework.startChain("chain1");
                chain = framework.getRunningChain("chain2");
                assertEquals(RuntimeState.STARTED, framework.getChannelState("tcp", chain));
                framework.stopChain("chain1", 0);
                chain = framework.getRunningChain("chain1");
                assertEquals(RuntimeState.STARTED, framework.getChannelState("tcp", chain));

                framework.stopChain("chain2", 0);
                chain = framework.getRunningChain("chain2");
                assertEquals(RuntimeState.INITIALIZED, framework.getChannelState("tcp", chain));

                framework.startChain("chain2");
                framework.stopChain("chain2", 5000);
                chain = framework.getRunningChain("chain2");
                assertEquals(RuntimeState.QUIESCED, framework.getChannelState("tcp", chain));
                framework.stopChain("chain2", 0);
                assertEquals(RuntimeState.INITIALIZED, framework.getChannelState("tcp", chain));

                framework.destroyChain("chain2");
                chain = framework.getRunningChain("chain1");
                framework.destroyChain("chain1");
                assertNull(framework.getRunningChannel("tcp", chain));
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
            outputMgr.failWithThrowable("testStopChannelInChain", t);
        }
    }

    /**
     * Test quiesceChannelInChain method.
     */
    @Test
    public void testQuiesceChannelInChain() {
        try {
            ChannelFrameworkImpl framework = new ChannelFrameworkImpl();
            Chain chain = null;

            try {
                framework.addChannel("tcp", TCPChannelFactory.class, getTcpProps("ChannelLifeCycleTest.testQuiesceChannelInChain", "7848"), 45);
                framework.addChannel("proto", ProtocolDummyFactory.class, null, 10);
                framework.addChannel("app", AppDummyFactory.class, null, 10);
                framework.addChain("chain1", FlowType.INBOUND, new String[] { "tcp" });
                framework.addChain("chain2", FlowType.INBOUND, new String[] { "tcp", "proto", "app" });

                framework.initChain("chain1");
                framework.startChain("chain1");
                chain = framework.getRunningChain("chain1");
                assertEquals(RuntimeState.STARTED, framework.getChannelState("tcp", chain));
                framework.stopChain("chain1", 5000);
                chain = framework.getRunningChain("chain1");
                assertNotNull(framework.getRunningChannel("tcp", chain));
                assertEquals(RuntimeState.QUIESCED, framework.getChannelState("tcp", chain));

                framework.initChain("chain2");
                framework.startChain("chain2");
                framework.stopChain("chain1", 0);
                framework.startChain("chain1");
                chain = framework.getRunningChain("chain2");
                assertEquals(RuntimeState.STARTED, framework.getChannelState("tcp", chain));
                framework.stopChain("chain1", 5000);
                chain = framework.getRunningChain("chain1");
                assertEquals(RuntimeState.STARTED, framework.getChannelState("tcp", chain));

                framework.stopChain("chain2", 5000);
                chain = framework.getRunningChain("chain2");
                assertEquals(RuntimeState.QUIESCED, framework.getChannelState("tcp", chain));
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
            outputMgr.failWithThrowable("testQuiesceChannelInChain", t);
        }
    }

    /**
     * Test destroyChannelInChain method.
     */
    @Test
    public void testDestroyChannelInChain() {
        try {
            ChannelFrameworkImpl framework = new ChannelFrameworkImpl();
            Chain chain = null;

            try {
                framework.addChannel("tcp", TCPChannelFactory.class, getTcpProps("ChannelLifeCycleTest.testDestroyChannelInChain", "7849"), 45);
                framework.addChannel("proto", ProtocolDummyFactory.class, null, 10);
                framework.addChannel("app", AppDummyFactory.class, null, 10);
                framework.addChain("chain1", FlowType.INBOUND, new String[] { "tcp" });
                framework.addChain("chain2", FlowType.INBOUND, new String[] { "tcp", "proto", "app" });

                framework.initChain("chain1");
                chain = framework.getRunningChain("chain1");
                assertEquals(RuntimeState.INITIALIZED, framework.getChannelState("tcp", chain));
                chain = framework.getRunningChain("chain1");
                assertNotNull(framework.getRunningChannel("tcp", chain));
                framework.destroyChain("chain1");
                assertNull(framework.getChannelState("tcp", chain));

                framework.initChain("chain1");
                framework.initChain("chain2");
                framework.startChain("chain2");
                chain = framework.getRunningChain("chain2");
                assertEquals(RuntimeState.STARTED, framework.getChannelState("tcp", chain));
                chain = framework.getRunningChain("chain2");
                framework.destroyChain("chain1");
                assertEquals(RuntimeState.STARTED, framework.getChannelState("tcp", chain));
                assertFalse(framework.doesChannelReferenceChain("tcp", "chain1"));
                assertTrue(framework.doesChannelReferenceChain("tcp", "chain2"));

                framework.stopChain("chain2", 0);
                chain = framework.getRunningChain("chain2");
                framework.destroyChain("chain2");
                assertNull(framework.getChannelState("tcp", chain));

                framework.initChain("chain2");
                framework.startChain("chain2");
                chain = framework.getRunningChain("chain2");
                assertEquals(RuntimeState.STARTED, framework.getChannelState("tcp", chain));
                try {
                    framework.destroyChain("chain2");
                    fail("Should have thrown exception due to invalid chain state.");
                } catch (InvalidRuntimeStateException e) {
                    // expected failure
                }

                framework.stopChain("chain2", 5000);
                chain = framework.getRunningChain("chain2");
                assertEquals(RuntimeState.QUIESCED, framework.getChannelState("tcp", chain));
                try {
                    framework.destroyChain("chain2");
                    fail("Should have thrown exception due to invalid chain state.");
                } catch (InvalidRuntimeStateException e) {
                    // expected failure
                }
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
            outputMgr.failWithThrowable("testDestroyChannelInChain", t);
        }
    }
}
