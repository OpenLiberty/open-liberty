/*******************************************************************************
 * Copyright (c) 2012, 2021 IBM Corporation and others.
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
import com.ibm.wsspi.adaptable.module.adapters.EntryAdapter;
import com.ibm.wsspi.artifact.ArtifactEntry;
import com.ibm.wsspi.artifact.overlay.OverlayContainer;

public class FacesConfigEntryAdapter implements EntryAdapter<FacesConfig> {
    private ServiceReference<FacesVersion> versionRef;
    private int version = FacesConfigAdapter.DEFAULT_JSF_VERSION;

    public synchronized void setVersion(ServiceReference<FacesVersion> reference) {
        versionRef = reference;
        version = (Integer) reference.getProperty("version");
    }

    public synchronized void unsetVersion(ServiceReference<FacesVersion> reference) {
        if ( reference == this.versionRef ) {
            versionRef = null;
            version = FacesConfigAdapter.DEFAULT_JSF_VERSION;
        }
    }

    public synchronized int getVersion() {
        return version;
    }
    
    //

    @FFDCIgnore(ParseException.class)
    @Override
    public FacesConfig adapt(
        Container root,
        OverlayContainer rootOverlay, ArtifactEntry artifactEntry,
        Entry ddEntry) throws UnableToAdaptException {

        if ( ddEntry == null ) {
            return null;
        }

        try {
            FacesConfigDDParser ddParser =
                new FacesConfigDDParser( root, ddEntry, getVersion() );
            return ddParser.parse();
        } catch ( ParseException e ) {
            throw new UnableToAdaptException(e);
        }
    }
}
