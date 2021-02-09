/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.openapi20;

import java.io.InputStream;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;

import io.openliberty.microprofile.openapi20.utils.Constants;
import io.openliberty.microprofile.openapi20.utils.LoggingUtils;
import io.smallrye.openapi.runtime.OpenApiStaticFile;
import io.smallrye.openapi.runtime.io.Format;

/**
 *
 */
public class StaticFileProcessor {

    private static final TraceComponent tc = Tr.register(StaticFileProcessor.class);

    @Trivial
    public static OpenApiStaticFile getOpenAPIFile(Container container) {
    	
    	// Create the variable to return
        OpenApiStaticFile staticFile = null;

        // Default to YAML format
        Format format = Format.YAML;
        
        // Attempt to find a static OpenAPI file in one of the supported locations
        Entry openAPIFileEntry = container.getEntry(Constants.STATIC_FILE_META_INF_OPENAPI_YAML);
        if (openAPIFileEntry == null) {
        	openAPIFileEntry = container.getEntry(Constants.STATIC_FILE_WEB_INF_CLASSES_META_INF_OPENAPI_YAML);
        }
        if (openAPIFileEntry == null) {
        	openAPIFileEntry = container.getEntry(Constants.STATIC_FILE_META_INF_OPENAPI_YML);
        }
        if (openAPIFileEntry == null) {
            openAPIFileEntry = container.getEntry(Constants.STATIC_FILE_WEB_INF_CLASSES_META_INF_OPENAPI_YML);
        }
        if (openAPIFileEntry == null) {
            openAPIFileEntry = container.getEntry(Constants.STATIC_FILE_META_INF_OPENAPI_JSON);
            format = Format.JSON;
        }
        if (openAPIFileEntry == null) {
            openAPIFileEntry = container.getEntry(Constants.STATIC_FILE_WEB_INF_CLASSES_META_INF_OPENAPI_JSON);
            format = Format.JSON;
        }

        // If we found a static file... process it
        if (openAPIFileEntry != null) {
            InputStream is = entryToInputStream(openAPIFileEntry);
            if (is != null) {
            	staticFile = new OpenApiStaticFile(is, format);
            }
        } else {
            if (LoggingUtils.isEventEnabled(tc)) {
                Tr.event(tc, "No static file was found");
            }
        }

        return staticFile;
    }

    @FFDCIgnore(UnableToAdaptException.class)
    public static InputStream entryToInputStream(Entry entry) {
        if (entry == null)
            return null;
        try {
            InputStream is = entry.adapt(InputStream.class);
            return is;
        } catch (UnableToAdaptException e) {
            if (LoggingUtils.isEventEnabled(tc)) {
                Tr.event(tc, "Unable to adapt {0} to InputStream", entry.getName());
            }
        }
        return null;
    }
}
