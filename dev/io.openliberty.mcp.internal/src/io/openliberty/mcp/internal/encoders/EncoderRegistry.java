/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.encoders;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import io.openliberty.mcp.content.ContentEncoder;
import io.openliberty.mcp.messaging.Encoder;
import io.openliberty.mcp.tools.ToolResponseEncoder;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class EncoderRegistry {

    private static final int DEFAULT_ENCODER_PRIORITY = 0;
    private List<ToolResponseEncoder<?>> toolResponseEncoders;
    private List<ContentEncoder<?>> contentEncoders;

    public void registerEncoders(List<ToolResponseEncoder<?>> toolResponseEncoders, List<ContentEncoder<?>> contentEncoders) {
        this.toolResponseEncoders = toolResponseEncoders;
        this.contentEncoders = contentEncoders;
        sortEncoders();
    }

    /**
     * Sort the registered encoders by priority (highest first, descending)
     */
    private void sortEncoders() {
        toolResponseEncoders.sort(Comparator.<ToolResponseEncoder<?>> comparingInt(toolResponseEncoder -> getPriority(toolResponseEncoder)).reversed());
        contentEncoders.sort(Comparator.<ContentEncoder<?>> comparingInt(contentEncoder -> getPriority(contentEncoder)).reversed());
    }

    private int getPriority(Object encoder) {
        Priority priority = encoder.getClass().getAnnotation(Priority.class);
        return priority != null ? priority.value() : DEFAULT_ENCODER_PRIORITY;
    }

    public Optional<Encoder<?, ?>> findEncoder(Class<?> returnType) {

        for (ToolResponseEncoder<?> encoder : toolResponseEncoders) {
            if (encoder.supports(returnType)) {
                return Optional.of(encoder);
            }
        }
        for (ContentEncoder<?> encoder : contentEncoders) {
            if (encoder.supports(returnType)) {
                return Optional.of(encoder);
            }
        }
        return Optional.empty();
    }

}
