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
import com.ibm.websphere.channelfw.ChannelData;
import com.ibm.websphere.channelfw.FlowType;
import com.ibm.websphere.channelfw.RegionType;
import com.ibm.ws.channelfw.internal.ChainDataImpl;
import com.ibm.ws.channelfw.internal.ChannelDataImpl;
import com.ibm.ws.channelfw.internal.ChannelFrameworkConstants;
import com.ibm.ws.channelfw.internal.ChannelFrameworkImpl;
import com.ibm.ws.channelfw.testsuite.channels.outbound.GetterFactory;
import com.ibm.ws.channelfw.testsuite.channels.outbound.OutboundDummyFactory2;
import com.ibm.ws.channelfw.testsuite.channels.protocol.PassThruFactory;
import com.ibm.ws.channelfw.testsuite.channels.protocol.ProtocolDummyFactory;
import com.ibm.ws.channelfw.testsuite.channels.server.AppDummyFactory;
import com.ibm.ws.channelfw.testsuite.channels.server.AppDummyFactory2;
import com.ibm.ws.channelfw.testsuite.channels.server.WebDummyFactory;
import com.ibm.ws.channelfw.testsuite.channels.server.Z1AppChannelFactory;
import com.ibm.ws.channelfw.testsuite.channels.server.Z2AppChannelFactory;
import com.ibm.ws.tcpchannel.internal.TCPChannelFactory;
import com.ibm.wsspi.channelfw.BoundRegion;
import com.ibm.wsspi.channelfw.exception.ChainException;
import com.ibm.wsspi.channelfw.exception.ChainGroupException;
import com.ibm.wsspi.channelfw.exception.ChannelException;
import com.ibm.wsspi.channelfw.exception.IncoherentChainException;
import com.ibm.wsspi.channelfw.exception.InvalidChainNameException;
import com.ibm.wsspi.channelfw.exception.InvalidChannelFactoryException;
import com.ibm.wsspi.channelfw.exception.InvalidChannelNameException;

import test.common.SharedOutputManager;

/**
 * Chain runtime configuration tests.
 */
public class ChainDataTest {
    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    @Rule
    public TestRule managerRule = outputMgr;

    // Helper method to create valid tcp props to start a chain with TCP in it.
    protected Map<Object, Object> getTcpProps(String portProp, String defaultPort) {
        String sysPropPort = System.getProperty(portProp, defaultPort);
        Map<Object, Object> tcpMap = new HashMap<Object, Object>();
        tcpMap.put("port", sysPropPort);
        return tcpMap;
    }

    /**
     * Test addChain method.
     */
    @Test
    public void testAddChain() {
        try {
            ChainData chainData = null;
            ChainData chainData2 = null;
            ChannelFrameworkImplFakeZOS framework = new ChannelFrameworkImplFakeZOS();

            try {
                framework.addChannel("tcp", TCPChannelFactory.class, getTcpProps("testAddChain.tcpPort", "15000"), 45);
                framework.addChannel("app", AppDummyFactory.class, null, 10);
                framework.addChannel("out2", OutboundDummyFactory2.class, null, 10);
                framework.addChannel("getter", GetterFactory.class, null, 10);
                framework.addChannel("proto", ProtocolDummyFactory.class, null, 10);
                framework.addChannel("app2", AppDummyFactory2.class, null, 10);
                framework.addChannel("Z1", Z1AppChannelFactory.class, null, 10);
                framework.addChannel("Z2", Z2AppChannelFactory.class, null, 10);
                chainData = framework.addChain("chain1", FlowType.INBOUND, new String[] { "tcp", "proto" });
                assertNotNull(chainData);
                chainData = framework.getChain("chain1");
                assertNotNull(chainData);
                assertEquals("chain1", chainData.getName());
                assertEquals(2, chainData.getChannelList().length);
                assertEquals("tcp", chainData.getChannelList()[0].getName());
                assertEquals("proto", chainData.getChannelList()[1].getName());

                framework.addChain("chain2", FlowType.OUTBOUND, new String[] { "proto", "tcp" });
                framework.addChain("chain3", FlowType.OUTBOUND, new String[] { "tcp" });
                assertEquals(3, framework.getNumChains());

                chainData2 = framework.getChain("chain2");
                assertEquals(FlowType.INBOUND, chainData.getType());
                assertEquals(FlowType.OUTBOUND, chainData2.getType());

                try {
                    chainData = framework.addChain("invalid1", FlowType.INBOUND, null);
                    fail("Incorrectly allowed null channel list");
                } catch (InvalidChannelNameException e) {
                    // expected failure
                }

                try {
                    chainData = framework.addChain("invalid2", FlowType.INBOUND, new String[] {});
                    fail("Incorrectly allowed empty list");
                } catch (InvalidChannelNameException e) {
                    // expected failure
                }

                try {
                    chainData = framework.addChain("invalid3", FlowType.INBOUND, new String[] { "tcp", "unknown" });
                    fail("Incorrectly allowed unknown channel");
                } catch (InvalidChannelNameException e) {
                    // expected failure
                }

                try {
                    framework.addChain("chain1", FlowType.INBOUND, new String[] { "tcp", "proto" });
                    fail("Incorrectly allowed 2nd add");
                } catch (InvalidChainNameException e) {
                    // expected failure
                }

                chainData = framework.addChain("in_1_good", FlowType.INBOUND, new String[] { "tcp" });
                assertNotNull(chainData);
                chainData = framework.addChain("in_2_good", FlowType.INBOUND, new String[] { "tcp", "proto" });
                assertNotNull(chainData);
                chainData = framework.addChain("in_3_good", FlowType.INBOUND,
                                               new String[] { "tcp", "proto", "app2" });
                assertNotNull(chainData);

                try {
                    chainData = framework.addChain("in_2_bad", FlowType.INBOUND,
                                                   new String[] { "app2", "proto" });
                    fail("Incorrectly allowed incoherent chain");
                } catch (IncoherentChainException e) {
                    // expected failure
                } catch (IllegalStateException e) {
                    // expected failure
                }

                try {
                    chainData = framework.addChain("in_3_bad", FlowType.INBOUND,
                                                   new String[] { "proto", "app", "tcp" });
                    fail("Incorrectly allowed incoherent chain");
                } catch (IncoherentChainException e) {
                    // expected failure
                } catch (IllegalStateException e) {
                    // expected failure
                }

                chainData = framework.addChain("out_1_good", FlowType.OUTBOUND, new String[] { "tcp" });
                assertNotNull(chainData);
                chainData = framework.addChain("out_2_good", FlowType.OUTBOUND,
                                               new String[] { "proto", "tcp" });
                assertNotNull(chainData);
                chainData = framework.addChain("out_3_good", FlowType.OUTBOUND,
                                               new String[] { "getter", "out2", "tcp" });
                assertNotNull(chainData);

                try {
                    chainData = framework.addChain("out_2_bad", FlowType.OUTBOUND,
                                                   new String[] { "tcp", "proto" });
                    fail("Incoherent chain found to be coherent");
                } catch (IncoherentChainException e) {
                    // expected failure
                } catch (IllegalStateException e) {
                    // expected failure
                }

                try {
                    chainData = framework.addChain("out_3_bad", FlowType.OUTBOUND,
                                                   new String[] { "proto", "tcp", "app2" });
                    fail("Incoherent chain found to be coherent");
                } catch (IncoherentChainException e) {
                    // expected failure
                } catch (IllegalStateException e) {
                    // expected failure
                }

                framework.setOnZ(true);
                framework.setCurrentRegion(RegionType.SR_REGION);

                chainData = framework.addChain("chainZ1-1", FlowType.INBOUND, new String[] { "tcp", "Z1" });
                assertNotNull(chainData);
                chainData = framework.getChain("chainZ1-1");
                assertNotNull(chainData);
                assertEquals("chainZ1-1", chainData.getName());
                chainData = framework.addChain("chainZ2-1", FlowType.INBOUND, new String[] { "tcp", "Z2" });
                assertNull(chainData);

                framework.setCurrentRegion(BoundRegion.CR_REGION);
                chainData = framework.addChain("chainZ2-2", FlowType.INBOUND, new String[] { "tcp", "Z2" });
                assertNull(chainData);
                chainData = framework.addChain("chainZ1-2", FlowType.INBOUND, new String[] { "tcp", "Z1" });
                assertNotNull(chainData);
                chainData = framework.getChain("chainZ1-2");
                assertNotNull(chainData);
                assertEquals("chainZ1-2", chainData.getName());

                framework.setCurrentRegion(BoundRegion.CRA_REGION);
                chainData = framework.addChain("chainZ2-3", FlowType.INBOUND, new String[] { "tcp", "Z2" });
                assertNotNull(chainData);
                // How to verify that the above logged a warning about a non-sharable channel
                // running in both CR and CRA
                chainData = framework.getChain("chainZ2-3");
                assertNotNull(chainData);
                assertEquals("chainZ2-3", chainData.getName());
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
            outputMgr.failWithThrowable("testAddChain", t);
        }
    }

    /**
     * Test removeChain method.
     */
    @Test
    public void testRemoveChain() {
        try {
            ChainData chainData = null;
            ChannelFrameworkImpl framework = new ChannelFrameworkImpl();

            try {
                framework.addChannel("tcp", TCPChannelFactory.class, getTcpProps("testRemoveChain.tcpPort", "15001"), 45);
                framework.addChannel("proto", ProtocolDummyFactory.class, null, 33);
                framework.addChannel("app", AppDummyFactory.class, null, 10);
                chainData = framework.addChain("chain1", FlowType.INBOUND, new String[] { "tcp", "proto" });
                chainData = framework.removeChain("chain1");
                assertNotNull(chainData);
                assertEquals(FlowType.INBOUND, chainData.getType());

                chainData = framework.getChain("chain1");
                assertNull(chainData);
                assertEquals(0, framework.getNumChains());

                try {
                    chainData = framework.removeChain("chain1");
                    if (chainData != null)
                        fail("Incorrectly allowed 2nd remove");
                } catch (InvalidChainNameException e) {
                    // expected failure
                }

                framework.addChain("chain1", FlowType.INBOUND, new String[] { "tcp", "proto" });
                framework.initChain("chain1");
                try {
                    chainData = framework.removeChain("chain1");
                    if (chainData != null)
                        fail("Incorrectly allowed remove on inited chain");
                } catch (ChainException e) {
                    // expected failure
                }

                framework.addChainGroup("group1", new String[] { "chain1" });
                framework.destroyChain("chain1");
                framework.removeChain("chain1");
                assertEquals(0, framework.getChainGroup("group1").getChains().length);

                ChainData chainDataArray[] = framework.getChainGroup("group1").getChains();
                assertNotNull(chainDataArray);
                assertEquals(1, framework.getNumChainGroups());
                framework.removeChainGroup("group1");

                chainData = framework.addChain("chain1", FlowType.INBOUND, new String[] { "tcp", "proto" });
                chainData = framework.addChain("chain2", FlowType.INBOUND, new String[] { "tcp", "proto" });
                chainData = framework.addChain("chain3", FlowType.INBOUND, new String[] { "tcp", "proto" });
                framework.addChainGroup("group1", new String[] { "chain1", "chain2" });
                framework.addChainGroup("group2", new String[] { "chain1" });
                framework.addChainGroup("group3", new String[] { "chain1", "chain2", "chain3" });
                framework.removeChain("chain1");
                assertEquals(1, framework.getChainGroup("group1").getChains().length);
                assertEquals(0, framework.getChainGroup("group2").getChains().length);
                assertEquals(2, framework.getChainGroup("group3").getChains().length);
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
            outputMgr.failWithThrowable("testRemoveChain", t);
        }
    }

    private void setupUpdateTest(ChannelFrameworkImpl framework) throws ChannelException, ChainException, ChainGroupException {
        framework.clear();
        framework.addChannel("tcp", TCPChannelFactory.class, getTcpProps("setupUpdateTest.tcpPort", "15002"), 45);
        framework.addChannel("proto", ProtocolDummyFactory.class, null, 33);
        framework.addChannel("web", WebDummyFactory.class, null, 10);
        framework.addChannel("passthru", PassThruFactory.class, null, 10);
        Map<Object, Object> propBag = new HashMap<Object, Object>();
        propBag.put("changeSlashes", "false");
        propBag.put("compressBody", "false");
        framework.addChannel("app2", AppDummyFactory2.class, propBag, 10);
        // Setup chains of 1 channel.
        framework.addChain("tcp", FlowType.INBOUND, new String[] { "tcp" });
        framework.addChain("proto", FlowType.INBOUND, new String[] { "proto" });
        framework.addChain("web", FlowType.INBOUND, new String[] { "web" });
        framework.addChain("app2", FlowType.INBOUND, new String[] { "app2" });
        // Setup chains of 2 channels.
        framework.addChain("tcp-proto", FlowType.INBOUND, new String[] { "tcp", "proto" });
        framework.addChain("proto-web", FlowType.INBOUND, new String[] { "proto", "web" });
        framework.addChain("proto-app2", FlowType.INBOUND, new String[] { "proto", "app2" });
        // Setup chains of 3 channels.
        framework.addChain("tcp-proto-web", FlowType.INBOUND, new String[] { "tcp", "proto", "web" });
        framework.addChain("tcp-passthru-proto", FlowType.INBOUND, new String[] { "tcp", "passthru", "proto" });
        framework.addChain("tcp-proto-app2", FlowType.INBOUND, new String[] { "tcp", "proto", "app2" });
    }

    private void runUpdateTest(ChannelFrameworkImpl framework, String chainName,
                               String[] newChainChannels) throws ChannelException, ChainException, ChainGroupException {
        setupUpdateTest(framework);
        framework.updateChain(chainName, newChainChannels);
        ChainData chainData = framework.getChain(chainName);
        ChannelData[] channelDataArray = chainData.getChannelList();
        assertNotNull(chainData);
        assertEquals(newChainChannels.length, channelDataArray.length);
        assertEquals(newChainChannels[0], channelDataArray[0].getName());
    }

    /**
     * Test updateChain method.
     */
    @Test
    public void testUpdateChain() {
        try {
            ChannelFrameworkImpl framework = new ChannelFrameworkImpl();
            try {
                setupUpdateTest(framework);

                try {
                    framework.updateChain(null, new String[] { "proto" });
                    fail("Incorrectly allowed null name");
                } catch (InvalidChainNameException e) {
                    // expected failure
                }

                try {
                    framework.updateChain("proto", null);
                    fail("Incorrectly allowed null list");
                } catch (InvalidChannelNameException e) {
                    // expected failure
                }

                try {
                    framework.updateChain("proto", new String[] {});
                    fail("Incorrectly allowed empty list");
                } catch (InvalidChannelNameException e) {
                    // expected failure
                }

                try {
                    framework.updateChain("unknown", new String[] { "tcp" });
                    fail("Incorrectly allowed unknown name");
                } catch (InvalidChainNameException e) {
                    // expected failure
                }

                try {
                    framework.updateChain("proto", new String[] { "tcp", "unknown" });
                    fail("Incorrectly allowed unknown channel in list");
                } catch (InvalidChannelNameException e) {
                    // expected failure
                }

                setupUpdateTest(framework);

                // try {
                // framework.updateChain("proto", new String[] { "proto" });
                // fail("Unable to restrict identical chain");
                // } catch (InvalidChannelNameException e) {
                // // expected failure
                // }
                //
                // try {
                // framework.updateChain("tcp-proto", new String[] { "tcp", "proto" });
                // fail("Unable to restrict identical chain");
                // } catch (InvalidChannelNameException e) {
                // // expected failure
                // }
                //
                // try {
                // framework.updateChain("tcp-proto-web", new String[] { "tcp", "proto", "web" });
                // fail("Unable to restrict identical chain");
                // } catch (InvalidChannelNameException e) {
                // // expected failure
                // }

                try {
                    framework.updateChain("tcp", new String[] { "proto", "tcp" });
                    fail("Unable to handle incoherent chain");
                } catch (IncoherentChainException e) {
                    // expected failure
                } catch (IllegalStateException e) {
                    // expected failure
                }

                try {
                    framework.updateChain("tcp", new String[] { "proto", "web", "tcp" });
                    fail("Unable to handle incoherent chain");
                } catch (IncoherentChainException e) {
                    // expected failure
                } catch (IllegalStateException e) {
                    // expected failure
                }

                runUpdateTest(framework, "proto", new String[] { "web" });
                runUpdateTest(framework, "web", new String[] { "tcp", "proto" });
                runUpdateTest(framework, "proto", new String[] { "tcp", "proto" });
                runUpdateTest(framework, "tcp", new String[] { "tcp", "proto" });
                runUpdateTest(framework, "tcp", new String[] { "tcp", "proto", "web" });
                runUpdateTest(framework, "proto", new String[] { "tcp", "proto", "web" });
                runUpdateTest(framework, "web", new String[] { "tcp", "proto", "web" });

                runUpdateTest(framework, "tcp-proto", new String[] { "proto" });
                runUpdateTest(framework, "tcp-proto", new String[] { "tcp" });
                runUpdateTest(framework, "tcp-proto", new String[] { "proto", "web" });
                runUpdateTest(framework, "proto-web", new String[] { "tcp", "proto" });
                runUpdateTest(framework, "tcp-proto", new String[] { "web" });
                runUpdateTest(framework, "tcp-proto", new String[] { "passthru", "proto" });
                runUpdateTest(framework, "proto-web", new String[] { "proto", "app2" });
                runUpdateTest(framework, "tcp-proto", new String[] { "tcp", "proto", "web" });
                runUpdateTest(framework, "proto-web", new String[] { "tcp", "proto", "web" });
                runUpdateTest(framework, "tcp-proto", new String[] { "tcp", "passthru", "proto" });

                runUpdateTest(framework, "tcp-proto-web", new String[] { "proto", "web" });
                runUpdateTest(framework, "tcp-passthru-proto", new String[] { "tcp", "proto" });
                runUpdateTest(framework, "tcp-proto-web", new String[] { "tcp", "proto" });
                runUpdateTest(framework, "tcp-proto-web", new String[] { "app2" });
                runUpdateTest(framework, "tcp-proto-web", new String[] { "tcp" });
                runUpdateTest(framework, "tcp-proto-web", new String[] { "proto" });
                runUpdateTest(framework, "tcp-proto-web", new String[] { "web" });

                framework.addChannel("tcp_channel", TCPChannelFactory.class, getTcpProps("testUpdateChain.newTcpPort", "15003"));
                framework.addChannel("proto_channel", ProtocolDummyFactory.class, null, 33);
                framework.addChain("chain_test", FlowType.INBOUND, new String[] { "tcp_channel" });
                ChainGroupData groupData = framework.addChainGroup("group", new String[] { "chain_test" });
                assertNotNull(groupData);
                assertEquals(1, groupData.getChains()[0].getChannelList().length);
                assertEquals("tcp_channel", groupData.getChains()[0].getChannelList()[0].getName());
                framework.updateChain("chain_test",
                                      new String[] { "tcp_channel", "proto_channel" });
                groupData = framework.getChainGroup("group");
                assertEquals(2, groupData.getChains()[0].getChannelList().length);
                assertEquals("tcp_channel", groupData.getChains()[0].getChannelList()[0].getName());
                assertEquals("proto_channel", groupData.getChains()[0].getChannelList()[1].getName());

                framework.startChain("tcp");
                try {
                    framework.updateChain("tcp", new String[] { "proto" });
                    fail("Should not have been able to update a live chain config.");
                } catch (ChainException e) {
                    // expected failure
                }

                Map<Object, Object> properties = null;
                Map<Object, Object> tcpMap = new HashMap<Object, Object>();
                String additionalPort = System.getProperty("testUpdateChain.extraTcpPort", "50008");
                tcpMap.put(ChannelFrameworkConstants.PORT, additionalPort);
                tcpMap.put(ChannelFrameworkConstants.HOST_NAME, "localhost");
                framework.addChannel("tcp_update", TCPChannelFactory.class, tcpMap, 45);
                framework.addChannel("proto_update", ProtocolDummyFactory.class, null, 33);
                // Setup chains of 1 channel.
                ChainDataImpl chain = (ChainDataImpl) framework.addChain("tcp_update_chain", FlowType.INBOUND,
                                                                         new String[] { "tcp_update" });
                assertNotNull(chain);
                properties = chain.getPropertyBag();
                assertEquals(additionalPort, properties.get(ChannelFrameworkConstants.PORT));
                assertEquals("localhost", properties.get(ChannelFrameworkConstants.HOST_NAME));
                framework.startChain("tcp_update_chain");
                chain = (ChainDataImpl) framework.getChain("tcp_update_chain");
                properties = chain.getPropertyBag();
                assertEquals(additionalPort, properties.get(ChannelFrameworkConstants.PORT));
                assertEquals("localhost", properties.get(ChannelFrameworkConstants.HOST_NAME));
                framework.stopChain("tcp_update_chain", 0);
                framework.destroyChain("tcp_update_chain");
                chain = (ChainDataImpl) framework.updateChain("tcp_update_chain",
                                                              new String[] { "tcp_update", "proto_update" });
                properties = chain.getPropertyBag();
                assertEquals(additionalPort, properties.get(ChannelFrameworkConstants.PORT));
                assertEquals("localhost", properties.get(ChannelFrameworkConstants.HOST_NAME));
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
            outputMgr.failWithThrowable("testUpdateChain", t);
        }
    }

    /**
     * Test getChain method.
     */
    @Test
    public void testGetChain() {
        try {
            ChannelFrameworkImpl framework = new ChannelFrameworkImpl();
            try {
                framework.addChannel("tcp", TCPChannelFactory.class, getTcpProps("testGetChain.tcpPort", "15004"), 45);
                framework.addChannel("proto", ProtocolDummyFactory.class, null, 33);
                framework.addChannel("app", AppDummyFactory.class, null, 10);
                framework.addChain("chain1", FlowType.INBOUND, new String[] { "tcp", "proto" });
                assertNotNull(framework.getChain("chain1"));
                assertNull(framework.getChain("nonexistent"));
                assertNull(framework.getChain("chain"));
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
            outputMgr.failWithThrowable("testGetChain", t);
        }
    }

    /**
     * Test getAllChains methods.
     */
    @Test
    public void testGetAllChains() {
        try {
            ChainData chainDataArray[] = null;
            ChannelFrameworkImpl framework = new ChannelFrameworkImpl();

            try {
                chainDataArray = framework.getAllChains();
                assertEquals(0, chainDataArray.length);

                framework.addChannel("tcp", TCPChannelFactory.class, getTcpProps("testGetAllChains.tcpPort", "15005"), 45);
                framework.addChannel("proto", ProtocolDummyFactory.class, null, 10);
                framework.addChannel("app", AppDummyFactory.class, null, 10);
                framework.addChain("tcp-proto-app", FlowType.INBOUND, new String[] { "tcp", "proto", "app" });
                chainDataArray = framework.getAllChains();
                assertEquals(1, chainDataArray.length);
                assertEquals("tcp-proto-app", chainDataArray[0].getName());

                framework.addChannel("transp", PassThruFactory.class, null, 10);
                framework.addChain("tcp-transp-proto", FlowType.INBOUND, new String[] { "tcp", "transp", "proto" });
                chainDataArray = framework.getAllChains();
                assertEquals(2, chainDataArray.length);

                try {
                    chainDataArray = framework.getAllChains((String) null);
                    fail("Incorrectly allowed null name");
                } catch (InvalidChannelNameException e) {
                    // expected failure
                }

                framework.addChannel("app2", AppDummyFactory2.class, null, 10);
                chainDataArray = framework.getAllChains("app2");
                assertEquals(0, chainDataArray.length);

                chainDataArray = framework.getAllChains("transp");
                assertEquals(1, chainDataArray.length);
                assertEquals("tcp-transp-proto", chainDataArray[0].getName());

                chainDataArray = framework.getAllChains("tcp");
                assertEquals(2, chainDataArray.length);

                try {
                    chainDataArray = framework.getAllChains((Class<?>) null);
                    fail("Incorrectly allowed null class");
                } catch (InvalidChannelFactoryException e) {
                    // expected failure
                }

                chainDataArray = framework.getAllChains(OutboundDummyFactory2.class);
                assertEquals(0, chainDataArray.length);

                chainDataArray = framework.getAllChains(PassThruFactory.class);
                assertEquals(1, chainDataArray.length);
                assertEquals("tcp-transp-proto", chainDataArray[0].getName());

                chainDataArray = framework.getAllChains(TCPChannelFactory.class);
                assertEquals(2, chainDataArray.length);
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
            outputMgr.failWithThrowable("testGetAllChains", t);
        }
    }

    /**
     * Test getRunningChains methods.
     */
    @Test
    public void testGetRunningChains() {
        try {
            ChainData chainDataArray[] = null;
            ChannelFrameworkImpl framework = new ChannelFrameworkImpl();

            try {
                chainDataArray = framework.getRunningChains();
                assertEquals(0, chainDataArray.length);

                framework.addChannel("tcp", TCPChannelFactory.class, getTcpProps("testGetRunningChains.tcpPort", "15006"), 45);
                framework.addChannel("proto", ProtocolDummyFactory.class, null, 10);
                framework.addChannel("app", AppDummyFactory.class, null, 10);
                framework.addChain("tcp-proto-app", FlowType.INBOUND, new String[] { "tcp", "proto", "app" });
                framework.initChain("tcp-proto-app");
                chainDataArray = framework.getRunningChains();
                assertEquals(1, chainDataArray.length);
                assertEquals("tcp-proto-app", chainDataArray[0].getName());

                framework.addChannel("transp", PassThruFactory.class, null, 10);
                framework.addChain("tcp-transp", FlowType.INBOUND, new String[] { "tcp", "transp" });
                framework.initChain("tcp-transp");
                chainDataArray = framework.getRunningChains();
                assertEquals(2, chainDataArray.length);

                try {
                    chainDataArray = framework.getRunningChains((String) null);
                    fail("Incorrectly allowed null name");
                } catch (InvalidChannelNameException e) {
                    // expected failure
                }

                framework.addChannel("app2", AppDummyFactory2.class, null, 10);
                chainDataArray = framework.getRunningChains("app2");
                assertEquals(0, chainDataArray.length);

                try {
                    chainDataArray = framework.getRunningChains("nonexistent");
                    fail("Channel doesn't exist.  Should have thrown exception.");
                } catch (InvalidChannelNameException e) {
                    // expected failure
                }

                chainDataArray = framework.getRunningChains("transp");
                assertEquals(1, chainDataArray.length);
                assertEquals("tcp-transp", chainDataArray[0].getName());

                chainDataArray = framework.getRunningChains("tcp");
                assertEquals(2, chainDataArray.length);

                try {
                    chainDataArray = framework.getRunningChains((Class<?>) null);
                    fail("Incorrectly allowed null class");
                } catch (InvalidChannelFactoryException e) {
                    // expected failure
                }

                chainDataArray = framework.getRunningChains(OutboundDummyFactory2.class);
                assertEquals(0, chainDataArray.length);

                chainDataArray = framework.getRunningChains(PassThruFactory.class);
                assertEquals(1, chainDataArray.length);
                assertEquals("tcp-transp", chainDataArray[0].getName());

                chainDataArray = framework.getRunningChains(TCPChannelFactory.class);
                assertEquals(2, chainDataArray.length);
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
            outputMgr.failWithThrowable("testGetRunningChains", t);
        }
    }

    /**
     * Test getInternalRunningChains methods.
     */
    @Test
    public void testGetInternalRunningChains() {
        try {
            ChainData chainDataArray[] = null;
            String channelSuffix = ChannelDataImpl.CHILD_STRING + "0";
            ChannelFrameworkImpl framework = new ChannelFrameworkImpl();

            try {
                framework.addChannel("tcp", TCPChannelFactory.class, getTcpProps("testGetInternalRunningChains.tcpPort", "15007"), 45);
                framework.addChannel("proto", ProtocolDummyFactory.class, null, 10);
                framework.addChannel("app", AppDummyFactory.class, null, 10);
                framework.addChain("tcp-proto-app", FlowType.INBOUND, new String[] { "tcp", "proto", "app" });
                framework.initChain("tcp-proto-app");
                chainDataArray = framework.getInternalRunningChains("tcp" + channelSuffix);
                assertEquals(1, chainDataArray.length);
                assertEquals("tcp-proto-app", chainDataArray[0].getName());

                framework.addChannel("transp", PassThruFactory.class, null, 10);
                framework.addChain("tcp-transp", FlowType.INBOUND, new String[] { "tcp", "transp" });
                framework.initChain("tcp-transp");
                chainDataArray = framework.getInternalRunningChains("tcp" + channelSuffix);
                assertEquals(2, chainDataArray.length);

                try {
                    chainDataArray = framework.getInternalRunningChains(null);
                    fail("Incorrectly allowed null input");
                } catch (InvalidChannelNameException e) {
                    // expected failure
                }

                framework.addChannel("app2", AppDummyFactory2.class, null, 10);
                try {
                    chainDataArray = framework.getRunningChains("app2" + channelSuffix);
                    fail("Incorrectly found unknown chain");
                } catch (InvalidChannelNameException e) {
                    // expected failured
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
            outputMgr.failWithThrowable("testGetInternalRunningChains", t);
        }
    }
}
