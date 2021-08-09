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
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.ibm.websphere.channelfw.ChainData;
import com.ibm.websphere.channelfw.FlowType;
import com.ibm.ws.channelfw.internal.ChannelDataImpl;
import com.ibm.ws.channelfw.internal.ChannelFrameworkImpl;
import com.ibm.ws.channelfw.internal.OutboundVirtualConnectionFactoryImpl;
import com.ibm.ws.channelfw.internal.RuntimeState;
import com.ibm.ws.channelfw.internal.chains.Chain;
import com.ibm.ws.channelfw.testsuite.chaincoherency.ConnectorChannelFactory;
import com.ibm.ws.channelfw.testsuite.chaincoherency.IntAddrInStrAddrOutFactory;
import com.ibm.ws.channelfw.testsuite.chaincoherency.IntDiscInIntDiscOutFactory;
import com.ibm.ws.channelfw.testsuite.chaincoherency.IntDiscInStrDiscOutFactory;
import com.ibm.ws.channelfw.testsuite.chaincoherency.StrAddrInIntAddrOutFactory;
import com.ibm.ws.channelfw.testsuite.chaincoherency.StrDiscInIntDiscOutFactory;
import com.ibm.ws.channelfw.testsuite.chaincoherency.StrDiscInStrDiscOutFactory;
import com.ibm.ws.channelfw.testsuite.chaincoherency.TcpAddrInBoolIntAddrOutFactory;
import com.ibm.ws.channelfw.testsuite.chaincoherency.TcpAddrInIntAddrOutFactory;
import com.ibm.ws.channelfw.testsuite.chaincoherency.TcpAddrInStrAddrOutFactory;
import com.ibm.ws.channelfw.testsuite.channels.outbound.OutboundDummyFactory;
import com.ibm.ws.channelfw.testsuite.channels.protocol.ProtocolDummyFactory;
import com.ibm.ws.channelfw.testsuite.channels.server.AppDummyFactory;
import com.ibm.ws.tcpchannel.internal.TCPChannelFactory;
import com.ibm.wsspi.channelfw.ChainEventListener;
import com.ibm.wsspi.channelfw.Channel;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.channelfw.exception.ChainException;
import com.ibm.wsspi.channelfw.exception.ChainGroupException;
import com.ibm.wsspi.channelfw.exception.ChainTimerException;
import com.ibm.wsspi.channelfw.exception.ChannelException;
import com.ibm.wsspi.channelfw.exception.IncoherentChainException;
import com.ibm.wsspi.channelfw.exception.InvalidChainNameException;
import com.ibm.wsspi.channelfw.exception.InvalidRuntimeStateException;

import test.common.SharedOutputManager;

/**
 * Test the lifecycle of chains.
 */
public class ChainLifeCycleTest {
    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    @Rule
    public TestRule managerRule = outputMgr;

    // Helper method to create valid tcp props to start a chain with TCP in it.
    private Map<Object, Object> getTcpProps(String portProp, String defPort) {
        String port = System.getProperty(portProp, defPort);
        Map<Object, Object> tcpMap = new HashMap<Object, Object>();
        tcpMap.put("port", port);
        return tcpMap;
    }

    /**
     * Test discriminator data type coherency between channels.
     */
    @Test
    public void testDiscCoherency() {
        try {
            ChannelFrameworkImpl framework = new ChannelFrameworkImpl();
            try {
                framework.addChannel("tcp", TCPChannelFactory.class, getTcpProps("ChainLifeCycleTest.testDiscCoherency", "13000"), 10);
                framework.addChannel("Ws-Int", ConnectorChannelFactory.class, null, 10);
                framework.addChannel("Int-Int", IntDiscInIntDiscOutFactory.class, null, 10);
                framework.addChannel("Int-Str", IntDiscInStrDiscOutFactory.class, null, 10);
                framework.addChannel("Str-Int", StrDiscInIntDiscOutFactory.class, null, 10);
                framework.addChannel("Str-Str", StrDiscInStrDiscOutFactory.class, null, 10);
                // Valid chain configs
                framework.addChain("Ws-Int", FlowType.INBOUND, new String[] { "tcp", "Ws-Int" });
                framework.addChain("Ws-Int,Int-Int", FlowType.INBOUND,
                                   new String[] { "tcp", "Ws-Int", "Int-Int" });
                framework.addChain("Ws-Int,Int-Int,Int-Str", FlowType.INBOUND,
                                   new String[] { "tcp", "Ws-Int", "Int-Int", "Int-Str" });
                framework.addChain("Ws-Int,Int-Str,Str-Str", FlowType.INBOUND,
                                   new String[] { "tcp", "Ws-Int", "Int-Str", "Str-Str" });
                framework.addChain("Ws-Int,Int-Str,Str-Int", FlowType.INBOUND,
                                   new String[] { "tcp", "Ws-Int", "Int-Str", "Str-Int" });
                // Invalid chain configs
                framework.addChain("Ws-Int,Str-Int", FlowType.INBOUND,
                                   new String[] { "tcp", "Ws-Int", "Str-Int" });
                framework.addChain("Ws-Int,Int-Str,Int-Str", FlowType.INBOUND,
                                   new String[] { "tcp", "Ws-Int", "Int-Str", "Int-Str" });
                framework.addChain("Ws-Int,Int-Str,Str-Int,Str-Str", FlowType.INBOUND,
                                   new String[] { "tcp", "Ws-Int", "Int-Str", "Str-Int", "Str-Str" });

                framework.initChain("Ws-Int");
                framework.initChain("Ws-Int,Int-Int");
                framework.initChain("Ws-Int,Int-Int,Int-Str");
                framework.initChain("Ws-Int,Int-Str,Str-Str");
                framework.initChain("Ws-Int,Int-Str,Str-Int");

                try {
                    framework.initChain("Ws-Int,Str-Int");
                    fail("Did not catch disc type mismatch.");
                } catch (IncoherentChainException e) {
                    // expected failure
                }

                try {
                    framework.initChain("Ws-Int,Int-Str,Int-Str");
                    fail("Did not catch disc type mismatch.");
                } catch (IncoherentChainException e) {
                    // expected failure
                }

                try {
                    framework.initChain("Ws-Int,Int-Str,Str-Int,Str-Str");
                    fail("Did not catch disc type mismatch.");
                } catch (IncoherentChainException e) {
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
            outputMgr.failWithThrowable("testDiscCoherency", t);
        }
    }

    /**
     * Test address coherency between outbound channels.
     */
    @Test
    public void testAddrCoherency() {
        try {
            ChannelFrameworkImpl framework = new ChannelFrameworkImpl();
            try {
                framework.addChannel("Tcp", TCPChannelFactory.class, getTcpProps("ChainLifeCycleTest.testAddrCoherency", "13001"), 10);
                framework.addChannel("Str-Int", IntAddrInStrAddrOutFactory.class, null, 10);
                framework.addChannel("Int-Str", StrAddrInIntAddrOutFactory.class, null, 10);
                framework.addChannel("BoolInt-Tcp", TcpAddrInBoolIntAddrOutFactory.class, null, 10);
                framework.addChannel("Int-Tcp", TcpAddrInIntAddrOutFactory.class, null, 10);
                framework.addChannel("Str-Tcp", TcpAddrInStrAddrOutFactory.class, null, 10);

                // Valid chain configs
                framework.addChain("BoolInt-Tcp,Tcp", FlowType.OUTBOUND,
                                   new String[] { "BoolInt-Tcp", "Tcp" });
                framework.addChain("Str-Int,BoolInt-Tcp,Tcp", FlowType.OUTBOUND,
                                   new String[] { "Str-Int", "BoolInt-Tcp", "Tcp" });
                framework.addChain("Int-Tcp,Tcp", FlowType.OUTBOUND,
                                   new String[] { "Int-Tcp", "Tcp" });
                framework.addChain("Str-Tcp,Tcp", FlowType.OUTBOUND,
                                   new String[] { "Str-Tcp", "Tcp" });
                framework.addChain("Int-Str,Str-Tcp,Tcp", FlowType.OUTBOUND,
                                   new String[] { "Int-Str", "Str-Tcp", "Tcp" });
                framework.addChain("Str-Int,Int-Tcp,Tcp", FlowType.OUTBOUND,
                                   new String[] { "Str-Int", "Int-Tcp", "Tcp" });

                // Invalid chain configs
                framework.addChain("Int-Str,Tcp", FlowType.OUTBOUND,
                                   new String[] { "Int-Str", "Tcp" });
                framework.addChain("Int-Str,BoolInt-Tcp,Tcp", FlowType.OUTBOUND,
                                   new String[] { "Int-Str", "BoolInt-Tcp", "Tcp" });
                framework.addChain("Str-Int,Int-Str,BoolInt-Tcp,Tcp", FlowType.OUTBOUND,
                                   new String[] { "Str-Int", "Int-Str", "BoolInt-Tcp", "Tcp" });

                framework.getOutboundVCFactory("BoolInt-Tcp,Tcp");
                framework.getOutboundVCFactory("Str-Int,BoolInt-Tcp,Tcp");
                framework.getOutboundVCFactory("Int-Tcp,Tcp");
                framework.getOutboundVCFactory("Str-Tcp,Tcp");
                framework.getOutboundVCFactory("Int-Str,Str-Tcp,Tcp");
                framework.getOutboundVCFactory("Str-Int,Int-Tcp,Tcp");

                try {
                    framework.getOutboundVCFactory("Int-Str,Tcp");
                    fail("Did not catch addr type mismatch.");
                } catch (IncoherentChainException e) {
                    // expected failure
                }

                try {
                    framework.getOutboundVCFactory("Int-Str,BoolInt-Tcp,Tcp");
                    fail("Did not catch addr type mismatch.");
                } catch (IncoherentChainException e) {
                    // expected failure
                }

                try {
                    framework.getOutboundVCFactory("Str-Int,Int-Str,BoolInt-Tcp,Tcp");
                    fail("Did not catch addr type mismatch.");
                } catch (IncoherentChainException e) {
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
            outputMgr.failWithThrowable("testAddrCoherency", t);
        }
    }

    /**
     * Test initChain method.
     */
    @Test
    public void testInitChain() {
        try {
            int numChains = 0;
            Chain chain = null;

            ChannelFrameworkImpl framework = new ChannelFrameworkImpl();
            try {
                framework.addChannel("tcp", TCPChannelFactory.class, getTcpProps("ChainLifeCycleTest.testInitChain", "13002"), 45);
                framework.addChannel("proto", ProtocolDummyFactory.class, null, 10);
                framework.addChannel("out", OutboundDummyFactory.class, null);
                framework.addChannel("app", AppDummyFactory.class, null, 10);
                framework.addChain("inbound", FlowType.INBOUND, new String[] { "tcp", "proto" });
                framework.addChain("outbound", FlowType.OUTBOUND, new String[] { "out", "tcp" });

                try {
                    framework.initChain(null);
                    fail("Incorrectly allowed null chain");
                } catch (InvalidChainNameException e) {
                    // expected failure
                }

                try {
                    framework.initChain("unknown");
                    fail("Incorrectly allowed unknown chain");
                } catch (InvalidChainNameException e) {
                    // expected failure
                }

                numChains = framework.getNumRunningChains();
                framework.getOutboundVCFactory("outbound");
                chain = framework.getRunningChain("outbound");
                assertNotNull(chain);
                assertEquals(RuntimeState.INITIALIZED, chain.getState());
                try {
                    framework.initChain("outbound");
                    fail("Should not allow outbound chains to use this interface");
                } catch (InvalidChainNameException e) {
                    // expected failure
                }

                numChains = framework.getNumRunningChains();
                framework.initChain("inbound");
                chain = framework.getRunningChain("inbound");
                assertEquals((numChains + 1), framework.getNumRunningChains());
                assertNotNull(chain);
                assertEquals(RuntimeState.INITIALIZED, chain.getState());

                numChains = framework.getNumRunningChains();
                try {
                    framework.initChain("inbound");
                    fail("Incorrectly allowed redundant init");
                } catch (InvalidRuntimeStateException e) {
                    // expected failure
                }

                numChains = framework.getNumRunningChains();
                framework.startChain("inbound");
                try {
                    framework.initChain("inbound");
                    fail("Incorrectly allowed init on started chain");
                } catch (InvalidRuntimeStateException e) {
                    // expected failure
                }

                numChains = framework.getNumRunningChains();
                framework.stopChain("inbound", 100);
                try {
                    framework.initChain("inbound");
                    fail("Incorrectly allowed init on stopped chain");
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
            outputMgr.failWithThrowable("testInitChain", t);
        }
    }

    /**
     * Test startChain method.
     */
    @Test
    public void testStartChain() {
        try {
            int numChains = 0;
            Chain chain = null;

            ChannelFrameworkImpl framework = new ChannelFrameworkImpl();
            try {
                framework.addChannel("tcp", TCPChannelFactory.class, getTcpProps("ChainLifeCycleTest.testStartChain", "13003"), 45);
                framework.addChannel("proto", ProtocolDummyFactory.class, null, 10);
                framework.addChannel("out", OutboundDummyFactory.class, null, 10);
                framework.addChain("inbound", FlowType.INBOUND, new String[] { "tcp", "proto" });
                framework.addChain("outbound", FlowType.OUTBOUND, new String[] { "out", "tcp" });

                try {
                    framework.startChain((String) null);
                    fail("Incorrectly allowed null chain");
                } catch (InvalidChainNameException e) {
                    // expected failure
                }

                try {
                    framework.startChain("unknown");
                    fail("Incorrectly alloewd unknown chain");
                } catch (InvalidChainNameException e) {
                    // expected failure
                }

                numChains = framework.getNumRunningChains();
                framework.getOutboundVCFactory("outbound");
                chain = framework.getRunningChain("outbound");
                assertNotNull(chain);
                assertEquals(RuntimeState.INITIALIZED, chain.getState());
                try {
                    framework.startChain("outbound");
                    fail("Should not allow outbound chains to use this interface");
                } catch (InvalidChainNameException e) {
                    // expected failure
                }

                numChains = framework.getNumRunningChains();
                framework.initChain("inbound");
                framework.startChain("inbound");
                chain = framework.getRunningChain("inbound");
                assertEquals((numChains + 1), framework.getNumRunningChains());
                assertNotNull(chain);
                assertEquals(RuntimeState.STARTED, chain.getState());

                // Redundant start should simply be ignored.
                numChains = framework.getNumRunningChains();
                framework.startChain("inbound");
                assertEquals(numChains, framework.getNumRunningChains());

                chain = framework.getRunningChain("inbound");
                framework.stopChain("inbound", 0);
                assertEquals(RuntimeState.INITIALIZED, chain.getState());
                framework.startChain("inbound");
                assertEquals(RuntimeState.STARTED, chain.getState());

                chain = framework.getRunningChain("inbound");
                framework.stopChain("inbound", 5000);
                assertEquals(RuntimeState.QUIESCED, chain.getState());
                try {
                    framework.startChain("inbound");
                    fail("Incorrectly started a quiesced chain");
                } catch (InvalidRuntimeStateException e) {
                    // expected failure
                }

                numChains = framework.getNumRunningChains();
                framework.stopChain("inbound", 0);
                framework.destroyChain("inbound");
                chain = framework.getRunningChain("inbound");
                assertNull(chain);
                framework.startChain("inbound");
                chain = framework.getRunningChain("inbound");
                assertNotNull(chain);
                assertEquals(RuntimeState.STARTED, chain.getState());
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
            outputMgr.failWithThrowable("testStartChain", t);
        }
    }

    /**
     * Test stopChain method.
     */
    @Test
    public void testStopChain() {
        try {
            Chain chain = null;

            ChannelFrameworkImpl framework = new ChannelFrameworkImpl();
            try {
                framework.addChannel("tcp", TCPChannelFactory.class, getTcpProps("ChainLifeCycleTest.testStopChain", "13004"), 45);
                framework.addChannel("proto", ProtocolDummyFactory.class, null, 10);
                framework.addChannel("out", OutboundDummyFactory.class, null, 10);
                framework.addChain("inbound", FlowType.INBOUND, new String[] { "tcp", "proto" });
                framework.addChain("outbound", FlowType.OUTBOUND, new String[] { "out", "tcp" });

                try {
                    framework.stopChain((String) null, 0);
                    fail("Incorrectly allowed null chain");
                } catch (InvalidChainNameException e) {
                    // expected failure
                }

                // This should be an allowed no-op
                framework.stopChain("unknown", 0);

                framework.getOutboundVCFactory("outbound");
                chain = framework.getRunningChain("outbound");
                assertNotNull(chain);
                assertEquals(RuntimeState.INITIALIZED, chain.getState());
                try {
                    framework.stopChain("outbound", 0);
                    fail("Should not allow outbound chains to use this interface");
                } catch (InvalidChainNameException e) {
                    // expected failure
                }

                framework.initChain("inbound");
                framework.startChain("inbound");
                chain = framework.getRunningChain("inbound");
                assertEquals(RuntimeState.STARTED, chain.getState());
                framework.stopChain("inbound", 0);
                assertEquals(RuntimeState.INITIALIZED, chain.getState());

                framework.destroyChain("inbound");
                framework.initChain("inbound");
                chain = framework.getRunningChain("inbound");
                assertEquals(RuntimeState.INITIALIZED, chain.getState());

                // a redundant stop should be a no-op
                framework.stopChain("inbound", 0);

                framework.startChain("inbound");
                framework.stopChain("inbound", 0);

                // redundant stop is a no-op
                framework.stopChain("inbound", 0);

                framework.destroyChain("inbound");
                framework.startChain("inbound");
                framework.stopChain("inbound", 5000);
                chain = framework.getRunningChain("inbound");
                assertEquals(RuntimeState.QUIESCED, chain.getState());
                framework.stopChain("inbound", 0);
                assertEquals(RuntimeState.INITIALIZED, chain.getState());
                assertNull(chain.getStopTask());
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
            outputMgr.failWithThrowable("testStopChain", t);
        }
    }

    static interface TestEventListener extends ChainEventListener {
        void waitForEvent() throws InterruptedException;
    }

    /**
     * Test quiesceChain method.
     */
    @Test
    public void testQuiesceChain() {
        try {
            Chain chain = null;

            ChannelFrameworkImpl framework = new ChannelFrameworkImpl();

            TestEventListener testListener = new TestEventListener() {
                @Override
                public synchronized void chainInitialized(ChainData chainData) {
                    System.out.println(chainData.getName() + " intitialized");
                    notifyAll();
                }

                @Override
                public synchronized void chainStarted(ChainData chainData) {
                    System.out.println(chainData.getName() + " started");
                    notifyAll();
                }

                @Override
                public synchronized void chainStopped(ChainData chainData) {
                    System.out.println(chainData.getName() + " stopped");
                    notifyAll();
                }

                @Override
                public synchronized void chainQuiesced(ChainData chainData) {
                    System.out.println(chainData.getName() + " quiesced");
                    notifyAll();
                }

                @Override
                public synchronized void chainDestroyed(ChainData chainData) {
                    System.out.println(chainData.getName() + " destroyed");
                    notifyAll();
                }

                @Override
                public synchronized void chainUpdated(ChainData chainData) {
                    System.out.println(chainData.getName() + " updated");
                    notifyAll();
                }

                @Override
                public synchronized void waitForEvent() throws InterruptedException {
                    this.wait(5 * 1000); // max wait of 5 seconds
                }
            };

            try {
                framework.addChannel("tcp", TCPChannelFactory.class, getTcpProps("ChainLifeCycleTest.testQuiesceChain1", "13005"), 45);
                framework.addChannel("proto", ProtocolDummyFactory.class, null, 10);
                framework.addChain("inbound", FlowType.INBOUND, new String[] { "tcp", "proto" });

                try {
                    framework.stopChain("inbound", -1);
                    fail("Incorrectly allowed negative time");
                } catch (ChainTimerException e) {
                    // expected failure
                }

                framework.initChain("inbound");
                framework.addChainEventListener(testListener, "inbound");
                chain = framework.getRunningChain("inbound");

                assertEquals(RuntimeState.INITIALIZED, chain.getState());
                // Stopping a not-started chain should be a no-op
                framework.stopChain("inbound", 50);

                framework.startChain("inbound");
                assertEquals(RuntimeState.STARTED, chain.getState());
                framework.stopChain("inbound", 50);
                assertEquals(RuntimeState.QUIESCED, chain.getState());
                // State change happened. Now wait for quiesce to finish.
                try {
                    testListener.waitForEvent();
                } catch (InterruptedException ie) {
                    // nothing
                }
                assertEquals(RuntimeState.INITIALIZED, chain.getState());

                framework.startChain("inbound");
                framework.stopChain("inbound", 50);
                assertEquals(RuntimeState.QUIESCED, chain.getState());
                // stop of quiesced chain is a no-op
                framework.stopChain("inbound", 50);

                try {
                    testListener.waitForEvent();
                } catch (InterruptedException ie) { // nothing
                }
                assertEquals(RuntimeState.INITIALIZED, chain.getState());

                //
                // Test a combination of state changes and channel sharing that caused
                // a defect, 254649, where a chain in quiesced state did not get its
                // discrimination group cleaned up leading to a future NPE.

                framework.clear();
                framework.addChannel("tcp", TCPChannelFactory.class, getTcpProps("ChainLifeCycleTest.testQuiesceChain2", "13006"), 45);
                framework.addChannel("proto", ProtocolDummyFactory.class, null, 45);
                framework.addChannel("http2", ProtocolDummyFactory.class, null, 45);
                framework.addChain("inbound1", FlowType.INBOUND, new String[] { "tcp", "proto" });
                framework.addChain("inbound2", FlowType.INBOUND, new String[] { "tcp", "http2" });

                framework.startChain("inbound1");
                framework.startChain("inbound2");
                framework.stopChain("inbound1", 2000);
                framework.stopChain("inbound1", 0);
                framework.destroyChain("inbound1");
                framework.stopChain("inbound2", 0);
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
            outputMgr.failWithThrowable("testQuiesceChain", t);
        }
    }

    /**
     * Test destroyChain method.
     */
    @Test
    public void testDestroyChain() {
        try {
            int numChains = 0;
            Chain chain = null;

            ChannelFrameworkImpl framework = new ChannelFrameworkImpl();
            try {
                framework.addChannel("tcp", TCPChannelFactory.class, getTcpProps("ChainLifeCycleTest.testDestroyChain", "13007"), 45);
                framework.addChannel("proto", ProtocolDummyFactory.class, null, 10);
                framework.addChannel("out", OutboundDummyFactory.class, null, 10);
                framework.addChain("inbound", FlowType.INBOUND, new String[] { "tcp", "proto" });
                framework.addChain("outbound", FlowType.OUTBOUND, new String[] { "out", "tcp" });

                try {
                    framework.destroyChain((String) null);
                    fail("Incorrectly allowed null chain");
                } catch (InvalidChainNameException e) {
                    // expected failure
                }

                framework.getOutboundVCFactory("outbound");
                numChains = framework.getNumRunningChains();
                chain = framework.getRunningChain("outbound");
                assertNotNull(chain);
                assertEquals(RuntimeState.INITIALIZED, chain.getState());
                try {
                    framework.destroyChain("outbound");
                    fail("Should not allow outbound chains to use this interface");
                } catch (InvalidChainNameException e) {
                    // expected failure
                }

                framework.initChain("inbound");
                numChains = framework.getNumRunningChains();
                chain = framework.getRunningChain("inbound");
                assertEquals(RuntimeState.INITIALIZED, chain.getState());
                framework.destroyChain("inbound");
                assertNull(framework.getRunningChain("inbound"));
                assertEquals(RuntimeState.UNINITIALIZED, chain.getState());
                assertEquals((numChains - 1), framework.getNumRunningChains());

                framework.initChain("inbound");
                framework.startChain("inbound");
                numChains = framework.getNumRunningChains();
                chain = framework.getRunningChain("inbound");
                assertEquals(RuntimeState.STARTED, chain.getState());
                try {
                    framework.destroyChain("inbound");
                    fail("Should have thrown an exception due to invalid state.");
                } catch (InvalidRuntimeStateException e) {
                    // expected failure
                }

                framework.stopChain("inbound", 100);
                chain = framework.getRunningChain("inbound");
                assertEquals(RuntimeState.QUIESCED, chain.getState());
                try {
                    framework.destroyChain("inbound");
                    fail("Incorrectly allowed destroy on quiesced chain");
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
            outputMgr.failWithThrowable("testDestroyChain", t);
        }
    }

    /**
     * Test VC Factory regarding framework references and life cycle.
     */
    @Test
    public void testVCFactory() {
        try {
            int numChains = 0;
            int numFactories = 0;
            int refCount = 0;
            OutboundVirtualConnectionFactoryImpl vc1 = null;
            OutboundVirtualConnectionFactoryImpl vc2 = null;
            OutboundVirtualConnectionFactoryImpl vc3 = null;
            Chain chain = null;

            ChannelFrameworkImpl framework = new ChannelFrameworkImpl();
            try {
                framework.addChannel("tcp", TCPChannelFactory.class, getTcpProps("ChainLifeCycleTest.testVCFactory", "13008"), 45);
                framework.addChannel("out", OutboundDummyFactory.class, null, 10);
                framework.addChannel("httpreq", OutboundDummyFactory.class, null, 10);
                framework.addChain("outbound1", FlowType.OUTBOUND, new String[] { "out", "tcp" });
                framework.addChain("outbound2", FlowType.OUTBOUND, new String[] { "httpreq", "tcp" });
                framework.addChain("inbound", FlowType.INBOUND, new String[] { "tcp", "httpreq" });

                try {
                    vc1 = (OutboundVirtualConnectionFactoryImpl) framework.getOutboundVCFactory(null);
                    fail("Incorrectly allowed null name");
                } catch (InvalidChainNameException e) {
                    // expected failure
                }

                try {
                    vc1 = (OutboundVirtualConnectionFactoryImpl) framework.getOutboundVCFactory("unknown");
                    fail("Incorrectly allowed unknown name");
                } catch (InvalidChainNameException e) {
                    // expected failure
                }

                numFactories = framework.getNumOutboundVCFs();
                numChains = framework.getNumRunningChains();
                vc1 = (OutboundVirtualConnectionFactoryImpl) framework.getOutboundVCFactory("outbound1");
                chain = framework.getRunningChain("outbound1");
                assertNotNull(chain);
                assertEquals((numChains + 1), framework.getNumRunningChains());
                assertEquals(RuntimeState.INITIALIZED, chain.getState());
                assertEquals((numFactories + 1), framework.getNumOutboundVCFs());
                assertEquals(1, vc1.getRefCount());

                refCount = vc1.getRefCount();
                numFactories = framework.getNumOutboundVCFs();
                numChains = framework.getNumRunningChains();
                vc2 = (OutboundVirtualConnectionFactoryImpl) framework.getOutboundVCFactory("outbound1");
                chain = framework.getRunningChain("outbound1");
                assertNotNull(chain);
                assertEquals(numChains, framework.getNumRunningChains());
                assertEquals(RuntimeState.INITIALIZED, chain.getState());
                assertEquals(numFactories, framework.getNumOutboundVCFs());
                assertEquals((refCount + 1), vc2.getRefCount());

                try {
                    ChainData cd = framework.removeChain("outbound1");
                    if (cd != null)
                        fail("Should have thrown exception");
                } catch (ChainException e) {
                    // expected failure
                }

                VirtualConnection vCon = vc1.createConnection();
                vCon.destroy();
                vc1.destroy();
                vc2.destroy();
                framework.removeChain("outbound1");
                framework.addChain("outbound1", FlowType.OUTBOUND, new String[] { "out", "tcp" });

                numFactories = framework.getNumOutboundVCFs();
                numChains = framework.getNumRunningChains();
                vc2 = (OutboundVirtualConnectionFactoryImpl) framework.getOutboundVCFactory("outbound2");
                chain = framework.getRunningChain("outbound2");
                assertNotNull(chain);
                assertEquals((numChains + 1), framework.getNumRunningChains());
                assertEquals(RuntimeState.INITIALIZED, chain.getState());
                assertEquals((numFactories + 1), framework.getNumOutboundVCFs());
                assertEquals(1, vc2.getRefCount());

                numFactories = framework.getNumOutboundVCFs();
                vc2.destroy();
                assertEquals((numFactories - 1), framework.getNumOutboundVCFs());

                vc1 = (OutboundVirtualConnectionFactoryImpl) framework.getOutboundVCFactory("outbound1");
                chain = framework.getRunningChain("outbound1");
                Channel channel = framework.getRunningChannel("out", chain);
                vc3 = (OutboundVirtualConnectionFactoryImpl) framework.getOutboundVCFactory(channel.getName());
                assertNotNull(vc3);
                assertEquals(channel.getName(), vc3.getName());
                assertEquals(1, vc3.getChain().getChannelsData().length);
                assertEquals("tcp" + ChannelDataImpl.CHILD_STRING + "2", vc3.getChain().getChannelsData()[0].getName());

                vc1 = (OutboundVirtualConnectionFactoryImpl) framework.getOutboundVCFactory("outbound1");
                numFactories = framework.getNumOutboundVCFs();
                numChains = framework.getNumRunningChains();
                refCount = vc1.getRefCount();
                vc1.createConnection();
                chain = framework.getRunningChain("outbound1");
                assertNotNull(chain);
                assertEquals(numChains, framework.getNumRunningChains());
                assertEquals(RuntimeState.STARTED, chain.getState());
                assertEquals(numFactories, framework.getNumOutboundVCFs());
                assertEquals(refCount, vc1.getRefCount());

                numFactories = framework.getNumOutboundVCFs();
                numChains = framework.getNumRunningChains();
                refCount = vc1.getRefCount();
                vc1.createConnection();
                chain = framework.getRunningChain("outbound1");
                assertNotNull(chain);
                assertEquals(numChains, framework.getNumRunningChains());
                assertEquals(RuntimeState.STARTED, chain.getState());
                assertEquals(numFactories, framework.getNumOutboundVCFs());
                assertEquals(refCount, vc1.getRefCount());

                numFactories = framework.getNumOutboundVCFs();
                numChains = framework.getNumRunningChains();
                refCount = vc3.getRefCount();
                vc3.createConnection();
                chain = framework.getRunningChain(vc3.getName());
                assertNotNull(chain);
                assertEquals(numChains, framework.getNumRunningChains());
                assertEquals(RuntimeState.STARTED, chain.getState());
                assertEquals(numFactories, framework.getNumOutboundVCFs());
                assertEquals(refCount, vc3.getRefCount());

                numFactories = framework.getNumOutboundVCFs();
                numChains = framework.getNumRunningChains();
                refCount = vc3.getRefCount();
                vc3.destroy();
                chain = framework.getRunningChain(vc3.getName());
                assertNull(chain);
                assertFalse(numChains == framework.getNumRunningChains());
                assertFalse(numFactories == framework.getNumOutboundVCFs());

                numFactories = framework.getNumOutboundVCFs();
                numChains = framework.getNumRunningChains();
                refCount = vc1.getRefCount();
                vc1.destroy();
                chain = framework.getRunningChain("outbound1");
                assertNotNull(chain);
                assertEquals(numChains, framework.getNumRunningChains());
                assertEquals(RuntimeState.STARTED, chain.getState());
                assertEquals(numFactories, framework.getNumOutboundVCFs());
                assertEquals((refCount - 1), vc1.getRefCount());

                numFactories = framework.getNumOutboundVCFs();
                numChains = framework.getNumRunningChains();
                refCount = vc1.getRefCount();
                vc1.destroy();
                chain = framework.getRunningChain("outbound1");
                assertNull(chain);
                assertFalse(numChains == framework.getNumRunningChains());
                assertFalse(numFactories == framework.getNumOutboundVCFs());

                framework.clear();
                chain = framework.getRunningChain("outbound1");
                assertNull(chain);
                assertEquals(0, framework.getNumRunningChains());
                assertEquals(0, framework.getNumOutboundVCFs());
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
            outputMgr.failWithThrowable("testVCFactory", t);
        }
    }
}
