/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.faulttolerance.cdi;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

public abstract class AbstractFTEnablementConfig implements FTEnablementConfig {

    private static final TraceComponent tc = Tr.register(AbstractFTEnablementConfig.class);
    private final Map<ClassLoader, Set<Class<?>>> activeAnnotationsCache = new WeakHashMap<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private static final Set<Class<?>> ONLY_FALLBACK = Collections.singleton(Fallback.class);
    protected static final Set<Class<?>> ALL_ANNOTATIONS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(Asynchronous.class, CircuitBreaker.class,
                                                                                                                   Retry.class, Timeout.class, Bulkhead.class, Fallback.class)));
    private static final String CONFIG_NONFALLBACK_ENABLED = "MP_Fault_Tolerance_NonFallback_Enabled";

    @Override
    public boolean isFaultTolerance(Annotation ann) {
        return ALL_ANNOTATIONS.contains(ann.annotationType());
    }

    protected Set<Class<?>> getActiveAnnotations(Class<?> clazz) {
        ClassLoader cl = FTUtils.getClassLoader(clazz);
        Set<Class<?>> activeAnnotations = null;
        //take the read lock
        lock.readLock().lock();
        try {
            //check the cache
            activeAnnotations = activeAnnotationsCache.get(cl);
            if (activeAnnotations == null) {
                //if nothing in the cache, release read lock, take write lock
                lock.readLock().unlock();
                lock.writeLock().lock();
                try {
                    //check cache again
                    activeAnnotations = activeAnnotationsCache.get(cl);
                    if (activeAnnotations == null) {
                        //if still nothing there, read the config
                        Config mpConfig = ConfigProvider.getConfig(cl);
                        boolean allAnnotationsEnabled = mpConfig.getOptionalValue(CONFIG_NONFALLBACK_ENABLED, boolean.class).orElse(true);

                        if (allAnnotationsEnabled) {
                            activeAnnotations = ALL_ANNOTATIONS;
                        } else {
                            if (tc.isDebugEnabled()) {
                                Tr.debug(tc, "All annotations except Fallback are disabled");
                            }
                            activeAnnotations = ONLY_FALLBACK;
                        }

                        //store the config in the cache
                        activeAnnotationsCache.put(cl, activeAnnotations);
                    }
                    // Downgrade by acquiring read lock before releasing write lock
                    lock.readLock().lock();
                } finally {
                    //release the write lock
                    lock.writeLock().unlock();
                }
            }
        } finally {
            //release the read lock
            lock.readLock().unlock();
        }
        return activeAnnotations;
    }

}