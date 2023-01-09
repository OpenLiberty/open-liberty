/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.openapi20.test.merge.parts;

import static io.openliberty.microprofile.openapi20.test.merge.AssertModel.assertModelMaps;
import static org.hamcrest.Matchers.hasKey;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.models.Extensible;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.junit.Assert;
import org.junit.Test;

import io.openliberty.microprofile.openapi20.internal.merge.ModelEquality;
import io.openliberty.microprofile.openapi20.internal.merge.ModelType;

/**
 *
 */
public class ExtensionsMergeTest {
    @Test
    public void testExtensionsMerge() {
        OpenAPI primaryOpenAPI;
        OpenAPI doc1 = OASFactory.createOpenAPI();
        OpenAPI doc2 = OASFactory.createOpenAPI();
        OpenAPI doc3 = OASFactory.createOpenAPI();

        doc1.addExtension("x-ibm", new HashMap<>());
        doc1.addExtension("x-ibm-1", new HashMap<>());
        doc1.addExtension("x-ibm-2", new HashMap<>());

        doc2.addExtension("x-ibm", new HashMap<>());
        doc2.addExtension("x-ibm-3", new HashMap<>());
        doc2.addExtension("x-ibm-4", new HashMap<>());

        doc3.addExtension("x-ibm", new HashMap<>());
        doc3.addExtension("x-ibm-5", new HashMap<>());
        doc3.addExtension("x-ibm-6", new HashMap<>());

        primaryOpenAPI = TestUtil.merge(doc1);
        validateMap(primaryOpenAPI.getExtensions(), doc1.getExtensions());
        primaryOpenAPI = TestUtil.merge(doc1, doc2);
        validateMap(primaryOpenAPI.getExtensions(), doc1.getExtensions(), doc2.getExtensions());
        primaryOpenAPI = TestUtil.merge(doc1, doc2, doc3);
        validateMap(primaryOpenAPI.getExtensions(), doc1.getExtensions(), doc2.getExtensions(), doc3.getExtensions());

        primaryOpenAPI = TestUtil.merge(doc1, doc3);
        validateMap(primaryOpenAPI.getExtensions(), doc1.getExtensions(), doc3.getExtensions());
        primaryOpenAPI = TestUtil.merge(doc3);
        validateMap(primaryOpenAPI.getExtensions(), doc3.getExtensions());
        primaryOpenAPI = TestUtil.merge();
        Assert.assertNull(primaryOpenAPI.getExtensions());
    }

    @Test
    public void testExtensionEquality() {
        for (ModelType mt : ModelType.values()) {
            Object testInstance = mt.createInstance();
            if (testInstance instanceof Extensible) {
                testExtensionEquality(mt);
            }
        }
    }

    public void testExtensionEquality(ModelType mt) {
        Extensible<?> a = (Extensible<?>) mt.createInstance();
        Extensible<?> b = (Extensible<?>) mt.createInstance();

        // Note, we're testing the ModelEquality.equals method here
        // which is why we're using assertTrue, rather than assertEquals()
        // or assertModelsEqual()
        assertTrue(mt + " not equal for original object", ModelEquality.equals(a, b));

        a.addExtension("x-foo", "bar");
        assertThat(a.getExtensions(), hasKey("x-foo"));
        assertFalse(mt + " equal after adding extension", ModelEquality.equals(a, b));

        b.addExtension("x-foo", "bar");
        assertTrue(mt + " not equal after adding similar extensions", ModelEquality.equals(a, b));
        b.removeExtension("x-foo");

        b.addExtension("x-foo", "baz");
        assertFalse(mt + " equal after adding equal extensions", ModelEquality.equals(a, b));
        b.removeExtension("x-foo");

        b.addExtension("x-qux", "bar");
        assertFalse(mt + " equal after adding different extension key", ModelEquality.equals(a, b));
    }

    private void validateMap(Map<String, ?> map, Map<?, ?>... maps) {
        Map<Object, Object> expectedMap = new HashMap<>();
        for (Map<?, ?> m : maps) {
            expectedMap.putAll(m);
        }
        assertModelMaps(expectedMap, map);
    }
}
