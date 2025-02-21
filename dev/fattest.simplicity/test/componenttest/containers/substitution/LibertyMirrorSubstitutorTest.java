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

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.testcontainers.utility.DockerImageName;

/**
 * Tests for {@linkcomponenttest.containers.substitution.LibertyMirrorSubstitutor}
 */
public class LibertyMirrorSubstitutorTest {

    @Test
    public void testSubstitution() {
        Map<DockerImageName, DockerImageName> testMap = new HashMap<>();
        // Images that exist in an artifactory mirror only
        testMap.put(DockerImageName.parse("ghcr.io/oracle:1.0"), DockerImageName.parse("wasliberty-ghcr-docker-remote/oracle:1.0"));
        testMap.put(DockerImageName.parse("icr.io/open-liberty:2.0"), DockerImageName.parse("wasliberty-icr-docker-remote/open-liberty:2.0"));
        testMap.put(DockerImageName.parse("public.ecr.aws/postgres:1.14.2"), DockerImageName.parse("wasliberty-aws-docker-remote/postgres:1.14.2"));

        //Images that exist in an internal mirror only
        testMap.put(DockerImageName.parse("cloudant:1.0"), DockerImageName.parse("wasliberty-infrastructure-docker/cloudant:1.0"));
        testMap.put(DockerImageName.parse("localhost/openliberty/testcontainers/kerberos:6.6"),
                    DockerImageName.parse("wasliberty-internal-docker-local/openliberty/testcontainers/kerberos:6.6"));

        //Images that do not exist in either registry
        testMap.put(DockerImageName.parse("sha256:5103a25d3efd8c0cbdbc80d358c5b1da91329c53e1fa99c43a8561a87eb61d3b"),
                    DockerImageName.parse("sha256:5103a25d3efd8c0cbdbc80d358c5b1da91329c53e1fa99c43a8561a87eb61d3b"));
        testMap.put(DockerImageName.parse("localhost/testcontainers/ryuk:latest"),
                    DockerImageName.parse("localhost/testcontainers/ryuk:latest"));

        LibertyMirrorSubstitutor substitutor = new LibertyMirrorSubstitutor();

        for (Map.Entry<DockerImageName, DockerImageName> test : testMap.entrySet()) {
            DockerImageName expected = test.getValue();
            DockerImageName actual = substitutor.apply(test.getKey());
            assertEquals("Only images supported by an artifactory mirror should have been substituted",
                         expected, actual);
        }
    }

    @Test
    public void testAlreadyMirrored() {
        LibertyMirrorSubstitutor substitutor = new LibertyMirrorSubstitutor();

        DockerImageName expected = DockerImageName.parse("wasliberty-intops-docker-local/websphere/internal/image:1.0");
        DockerImageName actual = substitutor.apply(expected);

        assertEquals("Already mirrored images should have been the same after substitution.",
                     expected, actual);
    }

}
