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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.ibm.websphere.channelfw.ChainData;
import com.ibm.websphere.channelfw.FlowType;
import com.ibm.ws.channelfw.internal.ChainDataImpl;
import com.ibm.ws.channelfw.internal.ChainGroupDataImpl;
import com.ibm.ws.channelfw.internal.ChannelFrameworkImpl;
import com.ibm.ws.channelfw.testsuite.channels.protocol.ProtocolDummyFactory;
import com.ibm.ws.tcpchannel.internal.TCPChannelFactory;
import com.ibm.wsspi.channelfw.ChainEventListener;
import com.ibm.wsspi.channelfw.RetryableChainEventListener;
import com.ibm.wsspi.channelfw.exception.ChainException;
import com.ibm.wsspi.channelfw.exception.ChainGroupException;
import com.ibm.wsspi.channelfw.exception.ChannelException;
import com.ibm.wsspi.channelfw.exception.InvalidChainNameException;
import com.ibm.wsspi.channelfw.exception.RetryableChannelException;

import test.common.SharedOutputManager;

/**
 * Testcase for the chain lifecycle listener code.
 */
public class ChainEventListenerTest {
    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    @Rule
    public TestRule managerRule = outputMgr;

    static Field chainEventListeners;

    /**
     * @throws Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        chainEventListeners = ChainDataImpl.class.getDeclaredField("chainEventListeners");
        chainEventListeners.setAccessible(true);
    }

    // Helper method to create valid tcp props to start a chain with TCP in it.
    private Map<Object, Object> getTcpProps(String port, String defPort) {
        String sysPort = System.getProperty(port, defPort);
        Map<Object, Object> tcpMap = new HashMap<Object, Object>();
        tcpMap.put("port", sysPort);
        return tcpMap;
    }

    private int getNumChainEventListeners(ChainDataImpl cd) throws IllegalArgumentException, IllegalAccessException {
        Set<ChainEventListener> ceList = (Set<ChainEventListener>) chainEventListeners.get(cd);
        return ceList.size();
    }

    // ---------------------------------------
    // Chain Event Listener for testing
    // ---------------------------------------

    private class MyChainEventListener implements ChainEventListener, RetryableChainEventListener {
        protected int numInitEvents = 0;
        protected int numStartEvents = 0;
        protected int numStartFailedEvents = 0;
        protected int numStopEvents = 0;
        protected int numQuiesceEvents = 0;
        protected int numDestroyEvents = 0;
        protected int numUpdateEvents = 0;
        protected int attemptsMade = 0;
        protected int attemptsLeft = 0;
        protected ChainData data = null;

        protected MyChainEventListener() {
            // nothing
        }

        @Override
        public void chainInitialized(ChainData chainData) {
            this.data = chainData;
            this.numInitEvents++;
        }

        @Override
        public void chainStarted(ChainData chainData) {
            this.data = chainData;
            this.numStartEvents++;
        }

        @Override
        public void chainStartFailed(ChainData chainData, int _attemptsMade, int _attemptsLeft) {
            this.data = chainData;
            this.numStartFailedEvents++;
            this.attemptsMade = _attemptsMade;
            this.attemptsLeft = _attemptsLeft;
        }

        @Override
        public void chainStopped(ChainData chainData) {
            this.data = chainData;
            this.numStopEvents++;
        }

        @Override
        public void chainQuiesced(ChainData chainData) {
            this.data = chainData;
            this.numQuiesceEvents++;
        }

        @Override
        public void chainDestroyed(ChainData chainData) {
            this.data = chainData;
            this.numDestroyEvents++;
        }

        @Override
        public void chainUpdated(ChainData chainData) {
            this.data = chainData;
            this.numUpdateEvents++;
        }

        @Override
        public String toString() {
            return "My Chain Event Listener";
        }
    }

    // ---------------------------------------
    // Chain Config Methods
    // ---------------------------------------

    /**
     * Test addChainEventListener method.
     */
    //SplitStartUp @Test
    public void testAddChainEventListener() {
        try {
            MyChainEventListener cel = new MyChainEventListener();
            ChainDataImpl chainData1 = null;
            ChainDataImpl chainData2 = null;

            ChannelFrameworkImpl framework = new ChannelFrameworkImpl();
            try {
                framework.addChannel("tcp", TCPChannelFactory.class, getTcpProps("ChainEventListenerTest.testAddChainEventListener1", "14000"), 45);
                framework.addChannel("tcp2", TCPChannelFactory.class, getTcpProps("ChainEventListenerTest.testAddChainEventListener2", "14004"), 45);
                framework.addChannel("tcp-binderror", TCPChannelFactory.class, getTcpProps("ChainEventListenerTest.testAddChainEventListener1", "14000"), 45);
                framework.addChannel("proto", ProtocolDummyFactory.class, null, 10);
                framework.addChain("chain1", FlowType.INBOUND, new String[] { "tcp", "proto" });
                framework.addChain("chain2", FlowType.INBOUND, new String[] { "tcp2" });
                framework.addChain("chain-binderror", FlowType.INBOUND, new String[] { "tcp-binderror" });
                chainData1 = (ChainDataImpl) framework.getChain("chain1");
                chainData2 = (ChainDataImpl) framework.getChain("chain2");

                framework.addChainEventListener(null, "chain1");
                assertEquals(0, getNumChainEventListeners(chainData1));

                try {
                    framework.addChainEventListener(cel, null);
                    fail("Incorrectly allowed null listener");
                } catch (InvalidChainNameException e) {
                    // expected failure
                }

                try {
                    framework.addChainEventListener(cel, "unknown");
                    fail("Incorrectly allowed unknown chain");
                } catch (InvalidChainNameException e) {
                    // expected failure
                }

                framework.addChainEventListener(cel, "chain1");
                assertEquals(1, getNumChainEventListeners(chainData1));

                framework.initChain("chain1");
                assertEquals(1, cel.numInitEvents);
                assertEquals("chain1", cel.data.getName());
                assertEquals(FlowType.INBOUND, cel.data.getType());
                assertEquals(0, cel.numStartEvents);
                assertEquals(0, cel.numStopEvents);
                assertEquals(0, cel.numDestroyEvents);
                assertEquals(0, cel.numUpdateEvents);

                framework.startChain("chain1");
                assertEquals(1, cel.numInitEvents);
                assertEquals(1, cel.numStartEvents);
                assertEquals(0, cel.numStopEvents);
                assertEquals(0, cel.numDestroyEvents);
                assertEquals(0, cel.numUpdateEvents);

                framework.addChainEventListener(cel, "chain-binderror");
                try {
                    framework.initChain("chain-binderror");
                    fail("Should have thrown RetryableChannelException");
                } catch (RetryableChannelException e) {
                    assertEquals(1, cel.numInitEvents);
                    assertEquals(1, cel.numStartEvents);
                    assertEquals(0, cel.numStartFailedEvents);
                    assertEquals(0, cel.attemptsMade);
                    assertEquals(0, cel.attemptsLeft);
                    assertEquals(0, cel.numStopEvents);
                    assertEquals(0, cel.numDestroyEvents);
                    assertEquals(0, cel.numUpdateEvents);
                }
                // Clean up from test.
                framework.removeChainEventListener(cel, "chain-binderror");

                framework.stopChain("chain1", 1000);
                assertEquals(1, cel.numQuiesceEvents);
                // wait a little bit after the chain stop
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ie) {
                    // ignore
                }
                assertEquals(1, cel.numInitEvents);
                assertEquals(1, cel.numStartEvents);
                assertEquals(1, cel.numQuiesceEvents);
                assertEquals(1, cel.numStopEvents);
                assertEquals(0, cel.numDestroyEvents);
                assertEquals(0, cel.numUpdateEvents);

                framework.destroyChain("chain1");
                assertEquals(1, cel.numInitEvents);
                assertEquals(1, cel.numStartEvents);
                assertEquals(1, cel.numQuiesceEvents);
                assertEquals(1, cel.numStopEvents);
                assertEquals(1, cel.numDestroyEvents);
                assertEquals(0, cel.numUpdateEvents);

                framework.updateChain("chain1", new String[] { "tcp2" });
                assertEquals(1, cel.numInitEvents);
                assertEquals(1, cel.numStartEvents);
                assertEquals(1, cel.numQuiesceEvents);
                assertEquals(1, cel.numStopEvents);
                assertEquals(1, cel.numDestroyEvents);
                assertEquals(1, cel.numUpdateEvents);

                chainData1 = (ChainDataImpl) framework.getChain("chain1");
                framework.removeChainEventListener(cel, "chain1");
                cel = new MyChainEventListener();
                framework.addChainEventListener(cel, ChainEventListener.ALL_CHAINS);
                assertEquals(1, getNumChainEventListeners(chainData1));
                assertEquals(1, getNumChainEventListeners(chainData2));

                framework.initChain("chain1");
                framework.initChain("chain2");
                assertEquals(2, cel.numInitEvents);
                assertEquals("chain2", cel.data.getName());
                assertEquals(0, cel.numStartEvents);
                assertEquals(0, cel.numQuiesceEvents);
                assertEquals(0, cel.numStopEvents);
                assertEquals(0, cel.numDestroyEvents);
                assertEquals(0, cel.numUpdateEvents);

                framework.startChain("chain1");
                framework.startChain("chain2");
                assertEquals(2, cel.numInitEvents);
                assertEquals(2, cel.numStartEvents);
                assertEquals(0, cel.numQuiesceEvents);
                assertEquals(0, cel.numStopEvents);
                assertEquals(0, cel.numDestroyEvents);
                assertEquals(0, cel.numUpdateEvents);

                framework.stopChain("chain1", 0);
                framework.stopChain("chain2", 0);
                assertEquals(2, cel.numInitEvents);
                assertEquals(2, cel.numStartEvents);
                assertEquals(0, cel.numQuiesceEvents);
                assertEquals(2, cel.numStopEvents);
                assertEquals(0, cel.numDestroyEvents);
                assertEquals(0, cel.numUpdateEvents);

                framework.destroyChain("chain1");
                framework.destroyChain("chain2");
                assertEquals(2, cel.numInitEvents);
                assertEquals(2, cel.numStartEvents);
                assertEquals(0, cel.numQuiesceEvents);
                assertEquals(2, cel.numStopEvents);
                assertEquals(2, cel.numDestroyEvents);
                assertEquals(0, cel.numUpdateEvents);

                framework.updateChain("chain1", new String[] { "tcp", "proto" });
                framework.updateChain("chain2", new String[] { "tcp", "proto" });
                assertEquals(2, cel.numInitEvents);
                assertEquals(2, cel.numStartEvents);
                assertEquals(0, cel.numQuiesceEvents);
                assertEquals(2, cel.numStopEvents);
                assertEquals(2, cel.numDestroyEvents);
                assertEquals(2, cel.numUpdateEvents);

                framework.removeChain("chain2");
                framework.addChain("chain2", FlowType.INBOUND, new String[] { "tcp" });
                chainData2 = (ChainDataImpl) framework.getChain("chain2");
                assertEquals(1, getNumChainEventListeners(chainData2));
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
            outputMgr.failWithThrowable("testAddChainEventListener", t);
        }
    }

    /**
     * Test removeChainEventListener method.
     */
    @Test
    public void testRemoveChainEventListener() {
        try {
            MyChainEventListener cel = new MyChainEventListener();
            ChainDataImpl chainData1 = null;
            ChainDataImpl chainData2 = null;
            ChainDataImpl chainData3 = null;

            ChannelFrameworkImpl framework = new ChannelFrameworkImpl();
            try {
                framework.addChannel("tcp", TCPChannelFactory.class, getTcpProps("ChainEventListenerTest.testRemoveChainEventListener", "14001"), 45);
                framework.addChannel("proto", ProtocolDummyFactory.class, null, 10);
                framework.addChain("chain1", FlowType.INBOUND, new String[] { "tcp", "proto" });
                framework.addChain("chain2", FlowType.INBOUND, new String[] { "tcp" });
                chainData1 = (ChainDataImpl) framework.getChain("chain1");
                chainData2 = (ChainDataImpl) framework.getChain("chain2");
                framework.addChainEventListener(cel, "chain1");

                framework.removeChainEventListener(null, "chain1");
                assertEquals(1, getNumChainEventListeners(chainData1));

                try {
                    framework.removeChainEventListener(cel, null);
                    fail("Incorrectly allowed null chain");
                } catch (InvalidChainNameException e) {
                    // expected failure
                }

                try {
                    framework.removeChainEventListener(cel, "unknown");
                    fail("Incorrectly allowed unknown chain");
                } catch (InvalidChainNameException e) {
                    // expected failure
                }

                framework.removeChainEventListener(cel, "chain1");
                assertEquals(0, getNumChainEventListeners(chainData1));

                framework.initChain("chain1");
                framework.startChain("chain1");
                framework.stopChain("chain1", 0);
                framework.destroyChain("chain1");
                framework.updateChain("chain1", new String[] { "tcp" });
                assertEquals(0, cel.numInitEvents);
                assertEquals(0, cel.numStartEvents);
                assertEquals(0, cel.numQuiesceEvents);
                assertEquals(0, cel.numStopEvents);
                assertEquals(0, cel.numDestroyEvents);
                assertEquals(0, cel.numUpdateEvents);

                framework.addChainEventListener(cel, ChainEventListener.ALL_CHAINS);
                framework.removeChainEventListener(cel, ChainEventListener.ALL_CHAINS);
                assertEquals(0, getNumChainEventListeners(chainData1));
                assertEquals(0, getNumChainEventListeners(chainData2));

                framework.addChain("chain3", FlowType.INBOUND, new String[] { "tcp" });
                chainData3 = (ChainDataImpl) framework.getChain("chain3");
                assertEquals(0, getNumChainEventListeners(chainData3));

                framework.addChainEventListener(cel, ChainEventListener.ALL_CHAINS);
                try {
                    framework.removeChainEventListener(cel, "chain1");
                    fail("Global listener removal was invalid and should have thrown exception.");
                } catch (InvalidChainNameException e) {
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
            outputMgr.failWithThrowable("testRemoveChainEventListener", t);
        }
    }

    /**
     * Test addGroupEventListener method.
     */
    @Test
    public void testAddGroupEventListener() {
        try {
            MyChainEventListener cel = new MyChainEventListener();
            ChainDataImpl chainData1 = null;
            ChainDataImpl chainData2 = null;

            ChannelFrameworkImpl framework = new ChannelFrameworkImpl();
            try {
                framework.addChannel("tcp", TCPChannelFactory.class, getTcpProps("ChainEventListenerTest.testAddGroupEventListener", "14002"), 45);
                framework.addChannel("proto", ProtocolDummyFactory.class, null, 10);
                framework.addChain("chain1", FlowType.INBOUND, new String[] { "tcp", "proto" });
                framework.addChain("chain2", FlowType.INBOUND, new String[] { "tcp" });
                framework.addChainGroup("group1", new String[] { "chain1", "chain2" });
                chainData1 = (ChainDataImpl) framework.getChain("chain1");
                chainData2 = (ChainDataImpl) framework.getChain("chain2");

                framework.addGroupEventListener(null, "group1");
                assertEquals(0, getNumChainEventListeners(chainData1));
                assertEquals(0, getNumChainEventListeners(chainData2));

                try {
                    framework.addGroupEventListener(cel, null);
                    fail("Incorrectly allowed null chain");
                } catch (ChainGroupException e) {
                    // excepted failure
                }

                try {
                    framework.addGroupEventListener(cel, "unknown");
                    fail("Incorrectly allowed unknown chain");
                } catch (ChainGroupException e) {
                    // expected failure
                }

                framework.addGroupEventListener(cel, "group1");
                assertEquals(1, getNumChainEventListeners(chainData1));
                assertEquals(1, getNumChainEventListeners(chainData2));
            } catch (ChainGroupException e) {
                e.printStackTrace();
                fail("chaingroup exception: " + e.getMessage());
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
            outputMgr.failWithThrowable("testAddGroupEventListener", t);
        }
    }

    /**
     * Test removeGroupEventListener method.
     */
    @Test
    public void testRemoveGroupEventListener() {
        try {
            MyChainEventListener cel = new MyChainEventListener();
            ChainGroupDataImpl groupData = null;
            ChainGroupDataImpl groupData2 = null;
            ChainDataImpl chainData1 = null;
            ChainDataImpl chainData2 = null;

            ChannelFrameworkImpl framework = new ChannelFrameworkImpl();
            try {
                framework.addChannel("tcp", TCPChannelFactory.class, getTcpProps("ChainEventListenerTest.testRemoveGroupEventListener", "14003"), 45);
                framework.addChannel("proto", ProtocolDummyFactory.class, null, 10);
                framework.addChain("chain1", FlowType.INBOUND, new String[] { "tcp", "proto" });
                framework.addChain("chain2", FlowType.INBOUND, new String[] { "tcp" });
                groupData = (ChainGroupDataImpl) framework.addChainGroup("group1", new String[] { "chain1", "chain2" });
                groupData2 = (ChainGroupDataImpl) framework.addChainGroup("group2", new String[] { "chain1" });
                framework.addGroupEventListener(cel, "group1");
                chainData1 = (ChainDataImpl) framework.getChain("chain1");
                chainData2 = (ChainDataImpl) framework.getChain("chain2");

                framework.removeGroupEventListener(null, "group1");
                assertEquals(1, getNumChainEventListeners(chainData1));
                assertEquals(1, getNumChainEventListeners(chainData2));

                try {
                    framework.removeGroupEventListener(cel, null);
                    fail("Incorrectly allowed null chain");
                } catch (ChainGroupException e) {
                    // expected failure
                }

                try {
                    framework.removeGroupEventListener(cel, "unknown");
                    fail("Incorrectly allowed unknown chain");
                } catch (ChainGroupException e) {
                    // expected failure
                }

                assertTrue(groupData.containsChainEventListener(cel));
                assertEquals(1, getNumChainEventListeners(chainData1));
                assertEquals(1, getNumChainEventListeners(chainData2));
                framework.removeGroupEventListener(cel, "group1");
                assertEquals(0, getNumChainEventListeners(chainData1));
                assertEquals(0, getNumChainEventListeners(chainData2));

                framework.addGroupEventListener(cel, "group1");
                framework.addGroupEventListener(cel, "group2");
                assertTrue(groupData.containsChainEventListener(cel));
                assertTrue(groupData2.containsChainEventListener(cel));
                assertEquals(1, getNumChainEventListeners(chainData1));
                assertEquals(1, getNumChainEventListeners(chainData2));
                framework.removeGroupEventListener(cel, "group1");
                assertEquals(1, getNumChainEventListeners(chainData1));
                assertEquals(0, getNumChainEventListeners(chainData2));
            } catch (ChainGroupException e) {
                e.printStackTrace();
                fail("chaingroup exception: " + e.getMessage());
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
            outputMgr.failWithThrowable("testRemoveGroupEventListener", t);
        }
    }
}
