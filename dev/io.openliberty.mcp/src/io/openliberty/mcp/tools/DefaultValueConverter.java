package io.openliberty.mcp.tools;

import io.openliberty.mcp.annotations.ToolArg;
import jakarta.interceptor.Interceptor.Priority;

/**
 * Converts default values from {@link String} to an argument object of a specific type.
 * <p>
 * Converters are discovered automatically, i.e. all implementations in a Quarkus application are registered.
 * <p>
 * Implementations must declare a public no-args constructor.
 * <p>
 * An implementation may be annotated with {@link Priority}. If multiple converters of the same priority exist for a specific
 * argument type, then only the converter with highest priority is registered.
 *
 * @see ToolArg#defaultValue()
 * @param <TYPE> the agrument type
 */
public interface DefaultValueConverter<TYPE> {

    /**
     * @param defaultValue (must not be {@code null})
     * @return the converted object
     */
    TYPE convert(String defaultValue);

}
