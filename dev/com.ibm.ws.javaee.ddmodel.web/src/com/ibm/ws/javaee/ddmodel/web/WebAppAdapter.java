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

import com.ibm.ws.container.service.app.deploy.ContainerInfo;
import com.ibm.ws.container.service.app.deploy.extended.AltDDEntryGetter;
import com.ibm.ws.javaee.dd.web.WebApp;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.NonPersistentCache;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.adaptable.module.adapters.ContainerAdapter;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.overlay.OverlayContainer;

public final class WebAppAdapter implements ContainerAdapter<WebApp> {

    @Override
    public WebApp adapt(Container root, OverlayContainer rootOverlay, ArtifactContainer artifactContainer, Container containerToAdapt) throws UnableToAdaptException {
        NonPersistentCache cache = containerToAdapt.adapt(NonPersistentCache.class);
        WebApp webApp = (WebApp) cache.getFromCache(WebApp.class);
        if (webApp != null) {
            return webApp;
        }
        AltDDEntryGetter altDDGetter = (AltDDEntryGetter) cache.getFromCache(AltDDEntryGetter.class);
        Entry ddEntry = altDDGetter != null ? altDDGetter.getAltDDEntry(ContainerInfo.Type.WEB_MODULE) : null;
        if (ddEntry == null) {
            ddEntry = containerToAdapt.getEntry(WebApp.DD_NAME);
        }
        return ddEntry == null ? null : ddEntry.adapt(WebApp.class);
    }
}
