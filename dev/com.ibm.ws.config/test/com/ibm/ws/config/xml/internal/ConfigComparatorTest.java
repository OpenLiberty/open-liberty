/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.config.xml.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.StringReader;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.osgi.service.metatype.AttributeDefinition;

import com.ibm.websphere.config.ConfigParserException;
import com.ibm.websphere.config.ConfigUpdateException;
import com.ibm.websphere.config.ConfigValidationException;
import com.ibm.ws.config.xml.internal.ConfigComparator.ComparatorResult;
import com.ibm.ws.config.xml.internal.ConfigComparator.DeltaType;
import com.ibm.ws.config.xml.internal.variables.ConfigVariableRegistry;
import com.ibm.ws.kernel.service.location.internal.VariableRegistryHelper;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;

import test.common.SharedLocationManager;
import test.common.SharedOutputManager;
import test.utils.SharedConstants;

public class ConfigComparatorTest {

    static WsLocationAdmin libertyLocation;
    static XMLConfigParser configParser;
    static SharedOutputManager outputMgr;
    static ConfigVariableRegistry variableRegistry = new ConfigVariableRegistry(new VariableRegistryHelper(), new String[0], null, libertyLocation);

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // make stdout/stderr "quiet"-- no output will show up for test
        // unless one of the copy methods or documentThrowable is called
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.captureStreams();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        // Make stdout and stderr "normal"
        outputMgr.restoreStreams();

        // Restore back to old kernel and let next test case set to new kernel
        // as needed
        SharedLocationManager.resetWsLocationAdmin();
    }

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
        // Clear the output generated after each method invocation, this keeps
        // things sane
        outputMgr.resetStreams();
    }

    private void changeLocationSettings(String profileName) {
        SharedLocationManager.createDefaultLocations(SharedConstants.SERVER_XML_INSTALL_ROOT, profileName);
        libertyLocation = (WsLocationAdmin) SharedLocationManager.getLocationInstance();

        configParser = new XMLConfigParser(libertyLocation, variableRegistry);
    }

    private ServerConfiguration parseServerConfiguration(String xml) throws ConfigParserException, ConfigValidationException {
        return configParser.parseServerConfiguration(new StringReader(xml), new ServerConfiguration());
    }

    @Test
    public void testSimpleSingletonChange() throws Exception {
        changeLocationSettings("default");

        ConfigComparator comparator;
        List<ConfigDelta> deltas;

        ServerConfiguration oldConfig = parseServerConfiguration("<server><httpConnector clientAuth=\"false\"/></server>");
        ServerConfiguration newConfig;

        // no change
        newConfig = parseServerConfiguration("<server><httpConnector clientAuth=\"false\"/></server>");
        comparator = new ConfigComparator(oldConfig, newConfig, null);
        deltas = comparator.computeDelta().getConfigDelta();
        assertEquals("No change", 0, deltas.size());

        // removed httpConnector
        newConfig = parseServerConfiguration("<server></server>");
        comparator = new ConfigComparator(oldConfig, newConfig, null);
        deltas = comparator.computeDelta().getConfigDelta();
        assertRemoved(deltas, "httpConnector", null);

        // added foo
        newConfig = parseServerConfiguration("<server><httpConnector clientAuth=\"false\"/><foo bar=\"test\"/></server>");
        comparator = new ConfigComparator(oldConfig, newConfig, null);
        deltas = comparator.computeDelta().getConfigDelta();
        assertAdded(deltas, "foo", null);

        // modified clientAuth in httpConnector
        newConfig = parseServerConfiguration("<server><httpConnector clientAuth=\"true\"/></server>");
        comparator = new ConfigComparator(oldConfig, newConfig, null);
        deltas = comparator.computeDelta().getConfigDelta();
        assertModified(deltas, "httpConnector", null);
    }

    @Test
    public void testSimpleFactoryChange() throws Exception {
        changeLocationSettings("default");

        ConfigComparator comparator;
        List<ConfigDelta> deltas;

        ServerConfiguration oldConfig = parseServerConfiguration("<server><threadPool id=\"one\" maxThreads=\"10\"/><threadPool id=\"two\" maxThreads=\"20\"/></server>");
        ServerConfiguration newConfig;

        // no change
        newConfig = parseServerConfiguration("<server><threadPool id=\"one\" maxThreads=\"10\"/><threadPool id=\"two\" maxThreads=\"20\"/></server>");
        comparator = new ConfigComparator(oldConfig, newConfig, null);
        deltas = comparator.computeDelta().getConfigDelta();
        assertEquals("No change", 0, deltas.size());

        // removed one instance
        newConfig = parseServerConfiguration("<server><threadPool id=\"two\" maxThreads=\"20\"/></server>");
        comparator = new ConfigComparator(oldConfig, newConfig, null);
        deltas = comparator.computeDelta().getConfigDelta();
        assertRemoved(deltas, "threadPool", "one");

        // added three instance
        newConfig = parseServerConfiguration("<server><threadPool id=\"one\" maxThreads=\"10\"/><threadPool id=\"two\" maxThreads=\"20\"/><threadPool id=\"three\" maxThreads=\"40\"/></server>");
        comparator = new ConfigComparator(oldConfig, newConfig, null);
        deltas = comparator.computeDelta().getConfigDelta();
        assertAdded(deltas, "threadPool", "three");

        // modified one instance - changed maxThreads
        newConfig = parseServerConfiguration("<server><threadPool id=\"one\" maxThreads=\"100\"/><threadPool id=\"two\" maxThreads=\"20\"/></server>");
        comparator = new ConfigComparator(oldConfig, newConfig, null);
        deltas = comparator.computeDelta().getConfigDelta();
        assertModified(deltas, "threadPool", "one");

        // added a new factory instance
        newConfig = parseServerConfiguration("<server><threadPool id=\"one\" maxThreads=\"10\"/><threadPool id=\"two\" maxThreads=\"20\"/><app id=\"1\"/></server>");
        comparator = new ConfigComparator(oldConfig, newConfig, null);
        deltas = comparator.computeDelta().getConfigDelta();
        assertAdded(deltas, "app", "1");
    }

    @Test
    public void testEnabledSingletonChange() throws Exception {
        changeLocationSettings("default");

        ConfigComparator comparator;
        List<ConfigDelta> deltas;

        ServerConfiguration oldConfig = parseServerConfiguration("<server><httpConnector clientAuth=\"false\"/></server>");
        ServerConfiguration newConfig;

        // remove httpConnector using "configurationEnabled"
        newConfig = parseServerConfiguration("<server><httpConnector clientAuth=\"true\" configurationEnabled=\"false\" /></server>");
        comparator = new ConfigComparator(oldConfig, newConfig, null);
        deltas = comparator.computeDelta().getConfigDelta();
        assertRemoved(deltas, "httpConnector", null);

        oldConfig = newConfig;

        // add httpConnector using "configurationEnabled"
        newConfig = parseServerConfiguration("<server><httpConnector clientAuth=\"true\" configurationEnabled=\"true\" /></server>");
        comparator = new ConfigComparator(oldConfig, newConfig, null);
        deltas = comparator.computeDelta().getConfigDelta();
        assertAdded(deltas, "httpConnector", null);
    }

    @Test
    public void testEnabledFactoryChange() throws Exception {
        changeLocationSettings("default");

        ConfigComparator comparator;
        List<ConfigDelta> deltas;

        ServerConfiguration oldConfig = parseServerConfiguration("<server><threadPool id=\"one\" maxThreads=\"10\"/><threadPool id=\"two\" maxThreads=\"20\"/></server>");
        ServerConfiguration newConfig;

        // remove instance "one" using "configurationEnabled"
        newConfig = parseServerConfiguration("<server><threadPool id=\"one\" maxThreads=\"10\" configurationEnabled=\"false\"/><threadPool id=\"two\" maxThreads=\"20\"/></server>");
        comparator = new ConfigComparator(oldConfig, newConfig, null);
        deltas = comparator.computeDelta().getConfigDelta();
        assertRemoved(deltas, "threadPool", "one");

        oldConfig = newConfig;

        // add instance "one" using "configurationEnabled"
        newConfig = parseServerConfiguration("<server><threadPool id=\"one\" maxThreads=\"10\" configurationEnabled=\"true\"/><threadPool id=\"two\" maxThreads=\"20\"/></server>");
        comparator = new ConfigComparator(oldConfig, newConfig, null);
        deltas = comparator.computeDelta().getConfigDelta();
        assertAdded(deltas, "threadPool", "one");
    }

    @Test
    @Ignore //invalid since nested requires metatype
    public void testEnabledNested() throws Exception {
        changeLocationSettings("default");

        ConfigComparator comparator;
        ConfigDelta delta;
        List<ConfigDelta> deltas;
        List<ConfigDelta> nestedDeltas;

        ServerConfiguration oldConfig = parseServerConfiguration("<server>" +
                                                                 "  <application id=\"app1\">" +
                                                                 "    <host ip=\"bart\">" +
                                                                 "      <port number=\"123\"/>" +
                                                                 "    </host>" +
                                                                 "  </application>" +
                                                                 "</server>");
        ServerConfiguration newConfig;

        // app1 removed via configurationEnabled
        newConfig = parseServerConfiguration("<server>" +
                                             "  <application id=\"app1\" configurationEnabled=\"false\">" +
                                             "    <host ip=\"bart\">" +
                                             "      <port number=\"123\"/>" +
                                             "    </host>" +
                                             "  </application>" +
                                             "</server>");

        comparator = new ConfigComparator(oldConfig, newConfig, null);
        deltas = comparator.computeDelta().getConfigDelta();
        assertEquals("No modifications", 1, deltas.size());
        delta = containsDelta(deltas, "application", "app1", DeltaType.REMOVED);
        assertNotNull(delta);
        nestedDeltas = delta.getNestedDelta();
        assertEquals(1, nestedDeltas.size());
        assertNotNull(containsDelta(nestedDeltas, "host", null, DeltaType.REMOVED));
        nestedDeltas = nestedDeltas.get(0).getNestedDelta();
        assertEquals(1, nestedDeltas.size());
        assertNotNull(containsDelta(nestedDeltas, "port", null, DeltaType.REMOVED));

        oldConfig = newConfig;

        // app1 added without port via configurationEnabled
        newConfig = parseServerConfiguration("<server>" +
                                             "  <application id=\"app1\" configurationEnabled=\"true\">" +
                                             "    <host ip=\"bart\">" +
                                             "      <port number=\"123\" configurationEnabled=\"false\"/>" +
                                             "    </host>" +
                                             "  </application>" +
                                             "</server>");

        comparator = new ConfigComparator(oldConfig, newConfig, null);
        deltas = comparator.computeDelta().getConfigDelta();
        assertEquals("No modifications", 1, deltas.size());
        delta = containsDelta(deltas, "application", "app1", DeltaType.ADDED);
        assertNotNull(delta);
        nestedDeltas = delta.getNestedDelta();
        assertEquals(1, nestedDeltas.size());
        assertNotNull(containsDelta(nestedDeltas, "host", null, DeltaType.ADDED));
        nestedDeltas = nestedDeltas.get(0).getNestedDelta();
        assertEquals(0, nestedDeltas.size());

        oldConfig = newConfig;

        // port added via configurationEnabled
        newConfig = parseServerConfiguration("<server>" +
                                             "  <application id=\"app1\" configurationEnabled=\"true\">" +
                                             "    <host ip=\"bart\">" +
                                             "      <port number=\"123\" configurationEnabled=\"true\"/>" +
                                             "    </host>" +
                                             "  </application>" +
                                             "</server>");

        comparator = new ConfigComparator(oldConfig, newConfig, null);
        deltas = comparator.computeDelta().getConfigDelta();
        assertEquals("No modifications", 1, deltas.size());
        delta = containsDelta(deltas, "application", "app1", DeltaType.MODIFIED);
        assertNotNull(delta);
        nestedDeltas = delta.getNestedDelta();
        assertEquals(1, nestedDeltas.size());
        assertNotNull(containsDelta(nestedDeltas, "host", null, DeltaType.MODIFIED));
        nestedDeltas = nestedDeltas.get(0).getNestedDelta();
        assertEquals(1, nestedDeltas.size());
        assertNotNull(containsDelta(nestedDeltas, "port", null, DeltaType.ADDED));
    }

    @Test
    public void testValueChanges() throws Exception {
        changeLocationSettings("default");

        ConfigComparator comparator;
        List<ConfigDelta> deltas;

        ServerConfiguration oldConfig;
        ServerConfiguration newConfig;

        newConfig = parseServerConfiguration("<server><httpConnector clientAuth=\"false\"/></server>");
        oldConfig = newConfig;
        comparator = new ConfigComparator(oldConfig, newConfig, null);
        deltas = comparator.computeDelta().getConfigDelta();
        assertEquals("No change", 0, deltas.size());

        oldConfig = newConfig;
        newConfig = parseServerConfiguration("<server><httpConnector clientAuth=\"false\"><a>value1</a></httpConnector></server>");
        comparator = new ConfigComparator(oldConfig, newConfig, null);
        deltas = comparator.computeDelta().getConfigDelta();
        assertModified(deltas, "httpConnector", null);

        oldConfig = newConfig;
        newConfig = parseServerConfiguration("<server><httpConnector clientAuth=\"false\"><a>value1</a><a>value2</a></httpConnector></server>");
        comparator = new ConfigComparator(oldConfig, newConfig, null);
        deltas = comparator.computeDelta().getConfigDelta();
        assertModified(deltas, "httpConnector", null);

        oldConfig = newConfig;
        newConfig = parseServerConfiguration("<server><httpConnector clientAuth=\"false\"></httpConnector></server>");
        comparator = new ConfigComparator(oldConfig, newConfig, null);
        deltas = comparator.computeDelta().getConfigDelta();
        assertModified(deltas, "httpConnector", null);

        oldConfig = newConfig;
        newConfig = parseServerConfiguration("<server><httpConnector clientAuth=\"true\"></httpConnector></server>");
        comparator = new ConfigComparator(oldConfig, newConfig, null);
        deltas = comparator.computeDelta().getConfigDelta();
        assertModified(deltas, "httpConnector", null);

        oldConfig = newConfig;
        newConfig = parseServerConfiguration("<server><httpConnector clientAuth=\"true\" port=\"5555\"></httpConnector></server>");
        comparator = new ConfigComparator(oldConfig, newConfig, null);
        deltas = comparator.computeDelta().getConfigDelta();
        assertModified(deltas, "httpConnector", null);
    }

    @Test
    @Ignore //invalid as nested requires metatype
    public void testNested() throws Exception {
        changeLocationSettings("default");

        ConfigComparator comparator;
        ConfigDelta delta;
        List<ConfigDelta> deltas;
        List<ConfigDelta> nestedDeltas;

        ServerConfiguration oldConfig = parseServerConfiguration("<server>" +
                                                                 "  <application id=\"app1\">" +
                                                                 "    <host ip=\"bart\"/>" +
                                                                 "  </application>" +
                                                                 "  <application id=\"app2\"/>" +
                                                                 "</server>");
        ServerConfiguration newConfig;

        // host and app1 modified
        newConfig = parseServerConfiguration("<server>" +
                                             "  <application id=\"app1\">" +
                                             "    <host ip=\"lisa\"/>" +
                                             "  </application>" +
                                             "  <application id=\"app2\"/>" +
                                             "</server>");
        comparator = new ConfigComparator(oldConfig, newConfig, null);
        deltas = comparator.computeDelta().getConfigDelta();
        assertEquals("No modifications", 1, deltas.size());
        delta = containsDelta(deltas, "application", "app1", DeltaType.MODIFIED);
        assertNotNull(delta);
        nestedDeltas = delta.getNestedDelta();
        assertEquals(1, nestedDeltas.size());
        assertNotNull(containsDelta(nestedDeltas, "host", null, DeltaType.MODIFIED));

        // app1, app2 modified, fileset added, host removed
        oldConfig = newConfig;
        newConfig = parseServerConfiguration("<server>" +
                                             "  <application id=\"app1\">" +
                                             "  </application>" +
                                             "  <application id=\"app2\">" +
                                             "    <fileset dir=\"lib\" />" +
                                             "  </application>" +
                                             "</server>");
        comparator = new ConfigComparator(oldConfig, newConfig, null);
        deltas = comparator.computeDelta().getConfigDelta();
        assertEquals("No modifications", 2, deltas.size());
        delta = containsDelta(deltas, "application", "app1", DeltaType.MODIFIED);
        assertNotNull(delta);
        nestedDeltas = delta.getNestedDelta();
        assertEquals(1, nestedDeltas.size());
        assertNotNull(containsDelta(nestedDeltas, "host", null, DeltaType.REMOVED));

        delta = containsDelta(deltas, "application", "app2", DeltaType.MODIFIED);
        assertNotNull(delta);
        nestedDeltas = delta.getNestedDelta();
        assertEquals(1, nestedDeltas.size());
        assertNotNull(containsDelta(nestedDeltas, "fileset", null, DeltaType.ADDED));

        // app2 and fileset removed
        oldConfig = newConfig;
        newConfig = parseServerConfiguration("<server>" +
                                             "  <application id=\"app1\">" +
                                             "  </application>" +
                                             "</server>");
        comparator = new ConfigComparator(oldConfig, newConfig, null);
        deltas = comparator.computeDelta().getConfigDelta();
        assertEquals("No modifications", 1, deltas.size());
        delta = containsDelta(deltas, "application", "app2", DeltaType.REMOVED);
        assertNotNull(delta);
        nestedDeltas = delta.getNestedDelta();
        assertEquals(1, nestedDeltas.size());
        assertNotNull(containsDelta(nestedDeltas, "fileset", null, DeltaType.REMOVED));
    }

    @Test
    public void testNestedReferences() throws Exception {
        changeLocationSettings("default");

        ConfigComparator comparator;
        List<ConfigDelta> deltas;

        ServerConfiguration oldConfig = parseServerConfiguration("<server>" +
                                                                 "  <host id=\"1\" ip=\"bart\"/>" +
                                                                 "  <application id=\"app1\">" +
                                                                 "    <host ref=\"1\"/>" +
                                                                 "  </application>" +
                                                                 "  <application id=\"app2\"/>" +
                                                                 "</server>");
        ServerConfiguration newConfig;

        // host and app1 should be modified
        newConfig = parseServerConfiguration("<server>" +
                                             "  <host id=\"1\" ip=\"lisa\"/>" +
                                             "  <application id=\"app1\">" +
                                             "    <host ref=\"1\"/>" +
                                             "  </application>" +
                                             "  <application id=\"app2\"/>" +
                                             "</server>");
        comparator = new ConfigComparator(oldConfig, newConfig, null);
        deltas = comparator.computeDelta().getConfigDelta();
        assertEquals("No modifications", 1, deltas.size());
        // assertTrue(containsDelta(deltas, "application", "app1", ConfigDelta.DeltaType.MODIFIED));
        assertNotNull(containsDelta(deltas, "host", null, DeltaType.MODIFIED));

        // app2 should be modified
        oldConfig = newConfig;
        newConfig = parseServerConfiguration("<server>" +
                                             "  <host id=\"1\" ip=\"lisa\"/>" +
                                             "  <application id=\"app1\">" +
                                             "    <host ref=\"1\"/>" +
                                             "  </application>" +
                                             "  <application id=\"app2\">" +
                                             "    <host ref=\"1\"/>" +
                                             "  </application>" +
                                             "</server>");
        comparator = new ConfigComparator(oldConfig, newConfig, null);
        deltas = comparator.computeDelta().getConfigDelta();
        assertEquals("No modifications", 1, deltas.size());
        assertNotNull(containsDelta(deltas, "application", "app2", DeltaType.MODIFIED));

        // host, app1, app2 should be modified
        oldConfig = newConfig;
        newConfig = parseServerConfiguration("<server>" +
                                             "  <host id=\"1\" ip=\"marge\"/>" +
                                             "  <application id=\"app1\">" +
                                             "    <host ref=\"1\"/>" +
                                             "  </application>" +
                                             "  <application id=\"app2\">" +
                                             "    <host ref=\"1\"/>" +
                                             "  </application>" +
                                             "</server>");
        comparator = new ConfigComparator(oldConfig, newConfig, null);
        deltas = comparator.computeDelta().getConfigDelta();
        assertEquals("No modifications", 1, deltas.size());
        assertNotNull(containsDelta(deltas, "host", null, DeltaType.MODIFIED));
        // assertTrue(containsDelta(deltas, "application", "app1", ConfigDelta.DeltaType.MODIFIED));
        // assertTrue(containsDelta(deltas, "application", "app2", ConfigDelta.DeltaType.MODIFIED));

        // app1 should be removed
        oldConfig = newConfig;
        newConfig = parseServerConfiguration("<server>" +
                                             "  <host id=\"1\" ip=\"marge\"/>" +
                                             "  <application id=\"app2\">" +
                                             "    <host ref=\"1\"/>" +
                                             "  </application>" +
                                             "</server>");
        comparator = new ConfigComparator(oldConfig, newConfig, null);
        deltas = comparator.computeDelta().getConfigDelta();
        assertEquals("No modifications", 1, deltas.size());
        assertNotNull(containsDelta(deltas, "application", "app1", DeltaType.REMOVED));
    }

    @Test
    public void testNestedMetatype() throws Exception {
        changeLocationSettings("default");

        // setup metatype:
        // nested "port" element within "host" element is of "com.ibm.ws.host.port" type
        // any other "port" element maps to "com.ibm.ws.port" type
        String applicationPid = "com.ibm.ws.application";
        String applicationAlias = "application";
        MockObjectClassDefinition applicationOCD = new MockObjectClassDefinition(applicationAlias);
        applicationOCD.addAttributeDefinition(new MockAttributeDefinition("name", AttributeDefinition.STRING, 0, null));
        applicationOCD.setAlias(applicationAlias);

        String portPid = "com.ibm.ws.port";
        MockObjectClassDefinition portOCD = new MockObjectClassDefinition("port");
        portOCD.addAttributeDefinition(new MockAttributeDefinition("number", AttributeDefinition.STRING, 0, null));
        portOCD.setAlias("port");

        String hostPortPid = "com.ibm.ws.host.port";
        MockObjectClassDefinition hostPortOCD = new MockObjectClassDefinition("hostPort");
        hostPortOCD.addAttributeDefinition(new MockAttributeDefinition("number", AttributeDefinition.STRING, 0, null));

        String hostPid = "com.ibm.ws.host";
        MockObjectClassDefinition hostOCD = new MockObjectClassDefinition("host");
        hostOCD.addAttributeDefinition(new MockAttributeDefinition("ip", AttributeDefinition.STRING, 0, null));
        MockAttributeDefinition portRef = new MockAttributeDefinition("port", AttributeDefinition.STRING, 0, null);
        portRef.setReferencePid(hostPortPid);
        hostOCD.addAttributeDefinition(portRef);
        hostOCD.setAlias("host");

        MockBundle bundle = new MockBundle();
        MockMetaTypeInformation metatype = new MockMetaTypeInformation(bundle);
        metatype.add(applicationPid, true, applicationOCD);
        metatype.add(portPid, true, portOCD);
        metatype.add(hostPid, true, hostOCD);
        metatype.add(hostPortPid, true, hostPortOCD);

        MetaTypeRegistry registry = new MetaTypeRegistry();
        assertFalse("The registry should be updated", registry.addMetaType(metatype).isEmpty());

        ConfigComparator comparator;
        ConfigDelta delta;
        List<ConfigDelta> deltas;
        List<ConfigDelta> nestedDeltas;

        ServerConfiguration oldConfig = parseServerConfiguration("<server>" +
                                                                 "  <application id=\"app1\">" +
                                                                 "    <host ip=\"bart\">" +
                                                                 "      <port number=\"123\"/>" +
                                                                 "    </host>" +
                                                                 "  </application>" +
                                                                 "</server>");
        ServerConfiguration newConfig;

        // port, host and app1 modified
        newConfig = parseServerConfiguration("<server>" +
                                             "  <application id=\"app1\">" +
                                             "    <host ip=\"bart\">" +
                                             "      <port number=\"456\"/>" +
                                             "    </host>" +
                                             "  </application>" +
                                             "  <port number=\"8080\" />" +
                                             "</server>");
        comparator = new ConfigComparator(oldConfig, newConfig, registry);

        deltas = comparator.computeDelta().getConfigDelta();
        assertEquals("No modifications", 2, deltas.size());
        delta = containsDelta(deltas, applicationPid, "app1", DeltaType.MODIFIED);
        assertNotNull(delta);
        nestedDeltas = delta.getNestedDelta();
        assertEquals(1, nestedDeltas.size());
        delta = containsDelta(nestedDeltas, hostPid, null, DeltaType.MODIFIED);
        assertNotNull(delta);
        nestedDeltas = delta.getNestedDelta();
        assertEquals(1, nestedDeltas.size());
        assertNotNull(containsDelta(nestedDeltas, hostPortPid, null, DeltaType.MODIFIED));
        assertNotNull(containsDelta(deltas, portPid, null, DeltaType.ADDED));

        // host and app1 modified
        oldConfig = newConfig;
        newConfig = parseServerConfiguration("<server>" +
                                             "  <application id=\"app1\">" +
                                             "    <host ip=\"lisa\">" +
                                             "      <port number=\"456\"/>" +
                                             "    </host>" +
                                             "  </application>" +
                                             "</server>");
        comparator = new ConfigComparator(oldConfig, newConfig, registry);

        deltas = comparator.computeDelta().getConfigDelta();
        assertEquals("No modifications", 2, deltas.size());
        delta = containsDelta(deltas, applicationPid, "app1", DeltaType.MODIFIED);
        assertNotNull(delta);
        nestedDeltas = delta.getNestedDelta();
        assertEquals(1, nestedDeltas.size());
        delta = containsDelta(nestedDeltas, hostPid, null, DeltaType.MODIFIED);
        assertNotNull(delta);
        nestedDeltas = delta.getNestedDelta();
        assertEquals(0, nestedDeltas.size());
        assertNotNull(containsDelta(deltas, portPid, null, DeltaType.REMOVED));
    }

    @Test
    public void testNestedMetatypeCardinalityOne() throws Exception {
        testNestedMetatypeCardinality("port", "portRef");
    }

    @Test
    public void testNestedMetatypeCardinalityTwo() throws Exception {
        testNestedMetatypeCardinality("portRef", "port");
    }

    private void testNestedMetatypeCardinality(String portAttribute, String portRefAttribute) throws ConfigParserException, ConfigUpdateException, ConfigValidationException {
        changeLocationSettings("default");

        // setup metatype:
        // nested "port" element within "host" element is of "com.ibm.ws.host.port" type
        // any other "port" element maps to "com.ibm.ws.port" type
        String portPid = "com.ibm.ws.port";
        MockObjectClassDefinition portOCD = new MockObjectClassDefinition("port");
        portOCD.addAttributeDefinition(new MockAttributeDefinition("number", AttributeDefinition.STRING, 0, null));
        portOCD.setAlias("port");

        String hostPortPid = "com.ibm.ws.host.port";
        MockObjectClassDefinition hostPortOCD = new MockObjectClassDefinition("hostPort");
        hostPortOCD.addAttributeDefinition(new MockAttributeDefinition("number", AttributeDefinition.STRING, 0, null));

        String hostPid = "com.ibm.ws.host";
        MockObjectClassDefinition hostOCD = new MockObjectClassDefinition("host");
        hostOCD.addAttributeDefinition(new MockAttributeDefinition("ip", AttributeDefinition.STRING, 0, null));
        MockAttributeDefinition portRef = new MockAttributeDefinition(portAttribute, AttributeDefinition.STRING, 0, null);
        portRef.setReferencePid(hostPortPid);
        hostOCD.addAttributeDefinition(portRef);
        hostOCD.setAlias("host");

        MockBundle bundle = new MockBundle();
        MockMetaTypeInformation metatype = new MockMetaTypeInformation(bundle);
        metatype.add(portPid, true, portOCD);
        metatype.add(hostPid, true, hostOCD);
        metatype.add(hostPortPid, true, hostPortOCD);

        MetaTypeRegistry registry = new MetaTypeRegistry();
        assertFalse("The registry should be updated", registry.addMetaType(metatype).isEmpty());

        ConfigComparator comparator;
        ConfigDelta delta;
        List<ConfigDelta> deltas;
        List<ConfigDelta> nestedDeltas;

        ServerConfiguration oldConfig = parseServerConfiguration("<server>" +
                                                                 "    <host id=\"lisa\">" +
                                                                 "      <port number=\"123\"/>" +
                                                                 "    </host>" +
                                                                 "    <host id=\"lisa\">" +
                                                                 "      <port number=\"456\"/>" +
                                                                 "    </host>" +
                                                                 "</server>");
        ServerConfiguration newConfig;

        // port, host and app1 modified
        newConfig = parseServerConfiguration("<server>" +
                                             "    <host id=\"lisa\">" +
                                             "      <port number=\"456\"/>" +
                                             "    </host>" +
                                             "    <host id=\"lisa\">" +
                                             "      <port number=\"789\"/>" +
                                             "    </host>" +
                                             "</server>");
        comparator = new ConfigComparator(oldConfig, newConfig, registry);

        deltas = comparator.computeDelta().getConfigDelta();
        assertEquals("No modifications", 1, deltas.size());
        delta = containsDelta(deltas, hostPid, "lisa", DeltaType.MODIFIED);
        assertNotNull(delta);
        nestedDeltas = delta.getNestedDelta();
        assertEquals(1, nestedDeltas.size());
        delta = containsDelta(nestedDeltas, hostPortPid, null, DeltaType.MODIFIED);
        assertNotNull(delta);
        assertEquals(0, nestedDeltas.get(0).getNestedDelta().size());

        // change to multiple cardinality
        portRef.setCardinality(10);

        // ports have no ids - so two entries should be modified
        oldConfig = newConfig;
        newConfig = parseServerConfiguration("<server>" +
                                             "    <host id=\"lisa\">" +
                                             "      <port number=\"789\"/>" +
                                             "    </host>" +
                                             "    <host id=\"lisa\">" +
                                             "      <port number=\"012\"/>" +
                                             "    </host>" +
                                             "</server>");
        comparator = new ConfigComparator(oldConfig, newConfig, registry);

        deltas = comparator.computeDelta().getConfigDelta();
        assertEquals("No modifications", 1, deltas.size());
        delta = containsDelta(deltas, hostPid, "lisa", DeltaType.MODIFIED);
        assertNotNull(delta);
        nestedDeltas = delta.getNestedDelta();
        assertEquals(2, nestedDeltas.size());
        assertNotNull(containsDelta(nestedDeltas, hostPortPid, "default-0", DeltaType.MODIFIED));
        assertNotNull(containsDelta(nestedDeltas, hostPortPid, "default-1", DeltaType.MODIFIED));

        // some ports have ids, some don't
        oldConfig = newConfig;
        newConfig = parseServerConfiguration("<server>" +
                                             "    <host id=\"lisa\">" +
                                             "      <port number=\"145\"/>" +
                                             "    </host>" +
                                             "    <host id=\"lisa\">" +
                                             "      <port number=\"178\"/>" +
                                             "      <port id=\"a\" number=\"1000\"/>" +
                                             "    </host>" +
                                             "    <host id=\"lisa\">" +
                                             "      <port id=\"a\" number=\"2000\"/>" +
                                             "    </host>" +
                                             "</server>");
        comparator = new ConfigComparator(oldConfig, newConfig, registry);

        deltas = comparator.computeDelta().getConfigDelta();
        assertEquals("No modifications", 1, deltas.size());
        delta = containsDelta(deltas, hostPid, "lisa", DeltaType.MODIFIED);
        assertNotNull(delta);
        nestedDeltas = delta.getNestedDelta();
        assertEquals(3, nestedDeltas.size());
        assertNotNull(containsDelta(nestedDeltas, hostPortPid, "default-0", DeltaType.MODIFIED));
        assertNotNull(containsDelta(nestedDeltas, hostPortPid, "default-1", DeltaType.MODIFIED));
        assertNotNull(containsDelta(nestedDeltas, hostPortPid, "a", DeltaType.ADDED));

        // modify second port entry
        oldConfig = newConfig;
        newConfig = parseServerConfiguration("<server>" +
                                             "    <host id=\"lisa\">" +
                                             "      <port number=\"145\"/>" +
                                             "    </host>" +
                                             "    <host id=\"lisa\">" +
                                             "      <port number=\"000\"/>" +
                                             "      <port id=\"a\" number=\"1000\"/>" +
                                             "    </host>" +
                                             "    <host id=\"lisa\">" +
                                             "      <port id=\"a\" number=\"2000\"/>" +
                                             "    </host>" +
                                             "</server>");
        comparator = new ConfigComparator(oldConfig, newConfig, registry);

        deltas = comparator.computeDelta().getConfigDelta();
        assertEquals("No modifications", 1, deltas.size());
        delta = containsDelta(deltas, hostPid, "lisa", DeltaType.MODIFIED);
        assertNotNull(delta);
        nestedDeltas = delta.getNestedDelta();
        assertEquals(1, nestedDeltas.size());
        assertNotNull(containsDelta(nestedDeltas, hostPortPid, "default-1", DeltaType.MODIFIED));

        // modify first port entry
        oldConfig = newConfig;
        newConfig = parseServerConfiguration("<server>" +
                                             "    <host id=\"lisa\">" +
                                             "      <port number=\"999\"/>" +
                                             "    </host>" +
                                             "    <host id=\"lisa\">" +
                                             "      <port number=\"000\"/>" +
                                             "      <port id=\"a\" number=\"1000\"/>" +
                                             "    </host>" +
                                             "    <host id=\"lisa\">" +
                                             "      <port id=\"a\" number=\"2000\"/>" +
                                             "    </host>" +
                                             "</server>");
        comparator = new ConfigComparator(oldConfig, newConfig, registry);

        deltas = comparator.computeDelta().getConfigDelta();
        assertEquals("No modifications", 1, deltas.size());
        delta = containsDelta(deltas, hostPid, "lisa", DeltaType.MODIFIED);
        assertNotNull(delta);
        nestedDeltas = delta.getNestedDelta();
        assertEquals(1, nestedDeltas.size());
        assertNotNull(containsDelta(nestedDeltas, hostPortPid, "default-0", DeltaType.MODIFIED));

        // modify first port "a" entry - no change
        oldConfig = newConfig;
        newConfig = parseServerConfiguration("<server>" +
                                             "    <host id=\"lisa\">" +
                                             "      <port number=\"999\"/>" +
                                             "    </host>" +
                                             "    <host id=\"lisa\">" +
                                             "      <port number=\"000\"/>" +
                                             "      <port id=\"a\" number=\"5000\"/>" +
                                             "    </host>" +
                                             "    <host id=\"lisa\">" +
                                             "      <port id=\"a\" number=\"2000\"/>" +
                                             "    </host>" +
                                             "</server>");
        comparator = new ConfigComparator(oldConfig, newConfig, registry);

        deltas = comparator.computeDelta().getConfigDelta();
        assertEquals("No modifications", 0, deltas.size());

        // modify second port "a" entry
        oldConfig = newConfig;
        newConfig = parseServerConfiguration("<server>" +
                                             "    <host id=\"lisa\">" +
                                             "      <port number=\"999\"/>" +
                                             "    </host>" +
                                             "    <host id=\"lisa\">" +
                                             "      <port number=\"000\"/>" +
                                             "      <port id=\"a\" number=\"1000\"/>" +
                                             "    </host>" +
                                             "    <host id=\"lisa\">" +
                                             "      <port id=\"a\" number=\"3000\"/>" +
                                             "    </host>" +
                                             "</server>");
        comparator = new ConfigComparator(oldConfig, newConfig, registry);

        deltas = comparator.computeDelta().getConfigDelta();
        assertEquals("No modifications", 1, deltas.size());
        delta = containsDelta(deltas, hostPid, "lisa", DeltaType.MODIFIED);
        assertNotNull(delta);
        nestedDeltas = delta.getNestedDelta();
        assertEquals(1, nestedDeltas.size());
        assertNotNull(containsDelta(nestedDeltas, hostPortPid, "a", DeltaType.MODIFIED));
    }

    @Test
    public void testNestedOptionalId() throws Exception {
        changeLocationSettings("default");

        // setup metatype:
        // nested "port" element within "host" element is of "com.ibm.ws.host.port" type
        // any other "port" element maps to "com.ibm.ws.port" type
        String applicationPid = "com.ibm.ws.application";
        MockObjectClassDefinition applicationOCD = new MockObjectClassDefinition("application");
        applicationOCD.addAttributeDefinition(new MockAttributeDefinition("name", AttributeDefinition.STRING, 0, null));
        applicationOCD.setAlias("application");

        MockBundle bundle = new MockBundle();
        MockMetaTypeInformation metatype = new MockMetaTypeInformation(bundle);
        metatype.add(applicationPid, true, applicationOCD);

        MetaTypeRegistry registry = new MetaTypeRegistry();
        assertFalse("The registry should be updated", registry.addMetaType(metatype).isEmpty());

        ConfigComparator comparator;
        ConfigDelta delta;
        List<ConfigDelta> deltas;
        List<ConfigDelta> nestedDeltas;

        ServerConfiguration oldConfig = parseServerConfiguration("<server>" +
                                                                 "  <application name=\"app1\">" +
                                                                 "    <host ip=\"bart\"/>" +
                                                                 "  </application>" +
                                                                 "  <application id=\"app15\" name=\"app15\"/>" +
                                                                 "  <application name=\"app2\"/>" +
                                                                 "</server>");
        ServerConfiguration newConfig;

        // host and app1 modified
        newConfig = parseServerConfiguration("<server>" +
                                             "  <application name=\"app1\">" +
                                             "    <host ip=\"lisa\"/>" +
                                             "  </application>" +
                                             "  <application id=\"app15\" name=\"app15\"/>" +
                                             "  <application name=\"app2\"/>" +
                                             "</server>");
        comparator = new ConfigComparator(oldConfig, newConfig, registry);

        deltas = comparator.computeDelta().getConfigDelta();
        assertEquals("No modifications", 1, deltas.size());
        delta = containsDelta(deltas, applicationPid, "default-0", DeltaType.MODIFIED);
        assertNotNull(delta);
        nestedDeltas = delta.getNestedDelta();
        assertEquals(1, nestedDeltas.size());
        assertNotNull(containsDelta(nestedDeltas, "host", null, DeltaType.MODIFIED));

        // app1, app2 modified, fileset added, host removed
        oldConfig = newConfig;
        newConfig = parseServerConfiguration("<server>" +
                                             "  <application name=\"app1\">" +
                                             "  </application>" +
                                             "  <application id=\"app15\" name=\"app15\"/>" +
                                             "  <application name=\"app2\">" +
                                             "    <fileset dir=\"lib\" />" +
                                             "  </application>" +
                                             "</server>");
        comparator = new ConfigComparator(oldConfig, newConfig, registry);

        deltas = comparator.computeDelta().getConfigDelta();
        assertEquals("No modifications", 2, deltas.size());
        delta = containsDelta(deltas, applicationPid, "default-0", DeltaType.MODIFIED);
        assertNotNull(delta);
        nestedDeltas = delta.getNestedDelta();
        assertEquals(1, nestedDeltas.size());
        assertNotNull(containsDelta(nestedDeltas, "host", null, DeltaType.REMOVED));

        delta = containsDelta(deltas, applicationPid, "default-1", DeltaType.MODIFIED);
        assertNotNull(delta);
        nestedDeltas = delta.getNestedDelta();
        assertEquals(1, nestedDeltas.size());
        assertNotNull(containsDelta(nestedDeltas, "fileset", null, DeltaType.ADDED));

        // app2 and fileset removed
        oldConfig = newConfig;
        newConfig = parseServerConfiguration("<server>" +
                                             "  <application name=\"app1\">" +
                                             "  </application>" +
                                             "  <application id=\"app15\" name=\"app15\"/>" +
                                             "</server>");
        comparator = new ConfigComparator(oldConfig, newConfig, registry);

        deltas = comparator.computeDelta().getConfigDelta();
        assertEquals("No modifications", 1, deltas.size());
        delta = containsDelta(deltas, applicationPid, "default-1", DeltaType.REMOVED);
        assertNotNull(delta);
        nestedDeltas = delta.getNestedDelta();
        assertEquals(1, nestedDeltas.size());
        assertNotNull(containsDelta(nestedDeltas, "fileset", null, DeltaType.REMOVED));
    }

    @Test
    public void testVariableChange() throws Exception {
        changeLocationSettings("default");

        ConfigComparator comparator;
        ComparatorResult result;

        ServerConfiguration oldConfig = parseServerConfiguration("<server>" +
                                                                 "  <variable name=\"maxSize\" value=\"100\"/>" +
                                                                 "  <variable name=\"minSize\" value=\"200\"/>" +
                                                                 "  <variable name=\"idleTime\" value=\"300\"/>" +
                                                                 "</server>");
        ServerConfiguration newConfig;

        // no change
        newConfig = parseServerConfiguration("<server>" +
                                             "  <variable name=\"maxSize\" value=\"100\"/>" +
                                             "  <variable name=\"minSize\" value=\"200\"/>" +
                                             "  <variable name=\"idleTime\" value=\"300\"/>" +
                                             "</server>");
        comparator = new ConfigComparator(oldConfig, newConfig, null);
        result = comparator.computeDelta();
        assertFalse("No change", result.hasDelta());

        // removed one variable
        newConfig = parseServerConfiguration("<server>" +
                                             "  <variable name=\"maxSize\" value=\"100\"/>" +
                                             "  <variable name=\"minSize\" value=\"200\"/>" +
                                             "</server>");
        comparator = new ConfigComparator(oldConfig, newConfig, null);
        result = comparator.computeDelta();
        assertTrue("No config change", result.getConfigDelta().isEmpty());
        assertEquals("Variable change", 1, result.getVariableDelta().size());
        assertVariable(result.getVariableDelta(), "idleTime", DeltaType.REMOVED);

        // added two new variables
        newConfig = parseServerConfiguration("<server>" +
                                             "  <variable name=\"maxSize\" value=\"100\"/>" +
                                             "  <variable name=\"minSize\" value=\"200\"/>" +
                                             "  <variable name=\"idleTime\" value=\"300\"/>" +
                                             "  <variable name=\"startTime\" value=\"200\"/>" +
                                             "  <variable name=\"stopTime\" value=\"200\"/>" +
                                             "</server>");
        comparator = new ConfigComparator(oldConfig, newConfig, null);
        result = comparator.computeDelta();
        assertTrue("No config change", result.getConfigDelta().isEmpty());
        assertEquals("Variable change", 2, result.getVariableDelta().size());
        assertVariable(result.getVariableDelta(), "startTime", DeltaType.ADDED);
        assertVariable(result.getVariableDelta(), "stopTime", DeltaType.ADDED);

        // removed one, modified one
        newConfig = parseServerConfiguration("<server>" +
                                             "  <variable name=\"maxSize\" value=\"300\"/>" +
                                             "  <variable name=\"minSize\" value=\"200\"/>" +
                                             "</server>");
        comparator = new ConfigComparator(oldConfig, newConfig, null);
        result = comparator.computeDelta();
        assertTrue("No config change", result.getConfigDelta().isEmpty());
        assertEquals("Variable change", 2, result.getVariableDelta().size());
        assertVariable(result.getVariableDelta(), "maxSize", DeltaType.MODIFIED);
        assertVariable(result.getVariableDelta(), "idleTime", DeltaType.REMOVED);

        // removed one, modified one
        newConfig = parseServerConfiguration("<server>" +
                                             "  <variable name=\"maxSize\" value=\"300\"/>" +
                                             "  <variable name=\"minSize\" value=\"200\"/>" +
                                             "  <variable name=\"startTime\" value=\"200\"/>" +
                                             "</server>");
        comparator = new ConfigComparator(oldConfig, newConfig, null);
        result = comparator.computeDelta();
        assertTrue("No config change", result.getConfigDelta().isEmpty());
        assertEquals("Variable change", 3, result.getVariableDelta().size());
        assertVariable(result.getVariableDelta(), "maxSize", DeltaType.MODIFIED);
        assertVariable(result.getVariableDelta(), "idleTime", DeltaType.REMOVED);
        assertVariable(result.getVariableDelta(), "startTime", DeltaType.ADDED);
    }

    @Test
    public void testVariableReferenceChange() throws Exception {
        changeLocationSettings("default");

        ConfigComparator comparator;
        ComparatorResult result;

        ServerConfiguration oldConfig = parseServerConfiguration("<server>" +
                                                                 "  <variable name=\"a\" value=\"A\"/>" +
                                                                 "  <variable name=\"az\" value=\"${a}-${z}\"/>" +
                                                                 "  <variable name=\"za\" value=\"${z}-${a}\"/>" +
                                                                 "  <variable name=\"combined\" value=\"${za}\"/>" +
                                                                 "  <variable name=\"z\" value=\"Z${foo}\"/>" +
                                                                 "</server>");
        ServerConfiguration newConfig;

        // no change
        newConfig = parseServerConfiguration("<server>" +
                                             "  <variable name=\"a\" value=\"A\"/>" +
                                             "  <variable name=\"az\" value=\"${a}-${z}\"/>" +
                                             "  <variable name=\"za\" value=\"${z}-${a}\"/>" +
                                             "  <variable name=\"combined\" value=\"${za}\"/>" +
                                             "  <variable name=\"z\" value=\"Z${foo}\"/>" +
                                             "</server>");
        comparator = new ConfigComparator(oldConfig, newConfig, null);
        result = comparator.computeDelta();
        assertFalse("No change", result.hasDelta());

        // change "a"
        newConfig = parseServerConfiguration("<server>" +
                                             "  <variable name=\"a\" value=\"myA\"/>" +
                                             "  <variable name=\"az\" value=\"${a}-${z}\"/>" +
                                             "  <variable name=\"za\" value=\"${z}-${a}\"/>" +
                                             "  <variable name=\"combined\" value=\"${za}\"/>" +
                                             "  <variable name=\"z\" value=\"Z${foo}\"/>" +
                                             "</server>");
        comparator = new ConfigComparator(oldConfig, newConfig, null);
        result = comparator.computeDelta();
        assertTrue("No config change", result.getConfigDelta().isEmpty());
        assertEquals("Variable change", 4, result.getVariableDelta().size());
        assertVariable(result.getVariableDelta(), "a", DeltaType.MODIFIED);
        assertVariable(result.getVariableDelta(), "az", DeltaType.MODIFIED);
        assertVariable(result.getVariableDelta(), "za", DeltaType.MODIFIED);
        assertVariable(result.getVariableDelta(), "combined", DeltaType.MODIFIED);

        // change "za"
        newConfig = parseServerConfiguration("<server>" +
                                             "  <variable name=\"a\" value=\"A\"/>" +
                                             "  <variable name=\"az\" value=\"${a}-${z}\"/>" +
                                             "  <variable name=\"za\" value=\"5\"/>" +
                                             "  <variable name=\"combined\" value=\"${za}\"/>" +
                                             "  <variable name=\"z\" value=\"Z${foo}\"/>" +
                                             "</server>");
        comparator = new ConfigComparator(oldConfig, newConfig, null);
        result = comparator.computeDelta();
        assertTrue("No config change", result.getConfigDelta().isEmpty());
        assertEquals("Variable change", 2, result.getVariableDelta().size());
        assertVariable(result.getVariableDelta(), "za", DeltaType.MODIFIED);
        assertVariable(result.getVariableDelta(), "combined", DeltaType.MODIFIED);

        // add "foo"
        newConfig = parseServerConfiguration("<server>" +
                                             "  <variable name=\"a\" value=\"A\"/>" +
                                             "  <variable name=\"az\" value=\"${a}-${z}\"/>" +
                                             "  <variable name=\"za\" value=\"${z}-${a}\"/>" +
                                             "  <variable name=\"combined\" value=\"${za}\"/>" +
                                             "  <variable name=\"z\" value=\"Z${foo}\"/>" +
                                             "  <variable name=\"foo\" value=\"bar\"/>" +
                                             "</server>");
        comparator = new ConfigComparator(oldConfig, newConfig, null);
        result = comparator.computeDelta();
        assertTrue("No config change", result.getConfigDelta().isEmpty());
        assertEquals("Variable change", 5, result.getVariableDelta().size());
        assertVariable(result.getVariableDelta(), "foo", DeltaType.ADDED);
        assertVariable(result.getVariableDelta(), "z", DeltaType.MODIFIED);
        assertVariable(result.getVariableDelta(), "az", DeltaType.MODIFIED);
        assertVariable(result.getVariableDelta(), "za", DeltaType.MODIFIED);
        assertVariable(result.getVariableDelta(), "combined", DeltaType.MODIFIED);

        // remove "a"
        newConfig = parseServerConfiguration("<server>" +
                                             "  <variable name=\"az\" value=\"${a}-${z}\"/>" +
                                             "  <variable name=\"za\" value=\"${z}-${a}\"/>" +
                                             "  <variable name=\"combined\" value=\"${za}\"/>" +
                                             "  <variable name=\"z\" value=\"Z${foo}\"/>" +
                                             "</server>");
        comparator = new ConfigComparator(oldConfig, newConfig, null);
        result = comparator.computeDelta();
        assertTrue("No config change", result.getConfigDelta().isEmpty());
        assertEquals("Variable change", 4, result.getVariableDelta().size());
        assertVariable(result.getVariableDelta(), "a", DeltaType.REMOVED);
        assertVariable(result.getVariableDelta(), "az", DeltaType.MODIFIED);
        assertVariable(result.getVariableDelta(), "za", DeltaType.MODIFIED);
        assertVariable(result.getVariableDelta(), "combined", DeltaType.MODIFIED);
    }

    @Test
    public void testVariableConfigChange() throws Exception {
        changeLocationSettings("default");

        ConfigComparator comparator;
        ComparatorResult result;
        List<ConfigDelta> deltas;
        List<ConfigDelta> nestedDeltas;
        ConfigDelta delta;

        ServerConfiguration oldConfig = parseServerConfiguration("<server>" +
                                                                 "  <variable name=\"maxSize\" value=\"100\"/>" +
                                                                 "  <application id=\"app2\">" +
                                                                 "    <fileset id=\"one\" dir=\"${maxSize}-${minSize}\" />" +
                                                                 "  </application>" +
                                                                 "  <fileset id=\"two\">" +
                                                                 "    <file>${minSize}</file>" +
                                                                 "  </fileset>" +
                                                                 "</server>");
        ServerConfiguration newConfig;

        // change maxSize variable. application and fileset-one should be modified
        newConfig = parseServerConfiguration("<server>" +
                                             "  <variable name=\"maxSize\" value=\"200\"/>" +
                                             "  <application id=\"app2\">" +
                                             "    <fileset id=\"one\" dir=\"${maxSize}-${minSize}\" />" +
                                             "  </application>" +
                                             "  <fileset id=\"two\">" +
                                             "    <file>${minSize}</file>" +
                                             "  </fileset>" +
                                             "</server>");
        comparator = new ConfigComparator(oldConfig, newConfig, null);
        result = comparator.computeDelta();
        assertEquals("Variable change", 1, result.getVariableDelta().size());
        assertVariable(result.getVariableDelta(), "maxSize", DeltaType.MODIFIED);

        assertEquals("Config change", 1, result.getConfigDelta().size());
        deltas = result.getConfigDelta();
        delta = containsDelta(deltas, "application", "app2", DeltaType.MODIFIED);
        assertNotNull(delta);
        nestedDeltas = delta.getNestedDelta();
        assertEquals(1, nestedDeltas.size());
        assertNotNull(containsDelta(nestedDeltas, "fileset", "one", DeltaType.MODIFIED));

        // add minSize variable. application, fileset-one, fileset-two should be modified
        newConfig = parseServerConfiguration("<server>" +
                                             "  <variable name=\"maxSize\" value=\"100\"/>" +
                                             "  <variable name=\"minSize\" value=\"400\"/>" +
                                             "  <application id=\"app2\">" +
                                             "    <fileset id=\"one\" dir=\"${maxSize}-${minSize}\" />" +
                                             "  </application>" +
                                             "  <fileset id=\"two\">" +
                                             "    <file>${minSize}</file>" +
                                             "  </fileset>" +
                                             "</server>");
        comparator = new ConfigComparator(oldConfig, newConfig, null);
        result = comparator.computeDelta();
        assertEquals("Variable change", 1, result.getVariableDelta().size());
        assertVariable(result.getVariableDelta(), "minSize", DeltaType.ADDED);

        assertEquals("Config change", 2, result.getConfigDelta().size());
        deltas = result.getConfigDelta();
        delta = containsDelta(deltas, "application", "app2", DeltaType.MODIFIED);
        assertNotNull(delta);
        nestedDeltas = delta.getNestedDelta();
        assertEquals(1, nestedDeltas.size());
        assertNotNull(containsDelta(nestedDeltas, "fileset", "one", DeltaType.MODIFIED));
        delta = containsDelta(deltas, "fileset", "two", DeltaType.MODIFIED);
        assertNotNull(delta);

        // remove maxSize variable. fileset and application should be modified
        newConfig = parseServerConfiguration("<server>" +
                                             "  <application id=\"app2\">" +
                                             "    <fileset id=\"one\" dir=\"${maxSize}-${minSize}\" />" +
                                             "  </application>" +
                                             "  <fileset id=\"two\">" +
                                             "    <file>${minSize}</file>" +
                                             "  </fileset>" +
                                             "</server>");
        comparator = new ConfigComparator(oldConfig, newConfig, null);
        result = comparator.computeDelta();
        assertEquals("Variable change", 1, result.getVariableDelta().size());
        assertVariable(result.getVariableDelta(), "maxSize", DeltaType.REMOVED);

        assertEquals("Config change", 1, result.getConfigDelta().size());
        deltas = result.getConfigDelta();
        delta = containsDelta(deltas, "application", "app2", DeltaType.MODIFIED);
        assertNotNull(delta);
        nestedDeltas = delta.getNestedDelta();
        assertEquals(1, nestedDeltas.size());
        assertNotNull(containsDelta(nestedDeltas, "fileset", "one", DeltaType.MODIFIED));
    }

    @Test
    public void testVariableConfigMetatypeChange() throws Exception {
        changeLocationSettings("default");

        String applicationPid = "com.ibm.ws.application";
        MockObjectClassDefinition applicationOCD = new MockObjectClassDefinition("application");
        MockAttributeDefinition nameAttribute = new MockAttributeDefinition("name", AttributeDefinition.STRING, 0, null);
        nameAttribute.setVariable("maxSize");
        applicationOCD.addAttributeDefinition(nameAttribute);
        applicationOCD.setAlias("application");

        String filesetPid = "com.ibm.ws.fileset";
        MockObjectClassDefinition filesetOCD = new MockObjectClassDefinition("fileset");
        MockAttributeDefinition filesetAttribute = new MockAttributeDefinition("file", AttributeDefinition.STRING, 0, new String[] { "${minSize}-${maxSize}" });
        filesetOCD.addAttributeDefinition(filesetAttribute);
        filesetOCD.setAlias("fileset");

        MockBundle bundle = new MockBundle();
        MockMetaTypeInformation metatype = new MockMetaTypeInformation(bundle);
        metatype.add(applicationPid, true, applicationOCD);
        metatype.add(filesetPid, true, filesetOCD);

        MetaTypeRegistry registry = new MetaTypeRegistry();
        assertFalse("The registry should be updated", registry.addMetaType(metatype).isEmpty());

        ConfigComparator comparator;
        ComparatorResult result;
        List<ConfigDelta> deltas;
        List<ConfigDelta> nestedDeltas;
        ConfigDelta delta;

        ServerConfiguration oldConfig = parseServerConfiguration("<server>" +
                                                                 "  <variable name=\"maxSize\" value=\"100\"/>" +
                                                                 "  <application id=\"app2\">" +
                                                                 "    <fileset id=\"one\" file=\"${minSize}\" />" +
                                                                 "  </application>" +
                                                                 "  <fileset id=\"two\">" +
                                                                 "  </fileset>" +
                                                                 "</server>");
        ServerConfiguration newConfig;

        // change maxSize variable. application, fileset-two should be modified
        newConfig = parseServerConfiguration("<server>" +
                                             "  <variable name=\"maxSize\" value=\"200\"/>" +
                                             "  <application id=\"app2\">" +
                                             "    <fileset id=\"one\" file=\"${minSize}\" />" +
                                             "  </application>" +
                                             "  <fileset id=\"two\">" +
                                             "  </fileset>" +
                                             "</server>");
        comparator = new ConfigComparator(oldConfig, newConfig, registry);

        result = comparator.computeDelta();
        assertEquals("Variable change", 1, result.getVariableDelta().size());
        assertVariable(result.getVariableDelta(), "maxSize", DeltaType.MODIFIED);

        assertEquals("Config change", 2, result.getConfigDelta().size());
        deltas = result.getConfigDelta();
        delta = containsDelta(deltas, applicationPid, "app2", DeltaType.MODIFIED);
        assertNotNull(delta);
        nestedDeltas = delta.getNestedDelta();
        assertEquals(0, nestedDeltas.size());
        delta = containsDelta(deltas, filesetPid, "two", DeltaType.MODIFIED);
        assertNotNull(delta);

        // add minSize variable. application, fileset-one, fileset-two should be modified
        newConfig = parseServerConfiguration("<server>" +
                                             "  <variable name=\"maxSize\" value=\"100\"/>" +
                                             "  <variable name=\"minSize\" value=\"400\"/>" +
                                             "  <application id=\"app2\">" +
                                             "    <fileset id=\"one\" file=\"${minSize}\" />" +
                                             "  </application>" +
                                             "  <fileset id=\"two\">" +
                                             "  </fileset>" +
                                             "</server>");
        comparator = new ConfigComparator(oldConfig, newConfig, registry);

        result = comparator.computeDelta();
        assertEquals("Variable change", 1, result.getVariableDelta().size());
        assertVariable(result.getVariableDelta(), "minSize", DeltaType.ADDED);

        assertEquals("Config change", 2, result.getConfigDelta().size());
        deltas = result.getConfigDelta();

        delta = containsDelta(deltas, applicationPid, "app2", DeltaType.MODIFIED);
        assertNotNull(delta);
        nestedDeltas = delta.getNestedDelta();
        assertEquals(1, nestedDeltas.size());
        assertNotNull(containsDelta(nestedDeltas, filesetPid, "one", DeltaType.MODIFIED));

        assertNotNull(containsDelta(deltas, filesetPid, "two", DeltaType.MODIFIED));

        // remove maxSize variable. application and fileset-two should be modified
        newConfig = parseServerConfiguration("<server>" +
                                             "  <application id=\"app2\">" +
                                             "    <fileset id=\"one\" file=\"${minSize}\" />" +
                                             "  </application>" +
                                             "  <fileset id=\"two\">" +
                                             "  </fileset>" +
                                             "</server>");
        comparator = new ConfigComparator(oldConfig, newConfig, registry);

        result = comparator.computeDelta();
        assertEquals("Variable change", 1, result.getVariableDelta().size());
        assertVariable(result.getVariableDelta(), "maxSize", DeltaType.REMOVED);

        assertEquals("Config change", 2, result.getConfigDelta().size());
        deltas = result.getConfigDelta();

        delta = containsDelta(deltas, applicationPid, "app2", DeltaType.MODIFIED);
        assertNotNull(delta);
        nestedDeltas = delta.getNestedDelta();
        assertEquals(0, nestedDeltas.size());

        assertNotNull(containsDelta(deltas, filesetPid, "two", DeltaType.MODIFIED));
    }

    private ConfigDelta containsDelta(List<ConfigDelta> deltas, String pid, String id, DeltaType deltaType) {
        for (ConfigDelta delta : deltas) {
            ConfigElement configElement = delta.getConfigElement();
            if (pid.equals(configElement.getNodeName()) && deltaType == delta.getDelta()) {
                if (id == null || id.equals(configElement.getId())) {
                    return delta;
                }
            }
        }
        return null;
    }

    private void assertModified(List<ConfigDelta> deltas, String pid, String id) {
        assertEquals("Modified deltas", 1, deltas.size());
        assertEquals("Modified delta type", DeltaType.MODIFIED, deltas.get(0).getDelta());
        assertEquals("Modified config name", pid, deltas.get(0).getConfigElement().getNodeName());
        assertEquals("Modified config id", id, deltas.get(0).getConfigElement().getId());
    }

    private void assertAdded(List<ConfigDelta> deltas, String pid, String id) {
        assertEquals("Added deltas", 1, deltas.size());
        assertEquals("Added delta type", DeltaType.ADDED, deltas.get(0).getDelta());
        assertEquals("Added config name", pid, deltas.get(0).getConfigElement().getNodeName());
        assertEquals("Added config id", id, deltas.get(0).getConfigElement().getId());
    }

    private void assertRemoved(List<ConfigDelta> deltas, String pid, String id) {
        assertEquals("Removed deltas", 1, deltas.size());
        assertEquals("Removed delta type", DeltaType.REMOVED, deltas.get(0).getDelta());
        assertEquals("Removed config name", pid, deltas.get(0).getConfigElement().getNodeName());
        assertEquals("Removed config id", id, deltas.get(0).getConfigElement().getId());
    }

    private void assertVariable(Map<String, DeltaType> variableDelta, String variableName, DeltaType deltaType) {
        assertTrue("Variable changed", variableDelta.containsKey(variableName));
        assertEquals("Variable delta type", deltaType, variableDelta.get(variableName));
    }
}
