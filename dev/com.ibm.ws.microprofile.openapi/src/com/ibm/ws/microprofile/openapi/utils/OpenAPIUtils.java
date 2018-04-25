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

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.OASModelReader;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
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

    @Trivial
    public static boolean isDebugEnabled(TraceComponent tc) {
        return TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled();
    }

    @Trivial
    public static boolean isEventEnabled(TraceComponent tc) {
        return TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled();
    }

    @Trivial
    public static boolean isDumpEnabled(TraceComponent tc) {
        return TraceComponent.isAnyTracingEnabled() && tc.isDumpEnabled();
    }

    @FFDCIgnore(UnableToAdaptException.class)
    public static WebModuleInfo getWebModuleInfo(Container container) {
        WebModuleInfo moduleInfo = null;
        NonPersistentCache overlayCache;
        try {
            overlayCache = container.adapt(NonPersistentCache.class);
        } catch (UnableToAdaptException e) {
            if (OpenAPIUtils.isEventEnabled(tc)) {
                Tr.event(tc, "Failed to get web module info: " + e.getMessage());
            }
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
            if (OpenAPIUtils.isEventEnabled(tc)) {
                Tr.event(tc, "Failed to create annotation scanner: " + e.getMessage());
            }
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    @FFDCIgnore({ ClassNotFoundException.class, InstantiationException.class, IllegalAccessException.class })
    public static OASModelReader getOASModelReader(ClassLoader appClassloader, String OASModelReaderClassName) {
        if (appClassloader == null || OASModelReaderClassName == null || OASModelReaderClassName.isEmpty()) {
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
            Tr.event(tc, "Failed to find class for model: " + OASModelReaderClassName);
        } catch (InstantiationException e) {
            Tr.event(tc, "Failed to instantiate class for model: " + OASModelReaderClassName);
        } catch (IllegalAccessException e) {
            if (OpenAPIUtils.isEventEnabled(tc)) {
                Tr.event(tc, "Failed to access class for model: " + OASModelReaderClassName);
            }
        }
        Tr.error(tc, "OPENAPI_MODEL_READER_LOAD_ERROR", OASModelReaderClassName);
        return null;
    }

    @SuppressWarnings("unchecked")
    @FFDCIgnore({ ClassNotFoundException.class, InstantiationException.class, IllegalAccessException.class })
    public static OASFilter getOASFilter(ClassLoader appClassloader, String OASFilterClassName) {
        if (appClassloader == null || OASFilterClassName == null || OASFilterClassName.isEmpty()) {
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
            if (OpenAPIUtils.isEventEnabled(tc)) {
                Tr.event(tc, "Failed to find class for filter: " + OASFilterClassName);
            }
        } catch (InstantiationException e) {
            if (OpenAPIUtils.isEventEnabled(tc)) {
                Tr.event(tc, "Failed to instantiate class for filter: " + OASFilterClassName);
            }
        } catch (IllegalAccessException e) {
            if (OpenAPIUtils.isEventEnabled(tc)) {
                Tr.event(tc, "Failed to access class for filter: " + OASFilterClassName);
            }
        }
        Tr.error(tc, "OPENAPI_FILTER_LOAD_ERROR", OASFilterClassName);
        return null;
    }

    /**
     * Get the resource at the specified URL as an InputStream
     *
     * @param url - resource location
     * @param acceptValue - Request property for 'Accept' header
     */
    public static InputStream getUrlAsStream(URL url, String acceptValue) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        if (acceptValue != null && !acceptValue.trim().isEmpty()) {
            connection.setRequestProperty("Accept", acceptValue);
        }
        final int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            return connection.getInputStream();
        } else {
            if (isEventEnabled(tc)) {
                Tr.event(tc, "Did not find resource at " + url + ".  ResponseCode: " + responseCode);
            }
        }
        return null;
    }
  
    /**
     * Print map to string
     *
     * @param map - the map to be printed to String
     */
    public static String mapToString(Map<String, ?> map) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        for (String key : map.keySet()) {
            if (map.get(key) != null) {
                sb.append(key + ": " + map.get(key) + "\n ");
            } else {
                continue;
            }
        }
        sb.append("}");
        return sb.toString();
    }
}
