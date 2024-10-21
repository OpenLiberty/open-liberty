/*******************************************************************************
 * Copyright (c) 2021, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.openapi20.test.merge;

import static io.openliberty.microprofile.openapi20.test.merge.AssertModel.assertModelsEqual;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.stringContainsInOrder;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.junit.Assert;
import org.junit.Test;

import io.openliberty.microprofile.openapi20.internal.merge.ModelCopy;
import io.openliberty.microprofile.openapi20.internal.services.MergeProcessor;
import io.openliberty.microprofile.openapi20.internal.services.OpenAPIProvider;
import io.openliberty.microprofile.openapi20.test.merge.parts.TestUtil.Format;
import io.openliberty.microprofile.openapi20.test.merge.parts.TestUtils;

public class MergeProcessorTest {

    MergeProcessor mergeProcessor = TestUtils.current().getMergeProcessor();

    /**
     * Test merge where context roots should be prepended to the start of paths, avoiding a clash
     */
    @Test
    public void testClashingPathWithContextRoots() {
        OpenAPIProvider model1 = loadModel("clashing-path-1.yaml", "/foo");
        OpenAPIProvider model2 = loadModel("clashing-path-2.yaml", "/bar");

        OpenAPIProvider resultProvider = mergeProcessor.mergeDocuments(Arrays.asList(model1, model2));
        OpenAPI result = resultProvider.getModel();
        assertThat(resultProvider.getApplicationPath(), is(nullValue()));

        assertThat(result.getComponents().getSchemas(), allOf(hasKey("bar"), hasKey("error"), hasKey("foo")));
        assertThat(result.getPaths().getPathItems(), allOf(hasKey("/foo/foo"), hasKey("/foo/login"), hasKey("/bar/bar"), hasKey("/bar/login")));

        OpenAPI expectedModel = loadModel("clashing-path-merged.yaml");
        assertModelsEqual(expectedModel, result);
    }

    /**
     * Test that a path clash where the context root cannot be prepended to the paths causes a merge problem
     */
    @Test
    public void testPathClashNoContextRootPrepend() {
        OpenAPIProvider model1 = loadModel("clashing-path-with-server-1.yaml", "/test1");
        OpenAPIProvider model2 = loadModel("clashing-path-with-server-2.yaml", "/test2");

        OpenAPIProvider resultProvider = mergeProcessor.mergeDocuments(Arrays.asList(model1, model2));

        // Path X from module Y clashes with module Z. Module Y will not be included.
        assertThat("Merge problems", resultProvider.getMergeProblems(),
                   contains(stringContainsInOrder(Arrays.asList("/test", "clashing-path-with-server-2.yaml", "clashing-path-with-server-1.yaml",
                                                                "clashing-path-with-server-2.yaml"))));

        // As there's only one model in the final merge, it should be returned without modification
        assertModelsEqual(model1.getModel(), resultProvider.getModel());
    }

    /**
     * Test that components are renamed if they clash and that references are updated accordingly
     */
    @Test
    public void testComponentNameClash() throws IOException {
        OpenAPIProvider model1 = loadModel("component-clash-1.yaml", "/test1");
        OpenAPIProvider model2 = loadModel("component-clash-2.yaml", "/test2");

        assertModelsEqual(model1.getModel(), (OpenAPI) ModelCopy.copy(model1.getModel()));
        assertModelsEqual(model2.getModel(), (OpenAPI) ModelCopy.copy(model2.getModel()));

        OpenAPIProvider resultProvider = mergeProcessor.mergeDocuments(Arrays.asList(model1, model2));
        OpenAPI result = resultProvider.getModel();
        assertThat(resultProvider.getApplicationPath(), is(nullValue()));
        assertThat(resultProvider.getMergeProblems(), is(empty()));

        System.out.println(TestUtils.current().serialize(resultProvider.getModel(), Format.YAML));

        OpenAPI expectedModel = loadModel("component-clash-merge.yaml");
        assertModelsEqual(expectedModel, result);
    }

    /**
     * Test that components are not renamed if they're identical across documents
     */
    @Test
    public void testComponentMerging() throws IOException {
        OpenAPIProvider model1 = loadModel("component-merging-1.yaml", "/test1");
        OpenAPIProvider model2 = loadModel("component-merging-2.yaml", "/test2");

        OpenAPIProvider resultProvider = mergeProcessor.mergeDocuments(Arrays.asList(model1, model2));
        OpenAPI result = resultProvider.getModel();
        assertThat(resultProvider.getApplicationPath(), is(nullValue()));
        assertThat(resultProvider.getMergeProblems(), is(empty()));

        System.out.println(TestUtils.current().serialize(resultProvider.getModel(), Format.YAML));

        OpenAPI expectedModel = loadModel("component-merging-merge.yaml");
        assertModelsEqual(expectedModel, result);
    }

    /**
     * Test that context roots can be moved from servers to paths
     * <p>
     * In the situation where all servers in a model end with the context root for the module, the context root should be removed from each server and prepended to each path
     */
    @Test
    public void testRemovingContextRootFromServers() {
        OpenAPIProvider model1 = loadModel("server-includes-context-root-1.yaml", "/test1");
        OpenAPIProvider model2 = loadModel("server-includes-context-root-2.yaml", "/test2");

        OpenAPIProvider resultProvider = mergeProcessor.mergeDocuments(Arrays.asList(model1, model2));
        OpenAPI result = resultProvider.getModel();
        assertThat(resultProvider.getApplicationPath(), is(nullValue()));
        assertThat(resultProvider.getMergeProblems(), is(empty()));

        OpenAPI expectedModel = loadModel("server-includes-context-root-merged.yaml");
        assertModelsEqual(expectedModel, result);
    }

    /**
     * Test that operationIds are renamed if they clash, and that references to operations and to operationIds are updated correctly
     */
    @Test
    public void testClashingOperationId() throws IOException {
        OpenAPIProvider model1 = loadModel("operationid-clash-1.yaml", "/test1");
        OpenAPIProvider model2 = loadModel("operationid-clash-2.yaml", "/test2");

        OpenAPIProvider resultProvider = mergeProcessor.mergeDocuments(Arrays.asList(model1, model2));
        OpenAPI result = resultProvider.getModel();
        assertThat(resultProvider.getApplicationPath(), is(nullValue()));
        assertThat(resultProvider.getMergeProblems(), is(empty()));

        System.out.println(TestUtils.current().serialize(result, Format.YAML));

        OpenAPI expectedModel = loadModel("operationid-clash-merged.yaml");
        assertModelsEqual(expectedModel, result);
    }

    /**
     * Test that clashing tag names are handled correctly
     * <p>
     * <ul>
     * <li>Tags defined at the top level are split if their definition is different (e.g. they have the same name but different descriptions)</li>
     * <li>Ad-hoc tags used without prior definition are never split</li>
     * </ul>
     */
    @Test
    public void testClashingTags() {
        OpenAPIProvider model1 = loadModel("tag-clash-1.yaml", "");
        OpenAPIProvider model2 = loadModel("tag-clash-2.yaml", "");

        OpenAPIProvider resultProvider = mergeProcessor.mergeDocuments(Arrays.asList(model1, model2));
        OpenAPI result = resultProvider.getModel();
        assertThat(resultProvider.getApplicationPath(), is(nullValue()));
        assertThat(resultProvider.getMergeProblems(), is(empty()));

        OpenAPI expectedModel = loadModel("tag-clash-merged.yaml");
        assertModelsEqual(expectedModel, result);
    }

    @Test
    public void testServerUnderPaths() {
        OpenAPIProvider model1 = loadModel("server-under-path-1.yaml", "/test1");
        OpenAPIProvider model2 = loadModel("server-under-path-2.yaml", "/test2");

        OpenAPIProvider resultProvider = mergeProcessor.mergeDocuments(Arrays.asList(model1, model2));
        OpenAPI result = resultProvider.getModel();
        assertThat(resultProvider.getApplicationPath(), is(nullValue()));
        assertThat(resultProvider.getMergeProblems(), is(empty()));

        OpenAPI expectedModel = loadModel("server-under-path-merged.yaml");
        assertModelsEqual(expectedModel, result);
    }

    @Test
    public void testServerMovedToPaths() {
        OpenAPIProvider model1 = loadModel("server-moved-to-paths-1.yaml", "/foo");
        OpenAPIProvider model2 = loadModel("server-moved-to-paths-2.yaml", "/bar");

        OpenAPIProvider resultProvider = mergeProcessor.mergeDocuments(Arrays.asList(model1, model2));
        OpenAPI result = resultProvider.getModel();
        assertThat(resultProvider.getApplicationPath(), is(nullValue()));
        assertThat(resultProvider.getMergeProblems(), is(empty()));

        OpenAPI expectedModel = loadModel("server-moved-to-paths-merged.yaml");
        assertModelsEqual(expectedModel, result);
    }

    @Test
    public void testSecurityMovedToOperations() {
        OpenAPIProvider model1 = loadModel("test-security-moved-1.yaml", "/test1");
        OpenAPIProvider model2 = loadModel("test-security-moved-2.yaml", "/test2");

        OpenAPIProvider resultProvider = mergeProcessor.mergeDocuments(Arrays.asList(model1, model2));
        OpenAPI result = resultProvider.getModel();
        assertThat(resultProvider.getApplicationPath(), is(nullValue()));
        assertThat(resultProvider.getMergeProblems(), is(empty()));

        OpenAPI expectedModel = loadModel("test-security-moved-merged.yaml");
        assertModelsEqual(expectedModel, result);
    }

    /**
     * Test that external docs links are retained when they're identical
     */
    @Test
    public void testDocsIdentical() {
        OpenAPIProvider model1 = loadModel("docs-identical-1.yaml", "/test1");
        OpenAPIProvider model2 = loadModel("docs-identical-2.yaml", "/test2");

        OpenAPIProvider resultProvider = mergeProcessor.mergeDocuments(Arrays.asList(model1, model2));
        OpenAPI result = resultProvider.getModel();
        assertThat(resultProvider.getApplicationPath(), is(nullValue()));
        assertThat(resultProvider.getMergeProblems(), is(empty()));

        OpenAPI expectedModel = loadModel("docs-identical-merged.yaml");
        assertModelsEqual(expectedModel, result);
    }

    /**
     * Test that external docs links are removed if they're different
     */
    @Test
    public void testDocsRemoved() {
        OpenAPIProvider model1 = loadModel("docs-removed-1.yaml", "/test1");
        OpenAPIProvider model2 = loadModel("docs-removed-2.yaml", "/test2");

        OpenAPIProvider resultProvider = mergeProcessor.mergeDocuments(Arrays.asList(model1, model2));
        OpenAPI result = resultProvider.getModel();
        assertThat(resultProvider.getApplicationPath(), is(nullValue()));
        assertThat(resultProvider.getMergeProblems(), is(empty()));

        OpenAPI expectedModel = loadModel("docs-removed-merged.yaml");
        assertModelsEqual(expectedModel, result);
    }

    /**
     * Test that external docs links are removed if they're different
     */
    @Test
    public void testInfoIdentical() {
        OpenAPIProvider model1 = loadModel("info-identical-1.yaml", "/test1");
        OpenAPIProvider model2 = loadModel("info-identical-2.yaml", "/test2");

        OpenAPIProvider resultProvider = mergeProcessor.mergeDocuments(Arrays.asList(model1, model2));
        OpenAPI result = resultProvider.getModel();
        assertThat(resultProvider.getApplicationPath(), is(nullValue()));
        assertThat(resultProvider.getMergeProblems(), is(empty()));

        OpenAPI expectedModel = loadModel("info-identical-merged.yaml");
        assertModelsEqual(expectedModel, result);
    }

    /**
     * Test that if a model is not included in the merge because there were unresolvable clashes, it doesn't cause changes in the final model
     * <p>
     * In particular, it shouldn't cause components or tags to be renamed or info or external docs to be removed
     */
    @Test
    public void testClashingModelDoesntCauseChanges() {
        OpenAPIProvider model1 = loadModel("no-phantom-changes-1.yaml", "/test1");
        OpenAPIProvider model2 = loadModel("no-phantom-changes-2.yaml", "/test2");
        OpenAPIProvider model3 = loadModel("no-phantom-changes-3.yaml", "/test3");

        // Note, model 2 clashes with model 1 and so won't be included in the final result
        // Names in model 3 overlap with model 2 and would be renamed, but shouldn't be because model 2 was discarded
        // Similarly, model 2 has a different info and external docs section so it would cause those to be discarded from the final model
        OpenAPIProvider resultProvider = mergeProcessor.mergeDocuments(Arrays.asList(model1, model2, model3));
        OpenAPI result = resultProvider.getModel();
        assertThat(resultProvider.getApplicationPath(), is(nullValue()));
        assertThat(resultProvider.getMergeProblems(), contains(containsString("no-phantom-changes-2")));

        OpenAPI expectedModel = loadModel("no-phantom-changes-merged.yaml");
        assertModelsEqual(expectedModel, result);
    }

    /**
     * Test that clashing extensions at the top level cause the clashing document not to be included
     */
    @Test
    public void testClashingExtension() {
        OpenAPIProvider model1 = loadModel("clashing-extension-1.yaml", "/test1");
        OpenAPIProvider model2 = loadModel("clashing-extension-2.yaml", "/test2");

        OpenAPIProvider resultProvider = mergeProcessor.mergeDocuments(Arrays.asList(model1, model2));
        OpenAPI result = resultProvider.getModel();
        // Extension key X in module Y conflicts with module Z, module Y will not be merged
        assertThat("Merge problems", resultProvider.getMergeProblems(),
                   contains(stringContainsInOrder(Arrays.asList("x-foo", "clashing-extension-2.yaml", "clashing-extension-1.yaml", "clashing-extension-2.yaml"))));

        assertThat(resultProvider.getApplicationPath(), is("/test1"));

        assertModelsEqual(model1.getModel(), result);
    }

    private OpenAPI loadModel(String modelResource) {
        try (InputStream is = MergeProcessorTest.class.getResourceAsStream(modelResource)) {
            Assert.assertNotNull("Test file not loaded: " + modelResource, is);
            return TestUtils.current().parse(is, Format.YAML);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private OpenAPIProvider loadModel(String modelResource, String contextRoot) {
        OpenAPI model = loadModel(modelResource);
        return new OpenAPIProvider() {

            @Override
            public OpenAPI getModel() {
                return model;
            }

            @Override
            public String getApplicationPath() {
                return contextRoot;
            }

            @Override
            public List<String> getMergeProblems() {
                return Collections.emptyList();
            }

            @Override
            public String toString() {
                return "Test model[" + modelResource + "]";
            }
        };
    }
}
