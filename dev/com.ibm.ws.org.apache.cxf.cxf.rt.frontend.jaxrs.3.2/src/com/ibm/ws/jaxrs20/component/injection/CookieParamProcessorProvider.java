/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jaxrs20.component.injection;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

import javax.ws.rs.CookieParam;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jaxrs20.injection.CookieParamProcessor;
import com.ibm.wsspi.injectionengine.InjectionSimpleProcessor;
import com.ibm.wsspi.injectionengine.InjectionSimpleProcessorProvider;

@Component(name = "com.ibm.ws.jaxrs20.component.injection.CookieParamProcessorProvider", service = com.ibm.wsspi.injectionengine.InjectionProcessorProvider.class,
           property = { "service.vendor=IBM" })
public class CookieParamProcessorProvider extends InjectionSimpleProcessorProvider<CookieParam> {

    final private static TraceComponent tc = Tr.register(CookieParamProcessorProvider.class);

    private Dictionary<String, Object> props = null;

    @Override
    /** {@inheritDoc} */
    public Class<CookieParam> getAnnotationClass() {
        // TODO: 11/11/11 Is returning the below correct? The java doc for 
        // InjectionSimpleProcessorProvider.createInjectionProcessor() says that
        // "the annotation class passed to the InjectionSimpleProcessor constructor 
        // must match the value returned from getAnnotationClass"
        return CookieParam.class;
    }

    @Override
    /** {@inheritDoc} */
    public InjectionSimpleProcessor<CookieParam> createInjectionProcessor() {
        return new CookieParamProcessor();
    }

    /*
     * Called by DS to activate service
     */
    @SuppressWarnings("unchecked")
    @Activate
    protected void activate(ComponentContext cc) {
        props = cc.getProperties();
    }

    /*
     * Called by DS to modify service config properties
     */
    @SuppressWarnings("unchecked")
    protected void modified(Map<?, ?> newProperties) {
        if (newProperties instanceof Dictionary) {
            props = (Dictionary<String, Object>) newProperties;
        } else {
            props = new Hashtable(newProperties);
        }
    }

}
