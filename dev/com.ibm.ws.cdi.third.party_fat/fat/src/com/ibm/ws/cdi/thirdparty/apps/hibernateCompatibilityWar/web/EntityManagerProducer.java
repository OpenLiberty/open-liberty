/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package com.ibm.ws.cdi.thirdparty.apps.hibernateCompatibilityWar.web;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import com.ibm.ws.cdi.thirdparty.apps.hibernateCompatibilityWar.web.CustomEM;

@ApplicationScoped
public class EntityManagerProducer {
    
    @PersistenceContext(unitName = "TestPU")
    private EntityManager em;
    
    @Produces
    @CustomEM
    public EntityManager getEntityManager() {
        return em;
    }
}
