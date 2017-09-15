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

import com.ibm.websphere.channelfw.ChannelData;
import com.ibm.websphere.channelfw.FlowType;
import com.ibm.ws.channelfw.internal.ChannelFrameworkImpl;
import com.ibm.ws.channelfw.internal.chains.Chain;
import com.ibm.ws.channelfw.testsuite.channels.protocol.ProtocolDummyFactory;
import com.ibm.ws.channelfw.testsuite.channels.server.AppDummyChannel;
import com.ibm.ws.channelfw.testsuite.channels.server.AppDummyFactory;
import com.ibm.ws.tcpchannel.internal.TCPChannelFactory;
import com.ibm.wsspi.channelfw.exception.ChainException;
import com.ibm.wsspi.channelfw.exception.ChannelException;
import com.ibm.wsspi.channelfw.exception.InvalidChannelFactoryException;
import com.ibm.wsspi.channelfw.exception.InvalidChannelNameException;
import com.ibm.wsspi.channelfw.exception.InvalidWeightException;

/**
 * Runtime channel configuration test code.
 */
public class ChannelDataTest {
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

    // Helper method to create valid tcp props to start a chain with TCP in it.
    protected Map<Object, Object> getTcpProps(String portProp, String defPort) {
        String port = System.getProperty(portProp, defPort);
        Map<Object, Object> tcpMap = new HashMap<Object, Object>();
        tcpMap.put("port", port);
        return tcpMap;
    }

    /**
     * Test addChannel method.
     */
    @Test
    public void testAddChannel() {
        try {
            ChannelData channelData = null;
            ChannelFrameworkImpl framework = new ChannelFrameworkImpl();
            int numChannels = 0;

            try {
                Map<Object, Object> channelProps = getTcpProps("ChannelDataTest.testAddChannel1", "6200");
                channelProps.put("item1", "value1");
                channelData = framework.addChannel("tcp", TCPChannelFactory.class, channelProps, 45);
                assertNotNull(channelData);

                channelData = framework.getChannel("tcp");
                assertNotNull(channelData);
                assertEquals(1, framework.getNumChannels());
                assertEquals("tcp", channelData.getName());
                assertEquals(TCPChannelFactory.class, channelData.getFactoryType());

                Map<Object, Object> properties = channelData.getPropertyBag();
                assertEquals(2, properties.size());
                assertTrue(properties.containsKey("item1"));
                assertEquals("value1", properties.get("item1"));

                channelData = framework.addChannel("tcp2", TCPChannelFactory.class, getTcpProps("ChannelDataTest.testAddChannel2", "6201"), 45);
                properties = channelData.getPropertyBag();
                assertNotNull(properties);
                assertEquals(1, properties.size());
                assertNull(properties.get("item1"));

                channelData = framework.getChannel("tcp");
                assertEquals(45, channelData.getDiscriminatorWeight());

                try {
                    framework.addChannel("tcpnew", TCPChannelFactory.class, channelProps, -1);
                    fail("Incorrectly allowed negative weight");
                } catch (InvalidWeightException e) {
                    // expected failure
                }

                try {
                    framework.addChannel(null, TCPChannelFactory.class, channelProps, 1);
                    fail("Incorrectly allowed null name");
                } catch (ChannelException e) {
                    // expected failure
                }

                try {
                    framework.addChannel("tcpnew", null, channelProps, 1);
                    fail("Incorrectly allowed null factory");
                } catch (ChannelException e) {
                    // expected failure
                }

                try {
                    framework.addChannel("tcp", TCPChannelFactory.class, channelProps, 45);
                    fail("Incorrectly allowed 2nd instance of channel");
                } catch (InvalidChannelNameException e) {
                    // expected failure
                }

                try {
                    channelData = framework.addChannel("error", String.class, channelProps, 45);
                    fail("Incorrectly allowed non-factory class");
                } catch (InvalidChannelFactoryException e) {
                    // expected failure
                }

                channelData = framework.addChannel("new", TCPChannelFactory.class, channelProps);
                assertEquals(ChannelFrameworkImpl.DEFAULT_DISC_WEIGHT, channelData.getDiscriminatorWeight());

                numChannels = framework.getNumChannels();
                framework.addChannel("c1", ProtocolDummyFactory.class, null, 10);
                framework.addChannel("c2", AppDummyFactory.class, null, 10);
                framework.addChannel("c3", AppDummyFactory.class, null, 10);
                assertEquals((numChannels + 3), framework.getNumChannels());
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
            outputMgr.failWithThrowable("testAddChannel", t);
        }
    }

    /**
     * Test removeChannel method.
     */
    @Test
    public void testRemoveChannel() {
        try {
            ChannelData channelData = null;
            ChannelFrameworkImpl framework = new ChannelFrameworkImpl();
            try {
                framework.addChannel("tcp", TCPChannelFactory.class, getTcpProps("ChannelDataTest.testRemoveChannel1", "6202"), 45);
                channelData = framework.removeChannel("tcp");
                assertNotNull(channelData);
                assertEquals(45, channelData.getDiscriminatorWeight());

                channelData = framework.getChannel("tcp");
                assertNull(channelData);
                assertEquals(0, framework.getNumChannels());

                framework.addChannel("tcp", TCPChannelFactory.class, getTcpProps("ChannelDataTest.testRemoveChannel2", "6203"), 45);
                framework.removeChannel("tcp");
                channelData = framework.removeChannel("tcp");
                if (channelData != null)
                    fail("Incorrectly allowed 2nd remove");

                framework.addChannel("tcp", TCPChannelFactory.class, getTcpProps("ChannelDataTest.testRemoveChannel3", "6204"), 45);
                framework.addChain("chain1", FlowType.OUTBOUND, new String[] { "tcp" });
                framework.removeChannel("tcp");
                assertNull(framework.getChain("chain1"));
                assertEquals(0, framework.getNumChannels());
                assertEquals(0, framework.getNumChains());

                framework.addChannel("tcp", TCPChannelFactory.class, getTcpProps("ChannelDataTest.testRemoveChannel4", "6205"), 45);
                framework.addChain("chain1", FlowType.OUTBOUND, new String[] { "tcp" });
                framework.addChain("chain2", FlowType.OUTBOUND, new String[] { "tcp" });
                framework.removeChannel("tcp");
                assertNull(framework.getChain("chain1"));
                assertNull(framework.getChain("chain2"));
                assertEquals(0, framework.getNumChannels());
                assertEquals(0, framework.getNumChains());

                framework.addChannel("tcp", TCPChannelFactory.class, getTcpProps("ChannelDataTest.testRemoveChannel5", "6300"), 45);
                framework.addChain("chain1", FlowType.INBOUND, new String[] { "tcp" });
                framework.initChain("chain1");
                try {
                    framework.removeChannel("tcp");
                    fail("Incorrectly allowed remove on running channel");
                } catch (ChannelException e) {
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
            outputMgr.failWithThrowable("testRemoveChannel", t);
        }
    }

    /**
     * Test updateChannelWeight method.
     */
    @Test
    public void testUpdateChannelWeight() {
        try {
            ChannelFrameworkImpl framework = new ChannelFrameworkImpl();
            try {
                framework.addChannel("tcp", TCPChannelFactory.class, getTcpProps("ChannelDataTest.testUpdateChannelWeight", "6301"), 45);

                try {
                    framework.updateChannelWeight(null, 55);
                    fail("Incorrectly allowed null name");
                } catch (ChannelException e) {
                    // expected failure
                }

                try {
                    framework.updateChannelWeight("unknown", 55);
                    fail("Incorrectly allowed unknown channel");
                } catch (ChannelException e) {
                    // expected failure
                }

                try {
                    framework.updateChannelWeight("tcp", -1);
                    fail("Incorrectly allowed negative weight");
                } catch (ChannelException e) {
                    // expected failure
                }

                ChannelData channelData = framework.updateChannelWeight("tcp", 75);
                assertEquals(75, channelData.getDiscriminatorWeight());

                framework.addChannel("proto", ProtocolDummyFactory.class, null, 10);
                framework.addChannel("app", AppDummyFactory.class, null, 10);
                framework.addChain("chain1", FlowType.INBOUND, new String[] { "tcp", "proto", "app" });
                framework.initChain("chain1");
                Chain chain = framework.getRunningChain("chain1");
                framework.updateChannelWeight("app", 25);
                AppDummyChannel baseChannel = (AppDummyChannel) framework.getRunningChannel("app", chain);
                assertEquals(25, baseChannel.getWeight());
                framework.destroyChain("chain1");
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
            outputMgr.failWithThrowable("testUpdateChannelWeight", t);
        }
    }

    /**
     * Test updateChannelProperty method.
     */
    @Test
    public void testUpdateChannelProperty() {
        try {
            ChannelData channelData = null;
            Map<Object, Object> channelProps = null;
            ChannelFrameworkImpl framework = new ChannelFrameworkImpl();

            try {
                framework.addChannel("tcp", TCPChannelFactory.class, getTcpProps("ChannelDataTest.testUpdateChannelProperty", "6302"), 45);
                framework.addChannel("proto", ProtocolDummyFactory.class, null, 45);

                try {
                    channelData = framework.updateChannelProperty(null, "item", "value");
                    fail("Incorrectly allowed null name");
                } catch (ChannelException e) {
                    // expected failure
                }

                try {
                    channelData = framework.updateChannelProperty("unknown", "item", "value");
                    fail("Incorrectly allowed unknown channel");
                } catch (ChannelException e) {
                    // expected failure
                }

                try {
                    channelData = framework.updateChannelProperty("proto", null, "value");
                    fail("Incorrectly allowed null prop key");
                } catch (ChannelException e) {
                    // expected failure
                }

                try {
                    channelData = framework.updateChannelProperty("proto", "item1", null);
                    fail("Incorrectly allowed null prop value");
                } catch (ChannelException e) {
                    // expected failure
                }

                channelData = framework.updateChannelProperty("proto", "item1", "value1");
                channelProps = framework.getChannel("proto").getPropertyBag();
                assertEquals(1, channelProps.size());
                assertEquals("value1", channelProps.get("item1"));

                channelData = framework.updateChannelProperty("proto", "item1", "value2");
                channelProps = framework.getChannel("proto").getPropertyBag();
                assertEquals(1, channelProps.size());
                assertEquals("value2", channelProps.get("item1"));

                framework.addChannel("app", AppDummyFactory.class, null, 10);
                framework.addChain("chain1", FlowType.INBOUND, new String[] { "tcp", "proto", "app" });
                framework.initChain("chain1");
                framework.updateChannelProperty("app", "item1", "newValue");
                Chain chain = framework.getRunningChain("chain1");
                AppDummyChannel baseChannel = (AppDummyChannel) framework.getRunningChannel("app", chain);
                channelData = baseChannel.getConfig();
                assertEquals("newValue", channelData.getPropertyBag().get("item1"));
                framework.destroyChain("chain1");
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
            outputMgr.failWithThrowable("testUpdateChannelProperty", t);
        }
    }

    /**
     * Test updateAllChannelProperties method.
     */
    @Test
    public void testUpdateAllChannelProperties() {
        try {
            ChannelData channelData = null;
            ChannelFrameworkImpl framework = new ChannelFrameworkImpl();

            try {
                Map<Object, Object> channelProps1 = new HashMap<Object, Object>();
                channelProps1.put("item1", "value1");
                channelProps1.put("item2", new Integer(2));
                Map<Object, Object> channelProps2 = new HashMap<Object, Object>();
                channelProps2.put("item1", "updatedvalue");
                channelProps2.put("item2", new Integer(22));
                channelProps2.put("item3", "value3");
                framework.addChannel("tcp", TCPChannelFactory.class, getTcpProps("ChannelDataTest.testUpdateAllChannelProperties", "6303"), 45);
                framework.addChannel("proto", ProtocolDummyFactory.class, null, 10);

                try {
                    channelData = framework.updateAllChannelProperties(null, channelProps1);
                    fail("Incorrectly allowed null channel");
                } catch (ChannelException e) {
                    // expected failure
                }

                try {
                    channelData = framework.updateAllChannelProperties("unknown", channelProps1);
                    fail("Incorrectly allowed unknown channel");
                } catch (ChannelException e) {
                    // expected failure
                }

                try {
                    channelData = framework.updateAllChannelProperties("tcp", null);
                    fail("Incorrectly allowed null props");
                } catch (ChannelException e) {
                    // expected failure
                }

                channelData = framework.updateAllChannelProperties("proto", channelProps1);
                assertEquals(2, channelData.getPropertyBag().size());
                assertEquals("value1", channelData.getPropertyBag().get("item1"));
                assertEquals(2, ((Integer) channelData.getPropertyBag().get("item2")).intValue());

                channelData = framework.updateAllChannelProperties("proto", channelProps2);
                assertEquals(3, channelData.getPropertyBag().size());
                assertEquals("updatedvalue", channelData.getPropertyBag().get("item1"));
                assertEquals(22, ((Integer) channelData.getPropertyBag().get("item2")).intValue());

                framework.addChannel("app", AppDummyFactory.class, null, 10);
                framework.addChain("chain1", FlowType.INBOUND, new String[] { "tcp", "proto", "app" });
                framework.initChain("chain1");
                Chain chain = framework.getRunningChain("chain1");
                channelProps1 = new HashMap<Object, Object>();
                channelProps1.put("item1", "newValue");
                framework.updateAllChannelProperties("app", channelProps1);
                AppDummyChannel baseChannel = (AppDummyChannel) framework.getRunningChannel("app", chain);
                channelData = baseChannel.getConfig();
                assertEquals("newValue", channelData.getPropertyBag().get("item1"));
                framework.destroyChain("chain1");
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
            outputMgr.failWithThrowable("testUpdateAllChannelProperties", t);
        }
    }

    /**
     * Test getChannel method.
     */
    @Test
    public void testGetChannel() {
        try {
            ChannelData channelData = null;
            ChannelFrameworkImpl framework = new ChannelFrameworkImpl();

            try {
                framework.addChannel("tcp", TCPChannelFactory.class, getTcpProps("ChannelDataTest.testGetChannel", "6305"), 45);
                channelData = framework.getChannel("tcp");
                assertNotNull(channelData);
                assertEquals(45, channelData.getDiscriminatorWeight());
                assertNull(framework.getChannel("nonexistent"));
                assertNull(framework.getChannel("tcp1"));
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
            outputMgr.failWithThrowable("testGetChannel", t);
        }
    }

    /**
     * Test getAllChannels method.
     */
    @Test
    public void testGetAllChannels() {
        try {
            ChannelData channelDataArray[] = null;
            ChannelFrameworkImpl framework = new ChannelFrameworkImpl();

            channelDataArray = framework.getAllChannels();
            assertEquals(0, channelDataArray.length);
            try {
                framework.addChannel("tcp", TCPChannelFactory.class, getTcpProps("ChannelDataTest.testGetAllChannels", "6306"), 45);
                framework.addChannel("proto", ProtocolDummyFactory.class, null, 10);
                framework.addChannel("app", AppDummyFactory.class, null, 10);
                channelDataArray = framework.getAllChannels();
                assertEquals(3, channelDataArray.length);
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
            outputMgr.failWithThrowable("testGetAllChannels", t);
        }
    }

    /**
     * Test geetRunningChannels method.
     */
    @Test
    public void testGetRunningChannels() {
        try {
            ChannelData channelDataArray[] = null;
            ChannelFrameworkImpl framework = new ChannelFrameworkImpl();

            try {
                channelDataArray = framework.getRunningChannels();
                assertEquals(0, channelDataArray.length);

                framework.addChannel("tcp", TCPChannelFactory.class, getTcpProps("ChannelDataTest.testGetRunningChannels", "6307"), 45);
                framework.addChannel("proto", ProtocolDummyFactory.class, null, 10);
                framework.addChannel("app", AppDummyFactory.class, null, 10);
                channelDataArray = framework.getRunningChannels();
                assertEquals(0, channelDataArray.length);

                framework.addChain("chain1", FlowType.INBOUND, new String[] { "tcp", "proto", "app" });
                framework.initChain("chain1");
                channelDataArray = framework.getAllChannels();
                assertEquals(3, channelDataArray.length);
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
            outputMgr.failWithThrowable("testGetRunningChannels", t);
        }
    }

}
