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

import io.openliberty.mcp.annotations.Tool;
import jakarta.json.JsonObject;

public class ToolDescription {

    private final String name;
    private final String title;
    private final String description;
    private final JsonObject inputSchema;
    private final JsonObject outputSchema;
    private final AnnotationsDescription annotations;

    public String getName() {
        return name;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public JsonObject getInputSchema() {
        return inputSchema;
    }

    public JsonObject getOutputSchema() {
        return outputSchema;
    }

    public AnnotationsDescription getAnnotations() {
        return annotations;
    }

    public ToolDescription(ToolMetadata toolMetadata) {
        this.name = toolMetadata.name();
        this.title = toolMetadata.title();
        this.description = toolMetadata.description();

        Tool.Annotations ann = toolMetadata.annotation().annotations();
        if (isDefaultAnnotation(ann)) {
            this.annotations = null;
        } else {
            this.annotations = new AnnotationsDescription(
                                                          ann.readOnlyHint() == false ? null : ann.readOnlyHint(),
                                                          ann.destructiveHint() == true ? null : ann.destructiveHint(),
                                                          ann.idempotentHint() == false ? null : ann.idempotentHint(),
                                                          ann.openWorldHint() == true ? null : ann.openWorldHint(),
                                                          ann.title().isEmpty() ? null : ann.title());
        }
        this.inputSchema = toolMetadata.inputSchema();
        this.outputSchema = toolMetadata.outputSchema();
    }

    /*
     * Helper Method for default Annotation
     */

    private boolean isDefaultAnnotation(Tool.Annotations ann) {
        return ann.readOnlyHint() == false
               && ann.destructiveHint() == true
               && ann.idempotentHint() == false
               && ann.openWorldHint() == true
               && ann.title().isEmpty();

    }

    public record AnnotationsDescription(
                                         Boolean readOnlyHint,
                                         Boolean destructiveHint,
                                         Boolean idempotentHint,
                                         Boolean openWorldHint,
                                         String title) {}
}