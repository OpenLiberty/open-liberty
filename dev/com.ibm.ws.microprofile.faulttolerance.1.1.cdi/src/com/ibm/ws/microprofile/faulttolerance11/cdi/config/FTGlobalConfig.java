/*******************************************************************************
 * Copyright (c) 2017,2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.faulttolerance11.cdi.config;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
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
import com.ibm.ws.microprofile.faulttolerance11.cdi.FTUtils;

public class FTGlobalConfig {

    private static final TraceComponent tc = Tr.register(FTGlobalConfig.class);

    private final static Map<ClassLoader, Set<Class<?>>> activeAnnotationsCache = new WeakHashMap<>();
    private final static ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private final static Set<Class<?>> ONLY_FALLBACK = Collections.singleton(Fallback.class);
    public final static Set<Class<?>> ALL_ANNOTATIONS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(Asynchronous.class, CircuitBreaker.class,
                                                                                                                Retry.class, Timeout.class, Bulkhead.class, Fallback.class)));
    private final static String CONFIG_NONFALLBACK_ENABLED = "MP_Fault_Tolerance_NonFallback_Enabled";

    /**
     * Checks if an annotation is enabled or disabled via configuration options. Annotations can be disabled with the following syntax:
     *
     * Disable on the class-level: com.acme.test.MyClient/serviceA/CircuitBreaker/enabled=false
     * Disable at the method-level: com.acme.test.MyClient/serviceA/methodA/CircuitBreaker/enabled=false
     * Disable globally: CircuitBreaker/enabled=false
     *
     * Method-level properties takes precedence over class-level properties, which in turn has precedence over global properties.
     *
     * @param ann The annotation
     * @param clazz The class containing the annotation.
     * @return Is the annotation enabled
     */
    public static boolean isAnnotationEnabled(Annotation ann, Class<?> clazz) {
        return isAnnotationEnabled(ann, clazz, null);
    }

    /**
     * Checks if an fault tolerance annotation is enabled or disabled via configuration options. Annotations can be disabled with the following syntax:
     *
     * <ul>
     * <li>Disable at the method-level: com.acme.test.MyClient/serviceA/methodA/CircuitBreaker/enabled=false</li>
     * <li>Disable on the class-level: com.acme.test.MyClient/serviceA/CircuitBreaker/enabled=false</li>
     * <li>Disable globally: CircuitBreaker/enabled=false</li>
     * </ul>
     *
     * Method-level properties take precedence over class-level properties, which in turn take precedence over global properties.
     *
     * @param ann The annotation
     * @param clazz The class containing the annotation.
     * @param method The method annotated with the annotation. If {@code null} only class and global scope properties will be checked.
     * @throws IllegalArgumentException If passed a non-fault-tolerance annotation
     * @return Is the annotation enabled
     */
    public static boolean isAnnotationEnabled(Annotation ann, Class<?> clazz, Method method) {
        if (!ALL_ANNOTATIONS.contains(ann.annotationType())) {
            throw new IllegalArgumentException(ann + " is not a fault tolerance annotation");
        }

        // Find the real class since we probably have a Weld proxy
        clazz = FTUtils.getRealClass(clazz);
        ClassLoader cl = FTUtils.getClassLoader(clazz);
        Config mpConfig = ConfigProvider.getConfig(cl);

        Boolean enabled = null;

        if (method != null) {
            String methodKey = clazz.getCanonicalName() + "/" + method.getName() + "/" + ann.annotationType().getSimpleName() + "/enabled";
            Optional<Boolean> methodEnabled = mpConfig.getOptionalValue(methodKey, Boolean.class);
            if (methodEnabled.isPresent()) {
                enabled = methodEnabled.get(); //Method scoped properties take precedence. We can return, otherwise move on to class scope.
            }
        }

        if (enabled == null) {
            String clazzKey = clazz.getCanonicalName() + "/" + ann.annotationType().getSimpleName() + "/enabled";
            Optional<Boolean> classEnabled = mpConfig.getOptionalValue(clazzKey, Boolean.class);
            if (classEnabled.isPresent()) {
                enabled = classEnabled.get();
            }
        }

        if (enabled == null) {
            String annKey = ann.annotationType().getSimpleName() + "/enabled";
            Optional<Boolean> globalEnabled = mpConfig.getOptionalValue(annKey, Boolean.class);
            if (enabled == null && globalEnabled.isPresent()) {
                enabled = globalEnabled.get();
            }
        }

        //The lowest priority is a global disabling of all fault tolerence annotations. (Only check FT annotations. Fallback is exempt from this global configuration)
        if (enabled == null && !getActiveAnnotations(clazz).contains(ann.annotationType())) {
            enabled = false;
        }

        if (enabled == null) {
            enabled = true; //The default is enabled.
        }

        return enabled;
    }

    public static Set<Class<?>> getActiveAnnotations(Class<?> clazz) {
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
