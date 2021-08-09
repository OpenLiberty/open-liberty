/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.websphere.monitor;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * A {@code Probe} contains information about a probe attach site that has
 * been injected by the monitoring runtime. Monitors callbacks that are
 * associated with a probe will be called with a reference to the {@code Probe}.
 */
public interface Probe {

    /**
     * Get the probe name. This name will indicate the class, method,
     * and probed method but will not include filter criteria.
     * 
     * @return the probe name
     */
    String getName();

    /**
     * Get a the {@link java.lang.Class} firing the probe.
     * 
     * @return the class firing the probe
     */
    Class<?> getSourceClass();

    /**
     * Get a reference to the {@link java.lang.reflect.Method} firing the
     * probe.
     * 
     * @return the method firing the probe or {@code null} for constructors
     */
    Method getSourceMethod();

/**
     * Get a reference to the {@link java.lang.reflect.Constructor) firing
     * the probe.
     *
     * @return the constructor firing the probe or {@code null} for methods
     *     other than the constructor
     */
    Constructor<?> getSourceConstructor();

    /**
     * Get the OSGi bundle identifier associated with the probed class or {@code -1} if the class was not loaded from an OSGi bundle.
     * 
     * @return the OSGi bundle identifier for the bundle owning the probed
     *         class
     */
    long getSourceBundleId();

    //    Class<?> getTargetClass();
    //    
    //    Method getTargetMethod();
    //    
    //    Constructor<?> getTargetConstructor();
    //
    //    Field getTargetField();

}
