/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package componenttest.containers.substitution;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.mockito.MockedStatic;
import org.testcontainers.utility.DockerImageName;

import componenttest.containers.registry.InternalRegistry;

/**
 * Tests for {@linkcomponenttest.containers.substitution.ImageBuilderSubstitutor}
 */
public class ImageBuilderSubstitutorTest {

    @Test
    public void testApplyLocalhost() throws Exception {

        try (MockedStatic<InternalRegistry> registry = MockedInstances.internalRegistry("", null, false)) {
            Map<DockerImageName, DockerImageName> testMap = new HashMap<>();
            // With and without prefix
            testMap.put(DockerImageName.parse("postgres-init:1.0"),
                        DockerImageName.parse("openliberty/testcontainers/postgres-init:1.0").withRegistry("localhost"));
            testMap.put(DockerImageName.parse("openliberty/testcontainers/postgres-ssl:2.0"),
                        DockerImageName.parse("openliberty/testcontainers/postgres-ssl:2.0").withRegistry("localhost"));

            // With compatible substitute set
            testMap.put(DockerImageName.parse("postgres-krb5:2.5").asCompatibleSubstituteFor("postgres"),
                        DockerImageName.parse("openliberty/testcontainers/postgres-krb5:2.5").withRegistry("localhost").asCompatibleSubstituteFor("postgres"));

            for (Map.Entry<DockerImageName, DockerImageName> test : testMap.entrySet()) {
                DockerImageName actual = getConstructor().newInstance().apply(test.getKey());
                DockerImageName expected = test.getValue();

                assertEquals("Docker image name was incorrectly substituted", expected, actual);
            }
        }
    }

    @Test
    public void testApplyIpAddress() throws Exception {

        try (MockedStatic<InternalRegistry> registry = MockedInstances.internalRegistry("172.0.0.1", null, true)) {
            Map<DockerImageName, DockerImageName> testMap = new HashMap<>();
            // With and without prefix
            testMap.put(DockerImageName.parse("postgres-init:1.0"),
                        DockerImageName.parse("172.0.0.1/openliberty/testcontainers/postgres-init:1.0"));
            testMap.put(DockerImageName.parse("openliberty/testcontainers/postgres-ssl:2.0"),
                        DockerImageName.parse("172.0.0.1/openliberty/testcontainers/postgres-ssl:2.0"));

            // With compatible substitute set
            testMap.put(DockerImageName.parse("postgres-krb5:2.5").asCompatibleSubstituteFor("postgres"),
                        DockerImageName.parse("172.0.0.1/openliberty/testcontainers/postgres-krb5:2.5").asCompatibleSubstituteFor("postgres"));

            for (Map.Entry<DockerImageName, DockerImageName> test : testMap.entrySet()) {
                DockerImageName actual = getConstructor().newInstance().apply(test.getKey());
                DockerImageName expected = test.getValue();

                assertEquals("Docker image name was incorrectly substituted", expected, actual);
            }
        }
    }

    @Test
    public void testErrorConditions() throws Exception {

        try (MockedStatic<InternalRegistry> registry = MockedInstances.internalRegistry("", null, false)) {
            ImageBuilderSubstitutor substitutor = getConstructor().newInstance();
            // Null
            try {
                substitutor.apply(null);
                fail("Expected NullPointerException");
            } catch (NullPointerException e) {
                //pass
            } catch (Exception e) {
                fail("Expected NullPointerException but got " + e.getMessage());
            }

            // With registry
            try {
                substitutor.apply(DockerImageName.parse("quay.io/oracle:1.0"));
                fail("Expected IllegalArgumentException");
            } catch (IllegalArgumentException e) {
                //pass
            } catch (Exception e) {
                fail("Expected IllegalArgumentException but got " + e.getMessage());
            }

            // Synthetic
            try {
                substitutor.apply(DockerImageName.parse("localhost/testcontainers/ryuk:1.0.0"));
                fail("Expected IllegalArgumentException");
            } catch (IllegalArgumentException e) {
                //pass
            } catch (Exception e) {
                fail("Expected IllegalArgumentException but got " + e.getMessage());
            }

            // Committed
            try {
                substitutor.apply(DockerImageName.parse("sha256:5103a25d3efd8c0cbdbc80d358c5b1da91329c53e1fa99c43a8561a87eb61d3b"));
                fail("Expected IllegalArgumentException");
            } catch (IllegalArgumentException e) {
                //pass
            } catch (Exception e) {
                fail("Expected IllegalArgumentException but got " + e.getMessage());
            }
        }
    }

    private static Constructor<ImageBuilderSubstitutor> getConstructor() throws Exception {
        Constructor<ImageBuilderSubstitutor> con = ImageBuilderSubstitutor.class.getDeclaredConstructor();
        con.setAccessible(true);
        return con;
    }

}
