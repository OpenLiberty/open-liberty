/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.openapi40.internal.impl.test;

import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigValue;
import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.Converter;

/**
 * A ConfigProviderResolver to satisfy the dependency on MP Config during unit tests.
 */
public class DummyConfigProviderResolver extends ConfigProviderResolver {

    @Override
    public ConfigBuilder getBuilder() {
        throw new UnsupportedOperationException("getBuilder not implemented");
    }

    @Override
    public Config getConfig() {
        return EMPTY_CONFIG;
    }

    @Override
    public Config getConfig(ClassLoader arg0) {
        // TODO Auto-generated method stub
        return EMPTY_CONFIG;
    }

    @Override
    public void registerConfig(Config arg0, ClassLoader arg1) {}

    @Override
    public void releaseConfig(Config arg0) {}

    private static final Config EMPTY_CONFIG = new Config() {

        @Override
        public <T> T unwrap(Class<T> arg0) {
            throw new IllegalArgumentException();
        }

        @Override
        public <T> T getValue(String arg0, Class<T> arg1) {
            throw new NoSuchElementException();
        }

        @Override
        public Iterable<String> getPropertyNames() {
            return Collections.emptySet();
        }

        @Override
        public <T> Optional<T> getOptionalValue(String arg0, Class<T> arg1) {
            return Optional.empty();
        }

        @Override
        public <T> Optional<Converter<T>> getConverter(Class<T> arg0) {
            return Optional.empty();
        }

        @Override
        public ConfigValue getConfigValue(String arg0) {
            return new EmptyConfigValue(arg0);
        }

        @Override
        public Iterable<ConfigSource> getConfigSources() {
            return Collections.emptySet();
        }
    };

    private static class EmptyConfigValue implements ConfigValue {

        private String name;

        public EmptyConfigValue(String name) {
            super();
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getRawValue() {
            return null;
        }

        @Override
        public String getSourceName() {
            return null;
        }

        @Override
        public int getSourceOrdinal() {
            return 0;
        }

        @Override
        public String getValue() {
            return null;
        }

    }

}
