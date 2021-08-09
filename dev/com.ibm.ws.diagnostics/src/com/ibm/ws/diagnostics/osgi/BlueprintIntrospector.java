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

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.wsspi.logging.Introspector;

@Component
public class BlueprintIntrospector implements Introspector {
    private BundleContext context = null;

    @Activate
    protected void activate(BundleContext context) {
        this.context = context.getBundle(Constants.SYSTEM_BUNDLE_LOCATION).getBundleContext();
    }

    @Override
    public String getIntrospectorDescription() {
        return "Introspects blueprint containers";
    }

    @Override
    public String getIntrospectorName() {
        return "BlueprintIntrospector";
    }

    @FFDCIgnore(Throwable.class)
    @Override
    public void introspect(PrintWriter out) throws IOException {
        try {
            // Avoid any/all reference to blueprint classes outside of this method: 
            // DS will vomit when the boot code asks for all introspector services.
            // we must manually iterate through known containers at the point where the
            // dump is requested.
            BlueprintIntrospectorDetails details = new BlueprintIntrospectorDetails(context);
            details.dump(out);
        } catch (ThreadDeath tde) {
            throw tde;
        } catch (Throwable e) {
            out.println("Unable to introspect blueprint containers and events. Blueprint may not be enabled");
        }
    }
}
