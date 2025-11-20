/*
 * Copyright The RestEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.resteasy.spi.config;

import java.math.BigDecimal;
import java.security.AccessController; //Liberty change
import java.security.PrivilegedAction; //Liberty change
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Function;

import org.jboss.resteasy.resteasy_jaxrs.i18n.Messages;
import org.jboss.resteasy.spi.ResteasyConfiguration;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

/**
 * A default configuration which searches for a property in the following order:
 * <ol>
 * <li>System properties</li>
 * <li>Environment variables</li>
 * <li>{@link ResteasyConfiguration}</li>
 * </ol>
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class DefaultConfiguration implements Configuration {
    private static final Function<String, String> DEFAULT_RESOLVER = new Resolver(null);

    private final Function<String, String> resolver;

    /**
     * Creates a new configuration .
     */
    public DefaultConfiguration() {
        this(resolveConfiguration());
    }

    /**
     * Creates a new configuration which uses the {@linkplain ResteasyConfiguration configuration} to resolve the values.
     *
     * @param config the resolver
     */
    public DefaultConfiguration(final ResteasyConfiguration config) {
        final ResteasyConfiguration delegate;
        if (config == null) {
            delegate = resolveConfiguration();
        } else {
            delegate = config;
        }
        this.resolver = delegate == null ? DEFAULT_RESOLVER : new Resolver(delegate);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getOptionalValue(final String name, final Class<T> type) {
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
        } else if (type == Path.class) {
            typedValue = Path.of(value);
        } else if (type == Threshold.class) {
            typedValue = Threshold.valueOf(value);
        } else {
            throw Messages.MESSAGES.cannotConvertParameter(value, type, name);
        }
        return (Optional<T>) Optional.of(typedValue);
    }

    @Override
    public <T> T getValue(final String name, final Class<T> type) {
        return getOptionalValue(name, type).orElseThrow(() -> Messages.MESSAGES.propertyNotFound(name));
    }

    private static ResteasyConfiguration resolveConfiguration() {
        final ResteasyProviderFactory factory = ResteasyProviderFactory.peekInstance();
        return factory == null ? null : factory.getContextData(ResteasyConfiguration.class);
    }

    private static class Resolver implements Function<String, String> {
        private final ResteasyConfiguration config;
        private final ThreadLocal<Boolean> entered = ThreadLocal.withInitial(() -> Boolean.FALSE);

        private Resolver(final ResteasyConfiguration config) {
            this.config = config;
        }

        @Override
        public String apply(final String name) {
		    // Check for recursion, if we're back here assume null
            if (entered.get()) {
                return null;
            }
            try{
                entered.set(Boolean.TRUE);

                //Liberty change start
                String value = config == null ? null : config.getInitParameter(name);
                if (value == null) {
                    if (System.getSecurityManager() == null) {
                        value = System.getProperty(name);
                        if (value == null) {
                            value = System.getenv(name);
                            if (value == null && config != null) {
                                value = config.getInitParameter(name);
                            }
                        }
                        return value;
                    }
                    return AccessController.doPrivileged((PrivilegedAction<String>) () -> {
                        String value2 = System.getProperty(name);
                        if (value2 == null) {
                            value2 = System.getenv(name);
                            if (value2 == null && config != null) {
                                value2 = config.getInitParameter(name);
                            }
                        }
                        return value2;
                    });
                }
                //Liberty change end

                return value;
            } finally {
                entered.remove();
            }
        }
    }
}
