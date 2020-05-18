/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.osgi.mbeans;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.api.Action;
import org.jmock.api.Invocation;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.ibm.ws.webcontainer.httpsession.SessionManager;
import com.ibm.ws.webcontainer.osgi.DynamicVirtualHost;
import com.ibm.ws.webcontainer.osgi.DynamicVirtualHostManager;
import com.ibm.ws.webcontainer.osgi.WebContainer;
import com.ibm.ws.webcontainer.osgi.mbeans.PluginGenerator.HttpEndpointInfo;
import com.ibm.ws.webcontainer.osgi.mbeans.PluginGenerator.ServerData;
import com.ibm.ws.webcontainer.osgi.mbeans.PluginGenerator.VHostData;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.location.WsResource;

import test.common.SharedOutputManager;
import test.common.junit.rules.MaximumJavaLevelRule;

/**
 *
 */
public class PluginGeneratorTest {
    
    @ClassRule
    public static MaximumJavaLevelRule maxLevel = new MaximumJavaLevelRule(8);
    
    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("*=info:webcontainer=all");

    final Mockery context = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    @Rule
    public TestRule mockRule = new TestRule() {
        @Override
        public Statement apply(final Statement stmt, final Description desc) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    // run the test
                    stmt.evaluate();
                    context.assertIsSatisfied();
                }
            };
        }
    };

    @Rule
    public TestRule rule = outputMgr;

    final BundleContext mockBundleContext = context.mock(BundleContext.class);
    final WsLocationAdmin mockLocationAdmin = context.mock(WsLocationAdmin.class);
    final DynamicVirtualHostManager mockVhostMgr = context.mock(DynamicVirtualHostManager.class);
    final DynamicVirtualHost mockDefaultHost = context.mock(DynamicVirtualHost.class, "default_host");

    final ServiceReference<?> mockDefVhostRef = context.mock(ServiceReference.class, "default_hostRef");
    final ServiceReference<?> mockEndpointInfoRef = context.mock(ServiceReference.class, "endpointInfo_Ref");
    final HttpEndpointInfo mockEndpointInfo = context.mock(HttpEndpointInfo.class, "EndpointInfo");
    final Element element = context.mock(Element.class);
    final Document doc = context.mock(Document.class);
    final Comment comment = context.mock(Comment.class);
    final WebContainer mockWebContainer = context.mock(WebContainer.class);
    final SessionManager mockSessionManager = context.mock(SessionManager.class);
    final WsResource mockWsResource = context.mock(WsResource.class);

    private static final String testClassesDir = System.getProperty("test.classesDir", "bin_test");

    String lastComment = null;

    @Test
    public void testBuildServerTransportData() throws Exception {

        context.checking(new Expectations() {
            {
                allowing(mockLocationAdmin).getServerName();
                will(returnValue("SystemProvidedServerName"));

                allowing(mockEndpointInfo).getEndpointId();
                will(returnValue(mockEndpointInfo.toString()));

                one(mockEndpointInfo).getProperty("_defaultHostName");
                will(returnValue("localhost")); // this is the default, will always be present in the system

                one(mockEndpointInfo).getProperty("host");
                will(returnValue("*"));

                one(mockEndpointInfo).getProperty("httpPort");
                will(returnValue(1));

                one(mockEndpointInfo).getProperty("httpsPort");
                will(returnValue(-1));
            }
        });

        List<ServerData> clusterServers = new LinkedList<ServerData>();
        Map<String, Object> config = new HashMap<String, Object>();
        config.put("httpEndpointRef", "Endpoint1");

        PluginGenerator pluginGen = new PluginGenerator(config, mockLocationAdmin, mockBundleContext);
        pluginGen.buildServerTransportData("serverName", "dummyId", mockEndpointInfo, clusterServers, false);
        System.out.println(clusterServers);
        assertEquals("1 elements in server data list", 1, clusterServers.size());

        assertTrue("clusterServer1 " + clusterServers.get(0), !"localhost".equals(clusterServers.get(0).hostName));
        assertEquals("clusterServer1 " + clusterServers.get(0), 1, clusterServers.get(0).transports.size());
        assertEquals("clusterServer1 " + clusterServers.get(0), 1, clusterServers.get(0).transports.get(0).port);
        assertFalse("clusterServer1 " + clusterServers.get(0), clusterServers.get(0).transports.get(0).isSslEnabled);
    }

    private void setCommentExpectations() throws Exception {
        context.checking(new Expectations() {
            {
                allowing(element).getOwnerDocument();
                will(returnValue(doc));

                allowing(doc).createComment(with(any(String.class)));
                will(new Action() {
                    @Override
                    public void describeTo(org.hamcrest.Description description) {
                        description.appendText("saves comment value");
                    }

                    @Override
                    public Object invoke(Invocation arg0) throws Throwable {
                        lastComment = (String) arg0.getParameter(0);
                        System.out.println(lastComment);
                        return comment;
                    }
                });

                allowing(element).appendChild(comment);
            }
        });
    }

    private void setCommonVHostExpectations() throws Exception {
        setCommentExpectations();
        context.checking(new Expectations() {
            {
                allowing(mockLocationAdmin).getServerName();
                will(returnValue("SystemProvidedServerName"));

                allowing(mockBundleContext).getAllServiceReferences(null, "(&(service.factoryPid=com.ibm.ws.http.virtualhost)(|(enabled=true)(id=default_host)))");
                will(returnValue(new ServiceReference<?>[] { mockDefVhostRef }));

                allowing(mockDefVhostRef).getProperty("id");
                will(returnValue("default_host"));

                allowing(mockVhostMgr).getVirtualHosts();
                will(returnIterator(mockDefaultHost));

                allowing(mockDefaultHost).getName();
                will(returnValue("default_host"));

                allowing(element).getOwnerDocument();
                will(returnValue(doc));
            }
        });
    }

    @Test
    public void testDefaultConfig() throws Exception {
        setCommonVHostExpectations();
        context.checking(new Expectations() {
            {
                // Variations..
                allowing(mockDefVhostRef).getProperty("hostAlias");
                will(returnValue(null));
                allowing(mockDefVhostRef).getProperty("allowFromEndpointRef");
                will(returnValue(null));
                allowing(mockEndpointInfo).getEndpointId();
                will(returnValue(mockEndpointInfo.toString()));
            }
        });

        Map<String, Object> config = new HashMap<String, Object>();
        setDefaultConfig(config);
        Map<String, List<VHostData>> vhostAliasData = new HashMap<String, List<VHostData>>();
        PluginGenerator pluginGen = new PluginGenerator(config, mockLocationAdmin, mockBundleContext);

        // First variation
        // This configuration is JUST the default_host with the default aliases for the transports..
        Set<DynamicVirtualHost> virtualHostSet = pluginGen.processVirtualHosts(mockVhostMgr, vhostAliasData, mockEndpointInfo, element);
        assertEquals("There should be one element in the virtual host set", 1, virtualHostSet.size());
        assertSame("The default host object should be in the virtual host set", mockDefaultHost, virtualHostSet.iterator().next());

        assertEquals("There should be one element in the virtual host alias data", 1, vhostAliasData.size());
        List<VHostData> data = vhostAliasData.get("default_host");
        assertNotNull("There should be a default_host element in the vhostAliasData map", data);
        assertEquals("There should be two elements in the VHostData", 2, data.size());
        assertTrue("VHostData should contain an alias for *:80", data.contains(new VHostData("*", 80)));
        assertTrue("VHostData should contain an alias for *:443", data.contains(new VHostData("*", 443)));
    }

    @Test
    public void testModifiedDefaultConfig() throws Exception {
        setCommonVHostExpectations();
        context.checking(new Expectations() {
            {
                // Variations..
                allowing(mockDefVhostRef).getProperty("hostAlias");
                will(returnValue(returnValue(Arrays.asList("*:1", "*:3"))));
                allowing(mockDefVhostRef).getProperty("allowFromEndpointRef");
                will(returnValue(null));

                one(mockDefaultHost).getAliases();
                will(returnValue(Arrays.asList("*:1", "*:3")));
            }
        });

        Map<String, Object> config = new HashMap<String, Object>();
        setDefaultConfig(config);
        Map<String, List<VHostData>> vhostAliasData = new HashMap<String, List<VHostData>>();
        PluginGenerator pluginGen = new PluginGenerator(config, mockLocationAdmin, mockBundleContext);

        // First variation
        // This configuration is JUST the default_host, but one of the aliases has been changed.
        Set<DynamicVirtualHost> virtualHostSet = pluginGen.processVirtualHosts(mockVhostMgr, vhostAliasData, mockEndpointInfo, element);
        assertEquals("There should be one element in the virtual host set", 1, virtualHostSet.size());
        assertSame("The default host object should be in the virtual host set", mockDefaultHost, virtualHostSet.iterator().next());

        assertEquals("There should be one element in the virtual host alias data", 1, vhostAliasData.size());
        List<VHostData> data = vhostAliasData.get("default_host");
        assertNotNull("There should be a default_host element in the vhostAliasData map", data);

        // There will now be more aliases, because the host aliases were explicitly configured.
        assertEquals("There should be two elements in the VHostData", 2, data.size());
        assertTrue("VHostData should contain an alias for *:1", data.contains(new VHostData("*", 1)));
        assertTrue("VHostData should contain an alias for *:3", data.contains(new VHostData("*", 3)));
        assertTrue("comment about missing port 80", outputMgr.checkForStandardOut("(\\*:80)"));
        assertTrue("comment about missing port 443", outputMgr.checkForStandardOut("(\\*:443)"));
        vhostAliasData.clear();
    }

    @Test
    public void testProcessTwoHosts() throws Exception {
        final DynamicVirtualHost mockAltHost = context.mock(DynamicVirtualHost.class, "alternate");
        final ServiceReference<?> mockAltVhostRef = context.mock(ServiceReference.class, "alternateRef");

        setCommentExpectations();

        context.checking(new Expectations() {
            {
                allowing(mockLocationAdmin).getServerName();
                will(returnValue("SystemProvidedServerName"));

                allowing(mockBundleContext).getAllServiceReferences(null, "(&(service.factoryPid=com.ibm.ws.http.virtualhost)(|(enabled=true)(id=default_host)))");
                will(returnValue(new ServiceReference<?>[] { mockDefVhostRef, mockAltVhostRef }));

                allowing(mockDefVhostRef).getProperty("id");
                will(returnValue("default_host"));
                allowing(mockDefVhostRef).getProperty("hostAlias");
                will(returnValue(returnValue(null)));
                allowing(mockDefVhostRef).getProperty("allowFromEndpointRef");
                will(returnValue(null));

                allowing(mockAltVhostRef).getProperty("id");
                will(returnValue("alternate"));
                allowing(mockAltVhostRef).getProperty("allowFromEndpointRef");
                will(returnValue(null));

                allowing(mockVhostMgr).getVirtualHosts();
                will(returnIterator(mockDefaultHost, mockAltHost));

                allowing(mockDefaultHost).getName();
                will(returnValue("default_host"));
                allowing(mockDefaultHost).getAliases();
                will(returnValue(Arrays.asList("*:1", "*:2")));

                allowing(mockAltHost).getName();
                will(returnValue("alternate"));
                allowing(mockAltHost).getAliases();
                will(returnValue(Arrays.asList("*:3", "*:4")));
            }
        });

        Map<String, Object> config = new HashMap<String, Object>();
        setDefaultConfig(config);
        PluginGenerator pluginGen = new PluginGenerator(config, mockLocationAdmin, mockBundleContext);

        // Map of virtual host name to the list of alias data being collected...
        Map<String, List<VHostData>> vhostAliasData = new HashMap<String, List<VHostData>>();

        // Process the virtual host configuration..
        Set<DynamicVirtualHost> virtualHostSet = pluginGen.processVirtualHosts(mockVhostMgr, vhostAliasData, mockEndpointInfo, element);
    }

    private String getCanonicalHost(String host) throws UnknownHostException {
        InetAddress addr = InetAddress.getByName(host);
        String canonHost = addr.getCanonicalHostName();
        // If this is an IPv6 address, we need extra text to make it usable in messages
        if (addr instanceof Inet6Address && canonHost.contains(":") && !canonHost.startsWith("[")) {
            canonHost = "[" + canonHost + "]";
        }

        System.out.println("getCanonicalHost: " + host + " --> " + canonHost);
        return canonHost;
    }

    /**
     * Test method for {@link PluginGenerator#tryDetermineHostName(String, String, String, boolean)}
     */
    @Test
    public void testTryDetermineHostName() throws Exception {
        String hostName = PluginGenerator.tryDetermineHostName("*", "localhost", false);
        assertFalse("Should resolve * to something other than localhost", "localhost".equals(hostName));

        String canonHost = getCanonicalHost("127.0.0.1");
        hostName = PluginGenerator.tryDetermineHostName("127.0.0.1", "we.should.not.see.this", false);
        assertEquals("Should resolve 127.0.0.1 to " + canonHost, canonHost, hostName);

        canonHost = getCanonicalHost("::1");
        hostName = PluginGenerator.tryDetermineHostName("::1", "we.should.not.see.this", false);
        assertEquals("::1 should resolve to " + canonHost, canonHost, hostName);

        canonHost = getCanonicalHost("[::1]");
        hostName = PluginGenerator.tryDetermineHostName("[::1]", "we.should.not.see.this", false);
        assertEquals("[::1] should resolve to " + canonHost, canonHost, hostName);
    }

    @Test
    public void testResolveHostName() throws Exception {
        // This behavior is somewhat different than what is done in http endpoint. Since the plugin generator
        // is run against a running server, it will only see ports that successfully bound

        //  defaultHostName == localhost (which is the default value), so it should try to resolve '*' to something else
        String hostName = PluginGenerator.tryDetermineHostName("*", "localhost", false);
        assertFalse("Should resolve * to something other than localhost", "localhost".equals(hostName));

        //  defaultHostName is something specific, so it should use that instead
        // (defaultHostName is not canonicalized, left as-is, but must be something reachable from this machine)
        hostName = PluginGenerator.tryDetermineHostName("*", "127.0.0.1", false);
        assertEquals("Should resolve * to 127.0.0.1, due to default host", "127.0.0.1", hostName);

        // specify a specific host value that is a bunch of garbage.
        hostName = PluginGenerator.tryDetermineHostName("unresolvable.nonsense.no.way", "we.should.not.see.this.either", false);
        assertEquals("Should resolve localhost, due to unresolvable value", "localhost", hostName);

        // specify a defaultHostName value that is a bunch of garbage.
        hostName = PluginGenerator.tryDetermineHostName("*", "we.should.not.see.this", false);
        assertEquals("Should resolve localhost, due to unresolvable defaultHostName value", "localhost", hostName);

        // The defaultHostName is empty, so is not used. The answer should not be localhost
        hostName = PluginGenerator.tryDetermineHostName("*", "", false);
        assertFalse("Should resolve * to something other than localhost, defaultHostName is also empty", "localhost".equals(hostName));
    }

    // Expectations for testing generateXML method
    private void setXMLGenerateExpectations() throws Exception {
        context.checking(new Expectations() {
            {
                allowing(mockBundleContext).getAllServiceReferences(null, "(&(enabled=true)(|(httpPort>=1)(httpsPort>=1))(service.pid=Endpoint1))");
                will(returnValue(new ServiceReference<?>[] { mockEndpointInfoRef }));
                allowing(mockEndpointInfoRef).getProperty("id");
                will(returnValue("Endpoint1"));

                allowing(mockEndpointInfo).getEndpointId();
                will(returnValue(mockEndpointInfo.toString()));
                one(mockEndpointInfoRef).getProperty("_defaultHostName");
                will(returnValue("localhost")); // this is the default, will always be present in the system
                one(mockEndpointInfoRef).getProperty("host");
                will(returnValue("*"));
                one(mockEndpointInfoRef).getProperty("httpPort");
                will(returnValue(1));
                one(mockEndpointInfoRef).getProperty("httpsPort");
                will(returnValue(-1));
                allowing(mockDefVhostRef).getProperty("hostAlias");
                will(returnValue(null));
                allowing(mockDefVhostRef).getProperty("allowFromEndpointRef");
                will(returnValue(null));
                allowing(mockEndpointInfo).getEndpointId();
                will(returnValue(mockEndpointInfo.toString()));
                allowing(mockSessionManager).getCloneSeparator();
                will(returnValue(':'));
                allowing(mockSessionManager).getCloneID();
                will(returnValue("ServerCloneID"));
                allowing(mockDefaultHost);
                allowing(mockSessionManager);
            }
        });
    }

    // Config settings required for XML generation - these are defaults that would be automatically provided
    // in a live server process.
    private void setDefaultConfig(Map<String, Object> config) throws Exception {
        config.put("pluginInstallRoot", "/opt/IBM/WebSphere/Plugins");
        config.put("httpEndpointRef", "Endpoint1");
        config.put("webserverName", "webserver1");
        config.put("webserverPort", "80");
        config.put("webserverSecurePort", "443");
        config.put("httpEndpointRef", "Endpoint1");
        config.put("httpEndpointRef", "Endpoint1");
        config.put("ipv6Preferred", new Boolean(false));
        config.put("sslKeyringLocation", "keyringString");
        config.put("sslStashfileLocation", "stashfileString");
        config.put("serverIOTimeout", new Long(900));
        config.put("connectTimeout", new Long(5));
        config.put("extendedHandshake", new Boolean(false));
        config.put("waitForContinue", new Boolean(false));
        config.put("logDirLocation", "/opt/IBM/WebSphere/Plugins/logs/webserver1");
        config.put("serverIOTimeoutRetry", new Integer(-1));
        config.put("loadBalanceWeight", new Integer(20));
        config.put("serverRole", "PRIMARY");
    }

    @Test
    public void testServerIOTimeoutRetry() throws Exception {
        setCommonVHostExpectations();
        setXMLGenerateExpectations();
        setCommentExpectations();

        // set expectations specific for this test
        context.checking(new Expectations() {
            {
                allowing(mockLocationAdmin).getServerOutputResource("plugin-cfg.xml");
                will(returnValue(mockWsResource));
                allowing(mockWsResource).putStream();
                will(returnValue(new FileOutputStream(new File(testClassesDir + "/plugin-cfg.xml"))));
                allowing(mockWsResource).asFile();
                will(returnValue((new File(testClassesDir + "/plugin-cfg.xml"))));
            }
        });

        Map<String, Object> config = new HashMap<String, Object>();
        setDefaultConfig(config);
        // set config values for this test
        config.put("serverIOTimeoutRetry", new Integer(14));

        PluginGenerator pluginGen = new PluginGenerator(config, mockLocationAdmin, mockBundleContext);
        pluginGen.generateXML("userSpecifiedWebserverLocation", "userSpecifiedServerName", mockWebContainer, mockSessionManager, mockVhostMgr, mockLocationAdmin, false, null);

        // check that the config file was created
        File testfile = new File(testClassesDir + "/plugin-cfg.xml");
        assertTrue(testfile.exists());
        // and that it contains the serverIOTimeoutRetry value
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        Document dom = null;
        try {
            //Using factory get an instance of document builder
            DocumentBuilder db = dbf.newDocumentBuilder();
            //parse using builder to get DOM representation of the XML file
            dom = db.parse(testfile);
            Element docEle = dom.getDocumentElement();
            //get a nodelist of ServerCluster elements
            NodeList nl = docEle.getElementsByTagName("ServerCluster");
            // we're only expecting one ServerCluser
            assertEquals(1, nl.getLength());
            Node node = nl.item(0);
            Element eElement = (Element) node;
            String value = eElement.getAttribute("ServerIOTimeoutRetry");
            assertEquals("14", value);
        } catch (ParserConfigurationException pce) {
            pce.printStackTrace();
        } catch (SAXException se) {
            se.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        // rename generated file to leave a clean space for the next test, but keep the file for debug
        testfile.renameTo(new File(testClassesDir + "/serverIOTimeoutRetry-plugin-cfg.xml"));
    }

    private void commonImplicitSetup() throws IOException {
        // create folder to contain file
        new File(testClassesDir + "/logs/state").mkdirs();

        // set expectations specific for this test
        context.checking(new Expectations() {
            {
                allowing(mockLocationAdmin).getServerOutputResource("logs/state/plugin-cfg.xml");
                will(returnValue(mockWsResource));
                allowing(mockWsResource).putStream();
                will(returnValue(new FileOutputStream(new File(testClassesDir + "/logs/state/plugin-cfg.xml"))));
                allowing(mockWsResource).asFile();
                will(returnValue(new File(testClassesDir + "/logs/state/plugin-cfg.xml")));
            }
        });
    }

    // Test generation of XML file via implicit request - which writes to different location and distinguishes
    // web server and app server names correctly
    @Test
    public void testImplicitDefaultXMLGenerate() throws Exception {
        setCommonVHostExpectations();
        setXMLGenerateExpectations();
        setCommentExpectations();
        commonImplicitSetup();

        Map<String, Object> config = new HashMap<String, Object>();
        setDefaultConfig(config);

        // invoke generateXML method specifying implicit request and not overriding webserver location or app server name (last argument is true)
        PluginGenerator pluginGen = new PluginGenerator(config, mockLocationAdmin, mockBundleContext);
        pluginGen.generateXML(null, null, mockWebContainer, mockSessionManager, mockVhostMgr, mockLocationAdmin, true, null);

        // check that the config file was created
        File testfile = new File(testClassesDir + "/logs/state/plugin-cfg.xml");
        assertTrue(testfile.exists());
        // and that it contains the correct default values
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        Document dom = null;
        try {
            //Using factory get an instance of document builder
            DocumentBuilder db = dbf.newDocumentBuilder();
            //parse using builder to get DOM representation of the XML file
            dom = db.parse(testfile);
            Element docEle = dom.getDocumentElement();
            // find the PluginInstallRoot property
            String installRoot = null;
            //get a nodelist of ServerCluster elements
            NodeList nl = docEle.getElementsByTagName("Property");
            System.out.println("number of Property elements is: " + nl.getLength());
            for (int temp = 0; temp < nl.getLength(); temp++) {
                Node nNode = nl.item(temp);
                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) nNode;
                    if (eElement.getAttribute("Name").equals("PluginInstallRoot"))
                        installRoot = eElement.getAttribute("Value");
                }
            }
            assertEquals("/opt/IBM/WebSphere/Plugins", installRoot);
            // make sure plugin log file name was correctly constructed
            NodeList nl2 = docEle.getElementsByTagName("Log");
            // we're only expecting one entry for Log
            assertEquals(1, nl2.getLength());
            Node node = nl2.item(0);
            Element eElement = (Element) node;
            String value = eElement.getAttribute("Name");
            assertEquals("/opt/IBM/WebSphere/Plugins/logs/webserver1/http_plugin.log", value);

        } catch (ParserConfigurationException pce) {
            pce.printStackTrace();
        } catch (SAXException se) {
            se.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        // rename generated file to leave a clean space for the next test, but keep the file for debug
        testfile.renameTo(new File(testClassesDir + "/logs/state/ImplicitDefaultXMLGenerate-plugin-cfg.xml"));
    }

    // Test generation of XML file via explicit request with user-provided plugin location and server name
    @Test
    public void testExplicitXMLGenerate() throws Exception {
        setCommonVHostExpectations();
        setXMLGenerateExpectations();
        setCommentExpectations();

        // set expectations specific for this test
        context.checking(new Expectations() {
            {
                allowing(mockLocationAdmin).getServerOutputResource("plugin-cfg.xml");
                will(returnValue(mockWsResource));
                allowing(mockWsResource).putStream();
                will(returnValue(new FileOutputStream(new File(testClassesDir + "/plugin-cfg.xml"))));
                allowing(mockWsResource).asFile();
                will(returnValue((new File(testClassesDir + "/plugin-cfg.xml"))));
            }
        });

        Map<String, Object> config = new HashMap<String, Object>();
        setDefaultConfig(config);

        // invoke generateXML method specifying implicit request (last argument is true)
        PluginGenerator pluginGen = new PluginGenerator(config, mockLocationAdmin, mockBundleContext);
        pluginGen.generateXML("userSpecifiedWebserverLocation", "userSpecifiedServerName", mockWebContainer, mockSessionManager, mockVhostMgr, mockLocationAdmin, false, null);

        // check that the config file was created
        File testfile = new File(testClassesDir + "/plugin-cfg.xml");
        assertTrue(testfile.exists());
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        Document dom = null;
        try {
            //Using factory get an instance of document builder
            DocumentBuilder db = dbf.newDocumentBuilder();
            //parse using builder to get DOM representation of the XML file
            dom = db.parse(testfile);
            Element docEle = dom.getDocumentElement();

            // find the PluginInstallRoot property
            String installRoot = null;
            //get a nodelist of ServerCluster elements
            NodeList nl = docEle.getElementsByTagName("Property");
            for (int temp = 0; temp < nl.getLength(); temp++) {
                Node nNode = nl.item(temp);
                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) nNode;
                    if (eElement.getAttribute("Name").equals("PluginInstallRoot"))
                        installRoot = eElement.getAttribute("Value");
                }
            }
            assertEquals("userSpecifiedWebserverLocation", installRoot);

            // make sure plugin log file name was correctly constructed
            NodeList nl2 = docEle.getElementsByTagName("Log");
            // we're only expecting one entry for Log
            assertEquals(1, nl2.getLength());
            Node node = nl2.item(0);
            Element eElement = (Element) node;
            String value = eElement.getAttribute("Name");
            assertEquals("userSpecifiedWebserverLocation/logs/userSpecifiedServerName/http_plugin.log", value);
        } catch (ParserConfigurationException pce) {
            pce.printStackTrace();
        } catch (SAXException se) {
            se.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        // rename generated file to leave a clean space for the next test, but keep the file for debug
        testfile.renameTo(new File(testClassesDir + "/ExplicitXMLGenerate-plugin-cfg.xml"));
    }

    @Test
    public void testAdditionalProperties() throws Exception {
        setCommonVHostExpectations();
        setXMLGenerateExpectations();
        setCommentExpectations();

        // set expectations specific for this test
        context.checking(new Expectations() {
            {
                allowing(mockLocationAdmin).getServerOutputResource("plugin-cfg.xml");
                will(returnValue(mockWsResource));
                allowing(mockWsResource).putStream();
                will(returnValue(new FileOutputStream(new File(testClassesDir + "/plugin-cfg.xml"))));
                allowing(mockWsResource).asFile();
                will(returnValue((new File(testClassesDir + "/plugin-cfg.xml"))));
            }
        });

        Map<String, Object> config = new HashMap<String, Object>();
        setDefaultConfig(config);
        // set config values for this test
        config.put("extraConfigProperties.0.config.referenceType", "true");
        config.put("extraConfigProperties.0.myKey", "myValue");
        config.put("extraConfigProperties.0.secondKey", "secondValue");
        config.put("not.an.extra.property", "invalid");
        // test override of property with hard-coded default
        config.put("extraConfigProperties.0.RefreshInterval", "99");

        PluginGenerator pluginGen = new PluginGenerator(config, mockLocationAdmin, mockBundleContext);
        pluginGen.generateXML("userSpecifiedWebserverLocation", "userSpecifiedServerName", mockWebContainer, mockSessionManager, mockVhostMgr, mockLocationAdmin, false, null);

        // check that the config file was created
        File testfile = new File(testClassesDir + "/plugin-cfg.xml");
        assertTrue(testfile.exists());
        // and that it contains the additional properties in the <Config> element
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        Document dom = null;
        try {
            //Using factory get an instance of document builder
            DocumentBuilder db = dbf.newDocumentBuilder();
            //parse using builder to get DOM representation of the XML file
            dom = db.parse(testfile);
            Element docEle = dom.getDocumentElement();
            String value = docEle.getAttribute("myKey");
            assertNotNull(value);
            assertEquals("myValue", value);
            value = docEle.getAttribute("secondKey");
            assertNotNull(value);
            assertEquals("secondValue", value);
            value = docEle.getAttribute("not.an.extra.property");
            assert (value.isEmpty());
            value = docEle.getAttribute("RefreshInterval");
            assertNotNull(value);
            assertEquals("99", value);
            // check one of the hard-coded defaults without override
            value = docEle.getAttribute("IISPluginPriority");
            assertNotNull(value);
            assertEquals("High", value);
        } catch (ParserConfigurationException pce) {
            pce.printStackTrace();
        } catch (SAXException se) {
            se.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        // rename generated file to leave a clean space for the next test, but keep the file for debug
        testfile.renameTo(new File(testClassesDir + "/AdditionalProperties-plugin-cfg.xml"));
    }

    @Test
    public void testLoadBalanceWeight() throws Exception {
        setCommonVHostExpectations();
        setXMLGenerateExpectations();
        setCommentExpectations();

        // set expectations specific for this test
        context.checking(new Expectations() {
            {
                allowing(mockLocationAdmin).getServerOutputResource("plugin-cfg.xml");
                will(returnValue(mockWsResource));
                allowing(mockWsResource).putStream();
                will(returnValue(new FileOutputStream(new File(testClassesDir + "/plugin-cfg.xml"))));
                allowing(mockWsResource).asFile();
                will(returnValue((new File(testClassesDir + "/plugin-cfg.xml"))));
            }
        });

        Map<String, Object> config = new HashMap<String, Object>();
        setDefaultConfig(config);
        // set config values for this test
        config.put("loadBalanceWeight", new Integer(15));

        PluginGenerator pluginGen = new PluginGenerator(config, mockLocationAdmin, mockBundleContext);
        pluginGen.generateXML("userSpecifiedWebserverLocation", "userSpecifiedServerName", mockWebContainer, mockSessionManager, mockVhostMgr, mockLocationAdmin, false, null);

        // check that the config file was created
        File testfile = new File(testClassesDir + "/plugin-cfg.xml");
        assertTrue(testfile.exists());
        // and that it contains the loadBalanceWeight value
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        Document dom = null;
        try {
            //Using factory get an instance of document builder
            DocumentBuilder db = dbf.newDocumentBuilder();
            //parse using builder to get DOM representation of the XML file
            dom = db.parse(testfile);
            Element docEle = dom.getDocumentElement();
            //get a nodelist of ServerCluster elements
            NodeList nl = docEle.getElementsByTagName("ServerCluster");
            // we're only expecting one ServerCluser
            assertEquals(1, nl.getLength());
            Node node = nl.item(0);
            Element eElement = (Element) node;
            //we're in the first Server tag of the ServerCluster
            NodeList nl1 = eElement.getElementsByTagName("Server");
            Node node1 = nl1.item(0);
            Element eElement1 = (Element) node1;
            String value = eElement1.getAttribute("LoadBalanceWeight");
            assertEquals("15", value);
        } catch (ParserConfigurationException pce) {
            pce.printStackTrace();
        } catch (SAXException se) {
            se.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        // rename generated file to leave a clean space for the next test, but keep the file for debug
        testfile.renameTo(new File(testClassesDir + "/loadBalanceWeight-plugin-cfg.xml"));
    }

    // Test generation of XML file and check value of ESIEnable whenh user-provided esiDisable is set in server.xml
    @Test
    public void testDisableESI() throws Exception {
        setCommonVHostExpectations();
        setXMLGenerateExpectations();
        setCommentExpectations();

        // set expectations specific for this test
        context.checking(new Expectations() {
            {
                allowing(mockLocationAdmin).getServerOutputResource("plugin-cfg.xml");
                will(returnValue(mockWsResource));
                allowing(mockWsResource).putStream();
                will(returnValue(new FileOutputStream(new File(testClassesDir + "/plugin-cfg.xml"))));
                allowing(mockWsResource).asFile();
                will(returnValue((new File(testClassesDir + "/plugin-cfg.xml"))));
            }
        });

        Map<String, Object> config = new HashMap<String, Object>();
        setDefaultConfig(config);
        // set config values for this test
        config.put("ESIEnable", new Boolean(false));
        config.put("ESIMaxCacheSize", new Integer(15));
        config.put("ESIInvalidationMonitor", new Boolean(true));
        config.put("ESIEnableToPassCookies", new Boolean(true));
        PluginGenerator pluginGen = new PluginGenerator(config, mockLocationAdmin, mockBundleContext);
        pluginGen.generateXML("userSpecifiedWebserverLocation", "userSpecifiedServerName", mockWebContainer, mockSessionManager, mockVhostMgr, mockLocationAdmin, false, null);

        // check that the config file was created
        File testfile = new File(testClassesDir + "/plugin-cfg.xml");
        assertTrue(testfile.exists());

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        Document dom = null;
        try {
            //Using factory get an instance of document builder
            DocumentBuilder db = dbf.newDocumentBuilder();
            //parse using builder to get DOM representation of the XML file
            dom = db.parse(testfile);
            Element docEle = dom.getDocumentElement();
            //get a nodelist of ServerCluster elements
            NodeList nl = docEle.getElementsByTagName("ServerCluster");
            // we're only expecting one ServerCluser
            assertEquals(1, nl.getLength());
            Node node = nl.item(0);
            Element eElement = (Element) node;
            //we're in the first Server tag of the ServerCluster
            NodeList nl1 = eElement.getElementsByTagName("Server");
            Node node1 = nl1.item(0);
            Element eElement1 = (Element) node1;
            String value = eElement1.getAttribute("ESIEnable");
            assertTrue("Check if ESIEnable is set in server.xml", (new Boolean(false)).equals(value));
            value = eElement1.getAttribute("ESIMaxCacheSize");
            assertTrue("Check if ESIMaxCacheSize is set in server.xml", (new Integer(15)).equals(value));
            value = eElement1.getAttribute("ESIInvalidationMonitor");
            assertTrue("Check if ESIInvalidationMonitor is set in server.xml", (new Boolean(true)).equals(value));
            value = eElement1.getAttribute("ESIEnableToPassCookies");
            assertTrue("Check if ESIEnableToPassCookies is set in server.xml", (new Boolean(true)).equals(value));

        } catch (ParserConfigurationException pce) {
            pce.printStackTrace();
        } catch (SAXException se) {
            se.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        // rename generated file to leave a clean space for the next test, but keep the file for debug
        testfile.renameTo(new File(testClassesDir + "/disableesi-plugin-cfg.xml"));
    }

    @Test
    public void testServerRole() throws Exception {
        setCommonVHostExpectations();
        setXMLGenerateExpectations();
        setCommentExpectations();

        // set expectations specific for this test
        context.checking(new Expectations() {
            {
                allowing(mockLocationAdmin).getServerOutputResource("plugin-cfg.xml");
                will(returnValue(mockWsResource));
                allowing(mockWsResource).putStream();
                will(returnValue(new FileOutputStream(new File(testClassesDir + "/plugin-cfg.xml"))));
                allowing(mockWsResource).asFile();
                will(returnValue((new File(testClassesDir + "/plugin-cfg.xml"))));
            }
        });

        Map<String, Object> config = new HashMap<String, Object>();
        setDefaultConfig(config);
        // set config values for this test
        config.put("serverRole", "BACKUP");

        PluginGenerator pluginGen = new PluginGenerator(config, mockLocationAdmin, mockBundleContext);
        pluginGen.generateXML("userSpecifiedWebserverLocation", "userSpecifiedServerName", mockWebContainer, mockSessionManager, mockVhostMgr, mockLocationAdmin, false, null);

        // check that the config file was created
        File testfile = new File(testClassesDir + "/plugin-cfg.xml");
        assertTrue(testfile.exists());
        // and that it contains the loadBalanceWeight value
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        Document dom = null;
        try {
            //Using factory get an instance of document builder
            DocumentBuilder db = dbf.newDocumentBuilder();
            //parse using builder to get DOM representation of the XML file
            dom = db.parse(testfile);
            Element docEle = dom.getDocumentElement();
            //get a nodelist of ServerCluster elements
            NodeList nl = docEle.getElementsByTagName("ServerCluster");
            // we're only expecting one ServerCluser
            assertEquals(1, nl.getLength());
            Node node = nl.item(0);
            Element eElement = (Element) node;
            //we're in the first Server tag of the ServerCluster
            NodeList nlBackupServers = eElement.getElementsByTagName("BackupServers");
            Node nodeBackupServers = nlBackupServers.item(0);
            Element eElementBackupServers = (Element) nodeBackupServers;
            //Counting how many Server tags inside BackupServers
            int nodesInBackupServers = nodeBackupServers.getChildNodes().getLength();
            int backupServersCount = 0;
            for (int i = 0; i < nodesInBackupServers; i++) {
                if (nodeBackupServers.getChildNodes().item(i).getNodeType() == Node.ELEMENT_NODE && nodeBackupServers.getChildNodes().item(i).getNodeName().equals("Server")) {
                    backupServersCount++;
                }
            }
            //We're expecting one server listed in the body of the BackupServers tag
            assertEquals(1, backupServersCount);
        } catch (ParserConfigurationException pce) {
            pce.printStackTrace();
        } catch (SAXException se) {
            se.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        // rename generated file to leave a clean space for the next test, but keep the file for debug
        testfile.renameTo(new File(testClassesDir + "/serverRole-plugin-cfg.xml"));
    }
    
    @Test
    public void testPersistTimeoutReduction() throws Exception {
        setCommonVHostExpectations();
        setXMLGenerateExpectations();
        setCommentExpectations();

        // set expectations specific for this test
        context.checking(new Expectations() {
            {
                allowing(mockLocationAdmin).getServerOutputResource("plugin-cfg.xml");
                will(returnValue(mockWsResource));
                allowing(mockWsResource).putStream();
                will(returnValue(new FileOutputStream(new File(testClassesDir + "/plugin-cfg.xml"))));
                allowing(mockWsResource).asFile();
                will(returnValue((new File(testClassesDir + "/plugin-cfg.xml"))));
            }
        });

        Map<String, Object> config = new HashMap<String, Object>();
        setDefaultConfig(config);
        // set config values for this test
        config.put("persistTimeoutReduction", new Integer(5));

        PluginGenerator pluginGen = new PluginGenerator(config, mockLocationAdmin, mockBundleContext);
        pluginGen.generateXML("userSpecifiedWebserverLocation", "userSpecifiedServerName", mockWebContainer, mockSessionManager, mockVhostMgr, mockLocationAdmin, false, null);

        // check that the config file was created
        File testfile = new File(testClassesDir + "/plugin-cfg.xml");
        assertTrue(testfile.exists());

     // and that it contains the correct default values
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        Document dom = null;
        try {
            //Using factory get an instance of document builder
            DocumentBuilder db = dbf.newDocumentBuilder();
            //parse using builder to get DOM representation of the XML file
            dom = db.parse(testfile);
            Element docEle = dom.getDocumentElement();
            //get a nodelist of ServerCluster elements
            NodeList nl = docEle.getElementsByTagName("ServerCluster");
            // we're only expecting one ServerCluser
            assertEquals(1, nl.getLength());
            Node node = nl.item(0);
            Element eElement = (Element) node;
            //we're in the first Server tag of the ServerCluster
            NodeList nl1 = eElement.getElementsByTagName("Server");
            Node node1 = nl1.item(0);
            Element eElement1 = (Element) node1;
            //we're in the first Transport tag of the Server
            NodeList nl2 = eElement1.getElementsByTagName("Transport");
            Node node2 = nl2.item(0);
            Element eElement2 = (Element) node2;

            String value = eElement2.getAttribute("ConnectionTTL");
            assertEquals("25", value); //25 is persistTimeout[30] minus persistTimeoutReduction[specified above as 5]
        } catch (ParserConfigurationException pce) {
            pce.printStackTrace();
        } catch (SAXException se) {
            se.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }    
    }
}
