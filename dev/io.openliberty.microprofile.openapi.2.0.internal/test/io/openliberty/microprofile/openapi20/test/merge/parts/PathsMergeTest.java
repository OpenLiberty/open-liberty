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

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.Paths;
import org.junit.Assert;
import org.junit.Test;

import io.openliberty.microprofile.openapi20.OpenAPIProvider;
import io.openliberty.microprofile.openapi20.merge.MergeProcessor;

public class PathsMergeTest {

    @Test
    public void testPathsMerger() {
        OpenAPI primaryOpenAPI;

        OpenAPI doc1 = OASFactory.createOpenAPI();
        Paths paths = OASFactory.createPaths();
        paths.addPathItem("/common", OASFactory.createPathItem());
        paths.addPathItem("/users", OASFactory.createPathItem());
        paths.addPathItem("/events", OASFactory.createPathItem());
        doc1.setPaths(paths);

        OpenAPI doc2 = OASFactory.createOpenAPI();
        paths = OASFactory.createPaths();
        paths.addPathItem("/common", OASFactory.createPathItem());
        paths.addPathItem("/feed", OASFactory.createPathItem());
        paths.addPathItem("/followers", OASFactory.createPathItem());
        doc2.setPaths(paths);

        OpenAPI doc3 = OASFactory.createOpenAPI();
        paths = OASFactory.createPaths();
        paths.addPathItem("/stores", OASFactory.createPathItem());
        paths.addPathItem("/orders", OASFactory.createPathItem());
        doc3.setPaths(paths);

        Paths doc1Paths = doc1.getPaths();
        Paths doc2Paths = doc2.getPaths();
        Paths doc3Paths = doc3.getPaths();

        primaryOpenAPI = TestUtil.merge(doc1);
        validatePaths(primaryOpenAPI.getPaths(), doc1Paths);

        primaryOpenAPI = mergeAndAssertClashes(doc1, doc2);
        validatePaths(primaryOpenAPI.getPaths(), doc1Paths);

        primaryOpenAPI = mergeAndAssertClashes(doc1, doc2, doc3);
        validatePaths(primaryOpenAPI.getPaths(), doc1Paths, doc3Paths);

        primaryOpenAPI = TestUtil.merge(doc1, doc3);
        validatePaths(primaryOpenAPI.getPaths(), doc1Paths, doc3Paths);

        primaryOpenAPI = TestUtil.merge(doc3);
        validatePaths(primaryOpenAPI.getPaths(), doc3Paths);
        
        primaryOpenAPI = TestUtil.merge(doc2, doc3);
        validatePaths(primaryOpenAPI.getPaths(), doc2Paths, doc3Paths);

        primaryOpenAPI = TestUtil.merge();
        Assert.assertNull(primaryOpenAPI.getPaths());

    }
    
    private OpenAPI mergeAndAssertClashes(OpenAPI... docs) {
        List<OpenAPIProvider> providers = Arrays.stream(docs)
                                                .map(TestUtil::createProvider)
                                                .collect(Collectors.toList());
        OpenAPIProvider result = MergeProcessor.mergeDocuments(providers);
        
        assertThat(result.getMergeProblems(), is(not(empty())));
        
        return result.getModel();
    }

    private void validatePaths(Paths primaryOpenAPIPaths, Paths... documentPaths) {
        Assert.assertNotNull(primaryOpenAPIPaths);
        Set<String> actualpathsKeys = primaryOpenAPIPaths.getPathItems().keySet();

        List<Paths> docPaths = Arrays.asList(documentPaths);

        Set<String> docsKeys = docPaths.stream().flatMap(paths -> {
            return paths.getPathItems().keySet().stream();
        }).collect(Collectors.toSet());

        Assert.assertEquals(docsKeys, actualpathsKeys);
    }
}
