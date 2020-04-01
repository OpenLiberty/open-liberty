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
package com.ibm.ws.config.xml.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.service.metatype.AttributeDefinition;

import com.ibm.ws.config.admin.ConfigID;
import com.ibm.ws.config.xml.internal.ConfigEvaluator.EvaluationResult;
import com.ibm.ws.kernel.service.location.internal.VariableRegistryHelper;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.location.WsResource;

import test.common.SharedLocationManager;
import test.common.SharedOutputManager;
import test.utils.SharedConstants;

/**
 *
 */
public class ChildAliasTest {

    static WsLocationAdmin wsLocation;
    static XMLConfigParser configParser;
    static SharedOutputManager outputMgr;
    static ConfigVariableRegistry variableRegistry;
    final static String CONFIG_ROOT = "${server.config.dir}/server.xml";

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // make stdout/stderr "quiet"-- no output will show up for test
        // unless one of the copy methods or documentThrowable is called
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.captureStreams();

        variableRegistry = new ConfigVariableRegistry(new VariableRegistryHelper(), new String[0], null);

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

        configParser = new XMLConfigParser(wsLocation, variableRegistry);
    }

    @Test
    public void testChildAlias1() throws Exception {
        changeLocationSettings("childalias");

        MetaTypeRegistry registry = new MetaTypeRegistry();
        addParentOne(registry);
        addChildOne(registry);
        addParentTwo(registry);
        addChildTwo(registry);

        WsResource resource = wsLocation.resolveResource(CONFIG_ROOT);

        ServerConfiguration serverConfig = configParser.parseServerConfiguration(resource);

        ConfigElement parent1 = serverConfig.getFactoryInstances("test.config.childalias.parent.1", "testCAParent1").values().iterator().next();
        TestConfigEvaluator eval = new TestConfigEvaluator(null, registry, variableRegistry);
        EvaluationResult result = eval.evaluate(parent1, registry.getRegistryEntryByPidOrAlias("testCAParent1"));
        assertEquals(1, result.getNested().values().size());

        EvaluationResult nested = result.getNested().values().iterator().next();
        ConfigElement child = nested.getConfigElement();
        ConfigID child1ID = new ConfigID(parent1.getConfigID(), "test.config.childalias.child.1", "default-0", "testCAChild");
        assertEquals(child1ID, child.getConfigID());

        ConfigElement parent2 = serverConfig.getFactoryInstances("test.config.childalias.parent.2", "testCAParent2").values().iterator().next();
        result = eval.evaluate(parent2, registry.getRegistryEntryByPidOrAlias("testCAParent2"));
        assertEquals(1, result.getNested().values().size());

        nested = result.getNested().values().iterator().next();
        child = nested.getConfigElement();
        ConfigID child2ID = new ConfigID(parent2.getConfigID(), "test.config.childalias.child.2", "default-0", "testCAChild");
        assertEquals(child2ID, child.getConfigID());
    }

    @Test
    public void testChildAliasSingleton() throws Exception {

        changeLocationSettings("childalias");

        MetaTypeRegistry registry = new MetaTypeRegistry();
        addParentOne(registry);
        addChildOne(registry);
        addParentTwo(registry);
        addChildTwo(registry);
        addParentThree(registry);
        addChildThree(registry);

        WsResource resource = wsLocation.resolveResource(CONFIG_ROOT);

        ServerConfiguration serverConfig = configParser.parseServerConfiguration(resource);

        ConfigElement parent1 = serverConfig.getSingleton("test.config.childalias.parent.3", "testCAParent3");
        TestConfigEvaluator eval = new TestConfigEvaluator(null, registry, variableRegistry);
        EvaluationResult result = eval.evaluate(parent1, registry.getRegistryEntryByPidOrAlias("testCAParent3"));
        assertEquals(1, result.getNested().values().size());

        EvaluationResult nested = result.getNested().values().iterator().next();
        ConfigElement child = nested.getConfigElement();
        assertTrue(child instanceof SingletonElement);
        assertEquals("test.config.childalias.child.3", child.getFullId());

    }

    /**
     * @param registry
     */
    private void addChildThree(MetaTypeRegistry registry) {

        MockObjectClassDefinition child = new MockObjectClassDefinition("test.config.childalias.child.3");
        child.addAttributeDefinition(new MockAttributeDefinition("testAttribute3", AttributeDefinition.STRING, 0, new String[] { "Attribute 3" }));
        child.addAttributeDefinition(new MockAttributeDefinition("defaultAttribute", AttributeDefinition.STRING, 0, new String[] { "coconut" }));
        child.setChildAlias("testCAChild");
        child.setParentPid("test.config.childalias.parent.3");

        addSingletonOCD(child, registry);

    }

    /**
     * @param registry
     */
    private void addParentThree(MetaTypeRegistry registry) {

        MockObjectClassDefinition parent1OCD = new MockObjectClassDefinition("test.config.childalias.parent.3");
        parent1OCD.addAttributeDefinition(new MockAttributeDefinition("testAttribute3", AttributeDefinition.STRING, 0, null));
        parent1OCD.setAlias("testCAParent3");
        parent1OCD.setSupportsExtensions(true);

        addSingletonOCD(parent1OCD, registry);

    }

    /**
     * @param parent1ocd
     * @param registry
     */
    private void addSingletonOCD(MockObjectClassDefinition ocd, MetaTypeRegistry registry) {
        addOCD(ocd, registry, false);
    }

    /**
     * @param registry
     */
    private void addChildTwo(MetaTypeRegistry registry) {
        MockObjectClassDefinition child = new MockObjectClassDefinition("test.config.childalias.child.2");
        child.addAttributeDefinition(new MockAttributeDefinition("color", AttributeDefinition.STRING, 0, new String[] { "blue" }));
        child.addAttributeDefinition(new MockAttributeDefinition("shape", AttributeDefinition.STRING, 0, new String[] { "square" }));
        child.setChildAlias("testCAChild");
        child.setParentPid("test.config.childalias.parent.2");

        addFactoryOCD(child, registry);

    }

    /**
     * @param registry
     */
    private void addParentTwo(MetaTypeRegistry registry) {
        MockObjectClassDefinition parent1OCD = new MockObjectClassDefinition("test.config.childalias.parent.2");
        parent1OCD.addAttributeDefinition(new MockAttributeDefinition("testAttribute1", AttributeDefinition.STRING, 0, null));
        parent1OCD.setAlias("testCAParent2");
        parent1OCD.setSupportsExtensions(true);

        addFactoryOCD(parent1OCD, registry);

    }

    /**
     * @param registry
     */
    private void addChildOne(MetaTypeRegistry registry) {

        MockObjectClassDefinition child = new MockObjectClassDefinition("test.config.childalias.child.1");
        child.addAttributeDefinition(new MockAttributeDefinition("testAttribute1", AttributeDefinition.STRING, 0, null));
        child.addAttributeDefinition(new MockAttributeDefinition("defaultAttribute", AttributeDefinition.STRING, 0, new String[] { "defaultValue" }));
        child.setChildAlias("testCAChild");
        child.setParentPid("test.config.childalias.parent.1");

        addFactoryOCD(child, registry);

    }

    /**
     * @param registry
     * @return
     */
    private MockObjectClassDefinition addParentOne(MetaTypeRegistry registry) {

        MockObjectClassDefinition parent1OCD = new MockObjectClassDefinition("test.config.childalias.parent.1");
        parent1OCD.addAttributeDefinition(new MockAttributeDefinition("testAttribute1", AttributeDefinition.STRING, 0, null));
        parent1OCD.setAlias("testCAParent1");
        parent1OCD.setSupportsExtensions(true);

        addFactoryOCD(parent1OCD, registry);

        return parent1OCD;
    }

    /**
     * @param parent1ocd
     * @param registry
     */
    private void addFactoryOCD(MockObjectClassDefinition ocd, MetaTypeRegistry registry) {
        addOCD(ocd, registry, true);
    }

    /**
     * @param ocd
     * @param registry
     * @param b
     */
    private void addOCD(MockObjectClassDefinition ocd, MetaTypeRegistry registry, boolean isFactory) {
        MockBundle bundle = new MockBundle();
        MockMetaTypeInformation metatype = new MockMetaTypeInformation(bundle);
        metatype.add(ocd.getID(), isFactory, ocd);

        registry.addMetaType(metatype);

    }

    private class TestConfigEvaluator extends ConfigEvaluator {

        /**
         * @param retriever
         * @param registry
         * @param variableRegistry
         */
        public TestConfigEvaluator(ConfigRetriever retriever, MetaTypeRegistry registry, ConfigVariableRegistry variableRegistry) {
            super(retriever, registry, variableRegistry, new ServerXMLConfiguration(null, wsLocation, null));
        }

        private Map<ConfigID, String> map;

        @Override
        public String getPid(ConfigID configId) {
            System.out.println("get: " + configId);
            if (map == null) {
                return String.valueOf(configId.hashCode());
            } else {
                return map.get(configId);
            }
        }

        @Override
        public String lookupPid(ConfigID configId) {
            System.out.println("lookup: " + configId);
            if (map == null) {
                return String.valueOf(configId.hashCode());
            } else {
                return map.get(configId);
            }
        }

        public void setLookupMap(Map<ConfigID, String> map) {
            this.map = map;
        }
    }
}
