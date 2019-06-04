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

package org.apache.cxf.jaxrs.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.Priorities;
import javax.ws.rs.RuntimeType;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Configurable;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Feature;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.jaxrs.utils.AnnotationUtils;

public class ConfigurableImpl<C extends Configurable<C>> implements Configurable<C> {
    // Liberty CXF 3.2.X backport begin
    private static final Logger LOG = LogUtils.getL7dLogger(ConfigurableImpl.class); 

    private static final Class<?>[] RESTRICTED_CLASSES_IN_SERVER = {ClientRequestFilter.class, 
                                                                    ClientResponseFilter.class};
    private static final Class<?>[] RESTRICTED_CLASSES_IN_CLIENT = {ContainerRequestFilter.class, 
                                                                    ContainerResponseFilter.class};
    // Liberty CXF 3.2.X backport end

    private ConfigurationImpl config;
    private final C configurable;
    private final Class<?>[] supportedProviderClasses;

    private final Class<?>[] restrictedContractTypes; // Liberty CXF 3.2.X backport

    private final Instantiator instantiator = new Instantiator() {
        @Override
        public <T> Object create(Class<T> cls) {
            return ConfigurationImpl.createProvider(cls);
        }
    };

    public interface Instantiator {
        <T> Object create(Class<T> cls);
    }

    public ConfigurableImpl(C configurable, RuntimeType rt, Class<?>[] supportedProviderClasses) {
        this(configurable, supportedProviderClasses, new ConfigurationImpl(rt));
    }

    public ConfigurableImpl(C configurable, Class<?>[] supportedProviderClasses, Configuration config) {
        this.configurable = configurable;
        this.supportedProviderClasses = supportedProviderClasses;
        this.config = config instanceof ConfigurationImpl ? (ConfigurationImpl) config : new ConfigurationImpl(config, supportedProviderClasses);
        restrictedContractTypes = RuntimeType.CLIENT.equals(config.getRuntimeType()) ? RESTRICTED_CLASSES_IN_CLIENT
                        : RESTRICTED_CLASSES_IN_SERVER; // Liberty CXF 3.2.X backport
    }

    // Liberty CXF 3.2.X backport begin
    static Class<?>[] getImplementedContracts(Object provider, Class<?>[] restrictedClasses) {
        Class<?> providerClass = provider instanceof Class<?> ? ((Class<?>)provider) : provider.getClass();

        Set<Class<?>> interfaces = collectAllInterfaces(providerClass);
        if (LOG.isLoggable(Level.FINEST)) {
            LOG.log(Level.FINEST, "all interfaces implemented by " + provider + ": " + interfaces);
        }

        //List<Class<?>> implementedContracts = new ArrayList<>();
        for (Class<?> iface : restrictedClasses) {
            if (interfaces.remove(iface) && LOG.isLoggable(Level.FINEST)) {
                LOG.log(Level.FINEST, "not registering contract " + iface + " for provider, " + provider);
            }
        }
        return interfaces.toArray(new Class<?>[]{});
    }

    private static Set<Class<?>> collectAllInterfaces(Class<?> providerClass) {
        Set<Class<?>> interfaces = new HashSet<>();
        do {
            for (Class<?> anInterface : providerClass.getInterfaces()) {
                collectInterfaces(interfaces, anInterface);
            }
            providerClass = providerClass.getSuperclass();
        } while (providerClass != null && providerClass != Object.class);

        return interfaces;
    }

    /**
     * internal helper function to recursively collect Interfaces.
     * This is needed since {@link Class#getInterfaces()} does only return directly implemented Interfaces,
     * But not the ones derived from those classes.
     */
    private static void collectInterfaces(Set<Class<?>> interfaces, Class<?> anInterface) {
        interfaces.add(anInterface);
        for (Class<?> superInterface : anInterface.getInterfaces()) {
            collectInterfaces(interfaces, superInterface);
        }
    }
    // Liberty CXF 3.2.X backport end

    protected C getConfigurable() {
        return configurable;
    }

    @Override
    public Configuration getConfiguration() {
        return config;
    }

    @Override
    public C property(String name, Object value) {
        config.setProperty(name, value);
        return configurable;
    }

    @Override
    public C register(Object provider) {
        return register(provider, AnnotationUtils.getBindingPriority(provider.getClass()));
    }

    @Override
    public C register(Object provider, int bindingPriority) {
        return doRegister(provider, bindingPriority, getImplementedContracts(provider, restrictedContractTypes));
    }

    @Override
    public C register(Object provider, Class<?>... contracts) {
        return doRegister(provider, Priorities.USER, contracts);
    }

    @Override
    public C register(Object provider, Map<Class<?>, Integer> contracts) {
        return doRegisterProvider(provider, contracts);
    }

    @Override
    public C register(Class<?> providerClass) {
        return register(providerClass, AnnotationUtils.getBindingPriority(providerClass));
    }

    @Override
    public C register(Class<?> providerClass, int bindingPriority) {
        return doRegister(getInstantiator().create(providerClass), bindingPriority, 
                          getImplementedContracts(providerClass, restrictedContractTypes));
    }

    @Override
    public C register(Class<?> providerClass, Class<?>... contracts) {
        return doRegister(providerClass, Priorities.USER, contracts);
    }

    @Override
    public C register(Class<?> providerClass, Map<Class<?>, Integer> contracts) {
        return register(getInstantiator().create(providerClass), contracts);
    }

    protected Instantiator getInstantiator() {
        return instantiator;
    }

    private C doRegister(Object provider, int bindingPriority, Class<?>... contracts) {
        // CXF 3.2.X backport begin
        if (contracts == null || contracts.length == 0) {
            LOG.warning("Null, empty or invalid contracts specified for " + provider + "; ignoring.");
            return configurable;
        }
        // CXF 3.2.X backport end
        return doRegisterProvider(provider, ConfigurationImpl.initContractsMap(bindingPriority, contracts));
    }

    private C doRegisterProvider(Object provider, Map<Class<?>, Integer> contracts) {
        if (provider instanceof Feature) {
            Feature feature = (Feature) provider;
            boolean enabled = feature.configure(new FeatureContextImpl(this));
            config.setFeature(feature, enabled);

            return configurable;
        }
        config.register(provider, contracts);
        return configurable;
    }
}
