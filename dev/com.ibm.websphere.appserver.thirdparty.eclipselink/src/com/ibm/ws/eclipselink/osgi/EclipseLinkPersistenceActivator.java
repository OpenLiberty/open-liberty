/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.eclipselink.osgi;

import java.util.Hashtable;

import javax.persistence.spi.PersistenceProvider;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 * A bundle activator for registering the EclipseLink providers into
 * the OSGi service registry.
 */
public class EclipseLinkPersistenceActivator implements BundleActivator {

    private static ServiceRegistration<?> elSvcReg = null;

    private static final String PERSISTENCE_PROVIDER = PersistenceProvider.class.getName();
    private static final String EL_OSGI_PERSISTENCE_PROVIDER = org.eclipse.persistence.jpa.PersistenceProvider.class.getName();
    private static final String JPA_WEAVING_PACKAGES = "org.apache.aries.jpa.container.weaving.packages";
    /* Note that packages listed here must also appear in the osgi.woven.packages property that
     * is defined in com.ibm.ws.app.manager.esa.internal.SubsystemHandler.
     */
    private static final String[] JPA_PACKAGES = new String[] { 
        "org.eclipse.persistence.internal.weaving",
        "org.eclipse.persistence.internal.descriptors",
        "org.eclipse.persistence.queries",
        "org.eclipse.persistence.descriptors.changetracking",
        "org.eclipse.persistence.internal.identitymaps",
        "org.eclipse.persistence.sessions",
        "org.eclipse.persistence.internal.jpa.rs.metadata.model",
        "org.eclipse.persistence.indirection" };

    /*
     * Registers the EclipseLink provider in the OSGi service registry.
     * 
     * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
     */
    @Override
    public void start(BundleContext ctx) throws Exception {
        // Call the base EclipseLink provider to register it within the service registry
        // There is currently a bug in the base class where it registers the provider name
        // using the incorrect property.  This prevents the service locator from finding
        // more than one JPA provider.  For now, register the EclipseLink provider here instead of
        // calling super to do the registration.
        // super.start(ctx);

        // Register the EclipseLink provider
        PersistenceProvider provider = new org.eclipse.persistence.jpa.PersistenceProvider();
        Hashtable<String, Object> props = new Hashtable<String, Object>();
        props.put(PERSISTENCE_PROVIDER, EL_OSGI_PERSISTENCE_PROVIDER);
        props.put("javax.persistence.provider", EL_OSGI_PERSISTENCE_PROVIDER);
        props.put("osgi.jpa.provider.version", "1.3.0");
        props.put(JPA_WEAVING_PACKAGES, JPA_PACKAGES);
        elSvcReg = ctx.registerService(PERSISTENCE_PROVIDER, provider, props);
    }

    /*
     * Removes the EclipseLinke provider from the OSGi service registry.
     * 
     * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
     */
    @Override
    public void stop(BundleContext ctx) throws Exception {
        // Unregister the EclipseLinke provider
        if (elSvcReg != null) {
            elSvcReg.unregister();
            elSvcReg = null;
        }
    }
}
