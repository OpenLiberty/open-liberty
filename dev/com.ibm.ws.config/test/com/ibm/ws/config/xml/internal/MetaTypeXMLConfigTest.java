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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.StringReader;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Vector;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.service.metatype.AttributeDefinition;

import com.ibm.websphere.metatype.MetaTypeFactory;
import com.ibm.ws.config.xml.internal.MetaTypeRegistry.RegistryEntry;
import com.ibm.ws.kernel.service.location.internal.VariableRegistryHelper;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.location.WsResource;
import com.ibm.wsspi.kernel.service.utils.PathUtils;
import com.ibm.wsspi.kernel.service.utils.SerializableProtectedString;

import test.common.SharedLocationManager;
import test.common.SharedOutputManager;
import test.utils.SharedConstants;

public class MetaTypeXMLConfigTest {
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

    private TestConfigEvaluator createConfigEvaluator() {
        TestConfigEvaluator evaluator = new TestConfigEvaluator(null, null, variableRegistry, wsLocation);
        return evaluator;
    }

    private RegistryEntry createAttributeMap(int cardinality) {
        MockObjectClassDefinition objectClass = new MockObjectClassDefinition("test");
        objectClass.addAttributeDefinition(new MockAttributeDefinition("testBoolean", AttributeDefinition.BOOLEAN, cardinality));
        objectClass.addAttributeDefinition(new MockAttributeDefinition("testInteger", AttributeDefinition.INTEGER, cardinality));
        objectClass.addAttributeDefinition(new MockAttributeDefinition("testLong", AttributeDefinition.LONG, cardinality));
        objectClass.addAttributeDefinition(new MockAttributeDefinition("testShort", AttributeDefinition.SHORT, cardinality));
        objectClass.addAttributeDefinition(new MockAttributeDefinition("testString", AttributeDefinition.STRING, cardinality));
        objectClass.addAttributeDefinition(new MockAttributeDefinition("testCharacter", AttributeDefinition.CHARACTER, cardinality));
        objectClass.addAttributeDefinition(new MockAttributeDefinition("testByte", AttributeDefinition.BYTE, cardinality));
        objectClass.addAttributeDefinition(new MockAttributeDefinition("testTimeout", MetaTypeFactory.DURATION_TYPE, cardinality), false);
        objectClass.addAttributeDefinition(new MockAttributeDefinition("testToken", MetaTypeFactory.TOKEN_TYPE, cardinality));
        return toRegistryEntry(objectClass);

    }

    private RegistryEntry toRegistryEntry(MockObjectClassDefinition objectClass) {
        MockBundle bundle = new MockBundle();
        MockMetaTypeInformation metatype = new MockMetaTypeInformation(bundle);
        metatype.add("com.ibm.ws.pid", true, objectClass);

        MetaTypeRegistry registry = new MetaTypeRegistry();
        assertFalse("The registry should be updated", registry.addMetaType(metatype).isEmpty());

        return registry.getRegistryEntry("com.ibm.ws.pid");
    }

    @Test
    public void testMetaTypeSimpleTypes() throws Exception {
        changeLocationSettings("default");

        ConfigElement config = configParser.parseConfigElement(new StringReader("<test testTimeout=\"10h\" testBoolean=\"true\" testInteger=\"123\" testLong=\"1234\" testShort=\"12\""
                                                                                + "      testString=\"abc\" testNoMapping=\"def\" testCharacter=\"a\" testByte=\"65\" testToken=\"   hello  \r\nworld \""
                                                                                + "      />"));

        TestConfigEvaluator evaluator = createConfigEvaluator();

        RegistryEntry re = createAttributeMap(0);

        Dictionary<String, Object> dict = evaluator.evaluateToDictionary(config, re);

        assertEquals(Boolean.TRUE, dict.get("testBoolean"));
        assertEquals(new Integer(123), dict.get("testInteger"));
        assertEquals(new Long(1234), dict.get("testLong"));
        assertEquals(new Short((short) 12), dict.get("testShort"));
        assertEquals("abc", dict.get("testString"));
        assertEquals("def", dict.get("testNoMapping"));
        assertEquals(new Character('a'), dict.get("testCharacter"));
        assertEquals(new Byte((byte) 'A'), dict.get("testByte"));
        assertEquals(Long.valueOf(36000000), dict.get("testTimeout"));
        assertEquals("hello world", dict.get("testToken"));
    }

    @Test
    public void testMetaTypeArrayTypes() throws Exception {
        changeLocationSettings("default");

        ConfigElement config = configParser.parseConfigElement(new StringReader("<test>" + "<testBoolean>true</testBoolean><testBoolean>false</testBoolean>"
                                                                                + "<testInteger>123</testInteger><testInteger>345</testInteger><testInteger>678</testInteger>"
                                                                                + "<testLong>1234</testLong><testLong>5678</testLong>"
                                                                                + "<testShort>12</testShort><testShort>34</testShort><testShort>56</testShort>"
                                                                                + "<testString>abc</testString><testString>ghi</testString>"
                                                                                + "<testCharacter>a</testCharacter><testCharacter>b</testCharacter>"
                                                                                + "<testByte>65</testByte><testByte>66</testByte>" +
                                                                                "<testToken>good  bye   </testToken><testToken>   hello </testToken>" + "</test>"));

        TestConfigEvaluator evaluator = createConfigEvaluator();

        RegistryEntry re = createAttributeMap(Integer.MAX_VALUE);

        Dictionary<String, Object> dict = evaluator.evaluateToDictionary(config, re);

        assertTrue(Arrays.equals(new boolean[] { true, false }, (boolean[]) dict.get("testBoolean")));
        assertTrue(Arrays.equals(new int[] { 123, 345, 678 }, (int[]) dict.get("testInteger")));
        assertTrue(Arrays.equals(new long[] { 1234, 5678 }, (long[]) dict.get("testLong")));
        assertTrue(Arrays.equals(new short[] { 12, 34, 56 }, (short[]) dict.get("testShort")));
        assertTrue(Arrays.equals(new String[] { "abc", "ghi" }, (String[]) dict.get("testString")));
        assertTrue(Arrays.equals(new char[] { 'a', 'b' }, (char[]) dict.get("testCharacter")));
        assertTrue(Arrays.equals(new byte[] { 'A', 'B' }, (byte[]) dict.get("testByte")));
        assertTrue(Arrays.equals(new String[] { "good bye", "hello" }, (String[]) dict.get("testToken")));
    }

    @Test
    public void testMetaTypeCollectionTypes() throws Exception {
        changeLocationSettings("default");

        ConfigElement config = configParser.parseConfigElement(new StringReader("<test>" + "<testBoolean>true</testBoolean><testBoolean>false</testBoolean>"
                                                                                + "<testInteger>123</testInteger><testInteger>345</testInteger><testInteger>678</testInteger>"
                                                                                + "<testLong>1234</testLong><testLong>5678</testLong>"
                                                                                + "<testShort>12</testShort><testShort>34</testShort><testShort>56</testShort>"
                                                                                + "<testString>abc</testString><testString>ghi</testString>"
                                                                                + "<testCharacter>a</testCharacter><testCharacter>b</testCharacter>"
                                                                                + "<testByte>65</testByte><testByte>66</testByte>" +
                                                                                " <testToken> a b c </testToken><testToken>  def  </testToken>" + "</test>"));

        TestConfigEvaluator evaluator = createConfigEvaluator();

        RegistryEntry re = createAttributeMap(Integer.MIN_VALUE);

        Dictionary<String, Object> dict = evaluator.evaluateToDictionary(config, re);

        assertEquals(new Vector<Boolean>(Arrays.asList(true, false)), dict.get("testBoolean"));
        assertEquals(new Vector<Integer>(Arrays.asList(123, 345, 678)), dict.get("testInteger"));
        assertEquals(new Vector<Long>(Arrays.asList(1234L, 5678L)), dict.get("testLong"));
        assertEquals(new Vector<Short>(Arrays.asList((short) 12, (short) 34, (short) 56)), dict.get("testShort"));
        assertEquals(new Vector<String>(Arrays.asList("abc", "ghi")), dict.get("testString"));
        assertEquals(new Vector<Character>(Arrays.asList('a', 'b')), dict.get("testCharacter"));
        assertEquals(new Vector<Byte>(Arrays.asList((byte) 'A', (byte) 'B')), dict.get("testByte"));
        assertEquals(new Vector<String>(Arrays.asList("a b c", "def")), dict.get("testToken"));
    }

    @Test
    public void testMetaTypeDefaultValues() throws Exception {
        changeLocationSettings("default");

        ConfigElement config = configParser.parseConfigElement(new StringReader("<test myValue=\"test\">" + "</test>"));

        TestConfigEvaluator evaluator = createConfigEvaluator();

        MockObjectClassDefinition objectClass = new MockObjectClassDefinition("test");

        objectClass.addAttributeDefinition(new MockAttributeDefinition("testSimpleBoolean", AttributeDefinition.BOOLEAN, 0, new String[] { "true" }));
        objectClass.addAttributeDefinition(new MockAttributeDefinition("testSimpleString", AttributeDefinition.STRING, 0, new String[] { "abc" }));
        objectClass.addAttributeDefinition(new MockAttributeDefinition("testVariable", AttributeDefinition.STRING, 0, new String[] { "${testSimpleBoolean}-${myValue}" }));
        objectClass.addAttributeDefinition(new MockAttributeDefinition("testArray", AttributeDefinition.INTEGER, 10, new String[] { "123", "345" }));
        objectClass.addAttributeDefinition(new MockAttributeDefinition("testCollection", AttributeDefinition.LONG, -10, new String[] { "1234", "5678" }));

        Dictionary<String, Object> dict = evaluator.evaluateToDictionary(config, toRegistryEntry(objectClass));

        assertEquals(Boolean.TRUE, dict.get("testSimpleBoolean"));
        assertEquals("abc", dict.get("testSimpleString"));
        assertEquals("true-test", dict.get("testVariable"));
        assertTrue(Arrays.equals(new int[] { 123, 345 }, (int[]) dict.get("testArray")));
        assertEquals(new Vector<Long>(Arrays.asList(1234L, 5678L)), dict.get("testCollection"));
    }

    @Test
    /* Same as testSingletonWithAlias() */
    public void testSingletonWithMetatTypeWithAlias() throws Exception {
        changeLocationSettings("singleton");

        WsResource resource = wsLocation.resolveResource(CONFIG_ROOT);

        ServerConfiguration serverConfig = configParser.parseServerConfiguration(resource);

        TestConfigEvaluator evaluator = createConfigEvaluator();

        MockObjectClassDefinition objectClass = new MockObjectClassDefinition("httpConnector");

        objectClass.setAlias("HTTP");
        objectClass.addAttributeDefinition(new MockAttributeDefinition("clientAuth", AttributeDefinition.BOOLEAN, 0, new String[] { "false" }));
        objectClass.addAttributeDefinition(new MockAttributeDefinition("logFile", AttributeDefinition.STRING, 0, new String[] { "none" }));
        objectClass.addAttributeDefinition(new MockAttributeDefinition("logging.enabled", AttributeDefinition.STRING, 0, new String[] { "false" }));
        objectClass.addAttributeDefinition(new MockAttributeDefinition("port", AttributeDefinition.INTEGER, 10, new String[] { "2222" }));
        objectClass.addAttributeDefinition(new MockAttributeDefinition("auth", AttributeDefinition.STRING, 10, new String[] { "saml" }));

        ConfigElement applied = serverConfig.getSingleton(objectClass.getID(), objectClass.getAlias());

        Dictionary<String, Object> map = evaluator.evaluateToDictionary(applied, toRegistryEntry(objectClass));

        assertEquals("true", map.get("logging.enabled"));
        assertArrayEquals(new String[] { "sslv2", "sslv3", "tls" }, (String[]) map.get("sslVersion"));
        assertArrayEquals(new String[] { "rsa" }, (String[]) map.get("ciphers"));
        assertArrayEquals(new int[] { 8080, 9999, 7777 }, (int[]) map.get("port"));
        assertArrayEquals(new String[] { "openid" }, (String[]) map.get("auth"));
        assertEquals(wsLocation.resolveString("${shared.config.dir}/access.txt"), PathUtils.normalize((String) map.get("logFile")));
        assertEquals(Boolean.TRUE, map.get("clientAuth")); // from alias
        assertEquals("yes", map.get("requireClientAuth")); // from alias
    }

    @Test
    /* Same as testSingleton() */
    public void testSingletonWithMetatType() throws Exception {
        changeLocationSettings("singleton");

        WsResource resource = wsLocation.resolveResource(CONFIG_ROOT);

        ServerConfiguration serverConfig = configParser.parseServerConfiguration(resource);

        TestConfigEvaluator evaluator = createConfigEvaluator();

        MockObjectClassDefinition objectClass = new MockObjectClassDefinition("httpConnector");

        objectClass.addAttributeDefinition(new MockAttributeDefinition("clientAuth", AttributeDefinition.BOOLEAN, 0, new String[] { "false" }));
        objectClass.addAttributeDefinition(new MockAttributeDefinition("logFile", AttributeDefinition.STRING, 0, new String[] { "none" }));
        objectClass.addAttributeDefinition(new MockAttributeDefinition("logging.enabled", AttributeDefinition.STRING, 0, new String[] { "false" }));
        objectClass.addAttributeDefinition(new MockAttributeDefinition("port", AttributeDefinition.INTEGER, 10, new String[] { "2222" }));
        objectClass.addAttributeDefinition(new MockAttributeDefinition("auth", AttributeDefinition.STRING, 10, new String[] { "saml" }));

        ConfigElement applied = serverConfig.getSingleton(objectClass.getID(), objectClass.getAlias());

        Dictionary<String, Object> map = evaluator.evaluateToDictionary(applied, toRegistryEntry(objectClass));

        assertEquals("false", map.get("logging.enabled"));
        assertNull(map.get("sslVersion"));
        assertArrayEquals(new String[] { "rsa", "dsa" }, (String[]) map.get("ciphers"));
        assertArrayEquals(new int[] { 8080, 9999 }, (int[]) map.get("port"));
        assertArrayEquals(new String[] { "saml" }, (String[]) map.get("auth"));
        assertEquals(wsLocation.resolveString("${shared.config.dir}/access.log"), PathUtils.normalize((String) map.get("logFile")));
        assertEquals(Boolean.FALSE, map.get("clientAuth"));
        assertEquals("no", map.get("requireClientAuth"));
    }

    @Test
    /* Same as testFactory() */
    public void testFactory() throws Exception {
        changeLocationSettings("factory");

        WsResource resource = wsLocation.resolveResource(CONFIG_ROOT);

        ServerConfiguration serverConfig = configParser.parseServerConfiguration(resource);

        TestConfigEvaluator evaluator = createConfigEvaluator();

        MockObjectClassDefinition objectClass = new MockObjectClassDefinition("threadpool");

        objectClass.addAttributeDefinition(new MockAttributeDefinition("daemon", AttributeDefinition.BOOLEAN, 0, new String[] { "false" }));
        objectClass.addAttributeDefinition(new MockAttributeDefinition("timeout", AttributeDefinition.INTEGER, 0, new String[] { "-1" }));
        objectClass.addAttributeDefinition(new MockAttributeDefinition("minThreads", AttributeDefinition.INTEGER, 0, new String[] { "0" }));
        objectClass.addAttributeDefinition(new MockAttributeDefinition("maxThreads", AttributeDefinition.INTEGER, 0, new String[] { "100" }));

        RegistryEntry re = toRegistryEntry(objectClass);

        ConfigElement entry;
        Dictionary<String, Object> map;

        entry = serverConfig.getFactoryInstance(objectClass.getID(), objectClass.getAlias(), "webPool");
        map = evaluator.evaluateToDictionary(entry, re);

        assertEquals(new Integer(100), map.get("maxThreads"));
        assertEquals(new Integer(0), map.get("minThreads"));
        assertEquals(Boolean.TRUE, map.get("daemon"));
        assertEquals(new Integer(-1), map.get("timeout"));
        assertEquals(null, map.get("interrupt"));
        assertEquals("5000", map.get("idle"));

        entry = serverConfig.getFactoryInstance(objectClass.getID(), objectClass.getAlias(), "connectorPool");
        map = evaluator.evaluateToDictionary(entry, re);

        assertEquals(new Integer(20), map.get("maxThreads"));
        assertEquals(new Integer(5), map.get("minThreads"));
        assertEquals(Boolean.FALSE, map.get("daemon"));
        assertEquals(new Integer(-1), map.get("timeout"));
        assertEquals(null, map.get("interrupt"));

        entry = serverConfig.getFactoryInstance(objectClass.getID(), objectClass.getAlias(), "ejbPool");
        map = evaluator.evaluateToDictionary(entry, re);

        assertEquals(new Integer(100), map.get("maxThreads"));
        assertEquals(new Integer(0), map.get("minThreads"));
        assertEquals(Boolean.FALSE, map.get("daemon"));
        assertEquals(new Integer(1000), map.get("timeout"));
        assertEquals(null, map.get("interrupt"));
    }

    @Test
    public void testFactoryWithAlias() throws Exception {
        changeLocationSettings("factory");

        WsResource resource = wsLocation.resolveResource(CONFIG_ROOT);

        ServerConfiguration serverConfig = configParser.parseServerConfiguration(resource);

        TestConfigEvaluator evaluator = createConfigEvaluator();

        MockObjectClassDefinition objectClass = new MockObjectClassDefinition("threadpool");

        objectClass.setAlias("TP");
        objectClass.addAttributeDefinition(new MockAttributeDefinition("daemon", AttributeDefinition.BOOLEAN, 0, new String[] { "false" }));
        objectClass.addAttributeDefinition(new MockAttributeDefinition("timeout", AttributeDefinition.INTEGER, 0, new String[] { "-1" }));
        objectClass.addAttributeDefinition(new MockAttributeDefinition("minThreads", AttributeDefinition.INTEGER, 0, new String[] { "0" }));
        objectClass.addAttributeDefinition(new MockAttributeDefinition("maxThreads", AttributeDefinition.INTEGER, 0, new String[] { "100" }));

        RegistryEntry re = toRegistryEntry(objectClass);

        ConfigElement entry;
        Dictionary<String, Object> map;

        entry = serverConfig.getFactoryInstance(objectClass.getID(), objectClass.getAlias(), "webPool");
        map = evaluator.evaluateToDictionary(entry, re);

        assertEquals(new Integer(10), map.get("maxThreads"));
        assertEquals(new Integer(0), map.get("minThreads"));
        assertEquals(Boolean.TRUE, map.get("daemon"));
        assertEquals(new Integer(-1), map.get("timeout"));
        assertEquals("8000", map.get("idle"));

        entry = serverConfig.getFactoryInstance(objectClass.getID(), objectClass.getAlias(), "connectorPool");
        map = evaluator.evaluateToDictionary(entry, re);

        assertEquals(new Integer(20), map.get("maxThreads"));
        assertEquals(new Integer(5), map.get("minThreads"));
        assertEquals(Boolean.FALSE, map.get("daemon"));
        assertEquals(new Integer(-1), map.get("timeout"));

        entry = serverConfig.getFactoryInstance(objectClass.getID(), objectClass.getAlias(), "ejbPool");
        map = evaluator.evaluateToDictionary(entry, re);

        assertEquals(new Integer(100), map.get("maxThreads"));
        assertEquals(new Integer(0), map.get("minThreads"));
        assertEquals(Boolean.FALSE, map.get("daemon"));
        assertEquals(new Integer(1000), map.get("timeout"));
    }

    @Test
    public void testMetaTypePassword() throws Exception {
        changeLocationSettings("default");

        TestConfigEvaluator evaluator = createConfigEvaluator();

        String defaultPassword = "{xor}KD4sazs="; //was4d
        String defaultUser = "root";
        String defaultLocation = "file:///i/am/hidden";

        MockObjectClassDefinition objectClass = new MockObjectClassDefinition("test");
        objectClass.addAttributeDefinition(new MockAttributeDefinition("dbPassword", MetaTypeFactory.PASSWORD_TYPE, 0, new String[] { defaultPassword }));
        objectClass.addAttributeDefinition(new MockAttributeDefinition("dbUser", AttributeDefinition.STRING, 0, new String[] { defaultUser }));
        MockAttributeDefinition location = new MockAttributeDefinition("dbLocation", AttributeDefinition.STRING, 0, new String[] { defaultLocation });
        location.setObscured("true");
        objectClass.addAttributeDefinition(location);
        RegistryEntry re = toRegistryEntry(objectClass);

        ConfigElement config = null;
        Dictionary<String, Object> dict = null;

        // test default values
        config = configParser.parseConfigElement(new StringReader("<test/>"));
        dict = evaluator.evaluateToDictionary(config, re);

        assertEquals(defaultUser, dict.get("dbUser"));
        assertEquals(defaultPassword, new String(((SerializableProtectedString) dict.get("dbPassword")).getChars()));
        assertEquals(defaultLocation, new String(((SerializableProtectedString) dict.get("dbLocation")).getChars()));

        // test overrides
        String overridePassword = "{xor}Mj4xPjg6LQ=="; // manager
        String overrideUser = "system";
        String overrideLocation = "http://hidden.com";

        config = configParser.parseConfigElement(new StringReader("<test dbLocation=\"" + overrideLocation + "\" dbPassword=\"" + overridePassword + "\" dbUser=\"" + overrideUser
                                                                  + "\" />"));
        dict = evaluator.evaluateToDictionary(config, re);

        assertEquals(overrideUser, dict.get("dbUser"));
        assertEquals(overridePassword, new String(((SerializableProtectedString) dict.get("dbPassword")).getChars()));
        assertEquals(overrideLocation, new String(((SerializableProtectedString) dict.get("dbLocation")).getChars()));
    }

}
