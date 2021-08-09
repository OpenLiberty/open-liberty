/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.jsf;

import org.osgi.framework.ServiceReference;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.javaee.dd.jsf.FacesConfig;
import com.ibm.ws.javaee.ddmodel.DDParser.ParseException;
import com.ibm.ws.javaee.version.FacesVersion;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.adaptable.module.adapters.ContainerAdapter;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.overlay.OverlayContainer;

public final class FacesConfigAdapter implements ContainerAdapter<FacesConfig> {

    private static final int DEFAULT_JSF_LOADED_VERSION = FacesConfig.VERSION_2_0;

    private ServiceReference<FacesVersion> versionRef;
    private volatile int version = DEFAULT_JSF_LOADED_VERSION;

    public synchronized void setVersion(ServiceReference<FacesVersion> reference) {

        versionRef = reference;
        version = (Integer) reference.getProperty("version");
    }

    public synchronized void unsetVersion(ServiceReference<FacesVersion> reference) {
        if (reference == this.versionRef) {
            versionRef = null;
            version = DEFAULT_JSF_LOADED_VERSION;
        }
    }

    @FFDCIgnore(ParseException.class)
    @Override
    public FacesConfig adapt(Container root, OverlayContainer rootOverlay, ArtifactContainer artifactContainer, Container containerToAdapt) throws UnableToAdaptException {
        Entry ddEntry = containerToAdapt.getEntry(FacesConfig.DD_NAME);
        if (ddEntry != null) {
            try {
                FacesConfigDDParser ddParser = new FacesConfigDDParser(containerToAdapt, ddEntry, version);
                FacesConfig facesConfig = ddParser.parse();
                return facesConfig;
            } catch (ParseException e) {
                throw new UnableToAdaptException(e);
            }
        }
        return null;
    }
}
