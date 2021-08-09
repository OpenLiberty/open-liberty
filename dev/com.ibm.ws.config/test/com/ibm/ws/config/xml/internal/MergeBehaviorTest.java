/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
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
import static org.junit.Assert.assertTrue;

import java.io.StringReader;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.websphere.config.ConfigEvaluatorException;
import com.ibm.ws.config.xml.internal.variables.ConfigVariableRegistry;
import com.ibm.ws.kernel.service.location.internal.VariableRegistryHelper;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;

import test.common.SharedLocationManager;
import test.common.SharedOutputManager;
import test.utils.SharedConstants;

/**
 * Test merge behavior on include elements
 */
public class MergeBehaviorTest {

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

    private Dictionary<String, Object> evaluateToDictionary(ConfigElement entry) throws ConfigEvaluatorException {
        assertNotNull("the config element should not be null", entry);

        TestConfigEvaluator evaluator = new TestConfigEvaluator(null, null, variableRegistry, wsLocation);
        return evaluator.evaluateToDictionary(entry);
    }

    private void changeLocationSettings(String profileName) {
        SharedLocationManager.createDefaultLocations(SharedConstants.SERVER_XML_INSTALL_ROOT, profileName);
        wsLocation = (WsLocationAdmin) SharedLocationManager.getLocationInstance();

        variableRegistry = new ConfigVariableRegistry(new VariableRegistryHelper(), new String[0], null, wsLocation);
        configParser = new XMLConfigParser(wsLocation, variableRegistry);

    }

    @Test
    public void testImportNotSpecifiedSingleValue() throws Exception {
        changeLocationSettings("default");

        // Import include.xml without a merge behavior, then specify an element
        // The top level config element will be used because it comes last.
        ServerConfiguration serverConfig = configParser.parseServerConfiguration(new StringReader("<server><include location=\"${shared.config.dir}/include.xml\"/><foo bar=\"test\"/></server>"));

        ConfigElement applied = serverConfig.getSingleton("foo", null);

        Dictionary<String, Object> map = evaluateToDictionary(applied);

        assertEquals("test", map.get("bar"));
    }

    @Test
    public void testImportNotSpecifiedSingleValue2() throws Exception {
        changeLocationSettings("default");

        // Import include.xml which contains <foo bar="include"/>.. Include will be used because it comes last
        ServerConfiguration serverConfig = configParser.parseServerConfiguration(new StringReader("<server><foo bar=\"test\"/><include location=\"${shared.config.dir}/include.xml\"/></server>"));

        ConfigElement applied = serverConfig.getSingleton("foo", null);

        Dictionary<String, Object> map = evaluateToDictionary(applied);

        assertEquals("include", map.get("bar"));
    }

    @Test
    public void testImportNotSpecifiedListValue() throws Exception {
        changeLocationSettings("default");

        // Import include.xml without a merge behavior, then specify an element.
        // The elements will be merged.
        ServerConfiguration serverConfig = configParser.parseServerConfiguration(new StringReader("<server><include location=\"${shared.config.dir}/include.xml\"/><foo><list>test1</list><list>test2</list></foo></server>"));

        ConfigElement applied = serverConfig.getSingleton("foo", null);

        Dictionary<String, Object> map = evaluateToDictionary(applied);

        String[] list = (String[]) map.get("list");
        assertEquals(3, list.length);

        List<String> values = Arrays.asList(list);
        assertTrue(values.contains("test1"));
        assertTrue(values.contains("test2"));
        assertTrue(values.contains("1"));
    }

    @Test
    public void testImportNotSpecifiedListValue2() throws Exception {
        changeLocationSettings("default");

        // Specify an element, then import include.xml
        // The elements will be merged
        ServerConfiguration serverConfig = configParser.parseServerConfiguration(new StringReader("<server><foo><list>test1</list><list>test2</list></foo><include location=\"${shared.config.dir}/include.xml\"/></server>"));

        ConfigElement applied = serverConfig.getSingleton("foo", null);

        Dictionary<String, Object> map = evaluateToDictionary(applied);

        String[] list = (String[]) map.get("list");
        assertEquals(3, list.length);

        List<String> values = Arrays.asList(list);
        assertTrue(values.contains("test1"));
        assertTrue(values.contains("test2"));
        assertTrue(values.contains("1"));
    }

    @Test
    public void testImportMergeListValue() throws Exception {
        changeLocationSettings("default");

        // Import include.xml with a merge behavior of "merge", then specify an element
        // The elements will be merged
        ServerConfiguration serverConfig = configParser.parseServerConfiguration(new StringReader("<server><include onConflict=\"merge\" location=\"${shared.config.dir}/include.xml\"/><foo><list>test1</list><list>test2</list></foo></server>"));

        ConfigElement applied = serverConfig.getSingleton("foo", null);

        Dictionary<String, Object> map = evaluateToDictionary(applied);

        String[] list = (String[]) map.get("list");
        assertEquals(3, list.length);

        List<String> values = Arrays.asList(list);
        assertTrue(values.contains("test1"));
        assertTrue(values.contains("test2"));
        assertTrue(values.contains("1"));
    }

    @Test
    public void testImportMergeListValue2() throws Exception {
        changeLocationSettings("default");

        // Specify an element, then import include.xml with a merge behavior of "merge"
        // The elements will be merged
        ServerConfiguration serverConfig = configParser.parseServerConfiguration(new StringReader("<server><foo><list>test1</list><list>test2</list></foo><include onConflict=\"merge\" location=\"${shared.config.dir}/include.xml\"/></server>"));

        ConfigElement applied = serverConfig.getSingleton("foo", null);

        Dictionary<String, Object> map = evaluateToDictionary(applied);

        String[] list = (String[]) map.get("list");
        assertEquals(3, list.length);

        List<String> values = Arrays.asList(list);
        assertTrue(values.contains("test1"));
        assertTrue(values.contains("test2"));
        assertTrue(values.contains("1"));
    }

    @Test
    public void testImportReplaceListValue() throws Exception {
        changeLocationSettings("default");

        // Import include.xml with a behavior of "replace", then specify an element.
        // The elements will be merged because last behavior wins
        ServerConfiguration serverConfig = configParser.parseServerConfiguration(new StringReader("<server><include onConflict=\"replace\" location=\"${shared.config.dir}/include.xml\"/><foo><list>test1</list><list>test2</list></foo></server>"));

        ConfigElement applied = serverConfig.getSingleton("foo", null);

        Dictionary<String, Object> map = evaluateToDictionary(applied);

        String[] list = (String[]) map.get("list");
        assertEquals(3, list.length);

        List<String> values = Arrays.asList(list);
        assertTrue(values.contains("test1"));
        assertTrue(values.contains("test2"));
        assertTrue(values.contains("1"));
    }

    @Test
    public void testImportReplaceListValue2() throws Exception {
        changeLocationSettings("default");

        // Specify an element and then import include.xml with a behavior of "replace"
        // The include.xml element will be used because it replaces the existing element
        ServerConfiguration serverConfig = configParser.parseServerConfiguration(new StringReader("<server><foo><list>test1</list><list>test2</list></foo><include onConflict=\"replace\" location=\"${shared.config.dir}/include.xml\"/></server>"));

        ConfigElement applied = serverConfig.getSingleton("foo", null);

        Dictionary<String, Object> map = evaluateToDictionary(applied);

        String[] list = (String[]) map.get("list");
        assertEquals(1, list.length);

        List<String> values = Arrays.asList(list);
        assertTrue(values.contains("1"));
    }

    @Test
    public void testImportIgnoreListValue() throws Exception {
        changeLocationSettings("default");

        // Import with "Ignore", then specify an element
        // The elements are merged because the behavior on the element wins
        ServerConfiguration serverConfig = configParser.parseServerConfiguration(new StringReader("<server><include onConflict=\"ignore\" location=\"${shared.config.dir}/include.xml\"/><foo><list>test1</list><list>test2</list></foo></server>"));

        ConfigElement applied = serverConfig.getSingleton("foo", null);

        Dictionary<String, Object> map = evaluateToDictionary(applied);

        String[] list = (String[]) map.get("list");
        assertEquals(3, list.length);

        List<String> values = Arrays.asList(list);
        assertTrue(values.contains("test1"));
        assertTrue(values.contains("test2"));
        assertTrue(values.contains("1"));
    }

    @Test
    public void testImportIgnoreListValue2() throws Exception {
        changeLocationSettings("default");

        // Specify an element, import with "ignore"
        // The included element is ignored
        ServerConfiguration serverConfig = configParser.parseServerConfiguration(new StringReader("<server><foo><list>test1</list><list>test2</list></foo><include onConflict=\"ignore\" location=\"${shared.config.dir}/include.xml\"/></server>"));

        ConfigElement applied = serverConfig.getSingleton("foo", null);

        Dictionary<String, Object> map = evaluateToDictionary(applied);

        String[] list = (String[]) map.get("list");
        assertEquals(2, list.length);

        List<String> values = Arrays.asList(list);
        assertTrue(values.contains("test1"));
        assertTrue(values.contains("test2"));

    }

}
