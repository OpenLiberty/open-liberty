/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
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
package com.ibm.ws.jcache.cdi;

import javax.enterprise.inject.spi.Extension;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;

import com.ibm.ws.cdi.extension.WebSphereCDIExtension;

/**
 * Extension point for CDI integration with JCache annotations.
 */
@Component(service = WebSphereCDIExtension.class, configurationPolicy = ConfigurationPolicy.IGNORE, immediate = true,
           property = { "api.classes=javax.cache.CacheDefaults;javax.cache.CacheKey;javax.cache.CachePut;javax.cache.CacheRemove;javax.cache.CacheRemoveAll;javax.cache.CacheResult;javax.cache.CacheValue" }
          )
public class JCacheExtension implements Extension, WebSphereCDIExtension {
    @Activate
    protected void activate(ComponentContext context) {
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
    }

    // TODO need to implement
}
