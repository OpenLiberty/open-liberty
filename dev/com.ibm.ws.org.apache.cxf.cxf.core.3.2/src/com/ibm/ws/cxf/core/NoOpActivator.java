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
package com.ibm.ws.cxf.core;


import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * This is intended to override the HTTPTransportActivator that
 * ships from Apache CXF.  We use the Liberty Channel Framework
 * to handle the underlying transport rather than the HTTP
 * transport layer provided by CXF.
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
