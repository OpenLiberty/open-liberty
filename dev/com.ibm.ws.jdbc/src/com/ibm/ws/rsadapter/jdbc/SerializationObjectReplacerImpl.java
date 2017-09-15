/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.rsadapter.jdbc;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.resource.ResourceRefConfigFactory;
import com.ibm.ws.serialization.SerializationObjectReplacer;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

public class SerializationObjectReplacerImpl implements SerializationObjectReplacer {

    /**
     * Reference to the resource reference config factory.
     */
    private final AtomicServiceReference<ResourceRefConfigFactory> resRefConfigFactoryRef =
                    new AtomicServiceReference<ResourceRefConfigFactory>("resRefConfigFactory");

    /**
     * DS method to activate this component.
     * Best practice: this should be a protected method, not public or private
     * 
     * @param context DeclarativeService defined/populated component context
     */
    protected void activate(ComponentContext context) throws Exception {
        resRefConfigFactoryRef.activate(context);
    }

    /**
     * DS method to deactivate this component.
     * Best practice: this should be a protected method, not public or private
     * 
     * @param context DeclarativeService defined/populated component context
     */
    protected void deactivate(ComponentContext context) throws Exception {
        resRefConfigFactoryRef.deactivate(context);
    }

    @Override
    public Object replaceObject(@Sensitive Object object) {
        if (object instanceof WSJdbcDataSource) {
            // Data sources only need to support container serialization
            // (stateful session EJB passivation, http session, etc.).
            // Arbitrary serialization won't work anyway due to ResourceRefInfo.
            return ((WSJdbcDataSource) object).replaceObject(resRefConfigFactoryRef.getServiceWithException());
        }
        return null;
    }

    /**
     * Declarative Services method for setting the ResourceRefConfigFactory reference
     * 
     * @param ref reference to the service
     */
    protected void setResRefConfigFactory(ServiceReference<ResourceRefConfigFactory> ref) {
        resRefConfigFactoryRef.setReference(ref);
    }

    /**
     * Declarative Services method for unsetting the ResourceRefConfigFactory reference
     * 
     * @param ref reference to the service
     */
    protected void unsetResRefConfigFactory(ServiceReference<ResourceRefConfigFactory> ref) {
        resRefConfigFactoryRef.unsetReference(ref);
    }
}
