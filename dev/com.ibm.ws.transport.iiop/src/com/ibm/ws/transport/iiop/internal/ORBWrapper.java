/*******************************************************************************
 * Copyright (c) 2015,2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/*
 * Some of the code was derived from code supplied by the Apache Software Foundation licensed under the Apache License, Version 2.0.
 */
package com.ibm.ws.transport.iiop.internal;

import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;
import static org.osgi.service.component.annotations.ConfigurationPolicy.REQUIRE;
import static org.osgi.service.component.annotations.ReferenceCardinality.MULTIPLE;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import org.osgi.service.component.ComponentFactory;
import org.osgi.service.component.ComponentInstance;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import com.ibm.ws.transport.iiop.spi.IIOPEndpoint;
import com.ibm.ws.transport.iiop.spi.ReadyListener;
import com.ibm.ws.transport.iiop.spi.SubsystemFactory;

/**
 * Provides access to the ORB.
 */
@Component(configurationPolicy = REQUIRE, service = {}, property = { "service.vendor=IBM", "service.ranking:Integer=5" })
public final class ORBWrapper {
    public static final String pid = ORBWrapperInternal.class.getName();
    private final ReadyListenerImpl readyListener;

    @Activate
    public ORBWrapper(
            Map<String,Object> properties,
            @Reference(name="ComponentFactory", target = "(component.factory=com.ibm.ws.transport.iiop.internal.ORBWrapperInternal)")
            ComponentFactory<ORBWrapperInternal> factory,
            @Reference(name="IIOPEndpoint", cardinality = MULTIPLE, policyOption = GREEDY)
            List<IIOPEndpoint> endpoints,
            @Reference(name="SubsystemFactory", cardinality = MULTIPLE, policyOption = GREEDY)
            List<SubsystemFactory> subsystemFactories
            ) {
        readyListener = new ReadyListenerImpl(
                unmodifiableMap(subsystemFactories.stream().sequential().collect(LinkedHashMap::new, (m,f) -> m.put(f, new AtomicBoolean()), Map::putAll)),
                unmodifiableMap(properties), factory);
        subsystemFactories.forEach(sf -> sf.register(readyListener, properties, unmodifiableList(endpoints)));
    }

    @Deactivate
    void deactivate() {
        readyListener.unregister();
    }

    private static final class ReadyListenerImpl implements ReadyListener {
        private final Map<SubsystemFactory, ? extends AtomicBoolean> subsystemFactories;
        private final Map<String, Object> properties;
        private final ComponentFactory<ORBWrapperInternal> factory;
        private final AcidReference<ComponentInstance<ORBWrapperInternal>> instanceRef = new AcidReference<>();

        ReadyListenerImpl(Map<SubsystemFactory, ? extends AtomicBoolean> subsystemFactories,
                Map<String, Object> properties, ComponentFactory<ORBWrapperInternal> factory) {
            this.subsystemFactories = subsystemFactories;
            this.properties = properties;
            this.factory = factory;
        }

        /** {@inheritDoc} */
        @Override
        public void readyChanged(SubsystemFactory id, boolean ready) {
            try {
                subsystemFactories.get(id).set(ready);
            } catch (NullPointerException unexpected) { // FFDC and continue
            }
            instanceRef.update(this::updateInstance);
        }

        Supplier<ComponentInstance<ORBWrapperInternal>> updateInstance(ComponentInstance<ORBWrapperInternal> current) {
            boolean shouldExist = subsystemFactories.values().stream().allMatch(AtomicBoolean::get);
            boolean doesExist = current != null;
            if (shouldExist == doesExist) return null;
            if (shouldExist) return this::newInstance;
            // if we get to here, it exists but needs to DIE
            return () -> {
		current.dispose();
		return null;
            };
        }

        /** {@inheritDoc} */
        @Override
        public String listenerId() {
            return (String)properties.get("id");
        }

        void unregister() {
            subsystemFactories.keySet().forEach(sf -> sf.unregister(this));
        }

        private ComponentInstance<ORBWrapperInternal> newInstance() {
            final Hashtable<String, Object> h = new Hashtable<>();
            h.putAll(properties);
            return factory.newInstance(h);
        }

    }
}
