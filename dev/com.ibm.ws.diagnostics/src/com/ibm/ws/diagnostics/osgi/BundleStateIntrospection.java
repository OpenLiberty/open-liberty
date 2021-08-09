/*******************************************************************************
 * Copyright (c) 2011, 2014 IBM Corporation and others.
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

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.startlevel.BundleStartLevel;

import com.ibm.wsspi.logging.Introspector;

public class BundleStateIntrospection implements Introspector {
    BundleContext context;

    protected void activate(BundleContext context) {
        this.context = context.getBundle(Constants.SYSTEM_BUNDLE_LOCATION).getBundleContext();
    }

    @Override
    public String getIntrospectorName() {
        return "BundleStateIntrospection";
    }

    @Override
    public String getIntrospectorDescription() {
        return "Introspect all bundles' state.";
    }

    @Override
    public void introspect(PrintWriter out) {
        Bundle[] bundles = context.getBundles();
        out.println("  Id   [BundleState] [StartLevel] SymbolicName (Version)");
        for (Bundle bundle : bundles) {
            out.printf("[%1$4d] [%2$s] [%3$4d] %4$s (%5$s)%n",
                       bundle.getBundleId(), getBundleState(bundle), getBundleStartLevel(bundle), bundle.getSymbolicName(), bundle.getVersion());
        }
    }

    private int getBundleStartLevel(Bundle b) {
        BundleStartLevel bsl = b.adapt(BundleStartLevel.class);
        if (bsl == null) {
            return 0;
        }
        return bsl.getStartLevel();
    }

    private static String getBundleState(Bundle bundle) {
        int state = bundle.getState();
        switch (state) {
            case Bundle.UNINSTALLED:
                return "Uninstalled";
            case Bundle.INSTALLED:
                return "Installed  ";
            case Bundle.RESOLVED:
                return "Resolved   ";
            case Bundle.STOPPING:
                return "Stopping   ";
            case Bundle.STARTING:
                return "Starting   ";
            case Bundle.ACTIVE:
                return "Active     ";
            default: //should not happen
                return "Unknown    ";
        }
    }
}
