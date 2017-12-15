/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.metadata.builder;

import java.util.HashMap;
import java.util.Map;

import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.wsspi.adaptable.module.Container;

/**
 * The context used in JaxWsModuleInfoBuilder and its extensions
 */
public class JaxRsModuleInfoBuilderContext {

    private ModuleMetaData moduleMetaData;

    private Container container;

    private EndpointInfoBuilderContext endpointInfoBuilderContext;

    private final Map<String, Object> contextEnv = new HashMap<String, Object>();

    public JaxRsModuleInfoBuilderContext(ModuleMetaData moduleMetaData, Container container, EndpointInfoBuilderContext endpointInfoBuilderContext) {
        this.moduleMetaData = moduleMetaData;
        this.container = container;
        this.endpointInfoBuilderContext = endpointInfoBuilderContext;
    }

    public EndpointInfoBuilderContext getEndpointInfoBuilderContext() {
        return endpointInfoBuilderContext;
    }

    public void setEndpointInfoBuilderContext(EndpointInfoBuilderContext endpointInfoBuilderContext) {
        this.endpointInfoBuilderContext = endpointInfoBuilderContext;
    }

    /**
     * @return the contextEnv object based on key
     */
    public Object getContextEnv(String key) {
        return this.contextEnv.get(key);
    }

    public void addContextEnv(String key, Object value) {
        this.contextEnv.put(key, value);
    }

    public void clearContextEnv() {
        this.contextEnv.clear();
    }

    public ModuleMetaData getModuleMetaData() {
        return moduleMetaData;
    }

    public void setModuleMetaData(ModuleMetaData moduleMetaData) {
        this.moduleMetaData = moduleMetaData;
    }

    public Container getContainer() {
        return container;
    }

    public void setContainer(Container container) {
        this.container = container;
    }

}
