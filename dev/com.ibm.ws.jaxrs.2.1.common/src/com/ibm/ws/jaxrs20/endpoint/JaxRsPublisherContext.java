/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.endpoint;

import java.util.HashMap;
import java.util.Map;

import org.apache.cxf.Bus;

import com.ibm.ws.container.service.app.deploy.ModuleInfo;
import com.ibm.ws.jaxrs20.metadata.JaxRsModuleMetaData;
import com.ibm.wsspi.adaptable.module.Container;

/**
 *
 */
public class JaxRsPublisherContext {

    public static final String SERVLET_CONTEXT = "SERVLET_CONTEXT";

    private final JaxRsModuleMetaData moduleMetaData;

    private final Container publisherModuleContainer;

    private final ModuleInfo publisherModuleInfo;

    private final Map<String, Object> attributes = new HashMap<String, Object>();

    public JaxRsPublisherContext(JaxRsModuleMetaData moduleMetaData, Container publisherModuleContainer, ModuleInfo publisherModuleInfo) {
        this.moduleMetaData = moduleMetaData;
        this.publisherModuleContainer = publisherModuleContainer;
        this.publisherModuleInfo = publisherModuleInfo;
    }

    /**
     * @return the applicationBus
     */
    public Bus getServerBus() {
        return moduleMetaData.getServerMetaData().getServerBus();
    }

    public ModuleInfo getPublisherModuleInfo() {
        return publisherModuleInfo;
    }

    public Container getPublisherModuleContainer() {
        return publisherModuleContainer;
    }

    public void setAttribute(String name, Object value) {
        attributes.put(name, value);
    }

    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    public <T> T getAttribute(String name, Class<T> cls) {
        return cls.cast(attributes.get(name));
    }

    public JaxRsModuleMetaData getModuleMetaData() {
        return this.moduleMetaData;
    }
}
