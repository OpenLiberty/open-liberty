/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs21.rt.frontend.jaxrs;


import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * This is intended to override the org.apache.cxf.jaxrs.blueprint.Activator
 * that ships from Apache CXF.  We don't use the blueprint functionality in
 * Liberty.
 */
public class NoOpActivator implements BundleActivator {

    @Override
    public void start(BundleContext context) throws Exception {
        // no op
    }

    @Override
   public void stop(BundleContext context) throws Exception {
       // no op
   }
}
