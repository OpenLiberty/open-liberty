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
 * This class demonstrates a hierarchical CDI bean structure where both parent and child beans
 * inject and use EntityManager instances.
 *
 * This test is important for validating that the JPA provider correctly handles
 * EntityManager instances across a CDI bean hierarchy, ensuring proper persistence
 * context propagation between parent and child beans.
 */
@RequestScoped
public class NestedEntityManagerTest {
    
    @Inject
    private EntityManager parentEM;
    
    @Inject
    private ChildBean1 child1;
    
    @Inject
    private ChildBean2 child2;
    
    private Long parentEntityId;
    
    /**
     * Creates entities using parent and child beans
     */
    @Transactional
    public void createEntities() {
        // Create entity using parent's EntityManager
        TestEntity parentEntity = new TestEntity("Created by parent");
        parentEM.persist(parentEntity);
        parentEntityId = parentEntity.getId();
        
        // Create entities using child beans
        child1.createEntity();
        child2.createEntity();
    }
    
    /**
     * Verifies that entities created by parent and children can be found by each other
     * @return true if all verifications pass
     */
    @Transactional
    public boolean verifyEntities() {
        // Verify parent can find its own entity
        TestEntity parentEntity = parentEM.find(TestEntity.class, parentEntityId);
        if (parentEntity == null) {
            return false;
        }
        
        // Verify parent can find child entities
        TestEntity child1Entity = parentEM.find(TestEntity.class, child1.getEntityId());
        TestEntity child2Entity = parentEM.find(TestEntity.class, child2.getEntityId());
        if (child1Entity == null || child2Entity == null) {
            return false;
        }
        
        // Verify children can find parent entity
        boolean child1CanFindParent = child1.canFindEntity(parentEntityId);
        boolean child2CanFindParent = child2.canFindEntity(parentEntityId);
        if (!child1CanFindParent || !child2CanFindParent) {
            return false;
        }
        
        // Verify children can find each other's entities
        boolean child1CanFindChild2 = child1.canFindEntity(child2.getEntityId());
        boolean child2CanFindChild1 = child2.canFindEntity(child1.getEntityId());
        
        return child1CanFindChild2 && child2CanFindChild1;
    }
    
    /**
     * Updates entities using different beans than the ones that created them
     */
    @Transactional
    public void updateEntities() {
        TestEntity child1Entity = parentEM.find(TestEntity.class, child1.getEntityId());
        child1Entity.setName("Child1 updated by parent");
        
        TestEntity child2Entity = parentEM.find(TestEntity.class, child2.getEntityId());
        child2Entity.setName("Child2 updated by parent");
        
        child1.updateEntity(parentEntityId, "Parent updated by child1");
        
        child2.updateEntity(child1.getEntityId(), "Child1 updated by child2");
    }
    
    /**
     * Verifies that updates made by different beans are visible to others
     * @return true if all verifications pass
     */
    @Transactional
    public boolean verifyUpdates() {
        TestEntity parentEntity = parentEM.find(TestEntity.class, parentEntityId);
        TestEntity child1Entity = parentEM.find(TestEntity.class, child1.getEntityId());
        TestEntity child2Entity = parentEM.find(TestEntity.class, child2.getEntityId());
        
        return "Parent updated by child1".equals(parentEntity.getName()) &&
               "Child1 updated by child2".equals(child1Entity.getName()) &&
               "Child2 updated by parent".equals(child2Entity.getName());
    }
    
    /**
     * Cleans up by removing all created entities
     */
    @Transactional
    public void cleanupEntities() {
        parentEM.remove(parentEM.find(TestEntity.class, parentEntityId));
        child1.removeEntity();
        child2.removeEntity();
    }
    
    public Long getParentEntityId() {
        return parentEntityId;
    }
    
    /**
     * Child bean 1 that injects and uses EntityManager
     */
    @RequestScoped
    public static class ChildBean1 {
        
        @Inject
        @ShortScoped
        private EntityManager entityManager;
        
        private Long entityId;
        
        @Transactional
        public void createEntity() {
            TestEntity entity = new TestEntity("Created by child1");
            entityManager.persist(entity);
            entityId = entity.getId();
        }
        
        public boolean canFindEntity(Long id) {
            return entityManager.find(TestEntity.class, id) != null;
        }
        
        @Transactional
        public void updateEntity(Long id, String newName) {
            TestEntity entity = entityManager.find(TestEntity.class, id);
            if (entity != null) {
                entity.setName(newName);
            }
        }
        
        @Transactional
        public void removeEntity() {
            TestEntity entity = entityManager.find(TestEntity.class, entityId);
            if (entity != null) {
                entityManager.remove(entity);
            }
        }
        
        public Long getEntityId() {
            return entityId;
        }
    }
    
    /**
     * Child bean 2 that injects and uses EntityManager
     */
    @RequestScoped
    public static class ChildBean2 {
        
        @Inject
        @LongScoped
        private EntityManager entityManager;
        
        private Long entityId;
        
        @Transactional
        public void createEntity() {
            TestEntity entity = new TestEntity("Created by child2");
            entityManager.persist(entity);
            entityId = entity.getId();
        }
        
        public boolean canFindEntity(Long id) {
            return entityManager.find(TestEntity.class, id) != null;
        }
        
        @Transactional
        public void updateEntity(Long id, String newName) {
            TestEntity entity = entityManager.find(TestEntity.class, id);
            if (entity != null) {
                entity.setName(newName);
            }
        }
        
        @Transactional
        public void removeEntity() {
            TestEntity entity = entityManager.find(TestEntity.class, entityId);
            if (entity != null) {
                entityManager.remove(entity);
            }
        }
        
        public Long getEntityId() {
            return entityId;
        }
    }
}

