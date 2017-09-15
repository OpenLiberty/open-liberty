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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.Sequence;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.ibm.websphere.channelfw.ChainData;
import com.ibm.websphere.channelfw.ChainGroupData;
import com.ibm.websphere.channelfw.ChannelFactoryData;
import com.ibm.websphere.channelfw.ChannelUtils;
import com.ibm.websphere.channelfw.EndPointInfo;
import com.ibm.websphere.channelfw.EndPointMgr;
import com.ibm.websphere.channelfw.FlowType;
import com.ibm.ws.channelfw.internal.ChannelFrameworkImpl;
import com.ibm.ws.channelfw.internal.chains.EndPointMgrImpl;
import com.ibm.ws.channelfw.testsuite.channels.outbound.OutboundDummyFactory;
import com.ibm.ws.channelfw.testsuite.channels.protocol.ProtocolDummyFactory;
import com.ibm.ws.channelfw.testsuite.channels.server.AppDummyFactory;
import com.ibm.ws.channelfw.testsuite.channels.server.WebDummyFactory;
import com.ibm.ws.tcpchannel.internal.TCPChannelFactory;
import com.ibm.wsspi.channelfw.ChainEventListener;
import com.ibm.wsspi.channelfw.ChannelFrameworkFactory;

import test.common.SharedOutputManager;

/**
 * Testcase for the ChannelUtils class.
 */
public class UtilsTest {
    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("*=all=enabled");

    private final Mockery mock = new JUnit4Mockery();
    private final EndPointMgr mgrMock = mock.mock(EndPointMgr.class);

    @Rule
    public TestRule outputRule = outputMgr;

    @Before
    public void setUp() {
        EndPointMgrImpl.setRef(mgrMock);
    }

    /**
     * Test the loadConfig api.
     */
    @Test
    public void testConfigUtils() {
        try {
            // this.context.checking(new Expectations() {{
            // ignoring(mockContext);
            // }});
            ChannelFrameworkImpl cf = new ChannelFrameworkImpl();

            cf.registerFactory("TCPInboundChannel", TCPChannelFactory.class);
            cf.registerFactory("TCPOutboundChannel", TCPChannelFactory.class);
            cf.registerFactory("ProtocolDummy", ProtocolDummyFactory.class);
            cf.registerFactory("AppDummy", AppDummyFactory.class);
            cf.registerFactory("WebDummy", WebDummyFactory.class);
            cf.registerFactory("OutboundDummy", OutboundDummyFactory.class);
            Map<String, List<String>> rc;

            Map<String, Object> props = new HashMap<String, Object>();
            // test empty props
            rc = ChannelUtils.loadConfig(props);
            assertEquals(0, cf.getAllChainGroups().length);
            assertEquals(0, cf.getAllChains().length);
            assertEquals(0, cf.getAllChannels().length);

            String port = System.getProperty("UtilsTest.testConfigUtils", "9080");
            // test some channels
            String[] tcpprops = new String[] { "type=TCPInboundChannel",
                                               "hostname=*",
                                               "port=" + port };
            String[] protoprops = new String[] { "type=ProtocolDummy" };
            props.put(ChannelUtils.CHANNEL_PREFIX + "TCP", tcpprops);
            props.put(ChannelUtils.CHANNEL_PREFIX + "PROTO", protoprops);
            rc = ChannelUtils.loadConfig(props);
            assertEquals(0, cf.getAllChainGroups().length);
            assertEquals(0, cf.getAllChains().length);
            assertEquals(2, cf.getAllChannels().length);
            assertNotNull(cf.getChannel("TCP"));
            assertNotNull(cf.getChannel("PROTO"));
            assertEquals(0, rc.get("factory").size());
            assertEquals(0, rc.get("chain").size());
            assertEquals(0, rc.get("group").size());
            assertEquals(2, rc.get("channel").size());

            // test a "reload" of the same channels
            rc = ChannelUtils.loadConfig(props);
            assertEquals(0, cf.getAllChainGroups().length);
            assertEquals(0, cf.getAllChains().length);
            assertEquals(2, cf.getAllChannels().length);
            assertNotNull(cf.getChannel("TCP"));
            assertNotNull(cf.getChannel("PROTO"));
            assertEquals(0, rc.get("factory").size());
            assertEquals(0, rc.get("chain").size());
            assertEquals(0, rc.get("group").size());
            assertEquals(2, rc.get("channel").size());
            props.clear();
            cf.clear();

            // test adding some chains
            String[] outprops = new String[] { "type=OutboundDummy" };
            String[] appprops = new String[] { "type=AppDummy" };
            String[] inchain1props = new String[] {
                                                    "enable=true", "flow=inbound", "channels=TCP, PROTO, APP" };
            String[] outchainprops = new String[] {
                                                    "enable=false", "flow=outbound", "channels=OUTD" };
            props.put(ChannelUtils.CHANNEL_PREFIX + "TCP", tcpprops);
            props.put(ChannelUtils.CHANNEL_PREFIX + "PROTO", protoprops);
            props.put(ChannelUtils.CHANNEL_PREFIX + "APP", appprops);
            props.put(ChannelUtils.CHANNEL_PREFIX + "OUTD", outprops);
            props.put(ChannelUtils.CHAIN_PREFIX + "inbound1", inchain1props);
            props.put(ChannelUtils.CHAIN_PREFIX + "outbound1", outchainprops);
            rc = ChannelUtils.loadConfig(props);
            assertEquals(0, cf.getAllChainGroups().length);
            assertEquals(2, cf.getAllChains().length);
            ChainData chain = cf.getChain("inbound1");
            assertNotNull(chain);
            assertEquals(FlowType.INBOUND, chain.getType());
            assertEquals(3, chain.getChannelList().length);
            assertEquals("TCP", chain.getChannelList()[0].getExternalName());
            assertEquals("PROTO", chain.getChannelList()[1].getExternalName());
            assertEquals("APP", chain.getChannelList()[2].getExternalName());
            assertTrue(chain.isEnabled());
            chain = cf.getChain("outbound1");
            assertNotNull(chain);
            assertEquals(FlowType.OUTBOUND, chain.getType());
            assertEquals(1, chain.getChannelList().length);
            assertEquals("OUTD", chain.getChannelList()[0].getExternalName());
            assertFalse(chain.isEnabled());
            assertEquals(4, cf.getAllChannels().length);
            assertNotNull(cf.getChannel("TCP"));
            assertNotNull(cf.getChannel("PROTO"));
            assertNotNull(cf.getChannel("APP"));
            assertNotNull(cf.getChannel("OUTD"));
            assertEquals(0, rc.get("factory").size());
            assertEquals(2, rc.get("chain").size());
            assertEquals(0, rc.get("group").size());
            assertEquals(4, rc.get("channel").size());
            props.clear();
            cf.clear();

            // test adding groups
            String[] webprops = new String[] { "type=WebDummy" };
            String[] inchain2props = new String[] {
                                                    "enable=true", "flow=inbound", "channels=TCP, PROTO, WEB" };
            props.put(ChannelUtils.CHANNEL_PREFIX + "TCP", tcpprops);
            props.put(ChannelUtils.CHANNEL_PREFIX + "PROTO", protoprops);
            props.put(ChannelUtils.CHANNEL_PREFIX + "APP", appprops);
            props.put(ChannelUtils.CHANNEL_PREFIX + "WEB", webprops);
            props.put(ChannelUtils.CHANNEL_PREFIX + "OUTD", outprops);
            props.put(ChannelUtils.CHAIN_PREFIX + "inbound1", inchain1props);
            props.put(ChannelUtils.CHAIN_PREFIX + "inbound2", inchain2props);
            props.put(ChannelUtils.CHAIN_PREFIX + "outbound1", outchainprops);
            props.put(ChannelUtils.GROUP_PREFIX + "in", new String[] { "inbound1", "inbound2" });
            props.put(ChannelUtils.GROUP_PREFIX + "out", new String[] { "outbound1" });
            rc = ChannelUtils.loadConfig(props);
            assertEquals(2, cf.getAllChainGroups().length);
            ChainGroupData group = cf.getChainGroup("in");
            assertNotNull(group);
            assertEquals(2, group.getChains().length);
            assertTrue(group.getChains()[0].getName().startsWith("inbound"));
            assertTrue(group.getChains()[1].getName().startsWith("inbound"));
            group = cf.getChainGroup("out");
            assertNotNull(group);
            assertEquals(1, group.getChains().length);
            assertEquals("outbound1", group.getChains()[0].getName());
            assertEquals(0, rc.get("factory").size());
            assertEquals(3, rc.get("chain").size());
            assertEquals(2, rc.get("group").size());
            assertEquals(5, rc.get("channel").size());
            props.clear();
            cf.clear();

            // test factory config
            props.put(ChannelUtils.FACTORY_PREFIX + "WebDummy", new String[] { "k1=v1" });
            rc = ChannelUtils.loadConfig(props);
            ChannelFactoryData factory = cf.getChannelFactory(WebDummyFactory.class);
            assertNotNull(factory);
            assertEquals("v1", factory.getProperties().get("k1"));
            assertEquals(1, rc.get("factory").size());
            assertEquals(0, rc.get("chain").size());
            assertEquals(0, rc.get("group").size());
            assertEquals(0, rc.get("channel").size());
            props.clear();
            cf.clear();

            // test various malformed config
            props.put(ChannelUtils.FACTORY_PREFIX + "MISSING", new String[] { "k1=v1" });
            props.put(ChannelUtils.CHANNEL_PREFIX, tcpprops);
            props.put(ChannelUtils.CHANNEL_PREFIX + "   ", protoprops);
            props.put(ChannelUtils.CHAIN_PREFIX + "noconfigchain", new String[] {});
            props.put(ChannelUtils.CHAIN_PREFIX + "badconfigchain",
                      new String[] { "channels=TCP" });
            props.put(ChannelUtils.GROUP_PREFIX + "badgroup",
                      new String[] { "noconfigchain" });
            rc = ChannelUtils.loadConfig(props);
            assertEquals(0, cf.getAllChainGroups().length);
            assertEquals(0, cf.getAllChains().length);
            assertEquals(0, cf.getAllChannels().length);
            // Note: these used to return empty values but with the "delayed"
            // config handling, they act as normal configs
            assertEquals(1, rc.get("factory").size());
            assertEquals(1, rc.get("chain").size());
            assertEquals(1, rc.get("group").size());
            assertEquals(0, rc.get("channel").size());
            props.clear();
            cf.clear();
        } catch (Throwable t) {
            outputMgr.failWithThrowable("testConfigUtils", t);
        }
    }

    /**
     * Test the startConfig api that takes an old config and compares against a new one.
     */
    @Test
    public void testStartConfig() throws Exception {
        final int port = Integer.parseInt(System.getProperty("UtilsTest.testStartConfig", "8000"));
        final Sequence removalSeq = mock.sequence("removal");
        final EndPointInfo EPTest = mock.mock(EndPointInfo.class, "EPTest");
        final EndPointInfo EPTest2 = mock.mock(EndPointInfo.class, "EPTest2");
        mock.checking(new Expectations() {
            {
                allowing(EPTest).getName();
                will(returnValue("EPTest"));
                allowing(EPTest).getHost();
                will(returnValue("*"));
                allowing(EPTest).getPort();
                will(returnValue(0));

                allowing(EPTest2).getName();
                will(returnValue("EP2"));
                allowing(EPTest2).getHost();
                will(returnValue("*"));
                allowing(EPTest2).getHost();
                will(returnValue(port));

                allowing(mgrMock).defineEndPoint("EPTest", "*", 0);
                will(returnValue(EPTest));

                allowing(mgrMock).getEndPoint("EPTest");
                will(returnValue(EPTest));

                one(mgrMock).defineEndPoint("EP2", "*", port);
                inSequence(removalSeq);
                will(returnValue(EPTest2));

                one(mgrMock).getEndPoint("EP2");
                inSequence(removalSeq);
                will(returnValue(EPTest2));

                allowing(mgrMock).removeEndPoint("EP2");
                inSequence(removalSeq);

                one(mgrMock).getEndPoint("EP2");
                inSequence(removalSeq);
                will(returnValue(null));
            }
        });

        try {
            ChannelFrameworkImpl cf = (ChannelFrameworkImpl) ChannelFrameworkFactory.getChannelFramework();
            if (null == cf) {
                cf = new ChannelFrameworkImpl();
            }
            cf.setDefaultChainQuiesceTimeout("250");

            cf.registerFactory("TCPInboundChannel", TCPChannelFactory.class);
            cf.registerFactory("ProtocolDummy", ProtocolDummyFactory.class);
            cf.registerFactory("AppDummy", AppDummyFactory.class);
            cf.registerFactory("WebDummy", WebDummyFactory.class);
            Map<String, List<String>> rc;

            HashMap<String, Object> props = new HashMap<String, Object>();
            String[] tcpprops = new String[] {
                                               "type=TCPInboundChannel", "endpoint=EPTest" };
            String[] protoprops = new String[] { "type=ProtocolDummy" };
            String[] webprops = new String[] { "type=WebDummy" };
            String[] epprops = new String[] { "host=*", "port=0" };
            String[] chainprops = new String[] { "channels=TCP, PROTO, WEB" };
            props.put(ChannelUtils.ENDPOINT_PREFIX + "EPTest", epprops);
            props.put(ChannelUtils.CHANNEL_PREFIX + "TCP", tcpprops);
            props.put(ChannelUtils.CHANNEL_PREFIX + "PROTO", protoprops);
            props.put(ChannelUtils.CHANNEL_PREFIX + "WEB", webprops);
            props.put(ChannelUtils.CHAIN_PREFIX + "testStartConfig", chainprops);

            // Start the chains for the first time... the return value should
            // contain the returned chain (size=1)
            System.out.println("***********************************");
            rc = ChannelUtils.startConfig(null, props);
            assertNotNull(rc);
            EndPointInfo ep = EndPointMgrImpl.getRef().getEndPoint("EPTest");
            assertNotNull(ep);
            assertEquals("*", ep.getHost());
            assertEquals(0, ep.getPort());
            assertEquals(1, rc.get("chain").size());

            // test zero changes
            ChainListener listener = new ChainListener();
            cf.addChainEventListener(listener, ChainEventListener.ALL_CHAINS);
            Map<String, Object> oldprops = props;
            // start the chain again -- the listener should not be invoked
            System.out.println("***********************************");
            rc = ChannelUtils.startConfig(oldprops, props);
            assertNotNull(rc);
            assertEquals(1, rc.get("chain").size());
            assertEquals(0, listener.stopCount);
            assertEquals(0, listener.startCount);

            // test a chain update
            oldprops = props;
            props = new HashMap<String, Object>();
            props.putAll(oldprops);
            chainprops = new String[] { "channels=TCP, PROTO, WEB", "k=v" };
            props.put(ChannelUtils.CHAIN_PREFIX + "testStartConfig", chainprops);
            // start the chain again -- the original chain should be stopped, and a new one created
            // expect both stop and start to be called
            System.out.println("***********************************");
            rc = ChannelUtils.startConfig(oldprops, props);
            assertNotNull(rc);
            assertEquals(1, rc.get("chain").size());
            assertEquals(1, listener.stopCount);
            assertEquals(1, listener.startCount);

            // test a new endpoint (no changes)
            oldprops = props;
            props = new HashMap<String, Object>();
            props.putAll(oldprops);
            props.put(ChannelUtils.ENDPOINT_PREFIX + "EP2", new String[] { "host=*",
                                                                           "port=" + port });
            rc = ChannelUtils.startConfig(oldprops, props);
            assertNotNull(rc);
            assertEquals(1, rc.get("chain").size());
            assertEquals(1, listener.stopCount);
            assertEquals(1, listener.startCount);

            // test an update on a used endpoint
            oldprops = props;
            props = new HashMap<String, Object>();
            props.putAll(oldprops);
            epprops = new String[] { "host=*", "port=0", "k=v" };
            props.put(ChannelUtils.ENDPOINT_PREFIX + "EPTest", epprops);
            rc = ChannelUtils.startConfig(oldprops, props);
            assertNotNull(rc);
            assertEquals(1, rc.get("chain").size());
            assertEquals(2, listener.stopCount);
            assertEquals(2, listener.startCount);

            // test deleting an endpoint
            assertNotNull(EndPointMgrImpl.getRef().getEndPoint("EP2"));
            oldprops = props;
            props = new HashMap<String, Object>();
            props.putAll(oldprops);
            props.remove(ChannelUtils.ENDPOINT_PREFIX + "EP2");
            rc = ChannelUtils.startConfig(oldprops, props);
            assertNotNull(rc);
            assertEquals(1, rc.get("chain").size());
            assertEquals(2, listener.stopCount);
            assertEquals(2, listener.startCount);
            assertNull("EP2 was not null after removal", EndPointMgrImpl.getRef().getEndPoint("EP2"));

            // test a channel update
            oldprops = props;
            props = new HashMap<String, Object>();
            props.putAll(oldprops);
            webprops = new String[] { "type=WebDummy", "k=v" };
            props.put(ChannelUtils.CHANNEL_PREFIX + "WEB", webprops);
            rc = ChannelUtils.startConfig(oldprops, props);
            assertNotNull(rc);
            assertEquals(1, rc.get("chain").size());
            assertEquals(3, listener.stopCount);
            assertEquals(3, listener.startCount);

            // test removing a channel (without removing the chain)
            oldprops = props;
            props = new HashMap<String, Object>();
            props.putAll(oldprops);
            props.remove(ChannelUtils.CHANNEL_PREFIX + "WEB");
            rc = ChannelUtils.startConfig(oldprops, props);
            assertNotNull(rc);
            assertEquals(0, rc.get("chain").size());
            assertEquals(4, listener.stopCount);
            assertEquals(3, listener.startCount);

        } catch (Throwable t) {
            outputMgr.failWithThrowable("testStartConfig", t);
        }
    }

    @SuppressWarnings("unused")
    private class ChainListener implements ChainEventListener {
        protected int destroyCount = 0;
        protected int startCount = 0;
        protected int stopCount = 0;
        protected int quiesceCount = 0;

        protected ChainListener() {
            // do nothing
        }

        @Override
        public void chainDestroyed(ChainData chainData) {
            this.destroyCount++;
        }

        @Override
        public void chainInitialized(ChainData chainData) {
            // do nothing
        }

        @Override
        public void chainQuiesced(ChainData chainData) {
            this.quiesceCount++;
        }

        @Override
        public void chainStarted(ChainData chainData) {
            System.out.println("** START ** ");
            this.startCount++;
        }

        @Override
        public void chainStopped(ChainData chainData) {
            System.out.println("** STOP ** ");
            this.stopCount++;
        }

        @Override
        public void chainUpdated(ChainData chainData) {
            // do nothing
        }

    }
}
