/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.openapi40.internal.impl.test;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasKey;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.junit.BeforeClass;
import org.junit.Test;

import io.openliberty.microprofile.openapi20.test.merge.parts.TestUtils;

public class WebhookMergeTest {

    @BeforeClass
    public static void setup() {
        TestUtils.setCurrent(new TestUtil40Impl());
    }

    @Test
    public void testWebhookMerge() {
        OpenAPI doc1 = OASFactory.createOpenAPI();
        doc1.addWebhook("postData", OASFactory.createPathItem()
                                              .POST(OASFactory.createOperation()
                                                              .operationId("postData")));

        OpenAPI doc2 = OASFactory.createOpenAPI();
        // Identical to doc1
        doc2.addWebhook("postData", OASFactory.createPathItem()
                                              .POST(OASFactory.createOperation()
                                                              .operationId("postData")));
        // New, not in doc1
        doc2.addWebhook("sendUpdate", OASFactory.createPathItem()
                                                .POST(OASFactory.createOperation()
                                                                .operationId("sendUpdate")));

        OpenAPI doc3 = OASFactory.createOpenAPI();
        // Clashes with doc1 & doc2
        doc3.addWebhook("postData", OASFactory.createPathItem()
                                              .POST(OASFactory.createOperation()
                                                              .operationId("postDifferentData")));

        OpenAPI merged = TestUtils.current().merge(doc1);
        assertThat(merged.getWebhooks(), hasKey("postData"));

        merged = TestUtils.current().merge(doc2);
        assertThat(merged.getWebhooks().keySet(), containsInAnyOrder("postData", "sendUpdate"));

        merged = TestUtils.current().merge(doc1, doc2);
        assertThat(merged.getWebhooks().keySet(), containsInAnyOrder("postData", "sendUpdate"));

        merged = TestUtils.current().merge(doc1, doc2, doc3);
        assertThat(merged.getWebhooks().keySet(), containsInAnyOrder("postData", "postData1", "sendUpdate"));
        assertEquals("postData", merged.getWebhooks().get("postData").getPOST().getOperationId());
        assertEquals("postDifferentData", merged.getWebhooks().get("postData1").getPOST().getOperationId());
    }
}
