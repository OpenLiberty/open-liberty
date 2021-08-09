/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/


package com.ibm.ws.logging.hpel.osgi;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {
//    private ProviderTracker providerTracker;
//    private static Logger tracer;
    private static BundleContext bundleContext = null;

    /*
     * (non-Javadoc)
     * 
     * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
     */
    public void start(BundleContext context) throws Exception {
    	 //tracer = Logger.getLogger(Activator.class.getName());
        // create a default provider and register it
//        context.registerService(RASHPELProvider.class.getName(), makeDefaultProvider(), new Hashtable<String, String>());
        // create a tracker and track the log service
//        providerTracker = new ProviderTracker(context);
//        providerTracker.open();
        bundleContext = context;/*
        if (tracer.isLoggable(Level.FINE))
            tracer.log(Level.FINE, "RASHPEL started");*/
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
     */
    public void stop(BundleContext context) throws Exception {
//        providerTracker.close();
/*//        providerTracker = null;
        if (tracer.isLoggable(Level.FINE))
            tracer.log(Level.FINE, "RASHPEL stopped");*/
    }

    /**
     * provide bundle context for this bundle for some dynamic OSGi work
     * 
     * @return the OSGi bundle context used when starting this bundle
     */
    public static BundleContext getBundleContext() {
        return bundleContext;
    }

}
