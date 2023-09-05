/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package componenttest.containers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ArtifactoryRegistryTest {

    private static final String nl = System.lineSeparator();
    private static final String tab = "\t";

    private static final String TESTREGISTRY = "example.com";
    private static final String TESTAUTHTOKEN = "token123456789";
    private static final File TESTDIR = new File(System.getProperty("java.io.tmpdir"), ".docker");
    private static final File TESTFILE = new File(TESTDIR, "config.json");

    @Before
    public void setupTest() throws Exception {
        Files.deleteIfExists(TESTFILE.toPath());
    }

    @Test
    public void testNoExisingConfig() throws Exception {
        final String m = "testNoExisingConfig";
        getGenerateDockerConfig().invoke(null, TESTREGISTRY, TESTAUTHTOKEN, TESTDIR);

        //TODO convert this to a text block once we are building and running on Java 17!
        String expected = "{" + nl +
                          tab + "\"auths\": {" + nl +
                          tab + tab + "\"" + TESTREGISTRY + "\": {" + nl +
                          tab + tab + tab + "\"auth\": \"" + TESTAUTHTOKEN + "\"," + nl +
                          tab + tab + tab + "\"email\": null" + nl +
                          tab + tab + "}" + nl +
                          tab + "}" + nl +
                          "}";
        String actual = Files.readAllLines(TESTFILE.toPath()).stream().collect(Collectors.joining(nl));
        assertJsonEquals(expected, actual, m);
    }

    @Test
    public void testExistingEmptyConfig() throws Exception {
        final String m = "testExistingEmptyConfig";
        ArtifactoryRegistry.writeFile(TESTFILE, "{" + nl + "}");
        getGenerateDockerConfig().invoke(null, TESTREGISTRY, TESTAUTHTOKEN, TESTDIR);

        //TODO convert this to a text block once we are building and running on Java 17!
        String expected = "{" + nl +
                          tab + "\"auths\": {" + nl +
                          tab + tab + "\"" + TESTREGISTRY + "\": {" + nl +
                          tab + tab + tab + "\"auth\": \"" + TESTAUTHTOKEN + "\"," + nl +
                          tab + tab + tab + "\"email\": null" + nl +
                          tab + tab + "}" + nl +
                          tab + "}" + nl +
                          "}";
        String actual = Files.readAllLines(TESTFILE.toPath()).stream().collect(Collectors.joining(nl));
        assertJsonEquals(expected, actual, m);
    }

    @Test
    public void testExistingDefaultConfig() throws Exception {
        final String m = "testExistingDefaultConfig";
        String existingConfig = "{" + nl +
                                tab + "\"credsStore\": \"desktop\"," + nl +
                                tab + "\"currentContext\": \"desktop-linux\"" + nl +
                                "}";
        ArtifactoryRegistry.writeFile(TESTFILE, existingConfig);
        getGenerateDockerConfig().invoke(null, TESTREGISTRY, TESTAUTHTOKEN, TESTDIR);

        //TODO convert this to a text block once we are building and running on Java 17!
        String expected = "{" + nl +
                          tab + "\"auths\": {" + nl +
                          tab + tab + "\"" + TESTREGISTRY + "\": {" + nl +
                          tab + tab + tab + "\"auth\": \"" + TESTAUTHTOKEN + "\"," + nl +
                          tab + tab + tab + "\"email\": null" + nl +
                          tab + tab + "}" + nl +
                          tab + "}," + nl +
                          tab + "\"credsStore\": \"desktop\"," + nl +
                          tab + "\"currentContext\": \"desktop-linux\"" + nl +
                          "}";
        String actual = Files.readAllLines(TESTFILE.toPath()).stream().collect(Collectors.joining(nl));
        assertJsonEquals(expected, actual, m);
    }

    @Test
    public void testExistingRegistryAuth() throws Exception {
        final String m = "testExistingRegistryAuth";
        final String additionalRegistry = "fake.com";
        final String additionalToken = "fakeToken987654321";
        String existingConfig = "{" + nl +
                                tab + "\"auths\": {" + nl +
                                tab + tab + "\"" + additionalRegistry + "\": {" + nl +
                                tab + tab + tab + "\"auth\": \"" + additionalToken + "\"," + nl +
                                tab + tab + tab + "\"email\": null" + nl +
                                tab + tab + "}" + nl +
                                tab + "}" + nl +
                                "}";
        ArtifactoryRegistry.writeFile(TESTFILE, existingConfig);
        getGenerateDockerConfig().invoke(null, TESTREGISTRY, TESTAUTHTOKEN, TESTDIR);

        //TODO convert this to a text block once we are building and running on Java 17!
        String expected = "{" + nl +
                          tab + "\"auths\": {" + nl +
                          tab + tab + "\"" + TESTREGISTRY + "\": {" + nl +
                          tab + tab + tab + "\"auth\": \"" + TESTAUTHTOKEN + "\"," + nl +
                          tab + tab + tab + "\"email\": null" + nl +
                          tab + tab + "}," + nl +
                          tab + tab + "\"" + additionalRegistry + "\": {" + nl +
                          tab + tab + tab + "\"auth\": \"" + additionalToken + "\"," + nl +
                          tab + tab + tab + "\"email\": null" + nl +
                          tab + tab + "}" + nl +
                          tab + "}" + nl +
                          "}";
        String actual = Files.readAllLines(TESTFILE.toPath()).stream().collect(Collectors.joining(nl));
        assertJsonEquals(expected, actual, m);
    }

    @Test
    public void testExistingRegistryAuthMatched() throws Exception {
        final String m = "testExistingRegistryAuthMatched";
        String expected = "{" + nl +
                          tab + "\"auths\": {" + nl +
                          tab + tab + "\"" + TESTREGISTRY + "\": {" + nl +
                          tab + tab + tab + "\"auth\": \"" + TESTAUTHTOKEN + "\"," + nl +
                          tab + tab + tab + "\"email\": null" + nl +
                          tab + tab + "}" + nl +
                          tab + "}" + nl +
                          "}";
        ArtifactoryRegistry.writeFile(TESTFILE, expected);
        getGenerateDockerConfig().invoke(null, TESTREGISTRY, TESTAUTHTOKEN, TESTDIR);

        String actual = Files.readAllLines(TESTFILE.toPath()).stream().collect(Collectors.joining(nl));
        assertJsonEquals(expected, actual, m);
    }

    private void assertJsonEquals(String expected, String actual, String testName) {
        System.out.println("### TestName: " + testName + " ###");
        System.out.println("expected: " + nl + expected);
        System.out.println("actual:   " + nl + actual);
        assertTrue(isJsonValid(expected));
        assertTrue(isJsonValid(actual));
        assertEquals(expected, actual);
    }

    private static boolean isJsonValid(String json) {
        try {
            final ObjectMapper mapper = new ObjectMapper();
            mapper.readTree(json);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static Method getGenerateDockerConfig() throws Exception {
        Method method = ArtifactoryRegistry.class.getDeclaredMethod("generateDockerConfig", String.class, String.class, File.class);
        method.setAccessible(true);
        return method;
    }

}
