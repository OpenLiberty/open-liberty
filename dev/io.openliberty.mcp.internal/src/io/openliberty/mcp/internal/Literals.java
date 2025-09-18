/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal;

import java.util.Arrays;
import java.util.EnumSet;

import io.openliberty.mcp.annotations.Tool;
import io.openliberty.mcp.annotations.Tool.Annotations;
import io.openliberty.mcp.annotations.Tool.OutputSchema;
import jakarta.enterprise.util.AnnotationLiteral;

/**
 * Methods for creating MCP annotations in code
 */
public class Literals {

    private static abstract class ToolLiteral extends AnnotationLiteral<Tool> implements Tool {
        private static final long serialVersionUID = 1L;
    }

    /**
     * Creates a literal {@link Tool} annotation which doesn't specify any {@link Annotations} or structuredContent.
     */
    public static Tool tool(String name, String title, String description) {
        return tool(name, title, description, false, toolAnnotations(""), null);
    }

    /**
     * Creates a literal {@link Tool} annotation which doesn't specify any {@link Annotations}.
     */
    public static Tool tool(String name, String title, String description, boolean structuredContent) {
        return tool(name, title, description, structuredContent, toolAnnotations(""), null);
    }

    public static Tool tool(String name, String title, String description, boolean structuredContent, Annotations toolAnntations, OutputSchema outputSchema) {
        return new ToolLiteral() {
            private static final long serialVersionUID = 1L;

            @Override
            public String title() {
                return title;
            }

            @Override
            public String name() {
                return name;
            }

            @Override
            public String description() {
                return description;
            }

            @Override
            public boolean structuredContent() {
                return structuredContent;
            }

            @Override
            public Annotations annotations() {
                return toolAnntations;
            }

            @Override
            public OutputSchema outputSchema() {
                return outputSchema;
            }
        };
    }

    private static abstract class ToolAnnotationsLiteral extends AnnotationLiteral<Annotations> implements Annotations {
        private static final long serialVersionUID = 1L;
    }

    /**
     * Creates a literal {@link Annotations Tool.Annotations} annotation
     *
     * @param title the title
     * @param hintArgs the hints to set for the tool. An empty list represents the default settings. Each of the entries in {@link ToolAnnotationHints} represents a change from the
     *     default.
     * @return the Tool.Annotations object
     */
    public static Annotations toolAnnotations(String title, ToolAnnotationHints... hintArgs) {
        EnumSet<ToolAnnotationHints> hints = hintArgs.length == 0 ? EnumSet.noneOf(ToolAnnotationHints.class) : EnumSet.copyOf(Arrays.asList(hintArgs));
        return new ToolAnnotationsLiteral() {

            @Override
            public String title() {
                return title;
            }

            @Override
            public boolean readOnlyHint() {
                return hints.contains(ToolAnnotationHints.READ_ONLY);
            }

            @Override
            public boolean destructiveHint() {
                return !hints.contains(ToolAnnotationHints.NON_DESTRUCTIVE);
            }

            @Override
            public boolean idempotentHint() {
                return hints.contains(ToolAnnotationHints.IDEMPONENT);
            }

            @Override
            public boolean openWorldHint() {
                return !hints.contains(ToolAnnotationHints.NOT_OPEN_WORLD);
            }
        };
    }

    /**
     * The hints that can be set on a tool.
     * <p>
     * Each member of this enum represents a change from the default.
     * <p>
     * E.g. the default value for {@code Annotations.destructiveHint} is {@code true}, therefore we have the member {@link #NON_DESTRUCTIVE} to set this value to {@code false}.
     */
    public enum ToolAnnotationHints {
        READ_ONLY,
        NON_DESTRUCTIVE,
        IDEMPONENT,
        NOT_OPEN_WORLD
    }

}
