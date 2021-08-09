/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2021 Red Hat, Inc., and individual contributors
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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Optional;
import java.util.function.Function;

import org.jboss.resteasy.resteasy_jaxrs.i18n.LogMessages;
import org.jboss.resteasy.resteasy_jaxrs.i18n.Messages;
import org.jboss.resteasy.spi.ResteasyConfiguration;

/**
 * A default configuration which first attempts to use the Eclipse MicroProfile Config API. If not present on the class
 * path the {@linkplain ResteasyConfiguration configuration} is used to resolve the value, followed by system properties
 * and then environment variables if not found in the previous search.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class DefaultConfiguration implements Configuration {
    private static final Function<String, String> DEFAULT_RESOLVER = new Resolver(null);
    private static final Method GET_CONFIG;
    private static final Method GET_OPTIONAL_VALUE;
    private static final Method GET_VALUE;

    static {
        Method getConfig;
        Method getOptionalValue;
        Method getValue;
        try {
            final ClassLoader classLoader = getClassLoader();
            final Class<?> configProvider = Class.forName("org.eclipse.microprofile.config.ConfigProvider", false, classLoader);
            getConfig = configProvider.getDeclaredMethod("getConfig", ClassLoader.class);
            final Class<?> config = Class.forName("org.eclipse.microprofile.config.Config", false, classLoader);
            getOptionalValue = config.getDeclaredMethod("getOptionalValue", String.class, Class.class);
            getValue = config.getDeclaredMethod("getValue", String.class, Class.class);
        } catch (Throwable ignore) {
            getConfig = null;
            getOptionalValue = null;
            getValue = null;
        }
        GET_CONFIG = getConfig;
        GET_OPTIONAL_VALUE = getOptionalValue;
        GET_VALUE = getValue;
    }

    private final Function<String, String> resolver;

    /**
     * Creates a new configuration which uses system properties to resolve the values if the Eclipse MicroProfile Config
     * is not on the class path.
     */
    public DefaultConfiguration() {
        this(null);
    }

    /**
     * Creates a new configuration which uses the {@linkplain ResteasyConfiguration configuration} to resolve the values
     * if the Eclipse MicroProfile Config is not on the class path.
     *
     * @param config the resolver
     */
    public DefaultConfiguration(final ResteasyConfiguration config) {
        this.resolver = config == null ? DEFAULT_RESOLVER : new Resolver(config);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getOptionalValue(final String name, final Class<T> type) {
        if (GET_CONFIG != null) {
            try {
                final Object config = GET_CONFIG.invoke(null, getClassLoader());
                return (Optional<T>) GET_OPTIONAL_VALUE.invoke(config, name, type);
            } catch (IllegalAccessException | InvocationTargetException e) {
                LogMessages.LOGGER.debugf(e, "Failed to invoke the configuration API method %s.", GET_OPTIONAL_VALUE);
            }
        }
        final String value = resolver.apply(name);
        if (value == null) {
            return Optional.empty();
        }
        final Object typedValue;
        if (type == String.class) {
            typedValue = type.cast(value);
        } else if (type == Boolean.class || type == boolean.class) {
            typedValue = Boolean.valueOf(value);
        } else if (type == Character.class || type == char.class) {
            if (value.isEmpty()) {
                return Optional.empty();
            }
            typedValue = value.charAt(0);
        } else if (type == Byte.class || type == byte.class) {
            typedValue = Byte.valueOf(value.trim());
        } else if (type == Short.class || type == short.class) {
            typedValue = Short.valueOf(value);
        } else if (type == Integer.class || type == int.class) {
            typedValue = Integer.valueOf(value);
        } else if (type == Long.class || type == long.class) {
            typedValue = Long.valueOf(value);
        } else if (type == Float.class || type == float.class) {
            typedValue = Float.valueOf(value);
        } else if (type == Double.class || type == double.class) {
            typedValue = Double.valueOf(value);
        } else if (type == BigDecimal.class) {
            typedValue = new BigDecimal(value);
        } else if (type.isEnum()) {
            typedValue = Enum.valueOf(type.asSubclass(Enum.class), value);
        } else {
            throw Messages.MESSAGES.cannotConvertParameter(value, type, name);
        }
        return (Optional<T>) Optional.of(typedValue);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getValue(final String name, final Class<T> type) {
        if (GET_CONFIG != null) {
            try {
                final Object config = GET_CONFIG.invoke(null, getClassLoader());
                return (T) GET_VALUE.invoke(config, name, type);
            } catch (IllegalAccessException | InvocationTargetException e) {
                LogMessages.LOGGER.debugf(e, "Failed to invoke the configuration API method %s.", GET_VALUE);
            }
        }
        return getOptionalValue(name, type).orElseThrow(() -> Messages.MESSAGES.propertyNotFound(name));
    }

    private static ClassLoader getClassLoader() {
        if (System.getSecurityManager() == null) {
            final ClassLoader tccl = Thread.currentThread().getContextClassLoader();
            return tccl != null ? tccl : DefaultConfiguration.class.getClassLoader();
        }
        return AccessController.doPrivileged((PrivilegedAction<ClassLoader>) () -> {
            final ClassLoader tccl = Thread.currentThread().getContextClassLoader();
            return tccl != null ? tccl : DefaultConfiguration.class.getClassLoader();
        });
    }

    private static class Resolver implements Function<String, String> {
        private final ResteasyConfiguration config;

        private Resolver(final ResteasyConfiguration config) {
            this.config = config;
        }

        @Override
        public String apply(final String name) {
            //Liberty change - adding doPriv
            if (System.getSecurityManager() == null) {
                String value = System.getProperty(name);
                if (value == null) {
                    value = System.getenv(name);
                    if (value == null && config != null) {
                        value = config.getInitParameter(name);
                    }
                }
                return value;
            }
            return AccessController.doPrivileged((PrivilegedAction<String>) () -> {
                String value = System.getProperty(name);
                if (value == null) {
                    value = System.getenv(name);
                    if (value == null && config != null) {
                        value = config.getInitParameter(name);
                    }
                }
                return value;
            });
        }
    }
}
