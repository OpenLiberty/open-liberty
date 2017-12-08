/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.openapi.utils;

import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.OASModelReader;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.container.service.app.deploy.WebModuleInfo;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.microprofile.openapi.AnnotationScanner;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.NonPersistentCache;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;

/**
 *
 */
public class OpenAPIUtils {
    private static final TraceComponent tc = Tr.register(OpenAPIUtils.class);

    @FFDCIgnore(UnableToAdaptException.class)
    public static WebModuleInfo getWebModuleInfo(Container container) {
        WebModuleInfo moduleInfo = null;
        NonPersistentCache overlayCache;
        try {
            overlayCache = container.adapt(NonPersistentCache.class);
        } catch (UnableToAdaptException e) {
            //TODO: add tracing
            return null;
        }
        if (overlayCache != null) {
            moduleInfo = (WebModuleInfo) overlayCache.getFromCache(WebModuleInfo.class);
        }
        return moduleInfo;
    }

    @FFDCIgnore(UnableToAdaptException.class)
    public static AnnotationScanner creatAnnotationScanner(ClassLoader classloader, Container cotainer) {
        try {
            AnnotationScanner scanner = new AnnotationScanner(classloader, cotainer);
            return scanner;
        } catch (UnableToAdaptException e) {
            //TODO: add tracing
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    @FFDCIgnore({ ClassNotFoundException.class, InstantiationException.class, IllegalAccessException.class })
    public static OASModelReader getOASModelReader(ClassLoader appClassloader, String OASModelReaderClassName) {
        if (appClassloader == null || OASModelReaderClassName == null || OASModelReaderClassName.isEmpty()) {
            //TODO: add tracing
            return null;
        }
        try {
            Class<?> clazz = appClassloader.loadClass(OASModelReaderClassName);
            Class<OASModelReader> modelReaderClass = null;
            if (OASModelReader.class.isAssignableFrom(clazz)) {
                modelReaderClass = (Class<OASModelReader>) clazz;
                OASModelReader modelReader = modelReaderClass.newInstance();
                return modelReader;
            }
        } catch (ClassNotFoundException e) {
            //TODO: add tracing
        } catch (InstantiationException e) {
            //TODO: add tracing
        } catch (IllegalAccessException e) {
            //TODO: add tracing
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    @FFDCIgnore({ ClassNotFoundException.class, InstantiationException.class, IllegalAccessException.class })
    public static OASFilter getOASFilter(ClassLoader appClassloader, String OASFilterClassName) {
        if (appClassloader == null || OASFilterClassName == null || OASFilterClassName.isEmpty()) {
            //TODO: add tracing
            return null;
        }
        try {
            Class<?> clazz = appClassloader.loadClass(OASFilterClassName);
            Class<OASFilter> oasFilterClass = null;
            if (OASFilter.class.isAssignableFrom(clazz)) {
                oasFilterClass = (Class<OASFilter>) clazz;
                OASFilter oasFilter = oasFilterClass.newInstance();
                return oasFilter;
            }
        } catch (ClassNotFoundException e) {
            //TODO: add tracing
        } catch (InstantiationException e) {
            //TODO: add tracing
        } catch (IllegalAccessException e) {
            //TODO: add tracing
        }
        return null;
    }
}
