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
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.IOUtils;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;

/**
 *
 */
public class StaticFileProcessor {

    private static final TraceComponent tc = Tr.register(StaticFileProcessor.class);

    private static final String META_INF_OPENAPI = "META-INF/openapi";

    public static boolean isYamlJsonFile(String filename) {
        if (filename.endsWith(".yaml") || filename.endsWith(".yml")) {
            return true;
        }
        if (filename.endsWith(".json")) {
            return true;
        }
        return false;
    }

    @FFDCIgnore(IOException.class)
    public static String getOpenAPIFile(Container container) {
        String result = null;
        Set<Entry> folderEntries = getOpenAPIFolderEntries(container);
        if (folderEntries == null) {
            return null;
        }
        Entry openAPIFileEntry = getOpenAPIFolderEntries(container).stream().filter(entry -> isYamlJsonFile(entry.getName())).findFirst().orElse(null);
        if (openAPIFileEntry == null) {
            return result;
        }
        InputStream is = entryToInputStream(openAPIFileEntry);
        if (is == null) {
            return result;
        }

        try {
            result = IOUtils.toString(is, "UTF-8");
        } catch (IOException e) {
            Tr.event(tc, "Unable to read openapi file into a string");
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                Tr.event(tc, "Failed to close openapi file InputSteam");
            }
        }

        return result;
    }

    @FFDCIgnore(UnableToAdaptException.class)
    public static Set<Entry> getOpenAPIFolderEntries(Container container) {
        Set<Entry> result = null;
        if (container == null) {
            return result;
        }
        Entry openAPIFolderEntry = container.getEntry(META_INF_OPENAPI);
        if (openAPIFolderEntry == null) {
            return result;
        }
        Container openAPIFolderContainer = null;
        try {
            openAPIFolderContainer = openAPIFolderEntry.adapt(Container.class);
        } catch (UnableToAdaptException e) {
            Tr.event(tc, "Unable to adapt openapi folder into a container");
        }
        if (openAPIFolderContainer == null)
            return result;
        result = new HashSet<>();
        for (Entry entry : openAPIFolderContainer) {
            result.add(entry);
        }
        return Collections.unmodifiableSet(result);
    }

    public static InputStream entryToInputStream(Entry entry) {
        if (entry == null)
            return null;
        try {
            InputStream is = entry.adapt(InputStream.class);
            return is;
        } catch (UnableToAdaptException e) {
            Tr.event(tc, "Unable to adapt {0} to InputStream", entry.getName());
        }
        return null;
    }

}
