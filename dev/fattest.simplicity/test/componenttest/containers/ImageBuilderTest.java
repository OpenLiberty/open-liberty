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
import static org.junit.Assert.fail;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.testcontainers.utility.DockerImageName;

/**
 * Unit tests for {@link ImageBuilder}
 */
public class ImageBuilderTest {

    @Test
    public void imageBuilderSubstitutorTest() throws Exception {
        final HashMap<String, DockerImageName> expectedImageMap = new HashMap<>();
        expectedImageMap.put("postgres-init:17.0-alpine",
                             DockerImageName.parse("localhost/openliberty/testcontainers/postgres-init:17.0-alpine"));
        expectedImageMap.put("openliberty/testcontainers/postgres-init:17.0-alpine",
                             DockerImageName.parse("localhost/openliberty/testcontainers/postgres-init:17.0-alpine"));

        for (Map.Entry<String, DockerImageName> entry : expectedImageMap.entrySet()) {
            DockerImageName actual = getImage(ImageBuilder.build(entry.getKey()));
            DockerImageName expected = entry.getValue();

            assertEquals("expected " + expected.asCanonicalNameString() + " but was " + actual.asCanonicalNameString(),
                         expected, actual);
        }

        // TODO write test when using an internal registry
    }

    @Test
    public void imageBuilderSubstitutorErrorTest() {
        try {
            ImageBuilder.build(null);
            fail("Should have thrown NullPointerException");
        } catch (NullPointerException npe) {
            //pass
        }

        try {
            ImageBuilder.build("quay.io/testcontainers/ryuk:1.0.0");
            fail("Should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException iae) {
            //pass
        }

    }

    private DockerImageName getImage(ImageBuilder builder) throws Exception {
        Field image = builder.getClass().getDeclaredField("image");
        image.setAccessible(true);
        return (DockerImageName) image.get(builder);
    }
}
