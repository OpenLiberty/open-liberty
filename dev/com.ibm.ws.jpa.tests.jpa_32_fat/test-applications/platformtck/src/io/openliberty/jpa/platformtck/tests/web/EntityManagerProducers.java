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

/**
 * CDI producer class that creates and provides EntityManager instances with different scopes.
 *
 * This class demonstrates how to create multiple EntityManager instances from the same
 * persistence unit but with different CDI scopes. It's used in the JPA- CDI Integration
 * tests to verify that EntityManager instances with different scopes behave correctly
 * when used in various contexts.
 */
public class EntityManagerProducers {
    
    /**
     * The container-managed EntityManager injected by the container.
     * This is the source EntityManager that will be exposed through producer methods.
     */
    @PersistenceContext(unitName = "PlatformTCKPersistenceUnit")
    private EntityManager defaultEM;
    
    /**
     * Produces a short-lived EntityManager with @Dependent scope.
     * Each injection point will receive a new EntityManager.
     *
     * @return The EntityManager instance with @Dependent scope
     */
    @Produces
    @Dependent
    @ShortScoped
    public EntityManager getShortScopedEM() {
        return defaultEM;
    }
    
    /**
     * Produces a long-lived EntityManager with @ApplicationScoped scope.
     * All injection points will share the same EntityManager instance.
     *
     * @return The EntityManager instance with @ApplicationScoped scope
     */
    @Produces
    @ApplicationScoped
    @LongScoped
    public EntityManager getLongScopedEM() {
        return defaultEM;
    }
}

