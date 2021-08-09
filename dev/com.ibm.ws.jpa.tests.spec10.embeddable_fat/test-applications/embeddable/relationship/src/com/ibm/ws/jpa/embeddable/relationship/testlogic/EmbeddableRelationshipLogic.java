/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.embeddable.relationship.testlogic;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import javax.persistence.EntityManager;

import org.junit.Assert;

import com.ibm.ws.jpa.embeddable.relationship.model.BiM2MInverseEntity;
import com.ibm.ws.jpa.embeddable.relationship.model.BiM2MOwnerEmbed;
import com.ibm.ws.jpa.embeddable.relationship.model.BiM2OOwnerEmbed;
import com.ibm.ws.jpa.embeddable.relationship.model.BiM2OOwnerEntity;
import com.ibm.ws.jpa.embeddable.relationship.model.BiO2MInverseEmbed;
import com.ibm.ws.jpa.embeddable.relationship.model.BiO2MInverseEntity;
import com.ibm.ws.jpa.embeddable.relationship.model.BiO2OInverseAssociationOverridesEntity;
import com.ibm.ws.jpa.embeddable.relationship.model.BiO2OInverseEmbed;
import com.ibm.ws.jpa.embeddable.relationship.model.BiO2OInverseEntity;
import com.ibm.ws.jpa.embeddable.relationship.model.BiO2OOwnerAssociationOverridesEmbed;
import com.ibm.ws.jpa.embeddable.relationship.model.BiO2OOwnerEmbed;
import com.ibm.ws.jpa.embeddable.relationship.model.BiO2OOwnerEntity;
import com.ibm.ws.jpa.embeddable.relationship.model.JPAEmbeddableRelationshipEntity;
import com.ibm.ws.jpa.embeddable.relationship.model.UniM2OOwnerEmbed;
import com.ibm.ws.jpa.embeddable.relationship.model.UniO2ODummyEntity;
import com.ibm.ws.jpa.embeddable.relationship.model.UniO2OOwnerFieldAccessEmbed;
import com.ibm.ws.jpa.embeddable.relationship.model.UniO2OOwnerPropertyAccessEmbed;
import com.ibm.ws.jpa.embeddable.relationship.model.XMLBiM2MInverseEntity;
import com.ibm.ws.jpa.embeddable.relationship.model.XMLBiM2MOwnerEmbed;
import com.ibm.ws.jpa.embeddable.relationship.model.XMLBiM2OOwnerEmbed;
import com.ibm.ws.jpa.embeddable.relationship.model.XMLBiM2OOwnerEntity;
import com.ibm.ws.jpa.embeddable.relationship.model.XMLBiO2MInverseEmbed;
import com.ibm.ws.jpa.embeddable.relationship.model.XMLBiO2MInverseEntity;
import com.ibm.ws.jpa.embeddable.relationship.model.XMLBiO2OInverseAssociationOverridesEntity;
import com.ibm.ws.jpa.embeddable.relationship.model.XMLBiO2OInverseEmbed;
import com.ibm.ws.jpa.embeddable.relationship.model.XMLBiO2OInverseEntity;
import com.ibm.ws.jpa.embeddable.relationship.model.XMLBiO2OOwnerAssociationOverridesEmbed;
import com.ibm.ws.jpa.embeddable.relationship.model.XMLBiO2OOwnerEmbed;
import com.ibm.ws.jpa.embeddable.relationship.model.XMLBiO2OOwnerEntity;
import com.ibm.ws.jpa.embeddable.relationship.model.XMLJPAEmbeddableRelationshipEntity;
import com.ibm.ws.jpa.embeddable.relationship.model.XMLUniM2OOwnerEmbed;
import com.ibm.ws.jpa.embeddable.relationship.model.XMLUniO2ODummyEntity;
import com.ibm.ws.jpa.embeddable.relationship.model.XMLUniO2OOwnerFieldAccessEmbed;
import com.ibm.ws.jpa.embeddable.relationship.model.XMLUniO2OOwnerPropertyAccessEmbed;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.testlogic.AbstractTestLogic;
import com.ibm.ws.testtooling.tranjacket.TransactionJacket;
import com.ibm.ws.testtooling.vehicle.resources.JPAResource;
import com.ibm.ws.testtooling.vehicle.resources.TestExecutionResources;

public class EmbeddableRelationshipLogic extends AbstractTestLogic {

    /**
     * Test Logic: testScenario01
     *
     * <p>
     * JPA 2.0 Specifications Tested (per PFD2 spec, 11/11/2009):
     * <ul>
     * <li>2.3.2 Explicit Access Type
     * <li>2.3.3 Access Type of an Embeddable Class
     * <li>2.5 Embeddable Classes
     * <li>2.6 Collections of Embeddable Classes and Basic Types
     * <li>2.7 Map Collections
     * <li>2.7.1 Map Keys
     * <li>2.7.2 Map Values
     * <li>2.9 Entity Relationships
     * <li>2.10.1 Bidirectional OneToOne Relationships
     * <li>2.10.2 Bidirectional ManyToOne / OneToMany Relationships
     * <li>2.10.3 Unidirectional Single-Valued Relationships
     * <li>2.10.3.1 Unidirectional OneToOne Relationships
     * <li>2.10.4 Bidirectional ManyToMany Relationships
     * <li>11.1.1 Access Annotation
     * <li>11.1.2 AssociationOverride Annotation
     * <li>11.1.3 AssociationOverrides Annotation
     * <li>11.1.8 CollectionTable Annotation
     * <li>11.1.12 ElementCollection Annotation
     * <li>11.1.13 Embeddable Annotation
     * <li>11.1.14 Embedded Annotation
     * <li>11.1.25 ManyToMany Annotation
     * <li>11.1.26 ManyToOne Annotation
     * <li>11.1.29 MapKeyColumn Annotation
     * <li>11.1.36 OneToMany Annotation
     * <li>11.1.37 OneToOne Annotation
     * <li>11.1.39 OrderColumn Annotation
     * <li>12.2.2.12 embeddable
     * <li>12.2.3.15 association-override
     * <li>12.2.3.23.9 element-collection
     * <li>12.2.3.23.10 embedded
     * <li>12.2.5.2 access
     * <li>12.2.5.3.2 many-to-one
     * <li>12.2.5.3.3 one-to-many
     * <li>12.2.5.3.4 one-to-one
     * <li>12.2.5.3.5 many-to-many
     * <li>12.2.5.3.6 element-collection
     * </ul>
     *
     * <p>
     * Description: Performs basic CRUD operations:
     * <ol>
     * <li>Create a new initialized entity, and persist it to the database.
     * <li>Verify the entity was saved to the database. This verification
     * ensures <b>both</b> annotations and XML on these artifacts are respected:
     * <ol>
     * <li>Embedded field access uni-directional one-to-one relationship.
     * <li>Embedded property access uni-directional one-to-one relationship
     * (mixing field/property access).
     * <li>Embedded field access owner of a bi-directional one-to-one
     * relationship.
     * <li>Embedded field access inverse of a bi-directional one-to-one
     * relationship.
     * <li>AssociationOverrides of embedded field access inverse of a
     * bi-directional one-to-one relationship.
     * <li>Embedded field access owner of uni-directional many-to-one
     * relationship.
     * <li>Embedded field access owner of bi-directional many-to-one
     * relationship.
     * <li>Embedded field access inverse of bi-directional one-to-many
     * relationship.
     * <li>Embedded field access owner of bi-directional many-to-many
     * relationship.
     * <li>Collection&lt;Embeddable.P/A uni-directional one-to-one owner&gt;
     * using ElementCollection, CollectionTable, and sorted by OrderColumn
     * (mixing field/property access).
     * <li>List&lt;Embeddable.F/A uni-directional one-to-one owner&gt; using
     * ElementCollection, CollectionTable, and sorted by OrderColumn.
     * <li>AssociationOverrides of List&lt;Embeddable.F/A uni-directional
     * one-to-one owner&gt; using ElementCollection, CollectionTable, and sorted
     * by OrderColumn.
     * <li>Non-generic Set&lt;Embeddable.F/A uni-directional one-to-one
     * owner&gt; using ElementCollection, CollectionTable, and sorted by
     * OrderColumn.
     * <li>AssociationOverrides of key of Map&lt;Integer, Embeddable.F/A
     * uni-directional one-to-one owner&gt; using ElementCollection,
     * CollectionTable, MapKeyColumn, and sorted by OrderColumn.
     * <li>AssociationOverrides of key and value of Map&lt;Embeddable.F/A
     * uni-directional one-to-one owner, Embeddable.F/A uni-directional
     * one-to-one owner&gt; using ElementCollection, CollectionTable, and sorted
     * by OrderColumn.
     * </ol>
     * <li>Update the entity, and re-persist it to the database.
     * <li>Verify the entity update was saved to the database, as indicated
     * above.
     * <li>Delete the entity from the database.
     * <li>Verify the entity remove is successful, and all cascaded entity
     * removes are successful.
     * </ol>
     */
    @SuppressWarnings("deprecation")
    public void testEmbeddableRelationship01(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                             Object managedComponentObject) {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        // Process Test Properties
        final Map<String, Serializable> testProps = testExecCtx.getProperties();
        if (testProps != null) {
            for (String key : testProps.keySet()) {
                System.out.println("Test Property: " + key + " = " + testProps.get(key));
            }
        }

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            int id = 1;

            JPAEmbeddableRelationshipEntity newEntity = new JPAEmbeddableRelationshipEntity();

            HashSet<JPAEmbeddableRelationshipEntity> me = new HashSet<JPAEmbeddableRelationshipEntity>();
            me.add(newEntity);
            newEntity.setId(id);
            newEntity.setUniO2OOwnerFieldAccessEmbed(new UniO2OOwnerFieldAccessEmbed(new UniO2ODummyEntity(1)));
            newEntity.setUniO2OOwnerPropertyAccessEmbed(new UniO2OOwnerPropertyAccessEmbed(new UniO2ODummyEntity(2)));
            newEntity.setBiO2OOwnerEmbed(new BiO2OOwnerEmbed(new BiO2OInverseEntity(1, newEntity)));
            newEntity.setBiO2OInverseEmbed(new BiO2OInverseEmbed(new BiO2OOwnerEntity(1, newEntity)));
            newEntity.setBiO2OOwnerAssociationOverridesEmbed(new BiO2OOwnerAssociationOverridesEmbed(new BiO2OInverseAssociationOverridesEntity(1, newEntity)));
            newEntity.setUniM2OOwnerEmbed(new UniM2OOwnerEmbed(new UniO2ODummyEntity(3)));
            newEntity.setBiM2OOwnerEmbed(new BiM2OOwnerEmbed(null));
            newEntity.setBiO2MInverseEmbed(new BiO2MInverseEmbed(null));
            newEntity.setBiM2MOwnerEmbed(new BiM2MOwnerEmbed(new HashSet<BiM2MInverseEntity>()));
            newEntity.getBiM2MOwnerEmbed().getBiM2MInverseEntities().add(new BiM2MInverseEntity(1, me));
            newEntity.getBiM2MOwnerEmbed().getBiM2MInverseEntities().add(new BiM2MInverseEntity(2, me));
            newEntity.setCollectionUniO2OOwnerPropertyAccessEmbedOrderColumn(new HashSet<UniO2OOwnerPropertyAccessEmbed>());
            newEntity.getCollectionUniO2OOwnerPropertyAccessEmbedOrderColumn().add(new UniO2OOwnerPropertyAccessEmbed(new UniO2ODummyEntity(101)));
            newEntity.getCollectionUniO2OOwnerPropertyAccessEmbedOrderColumn().add(new UniO2OOwnerPropertyAccessEmbed(new UniO2ODummyEntity(100)));
            newEntity.setListUniO2OOwnerFieldAccessEmbedOrderColumn(new ArrayList<UniO2OOwnerFieldAccessEmbed>());
            newEntity.getListUniO2OOwnerFieldAccessEmbedOrderColumn().add(new UniO2OOwnerFieldAccessEmbed(new UniO2ODummyEntity(201)));
            newEntity.getListUniO2OOwnerFieldAccessEmbedOrderColumn().add(new UniO2OOwnerFieldAccessEmbed(new UniO2ODummyEntity(200)));
            newEntity.setListUniO2OOwnerFieldAccessEmbedAssociationOverridesOrderColumn(new ArrayList<UniO2OOwnerFieldAccessEmbed>());
            newEntity.getListUniO2OOwnerFieldAccessEmbedAssociationOverridesOrderColumn().add(new UniO2OOwnerFieldAccessEmbed(new UniO2ODummyEntity(301)));
            newEntity.getListUniO2OOwnerFieldAccessEmbedAssociationOverridesOrderColumn().add(new UniO2OOwnerFieldAccessEmbed(new UniO2ODummyEntity(300)));
            newEntity.setSetUniO2OOwnerFieldAccessEmbedOrderColumn(new HashSet<>());
            newEntity.getSetUniO2OOwnerFieldAccessEmbedOrderColumn().add(new UniO2OOwnerFieldAccessEmbed(new UniO2ODummyEntity(401)));
            newEntity.setMapKeyIntegerValueUniO2OOwnerFieldAccessEmbedOrderColumn(new HashMap<Integer, UniO2OOwnerFieldAccessEmbed>());
            newEntity.getMapKeyIntegerValueUniO2OOwnerFieldAccessEmbedOrderColumn().put(new Integer(2), new UniO2OOwnerFieldAccessEmbed(new UniO2ODummyEntity(502)));
            newEntity.getMapKeyIntegerValueUniO2OOwnerFieldAccessEmbedOrderColumn().put(new Integer(1), new UniO2OOwnerFieldAccessEmbed(new UniO2ODummyEntity(501)));
            newEntity.setMapKeyUniO2OOwnerFieldAccessEmbedValueUniO2OOwnerFieldAccessEmbedOrderColumn(new HashMap<UniO2OOwnerFieldAccessEmbed, UniO2OOwnerFieldAccessEmbed>());
            newEntity.getMapKeyUniO2OOwnerFieldAccessEmbedValueUniO2OOwnerFieldAccessEmbedOrderColumn()
                            .put(new UniO2OOwnerFieldAccessEmbed(new UniO2ODummyEntity(602)),
                                 new UniO2OOwnerFieldAccessEmbed(new UniO2ODummyEntity(702)));
            newEntity.getMapKeyUniO2OOwnerFieldAccessEmbedValueUniO2OOwnerFieldAccessEmbedOrderColumn()
                            .put(new UniO2OOwnerFieldAccessEmbed(new UniO2ODummyEntity(601)),
                                 new UniO2OOwnerFieldAccessEmbed(new UniO2ODummyEntity(701)));

            System.out.println("Beginning new transaction...");
            tj.beginTransaction();
            if (tj.isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                em.joinTransaction();
            }

            System.out.println("Performing persist(" + newEntity + ") operation");
            em.persist(newEntity);

            System.out.println("Committing transaction...");
            if (tj.isTransactionActive()) {
                tj.commitTransaction();
            }

            em.clear();

            System.out.println("Beginning new transaction...");
            tj.beginTransaction();
            if (tj.isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                em.joinTransaction();
            }

            // Verify the entity was saved to the database
            System.out.println("Performing find(" + JPAEmbeddableRelationshipEntity.class + ", " + id + ") operation");
            JPAEmbeddableRelationshipEntity findEntity = em.find(JPAEmbeddableRelationshipEntity.class, id);
            Assert.assertNotNull("find(" + JPAEmbeddableRelationshipEntity.class + ", " + id + ") did not return an entity.", findEntity);

            // Perform content verifications
            Assert.assertTrue(newEntity != findEntity);
            Assert.assertTrue(em.contains(findEntity));
            Assert.assertEquals(newEntity.getId(), findEntity.getId());
            Assert.assertEquals(newEntity.getUniO2OOwnerFieldAccessEmbed(), findEntity.getUniO2OOwnerFieldAccessEmbed());
            Assert.assertEquals(newEntity.getUniO2OOwnerPropertyAccessEmbed(), findEntity.getUniO2OOwnerPropertyAccessEmbed());
            Assert.assertEquals(newEntity.getBiO2OOwnerEmbed(), findEntity.getBiO2OOwnerEmbed());
            Assert.assertEquals(newEntity.getBiO2OInverseEmbed(), findEntity.getBiO2OInverseEmbed());
            Assert.assertEquals(newEntity.getBiO2OOwnerAssociationOverridesEmbed(), findEntity.getBiO2OOwnerAssociationOverridesEmbed());
            Assert.assertEquals(newEntity.getUniM2OOwnerEmbed(), findEntity.getUniM2OOwnerEmbed());
            Assert.assertEquals(newEntity.getBiM2OOwnerEmbed(), findEntity.getBiM2OOwnerEmbed());
            Assert.assertEquals(newEntity.getBiO2MInverseEmbed(), findEntity.getBiO2MInverseEmbed());
            Assert.assertEquals(newEntity.getBiM2MOwnerEmbed(), findEntity.getBiM2MOwnerEmbed());
            Assert.assertEquals(newEntity.getCollectionUniO2OOwnerPropertyAccessEmbedOrderColumn(), findEntity.getCollectionUniO2OOwnerPropertyAccessEmbedOrderColumn());
            Assert.assertEquals(newEntity.getListUniO2OOwnerFieldAccessEmbedOrderColumn(), findEntity.getListUniO2OOwnerFieldAccessEmbedOrderColumn());
            Assert.assertEquals(newEntity.getListUniO2OOwnerFieldAccessEmbedAssociationOverridesOrderColumn(),
                                findEntity.getListUniO2OOwnerFieldAccessEmbedAssociationOverridesOrderColumn());
            Assert.assertEquals(newEntity.getSetUniO2OOwnerFieldAccessEmbedOrderColumn(), findEntity.getSetUniO2OOwnerFieldAccessEmbedOrderColumn());
            Assert.assertEquals(newEntity.getMapKeyIntegerValueUniO2OOwnerFieldAccessEmbedOrderColumn(), findEntity.getMapKeyIntegerValueUniO2OOwnerFieldAccessEmbedOrderColumn());
            Assert.assertEquals(newEntity.getMapKeyUniO2OOwnerFieldAccessEmbedValueUniO2OOwnerFieldAccessEmbedOrderColumn(),
                                findEntity.getMapKeyUniO2OOwnerFieldAccessEmbedValueUniO2OOwnerFieldAccessEmbedOrderColumn());

            // Update the entity
            System.out.println("Performing remove(" + findEntity.getBiO2OInverseEmbed().getBiO2OOwnerEntity() + ") operation");
            em.remove(findEntity.getBiO2OInverseEmbed().getBiO2OOwnerEntity());

            me.clear();
            me.add(findEntity);
            // Don't touch ID.
            //TODO: OPENJPA-2874: orphaned reference is not removed from the database
            findEntity.setUniO2OOwnerFieldAccessEmbed(new UniO2OOwnerFieldAccessEmbed(new UniO2ODummyEntity(11)));
            findEntity.getUniO2OOwnerPropertyAccessEmbed().setUniO2ODummyEntity_PA(new UniO2ODummyEntity(12));
            findEntity.setBiO2OOwnerEmbed(new BiO2OOwnerEmbed(new BiO2OInverseEntity(11, findEntity)));
            findEntity.setBiO2OInverseEmbed(new BiO2OInverseEmbed(new BiO2OOwnerEntity(11, findEntity)));
            findEntity.setBiO2OOwnerAssociationOverridesEmbed(new BiO2OOwnerAssociationOverridesEmbed(new BiO2OInverseAssociationOverridesEntity(11, findEntity)));
            findEntity.getUniM2OOwnerEmbed().setUniO2MDummyEntity(null);
            findEntity.getBiM2OOwnerEmbed().setBiO2MInverseEntity(new BiO2MInverseEntity(11, me));
            HashSet<BiM2OOwnerEntity> other = new HashSet<BiM2OOwnerEntity>();
            other.add(new BiM2OOwnerEntity(11, findEntity));
            other.add(new BiM2OOwnerEntity(12, findEntity));
            findEntity.setBiO2MInverseEmbed(new BiO2MInverseEmbed(other));
            findEntity.getBiM2MOwnerEmbed().getBiM2MInverseEntities().add(new BiM2MInverseEntity(3, me));
            findEntity.getCollectionUniO2OOwnerPropertyAccessEmbedOrderColumn().add(new UniO2OOwnerPropertyAccessEmbed(new UniO2ODummyEntity(102)));
            findEntity.getListUniO2OOwnerFieldAccessEmbedOrderColumn().add(new UniO2OOwnerFieldAccessEmbed(new UniO2ODummyEntity(202)));
            findEntity.getListUniO2OOwnerFieldAccessEmbedAssociationOverridesOrderColumn().add(new UniO2OOwnerFieldAccessEmbed(new UniO2ODummyEntity(302)));
            findEntity.getSetUniO2OOwnerFieldAccessEmbedOrderColumn().add(new UniO2OOwnerFieldAccessEmbed(new UniO2ODummyEntity(401))); // Dup.
            findEntity.getSetUniO2OOwnerFieldAccessEmbedOrderColumn().add(new UniO2OOwnerFieldAccessEmbed(new UniO2ODummyEntity(400)));
            findEntity.getMapKeyIntegerValueUniO2OOwnerFieldAccessEmbedOrderColumn().put(new Integer(0), new UniO2OOwnerFieldAccessEmbed(new UniO2ODummyEntity(500)));
            findEntity.getMapKeyUniO2OOwnerFieldAccessEmbedValueUniO2OOwnerFieldAccessEmbedOrderColumn()
                            .put(new UniO2OOwnerFieldAccessEmbed(new UniO2ODummyEntity(600)),
                                 new UniO2OOwnerFieldAccessEmbed(new UniO2ODummyEntity(700)));

            System.out.println("Committing transaction...");
            if (tj.isTransactionActive()) {
                tj.commitTransaction();
            }

            em.clear();

            System.out.println("Beginning new transaction...");
            tj.beginTransaction();
            if (tj.isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                em.joinTransaction();
            }

            // Verify the entity update was saved to the database
            System.out.println("Performing find(" + JPAEmbeddableRelationshipEntity.class + ", " + id + ") operation");
            JPAEmbeddableRelationshipEntity updatedEntity = em.find(JPAEmbeddableRelationshipEntity.class, id);
            Assert.assertNotNull("find(" + JPAEmbeddableRelationshipEntity.class + ", " + id + ") did not return an entity.", updatedEntity);

            // Perform content verifications
            Assert.assertTrue(newEntity != updatedEntity);
            Assert.assertTrue(findEntity != updatedEntity);
            Assert.assertTrue(em.contains(updatedEntity));
            Assert.assertEquals(updatedEntity.getId(), findEntity.getId());
            Assert.assertEquals(findEntity.getUniO2OOwnerFieldAccessEmbed(), updatedEntity.getUniO2OOwnerFieldAccessEmbed());
            Assert.assertEquals(findEntity.getUniO2OOwnerPropertyAccessEmbed(), updatedEntity.getUniO2OOwnerPropertyAccessEmbed());
            Assert.assertEquals(findEntity.getBiO2OOwnerEmbed(), updatedEntity.getBiO2OOwnerEmbed());
            Assert.assertEquals(findEntity.getBiO2OInverseEmbed(), updatedEntity.getBiO2OInverseEmbed());
            Assert.assertEquals(findEntity.getBiO2OOwnerAssociationOverridesEmbed(), updatedEntity.getBiO2OOwnerAssociationOverridesEmbed());
            Assert.assertEquals(findEntity.getUniM2OOwnerEmbed(), updatedEntity.getUniM2OOwnerEmbed());
            Assert.assertEquals(findEntity.getBiM2OOwnerEmbed(), updatedEntity.getBiM2OOwnerEmbed());
            Assert.assertEquals(findEntity.getBiO2MInverseEmbed(), updatedEntity.getBiO2MInverseEmbed());
            Assert.assertEquals(findEntity.getBiM2MOwnerEmbed(), updatedEntity.getBiM2MOwnerEmbed());
            Assert.assertEquals(findEntity.getCollectionUniO2OOwnerPropertyAccessEmbedOrderColumn(), updatedEntity.getCollectionUniO2OOwnerPropertyAccessEmbedOrderColumn());
            Assert.assertEquals(findEntity.getListUniO2OOwnerFieldAccessEmbedOrderColumn(), updatedEntity.getListUniO2OOwnerFieldAccessEmbedOrderColumn());
            Assert.assertEquals(findEntity.getListUniO2OOwnerFieldAccessEmbedAssociationOverridesOrderColumn(),
                                updatedEntity.getListUniO2OOwnerFieldAccessEmbedAssociationOverridesOrderColumn());
            Assert.assertEquals(findEntity.getSetUniO2OOwnerFieldAccessEmbedOrderColumn(), updatedEntity.getSetUniO2OOwnerFieldAccessEmbedOrderColumn());
            Assert.assertEquals(findEntity.getMapKeyIntegerValueUniO2OOwnerFieldAccessEmbedOrderColumn(),
                                updatedEntity.getMapKeyIntegerValueUniO2OOwnerFieldAccessEmbedOrderColumn());
            Assert.assertEquals(findEntity.getMapKeyUniO2OOwnerFieldAccessEmbedValueUniO2OOwnerFieldAccessEmbedOrderColumn(),
                                updatedEntity.getMapKeyUniO2OOwnerFieldAccessEmbedValueUniO2OOwnerFieldAccessEmbedOrderColumn());

            // Delete the entity from the database
            System.out.println("Performing remove(" + updatedEntity + ") operation");
            em.remove(updatedEntity);

            System.out.println("Committing transaction...");
            if (tj.isTransactionActive()) {
                tj.commitTransaction();
            }

            em.clear();

            // Verify the entity remove was successful
            System.out.println("Performing find(" + JPAEmbeddableRelationshipEntity.class + ", " + id + ") operation");
            Object findRemovedEntity = em.find(JPAEmbeddableRelationshipEntity.class, id);
            Assert.assertNull("find(" + JPAEmbeddableRelationshipEntity.class + ", " + id + ") returned an entity.", findRemovedEntity);

            // Perform the find on related objects that should cascade in their delete.
            findRemovedEntity = em.find(UniO2ODummyEntity.class, 11);
            Assert.assertNull(findRemovedEntity);

            findRemovedEntity = em.find(UniO2ODummyEntity.class, 12);
            Assert.assertNull(findRemovedEntity);

            findRemovedEntity = em.find(BiO2OInverseEntity.class, 11);
            Assert.assertNull(findRemovedEntity);

            findRemovedEntity = em.find(BiO2OInverseAssociationOverridesEntity.class, 11);
            Assert.assertNull(findRemovedEntity);

            findRemovedEntity = em.find(BiO2MInverseEntity.class, 11);
            Assert.assertNull(findRemovedEntity);

            findRemovedEntity = em.find(BiM2OOwnerEntity.class, 11);
            Assert.assertNull(findRemovedEntity);

            findRemovedEntity = em.find(BiM2OOwnerEntity.class, 12);
            Assert.assertNull(findRemovedEntity);

            findRemovedEntity = em.find(BiM2MInverseEntity.class, 1);
            Assert.assertNull(findRemovedEntity);

            findRemovedEntity = em.find(BiM2MInverseEntity.class, 2);
            Assert.assertNull(findRemovedEntity);

            findRemovedEntity = em.find(BiM2MInverseEntity.class, 3);
            Assert.assertNull(findRemovedEntity);

            findRemovedEntity = em.find(UniO2ODummyEntity.class, 100);
            Assert.assertNull(findRemovedEntity);

            findRemovedEntity = em.find(UniO2ODummyEntity.class, 101);
            Assert.assertNull(findRemovedEntity);

            findRemovedEntity = em.find(UniO2ODummyEntity.class, 102);
            Assert.assertNull(findRemovedEntity);

            findRemovedEntity = em.find(UniO2ODummyEntity.class, 200);
            Assert.assertNull(findRemovedEntity);

            findRemovedEntity = em.find(UniO2ODummyEntity.class, 201);
            Assert.assertNull(findRemovedEntity);

            findRemovedEntity = em.find(UniO2ODummyEntity.class, 202);
            Assert.assertNull(findRemovedEntity);

            findRemovedEntity = em.find(UniO2ODummyEntity.class, 300);
            Assert.assertNull(findRemovedEntity);

            findRemovedEntity = em.find(UniO2ODummyEntity.class, 301);
            Assert.assertNull(findRemovedEntity);

            findRemovedEntity = em.find(UniO2ODummyEntity.class, 302);
            Assert.assertNull(findRemovedEntity);

            findRemovedEntity = em.find(UniO2ODummyEntity.class, 400);
            Assert.assertNull(findRemovedEntity);

            findRemovedEntity = em.find(UniO2ODummyEntity.class, 401);
            Assert.assertNull(findRemovedEntity);

            findRemovedEntity = em.find(UniO2ODummyEntity.class, 600);
            Assert.assertNull(findRemovedEntity);

            findRemovedEntity = em.find(UniO2ODummyEntity.class, 601);
            Assert.assertNull(findRemovedEntity);

            findRemovedEntity = em.find(UniO2ODummyEntity.class, 602);
            Assert.assertNull(findRemovedEntity);

            findRemovedEntity = em.find(UniO2ODummyEntity.class, 700);
            Assert.assertNull(findRemovedEntity);

            findRemovedEntity = em.find(UniO2ODummyEntity.class, 701);
            Assert.assertNull(findRemovedEntity);

            findRemovedEntity = em.find(UniO2ODummyEntity.class, 702);
            Assert.assertNull(findRemovedEntity);
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace(System.err);
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    @SuppressWarnings("deprecation")
    public void testEmbeddableRelationship02(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                             Object managedComponentObject) {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        // Process Test Properties
        final Map<String, Serializable> testProps = testExecCtx.getProperties();
        if (testProps != null) {
            for (String key : testProps.keySet()) {
                System.out.println("Test Property: " + key + " = " + testProps.get(key));
            }
        }

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            int id = 2;

            XMLJPAEmbeddableRelationshipEntity newEntity = new XMLJPAEmbeddableRelationshipEntity();

            HashSet<XMLJPAEmbeddableRelationshipEntity> me = new HashSet<XMLJPAEmbeddableRelationshipEntity>();
            me.add(newEntity);
            newEntity.setId(id);
            newEntity.setUniO2OOwnerFieldAccessEmbed(new XMLUniO2OOwnerFieldAccessEmbed(new XMLUniO2ODummyEntity(1)));
            newEntity.setUniO2OOwnerPropertyAccessEmbed(new XMLUniO2OOwnerPropertyAccessEmbed(new XMLUniO2ODummyEntity(2)));
            newEntity.setBiO2OOwnerEmbed(new XMLBiO2OOwnerEmbed(new XMLBiO2OInverseEntity(1, newEntity)));
            newEntity.setBiO2OInverseEmbed(new XMLBiO2OInverseEmbed(new XMLBiO2OOwnerEntity(1, newEntity)));
            newEntity.setBiO2OOwnerAssociationOverridesEmbed(new XMLBiO2OOwnerAssociationOverridesEmbed(new XMLBiO2OInverseAssociationOverridesEntity(1, newEntity)));
            newEntity.setUniM2OOwnerEmbed(new XMLUniM2OOwnerEmbed(new XMLUniO2ODummyEntity(3)));
            newEntity.setBiM2OOwnerEmbed(new XMLBiM2OOwnerEmbed(null));
            newEntity.setBiO2MInverseEmbed(new XMLBiO2MInverseEmbed(null));
            newEntity.setBiM2MOwnerEmbed(new XMLBiM2MOwnerEmbed(new HashSet<XMLBiM2MInverseEntity>()));
            newEntity.getBiM2MOwnerEmbed().getBiM2MInverseEntities().add(new XMLBiM2MInverseEntity(1, me));
            newEntity.getBiM2MOwnerEmbed().getBiM2MInverseEntities().add(new XMLBiM2MInverseEntity(2, me));
            newEntity.setCollectionUniO2OOwnerPropertyAccessEmbedOrderColumn(new HashSet<XMLUniO2OOwnerPropertyAccessEmbed>());
            newEntity.getCollectionUniO2OOwnerPropertyAccessEmbedOrderColumn().add(new XMLUniO2OOwnerPropertyAccessEmbed(new XMLUniO2ODummyEntity(101)));
            newEntity.getCollectionUniO2OOwnerPropertyAccessEmbedOrderColumn().add(new XMLUniO2OOwnerPropertyAccessEmbed(new XMLUniO2ODummyEntity(100)));
            newEntity.setListUniO2OOwnerFieldAccessEmbedOrderColumn(new ArrayList<XMLUniO2OOwnerFieldAccessEmbed>());
            newEntity.getListUniO2OOwnerFieldAccessEmbedOrderColumn().add(new XMLUniO2OOwnerFieldAccessEmbed(new XMLUniO2ODummyEntity(201)));
            newEntity.getListUniO2OOwnerFieldAccessEmbedOrderColumn().add(new XMLUniO2OOwnerFieldAccessEmbed(new XMLUniO2ODummyEntity(200)));
            newEntity.setListUniO2OOwnerFieldAccessEmbedAssociationOverridesOrderColumn(new ArrayList<XMLUniO2OOwnerFieldAccessEmbed>());
            newEntity.getListUniO2OOwnerFieldAccessEmbedAssociationOverridesOrderColumn().add(new XMLUniO2OOwnerFieldAccessEmbed(new XMLUniO2ODummyEntity(301)));
            newEntity.getListUniO2OOwnerFieldAccessEmbedAssociationOverridesOrderColumn().add(new XMLUniO2OOwnerFieldAccessEmbed(new XMLUniO2ODummyEntity(300)));
            newEntity.setSetUniO2OOwnerFieldAccessEmbedOrderColumn(new HashSet<XMLUniO2OOwnerFieldAccessEmbed>());
            newEntity.getSetUniO2OOwnerFieldAccessEmbedOrderColumn().add(new XMLUniO2OOwnerFieldAccessEmbed(new XMLUniO2ODummyEntity(401)));
            newEntity.setMapKeyIntegerValueUniO2OOwnerFieldAccessEmbedOrderColumn(new HashMap<Integer, XMLUniO2OOwnerFieldAccessEmbed>());
            newEntity.getMapKeyIntegerValueUniO2OOwnerFieldAccessEmbedOrderColumn().put(new Integer(2), new XMLUniO2OOwnerFieldAccessEmbed(new XMLUniO2ODummyEntity(502)));
            newEntity.getMapKeyIntegerValueUniO2OOwnerFieldAccessEmbedOrderColumn().put(new Integer(1), new XMLUniO2OOwnerFieldAccessEmbed(new XMLUniO2ODummyEntity(501)));
            newEntity.setMapKeyUniO2OOwnerFieldAccessEmbedValueUniO2OOwnerFieldAccessEmbedOrderColumn(new HashMap<XMLUniO2OOwnerFieldAccessEmbed, XMLUniO2OOwnerFieldAccessEmbed>());
            newEntity.getMapKeyUniO2OOwnerFieldAccessEmbedValueUniO2OOwnerFieldAccessEmbedOrderColumn()
                            .put(new XMLUniO2OOwnerFieldAccessEmbed(new XMLUniO2ODummyEntity(602)),
                                 new XMLUniO2OOwnerFieldAccessEmbed(new XMLUniO2ODummyEntity(702)));
            newEntity.getMapKeyUniO2OOwnerFieldAccessEmbedValueUniO2OOwnerFieldAccessEmbedOrderColumn()
                            .put(new XMLUniO2OOwnerFieldAccessEmbed(new XMLUniO2ODummyEntity(601)),
                                 new XMLUniO2OOwnerFieldAccessEmbed(new XMLUniO2ODummyEntity(701)));

            System.out.println("Beginning new transaction...");
            tj.beginTransaction();
            if (tj.isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                em.joinTransaction();
            }

            System.out.println("Performing persist(" + newEntity + ") operation");
            em.persist(newEntity);

            System.out.println("Committing transaction...");
            if (tj.isTransactionActive()) {
                tj.commitTransaction();
            }

            em.clear();

            System.out.println("Beginning new transaction...");
            tj.beginTransaction();
            if (tj.isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                em.joinTransaction();
            }

            // Verify the entity was saved to the database
            System.out.println("Performing find(" + XMLJPAEmbeddableRelationshipEntity.class + ", " + id + ") operation");
            XMLJPAEmbeddableRelationshipEntity findEntity = em.find(XMLJPAEmbeddableRelationshipEntity.class, id);
            Assert.assertNotNull("find(" + XMLJPAEmbeddableRelationshipEntity.class + ", " + id + ") did not return an entity.", findEntity);

            // Perform content verifications
            Assert.assertTrue(newEntity != findEntity);
            Assert.assertTrue(em.contains(findEntity));
            Assert.assertEquals(newEntity.getId(), findEntity.getId());
            Assert.assertEquals(newEntity.getUniO2OOwnerFieldAccessEmbed(), findEntity.getUniO2OOwnerFieldAccessEmbed());
            Assert.assertEquals(newEntity.getUniO2OOwnerPropertyAccessEmbed(), findEntity.getUniO2OOwnerPropertyAccessEmbed());
            Assert.assertEquals(newEntity.getBiO2OOwnerEmbed(), findEntity.getBiO2OOwnerEmbed());
            Assert.assertEquals(newEntity.getBiO2OInverseEmbed(), findEntity.getBiO2OInverseEmbed());
            Assert.assertEquals(newEntity.getBiO2OOwnerAssociationOverridesEmbed(), findEntity.getBiO2OOwnerAssociationOverridesEmbed());
            Assert.assertEquals(newEntity.getUniM2OOwnerEmbed(), findEntity.getUniM2OOwnerEmbed());
            Assert.assertEquals(newEntity.getBiM2OOwnerEmbed(), findEntity.getBiM2OOwnerEmbed());
            Assert.assertEquals(newEntity.getBiO2MInverseEmbed(), findEntity.getBiO2MInverseEmbed());
            Assert.assertEquals(newEntity.getBiM2MOwnerEmbed(), findEntity.getBiM2MOwnerEmbed());
            Assert.assertEquals(newEntity.getCollectionUniO2OOwnerPropertyAccessEmbedOrderColumn(), findEntity.getCollectionUniO2OOwnerPropertyAccessEmbedOrderColumn());
            Assert.assertEquals(newEntity.getListUniO2OOwnerFieldAccessEmbedOrderColumn(), findEntity.getListUniO2OOwnerFieldAccessEmbedOrderColumn());
            Assert.assertEquals(newEntity.getListUniO2OOwnerFieldAccessEmbedAssociationOverridesOrderColumn(),
                                findEntity.getListUniO2OOwnerFieldAccessEmbedAssociationOverridesOrderColumn());
            Assert.assertEquals(newEntity.getSetUniO2OOwnerFieldAccessEmbedOrderColumn(), findEntity.getSetUniO2OOwnerFieldAccessEmbedOrderColumn());
            Assert.assertEquals(newEntity.getMapKeyIntegerValueUniO2OOwnerFieldAccessEmbedOrderColumn(), findEntity.getMapKeyIntegerValueUniO2OOwnerFieldAccessEmbedOrderColumn());
            Assert.assertEquals(newEntity.getMapKeyUniO2OOwnerFieldAccessEmbedValueUniO2OOwnerFieldAccessEmbedOrderColumn(),
                                findEntity.getMapKeyUniO2OOwnerFieldAccessEmbedValueUniO2OOwnerFieldAccessEmbedOrderColumn());

            // Update the entity
            System.out.println("Performing remove(" + findEntity.getBiO2OInverseEmbed().getBiO2OOwnerEntity() + ") operation");
            em.remove(findEntity.getBiO2OInverseEmbed().getBiO2OOwnerEntity());

            me.clear();
            me.add(findEntity);
            // Don't touch ID.
            findEntity.setUniO2OOwnerFieldAccessEmbed(new XMLUniO2OOwnerFieldAccessEmbed(new XMLUniO2ODummyEntity(11)));
            findEntity.getUniO2OOwnerPropertyAccessEmbed().setUniO2ODummyEntity_PA(new XMLUniO2ODummyEntity(12));
            findEntity.setBiO2OOwnerEmbed(new XMLBiO2OOwnerEmbed(new XMLBiO2OInverseEntity(11, findEntity)));
            findEntity.setBiO2OInverseEmbed(new XMLBiO2OInverseEmbed(new XMLBiO2OOwnerEntity(11, findEntity)));
            findEntity.setBiO2OOwnerAssociationOverridesEmbed(new XMLBiO2OOwnerAssociationOverridesEmbed(new XMLBiO2OInverseAssociationOverridesEntity(11, findEntity)));
            findEntity.getUniM2OOwnerEmbed().setUniO2MDummyEntity(null);
            findEntity.getBiM2OOwnerEmbed().setBiO2MInverseEntity(new XMLBiO2MInverseEntity(11, me));
            HashSet<XMLBiM2OOwnerEntity> other = new HashSet<XMLBiM2OOwnerEntity>();
            other.add(new XMLBiM2OOwnerEntity(11, findEntity));
            other.add(new XMLBiM2OOwnerEntity(12, findEntity));
            findEntity.setBiO2MInverseEmbed(new XMLBiO2MInverseEmbed(other));
            findEntity.getBiM2MOwnerEmbed().getBiM2MInverseEntities().add(new XMLBiM2MInverseEntity(3, me));
            findEntity.getCollectionUniO2OOwnerPropertyAccessEmbedOrderColumn().add(new XMLUniO2OOwnerPropertyAccessEmbed(new XMLUniO2ODummyEntity(102)));
            findEntity.getListUniO2OOwnerFieldAccessEmbedOrderColumn().add(new XMLUniO2OOwnerFieldAccessEmbed(new XMLUniO2ODummyEntity(202)));
            findEntity.getListUniO2OOwnerFieldAccessEmbedAssociationOverridesOrderColumn().add(new XMLUniO2OOwnerFieldAccessEmbed(new XMLUniO2ODummyEntity(302)));
            findEntity.getSetUniO2OOwnerFieldAccessEmbedOrderColumn().add(new XMLUniO2OOwnerFieldAccessEmbed(new XMLUniO2ODummyEntity(401))); // Dup.
            findEntity.getSetUniO2OOwnerFieldAccessEmbedOrderColumn().add(new XMLUniO2OOwnerFieldAccessEmbed(new XMLUniO2ODummyEntity(400)));
            findEntity.getMapKeyIntegerValueUniO2OOwnerFieldAccessEmbedOrderColumn().put(new Integer(0), new XMLUniO2OOwnerFieldAccessEmbed(new XMLUniO2ODummyEntity(500)));
            findEntity.getMapKeyUniO2OOwnerFieldAccessEmbedValueUniO2OOwnerFieldAccessEmbedOrderColumn()
                            .put(new XMLUniO2OOwnerFieldAccessEmbed(new XMLUniO2ODummyEntity(600)),
                                 new XMLUniO2OOwnerFieldAccessEmbed(new XMLUniO2ODummyEntity(700)));

            System.out.println("Committing transaction...");
            if (tj.isTransactionActive()) {
                tj.commitTransaction();
            }

            em.clear();

            System.out.println("Beginning new transaction...");
            tj.beginTransaction();
            if (tj.isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                em.joinTransaction();
            }

            // Verify the entity update was saved to the database
            System.out.println("Performing find(" + XMLJPAEmbeddableRelationshipEntity.class + ", " + id + ") operation");
            XMLJPAEmbeddableRelationshipEntity updatedEntity = em.find(XMLJPAEmbeddableRelationshipEntity.class, id);
            Assert.assertNotNull("find(" + XMLJPAEmbeddableRelationshipEntity.class + ", " + id + ") did not return an entity.", updatedEntity);

            // Perform content verifications
            Assert.assertTrue(newEntity != updatedEntity);
            Assert.assertTrue(findEntity != updatedEntity);
            Assert.assertTrue(em.contains(updatedEntity));
            Assert.assertEquals(updatedEntity.getId(), findEntity.getId());
            Assert.assertEquals(findEntity.getUniO2OOwnerFieldAccessEmbed(), updatedEntity.getUniO2OOwnerFieldAccessEmbed());
            Assert.assertEquals(findEntity.getUniO2OOwnerPropertyAccessEmbed(), updatedEntity.getUniO2OOwnerPropertyAccessEmbed());
            Assert.assertEquals(findEntity.getBiO2OOwnerEmbed(), updatedEntity.getBiO2OOwnerEmbed());
            Assert.assertEquals(findEntity.getBiO2OInverseEmbed(), updatedEntity.getBiO2OInverseEmbed());
            Assert.assertEquals(findEntity.getBiO2OOwnerAssociationOverridesEmbed(), updatedEntity.getBiO2OOwnerAssociationOverridesEmbed());
            Assert.assertEquals(findEntity.getUniM2OOwnerEmbed(), updatedEntity.getUniM2OOwnerEmbed());
            Assert.assertEquals(findEntity.getBiM2OOwnerEmbed(), updatedEntity.getBiM2OOwnerEmbed());
            Assert.assertEquals(findEntity.getBiO2MInverseEmbed(), updatedEntity.getBiO2MInverseEmbed());
            Assert.assertEquals(findEntity.getBiM2MOwnerEmbed(), updatedEntity.getBiM2MOwnerEmbed());
            Assert.assertEquals(findEntity.getCollectionUniO2OOwnerPropertyAccessEmbedOrderColumn(), updatedEntity.getCollectionUniO2OOwnerPropertyAccessEmbedOrderColumn());
            Assert.assertEquals(findEntity.getListUniO2OOwnerFieldAccessEmbedOrderColumn(), updatedEntity.getListUniO2OOwnerFieldAccessEmbedOrderColumn());
            Assert.assertEquals(findEntity.getListUniO2OOwnerFieldAccessEmbedAssociationOverridesOrderColumn(),
                                updatedEntity.getListUniO2OOwnerFieldAccessEmbedAssociationOverridesOrderColumn());
            Assert.assertEquals(findEntity.getSetUniO2OOwnerFieldAccessEmbedOrderColumn(), updatedEntity.getSetUniO2OOwnerFieldAccessEmbedOrderColumn());
            Assert.assertEquals(findEntity.getMapKeyIntegerValueUniO2OOwnerFieldAccessEmbedOrderColumn(),
                                updatedEntity.getMapKeyIntegerValueUniO2OOwnerFieldAccessEmbedOrderColumn());
            Assert.assertEquals(findEntity.getMapKeyUniO2OOwnerFieldAccessEmbedValueUniO2OOwnerFieldAccessEmbedOrderColumn(),
                                updatedEntity.getMapKeyUniO2OOwnerFieldAccessEmbedValueUniO2OOwnerFieldAccessEmbedOrderColumn());

            // Delete the entity from the database
            System.out.println("Performing remove(" + updatedEntity + ") operation");
            em.remove(updatedEntity);

            System.out.println("Committing transaction...");
            if (tj.isTransactionActive()) {
                tj.commitTransaction();
            }

            em.clear();

            // Verify the entity remove was successful
            System.out.println("Performing find(" + XMLJPAEmbeddableRelationshipEntity.class + ", " + id + ") operation");
            Object findRemovedEntity = em.find(XMLJPAEmbeddableRelationshipEntity.class, id);
            Assert.assertNull("find(" + XMLJPAEmbeddableRelationshipEntity.class + ", " + id + ") returned an entity.", findRemovedEntity);

            // Perform the find on related objects that should cascade in their delete.
            Assert.assertNull(findRemovedEntity);

            // Perform the find on related objects that should cascade in their delete.
            findRemovedEntity = em.find(XMLUniO2ODummyEntity.class, 11);
            Assert.assertNull(findRemovedEntity);

            findRemovedEntity = em.find(XMLUniO2ODummyEntity.class, 12);
            Assert.assertNull(findRemovedEntity);

            findRemovedEntity = em.find(XMLBiO2OInverseEntity.class, 11);
            Assert.assertNull(findRemovedEntity);

            findRemovedEntity = em.find(XMLBiO2OInverseAssociationOverridesEntity.class, 11);
            Assert.assertNull(findRemovedEntity);

            findRemovedEntity = em.find(XMLBiO2MInverseEntity.class, 11);
            Assert.assertNull(findRemovedEntity);

            findRemovedEntity = em.find(XMLBiM2OOwnerEntity.class, 11);
            Assert.assertNull(findRemovedEntity);

            findRemovedEntity = em.find(XMLBiM2OOwnerEntity.class, 12);
            Assert.assertNull(findRemovedEntity);

            findRemovedEntity = em.find(XMLBiM2MInverseEntity.class, 1);
            Assert.assertNull(findRemovedEntity);

            findRemovedEntity = em.find(XMLBiM2MInverseEntity.class, 2);
            Assert.assertNull(findRemovedEntity);

            findRemovedEntity = em.find(XMLBiM2MInverseEntity.class, 3);
            Assert.assertNull(findRemovedEntity);

            findRemovedEntity = em.find(XMLUniO2ODummyEntity.class, 100);
            Assert.assertNull(findRemovedEntity);

            findRemovedEntity = em.find(XMLUniO2ODummyEntity.class, 101);
            Assert.assertNull(findRemovedEntity);

            findRemovedEntity = em.find(XMLUniO2ODummyEntity.class, 102);
            Assert.assertNull(findRemovedEntity);

            findRemovedEntity = em.find(XMLUniO2ODummyEntity.class, 200);
            Assert.assertNull(findRemovedEntity);

            findRemovedEntity = em.find(XMLUniO2ODummyEntity.class, 201);
            Assert.assertNull(findRemovedEntity);

            findRemovedEntity = em.find(XMLUniO2ODummyEntity.class, 202);
            Assert.assertNull(findRemovedEntity);

            findRemovedEntity = em.find(XMLUniO2ODummyEntity.class, 300);
            Assert.assertNull(findRemovedEntity);

            findRemovedEntity = em.find(XMLUniO2ODummyEntity.class, 301);
            Assert.assertNull(findRemovedEntity);

            findRemovedEntity = em.find(XMLUniO2ODummyEntity.class, 302);
            Assert.assertNull(findRemovedEntity);

            findRemovedEntity = em.find(XMLUniO2ODummyEntity.class, 400);
            Assert.assertNull(findRemovedEntity);

            findRemovedEntity = em.find(XMLUniO2ODummyEntity.class, 401);
            Assert.assertNull(findRemovedEntity);

            findRemovedEntity = em.find(XMLUniO2ODummyEntity.class, 600);
            Assert.assertNull(findRemovedEntity);

            findRemovedEntity = em.find(XMLUniO2ODummyEntity.class, 601);
            Assert.assertNull(findRemovedEntity);

            findRemovedEntity = em.find(XMLUniO2ODummyEntity.class, 602);
            Assert.assertNull(findRemovedEntity);

            findRemovedEntity = em.find(XMLUniO2ODummyEntity.class, 700);
            Assert.assertNull(findRemovedEntity);

            findRemovedEntity = em.find(XMLUniO2ODummyEntity.class, 701);
            Assert.assertNull(findRemovedEntity);

            findRemovedEntity = em.find(XMLUniO2ODummyEntity.class, 702);
            Assert.assertNull(findRemovedEntity);
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace(System.err);
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    /**
     * Test Logic: testScenario02
     *
     * <p>
     * JPA 2.0 Specifications Tested (per PFD2 spec, 11/11/2009):
     * <ul>
     * <li>3.2.6 Evicting an Entity Instance from the Persistence Context
     * <li>3.2.7 Detached Entities
     * <li>3.2.7.1 Merging Detached Entity State
     * <li>3.2.8 Managed Instances
     * </ul>
     *
     * <p>
     * Description: Verify created and updated entities can be detached and
     * merged:
     * <ol>
     * <li>Create a new entity, detach it, initialize it, merge it (which also
     * auto-persists it to the database).
     * <li>Verify the new merged entity was saved to the database. This
     * verification ensures <b>both</b> annotations and XML on these artifacts
     * are respected:
     * <ol>
     * <li>Embedded field access uni-directional one-to-one relationship.
     * <li>Embedded property access uni-directional one-to-one relationship
     * (mixing field/property access).
     * <li>Embedded field access owner of a bi-directional one-to-one
     * relationship.
     * <li>Embedded field access inverse of a bi-directional one-to-one
     * relationship.
     * <li>AssociationOverrides of embedded field access inverse of a
     * bi-directional one-to-one relationship.
     * <li>Embedded field access owner of uni-directional many-to-one
     * relationship.
     * <li>Embedded field access owner of bi-directional many-to-one
     * relationship.
     * <li>Embedded field access inverse of bi-directional one-to-many
     * relationship.
     * <li>Embedded field access owner of bi-directional many-to-many
     * relationship.
     * <li>Collection&lt;Embeddable.P/A uni-directional one-to-one owner&gt;
     * using ElementCollection, CollectionTable, and sorted by OrderColumn
     * (mixing field/property access).
     * <li>List&lt;Embeddable.F/A uni-directional one-to-one owner&gt; using
     * ElementCollection, CollectionTable, and sorted by OrderColumn.
     * <li>AssociationOverrides of List&lt;Embeddable.F/A uni-directional
     * one-to-one owner&gt; using ElementCollection, CollectionTable, and sorted
     * by OrderColumn.
     * <li>Non-generic Set&lt;Embeddable.F/A uni-directional one-to-one
     * owner&gt; using ElementCollection, CollectionTable, and sorted by
     * OrderColumn.
     * <li>AssociationOverrides of key of Map&lt;Integer, Embeddable.F/A
     * uni-directional one-to-one owner&gt; using ElementCollection,
     * CollectionTable, MapKeyColumn, and sorted by OrderColumn.
     * <li>AssociationOverrides of key and value of Map&lt;Embeddable.F/A
     * uni-directional one-to-one owner, Embeddable.F/A uni-directional
     * one-to-one owner&gt; using ElementCollection, CollectionTable, and sorted
     * by OrderColumn.
     * </ol>
     * <li>Detach the found entity, update it, and re-merge it (which also
     * auto-persists it to the database).
     * <li>Verify the updated merged entity was saved to the database, as
     * indicated above.
     * </ol>
     *
     * <p>
     * Much of detach/merge is oriented towards entities, and not embeddables.
     * Consequently, this method does not re-invent detach/merge testing as
     * already provided by <code>\suite\r70\base\jpaspec\entitymanager</code>
     * and <code>\suite\r70\base\jpaspec\relationships</code>.
     */
    @SuppressWarnings("deprecation")
    public void testEmbeddableRelationship03(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                             Object managedComponentObject) {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        // Process Test Properties
        final Map<String, Serializable> testProps = testExecCtx.getProperties();
        if (testProps != null) {
            for (String key : testProps.keySet()) {
                System.out.println("Test Property: " + key + " = " + testProps.get(key));
            }
        }

        // Execute Test Case
        // 1) Create a new entity, detach it, initialize it, merge it (which also auto-persists it to the database)
        try {
            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            int id = 3;

            JPAEmbeddableRelationshipEntity newEntity = new JPAEmbeddableRelationshipEntity();
            newEntity.setId(id);
            newEntity.setUniO2OOwnerFieldAccessEmbed(new UniO2OOwnerFieldAccessEmbed(new UniO2ODummyEntity(4)));
            newEntity.setUniO2OOwnerPropertyAccessEmbed(new UniO2OOwnerPropertyAccessEmbed(new UniO2ODummyEntity(5)));
            newEntity.setBiO2OOwnerEmbed(new BiO2OOwnerEmbed(null)); // Bi-directional child merged after parent.
            newEntity.setBiO2OInverseEmbed(new BiO2OInverseEmbed(null)); // Bi-directional child merged after parent.
            newEntity.setBiO2OOwnerAssociationOverridesEmbed(new BiO2OOwnerAssociationOverridesEmbed(null)); // Bi-directional child merged after parent.
            newEntity.setUniM2OOwnerEmbed(new UniM2OOwnerEmbed(new UniO2ODummyEntity(6)));
            newEntity.setBiM2OOwnerEmbed(new BiM2OOwnerEmbed(null)); // Bi-directional child merged after parent.
            newEntity.setBiO2MInverseEmbed(new BiO2MInverseEmbed(null)); // Bi-directional child merged after parent.
            newEntity.setBiM2MOwnerEmbed(new BiM2MOwnerEmbed(null)); // Bi-directional child merged after parent.
            newEntity.setCollectionUniO2OOwnerPropertyAccessEmbedOrderColumn(new HashSet<UniO2OOwnerPropertyAccessEmbed>());
            newEntity.getCollectionUniO2OOwnerPropertyAccessEmbedOrderColumn().add(new UniO2OOwnerPropertyAccessEmbed(new UniO2ODummyEntity(111)));
            newEntity.getCollectionUniO2OOwnerPropertyAccessEmbedOrderColumn().add(new UniO2OOwnerPropertyAccessEmbed(new UniO2ODummyEntity(110)));
            newEntity.setListUniO2OOwnerFieldAccessEmbedOrderColumn(new ArrayList<UniO2OOwnerFieldAccessEmbed>());
            newEntity.getListUniO2OOwnerFieldAccessEmbedOrderColumn().add(new UniO2OOwnerFieldAccessEmbed(new UniO2ODummyEntity(211)));
            newEntity.getListUniO2OOwnerFieldAccessEmbedOrderColumn().add(new UniO2OOwnerFieldAccessEmbed(new UniO2ODummyEntity(210)));
            newEntity.setListUniO2OOwnerFieldAccessEmbedAssociationOverridesOrderColumn(new ArrayList<UniO2OOwnerFieldAccessEmbed>());
            newEntity.getListUniO2OOwnerFieldAccessEmbedAssociationOverridesOrderColumn().add(new UniO2OOwnerFieldAccessEmbed(new UniO2ODummyEntity(311)));
            newEntity.getListUniO2OOwnerFieldAccessEmbedAssociationOverridesOrderColumn().add(new UniO2OOwnerFieldAccessEmbed(new UniO2ODummyEntity(310)));
            newEntity.setSetUniO2OOwnerFieldAccessEmbedOrderColumn(new HashSet<>());
            newEntity.getSetUniO2OOwnerFieldAccessEmbedOrderColumn().add(new UniO2OOwnerFieldAccessEmbed(new UniO2ODummyEntity(411)));
            newEntity.setMapKeyIntegerValueUniO2OOwnerFieldAccessEmbedOrderColumn(new HashMap<Integer, UniO2OOwnerFieldAccessEmbed>());
            newEntity.getMapKeyIntegerValueUniO2OOwnerFieldAccessEmbedOrderColumn().put(new Integer(2), new UniO2OOwnerFieldAccessEmbed(new UniO2ODummyEntity(512)));
            newEntity.getMapKeyIntegerValueUniO2OOwnerFieldAccessEmbedOrderColumn().put(new Integer(1), new UniO2OOwnerFieldAccessEmbed(new UniO2ODummyEntity(511)));
            newEntity.setMapKeyUniO2OOwnerFieldAccessEmbedValueUniO2OOwnerFieldAccessEmbedOrderColumn(new HashMap<UniO2OOwnerFieldAccessEmbed, UniO2OOwnerFieldAccessEmbed>());
            newEntity.getMapKeyUniO2OOwnerFieldAccessEmbedValueUniO2OOwnerFieldAccessEmbedOrderColumn()
                            .put(new UniO2OOwnerFieldAccessEmbed(new UniO2ODummyEntity(612)),
                                 new UniO2OOwnerFieldAccessEmbed(new UniO2ODummyEntity(712)));
            newEntity.getMapKeyUniO2OOwnerFieldAccessEmbedValueUniO2OOwnerFieldAccessEmbedOrderColumn()
                            .put(new UniO2OOwnerFieldAccessEmbed(new UniO2ODummyEntity(611)),
                                 new UniO2OOwnerFieldAccessEmbed(new UniO2ODummyEntity(711)));

            System.out.println("Beginning new transaction...");
            tj.beginTransaction();
            if (tj.isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                em.joinTransaction();
            }

            System.out.println("Performing merge(" + newEntity + ") operation");
            JPAEmbeddableRelationshipEntity premergedNewEntity = em.merge(newEntity);

            em.clear();

            // Update merged/managed entity
            premergedNewEntity.setBiO2OOwnerEmbed(new BiO2OOwnerEmbed(new BiO2OInverseEntity(2, premergedNewEntity)));
            premergedNewEntity.setBiO2OInverseEmbed(new BiO2OInverseEmbed(new BiO2OOwnerEntity(2, premergedNewEntity)));
            premergedNewEntity.setBiO2OOwnerAssociationOverridesEmbed(new BiO2OOwnerAssociationOverridesEmbed(new BiO2OInverseAssociationOverridesEntity(2, premergedNewEntity)));

            HashSet<JPAEmbeddableRelationshipEntity> me = new HashSet<JPAEmbeddableRelationshipEntity>();
            me.add(premergedNewEntity);
            premergedNewEntity.setBiM2OOwnerEmbed(new BiM2OOwnerEmbed(new BiO2MInverseEntity(22, me)));

            HashSet<BiM2OOwnerEntity> other = new HashSet<BiM2OOwnerEntity>();
            other.add(new BiM2OOwnerEntity(22, premergedNewEntity));
            premergedNewEntity.setBiO2MInverseEmbed(new BiO2MInverseEmbed(other));

            premergedNewEntity.setBiM2MOwnerEmbed(new BiM2MOwnerEmbed(new HashSet<BiM2MInverseEntity>()));
            premergedNewEntity.getBiM2MOwnerEmbed().getBiM2MInverseEntities().add(new BiM2MInverseEntity(5, me));
            premergedNewEntity.getBiM2MOwnerEmbed().getBiM2MInverseEntities().add(new BiM2MInverseEntity(6, me));

            System.out.println("Performing merge(" + premergedNewEntity + ") operation");
            JPAEmbeddableRelationshipEntity mergedNewEntity = em.merge(premergedNewEntity);

            // Perform content verifications
            Assert.assertFalse(em.contains(newEntity));
            Assert.assertFalse(em.contains(premergedNewEntity));
            Assert.assertTrue(em.contains(mergedNewEntity));
            Assert.assertFalse(newEntity == premergedNewEntity);
            Assert.assertFalse(newEntity == mergedNewEntity);
            Assert.assertFalse(premergedNewEntity == mergedNewEntity);
            Assert.assertTrue(em.contains(mergedNewEntity.getUniO2OOwnerFieldAccessEmbed().getUniO2ODummyEntity_FA()));
            Assert.assertTrue(em.contains(mergedNewEntity.getUniO2OOwnerPropertyAccessEmbed().getUniO2ODummyEntity_PA()));
            Assert.assertTrue(em.contains(mergedNewEntity.getBiO2OOwnerEmbed().getBiO2OInverseEntity()));
            Assert.assertTrue(em.contains(mergedNewEntity.getBiO2OInverseEmbed().getBiO2OOwnerEntity()));
            Assert.assertTrue(em.contains(mergedNewEntity.getBiO2OOwnerAssociationOverridesEmbed().getBiO2OInverseAssociationOverridesEntity()));
            Assert.assertTrue(em.contains(mergedNewEntity.getUniM2OOwnerEmbed().getUniO2MDummyEntity()));
            Assert.assertTrue(em.contains(mergedNewEntity.getBiM2OOwnerEmbed().getBiO2MInverseEntity()));

            Iterator<?> i = mergedNewEntity.getBiO2MInverseEmbed().getBiM2OOwnerEntities().iterator();
            Assert.assertTrue(em.contains(i.next()));

            i = mergedNewEntity.getBiM2MOwnerEmbed().getBiM2MInverseEntities().iterator();
            Assert.assertTrue(em.contains(i.next()));
            Assert.assertTrue(em.contains(i.next()));

            i = mergedNewEntity.getCollectionUniO2OOwnerPropertyAccessEmbedOrderColumn().iterator();
            Assert.assertTrue(em.contains(i.next()));
            Assert.assertTrue(em.contains(i.next()));

            i = mergedNewEntity.getListUniO2OOwnerFieldAccessEmbedOrderColumn().iterator();
            Assert.assertTrue(em.contains(i.next()));
            Assert.assertTrue(em.contains(i.next()));

            i = mergedNewEntity.getListUniO2OOwnerFieldAccessEmbedAssociationOverridesOrderColumn().iterator();
            Assert.assertTrue(em.contains(i.next()));
            Assert.assertTrue(em.contains(i.next()));

            i = mergedNewEntity.getSetUniO2OOwnerFieldAccessEmbedOrderColumn().iterator();
            Assert.assertTrue(em.contains(i.next()));

            i = mergedNewEntity.getMapKeyIntegerValueUniO2OOwnerFieldAccessEmbedOrderColumn().values().iterator();
            Assert.assertTrue(em.contains(i.next()));
            Assert.assertTrue(em.contains(i.next()));

            i = mergedNewEntity.getMapKeyUniO2OOwnerFieldAccessEmbedValueUniO2OOwnerFieldAccessEmbedOrderColumn().keySet().iterator();
            Assert.assertTrue(em.contains(i.next()));
            Assert.assertTrue(em.contains(i.next()));

            i = mergedNewEntity.getMapKeyUniO2OOwnerFieldAccessEmbedValueUniO2OOwnerFieldAccessEmbedOrderColumn().values().iterator();
            Assert.assertTrue(em.contains(i.next()));
            Assert.assertTrue(em.contains(i.next()));

            System.out.println("Committing transaction...");
            if (tj.isTransactionActive()) {
                tj.commitTransaction();
            }

            em.clear();

            System.out.println("Beginning new transaction...");
            tj.beginTransaction();
            if (tj.isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                em.joinTransaction();
            }

            // Verify the entity was saved to the database
            System.out.println("Performing find(" + JPAEmbeddableRelationshipEntity.class + ", " + id + ") operation");
            JPAEmbeddableRelationshipEntity findEntity = em.find(JPAEmbeddableRelationshipEntity.class, id);
            Assert.assertNotNull("find(" + JPAEmbeddableRelationshipEntity.class + ", " + id + ") did not return an entity.", findEntity);

            // Perform content verifications
            Assert.assertTrue(newEntity != findEntity);
            Assert.assertTrue(em.contains(findEntity));
            Assert.assertEquals(newEntity.getId(), findEntity.getId());
            Assert.assertEquals(newEntity.getUniO2OOwnerFieldAccessEmbed(), findEntity.getUniO2OOwnerFieldAccessEmbed());
            Assert.assertEquals(newEntity.getUniO2OOwnerPropertyAccessEmbed(), findEntity.getUniO2OOwnerPropertyAccessEmbed());
            Assert.assertEquals(premergedNewEntity.getBiO2OOwnerEmbed(), findEntity.getBiO2OOwnerEmbed());
            Assert.assertEquals(premergedNewEntity.getBiO2OInverseEmbed(), findEntity.getBiO2OInverseEmbed());
            Assert.assertEquals(premergedNewEntity.getBiO2OOwnerAssociationOverridesEmbed(), findEntity.getBiO2OOwnerAssociationOverridesEmbed());
            Assert.assertEquals(newEntity.getUniM2OOwnerEmbed(), findEntity.getUniM2OOwnerEmbed());
            Assert.assertEquals(premergedNewEntity.getBiM2OOwnerEmbed(), findEntity.getBiM2OOwnerEmbed());
            Assert.assertEquals(premergedNewEntity.getBiO2MInverseEmbed(), findEntity.getBiO2MInverseEmbed());
            Assert.assertEquals(premergedNewEntity.getBiM2MOwnerEmbed(), findEntity.getBiM2MOwnerEmbed());
            Assert.assertEquals(new HashSet<>(newEntity.getCollectionUniO2OOwnerPropertyAccessEmbedOrderColumn()),
                                new HashSet<>(findEntity.getCollectionUniO2OOwnerPropertyAccessEmbedOrderColumn()));
            Assert.assertEquals(newEntity.getListUniO2OOwnerFieldAccessEmbedOrderColumn(), findEntity.getListUniO2OOwnerFieldAccessEmbedOrderColumn());
            Assert.assertEquals(newEntity.getListUniO2OOwnerFieldAccessEmbedAssociationOverridesOrderColumn(),
                                findEntity.getListUniO2OOwnerFieldAccessEmbedAssociationOverridesOrderColumn());
            Assert.assertEquals(newEntity.getSetUniO2OOwnerFieldAccessEmbedOrderColumn(), findEntity.getSetUniO2OOwnerFieldAccessEmbedOrderColumn());
            Assert.assertEquals(newEntity.getMapKeyIntegerValueUniO2OOwnerFieldAccessEmbedOrderColumn(), findEntity.getMapKeyIntegerValueUniO2OOwnerFieldAccessEmbedOrderColumn());
            Assert.assertEquals(newEntity.getMapKeyUniO2OOwnerFieldAccessEmbedValueUniO2OOwnerFieldAccessEmbedOrderColumn(),
                                findEntity.getMapKeyUniO2OOwnerFieldAccessEmbedValueUniO2OOwnerFieldAccessEmbedOrderColumn());

            // 3) Detach the found entity, update it, and re-merge it (which also auto-persists it to the database)
            BiO2OOwnerEntity removeOldOwner = findEntity.getBiO2OInverseEmbed().getBiO2OOwnerEntity();

            em.clear();

            System.out.println("Performing em.contains(" + findEntity + ") operation");
            Assert.assertFalse("Entity " + findEntity + " is managed but should be detached", em.contains(findEntity));

            // Update the entity
            // Don't touch ID.
            findEntity.setUniO2OOwnerFieldAccessEmbed(new UniO2OOwnerFieldAccessEmbed(new UniO2ODummyEntity(11)));
            findEntity.getUniO2OOwnerPropertyAccessEmbed().setUniO2ODummyEntity_PA(new UniO2ODummyEntity(12));
            // biO2OOwnerEmbed: Bi-directional child merged after parent.
            // biO2OInverseEmbed: Bi-directional child merged after parent.
            // biO2OOwnerAssociationOverridesEmbed: Bi-directional child merged after parent.
            findEntity.getUniM2OOwnerEmbed().setUniO2MDummyEntity(null);
            // biM2OOwnerEmbed: Bi-directional child merged after parent.
            // biO2MInverseEmbed(): Bi-directional child merged after parent.
            // biM2MOwnerEmbed: Bi-directional child merged after parent.
            findEntity.getCollectionUniO2OOwnerPropertyAccessEmbedOrderColumn().add(new UniO2OOwnerPropertyAccessEmbed(new UniO2ODummyEntity(102)));
            findEntity.getListUniO2OOwnerFieldAccessEmbedOrderColumn().add(new UniO2OOwnerFieldAccessEmbed(new UniO2ODummyEntity(202)));
            findEntity.getListUniO2OOwnerFieldAccessEmbedAssociationOverridesOrderColumn().add(new UniO2OOwnerFieldAccessEmbed(new UniO2ODummyEntity(302)));
            findEntity.getSetUniO2OOwnerFieldAccessEmbedOrderColumn().add(new UniO2OOwnerFieldAccessEmbed(new UniO2ODummyEntity(401))); // Dup.
            findEntity.getSetUniO2OOwnerFieldAccessEmbedOrderColumn().add(new UniO2OOwnerFieldAccessEmbed(new UniO2ODummyEntity(400)));
            findEntity.getMapKeyIntegerValueUniO2OOwnerFieldAccessEmbedOrderColumn().put(new Integer(0), new UniO2OOwnerFieldAccessEmbed(new UniO2ODummyEntity(500)));
            findEntity.getMapKeyUniO2OOwnerFieldAccessEmbedValueUniO2OOwnerFieldAccessEmbedOrderColumn()
                            .put(new UniO2OOwnerFieldAccessEmbed(new UniO2ODummyEntity(600)),
                                 new UniO2OOwnerFieldAccessEmbed(new UniO2ODummyEntity(700)));

            System.out.println("Performing merge(" + findEntity + ") operation");
            JPAEmbeddableRelationshipEntity preMergedFindEntity = em.merge(findEntity);

            // Detach the merged entity
            em.clear();

            // Update the entity
            preMergedFindEntity.setBiO2OOwnerEmbed(new BiO2OOwnerEmbed(new BiO2OInverseEntity(22, preMergedFindEntity)));
            preMergedFindEntity.setBiO2OInverseEmbed(new BiO2OInverseEmbed(new BiO2OOwnerEntity(22, preMergedFindEntity)));
            preMergedFindEntity
                            .setBiO2OOwnerAssociationOverridesEmbed(new BiO2OOwnerAssociationOverridesEmbed(new BiO2OInverseAssociationOverridesEntity(22, preMergedFindEntity)));

            me.clear();
            me.add(preMergedFindEntity);
            preMergedFindEntity.getBiM2OOwnerEmbed().setBiO2MInverseEntity(new BiO2MInverseEntity(12, me));

            other = preMergedFindEntity.getBiO2MInverseEmbed().getBiM2OOwnerEntities();
            other.add(new BiM2OOwnerEntity(12, preMergedFindEntity));
            preMergedFindEntity.getBiO2MInverseEmbed().setBiM2OOwnerEntities(other);
            preMergedFindEntity.getBiM2MOwnerEmbed().getBiM2MInverseEntities().add(new BiM2MInverseEntity(3, me));

            System.out.println("Performing merge(" + preMergedFindEntity + ") operation");
            JPAEmbeddableRelationshipEntity mergedFindEntity = em.merge(preMergedFindEntity);

            // Perform content verifications
            Assert.assertFalse(em.contains(findEntity));
            Assert.assertFalse(em.contains(preMergedFindEntity));
            Assert.assertTrue(em.contains(mergedFindEntity));
            Assert.assertFalse(preMergedFindEntity == findEntity);
            Assert.assertFalse(preMergedFindEntity == mergedFindEntity);
            Assert.assertFalse(findEntity == mergedFindEntity);
            Assert.assertTrue(em.contains(mergedFindEntity.getUniO2OOwnerFieldAccessEmbed().getUniO2ODummyEntity_FA()));
            Assert.assertTrue(em.contains(mergedFindEntity.getUniO2OOwnerPropertyAccessEmbed().getUniO2ODummyEntity_PA()));
            Assert.assertTrue(em.contains(mergedFindEntity.getBiO2OOwnerEmbed().getBiO2OInverseEntity()));
            Assert.assertTrue(em.contains(mergedFindEntity.getBiO2OInverseEmbed().getBiO2OOwnerEntity()));
            Assert.assertTrue(em.contains(mergedFindEntity.getBiO2OOwnerAssociationOverridesEmbed().getBiO2OInverseAssociationOverridesEntity()));
            Assert.assertFalse(em.contains(mergedFindEntity.getUniM2OOwnerEmbed().getUniO2MDummyEntity()));
            Assert.assertTrue(em.contains(mergedFindEntity.getBiM2OOwnerEmbed().getBiO2MInverseEntity()));

            i = mergedFindEntity.getBiO2MInverseEmbed().getBiM2OOwnerEntities().iterator();
            Assert.assertTrue(em.contains(i.next()));
            Assert.assertTrue(em.contains(i.next()));

            i = mergedFindEntity.getBiM2MOwnerEmbed().getBiM2MInverseEntities().iterator();
            Assert.assertTrue(em.contains(i.next()));
            Assert.assertTrue(em.contains(i.next()));
            Assert.assertTrue(em.contains(i.next()));

            i = mergedFindEntity.getCollectionUniO2OOwnerPropertyAccessEmbedOrderColumn().iterator();
            Assert.assertTrue(em.contains(i.next()));
            Assert.assertTrue(em.contains(i.next()));
            Assert.assertTrue(em.contains(i.next()));

            i = mergedFindEntity.getListUniO2OOwnerFieldAccessEmbedOrderColumn().iterator();
            Assert.assertTrue(em.contains(i.next()));
            Assert.assertTrue(em.contains(i.next()));
            Assert.assertTrue(em.contains(i.next()));

            i = mergedFindEntity.getListUniO2OOwnerFieldAccessEmbedAssociationOverridesOrderColumn().iterator();
            Assert.assertTrue(em.contains(i.next()));
            Assert.assertTrue(em.contains(i.next()));
            Assert.assertTrue(em.contains(i.next()));

            i = mergedFindEntity.getSetUniO2OOwnerFieldAccessEmbedOrderColumn().iterator();
            Assert.assertTrue(em.contains(i.next()));
            Assert.assertTrue(em.contains(i.next()));

            i = mergedFindEntity.getMapKeyIntegerValueUniO2OOwnerFieldAccessEmbedOrderColumn().values().iterator();
            Assert.assertTrue(em.contains(i.next()));
            Assert.assertTrue(em.contains(i.next()));
            Assert.assertTrue(em.contains(i.next()));

            i = mergedFindEntity.getMapKeyUniO2OOwnerFieldAccessEmbedValueUniO2OOwnerFieldAccessEmbedOrderColumn().keySet().iterator();
            Assert.assertTrue(em.contains(i.next()));
            Assert.assertTrue(em.contains(i.next()));
            Assert.assertTrue(em.contains(i.next()));

            i = mergedFindEntity.getMapKeyUniO2OOwnerFieldAccessEmbedValueUniO2OOwnerFieldAccessEmbedOrderColumn().values().iterator();
            Assert.assertTrue(em.contains(i.next()));
            Assert.assertTrue(em.contains(i.next()));
            Assert.assertTrue(em.contains(i.next()));

            System.out.println("Performing merge(" + removeOldOwner + ") operation");
            BiO2OOwnerEntity mergedRemoveOldOwner = em.merge(removeOldOwner);

            System.out.println("Performing remove(" + mergedRemoveOldOwner + ") operation");
            em.remove(mergedRemoveOldOwner);

            System.out.println("Committing transaction...");
            if (tj.isTransactionActive()) {
                tj.commitTransaction();
            }

            em.clear();

            System.out.println("Beginning new transaction...");
            tj.beginTransaction();
            if (tj.isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                em.joinTransaction();
            }

            // Verify the entity update was saved to the database
            System.out.println("Performing find(" + JPAEmbeddableRelationshipEntity.class + ", " + id + ") operation");
            JPAEmbeddableRelationshipEntity updatedFindEntity = em.find(JPAEmbeddableRelationshipEntity.class, id);
            Assert.assertNotNull("find(" + JPAEmbeddableRelationshipEntity.class + ", " + id + ") did not return an entity.", updatedFindEntity);

            // Perform content verifications
            Assert.assertTrue(newEntity != updatedFindEntity);
            Assert.assertTrue(findEntity != updatedFindEntity);
            Assert.assertTrue(em.contains(updatedFindEntity));
            Assert.assertEquals(updatedFindEntity.getId(), findEntity.getId());
            Assert.assertEquals(findEntity.getUniO2OOwnerFieldAccessEmbed(), updatedFindEntity.getUniO2OOwnerFieldAccessEmbed());
            Assert.assertEquals(findEntity.getUniO2OOwnerPropertyAccessEmbed(), updatedFindEntity.getUniO2OOwnerPropertyAccessEmbed());
            Assert.assertEquals(preMergedFindEntity.getBiO2OOwnerEmbed(), updatedFindEntity.getBiO2OOwnerEmbed());
            Assert.assertEquals(preMergedFindEntity.getBiO2OInverseEmbed(), updatedFindEntity.getBiO2OInverseEmbed());
            Assert.assertEquals(preMergedFindEntity.getBiO2OOwnerAssociationOverridesEmbed(), updatedFindEntity.getBiO2OOwnerAssociationOverridesEmbed());
            Assert.assertEquals(findEntity.getUniM2OOwnerEmbed(), updatedFindEntity.getUniM2OOwnerEmbed());
            Assert.assertEquals(preMergedFindEntity.getBiM2OOwnerEmbed(), updatedFindEntity.getBiM2OOwnerEmbed());
            Assert.assertEquals(preMergedFindEntity.getBiO2MInverseEmbed(), updatedFindEntity.getBiO2MInverseEmbed());
            Assert.assertEquals(preMergedFindEntity.getBiM2MOwnerEmbed(), updatedFindEntity.getBiM2MOwnerEmbed());
            Assert.assertEquals(new HashSet<>(findEntity.getCollectionUniO2OOwnerPropertyAccessEmbedOrderColumn()),
                                new HashSet<>(updatedFindEntity.getCollectionUniO2OOwnerPropertyAccessEmbedOrderColumn()));
            Assert.assertEquals(findEntity.getListUniO2OOwnerFieldAccessEmbedOrderColumn(), updatedFindEntity.getListUniO2OOwnerFieldAccessEmbedOrderColumn());
            Assert.assertEquals(findEntity.getListUniO2OOwnerFieldAccessEmbedAssociationOverridesOrderColumn(),
                                updatedFindEntity.getListUniO2OOwnerFieldAccessEmbedAssociationOverridesOrderColumn());
            Assert.assertEquals(findEntity.getSetUniO2OOwnerFieldAccessEmbedOrderColumn(), updatedFindEntity.getSetUniO2OOwnerFieldAccessEmbedOrderColumn());
            Assert.assertEquals(findEntity.getMapKeyIntegerValueUniO2OOwnerFieldAccessEmbedOrderColumn(),
                                updatedFindEntity.getMapKeyIntegerValueUniO2OOwnerFieldAccessEmbedOrderColumn());
            Assert.assertEquals(findEntity.getMapKeyUniO2OOwnerFieldAccessEmbedValueUniO2OOwnerFieldAccessEmbedOrderColumn(),
                                updatedFindEntity.getMapKeyUniO2OOwnerFieldAccessEmbedValueUniO2OOwnerFieldAccessEmbedOrderColumn());

            // Delete the entity from the database
            System.out.println("Performing remove(" + updatedFindEntity + ") operation");
            em.remove(updatedFindEntity);

            System.out.println("Committing transaction...");
            if (tj.isTransactionActive()) {
                tj.commitTransaction();
            }

            em.clear();

            // Verify the entity remove was successful
            System.out.println("Performing find(" + JPAEmbeddableRelationshipEntity.class + ", " + id + ") operation");
            Object findRemovedEntity = em.find(JPAEmbeddableRelationshipEntity.class, id);
            Assert.assertNull("find(" + JPAEmbeddableRelationshipEntity.class + ", " + id + ") returned an entity.", findRemovedEntity);
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace(System.err);
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    @SuppressWarnings("deprecation")
    public void testEmbeddableRelationship04(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                             Object managedComponentObject) {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        // Process Test Properties
        final Map<String, Serializable> testProps = testExecCtx.getProperties();
        if (testProps != null) {
            for (String key : testProps.keySet()) {
                System.out.println("Test Property: " + key + " = " + testProps.get(key));
            }
        }

        // Execute Test Case
        // 1) Create a new entity, detach it, initialize it, merge it (which also auto-persists it to the database)
        try {
            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            int id = 4;

            XMLJPAEmbeddableRelationshipEntity newEntity = new XMLJPAEmbeddableRelationshipEntity();
            newEntity.setId(id);
            newEntity.setUniO2OOwnerFieldAccessEmbed(new XMLUniO2OOwnerFieldAccessEmbed(new XMLUniO2ODummyEntity(4)));
            newEntity.setUniO2OOwnerPropertyAccessEmbed(new XMLUniO2OOwnerPropertyAccessEmbed(new XMLUniO2ODummyEntity(5)));
            newEntity.setBiO2OOwnerEmbed(new XMLBiO2OOwnerEmbed(null)); // Bi-directional child merged after parent.
            newEntity.setBiO2OInverseEmbed(new XMLBiO2OInverseEmbed(null)); // Bi-directional child merged after parent.
            newEntity.setBiO2OOwnerAssociationOverridesEmbed(new XMLBiO2OOwnerAssociationOverridesEmbed(null)); // Bi-directional child merged after parent.
            newEntity.setUniM2OOwnerEmbed(new XMLUniM2OOwnerEmbed(new XMLUniO2ODummyEntity(6)));
            newEntity.setBiM2OOwnerEmbed(new XMLBiM2OOwnerEmbed(null)); // Bi-directional child merged after parent.
            newEntity.setBiO2MInverseEmbed(new XMLBiO2MInverseEmbed(null)); // Bi-directional child merged after parent.
            newEntity.setBiM2MOwnerEmbed(new XMLBiM2MOwnerEmbed(null)); // Bi-directional child merged after parent.
            newEntity.setCollectionUniO2OOwnerPropertyAccessEmbedOrderColumn(new HashSet<XMLUniO2OOwnerPropertyAccessEmbed>());
            newEntity.getCollectionUniO2OOwnerPropertyAccessEmbedOrderColumn().add(new XMLUniO2OOwnerPropertyAccessEmbed(new XMLUniO2ODummyEntity(111)));
            newEntity.getCollectionUniO2OOwnerPropertyAccessEmbedOrderColumn().add(new XMLUniO2OOwnerPropertyAccessEmbed(new XMLUniO2ODummyEntity(110)));
            newEntity.setListUniO2OOwnerFieldAccessEmbedOrderColumn(new ArrayList<XMLUniO2OOwnerFieldAccessEmbed>());
            newEntity.getListUniO2OOwnerFieldAccessEmbedOrderColumn().add(new XMLUniO2OOwnerFieldAccessEmbed(new XMLUniO2ODummyEntity(211)));
            newEntity.getListUniO2OOwnerFieldAccessEmbedOrderColumn().add(new XMLUniO2OOwnerFieldAccessEmbed(new XMLUniO2ODummyEntity(210)));
            newEntity.setListUniO2OOwnerFieldAccessEmbedAssociationOverridesOrderColumn(new ArrayList<XMLUniO2OOwnerFieldAccessEmbed>());
            newEntity.getListUniO2OOwnerFieldAccessEmbedAssociationOverridesOrderColumn().add(new XMLUniO2OOwnerFieldAccessEmbed(new XMLUniO2ODummyEntity(311)));
            newEntity.getListUniO2OOwnerFieldAccessEmbedAssociationOverridesOrderColumn().add(new XMLUniO2OOwnerFieldAccessEmbed(new XMLUniO2ODummyEntity(310)));
            newEntity.setSetUniO2OOwnerFieldAccessEmbedOrderColumn(new HashSet<XMLUniO2OOwnerFieldAccessEmbed>());
            newEntity.getSetUniO2OOwnerFieldAccessEmbedOrderColumn().add(new XMLUniO2OOwnerFieldAccessEmbed(new XMLUniO2ODummyEntity(411)));
            newEntity.setMapKeyIntegerValueUniO2OOwnerFieldAccessEmbedOrderColumn(new HashMap<Integer, XMLUniO2OOwnerFieldAccessEmbed>());
            newEntity.getMapKeyIntegerValueUniO2OOwnerFieldAccessEmbedOrderColumn().put(new Integer(2), new XMLUniO2OOwnerFieldAccessEmbed(new XMLUniO2ODummyEntity(512)));
            newEntity.getMapKeyIntegerValueUniO2OOwnerFieldAccessEmbedOrderColumn().put(new Integer(1), new XMLUniO2OOwnerFieldAccessEmbed(new XMLUniO2ODummyEntity(511)));
            newEntity.setMapKeyUniO2OOwnerFieldAccessEmbedValueUniO2OOwnerFieldAccessEmbedOrderColumn(new HashMap<XMLUniO2OOwnerFieldAccessEmbed, XMLUniO2OOwnerFieldAccessEmbed>());
            newEntity.getMapKeyUniO2OOwnerFieldAccessEmbedValueUniO2OOwnerFieldAccessEmbedOrderColumn()
                            .put(new XMLUniO2OOwnerFieldAccessEmbed(new XMLUniO2ODummyEntity(612)),
                                 new XMLUniO2OOwnerFieldAccessEmbed(new XMLUniO2ODummyEntity(712)));
            newEntity.getMapKeyUniO2OOwnerFieldAccessEmbedValueUniO2OOwnerFieldAccessEmbedOrderColumn()
                            .put(new XMLUniO2OOwnerFieldAccessEmbed(new XMLUniO2ODummyEntity(611)),
                                 new XMLUniO2OOwnerFieldAccessEmbed(new XMLUniO2ODummyEntity(711)));

            System.out.println("Beginning new transaction...");
            tj.beginTransaction();
            if (tj.isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                em.joinTransaction();
            }

            System.out.println("Performing merge(" + newEntity + ") operation");
            XMLJPAEmbeddableRelationshipEntity premergedNewEntity = em.merge(newEntity);

            em.clear();

            // Update merged/managed entity
            premergedNewEntity.setBiO2OOwnerEmbed(new XMLBiO2OOwnerEmbed(new XMLBiO2OInverseEntity(2, premergedNewEntity)));

            premergedNewEntity.setBiO2OInverseEmbed(new XMLBiO2OInverseEmbed(new XMLBiO2OOwnerEntity(2, premergedNewEntity)));

            premergedNewEntity
                            .setBiO2OOwnerAssociationOverridesEmbed(new XMLBiO2OOwnerAssociationOverridesEmbed(new XMLBiO2OInverseAssociationOverridesEntity(2, premergedNewEntity)));

            HashSet<XMLJPAEmbeddableRelationshipEntity> me = new HashSet<XMLJPAEmbeddableRelationshipEntity>();
            me.add(premergedNewEntity);
            premergedNewEntity.setBiM2OOwnerEmbed(new XMLBiM2OOwnerEmbed(new XMLBiO2MInverseEntity(22, me)));

            HashSet<XMLBiM2OOwnerEntity> other = new HashSet<XMLBiM2OOwnerEntity>();
            other.add(new XMLBiM2OOwnerEntity(22, premergedNewEntity));
            premergedNewEntity.setBiO2MInverseEmbed(new XMLBiO2MInverseEmbed(other));

            premergedNewEntity.setBiM2MOwnerEmbed(new XMLBiM2MOwnerEmbed(new HashSet<XMLBiM2MInverseEntity>()));
            premergedNewEntity.getBiM2MOwnerEmbed().getBiM2MInverseEntities().add(new XMLBiM2MInverseEntity(5, me));
            premergedNewEntity.getBiM2MOwnerEmbed().getBiM2MInverseEntities().add(new XMLBiM2MInverseEntity(6, me));

            System.out.println("Performing merge(" + premergedNewEntity + ") operation");
            XMLJPAEmbeddableRelationshipEntity mergedNewEntity = em.merge(premergedNewEntity);

            // Perform content verifications
            Assert.assertFalse(em.contains(newEntity));
            Assert.assertFalse(em.contains(premergedNewEntity));
            Assert.assertTrue(em.contains(mergedNewEntity));
            Assert.assertFalse(newEntity == premergedNewEntity);
            Assert.assertFalse(newEntity == mergedNewEntity);
            Assert.assertFalse(premergedNewEntity == mergedNewEntity);
            Assert.assertTrue(em.contains(mergedNewEntity.getUniO2OOwnerFieldAccessEmbed().getUniO2ODummyEntity_FA()));
            Assert.assertTrue(em.contains(mergedNewEntity.getUniO2OOwnerPropertyAccessEmbed().getUniO2ODummyEntity_PA()));
            Assert.assertTrue(em.contains(mergedNewEntity.getBiO2OOwnerEmbed().getBiO2OInverseEntity()));
            Assert.assertTrue(em.contains(mergedNewEntity.getBiO2OInverseEmbed().getBiO2OOwnerEntity()));
            Assert.assertTrue(em.contains(mergedNewEntity.getBiO2OOwnerAssociationOverridesEmbed().getBiO2OInverseAssociationOverridesEntity()));
            Assert.assertTrue(em.contains(mergedNewEntity.getUniM2OOwnerEmbed().getUniO2MDummyEntity()));
            Assert.assertTrue(em.contains(mergedNewEntity.getBiM2OOwnerEmbed().getBiO2MInverseEntity()));
            Iterator<?> i = mergedNewEntity.getBiO2MInverseEmbed().getBiM2OOwnerEntities().iterator();
            Assert.assertTrue(em.contains(i.next()));

            i = mergedNewEntity.getBiM2MOwnerEmbed().getBiM2MInverseEntities().iterator();
            Assert.assertTrue(em.contains(i.next()));
            Assert.assertTrue(em.contains(i.next()));

            i = mergedNewEntity.getCollectionUniO2OOwnerPropertyAccessEmbedOrderColumn().iterator();
            Assert.assertTrue(em.contains(i.next()));
            Assert.assertTrue(em.contains(i.next()));

            i = mergedNewEntity.getListUniO2OOwnerFieldAccessEmbedOrderColumn().iterator();
            Assert.assertTrue(em.contains(i.next()));
            Assert.assertTrue(em.contains(i.next()));

            i = mergedNewEntity.getListUniO2OOwnerFieldAccessEmbedAssociationOverridesOrderColumn().iterator();
            Assert.assertTrue(em.contains(i.next()));
            Assert.assertTrue(em.contains(i.next()));

            i = mergedNewEntity.getSetUniO2OOwnerFieldAccessEmbedOrderColumn().iterator();
            Assert.assertTrue(em.contains(i.next()));

            i = mergedNewEntity.getMapKeyIntegerValueUniO2OOwnerFieldAccessEmbedOrderColumn().values().iterator();
            Assert.assertTrue(em.contains(i.next()));
            Assert.assertTrue(em.contains(i.next()));

            i = mergedNewEntity.getMapKeyUniO2OOwnerFieldAccessEmbedValueUniO2OOwnerFieldAccessEmbedOrderColumn().keySet().iterator();
            Assert.assertTrue(em.contains(i.next()));
            Assert.assertTrue(em.contains(i.next()));

            i = mergedNewEntity.getMapKeyUniO2OOwnerFieldAccessEmbedValueUniO2OOwnerFieldAccessEmbedOrderColumn().values().iterator();
            Assert.assertTrue(em.contains(i.next()));
            Assert.assertTrue(em.contains(i.next()));

            System.out.println("Committing transaction...");
            if (tj.isTransactionActive()) {
                tj.commitTransaction();
            }

            // 2) Verify the entity was saved to the database

            em.clear();

            // Begin a new transaction, to ensure the entity returned by find is
            // managed by the persistence context in all environments, including
            // CM-TS.

            System.out.println("Beginning new transaction...");
            tj.beginTransaction();
            if (tj.isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                em.joinTransaction();
            }

            // Verify the entity was saved to the database
            System.out.println("Performing find(" + XMLJPAEmbeddableRelationshipEntity.class + ", " + id + ") operation");
            XMLJPAEmbeddableRelationshipEntity findEntity = em.find(XMLJPAEmbeddableRelationshipEntity.class, id);
            Assert.assertNotNull("find(" + XMLJPAEmbeddableRelationshipEntity.class + ", " + id + ") did not return an entity.", findEntity);

            // Perform content verifications
            Assert.assertTrue(newEntity != findEntity);
            Assert.assertTrue(em.contains(findEntity));
            Assert.assertEquals(newEntity.getId(), findEntity.getId());
            Assert.assertEquals(newEntity.getUniO2OOwnerFieldAccessEmbed(), findEntity.getUniO2OOwnerFieldAccessEmbed());
            Assert.assertEquals(newEntity.getUniO2OOwnerPropertyAccessEmbed(), findEntity.getUniO2OOwnerPropertyAccessEmbed());
            Assert.assertEquals(premergedNewEntity.getBiO2OOwnerEmbed(), findEntity.getBiO2OOwnerEmbed());
            Assert.assertEquals(premergedNewEntity.getBiO2OInverseEmbed(), findEntity.getBiO2OInverseEmbed());
            Assert.assertEquals(premergedNewEntity.getBiO2OOwnerAssociationOverridesEmbed(), findEntity.getBiO2OOwnerAssociationOverridesEmbed());
            Assert.assertEquals(newEntity.getUniM2OOwnerEmbed(), findEntity.getUniM2OOwnerEmbed());
            Assert.assertEquals(premergedNewEntity.getBiM2OOwnerEmbed(), findEntity.getBiM2OOwnerEmbed());
            Assert.assertEquals(premergedNewEntity.getBiO2MInverseEmbed(), findEntity.getBiO2MInverseEmbed());
            Assert.assertEquals(premergedNewEntity.getBiM2MOwnerEmbed(), findEntity.getBiM2MOwnerEmbed());
            Assert.assertEquals(new HashSet<>(newEntity.getCollectionUniO2OOwnerPropertyAccessEmbedOrderColumn()),
                                new HashSet<>(findEntity.getCollectionUniO2OOwnerPropertyAccessEmbedOrderColumn()));
            Assert.assertEquals(newEntity.getListUniO2OOwnerFieldAccessEmbedOrderColumn(), findEntity.getListUniO2OOwnerFieldAccessEmbedOrderColumn());
            Assert.assertEquals(newEntity.getListUniO2OOwnerFieldAccessEmbedAssociationOverridesOrderColumn(),
                                findEntity.getListUniO2OOwnerFieldAccessEmbedAssociationOverridesOrderColumn());
            Assert.assertEquals(newEntity.getSetUniO2OOwnerFieldAccessEmbedOrderColumn(), findEntity.getSetUniO2OOwnerFieldAccessEmbedOrderColumn());
            Assert.assertEquals(newEntity.getMapKeyIntegerValueUniO2OOwnerFieldAccessEmbedOrderColumn(), findEntity.getMapKeyIntegerValueUniO2OOwnerFieldAccessEmbedOrderColumn());
            Assert.assertEquals(newEntity.getMapKeyUniO2OOwnerFieldAccessEmbedValueUniO2OOwnerFieldAccessEmbedOrderColumn(),
                                findEntity.getMapKeyUniO2OOwnerFieldAccessEmbedValueUniO2OOwnerFieldAccessEmbedOrderColumn());

            // 3) Detach the found entity, update it, and re-merge it (which also auto-persists it to the database)
            XMLBiO2OOwnerEntity removeOldOwner = findEntity.getBiO2OInverseEmbed().getBiO2OOwnerEntity();

            em.clear();

            System.out.println("Performing em.contains(" + findEntity + ") operation");
            Assert.assertFalse("Entity " + findEntity + " is managed but should be detached", em.contains(findEntity));

            // Update the entity
            // Don't touch ID.
            findEntity.setUniO2OOwnerFieldAccessEmbed(new XMLUniO2OOwnerFieldAccessEmbed(new XMLUniO2ODummyEntity(11)));
            findEntity.getUniO2OOwnerPropertyAccessEmbed().setUniO2ODummyEntity_PA(new XMLUniO2ODummyEntity(12));
            // biO2OOwnerEmbed: Bi-directional child merged after parent.
            // biO2OInverseEmbed: Bi-directional child merged after parent.
            // biO2OOwnerAssociationOverridesEmbed: Bi-directional child merged after parent.
            findEntity.getUniM2OOwnerEmbed().setUniO2MDummyEntity(null);
            // biM2OOwnerEmbed: Bi-directional child merged after parent.
            // biO2MInverseEmbed(): Bi-directional child merged after parent.
            // biM2MOwnerEmbed: Bi-directional child merged after parent.
            findEntity.getCollectionUniO2OOwnerPropertyAccessEmbedOrderColumn().add(new XMLUniO2OOwnerPropertyAccessEmbed(new XMLUniO2ODummyEntity(102)));
            findEntity.getListUniO2OOwnerFieldAccessEmbedOrderColumn().add(new XMLUniO2OOwnerFieldAccessEmbed(new XMLUniO2ODummyEntity(202)));
            findEntity.getListUniO2OOwnerFieldAccessEmbedAssociationOverridesOrderColumn().add(new XMLUniO2OOwnerFieldAccessEmbed(new XMLUniO2ODummyEntity(302)));
            findEntity.getSetUniO2OOwnerFieldAccessEmbedOrderColumn().add(new XMLUniO2OOwnerFieldAccessEmbed(new XMLUniO2ODummyEntity(401))); // Dup.
            findEntity.getSetUniO2OOwnerFieldAccessEmbedOrderColumn().add(new XMLUniO2OOwnerFieldAccessEmbed(new XMLUniO2ODummyEntity(400)));
            findEntity.getMapKeyIntegerValueUniO2OOwnerFieldAccessEmbedOrderColumn().put(new Integer(0), new XMLUniO2OOwnerFieldAccessEmbed(new XMLUniO2ODummyEntity(500)));
            findEntity.getMapKeyUniO2OOwnerFieldAccessEmbedValueUniO2OOwnerFieldAccessEmbedOrderColumn()
                            .put(new XMLUniO2OOwnerFieldAccessEmbed(new XMLUniO2ODummyEntity(600)),
                                 new XMLUniO2OOwnerFieldAccessEmbed(new XMLUniO2ODummyEntity(700)));

            System.out.println("Performing merge(" + findEntity + ") operation");
            XMLJPAEmbeddableRelationshipEntity preMergedFindEntity = em.merge(findEntity);

            // Detach the merged entity
            em.clear();

            // Update the entity
            preMergedFindEntity.setBiO2OOwnerEmbed(new XMLBiO2OOwnerEmbed(new XMLBiO2OInverseEntity(22, preMergedFindEntity)));
            preMergedFindEntity.setBiO2OInverseEmbed(new XMLBiO2OInverseEmbed(new XMLBiO2OOwnerEntity(22, preMergedFindEntity)));
            preMergedFindEntity
                            .setBiO2OOwnerAssociationOverridesEmbed(new XMLBiO2OOwnerAssociationOverridesEmbed(new XMLBiO2OInverseAssociationOverridesEntity(22, preMergedFindEntity)));

            me.clear();
            me.add(preMergedFindEntity);
            preMergedFindEntity.getBiM2OOwnerEmbed().setBiO2MInverseEntity(new XMLBiO2MInverseEntity(12, me));

            other = preMergedFindEntity.getBiO2MInverseEmbed().getBiM2OOwnerEntities();
            other.add(new XMLBiM2OOwnerEntity(12, preMergedFindEntity));
            preMergedFindEntity.getBiO2MInverseEmbed().setBiM2OOwnerEntities(other);
            preMergedFindEntity.getBiM2MOwnerEmbed().getBiM2MInverseEntities().add(new XMLBiM2MInverseEntity(3, me));

            System.out.println("Performing merge(" + preMergedFindEntity + ") operation");
            XMLJPAEmbeddableRelationshipEntity mergedFindEntity = em.merge(preMergedFindEntity);

            // Perform content verifications
            Assert.assertFalse(em.contains(findEntity));
            Assert.assertFalse(em.contains(preMergedFindEntity));
            Assert.assertTrue(em.contains(mergedFindEntity));
            Assert.assertFalse(preMergedFindEntity == findEntity);
            Assert.assertFalse(preMergedFindEntity == mergedFindEntity);
            Assert.assertFalse(findEntity == mergedFindEntity);
            Assert.assertTrue(em.contains(mergedFindEntity.getUniO2OOwnerFieldAccessEmbed().getUniO2ODummyEntity_FA()));
            Assert.assertTrue(em.contains(mergedFindEntity.getUniO2OOwnerPropertyAccessEmbed().getUniO2ODummyEntity_PA()));
            Assert.assertTrue(em.contains(mergedFindEntity.getBiO2OOwnerEmbed().getBiO2OInverseEntity()));
            Assert.assertTrue(em.contains(mergedFindEntity.getBiO2OInverseEmbed().getBiO2OOwnerEntity()));
            Assert.assertTrue(em.contains(mergedFindEntity.getBiO2OOwnerAssociationOverridesEmbed().getBiO2OInverseAssociationOverridesEntity()));
            Assert.assertFalse(em.contains(mergedFindEntity.getUniM2OOwnerEmbed().getUniO2MDummyEntity()));
            Assert.assertTrue(em.contains(mergedFindEntity.getBiM2OOwnerEmbed().getBiO2MInverseEntity()));

            i = mergedFindEntity.getBiO2MInverseEmbed().getBiM2OOwnerEntities().iterator();
            Assert.assertTrue(em.contains(i.next()));
            Assert.assertTrue(em.contains(i.next()));

            i = mergedFindEntity.getBiM2MOwnerEmbed().getBiM2MInverseEntities().iterator();
            Assert.assertTrue(em.contains(i.next()));
            Assert.assertTrue(em.contains(i.next()));
            Assert.assertTrue(em.contains(i.next()));

            i = mergedFindEntity.getCollectionUniO2OOwnerPropertyAccessEmbedOrderColumn().iterator();
            Assert.assertTrue(em.contains(i.next()));
            Assert.assertTrue(em.contains(i.next()));
            Assert.assertTrue(em.contains(i.next()));

            i = mergedFindEntity.getListUniO2OOwnerFieldAccessEmbedOrderColumn().iterator();
            Assert.assertTrue(em.contains(i.next()));
            Assert.assertTrue(em.contains(i.next()));
            Assert.assertTrue(em.contains(i.next()));

            i = mergedFindEntity.getListUniO2OOwnerFieldAccessEmbedAssociationOverridesOrderColumn().iterator();
            Assert.assertTrue(em.contains(i.next()));
            Assert.assertTrue(em.contains(i.next()));
            Assert.assertTrue(em.contains(i.next()));

            i = mergedFindEntity.getSetUniO2OOwnerFieldAccessEmbedOrderColumn().iterator();
            Assert.assertTrue(em.contains(i.next()));
            Assert.assertTrue(em.contains(i.next()));

            i = mergedFindEntity.getMapKeyIntegerValueUniO2OOwnerFieldAccessEmbedOrderColumn().values().iterator();
            Assert.assertTrue(em.contains(i.next()));
            Assert.assertTrue(em.contains(i.next()));
            Assert.assertTrue(em.contains(i.next()));

            i = mergedFindEntity.getMapKeyUniO2OOwnerFieldAccessEmbedValueUniO2OOwnerFieldAccessEmbedOrderColumn().keySet().iterator();
            Assert.assertTrue(em.contains(i.next()));
            Assert.assertTrue(em.contains(i.next()));
            Assert.assertTrue(em.contains(i.next()));

            i = mergedFindEntity.getMapKeyUniO2OOwnerFieldAccessEmbedValueUniO2OOwnerFieldAccessEmbedOrderColumn().values().iterator();
            Assert.assertTrue(em.contains(i.next()));
            Assert.assertTrue(em.contains(i.next()));
            Assert.assertTrue(em.contains(i.next()));

            System.out.println("Performing merge(" + removeOldOwner + ") operation");
            XMLBiO2OOwnerEntity mergedRemoveOldOwner = em.merge(removeOldOwner);

            System.out.println("Performing remove(" + mergedRemoveOldOwner + ") operation");
            em.remove(mergedRemoveOldOwner);

            System.out.println("Committing transaction...");
            if (tj.isTransactionActive()) {
                tj.commitTransaction();
            }

            // 4) Verify the updated merged entity was saved to the database

            em.clear();

            System.out.println("Beginning new transaction...");
            tj.beginTransaction();
            if (tj.isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                em.joinTransaction();
            }

            // Verify the entity update was saved to the database
            System.out.println("Performing find(" + XMLJPAEmbeddableRelationshipEntity.class + ", " + id + ") operation");
            XMLJPAEmbeddableRelationshipEntity updatedFindEntity = em.find(XMLJPAEmbeddableRelationshipEntity.class, id);
            Assert.assertNotNull("find(" + XMLJPAEmbeddableRelationshipEntity.class + ", " + id + ") did not return an entity.", updatedFindEntity);

            // Perform content verifications
            Assert.assertTrue(newEntity != updatedFindEntity);
            Assert.assertTrue(findEntity != updatedFindEntity);
            Assert.assertTrue(em.contains(updatedFindEntity));
            Assert.assertEquals(updatedFindEntity.getId(), findEntity.getId());
            Assert.assertEquals(findEntity.getUniO2OOwnerFieldAccessEmbed(), updatedFindEntity.getUniO2OOwnerFieldAccessEmbed());
            Assert.assertEquals(findEntity.getUniO2OOwnerPropertyAccessEmbed(), updatedFindEntity.getUniO2OOwnerPropertyAccessEmbed());
            Assert.assertEquals(preMergedFindEntity.getBiO2OOwnerEmbed(), updatedFindEntity.getBiO2OOwnerEmbed());
            Assert.assertEquals(preMergedFindEntity.getBiO2OInverseEmbed(), updatedFindEntity.getBiO2OInverseEmbed());
            Assert.assertEquals(preMergedFindEntity.getBiO2OOwnerAssociationOverridesEmbed(), updatedFindEntity.getBiO2OOwnerAssociationOverridesEmbed());
            Assert.assertEquals(findEntity.getUniM2OOwnerEmbed(), updatedFindEntity.getUniM2OOwnerEmbed());
            Assert.assertEquals(preMergedFindEntity.getBiM2OOwnerEmbed(), updatedFindEntity.getBiM2OOwnerEmbed());
            Assert.assertEquals(preMergedFindEntity.getBiO2MInverseEmbed(), updatedFindEntity.getBiO2MInverseEmbed());
            Assert.assertEquals(preMergedFindEntity.getBiM2MOwnerEmbed(), updatedFindEntity.getBiM2MOwnerEmbed());
            Assert.assertEquals(new HashSet<>(findEntity.getCollectionUniO2OOwnerPropertyAccessEmbedOrderColumn()),
                                new HashSet<>(updatedFindEntity.getCollectionUniO2OOwnerPropertyAccessEmbedOrderColumn()));
            Assert.assertEquals(findEntity.getListUniO2OOwnerFieldAccessEmbedOrderColumn(), updatedFindEntity.getListUniO2OOwnerFieldAccessEmbedOrderColumn());
            Assert.assertEquals(findEntity.getListUniO2OOwnerFieldAccessEmbedAssociationOverridesOrderColumn(),
                                updatedFindEntity.getListUniO2OOwnerFieldAccessEmbedAssociationOverridesOrderColumn());
            Assert.assertEquals(findEntity.getSetUniO2OOwnerFieldAccessEmbedOrderColumn(), updatedFindEntity.getSetUniO2OOwnerFieldAccessEmbedOrderColumn());
            Assert.assertEquals(findEntity.getMapKeyIntegerValueUniO2OOwnerFieldAccessEmbedOrderColumn(),
                                updatedFindEntity.getMapKeyIntegerValueUniO2OOwnerFieldAccessEmbedOrderColumn());
            Assert.assertEquals(findEntity.getMapKeyUniO2OOwnerFieldAccessEmbedValueUniO2OOwnerFieldAccessEmbedOrderColumn(),
                                updatedFindEntity.getMapKeyUniO2OOwnerFieldAccessEmbedValueUniO2OOwnerFieldAccessEmbedOrderColumn());

            // Delete the entity from the database
            System.out.println("Performing remove(" + updatedFindEntity + ") operation");
            em.remove(updatedFindEntity);

            System.out.println("Committing transaction...");
            if (tj.isTransactionActive()) {
                tj.commitTransaction();
            }

            em.clear();

            // Verify the entity remove was successful
            System.out.println("Performing find(" + JPAEmbeddableRelationshipEntity.class + ", " + id + ") operation");
            Object findRemovedEntity = em.find(JPAEmbeddableRelationshipEntity.class, id);
            Assert.assertNull("find(" + JPAEmbeddableRelationshipEntity.class + ", " + id + ") returned an entity.", findRemovedEntity);
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace(System.err);
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }
}
