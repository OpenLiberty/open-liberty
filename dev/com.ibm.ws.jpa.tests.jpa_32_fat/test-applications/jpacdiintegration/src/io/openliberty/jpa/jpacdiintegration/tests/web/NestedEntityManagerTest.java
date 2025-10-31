/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.jpa.jpacdiintegration.tests.web;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import io.openliberty.jpa.jpacdiintegration.tests.models.TestEntity;

/**
 * This class demonstrates a hierarchical CDI bean structure where both parent and child beans
 * inject and use EntityManager instances.
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
        if (!parentEM.isJoinedToTransaction()) {
            parentEM.joinTransaction();
        }
        parentEM.flush();
        parentEntityId = parentEntity.getId();
        System.out.println("Parent created entity with ID: " + parentEntityId);
        
        // Create entities using child beans
        child1.createEntity();
        child2.createEntity();
        
        // Ensure all entity managers are flushed
        parentEM.flush();
    }
    
    /**
     * Verifies that entities created by parent and children can be found by each other
     * @return true if all verifications pass
     */
    @Transactional
    public boolean verifyEntities() {
        try {
            // Clear the persistence context to ensure fresh data
            parentEM.clear();
            
            // Log entity IDs for debugging
            System.out.println("Parent entity ID: " + parentEntityId);
            System.out.println("Child1 entity ID: " + child1.getEntityId());
            System.out.println("Child2 entity ID: " + child2.getEntityId());
            
            // Verify parent can find its own entity
            TestEntity parentEntity = parentEM.find(TestEntity.class, parentEntityId);
            if (parentEntity == null) {
                System.out.println("Parent cannot find its own entity");
                return false;
            }
            System.out.println("Parent found its own entity: " + parentEntity.getName());
            
            // Verify parent can find child entities
            TestEntity child1Entity = parentEM.find(TestEntity.class, child1.getEntityId());
            TestEntity child2Entity = parentEM.find(TestEntity.class, child2.getEntityId());
            if (child1Entity == null || child2Entity == null) {
                System.out.println("Parent cannot find child entities");
                System.out.println("Child1 entity found: " + (child1Entity != null));
                System.out.println("Child2 entity found: " + (child2Entity != null));
                return false;
            }
            System.out.println("Parent found child1 entity: " + child1Entity.getName());
            System.out.println("Parent found child2 entity: " + child2Entity.getName());
            
            // Verify children can find parent entity
            boolean child1CanFindParent = child1.canFindEntity(parentEntityId);
            boolean child2CanFindParent = child2.canFindEntity(parentEntityId);
            if (!child1CanFindParent || !child2CanFindParent) {
                System.out.println("Children cannot find parent entity");
                System.out.println("Child1 can find parent: " + child1CanFindParent);
                System.out.println("Child2 can find parent: " + child2CanFindParent);
                return false;
            }
            System.out.println("Both children can find parent entity");
            
            // Verify children can find each other's entities
            boolean child1CanFindChild2 = child1.canFindEntity(child2.getEntityId());
            boolean child2CanFindChild1 = child2.canFindEntity(child1.getEntityId());
            
            System.out.println("Child1 can find Child2's entity: " + child1CanFindChild2);
            System.out.println("Child2 can find Child1's entity: " + child2CanFindChild1);
            
            return child1CanFindChild2 && child2CanFindChild1;
        } catch (Exception e) {
            System.out.println("Exception during verifyEntities: " + e.getMessage());
            e.printStackTrace(System.out);
            return false;
        }
    }
    
    /**
     * Updates entities using different beans than the ones that created them
     */
    @Transactional
    public void updateEntities() {
        try {
            // Clear the persistence context to ensure fresh data
            parentEM.clear();
            
            // Update child entities using parent's EntityManager
            TestEntity child1Entity = parentEM.find(TestEntity.class, child1.getEntityId());
            if (child1Entity != null) {
                child1Entity.setName("Child1 updated by parent");
                System.out.println("Parent updated Child1 entity with ID: " + child1.getEntityId());
                if (!parentEM.isJoinedToTransaction()) {
                    parentEM.joinTransaction();
                }
                parentEM.flush();
            } else {
                System.out.println("ERROR: Parent could not find Child1 entity with ID: " + child1.getEntityId());
            }
            
            TestEntity child2Entity = parentEM.find(TestEntity.class, child2.getEntityId());
            if (child2Entity != null) {
                child2Entity.setName("Child2 updated by parent");
                System.out.println("Parent updated Child2 entity with ID: " + child2.getEntityId());
                if (!parentEM.isJoinedToTransaction()) {
                    parentEM.joinTransaction();
                }
                parentEM.flush();
            } else {
                System.out.println("ERROR: Parent could not find Child2 entity with ID: " + child2.getEntityId());
            }
            
            // Have child1 update the parent entity
            child1.updateEntity(parentEntityId, "Parent updated by child1");
            
            // Have child2 update child1's entity
            child2.updateEntity(child1.getEntityId(), "Child1 updated by child2");
        } catch (Exception e) {
            System.out.println("Exception during updateEntities: " + e.getMessage());
            e.printStackTrace(System.out);
        }
    }
    
    /**
     * Verifies that updates made by different beans are visible to others
     * @return true if all verifications pass
     */
    @Transactional
    public boolean verifyUpdates() {
        try {
            System.out.println("Starting verifyUpdates...");
            System.out.println("Parent entity ID: " + parentEntityId);
            System.out.println("Child1 entity ID: " + child1.getEntityId());
            System.out.println("Child2 entity ID: " + child2.getEntityId());
            
            // Clear the persistence context to ensure we get fresh data from the database
            parentEM.clear();
            
            // Refresh entities to get the latest state
            TestEntity parentEntity = parentEM.find(TestEntity.class, parentEntityId);
            TestEntity child1Entity = parentEM.find(TestEntity.class, child1.getEntityId());
            TestEntity child2Entity = parentEM.find(TestEntity.class, child2.getEntityId());
            
            // Check if any entities are null
            if (parentEntity == null || child1Entity == null || child2Entity == null) {
                System.out.println("One or more entities not found during update verification");
                System.out.println("Parent entity found: " + (parentEntity != null));
                System.out.println("Child1 entity found: " + (child1Entity != null));
                System.out.println("Child2 entity found: " + (child2Entity != null));
                return false;
            }
            
            // Log the actual values for debugging
            System.out.println("Parent entity name: " + parentEntity.getName());
            System.out.println("Child1 entity name: " + child1Entity.getName());
            System.out.println("Child2 entity name: " + child2Entity.getName());
            
            // Check expected values using parent's EntityManager
            boolean parentUpdated = "Parent updated by child1".equals(parentEntity.getName());
            boolean child1Updated = "Child1 updated by child2".equals(child1Entity.getName());
            boolean child2Updated = "Child2 updated by parent".equals(child2Entity.getName());
            
            System.out.println("Expected parent name: 'Parent updated by child1', actual: '" + parentEntity.getName() + "'");
            System.out.println("Expected child1 name: 'Child1 updated by child2', actual: '" + child1Entity.getName() + "'");
            System.out.println("Expected child2 name: 'Child2 updated by parent', actual: '" + child2Entity.getName() + "'");
            
            System.out.println("Parent correctly updated: " + parentUpdated);
            System.out.println("Child1 correctly updated: " + child1Updated);
            System.out.println("Child2 correctly updated: " + child2Updated);
            
            // If any verification failed, try cross-checking with child beans
            if (!parentUpdated || !child1Updated || !child2Updated) {
                System.out.println("Some updates not verified by parent, trying with child beans");
                
                // Check parent entity using child EntityManagers
                if (!parentUpdated) {
                    boolean child1VerifiesParent = child1.verifyEntityName(parentEntityId, "Parent updated by child1");
                    boolean child2VerifiesParent = child2.verifyEntityName(parentEntityId, "Parent updated by child1");
                    System.out.println("Child1 verifies parent update: " + child1VerifiesParent);
                    System.out.println("Child2 verifies parent update: " + child2VerifiesParent);
                    parentUpdated = child1VerifiesParent || child2VerifiesParent;
                }
                
                // Check child1 entity using child2's EntityManager
                if (!child1Updated) {
                    boolean child2VerifiesChild1 = child2.verifyEntityName(child1.getEntityId(), "Child1 updated by child2");
                    System.out.println("Child2 verifies Child1 update: " + child2VerifiesChild1);
                    child1Updated = child2VerifiesChild1;
                }
                
                // Check child2 entity using child1's EntityManager
                if (!child2Updated) {
                    boolean child1VerifiesChild2 = child1.verifyEntityName(child2.getEntityId(), "Child2 updated by parent");
                    System.out.println("Child1 verifies Child2 update: " + child1VerifiesChild2);
                    child2Updated = child1VerifiesChild2;
                }
                
                System.out.println("After cross-check - Parent correctly updated: " + parentUpdated);
                System.out.println("After cross-check - Child1 correctly updated: " + child1Updated);
                System.out.println("After cross-check - Child2 correctly updated: " + child2Updated);
            }
            
            boolean result = parentUpdated && child1Updated && child2Updated;
            System.out.println("Final verification result: " + result);
            return result;
        } catch (Exception e) {
            System.out.println("Exception during verifyUpdates: " + e.getMessage());
            e.printStackTrace(System.out);
            return false;
        }
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
            if (!entityManager.isJoinedToTransaction()) {
                entityManager.joinTransaction();
            }
            entityManager.flush();
            entityId = entity.getId();
            System.out.println("Child1 created entity with ID: " + entityId);
        }
        
        public boolean canFindEntity(Long id) {
            return entityManager.find(TestEntity.class, id) != null;
        }
        
        public boolean verifyEntityName(Long id, String expectedName) {
            entityManager.clear(); // Clear cache to get fresh data
            TestEntity entity = entityManager.find(TestEntity.class, id);
            if (entity == null) {
                System.out.println("ChildBean1: Entity with ID " + id + " not found");
                return false;
            }
            boolean result = expectedName.equals(entity.getName());
            System.out.println("ChildBean1: Entity " + id + " name is '" + entity.getName() +
                              "', expected '" + expectedName + "', match: " + result);
            return result;
        }
        
        @Transactional
        public void updateEntity(Long id, String newName) {
            TestEntity entity = entityManager.find(TestEntity.class, id);
            if (entity != null) {
                entity.setName(newName);
                if (!entityManager.isJoinedToTransaction()) {
                    entityManager.joinTransaction();
                }
                entityManager.flush();
                System.out.println("Child1 updated entity " + id + " to name: " + newName);
            } else {
                System.out.println("Child1 could not find entity with ID: " + id);
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
            if (!entityManager.isJoinedToTransaction()) {
                entityManager.joinTransaction();
            }
            entityManager.flush();
            entityId = entity.getId();
            System.out.println("Child2 created entity with ID: " + entityId);
        }
        
        public boolean canFindEntity(Long id) {
            return entityManager.find(TestEntity.class, id) != null;
        }
        
        public boolean verifyEntityName(Long id, String expectedName) {
            entityManager.clear(); // Clear cache to get fresh data
            TestEntity entity = entityManager.find(TestEntity.class, id);
            if (entity == null) {
                System.out.println("ChildBean2: Entity with ID " + id + " not found");
                return false;
            }
            boolean result = expectedName.equals(entity.getName());
            System.out.println("ChildBean2: Entity " + id + " name is '" + entity.getName() +
                              "', expected '" + expectedName + "', match: " + result);
            return result;
        }
        
        @Transactional
        public void updateEntity(Long id, String newName) {
            TestEntity entity = entityManager.find(TestEntity.class, id);
            if (entity != null) {
                entity.setName(newName);
                if (!entityManager.isJoinedToTransaction()) {
                    entityManager.joinTransaction();
                }
                entityManager.flush();
                System.out.println("Child2 updated entity " + id + " to name: " + newName);
            } else {
                System.out.println("Child2 could not find entity with ID: " + id);
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

