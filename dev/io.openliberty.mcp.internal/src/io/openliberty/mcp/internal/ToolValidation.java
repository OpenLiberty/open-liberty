/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.regex.Pattern;

import io.openliberty.mcp.internal.requests.BuiltinDefaultValueConverters;
import io.openliberty.mcp.internal.requests.DefaultValueConverter;
import io.openliberty.mcp.internal.schemas.TypeUtility;
import io.openliberty.mcp.tools.ToolManager.ToolArgument;

/**
 * Methods to validate tools and arguments
 * <p>
 * This code is separated out because the extension and ToolRegistry both need to validate tools but report the validation failures in different ways.
 * <p>
 * Annotated methods need an error logged which includes the fully qualified name of the method, while ToolRegistry throws exceptions.
 */
public class ToolValidation {

    public static final Pattern TOOL_NAME_CHARACTER_PATTERN = Pattern.compile("[\\w.-]+");

    public enum ToolNameErrors {
        INVALID_LENGTH,
        INVALID_CHARACTERS
    }

    public static Collection<ToolNameErrors> validateToolName(String name) {
        var result = EnumSet.noneOf(ToolNameErrors.class);
        if (name.length() == 0 || name.length() > 128) {
            result.add(ToolNameErrors.INVALID_LENGTH);
        }
        if (!TOOL_NAME_CHARACTER_PATTERN.matcher(name).matches()) {
            result.add(ToolNameErrors.INVALID_CHARACTERS);
        }
        return result;
    }

    public enum ToolArgumentErrorType {
        NAME_BLANK,
        NAME_MISSING,
        NO_CONVERTER,
        CONVERSION_ERROR,
    }

    public record ToolArgumentValidationError(ToolArgumentErrorType type, Throwable exception) {};

    public static Collection<ToolArgumentValidationError> validateToolArgument(ToolArgument argMetadata) {
        List<ToolArgumentValidationError> results = new ArrayList<>();
        // Check name
        if (argMetadata.name().isBlank()) {
            results.add(new ToolArgumentValidationError(ToolArgumentErrorType.NAME_BLANK, null));
        } else if (argMetadata.name().equals(ToolMetadata.MISSING_TOOL_ARG_NAME)) {
            results.add(new ToolArgumentValidationError(ToolArgumentErrorType.NAME_MISSING, null));
        }

        // Check default value
        if (argMetadata.defaultValue() != null && !argMetadata.defaultValue().isEmpty()) {
            Type typeWrapperClass = TypeUtility.box(argMetadata.type());
            DefaultValueConverter<?> converter = BuiltinDefaultValueConverters.CONVERTERS.get(typeWrapperClass);
            if (converter != null) {
                try {
                    converter.convert(argMetadata.defaultValue());
                } catch (Exception e) {
                    results.add(new ToolArgumentValidationError(ToolArgumentErrorType.CONVERSION_ERROR, e));
                }
            } else {
                results.add(new ToolArgumentValidationError(ToolArgumentErrorType.NO_CONVERTER, null));
            }
        }

        return results;
    }

}
