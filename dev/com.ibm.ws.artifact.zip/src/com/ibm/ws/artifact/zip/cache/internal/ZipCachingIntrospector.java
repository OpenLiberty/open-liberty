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
package com.ibm.ws.artifact.zip.cache.internal;

import java.io.PrintWriter;

import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.artifact.zip.cache.ZipCachingService;
import com.ibm.wsspi.logging.Introspector;

/**
 * Service hook for introspecting the zip caching service.
 * 
 * A declarative service component annotation links the introspector
 * to the zip caching service.
 */
@Component(immediate = true,
           configurationPolicy = ConfigurationPolicy.IGNORE,
           property = { Constants.SERVICE_VENDOR + "=" + "IBM" })
public class ZipCachingIntrospector implements Introspector {

    private ZipCachingServiceImpl zipCachingService;

    @Reference
    protected void setZipCachingService(ZipCachingService zipCachingService) {
        if ( zipCachingService instanceof ZipCachingServiceImpl ) {
            this.zipCachingService = (ZipCachingServiceImpl) zipCachingService;
        }
    }

    @SuppressWarnings("hiding")
	protected void unsetZipCachingService(ZipCachingService zipCachingService) {
        this.zipCachingService = null;
    }

    //

    @Override
    @Trivial
    public String getIntrospectorName() {
        return "ZipCachingIntrospector";
    }

    @Override
    @Trivial
    public String getIntrospectorDescription() {
        return "Liberty zip file caching diagnostics";
    }

    @Override
    public void introspect(PrintWriter outputWriter) throws Exception {
        if ( zipCachingService == null ) {
            outputWriter.println("No ZipCachingServiceImpl configured");
        } else {
        	zipCachingService.introspect(outputWriter);
        }
        outputWriter.println();
    }
}
