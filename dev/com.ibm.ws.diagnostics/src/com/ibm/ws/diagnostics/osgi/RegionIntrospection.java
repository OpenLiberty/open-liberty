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

import java.io.PrintWriter;

import org.eclipse.equinox.region.Region;
import org.eclipse.equinox.region.RegionDigraph;
import org.eclipse.equinox.region.RegionDigraph.FilteredRegion;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.ibm.wsspi.logging.Introspector;

/**
 *
 */
@Component
public class RegionIntrospection implements Introspector {

    @Reference
    private RegionDigraph digraph;

    private BundleContext systemContext;

    @Activate
    protected void activate(BundleContext ctx) {
        systemContext = ctx.getBundle(Constants.SYSTEM_BUNDLE_LOCATION).getBundleContext();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.wsspi.logging.Introspector#getIntrospectorName()
     */
    @Override
    public String getIntrospectorName() {
        return "RegionIntrospection";
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.wsspi.logging.Introspector#getIntrospectorDescription()
     */
    @Override
    public String getIntrospectorDescription() {
        return "Information on the region digraph";
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.wsspi.logging.Introspector#introspect(java.io.PrintWriter)
     */
    @Override
    public void introspect(PrintWriter out) throws Exception {
        for (Region region : digraph) {
            out.println(region.getName());
            out.println("  Associated Bundles:");
            for (Long id : region.getBundleIds()) {
                Bundle b = systemContext.getBundle(id);
                out.append("    ");
                out.println(b);
            }
            out.println("  Edges:");
            for (FilteredRegion edge : region.getEdges()) {
                out.append("    ").append(edge.getRegion().getName()).append(" -> ");
                out.println(edge.getFilter());
            }
            out.println();
        }
    }

}
