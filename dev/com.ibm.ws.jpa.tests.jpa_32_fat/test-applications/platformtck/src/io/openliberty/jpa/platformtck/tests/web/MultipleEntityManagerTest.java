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

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import io.openliberty.jpa.platformtck.tests.models.TestEntity;

/**
 * This class demonstrates injecting and using EntityManager in multiple places
 * within a CDI bean.
 */
@RequestScoped
public class MultipleEntityManagerTest {
    
    // Inject EntityManager with different qualifiers
    @Inject
    private EntityManager defaultEM;
    
    @Inject
    @ShortScoped
    private EntityManager shortScopedEM;
    
    @Inject
    @LongScoped
    private EntityManager longScopedEM;
    
    // Fields to store entity IDs for verification
    private Long defaultEntityId;
    private Long shortScopedEntityId;
    private Long longScopedEntityId;
    
    /**
     * Creates entities using different EntityManager instances
     */
    @Transactional
    public void createEntities() {
        // Create and persist entities using different EntityManager instances
        TestEntity defaultEntity = new TestEntity("Created by defaultEM");
        defaultEM.persist(defaultEntity);
        defaultEntityId = defaultEntity.getId();
        
        TestEntity shortScopedEntity = new TestEntity("Created by shortScopedEM");
        shortScopedEM.persist(shortScopedEntity);
        shortScopedEntityId = shortScopedEntity.getId();
        
        TestEntity longScopedEntity = new TestEntity("Created by longScopedEM");
        longScopedEM.persist(longScopedEntity);
        longScopedEntityId = longScopedEntity.getId();
    }
    
    /**
     * Verifies that entities created by one EntityManager can be found by others
     * @return true if all verifications pass
     */
    @Transactional
    public boolean verifyEntities() {
        // Verify each EntityManager can find its own entity
        TestEntity defaultFound = defaultEM.find(TestEntity.class, defaultEntityId);
        TestEntity shortFound = shortScopedEM.find(TestEntity.class, shortScopedEntityId);
        TestEntity longFound = longScopedEM.find(TestEntity.class, longScopedEntityId);
        
        if (defaultFound == null || shortFound == null || longFound == null) {
            return false;
        }
        
        // Cross-verification: each EntityManager should be able to find entities created by others
        TestEntity defaultFoundByShort = shortScopedEM.find(TestEntity.class, defaultEntityId);
        TestEntity defaultFoundByLong = longScopedEM.find(TestEntity.class, defaultEntityId);
        
        TestEntity shortFoundByDefault = defaultEM.find(TestEntity.class, shortScopedEntityId);
        TestEntity shortFoundByLong = longScopedEM.find(TestEntity.class, shortScopedEntityId);
        
        TestEntity longFoundByDefault = defaultEM.find(TestEntity.class, longScopedEntityId);
        TestEntity longFoundByShort = shortScopedEM.find(TestEntity.class, longScopedEntityId);
        
        return defaultFoundByShort != null && defaultFoundByLong != null &&
               shortFoundByDefault != null && shortFoundByLong != null &&
               longFoundByDefault != null && longFoundByShort != null;
    }
    
    /**
     * Updates entities using different EntityManager instances than the ones that created them
     */
    @Transactional
    public void updateEntities() {
        // Update entities using different EntityManager instances than the ones that created them
        TestEntity defaultEntity = shortScopedEM.find(TestEntity.class, defaultEntityId);
        defaultEntity.setName("Updated by shortScopedEM");
        
        TestEntity shortEntity = longScopedEM.find(TestEntity.class, shortScopedEntityId);
        shortEntity.setName("Updated by longScopedEM");
        
        TestEntity longEntity = defaultEM.find(TestEntity.class, longScopedEntityId);
        longEntity.setName("Updated by defaultEM");
    }
    
    /**
     * Verifies that updates made by one EntityManager are visible to others
     * @return true if all verifications pass
     */
    @Transactional
    public boolean verifyUpdates() {
        TestEntity defaultEntity = defaultEM.find(TestEntity.class, defaultEntityId);
        TestEntity shortEntity = shortScopedEM.find(TestEntity.class, shortScopedEntityId);
        TestEntity longEntity = longScopedEM.find(TestEntity.class, longScopedEntityId);
        
        return "Updated by shortScopedEM".equals(defaultEntity.getName()) &&
               "Updated by longScopedEM".equals(shortEntity.getName()) &&
               "Updated by defaultEM".equals(longEntity.getName());
    }
    
    /**
     * Cleans up by removing all created entities
     */
    @Transactional
    public void cleanupEntities() {
        defaultEM.remove(defaultEM.find(TestEntity.class, defaultEntityId));
        shortScopedEM.remove(shortScopedEM.find(TestEntity.class, shortScopedEntityId));
        longScopedEM.remove(longScopedEM.find(TestEntity.class, longScopedEntityId));
    }
    
    public EntityManager getDefaultEM() {
        return defaultEM;
    }
    
    public EntityManager getShortScopedEM() {
        return shortScopedEM;
    }
    
    public EntityManager getLongScopedEM() {
        return longScopedEM;
    }
}

