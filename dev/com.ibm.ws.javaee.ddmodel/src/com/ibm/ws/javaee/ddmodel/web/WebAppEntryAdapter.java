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
    private volatile int version = DEFAULT_MAX_VERSION;

    public synchronized void setVersion(ServiceReference<ServletVersion> reference) {

        versionRef = reference;
        version = (Integer) reference.getProperty("version");
    }

    public synchronized void unsetVersion(ServiceReference<ServletVersion> reference) {
        if (reference == this.versionRef) {
            versionRef = null;
            version = DEFAULT_MAX_VERSION;
        }
    }

    @FFDCIgnore(ParseException.class)
    @Override
    public WebApp adapt(Container root, OverlayContainer rootOverlay, ArtifactEntry artifactEntry, Entry entryToAdapt) throws UnableToAdaptException {
        String path = artifactEntry.getPath();
        WebApp webApp = (WebApp) rootOverlay.getFromNonPersistentCache(path, WebApp.class);
        if (webApp == null) {
            try {
                WebAppDDParser ddParser = new WebAppDDParser(root, entryToAdapt, version);
                webApp = ddParser.parse();
            } catch (ParseException e) {
                throw new UnableToAdaptException(e);
            }

            rootOverlay.addToNonPersistentCache(path, WebApp.class, webApp);
        }

        return webApp;
    }
}
