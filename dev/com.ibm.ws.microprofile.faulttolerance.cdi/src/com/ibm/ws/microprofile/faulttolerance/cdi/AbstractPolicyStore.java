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
package com.ibm.ws.microprofile.faulttolerance.cdi;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import javax.annotation.PreDestroy;
import javax.enterprise.inject.spi.Bean;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * A PolicyStore which uses a ConcurrentHashMap
 * <p>
 * This is an abstract class which allows subclasses to define what is used for the map key when policies are looked up from the store.
 * <p>
 * This class includes a {@link PreDestroy} method which calls {@link AggregatedFTPolicy#close()} on each stored policy.
 *
 * @param K the key type
 */
public abstract class AbstractPolicyStore<K> implements PolicyStore {

    private static final TraceComponent tc = Tr.register(AbstractPolicyStore.class);

    private final ConcurrentHashMap<K, AggregatedFTPolicy> store = new ConcurrentHashMap<>();

    @Override
    public AggregatedFTPolicy getOrCreate(Bean<?> bean, Method method, Supplier<AggregatedFTPolicy> supplier) {
        K key = getKey(bean, method);
        AggregatedFTPolicy result = store.get(key);

        if (result == null) {
            result = supplier.get();
            AggregatedFTPolicy previous = store.putIfAbsent(key, result);
            if (previous != null) {
                result = previous;
            }
        }

        return result;
    }

    /**
     * Create a key for the map
     *
     * @param bean   the bean
     * @param method the method
     * @return the map key
     */
    protected abstract K getKey(Bean<?> bean, Method method);

    @PreDestroy
    public void cleanUpExecutors() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Cleaning up executors");
        }

        store.values().forEach((e) -> {
            e.close();
        });
    }

}
