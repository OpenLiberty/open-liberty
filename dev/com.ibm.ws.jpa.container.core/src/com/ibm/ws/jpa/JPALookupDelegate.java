/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jpa;

import java.util.Map;

import com.ibm.websphere.csi.J2EEName;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

/**
 * WAS JPA lookup delegation interface.
 */
public interface JPALookupDelegate
{

    /**
     * Returns the EntityManagerFactory defines by the application/module/persistence unit spcified.
     * This is used by the resolver and naming object factory to retrieve the factory for
     * 
     * @PersistenceUnit.
     * 
     * @param unitName
     *            The name of the persistence unit to locate
     * @param j2eeName
     *            JavaEE unique identifier for the component, identifying the
     *            java:comp/env context used.
     * @return The EntityManagerFactory for the specified unit, or null if none is found.
     */
    public EntityManagerFactory getEntityManagerFactory(String unitName,
                                                        J2EEName j2eeName);

    /**
     * Returns the EntityManager defines by the application/module/persistence unit specified. This
     * is used by the naming object factory to retrieve the entity manager for
     * 
     * @PersistenceContext.
     * 
     * @param unitName
     *            The name of the persistence unit to locate
     * @param j2eeName
     *            JavaEE unique identifier for the component, identifying the
     *            java:comp/env context used.
     * @param isExtendedContextType
     *            {@code true} if the EntityManager is extended scope.
     * @param properties
     *            additional properties to create the EntityManager with
     * 
     * @return A managed EntityManager for the specified unit or null if none is found.
     */
    public EntityManager getEntityManager(String unitName,
                                          J2EEName j2eeName,
                                          boolean isExtendedContextType,
                                          Map<?, ?> properties);
}
