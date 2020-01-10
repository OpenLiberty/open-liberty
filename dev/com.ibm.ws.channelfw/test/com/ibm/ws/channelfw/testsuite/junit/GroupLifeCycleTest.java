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
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.ibm.websphere.channelfw.ChainData;
import com.ibm.websphere.channelfw.ChainGroupData;
import com.ibm.websphere.channelfw.ChainStartMode;
import com.ibm.websphere.channelfw.FlowType;
import com.ibm.ws.channelfw.internal.ChainDataImpl;
import com.ibm.ws.channelfw.internal.ChannelFrameworkConstants;
import com.ibm.ws.channelfw.internal.ChannelFrameworkImpl;
import com.ibm.ws.channelfw.internal.RuntimeState;
import com.ibm.ws.channelfw.internal.chains.Chain;
import com.ibm.ws.channelfw.testsuite.channels.broken.BrokenChannel;
import com.ibm.ws.channelfw.testsuite.channels.broken.BrokenChannelFactory;
import com.ibm.ws.channelfw.testsuite.channels.protocol.ProtocolDummyFactory;
import com.ibm.ws.tcpchannel.internal.TCPChannelFactory;
import com.ibm.wsspi.channelfw.exception.ChainException;
import com.ibm.wsspi.channelfw.exception.ChainGroupException;
import com.ibm.wsspi.channelfw.exception.ChainTimerException;
import com.ibm.wsspi.channelfw.exception.ChannelException;

import test.common.SharedOutputManager;

/**
 * The purpose of this class is to test the group configuration methods
 * in ChannelFrameworkImpl.
 */
public class GroupLifeCycleTest {
    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    @Rule
    public TestRule managerRule = outputMgr;

    private Map<Object, Object> getTcpProps(String portProp, String defPort) {
        String port = System.getProperty(portProp, defPort);
        Map<Object, Object> tcpMap = new HashMap<Object, Object>();
        tcpMap.put(ChannelFrameworkConstants.PORT, port);
        tcpMap.put(ChannelFrameworkConstants.HOST_NAME, "localhost");
        return tcpMap;
    }

    /**
     * Test initGroup method.
     */
    @Test
    public void testInitGroup() {
        try {
            Chain chain1 = null;
            Chain chain2 = null;
            Chain chain3 = null;
            ChainData[] changedChains = null;

            ChannelFrameworkImpl framework = new ChannelFrameworkImpl();
            try {
                framework.addChannel("broken", BrokenChannelFactory.class, null, 45);
                framework.addChannel("tcp", TCPChannelFactory.class, getTcpProps("GroupLifeCycleTest.testInitGroup", "7850"), 45);
                framework.addChannel("proto", ProtocolDummyFactory.class, null, 10);
                framework.addChain("chain1", FlowType.INBOUND, new String[] { "tcp", "proto" });
                framework.addChain("chain2", FlowType.INBOUND, new String[] { "tcp" });
                framework.addChain("chain3", FlowType.INBOUND, new String[] { "tcp" });
                framework.addChain("broken", FlowType.INBOUND, new String[] { "broken" });
                framework.addChainGroup("group1", new String[] { "chain1", "chain2", "chain3" });
                framework.addChainGroup("broken", new String[] { "chain1", "broken", "chain2", "chain3" });

                try {
                    framework.initChainGroup(null);
                    fail("Incorrectly allowed null group name");
                } catch (ChainGroupException e) {
                    // expected failure
                }

                try {
                    framework.initChainGroup("unknown");
                    fail("Incorectly initialized unknown group");
                } catch (ChainGroupException e) {
                    // expected failure
                }

                try {
                    framework.initChainGroup("broken");
                    fail("Incorrectly initialized broken group");
                } catch (ChainGroupException e) {
                    assertNotNull(framework.getChainGroup("group1"));
                    chain1 = framework.getRunningChain("chain1");
                    chain2 = framework.getRunningChain("chain2");
                    chain3 = framework.getRunningChain("chain3");
                    assertNull(framework.getRunningChain("broken"));
                    assertEquals(RuntimeState.INITIALIZED, chain1.getState());
                    assertEquals(RuntimeState.INITIALIZED, chain2.getState());
                    assertEquals(RuntimeState.INITIALIZED, chain3.getState());
                }

                framework.destroyChain("chain1");
                framework.startChain("chain2");
                // Note that chain3 is still in init state.
                changedChains = framework.initChainGroup("group1");
                chain1 = framework.getRunningChain("chain1");
                chain2 = framework.getRunningChain("chain2");
                chain3 = framework.getRunningChain("chain3");
                assertEquals(RuntimeState.INITIALIZED, chain1.getState());
                assertEquals(RuntimeState.STARTED, chain2.getState());
                assertEquals(RuntimeState.INITIALIZED, chain3.getState());
                assertEquals(1, changedChains.length);
                assertEquals("chain1", changedChains[0].getName());
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
            outputMgr.failWithThrowable("testInitGroup", t);
        }
    }

    /**
     * Test startGroup method.
     */
    //SplitStartUp @Test
    public void testStartGroup() {
        try {
            Chain chain1 = null;
            Chain chain2 = null;
            Chain chain3 = null;
            ChainGroupData group = null;
            ChainData[] changedChains = null;

            ChannelFrameworkImpl framework = new ChannelFrameworkImpl();
            try {
                Map<Object, Object> tcpProps = getTcpProps("GroupLifeCycleTest.testStartGroup1", "7844");
                final String EXPECTED_PORT_STRING = (String) tcpProps.get(ChannelFrameworkConstants.PORT);
                framework.addChannel("tcp", TCPChannelFactory.class, tcpProps, 45);
                framework.addChannel("broken", BrokenChannelFactory.class, null, 45);
                framework.addChannel("proto", ProtocolDummyFactory.class, null, 10);
                framework.addChain("chain1", FlowType.INBOUND, new String[] { "tcp", "proto" });
                framework.addChain("chain2", FlowType.INBOUND, new String[] { "tcp" });
                framework.addChain("chain3", FlowType.INBOUND, new String[] { "tcp" });
                framework.addChain("broken", FlowType.INBOUND, new String[] { "broken" });
                framework.addChainGroup("group1", new String[] { "chain1", "chain2", "chain3" });
                framework.addChainGroup("broken", new String[] { "chain1", "broken", "chain2", "chain3" });

                try {
                    framework.startChainGroup(null);
                    fail("Incorrectly allowed null group");
                } catch (ChainGroupException e) {
                    // expected failure
                }

                try {
                    framework.startChainGroup("unknown");
                    fail("Incorrectly allowed unknown group");
                } catch (ChainGroupException e) {
                    // expected failure
                }

                framework.initChainGroup("group1");
                try {
                    framework.startChainGroup("broken");
                    fail("Incorrectly started broken group");
                } catch (Exception e) {
                    group = framework.getChainGroup("broken");
                    chain1 = framework.getRunningChain("chain1");
                    chain2 = framework.getRunningChain("chain2");
                    chain3 = framework.getRunningChain("chain3");
                    assertNull(framework.getRunningChain("broken"));
                    ChainDataImpl chainData = (ChainDataImpl) framework.getChain("chain1");
                    String portString = (String) chainData.getPropertyBag().get(ChannelFrameworkConstants.PORT);
                    String hostString = (String) chainData.getPropertyBag().get(ChannelFrameworkConstants.HOST_NAME);
                    assertNotNull(group);
                    assertEquals(4, group.getChains().length);
                    assertEquals(RuntimeState.STARTED, chain1.getState());
                    assertEquals(RuntimeState.STARTED, chain2.getState());
                    assertEquals(RuntimeState.STARTED, chain3.getState());
                    assertEquals(EXPECTED_PORT_STRING, portString);
                    assertEquals("localhost", hostString);
                }

                framework.stopChainGroup("group1", 0);
                framework.destroyChainGroup("group1");
                assertNull(framework.getRunningChain("chain1"));
                assertNull(framework.getRunningChain("chain2"));
                assertNull(framework.getRunningChain("chain3"));
                assertNull(framework.getRunningChain("broken"));
                try {
                    framework.startChainGroup("broken");
                    fail("Incorrectly started broken group");
                } catch (Exception e) {
                    group = framework.getChainGroup("broken");
                    chain1 = framework.getRunningChain("chain1");
                    chain2 = framework.getRunningChain("chain2");
                    chain3 = framework.getRunningChain("chain3");
                    assertNotNull(group);
                    assertEquals(4, group.getChains().length);
                    assertEquals(RuntimeState.STARTED, chain1.getState());
                    assertEquals(RuntimeState.STARTED, chain2.getState());
                    assertEquals(RuntimeState.STARTED, chain3.getState());
                }

                framework.stopChain("chain1", 5000);
                framework.stopChain("chain2", 0);
                // Note that chain3 is still in started state.
                try {
                    framework.startChainGroup("group1");
                    fail("Group start should throw exception");
                } catch (Exception e) {
                    chain1 = framework.getRunningChain("chain1");
                    chain2 = framework.getRunningChain("chain2");
                    chain3 = framework.getRunningChain("chain3");
                    assertEquals(RuntimeState.QUIESCED, chain1.getState());
                    assertEquals(RuntimeState.STARTED, chain2.getState());
                    assertEquals(RuntimeState.STARTED, chain3.getState());
                }

                framework.stopChainGroup("group1", 0);
                // Note that chain3 is still in started state.
                try {
                    changedChains = framework.startChainGroup("group1");
                    chain1 = framework.getRunningChain("chain1");
                    chain2 = framework.getRunningChain("chain2");
                    chain3 = framework.getRunningChain("chain3");
                    assertEquals(3, changedChains.length);
                    assertEquals(RuntimeState.STARTED, chain1.getState());
                    assertEquals(RuntimeState.STARTED, chain2.getState());
                    assertEquals(RuntimeState.STARTED, chain3.getState());
                } catch (Exception e) {
                    e.printStackTrace();
                    fail("Group start should not throw exception");
                }

                // Clean up the config for a fresh start.
                // Now start the testing of startChainGroup with retries for failing chains
                framework.clear();

                framework.addChannel("tcp1", TCPChannelFactory.class, getTcpProps("GroupLifeCycleTest.testStartGroup2", "10167"));
                framework.addChannel("tcp1fail", TCPChannelFactory.class, getTcpProps("GroupLifeCycleTest.testStartGroup2", "10167"));
                framework.addChannel("tcp2", TCPChannelFactory.class, getTcpProps("GroupLifeCycleTest.testStartGroup3", "10168"));
                framework.addChain("chain1", FlowType.INBOUND, new String[] { "tcp1" });
                framework.addChain("chain1fail", FlowType.INBOUND, new String[] { "tcp1fail" });
                framework.addChain("chain2", FlowType.INBOUND, new String[] { "tcp2" });
                framework.addChainGroup("group1", new String[] { "chain1" });
                framework.addChainGroup("group2", new String[] { "chain1", "chain2" });
                framework.addChainGroup("groupfail", new String[] { "chain1", "chain1fail" });

                framework.setChainStartRetryAttempts("2");
                framework.setChainStartRetryInterval("10");
                changedChains = framework.startChainGroup("group1", ChainStartMode.RETRY_EACH_ON_FAIL);
                assertEquals(1, changedChains.length);
                assertNotNull(framework.getRunningChain("chain1"));
                // Clean up after the test.
                framework.stopChain("chain1", 0);

                framework.setChainStartRetryAttempts("2");
                framework.setChainStartRetryInterval("10");
                changedChains = framework.startChainGroup("group2", ChainStartMode.RETRY_EACH_ON_FAIL);
                assertEquals(2, changedChains.length);
                assertNotNull(framework.getRunningChain("chain1"));
                assertNotNull(framework.getRunningChain("chain2"));
                // Clean up after the test.
                framework.stopChain("chain1", 0);
                framework.stopChain("chain2", 0);

                framework.setChainStartRetryAttempts("2");
                framework.setChainStartRetryInterval("10");
                changedChains = framework.startChainGroup("groupfail", ChainStartMode.RETRY_EACH_ON_FAIL);
                assertEquals(1, changedChains.length);
                chain1 = framework.getRunningChain("chain1");
                assertEquals(RuntimeState.STARTED, chain1.getState());
                assertNull(framework.getRunningChain("chain1fail"));
                // Clean up after the test.
                framework.stopChain("chain1", 0);

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
            outputMgr.failWithThrowable("testStartGroup", t);
        }
    }

    /**
     * Test stopGroup method.
     */
    @Test
    public void testStopGroup() {
        try {
            Chain chain1 = null;
            Chain chain2 = null;
            Chain chain3 = null;
            Chain broken = null;
            ChainGroupData group = null;
            ChainData[] changedChains = null;
            Map<Object, Object> brokenMap = new HashMap<Object, Object>();
            brokenMap.put(BrokenChannel.HANDLE_INIT, BrokenChannel.SUCCEED);
            brokenMap.put(BrokenChannel.HANDLE_START, BrokenChannel.SUCCEED);
            brokenMap.put(BrokenChannel.HANDLE_STOP, BrokenChannel.FAIL);
            brokenMap.put(BrokenChannel.HANDLE_QUIESCE, BrokenChannel.SUCCEED);
            brokenMap.put(BrokenChannel.HANDLE_DESTROY, BrokenChannel.SUCCEED);

            ChannelFrameworkImpl framework = new ChannelFrameworkImpl();
            try {
                framework.addChannel("broken", BrokenChannelFactory.class, brokenMap);
                framework.addChannel("tcp", TCPChannelFactory.class, getTcpProps("GroupLifeCycleTest.testStopGroup", "7851"));
                framework.addChannel("proto", ProtocolDummyFactory.class, null);
                framework.addChain("chain1", FlowType.INBOUND, new String[] { "tcp", "proto" });
                framework.addChain("chain2", FlowType.INBOUND, new String[] { "tcp" });
                framework.addChain("chain3", FlowType.INBOUND, new String[] { "tcp" });
                framework.addChain("broken", FlowType.INBOUND, new String[] { "broken" });
                framework.addChainGroup("group1", new String[] { "chain1", "chain2", "chain3" });
                framework.addChainGroup("broken", new String[] { "chain1", "chain2", "broken", "chain3" });

                try {
                    framework.stopChainGroup(null, 0);
                    fail("Incorrectly allowed null group");
                } catch (ChainGroupException e) {
                    // expected failure
                }

                try {
                    framework.stopChainGroup("unknown", 0);
                    fail("Incorrectly allowed unknown group");
                } catch (ChainGroupException e) {
                    // expected failure
                }

                framework.initChainGroup("group1");
                framework.startChainGroup("group1");
                changedChains = framework.stopChainGroup("group1", 0);
                assertEquals(3, changedChains.length);
                assertEquals("chain1", changedChains[0].getName());
                assertEquals("chain2", changedChains[1].getName());
                assertEquals("chain3", changedChains[2].getName());
                group = framework.getChainGroup("group1");
                assertNotNull(group);
                chain1 = framework.getRunningChain("chain1");
                chain2 = framework.getRunningChain("chain2");
                chain3 = framework.getRunningChain("chain3");
                assertEquals(RuntimeState.INITIALIZED, chain1.getState());
                assertEquals(RuntimeState.INITIALIZED, chain2.getState());
                assertEquals(RuntimeState.INITIALIZED, chain3.getState());

                framework.startChain("chain1");
                // Note that chain2 is still in init state.
                changedChains = framework.stopChainGroup("group1", 0);
                assertEquals(1, changedChains.length);
                assertEquals("chain1", changedChains[0].getName());
                chain1 = framework.getRunningChain("chain1");
                chain2 = framework.getRunningChain("chain2");
                assertNull(framework.getRunningChain("broken"));
                assertEquals(RuntimeState.INITIALIZED, chain1.getState());
                assertEquals(RuntimeState.INITIALIZED, chain2.getState());

                // Put chain1 in started state. Stop should succeed.
                framework.startChain("chain1");
                chain1 = framework.getRunningChain("chain1");
                // Put chain2 in stopped state. Stop should not take place.
                framework.startChain("chain2");
                framework.stopChain("chain2", 0);
                chain2 = framework.getRunningChain("chain2");
                // Put chain3 in quiesced state. Stop should succeed.
                framework.startChain("chain3");
                framework.stopChain("chain3", 500);
                chain3 = framework.getRunningChain("chain3");
                // Put broken chain in started state. Stop should cause exception.
                framework.startChain("broken");
                broken = framework.getRunningChain("broken");
                try {
                    framework.stopChainGroup("broken", 0);
                    fail("Incorrectly stopped broken gorup");
                } catch (ChainGroupException e) {
                    assertEquals(RuntimeState.INITIALIZED, chain1.getState());
                    assertEquals(RuntimeState.INITIALIZED, chain2.getState());
                    assertEquals(RuntimeState.INITIALIZED, chain3.getState());
                    assertEquals(RuntimeState.STARTED, broken.getState());
                }
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
            outputMgr.failWithThrowable("testStopGroup", t);
        }
    }

    /**
     * Test quiesceGroup method.
     */
    @Test
    public void testQuiesceGroup() {
        try {
            Chain chain1 = null;
            Chain chain2 = null;
            Chain chain3 = null;
            Chain chain4 = null;
            Chain broken = null;
            ChainGroupData group = null;
            ChainData[] changedChains = null;
            Map<Object, Object> brokenMap = new HashMap<Object, Object>();
            brokenMap.put(BrokenChannel.HANDLE_INIT, BrokenChannel.SUCCEED);
            brokenMap.put(BrokenChannel.HANDLE_START, BrokenChannel.SUCCEED);
            brokenMap.put(BrokenChannel.HANDLE_STOP, BrokenChannel.SUCCEED);
            brokenMap.put(BrokenChannel.HANDLE_QUIESCE, BrokenChannel.FAIL);
            brokenMap.put(BrokenChannel.HANDLE_DESTROY, BrokenChannel.SUCCEED);

            ChannelFrameworkImpl framework = new ChannelFrameworkImpl();
            try {
                framework.addChannel("broken", BrokenChannelFactory.class, brokenMap);
                framework.addChannel("tcp", TCPChannelFactory.class, getTcpProps("GroupLifeCycleTest.testQuiesceGroup", "7852"));
                framework.addChannel("proto", ProtocolDummyFactory.class, null);
                framework.addChain("chain1", FlowType.INBOUND, new String[] { "tcp", "proto" });
                framework.addChain("chain2", FlowType.INBOUND, new String[] { "tcp" });
                framework.addChain("chain3", FlowType.INBOUND, new String[] { "tcp" });
                framework.addChain("chain4", FlowType.INBOUND, new String[] { "tcp" });
                framework.addChain("chain5", FlowType.INBOUND, new String[] { "tcp" });
                framework.addChain("broken", FlowType.INBOUND, new String[] { "broken" });
                framework.addChainGroup("group1", new String[] { "chain1", "chain2", "chain3", "chain4", "chain5" });
                framework.addChainGroup("broken", new String[] { "chain1", "broken" });

                try {
                    framework.stopChainGroup("group1", -1);
                    fail("Incorrectly allowed invalid time");
                } catch (ChainTimerException e) {
                    // expected failure
                }

                // Put chain 1 in init state
                framework.initChain("chain1");
                // Put chain2 in started state
                framework.initChain("chain2");
                framework.startChain("chain2");
                // Put chain3 in quiesced state
                framework.initChain("chain3");
                framework.startChain("chain3");
                framework.stopChain("chain3", 50);
                // Put chain4 in stopped state.
                framework.initChain("chain4");
                framework.startChain("chain4");
                framework.stopChain("chain4", 0);
                // Note chain5 is not in runtime.
                changedChains = framework.stopChainGroup("group1", 100);
                assertEquals(1, changedChains.length);
                assertEquals("chain2", changedChains[0].getName());
                group = framework.getChainGroup("group1");
                assertNotNull(group);
                chain1 = framework.getRunningChain("chain1");
                chain2 = framework.getRunningChain("chain2");
                chain3 = framework.getRunningChain("chain3");
                chain4 = framework.getRunningChain("chain4");
                assertNull(framework.getRunningChain("chain5"));
                assertEquals(RuntimeState.INITIALIZED, chain1.getState());
                assertEquals(RuntimeState.QUIESCED, chain2.getState());
                assertEquals(RuntimeState.QUIESCED, chain3.getState());
                assertEquals(RuntimeState.INITIALIZED, chain4.getState());
                // State change happened. Now wait for quiesce to finish.
                try {
                    Thread.sleep(200);
                } catch (InterruptedException ie) {
                    // nothing
                }
                assertEquals(RuntimeState.INITIALIZED, chain1.getState());
                assertEquals(RuntimeState.INITIALIZED, chain1.getState());
                assertEquals(RuntimeState.INITIALIZED, chain1.getState());

                // Put chain1 in stopped state where it can be destroyed.
                framework.startChain("chain1");
                chain1 = framework.getRunningChain("chain1");
                // Put broken chain in stopped state, but note it will throw exception on quiesce.
                framework.startChain("broken");
                broken = framework.getRunningChain("broken");
                try {
                    changedChains = framework.stopChainGroup("broken", 50);
                    fail("Exception should be thrown.");
                } catch (ChainGroupException e) {
                    assertEquals(RuntimeState.QUIESCED, chain1.getState());
                    assertEquals(RuntimeState.STARTED, broken.getState());
                }
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
            outputMgr.failWithThrowable("testQuiesceGroup", t);
        }
    }

    /**
     * Test destroyGroup method.
     */
    @Test
    public void testDestroyGroup() {
        try {
            Chain chain1 = null;
            Chain chain2 = null;
            Chain chain3 = null;
            Chain chain4 = null;
            ChainData[] changedChains = null;
            Map<Object, Object> brokenMap = new HashMap<Object, Object>();
            brokenMap.put(BrokenChannel.HANDLE_INIT, BrokenChannel.SUCCEED);
            brokenMap.put(BrokenChannel.HANDLE_START, BrokenChannel.SUCCEED);
            brokenMap.put(BrokenChannel.HANDLE_QUIESCE, BrokenChannel.SUCCEED);
            brokenMap.put(BrokenChannel.HANDLE_STOP, BrokenChannel.SUCCEED);
            brokenMap.put(BrokenChannel.HANDLE_DESTROY, BrokenChannel.FAIL);

            ChannelFrameworkImpl framework = new ChannelFrameworkImpl();
            try {
                framework.addChannel("broken", BrokenChannelFactory.class, brokenMap);
                framework.addChannel("tcp", TCPChannelFactory.class, getTcpProps("GroupLifeCycleTest.testDestroyGroup", "7847"));
                framework.addChannel("proto", ProtocolDummyFactory.class, null);
                framework.addChain("chain1", FlowType.INBOUND, new String[] { "tcp", "proto" });
                framework.addChain("chain2", FlowType.INBOUND, new String[] { "tcp" });
                framework.addChain("chain3", FlowType.INBOUND, new String[] { "tcp" });
                framework.addChain("chain4", FlowType.INBOUND, new String[] { "tcp" });
                framework.addChain("broken", FlowType.INBOUND, new String[] { "broken" });
                framework.addChainGroup("group1", new String[] { "chain1", "chain2", "chain3" });
                framework.addChainGroup("broken", new String[] { "chain1", "broken" });

                try {
                    framework.destroyChainGroup(null);
                    fail("Incorrectly allowed null group");
                } catch (ChainGroupException e) {
                    // expected failure
                }

                try {
                    framework.destroyChainGroup("unknown");
                    fail("Incorrectly allowed unknow group");
                } catch (ChainGroupException e) {
                    // expected failured
                }

                framework.initChainGroup("group1");
                chain1 = framework.getRunningChain("chain1");
                chain2 = framework.getRunningChain("chain2");
                chain3 = framework.getRunningChain("chain3");
                changedChains = framework.destroyChainGroup("group1");
                assertEquals(3, changedChains.length);
                assertEquals("chain1", changedChains[0].getName());
                assertEquals("chain2", changedChains[1].getName());
                assertEquals("chain3", changedChains[2].getName());
                assertNotNull(framework.getChainGroup("group1"));
                assertEquals(RuntimeState.UNINITIALIZED, chain1.getState());
                assertEquals(RuntimeState.UNINITIALIZED, chain2.getState());
                assertEquals(RuntimeState.UNINITIALIZED, chain3.getState());
                assertEquals(0, framework.getNumRunningChains());

                // Put chain1 in quiesced state.
                framework.startChain("chain1");
                chain1 = framework.getRunningChain("chain1");
                framework.stopChain("chain1", 5000);
                // Put chain2 in started state
                framework.startChain("chain2");
                chain2 = framework.getRunningChain("chain2");
                // Put chain3 in stopped state
                framework.startChain("chain3");
                chain3 = framework.getRunningChain("chain3");
                framework.stopChain("chain3", 0);
                // Put chain4 in uninit state
                framework.startChain("chain4");
                chain4 = framework.getRunningChain("chain4");
                framework.stopChain("chain4", 0);
                framework.destroyChain("chain4");
                changedChains = framework.destroyChainGroup("group1");
                assertEquals(1, changedChains.length);
                assertEquals("chain3", changedChains[0].getName());
                assertEquals(RuntimeState.QUIESCED, chain1.getState());
                assertEquals(RuntimeState.STARTED, chain2.getState());
                assertEquals(RuntimeState.UNINITIALIZED, chain3.getState());
                assertEquals(RuntimeState.UNINITIALIZED, chain4.getState());

                // Put chain1 in stopped state where it can be destroyed.
                framework.stopChain("chain1", 0);
                framework.startChain("chain1");
                chain1 = framework.getRunningChain("chain1");
                framework.stopChain("chain1", 0);
                // Put broken chain in stopped state.
                // It should no longer throw an exception, but clean things up internally.
                framework.startChain("broken");
                framework.getRunningChain("broken");
                framework.stopChain("broken", 0);
                // Destroy the chain group.
                changedChains = framework.destroyChainGroup("broken");
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
            outputMgr.failWithThrowable("testDestroyGroup", t);
        }
    }
}
