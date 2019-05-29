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
package com.ibm.ws.jaxws.metadata.builder;

import java.util.HashMap;
import java.util.Map;

import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.anno.info.InfoStore;

/**
 * The context used across EndpointInfoConfigurators.
 */
public class EndpointInfoBuilderContext {

    private final InfoStore infoStore;
    private final Container container;

    //The temporary context env. The map is cleaned-up each time invoking the EndpointInfoBuilder.build
    private final Map<String, Object> contextEnv = new HashMap<String, Object>();

    public EndpointInfoBuilderContext(InfoStore infoStore, Container container) {
        this.infoStore = infoStore;
        this.container = container;
    }

    /**
     * @return the infoStore
     */
    public InfoStore getInfoStore() {
        return infoStore;
    }

    /**
     * @return the container
     */
    public Container getContainer() {
        return container;
    }

    /**
     * @return the contextEnv
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

}
