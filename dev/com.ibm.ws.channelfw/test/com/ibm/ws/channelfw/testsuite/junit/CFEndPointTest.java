/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
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

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.management.DynamicMBean;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import com.ibm.websphere.channelfw.CFEndPoint;
import com.ibm.websphere.channelfw.CFEndPointCriteria;
import com.ibm.websphere.channelfw.ChainData;
import com.ibm.websphere.channelfw.EndPointInfo;
import com.ibm.websphere.channelfw.EndPointMgr;
import com.ibm.websphere.channelfw.FlowType;
import com.ibm.websphere.channelfw.OutboundChannelDefinition;
import com.ibm.ws.channelfw.internal.chains.EndPointMgrImpl;
import com.ibm.ws.channelfw.testsuite.channels.protocol.PassThruFactory;
import com.ibm.ws.channelfw.testsuite.channels.protocol.ProtocolDummyContext;
import com.ibm.ws.channelfw.testsuite.channels.protocol.ProtocolDummyFactory;
import com.ibm.ws.channelfw.testsuite.channels.protocol.ProtocolLocalFactory;
import com.ibm.ws.channelfw.testsuite.channels.protocol.ProtocolSecureFactory;
import com.ibm.ws.tcpchannel.internal.TCPChannelFactory;
import com.ibm.wsspi.channelfw.ChannelFramework;
import com.ibm.wsspi.channelfw.ChannelFrameworkFactory;
import com.ibm.wsspi.channelfw.VirtualConnectionFactory;
import com.ibm.wsspi.tcpchannel.TCPConnectionContext;
import com.ibm.wsspi.udpchannel.UDPContext;

import test.common.SharedOutputManager;

/**
 * Unit tests for the CFEndPoint logic.
 */
public class CFEndPointTest {
    private static SharedOutputManager outputMgr;

    private final Mockery mock = new JUnit4Mockery();
    private final EndPointMgr mgrMock = mock.mock(EndPointMgr.class);
    private final BundleContext bundleMock = mock.mock(BundleContext.class);
    private final Bundle bundlMock = mock.mock(Bundle.class);
    private int endpointPort = 0;

    @Before
    public void setUp() {
        endpointPort = Integer.parseInt(System.getProperty("CFEndPointTest.endpointPort", "1010"));
        EndPointMgrImpl.setRef(mgrMock);
    }

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
     *
     * @throws Exception
     */
    @Test
    public void testEndPointMgrImpl() throws Exception {
        final String endpointName = "testEndpoint";
        final String endpointHost = "testHost";

        final Hashtable<String, String> properties = new Hashtable<String, String>();
        properties.put("service.vendor", "IBM");
        properties.put("jmx.objectname", "WebSphere:feature=channelfw,type=endpoint,name=testEndpoint");
        properties.put("type", "endpoint");

        mock.checking(new Expectations() {
            {
                allowing(bundleMock).registerService(with(any(Class.class)), with(any(DynamicMBean.class)), with(any(Dictionary.class)));
            }
        });

        // Define new Endpoint
        EndPointMgrImpl mgrImpl = new EndPointMgrImpl(bundleMock);
        mgrImpl.defineEndPoint(endpointName, endpointHost, endpointPort);
        assertTrue("Incorrect Endpoint was returned.", mgrImpl.getEndPoint(endpointName).toString().equals("EndPoint testEndpoint=testHost:" + endpointPort + ""));

        // Define an endpoint that already exists, causing it to get removed and added again
        mgrImpl.defineEndPoint(endpointName, endpointHost, endpointPort);
        assertTrue("Incorrect Endpoint was returned. Returned \"" + mgrImpl.getEndPoint(endpointName).toString()
                   + "\", but expected \"EndPoint testEndpoint=testHost:" + endpointPort + "\"",
                   mgrImpl.getEndPoint(endpointName).toString().equals("EndPoint testEndpoint=testHost:" + endpointPort + ""));

        // Check the endpoing is properly returned
        assertTrue("Incorrect Endpoint was returned. Returned \"" + mgrImpl.getEndPoints(endpointName, endpointPort).toString()
                   + "\", but expected \"[EndPoint testEndpoint=testHost:" + endpointPort + "]\"",
                   mgrImpl.getEndPoints(endpointName, endpointPort).toString().equals("[EndPoint testEndpoint=testHost:" + endpointPort + "]"));

        // Check the endpoing is properly returned
        assertTrue("Incorrect Endpoint was returned. Returned \"" + mgrImpl.getEndsPoints().toString()
                   + "\", but expected \"[EndPoint testEndpoint=testHost:" + endpointPort + "]\"",
                   mgrImpl.getEndPoints(endpointName, endpointPort).toString().equals("[EndPoint testEndpoint=testHost:" + endpointPort + "]"));
    }

    /**
     * Test endpoints.
     *
     * @throws Exception
     */
    @Test
    public void test() throws Exception {
        final int port1 = Integer.parseInt(System.getProperty("CFEndPointTest.port1", "11551"));
        final int port2 = Integer.parseInt(System.getProperty("CFEndPointTest.port2", "11552"));
        final int port3 = Integer.parseInt(System.getProperty("CFEndPointTest.port3", "11553"));
        ChannelFramework cf = ChannelFrameworkFactory.getChannelFramework();

        final EndPointInfo EPTest = mock.mock(EndPointInfo.class, "EPTest");
        final List<EndPointInfo> EP1List = new ArrayList<EndPointInfo>();
        EP1List.add(EPTest);

        final EndPointInfo EPTest3 = mock.mock(EndPointInfo.class, "EPTest3");
        final List<EndPointInfo> EP3List = new ArrayList<EndPointInfo>();
        EP3List.add(EPTest3);

        mock.checking(new Expectations() {
            {
                allowing(EPTest).getName();
                will(returnValue("LocalEP1"));
                allowing(EPTest).getHost();
                will(returnValue("localhost"));
                allowing(EPTest).getPort();
                will(returnValue(port1));

                allowing(EPTest3).getName();
                will(returnValue("LocalEP3"));
                allowing(EPTest3).getHost();
                will(returnValue("localhost"));
                allowing(EPTest3).getHost();
                will(returnValue(port3));

                allowing(mgrMock).defineEndPoint("LocalEP1", "localhost", port1);
                will(returnValue(EPTest));

                allowing(mgrMock).getEndPoints("localhost", port1);
                will(returnValue(EP1List));

                allowing(mgrMock).defineEndPoint("LocalEP2", "localhost", port2);

                allowing(mgrMock).getEndPoints("localhost", port2);

                allowing(mgrMock).defineEndPoint("LocalEP3", "localhost", port3);

                allowing(mgrMock).getEndPoints("localhost", port3);
                will(returnValue(EP3List));
            }
        });
        try {
            Map<Object, Object> tcpMap = new HashMap<Object, Object>();
            EndPointMgr mgr = EndPointMgrImpl.getRef();
            mgr.defineEndPoint("LocalEP1", "localhost", port1);
            tcpMap.put("hostname", "localhost");
            tcpMap.put("port", "" + port1);
            tcpMap.put("soReuseAddr", "true");

            // test a simple single chain and getEndPoint
            cf.addChannel("TCP1", TCPChannelFactory.class, tcpMap);
            cf.addChannel("APP1", PassThruFactory.class, null);
            cf.addChain("CHAIN1", FlowType.INBOUND, new String[] { "TCP1", "APP1" });
            cf.startChain("CHAIN1");
            CFEndPoint ep1 = cf.getEndPoint("CHAIN1");
            assertNotNull(ep1);
            assertEquals("CHAIN1", ep1.getName());
            assertEquals(port1, ep1.getPort());
            assertEquals(InetAddress.getByName("localhost"), ep1.getAddress());
            assertEquals(TCPConnectionContext.class, ep1.getChannelAccessor());
            assertFalse(ep1.isLocal());
            assertFalse(ep1.isSSLEnabled());
            List<OutboundChannelDefinition> defs = ep1.getOutboundChannelDefs();
            assertNotNull(defs);
            assertEquals(1, defs.size());
            OutboundChannelDefinition def = defs.get(0);
            assertNotNull(def);
            ChainData outc = ep1.createOutboundChain();
            assertNotNull(outc);
            assertNotNull(cf.getChain(outc.getName()));
            assertEquals(TCPChannelFactory.class, def.getOutboundFactory());
            VirtualConnectionFactory vcf = ep1.getOutboundVCFactory();
            assertNotNull(vcf);
            List<String> vhosts = ep1.getVirtualHosts();
            try {
                ep1.getOutboundVCFactory(null, true);
                fail("Should not have allowed on non-secure chain");
            } catch (IllegalStateException ise) {
                // expected exception
            }
            assertNotNull(vhosts);
            assertEquals(1, vhosts.size());
            assertEquals("LocalEP1", vhosts.get(0));

            // test multiple chains and endpoints
            mgr.defineEndPoint("LocalEP2", "localhost", port2);
            tcpMap.put("hostname", "localhost");
            tcpMap.put("port", "" + port2);
            tcpMap.put("soReuseAddr", "true");
            cf.addChannel("TCP2", TCPChannelFactory.class, tcpMap);
            cf.addChannel("APP2", ProtocolSecureFactory.class, null);
            cf.addChain("CHAIN2", FlowType.INBOUND, new String[] { "TCP2", "APP2" });
            cf.startChain("CHAIN2");
            CFEndPoint ep2 = cf.getEndPoint("CHAIN2");
            assertTrue(ep2.isSSLEnabled());

            mgr.defineEndPoint("LocalEP3", "localhost", port3);
            tcpMap.put("hostname", "localhost");
            tcpMap.put("port", "" + port3);
            cf.addChannel("TCP3", TCPChannelFactory.class, tcpMap);
            cf.addChannel("APP3", ProtocolDummyFactory.class, null);
            cf.addChain("CHAIN3", FlowType.INBOUND, new String[] { "TCP3", "APP3" });
            cf.startChain("CHAIN3");
            CFEndPoint ep3 = cf.getEndPoint("CHAIN3");
            assertFalse(ep3.isLocal());
            assertFalse(ep3.isSSLEnabled());
            assertEquals("LocalEP3", ep3.getVirtualHosts().get(0));

            // make port sharing chain that overlaps
            cf.addChannel("APP4", ProtocolLocalFactory.class, null);
            cf.addChain("CHAIN4", FlowType.INBOUND, new String[] { "TCP3", "APP4" });
            cf.startChain("CHAIN4");
            CFEndPoint ep4 = cf.getEndPoint("CHAIN4");
            assertTrue(ep4.isLocal());
            assertEquals("LocalEP3", ep4.getVirtualHosts().get(0));

            // search based on chain name
            CFEndPoint[] epList = new CFEndPoint[] { ep1, ep2, ep3, ep4 };
            TestCriteria criteria = new TestCriteria(TCPConnectionContext.class);
            criteria.name = "CHAIN2";
            CFEndPoint[] rc = cf.getEndPoints(epList, criteria);
            assertNotNull(rc);
            assertEquals(1, rc.length);
            assertEquals(ep2, rc[0]);
            // search based on vhost
            criteria = new TestCriteria(TCPConnectionContext.class);
            criteria.host = "LocalEP1";
            rc = cf.getEndPoints(epList, criteria);
            assertNotNull(rc);
            assertEquals(1, rc.length);
            assertEquals(ep1, rc[0]);
            criteria = new TestCriteria(ProtocolDummyContext.class);
            criteria.host = "LocalEP3";
            rc = cf.getEndPoints(epList, criteria);
            assertNotNull(rc);
            assertEquals(2, rc.length);
            assertTrue(ep3.equals(rc[0]) || ep3.equals(rc[1]));
            assertTrue(ep4.equals(rc[0]) || ep4.equals(rc[1]));
            // test failed search
            criteria = new TestCriteria(UDPContext.class);
            rc = cf.getEndPoints(epList, criteria);
            assertNull(rc);

            // test best choice
            criteria = new TestCriteria(ProtocolDummyContext.class);
            CFEndPoint ep = cf.determineBestEndPoint(epList, criteria);
            assertNotNull(ep);
            assertEquals(ep4, ep);
            // test non-found best choice
            criteria = new TestCriteria(UDPContext.class);
            ep = cf.determineBestEndPoint(epList, criteria);
            assertNull(ep);

            // test required factories
            criteria = new TestCriteria(TCPConnectionContext.class);
            criteria.factories = new Class<?>[] { TCPChannelFactory.class };
            rc = cf.getEndPoints(epList, criteria);
            assertNotNull(rc);
            assertEquals(1, rc.length);
            assertEquals(ep1, rc[0]); // ssl is ignored in this case
            criteria = new TestCriteria(ProtocolDummyContext.class);
            criteria.factories = new Class<?>[] { ProtocolDummyFactory.class };
            rc = cf.getEndPoints(epList, criteria);
            assertNotNull(rc);
            assertEquals(1, rc.length);
            assertEquals(ep3, rc[0]);

            // search for ssl only
            criteria = new TestCriteria(TCPConnectionContext.class);
            criteria.ssl = true;
            rc = cf.getEndPoints(epList, criteria);
            assertNotNull(rc);
            assertEquals(1, rc.length);
            assertEquals(ep2, rc[0]);

            // search for ssl + factory
            criteria = new TestCriteria(TCPConnectionContext.class);
            criteria.ssl = true;
            criteria.factories = new Class<?>[] { TCPChannelFactory.class };
            rc = cf.getEndPoints(epList, criteria);
            assertNotNull(rc);
            assertEquals(1, rc.length);
            assertEquals(ep2, rc[0]);

            // TODO ep.serializeToXML()
        } catch (Throwable t) {
            try {
                if (cf.isChainRunning("CHAIN1"))
                    cf.stopChain("CHAIN1", 1);
                if (cf.isChainRunning("CHAIN2"))
                    cf.stopChain("CHAIN2", 1);
                if (cf.isChainRunning("CHAIN3"))
                    cf.stopChain("CHAIN3", 1);
                if (cf.isChainRunning("CHAIN4"))
                    cf.stopChain("CHAIN4", 1);
            } catch (Exception e) {
                // ignore
            }
            outputMgr.failWithThrowable("test", t);
        }
    }

    private class TestCriteria implements CFEndPointCriteria {
        protected String name = null;
        protected Class<?> accessor = null;
        protected String host = null;
        protected boolean ssl = false;
        protected Class<?>[] factories = null;

        protected TestCriteria(Class<?> acc) {
            this.accessor = acc;
        }

        @Override
        public String getChainName() {
            return this.name;
        }

        @Override
        public Class<?> getChannelAccessor() {
            return this.accessor;
        }

        @Override
        public Class<?>[] getOptionalChannelFactories() {
            return this.factories;
        }

        @Override
        public String getVirtualHost() {
            return this.host;
        }

        @Override
        public boolean isSSLRequired() {
            return this.ssl;
        }
    }
}
