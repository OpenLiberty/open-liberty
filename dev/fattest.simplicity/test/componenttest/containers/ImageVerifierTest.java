/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package componenttest.containers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.testcontainers.utility.DockerImageName;

public class ImageVerifierTest {

    @Test
    public void updateImageVersionRequiredTest() throws Exception {
        DockerImageName compareTo = DockerImageName.parse("quay.io/ibm/openliberty:1.0.0");

        DockerImageName differentRegistry = DockerImageName.parse("icr.io/ibm/openliberty:1.0.0");
        DockerImageName noRegistry = DockerImageName.parse("ibm/openliberty:1.0.0");

        assertFalse(invokeUpdateImageVersionRequired(compareTo, differentRegistry));
        assertFalse(invokeUpdateImageVersionRequired(compareTo, noRegistry));

        DockerImageName differentRepository = DockerImageName.parse("quay.io/kyleaure/openliberty:1.0.0");
        DockerImageName noRepository = DockerImageName.parse("quay.io/openliberty:1.0.0");

        assertFalse(invokeUpdateImageVersionRequired(compareTo, differentRepository));
        assertFalse(invokeUpdateImageVersionRequired(compareTo, noRepository));

        DockerImageName differentArtifact = DockerImageName.parse("quay.io/ibm/websphereliberty:1.0.0");

        assertFalse(invokeUpdateImageVersionRequired(compareTo, differentArtifact));

        DockerImageName differentVersion = DockerImageName.parse("quay.io/ibm/openliberty:1.0.1");
        DockerImageName latestVersion = DockerImageName.parse("quay.io/ibm/openliberty:latest");

        assertTrue(invokeUpdateImageVersionRequired(compareTo, differentVersion));
        assertTrue(invokeUpdateImageVersionRequired(compareTo, latestVersion));
    }

    @Test
    public void compareImagesTest() throws Exception {
        List<String> results;

        // Test empty sets
        results = invokeCompareImages(Collections.emptySet(), Collections.emptySet());
        assertEquals(0, results.size());
        assertTrue(String.join(System.lineSeparator(), results).isEmpty());

        // Test no set intersection
        Set<DockerImageName> of3Images = new HashSet<>(Arrays.asList(DockerImageName.parse("openliberty:1.0.0"),
                                                                     DockerImageName.parse("websphereliberty:1.0.0"),
                                                                     DockerImageName.parse("db2:1.0.0")));

        results = invokeCompareImages(of3Images, Collections.emptySet());
        assertEquals(0, results.size());

        results = invokeCompareImages(Collections.emptySet(), of3Images);
        assertEquals(0, results.size());

        // Test set intersection
        Set<DockerImageName> expected;
        Set<DockerImageName> forgotten;

        expected = Collections.unmodifiableSet(of3Images);
        forgotten = new HashSet<>(Arrays.asList(DockerImageName.parse("oracle:18.0.0.0"),
                                                DockerImageName.parse("sqlserver:11.0.0"),
                                                DockerImageName.parse("db2:1.0.1")));

        results = invokeCompareImages(expected, forgotten);
        assertEquals(1, results.size());
        assertEquals(expected.size(), forgotten.size() + results.size());

    }

    private static List<String> invokeCompareImages(Set<DockerImageName> expected, Set<DockerImageName> forgotten) throws Exception {
        return (List<String>) getCompareImages().invoke(null, expected, forgotten);
    }

    private static Method getCompareImages() throws Exception {
        Method method = ImageVerifier.class.getDeclaredMethod("compareImages", Set.class, Set.class);
        method.setAccessible(true);
        return method;
    }

    private static boolean invokeUpdateImageVersionRequired(DockerImageName a, DockerImageName b) throws Exception {
        return (boolean) getUpdateImageVersionRequired().invoke(null, a, b);
    }

    private static Method getUpdateImageVersionRequired() throws Exception {
        Method method = ImageVerifier.class.getDeclaredMethod("updateImageVersionRequired", DockerImageName.class, DockerImageName.class);
        method.setAccessible(true);
        return method;
    }
}
