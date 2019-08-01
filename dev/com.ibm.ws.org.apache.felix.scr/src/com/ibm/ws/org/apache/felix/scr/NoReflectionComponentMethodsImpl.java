/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.org.apache.felix.scr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.scr.impl.inject.ComponentConstructor;
import org.apache.felix.scr.impl.inject.ComponentMethods;
import org.apache.felix.scr.impl.inject.DuplexReferenceMethods;
import org.apache.felix.scr.impl.inject.LifecycleMethod;
import org.apache.felix.scr.impl.inject.ReferenceMethods;
import org.apache.felix.scr.impl.inject.field.FieldMethods;
import org.apache.felix.scr.impl.inject.methods.BindMethods;
import org.apache.felix.scr.impl.logger.ComponentLogger;
import org.apache.felix.scr.impl.metadata.ComponentMetadata;
import org.apache.felix.scr.impl.metadata.ReferenceMetadata;

import com.ibm.ws.org.apache.felix.scr.NoReflectionLifecycleMethod.LifeCycleMethodType;

public class NoReflectionComponentMethodsImpl<T> implements ComponentMethods<T> {

    private final StaticComponentManager componentManager;

    private NoReflectionLifecycleMethod activate;
    private NoReflectionLifecycleMethod deactivate;
    private NoReflectionLifecycleMethod modified;
    private ComponentConstructor<T> constructor;

    private final Map<String, ReferenceMethods> bindMethodMap = new HashMap<>();

    public NoReflectionComponentMethodsImpl(StaticComponentManager componentManager) {
        this.componentManager = componentManager;
    }

    @Override
    public synchronized void initComponentMethods(ComponentMetadata componentMetadata, Class<T> implementationObjectClass,
            ComponentLogger logger) {
        if (activate != null) {
            return;
        }
        for ( ReferenceMetadata referenceMetadata: componentMetadata.getDependencies()) {
            final String refName = referenceMetadata.getName();
            if ( referenceMetadata.getField() != null || referenceMetadata.getBind() != null) {
                bindMethodMap.put(refName, new NoReflectionBindMethods(componentManager, referenceMetadata));
            } else {
                bindMethodMap.put( refName, ReferenceMethods.NOPReferenceMethod );
            }
        }
        activate = new NoReflectionLifecycleMethod(componentManager, LifeCycleMethodType.ACTIVATE);
        deactivate = new NoReflectionLifecycleMethod(componentManager, LifeCycleMethodType.DEACTIVATE);
        modified = new NoReflectionLifecycleMethod(componentManager, LifeCycleMethodType.MODIFIED);
        constructor = new ComponentConstructor(componentMetadata, implementationObjectClass, logger);
    }

    @Override
    public LifecycleMethod getActivateMethod() {
        return activate;
    }

    @Override
    public LifecycleMethod getDeactivateMethod() {
        return deactivate;
    }

    @Override
    public LifecycleMethod getModifiedMethod() {
        return modified;
    }

    @Override
    public ReferenceMethods getBindMethods(String refName) {
        return bindMethodMap.get(refName);
    }

    @Override
    public ComponentConstructor<T> getConstructor() {
        return constructor;
    }
}