/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.container.service.config.internal;

import org.osgi.framework.ServiceReference;

import com.ibm.ws.container.service.config.WebFragmentsInfo;
import com.ibm.ws.javaee.dd.web.WebApp;
import com.ibm.ws.javaee.version.ServletVersion;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.adaptable.module.adapters.ContainerAdapter;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.overlay.OverlayContainer;

/**
 *
 */
public class WebFragmentsInfoAdapter implements ContainerAdapter<WebFragmentsInfo> {

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

    @Override
    public WebFragmentsInfo adapt(Container root, OverlayContainer rootOverlay, ArtifactContainer artifactContainer, Container containerToAdapt) throws UnableToAdaptException {
        WebFragmentsInfo fragmentsInfo = (WebFragmentsInfo) rootOverlay.getFromNonPersistentCache(artifactContainer.getPath(), WebFragmentsInfo.class);
        if (fragmentsInfo != null) {
            return fragmentsInfo;
        }

        fragmentsInfo = new WebFragmentsInfoImpl(containerToAdapt, version);

        rootOverlay.addToNonPersistentCache(artifactContainer.getPath(), WebFragmentsInfo.class, fragmentsInfo);
        return fragmentsInfo;
    }
}
