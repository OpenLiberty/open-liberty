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

import org.junit.Test;
import org.mockito.MockedStatic;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.dockerclient.EnvironmentAndSystemPropertyClientProviderStrategy;
import org.testcontainers.dockerclient.UnixSocketClientProviderStrategy;
import org.testcontainers.utility.DockerImageName;

import componenttest.containers.registry.ArtifactoryRegistry;
import componenttest.containers.registry.InternalRegistry;

/**
 * Tests for {@linkcomponenttest.containers.substitution.LibertyImageNameSubstitor}
 */
@SuppressWarnings("deprecation")
public class LibertyImageNameSubstitorTest {

    @Test //Priority 1
    public void testSyntheticOrCommitted() {
        LibertyImageNameSubstitutor substitutor = new LibertyImageNameSubstitutor();

        DockerImageName expected = DockerImageName.parse("sha256:5103a25d3efd8c0cbdbc80d358c5b1da91329c53e1fa99c43a8561a87eb61d3b");
        DockerImageName actual = substitutor.apply(expected);

        assertEquals("Expected synthetic image name to not be substituted", expected, actual);

        expected = DockerImageName.parse("localhost/testcontainers/ryuk:latest");
        actual = substitutor.apply(expected);

        assertEquals("Expected committed image name to not be substituted", expected, actual);
    }

    @Test //Priority 2b
    public void testArtifactoryRegistry() {
        LibertyImageNameSubstitutor substitutor = new LibertyImageNameSubstitutor();

        try {
            DockerImageName result = substitutor.apply(DockerImageName.parse("department-artifactory.swg-devops.com/websphere/internal/image:1.0"));
            fail("Expected to catch RuntimeException, but instead the image was substituted for: " + result.asCanonicalNameString());
        } catch (RuntimeException e) {
            //pass
        } catch (Exception e) {
            fail("Expected to catch RuntimeException, but instead got: " + e.getMessage());
        }
    }

    @Test //Priority 4
    public void testRemoteDockerHost() {
        try (MockedStatic<DockerClientFactory> envProvider = MockedInstances.dockerClientFactory(EnvironmentAndSystemPropertyClientProviderStrategy.class)) {
            // No mirror available
            LibertyImageNameSubstitutor substitutor = new LibertyImageNameSubstitutor();
            try {
                DockerImageName result = substitutor.apply(DockerImageName.parse("ghcr.io/oracle:1.0"));
                fail("Expected to catch IllegalStateException, but instead the image was substituted for: " + result.asCanonicalNameString());
            } catch (IllegalStateException e) {
                // pass
            } catch (Exception e) {
                fail("Expected to catch IllegalStateException, but instead got: " + e.getMessage());
            }

            // With artifactory registry available
            try (MockedStatic<ArtifactoryRegistry> artifactory = MockedInstances.artifactoryRegistry("artifactory.swg-devops.com", null, true);
                            MockedStatic<InternalRegistry> internal = MockedInstances.internalRegistry("", new IllegalStateException("TEST EXCEPTION"), false)) {

                substitutor = new LibertyImageNameSubstitutor();

                DockerImageName input = DockerImageName.parse("ghcr.io/oracle:1.0");
                DockerImageName expected = DockerImageName.parse("artifactory.swg-devops.com/wasliberty-ghcr-docker-remote/oracle:1.0");
                DockerImageName actual = substitutor.apply(input);

                assertEquals("Expected image to be substituted for an artifactory mirror image",
                             expected, actual);
            }

            // With internal registry available
            try (MockedStatic<ArtifactoryRegistry> artifactory = MockedInstances.artifactoryRegistry("", new IllegalStateException("TEST EXCEPTION"), false);
                            MockedStatic<InternalRegistry> internal = MockedInstances.internalRegistry("172.0.0.1", null, true)) {

                substitutor = new LibertyImageNameSubstitutor();

                DockerImageName input = DockerImageName.parse("cloudant:1.0");
                DockerImageName expected = DockerImageName.parse("172.0.0.1/wasliberty-infrastructure-docker/cloudant:1.0");
                DockerImageName actual = substitutor.apply(input);

                assertEquals("Expected image to be substituted for an artifactory mirror image",
                             expected, actual);
            }
        }
    }

    @Test //Priority 6 (artifactory)
    public void testArtifactoryAvailable() {
        try (MockedStatic<DockerClientFactory> unixProvider = MockedInstances.dockerClientFactory(UnixSocketClientProviderStrategy.class);
                        MockedStatic<ArtifactoryRegistry> artifactory = MockedInstances.artifactoryRegistry("artifactory.swg-devops.com", null, true);
                        MockedStatic<InternalRegistry> internal = MockedInstances.internalRegistry("", new IllegalStateException("TEST EXCEPTION"), false)) {

            LibertyImageNameSubstitutor substitutor = new LibertyImageNameSubstitutor();

            DockerImageName input = DockerImageName.parse("ghcr.io/oracle:1.0");
            DockerImageName expected = DockerImageName.parse("artifactory.swg-devops.com/wasliberty-ghcr-docker-remote/oracle:1.0");
            DockerImageName actual = substitutor.apply(input);

            assertEquals("Expected image to be substituted for an artifactory mirror image",
                         expected, actual);
        }
    }

    @Test //Priority 6 (internal)
    public void testInternalAvailable() {
        try (MockedStatic<DockerClientFactory> unixProvider = MockedInstances.dockerClientFactory(UnixSocketClientProviderStrategy.class);
                        MockedStatic<ArtifactoryRegistry> artifactory = MockedInstances.artifactoryRegistry("", new IllegalStateException("TEST EXCEPTION"), false);
                        MockedStatic<InternalRegistry> internal = MockedInstances.internalRegistry("172.0.0.1", null, true)) {

            LibertyImageNameSubstitutor substitutor = new LibertyImageNameSubstitutor();

            DockerImageName input = DockerImageName.parse("cloudant:1.0");
            DockerImageName expected = DockerImageName.parse("172.0.0.1/wasliberty-infrastructure-docker/cloudant:1.0");
            DockerImageName actual = substitutor.apply(input);

            assertEquals("Expected image to be substituted for an artifactory mirror image",
                         expected, actual);
        }
    }

    @Test //default
    public void testNoRegistryAvailable() {
        try (MockedStatic<DockerClientFactory> unixProvider = MockedInstances.dockerClientFactory(UnixSocketClientProviderStrategy.class)) {
            LibertyImageNameSubstitutor substitutor = new LibertyImageNameSubstitutor();

            DockerImageName expected = DockerImageName.parse("ghcr.io/oracle:1.0");
            DockerImageName actual = substitutor.apply(expected);
            assertEquals("Expected image not to be substituted", expected, actual);

            expected = DockerImageName.parse("cloudant:1.0");
            actual = substitutor.apply(expected);
            assertEquals("Expected image not to be substituted", expected, actual);
        }
    }
}
