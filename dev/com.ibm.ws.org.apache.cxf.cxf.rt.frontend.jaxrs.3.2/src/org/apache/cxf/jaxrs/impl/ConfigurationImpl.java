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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;

import javax.ws.rs.RuntimeType;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Feature;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.jaxrs.utils.AnnotationUtils;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;

public class ConfigurationImpl implements Configuration {
    private static final Logger LOG = LogUtils.getL7dLogger(ConfigurationImpl.class);
    private Map<String, Object> props = new HashMap<>();
    private RuntimeType runtimeType;
    private Map<Object, Map<Class<?>, Integer>> providers =
        new LinkedHashMap<Object, Map<Class<?>, Integer>>();
    private Map<Feature, Boolean> features = new LinkedHashMap<Feature, Boolean>();

    public ConfigurationImpl(RuntimeType rt) {
        this.runtimeType = rt;
    }

    public ConfigurationImpl(Configuration parent) {
        if (parent != null) {
            this.props.putAll(parent.getProperties());
            this.runtimeType = parent.getRuntimeType();

            Set<Class<?>> providerClasses = new HashSet<Class<?>>(parent.getClasses());
            for (Object o : parent.getInstances()) {
                if (!(o instanceof Feature)) {
                    registerParentProvider(o, parent);
                } else {
                    Feature f = (Feature)o;
                    features.put(f, parent.isEnabled(f));
                }
                providerClasses.remove(o.getClass());
            }
            for (Class<?> cls : providerClasses) {
                registerParentProvider(createProvider(cls), parent);
            }

        }
    }

    //Liberty Change start
    // keep old constructor for compatibility with 3.1.X...
    public ConfigurationImpl(Configuration parent, Class<?>[] defaultContracts) {
        this(parent);
    }
    //Liberty Change end
    private void registerParentProvider(Object o, Configuration parent) {
        Map<Class<?>, Integer> contracts = parent.getContracts(o.getClass());
        if (contracts != null) {
            providers.put(o, contracts);
        } else {
            register(o, AnnotationUtils.getBindingPriority(o.getClass()), 
                        ConfigurableImpl.getImplementedContracts(o, new Class<?>[]{}));
        }
    }

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<Class<?>>();
        for (Object o : getInstances()) {
            classes.add(o.getClass());
        }
        return classes;
    }

    @Override
    public Map<Class<?>, Integer> getContracts(Class<?> cls) {
        for (Object o : getInstances()) {
            if (cls.isAssignableFrom(o.getClass())) {
                if (o instanceof Feature) {
                    return Collections.emptyMap();
                } else {
                    return providers.get(o);
                }
            }
        }
        return Collections.emptyMap();
    }

    @Override
    public Set<Object> getInstances() {
        Set<Object> allInstances = new HashSet<>();
        allInstances.addAll(providers.keySet());
        allInstances.addAll(features.keySet());
        return Collections.unmodifiableSet(allInstances);
    }

    @Override
    public Map<String, Object> getProperties() {
        return Collections.unmodifiableMap(props);
    }

    @Override
    public Object getProperty(String name) {
        return props.get(name);
    }

    @Override
    public Collection<String> getPropertyNames() {
        return Collections.unmodifiableSet(props.keySet());
    }

    @Override
    public RuntimeType getRuntimeType() {
        return runtimeType;
    }

    @Override
    public boolean isEnabled(Feature f) {
        return features.containsKey(f) && features.get(f);
    }

    @Override
    public boolean isEnabled(Class<? extends Feature> f) {
        for (Entry<Feature, Boolean> entry : features.entrySet()) {
            Feature feature = entry.getKey();
            Boolean enabled = entry.getValue();
            if (f.isAssignableFrom(feature.getClass()) && enabled.booleanValue()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isRegistered(Object obj) {
        for (Object o : getInstances()) {
            if (o.equals(obj)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isRegistered(Class<?> cls) {
        for (Object o : getInstances()) {
            if (cls == o.getClass()) {
                return true;
            }
        }
        return false;
    }

    public void setProperty(String name, Object value) {
        if (name == null) {
            props.remove(name);
        } else {
            props.put(name, value);
        }
    }

    public void setFeature(Feature f, boolean enabled) {
        features.put(f, enabled);
    }


    private void register(Object provider, int bindingPriority, Class<?>... contracts) {
        register(provider, initContractsMap(bindingPriority, contracts));
    }

    public boolean register(Object provider, Map<Class<?>, Integer> contracts) {
        if (provider.getClass() == Class.class) {
            if (isRegistered((Class<?>)provider)) {
                LOG.warning("Provider class " + ((Class<?>)provider).getName() + " has already been registered");
                return false;
            }
            provider = createProvider((Class<?>)provider);
        }
        if (isRegistered(provider)) {
            LOG.warning("Provider " + provider.getClass().getName() + " has already been registered");
            return false;
        }

        if (!contractsValid(provider, contracts)) {
            return false;
        }

        Map<Class<?>, Integer> metadata = providers.get(provider);
        if (metadata == null) {
            metadata = new HashMap<>();
            providers.put(provider, metadata);
        }
        for (Entry<Class<?>, Integer> entry : contracts.entrySet()) {
            if (entry.getKey().isAssignableFrom(provider.getClass())) {
                metadata.put(entry.getKey(), entry.getValue());
            }
        }
        return true;
    }

    private boolean contractsValid(Object provider, Map<Class<?>, Integer> contracts) {
        final Class<?> providerClass = provider.getClass();
        for (Class<?> contractInterface : contracts.keySet()) {
            if (!contractInterface.isAssignableFrom(providerClass)) {
                LOG.warning("Provider " + providerClass.getName() + " does not implement specified contract: "
                    + contractInterface.getName());
                return false;
            }
        }
        return true;
    }
    public static Map<Class<?>, Integer> initContractsMap(int bindingPriority, Class<?>... contracts) {
        Map<Class<?>, Integer> metadata = new HashMap<>();
        for (Class<?> contract : contracts) {
            metadata.put(contract, bindingPriority);
        }
        return metadata;
    }

    @FFDCIgnore(Throwable.class)
    public static Object createProvider(Class<?> cls) {
        try {
            return cls.newInstance();
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }
}
