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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import io.openliberty.mcp.content.ContentEncoder;
import io.openliberty.mcp.messaging.Encoder;
import io.openliberty.mcp.tools.ToolResponseEncoder;
import jakarta.annotation.Priority;

public class EncoderRegistry {

    private static final int DEFAULT_ENCODER_PRIORITY = 0;
    private List<ToolResponseEncoder<?>> toolResponseEncoders = new ArrayList<>();
    private List<ContentEncoder<?>> contentEncoders = new ArrayList<>();
    private final EncoderRegistry globalRegistry;

    /**
     * Constructor for module registries (receives global reference)
     */
    public EncoderRegistry(EncoderRegistry globalRegistry) {
        this.globalRegistry = globalRegistry;
    }

    /**
     * Constructor for the global registry (which no parent registry)
     */
    public EncoderRegistry() {
        this.globalRegistry = null;
    }

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
        int result = DEFAULT_ENCODER_PRIORITY;
        Class<?> encoderClass = encoder.getClass();

        while (encoderClass != null && encoderClass != Object.class) {
            Priority priority = encoderClass.getAnnotation(Priority.class);
            if (priority != null) {
                result = priority.value();
                break;
            }
            encoderClass = encoderClass.getSuperclass();
        }
        return result;
    }

    public Optional<Encoder<?, ?>> findEncoder(Class<?> returnType) {
        // Check local encoders first
        Optional<Encoder<?, ?>> encoder = findEncoderLocally(returnType);

        // Fallback to global if not found and we have a global registry
        if (encoder.isEmpty() && globalRegistry != null) {
            encoder = globalRegistry.findEncoder(returnType);
        }

        return encoder;
    }

    /**
     * Search for encoder in this registry's local encoders only (no fallback)
     */
    private Optional<Encoder<?, ?>> findEncoderLocally(Class<?> returnType) {
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
