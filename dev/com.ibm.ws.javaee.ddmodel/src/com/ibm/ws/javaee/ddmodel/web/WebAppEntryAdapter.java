/*******************************************************************************
 * Copyright (c) 2011, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.web;

import org.osgi.framework.ServiceReference;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.javaee.dd.web.WebApp;
import com.ibm.ws.javaee.ddmodel.DDParser.ParseException;
import com.ibm.ws.javaee.version.ServletVersion;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.adaptable.module.adapters.EntryAdapter;
import com.ibm.wsspi.artifact.ArtifactEntry;
import com.ibm.wsspi.artifact.overlay.OverlayContainer;

public final class WebAppEntryAdapter implements EntryAdapter<WebApp> {
    private static final int DEFAULT_MAX_VERSION = WebApp.VERSION_3_0;

    private ServiceReference<ServletVersion> versionRef;
    private volatile int maxVersion = DEFAULT_MAX_VERSION;

    public synchronized void setVersion(ServiceReference<ServletVersion> versionRef) {
        this.versionRef = versionRef;

        Integer maxVersionValue = (Integer) versionRef.getProperty("version");
        maxVersion = maxVersionValue.intValue();
    }

    public synchronized void unsetVersion(ServiceReference<ServletVersion> versionRef) {
        if ( versionRef == this.versionRef ) {
            this.versionRef = null;
            this.maxVersion = DEFAULT_MAX_VERSION;
        }
    }

    @FFDCIgnore(ParseException.class)
    @Override
    public WebApp adapt(
        Container rawContainer,
        OverlayContainer container,
        ArtifactEntry rawWebAppEntry,
        Entry webAppEntry) throws UnableToAdaptException {

        String webAppPath = rawWebAppEntry.getPath();
        WebApp webAppDD = (WebApp) container.getFromNonPersistentCache(webAppPath, WebApp.class);
        if ( webAppDD == null ) {
            try {
                WebAppDDParser webAppDDParser = new WebAppDDParser(rawContainer, webAppEntry, maxVersion); // throws ParseException
                webAppDD = webAppDDParser.parse(); // throws ParseException
            } catch ( ParseException e ) {
                throw new UnableToAdaptException(e);
            }
            container.addToNonPersistentCache(webAppPath, WebApp.class, webAppDD);
        }

        return webAppDD;
    }
}
