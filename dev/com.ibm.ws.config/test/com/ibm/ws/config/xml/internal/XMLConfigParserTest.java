/*******************************************************************************
 * Copyright (c) 2010, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.config.xml.internal;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.StringReader;
import java.net.URI;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.websphere.config.ConfigEvaluatorException;
import com.ibm.websphere.config.ConfigParserException;
import com.ibm.ws.config.admin.ConfigID;
import com.ibm.ws.kernel.service.location.internal.SymbolRegistry;
import com.ibm.ws.kernel.service.location.internal.VariableRegistryHelper;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.location.WsResource;
import com.ibm.wsspi.kernel.service.utils.PathUtils;

import test.common.SharedLocationManager;
import test.common.SharedOutputManager;
import test.utils.SharedConstants;

public class XMLConfigParserTest {
    final static String CONFIG_ROOT = "${server.config.dir}/server.xml";

    static WsLocationAdmin wsLocation;
    static XMLConfigParser configParser;
    static SharedOutputManager outputMgr;
    static ConfigVariableRegistry variableRegistry;

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

    private Dictionary<String, Object> evaluateToDictionary(ConfigElement entry) throws ConfigEvaluatorException {
        assertNotNull("the config element should not be null", entry);

        TestConfigEvaluator evaluator = new TestConfigEvaluator(null, null, variableRegistry, wsLocation);
        return evaluator.evaluateToDictionary(entry);
    }

    private ConfigElement applyOverrides(ConfigElement overrideConfigElement, ConfigElement configElement) throws ConfigParserException, ConfigMergeException {
        ConfigElement mergedConfigElement = new SimpleElement(configElement);
        mergedConfigElement.override(overrideConfigElement);
        return mergedConfigElement;
    }

    @Test
    public void testNestedServerElement() throws Exception {
        changeLocationSettings("error1");
        WsResource resource = wsLocation.resolveResource(CONFIG_ROOT);

        configParser.parseServerConfiguration(resource);

        // Check that a warning is received for a nested server element
        assertTrue("A warning should be issued", outputMgr.checkForMessages("CWWKG0085W.*"));
    }

    @Test
    public void testSingleton() throws Exception {
        changeLocationSettings("singleton");

        WsResource resource = wsLocation.resolveResource(CONFIG_ROOT);

        ServerConfiguration serverConfig = configParser.parseServerConfiguration(resource);

        ConfigElement bundleConfig = configParser.parseConfigElement(new StringReader("<httpConnector clientAuth=\"false\" logFile=\"none\" logging.enabled=\"false\">"
                                                                                      + "<port>2222</port><auth>saml</auth></httpConnector>"));

        ConfigElement overrideConfig = serverConfig.getSingleton("httpConnector", null);
        ConfigElement applied = applyOverrides(overrideConfig, bundleConfig);

        Dictionary<String, Object> map = evaluateToDictionary(applied);

        assertEquals("false", map.get("logging.enabled"));
        assertNull(map.get("sslVersion"));
        assertArrayEquals(new String[] { "rsa", "dsa" }, (String[]) map.get("ciphers"));
        assertArrayEquals(new String[] { "2222", "8080", "9999" }, (String[]) map.get("port"));
        assertArrayEquals(new String[] { "saml" }, (String[]) map.get("auth"));
        assertEquals(wsLocation.resolveString("${shared.config.dir}/access.log"), PathUtils.normalize((String) map.get("logFile")));
        assertEquals("false", map.get("clientAuth"));
        assertEquals("no", map.get("requireClientAuth"));
        assertEquals("httpConnector", bundleConfig.getDisplayId());
        assertEquals("httpConnector", map.get(XMLConfigConstants.CFG_CONFIG_INSTANCE_DISPLAY_ID));
    }

    @Test
    public void testSingletonWithAlias() throws Exception {
        changeLocationSettings("singleton");

        WsResource resource = wsLocation.resolveResource(CONFIG_ROOT);

        ServerConfiguration serverConfig = configParser.parseServerConfiguration(resource);

        ConfigElement bundleConfig = configParser.parseConfigElement(new StringReader("<httpConnector clientAuth=\"false\" logFile=\"none\" logging.enabled=\"false\">"
                                                                                      + "<port>2222</port><auth>saml</auth></httpConnector>"));

        ConfigElement overrideConfig = serverConfig.getSingleton("httpConnector", "HTTP");
        ConfigElement applied = applyOverrides(overrideConfig, bundleConfig);

        Dictionary<String, Object> map = evaluateToDictionary(applied);

        assertEquals("true", map.get("logging.enabled"));
        assertArrayEquals(new String[] { "sslv2", "sslv3", "tls" }, (String[]) map.get("sslVersion"));
        assertArrayEquals(new String[] { "rsa" }, (String[]) map.get("ciphers"));
        assertArrayEquals(new String[] { "2222", "8080", "9999", "7777" }, (String[]) map.get("port"));
        assertArrayEquals(new String[] { "saml", "openid" }, (String[]) map.get("auth"));
        assertEquals(wsLocation.resolveString("${shared.config.dir}/access.txt"), PathUtils.normalize((String) map.get("logFile")));
        assertEquals("true", map.get("clientAuth")); // override from alias
        assertEquals("yes", map.get("requireClientAuth")); // merged from alias
        assertEquals("httpConnector", applied.getDisplayId());
        assertEquals("httpConnector", map.get("config.displayId"));
    }

    @Test
    public void testSingletonWithDictionary() throws Exception {
        changeLocationSettings("singleton");

        WsResource resource = wsLocation.resolveResource(CONFIG_ROOT);

        ServerConfiguration serverConfig = configParser.parseServerConfiguration(resource);

        ConfigElement bundleConfig = configParser.parseConfigElement(new StringReader("<httpConnector clientAuth=\"false\" logFile=\"none\" logging.enabled=\"false\">"
                                                                                      + "<port>2222</port><auth>saml</auth></httpConnector>"));

        ConfigElement overrideConfig = serverConfig.getSingleton("httpConnector", null);
        ConfigElement applied = applyOverrides(overrideConfig, bundleConfig);

        Dictionary<String, Object> dict = evaluateToDictionary(applied);

        assertEquals("false", dict.get("logging.enabled"));
        assertNull(dict.get("sslVersion"));
        assertArrayEquals(new String[] { "rsa", "dsa" }, (String[]) dict.get("ciphers"));
        assertArrayEquals(new String[] { "2222", "8080", "9999" }, (String[]) dict.get("port"));
        assertArrayEquals(new String[] { "saml" }, (String[]) dict.get("auth"));
        assertEquals(wsLocation.resolveString("${shared.config.dir}/access.log"), PathUtils.normalize((String) dict.get("logFile")));
        assertEquals("false", dict.get("clientAuth"));
        assertEquals("no", dict.get("requireClientAuth"));
    }

    @Test
    public void testFactory() throws Exception {
        changeLocationSettings("factory");

        WsResource resource = wsLocation.resolveResource(CONFIG_ROOT);

        ServerConfiguration serverConfig = configParser.parseServerConfiguration(resource);

        ConfigElement bundleConfig = configParser.parseConfigElement(new StringReader("<threadpool daemon=\"false\" timeout=\"-1\" minThreads=\"0\" maxThreads=\"100\"/>"));

        ConfigElement override;
        SimpleElement instance;
        ConfigElement entry;
        Dictionary<String, Object> map;

        instance = new SimpleElement(bundleConfig);
        instance.setId("webPool");
        override = serverConfig.getFactoryInstance("threadpool", null, "webPool");
        entry = applyOverrides(override, instance);
        map = evaluateToDictionary(entry);

        assertEquals("100", map.get("maxThreads"));
        assertEquals("0", map.get("minThreads"));
        assertEquals("true", map.get("daemon"));
        assertEquals("-1", map.get("timeout"));
        assertEquals("5000", map.get("idle"));
        assertEquals("threadpool[webPool]", map.get(XMLConfigConstants.CFG_CONFIG_INSTANCE_DISPLAY_ID));

        instance = new SimpleElement(bundleConfig);
        instance.setId("connectorPool");
        override = serverConfig.getFactoryInstance("threadpool", null, "connectorPool");
        entry = applyOverrides(override, instance);
        map = evaluateToDictionary(entry);

        assertEquals("20", map.get("maxThreads"));
        assertEquals("5", map.get("minThreads"));
        assertEquals("false", map.get("daemon"));
        assertEquals("-1", map.get("timeout"));
        assertNull(map.get("idle"));
        assertEquals("threadpool[connectorPool]", map.get(XMLConfigConstants.CFG_CONFIG_INSTANCE_DISPLAY_ID));

        instance = new SimpleElement(bundleConfig);
        instance.setId("ejbPool");
        override = serverConfig.getFactoryInstance("threadpool", null, "ejbPool");
        entry = applyOverrides(override, instance);
        map = evaluateToDictionary(entry);

        assertEquals("100", map.get("maxThreads"));
        assertEquals("0", map.get("minThreads"));
        assertEquals("false", map.get("daemon"));
        assertEquals("1000", map.get("timeout"));
        assertNull(map.get("idle"));
        assertEquals("threadpool[ejbPool]", map.get(XMLConfigConstants.CFG_CONFIG_INSTANCE_DISPLAY_ID));
    }

    @Test
    public void testFactoryWithAlias() throws Exception {
        changeLocationSettings("factory");

        WsResource resource = wsLocation.resolveResource(CONFIG_ROOT);

        ServerConfiguration serverConfig = configParser.parseServerConfiguration(resource);

        ConfigElement bundleConfig = configParser.parseConfigElement(new StringReader("<threadpool daemon=\"false\" timeout=\"-1\" minThreads=\"0\" maxThreads=\"100\"/>"));

        ConfigElement override;
        SimpleElement instance;
        ConfigElement entry;
        Dictionary<String, Object> map;

        instance = new SimpleElement(bundleConfig);
        instance.setId("webPool");
        override = serverConfig.getFactoryInstance("threadpool", "TP", "webPool");
        entry = applyOverrides(override, instance);
        map = evaluateToDictionary(entry);

        assertEquals("10", map.get("maxThreads")); // from alias
        assertEquals("0", map.get("minThreads"));
        assertEquals("true", map.get("daemon"));
        assertEquals("-1", map.get("timeout"));
        assertEquals("8000", map.get("idle")); // from alias
        assertEquals("threadpool[webPool]", map.get(XMLConfigConstants.CFG_CONFIG_INSTANCE_DISPLAY_ID));

        instance = new SimpleElement(bundleConfig);
        instance.setId("connectorPool");
        override = serverConfig.getFactoryInstance("threadpool", "TP", "connectorPool");
        entry = applyOverrides(override, instance);
        map = evaluateToDictionary(entry);

        assertEquals("20", map.get("maxThreads"));
        assertEquals("5", map.get("minThreads"));
        assertEquals("false", map.get("daemon"));
        assertEquals("-1", map.get("timeout"));
        assertNull(map.get("idle"));
        assertEquals("threadpool[connectorPool]", map.get(XMLConfigConstants.CFG_CONFIG_INSTANCE_DISPLAY_ID));

        instance = new SimpleElement(bundleConfig);
        instance.setId("ejbPool");
        override = serverConfig.getFactoryInstance("threadpool", "TP", "ejbPool");
        entry = applyOverrides(override, instance);
        map = evaluateToDictionary(entry);

        assertEquals("100", map.get("maxThreads"));
        assertEquals("0", map.get("minThreads"));
        assertEquals("false", map.get("daemon"));
        assertEquals("1000", map.get("timeout"));
        assertNull(map.get("idle"));
        assertEquals("threadpool[ejbPool]", map.get(XMLConfigConstants.CFG_CONFIG_INSTANCE_DISPLAY_ID));
    }

    @Test
    public void testListConfigurations() throws Exception {
        changeLocationSettings("factory");

        WsResource resource = wsLocation.resolveResource(CONFIG_ROOT);

        ServerConfiguration serverConfig = configParser.parseServerConfiguration(resource);

        Map<String, Dictionary<String, Object>> configs = new HashMap<String, Dictionary<String, Object>>();

        for (String name : serverConfig.getSingletonNames()) {
            ConfigElement configElement = serverConfig.getSingleton(name, null);
            configs.put(configElement.getFullId(), evaluateToDictionary(configElement));
        }

        for (String name : serverConfig.getFactoryNames()) {
            Map<ConfigID, FactoryElement> map = serverConfig.getFactoryInstances(name, null);
            for (FactoryElement configElement : map.values()) {
                configs.put(configElement.getFullId(), evaluateToDictionary(configElement));
            }
        }

        Dictionary<String, Object> map;

        map = configs.get("httpConnector");
        assertNotNull(map);
        assertArrayEquals(new String[] { "rsa", "dsa" }, (String[]) map.get("ciphers"));
        assertArrayEquals(new String[] { "8080", "9999" }, (String[]) map.get("port"));
        assertEquals(wsLocation.resolveString("${shared.config.dir}/access.log"), PathUtils.normalize((String) map.get("logFile")));

        map = configs.get("threadpool[webPool]");
        assertNotNull(map);
        assertEquals("webPool", map.get(XMLConfigConstants.CFG_INSTANCE_ID));
        assertEquals("true", map.get("daemon"));
        assertEquals("5000", map.get("idle"));

        map = configs.get("threadpool[ejbPool]");
        assertNotNull(map);
        assertEquals("ejbPool", map.get(XMLConfigConstants.CFG_INSTANCE_ID));
        assertEquals("1000", map.get("timeout"));

        map = configs.get("threadpool[connectorPool]");
        assertNotNull(map);
        assertEquals("connectorPool", map.get(XMLConfigConstants.CFG_INSTANCE_ID));
        assertEquals("5", map.get("minThreads"));
        assertEquals("20", map.get("maxThreads"));
    }

    @Test
    public void testVariables() throws Exception {
        changeLocationSettings("variables");

        WsResource resource = wsLocation.resolveResource(CONFIG_ROOT);

        ServerConfiguration serverConfig = configParser.parseServerConfiguration(resource);
        variableRegistry.updateSystemVariables(serverConfig.getVariables());

        ConfigElement bundleConfig = configParser.parseConfigElement(new StringReader("<serverInfo jdkVersion=\"1.5\"/>"));

        ConfigElement overrideConfig = serverConfig.getSingleton("serverInfo", null);
        ConfigElement applied = applyOverrides(overrideConfig, bundleConfig);

        Dictionary<String, Object> map = evaluateToDictionary(applied);

        assertEquals("1.0", map.get("minVersion"));
        assertEquals("1.0.9", map.get("maxVersion"));
        assertArrayEquals(new String[] { "1.0", "1.0.9", "1.0 - 1.0.9", "1.5", "999" }, (String[]) map.get("supportedVersions"));
        assertEquals(wsLocation.resolveString("${shared.resource.dir}/version.info"), PathUtils.normalize((String) map.get("versionFile")));
        assertEquals("v1, v2, v3\\,v4, v\\\\5", map.get("single"));
    }

    @Test
    public void testVariablesError() throws Exception {
        changeLocationSettings("default");

        ServerConfiguration serverConfig = null;

        // use a variable with the name attribute misspelled
        try {
            serverConfig = new ServerConfiguration();
            configParser.parseServerConfiguration(new StringReader("<server><variable nam=\"x\" value=\"y\"/><foo bar=\"test\"/></server>"), serverConfig);
            fail("Did not throw expected exception");
        } catch (ConfigParserTolerableException e) {
            // ignore
        }

        assertNotNull("server config shouldn't be null after parsing with a variable having it's name attribute misspelled", serverConfig);

        // verify that the rest of the server.xml gets parsed
        ConfigElement applied = serverConfig.getSingleton("foo", null);
        Dictionary<String, Object> map = evaluateToDictionary(applied);
        assertEquals("test", map.get("bar"));

        // use a variable with the value attribute misspelled
        try {
            serverConfig = new ServerConfiguration();
            configParser.parseServerConfiguration(new StringReader("<server><variable name=\"x\" valu=\"y\"/><foo bar=\"test\"/></server>"), serverConfig);
            fail("Did not throw expected exception");
        } catch (ConfigParserTolerableException e) {
            // ignore
        }

        assertNotNull("server config shouldn't be null after parsing with a variable having it's value attribute misspelled", serverConfig);

        // verify that the rest of the server.xml gets parsed
        applied = serverConfig.getSingleton("foo", null);
        map = evaluateToDictionary(applied);
        assertEquals("test", map.get("bar"));
    }

    @Test
    public void testImportError() throws Exception {
        changeLocationSettings("default");

        ServerConfiguration serverConfig = null;

        // import file that does not exist
        try {
            serverConfig = new ServerConfiguration();
            configParser.parseServerConfiguration(new StringReader("<server><include location=\"doesNotExist.xml\"/><foo bar=\"test\"/></server>"), serverConfig);
            fail("Did not throw expected exception");
        } catch (ConfigParserTolerableException e) {
            // ignore
        }

        assertNotNull("server config shouldn't be null after parsing with a location that doesn't exist", serverConfig);

        // verify that the rest of the server.xml gets parsed
        ConfigElement applied = serverConfig.getSingleton("foo", null);
        Dictionary<String, Object> map = evaluateToDictionary(applied);
        assertEquals("test", map.get("bar"));

        // misspell location in the include tag
        try {
            serverConfig = new ServerConfiguration();
            configParser.parseServerConfiguration(new StringReader("<server><include locati=\"doesNotExist.xml\"/><foo bar=\"test\"/></server>"), serverConfig);
            fail("Did not throw expected exception");
        } catch (ConfigParserTolerableException e) {
            // ignore
        }

        // env variable that doesn't resolve in the location in an optional include
        try {
            serverConfig = new ServerConfiguration();
            configParser.parseServerConfiguration(new StringReader("<server><include optional=\"true\" location=\"${env.NOHERE}-doesNotExist.xml\"/><foo bar=\"test\"/></server>"),
                                                  serverConfig);

        } catch (ConfigParserTolerableException e) {
            fail("We should handle this with a WARNING, not exception!");
        }

        assertNotNull("server config shouldn't be null after parsing with a misspelled attribute", serverConfig);

        // verify that the rest of the server.xml gets parsed
        applied = serverConfig.getSingleton("foo", null);
        map = evaluateToDictionary(applied);
        assertEquals("test", map.get("bar"));
    }

    @Test
    public void testNestedImportError() throws Exception {
        changeLocationSettings("nested.import");

        WsResource resource = wsLocation.resolveResource(CONFIG_ROOT);
        ServerConfiguration serverConfig = null;
        try {
            serverConfig = new ServerConfiguration();
            configParser.parseServerConfiguration(resource, serverConfig);
            fail("Did not throw expected exception");
        } catch (ConfigParserTolerableException e) {
            // ignore
        }

        assertNotNull("server config shouldn't be null after parsing", serverConfig);

        // verify that server.xml gets parsed before the good import
        ConfigElement applied = serverConfig.getSingleton("foo1", null);
        Dictionary<String, Object> map = evaluateToDictionary(applied);
        assertEquals("test1", map.get("bar"));

        // verify that server.xml gets parsed after the good import
        applied = serverConfig.getSingleton("foo2", null);
        map = evaluateToDictionary(applied);
        assertEquals("test2", map.get("bar"));

        // verify that good-server.xml gets parsed before the bad import
        applied = serverConfig.getSingleton("foo3", null);
        map = evaluateToDictionary(applied);
        assertEquals("test3", map.get("bar"));

        // verify that good-server.xml gets parsed after the bad import
        applied = serverConfig.getSingleton("foo4", null);
        map = evaluateToDictionary(applied);
        assertEquals("test4", map.get("bar"));
    }

    @Test
    public void testVariableImport() throws Exception {
        changeLocationSettings("variable.import");

        WsResource resource = wsLocation.resolveResource(CONFIG_ROOT);
        ServerConfiguration serverConfig = null;

        // Run tests twice to test for variable registry contamination
        for (int i = 0; i < 2; i++) {
            serverConfig = new ServerConfiguration();
            configParser.parseServerConfiguration(resource, serverConfig);

            assertNotNull("server config should not be null after parsing", serverConfig);

            // Verify server.xml
            ConfigElement applied = serverConfig.getSingleton("foo1", null);
            Dictionary<String, Object> map = evaluateToDictionary(applied);
            assertEquals("test1", map.get("bar"));

            // verify that server.xml gets parsed after the good import
            applied = serverConfig.getSingleton("foo2", null);
            map = evaluateToDictionary(applied);
            assertEquals("test2", map.get("bar"));

            // verify that good-import.xml gets parsed
            applied = serverConfig.getSingleton("foo3", null);
            map = evaluateToDictionary(applied);
            assertEquals("test3", map.get("bar"));

            // verify that good-import.xml gets parsed after the nested import
            applied = serverConfig.getSingleton("foo4", null);
            map = evaluateToDictionary(applied);
            assertEquals("test4", map.get("bar"));

            // verify that nested-good-import gets parsed
            applied = serverConfig.getSingleton("foo5", null);
            map = evaluateToDictionary(applied);
            assertEquals("test5", map.get("bar"));
        }

    }

    @Test
    public void testOptionalImport() throws Exception {
        changeLocationSettings("default");

        // optionally import file that does not exist
        ServerConfiguration serverConfig = configParser.parseServerConfiguration(new StringReader("<server><include optional=\"true\" location=\"doesNotExist.xml\"/><foo bar=\"test\"/></server>"));

        ConfigElement applied = serverConfig.getSingleton("foo", null);

        Dictionary<String, Object> map = evaluateToDictionary(applied);

        assertEquals("test", map.get("bar"));
    }

    @Test
    public void testErrors() throws Exception {
        changeLocationSettings("default");

        // cannot specify the same property using attribute and elements
        try {
            configParser.parseConfigElement(new StringReader("<serverInfo name=\"a\"><name>b</name></serverInfo>"));
            fail("Did not throw expected exception");
        } catch (ConfigParserException e) {
            // ignore
        }

        ServerConfiguration serverConfig = null;

        // some malformed xml input
        try {
            serverConfig = new ServerConfiguration();
            configParser.parseServerConfiguration(new StringReader("<server><foo1 bar1=\"test1\"/><<foo bar=\"test\"/></server>"), serverConfig);
            fail("Did not throw expected exception");
        } catch (ConfigParserException e) {
            // ignore
        }

        // verify everything before the malformed xml gets parsed
        ConfigElement applied = serverConfig.getSingleton("foo1", null);
        Dictionary<String, Object> map = evaluateToDictionary(applied);
        assertEquals("test1", map.get("bar1"));

        // verify that nothing after the malformed xml gets parsed into the server config
        assertNull(serverConfig.getSingleton("foo", null));
    }

    @Test
    public void testDescription() throws Exception {
        changeLocationSettings("default");

        String description = "My Server";

        ServerConfiguration serverConfig = configParser.parseServerConfiguration(new StringReader("<server description=\"" + description + "\"><foo a=\"b\"/></server>"));

        assertEquals(description, serverConfig.getDescription());
    }

    @Test
    public void testRemoteIncludeResolution() throws Exception {
        changeLocationSettings("default");

        String base;
        WsResource resource;

        // test absolute
        base = CONFIG_ROOT;

        Map<String, ConfigVariable> vars = Collections.emptyMap();
        XMLConfigParser parser = new XMLConfigParser(wsLocation, variableRegistry);
        resource = parser.resolveInclude("http://localhost/xml/server.xml", base, wsLocation);
        assertEquals(new URI("http://localhost/xml/server.xml"), resource.toExternalURI());

        // test absolute as variable
        SymbolRegistry.getRegistry().addStringSymbol("absolute.import", "https://localhost:8080/server-config.xml");
        resource = parser.resolveInclude("${absolute.import}", base, wsLocation);
        assertEquals(new URI("https://localhost:8080/server-config.xml"), resource.toExternalURI());

        // test relative
        base = "http://localhost/xml/server.xml";

        resource = parser.resolveInclude("a.xml", base, wsLocation);
        assertEquals(new URI("http://localhost/xml/a.xml"), resource.toExternalURI());

        resource = parser.resolveInclude("../b.xml", base, wsLocation);
        assertEquals(new URI("http://localhost/xml/../b.xml"), resource.toExternalURI());

        resource = parser.resolveInclude("common/c.xml", base, wsLocation);
        assertEquals(new URI("http://localhost/xml/common/c.xml"), resource.toExternalURI());

        // test relative as variable
        SymbolRegistry.getRegistry().addStringSymbol("relative.import", "shared/d.xml");
        resource = parser.resolveInclude("${relative.import}", base, wsLocation);
        assertEquals(new URI("http://localhost/xml/shared/d.xml"), resource.toExternalURI());
    }
}
