/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.faulttolerance.cdi.config.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Optional;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import com.ibm.ws.microprofile.faulttolerance.cdi.AbstractFTEnablementConfig;
import com.ibm.ws.microprofile.faulttolerance.cdi.FTUtils;

/**
 * FT 1.1 implementation for determining whether an annotation is enabled or not
 * <p>
 * Features using this implementation should extend it and register it as a component
 */
public class FTEnablementConfig11Impl extends AbstractFTEnablementConfig {

    /**
     * {@inheritDoc}
     * <p>
     * In FT 1.1, annotations can be disabled with the following syntax:
     *
     * <ul>
     * <li>Disable at the method-level: com.acme.test.MyClient/serviceA/methodA/CircuitBreaker/enabled=false</li>
     * <li>Disable on the class-level: com.acme.test.MyClient/serviceA/CircuitBreaker/enabled=false</li>
     * <li>Disable globally: CircuitBreaker/enabled=false</li>
     * </ul>
     *
     * Method-level properties take precedence over class-level properties, which in turn take precedence over global properties.
     */
    @Override
    public boolean isAnnotationEnabled(Annotation ann, Class<?> clazz) {
        return isAnnotationEnabled(ann, clazz, null);
    }

    /**
     * {@inheritDoc}
     * <p>
     * In FT 1.1, annotations can be disabled with the following syntax:
     *
     * <ul>
     * <li>Disable at the method-level: com.acme.test.MyClient/serviceA/methodA/CircuitBreaker/enabled=false</li>
     * <li>Disable on the class-level: com.acme.test.MyClient/serviceA/CircuitBreaker/enabled=false</li>
     * <li>Disable globally: CircuitBreaker/enabled=false</li>
     * </ul>
     *
     * Method-level properties take precedence over class-level properties, which in turn take precedence over global properties.
     */
    @Override
    public boolean isAnnotationEnabled(Annotation ann, Class<?> clazz, Method method) {
        if (!isFaultTolerance(ann)) {
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

}
