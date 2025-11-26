package io.openliberty.mcp.internal.requests;

import io.openliberty.mcp.annotations.ToolArg;

/**
 * Converts default values from {@link String} to an argument object of a specific type.
 * <p>
 * Converters are discovered automatically
 * <p>
 * Implementations must declare a public no-args constructor.
 * <p>
 * An implementation may be annotated with {@link Priority}. If multiple converters of the same priority exist for a specific
 * argument type, then only the converter with highest priority is registered.
 *
 * @see ToolArg#defaultValue()
 * @param <TYPE> the argument type
 */
public interface DefaultValueConverter<TYPE> {

    /**
     * @param defaultValue (must not be {@code null})
     * @return the converted object
     */
    TYPE convert(String defaultValue);

}
