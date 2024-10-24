/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2022 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.resteasy.spi.config;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import jakarta.ws.rs.sse.SseEventSink;

import org.jboss.resteasy.resteasy_jaxrs.i18n.LogMessages;
import org.jboss.resteasy.spi.util.Functions;

/**
 * Configuration options to be looked up in the {@link org.jboss.resteasy.spi.config.Configuration}. Unless noted all
 * options are optional and return a default value if the value was not found in the configuration.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class Options<T> {

    /**
     * An option for enabling or disabling the default exception mapper. By default the default exception mapper is
     * enabled.
     */
    public static final Options<Boolean> ENABLE_DEFAULT_EXCEPTION_MAPPER = new Options<>("dev.resteasy.exception.mapper",
            Boolean.class, Functions.singleton(() -> true));

    /**
     * An option for the threshold of the {@link org.jboss.resteasy.spi.EntityOutputStream}. The threshold
     * is used to determine when to offload an entity to a file out of memory.
     * <p>
     * The default is 5 MB.
     * </p>
     */
    public static final Options<Threshold> ENTITY_MEMORY_THRESHOLD = new Options<>("dev.resteasy.entity.memory.threshold",
            Threshold.class,
            Functions.singleton(() -> Threshold.of(5L, SizeUnit.MEGABYTE)));

    /**
     * An option for the threshold of the {@link org.jboss.resteasy.spi.EntityOutputStream} to write to a
     * file. A value of {@code -1} indicates no threshold limit.
     * <p>
     * The default is 50 MB.
     * </p>
     */
    public static final Options<Threshold> ENTITY_FILE_THRESHOLD = new Options<>("dev.resteasy.entity.file.threshold",
            Threshold.class,
            Functions.singleton(() -> Threshold.of(50L, SizeUnit.MEGABYTE)));

    /**
     * An option which allows which HTTP status code should be sent when the {@link SseEventSink#close()} is invoked.
     * In some implementations 200 (OK) is the default. However, RESTEasy prefers 204 (No Content) as no content has
     * been sent the response.
     * <p>
     * The default is 204 - No Content
     * </p>
     */
    public static final Options<Integer> SSE_CLOSED_RESPONSE_CODE = new Options<>("dev.resteasy.sse.closed.response.code",
            Integer.class,
            Functions.singleton(() -> 200));  //Liberty change... default is 200.
//            Functions.singleton(() -> 204));

    private final String key;
    private final Class<T> name;
    private final Supplier<T> dftValue;

    protected Options(final String key, final Class<T> name, final Supplier<T> dftValue) {
        this.key = key;
        this.name = name;
        this.dftValue = dftValue;
    }

    /**
     * Resolves the value from the configuration
     *
     * @return the value or the default value which may be {@code null}
     */
    public T getValue() {
        return getProperty(key, name, dftValue);
    }

    /**
     * The key for the property.
     *
     * @return the key for the property
     */
    public String name() {
        return key;
    }

    @Override
    public int hashCode() {
        return Objects.hash(key);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Options)) {
            return false;
        }
        final Options<?> other = (Options<?>) obj;
        return Objects.equals(key, other.key);
    }

    @Override
    public String toString() {
        return "Option[name=" + key + ", type=" + name.getName() + "]";
    }

    /**
     * Checks the {@link Configuration} for the property returning the value or the given default.
     *
     * @param name       the name of the property
     * @param returnType the type of the property
     * @param dft        the default value if the property was not found
     *
     * @return the value found in the configuration or the default value
     */
    protected static <T> T getProperty(final String name, final Class<T> returnType, final Supplier<T> dft) {
        return getProperty(name, returnType).orElseGet(dft);
    }

    private static <T> Optional<T> getProperty(final String name, final Class<T> returnType) {
        // If the MicroProfile Config is available, we might not have Converters for these types. If an error occurs
        // attempting to get the value, log it, but return an empty optional.
        try {
            return ConfigurationFactory.getInstance()
                    .getConfiguration()
                    .getOptionalValue(name, returnType);
        } catch (SecurityException e) {
            throw e;
        } catch (Exception e) {
            LogMessages.LOGGER.tracef(e, "Failed to get property for %s of type %s.", name, returnType);
        }
        return Optional.empty();
    }
}
