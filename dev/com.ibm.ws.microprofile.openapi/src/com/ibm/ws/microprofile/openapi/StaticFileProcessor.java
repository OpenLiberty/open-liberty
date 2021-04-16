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
package com.ibm.ws.microprofile.openapi;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.microprofile.openapi.utils.OpenAPIUtils;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;

/**
 *
 */
public class StaticFileProcessor {

    private static final TraceComponent tc = Tr.register(StaticFileProcessor.class);

    @Trivial
    @FFDCIgnore(IOException.class)
    public static String getOpenAPIFile(Container container) {
        String result = null;
        Entry openAPIFileEntry = container.getEntry("META-INF/openapi.yaml");
        if (openAPIFileEntry == null) {
            openAPIFileEntry = container.getEntry("META-INF/openapi.yml");
        }
        if (openAPIFileEntry == null) {
            openAPIFileEntry = container.getEntry("META-INF/openapi.json");
        }

        if (openAPIFileEntry == null) {
            if (OpenAPIUtils.isEventEnabled(tc)) {
                Tr.event(tc, "No static file was found");
            }
            return null;
        }

        InputStream is = entryToInputStream(openAPIFileEntry);
        if (is == null) {
            return result;
        }

        try {
            result = IOUtils.toString(is, StandardCharsets.UTF_8);
        } catch (IOException e) {
            if (OpenAPIUtils.isEventEnabled(tc)) {
                Tr.event(tc, "Unable to read openapi file into a string");
            }
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                if (OpenAPIUtils.isEventEnabled(tc)) {
                    Tr.event(tc, "Failed to close openapi file InputSteam");
                }
            }
        }

        return result;
    }

    @FFDCIgnore(UnableToAdaptException.class)
    public static InputStream entryToInputStream(Entry entry) {
        if (entry == null)
            return null;
        try {
            InputStream is = entry.adapt(InputStream.class);
            return is;
        } catch (UnableToAdaptException e) {
            if (OpenAPIUtils.isEventEnabled(tc)) {
                Tr.event(tc, "Unable to adapt {0} to InputStream", entry.getName());
            }
        }
        return null;
    }

}
