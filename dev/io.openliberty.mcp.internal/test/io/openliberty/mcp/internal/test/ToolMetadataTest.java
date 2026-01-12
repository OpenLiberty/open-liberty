/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import io.openliberty.mcp.annotations.Schema;
import io.openliberty.mcp.annotations.Tool;
import io.openliberty.mcp.annotations.ToolArg;
import io.openliberty.mcp.internal.schemas.SchemaRegistry;
import io.openliberty.mcp.internal.schemas.TypeUtility;
import io.openliberty.mcp.internal.testutils.TestUtils;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;

/**
 *
 */
public class ToolMetadataTest {
    Jsonb jsonb;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setup() throws Exception {
        SchemaRegistry.set(new SchemaRegistry());
        jsonb = JsonbBuilder.create();
    }

    @Tool(name = "addGenericToGenericArray", title = "adds generic to generic Array", description = "adds person to Generic Array, returns nothing")
    public @Schema(description = "Returns list of  object") <T> List<T> addGenericToGenericArray(@ToolArg(name = "generic list 1",
                                                                                                          description = "List of generics 1") T[] list1,
                                                                                                 @ToolArg(name = "generic list 2",
                                                                                                          description = "List of generics 1 ") List<List<T>>[] list2,
                                                                                                 @ToolArg(name = "generic", description = "Generic object") T item,
                                                                                                 @ToolArg(name = "concrete", description = "Concrete object") List<String> item2) {
        return null;
    }

    @Test
    public void testGenericParams() {
        Method tm = TestUtils.findActualMethod(ToolMetadataTest.class, "addGenericToGenericArray");
        assertTrue(TypeUtility.hasGenericParams(tm.getAnnotatedParameterTypes()[0].getType()));
        assertTrue(TypeUtility.hasGenericParams(tm.getAnnotatedParameterTypes()[1].getType()));
        assertTrue(TypeUtility.hasGenericParams(tm.getAnnotatedParameterTypes()[2].getType()));
        assertFalse(TypeUtility.hasGenericParams(tm.getAnnotatedParameterTypes()[3].getType()));

    }

}
