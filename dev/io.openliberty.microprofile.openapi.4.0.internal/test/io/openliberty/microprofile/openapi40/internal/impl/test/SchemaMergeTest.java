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

import static org.junit.Assert.assertEquals;

import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.junit.BeforeClass;
import org.junit.Test;

import io.openliberty.microprofile.openapi20.test.merge.parts.TestUtils;

public class SchemaMergeTest {

    @BeforeClass
    public static void setup() {
        TestUtils.setCurrent(new TestUtil40Impl());
    }

    /**
     * Test that when a schema has custom properties, any references in objects under those properties are updated as needed.
     */
    @Test
    public void testSchemaCustomProperties() {
        // Create two docs with conflicting schemas
        OpenAPI doc1 = OASFactory.createOpenAPI();
        Schema schema1 = OASFactory.createSchema()
                                   .description("schema1");
        doc1.components(OASFactory.createComponents().addSchema("conflictKey", schema1));

        OpenAPI doc2 = OASFactory.createOpenAPI();
        Schema schema2 = OASFactory.createSchema()
                                   .description("schema2");
        doc2.components(OASFactory.createComponents().addSchema("conflictKey", schema2));

        // Create a test schema with a child under a custom property name which references the conflicting schema
        Schema testSchema = OASFactory.createSchema()
                                      .description("testSchema");
        testSchema.set("testKey", OASFactory.createSchema().ref("conflictKey"));
        doc2.getComponents().addSchema("testSchema", testSchema);

        // Merge the two schemas
        OpenAPI merged = TestUtils.current().merge(doc1, doc2);

        // the conflictKey schema from doc2 should be renamed
        assertEquals("schema2", merged.getComponents().getSchemas().get("conflictKey1").getDescription());

        // the reference in testSchema from doc2 should have been updated
        Schema mergedTestSchema = merged.getComponents().getSchemas().get("testSchema");
        String mergedRef = ((Schema) mergedTestSchema.get("testKey")).getRef();
        assertEquals("#/components/schemas/conflictKey1", mergedRef);
    }
}
