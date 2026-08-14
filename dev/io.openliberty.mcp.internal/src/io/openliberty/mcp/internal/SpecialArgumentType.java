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

import static java.util.stream.Collectors.toMap;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import org.mcpjava.server.Cancellation;
import org.mcpjava.server.McpRequest;
import org.mcpjava.server.progress.Progress;

public enum SpecialArgumentType {
    CANCELLATION(Cancellation.class),
    REQUEST(McpRequest.class),
    PROGRESS(Progress.class);

    private static final Map<Class<?>, SpecialArgumentType> typeByClass;

    static {
        typeByClass = Stream.of(SpecialArgumentType.values())
                            .collect(toMap(SpecialArgumentType::getTypeClass, Function.identity()));
    }

    private final Class<?> typeClass;

    SpecialArgumentType(Class<?> typeClass) {
        this.typeClass = typeClass;
    }

    public static Optional<SpecialArgumentType> fromClass(Type type) {
        return Optional.ofNullable(typeByClass.get(type));
    }

    public Class<?> getTypeClass() {
        return typeClass;
    }
}
