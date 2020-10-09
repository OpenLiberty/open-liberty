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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.StringReader;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.websphere.config.ConfigEvaluatorException;
import com.ibm.ws.config.admin.ConfigID;
import com.ibm.ws.config.xml.LibertyVariable;
import com.ibm.ws.config.xml.internal.XMLConfigParser.MergeBehavior;
import com.ibm.ws.config.xml.internal.variables.ConfigVariable;
import com.ibm.ws.config.xml.internal.variables.ConfigVariableRegistry;
import com.ibm.ws.kernel.service.location.internal.VariableRegistryHelper;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;

import test.common.SharedLocationManager;
import test.common.SharedOutputManager;
import test.utils.SharedConstants;

public class ConfigurationTest {
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

        ConfigVariableRegistry variableRegistry = new ConfigVariableRegistry(new VariableRegistryHelper(), new String[0], null, wsLocation);

        configParser = new XMLConfigParser(wsLocation, variableRegistry);
    }

    private Dictionary<String, Object> evaluateToDictionary(ConfigElement entry) throws ConfigEvaluatorException {
        TestConfigEvaluator evaluator = new TestConfigEvaluator(null, null, null, wsLocation);
        return evaluator.evaluateToDictionary(entry);
    }

    @Test
    public void testDefaultSingleton() throws Exception {
        changeLocationSettings("default");

        ServerConfiguration serverConfig;
        ConfigElement config;
        Dictionary<String, Object> map;

        BaseConfiguration defaultConfig = configParser.parseDefaultConfiguration(new StringReader("<server>" +
                                                                                                  "  <host port=\"1234\" />" +
                                                                                                  "</server>"),
                                                                                 "test");

        serverConfig = configParser.parseServerConfiguration(new StringReader("<server>" +
                                                                              "  <host name=\"localhost\" />" +
                                                                              "</server>"));

        // test without default
        config = serverConfig.getSingleton("host", null);
        map = evaluateToDictionary(config);
        assertEquals("localhost", map.get("name"));
        assertNull(map.get("port"));

        // test with default
        serverConfig.setDefaultConfiguration(defaultConfig);

        config = serverConfig.getSingleton("host", null);
        map = evaluateToDictionary(config);
        assertEquals("localhost", map.get("name"));
        assertEquals("1234", map.get("port"));
    }

    @Test
    public void testDefaultFactory() throws Exception {
        changeLocationSettings("default");

        ServerConfiguration serverConfig;
        ConfigElement config;
        Dictionary<String, Object> map;

        BaseConfiguration defaultConfig = configParser.parseDefaultConfiguration(new StringReader("<server>" +
                                                                                                  "  <host id=\"one\" port=\"1234\"/>" +
                                                                                                  "  <host id=\"two\" port=\"5678\"/>" +
                                                                                                  "</server>"),
                                                                                 "test");

        serverConfig = configParser.parseServerConfiguration(new StringReader("<server>" +
                                                                              "  <host id=\"one\" port=\"9999\" name=\"localhost\" />" +
                                                                              "</server>"));

        // test without default
        config = serverConfig.getFactoryInstance("host", null, "one");
        map = evaluateToDictionary(config);
        assertEquals("localhost", map.get("name"));
        assertEquals("9999", map.get("port"));
        config = serverConfig.getFactoryInstance("host", null, "two");
        assertNull(config);

        // test with default
        serverConfig.setDefaultConfiguration(defaultConfig);

        config = serverConfig.getFactoryInstance("host", null, "one");
        map = evaluateToDictionary(config);
        assertEquals("localhost", map.get("name"));
        assertEquals("9999", map.get("port"));
        config = serverConfig.getFactoryInstance("host", null, "two");
        map = evaluateToDictionary(config);
        assertNull(map.get("name"));
        assertEquals("5678", map.get("port"));
    }

    @Test
    public void testDefaultFactories() throws Exception {
        changeLocationSettings("default");

        ServerConfiguration serverConfig;
        Dictionary<String, Object> map;
        Map<ConfigID, FactoryElement> configs;

        BaseConfiguration defaultConfig = configParser.parseDefaultConfiguration(new StringReader("<server>" +
                                                                                                  "  <host id=\"one\" port=\"1234\"/>" +
                                                                                                  "  <host id=\"two\" port=\"5678\"/>" +
                                                                                                  "</server>"),
                                                                                 "test");

        serverConfig = configParser.parseServerConfiguration(new StringReader("<server>" +
                                                                              "  <host id=\"one\" name=\"localhost\" />" +
                                                                              "</server>"));

        // test without default
        configs = serverConfig.getFactoryInstances("host", null);
        map = evaluateToDictionary(configs.get(new ConfigID("host", "one")));
        assertEquals("localhost", map.get("name"));
        assertNull(map.get("port"));
        assertNull(configs.get(new ConfigID("host", "two")));

        // test with default
        serverConfig.setDefaultConfiguration(defaultConfig);

        configs = serverConfig.getFactoryInstances("host", null);
        map = evaluateToDictionary(configs.get(new ConfigID("host", "one")));
        assertEquals("localhost", map.get("name"));
        assertEquals("1234", map.get("port"));
        map = evaluateToDictionary(configs.get(new ConfigID("host", "two")));
        assertNull(map.get("name"));
        assertEquals("5678", map.get("port"));
    }

    @Test
    public void testNames() throws Exception {
        changeLocationSettings("default");

        ServerConfiguration serverConfig;

        BaseConfiguration defaultConfig = configParser.parseDefaultConfiguration(new StringReader("<server>" +
                                                                                                  "  <host id=\"one\" port=\"1234\"/> " +
                                                                                                  "  <host id=\"two\" port=\"5678\"/>" +
                                                                                                  "  <port value=\"\123\"/>" +
                                                                                                  "</server>"),
                                                                                 "test");

        serverConfig = configParser.parseServerConfiguration(new StringReader("<server>" +
                                                                              "  <virtualHost id=\"hostOne\" name=\"localhost\" />" +
                                                                              "  <properties driver=\"derby\" />" +
                                                                              "  <host id=\"one\" name=\"localhost\"/>" +
                                                                              "</server>"));

        // test without defaults
        assertEquals(toSet("virtualHost", "properties", "host"), serverConfig.getConfigurationNames());
        assertEquals(toSet("properties"), serverConfig.getSingletonNames());
        assertEquals(toSet("virtualHost", "host"), serverConfig.getFactoryNames());

        // test with default
        serverConfig.setDefaultConfiguration(defaultConfig);
        assertEquals(toSet("virtualHost", "properties", "host", "port"), serverConfig.getConfigurationNames());
        assertEquals(toSet("properties", "port"), serverConfig.getSingletonNames());
        assertEquals(toSet("virtualHost", "host"), serverConfig.getFactoryNames());
    }

    @Test
    public void testAddRemove() throws Exception {
        changeLocationSettings("default");

        ServerConfiguration serverConfig;
        ConfigElement config;
        Dictionary<String, Object> map;

        ServerConfiguration extraConfig = configParser.parseServerConfiguration(new StringReader("<server>" +
                                                                                                 "  <host id=\"one\" port=\"1234\"/> " +
                                                                                                 "  <host id=\"two\" port=\"5678\"/>" +
                                                                                                 "  <port value=\"123\"/>" +
                                                                                                 "</server>"));

        serverConfig = configParser.parseServerConfiguration(new StringReader("<server>" +
                                                                              "  <virtualHost id=\"hostOne\" name=\"127.0.0.1\" />" +
                                                                              "  <properties driver=\"derby\" />" +
                                                                              "  <host id=\"one\" name=\"localhost\"/>" +
                                                                              "</server>"));

        // test initial config
        assertEquals(toSet("virtualHost", "properties", "host"), serverConfig.getConfigurationNames());
        assertEquals(toSet("properties"), serverConfig.getSingletonNames());
        assertEquals(toSet("virtualHost", "host"), serverConfig.getFactoryNames());

        config = serverConfig.getFactoryInstance("virtualHost", null, "hostOne");
        map = evaluateToDictionary(config);
        assertEquals("127.0.0.1", map.get("name"));
        config = serverConfig.getFactoryInstance("host", null, "one");
        map = evaluateToDictionary(config);
        assertEquals("localhost", map.get("name"));
        assertNull(map.get("port"));
        config = serverConfig.getFactoryInstance("host", null, "two");
        assertNull(config);
        config = serverConfig.getSingleton("properties", null);
        map = evaluateToDictionary(config);
        assertEquals("derby", map.get("driver"));
        config = serverConfig.getSingleton("port", null);
        assertNull(config);

        // append extra configuration
        serverConfig.add(extraConfig);

        assertEquals(toSet("virtualHost", "properties", "host", "port"), serverConfig.getConfigurationNames());
        assertEquals(toSet("properties", "port"), serverConfig.getSingletonNames());
        assertEquals(toSet("virtualHost", "host"), serverConfig.getFactoryNames());

        config = serverConfig.getFactoryInstance("virtualHost", null, "hostOne");
        map = evaluateToDictionary(config);
        assertEquals("127.0.0.1", map.get("name"));
        config = serverConfig.getFactoryInstance("host", null, "one");
        map = evaluateToDictionary(config);
        assertEquals("localhost", map.get("name"));
        assertEquals("1234", map.get("port"));
        config = serverConfig.getFactoryInstance("host", null, "two");
        map = evaluateToDictionary(config);
        assertEquals("5678", map.get("port"));
        config = serverConfig.getSingleton("properties", null);
        map = evaluateToDictionary(config);
        assertEquals("derby", map.get("driver"));
        config = serverConfig.getSingleton("port", null);
        map = evaluateToDictionary(config);
        assertEquals("123", map.get("value"));

        // remove extra configuration
        serverConfig.remove(extraConfig);

        assertEquals(toSet("virtualHost", "properties", "host"), serverConfig.getConfigurationNames());
        assertEquals(toSet("properties"), serverConfig.getSingletonNames());
        assertEquals(toSet("virtualHost", "host"), serverConfig.getFactoryNames());

        config = serverConfig.getFactoryInstance("virtualHost", null, "hostOne");
        map = evaluateToDictionary(config);
        assertEquals("127.0.0.1", map.get("name"));
        config = serverConfig.getFactoryInstance("host", null, "one");
        map = evaluateToDictionary(config);
        assertEquals("localhost", map.get("name"));
        assertNull(map.get("port"));
        config = serverConfig.getFactoryInstance("host", null, "two");
        assertNull(config);
        config = serverConfig.getSingleton("properties", null);
        map = evaluateToDictionary(config);
        assertEquals("derby", map.get("driver"));
        config = serverConfig.getSingleton("port", null);
        assertNull(config);
    }

    /**
     * Drive getVariables when there have been no variables established.
     *
     * @throws ConfigMergeException
     */
    @Test
    public void getVariables_noVariables() throws ConfigMergeException {
        BaseConfiguration config = new BaseConfiguration();
        assertTrue("FAIL: Map should be empty", config.getVariables().isEmpty());
    }

    /**
     * Drive getVariables when there have been at least some variables established.
     *
     * @throws ConfigMergeException
     */
    @Test
    public void getVariables_populatedVariableList() throws ConfigMergeException {
        BaseConfiguration config = new BaseConfiguration();
        ConfigVariable inVariable = new ConfigVariable("name", "value", null, MergeBehavior.MERGE, "location", false);
        BaseConfiguration in = new BaseConfiguration();
        in.addVariable(inVariable);
        config.add(in);
        assertFalse("FAIL: Map should not be empty", config.getVariables().isEmpty());
    }

    /**
     * Drive getVariables when variables have been established, but were then removed.
     *
     * @throws ConfigMergeException
     */
    @Test
    public void getVariables_emptyVariableList() throws ConfigMergeException {
        BaseConfiguration config = new BaseConfiguration();

        ConfigVariable inVariable = new ConfigVariable("name", "value", null, MergeBehavior.MERGE, "location", false);
        BaseConfiguration in = new BaseConfiguration();
        in.addVariable(inVariable);
        config.add(in);
        config.remove(in);

        assertTrue("FAIL: Map should be empty", config.getVariables().isEmpty());
    }

    @Test
    public void getVariables_conflictReplace() throws ConfigMergeException {
        BaseConfiguration config = new BaseConfiguration();

        ConfigVariable inVariable = new ConfigVariable("name", "value", null, MergeBehavior.REPLACE, "location", false);
        ConfigVariable anotherVariable = new ConfigVariable("name", "value2", null, MergeBehavior.MERGE, "location2", false);
        BaseConfiguration in = new BaseConfiguration();
        in.addVariable(inVariable);
        in.addVariable(anotherVariable);
        config.add(in);

        LibertyVariable name = config.getVariables().get("name");
        assertNotNull(name);

        // Last value should win when onConflict = REPLACE
        assertEquals("The variable value should be 'value2'", "value2", name.getValue());

    }

    @Test
    public void getVariables_conflictReplaceDifferentOrder() throws ConfigMergeException {
        BaseConfiguration config = new BaseConfiguration();

        ConfigVariable inVariable = new ConfigVariable("name", "value", null, MergeBehavior.MERGE, "location", false);
        ConfigVariable anotherVariable = new ConfigVariable("name", "value2", null, MergeBehavior.REPLACE, "location2", false);
        BaseConfiguration in = new BaseConfiguration();
        in.addVariable(inVariable);
        in.addVariable(anotherVariable);
        config.add(in);

        LibertyVariable name = config.getVariables().get("name");
        assertNotNull(name);

        // Last value should win when onConflict = REPLACE
        assertEquals("The variable value should be 'value2'", "value2", name.getValue());

    }

    @Test
    public void getVariables_conflictIgnore() throws ConfigMergeException {
        BaseConfiguration config = new BaseConfiguration();

        ConfigVariable inVariable = new ConfigVariable("name", "value", null, MergeBehavior.IGNORE, "location", false);
        ConfigVariable anotherVariable = new ConfigVariable("name", "value2", null, MergeBehavior.MERGE, "location2", false);
        BaseConfiguration in = new BaseConfiguration();
        in.addVariable(inVariable);
        in.addVariable(anotherVariable);
        config.add(in);

        LibertyVariable name = config.getVariables().get("name");
        assertNotNull(name);

        // Last value will win because its behavior is MERGE
        // <include a.xml onConflict="IGNORE"/>
        // <variable.../>
        assertEquals("The variable value should be 'value2'", "value2", name.getValue());

    }

    @Test
    public void getVariables_conflictIgnoreDifferentOrder() throws ConfigMergeException {
        BaseConfiguration config = new BaseConfiguration();

        ConfigVariable inVariable = new ConfigVariable("name", "value", null, MergeBehavior.MERGE, "location", false);
        ConfigVariable anotherVariable = new ConfigVariable("name", "value2", null, MergeBehavior.IGNORE, "location2", false);
        BaseConfiguration in = new BaseConfiguration();
        in.addVariable(inVariable);
        in.addVariable(anotherVariable);
        config.add(in);

        LibertyVariable name = config.getVariables().get("name");
        assertNotNull(name);

        // First value should win
        // <variable .../>
        // <include a.xml onConflict=IGNORE/>

        assertEquals("The variable value should be 'value'", "value", name.getValue());

    }

    @Test
    public void getObscuredVariables() {
        ConfigVariable obscured = new ConfigVariable("name", "{aes}asdfjkl", null, MergeBehavior.MERGE, "location", false);
        assertEquals("The variable value should be returned", "{aes}asdfjkl", obscured.getValue());
        assertFalse("toString should not show the variable value " + obscured, obscured.toString().contains("{aes}"));

        ConfigVariable intentionallyObscured = new ConfigVariable("name", "hidden", null, MergeBehavior.MERGE, "location", true);
        assertEquals("The variable value should be returned", "hidden", intentionallyObscured.getValue());
        assertFalse("toString should not show the variable value " + obscured, obscured.toString().contains("hidden"));
    }

    private static Set<String> toSet(String... values) {
        Set<String> set = new HashSet<String>();
        for (String value : values) {
            set.add(value);
        }
        return set;
    }
}
