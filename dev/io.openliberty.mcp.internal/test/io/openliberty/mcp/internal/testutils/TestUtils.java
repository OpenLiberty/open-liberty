/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.testutils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import io.openliberty.mcp.annotations.Tool;
import io.openliberty.mcp.internal.ToolMetadata;

/**
 *
 */
public class TestUtils {
    /**
     * finds and mocks method of tool method
     *
     * @param cls
     * @param name
     * @return the matching {@link MockAnnotatedMethod}
     */
    public static MockAnnotatedMethod<Object> findMethod(Class<?> cls, String name) {
        List<MockAnnotatedMethod<Object>> tools = Arrays.stream(cls.getDeclaredMethods())
                                                        .filter(m -> m.isAnnotationPresent(Tool.class))
                                                        .filter(m -> m.getName().equals(name))
                                                        .map(m -> new MockAnnotatedMethod<>(m))
                                                        .collect(Collectors.toList());
        if (tools.size() != 1) {
            throw new RuntimeException("Found " + tools.size() + " tools with name " + name);
        }

        return tools.get(0);
    }

    /**
     * finds and mocks ToolMetadata of tool method
     *
     * @param cls
     * @param name
     * @return the matching {@link ToolMetadata}
     */
    public static ToolMetadata findTool(Class<?> cls, String name) {
        List<ToolMetadata> tools = Arrays.stream(cls.getDeclaredMethods())
                                         .filter(m -> m.isAnnotationPresent(Tool.class))
                                         .map(m -> ToolMetadata.createFrom(m.getAnnotation(Tool.class), null, new MockAnnotatedMethod<>(m)))
                                         .filter(m -> m.name().equals(name))
                                         .collect(Collectors.toList());
        if (tools.size() != 1) {
            throw new RuntimeException("Found " + tools.size() + " tools with name " + name);
        }

        return tools.get(0);
    }
}
