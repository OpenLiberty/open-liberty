/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.artifact.overlay.internal;

import java.io.PrintWriter;

import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.wsspi.artifact.overlay.OverlayContainerFactory;
import com.ibm.wsspi.logging.Introspector;

/**
 * Service hook for introspecting overlay containers.
 */
@Component(immediate = true,
           configurationPolicy = ConfigurationPolicy.IGNORE,
           property = { Constants.SERVICE_VENDOR + "=" + "IBM" })
public class OverlayContainerIntrospector implements Introspector {
    // Tie the overlay container introspector to the overlay container
    // factory.  There should be at most one active overlay container
    // factory.

    private OverlayContainerFactoryImpl factory;

    @Reference
    public synchronized void setOverlayContainerFactory(OverlayContainerFactory factory) {
        if ( factory instanceof OverlayContainerFactoryImpl ) {
            this.factory = (OverlayContainerFactoryImpl) factory; 
        }
    }

    @SuppressWarnings("hiding")
    protected synchronized void unsetOverlayContainerFactory(OverlayContainerFactory factory) {
        this.factory = null;
    }

    // Introspector API ...

    @Override
    @Trivial
    public String getIntrospectorName() {
        return "OverlayContainerIntrospector";
    }

    @Override
    @Trivial
    public String getIntrospectorDescription() {
        return "Overlay container diagnostics";
    }

    @Override
    public void introspect(PrintWriter outputWriter) throws Exception {
        if ( factory == null ) {
            outputWriter.println("No overlay factory configured");
        } else {
            factory.introspect(outputWriter);
        }
    }
}
