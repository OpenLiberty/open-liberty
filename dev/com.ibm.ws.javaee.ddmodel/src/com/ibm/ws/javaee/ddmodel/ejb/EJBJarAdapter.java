/*******************************************************************************
 * Copyright (c) 2012, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.ejb;

import com.ibm.ws.container.service.app.deploy.ContainerInfo;
import com.ibm.ws.container.service.app.deploy.WebModuleInfo;
import com.ibm.ws.container.service.app.deploy.extended.AltDDEntryGetter;
import com.ibm.ws.javaee.dd.ejb.EJBJar;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.NonPersistentCache;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.adaptable.module.adapters.ContainerAdapter;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.overlay.OverlayContainer;

public final class EJBJarAdapter implements ContainerAdapter<EJBJar> {

    @Override
    public EJBJar adapt(Container root, OverlayContainer rootOverlay, ArtifactContainer artifactContainer, Container containerToAdapt) throws UnableToAdaptException {
        NonPersistentCache cache = containerToAdapt.adapt(NonPersistentCache.class);
        WebModuleInfo webModuleInfo = (WebModuleInfo) cache.getFromCache(WebModuleInfo.class);
        Entry ddEntry;
        if (webModuleInfo != null) {
            ddEntry = containerToAdapt.getEntry("WEB-INF/ejb-jar.xml");
        } else {
            AltDDEntryGetter altDDGetter = (AltDDEntryGetter) cache.getFromCache(AltDDEntryGetter.class);
            ddEntry = altDDGetter != null ? altDDGetter.getAltDDEntry(ContainerInfo.Type.EJB_MODULE) : null;
            if (ddEntry == null) {
                ddEntry = containerToAdapt.getEntry("META-INF/ejb-jar.xml");
            }
        }
        return ddEntry == null ? null : ddEntry.adapt(EJBJar.class);
    }
}
