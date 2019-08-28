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
package com.ibm.ws.jaxrs20.metadata;

import com.ibm.ws.jaxrs20.bus.LibertyApplicationBus;
import com.ibm.ws.jaxrs20.bus.LibertyApplicationBusFactory;
import com.ibm.ws.jaxrs20.cache.LibertyJaxRsResourceMethodCache;

/**
 *
 */
public class JaxRsServerMetaData {

    private LibertyApplicationBus applicationBus;

    private final LibertyJaxRsResourceMethodCache resourceMethodCache = new LibertyJaxRsResourceMethodCache();

    public JaxRsServerMetaData(JaxRsModuleMetaData moduleMetaData) {
        this.applicationBus = LibertyApplicationBusFactory.getInstance().createServerScopedBus(moduleMetaData);
        //add LibertyJaxRsUriToResourceCache to server bus
        this.applicationBus.setExtension(resourceMethodCache, LibertyJaxRsResourceMethodCache.class);
    }

    public void destroy() {
        if (applicationBus != null)
            applicationBus.shutdown(false);
        applicationBus = null;

        //destroy resourceMethodCache cache
        this.resourceMethodCache.destroy();
    }

    /**
     * @return the applicationBus
     */
    public LibertyApplicationBus getServerBus() {
        return applicationBus;
    }
}
