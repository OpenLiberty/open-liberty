/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.interfaces;

import javax.ejb.EJB;

import com.ibm.ws.cdi.CDIException;

/**
 * Interface to process an EJB module
 */
public interface EjbEndpointService {

    public WebSphereEjbServices getWebSphereEjbServices(String applicationID);

    /**
     * Validate an @EJB injection point
     *
     * @param ejb the EJB annotation
     * @param archive the archive containing the injection point
     * @param injectionType the type of the field being injected into
     * @throws ClassCastException if the corresponding EJB is found but does not match the injectionType
     * @throws CDIException if there is another problem validating the EJB
     */
    public void validateEjbInjection(EJB ejb, CDIArchive archive, Class<?> injectionType) throws ClassCastException, CDIException;

    /**
     * Create the EndPointsInfo, which includes all managed bean descriptors, ejb descriptors and all non-cdi interceptors
     *
     * @param archive The archive
     * @return the EndPointInfo object that contains all managed bean descriptors, ejb descriptors and all non-cdi interceptors
     * @throws EjbEndpointServiceException if anything goes wrong in finding the EJB/ManagedBean Endpoint information
     */
    EndPointsInfo getEndPointsInfo(CDIArchive archive) throws CDIException;

}
