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
package com.ibm.ws.jaxrs20.injection;

import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.Application;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jaxrs20.injection.metadata.InjectionRuntimeContext;

/**
 * A proxy that can be injected for Applications
 */
public class ApplicationInjectionProxy extends Application {
    private static final TraceComponent tc = Tr.register(ApplicationInjectionProxy.class);
    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = appProxy().getClasses();
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "getClasses", classes);
        }
        return classes;
    }
    @Override
    public Set<Object> getSingletons() {
        Set<Object> singletons = appProxy().getSingletons();
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "getSingletons", singletons);
        }
        return singletons;
    }
    @Override
    public Map<String, Object> getProperties() {
        Map<String, Object> props = appProxy().getProperties();
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "getProperties", props);
        }
        return props;
    }
    private Application appProxy() {
        // use runtimeContext from TLS
        InjectionRuntimeContext runtimeContext = InjectionRuntimeContextHelper.getRuntimeContext();
        // get the real context from the
        // RuntimeContext
        Object context = runtimeContext.getRuntimeCtxObject(Application.class.getName());
        return (Application) context;
    }
}