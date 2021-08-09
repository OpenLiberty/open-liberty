/*******************************************************************************
 * Copyright (c) 2013, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.ejb.interceptor.liberty;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;

import com.ibm.ws.cdi.CDIException;
import com.ibm.ws.cdi.ejb.impl.AbstractEjbEndpointService;
import com.ibm.ws.cdi.internal.archive.liberty.CDIArchiveImpl;
import com.ibm.ws.cdi.internal.interfaces.CDIArchive;
import com.ibm.ws.cdi.internal.interfaces.EjbEndpointService;
import com.ibm.ws.ejbcontainer.EJBEndpoints;
import com.ibm.ws.ejbcontainer.ManagedBeanEndpoints;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;

/**
 * Interface to process an EJB module
 */

@Component(name = "com.ibm.ws.cdi.ejb.interceptor.liberty.EjbEndpointService", immediate = true, property = { "service.vendor=IBM", "service.ranking:Integer=100" })
public class EjbEndpointServiceImpl extends AbstractEjbEndpointService implements EjbEndpointService {

    public void activate(ComponentContext cc) {
        setInstance(this);
    }

    public void deactivate(ComponentContext cc) {
        setInstance(null);
    }

    @Override
    protected EJBEndpoints getEJBEndpoints(CDIArchive archive) throws CDIException {
        CDIArchiveImpl libertyArchive = (CDIArchiveImpl) archive;
        Container container = libertyArchive.getContainer();
        EJBEndpoints endpoints;
        try {
            endpoints = container.adapt(EJBEndpoints.class);
        } catch (UnableToAdaptException e) {
            throw new CDIException(e);
        }
        return endpoints;
    }

    @Override
    protected ManagedBeanEndpoints getManagedBeanEndpoints(CDIArchive archive) throws CDIException {
        CDIArchiveImpl libertyArchive = (CDIArchiveImpl) archive;
        Container container = libertyArchive.getContainer();
        ManagedBeanEndpoints endpoints;
        try {
            endpoints = container.adapt(ManagedBeanEndpoints.class);
        } catch (UnableToAdaptException e) {
            throw new CDIException(e);
        }
        return endpoints;
    }
}
