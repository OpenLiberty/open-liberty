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

package org.apache.cxf.microprofile.client.config;

import java.util.Optional;
import java.util.OptionalLong;

import org.apache.cxf.common.util.StringUtils;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;

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

    public static <T> Optional<T> getOptionalValue(String propertyNameFormat, Class<?> clientIntf, Class<T> clazz) {
        Optional<Config> c = config();
        if (c.isPresent()) {
            String propertyName = String.format(propertyNameFormat, clientIntf.getName());
            T value = c.get().getOptionalValue(propertyName, clazz).orElseGet(() -> {
                RegisterRestClient anno = clientIntf.getAnnotation(RegisterRestClient.class);
                if (anno != null && !StringUtils.isEmpty(getConfigKey(anno))) {
                    String configKeyPropName = String.format(propertyNameFormat, getConfigKey(anno));
                    return c.get().getOptionalValue(configKeyPropName, clazz).orElse(null);
                }
                return null;
            });
            return Optional.ofNullable(value);
        }

        return Optional.empty();
    }

    public static <T> T getValue(String propertyName, Class<T> clazz) {
        Optional<Config> c = config();
        return c.isPresent() ? c.get().getValue(propertyName, clazz) : null;
    }

    public static <T> T getValue(String propertyNameFormat, Class<?> clientIntf, Class<T> clazz) {
        Optional<Config> c = config();
        T value = null;
        if (c.isPresent()) {
            String propertyName = String.format(propertyNameFormat, clientIntf.getName());
            value = c.get().getOptionalValue(propertyName, clazz).orElseGet(() -> {
                RegisterRestClient anno = clientIntf.getAnnotation(RegisterRestClient.class);
                if (anno != null && !StringUtils.isEmpty(getConfigKey(anno))) {
                    String configKeyPropName = String.format(propertyNameFormat, getConfigKey(anno));
                    return c.get().getValue(configKeyPropName, clazz);
                }
                return null;
            });
        }

        return value;
    }

    public static OptionalLong getOptionalLong(String propName) {
        Optional<Config> c = config();
        Optional<Long> opt =
            c.isPresent() ? c.get().getOptionalValue(propName, Long.class) : Optional.empty();
        return opt.isPresent() ? OptionalLong.of(opt.get()) : OptionalLong.empty();
    }

    public static OptionalLong getOptionalLong(String propNameFormat, Class<?> clientIntf) {
        Optional<Config> c = config();
        if (c.isPresent()) {
            String propertyName = String.format(propNameFormat, clientIntf.getName());
            Long value = c.get().getOptionalValue(propertyName, Long.class).orElseGet(() -> {
                RegisterRestClient anno = clientIntf.getAnnotation(RegisterRestClient.class);
                if (anno != null && !StringUtils.isEmpty(getConfigKey(anno))) {
                    String configKeyPropName = String.format(propNameFormat, getConfigKey(anno));
                    return c.get().getOptionalValue(configKeyPropName, Long.class).orElse(null);
                }
                return null;
            });
            return value == null ? OptionalLong.empty() : OptionalLong.of(value);
        }
        return OptionalLong.empty();
    }

    @FFDCIgnore(Throwable.class)
    private static String getConfigKey(RegisterRestClient anno) {
        try {
            return anno.configKey();
        } catch (Throwable t) {
            return "";
        }
    }
}
