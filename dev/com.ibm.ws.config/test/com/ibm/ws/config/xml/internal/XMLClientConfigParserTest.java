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
import static org.junit.Assert.assertNotNull;

import java.io.StringReader;
import java.util.Dictionary;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.ibm.websphere.config.ConfigEvaluatorException;
import com.ibm.ws.config.xml.internal.variables.ConfigVariableRegistry;
import com.ibm.ws.kernel.service.location.internal.VariableRegistryHelper;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.location.WsResource;

import test.common.SharedLocationManager;
import test.common.SharedOutputManager;
import test.utils.SharedConstants;

public class XMLClientConfigParserTest {
    final static String CONFIG_ROOT = "${server.config.dir}/client.xml";

    static WsLocationAdmin wsLocation;
    static XMLConfigParser configParser;
    static ConfigVariableRegistry variableRegistry;

    static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("*=all");
    @Rule
    public TestRule outputRule = outputMgr;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {

    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
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
        SharedLocationManager.createDefaultLocations(SharedConstants.SERVER_XML_INSTALL_ROOT, profileName, null, true);
        wsLocation = (WsLocationAdmin) SharedLocationManager.getLocationInstance();

        configParser = new XMLConfigParser(wsLocation, variableRegistry);
        variableRegistry = new ConfigVariableRegistry(new VariableRegistryHelper(), new String[0], null, wsLocation);
    }

    private Dictionary<String, Object> evaluateToDictionary(ConfigElement entry) throws ConfigEvaluatorException {
        assertNotNull("the config element should not be null", entry);

        TestConfigEvaluator evaluator = new TestConfigEvaluator(null, null, variableRegistry, wsLocation);
        return evaluator.evaluateToDictionary(entry);
    }

    @Test
    public void testClientConfigRoot() throws Exception {
        // When the process type is client, the root element in client.xml should be <client></client>.
        changeLocationSettings("default");

        WsResource resource = wsLocation.resolveResource(CONFIG_ROOT);
        ServerConfiguration clientConfig = configParser.parseServerConfiguration(resource);

        assertNotNull("the clientConfig should not be null", clientConfig);
    }

    @Test
    public void testServerRootInsideClient() throws Exception {
        // <client><server></server></client> is allowed.
        changeLocationSettings("default");

        ServerConfiguration clientConfig = configParser.parseServerConfiguration(new StringReader("<client><server></server></client>"));

        assertNotNull("the clientConfig should not be null", clientConfig);
    }

    @Test
    public void testClientConfigRootError() throws Exception {
        // When the process type is client, the root element in client.xml may contain <server></server>.
        // This case occurs during framework start where logging bundle is loaded of which defaultInstances.xml
        // that contains <server></server> is parsed.
        changeLocationSettings("defaultInstances");

        WsResource resource = wsLocation.resolveResource(CONFIG_ROOT);
        ServerConfiguration clientConfig = configParser.parseServerConfiguration(resource);

        assertNotNull("the clientConfig should not be null", clientConfig);
    }

    @Test
    public void testOptionalImport() throws Exception {
        changeLocationSettings("default");

        // optionally import file that does not exist
        ServerConfiguration clientConfig = configParser.parseServerConfiguration(new StringReader("<client><include optional=\"true\" location=\"doesNotExist.xml\"/><foo bar=\"test\"/></client>"));

        ConfigElement applied = clientConfig.getSingleton("foo", null);

        Dictionary<String, Object> map = evaluateToDictionary(applied);

        assertEquals("test", map.get("bar"));
    }

    @Test
    public void testDescription() throws Exception {
        changeLocationSettings("default");

        String description = "My Client";

        ServerConfiguration clientConfig = configParser.parseServerConfiguration(new StringReader("<client description=\"" + description + "\"><foo a=\"b\"/></client>"));

        assertEquals(description, clientConfig.getDescription());
    }

}
