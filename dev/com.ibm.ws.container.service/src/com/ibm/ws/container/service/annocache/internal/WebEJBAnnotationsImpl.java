/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.container.service.annocache.internal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ibm.websphere.ras.Tr;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.annocache.classsource.ClassSource;
import com.ibm.wsspi.annocache.classsource.ClassSource_Aggregate;
import com.ibm.wsspi.annocache.classsource.ClassSource_Exception;
import com.ibm.wsspi.annocache.classsource.ClassSource_Factory;
import com.ibm.wsspi.annocache.classsource.ClassSource_MappedContainer;
import com.ibm.wsspi.annocache.classsource.ClassSource_Options;

public class WebEJBAnnotationsImpl extends WebAnnotationsImpl {
    private static final String CLASS_NAME = WebEJBAnnotationsImpl.class.getSimpleName();

    public static final String WEB_EJB_CAT_NAME = "web_ejb";

    protected WebEJBAnnotationsImpl(WebAnnotationsImpl webAnnotations) throws UnableToAdaptException {
        super(webAnnotations, WEB_EJB_CAT_NAME);

        this.webAnnotations = webAnnotations;
    }

    protected final WebAnnotationsImpl webAnnotations;

    public WebAnnotationsImpl getWebAnnotations() {
        return webAnnotations;
    }

    /**
     * Override: Create a root class source which is very
     * similar to the root class source of the base web annotations.
     *
     * Use the same application name and module name as the base
     * web annotations.
     *
     * Use the alternate "web_ejb" category name.
     *
     * Use the same intern map as the base web annotations.
     *
     * Use the same options as the base web annotations.
     *
     * @return A new root class source.
     */
    @Override
    protected ClassSource_Aggregate createRootClassSource() {
        ClassSource_Factory classSourceFactory = getClassSourceFactory();
        if (classSourceFactory == null) {
            return null;
        }

        // Mostly duplicate the web annotations:
        //
        // Use the local module category name.  The EJB in WAR results must
        // be distinguised from the WAR results.
        //
        // Later, add the containers of the web annotations class source, except,
        // do not add extra libraries.

        WebAnnotationsImpl useWebAnnotations = getWebAnnotations();

        String useAppName = useWebAnnotations.getAppName();
        String useModName = useWebAnnotations.getModName();

        String useModCatName = getModCategoryName();

        ClassSource_Aggregate webClassSource = useWebAnnotations.getClassSource();
        ClassSource_Options webOptions = webClassSource.getOptions();

        if (tc.isDebugEnabled()) {
            String prefix = CLASS_NAME + ".createRootClassSource:";
            Tr.debug(tc, prefix + " Class source" +
                         " app [ " + useAppName + " ]" +
                         " mod [ " + useModName + " ]" +
                         " mod cat [ " + useModCatName + " ]");
            Tr.debug(tc, prefix + " Scan threads [ " + webOptions.getScanThreads() + " ]");
        }

        try {
            return classSourceFactory.createAggregateClassSource(
                                                                 webClassSource.getInternMap(),
                                                                 useAppName, useModName, useModCatName,
                                                                 webOptions); // throws ClassSource_Exception
        } catch (ClassSource_Exception e) {
            return null; // FFDC
        }
    }

    /**
     * Add internal containers to the class source.
     *
     * This replicates the class source of the base web module, adding the
     * same containers in the same order, and with the same prefix and scan policies.
     *
     * Except, the extra containers are not added. These are not scanned for EJB metadata.
     */
    @Override
    protected void addInternalToClassSource() {
        if (rootClassSource == null) {
            return;
        }

        boolean isDebug = tc.isDebugEnabled();
        String prefix = (isDebug ? CLASS_NAME + ".addInternalToClassSource" : null);

        WebAnnotationsImpl useWebAnnotations = getWebAnnotations();
        ClassSource_Aggregate webSource = useWebAnnotations.getClassSource();
        List<? extends ClassSource> webLeafSources = webSource.getClassSources();

        Map<String, ClassSource> webLeafSourceMap = new HashMap<>(webLeafSources.size());
        for (ClassSource webLeafSource : webLeafSources) {
            webLeafSourceMap.put(webLeafSource.getName(), webLeafSource);
        }

        for (String internalPath : useWebAnnotations.getInternalContainers()) {
            ClassSource webLeafSource = webLeafSourceMap.get(internalPath);
            if (!(webLeafSource instanceof ClassSource_MappedContainer)) {
                if (isDebug) {
                    Tr.debug(tc, prefix + " Skip non-container source [ " + internalPath + " ]");
                }
                return; // Unexpected
            }

            ClassSource_MappedContainer typedSource = (ClassSource_MappedContainer) webLeafSource;
            Container nextContainer = typedSource.getContainer();
            String nextPrefix = typedSource.getEntryPrefix();
            ClassSource_Aggregate.ScanPolicy nextPolicy = webSource.getScanPolicy(webLeafSource);

            if (isDebug) {
                Tr.debug(tc, prefix + " Container source [ " + internalPath + " ] [ " + nextPrefix + " ] [ " + nextPolicy + " ]");
            }

            // Don't add the class source; add a new duplicate class source.

            // TODO: Can the leaf class sources be shared?

            if (!addContainerClassSource(internalPath, nextContainer, nextPrefix, nextPolicy)) {
                return; // FFDC in 'addContainerClassSource'
            }
        }

        // Don't add extra containers ... EJB does not scan them.
    }
}
