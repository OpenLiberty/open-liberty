/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.ibm.ws.microprofile.graphql.component;

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
