/*******************************************************************************
 * Copyright (c) 2011, 2021 IBM Corporation and others.
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
import com.ibm.ws.javaee.dd.web.WebFragment;
import com.ibm.ws.javaee.ddmodel.DDParser.ParseException;
import com.ibm.ws.javaee.version.ServletVersion;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.adaptable.module.adapters.ContainerAdapter;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.overlay.OverlayContainer;

public final class WebFragmentAdapter implements ContainerAdapter<WebFragment> {
    private ServiceReference<ServletVersion> versionRef;
    private int version = WebAppEntryAdapter.DEFAULT_MAX_VERSION;

    public synchronized void setVersion(ServiceReference<ServletVersion> reference) {
        versionRef = reference;
        version = (Integer) reference.getProperty("version");
    }

    public synchronized void unsetVersion(ServiceReference<ServletVersion> reference) {
        if ( reference == this.versionRef ) {
            versionRef = null;
            version = WebAppEntryAdapter.DEFAULT_MAX_VERSION;
        }
    }

    public synchronized int getVersion() {
        return version;
    }
    
    @FFDCIgnore(ParseException.class)
    @Override
    public WebFragment adapt(
            Container ddRoot,
            OverlayContainer rootOverlay,
            ArtifactContainer artifactContainer,
            Container ddAdaptRoot) throws UnableToAdaptException {

        Entry ddEntry = ddAdaptRoot.getEntry(WebFragment.DD_NAME);
        if ( ddEntry == null ) {
            return null;
        }

        try {
            WebFragmentDDParser ddParser =
                new WebFragmentDDParser( ddAdaptRoot, ddEntry, getVersion() );
            return ddParser.parse();
        } catch ( ParseException e ) {
            throw new UnableToAdaptException(e);
        }
    }
}
