/*******************************************************************************
 * Copyright (c) 2012, 2024 IBM Corporation and others.
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
package com.ibm.ws.container.service.app.deploy;

import java.util.Collection;
import java.util.List;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.container.service.app.deploy.extended.ManifestClassPathHelper;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;

/**
 * Utilities to assist with processing manifest classpaths
 */
public class ManifestClassPathUtils {
    static final TraceComponent tc = Tr.register(ManifestClassPathUtils.class);

    /**
     * create an Entry Identity that can identify an entry in an ear/war archive
     * This is used to avoid the cross reference in jar files' Class-Path causing the non-stopping recursion
     *
     * @param entry
     * @return
     * @throws UnableToAdaptException
     */
    public static String createEntryIdentity(Entry entry) throws UnableToAdaptException {
        String result = "";
        while (entry != null && !entry.getPath().isEmpty()) {
            result = entry.getPath() + result;
            entry = entry.getRoot().adapt(Entry.class);
        }

        return result;
    }

    public static void processMFClasspath(Entry jarEntry, List<ContainerInfo> containers, Collection<String> resolved) throws UnableToAdaptException {
        processMFClasspath(jarEntry, containers, resolved, false);
    }

    public static void processMFClasspath(Entry jarEntry, List<ContainerInfo> containers, Collection<String> resolved, boolean addRoot) throws UnableToAdaptException {
        if (jarEntry != null) {
            Container jarContainer = jarEntry.adapt(Container.class);
            if (jarContainer != null) {
                ManifestClassPathHelper.processMFClasspath(jarEntry, jarContainer, containers, resolved, addRoot);
            }
        }
    }

    /**
     * Add the jar entry URLs and its class path URLs.
     * We need deal with all the thrown exceptions so that it won't interrupt the caller's processing.
     *
     * @param urls
     * @param jarEntry
     */
    public static void addCompleteJarEntryUrls(List<ContainerInfo> containers, Entry jarEntry, Collection<String> resolved) throws UnableToAdaptException {
        String entryIdentity = createEntryIdentity(jarEntry);
        if (!entryIdentity.isEmpty() && !resolved.contains(entryIdentity)) {
            resolved.add(entryIdentity);
            processMFClasspath(jarEntry, containers, resolved);
        }
    }
}
