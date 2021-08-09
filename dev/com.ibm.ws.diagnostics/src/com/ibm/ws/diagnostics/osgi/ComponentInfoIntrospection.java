/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.diagnostics.osgi;

import java.io.IOException;
import java.io.PrintWriter;

import org.apache.felix.scr.info.ScrInfo;
import org.osgi.service.component.annotations.ReferenceCardinality;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.wsspi.logging.Introspector;

/**
 * This introspection lists the components by bundle and then lists the component details by bundle"
 */
@org.osgi.service.component.annotations.Component
public class ComponentInfoIntrospection implements Introspector {
    private ScrInfo scrInfo;

    @org.osgi.service.component.annotations.Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected void setScrInfo(ScrInfo scrInfo) {
        this.scrInfo = scrInfo;
    }

    protected void unsetScrInfo(ScrInfo scrInfo) {
        if (scrInfo == this.scrInfo) {
            this.scrInfo = null;
        }
    }

    @Override
    public String getIntrospectorName() {
        return "ComponentInfoIntrospection";
    }

    @Override
    public String getIntrospectorDescription() {
        return "Introspect all components' info.";
    }

    @Override
    @FFDCIgnore(value = { IllegalArgumentException.class })
    public void introspect(PrintWriter ps) throws IOException {
        ps.println("Felix DS configuration");
        ps.println();
        scrInfo.config(ps);
        ps.println();
        ps.println();

        ps.println("Summary by componentId");
        ps.println();
        try {
            scrInfo.list(null, ps);
        } catch (IllegalArgumentException e) {
            ps.println(e.getMessage());
        }

        ps.println();
        ps.println();
        ps.println("Details by bundleId, then componentId");

        try {
            scrInfo.info(null, ps);
        } catch (IllegalArgumentException e) {
            ps.println(e.getMessage());
        }
    }

}
