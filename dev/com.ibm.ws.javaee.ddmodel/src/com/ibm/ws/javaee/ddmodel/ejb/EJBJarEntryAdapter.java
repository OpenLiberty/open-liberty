/*******************************************************************************
 * Copyright (c) 2012, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.ejb;

import org.osgi.framework.ServiceReference;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.javaee.dd.ejb.EJBJar;
import com.ibm.ws.javaee.ddmodel.DDParser.ParseException;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.adaptable.module.adapters.EntryAdapter;
import com.ibm.wsspi.artifact.ArtifactEntry;
import com.ibm.wsspi.artifact.overlay.OverlayContainer;

public class EJBJarEntryAdapter implements EntryAdapter<EJBJar> {
    private static final int DEFAULT_MAX_VERSION = EJBJar.VERSION_4_0;

    private ServiceReference<EJBJarDDParserVersion> versionRef;
    private volatile int version = DEFAULT_MAX_VERSION;

    public synchronized void setVersion(ServiceReference<EJBJarDDParserVersion> reference) {
        versionRef = reference;
        version = (Integer) reference.getProperty("version");
    }

    public synchronized void unsetVersion(ServiceReference<EJBJarDDParserVersion> reference) {
        if (reference == this.versionRef) {
            versionRef = null;
            version = DEFAULT_MAX_VERSION;
        }
    }

    @FFDCIgnore(ParseException.class)
    @Override
    public EJBJar adapt(Container root, OverlayContainer rootOverlay, ArtifactEntry artifactEntry, Entry entryToAdapt) throws UnableToAdaptException {
        String path = artifactEntry.getPath();
        EJBJar ejbJar = (EJBJar) rootOverlay.getFromNonPersistentCache(path, EJBJar.class);
        if (ejbJar == null) {
            try {
                EJBJarDDParser ddParser = new EJBJarDDParser(root, entryToAdapt, version);
                ejbJar = ddParser.parse();
            } catch (ParseException e) {
                throw new UnableToAdaptException(e);
            }

            rootOverlay.addToNonPersistentCache(path, EJBJar.class, ejbJar);
        }

        return ejbJar;
    }
}
