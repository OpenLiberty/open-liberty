/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.jpa.platformtck.tests.web;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

public class EntityManagerProducers {
    
    @PersistenceContext(unitName = "PlatformTCKPersistenceUnit")
    private EntityManager defaultEM;
    
    @Produces
    @Dependent
    @ShortScoped
    public EntityManager getShortScopedEM() {
        return defaultEM;
    }
    @Produces
    @ApplicationScoped
    @LongScoped
    public EntityManager getLongScopedEM() {
        return defaultEM;
    }
}

