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

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import test.common.SharedOutputManager;

import com.ibm.websphere.channelfw.ChainData;
import com.ibm.websphere.channelfw.ChainGroupData;
import com.ibm.websphere.channelfw.FlowType;
import com.ibm.ws.channelfw.internal.ChainDataImpl;
import com.ibm.ws.channelfw.internal.ChainGroupDataImpl;
import com.ibm.ws.channelfw.internal.ChannelFrameworkImpl;
import com.ibm.ws.channelfw.testsuite.channels.protocol.ProtocolDummyFactory;
import com.ibm.ws.tcpchannel.internal.TCPChannelFactory;
import com.ibm.wsspi.channelfw.ChainEventListener;
import com.ibm.wsspi.channelfw.exception.ChainException;
import com.ibm.wsspi.channelfw.exception.ChainGroupException;
import com.ibm.wsspi.channelfw.exception.ChannelException;
import com.ibm.wsspi.channelfw.exception.InvalidChainNameException;

/**
 * The purpose of this class is to test the chain group methods
 * in ChannelFrameworkImpl.
 */
@SuppressWarnings("unused")
public class GroupDataTest {
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
     * Test addChainGroup method.
     **/
    @Test
    public void testAddChainGroup() {
        try {
            ChainGroupData group = null;
            ChainData[] chainDataArray = null;
            ChannelFrameworkImpl framework = new ChannelFrameworkImpl();
            try {
                framework.addChannel("tcp", TCPChannelFactory.class, null, 45);
                framework.addChannel("proto", ProtocolDummyFactory.class, null, 10);
                framework.addChain("chain1", FlowType.INBOUND, new String[] { "tcp", "proto" });
                framework.addChain("chain2", FlowType.INBOUND, new String[] { "tcp" });
                framework.addChain("chain3", FlowType.INBOUND, new String[] { "proto" });
                framework.addChain("outbound", FlowType.OUTBOUND, new String[] { "proto", "tcp" });

                try {
                    group = framework.addChainGroup(null, new String[] { "chain1", "chain2" });
                    fail("Incorrectly allowed null name");
                } catch (ChainGroupException e) {
                    // expected failure
                }

                group = framework.addChainGroup("error", new String[] { "chain1", "outbound" });
                group = framework.addChainGroup("group1", new String[] { "chain1", "chain2" });
                assertNotNull(group);
                group = framework.getChainGroup("group1");
                assertNotNull(group);

                chainDataArray = group.getChains();
                assertEquals(2, chainDataArray.length);
                assertEquals("chain1", chainDataArray[0].getName());
                assertEquals("chain2", chainDataArray[1].getName());

                framework.addChainGroup("group2", new String[] { "chain1" });
                framework.addChainGroup("group3", new String[] { "chain2" });
                assertEquals(4, framework.getNumChainGroups());

                try {
                    group = framework.addChainGroup("invalid1", null);
                    fail("Incorrectly allowed allowed null chain list");
                } catch (InvalidChainNameException e) {
                    // expected failure
                }

                try {
                    group = framework.addChainGroup("invalid2", new String[] {});
                    fail("Incorrectly allowed empty chain list");
                } catch (InvalidChainNameException e) {
                    // expected failure
                }

                try {
                    group = framework.addChainGroup(
                                                    "invalid3", new String[] { "group1", "group2", "unknown" });
                    fail("Unable to handle nonexistent chain config in group");
                } catch (InvalidChainNameException e) {
                    // expected failure
                }

                int numGroups = framework.getNumChainGroups();
                framework.addChainGroup("group1", new String[] { "chain1", "chain3" });
                assertEquals(numGroups, framework.getNumChainGroups());
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
            outputMgr.failWithThrowable("testAddChainGroup", t);
        }
    }

    /**
     * Test removeChainGroup method.
     */
    @Test
    public void testRemoveChainGroup() {
        try {
            ChainGroupData group = null;
            ChainData[] chainDataArray = null;
            ChannelFrameworkImpl framework = new ChannelFrameworkImpl();
            try {
                framework.addChannel("tcp", TCPChannelFactory.class, null, 45);
                framework.addChannel("proto", ProtocolDummyFactory.class, null, 10);
                framework.addChain("chain1", FlowType.INBOUND, new String[] { "tcp", "proto" });
                framework.addChain("chain2", FlowType.INBOUND, new String[] { "tcp" });
                framework.addChainGroup("group1", new String[] { "chain1", "chain2" });
                group = framework.removeChainGroup("group1");
                assertNotNull(group);
                chainDataArray = group.getChains();
                assertEquals(2, chainDataArray.length);
                assertEquals("chain1", chainDataArray[0].getName());
                assertEquals("chain2", chainDataArray[1].getName());

                group = framework.getChainGroup("group1");
                assertNull(group);

                try {
                    group = framework.removeChainGroup("chain1");
                    fail("Unable to handle redundant remove / nonexistent group");
                } catch (ChainGroupException e) {
                    // expected failure
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
            outputMgr.failWithThrowable("testRemoveChainGroup", t);
        }
    }

    /**
     * Test updateChainGroup method.
     */
    @Test
    public void testUpdateChainGroup() {
        try {
            int numGroups = 0;
            ChainGroupData group = null;
            ChainData[] chainDataArray = null;
            ChannelFrameworkImpl framework = new ChannelFrameworkImpl();

            try {
                framework.addChannel("tcp", TCPChannelFactory.class, null, 45);
                framework.addChannel("proto", ProtocolDummyFactory.class, null, 10);
                framework.addChain("chain1", FlowType.INBOUND, new String[] { "tcp", "proto" });
                framework.addChain("chain2", FlowType.INBOUND, new String[] { "tcp" });
                framework.addChain("outbound", FlowType.OUTBOUND, new String[] { "proto", "tcp" });
                framework.addChainGroup("group1", new String[] { "chain1" });

                group = framework.updateChainGroup("group1", new String[] { "chain2" });
                assertNotNull(group);
                chainDataArray = group.getChains();
                assertEquals(1, chainDataArray.length);
                assertEquals("chain2", chainDataArray[0].getName());

                group = framework.getChainGroup("group1");
                assertNotNull(group);

                chainDataArray = group.getChains();
                assertEquals(1, chainDataArray.length);
                assertEquals("chain2", chainDataArray[0].getName());

                numGroups = framework.getNumChainGroups();
                framework.updateChainGroup("newGroup", new String[] { "chain1" });
                group = framework.getChainGroup("newGroup");
                assertNotNull(group);
                chainDataArray = group.getChains();
                assertEquals(1, chainDataArray.length);
                assertEquals((numGroups + 1), framework.getNumChainGroups());
                assertEquals("chain1", chainDataArray[0].getName());

                try {
                    group = framework.updateChainGroup(null, new String[] { "chain1", "chain2" });
                    fail("Incorrectly allowed null group");
                } catch (ChainGroupException e) {
                    // expected failure
                }

                group = framework.updateChainGroup("error", new String[] { "chain1", "outbound" });

                try {
                    group = framework.updateChainGroup("invalid1", null);
                    fail("Incorrectly allowed null chain list");
                } catch (InvalidChainNameException e) {
                    // expected failure
                }

                try {
                    group = framework.updateChainGroup("invalid2", new String[] {});
                    fail("Incorrectly allowed empty chain list");
                } catch (InvalidChainNameException e) {
                    // expected failure
                }

                try {
                    group = framework.updateChainGroup("invalid3", new String[] { "group1", "group2", "unknown" });
                    fail("Incorrectly allowed invalid chain name");
                } catch (InvalidChainNameException e) {
                    // expected failure
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
            outputMgr.failWithThrowable("testUpdateChainGroup", t);
        }
    }

    /**
     * Test getChainGroup method.
     */
    @Test
    public void testGetChainGroup() {
        try {
            ChainGroupData group = null;
            ChainData[] chainDataArray = null;
            ChannelFrameworkImpl framework = new ChannelFrameworkImpl();
            try {
                framework.addChannel("tcp", TCPChannelFactory.class, null, 45);
                framework.addChannel("proto", ProtocolDummyFactory.class, null, 10);
                framework.addChain("chain1", FlowType.INBOUND, new String[] { "tcp", "proto" });
                framework.addChain("chain2", FlowType.INBOUND, new String[] { "tcp" });
                group = framework.addChainGroup("group1", new String[] { "chain1" });
                assertNotNull(group);
                chainDataArray = group.getChains();
                assertEquals(1, chainDataArray.length);
                assertEquals("chain1", chainDataArray[0].getName());

                group = framework.getChainGroup("nonexistent");
                assertNull(group);
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
            outputMgr.failWithThrowable("testGetChainGroup", t);
        }
    }

    /**
     * Test addChainToGroup method.
     */
    @Test
    public void testAddChainToGroup() {
        try {
            ChainGroupData group = null;
            ChainGroupDataImpl groupImpl = null;
            ChainDataImpl chainImpl = null;
            ChainEventListener cel = null;
            ChannelFrameworkImpl framework = new ChannelFrameworkImpl();
            try {
                framework.addChannel("tcp", TCPChannelFactory.class, null, 45);
                framework.addChannel("proto", ProtocolDummyFactory.class, null, 10);
                framework.addChain("chain1", FlowType.INBOUND, new String[] { "tcp", "proto" });
                framework.addChain("chain2", FlowType.INBOUND, new String[] { "tcp" });
                framework.addChainGroup("group1", new String[] { "chain1" });
                framework.addChainGroup("group2", new String[] { "chain1", "chain2" });

                try {
                    framework.addChainToGroup("group1", null);
                    fail("Null chain not handled.");
                } catch (ChainException e) {
                    // expected failure
                }

                try {
                    framework.addChainToGroup(null, "chain2");
                    fail("Null group not handled.");
                } catch (ChainGroupException e) {
                    // expected failure
                }

                try {
                    framework.addChainToGroup("group1", "unknown");
                    fail("Unknown chain not handled.");
                } catch (ChainException e) {
                    // expected failure
                }

                try {
                    framework.addChainToGroup("unknown", "chain1");
                    fail("Unknown group not handled.");
                } catch (ChainGroupException e) {
                    // expected failure
                }

                try {
                    group = framework.addChainToGroup("group1", "chain1");
                    fail("Redundant chain add should not be allowed.");
                } catch (ChainException e) {
                    // expected failure
                }

                group = framework.addChainToGroup("group1", "chain2");
                assertTrue(group.containsChain("chain2"));
                assertTrue(group.containsChain("chain1"));
                assertEquals(2, group.getChains().length);

                framework.removeChainFromGroup("group1", "chain2");
                groupImpl = (ChainGroupDataImpl) framework.getChainGroup("group1");
                chainImpl = (ChainDataImpl) framework.getChain("chain2");
                // Add a new listener to the group.
                cel = new MyChainEventListener();
                framework.addGroupEventListener(cel, "group1");
                assertNotNull(groupImpl);
                assertNotNull(chainImpl);
                assertTrue(groupImpl.containsChainEventListener(cel));
                assertFalse(chainImpl.containsChainEventListener(cel));
                framework.addChainToGroup("group1", "chain2");
                // Check if the listener of the group was added to the chain.
                assertTrue(chainImpl.containsChainEventListener(cel));

                assertTrue(groupImpl.containsChain("chain2"));
                assertTrue(groupImpl.containsChain("chain1"));
                assertEquals(2, groupImpl.getChains().length);
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
            outputMgr.failWithThrowable("testAddChainToGroup", t);
        }
    }

    /**
     * Test removeChainFromGroup method.
     */
    @Test
    public void testRemoveChainFromGroup() {
        try {
            ChainGroupData group = null;
            ChainGroupDataImpl groupImpl = null;
            ChainEventListener cel = null;
            ChainDataImpl chainImpl = null;
            ChannelFrameworkImpl framework = new ChannelFrameworkImpl();

            try {
                framework.addChannel("tcp", TCPChannelFactory.class, null, 45);
                framework.addChannel("proto", ProtocolDummyFactory.class, null, 10);
                framework.addChain("chain1", FlowType.INBOUND, new String[] { "tcp", "proto" });
                framework.addChain("chain2", FlowType.INBOUND, new String[] { "tcp" });
                framework.addChainGroup("group1", new String[] { "chain1" });
                framework.addChainGroup("group2", new String[] { "chain1", "chain2" });

                try {
                    framework.removeChainFromGroup("group1", null);
                    fail("Incorrectly allowed null chain");
                } catch (ChainException e) {
                    // expected failure
                }

                try {
                    framework.removeChainFromGroup(null, "chain1");
                    fail("Incorrectly allowed null group");
                } catch (ChainGroupException e) {
                    // expected failure
                }

                try {
                    framework.removeChainFromGroup("group1", "unknown");
                    fail("Incorrectly allowed unknown chain");
                } catch (ChainException e) {
                    // expected failure
                }

                try {
                    framework.removeChainFromGroup("unknown", "chain1");
                    fail("Incorrectly removed unknown group");
                } catch (ChainGroupException e) {
                    // expected failure
                }

                try {
                    framework.removeChainFromGroup("group1", "chain2");
                    fail("Incorrectly allowed missing chain");
                } catch (ChainException e) {
                    // expected failure
                }

                group = framework.removeChainFromGroup("group1", "chain1");
                assertFalse(group.containsChain("chain1"));
                assertEquals(0, group.getChains().length);

                // Undo previous test removeChainFromGroup.
                groupImpl = (ChainGroupDataImpl) framework.addChainToGroup("group1", "chain1");
                chainImpl = (ChainDataImpl) framework.getChain("chain1");
                // Add the listener to the group.
                cel = new MyChainEventListener();
                framework.addGroupEventListener(cel, "group1");
                // Verify test setup.
                assertNotNull(groupImpl);
                assertNotNull(chainImpl);
                assertTrue(groupImpl.containsChainEventListener(cel));
                assertTrue(chainImpl.containsChainEventListener(cel));
                // Remove the chain and the listener should be disassociated with the chain.
                framework.removeChainFromGroup("group1", "chain1");
                assertFalse(groupImpl.containsChain("chain1"));
                assertTrue(groupImpl.containsChainEventListener(cel));
                assertFalse(chainImpl.containsChainEventListener(cel));

                chainImpl = (ChainDataImpl) framework.getChain("chain1");
                // Ensure the chain is in both groups.
                groupImpl = (ChainGroupDataImpl) framework.getChainGroup("group1");
                if (!groupImpl.containsChain("chain1")) {
                    framework.addChainToGroup("group1", "chain1");
                }
                ChainGroupDataImpl groupImpl2 = (ChainGroupDataImpl) framework.getChainGroup("group2");
                if (!groupImpl2.containsChain("chain1")) {
                    framework.addChainToGroup("group2", "chain1");
                }
                // Add the listener to both groups.
                cel = new MyChainEventListener();
                framework.addGroupEventListener(cel, "group1");
                framework.addGroupEventListener(cel, "group2");
                assertNotNull(groupImpl);
                assertNotNull(groupImpl2);
                assertNotNull(chainImpl);
                assertTrue(groupImpl.containsChainEventListener(cel));
                assertTrue(groupImpl2.containsChainEventListener(cel));
                assertTrue(chainImpl.containsChainEventListener(cel));
                // Remove the chain and from group1. The listener should be associated with the chain (from group2)
                framework.removeChainFromGroup("group1", "chain1");
                assertFalse(groupImpl.containsChain("chain1"));
                assertTrue(groupImpl.containsChainEventListener(cel));
                assertTrue(groupImpl.containsChainEventListener(cel));
                assertTrue(chainImpl.containsChainEventListener(cel));
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
            outputMgr.failWithThrowable("testRemoveChainFromGroup", t);
        }
    }

    /**
     * Test getAllChainGroups method.
     */
    @Test
    public void testGetAllChainGroups() {
        try {
            ChainGroupData[] groupArray = null;
            ChannelFrameworkImpl framework = new ChannelFrameworkImpl();

            try {
                framework.addChannel("tcp", TCPChannelFactory.class, null, 45);
                framework.addChannel("proto", ProtocolDummyFactory.class, null, 10);
                framework.addChain("chain1", FlowType.INBOUND, new String[] { "tcp", "proto" });
                framework.addChain("chain2", FlowType.INBOUND, new String[] { "tcp" });

                groupArray = framework.getAllChainGroups();
                assertNotNull(groupArray);
                assertEquals(0, groupArray.length);

                framework.addChainGroup("group1", new String[] { "chain1" });
                groupArray = framework.getAllChainGroups();
                assertNotNull(groupArray);
                assertEquals(1, groupArray.length);
                assertEquals("group1", groupArray[0].getName());

                framework.addChainGroup("group2", new String[] { "chain1", "chain2" });
                groupArray = framework.getAllChainGroups();
                assertEquals(2, groupArray.length);

                try {
                    framework.getAllChainGroups(null);
                    fail("Incorrectly allowed null chain name");
                } catch (ChainException e) {
                    // expected failure
                }

                try {
                    framework.getAllChainGroups("unknown");
                    fail("Incorrectly allowed unexpected chain name");
                } catch (ChainException e) {
                    // expected failure
                }

                framework.addChain("chain3", FlowType.INBOUND, new String[] { "tcp" });
                groupArray = framework.getAllChainGroups("chain3");
                assertEquals(0, groupArray.length);
                groupArray = framework.getAllChainGroups("chain2");
                assertEquals(1, groupArray.length);
                assertEquals("group2", groupArray[0].getName());
                groupArray = framework.getAllChainGroups("chain1");
                assertEquals(2, groupArray.length);
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
            outputMgr.failWithThrowable("testGetAllChainGroups", t);
        }
    }

    /**
     * Utility class for testing.
     */
    public class MyChainEventListener implements ChainEventListener {
        public void chainInitialized(ChainData chainData) {
            // nothing
        }

        public void chainStarted(ChainData chainData) {
            // nothing
        }

        public void chainStopped(ChainData chainData) {
            // nothing
        }

        public void chainQuiesced(ChainData chainData) {
            // nothing
        }

        public void chainDestroyed(ChainData chainData) {
            // nothing
        }

        public void chainUpdated(ChainData chainData) {
            // nothing
        }
    }

}
