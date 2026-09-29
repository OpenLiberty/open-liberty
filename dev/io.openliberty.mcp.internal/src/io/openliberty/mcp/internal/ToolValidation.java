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

    public static Collection<ToolArgumentValidationError> validateToolArgument(ToolArgument argMetadata, ConverterRegistry converterRegistry) {
        List<ToolArgumentValidationError> results = new ArrayList<>();
        // Check name
        if (argMetadata.name().isBlank()) {
            results.add(new ToolArgumentValidationError(ToolArgumentErrorType.NAME_BLANK, null));
        } else if (argMetadata.name().equals(ToolMetadata.MISSING_TOOL_ARG_NAME)) {
            results.add(new ToolArgumentValidationError(ToolArgumentErrorType.NAME_MISSING, null));
        }

        // Check default value
        if (argMetadata.defaultValue() != null && !argMetadata.defaultValue().isEmpty()) {
            Type boxedType = TypeUtility.box(argMetadata.type());
            converterRegistry.getConverter(boxedType).ifPresentOrElse(converter -> {
                try {
                    converter.convert(argMetadata.defaultValue());
                } catch (Exception e) {
                    results.add(new ToolArgumentValidationError(ToolArgumentErrorType.CONVERSION_ERROR, e));
                }
            }, () -> results.add(new ToolArgumentValidationError(ToolArgumentErrorType.NO_CONVERTER, null)));
        }
        return results;
    }

    /*
     * These rules taken from https://modelcontextprotocol.org/specification/2025-11-25/basic#_meta
     *
     * Prefix: If specified, MUST be a series of labels separated by dots (.), followed by a slash (/).
     * Labels: MUST start with a letter and end with a letter or digit; interior characters can be letters, digits, or hyphens (-).
     * Name: MUST begin and end with an alphanumeric character ([a-z0-9A-Z]), MAY contain hyphens (-), underscores (_), dots (.), and alphanumerics in between
     */
    private static final String META_PREFIX_LABEL = "[a-zA-Z](?:[a-zA-Z0-9\\-]*[a-zA-Z0-9])?";
    private static final String META_PREFIX = META_PREFIX_LABEL + "(?:\\." + META_PREFIX_LABEL + ")*/";
    private static final String META_NAME = "[a-zA-Z0-9](?:[a-zA-Z0-9\\-_\\.]*[a-zA-Z0-9])?";
    private static final Pattern META_PREFIX_LABEL_PATTERN = Pattern.compile(META_PREFIX_LABEL);
    private static final Pattern META_PREFIX_PATTERN = Pattern.compile(META_PREFIX);
    private static final Pattern META_NAME_PATTERN = Pattern.compile(META_NAME);
    private static final Pattern META_KEY_PATTERN = Pattern.compile("(?:" + META_PREFIX + ")?" + META_NAME);

    public static boolean isValidMetaKey(String metaKey) {
        if (metaKey == null) {
            return false;
        }
        if (metaKey.isEmpty()) {
            return true; // Empty name explicitly allowed in spec
        }
        return META_KEY_PATTERN.matcher(metaKey).matches();
    }

    public static boolean isValidMetaPrefix(String metaPrefix) {
        if (metaPrefix == null) {
            return false;
        }
        return META_PREFIX_PATTERN.matcher(metaPrefix).matches();
    }

    public static boolean isValidMetaPrefixLabel(String metaPrefixLabel) {
        if (metaPrefixLabel == null) {
            return false;
        }
        return META_PREFIX_LABEL_PATTERN.matcher(metaPrefixLabel).matches();
    }

    public static boolean isValidMetaName(String metaName) {
        if (metaName == null) {
            return false;
        }
        if (metaName.isEmpty()) {
            return true; // Empty name explicitly allowed in spec
        }
        return META_NAME_PATTERN.matcher(metaName).matches();
    }

    /**
     * A validation error which should later be logged
     */
    public record ToolValidationError(String messageKey, Object... objects) {}

}
