/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.openapi20.test.merge.parts;

import static io.openliberty.microprofile.openapi20.test.merge.AssertModel.assertModelMaps;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.junit.Assert;
import org.junit.Test;

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

    private void validateMap(Map<String, ?> map, Map<?, ?>... maps) {
        Map<Object, Object> expectedMap = new HashMap<>();
        for (Map<?, ?> m : maps) {
            expectedMap.putAll(m);
        }
        assertModelMaps(expectedMap, map);
    }
}
