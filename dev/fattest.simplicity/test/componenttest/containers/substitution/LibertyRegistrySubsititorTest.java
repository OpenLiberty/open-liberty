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

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.mockito.MockedStatic;
import org.testcontainers.utility.DockerImageName;

import componenttest.containers.registry.ArtifactoryRegistry;
import componenttest.containers.registry.InternalRegistry;

/**
 * Tests for {@linkcomponenttest.containers.substitution.LibertyRegistrySubsititor}
 */
public class LibertyRegistrySubsititorTest {

    @Test
    public void testWithArtifactoryWithoutInternal() {
        try (MockedStatic<ArtifactoryRegistry> artifactory = MockedInstances.artifactoryRegistry("artifactory.swg-devops.com", null, true);
                        MockedStatic<InternalRegistry> internal = MockedInstances.internalRegistry("", new IllegalStateException("TEST EXCEPTION"), false)) {

            LibertyRegistrySubstitutor substitutor = new LibertyRegistrySubstitutor();

            // Artifactory registry supports the mirrored image, but was not available
            // CANNOT TEST

            // Internal registry supports the mirrored image, but was not available
            try {
                DockerImageName result = substitutor.apply(DockerImageName.parse("wasliberty-infrastructure-docker/cloudant:1.0"));
                fail("No internal registry avaialble, expected IllegalStateException, but got substitued image: " + result.asCanonicalNameString());
            } catch (IllegalStateException e) {
                //pass
            } catch (Exception e) {
                fail("Expected to catch IllegalStateException, but instead got: " + e.getMessage());
            }

            // Mirrored image name already has a registry, this indicates that a developer
            // may have introduced an unsupported registry, throw an error so they know to fix it.
            try {
                DockerImageName result = substitutor.apply(DockerImageName.parse("example.com/library/cassandra:5.3"));
                fail("Existing registry configured on image, expected IllegalStateException, but got substitued image: " + result.asCanonicalNameString());
            } catch (IllegalStateException e) {
                //pass
            } catch (Exception e) {
                fail("Expected to catch IllegalStateException, but instead got: " + e.getMessage());
            }

            // Registry supports image
            DockerImageName input = DockerImageName.parse("wasliberty-aws-docker-remote/docker/library/postgres:17.0-alpine");
            DockerImageName expected = DockerImageName.parse("artifactory.swg-devops.com/wasliberty-aws-docker-remote/docker/library/postgres:17.0-alpine");
            DockerImageName actual = substitutor.apply(input);

            assertEquals("Expected substituted image to have been prefixed with the artifactory hostname.",
                         expected, actual);
        }

    }

    @Test
    public void testWithoutArtifactoryWithInternal() {
        try (MockedStatic<ArtifactoryRegistry> artifactory = MockedInstances.artifactoryRegistry("", new IllegalStateException("TEST EXCEPTION"), false);
                        MockedStatic<InternalRegistry> internal = MockedInstances.internalRegistry("172.0.0.1", null, true)) {

            LibertyRegistrySubstitutor substitutor = new LibertyRegistrySubstitutor();

            // Artifactory registry supports the mirrored image, but was not available
            try {
                DockerImageName result = substitutor.apply(DockerImageName.parse("wasliberty-aws-docker-remote/cloudant:1.0"));
                fail("No artifactory registry avaialble, expected IllegalStateException, but got substitued image: " + result.asCanonicalNameString());
            } catch (IllegalStateException e) {
                //pass
            } catch (Exception e) {
                fail("Expected to catch IllegalStateException, but instead got: " + e.getMessage());
            }

            // Internal registry supports the mirrored image, but was not available
            // CANNOT TEST

            // Mirrored image name already has a registry, this indicates that a developer
            // may have introduced an unsupported registry, throw an error so they know to fix it.
            try {
                DockerImageName result = substitutor.apply(DockerImageName.parse("example.com/library/cassandra:5.3"));
                fail("Existing registry configured on image, expected IllegalStateException, but got substitued image: " + result.asCanonicalNameString());
            } catch (IllegalStateException e) {
                //pass
            } catch (Exception e) {
                fail("Expected to catch IllegalStateException, but instead got: " + e.getMessage());
            }

            // Registry supports image
            DockerImageName input = DockerImageName.parse("wasliberty-internal-docker-local/openliberty/testcontainers/prostgres-init/17.0-alpine");
            DockerImageName expected = DockerImageName.parse("172.0.0.1/wasliberty-internal-docker-local/openliberty/testcontainers/prostgres-init/17.0-alpine");
            DockerImageName actual = substitutor.apply(input);

            assertEquals("Expected substituted image to have been prefixed with the internal ipaddress.",
                         expected, actual);
        }
    }

    @Test
    public void testWithoutArtifactoryWithoutInternal() {
        List<DockerImageName> testImages = new ArrayList<>();
        testImages.add(DockerImageName.parse("wasliberty-aws-docker-remote/docker/library/postgres:17.0-alpine"));
        testImages.add(DockerImageName.parse("wasliberty-internal-docker-local/openliberty/testcontainers/prostgres-init/17.0-alpine"));

        LibertyRegistrySubstitutor substitutor = new LibertyRegistrySubstitutor();

        for (DockerImageName image : testImages) {
            try {
                DockerImageName result = substitutor.apply(image);
                fail("No registries available that support this image, expected IllegalStateException, but got substitued image: " + result.asCanonicalNameString());
            } catch (IllegalStateException e) {
                //pass
            } catch (Exception e) {
                fail("Expected to catch IllegalStateException, but instead got: " + e.getMessage());
            }
        }
    }
}
