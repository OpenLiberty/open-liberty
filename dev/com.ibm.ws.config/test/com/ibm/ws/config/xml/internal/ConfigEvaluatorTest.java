/*******************************************************************************
 * Copyright (c) 2010, 2014 IBM Corporation and others.
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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.StringReader;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.service.metatype.AttributeDefinition;

import com.ibm.websphere.config.ConfigEvaluatorException;
import com.ibm.websphere.config.ConfigParserException;
import com.ibm.websphere.config.ConfigValidationException;
import com.ibm.websphere.metatype.MetaTypeFactory;
import com.ibm.ws.config.admin.ConfigID;
import com.ibm.ws.config.xml.internal.ConfigComparator.ComparatorResult;
import com.ibm.ws.config.xml.internal.ConfigComparator.DeltaType;
import com.ibm.ws.config.xml.internal.ConfigEvaluator.EvaluationResult;
import com.ibm.ws.config.xml.internal.ConfigEvaluator.UnresolvedPidType;
import com.ibm.ws.config.xml.internal.ConfigEvaluator.UnresolvedReference;
import com.ibm.ws.config.xml.internal.ConfigEvaluator.UnresolvedService;
import com.ibm.ws.config.xml.internal.MetaTypeRegistry.RegistryEntry;
import com.ibm.ws.config.xml.internal.metatype.ExtendedAttributeDefinition;
import com.ibm.ws.config.xml.internal.nester.Nester;
import com.ibm.ws.kernel.service.location.internal.SymbolRegistry;
import com.ibm.ws.kernel.service.location.internal.VariableRegistryHelper;
import com.ibm.wsspi.kernel.service.location.VariableRegistry;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.location.WsLocationConstants;

import test.common.SharedLocationManager;
import test.common.SharedOutputManager;
import test.utils.SharedConstants;

public class ConfigEvaluatorTest {
    final static String CONFIG_ROOT = "${server.config.dir}/server.xml";

    static WsLocationAdmin wsLocation;
    static XMLConfigParser configParser;
    static SharedOutputManager outputMgr;

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
        wsLocation = (WsLocationAdmin) SharedLocationManager.getLocationInstance();

        ConfigVariableRegistry variableRegistry = new ConfigVariableRegistry(new VariableRegistryHelper(), new String[0], null);

        configParser = new XMLConfigParser(wsLocation, variableRegistry);
    }

    private class TestConfigEvaluator extends ConfigEvaluator {

        /**
         * @param retriever
         * @param registry
         * @param variableRegistry
         */
        public TestConfigEvaluator(ConfigRetriever retriever, MetaTypeRegistry registry, ConfigVariableRegistry variableRegistry, ServerXMLConfiguration serverXMLConfiguration) {
            super(retriever, registry, variableRegistry, serverXMLConfiguration);
        }

        private Map<ConfigID, String> map;

        private String getPid(ConfigID configId, String method) {
            String pid;
            if (map == null) {
                pid = String.valueOf(configId.hashCode());
            } else {
                pid = map.get(configId);
            }
            System.out.println(method + ": " + configId + " = " + pid);
            return pid;
        }

        @Override
        public String getPid(ConfigID configId) {
            return getPid(configId, "get");
        }

        @Override
        public String lookupPid(ConfigID configId) {
            return getPid(configId, "lookup");
        }

        public void setLookupMap(Map<ConfigID, String> map) {
            this.map = map;
        }
    }

    private TestConfigEvaluator createConfigEvaluator(MetaTypeRegistry registry, ServerConfiguration serverConfiguration) {
        return createConfigEvaluator(registry, null, serverConfiguration);
    }

    private TestConfigEvaluator createConfigEvaluator(MetaTypeRegistry registry, ConfigVariableRegistry variableRegistry, ServerConfiguration serverConfiguration) {
        ServerXMLConfiguration serverXMLConfiguration = new ServerXMLConfiguration(null, wsLocation, null);
        serverXMLConfiguration.setNewConfiguration(serverConfiguration);
        TestConfigEvaluator evaluator = new TestConfigEvaluator(null, registry, variableRegistry, serverXMLConfiguration);
        return evaluator;
    }

    private MockObjectClassDefinition createServiceOCD() {

        MockObjectClassDefinition serviceOCD = new MockObjectClassDefinition("com.ibm.ws.serviceOCD");
        MockAttributeDefinition serviceRef = new MockAttributeDefinition("service", AttributeDefinition.STRING, 0, null);
        serviceRef.setService("service1");
        serviceOCD.addAttributeDefinition(serviceRef);
        serviceOCD.setAlias("serviceOCD");

        return serviceOCD;
    }

    private MockObjectClassDefinition createObjectClassTarget(String name) {
        MockObjectClassDefinition target = new MockObjectClassDefinition("com.ibm.ws." + name);
        target.setObjectClass("service1");
        target.setAlias(name);
        return target;
    }

    private MetaTypeRegistry createRegistry(String portPid, String hostPid, String hostPortPid, int portAttributeCardinality) {
        return createRegistry(portPid, hostPid, hostPortPid, "port", portAttributeCardinality, hostPortPid, true);
    }

    private MetaTypeRegistry createRegistry(String portPid,
                                            String hostPid,
                                            String hostPortPid,
                                            String portAttributeName,
                                            int portAttributeCardinality,
                                            String portAttributePid,
                                            boolean isPortFactory) {
        // setup metatype:
        // nested "port" element within "host" element is of "com.ibm.ws.host.port" type
        // any other "port" element maps to "com.ibm.ws.port" type

        MockObjectClassDefinition portOCD = new MockObjectClassDefinition("port");
        portOCD.addAttributeDefinition(new MockAttributeDefinition("number", AttributeDefinition.STRING, 0, null));
        portOCD.setAlias("port");

        MockObjectClassDefinition hostPortOCD = new MockObjectClassDefinition("hostPort");
        hostPortOCD.addAttributeDefinition(new MockAttributeDefinition("number", AttributeDefinition.STRING, 0, null));

        MockObjectClassDefinition hostOCD = new MockObjectClassDefinition("host");
        hostOCD.addAttributeDefinition(new MockAttributeDefinition("ip", AttributeDefinition.STRING, 0, null), false);
        MockAttributeDefinition portRef = new MockAttributeDefinition(portAttributeName, AttributeDefinition.STRING, portAttributeCardinality, null);
        portRef.setReferencePid(portAttributePid);
        hostOCD.addAttributeDefinition(portRef);
        MockAttributeDefinition filterRef = new MockAttributeDefinition("filter", AttributeDefinition.STRING, 0, null);
        filterRef.setService("service1");
        hostOCD.addAttributeDefinition(filterRef);
        hostOCD.setAlias("host");

        MockBundle bundle = new MockBundle();
        MockMetaTypeInformation metatype = new MockMetaTypeInformation(bundle);
        metatype.add(portPid, isPortFactory, portOCD);
        metatype.add(hostPid, true, hostOCD);
        metatype.add(hostPortPid, true, hostPortOCD);

        MetaTypeRegistry registry = new MetaTypeRegistry();
        assertFalse("The registry should be updated", registry.addMetaType(metatype).isEmpty());

        return registry;
    }

    private class RegistryFactory {
        private final Set<MockObjectClassDefinition> factories = new HashSet<MockObjectClassDefinition>();
        private final Set<MockObjectClassDefinition> singletons = new HashSet<MockObjectClassDefinition>();

        void addFactoryOCD(MockObjectClassDefinition ocd) {
            this.factories.add(ocd);
        }

        @SuppressWarnings("unused")
        void addSingletonOCD(MockObjectClassDefinition ocd) {
            this.singletons.add(ocd);
        }

        MetaTypeRegistry create() {
            MockBundle bundle = new MockBundle();
            MockMetaTypeInformation metatype = new MockMetaTypeInformation(bundle);
            for (MockObjectClassDefinition ocd : factories) {
                metatype.add(ocd.getID(), true, ocd);
            }
            for (MockObjectClassDefinition ocd : singletons) {
                metatype.add(ocd.getID(), false, ocd);
            }
            MetaTypeRegistry registry = new MetaTypeRegistry();
            assertFalse("The registry should be updated", registry.addMetaType(metatype).isEmpty());

            return registry;
        }
    }

    private MockAttributeDefinition addIntegerTypeToRegistry(MetaTypeRegistry registry,
                                                             String loggingPid) {
        // Add a new integer logging element
        MockObjectClassDefinition loggingOCD = new MockObjectClassDefinition("logging");
        MockAttributeDefinition ad = new MockAttributeDefinition("integer", AttributeDefinition.INTEGER, 0, null);
        ad.setValidate(new String[] { "8", "8" });
        loggingOCD.addAttributeDefinition(ad);
        loggingOCD.setAlias("logging");

        MockBundle bundle = new MockBundle();
        MockMetaTypeInformation metatype = new MockMetaTypeInformation(bundle);
        metatype.add(loggingPid, true, loggingOCD);

        assertFalse("The registry should be updated", registry.addMetaType(metatype).isEmpty());
        return ad;

    }

    private void addBooleanTypeToRegistry(MetaTypeRegistry registry,
                                          String loggingPid) {
        // Add a new boolean logging element
        MockObjectClassDefinition loggingOCD = new MockObjectClassDefinition("logging");
        loggingOCD.addAttributeDefinition(new MockAttributeDefinition("boolean", AttributeDefinition.BOOLEAN, 0, null));
        loggingOCD.setAlias("logging");

        MockBundle bundle = new MockBundle();
        MockMetaTypeInformation metatype = new MockMetaTypeInformation(bundle);
        metatype.add(loggingPid, true, loggingOCD);

        assertFalse("The registry should be updated", registry.addMetaType(metatype).isEmpty());

    }

    private void addObscuredBooleanTypeToRegistry(MetaTypeRegistry registry,
                                                  String loggingPid) {
        // Add a new boolean logging element
        MockObjectClassDefinition loggingOCD = new MockObjectClassDefinition("logging");
        MockAttributeDefinition ad = new MockAttributeDefinition("boolean", AttributeDefinition.BOOLEAN, 0, null);
        ad.setObscured("true");
        loggingOCD.addAttributeDefinition(ad);
        loggingOCD.setAlias("logging");

        MockBundle bundle = new MockBundle();
        MockMetaTypeInformation metatype = new MockMetaTypeInformation(bundle);
        metatype.add(loggingPid, true, loggingOCD);

        assertFalse("The registry should be updated", registry.addMetaType(metatype).isEmpty());
    }

    @Test
    public void testConfigID() throws Exception {
        ConfigID parent = new ConfigID("com.ibm.parent", "one");
        ConfigID child = new ConfigID(parent, "com.ibm.child", "aChild", "child");

        assertEquals("ConfigIDs should round trip", parent, ConfigID.fromProperty(parent.toString()));
        assertEquals("ConfigIDs should round trip", child, ConfigID.fromProperty(child.toString()));
    }

    @Test
    public void testOptionsConversion() throws Exception {
        changeLocationSettings("default");

        MetaTypeRegistry registry = new MetaTypeRegistry();
        MockMetaTypeInformation metatype = new MockMetaTypeInformation(new MockBundle());

        MockObjectClassDefinition ocd = new MockObjectClassDefinition("com.ibm.enum");
        ocd.setAlias("enum");
        MockAttributeDefinition ad = new MockAttributeDefinition("enum", AttributeDefinition.INTEGER);
        ad.setOptions(new String[] { "1", "2", "3" }, new String[] { "one", "TWO", "THREE" });
        ocd.addAttributeDefinition(ad);

        metatype.add(ocd.getID(), true, ocd);
        registry.addMetaType(metatype);
        RegistryEntry registryEntry = registry.getRegistryEntry(ocd.getID());

        ConfigEvaluator evaluator = createConfigEvaluator(registry, null);
        String xml = "<server>" +
                     "  <enum id='idone' enum='one'/>" +
                     "  <enum id='idONE' enum='ONE'/>" +
                     "  <enum id='idTWO' enum='TWO'/>" +
                     "</server>";

        ServerConfiguration serverConfig = configParser.parseServerConfiguration(new StringReader(xml));

        ConfigElement entry = serverConfig.getFactoryInstance(registryEntry.getPid(), ocd.getAlias(), "idone");
        assertEquals("Expected integer 1", 1, evaluator.evaluate(entry, registryEntry).getProperties().get("enum"));

        entry = serverConfig.getFactoryInstance(registryEntry.getPid(), ocd.getAlias(), "idONE");
        assertEquals("Expected integer 1", 1, evaluator.evaluate(entry, registryEntry).getProperties().get("enum"));

        entry = serverConfig.getFactoryInstance(registryEntry.getPid(), ocd.getAlias(), "idTWO");
        assertEquals("Expected integer 2", 2, evaluator.evaluate(entry, registryEntry).getProperties().get("enum"));
    }

    @Test
    public void testOptionsCaseInsensitivity() throws Exception {
        changeLocationSettings("default");

        MetaTypeRegistry registry = new MetaTypeRegistry();
        MockMetaTypeInformation metatype = new MockMetaTypeInformation(new MockBundle());

        MockObjectClassDefinition ocd = new MockObjectClassDefinition("com.ibm.enum");
        ocd.setAlias("enum");
        MockAttributeDefinition ad = new MockAttributeDefinition("enum", AttributeDefinition.STRING);
        ad.setOptions(new String[] { "aaa", "bbb" }, new String[] { "labelaaa", "labelbbb" });
        ocd.addAttributeDefinition(ad);

        metatype.add(ocd.getID(), true, ocd);
        registry.addMetaType(metatype);
        RegistryEntry registryEntry = registry.getRegistryEntry(ocd.getID());

        ConfigEvaluator evaluator = createConfigEvaluator(registry, null);
        String xml = "<server>" +
                     "  <enum id='idaaa' enum='aaa'/>" +
                     "  <enum id='idAAA' enum='AAA'/>" +
                     "</server>";

        ServerConfiguration serverConfig = configParser.parseServerConfiguration(new StringReader(xml));

        ConfigElement entry = serverConfig.getFactoryInstance(registryEntry.getPid(), ocd.getAlias(), "idaaa");
        assertEquals("Expected string aaa", "aaa", evaluator.evaluate(entry, registryEntry).getProperties().get("enum"));

        entry = serverConfig.getFactoryInstance(registryEntry.getPid(), ocd.getAlias(), "idAAA");
        assertEquals("Expected string aaa", "aaa", evaluator.evaluate(entry, registryEntry).getProperties().get("enum"));
    }

    @Test
    public void testOptionsInvalid() throws Exception {
        changeLocationSettings("default");

        MetaTypeRegistry registry = new MetaTypeRegistry();
        MockMetaTypeInformation metatype = new MockMetaTypeInformation(new MockBundle());

        MockObjectClassDefinition ocd = new MockObjectClassDefinition("com.ibm.enum");
        ocd.setAlias("enum");
        MockAttributeDefinition ad = new MockAttributeDefinition("enum", AttributeDefinition.INTEGER);
        ad.setOptions(new String[] { "1", "2", "3" }, new String[] { "one", "TWO", "THREE" });
        ad.setDefaultValue(new String[] { "TWO" });
        ad.setValidate(new String[] { "TWO" });
        ocd.addAttributeDefinition(ad);

        metatype.add(ocd.getID(), true, ocd);
        registry.addMetaType(metatype);
        RegistryEntry registryEntry = registry.getRegistryEntry(ocd.getID());

        ConfigEvaluator evaluator = createConfigEvaluator(registry, null);
        String xml = "<server>" +
                     "  <enum id='idone' enum='FOUR'/>" +
                     "  <enum id='idONE' enum='ONE'/>" +
                     "  <enum id='idTWO' enum='TWO'/>" +
                     "</server>";

        ServerConfiguration serverConfig = configParser.parseServerConfiguration(new StringReader(xml));

        ConfigElement entry = serverConfig.getFactoryInstance(registryEntry.getPid(), ocd.getAlias(), "idone");
        assertEquals("Expected integer 2 from default value",
                     2, evaluator.evaluate(entry, registryEntry).getProperties().get("enum"));

        entry = serverConfig.getFactoryInstance(registryEntry.getPid(), ocd.getAlias(), "idONE");
        assertEquals("Expected integer 1", 1, evaluator.evaluate(entry, registryEntry).getProperties().get("enum"));

        entry = serverConfig.getFactoryInstance(registryEntry.getPid(), ocd.getAlias(), "idTWO");
        assertEquals("Expected integer 2", 2, evaluator.evaluate(entry, registryEntry).getProperties().get("enum"));

        outputMgr.dumpStreams();
        assertTrue("We should get a message that an option is invalid", outputMgr.checkForMessages("CWWKG0032W.*TWO"));

    }

    @Test
    public void testNestedMergeSingle() throws Exception {
        changeLocationSettings("default");

        String portPid = "com.ibm.ws.port";
        String hostPid = "com.ibm.ws.host";
        String hostPortPid = "com.ibm.ws.host.port";
        MetaTypeRegistry registry = createRegistry(portPid, hostPid, hostPortPid, 0);

        String xml = "<server>" +
                     "    <host id=\"one\">" +
                     "      <port id=\"two\" number=\"1\"/>" +
                     "    </host>" +
                     "    <host id=\"one\">" +
                     "      <port id=\"two\" number=\"2\"/>" +
                     "    </host>" +
                     "</server>";

        ServerConfiguration serverConfig = configParser.parseServerConfiguration(new StringReader(xml));

        ConfigEvaluator evaluator = createConfigEvaluator(registry, null);

        ConfigElement entry = null;
        EvaluationResult result = null;
        EvaluationResult nestedResult = null;

        RegistryEntry registryEntry = registry.getRegistryEntry(hostPid);
        entry = serverConfig.getFactoryInstance(hostPid, registryEntry.getAlias(), "one");
        result = evaluator.evaluate(entry, registryEntry);

        ConfigID parentId = new ConfigID(hostPid, "one");
        ConfigID nestedSingleId = new ConfigID(parentId, hostPortPid, "two", "port");

        Map<ConfigID, EvaluationResult> nested = result.getNested();
        assertEquals(1, nested.size());
        ConfigID id = nested.keySet().iterator().next();
        assertEquals(nestedSingleId, id);

        nestedResult = result.getNested().get(nestedSingleId);
        assertNotNull(nestedResult);
        assertEquals("2", nestedResult.getProperties().get("number"));
    }

    @Test
    public void testNestedMergeArray() throws ConfigEvaluatorException, ConfigParserException, ConfigValidationException {
        changeLocationSettings("default");

        String portPid = "com.ibm.ws.port";
        String hostPid = "com.ibm.ws.host";
        String hostPortPid = "com.ibm.ws.host.port";
        MetaTypeRegistry registry = createRegistry(portPid, hostPid, hostPortPid, 10);

        testNestedMerge(hostPid, hostPortPid, registry);

        testNestedMergeWithId(hostPid, hostPortPid, registry);
    }

    @Test
    public void testNestedMergeVector() throws ConfigEvaluatorException, ConfigParserException, ConfigValidationException {
        changeLocationSettings("default");

        String portPid = "com.ibm.ws.port";
        String hostPid = "com.ibm.ws.host";
        String hostPortPid = "com.ibm.ws.host.port";
        MetaTypeRegistry registry = createRegistry(portPid, hostPid, hostPortPid, 10);

        testNestedMerge(hostPid, hostPortPid, registry);

        testNestedMergeWithId(hostPid, hostPortPid, registry);
    }

    @Test
    public void testNestedMerge() throws Exception {
        changeLocationSettings("default");

        testNestedMergeWithId("host", "port", null);
    }

    /*
     * Assumes cardinality != 0
     */
    private void testNestedMerge(String hostPid, String portPid, MetaTypeRegistry registry) throws ConfigEvaluatorException, ConfigParserException, ConfigValidationException {
        String xml = "<server>" +
                     "    <host id=\"one\">" +
                     "      <port number=\"1\"/>" +
                     "    </host>" +
                     "    <host id=\"one\">" +
                     "      <port number=\"2\"/>" +
                     "    </host>" +
                     "</server>";

        ServerConfiguration serverConfig = configParser.parseServerConfiguration(new StringReader(xml));

        ConfigEvaluator evaluator = createConfigEvaluator(registry, null);

        ConfigElement entry = null;
        EvaluationResult result = null;
        EvaluationResult nestedResult = null;

        RegistryEntry registryEntry = registry.getRegistryEntry(hostPid);
        try {
            entry = serverConfig.getFactoryInstance(hostPid, registryEntry.getAlias(), "one");
        } catch (ConfigMergeException e) {
            throw new ConfigEvaluatorException(e);
        }
        result = evaluator.evaluate(entry, registryEntry);

        ConfigID parentId = new ConfigID(hostPid, "one");
        ConfigID nestedOneId = new ConfigID(parentId, portPid, "default-0", "port");
        ConfigID nestedTwoId = new ConfigID(parentId, portPid, "default-1", "port");

        assertEquals(2, result.getNested().size());
        nestedResult = result.getNested().get(nestedOneId);
        assertNotNull(nestedResult);
        assertEquals("1", nestedResult.getProperties().get("number"));

        nestedResult = result.getNested().get(nestedTwoId);
        assertNotNull(nestedResult);
        assertEquals("2", nestedResult.getProperties().get("number"));

    }

    /*
     * Assumes cardinality != 0.
     */
    private void testNestedMergeWithId(String hostPid, String portPid,
                                       MetaTypeRegistry registry) throws ConfigEvaluatorException, ConfigParserException, ConfigValidationException {
        String xml = "<server>" +
                     "    <host id=\"one\">" +
                     "      <port number=\"1\"/>" +
                     "    </host>" +
                     "    <host id=\"one\">" +
                     "      <port number=\"2\"/>" +
                     "      <port id=\"a\" number=\"1000\"/>" +
                     "    </host>" +
                     "    <host id=\"one\">" +
                     "      <port id=\"a\" number=\"2000\"/>" +
                     "    </host>" +
                     "</server>";

        ServerConfiguration serverConfig = configParser.parseServerConfiguration(new StringReader(xml));

        ConfigEvaluator evaluator = createConfigEvaluator(registry, null);

        ConfigElement entry = null;
        EvaluationResult result = null;
        EvaluationResult nestedResult = null;

        String hostAlias = null;
        RegistryEntry hostRE = null;
        if (registry != null) {
            RegistryEntry registryEntry = registry.getRegistryEntry(hostPid);
            hostAlias = registryEntry.getAlias();
            hostRE = registryEntry;
        }
        try {
            entry = serverConfig.getFactoryInstance(hostPid, hostAlias, "one");
        } catch (ConfigMergeException e) {
            throw new ConfigEvaluatorException(e);
        }
        result = evaluator.evaluate(entry, hostRE);

        ConfigID parentId = new ConfigID(hostPid, "one");
        ConfigID nestedOneId = new ConfigID(parentId, portPid, "default-0", "port");
        ConfigID nestedTwoId = new ConfigID(parentId, portPid, "default-1", "port");
        ConfigID nestedThreeId = new ConfigID(parentId, portPid, "a", "port");

        assertEquals(3, result.getNested().size());
        nestedResult = result.getNested().get(nestedOneId);
        assertNotNull(nestedResult);
        assertEquals("1", nestedResult.getProperties().get("number"));

        nestedResult = result.getNested().get(nestedTwoId);
        assertNotNull(nestedResult);
        assertEquals("2", nestedResult.getProperties().get("number"));

        nestedResult = result.getNested().get(nestedThreeId);
        assertNotNull(nestedResult);
        assertEquals("2000", nestedResult.getProperties().get("number"));
    }

    @Test
    public void testSingleReferenceOne() throws Exception {
        testSingleReference("port", "portRef");
    }

    @Test
    public void testSingleReferenceTwo() throws Exception {
        testSingleReference("portRef", "port");
    }

    private void testSingleReference(String portAttribute, String portRefAttribute) throws Exception {
        changeLocationSettings("default");

        String portPid = "com.ibm.ws.port";
        String hostPid = "com.ibm.ws.host";
        String hostPortPid = "com.ibm.ws.host.port";
        MetaTypeRegistry registry = createRegistry(portPid, hostPid, hostPortPid, portAttribute, 0, hostPortPid, true);

        ConfigEvaluator evaluator = createConfigEvaluator(registry, null);

        String xml;
        ServerConfiguration serverConfig;
        ConfigElement entry = null;
        EvaluationResult result = null;
        Dictionary<String, Object> dictionary = null;

        RegistryEntry registryEntry = registry.getRegistryEntry(hostPid);
        String hostAlias = registryEntry.getAlias();

        xml = "<server>" +
              "    <com.ibm.ws.host.port id=\"host1\" number=\"8080\"/>" +
              "    <host id=\"one\" portRef=\"host1\">" +
              "    </host>" +
              "</server>";

        serverConfig = configParser.parseServerConfiguration(new StringReader(xml));

        entry = serverConfig.getFactoryInstance(hostPid, hostAlias, "one");
        result = evaluator.evaluate(entry, registryEntry);
        dictionary = result.getProperties();

        assertNull(dictionary.get(portRefAttribute));
        Object value = dictionary.get(portAttribute);
        assertNotNull(value);
        assertTrue(value instanceof String);
        assertTrue(result.getNested().isEmpty());

        // nested element wins if cardinality == 0

        xml = "<server>" +
              "    <com.ibm.ws.host.port id=\"host1\" number=\"8080\"/>" +
              "    <host id=\"one\" portRef=\"host1\">" +
              "        <port number=\"9000\" />" +
              "    </host>" +
              "</server>";

        serverConfig = configParser.parseServerConfiguration(new StringReader(xml));

        entry = serverConfig.getFactoryInstance(hostPid, hostAlias, "one");
        result = evaluator.evaluate(entry, registryEntry);
        dictionary = result.getProperties();

        assertNull(dictionary.get(portRefAttribute));
        value = dictionary.get(portAttribute);
        assertNotNull(value);
        assertTrue(value instanceof String);
        assertEquals(1, result.getNested().size());
        dictionary = result.getNested().values().iterator().next().getProperties();
        assertEquals("9000", dictionary.get("number"));
    }

    @Test
    public void testMultipleReferencesOne() throws Exception {
        testMultipleReferences("port", "portRef", true);
        testMultipleReferences("port", "portRef", false);
    }

    @Test
    public void testMultipleReferencesTwo() throws Exception {
        testMultipleReferences("portRef", "port", true);
        testMultipleReferences("portRef", "port", false);
    }

    private void testMultipleReferences(String portAttribute, String portRefAttribute, boolean array) throws Exception {
        changeLocationSettings("default");

        String portPid = "com.ibm.ws.port";
        String hostPid = "com.ibm.ws.host";
        String hostPortPid = "com.ibm.ws.host.port";
        MetaTypeRegistry registry = createRegistry(portPid, hostPid, hostPortPid, portAttribute, (array) ? 10 : -10, hostPortPid, true);

        TestConfigEvaluator evaluator = createConfigEvaluator(registry, null);

        ConfigID parentId = new ConfigID(hostPid, "one");
        ConfigID host1 = new ConfigID(hostPortPid, "host1");
        ConfigID host2 = new ConfigID(hostPortPid, "host2");
        ConfigID host3 = new ConfigID(parentId, hostPortPid, "host3", "port");
        ConfigID hostGenId = new ConfigID(parentId, hostPortPid, "default-2", "port");

        Map<ConfigID, String> pidMap = new HashMap<ConfigID, String>();
        pidMap.put(host1, "pid-1");
        pidMap.put(host2, "pid-2");
        pidMap.put(host3, "pid-3");
        pidMap.put(hostGenId, "pid-4");
        evaluator.setLookupMap(pidMap);

        String xml;
        ServerConfiguration serverConfig;
        ConfigElement entry = null;
        EvaluationResult result = null;
        EvaluationResult nestedResult = null;
        Dictionary<String, Object> dictionary = null;

        xml = "<server>" +
              "    <com.ibm.ws.host.port id=\"host1\" number=\"8080\"/>" +
              "    <com.ibm.ws.host.port id=\"host2\" number=\"9090\"/>" +
              "    <host id=\"one\" portRef=\"host1, host2\">" +
              "        <port number=\"9000\" />" +
              "        <port id=\"host3\" number=\"9999\" />" +
              "    </host>" +
              "    <host id=\"one\" >" +
              "        <port id=\"host3\" number=\"1111\" />" +
              "    </host>" +
              "</server>";

        serverConfig = configParser.parseServerConfiguration(new StringReader(xml));

        RegistryEntry registryEntry = registry.getRegistryEntry(hostPid);
        String hostAlias = registryEntry.getAlias();

        entry = serverConfig.getFactoryInstance(hostPid, hostAlias, "one");
        result = evaluator.evaluate(entry, registryEntry);
        dictionary = result.getProperties();

        // we should have 3 injected pids
        assertNull(dictionary.get(portRefAttribute));
        Object value = dictionary.get(portAttribute);
        assertNotNull(value);
        if (array) {
            assertTrue(value instanceof String[]);
            assertEquals("values: " + toSet((String[]) value), 4, ((String[]) value).length);
            assertEquals(toSet("pid-1", "pid-2", "pid-3", "pid-4"), toSet((String[]) value));
        } else {
            assertTrue(value instanceof List<?>);
            assertEquals(4, ((List<?>) value).size());
            assertEquals(toSet("pid-1", "pid-2", "pid-3", "pid-4"), new HashSet<Object>((List<?>) value));
        }

        // we have two nested elements
        assertEquals(2, result.getNested().size());
        nestedResult = result.getNested().get(host3);
        assertNotNull(nestedResult);
        assertEquals("1111", nestedResult.getProperties().get("number"));

        nestedResult = result.getNested().get(hostGenId);
        assertNotNull(nestedResult);
        assertEquals("9000", nestedResult.getProperties().get("number"));
    }

    @Test
    public void testSupportHiddenExtensions() throws Exception {
        changeLocationSettings("default");

        String pid = "com.ibm.ws.test";
        String alias = "test";
        MockObjectClassDefinition ocd = new MockObjectClassDefinition(alias);
        ocd.setSupportsHiddenExtensions(true);

        MockBundle bundle = new MockBundle();
        MockMetaTypeInformation metatype = new MockMetaTypeInformation(bundle);
        metatype.add(pid, true, ocd);

        metatype.add("com.ibm.ws.test.child", true, new MockObjectClassDefinition("child"));
        metatype.add("com.ibm.ws.test.child2", true, new MockObjectClassDefinition("child2"));

        MetaTypeRegistry registry = new MetaTypeRegistry();
        assertFalse("The registry should be updated", registry.addMetaType(metatype).isEmpty());

        RegistryEntry testRE = registry.getRegistryEntry(pid);

        TestConfigEvaluator evaluator = createConfigEvaluator(registry, null);
        Map<ConfigID, String> pids = new HashMap<ConfigID, String>();
        pids.put(new ConfigID("com.ibm.ws.test", "one"), "test-pid");
        pids.put(new ConfigID(new ConfigID("com.ibm.ws.test", "one"), "child", "default-0", "child"), "child-pid");
        pids.put(new ConfigID(new ConfigID("com.ibm.ws.test", "one"), "child2", "default-0", "child2"), "child2-pid0");
        pids.put(new ConfigID(new ConfigID("com.ibm.ws.test", "one"), "child2", "default-1", "child2"), "child2-pid1");
        pids.put(new ConfigID(new ConfigID("com.ibm.ws.test", "one"), "unknownChild", "default-0", "unknownChild"), "unknownChild-pid");
        pids.put(new ConfigID(new ConfigID("com.ibm.ws.test", "one"), "unknownChild2", "default-0", "unknownChild2"), "unknownChild2-pid0");
        pids.put(new ConfigID(new ConfigID("com.ibm.ws.test", "one"), "unknownChild2", "default-1", "unknownChild2"), "unknownChild2-pid1");
        evaluator.setLookupMap(pids);

        String xml = "<server>" +
                     "    <test id='one'>" +
                     "        <child/>" +
                     "        <child2/>" +
                     "        <child2/>" +
                     "        <unknownChild/>" +
                     "        <unknownChild2/>" +
                     "        <unknownChild2/>" +
                     "    </test>" +
                     "</server>";

        ServerConfiguration serverConfig = configParser.parseServerConfiguration(new StringReader(xml));
        ConfigElement entry = serverConfig.getFactoryInstance(pid, alias, "one");
        EvaluationResult result = evaluator.evaluate(entry, testRE);
        Dictionary<String, Object> props = result.getProperties();

        Assert.assertNull(props.get("child"));
        Assert.assertNull(props.get("child2"));
        Assert.assertNull(props.get("unknownChild"));
        Assert.assertNull(props.get("unknownChild2"));
        Assert.assertTrue(result.getReferences().toString(), result.getReferences().isEmpty());
    }

    @Test
    public void testVariablesInIDFields() throws Exception {
        changeLocationSettings("default");

        String loggingPid = "com.ibm.ws.logging";
        String singletonPid = "com.ibm.ws.singleton";
        MockObjectClassDefinition loggingOCD = new MockObjectClassDefinition("logging");
        MockAttributeDefinition logDirectoryAttribute = new MockAttributeDefinition("logDirectory", AttributeDefinition.STRING, 0, null);
        loggingOCD.addAttributeDefinition(logDirectoryAttribute);
        loggingOCD.setAlias("logging");

        MockObjectClassDefinition singletonOCD = new MockObjectClassDefinition("someSingleton");
        MockAttributeDefinition someAttribute = new MockAttributeDefinition("someAttribute", AttributeDefinition.STRING, 0, null);
        singletonOCD.addAttributeDefinition(someAttribute);
        singletonOCD.setAlias("singleton");

        MockBundle bundle = new MockBundle();
        MockMetaTypeInformation metatype = new MockMetaTypeInformation(bundle);
        metatype.add(loggingPid, true, loggingOCD);
        metatype.add(singletonPid, false, singletonOCD);

        MetaTypeRegistry registry = new MetaTypeRegistry();
        assertFalse("The registry should be updated", registry.addMetaType(metatype).isEmpty());

        RegistryEntry loggingRE = registry.getRegistryEntry(loggingPid);
        RegistryEntry singletonRE = registry.getRegistryEntry(singletonPid);
        String xml = "<server>" +
                     "  <logging id=\"${one}\" logDirectory=\"${one}\"/>\n" +
                     "  <singleton id=\"${one}\" someAttribute=\"${one}\"/>\n" +
                     "</server>";
        ServerConfiguration serverConfig = configParser.parseServerConfiguration(new StringReader(xml));

        VariableRegistryHelper variableRegistryHelper = new VariableRegistryHelper(new SymbolRegistry());
        variableRegistryHelper.addVariable("one", "replacedValue");
        ConfigVariableRegistry variableRegistry = new ConfigVariableRegistry(variableRegistryHelper, new String[0], null);
        TestConfigEvaluator evaluator = createConfigEvaluator(registry, variableRegistry, null);

        ConfigElement entry = serverConfig.getFactoryInstance(loggingPid, "logging", "${one}");

        EvaluationResult result = evaluator.evaluate(entry, loggingRE);
        Dictionary<String, Object> dictionary = result.getProperties();
        assertEquals("replacedValue", dictionary.get("id"));
        assertEquals("replacedValue", dictionary.get("logDirectory"));

        ConfigElement singleton = serverConfig.getSingleton(singletonPid, "singleton");
        result = evaluator.evaluate(singleton, singletonRE);
        dictionary = result.getProperties();
        assertEquals("replacedValue", dictionary.get("id"));
        assertEquals("replacedValue", dictionary.get("someAttribute"));

        assertTrue("We should get a warning messages about variables in ID fields",
                   outputMgr.checkForMessages("CWWKG0104W.*\\{one\\}.*logging.*"));
        assertFalse("We should not get a warning message for variables in a singleton",
                    outputMgr.checkForMessages("CWWKG0104W.*\\{one\\}.*singleton.*"));

    }

    @Test
    public void testImplicitVariables() throws Exception {
        changeLocationSettings("default");

        String portPid = "com.ibm.ws.port";
        MockObjectClassDefinition portOCD = new MockObjectClassDefinition("port");
        portOCD.addAttributeDefinition(new MockAttributeDefinition("number", AttributeDefinition.STRING, 0, null));
        portOCD.setAlias("port");

        String hostPid = "com.ibm.ws.host";
        String hostAlias = "host";
        MockObjectClassDefinition hostOCD = new MockObjectClassDefinition("host");
        hostOCD.addAttributeDefinition(new MockAttributeDefinition("a", AttributeDefinition.STRING, 0, new String[] { "a=${portRef}" }));
        MockAttributeDefinition portRef = new MockAttributeDefinition("portRef", AttributeDefinition.STRING, 0, new String[] { "default" });
        portRef.setReferencePid(portPid);
        hostOCD.addAttributeDefinition(portRef);
        hostOCD.addAttributeDefinition(new MockAttributeDefinition("z", AttributeDefinition.STRING, 0, new String[] { "z=${portRef}" }));
        hostOCD.setAlias(hostAlias);

        MockBundle bundle = new MockBundle();
        MockMetaTypeInformation metatype = new MockMetaTypeInformation(bundle);
        metatype.add(portPid, true, portOCD);
        metatype.add(hostPid, true, hostOCD);

        MetaTypeRegistry registry = new MetaTypeRegistry();
        assertFalse("The registry should be updated", registry.addMetaType(metatype).isEmpty());

        RegistryEntry hostRE = registry.getRegistryEntry(hostPid);

        TestConfigEvaluator evaluator = createConfigEvaluator(registry, null);

        ConfigID portDefaultId = new ConfigID(portPid, "default");
        ConfigID parentId = new ConfigID(hostPid, "one");
        ConfigID portNestedId = new ConfigID(parentId, portPid, "nested", "port");

        String defaultPortPid = "default-port-pid";
        String nestedPortPid = "nested-port-pid";

        Map<ConfigID, String> pidMap = new HashMap<ConfigID, String>();
        pidMap.put(portDefaultId, defaultPortPid);
        pidMap.put(portNestedId, nestedPortPid);
        evaluator.setLookupMap(pidMap);

        String xml;
        ServerConfiguration serverConfig;
        ConfigElement entry = null;
        EvaluationResult result = null;
        Dictionary<String, Object> dictionary = null;

        xml = "<server>" +
              "    <port id=\"default\" number=\"8080\"/>" +
              "    <host id=\"one\" >" +
              "    </host>" +
              "</server>";

        serverConfig = configParser.parseServerConfiguration(new StringReader(xml));

        entry = serverConfig.getFactoryInstance(hostPid, hostAlias, "one");
        result = evaluator.evaluate(entry, hostRE);
        dictionary = result.getProperties();

        System.out.println(dictionary);

        assertEquals(defaultPortPid, dictionary.get("portRef"));
        assertNull(dictionary.get("port"));
        assertEquals("a=" + defaultPortPid, dictionary.get("a"));
        assertEquals("z=" + defaultPortPid, dictionary.get("z"));

        xml = "<server>" +
              "    <port id=\"default\" number=\"8080\"/>" +
              "    <host id=\"one\" >" +
              "        <port id=\"nested\" number=\"9999\"/>" +
              "    </host>" +
              "</server>";

        serverConfig = configParser.parseServerConfiguration(new StringReader(xml));

        entry = serverConfig.getFactoryInstance(hostPid, hostAlias, "one");
        result = evaluator.evaluate(entry, hostRE);
        dictionary = result.getProperties();

        System.out.println(dictionary);

        assertEquals(nestedPortPid, dictionary.get("portRef"));
        assertNull(dictionary.get("port"));
        assertEquals("a=" + nestedPortPid, dictionary.get("a"));
        assertEquals("z=" + nestedPortPid, dictionary.get("z"));

        VariableRegistry vr = new ConfigVariableRegistry(new VariableRegistryHelper(), new String[0], null);
        assertEquals("portRef should not be in the variable registry", "${portRef}", vr.resolveString("${portRef}"));
        // Just checking that the variable registry is sane
        assertEquals("server config dir should be in the variable registry", wsLocation.resolveString("${" + WsLocationConstants.LOC_SERVER_CONFIG_DIR + "}"),
                     vr.resolveString("${" + WsLocationConstants.LOC_SERVER_CONFIG_DIR + "}"));
    }

    /**
     * Test the metatype attribute for delayed variable expansion. If subsititution is 'immediate' or not specified, the variable should be expanded.
     * If subsitution is 'delayed', the variable should not be expanded.
     *
     * @throws Exception
     */
    @Test
    public void testVariableDelay() throws Exception {
        changeLocationSettings("default");

        String loggingPid = "com.ibm.ws.logging";
        MockObjectClassDefinition loggingOCD = new MockObjectClassDefinition("logging");
        MockAttributeDefinition deferredAttribute = new MockAttributeDefinition("deferred", AttributeDefinition.STRING, 0, null);
        deferredAttribute.setSubstitution("false");

        MockAttributeDefinition immediateAttribute = new MockAttributeDefinition("immediate", AttributeDefinition.STRING, 0, null);
        immediateAttribute.setSubstitution("true");

        MockAttributeDefinition defaultAttribute = new MockAttributeDefinition("defaultAttr", AttributeDefinition.STRING, 0, null);

        loggingOCD.addAttributeDefinition(defaultAttribute);
        loggingOCD.addAttributeDefinition(immediateAttribute);
        loggingOCD.addAttributeDefinition(deferredAttribute);
        loggingOCD.setAlias("logging");

        MockBundle bundle = new MockBundle();
        MockMetaTypeInformation metatype = new MockMetaTypeInformation(bundle);
        metatype.add(loggingPid, true, loggingOCD);

        MetaTypeRegistry registry = new MetaTypeRegistry();
        assertFalse("The registry should be updated", registry.addMetaType(metatype).isEmpty());

        RegistryEntry loggingRE = registry.getRegistryEntry(loggingPid);

        String xml = "<server>" +
                     "  <logging id=\"one\" deferred=\"${test}\" immediate=\"${test}\" defaultAttr=\"${test}\" />\n" +
                     "</server>";
        ServerConfiguration serverConfig = configParser.parseServerConfiguration(new StringReader(xml));

        VariableRegistryHelper variableRegistryHelper = new VariableRegistryHelper(new SymbolRegistry());
        variableRegistryHelper.addVariable("test", "expanded variable");
        ConfigVariableRegistry variableRegistry = new ConfigVariableRegistry(variableRegistryHelper, new String[0], null);
        TestConfigEvaluator evaluator = createConfigEvaluator(registry, variableRegistry, null);

        ConfigElement entry = serverConfig.getFactoryInstance(loggingPid, "logging", "one");

        EvaluationResult result = evaluator.evaluate(entry, loggingRE);
        Dictionary<String, Object> dictionary = result.getProperties();
        assertEquals("expanded variable", dictionary.get("immediate"));
        assertEquals("expanded variable", dictionary.get("defaultAttr"));
        assertEquals("${test}", dictionary.get("deferred"));
    }

    @Test
    public void testVariableExtension() throws Exception {
        changeLocationSettings("default");

        String loggingPid = "com.ibm.ws.logging";
        MockObjectClassDefinition loggingOCD = new MockObjectClassDefinition("logging");
        MockAttributeDefinition logDirectoryAttribute = new MockAttributeDefinition("logDirectory", AttributeDefinition.STRING, 0, null);
        logDirectoryAttribute.setVariable("com.ibm.ws.logging.log.directory");
        loggingOCD.addAttributeDefinition(logDirectoryAttribute);
        loggingOCD.setAlias("logging");

        MockBundle bundle = new MockBundle();
        MockMetaTypeInformation metatype = new MockMetaTypeInformation(bundle);
        metatype.add(loggingPid, true, loggingOCD);

        MetaTypeRegistry registry = new MetaTypeRegistry();
        assertFalse("The registry should be updated", registry.addMetaType(metatype).isEmpty());

        RegistryEntry loggingRE = registry.getRegistryEntry(loggingPid);

        String xml = "<server>" +
                     "  <logging id=\"one\"/>\n" +
                     "</server>";
        ServerConfiguration serverConfig = configParser.parseServerConfiguration(new StringReader(xml));

        VariableRegistryHelper variableRegistryHelper = new VariableRegistryHelper(new SymbolRegistry());
        variableRegistryHelper.addVariable("com.ibm.ws.logging.log.directory", "a,b,c");
        ConfigVariableRegistry variableRegistry = new ConfigVariableRegistry(variableRegistryHelper, new String[0], null);
        TestConfigEvaluator evaluator = createConfigEvaluator(registry, variableRegistry, null);

        ConfigElement entry = serverConfig.getFactoryInstance(loggingPid, "logging", "one");

        EvaluationResult result = evaluator.evaluate(entry, loggingRE);
        Dictionary<String, Object> dictionary = result.getProperties();
        assertEquals("a,b,c", dictionary.get("logDirectory"));
    }

    // SAme as testVariableExtension, but using a variable list function
    @Test
    public void testVariableExtensionList() throws Exception {
        changeLocationSettings("default");

        String loggingPid = "com.ibm.ws.logging";
        MockObjectClassDefinition loggingOCD = new MockObjectClassDefinition("logging");
        MockAttributeDefinition logDirectoryAttribute = new MockAttributeDefinition("logDirectory", AttributeDefinition.STRING, 5, null);
        logDirectoryAttribute.setVariable("list(com.ibm.ws.logging.log.directory)");
        loggingOCD.addAttributeDefinition(logDirectoryAttribute);
        loggingOCD.setAlias("logging");

        MockBundle bundle = new MockBundle();
        MockMetaTypeInformation metatype = new MockMetaTypeInformation(bundle);
        metatype.add(loggingPid, true, loggingOCD);

        MetaTypeRegistry registry = new MetaTypeRegistry();
        assertFalse("The registry should be updated", registry.addMetaType(metatype).isEmpty());

        RegistryEntry loggingRE = registry.getRegistryEntry(loggingPid);

        String xml = "<server>" +
                     "  <logging id=\"one\"/>\n" +
                     "</server>";
        ServerConfiguration serverConfig = configParser.parseServerConfiguration(new StringReader(xml));

        VariableRegistryHelper variableRegistryHelper = new VariableRegistryHelper(new SymbolRegistry());
        variableRegistryHelper.addVariable("com.ibm.ws.logging.log.directory", "a,b,c");
        ConfigVariableRegistry variableRegistry = new ConfigVariableRegistry(variableRegistryHelper, new String[0], null);
        TestConfigEvaluator evaluator = createConfigEvaluator(registry, variableRegistry, null);

        ConfigElement entry = serverConfig.getFactoryInstance(loggingPid, "logging", "one");

        EvaluationResult result = evaluator.evaluate(entry, loggingRE);
        Dictionary<String, Object> dictionary = result.getProperties();
        String[] value = (String[]) dictionary.get("logDirectory");
        assertEquals("a", value[0]);
        assertEquals("b", value[1]);
        assertEquals("c", value[2]);

    }

    @Test
    public void testVariableExtensionListCardinalityFailure() throws Exception {
        changeLocationSettings("default");

        String loggingPid = "com.ibm.ws.logging";
        MockObjectClassDefinition loggingOCD = new MockObjectClassDefinition("logging");
        MockAttributeDefinition logDirectoryAttribute = new MockAttributeDefinition("logDirectory", AttributeDefinition.STRING, 2, null);
        logDirectoryAttribute.setVariable("list(com.ibm.ws.logging.log.directory)");
        loggingOCD.addAttributeDefinition(logDirectoryAttribute);
        loggingOCD.setAlias("logging");

        MockBundle bundle = new MockBundle();
        MockMetaTypeInformation metatype = new MockMetaTypeInformation(bundle);
        metatype.add(loggingPid, true, loggingOCD);

        MetaTypeRegistry registry = new MetaTypeRegistry();
        assertFalse("The registry should be updated", registry.addMetaType(metatype).isEmpty());

        RegistryEntry loggingRE = registry.getRegistryEntry(loggingPid);

        String xml = "<server>" +
                     "  <logging id=\"one\"/>\n" +
                     "</server>";
        ServerConfiguration serverConfig = configParser.parseServerConfiguration(new StringReader(xml));

        VariableRegistryHelper variableRegistryHelper = new VariableRegistryHelper(new SymbolRegistry());
        variableRegistryHelper.addVariable("com.ibm.ws.logging.log.directory", "a,b,c");
        ConfigVariableRegistry variableRegistry = new ConfigVariableRegistry(variableRegistryHelper, new String[0], null);
        TestConfigEvaluator evaluator = createConfigEvaluator(registry, variableRegistry, null);

        ConfigElement entry = serverConfig.getFactoryInstance(loggingPid, "logging", "one");

        try {
            evaluator.evaluate(entry, loggingRE);
        } catch (ConfigEvaluatorException ex) {
            // Passed
            return;
        }

        fail("A ConfigEvaluatorException should have been thrown for a cardinality violation");

    }

    @Test
    public void testVariableExpression() throws Exception {
        changeLocationSettings("default");

        String topPid = "com.ibm.ws.top";
        MockObjectClassDefinition topOCD = new MockObjectClassDefinition("top");
        MockAttributeDefinition stringAttribute = new MockAttributeDefinition("string", AttributeDefinition.STRING, 0, null);
        topOCD.addAttributeDefinition(stringAttribute);
        for (int cardinality : new int[] { Integer.MAX_VALUE, Integer.MIN_VALUE }) {
            for (int num = 0; num < 3; num++) {
                String name = cardinality < 0 ? "vector" : "array";
                MockAttributeDefinition pidAttribute = new MockAttributeDefinition(name + num, AttributeDefinition.STRING, cardinality, null);
                pidAttribute.setReferencePid(topPid);
                topOCD.addAttributeDefinition(pidAttribute);
            }
        }
        MockAttributeDefinition intArray2Attribute = new MockAttributeDefinition("intArray2", AttributeDefinition.INTEGER, Integer.MAX_VALUE, null);
        topOCD.addAttributeDefinition(intArray2Attribute);
        MockAttributeDefinition intVector2Attribute = new MockAttributeDefinition("intVector2", AttributeDefinition.INTEGER, Integer.MIN_VALUE, null);
        topOCD.addAttributeDefinition(intVector2Attribute);
        MockAttributeDefinition valueAttribute = new MockAttributeDefinition("value", AttributeDefinition.STRING, Integer.MAX_VALUE, null);
        topOCD.addAttributeDefinition(valueAttribute);
        MockAttributeDefinition intValueAttribute = new MockAttributeDefinition("intValue", AttributeDefinition.INTEGER, Integer.MAX_VALUE, null);
        topOCD.addAttributeDefinition(intValueAttribute);
        topOCD.setAlias("top");

        MockBundle bundle = new MockBundle();
        MockMetaTypeInformation metatype = new MockMetaTypeInformation(bundle);
        metatype.add(topPid, true, topOCD);

        MetaTypeRegistry registry = new MetaTypeRegistry();
        assertFalse("The registry should be updated", registry.addMetaType(metatype).isEmpty());

        RegistryEntry topRE = registry.getRegistryEntry(topPid);

        String xml = "<server>" +
                     "  <top id=\"one\"" +
                     "    string=\"str\"" +
                     "    array0=\"\"" +
                     "    array1=\"one\"" +
                     "    array2=\"one, two\"" +
                     "    vector0=\"\"" +
                     "    vector1=\"one\"" +
                     "    vector2=\"one, two\"" +
                     "    int=\"1024\"" +
                     "    intArray2=\"1, 2\"" +
                     "    intVector2=\"1, 2\"" +
                     "    int.with.dots=\"2048\"" +
                     "    maxLong=\"9223372036854775807\"" +
                     "    minLong=\"-9223372036854775808\"" +
                     "    maxLongPlusOne=\"9223372036854775808\"" +
                     "    minLongMinusOne=\"-9223372036854775809\"" +
                     "  >\n" +
                     // servicePidFilter function
                     "    <value>${servicePidOrFilter(unspecified)}</value>\n" +
                     "    <value>${servicePidOrFilter(string)}</value>\n" +
                     "    <value>${servicePidOrFilter(array0)}</value>\n" +
                     "    <value>${servicePidOrFilter(array1)}</value>\n" +
                     "    <value>${servicePidOrFilter(array2)}</value>\n" +
                     "    <value>${servicePidOrFilter(vector0)}</value>\n" +
                     "    <value>${servicePidOrFilter(vector1)}</value>\n" +
                     "    <value>${servicePidOrFilter(vector2)}</value>\n" +
                     // servicePidOrFilter function errors
                     "    <value>${servicePidOrFilter(intArray2)}</value>\n" +
                     "    <value>${servicePidOrFilter(intVector2)}</value>\n" +
                     // Function syntax errors
                     "    <value>${servicePidOrFilter(}</value>\n" +
                     "    <value>${servicePidOrFilter()}</value>\n" +
                     "    <value>${servicePidOrFilter(string}</value>\n" +
                     "    <value>${servicePidOrFilter(0)}</value>\n" +
                     "    <value>${ servicePidOrFilter(string)}</value>\n" +
                     "    <value>${servicePidOrFilter (string)}</value>\n" +
                     "    <value>${servicePidOrFilter( string)}</value>\n" +
                     "    <value>${servicePidOrFilter(string )}</value>\n" +
                     "    <value>${servicePidOrFilter(string) }</value>\n" +
                     "    <value>${unknownFunction(string)}</value>\n" +
                     // count function
                     "    <value>${count(unspecified)}</value>\n" +
                     "    <value>${count(int)}</value>\n" +
                     "    <value>${count(string)}</value>\n" +
                     "    <value>${count(array0)}</value>\n" +
                     "    <value>${count(array1)}</value>\n" +
                     "    <value>${count(array2)}</value>\n" +
                     "    <value>${count(vector0)}</value>\n" +
                     "    <value>${count(vector1)}</value>\n" +
                     "    <value>${count(vector2)}</value>\n" +
                     "    <value>${count(intArray2)}</value>\n" +
                     "    <value>${count(intVector2)}</value>\n" +
                     // Add operator
                     "    <value>${0+0}</value>\n" +
                     "    <value>${int+0}</value>\n" +
                     "    <value>${int+16}</value>\n" +
                     "    <value>${0+int}</value>\n" +
                     "    <value>${16+int}</value>\n" +
                     "    <value>${int+int}</value>\n" +
                     "    <value>${0+9223372036854775807}</value>\n" +
                     "    <value>${9223372036854775807+9223372036854775807}</value>\n" +
                     "    <value>${0+maxLong}</value>\n" +
                     "    <value>${0+minLong}</value>\n" +
                     "    <value>${count(array0)+count(array1)}</value>\n" +
                     // Operator errors
                     "    <value>${ 0+0}</value>\n" +
                     "    <value>${0+0 }</value>\n" +
                     "    <value>${0 +0}</value>\n" +
                     "    <value>${0+ 0}</value>\n" +
                     "    <value>${0 + 0}</value>\n" +
                     "    <value>${str+0}</value>\n" +
                     "    <value>${0+str}</value>\n" +
                     "    <value>${unspecified+0}</value>\n" +
                     "    <value>${0+unspecified}</value>\n" +
                     "    <value>${0+9223372036854775808}</value>\n" +
                     "    <value>${0+maxLongPlusOne}</value>\n" +
                     "    <value>${0+minLongMinusOne}</value>\n" +
                     // Subtract operator
                     "    <value>${0-0}</value>\n" +
                     "    <value>${int-0}</value>\n" +
                     "    <value>${int-16}</value>\n" +
                     "    <value>${0-int}</value>\n" +
                     "    <value>${16-int}</value>\n" +
                     "    <value>${int-int}</value>\n" +
                     // Multiply operator
                     "    <value>${0*0}</value>\n" +
                     "    <value>${int*0}</value>\n" +
                     "    <value>${int*16}</value>\n" +
                     "    <value>${0*int}</value>\n" +
                     "    <value>${16*int}</value>\n" +
                     "    <value>${int*int}</value>\n" +
                     // Divide operator
                     "    <value>${0/0}</value>\n" +
                     "    <value>${int/0}</value>\n" +
                     "    <value>${int/16}</value>\n" +
                     "    <value>${0/int}</value>\n" +
                     "    <value>${16/int}</value>\n" +
                     "    <value>${int/int}</value>\n" +
                     // Misc
                     "    <value>${int.with.dots+0}</value>" +
                     // list operator
                     "    <value>${list(array2)}</value>" +
                     "    <value>${list(multiValue)}</value>" +
                     "    <value>${list(singleValue)}</value>" +
                     "    <value>${list(IDoNotExist)}</value>" +
                     "    <value>${  list(array2)}</value>" +
                     "    <value>${list( array2 )}</value>" +
                     "    <value>${list(}</value>" +
                     "    <value>${list(varArray)}</value>" +
                     "    <intValue>${list(intArray2)}</intValue>" +
                     "    <intValue>${list(intValues)}</intValue>" +
                     "  </top>\n" +
                     "  <top id=\"two\"/>" +
                     "</server>";
        ServerConfiguration serverConfig = configParser.parseServerConfiguration(new StringReader(xml));

        VariableRegistryHelper variableRegistryHelper = new VariableRegistryHelper(new SymbolRegistry());
        variableRegistryHelper.addVariable("multiValue", "a,b,c");
        variableRegistryHelper.addVariable("singleValue", "a");
        variableRegistryHelper.addVariable("intValues", "1,2,3,4,5");
        variableRegistryHelper.addVariable("varArray", "${multiValue}, ${singleValue}, ${intValues}");
        ConfigVariableRegistry variableRegistry = new ConfigVariableRegistry(variableRegistryHelper, new String[0], null);
        TestConfigEvaluator evaluator = createConfigEvaluator(registry, variableRegistry, null);

        Map<ConfigID, String> pids = new HashMap<ConfigID, String>();
        pids.put(new ConfigID(topPid, "one"), "pid_one");
        pids.put(new ConfigID(topPid, "two"), "pid_two");
        evaluator.setLookupMap(pids);

        ConfigElement entry = serverConfig.getFactoryInstance(topPid, "top", "one");
        Assert.assertNotNull(entry);

        EvaluationResult result = evaluator.evaluate(entry, topRE);
        Dictionary<String, Object> dictionary = result.getProperties();

        String[] values = (String[]) dictionary.get("value");
        int index = 0;
        // servicePidOrFilter function
        Assert.assertEquals("(service.pid=unbound)", values[index++]);
        Assert.assertEquals("(service.pid=str)", values[index++]);
        Assert.assertEquals("(service.pid=unbound)", values[index++]);
        Assert.assertEquals("(service.pid=pid_one)", values[index++]);
        Assert.assertEquals("(|(service.pid=pid_one)(service.pid=pid_two))", values[index++]);
        Assert.assertEquals("(service.pid=unbound)", values[index++]);
        Assert.assertEquals("(service.pid=pid_one)", values[index++]);
        Assert.assertEquals("(|(service.pid=pid_one)(service.pid=pid_two))", values[index++]);
        // servicePidOrFilter errors
        Assert.assertEquals("${servicePidOrFilter(intArray2)}", values[index++]);
        Assert.assertEquals("${servicePidOrFilter(intVector2)}", values[index++]);
        // Function syntax errors
        Assert.assertEquals("${servicePidOrFilter(}", values[index++]);
        Assert.assertEquals("${servicePidOrFilter()}", values[index++]);
        Assert.assertEquals("${servicePidOrFilter(string}", values[index++]);
        Assert.assertEquals("${servicePidOrFilter(0)}", values[index++]);
        Assert.assertEquals("${ servicePidOrFilter(string)}", values[index++]);
        Assert.assertEquals("${servicePidOrFilter (string)}", values[index++]);
        Assert.assertEquals("${servicePidOrFilter( string)}", values[index++]);
        Assert.assertEquals("${servicePidOrFilter(string )}", values[index++]);
        Assert.assertEquals("${servicePidOrFilter(string) }", values[index++]);
        Assert.assertEquals("${unknownFunction(string)}", values[index++]);
        // count function
        Assert.assertEquals("0", values[index++]);
        Assert.assertEquals("1", values[index++]);
        Assert.assertEquals("1", values[index++]);
        Assert.assertEquals("0", values[index++]);//array0
        Assert.assertEquals("1", values[index++]);//array1
        Assert.assertEquals("2", values[index++]);//array2
        Assert.assertEquals("0", values[index++]);
        Assert.assertEquals("1", values[index++]);
        Assert.assertEquals("2", values[index++]);
        Assert.assertEquals("2", values[index++]);
        Assert.assertEquals("2", values[index++]);
        // Add operator
        Assert.assertEquals("0", values[index++]);
        Assert.assertEquals("1024", values[index++]);
        Assert.assertEquals("1040", values[index++]);
        Assert.assertEquals("1024", values[index++]);
        Assert.assertEquals("1040", values[index++]);
        Assert.assertEquals("2048", values[index++]);
        Assert.assertEquals("9223372036854775807", values[index++]);
        Assert.assertEquals("-2", values[index++]);
        Assert.assertEquals("9223372036854775807", values[index++]);
        Assert.assertEquals("-9223372036854775808", values[index++]);
        Assert.assertEquals("index: " + index, "1", values[index++]);
        // Operator errors
        Assert.assertEquals("${ 0+0}", values[index++]);
        Assert.assertEquals("${0+0 }", values[index++]);
        Assert.assertEquals("${0 +0}", values[index++]);
        Assert.assertEquals("${0+ 0}", values[index++]);
        Assert.assertEquals("${0 + 0}", values[index++]);
        Assert.assertEquals("${str+0}", values[index++]);
        Assert.assertEquals("${0+str}", values[index++]);
        Assert.assertEquals("${unspecified+0}", values[index++]);
        Assert.assertEquals("${0+unspecified}", values[index++]);
        Assert.assertEquals("${0+9223372036854775808}", values[index++]);
        Assert.assertEquals("${0+maxLongPlusOne}", values[index++]);
        Assert.assertEquals("${0+minLongMinusOne}", values[index++]);
        // Subtract operator
        Assert.assertEquals("0", values[index++]);
        Assert.assertEquals("1024", values[index++]);
        Assert.assertEquals("1008", values[index++]);
        Assert.assertEquals("-1024", values[index++]);
        Assert.assertEquals("-1008", values[index++]);
        Assert.assertEquals("0", values[index++]);
        // Multiply operator
        Assert.assertEquals("0", values[index++]);
        Assert.assertEquals("0", values[index++]);
        Assert.assertEquals("16384", values[index++]);
        Assert.assertEquals("0", values[index++]);
        Assert.assertEquals("16384", values[index++]);
        Assert.assertEquals("1048576", values[index++]);
        // Divide operator
        Assert.assertEquals("${0/0}", values[index++]);
        Assert.assertEquals("${int/0}", values[index++]);
        Assert.assertEquals("64", values[index++]);
        Assert.assertEquals("0", values[index++]);
        Assert.assertEquals("0", values[index++]);
        Assert.assertEquals("1", values[index++]);
        // Misc
        Assert.assertEquals("2048", values[index++]);
        // Lists
        // List expression 1 -- ${list(array2)
        Assert.assertEquals("pid_one", values[index++]);
        Assert.assertEquals("pid_two", values[index++]);

        // List expression 2 -- ${list(multiValues)}
        Assert.assertEquals("a", values[index++]);
        Assert.assertEquals("b", values[index++]);
        Assert.assertEquals("c", values[index++]);

        // List expression 3 -- ${list(singleValue)}
        Assert.assertEquals("a", values[index++]);

        // Broken List expressions
        Assert.assertEquals("${list(IDoNotExist)}", values[index++]);
        Assert.assertEquals("${  list(array2)}", values[index++]);
        Assert.assertEquals("${list( array2 )}", values[index++]);
        Assert.assertEquals("${list(}", values[index++]);

        // List expression composed of variables -- ${list(varValue)}  (composed of ${multiValues}, ${singleValue}, ${intValues})
        Assert.assertEquals("a", values[index++]);
        Assert.assertEquals("b", values[index++]);
        Assert.assertEquals("c", values[index++]);
        Assert.assertEquals("a", values[index++]);
        Assert.assertEquals("1", values[index++]);
        Assert.assertEquals("2", values[index++]);
        Assert.assertEquals("3", values[index++]);
        Assert.assertEquals("4", values[index++]);
        Assert.assertEquals("5", values[index++]);

        // Integer List expression 1 -- ${list(intArray)}
        index = 0;
        int[] intValues = (int[]) dictionary.get("intValue");
        Assert.assertEquals(1, intValues[index++]);
        Assert.assertEquals(2, intValues[index++]);

        // Integer List expression 2 -- ${list(intValues)}
        Assert.assertEquals(1, intValues[index++]);
        Assert.assertEquals(2, intValues[index++]);
        Assert.assertEquals(3, intValues[index++]);
        Assert.assertEquals(4, intValues[index++]);
        Assert.assertEquals(5, intValues[index++]);

    }

    /**
     * Test that a missing required attribute will generate an error.
     */
    @Test
    public void testRequiredAttributeMissing() throws Exception {
        changeLocationSettings("default");

        String portPid = "com.ibm.ws.port";
        String hostPid = "com.ibm.ws.host";
        String hostPortPid = "com.ibm.ws.host.port";
        MetaTypeRegistry registry = createRegistry(portPid, hostPid, hostPortPid, 0);

        String xml = "<server>" +
                     "    <host id=\"one\"/>" +
                     "</server>";

        ServerConfiguration serverConfig = configParser.parseServerConfiguration(new StringReader(xml));

        ConfigEvaluator evaluator = createConfigEvaluator(registry, null);

        ConfigElement entry = null;

        RegistryEntry registryEntry = registry.getRegistryEntry(hostPid);
        entry = serverConfig.getFactoryInstance(hostPid, registryEntry.getAlias(), "one");
        EvaluationResult result = evaluator.evaluate(entry, registryEntry);
        assertFalse(result.isValid());

        assertTrue("We should get a message that a required attribute is missing", outputMgr.checkForMessages("CWWKG0058E"));

    }

    /**
     * Test that a missing required attribute will generate an error.
     */
    @Test
    public void testRequiredAttributeMissingSingleton() throws Exception {
        changeLocationSettings("default");

        String portPid = "com.ibm.ws.port";
        String hostPid = "com.ibm.ws.host";
        String hostPortPid = "com.ibm.ws.host.port";
        MetaTypeRegistry registry = createRegistry(portPid, hostPid, hostPortPid, "port", 0, hostPortPid, false);

        String xml = "<server>" +
                     "    <port/>" +
                     "</server>";

        ServerConfiguration serverConfig = configParser.parseServerConfiguration(new StringReader(xml));

        ConfigEvaluator evaluator = createConfigEvaluator(registry, null);

        ConfigElement entry = null;

        RegistryEntry registryEntry = registry.getRegistryEntry(portPid);
        entry = serverConfig.getSingleton(portPid, registryEntry.getAlias());
        EvaluationResult result = evaluator.evaluate(entry, registryEntry);
        assertFalse(result.isValid());

        assertTrue("We should get a message that a required attribute is missing", outputMgr.checkForMessages("CWWKG0095E"));

    }

    @Test
    public void testCommandLineVariable() throws Exception {
        changeLocationSettings("default");

        String loggingPid = "com.ibm.ws.logging";
        MockObjectClassDefinition loggingOCD = new MockObjectClassDefinition("logging");
        MockAttributeDefinition logDirectoryAttribute = new MockAttributeDefinition("logDirectory", AttributeDefinition.STRING, 6, null);
        logDirectoryAttribute.setVariable("list(com.ibm.ws.logging.log.directory)");
        loggingOCD.addAttributeDefinition(logDirectoryAttribute);
        loggingOCD.setAlias("logging");

        MockBundle bundle = new MockBundle();
        MockMetaTypeInformation metatype = new MockMetaTypeInformation(bundle);
        metatype.add(loggingPid, true, loggingOCD);

        MetaTypeRegistry registry = new MetaTypeRegistry();
        assertFalse("The registry should be updated", registry.addMetaType(metatype).isEmpty());

        RegistryEntry loggingRE = registry.getRegistryEntry(loggingPid);

        String xml = "<server>" +
                     "  <logging id=\"one\" logDirectory=\"${inBoth}, ${onlyCLV}, ${emptyInCLV}, ${onlyServerXML}, ${inNeither}, ${empty}\"/>\n" +
                     "</server>";
        ServerConfiguration serverConfig = configParser.parseServerConfiguration(new StringReader(xml));

        VariableRegistryHelper variableRegistryHelper = new VariableRegistryHelper(new SymbolRegistry());
        variableRegistryHelper.addVariable("inBoth", "server.xml dir");
        variableRegistryHelper.addVariable("onlyServerXML", "server.xml dir");
        variableRegistryHelper.addVariable("emptyInCLV", "server.xml dir");
        variableRegistryHelper.addVariable("empty", "server.xml dir");

        String[] commandLineVariables = new String[] { "--onlyCLV=foo", "--inBoth=fromCLV", "--zzz", "--emptyInCLV", "--=====", "--=bcd", "--empty=", "onlyCLV=invalidFromCLV",
                                                       "-inBoth=invalidFromBoth" };
        ConfigVariableRegistry variableRegistry = new ConfigVariableRegistry(variableRegistryHelper, commandLineVariables, null);
        TestConfigEvaluator evaluator = createConfigEvaluator(registry, variableRegistry, null);

        ConfigElement loggingElement = serverConfig.getSingleton(loggingPid, loggingRE.getAlias());
        EvaluationResult result = evaluator.evaluate(loggingElement, loggingRE);
        String[] logDirectories = (String[]) result.getProperties().get("logDirectory");
        assertEquals("The value in server.xml should be overridden by command line variables", "fromCLV", logDirectories[0]);
        assertEquals("The value in CLV should be used when no server.xml var present", "foo", logDirectories[1]);
        assertEquals("The value in server.xml should be used when CLV is invalid", "server.xml dir", logDirectories[2]);
        assertEquals("The value in server.xml should be used when no CLV present", "server.xml dir", logDirectories[3]);
        assertEquals("The value should be unresolved when neither exists", "${inNeither}", logDirectories[4]);
        assertEquals("The value should be empty when the CLV specifies an empty value", "", logDirectories[5]);
    }

    @Test
    public void testParentPid() throws ConfigEvaluatorException, ConfigParserException, ConfigValidationException, ConfigMergeException {
        changeLocationSettings("default");

        String portPid = "com.ibm.ws.port";
        String hostPid = "com.ibm.ws.host";
        String hostPortPid = "com.ibm.ws.host.port";
        MetaTypeRegistry registry = createRegistry(portPid, hostPid, hostPortPid, 0);

        String xml = "<server>" +
                     "    <host id=\"one\">" +
                     "      <port number=\"1\"/>" +
                     "    </host>" +
                     "    <host id=\"two\">" +
                     "      <port number=\"2\"/>" +
                     "    </host>" +
                     "</server>";

        ServerConfiguration serverConfig = configParser.parseServerConfiguration(new StringReader(xml));

        ConfigEvaluator evaluator = createConfigEvaluator(registry, null);

        ConfigElement entry = null;
        EvaluationResult result = null;
        EvaluationResult nestedResult = null;

        RegistryEntry registryEntry = registry.getRegistryEntry(hostPid);
        entry = serverConfig.getFactoryInstance(hostPid, registryEntry.getAlias(), "one");
        result = evaluator.evaluate(entry, registryEntry);
        assertNotNull(entry.getConfigID());

        ConfigID parentId = new ConfigID(hostPid, "one");
        ConfigID nestedSingleId = new ConfigID(parentId, hostPortPid, "default-0", "port");

        assertEquals(1, result.getNested().size());
        assertEquals(nestedSingleId, result.getNested().keySet().iterator().next());
        nestedResult = result.getNested().get(nestedSingleId);
        assertNotNull(nestedResult);

        entry = serverConfig.getFactoryInstance(hostPid, registryEntry.getAlias(), "two");
        result = evaluator.evaluate(entry, registryEntry);
        assertNotNull(entry.getConfigID());
        ConfigID parent2Id = new ConfigID(hostPid, "two");
        nestedSingleId = new ConfigID(parent2Id, hostPortPid, "default-0", "port");
        nestedResult = result.getNested().get(nestedSingleId);
        assertNotNull(nestedResult);

    }

    @Test
    public void testValidation() throws Exception {
        changeLocationSettings("default");

        String[] values = {
                            "simple text",
                            "  simple text   ",
                            "  simple    text   ",
                            "value a, value b",
                            "  value a  ,  value b  ",
                            "\\ value a\\ , value  b \\ ",
                            ",\\ value a\\ \\, value  b \\ ",
        };

        String pid = "com.ibm.ws.test";
        String alias = "test";
        MockObjectClassDefinition ocd = new MockObjectClassDefinition(alias);

        for (int i = 0; i < values.length; i++) {
            MockAttributeDefinition singleAD = new MockAttributeDefinition("single" + i, AttributeDefinition.STRING, 0, null);
            singleAD.setValidate(new String[] { MetaTypeHelper.escapeValue(values[i]) });
            ocd.addAttributeDefinition(singleAD);

            String[] validateMultiple = MetaTypeHelper.parseValue(values[i]).toArray(new String[0]);
            for (int j = 0; j < validateMultiple.length; j++) {
                validateMultiple[j] = MetaTypeHelper.escapeValue(validateMultiple[j]);
            }

            MockAttributeDefinition arrayAD = new MockAttributeDefinition("array" + i, AttributeDefinition.STRING, validateMultiple.length, null);
            arrayAD.setValidate(validateMultiple);
            ocd.addAttributeDefinition(arrayAD);

            MockAttributeDefinition vectorAD = new MockAttributeDefinition("vector" + i, AttributeDefinition.STRING, -validateMultiple.length, null);
            vectorAD.setValidate(validateMultiple);
            ocd.addAttributeDefinition(vectorAD);
        }

        MockBundle bundle = new MockBundle();
        MockMetaTypeInformation metatype = new MockMetaTypeInformation(bundle);
        metatype.add(pid, true, ocd);

        MetaTypeRegistry registry = new MetaTypeRegistry();
        assertFalse("The registry should be updated", registry.addMetaType(metatype).isEmpty());

        RegistryEntry re = registry.getRegistryEntry(pid);

        TestConfigEvaluator evaluator = createConfigEvaluator(registry, null);

        StringBuilder attrs = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            attrs.append(" single").append(i).append("='").append(values[i]).append("'");
            attrs.append(" array").append(i).append("='").append(values[i]).append("'");
            attrs.append(" vector").append(i).append("='").append(values[i]).append("'");
        }

        String xml = "<server>" +
                     "    <test id=\"one\" " + attrs + " />" +
                     "</server>";

        ServerConfiguration serverConfig = configParser.parseServerConfiguration(new StringReader(xml));
        ConfigElement entry = serverConfig.getFactoryInstance(pid, alias, "one");
        evaluator.evaluate(entry, re);
    }

    enum EvaluateMetaTypeStringMode {
        Attribute,
        Element,
        ElementWithIgnoredAttributes,
    }

    @Test
    public void testEvaluateMetaType() throws Exception {
        changeLocationSettings("default");

        class EvaluateMetaTypeTest {
            final int type;
            final Object array;

            EvaluateMetaTypeTest(int type, Object array) {
                this.type = type;
                this.array = array;
            }
        }

        EvaluateMetaTypeTest[] tests;
        // TODO: PASSWORD_TYPE, HASHED_PASSWORD_TYPE, ON_ERROR_TYPE - cannot have multiple cardinality
        tests = new EvaluateMetaTypeTest[] {
                                             new EvaluateMetaTypeTest(ExtendedAttributeDefinition.BOOLEAN, new boolean[] { false, true }),
                                             new EvaluateMetaTypeTest(ExtendedAttributeDefinition.BYTE, new byte[] { 1, 2 }),
                                             new EvaluateMetaTypeTest(ExtendedAttributeDefinition.CHARACTER, new char[] { 'a', 'b' }),
                                             new EvaluateMetaTypeTest(ExtendedAttributeDefinition.DOUBLE, new double[] { 1, 2 }),
                                             new EvaluateMetaTypeTest(ExtendedAttributeDefinition.FLOAT, new float[] { 1, 2 }),
                                             new EvaluateMetaTypeTest(ExtendedAttributeDefinition.INTEGER, new int[] { 1, 2 }),
                                             new EvaluateMetaTypeTest(ExtendedAttributeDefinition.LONG, new long[] { 1, 2 }),
                                             new EvaluateMetaTypeTest(ExtendedAttributeDefinition.SHORT, new short[] { 1, 2 }),
                                             new EvaluateMetaTypeTest(MetaTypeFactory.DURATION_TYPE, new long[] { 1, 2 }),
                                             new EvaluateMetaTypeTest(MetaTypeFactory.DURATION_S_TYPE, new long[] { 1, 2 }),
                                             new EvaluateMetaTypeTest(MetaTypeFactory.DURATION_M_TYPE, new long[] { 1, 2 }),
                                             new EvaluateMetaTypeTest(MetaTypeFactory.DURATION_H_TYPE, new long[] { 1, 2 }),
                                             new EvaluateMetaTypeTest(MetaTypeFactory.TOKEN_TYPE, new String[] { "a", "b" }),
                                             new EvaluateMetaTypeTest(ExtendedAttributeDefinition.STRING, new String[] { "a", "b" }),
        };

        for (EvaluateMetaTypeTest test : tests) {
            String pid = "com.ibm.ws.test" + test.type;
            String alias = "test" + test.type;
            MockObjectClassDefinition ocd = new MockObjectClassDefinition(alias);

            MockAttributeDefinition singleAD = new MockAttributeDefinition("single", test.type, 0, null);
            ocd.addAttributeDefinition(singleAD, false);

            MockAttributeDefinition arrayAD = new MockAttributeDefinition("array", test.type, 2, null);
            ocd.addAttributeDefinition(arrayAD, false);

            MockAttributeDefinition vectorAD = new MockAttributeDefinition("vector", test.type, -2, null);
            ocd.addAttributeDefinition(vectorAD, false);

            MockBundle bundle = new MockBundle();
            MockMetaTypeInformation metatype = new MockMetaTypeInformation(bundle);
            metatype.add(pid, true, ocd);

            MetaTypeRegistry registry = new MetaTypeRegistry();
            assertFalse("The registry should be updated", registry.addMetaType(metatype).isEmpty());

            RegistryEntry re = registry.getRegistryEntry(pid);

            VariableRegistryHelper variableRegistryHelper = new VariableRegistryHelper(new SymbolRegistry());
            ConfigVariableRegistry variableRegistry = new ConfigVariableRegistry(variableRegistryHelper, new String[0], null);
            TestConfigEvaluator evaluator = createConfigEvaluator(registry, variableRegistry, null);

            for (boolean variables : new boolean[] { false, true }) {
                String[] multipleStrings = new String[Array.getLength(test.array)];
                for (int i = 0; i < multipleStrings.length; i++) {
                    multipleStrings[i] = String.valueOf(Array.get(test.array, i));
                }
                String singleString = multipleStrings[0];
                String xml;

                if (variables) {
                    Assert.assertTrue(variableRegistryHelper.addVariable("singleVar", singleString));
                    singleString = "${singleVar}";

                    for (int i = 0; i < multipleStrings.length; i++) {
                        Assert.assertTrue(variableRegistryHelper.addVariable("multipleVar" + i, multipleStrings[i]));
                        multipleStrings[i] = "${multipleVar" + i + "}";
                    }
                }

                String pfx = "type " + test.type + " variables " + variables;

                // Test array/vector with varying number of default values
                for (int defaultSize = 0; defaultSize < multipleStrings.length; defaultSize++) {
                    String pfx2 = pfx + " defaultSize " + defaultSize;

                    arrayAD.setDefaultValue(Arrays.copyOf(multipleStrings, defaultSize));
                    vectorAD.setDefaultValue(Arrays.copyOf(multipleStrings, defaultSize));

                    xml = "<server>" +
                          "    <" + alias + " id='one'/>" +
                          "</server>";

                    ServerConfiguration serverConfig = configParser.parseServerConfiguration(new StringReader(xml));
                    ConfigElement entry = serverConfig.getFactoryInstance(pid, alias, "one");
                    EvaluationResult result = evaluator.evaluate(entry, re);
                    Dictionary<String, Object> props = result.getProperties();

                    Object actualArray = props.get("array");
                    Assert.assertNotNull(pfx2 + " array");
                    Assert.assertEquals(pfx2 + " array", test.array.getClass(), actualArray.getClass());
                    Assert.assertEquals(pfx2 + " array", defaultSize, Array.getLength(actualArray));
                    for (int i = 0; i < defaultSize; i++) {
                        Assert.assertEquals(pfx2 + " array[" + i + "]", Array.get(test.array, i), Array.get(actualArray, i));
                    }

                    Vector<Object> expectedVector = new Vector<Object>();
                    for (int i = 0; i < defaultSize; i++) {
                        expectedVector.add(Array.get(test.array, i));
                    }
                    Assert.assertEquals(pfx2 + " vector", expectedVector, props.get("vector"));
                }

                // Reset default values.
                arrayAD.setDefaultValue(null);
                vectorAD.setDefaultValue(null);

                for (EvaluateMetaTypeStringMode mode : EvaluateMetaTypeStringMode.values()) {
                    String pfx2 = pfx + " mode " + mode;

                    if (mode == EvaluateMetaTypeStringMode.Attribute) {
                        // Test values as XML attributes.
                        StringBuilder multipleAttrBuilder = new StringBuilder();
                        for (String multipleString : multipleStrings) {
                            multipleAttrBuilder.append(',').append(multipleString);
                        }
                        String multipleAttrString = multipleAttrBuilder.substring(1);

                        xml = "<server>" +
                              "    <" + alias + " id='one' single='" + singleString + "' array='" + multipleAttrString + "' vector='" + multipleAttrString + "'/>" +
                              "</server>";
                    } else {
                        // Test values as XML string elements (optionally with ignored attributes).
                        StringBuilder xmlb = new StringBuilder();
                        String extraAttr = mode == EvaluateMetaTypeStringMode.ElementWithIgnoredAttributes ? " attr='ignored'" : "";
                        xmlb.append("<server>");
                        xmlb.append("    <").append(alias).append(" id='one'>");
                        xmlb.append("        <single").append(extraAttr).append(">").append(singleString).append("</single>");
                        for (String multipleString : multipleStrings) {
                            xmlb.append("        <array").append(extraAttr).append(">").append(multipleString).append("</array>");
                            xmlb.append("        <vector").append(extraAttr).append(">").append(multipleString).append("</vector>");
                        }
                        xmlb.append("    </").append(alias).append(">");
                        xmlb.append("</server>");
                        xml = xmlb.toString();
                    }

                    ServerConfiguration serverConfig = configParser.parseServerConfiguration(new StringReader(xml));
                    ConfigElement entry = serverConfig.getFactoryInstance(pid, alias, "one");
                    EvaluationResult result = evaluator.evaluate(entry, re);
                    Dictionary<String, Object> props = result.getProperties();

                    Assert.assertEquals(pfx2 + " single", Array.get(test.array, 0), props.get("single"));

                    Object actualArray = props.get("array");
                    Assert.assertNotNull(pfx2 + " array");
                    Assert.assertEquals(pfx2 + " array", test.array.getClass(), actualArray.getClass());
                    Assert.assertEquals(pfx2 + " array", Array.getLength(test.array), Array.getLength(actualArray));
                    for (int i = 0, length = Array.getLength(test.array); i < length; i++) {
                        Assert.assertEquals(pfx2 + " array[" + i + "]", Array.get(test.array, i), Array.get(actualArray, i));
                    }

                    Vector<Object> expectedVector = new Vector<Object>();
                    for (int i = 0, length = Array.getLength(test.array); i < length; i++) {
                        expectedVector.add(Array.get(test.array, i));
                    }
                    Assert.assertEquals(pfx2 + " vector", expectedVector, props.get("vector"));
                }

                // Test a single value as an XML string element.
                {
                    xml = "<server>" +
                          "    <" + alias + " id='one'>" +
                          "        <single>" + singleString + "</single>" +
                          "    </" + alias + ">" +
                          "</server>";

                    ServerConfiguration serverConfig = configParser.parseServerConfiguration(new StringReader(xml));
                    ConfigElement entry = serverConfig.getFactoryInstance(pid, alias, "one");
                    EvaluationResult result = evaluator.evaluate(entry, re);
                    Dictionary<String, Object> props = result.getProperties();
                    Assert.assertEquals(pfx + " single nested", Array.get(test.array, 0), props.get("single"));
                }

                // Test single value with multiple XML string elements.
                // (This is questionable and should probably give a warning.)
                {
                    xml = "<server>" +
                          "    <" + alias + " id='one'>" +
                          "        <single>" + singleString + "</single>" +
                          "        <single>ignored</single>" +
                          "    </" + alias + ">" +
                          "</server>";

                    ServerConfiguration serverConfig = configParser.parseServerConfiguration(new StringReader(xml));
                    ConfigElement entry = serverConfig.getFactoryInstance(pid, alias, "one");
                    EvaluationResult result = evaluator.evaluate(entry, re);
                    Dictionary<String, Object> props = result.getProperties();
                    Assert.assertEquals(pfx + " single nested multiple", Array.get(test.array, 0), props.get("single"));
                }

                // Test validation errors for XML attributes.
                singleAD.setValidate(new String[] { "expected" });
                arrayAD.setValidate(new String[] { "expected" });
                vectorAD.setValidate(new String[] { "expected" });
                for (String attr : new String[] { "single", "array", "vector" }) {
                    xml = "<server>" +
                          "    <" + alias + " id='one' " + attr + "='unexpected'/>" +
                          "</server>";
                    ServerConfiguration serverConfig = configParser.parseServerConfiguration(new StringReader(xml));
                    ConfigElement entry = serverConfig.getFactoryInstance(pid, alias, "one");
                    try {
                        evaluator.evaluate(entry, re);
                        fail(pfx + " " + attr + " invalid");
                    } catch (ConfigEvaluatorException e) {
                        System.out.println(pfx + " " + attr + " invalid attribute value caught expected " + e);
                    }
                }

                // Test validation errors for XML string elements.
                singleAD.setValidate(new String[] { "expected" });
                arrayAD.setValidate(new String[] { "expected" });
                vectorAD.setValidate(new String[] { "expected" });
                for (String attr : new String[] { "single", "array", "vector" }) {
                    xml = "<server>" +
                          "    <" + alias + " id='one'>" +
                          "        <" + attr + ">unexpected</" + attr + ">" +
                          "    </" + alias + ">" +
                          "</server>";
                    ServerConfiguration serverConfig = configParser.parseServerConfiguration(new StringReader(xml));
                    ConfigElement entry = serverConfig.getFactoryInstance(pid, alias, "one");
                    try {
                        evaluator.evaluate(entry, re);
                        fail(pfx + " " + attr + " invalid");
                    } catch (ConfigEvaluatorException e) {
                        System.out.println(pfx + " " + attr + " invalid element value caught expected " + e);
                    }
                }

                // Reset validation.
                singleAD.setValidate(null);
                arrayAD.setValidate(null);
                vectorAD.setValidate(null);

                // Test cardinality violation for XML attribute.
                for (String attr : new String[] { "array", "vector" }) {
                    xml = "<server>" +
                          "    <" + alias + " id='one' " + attr + "='" + singleString + "," + singleString + "," + singleString + "'/>" +
                          "</server>";
                    ServerConfiguration serverConfig = configParser.parseServerConfiguration(new StringReader(xml));
                    ConfigElement entry = serverConfig.getFactoryInstance(pid, alias, "one");
                    try {
                        evaluator.evaluate(entry, re);
                        fail(pfx + " " + attr + " invalid");
                    } catch (ConfigEvaluatorException e) {
                        System.out.println("type " + test.type + " " + attr + " invalid attribute cardinality caught expected " + e);
                    }
                }

                // Test cardinality violation for XML string elements.
                for (String attr : new String[] { "array", "vector" }) {
                    xml = "<server>" +
                          "    <" + alias + " id='one'>" +
                          "        <" + attr + ">" + singleString + "</" + attr + ">" +
                          "        <" + attr + ">" + singleString + "</" + attr + ">" +
                          "        <" + attr + ">" + singleString + "</" + attr + ">" +
                          "    </" + alias + ">" +
                          "</server>";
                    ServerConfiguration serverConfig = configParser.parseServerConfiguration(new StringReader(xml));
                    ConfigElement entry = serverConfig.getFactoryInstance(pid, alias, "one");
                    try {
                        evaluator.evaluate(entry, re);
                        fail(pfx + " " + attr + " invalid");
                    } catch (ConfigEvaluatorException e) {
                        System.out.println(pfx + " " + attr + " invalid element cardinality caught expected " + e);
                    }
                }
            }
        }
    }

    @Test
    public void testEvaluateSimple() throws Exception {
        changeLocationSettings("default");

        String pid = "com.ibm.ws.test";
        String alias = "test";

        MetaTypeRegistry registry = new MetaTypeRegistry();
        TestConfigEvaluator evaluator = createConfigEvaluator(registry, null);
        Map<ConfigID, String> pids = new HashMap<ConfigID, String>();
        pids.put(new ConfigID(new ConfigID(pid, "one"), "emptyElem", "default-0", "emptyElem"), "emptyElem-pid");
        pids.put(new ConfigID(new ConfigID(pid, "one"), "emptyElem", "emptyElem-id", "emptyElem"), "emptyElem-id-pid");
        pids.put(new ConfigID(new ConfigID(pid, "one"), "childElem", "default-0", "childElem"), "childElem-pid");
        pids.put(new ConfigID(new ConfigID(pid, "one"), "childElem", "childElem-id", "childElem"), "childElem-id-pid");
        pids.put(new ConfigID(new ConfigID(pid, "one"), "nestedElem", "default-0", "nestedElem"), "nestedElem-pid");
        pids.put(new ConfigID(new ConfigID(pid, "one"), "nestedElem", "nestedElem-id", "nestedElem"), "nestedElem-id-pid");
        pids.put(new ConfigID("refElem", "refElem-id"), "refElem-pid");
        evaluator.setLookupMap(pids);

        String xml = "<server>" +
                     "    <test id='one' singleAttr='abc' multipleAttr='a,b,c' untypedInt='1' untypedRef='id'>" +
                     "        <elem>a</elem>" +
                     "        <emptyElem/>" +
                     "        <emptyElem id='emptyElem-id'/>" +
                     "        <childElem a='1'/>" +
                     "        <childElem id='childElem-id' a='1'/>" +
                     "        <nestedElem><sub/></nestedElem>" +
                     "        <nestedElem id='nestedElem-id'><sub/></nestedElem>" +
                     "        <refElem ref='refElem-id'/>" +
                     "        <refElemMissing ref='refElemMissing-id'/>" +
                     "    </test>" +
                     "</server>";

        ServerConfiguration serverConfig = configParser.parseServerConfiguration(new StringReader(xml));
        ConfigElement entry = serverConfig.getFactoryInstance(pid, alias, "one");
        EvaluationResult result = evaluator.evaluate(entry, null);
        Dictionary<String, Object> props = result.getProperties();

        Assert.assertEquals("singleAttr", "abc", props.get("singleAttr"));
        Assert.assertEquals("multipleAttr", "a,b,c", props.get("multipleAttr"));
        Assert.assertEquals("untypedInt", "1", props.get("untypedInt"));
        Assert.assertEquals("untypedRef", "id", props.get("untypedRef"));
        Assert.assertEquals("elem",
                            Arrays.asList(new String[] { "a" }),
                            Arrays.asList((String[]) props.get("elem")));
        Assert.assertEquals("emptyElem",
                            Arrays.asList(new String[] { "emptyElem-pid", "emptyElem-id-pid" }),
                            Arrays.asList((String[]) props.get("emptyElem")));
        Assert.assertEquals("childElem",
                            Arrays.asList(new String[] { "childElem-pid", "childElem-id-pid" }),
                            Arrays.asList((String[]) props.get("childElem")));
        Assert.assertEquals("nestedElem",
                            Arrays.asList(new String[] { "nestedElem-pid", "nestedElem-id-pid" }),
                            Arrays.asList((String[]) props.get("nestedElem")));
        Assert.assertEquals("refElem",
                            Arrays.asList(new String[] { "refElem-pid" }),
                            Arrays.asList((String[]) props.get("refElem")));
        Assert.assertEquals("refElemMissing",
                            Arrays.asList(new String[0]),
                            Arrays.asList((String[]) props.get("refElemMissing")));
    }

    @Test
    public void testCaseInsensitivity() throws Exception {
        changeLocationSettings("default");

        String hostPid = "com.ibm.ws.host";
        String hostAlias = "host";
        MockObjectClassDefinition hostOCD = new MockObjectClassDefinition("host");
        MockAttributeDefinition nameAD = new MockAttributeDefinition("name", AttributeDefinition.STRING, 0, new String[] { "localhost" });
        hostOCD.addAttributeDefinition(nameAD);
        hostOCD.setAlias(hostAlias);

        MockBundle bundle = new MockBundle();
        MockMetaTypeInformation metatype = new MockMetaTypeInformation(bundle);
        metatype.add(hostPid, true, hostOCD);

        MetaTypeRegistry registry = new MetaTypeRegistry();
        assertFalse("The registry should be updated", registry.addMetaType(metatype).isEmpty());

        RegistryEntry hostRE = registry.getRegistryEntry(hostPid);

        TestConfigEvaluator evaluator = createConfigEvaluator(registry, null);

        String xml = "<server>" +
                     "    <host id=\"one\" Name=\"one\" />" +
                     "</server>";

        ServerConfiguration serverConfig = configParser.parseServerConfiguration(new StringReader(xml));

        ConfigElement entry = serverConfig.getFactoryInstance(hostPid, hostAlias, "one");

        EvaluationResult result = evaluator.evaluate(entry, hostRE);
        Dictionary<String, Object> dictionary = result.getProperties();

        // should only be one instance of name attribute
        Object name = dictionary.remove("name");
        assertNotNull("name not found", name);
        Object Name = dictionary.remove("Name");
        assertNull("Name found", Name);
    }

    // Multiple port elements with cardinality zero should no longer cause an exception. The elements are merged.
    @Test
    public void testReferenceValidationCardinalityZero() throws ConfigParserException, ConfigValidationException, ConfigEvaluatorException, ConfigMergeException {
        changeLocationSettings("default");

        String portPid = "com.ibm.ws.port";
        String hostPid = "com.ibm.ws.host";
        String hostPortPid = "com.ibm.ws.host.port";
        String portAttribute = "portRef";
        MetaTypeRegistry registry = createRegistry(portPid, hostPid, hostPortPid, portAttribute, 0, hostPortPid, true);

        String xml = "<server>" +
                     "    <host id=\"one\">" +
                     "      <port id=\"1\" number=\"1\"/>" +
                     "      <port id=\"2\" number=\"2\"/>" +
                     "    </host>" +
                     "</server>";

        ServerConfiguration serverConfig = configParser.parseServerConfiguration(new StringReader(xml));

        ConfigEvaluator evaluator = createConfigEvaluator(registry, null);

        ConfigElement entry = null;

        RegistryEntry registryEntry = registry.getRegistryEntry(hostPid);
        entry = serverConfig.getFactoryInstance(hostPid, registryEntry.getAlias(), "one");
        evaluator.evaluate(entry, registryEntry);
    }

    // Port is a singleton, so the elements should be merged rather than causing a cardinality violation
    @Test
    public void testReferenceValidationCardinalityZeroSingleton() throws ConfigParserException, ConfigValidationException, ConfigEvaluatorException, ConfigMergeException {
        changeLocationSettings("default");

        String portPid = "com.ibm.ws.port";
        String hostPid = "com.ibm.ws.host";
        String hostPortPid = "com.ibm.ws.host.port";
        String portAttribute = "portRef";
        MetaTypeRegistry registry = createRegistry(portPid, hostPid, hostPortPid, portAttribute, 0, portPid, false);

        String xml = "<server>" +
                     "    <host id=\"one\">" +
                     "      <port id=\"1\" number=\"1\"/>" +
                     "      <port id=\"2\" number=\"2\"/>" +
                     "    </host>" +
                     "</server>";

        ServerConfiguration serverConfig = configParser.parseServerConfiguration(new StringReader(xml));

        ConfigEvaluator evaluator = createConfigEvaluator(registry, null);

        ConfigElement entry = null;

        RegistryEntry registryEntry = registry.getRegistryEntry(hostPid);
        entry = serverConfig.getFactoryInstance(hostPid, registryEntry.getAlias(), "one");
        evaluator.evaluate(entry, registryEntry);
    }

    // Should throw an exception because the number of child elements (4) exceeds the cardinality (2)
    @Test(expected = ConfigEvaluatorException.class)
    public void testReferenceValidationCardinalityTwo() throws ConfigParserException, ConfigValidationException, ConfigEvaluatorException, ConfigMergeException {
        changeLocationSettings("default");

        String portPid = "com.ibm.ws.port";
        String hostPid = "com.ibm.ws.host";
        String hostPortPid = "com.ibm.ws.host.port";
        String portAttribute = "portRef";
        MetaTypeRegistry registry = createRegistry(portPid, hostPid, hostPortPid, portAttribute, 2, portPid, true);

        String xml = "<server>" +
                     "    <host id=\"one\">" +
                     "      <port number=\"1\"/>" +
                     "      <port number=\"2\"/>" +
                     "      <port number=\"3\"/>" +
                     "      <port number=\"4\"/>" +
                     "    </host>" +
                     "</server>";

        ServerConfiguration serverConfig = configParser.parseServerConfiguration(new StringReader(xml));

        ConfigEvaluator evaluator = createConfigEvaluator(registry, null);

        ConfigElement entry = null;

        RegistryEntry registryEntry = registry.getRegistryEntry(hostPid);
        entry = serverConfig.getFactoryInstance(hostPid, registryEntry.getAlias(), "one");
        evaluator.evaluate(entry, registryEntry);
    }

    // Should throw an exception because the value of a boolean field is not "true" or "false"
    @Test(expected = ConfigEvaluatorException.class)
    public void testInvalidBoolean() throws ConfigParserException, ConfigValidationException, ConfigEvaluatorException, ConfigMergeException {
        changeLocationSettings("default");

        String portPid = "com.ibm.ws.port";
        String hostPid = "com.ibm.ws.host";
        String hostPortPid = "com.ibm.ws.host.port";
        String portAttribute = "portRef";
        String loggingPid = "com.ibm.ws.logging";
        MetaTypeRegistry registry = createRegistry(portPid, hostPid, hostPortPid, portAttribute, 2, portPid, true);
        addBooleanTypeToRegistry(registry, loggingPid);

        String xml = "<server>" +
                     "    <logging id=\"one\" boolean=\"8\"/>" +
                     "</server>";

        ServerConfiguration serverConfig = configParser.parseServerConfiguration(new StringReader(xml));

        ConfigEvaluator evaluator = createConfigEvaluator(registry, null);

        ConfigElement entry = null;

        RegistryEntry registryEntry = registry.getRegistryEntry(loggingPid);
        entry = serverConfig.getFactoryInstance(loggingPid, registryEntry.getAlias(), "one");
        evaluator.evaluate(entry, registryEntry);
    }

    @Test
    public void testObscuredBoolean() throws ConfigParserException, ConfigValidationException, ConfigEvaluatorException, ConfigMergeException {
        changeLocationSettings("default");

        String portPid = "com.ibm.ws.port";
        String hostPid = "com.ibm.ws.host";
        String hostPortPid = "com.ibm.ws.host.port";
        String portAttribute = "portRef";
        String loggingPid = "com.ibm.ws.logging";
        MetaTypeRegistry registry = createRegistry(portPid, hostPid, hostPortPid, portAttribute, 2, portPid, true);
        addObscuredBooleanTypeToRegistry(registry, loggingPid);

        String xml = "<server>" +
                     "    <logging id=\"one\" boolean=\"true\"/>" +
                     "</server>";

        ServerConfiguration serverConfig = configParser.parseServerConfiguration(new StringReader(xml));

        ConfigEvaluator evaluator = createConfigEvaluator(registry, null);

        ConfigElement entry = null;

        RegistryEntry registryEntry = registry.getRegistryEntry(loggingPid);
        entry = serverConfig.getFactoryInstance(loggingPid, registryEntry.getAlias(), "one");
        evaluator.evaluate(entry, registryEntry);
        assertEquals("true", entry.getAttribute("boolean").toString());
    }

    // Throws an exception because there is no default value
    @Test(expected = ConfigEvaluatorException.class)
    public void testInvalidInteger() throws ConfigParserException, ConfigValidationException, ConfigEvaluatorException, ConfigMergeException {
        changeLocationSettings("default");

        String portPid = "com.ibm.ws.port";
        String hostPid = "com.ibm.ws.host";
        String hostPortPid = "com.ibm.ws.host.port";
        String portAttribute = "portRef";
        String loggingPid = "com.ibm.ws.logging";
        MetaTypeRegistry registry = createRegistry(portPid, hostPid, hostPortPid, portAttribute, 2, portPid, true);
        addIntegerTypeToRegistry(registry, loggingPid);

        String xml = "<server>" +
                     "    <logging id=\"one\" integer=\"eight\"/>" +
                     "</server>";

        ServerConfiguration serverConfig = configParser.parseServerConfiguration(new StringReader(xml));

        ConfigEvaluator evaluator = createConfigEvaluator(registry, null);

        ConfigElement entry = null;

        RegistryEntry registryEntry = registry.getRegistryEntry(loggingPid);
        entry = serverConfig.getFactoryInstance(loggingPid, registryEntry.getAlias(), "one");

        evaluator.evaluate(entry, registryEntry);

    }

    @Test
    public void testInvalidIntegerDefaultValue() throws ConfigParserException, ConfigValidationException, ConfigEvaluatorException, ConfigMergeException {
        changeLocationSettings("default");

        String portPid = "com.ibm.ws.port";
        String hostPid = "com.ibm.ws.host";
        String hostPortPid = "com.ibm.ws.host.port";
        String portAttribute = "portRef";
        String loggingPid = "com.ibm.ws.logging";
        MetaTypeRegistry registry = createRegistry(portPid, hostPid, hostPortPid, portAttribute, 2, portPid, true);
        MockAttributeDefinition ad = addIntegerTypeToRegistry(registry, loggingPid);
        ad.setDefaultValue(new String[] { "8" });

        String xml = "<server>" +
                     "    <logging id=\"one\" integer=\"eight\"/>" +
                     "</server>";

        ServerConfiguration serverConfig = configParser.parseServerConfiguration(new StringReader(xml));

        ConfigEvaluator evaluator = createConfigEvaluator(registry, null);

        ConfigElement entry = null;

        RegistryEntry registryEntry = registry.getRegistryEntry(loggingPid);
        entry = serverConfig.getFactoryInstance(loggingPid, registryEntry.getAlias(), "one");
        try {
            evaluator.evaluate(entry, registryEntry);
        } finally {
            assertTrue("We should get a message that the default is being used",
                       outputMgr.checkForMessages("CWWKG0083W.*eight.*8"));
        }

    }

    // Port is a singleton here so child elements should be merged. There should not be a cardinality violation
    @Test
    public void testReferenceValidationCardinalityTwoSingleton() throws ConfigParserException, ConfigValidationException, ConfigEvaluatorException, ConfigMergeException {
        changeLocationSettings("default");

        String portPid = "com.ibm.ws.port";
        String hostPid = "com.ibm.ws.host";
        String hostPortPid = "com.ibm.ws.host.port";
        String portAttribute = "portRef";
        MetaTypeRegistry registry = createRegistry(portPid, hostPid, hostPortPid, portAttribute, 2, portPid, false);

        String xml = "<server>" +
                     "    <host id=\"one\">" +
                     "      <port number=\"1\"/>" +
                     "      <port number=\"2\"/>" +
                     "      <port number=\"3\"/>" +
                     "      <port number=\"4\"/>" +
                     "    </host>" +
                     "</server>";

        ServerConfiguration serverConfig = configParser.parseServerConfiguration(new StringReader(xml));

        ConfigEvaluator evaluator = createConfigEvaluator(registry, null);

        ConfigElement entry = null;

        RegistryEntry registryEntry = registry.getRegistryEntry(hostPid);
        entry = serverConfig.getFactoryInstance(hostPid, registryEntry.getAlias(), "one");
        evaluator.evaluate(entry, registryEntry);
    }

    // The port elements have the same id, so they can be merged. There should not be a cardinality violation
    @Test
    public void testReferenceValidationCardinalityTwoMergeable() throws ConfigParserException, ConfigValidationException, ConfigEvaluatorException, ConfigMergeException {
        changeLocationSettings("default");

        String portPid = "com.ibm.ws.port";
        String hostPid = "com.ibm.ws.host";
        String hostPortPid = "com.ibm.ws.host.port";
        String portAttribute = "portRef";
        MetaTypeRegistry registry = createRegistry(portPid, hostPid, hostPortPid, portAttribute, 2, portPid, true);

        String xml = "<server>" +
                     "    <host id=\"one\">" +
                     "      <port id=\"1\" number=\"7\" foo=\"bar\"/>" +
                     "      <port id=\"1\" number=\"7\"/>" +
                     "      <port id=\"1\" number=\"7\" color=\"orange\"/>" +
                     "      <port id=\"1\" number=\"7\" shape=\"square\"/>" +
                     "    </host>" +
                     "</server>";

        ServerConfiguration serverConfig = configParser.parseServerConfiguration(new StringReader(xml));

        ConfigEvaluator evaluator = createConfigEvaluator(registry, null);

        ConfigElement entry = null;

        RegistryEntry registryEntry = registry.getRegistryEntry(hostPid);
        entry = serverConfig.getFactoryInstance(hostPid, registryEntry.getAlias(), "one");
        EvaluationResult result = evaluator.evaluate(entry, registryEntry);
        // Check merged values

        ConfigID parentId = new ConfigID(hostPid, "one");
        ConfigID nestedId = new ConfigID(parentId, portPid, "1", "port");

        assertEquals(1, result.getNested().size());
        EvaluationResult nestedResult = result.getNested().get(nestedId);
        assertNotNull(nestedResult);
        assertEquals("7", nestedResult.getProperties().get("number"));

        assertEquals("bar", nestedResult.getProperties().get("foo"));
        assertEquals("orange", nestedResult.getProperties().get("color"));
        assertEquals("square", nestedResult.getProperties().get("shape"));
    }

    // Should throw an exception because the number of child elements (4) exceeds the cardinality (2)
    @Test(expected = ConfigEvaluatorException.class)
    public void testReferenceValidationCardinalityNegativeTwo() throws ConfigParserException, ConfigValidationException, ConfigEvaluatorException, ConfigMergeException {
        changeLocationSettings("default");

        String portPid = "com.ibm.ws.port";
        String hostPid = "com.ibm.ws.host";
        String hostPortPid = "com.ibm.ws.host.port";
        String portAttribute = "portRef";
        MetaTypeRegistry registry = createRegistry(portPid, hostPid, hostPortPid, portAttribute, 2, hostPortPid, true);

        String xml = "<server>" +
                     "    <host id=\"one\">" +
                     "      <port number=\"1\"/>" +
                     "      <port number=\"2\"/>" +
                     "      <port number=\"3\"/>" +
                     "      <port number=\"4\"/>" +
                     "    </host>" +
                     "</server>";

        ServerConfiguration serverConfig = configParser.parseServerConfiguration(new StringReader(xml));

        ConfigEvaluator evaluator = createConfigEvaluator(registry, null);

        ConfigElement entry = null;

        RegistryEntry registryEntry = registry.getRegistryEntry(hostPid);
        entry = serverConfig.getFactoryInstance(hostPid, registryEntry.getAlias(), "one");
        evaluator.evaluate(entry, registryEntry);
    }

    @Test
    public void testSimpleCardinality() throws Exception {
        changeLocationSettings("default");

        String portPid = "com.ibm.ws.port";
        String hostPid = "com.ibm.ws.host";
        String hostPortPid = "com.ibm.ws.host.port";
        String portAttribute = "portRef";
        MetaTypeRegistry registry = createRegistry(portPid, hostPid, hostPortPid, portAttribute, 2, hostPortPid, true);

        String xml = "<server>" +
                     "    <host id=\"one\" ip=\"0.0.0.0, 1.1.1.1\">" +
                     "      <port number=\"1\"/>" +
                     "    </host>" +
                     "</server>";

        ServerConfiguration serverConfig = configParser.parseServerConfiguration(new StringReader(xml));

        ConfigEvaluator evaluator = createConfigEvaluator(registry, null);

        ConfigElement entry = null;

        RegistryEntry registryEntry = registry.getRegistryEntry(hostPid);
        entry = serverConfig.getFactoryInstance(hostPid, registryEntry.getAlias(), "one");
        EvaluationResult result = evaluator.evaluate(entry, registryEntry);
        assertEquals("0.0.0.0, 1.1.1.1", result.getProperties().get("ip"));
    }

    // Multiple values for metatype attribute IP under host with cardinality 0, should not throw an exception
    @Test
    public void testSimpleCardinalityViolation() throws ConfigParserException, ConfigValidationException, ConfigEvaluatorException, ConfigMergeException {
        changeLocationSettings("default");

        String portPid = "com.ibm.ws.port";
        String hostPid = "com.ibm.ws.host";
        String hostPortPid = "com.ibm.ws.host.port";
        String portAttribute = "portRef";
        MetaTypeRegistry registry = createRegistry(portPid, hostPid, hostPortPid, portAttribute, 2, hostPortPid, true);

        String xml = "<server>" +
                     "    <host id=\"one\">" +
                     "      <ip>0.0.0.0</ip>" +
                     "      <ip>1.1.1.1</ip>" +
                     "      <port number=\"1\"/>" +
                     "    </host>" +
                     "</server>";

        ServerConfiguration serverConfig = configParser.parseServerConfiguration(new StringReader(xml));

        ConfigEvaluator evaluator = createConfigEvaluator(registry, null);

        ConfigElement entry = null;

        RegistryEntry registryEntry = registry.getRegistryEntry(hostPid);
        entry = serverConfig.getFactoryInstance(hostPid, registryEntry.getAlias(), "one");
        evaluator.evaluate(entry, registryEntry);
    }

    // The reference lookup will fail because "host1z" is not in the config. However, we will not issue a warning
    // because it appears that we have not processed "com.ibm.ws.host.port" yet.
    @Test
    public void testReferenceLookupFailure1() throws Exception {
        changeLocationSettings("default");

        String portPid = "com.ibm.ws.port";
        String hostPid = "com.ibm.ws.host";
        String hostPortPid = "com.ibm.ws.host.port";
        MetaTypeRegistry registry = createRegistry(portPid, hostPid, hostPortPid, "port", 0, hostPortPid, true);

        TestConfigEvaluator evaluator = createConfigEvaluator(registry, null);
        evaluator.setLookupMap(new HashMap<ConfigID, String>());

        String xml;
        ServerConfiguration serverConfig;
        ConfigElement entry = null;
        EvaluationResult result = null;
        Dictionary<String, Object> dictionary = null;

        RegistryEntry registryEntry = registry.getRegistryEntry(hostPid);
        String hostAlias = registryEntry.getAlias();

        xml = "<server>" +
              "    <com.ibm.ws.host.port id=\"host1\" number=\"8080\"/>" +
              "    <host id=\"one\" portRef=\"host1z\">" +
              "    </host>" +
              "</server>";

        serverConfig = configParser.parseServerConfiguration(new StringReader(xml));

        entry = serverConfig.getFactoryInstance(hostPid, hostAlias, "one");
        result = evaluator.evaluate(entry, registryEntry);
        dictionary = result.getProperties();

        assertNull(dictionary.get("portRef"));
        assertNull(dictionary.get("port"));

        assertNotNull(result.getUnresolvedReferences());
        assertEquals(1, result.getUnresolvedReferences().size());
        for (UnresolvedPidType ref : result.getUnresolvedReferences()) {
            assertTrue(ref instanceof UnresolvedReference);
            assertEquals(hostPortPid + " should be in the list of unresolved PIDs",
                         hostPortPid, ((UnresolvedReference) ref).getPid());
        }

        assertFalse("We should not get any warning messages about unresolved references", outputMgr.checkForMessages("CWWKG0033W"));

    }

    // Reference lookup will fail because (1) we can't find the referenced pid and (2) we have already
    // processed the offending reference type, so we should be able to find it if it actually exists.
    @Test
    public void testReferenceLookupFailure2() throws Exception {
        changeLocationSettings("default");

        String portPid = "com.ibm.ws.port";
        String hostPid = "com.ibm.ws.host";
        String hostPortPid = "com.ibm.ws.host.port";
        MetaTypeRegistry registry = createRegistry(portPid, hostPid, hostPortPid, "port", 0, hostPortPid, true);

        TestConfigEvaluator evaluator = createConfigEvaluator(registry, null);
        evaluator.setLookupMap(new HashMap<ConfigID, String>());

        String xml;
        ServerConfiguration serverConfig;
        ConfigElement entry = null;
        EvaluationResult result = null;

        RegistryEntry registryEntry = registry.getRegistryEntry(hostPid);
        String hostAlias = registryEntry.getAlias();

        xml = "<server>" +
              "    <com.ibm.ws.host.port id=\"host1\" number=\"8080\"/>" +
              "    <host id=\"one\" portRef=\"host1z\">" +
              "    </host>" +
              "</server>";

        serverConfig = configParser.parseServerConfiguration(new StringReader(xml));

        entry = serverConfig.getFactoryInstance(hostPid, hostAlias, "one");
        result = evaluator.evaluate(entry, registryEntry);

        assertNotNull(result.getUnresolvedReferences());
        assertEquals(1, result.getUnresolvedReferences().size());
        for (UnresolvedPidType ref : result.getUnresolvedReferences()) {
            assertTrue(ref instanceof UnresolvedReference);
            assertEquals(hostPortPid + " should be in the list of unresolved PIDs",
                         hostPortPid, ((UnresolvedReference) ref).getPid());
            ref.reportError();
        }

        assertTrue("We should get a warning message about unresolved references", outputMgr.checkForMessages("CWWKG0033W"));
    }

    // Test case where the target PID has not been defined in the metatype registry
    @Test
    public void testReferenceLookupFailure3() throws Exception {
        changeLocationSettings("default");

        String parentPid = "com.ibm.parent";

        MockObjectClassDefinition parentOCD = new MockObjectClassDefinition("parent");
        parentOCD.addAttributeDefinition(new MockAttributeDefinition("ip", AttributeDefinition.STRING, 0, null), false);
        MockAttributeDefinition portRef = new MockAttributeDefinition("childRef", AttributeDefinition.STRING, 10, null);
        portRef.setReferencePid("com.ibm.does.not.exist");
        parentOCD.addAttributeDefinition(portRef);

        MockBundle bundle = new MockBundle();
        MockMetaTypeInformation metatype = new MockMetaTypeInformation(bundle);
        metatype.add(parentPid, true, parentOCD);

        MetaTypeRegistry registry = new MetaTypeRegistry();
        assertFalse("The registry should be updated", registry.addMetaType(metatype).isEmpty());

        TestConfigEvaluator evaluator = createConfigEvaluator(registry, null);
        evaluator.setLookupMap(new HashMap<ConfigID, String>());

        String xml;
        ServerConfiguration serverConfig;
        ConfigElement entry = null;
        EvaluationResult result = null;

        RegistryEntry registryEntry = registry.getRegistryEntry(parentPid);

        xml = "<server>" +
              "    <com.ibm.parent id=\"one\" childRef=\"zzz\"/>" +
              "</server>";

        serverConfig = configParser.parseServerConfiguration(new StringReader(xml));

        entry = serverConfig.getFactoryInstance(parentPid, null, "one");
        result = evaluator.evaluate(entry, registryEntry);

        assertNotNull(result.getUnresolvedReferences());
        assertEquals(0, result.getUnresolvedReferences().size());

        assertFalse("We should not get any warning messages about unresolved references", outputMgr.checkForMessages("CWWKG0033W"));

    }

    // Test the case where a filter doesn't match any configurations
    @Test
    public void testFilterLookupFailure1() throws Exception {
        changeLocationSettings("default");

        RegistryFactory factory = new RegistryFactory();
        factory.addFactoryOCD(createServiceOCD());
        factory.addFactoryOCD(createObjectClassTarget("filterTargetOne"));
        factory.addFactoryOCD(createObjectClassTarget("filterTargetTwo"));

        MetaTypeRegistry registry = factory.create();

        TestConfigEvaluator evaluator = createConfigEvaluator(registry, null);
        evaluator.setLookupMap(new HashMap<ConfigID, String>());

        String xml;
        ServerConfiguration serverConfig;
        ConfigElement entry = null;
        EvaluationResult result = null;

        RegistryEntry registryEntry = registry.getRegistryEntry("com.ibm.ws.serviceOCD");

        xml = "<server>" +
              "    <serviceOCD id=\"one\" serviceRef=\"foo\"/>" +
              "</server>";

        serverConfig = configParser.parseServerConfiguration(new StringReader(xml));

        entry = serverConfig.getFactoryInstance("com.ibm.ws.serviceOCD", "serviceOCD", "one");
        result = evaluator.evaluate(entry, registryEntry);

        assertNotNull(result.getUnresolvedReferences());
        assertEquals(1, result.getUnresolvedReferences().size());
        for (UnresolvedPidType ref : result.getUnresolvedReferences()) {
            assertTrue(ref instanceof UnresolvedService);
            // There were no potential matches
            assertEquals(0, ((UnresolvedService) ref).getCount());
            ref.reportError();
        }

    }

    // Test the case where a filter doesn't match any configurations and there aren't any ocds that match the filter
    @Test
    public void testFilterLookupFailure2() throws Exception {
        changeLocationSettings("default");

        RegistryFactory factory = new RegistryFactory();
        factory.addFactoryOCD(createServiceOCD());

        MetaTypeRegistry registry = factory.create();

        TestConfigEvaluator evaluator = createConfigEvaluator(registry, null);
        evaluator.setLookupMap(new HashMap<ConfigID, String>());

        String xml;
        ServerConfiguration serverConfig;
        ConfigElement entry = null;
        EvaluationResult result = null;

        RegistryEntry registryEntry = registry.getRegistryEntry("com.ibm.ws.serviceOCD");

        xml = "<server>" +
              "    <serviceOCD id=\"one\" serviceRef=\"foo\"/>" +
              "</server>";

        serverConfig = configParser.parseServerConfiguration(new StringReader(xml));

        entry = serverConfig.getFactoryInstance("com.ibm.ws.serviceOCD", "serviceOCD", "one");
        result = evaluator.evaluate(entry, registryEntry);

        assertNotNull(result.getUnresolvedReferences());
        assertEquals(1, result.getUnresolvedReferences().size());
        for (UnresolvedPidType ref : result.getUnresolvedReferences()) {
            assertTrue(ref instanceof UnresolvedService);
            // There are no potential matches
            assertEquals(0, ((UnresolvedService) ref).getCount());
            ref.reportError();
        }

    }

    // Test the case where a filter matches more than one
    @Test
    public void testFilterLookupFailure3() throws Exception {
        changeLocationSettings("default");

        RegistryFactory factory = new RegistryFactory();
        factory.addFactoryOCD(createServiceOCD());
        factory.addFactoryOCD(createObjectClassTarget("serviceTargetOne"));
        factory.addFactoryOCD(createObjectClassTarget("serviceTargetTwo"));

        MetaTypeRegistry registry = factory.create();

        TestConfigEvaluator evaluator = createConfigEvaluator(registry, null);
        Map<ConfigID, String> lookupMap = new HashMap<ConfigID, String>();
        lookupMap.put(new ConfigID("com.ibm.ws.serviceTargetOne", "a"), "one");
        lookupMap.put(new ConfigID("com.ibm.ws.serviceTargetTwo", "a"), "two");

        evaluator.setLookupMap(lookupMap);

        String xml;
        ServerConfiguration serverConfig;
        ConfigElement entry = null;
        EvaluationResult result = null;

        RegistryEntry registryEntry = registry.getRegistryEntry("com.ibm.ws.serviceOCD");

        xml = "<server>" +
              "    <serviceOCD id=\"one\" serviceRef=\"a\"/>" +
              " <serviceTargetOne id=\"a\"/>" +
              " <serviceTargetTwo id=\"a\"/>" +
              "</server>";

        serverConfig = configParser.parseServerConfiguration(new StringReader(xml));

        entry = serverConfig.getFactoryInstance("com.ibm.ws.serviceOCD", "serviceOCD", "one");
        result = evaluator.evaluate(entry, registryEntry);

        assertNotNull(result.getUnresolvedReferences());
        assertEquals(1, result.getUnresolvedReferences().size());
        for (UnresolvedPidType ref : result.getUnresolvedReferences()) {
            assertTrue(ref instanceof UnresolvedService);
            // There were two potential matches
            assertEquals(2, ((UnresolvedService) ref).getCount());
            ref.reportError();
        }

    }

    // The port attribute is specified both as a nested element and as a ref attribute. The duplicate ref attribute should be ignored.
    // The two legitimate ref attributes should still be around with the nested element, making three total ports for the port attribute.
    @Test
    public void testReferenceSpecifiedTwoWays() throws ConfigParserException, ConfigValidationException, ConfigEvaluatorException, ConfigMergeException {
        changeLocationSettings("default");

        String portPid = "com.ibm.ws.port";
        String hostPid = "com.ibm.ws.host";
        String hostPortPid = "com.ibm.ws.host.port";
        String portAttribute = "portRef";
        MetaTypeRegistry registry = createRegistry(portPid, hostPid, hostPortPid, portAttribute, 3, portPid, true);

        String xml = "<server>" +
                     "    <host id=\"one\" portRef=\"1,someOtherId,anotherId\">" +
                     "      <port id=\"1\" number=\"7\" foo=\"bar\"/>" +
                     "      <port id=\"1\" number=\"7\"/>" +
                     "      <port id=\"1\" number=\"7\" color=\"orange\"/>" +
                     "      <port id=\"1\" number=\"7\" shape=\"square\"/>" +
                     "    </host>" +
                     "<port id=\"someOtherId\" number=\"8\"/>" +
                     "<port id=\"anotherId\" number=\"9\"/>" +
                     "</server>";

        ServerConfiguration serverConfig = configParser.parseServerConfiguration(new StringReader(xml));

        TestConfigEvaluator evaluator = createConfigEvaluator(registry, null);

        ConfigID parentId = new ConfigID(hostPid, "one");
        ConfigID nestedIdOne = new ConfigID(parentId, portPid, "1", "port");
        ConfigID nestedIdOther = new ConfigID(portPid, "someOtherId");
        ConfigID nestedIdAnother = new ConfigID(portPid, "anotherId");

        Map<ConfigID, String> pidMap = new HashMap<ConfigID, String>();
        pidMap.put(nestedIdOne, "pid-1");
        pidMap.put(nestedIdOther, "pid-2");
        pidMap.put(nestedIdAnother, "pid-3");
        evaluator.setLookupMap(pidMap);

        ConfigElement entry = null;

        RegistryEntry registryEntry = registry.getRegistryEntry(hostPid);
        entry = serverConfig.getFactoryInstance(hostPid, registryEntry.getAlias(), "one");
        EvaluationResult result = evaluator.evaluate(entry, registryEntry);

        assertEquals(3, result.getReferences().size());
        EvaluationResult nestedResult = result.getNested().get(nestedIdOne);
        assertNotNull(nestedResult);
        assertEquals("7", nestedResult.getProperties().get("number"));

        assertEquals("bar", nestedResult.getProperties().get("foo"));
        assertEquals("orange", nestedResult.getProperties().get("color"));
        assertEquals("square", nestedResult.getProperties().get("shape"));

        String[] portarray = (String[]) result.getProperties().get("portRef");
        assertEquals(3, portarray.length);
        assertEquals(toSet("pid-1", "pid-2", "pid-3"), toSet(portarray));

        assertEquals(0, result.getAllUnresolvedReferences().size());

    }

//    @Test
//    public void testEscape() throws Exception {
//        ConfigEvaluator ce = createConfigEvaluator(null);
//        assertEquals("simple text", ce.escapeValue("simple text"));
//
//        assertEquals("value a\\, value b", ce.escapeValue("value a, value b"));
//
//        assertEquals("\\,value a\\, value b\\,", ce.escapeValue(",value a, value b,"));
//
//        assertEquals("value a\\\\value b", ce.escapeValue("value a\\value b"));
//
//        assertEquals("\\\\value a\\\\value b\\\\", ce.escapeValue("\\value a\\value b\\"));
//
//        assertEquals("\\,\\\\value a\\\\\\,value b\\\\\\,\\\\\\,", ce.escapeValue(",\\value a\\,value b\\,\\,"));
//    }

    private MockObjectClassDefinition createFlatOCD(String id, String alias, String extendsAlias, String extendsPid, boolean isFlat, String... flats) {
        MockObjectClassDefinition ocd = new MockObjectClassDefinition(id);
        if (alias != null) {
            ocd.setAlias(alias);
        }
        if (extendsAlias != null) {
            ocd.setExtendsAlias(extendsAlias);
        }
        if (extendsPid != null) {
            ocd.setExtensionAttribute(XMLConfigConstants.METATYPE_EXTENSION_URI, "extends", extendsPid);
        }
        ocd.addAttributeDefinition(new MockAttributeDefinition("attr", AttributeDefinition.STRING, 0, null), false);
        for (String flatAttribute : flats) {
            MockAttributeDefinition ad = new MockAttributeDefinition(flatAttribute, MetaTypeFactory.PID_TYPE);
            ad.setReferencePid("com.ibm.ws." + flatAttribute);
            if (isFlat) {
                ad.setExtensionAttribute(XMLConfigConstants.METATYPE_EXTENSION_URI, ExtendedAttributeDefinition.FLAT_ATTR_NAME, "true");
            }
            ocd.addAttributeDefinition(ad, false);
            ad.setCardinality(-4);
        }
        return ocd;
    }

    private MetaTypeRegistry createFlatRegistry(boolean isFlat) {
        return createFlatRegistry(isFlat, false);
    }

    private MetaTypeRegistry createFlatRegistry(boolean isFlat, boolean refRequired) {
        MockBundle bundle = new MockBundle();
        MockMetaTypeInformation metatype = new MockMetaTypeInformation(bundle);
        metatype.add("com.ibm.ws.top", true, createFlatOCD("top", "top", null, null, isFlat, "c1", "c.2", "b1"));

        MockObjectClassDefinition c1 = createFlatOCD("c1", "c1", null, null, isFlat, "c11", "c12");
        c1.addAttributeDefinition(new MockAttributeDefinition("multiAttr", AttributeDefinition.STRING, 1));
        MockAttributeDefinition ref = new MockAttributeDefinition("ref", MetaTypeFactory.PID_TYPE);
        ref.setReferencePid("com.ibm.ws.ref");
        c1.addAttributeDefinition(ref, refRequired);
        metatype.add("com.ibm.ws.c1", true, c1);
        metatype.add("com.ibm.ws.c11", true, createFlatOCD("c11", null, null, null, isFlat));
        metatype.add("com.ibm.ws.c12", true, createFlatOCD("c12", null, null, null, isFlat));

        metatype.add("com.ibm.ws.c.2", true, createFlatOCD("c.2", null, null, null, isFlat, "c.2.1"));
        metatype.add("com.ibm.ws.c.2.1", true, createFlatOCD("c.2.1", null, null, null, isFlat));

        //extends
        metatype.add("com.ibm.ws.b1", true, createFlatOCD("b1", null, null, null, isFlat));
        metatype.add("com.ibm.ws.bsub1", true, createFlatOCD("bsub1", null, "bsub1", "com.ibm.ws.b1", isFlat));
        metatype.add("com.ibm.ws.bsub2", true, createFlatOCD("bsub2", null, "bsub2", "com.ibm.ws.b1", isFlat));
        metatype.add("com.ibm.ws.bsub2sub1", true, createFlatOCD("bsub2sub1", null, "bsub2sub1", "com.ibm.ws.bsub2", isFlat));

        //extending an attribute
        metatype.add("com.ibm.ws.bsub3", true, createFlatOCD("bsub3", null, "bsub3", "com.ibm.ws.b1", isFlat, "bsub3.1"));
        metatype.add("com.ibm.ws.bsub3.1", true, createFlatOCD("bsub3.1", null, null, null, isFlat));
        metatype.add("com.ibm.ws.bsub3.1sub1", true, createFlatOCD("bsub3.1sub1", null, "bsub3.1sub1", "com.ibm.ws.bsub3.1", isFlat));
        metatype.add("com.ibm.ws.bsub3.1sub2", true, createFlatOCD("bsub3.1sub2", null, "bsub3.1sub2", "com.ibm.ws.bsub3.1", isFlat));

        metatype.add("com.ibm.ws.ref", true, createFlatOCD("ref", null, null, null, false));
        MetaTypeRegistry registry = new MetaTypeRegistry();
        assertTrue(!registry.addMetaType(metatype).isEmpty());

        return registry;
    }

    @Test
    public void testFlat() throws Exception {
        String xml = "<server>" +
                     "    <top id='top' varTest='${c1.0.multiAttr}' attr='top'>" +
                     "      <c1 attr='c1a' multiAttr='c1First' varTest2='${c1.0.c11.0.attr}'>" +
                     //extra non-metatype attribute
                     "        <c11 attr='c11a' foo='foo'/>" +
                     //attribute as nested element
                     "        <c11><attr>c11b</attr></c11>" +
                     "        <c12 attr='c12a'/>" +
                     "        <c12 attr='c12b'/>" +
                     "      </c1>" +
                     "      <c.2 attr='c2a'>" +
                     "        <c.2.1 attr='c21a'/>" +
                     "        <c.2.1 attr='c21b'/>" +
                     "      </c.2>" +
                     "      <c.2 attr='c2b'>" +
                     "        <c.2.1 attr='c21c'/>" +
                     "        <c.2.1 attr='c21d'/>" +
                     "      </c.2>" +
                     "    </top>" +
                     "</server>";

        internalTestFlat(xml, null);
    }

    @Test
    public void testFlatCardinalityViolation() throws Exception {
        String xml = "<server>" +
                     "    <top id='top' varTest='${c1.0.multiAttr}' attr='top'>" +
                     "      <c.2 attr='c2a'>" +
                     "        <c.2.1 attr='c21a'/>" +
                     "        <c.2.1 attr='c21b'/>" +
                     "      </c.2>" +
                     "      <c.2 attr='c2b'>" +
                     "        <c.2.1 attr='c21c'/>" +
                     "        <c.2.1 attr='c21d'/>" +
                     "      </c.2>" +
                     "      <c.2 attr='c2c'>" +
                     "        <c.2.1 attr='c21c'/>" +
                     "        <c.2.1 attr='c21d'/>" +
                     "      </c.2>" +
                     "      <c.2 attr='c2d'>" +
                     "        <c.2.1 attr='c21c'/>" +
                     "        <c.2.1 attr='c21d'/>" +
                     "      </c.2>" +
                     "      <c.2 attr='c2e'>" +
                     "        <c.2.1 attr='c21c'/>" +
                     "        <c.2.1 attr='c21d'/>" +
                     "      </c.2>" +
                     "    </top>" +
                     "</server>";

        try {
            internalTestFlat(xml, null);
            fail("Expected cardinality violation exception");
        } catch (ConfigEvaluatorException e) {
            assertTrue("Expected exception message to contain string 'exceeded maximum allowed size'", e.getMessage().indexOf("exceeded maximum allowed size") > -1);
        }

    }

    @Test
    public void testFlatWithId() throws Exception {
        String xml = "<server>" +
                     "    <top id='top' varTest='${c1.0.multiAttr}' attr='top'>" +
                     "      <c1 id='myc1' attr='c1a' multiAttr='c1First' varTest2='${c1.0.c11.0.attr}'>" +
                     //extra non-metatype attribute
                     "        <c11 attr='c11a' foo='foo'/>" +
                     //attribute as nested element
                     "        <c11><attr>c11b</attr></c11>" +
                     "        <c12 attr='c12a'/>" +
                     "        <c12 attr='c12b'/>" +
                     "      </c1>" +
                     "      <c.2 attr='c2a'>" +
                     "        <c.2.1 attr='c21a'/>" +
                     "        <c.2.1 attr='c21b'/>" +
                     "      </c.2>" +
                     "      <c.2 attr='c2b'>" +
                     "        <c.2.1 attr='c21c'/>" +
                     "        <c.2.1 attr='c21d'/>" +
                     "      </c.2>" +
                     "    </top>" +
                     "</server>";

        internalTestFlat(xml, "myc1");

    }

    @Test
    public void testFlatNotNested() throws Exception {
        String xml = "<server>" +
                     "    <top id='top' varTest='${c1.0.multiAttr}' attr='top' c1='myc1'>" +
                     "      <c.2 attr='c2a'>" +
                     "        <c.2.1 attr='c21a'/>" +
                     "        <c.2.1 attr='c21b'/>" +
                     "      </c.2>" +
                     "      <c.2 attr='c2b'>" +
                     "        <c.2.1 attr='c21c'/>" +
                     "        <c.2.1 attr='c21d'/>" +
                     "      </c.2>" +
                     "    </top>" +
                     "      <c1 id='myc1' attr='c1a' multiAttr='c1First' varTest2='${c1.0.c11.0.attr}'>" +
                     //extra non-metatype attribute
                     "        <c11 attr='c11a' foo='foo'/>" +
                     //attribute as nested element
                     "        <c11><attr>c11b</attr></c11>" +
                     "        <c12 attr='c12a'/>" +
                     "        <c12 attr='c12b'/>" +
                     "      </c1>" +
                     "</server>";

        internalTestFlat(xml, "myc1");

    }

    @Test
    public void testFlatNotNestedRef() throws Exception {
        String xml = "<server>" +
                     "    <top id='top' varTest='${c1.0.multiAttr}' attr='top' c1Ref='myc1'>" +
                     "      <c.2 attr='c2a'>" +
                     "        <c.2.1 attr='c21a'/>" +
                     "        <c.2.1 attr='c21b'/>" +
                     "      </c.2>" +
                     "      <c.2 attr='c2b'>" +
                     "        <c.2.1 attr='c21c'/>" +
                     "        <c.2.1 attr='c21d'/>" +
                     "      </c.2>" +
                     "    </top>" +
                     "      <c1 id='myc1' attr='c1a' multiAttr='c1First' varTest2='${c1.0.c11.0.attr}'>" +
                     //extra non-metatype attribute
                     "        <c11 attr='c11a' foo='foo'/>" +
                     //attribute as nested element
                     "        <c11><attr>c11b</attr></c11>" +
                     "        <c12 attr='c12a'/>" +
                     "        <c12 attr='c12b'/>" +
                     "      </c1>" +
                     "</server>";

        internalTestFlat(xml, "myc1");

    }

    private void internalTestFlat(String xml, String c1Id) throws Exception {
        internalTestFlat(xml, c1Id, false, false);
        internalTestFlat(xml, c1Id, true, false);
        internalTestFlat(xml, c1Id, false, true);
        internalTestFlat(xml, c1Id, true, true);
    }

    private void internalTestFlat(String xml, String c1Id, boolean fromDelta, boolean refRequired) throws Exception {
        changeLocationSettings("default");

        String topPid = "com.ibm.ws.top";

        MetaTypeRegistry registry = createFlatRegistry(true, refRequired);

        ServerConfiguration serverConfig = configParser.parseServerConfiguration(new StringReader(xml));

        TestConfigEvaluator evaluator = createConfigEvaluator(registry, serverConfig);

        ConfigElement entry = null;

        RegistryEntry registryEntry = registry.getRegistryEntry(topPid);
        if (fromDelta) {
            ServerConfiguration emptyServerConfig = configParser.parseServerConfiguration(new StringReader("<server/>"));
            ConfigComparator comparator = new ConfigComparator(emptyServerConfig, serverConfig, registry);
            ComparatorResult comparatorResult = comparator.computeDelta();
            for (ConfigDelta delta : comparatorResult.getConfigDelta()) {
                if (delta.getDelta() == DeltaType.ADDED && delta.getConfigElement().getNodeName().equals(topPid)) {
                    entry = delta.getConfigElement();
                }
            }
            assertNotNull(comparatorResult.toString(), entry);
        } else {
            entry = serverConfig.getFactoryInstance(topPid, registryEntry.getAlias(), "top");
        }
        EvaluationResult result = evaluator.evaluate(entry, registryEntry);
        Dictionary<String, Object> properties = result.getProperties();
        assertNotNull(properties);
        assertEquals(properties.toString(), refRequired ? 17 : 30 + (c1Id == null ? 0 : 1), properties.size());
        assertEquals("top", properties.get("attr"));
        assertEquals("top", properties.get("id"));
        assertEquals(new ConfigID(topPid, "top").toString(), properties.get("config.id"));
        assertEquals(new ConfigID("top", "top").toString(), properties.get("config.displayId"));
        if (refRequired) {
            assertTrue("Expected missing required attribute message",
                       outputMgr.checkForStandardErr("CWWKG0095E: The element") || outputMgr.checkForStandardErr("CWWKG0058E: The element"));
            tearDown();
        } else {
            assertEquals("c1First", properties.get("varTest"));
            assertEquals(c1Id, properties.get("c1.0.id"));
            assertEquals("com.ibm.ws.c1", properties.get("c1.0.config.referenceType"));
            assertEquals("c1a", properties.get("c1.0.attr"));
            assertEquals("c11a", properties.get("c1.0.varTest2"));
            assertEquals("com.ibm.ws.c11", properties.get("c1.0.c11.0.config.referenceType"));
            assertEquals("c11a", properties.get("c1.0.c11.0.attr"));
            //undeclared simple attribute
            assertEquals("foo", properties.get("c1.0.c11.0.foo"));
            assertEquals("com.ibm.ws.c11", properties.get("c1.0.c11.1.config.referenceType"));
            assertEquals("c11b", properties.get("c1.0.c11.1.attr"));
            assertEquals("com.ibm.ws.c12", properties.get("c1.0.c12.0.config.referenceType"));
            assertEquals("c12a", properties.get("c1.0.c12.0.attr"));
            assertEquals("com.ibm.ws.c12", properties.get("c1.0.c12.0.config.referenceType"));
            assertEquals("c12b", properties.get("c1.0.c12.1.attr"));
        }
        assertEquals("com.ibm.ws.c.2", properties.get("c.2.0.config.referenceType"));
        assertEquals("c2a", properties.get("c.2.0.attr"));
        assertEquals("com.ibm.ws.c.2.1", properties.get("c.2.0.c.2.1.0.config.referenceType"));
        assertEquals("c21a", properties.get("c.2.0.c.2.1.0.attr"));
        assertEquals("com.ibm.ws.c.2.1", properties.get("c.2.0.c.2.1.1.config.referenceType"));
        assertEquals("c21b", properties.get("c.2.0.c.2.1.1.attr"));
        assertEquals("com.ibm.ws.c.2", properties.get("c.2.1.config.referenceType"));
        assertEquals("c2b", properties.get("c.2.1.attr"));
        assertEquals("com.ibm.ws.c.2.1", properties.get("c.2.1.c.2.1.0.config.referenceType"));
        assertEquals("c21c", properties.get("c.2.1.c.2.1.0.attr"));
        assertEquals("com.ibm.ws.c.2.1", properties.get("c.2.1.c.2.1.0.config.referenceType"));
        assertEquals("c21d", properties.get("c.2.1.c.2.1.1.attr"));

        Map<String, Object> map = new HashMap<String, Object>();
        for (Enumeration<String> e = properties.keys(); e.hasMoreElements();) {
            String key = e.nextElement();
            Object value = properties.get(key);
            map.put(key, value);
        }
        {
            //test single-key nester
            List<Map<String, Object>> c1 = Nester.nest("c1", map);
            if (refRequired) {
                assertEquals(0, c1.size());
            } else {
                assertEquals(1, c1.size());
                Map<String, Object> dc1 = c1.get(0);
                assertEquals(dc1.toString(), 13 + (c1Id == null ? 0 : 1), dc1.size());
                List<Map<String, Object>> c11 = Nester.nest("c11", dc1);
                assertEquals(2, c11.size());
            }
            List<Map<String, Object>> c2 = Nester.nest("c.2", map);
            assertEquals(2, c2.size());
        }
        {
            //test multiple-key nester
            Map<String, List<Map<String, Object>>> m1 = Nester.nest(map, "c1", "c.2");
            assertEquals(2, m1.size());
            List<Map<String, Object>> c1 = m1.get("c1");

            if (refRequired) {
                assertEquals(0, c1.size());
            } else {
                assertEquals(1, c1.size());

                Map<String, List<Map<String, Object>>> m11 = Nester.nest(c1.get(0), "c11", "c12");
                assertEquals(2, m11.size());
                List<Map<String, Object>> c11 = m11.get("c11");
                assertEquals(2, c11.size());
                List<Map<String, Object>> c12 = m11.get("c12");
                assertEquals(2, c12.size());
            }

            List<Map<String, Object>> c2 = m1.get("c.2");
            assertEquals(2, c2.size());

            Map<String, List<Map<String, Object>>> m21 = Nester.nest(c2.get(0), "c.2.1");
            assertEquals(1, m21.size());
            List<Map<String, Object>> c21 = m21.get("c.2.1");
            assertEquals(2, c21.size());
        }

        VariableRegistry vr = new ConfigVariableRegistry(new VariableRegistryHelper(), new String[0], null);
        assertEquals("flat variable should not be in the variable registry", "${c1.0.multiAttr}", vr.resolveString("${c1.0.multiAttr}"));
    }

    @Test
    public void testFlatExtends() throws Exception {
        MetaTypeRegistry registry = createFlatRegistry(true);
        internalTestFlatExtends(registry);

    }

    /**
     * same as testFlatExtends except the metatype is registered in reverse order so the extended
     * entry is never present when an extending entry is registered.
     *
     * @throws Exception
     */
    @Test
    public void testFlatExtendsReverseRegistrationOrder() throws Exception {
        MetaTypeRegistry registry = new MetaTypeRegistry();

        //register the metatype info in reverse inheritance order, leaves first
        MockMetaTypeInformation metatype = new MockMetaTypeInformation(new MockBundle());
        metatype.add("com.ibm.ws.bsub2sub1", true, createFlatOCD("bsub2sub1", null, "bsub2sub1", "com.ibm.ws.bsub2", true));
        metatype.add("com.ibm.ws.bsub3.1sub1", true, createFlatOCD("bsub3.1sub1", null, "bsub3.1sub1", "com.ibm.ws.bsub3.1", true));
        metatype.add("com.ibm.ws.bsub3.1sub2", true, createFlatOCD("bsub3.1sub2", null, "bsub3.1sub2", "com.ibm.ws.bsub3.1", true));
        assertTrue(registry.addMetaType(metatype).isEmpty());

        metatype = new MockMetaTypeInformation(new MockBundle());
        metatype.add("com.ibm.ws.bsub1", true, createFlatOCD("bsub1", null, "bsub1", "com.ibm.ws.b1", true));
        metatype.add("com.ibm.ws.bsub2", true, createFlatOCD("bsub2", null, "bsub2", "com.ibm.ws.b1", true));
        metatype.add("com.ibm.ws.bsub3", true, createFlatOCD("bsub3", null, "bsub3", "com.ibm.ws.b1", true, "bsub3.1"));
        metatype.add("com.ibm.ws.bsub3.1", true, createFlatOCD("bsub3.1", null, null, null, true));
        assertTrue(!registry.addMetaType(metatype).isEmpty());

        metatype = new MockMetaTypeInformation(new MockBundle());
        metatype.add("com.ibm.ws.top", true, createFlatOCD("top", "top", null, null, true, "b1"));
        metatype.add("com.ibm.ws.b1", true, createFlatOCD("b1", "b1", null, null, true));
        assertTrue(!registry.addMetaType(metatype).isEmpty());

        internalTestFlatExtends(registry);

    }

    /**
     * Tests that a flat AD referencing a completely missing pid does not cause an exception.
     * Imitates testFlatExtendsReverseRegistrationOrder for convenience
     *
     * @throws Exception
     */
    @Test
    public void testFlatMissingReferencedPid() throws Exception {
        MetaTypeRegistry registry = new MetaTypeRegistry();

        //register the metatype info in reverse inheritance order, leaves first
        MockMetaTypeInformation metatype = new MockMetaTypeInformation(new MockBundle());
        metatype.add("com.ibm.ws.bsub2sub1", true, createFlatOCD("bsub2sub1", null, "bsub2sub1", "com.ibm.ws.bsub2", true));
        metatype.add("com.ibm.ws.bsub3.1sub1", true, createFlatOCD("bsub3.1sub1", null, "bsub3.1sub1", "com.ibm.ws.bsub3.1", true));
        metatype.add("com.ibm.ws.bsub3.1sub2", true, createFlatOCD("bsub3.1sub2", null, "bsub3.1sub2", "com.ibm.ws.bsub3.1", true));
        assertTrue(registry.addMetaType(metatype).isEmpty());

        metatype = new MockMetaTypeInformation(new MockBundle());
        metatype.add("com.ibm.ws.bsub1", true, createFlatOCD("bsub1", null, "bsub1", "com.ibm.ws.b1", true, "missing1"));
        metatype.add("com.ibm.ws.bsub2", true, createFlatOCD("bsub2", null, "bsub2", "com.ibm.ws.b1", true));
        metatype.add("com.ibm.ws.bsub3", true, createFlatOCD("bsub3", null, "bsub3", "com.ibm.ws.b1", true, "bsub3.1"));
        metatype.add("com.ibm.ws.bsub3.1", true, createFlatOCD("bsub3.1", null, null, null, true, "missing2"));
        assertTrue(!registry.addMetaType(metatype).isEmpty());

        metatype = new MockMetaTypeInformation(new MockBundle());
        metatype.add("com.ibm.ws.top", true, createFlatOCD("top", "top", null, null, true, "b1"));
        metatype.add("com.ibm.ws.b1", true, createFlatOCD("b1", "b1", null, null, true, "missing3"));
        assertTrue(!registry.addMetaType(metatype).isEmpty());

        internalTestFlatExtends(registry);

    }

    private void internalTestFlatExtends(MetaTypeRegistry registry) throws ConfigParserException, ConfigValidationException, ConfigEvaluatorException, ConfigMergeException {
        changeLocationSettings("default");

        String topPid = "com.ibm.ws.top";

        String xml = "<server>" +
                     "    <top id='top' attr='top'>" +
                     "      <b1.bsub1 attr='bsub1a'/>" +
                     "      <b1.bsub2 attr='bsub2a'/>" +
                     "      <b1.bsub2sub1 attr='bsub2sub1a'/>" +
                     "      <b1.bsub3 attr='bsub3sub1a'>" +
                     "          <bsub3.1.bsub3.1sub1 attr='bsub3.1sub1'/>" +
                     "          <bsub3.1.bsub3.1sub2 attr='bsub3.1sub2'/>" +
                     "      </b1.bsub3>" +
                     "    </top>" +
                     "</server>";

        ServerConfiguration serverConfig = configParser.parseServerConfiguration(new StringReader(xml));

        TestConfigEvaluator evaluator = createConfigEvaluator(registry, null);

        ConfigElement entry = null;

        RegistryEntry registryEntry = registry.getRegistryEntry(topPid);
        entry = serverConfig.getFactoryInstance(topPid, registryEntry.getAlias(), "top");
        EvaluationResult result = evaluator.evaluate(entry, registryEntry);
        Dictionary<String, Object> properties = result.getProperties();

        assertEquals("top", properties.get("attr"));
        assertEquals("top[top]", properties.get("config.displayId"));
        assertEquals("com.ibm.ws.top[top]", properties.get("config.id"));
        assertEquals("top", properties.get("id"));

        List<Map<String, Object>> b1 = Nester.nest("b1", properties);

        assertEquals("should be 4 subelements for b1", 4, b1.size());
        Set<String> referenceTypes = new HashSet<String>();

        int bsub3Index = -1;
        for (Map<String, Object> b1sub : b1) {
            String referenceType = (String) b1sub.get("config.referenceType");
            referenceTypes.add(referenceType);
            if ("com.ibm.ws.bsub3".equals(referenceType)) {
                bsub3Index = b1.indexOf(b1sub);
                assertEquals(6, b1sub.size());
                assertEquals("bsub3sub1a", b1sub.get("attr"));
                List<Map<String, Object>> bsub3_1 = Nester.nest("bsub3.1", b1sub);
                assertEquals(2, bsub3_1.size());
                for (Map<String, Object> b3sub : bsub3_1) {
                    assertEquals(2, b3sub.size());
                    referenceType = (String) b3sub.get("config.referenceType");
                    referenceTypes.add(referenceType);
                    if ("com.ibm.ws.bsub3.1sub2".equals(referenceType)) {
                        assertEquals("bsub3.1sub2", b3sub.get("attr"));
                    } else if ("com.ibm.ws.bsub3.1sub1".equals(referenceType)) {
                        assertEquals("bsub3.1sub1", b3sub.get("attr"));
                    }
                }
            } else if ("com.ibm.ws.bsub1".equals(referenceType)) {
                assertEquals(2, b1sub.size());
                assertEquals("bsub1a", b1sub.get("attr"));
            } else if ("com.ibm.ws.bsub2".equals(referenceType)) {
                assertEquals(2, b1sub.size());
                assertEquals("bsub2a", b1sub.get("attr"));
            } else if ("com.ibm.ws.bsub2sub1".equals(referenceType)) {
                assertEquals(2, b1sub.size());
                assertEquals("bsub2sub1a", b1sub.get("attr"));
            }

        }
        assertEquals("should be 6 reference types", 6, referenceTypes.size());

        Map<String, Object> map = new HashMap<String, Object>();
        for (Enumeration<String> e = properties.keys(); e.hasMoreElements();) {
            String key = e.nextElement();
            Object value = properties.get(key);
            map.put(key, value);
        }
        //test multiple-key nester
        Map<String, List<Map<String, Object>>> m1 = Nester.nest(map, "b1");
        assertEquals(1, m1.size());
        List<Map<String, Object>> c1 = m1.get("b1");
        assertEquals(c1.toString(), 4, c1.size());

        Map<String, List<Map<String, Object>>> m11 = Nester.nest(c1.get(bsub3Index), "bsub3.1");
        assertEquals(1, m11.size());
        List<Map<String, Object>> c11 = m11.get("bsub3.1");
        assertEquals("c1" + c1, 2, c11.size());

    }

    /**
     * two parents
     * two distinct abstract nested elemsnts
     * each nested element is extended by a concrete element with the same extendsAlias
     * xml uses extendsAlias.
     * Check that the correct config.referenceType is set.
     *
     * @throws Exception
     */
    @Test
    public void testFlatExtendsWithExtendsAlias() throws Exception {

        MockBundle bundle = new MockBundle();
        MockMetaTypeInformation metatype = new MockMetaTypeInformation(bundle);
        metatype.add("com.ibm.ws.top1", true, createFlatOCD("top1", "top1", null, null, true, "b1"));

        metatype.add("com.ibm.ws.top2", true, createFlatOCD("top2", "top2", null, null, true, "b2"));

        //extends
        metatype.add("com.ibm.ws.b1", true, createFlatOCD("b1", null, null, null, true));
        metatype.add("com.ibm.ws.bsub1", true, createFlatOCD("bsub1", null, "foo", "com.ibm.ws.b1", true));

        metatype.add("com.ibm.ws.b2", true, createFlatOCD("b2", null, null, null, true));
        metatype.add("com.ibm.ws.bsub2", true, createFlatOCD("bsub2", null, "foo", "com.ibm.ws.b2", true));

        MetaTypeRegistry registry = new MetaTypeRegistry();
        assertTrue(!registry.addMetaType(metatype).isEmpty());

        changeLocationSettings("default");

        String xml = "<server>" +
                     "    <top1 id='top1' attr='top1'>" +
                     "      <b1.foo attr='b1'/>" +
                     "    </top1>" +
                     "    <top2 id='top2' attr='top2'>" +
                     "      <b2.foo attr='b2'/>" +
                     "    </top2>" +
                     "</server>";

        ServerConfiguration serverConfig = configParser.parseServerConfiguration(new StringReader(xml));

        TestConfigEvaluator evaluator = createConfigEvaluator(registry, null);

        ConfigElement entry = null;

        String topPID1 = "com.ibm.ws.top1";
        RegistryEntry registryEntry = registry.getRegistryEntry(topPID1);
        entry = serverConfig.getFactoryInstance(topPID1, registryEntry.getAlias(), "top1");
        EvaluationResult result = evaluator.evaluate(entry, registryEntry);
        Dictionary<String, Object> properties = result.getProperties();

        assertEquals("top1", properties.get("attr"));
        assertEquals("com.ibm.ws.bsub1", properties.get("b1.0.config.referenceType"));

        assertEquals(properties.toString(), 6, properties.size());

        String topPID2 = "com.ibm.ws.top2";
        registryEntry = registry.getRegistryEntry(topPID2);
        entry = serverConfig.getFactoryInstance(topPID2, registryEntry.getAlias(), "top2");
        result = evaluator.evaluate(entry, registryEntry);
        properties = result.getProperties();

        assertEquals("top2", properties.get("attr"));
        assertEquals("com.ibm.ws.bsub2", properties.get("b2.0.config.referenceType"));

        assertEquals(properties.toString(), 6, properties.size());

    }

    @Test
    public void testExtends() throws Exception {
        MetaTypeRegistry registry = createFlatRegistry(false);
        changeLocationSettings("default");

        String topPid = "com.ibm.ws.top";

        String xml = "<server>" +
                     "    <top id='top' attr='top'>" +
                     "      <b1.bsub1 attr='bsub1a'/>" +
                     "      <b1.bsub2 attr='bsub2a'/>" +
                     "      <b1.bsub2sub1 attr='bsub2sub1a'/>" +
                     "      <b1.bsub3 attr='bsub3sub1a'>" +
                     "          <bsub3.1.bsub3.1sub1 attr='bsub3.1sub1'/>" +
                     "          <bsub3.1.bsub3.1sub2 attr='bsub3.1sub2'/>" +
                     "      </b1.bsub3>" +
                     "    </top>" +
                     "</server>";

        ServerConfiguration serverConfig = configParser.parseServerConfiguration(new StringReader(xml));

        TestConfigEvaluator evaluator = createConfigEvaluator(registry, null);

        ConfigElement entry = null;

        RegistryEntry registryEntry = registry.getRegistryEntry(topPid);
        entry = serverConfig.getFactoryInstance(topPid, registryEntry.getAlias(), "top");
        EvaluationResult result = evaluator.evaluate(entry, registryEntry);
        Dictionary<String, Object> properties = result.getProperties();

        assertTrue(properties.get("b1") instanceof List);
        assertEquals(4, ((List<?>) properties.get("b1")).size());

        Map<ConfigID, EvaluationResult> nested = result.getNested();
        assertEquals(4, nested.size());

        ConfigID top = new ConfigID("com.ibm.ws.top", "top");
        ConfigID bsub1 = new ConfigID(top, "com.ibm.ws.bsub1", "default-0", "b1.bsub1");
        ConfigID bsub2 = new ConfigID(top, "com.ibm.ws.bsub2", "default-2", "b1.bsub2");
        ConfigID bsub2sub1 = new ConfigID(top, "com.ibm.ws.bsub2sub1", "default-1", "b1.bsub2sub1");
        ConfigID bsub3 = new ConfigID(top, "com.ibm.ws.bsub3", "default-3", "b1.bsub3");

        EvaluationResult b1 = nested.get(bsub1);
        EvaluationResult b2 = nested.get(bsub2);
        EvaluationResult b3 = nested.get(bsub2sub1);
        EvaluationResult b4 = nested.get(bsub3);

        //check that the pids of the nested elements are in fact the pids of the concrete metatype, not the abstract super metatype
        assertEquals("com.ibm.ws.bsub1", b1.getConfigElement().getNodeName());
        assertEquals("com.ibm.ws.bsub2", b2.getConfigElement().getNodeName());
        assertEquals("com.ibm.ws.bsub2sub1", b3.getConfigElement().getNodeName());
        assertEquals("com.ibm.ws.bsub3", b4.getConfigElement().getNodeName());

        //look at the sub-nested elements
        ConfigElement sub3Entry = b4.getConfigElement();
        assertNotNull(sub3Entry);
        EvaluationResult sub3Result = evaluator.evaluate(sub3Entry, b4.getRegistryEntry());
        Dictionary<String, Object> sub3Properties = sub3Result.getProperties();

        assertTrue(sub3Properties.get("bsub3.1") instanceof List);
        assertEquals(2, ((List<?>) sub3Properties.get("bsub3.1")).size());

        Map<ConfigID, EvaluationResult> sub3Nested = sub3Result.getNested();
        assertEquals(2, sub3Nested.size());

        ConfigID bsub3sub1 = new ConfigID(bsub3, "com.ibm.ws.bsub3.1sub1", "default-0", "bsub3.1.bsub3.1sub1");
        ConfigID bsub3sub2 = new ConfigID(bsub3, "com.ibm.ws.bsub3.1sub2", "default-1", "bsub3.1.bsub3.1sub2");

        EvaluationResult b4sub1 = sub3Nested.get(bsub3sub1);
        EvaluationResult b4sub2 = sub3Nested.get(bsub3sub2);

        //check that the pids of the nested elements are in fact the pids of the concreate metatype, not the abstract super metatype
        assertEquals("com.ibm.ws.bsub3.1sub1", b4sub1.getConfigElement().getNodeName());
        assertEquals("com.ibm.ws.bsub3.1sub2", b4sub2.getConfigElement().getNodeName());
    }

    @Test
    public void testFlatWithNotFlatNested() throws Exception {
        String xml = "<server>" +
                     "    <top id='top' varTest='${c1.0.multiAttr}' attr='top'>" +
                     "      <c1 attr='c1a' multiAttr='c1First'>" +
                     "        <ref attr='ref'/>" +
                     "      </c1>" +
                     "    </top>" +
                     "</server>";

        changeLocationSettings("default");

        String topPid = "com.ibm.ws.top";

        MetaTypeRegistry registry = createFlatRegistry(true);

        ServerConfiguration serverConfig = configParser.parseServerConfiguration(new StringReader(xml));

        TestConfigEvaluator evaluator = createConfigEvaluator(registry, serverConfig);

        ConfigElement entry = null;

        RegistryEntry registryEntry = registry.getRegistryEntry(topPid);
        entry = serverConfig.getFactoryInstance(topPid, registryEntry.getAlias(), "top");
        EvaluationResult result = evaluator.evaluate(entry, registryEntry);
        Dictionary<String, Object> properties = result.getProperties();
        assertNotNull(properties);
        assertEquals(properties.toString(), 9, properties.size());
        assertEquals("top", properties.get("attr"));
        assertEquals("top", properties.get("id"));
        assertEquals("c1First", properties.get("varTest"));
        assertEquals(new ConfigID(topPid, "top").toString(), properties.get("config.id"));
        assertEquals(new ConfigID("top", "top").toString(), properties.get("config.displayId"));
        assertEquals("com.ibm.ws.c1", properties.get("c1.0.config.referenceType"));
        assertEquals("c1a", properties.get("c1.0.attr"));

        assertEquals(1, result.getNested().size());
        EvaluationResult nested = result.getNested().get(ConfigID.fromProperty("top[top]//c1(c1)//com.ibm.ws.ref(ref)[default-0]"));
        assertNotNull(nested);
    }

    @Test
    public void testFinalIDAttribute() throws Exception {

        String topPid = "com.ibm.ws.config.test.top";

        MockObjectClassDefinition ocd = new MockObjectClassDefinition("com.ibm.ws.config.test.top");
        MockAttributeDefinition idAttr = new MockAttributeDefinition("id", AttributeDefinition.STRING, 0, new String[] { "topId" });
        idAttr.setExtensionAttribute(XMLConfigConstants.METATYPE_EXTENSION_URI, ExtendedAttributeDefinition.FINAL_ATTR_NAME, "true");
        ocd.addAttributeDefinition(idAttr);
        ocd.addAttributeDefinition(new MockAttributeDefinition("attr1", AttributeDefinition.STRING, 0, null));
        ocd.addAttributeDefinition(new MockAttributeDefinition("attr2", AttributeDefinition.STRING, 0, null));
        ocd.setAlias("top");

        MockBundle bundle = new MockBundle();
        MockMetaTypeInformation metatype = new MockMetaTypeInformation(bundle);
        metatype.add(topPid, true, ocd);

        MetaTypeRegistry registry = new MetaTypeRegistry();
        assertFalse("The registry should be updated", registry.addMetaType(metatype).isEmpty());

        changeLocationSettings("default");

        String xml = "<server>" +
                     "    <top attr1='top1' attr2='z'/>" +
                     "    <top attr1='top2' attr2='z'/>" +
                     "    <top attr1='top3' attr2='z' id='foo'/>" +
                     "</server>";

        ServerConfiguration serverConfig = configParser.parseServerConfiguration(new StringReader(xml));

        TestConfigEvaluator evaluator = createConfigEvaluator(registry, null);

        RegistryEntry registryEntry = registry.getRegistryEntry(topPid);
        Map<ConfigID, FactoryElement> map = serverConfig.getFactoryInstances(topPid, "top");
        Collection<FactoryElement> elements = map.values();
        assertEquals("There should be three unprocessed elements", 3, elements.size());
        Iterator<FactoryElement> iter = elements.iterator();
        EvaluationResult result = evaluator.evaluate(iter.next(), registryEntry);
        Dictionary<String, Object> properties = result.getProperties();
        assertEquals("topId", properties.get("id"));

        result = evaluator.evaluate(iter.next(), registryEntry);
        properties = result.getProperties();
        assertEquals("topId", properties.get("id"));

        result = evaluator.evaluate(iter.next(), registryEntry);
        properties = result.getProperties();
        assertEquals("topId", properties.get("id"));

        assertTrue("We should get a message that invalid config is being ignored", outputMgr.checkForMessages("CWWKG0103W"));

    }

    private static Set<String> toSet(String... values) {
        Set<String> set = new HashSet<String>();
        for (String value : values) {
            set.add(value);
        }
        return set;
    }
}
