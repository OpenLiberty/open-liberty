/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
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

//    private final Map<String, J2EEName> endpointNameJ2EENameMap = new HashMap<String, J2EEName>();

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
