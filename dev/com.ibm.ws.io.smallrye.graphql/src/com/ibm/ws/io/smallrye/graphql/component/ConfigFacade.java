/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.io.smallrye.graphql.component;

import java.util.Optional;
import java.util.OptionalLong;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

public final class ConfigFacade {

    private ConfigFacade() {
    }

    private static Optional<Config> config() {
        Config c;
        try {
            c = ConfigProvider.getConfig();
        } catch (ExceptionInInitializerError | NoClassDefFoundError | IllegalStateException ex) {
            // expected if no MP Config implementation is available
            c = null;
        }
        return Optional.ofNullable(c);
    }

    public static <T> Optional<T> getOptionalValue(String propertyName, Class<T> clazz) {
        Optional<Config> c = config();
        return c.isPresent() ? c.get().getOptionalValue(propertyName, clazz) : Optional.empty();
    }

    public static <T> T getValue(String propertyName, Class<T> clazz) {
        Optional<Config> c = config();
        return c.isPresent() ? c.get().getValue(propertyName, clazz) : null;
    }

    public static OptionalLong getOptionalLong(String propName) {
        Optional<Config> c = config();
        Optional<Long> opt =
            c.isPresent() ? c.get().getOptionalValue(propName, Long.class) : Optional.empty();
        return opt.isPresent() ? OptionalLong.of(opt.get()) : OptionalLong.empty();

    }
}
