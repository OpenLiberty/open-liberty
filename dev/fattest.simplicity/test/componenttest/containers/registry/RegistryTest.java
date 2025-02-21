/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package componenttest.containers.registry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Tests methods on {@link componenttest.containers.registry.Registry}
 * Utility methods + Setup methods
 */
public class RegistryTest {

    private static final String nl = System.lineSeparator();
    private static final String tab = "\t";

    private static final String TESTREGISTRY = "example.com";
    private static final String TESTAUTHTOKEN = "token123456789";
    private static final File TESTDIR = new File(System.getProperty("java.io.tmpdir"), ".docker");
    private static final File TESTFILE = new File(TESTDIR, "config.json");

    @BeforeClass
    public static void setupTests() throws Exception {
        Files.createDirectories(TESTDIR.toPath());
    }

    @Before
    public void setupTest() throws Exception {
        Files.deleteIfExists(TESTFILE.toPath());
    }

    @AfterClass
    public static void tearDownTests() throws Exception {
        Files.deleteIfExists(TESTFILE.toPath());
        Files.deleteIfExists(TESTDIR.toPath());
    }

    // UTILITY METHOD TESTS

    /**
     * Ensure a new file is created with the correct authentication
     */
    @Test
    public void testNoExisingConfig() throws Exception {
        final String m = "testNoExisingConfig";
        getPersistAuthToken().invoke(null, TESTREGISTRY, TESTAUTHTOKEN, TESTDIR);

        //TODO convert this to a text block once we are building and running on Java 17!
        String expected = "{" + nl +
                          tab + "\"auths\" : {" + nl +
                          tab + tab + "\"" + TESTREGISTRY + "\" : {" + nl +
                          tab + tab + tab + "\"auth\" : \"" + TESTAUTHTOKEN + "\"" + nl +
                          tab + tab + "}" + nl +
                          tab + "}" + nl +
                          "}";
        String actual = Files.readAllLines(TESTFILE.toPath()).stream().collect(Collectors.joining(nl));
        assertJsonEquals("", expected, actual, m);
    }

    /**
     * Ensure the correct authentication is appended to config
     */
    @Test
    public void testExistingEmptyConfig() throws Exception {
        final String m = "testExistingEmptyConfig";
        String existingConfig = "{" + nl + "}";
        Registry.writeFile(TESTFILE, "{" + nl + "}");
        getPersistAuthToken().invoke(null, TESTREGISTRY, TESTAUTHTOKEN, TESTDIR);

        //TODO convert this to a text block once we are building and running on Java 17!
        String expected = "{" + nl +
                          tab + "\"auths\" : {" + nl +
                          tab + tab + "\"" + TESTREGISTRY + "\" : {" + nl +
                          tab + tab + tab + "\"auth\" : \"" + TESTAUTHTOKEN + "\"" + nl +
                          tab + tab + "}" + nl +
                          tab + "}" + nl +
                          "}";
        String actual = Files.readAllLines(TESTFILE.toPath()).stream().collect(Collectors.joining(nl));
        assertJsonEquals(existingConfig, expected, actual, m);
    }

    /**
     * Ensure the correct authentication is appended to default config set by docker-desktop
     * Ensure alphabetical order is maintained
     */
    @Test
    public void testExistingDefaultConfig() throws Exception {
        final String m = "testExistingDefaultConfig";
        String existingConfig = "{" + nl +
                                tab + "\"credsStore\" : \"desktop\"," + nl +
                                tab + "\"currentContext\" : \"desktop-linux\"" + nl +
                                "}";
        Registry.writeFile(TESTFILE, existingConfig);
        getPersistAuthToken().invoke(null, TESTREGISTRY, TESTAUTHTOKEN, TESTDIR);

        //TODO convert this to a text block once we are building and running on Java 17!
        String expected = "{" + nl +
                          tab + "\"auths\" : {" + nl +
                          tab + tab + "\"" + TESTREGISTRY + "\" : {" + nl +
                          tab + tab + tab + "\"auth\" : \"" + TESTAUTHTOKEN + "\"" + nl +
                          tab + tab + "}" + nl +
                          tab + "}," + nl +
                          tab + "\"credsStore\" : \"desktop\"," + nl +
                          tab + "\"currentContext\" : \"desktop-linux\"" + nl +
                          "}";
        String actual = Files.readAllLines(TESTFILE.toPath()).stream().collect(Collectors.joining(nl));
        assertJsonEquals(existingConfig, expected, actual, m);
    }

    /**
     * Ensure the incorrect token is replaced by the correct token.
     * Ensure email is preserved if it was already set
     */
    @Test
    public void testExistingIncorrectConfig() throws Exception {
        final String m = "testExistingIncorrectConfig";
        final String incorrectToken = "fakeToken987654321";
        String existingConfig = "{" + nl +
                                tab + "\"auths\" : {" + nl +
                                tab + tab + "\"" + TESTREGISTRY + "\" : {" + nl +
                                tab + tab + tab + "\"auth\" : \"" + incorrectToken + "\"," + nl +
                                tab + tab + tab + "\"email\" : null" + nl +
                                tab + tab + "}" + nl +
                                tab + "}" + nl +
                                "}";
        Registry.writeFile(TESTFILE, existingConfig);
        getPersistAuthToken().invoke(null, TESTREGISTRY, TESTAUTHTOKEN, TESTDIR);

        //TODO convert this to a text block once we are building and running on Java 17!
        String expected = "{" + nl +
                          tab + "\"auths\" : {" + nl +
                          tab + tab + "\"" + TESTREGISTRY + "\" : {" + nl +
                          tab + tab + tab + "\"auth\" : \"" + TESTAUTHTOKEN + "\"," + nl +
                          tab + tab + tab + "\"email\" : null" + nl +
                          tab + tab + "}" + nl +
                          tab + "}" + nl +
                          "}";
        String actual = Files.readAllLines(TESTFILE.toPath()).stream().collect(Collectors.joining(nl));
        assertJsonEquals(existingConfig, expected, actual, m);
    }

    /**
     * Ensure the correct authentication is appended when existing auths exist.
     * Ensure alphabetical order is maintained
     */
    @Test
    public void testExistingRegistryAuth() throws Exception {
        final String m = "testExistingRegistryAuth";
        final String additionalRegistry = "fake.com";
        final String additionalToken = "fakeToken987654321";
        String existingConfig = "{" + nl +
                                tab + "\"auths\" : {" + nl +
                                tab + tab + "\"" + additionalRegistry + "\" : {" + nl +
                                tab + tab + tab + "\"auth\" : \"" + additionalToken + "\"," + nl +
                                tab + tab + tab + "\"email\" : null" + nl +
                                tab + tab + "}" + nl +
                                tab + "}" + nl +
                                "}";
        Registry.writeFile(TESTFILE, existingConfig);
        getPersistAuthToken().invoke(null, TESTREGISTRY, TESTAUTHTOKEN, TESTDIR);

        //TODO convert this to a text block once we are building and running on Java 17!
        String expected = "{" + nl +
                          tab + "\"auths\" : {" + nl +
                          tab + tab + "\"" + TESTREGISTRY + "\" : {" + nl +
                          tab + tab + tab + "\"auth\" : \"" + TESTAUTHTOKEN + "\"" + nl +
                          tab + tab + "}," + nl +
                          tab + tab + "\"" + additionalRegistry + "\" : {" + nl +
                          tab + tab + tab + "\"auth\" : \"" + additionalToken + "\"," + nl +
                          tab + tab + tab + "\"email\" : null" + nl +
                          tab + tab + "}" + nl +
                          tab + "}" + nl +
                          "}";
        String actual = Files.readAllLines(TESTFILE.toPath()).stream().collect(Collectors.joining(nl));
        assertJsonEquals(existingConfig, expected, actual, m);
    }

    /**
     * Ensure the correct authentication is appended when existing auths exist and default configuration from docker-desktop.
     * Ensure alphabetical order is maintained
     */
    @Test
    public void testExistingRegistryAuthAndDefaultConfig() throws Exception {
        final String m = "testExistingRegistryAuthAndDefaultConfig";
        final String additionalRegistry = "fake.com";
        final String additionalToken = "fakeToken987654321";

        String existingConfig = "{" + nl +
                                tab + "\"auths\" : {" + nl +
                                tab + tab + "\"" + additionalRegistry + "\" : {" + nl +
                                tab + tab + tab + "\"auth\" : \"" + additionalToken + "\"" + nl +
                                tab + tab + "}" + nl +
                                tab + "}," + nl +
                                tab + "\"credsStore\" : \"desktop\"," + nl +
                                tab + "\"currentContext\" : \"desktop-linux\"" + nl +
                                "}";

        Registry.writeFile(TESTFILE, existingConfig);
        getPersistAuthToken().invoke(null, TESTREGISTRY, TESTAUTHTOKEN, TESTDIR);

        //TODO convert this to a text block once we are building and running on Java 17!
        String expected = "{" + nl +
                          tab + "\"auths\" : {" + nl +
                          tab + tab + "\"" + TESTREGISTRY + "\" : {" + nl +
                          tab + tab + tab + "\"auth\" : \"" + TESTAUTHTOKEN + "\"" + nl +
                          tab + tab + "}," + nl +
                          tab + tab + "\"" + additionalRegistry + "\" : {" + nl +
                          tab + tab + tab + "\"auth\" : \"" + additionalToken + "\"" + nl +
                          tab + tab + "}" + nl +
                          tab + "}," + nl +
                          tab + "\"credsStore\" : \"desktop\"," + nl +
                          tab + "\"currentContext\" : \"desktop-linux\"" + nl +
                          "}";
        String actual = Files.readAllLines(TESTFILE.toPath()).stream().collect(Collectors.joining(nl));
        assertJsonEquals(existingConfig, expected, actual, m);
    }

    /**
     * Ensure if the correct authentication already exists the file is not modified
     */
    @Test
    public void testExistingRegistryAuthMatched() throws Exception {
        final String m = "testExistingRegistryAuthMatched";
        String expected = "{" + nl +
                          tab + "\"auths\" : {" + nl +
                          tab + tab + "\"" + TESTREGISTRY + "\" : {" + nl +
                          tab + tab + tab + "\"auth\" : \"" + TESTAUTHTOKEN + "\"," + nl +
                          tab + tab + tab + "\"email\" : null" + nl +
                          tab + tab + "}" + nl +
                          tab + "}" + nl +
                          "}";
        Registry.writeFile(TESTFILE, expected);
        getPersistAuthToken().invoke(null, TESTREGISTRY, TESTAUTHTOKEN, TESTDIR);

        String actual = Files.readAllLines(TESTFILE.toPath()).stream().collect(Collectors.joining(nl));
        assertJsonEquals(expected, expected, actual, m);
    }

    @Test
    public void testMalformedConfigIsFixed() throws Exception {
        final String m = "testMalformedConfigIsFixed";

        String existingConfig = "{" + nl +
                                tab + "\"auths\" : {" + nl +
                                tab + tab + "\"" + TESTREGISTRY + "\" : {" + nl +
                                tab + tab + tab + "\"auth\" : \"" + TESTAUTHTOKEN + "\"" + nl +
                                tab + tab + "}" + nl +
                                tab + "},}," + nl + // MALFORMED: extra },
                                "}";

        Registry.writeFile(TESTFILE, existingConfig);
        getPersistAuthToken().invoke(null, TESTREGISTRY, TESTAUTHTOKEN, TESTDIR);

        //TODO convert this to a text block once we are building and running on Java 17!
        String expected = "{" + nl +
                          tab + "\"auths\" : {" + nl +
                          tab + tab + "\"" + TESTREGISTRY + "\" : {" + nl +
                          tab + tab + tab + "\"auth\" : \"" + TESTAUTHTOKEN + "\"" + nl +
                          tab + tab + "}" + nl +
                          tab + "}" + nl +
                          "}";
        String actual = Files.readAllLines(TESTFILE.toPath()).stream().collect(Collectors.joining(nl));
        assertJsonEquals(existingConfig, expected, actual, m);
    }

    /**
     * Ensure existing config is correctly parsed and only returns true when the correct authToken is present.
     *
     * @throws Exception
     */
    @Test
    public void testFindExistingConfigResults() throws Exception {
        Optional<String> result;
        String json;
        ObjectNode root;

        final ObjectMapper mapper = JsonMapper.builder().build();
        final String additionalToken = "fakeToken987654321";

        //root.isNull
        //Cannot be tested since ObjectNode cannot be NullNode

        //root.has auths
        json = "{ \"name\" : \"kyle\" }";
        root = (ObjectNode) mapper.readTree(json);
        result = Registry.findExistingConfig(root, TESTREGISTRY);
        assertFalse(result.isPresent());

        //root.nonNull auths
        json = "{ \"auths\" : null }";
        root = (ObjectNode) mapper.readTree(json);
        result = Registry.findExistingConfig(root, TESTREGISTRY);
        assertFalse(result.isPresent());

        //root.auths.has registry
        json = "{ \"auths\" : { \"name\" : \"kyle\" } }";
        root = (ObjectNode) mapper.readTree(json);
        result = Registry.findExistingConfig(root, TESTREGISTRY);
        assertFalse(result.isPresent());

        //root.auths.nonNull registry
        json = "{ \"auths\" : { \"" + TESTREGISTRY + "\" : null } }";
        root = (ObjectNode) mapper.readTree(json);
        result = Registry.findExistingConfig(root, TESTREGISTRY);
        assertFalse(result.isPresent());

        //root.auths.registry.has auth
        json = "{ \"auths\" : { \"" + TESTREGISTRY + "\" : { \"name\" : \"kyle\" } } }";
        root = (ObjectNode) mapper.readTree(json);
        result = Registry.findExistingConfig(root, TESTREGISTRY);
        assertTrue(result.isPresent());
        assertTrue(result.get().isEmpty());

        //root.auths.registry.nonNull auth
        json = "{ \"auths\" : { \"" + TESTREGISTRY + "\" : { \"auth\" : null } } }";
        root = (ObjectNode) mapper.readTree(json);
        result = Registry.findExistingConfig(root, TESTREGISTRY);
        assertTrue(result.isPresent());
        assertTrue(result.get().isEmpty());

        //root.auths.registry.auth.isTextual
        json = "{ \"auths\" : { \"" + TESTREGISTRY + "\" : { \"auth\" : { \"name\" : \"kyle\" } } } }";
        root = (ObjectNode) mapper.readTree(json);
        result = Registry.findExistingConfig(root, TESTREGISTRY);
        assertTrue(result.isPresent());
        assertTrue(result.get().isEmpty());

        //root.auths.registry.auth equals - false
        json = "{ \"auths\" : { \"" + TESTREGISTRY + "\" : { \"auth\" : \"" + additionalToken + "\" } } }";
        root = (ObjectNode) mapper.readTree(json);
        result = Registry.findExistingConfig(root, TESTREGISTRY);
        assertTrue(result.isPresent());
        assertFalse(result.get().isEmpty());
        assertNotSame(TESTAUTHTOKEN, result.get());

        //root.auths.registry.auth equals - true
        json = "{ \"auths\" : { \"" + TESTREGISTRY + "\" : { \"auth\" : \"" + TESTAUTHTOKEN + "\" } } }";
        root = (ObjectNode) mapper.readTree(json);
        result = Registry.findExistingConfig(root, TESTREGISTRY);
        assertTrue(result.isPresent());
        assertFalse(result.get().isEmpty());
        assertEquals(TESTAUTHTOKEN, result.get());
    }

    private void assertJsonEquals(String initial, String expected, String actual, String testName) {
        System.out.println("### TestName: " + testName + " ###");
        System.out.println("initial: " + nl + initial);
        System.out.println("expected: " + nl + expected);
        System.out.println("actual:   " + nl + actual);
        assertJsonValid(expected);
        assertJsonValid(actual);
        assertEquals(expected, actual);
    }

    private static void assertJsonValid(String json) {
        try {
            final ObjectMapper mapper = new ObjectMapper();
            mapper.readTree(json);
        } catch (Exception e) {
            throw new AssertionError("Invalid json", e);
        }
    }

    private static Method getPersistAuthToken() throws Exception {
        Method method = Registry.class.getDeclaredMethod("persistAuthToken", String.class, String.class, File.class);
        method.setAccessible(true);
        return method;
    }

    // SETUP METHOD TESTS

    private static final String REGISTRY = "test.registry.prop";
    private static final String REGISTRY_USER = "test.registry.user";
    private static final String REGISTRY_PASSWORD = "test.registry.password";

    @Test
    public void testFindRegistry() throws Exception {
        Method findRegistry = getFindRegistry();

        // Unset
        System.clearProperty(REGISTRY);
        try {
            findRegistry.invoke(null, REGISTRY);
            fail("Should not have found registry when property " + REGISTRY + " was unset");
        } catch (InvocationTargetException e) {
            assertTrue(e.getCause() instanceof IllegalStateException);
        }

        // Empty
        System.setProperty(REGISTRY, "");
        try {
            findRegistry.invoke(null, REGISTRY);
            fail("Should not have found registry when property " + REGISTRY + " was empty");
        } catch (InvocationTargetException e) {
            assertTrue(e.getCause() instanceof IllegalStateException);
        }

        // Missing
        System.setProperty(REGISTRY, "${" + REGISTRY.substring(9) + "}");
        try {
            findRegistry.invoke(null, REGISTRY);
            fail("Should not have found registry when property " + REGISTRY + " was missing from gradle");
        } catch (InvocationTargetException e) {
            assertTrue(e.getCause() instanceof IllegalStateException);
        }

        // Null
        System.setProperty(REGISTRY, "null");
        try {
            findRegistry.invoke(null, REGISTRY);
            fail("Should not have found registry when property " + REGISTRY + " was null");
        } catch (InvocationTargetException e) {
            assertTrue(e.getCause() instanceof IllegalStateException);
        }

        // Valid (host name)
        String expected = "artifactory.swg-devops.com";
        System.setProperty(REGISTRY, expected);
        String actual = (String) findRegistry.invoke(null, REGISTRY);

        assertEquals(expected, actual);

        // Valid (IP Address)
        expected = "127.0.0.1";
        System.setProperty(REGISTRY, expected);
        actual = (String) findRegistry.invoke(null, REGISTRY);

        assertEquals(expected, actual);
    }

    @Test
    public void testGenerateAuthToken() throws Exception {
        String testUsername = "test.email@example.com";
        String testToken = "aToTallyFakeTokenThaTWouldNeverBeUsed";
        String expectedAuthToken = "dGVzdC5lbWFpbEBleGFtcGxlLmNvbTphVG9UYWxseUZha2VUb2tlblRoYVRXb3VsZE5ldmVyQmVVc2Vk";

        Method generateAuthToken = getGenerateAuthToken();

        // Ensure failure path
        System.clearProperty(REGISTRY_USER);
        System.clearProperty(REGISTRY_PASSWORD);

        try {
            generateAuthToken.invoke(null, REGISTRY_USER, REGISTRY_PASSWORD);
            fail("Should not have generated authToken when property "
                 + REGISTRY_USER + " and " + REGISTRY_PASSWORD + " were unset");
        } catch (InvocationTargetException e) {
            assertTrue(e.getCause() instanceof IllegalStateException);
        }

        // Ensure successful path
        System.setProperty(REGISTRY_USER, testUsername);
        System.setProperty(REGISTRY_PASSWORD, testToken);

        String actualAuthToken = (String) generateAuthToken.invoke(null, REGISTRY_USER, REGISTRY_PASSWORD);

        assertEquals(expectedAuthToken, actualAuthToken);

        String actualAuthData = new String(Base64.getDecoder().decode(actualAuthToken.getBytes()), StandardCharsets.UTF_8);
        String actualUsername = actualAuthData.split(":")[0];
        String actualToken = actualAuthData.split(":")[1];

        assertEquals(testUsername, actualUsername);
        assertEquals(testToken, actualToken);
    }

    private static Method getFindRegistry() throws Exception {
        Method method = Registry.class.getDeclaredMethod("findRegistry", String.class);
        method.setAccessible(true);
        return method;
    }

    private static Method getGenerateAuthToken() throws Exception {
        Method method = Registry.class.getDeclaredMethod("generateAuthToken", String.class, String.class);
        method.setAccessible(true);
        return method;
    }

}
