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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.ws.rs.ConstrainedTo;
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
    private static final Logger LOG = LogUtils.getL7dLogger(ConfigurableImpl.class);
    
    private static final Class<?>[] RESTRICTED_CLASSES_IN_SERVER = {ClientRequestFilter.class, 
                                                                    ClientResponseFilter.class};
    private static final Class<?>[] RESTRICTED_CLASSES_IN_CLIENT = {ContainerRequestFilter.class, 
                                                                    ContainerResponseFilter.class};
    
    private ConfigurationImpl config;
    private final C configurable;
    
    private final Class<?>[] restrictedContractTypes;

    public interface Instantiator {
        <T> Object create(Class<T> cls);
    }
    
    public ConfigurableImpl(C configurable, RuntimeType rt) {
        this(configurable, new ConfigurationImpl(rt));
    }
    
    public ConfigurableImpl(C configurable, Configuration config) {
        this.configurable = configurable;
        this.config = config instanceof ConfigurationImpl
            ? (ConfigurationImpl)config : new ConfigurationImpl(config);
        restrictedContractTypes = RuntimeType.CLIENT.equals(config.getRuntimeType()) ? RESTRICTED_CLASSES_IN_CLIENT 
            : RESTRICTED_CLASSES_IN_SERVER;
    }

    //Liberty change start
    // keeping old constructors so that MP Rest Client can extend both the 3.1.X and 3.2.X version of this class
    public ConfigurableImpl(C configurable, RuntimeType rt, Class<?>[] supportedProviderClasses) {
        this(configurable, rt);
    }
    public ConfigurableImpl(C configurable, Class<?>[] supportedProviderClasses, Configuration config) {
        this(configurable, config);
    }
    //Liberty change end
    
    static Class<?>[] getImplementedContracts(Object provider, Class<?>[] restrictedClasses) {
        Class<?> providerClass = provider instanceof Class<?> ? ((Class<?>)provider) : provider.getClass();
        Set<Class<?>> interfaces = Arrays.stream(providerClass.getInterfaces()).collect(Collectors.toSet());
        providerClass = providerClass.getSuperclass();
        for (;providerClass != null && providerClass != Object.class; providerClass = providerClass.getSuperclass()) {
            interfaces.addAll(Arrays.stream(providerClass.getInterfaces()).collect(Collectors.toSet()));
        }
        List<Class<?>> implementedContracts = interfaces.stream()
            .filter(el -> Arrays.stream(restrictedClasses).noneMatch(el::equals))
            .collect(Collectors.toList());
        return implementedContracts.toArray(new Class<?>[]{});
    }
    
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
        return doRegister(provider, contracts);
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
        return ConfigurationImpl::createProvider;
    }

    private C doRegister(Object provider, int bindingPriority, Class<?>... contracts) {
        if (contracts == null || contracts.length == 0) {
            LOG.warning("Null, empty or invalid contracts specified for " + provider + "; ignoring.");
            return configurable;
        }
        return doRegister(provider, ConfigurationImpl.initContractsMap(bindingPriority, contracts));
    }

    private C doRegister(Object provider, Map<Class<?>, Integer> contracts) {
        if (!checkConstraints(provider)) {
            return configurable;
        }
        if (provider instanceof Feature) {
            Feature feature = (Feature)provider;
            boolean enabled = feature.configure(new FeatureContextImpl(this));
            config.setFeature(feature, enabled);

            return configurable;
        }
        config.register(provider, contracts);
        return configurable;
    }

    private boolean checkConstraints(Object provider) {
        Class<?> providerClass = provider.getClass();
        ConstrainedTo providerConstraint = providerClass.getAnnotation(ConstrainedTo.class);
        if (providerConstraint != null) {
            RuntimeType currentRuntime = config.getRuntimeType();
            RuntimeType providerRuntime = providerConstraint.value();
            // need to check (1) whether the registration is occurring in the specified runtime type
            // and (2) does the provider implement an invalid interface based on the constrained runtime type
            if (!providerRuntime.equals(currentRuntime)) {
                LOG.warning("Provider " + provider + " cannot be registered in this " + currentRuntime
                            + " runtime because it is constrained to " + providerRuntime + " runtimes.");
                return false;
            }
            
            Class<?>[] restrictedInterfaces = RuntimeType.CLIENT.equals(providerRuntime) ? RESTRICTED_CLASSES_IN_CLIENT
                                                                                         : RESTRICTED_CLASSES_IN_SERVER;
            for (Class<?> restrictedContract : restrictedInterfaces) {
                if (restrictedContract.isAssignableFrom(providerClass)) {
                    RuntimeType opposite = RuntimeType.CLIENT.equals(providerRuntime) ? RuntimeType.SERVER
                                                                                      : RuntimeType.CLIENT;
                    LOG.warning("Provider " + providerClass.getName() + " is invalid - it is constrained to "
                        + providerRuntime + " runtimes but implements a " + opposite + " interface ");
                    return false;
                }
            }
        }
        return true;
    }
}
