/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package componenttest.containers;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.testcontainers.utility.DockerImageName;

/**
 * Unit tests for {@link ImageHelper}
 */
public class ImageHelperTest {

    private static final Map<DockerImageName, Tuple> testMap = new HashMap<>();
    static {
        // Images from a registry
        Tuple allFalse = Tuple.of(false, false, false);
        testMap.put(DockerImageName.parse("quay.io/testcontainers/ryuk:1.0.0"), allFalse);
        testMap.put(DockerImageName.parse("quay.io/testcontainers/ryuk:latest"), allFalse);
        testMap.put(DockerImageName.parse("quay.io/kyleaure/oracle-xe:1.0.0"), allFalse);
        testMap.put(DockerImageName.parse("quay.io/kyleaure/oracle-xe:latest"), allFalse);
        testMap.put(DockerImageName.parse("quay.io/openliberty:1.0.0"), allFalse);
        testMap.put(DockerImageName.parse("quay.io/openliberty:latest"), allFalse);

        // Images from a default registry (docker.io)
        testMap.put(DockerImageName.parse("testcontainers/ryuk:1.0.0"), allFalse);
        testMap.put(DockerImageName.parse("testcontainers/ryuk:latest"), allFalse);
        testMap.put(DockerImageName.parse("kyleaure/oracle-xe:1.0.0"), allFalse);
        testMap.put(DockerImageName.parse("kyleaure/oracle-xe:latest"), allFalse);
        testMap.put(DockerImageName.parse("openliberty:1.0.0"), allFalse);
        testMap.put(DockerImageName.parse("openliberty:latest"), allFalse);

        // Images from local cache (localhost)
        testMap.put(DockerImageName.parse("localhost/testcontainers/ryuk:1.0.0"), allFalse);
        testMap.put(DockerImageName.parse("localhost/kyleaure/oracle-xe:1.0.0"), allFalse);
        testMap.put(DockerImageName.parse("localhost/openliberty:1.0.0"), allFalse);
        testMap.put(DockerImageName.parse("localhost/openliberty/testcontainers/ryuk:latest"), allFalse);

        //Synthetic Images
        Tuple synthenticOnly = Tuple.of(true, false, false);
        testMap.put(DockerImageName.parse("localhost/testcontainers/ryuk:latest"), synthenticOnly);
        testMap.put(DockerImageName.parse("localhost/testcontainers/kyleaure/oracle-xe:latest"), synthenticOnly);
        testMap.put(DockerImageName.parse("localhost/testcontainers/openliberty:latest"), synthenticOnly);

        //Committed Images
        Tuple committedOnly = Tuple.of(false, true, false);
        testMap.put(DockerImageName.parse("sha256:5103a25d3efd8c0cbdbc80d358c5b1da91329c53e1fa99c43a8561a87eb61d3b"), committedOnly);
        testMap.put(DockerImageName.parse("aes:5103a25d3efd8c0cbdbc80d358c5b1da91329c53e1fa99c43a8561a87eb61d3b"), committedOnly);
        testMap.put(DockerImageName.parse("xor:5103a25d3efd8c0cbdbc80d358c5b1da91329c53e1fa99c43a8561a87eb61d3b"), committedOnly);

        // Built images
        Tuple builtOnly = Tuple.of(false, false, true);
        testMap.put(DockerImageName.parse("localhost/openliberty/testcontainers/ryuk:1.0.0"), builtOnly);
        testMap.put(DockerImageName.parse("localhost/openliberty/testcontainers/kyleaure/oracle-xe:1.0.0"), builtOnly);
    }

    @Test
    public void testSyntheticImage() {
        for (Map.Entry<DockerImageName, Tuple> test : testMap.entrySet()) {
            boolean expected = test.getValue().synthetic;
            boolean actual = ImageHelper.isSyntheticImage(test.getKey());
            assertEquals("Incorrect return value for " + test.getKey().asCanonicalNameString(), expected, actual);
        }
    }

    @Test
    public void testCommittedImage() {
        for (Map.Entry<DockerImageName, Tuple> test : testMap.entrySet()) {
            boolean expected = test.getValue().committed;
            boolean actual = ImageHelper.isCommittedImage(test.getKey());
            assertEquals("Incorrect return value for " + test.getKey().asCanonicalNameString(), expected, actual);
        }
    }

    @Test
    public void testBuiltImage() {
        for (Map.Entry<DockerImageName, Tuple> test : testMap.entrySet()) {
            boolean expected = test.getValue().built;
            boolean actual = ImageHelper.isBuiltImage(test.getKey());
            assertEquals("Incorrect return value for " + test.getKey().asCanonicalNameString(), expected, actual);
        }
    }

    private static class Tuple {
        public boolean synthetic;
        public boolean committed;
        public boolean built;

        private Tuple() {}

        public static Tuple of(boolean synthetic, boolean committed, boolean built) {
            Tuple instance = new Tuple();
            instance.synthetic = synthetic;
            instance.committed = committed;
            instance.built = built;
            return instance;
        }
    }
}
