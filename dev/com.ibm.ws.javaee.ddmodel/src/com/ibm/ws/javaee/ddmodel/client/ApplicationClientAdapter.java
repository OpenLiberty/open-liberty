/*******************************************************************************
 * Copyright (c) 2014, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.client;

import com.ibm.ws.container.service.app.deploy.ContainerInfo;
import com.ibm.ws.container.service.app.deploy.extended.AltDDEntryGetter;
import com.ibm.ws.javaee.dd.client.ApplicationClient;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.NonPersistentCache;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.adaptable.module.adapters.ContainerAdapter;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.overlay.OverlayContainer;

public final class ApplicationClientAdapter implements ContainerAdapter<ApplicationClient> {

    @Override
    public ApplicationClient adapt(Container root, OverlayContainer rootOverlay, ArtifactContainer artifactContainer, Container containerToAdapt) throws UnableToAdaptException {
        NonPersistentCache cache = containerToAdapt.adapt(NonPersistentCache.class);
        ApplicationClient appClient = (ApplicationClient) cache.getFromCache(ApplicationClient.class);
        if (appClient != null) {
            return appClient;
        }
        AltDDEntryGetter altDDGetter = (AltDDEntryGetter) cache.getFromCache(AltDDEntryGetter.class);
        Entry ddEntry = altDDGetter != null ? altDDGetter.getAltDDEntry(ContainerInfo.Type.CLIENT_MODULE) : null;
        if (ddEntry == null) {
            ddEntry = containerToAdapt.getEntry(ApplicationClient.DD_NAME);
        }
        return ddEntry == null ? null : ddEntry.adapt(ApplicationClient.class);
    }

}
