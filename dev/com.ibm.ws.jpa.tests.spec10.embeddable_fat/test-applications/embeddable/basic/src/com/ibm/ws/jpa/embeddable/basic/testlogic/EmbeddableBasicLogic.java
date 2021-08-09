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

package com.ibm.ws.jpa.embeddable.basic.testlogic;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.junit.Assert;

import com.ibm.ws.jpa.embeddable.basic.model.CollectionEnumeratedEmbed;
import com.ibm.ws.jpa.embeddable.basic.model.CollectionIntegerEmbed;
import com.ibm.ws.jpa.embeddable.basic.model.CollectionLobEmbed;
import com.ibm.ws.jpa.embeddable.basic.model.CollectionTemporalEmbed;
import com.ibm.ws.jpa.embeddable.basic.model.EnumeratedFieldAccessEmbed;
import com.ibm.ws.jpa.embeddable.basic.model.EnumeratedFieldAccessEmbed.EnumeratedFieldAccessEnum;
import com.ibm.ws.jpa.embeddable.basic.model.EnumeratedPropertyAccessEmbed;
import com.ibm.ws.jpa.embeddable.basic.model.EnumeratedPropertyAccessEmbed.EnumeratedPropertyAccessEnum;
import com.ibm.ws.jpa.embeddable.basic.model.IntegerAttributeOverridesEmbed;
import com.ibm.ws.jpa.embeddable.basic.model.IntegerEmbed;
import com.ibm.ws.jpa.embeddable.basic.model.IntegerFieldAccessEmbed;
import com.ibm.ws.jpa.embeddable.basic.model.IntegerPropertyAccessEmbed;
import com.ibm.ws.jpa.embeddable.basic.model.IntegerTransientEmbed;
import com.ibm.ws.jpa.embeddable.basic.model.JPAEmbeddableBasicEntity;
import com.ibm.ws.jpa.embeddable.basic.model.ListEnumeratedEmbed;
import com.ibm.ws.jpa.embeddable.basic.model.ListIntegerOrderByEmbed;
import com.ibm.ws.jpa.embeddable.basic.model.ListIntegerOrderColumnEmbed;
import com.ibm.ws.jpa.embeddable.basic.model.ListLobEmbed;
import com.ibm.ws.jpa.embeddable.basic.model.ListTemporalEmbed;
import com.ibm.ws.jpa.embeddable.basic.model.LobFieldAccessEmbed;
import com.ibm.ws.jpa.embeddable.basic.model.LobPropertyAccessEmbed;
import com.ibm.ws.jpa.embeddable.basic.model.MapKeyEnumeratedValueEnumeratedEmbed;
import com.ibm.ws.jpa.embeddable.basic.model.MapKeyEnumeratedValueLobEmbed;
import com.ibm.ws.jpa.embeddable.basic.model.MapKeyIntegerEmbed;
import com.ibm.ws.jpa.embeddable.basic.model.MapKeyIntegerValueTemporalEmbed;
import com.ibm.ws.jpa.embeddable.basic.model.MapKeyTemporalValueTemporalEmbed;
import com.ibm.ws.jpa.embeddable.basic.model.SetEnumeratedEmbed;
import com.ibm.ws.jpa.embeddable.basic.model.SetIntegerEmbed;
import com.ibm.ws.jpa.embeddable.basic.model.SetLobEmbed;
import com.ibm.ws.jpa.embeddable.basic.model.SetTemporalEmbed;
import com.ibm.ws.jpa.embeddable.basic.model.TemporalFieldAccessEmbed;
import com.ibm.ws.jpa.embeddable.basic.model.TemporalPropertyAccessEmbed;
import com.ibm.ws.jpa.embeddable.basic.model.XMLCollectionEnumeratedEmbed;
import com.ibm.ws.jpa.embeddable.basic.model.XMLCollectionIntegerEmbed;
import com.ibm.ws.jpa.embeddable.basic.model.XMLCollectionLobEmbed;
import com.ibm.ws.jpa.embeddable.basic.model.XMLCollectionTemporalEmbed;
import com.ibm.ws.jpa.embeddable.basic.model.XMLEnumeratedFieldAccessEmbed;
import com.ibm.ws.jpa.embeddable.basic.model.XMLEnumeratedFieldAccessEmbed.XMLEnumeratedFieldAccessEnum;
import com.ibm.ws.jpa.embeddable.basic.model.XMLEnumeratedPropertyAccessEmbed;
import com.ibm.ws.jpa.embeddable.basic.model.XMLEnumeratedPropertyAccessEmbed.XMLEnumeratedPropertyAccessEnum;
import com.ibm.ws.jpa.embeddable.basic.model.XMLIntegerAttributeOverridesEmbed;
import com.ibm.ws.jpa.embeddable.basic.model.XMLIntegerEmbed;
import com.ibm.ws.jpa.embeddable.basic.model.XMLIntegerFieldAccessEmbed;
import com.ibm.ws.jpa.embeddable.basic.model.XMLIntegerPropertyAccessEmbed;
import com.ibm.ws.jpa.embeddable.basic.model.XMLIntegerTransientEmbed;
import com.ibm.ws.jpa.embeddable.basic.model.XMLJPAEmbeddableBasicEntity;
import com.ibm.ws.jpa.embeddable.basic.model.XMLListEnumeratedEmbed;
import com.ibm.ws.jpa.embeddable.basic.model.XMLListIntegerOrderByEmbed;
import com.ibm.ws.jpa.embeddable.basic.model.XMLListIntegerOrderColumnEmbed;
import com.ibm.ws.jpa.embeddable.basic.model.XMLListLobEmbed;
import com.ibm.ws.jpa.embeddable.basic.model.XMLListTemporalEmbed;
import com.ibm.ws.jpa.embeddable.basic.model.XMLLobFieldAccessEmbed;
import com.ibm.ws.jpa.embeddable.basic.model.XMLLobPropertyAccessEmbed;
import com.ibm.ws.jpa.embeddable.basic.model.XMLMapKeyEnumeratedValueEnumeratedEmbed;
import com.ibm.ws.jpa.embeddable.basic.model.XMLMapKeyEnumeratedValueLobEmbed;
import com.ibm.ws.jpa.embeddable.basic.model.XMLMapKeyIntegerEmbed;
import com.ibm.ws.jpa.embeddable.basic.model.XMLMapKeyIntegerValueTemporalEmbed;
import com.ibm.ws.jpa.embeddable.basic.model.XMLMapKeyTemporalValueTemporalEmbed;
import com.ibm.ws.jpa.embeddable.basic.model.XMLSetEnumeratedEmbed;
import com.ibm.ws.jpa.embeddable.basic.model.XMLSetIntegerEmbed;
import com.ibm.ws.jpa.embeddable.basic.model.XMLSetLobEmbed;
import com.ibm.ws.jpa.embeddable.basic.model.XMLSetTemporalEmbed;
import com.ibm.ws.jpa.embeddable.basic.model.XMLTemporalFieldAccessEmbed;
import com.ibm.ws.jpa.embeddable.basic.model.XMLTemporalPropertyAccessEmbed;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.testlogic.AbstractTestLogic;
import com.ibm.ws.testtooling.tranjacket.TransactionJacket;
import com.ibm.ws.testtooling.vehicle.resources.JPAResource;
import com.ibm.ws.testtooling.vehicle.resources.TestExecutionResources;

public class EmbeddableBasicLogic extends AbstractTestLogic {

    /**
     * Test Logic: testEmbeddableBasic01
     *
     * <p>
     * JPA 2.0 Specifications Tested (per V2 spec):
     * <ul>
     * <li>2.3.2 Explicit Access Type
     * <li>2.3.3 Access Type of an Embeddable Class
     * <li>2.5 Embeddable Classes
     * <li>2.6 Collections of Embeddable Classes and Basic Types
     * <li>2.7 Map Collections
     * <li>2.7.1 Map Keys
     * <li>2.7.2 Map Values
     * <li>11.1.1 Access Annotation
     * <li>11.1.4 AttributeOverride Annotation
     * <li>11.1.5 AttributeOverrides Annotation
     * <li>11.1.8 CollectionTable Annotation
     * <li>11.1.9 Column Annotation
     * <li>11.1.12 ElementCollection Annotation
     * <li>11.1.13 Embeddable Annotation
     * <li>11.1.14 Embedded Annotation
     * <li>11.1.16 Enumerated Annotation
     * <li>11.1.24 Lob Annotation
     * <li>11.1.29 MapKeyColumn Annotation
     * <li>11.1.30 MapKeyEnumerated Annotation
     * <li>11.1.33 MapKeyTemporal Annotation
     * <li>11.1.38 OrderBy Annotation
     * <li>11.1.39 OrderColumn Annotation
     * <li>11.1.47 Temporal Annotation
     * <li>11.1.48 Transient Annotation
     * <li>12.2.2.12 embeddable
     * <li>12.2.3.14 attribute-override
     * <li>12.2.3.23.9 element-collection
     * <li>12.2.3.23.10 embedded
     * <li>12.2.5.2 access
     * <li>12.2.5.3.1 basic
     * <li>12.2.5.3.6 element-collection
     * <li>12.2.5.3.8 transient
     * </ul>
     *
     * <p>
     * Description: Performs basic CRUD operations:
     * <ol>
     * <li>Create a new initialized entity, and persist it to the database.
     * <li>Verify the entity was saved to the database. This verification
     * ensures <b>both</b> annotations and XML on these artifacts are respected:
     * <ol>
     * <li>Embedded defaulted object.
     * <li>AttributeOverrides of embedded basic type.
     * <li>Embedded field access with Column override.
     * <li>Embedded field access of Enumerated ordinal and string.
     * <li>Embedded field access of Temporal date.
     * <li>Embedded field access of CLOB.
     * <li>Embedded Java transient field.
     * <li>Embedded field access Transient.
     * <li>Embedded property access with Column override (mixing field/property
     * access).
     * <li>Embedded property access of Enumerated ordinal and string (mixing
     * field/property access).
     * <li>Embedded property access of Temporal date (mixing field/property
     * access).
     * <li>Embedded property access of CLOB (mixing field/property access).
     * <li>Embedded Collection&lt;Integer&gt; using ElementCollection,
     * CollectionTable, Column, and sorted by OrderColumn.
     * <li>Embedded Collection&lt;Enumerated.String&gt; using ElementCollection,
     * CollectionTable, Column, Enumerated, and sorted by OrderColumn.
     * <li>Embedded Collection&lt;Date&gt; using ElementCollection,
     * CollectionTable, Column, Temporal, and sorted by OrderColumn.
     * <li>Embedded Collection&lt;CLOB&gt; using ElementCollection,
     * CollectionTable, Column, Lob, and sorted by OrderColumn.
     * <li>Embedded List&lt;Integer&gt; using ElementCollection,
     * CollectionTable, Column, and sorted by OrderColumn.
     * <li>Embedded List&lt;Integer&gt; using ElementCollection,
     * CollectionTable, Column, and sorted by OrderBy.
     * <li>Embedded List&lt;Enumerated.String&gt; using ElementCollection,
     * CollectionTable, Column, Enumerated, and sorted by OrderColumn.
     * <li>Embedded List&lt;Date&gt; using ElementCollection, CollectionTable,
     * Column, Temporal, and sorted by OrderColumn.
     * <li>Embedded List&lt;CLOB&gt; using ElementCollection, CollectionTable,
     * Column, Lob, and sorted by OrderColumn.
     * <li>Embedded Set&lt;Integer&gt; using ElementCollection, CollectionTable,
     * and Column.
     * <li>Embedded Set&lt;Enumerated.String&gt; using ElementCollection,
     * CollectionTable, Column, and Enumerated.
     * <li>Embedded Set&lt;Date&gt; using ElementCollection, CollectionTable,
     * Column, and Temporal.
     * <li>Embedded Set&lt;CLOB&gt; using ElementCollection, CollectionTable,
     * Column, and Lob.
     * <li>Embedded Map&lt;Integer,Integer&gt; using ElementCollection,
     * CollectionTable, MapKeyColumn and Column.
     * <li>Embedded Map&lt;Integer,Date&gt; using ElementCollection,
     * CollectionTable, MapKeyColumn, Column, and Temporal.
     * <li>Embedded Map&lt;Enumerated.String,Enumerated.String&gt; using
     * ElementCollection, CollectionTable, MapKeyEnumerated, MapKeyColumn,
     * Column, and Enumerated.
     * <li>Embedded Map&lt;Date,Date&gt; using ElementCollection,
     * CollectionTable, MapKeyTemporal, MapKeyColumn, Column, and Temporal.
     * <li>Embedded Map&lt;Enumerated.String,CLOB&gt; using ElementCollection,
     * CollectionTable, MapKeyEnumerated, MapKeyColumn, Column, and Lob.
     * <li>Collection&lt;Embeddable.P/A CLOB&gt; using ElementCollection,
     * CollectionTable, and sorted by OrderColumn (mixing field/property
     * access).
     * <li>List&lt;Embeddable.F/A Integer&gt; using ElementCollection,
     * CollectionTable, and sorted by OrderColumn.
     * <li>List&lt;Embeddable.F/A Integer&gt; using ElementCollection,
     * CollectionTable, AttributeOverrides, and sorted by OrderColumn.
     * <li>List&lt;Embeddable.P/A Integer w/ Column&gt; using ElementCollection,
     * CollectionTable, and sorted by OrderColumn (mixing field/property
     * access).
     * <li>List&lt;Embeddable.P/A enumerations&gt; using ElementCollection,
     * CollectionTable, and sorted by OrderColumn (mixing field/property
     * access).
     * <li>List&lt;Embeddable.P/A enumerations&gt; using ElementCollection,
     * CollectionTable, and sorted by OrderBy (mixing field/property access).
     * <li>List&lt;Embeddable.P/A Date&gt; using ElementCollection,
     * CollectionTable, and sorted by OrderColumn (mixing field/property
     * access).
     * <li>Set&lt;Embeddable.P/A Integer&gt; using ElementCollection and
     * CollectionTable (mixing field/property access).
     * <li>Map&lt;Integer,Embeddable.P/A Integer&gt; using ElementCollection,
     * CollectionTable, MapKeyColumn, and AttributeOverrides (mixing
     * field/property access).
     * <li>Map&lt;Integer,Embeddable.P/A Date&gt; using ElementCollection,
     * CollectionTable, and MapKeyColumn, and sorted by OrderColumn (mixing
     * field/property access).
     * <li>Map&lt;Date,Embeddable.P/A Date&gt; using ElementCollection,
     * CollectionTable, MapKeyTemporal, and MapKeyColumn, and sorted by
     * OrderColumn (mixing field/property access).
     * <li>Map&lt;Enumerated.String,Embeddable.P/A Enumerated.String and
     * Enumerated.Ordinal&gt; using ElementCollection, CollectionTable,
     * MapKeyEnumerated, and MapKeyColumn, AttributeOverrides on both values,
     * and sorted by OrderColumn (mixing field/property access).
     * <li>Map&lt;Embeddable.F/A Integer,Embeddable.F/A Integer&gt; using
     * ElementCollection, CollectionTable, and AttributeOverrides on key and
     * value.
     * <li>Map&lt;Embeddable.P/A CLOB,Embeddable.P/A CLOB&gt; using
     * ElementCollection, CollectionTable, and AttributeOverrides on key and
     * value (mixing field/property access).
     * </ol>
     * <li>Update the entity, and re-persist it to the database.
     * <li>Verify the entity update was saved to the database, as indicated
     * above.
     * <li>Delete the entity from the database.
     * <li>Verify the entity remove was successful.
     * </ol>
     *
     * @throws Exception
     */
    @SuppressWarnings({ "unchecked", "rawtypes", "deprecation" })
    public void testEmbeddableBasic01(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        JPAProviderImpl provider = getJPAProviderImpl(jpaResource);

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            int id = 1;

            JPAEmbeddableBasicEntity newEntity = new JPAEmbeddableBasicEntity();
            newEntity.setId(id);
            newEntity.setIntegerEmbed(new IntegerEmbed(11));
            newEntity.setIntegerAttributeOverridesEmbed(new IntegerAttributeOverridesEmbed(22));
            newEntity.setIntegerFieldAccessEmbed(new IntegerFieldAccessEmbed(33));
            newEntity.setEnumeratedFieldAccessEmbed(new EnumeratedFieldAccessEmbed(EnumeratedFieldAccessEnum.ONE));
            newEntity.setTemporalFieldAccessEmbed(new TemporalFieldAccessEmbed(new Date()));
            newEntity.setLobFieldAccessEmbed(new LobFieldAccessEmbed("InitFA"));
            newEntity.setIntegerTransientEmbed(new IntegerTransientEmbed(44, 55));
            newEntity.setIntegerPropertyAccessEmbed(new IntegerPropertyAccessEmbed(66));
            newEntity.setEnumeratedPropertyAccessEmbed(new EnumeratedPropertyAccessEmbed(EnumeratedPropertyAccessEnum.TWO));
            newEntity.setTemporalPropertyAccessEmbed(new TemporalPropertyAccessEmbed(new Date()));
            newEntity.setLobPropertyAccessEmbed(new LobPropertyAccessEmbed("InitPA"));
            newEntity.setCollectionIntegerEmbed(new CollectionIntegerEmbed(CollectionIntegerEmbed.INIT));
            newEntity.setCollectionEnumeratedEmbed(new CollectionEnumeratedEmbed(CollectionEnumeratedEmbed.INIT));
            newEntity.setCollectionTemporalEmbed(new CollectionTemporalEmbed(CollectionTemporalEmbed.INIT));
            newEntity.setCollectionLobEmbed(new CollectionLobEmbed(CollectionLobEmbed.INIT));
            newEntity.setListIntegerOrderColumnEmbed(new ListIntegerOrderColumnEmbed(ListIntegerOrderColumnEmbed.INIT));
            newEntity.setListIntegerOrderByEmbed(new ListIntegerOrderByEmbed(ListIntegerOrderByEmbed.INIT));
            newEntity.setListEnumeratedEmbed(new ListEnumeratedEmbed(ListEnumeratedEmbed.INIT));
            newEntity.setListTemporalEmbed(new ListTemporalEmbed(ListTemporalEmbed.INIT));
            newEntity.setListLobEmbed(new ListLobEmbed(ListLobEmbed.INIT));
            newEntity.setSetIntegerEmbed(new SetIntegerEmbed(SetIntegerEmbed.INIT));
            newEntity.setSetEnumeratedEmbed(new SetEnumeratedEmbed(SetEnumeratedEmbed.INIT));
            newEntity.setSetTemporalEmbed(new SetTemporalEmbed(SetTemporalEmbed.INIT));
            newEntity.setSetLobEmbed(new SetLobEmbed(SetLobEmbed.INIT));
            newEntity.setMapKeyIntegerEmbed(new MapKeyIntegerEmbed(MapKeyIntegerEmbed.INIT));
            newEntity.setMapKeyIntegerValueTemporalEmbed(new MapKeyIntegerValueTemporalEmbed(MapKeyIntegerValueTemporalEmbed.INIT));
            newEntity.setMapKeyEnumeratedValueEnumeratedEmbed(new MapKeyEnumeratedValueEnumeratedEmbed(MapKeyEnumeratedValueEnumeratedEmbed.INIT));
            newEntity.setMapKeyTemporalValueTemporalEmbed(new MapKeyTemporalValueTemporalEmbed(MapKeyTemporalValueTemporalEmbed.INIT));
            newEntity.setMapKeyEnumeratedValueLobEmbed(new MapKeyEnumeratedValueLobEmbed(MapKeyEnumeratedValueLobEmbed.INIT));
            newEntity.setCollectionLobPropertyAccessEmbed(new ArrayList(LobPropertyAccessEmbed.COLLECTION_INIT));
            newEntity.setListIntegerEmbedOrderColumn(new ArrayList(IntegerEmbed.LIST_INIT));
            newEntity.setListIntegerAttributeOverridesEmbedOrderColumn(new ArrayList(IntegerAttributeOverridesEmbed.LIST_INIT));
            newEntity.setListIntegerPropertyAccessEmbedOrderColumn(new ArrayList(IntegerPropertyAccessEmbed.LIST_INIT));
            newEntity.setListEnumeratedPropertyAccessEmbedOrderColumn(new ArrayList(EnumeratedPropertyAccessEmbed.LIST_INIT));
            newEntity.setListEnumeratedPropertyAccessEmbedOrderBy(new ArrayList(EnumeratedPropertyAccessEmbed.LIST_INIT));
            newEntity.setListTemporalPropertyAccessEmbedOrderColumn(new ArrayList(TemporalPropertyAccessEmbed.LIST_INIT));
            newEntity.setSetIntegerPropertyAccessEmbed(new HashSet(IntegerPropertyAccessEmbed.SET_INIT));
            newEntity.setMapKeyIntegerValueIntegerPropertyAccessEmbed(new HashMap(IntegerPropertyAccessEmbed.MAP_INIT));
            newEntity.setMapKeyIntegerValueTemporalPropertyAccessEmbed(new HashMap(TemporalPropertyAccessEmbed.MAP_INIT));
            newEntity.setMapKeyTemporalValueTemporalPropertyAccessEmbed(new HashMap(TemporalPropertyAccessEmbed.MAP2_INIT));
            newEntity.setMapKeyEnumeratedValueEnumeratedPropertyAccessEmbed(new HashMap(EnumeratedPropertyAccessEmbed.MAP_INIT));
            newEntity.setMapKeyIntegerEmbedValueIntegerEmbed(new HashMap(IntegerEmbed.MAP_INIT2));
            newEntity.setMapKeyLobEmbedValueLobEmbed(new HashMap(LobPropertyAccessEmbed.MAP_INIT));

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
            System.out.println("Performing find(" + JPAEmbeddableBasicEntity.class.getSimpleName() + ", " + id + ") operation");
            JPAEmbeddableBasicEntity findEntity = em.find(JPAEmbeddableBasicEntity.class, id);
            Assert.assertNotNull("find(" + JPAEmbeddableBasicEntity.class.getSimpleName() + ", " + id + ") did not return an entity.", findEntity);

            // Perform content verifications
            Assert.assertTrue(newEntity != findEntity);
            Assert.assertTrue(findEntity + " is not managed by the persistence context.", em.contains(findEntity));
            Assert.assertEquals(newEntity.getId(), findEntity.getId());
            Assert.assertEquals(newEntity.getIntegerEmbed(), findEntity.getIntegerEmbed());
            Assert.assertEquals(newEntity.getIntegerAttributeOverridesEmbed(), findEntity.getIntegerAttributeOverridesEmbed());
            Assert.assertEquals(newEntity.getIntegerFieldAccessEmbed(),
                                findEntity.getIntegerFieldAccessEmbed());
            Assert.assertEquals(newEntity.getEnumeratedFieldAccessEmbed(), findEntity.getEnumeratedFieldAccessEmbed());
            Assert.assertEquals(newEntity.getTemporalFieldAccessEmbed(), findEntity.getTemporalFieldAccessEmbed());
            Assert.assertEquals(newEntity.getLobFieldAccessEmbed(), findEntity.getLobFieldAccessEmbed());

            /*
             * If an Embeddable class has only one field and that field is transient, Ecipselink doesn't
             * create an object of type Embeddable class and set its transient field null.
             */
            if (JPAProviderImpl.ECLIPSELINK.equals(provider)) {
                Assert.assertNull(findEntity.getIntegerTransientEmbed());
            } else if (JPAProviderImpl.OPENJPA.equals(provider)) {
                Assert.assertNull(findEntity.getIntegerTransientEmbed().getTransientJavaValue());
                Assert.assertNull(findEntity.getIntegerTransientEmbed().getTransientValue());
            }

            Assert.assertEquals(newEntity.getIntegerPropertyAccessEmbed(),
                                findEntity.getIntegerPropertyAccessEmbed());
            Assert.assertEquals(newEntity.getEnumeratedPropertyAccessEmbed(),
                                findEntity.getEnumeratedPropertyAccessEmbed());
            Assert.assertEquals(newEntity.getTemporalPropertyAccessEmbed(), findEntity.getTemporalPropertyAccessEmbed());
            Assert.assertEquals(newEntity.getLobPropertyAccessEmbed(), findEntity.getLobPropertyAccessEmbed());
            Assert.assertEquals(newEntity.getCollectionIntegerEmbed(),
                                findEntity.getCollectionIntegerEmbed());
            Assert.assertEquals(newEntity.getCollectionEnumeratedEmbed(),
                                findEntity.getCollectionEnumeratedEmbed());
            Assert.assertEquals(newEntity.getCollectionTemporalEmbed(),
                                findEntity.getCollectionTemporalEmbed());
            Assert.assertEquals(newEntity.getCollectionLobEmbed(),
                                findEntity.getCollectionLobEmbed());
            Assert.assertEquals(newEntity.getListIntegerOrderColumnEmbed(),
                                findEntity.getListIntegerOrderColumnEmbed());
            Assert.assertEquals(ListIntegerOrderByEmbed.INIT_ORDERED, findEntity.getListIntegerOrderByEmbed().getNotListIntegerOrderBy());
            Assert.assertEquals(newEntity.getListEnumeratedEmbed(), findEntity.getListEnumeratedEmbed());
            Assert.assertEquals(newEntity.getListTemporalEmbed(),
                                findEntity.getListTemporalEmbed());
            Assert.assertEquals(newEntity.getListLobEmbed(), findEntity.getListLobEmbed());
            Assert.assertEquals(newEntity.getSetIntegerEmbed(), findEntity.getSetIntegerEmbed());
            Assert.assertEquals(newEntity.getSetEnumeratedEmbed(), findEntity.getSetEnumeratedEmbed());
            Assert.assertEquals(newEntity.getSetTemporalEmbed(), findEntity.getSetTemporalEmbed());
            Assert.assertEquals(newEntity.getSetLobEmbed(), findEntity.getSetLobEmbed());
            Assert.assertEquals(newEntity.getMapKeyIntegerEmbed(), findEntity.getMapKeyIntegerEmbed());
            Assert.assertEquals(newEntity.getMapKeyIntegerValueTemporalEmbed(),
                                findEntity.getMapKeyIntegerValueTemporalEmbed());
            Assert.assertEquals(newEntity.getMapKeyEnumeratedValueEnumeratedEmbed(),
                                findEntity.getMapKeyEnumeratedValueEnumeratedEmbed());
            Assert.assertEquals(newEntity.getMapKeyTemporalValueTemporalEmbed(),
                                findEntity.getMapKeyTemporalValueTemporalEmbed());
            Assert.assertEquals(newEntity.getMapKeyEnumeratedValueLobEmbed(),
                                findEntity.getMapKeyEnumeratedValueLobEmbed());
            Assert.assertEquals(newEntity.getCollectionLobPropertyAccessEmbed(),
                                findEntity.getCollectionLobPropertyAccessEmbed());
            Assert.assertEquals(newEntity.getListIntegerEmbedOrderColumn(),
                                findEntity.getListIntegerEmbedOrderColumn());
            Assert.assertEquals(newEntity.getListIntegerAttributeOverridesEmbedOrderColumn(), findEntity.getListIntegerAttributeOverridesEmbedOrderColumn());
            Assert.assertEquals(newEntity.getListIntegerPropertyAccessEmbedOrderColumn(), findEntity.getListIntegerPropertyAccessEmbedOrderColumn());
            Assert.assertEquals(newEntity.getListEnumeratedPropertyAccessEmbedOrderColumn(),
                                findEntity.getListEnumeratedPropertyAccessEmbedOrderColumn());
            Assert.assertEquals(EnumeratedPropertyAccessEmbed.LIST_INIT_ORDERED,
                                findEntity.getListEnumeratedPropertyAccessEmbedOrderBy());
            Assert.assertEquals(newEntity.getListTemporalPropertyAccessEmbedOrderColumn(),
                                findEntity.getListTemporalPropertyAccessEmbedOrderColumn());
            Assert.assertEquals(newEntity.getSetIntegerPropertyAccessEmbed(),
                                findEntity.getSetIntegerPropertyAccessEmbed());
            Assert.assertEquals(newEntity.getMapKeyIntegerValueIntegerPropertyAccessEmbed(),
                                findEntity.getMapKeyIntegerValueIntegerPropertyAccessEmbed());
            Assert.assertEquals(newEntity.getMapKeyIntegerValueTemporalPropertyAccessEmbed(),
                                findEntity.getMapKeyIntegerValueTemporalPropertyAccessEmbed());
            Assert.assertEquals(newEntity.getMapKeyTemporalValueTemporalPropertyAccessEmbed(),
                                findEntity.getMapKeyTemporalValueTemporalPropertyAccessEmbed());
            Assert.assertEquals(newEntity.getMapKeyEnumeratedValueEnumeratedPropertyAccessEmbed(),
                                findEntity.getMapKeyEnumeratedValueEnumeratedPropertyAccessEmbed());
            Assert.assertEquals(newEntity.getMapKeyIntegerEmbedValueIntegerEmbed(),
                                findEntity.getMapKeyIntegerEmbedValueIntegerEmbed());
            Assert.assertEquals(newEntity.getMapKeyLobEmbedValueLobEmbed(),
                                findEntity.getMapKeyLobEmbedValueLobEmbed());

            // Update the entity
            findEntity.getIntegerEmbed().setIntegerValue(111);
            findEntity.getIntegerAttributeOverridesEmbed().setNotIntegerValue(222);
            findEntity.getIntegerFieldAccessEmbed().setIntegerValueFieldAccessColumn(333);
            findEntity.getEnumeratedFieldAccessEmbed().setEnumeratedStringValueFA(EnumeratedFieldAccessEnum.TWO);
            findEntity.getEnumeratedFieldAccessEmbed().setEnumeratedOrdinalValueFA(EnumeratedFieldAccessEnum.TWO);
            findEntity.getTemporalFieldAccessEmbed().setTemporalValueFA(new Date(25000));
            findEntity.getLobFieldAccessEmbed().setClobValueFA("UpdateFA");

            /*
             * If an Embeddable class has only one field and that field is transient, Ecipselink doesn't
             * create an object of type Embeddable class and set its transient field null.
             */
            if (JPAProviderImpl.ECLIPSELINK.equals(provider)) {
                findEntity.setIntegerTransientEmbed(new IntegerTransientEmbed());
            }
            findEntity.getIntegerTransientEmbed().setTransientJavaValue(444);
            findEntity.getIntegerTransientEmbed().setTransientValue(555);

            findEntity.getIntegerPropertyAccessEmbed().setIntegerValuePropertyAccessColumn(666);
            findEntity.getEnumeratedPropertyAccessEmbed().setEnumeratedStringValuePA(EnumeratedPropertyAccessEnum.THREE);
            findEntity.getEnumeratedPropertyAccessEmbed().setEnumeratedOrdinalValuePA(EnumeratedPropertyAccessEnum.THREE);
            findEntity.getTemporalPropertyAccessEmbed().setTemporalValuePA(new Date(25000));
            findEntity.getLobPropertyAccessEmbed().setClobValuePA("UpdatePA");
            findEntity.getCollectionIntegerEmbed().setCollectionInteger(CollectionIntegerEmbed.UPDATE);
            findEntity.getCollectionEnumeratedEmbed().setCollectionEnumerated(CollectionEnumeratedEmbed.UPDATE);
            findEntity.getCollectionTemporalEmbed().setCollectionDate(CollectionTemporalEmbed.UPDATE);
            findEntity.getCollectionLobEmbed().setCollectionLob(CollectionLobEmbed.UPDATE);
            findEntity.getListIntegerOrderColumnEmbed().setNotListIntegerOrderColumn(ListIntegerOrderColumnEmbed.UPDATE);
            findEntity.getListIntegerOrderByEmbed().setNotListIntegerOrderBy(ListIntegerOrderByEmbed.UPDATE);
            findEntity.getListEnumeratedEmbed().setListEnumerated(ListEnumeratedEmbed.UPDATE);
            findEntity.getListTemporalEmbed().setListDate(ListTemporalEmbed.UPDATE);
            findEntity.getListLobEmbed().setListLob(ListLobEmbed.UPDATE);
            findEntity.getSetIntegerEmbed().getNotSetInteger().remove(new Integer(1));
            findEntity.getSetIntegerEmbed().getNotSetInteger().add(new Integer(2)); // dup.
            findEntity.getSetEnumeratedEmbed().setSetEnumerated(SetEnumeratedEmbed.UPDATE);
            findEntity.getSetTemporalEmbed().setSetDate(SetTemporalEmbed.UPDATE);
            findEntity.getSetLobEmbed().setSetLob(SetLobEmbed.UPDATE);
            findEntity.getMapKeyIntegerEmbed().getNotMapKeyInteger().put(new Integer(4), new Integer(400));
            findEntity.getMapKeyIntegerValueTemporalEmbed().setMapKeyIntegerValueTemporal(MapKeyIntegerValueTemporalEmbed.UPDATE);
            findEntity.getMapKeyEnumeratedValueEnumeratedEmbed().setMapKeyEnumeratedValueEnumerated(MapKeyEnumeratedValueEnumeratedEmbed.UPDATE);
            findEntity.getMapKeyTemporalValueTemporalEmbed().setMapKeyTemporalValueTemporal(MapKeyTemporalValueTemporalEmbed.UPDATE);
            findEntity.getMapKeyEnumeratedValueLobEmbed().setMapKeyEnumeratedValueLob(MapKeyEnumeratedValueLobEmbed.UPDATE);
            findEntity.setCollectionLobPropertyAccessEmbed(new ArrayList(LobPropertyAccessEmbed.COLLECTION_UPDATE));
            findEntity.setListIntegerEmbedOrderColumn(new ArrayList(IntegerEmbed.LIST_UPDATE));
            findEntity.setListIntegerAttributeOverridesEmbedOrderColumn(new ArrayList(IntegerAttributeOverridesEmbed.LIST_UPDATE));
            findEntity.setListIntegerPropertyAccessEmbedOrderColumn(new ArrayList(IntegerPropertyAccessEmbed.LIST_UPDATE));
            findEntity.setListEnumeratedPropertyAccessEmbedOrderColumn(new ArrayList(EnumeratedPropertyAccessEmbed.LIST_UPDATE));
            findEntity.setListEnumeratedPropertyAccessEmbedOrderBy(new ArrayList(EnumeratedPropertyAccessEmbed.LIST_UPDATE));
            findEntity.setListTemporalPropertyAccessEmbedOrderColumn(new ArrayList(TemporalPropertyAccessEmbed.LIST_UPDATE));
            findEntity.getSetIntegerPropertyAccessEmbed().remove(new IntegerPropertyAccessEmbed(new Integer(1)));
            findEntity.getSetIntegerPropertyAccessEmbed().add(new IntegerPropertyAccessEmbed(new Integer(2))); // dup.
            findEntity.getMapKeyIntegerValueIntegerPropertyAccessEmbed().put(new Integer(4), new IntegerPropertyAccessEmbed(new Integer(400)));
            findEntity.setMapKeyIntegerValueTemporalPropertyAccessEmbed(new HashMap(TemporalPropertyAccessEmbed.MAP_UPDATE));
            findEntity.setMapKeyTemporalValueTemporalPropertyAccessEmbed(new HashMap(TemporalPropertyAccessEmbed.MAP2_UPDATE));
            findEntity.setMapKeyEnumeratedValueEnumeratedPropertyAccessEmbed(new HashMap(EnumeratedPropertyAccessEmbed.MAP_UPDATE));
            findEntity.getMapKeyIntegerEmbedValueIntegerEmbed().put(new IntegerEmbed(new Integer(4)), new IntegerEmbed(new Integer(400)));
            findEntity.setMapKeyLobEmbedValueLobEmbed(new HashMap(LobPropertyAccessEmbed.MAP_UPDATE));

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
            System.out.println("Performing find(" + JPAEmbeddableBasicEntity.class.getSimpleName() + ", " + id + ") operation");
            JPAEmbeddableBasicEntity updatedFindEntity = em.find(JPAEmbeddableBasicEntity.class, id);
            Assert.assertNotNull("find(" + JPAEmbeddableBasicEntity.class.getSimpleName() + ", " + id + ") did not return an entity.", updatedFindEntity);

            // Perform content verifications
            Assert.assertTrue(newEntity != updatedFindEntity);
            Assert.assertTrue(findEntity != updatedFindEntity);
            Assert.assertTrue(updatedFindEntity + " is not managed by the persistence context.", em.contains(updatedFindEntity));
            Assert.assertEquals(updatedFindEntity.getId(), findEntity.getId());
            Assert.assertEquals(findEntity.getIntegerEmbed(), updatedFindEntity.getIntegerEmbed());
            Assert.assertEquals(findEntity.getIntegerAttributeOverridesEmbed(),
                                updatedFindEntity.getIntegerAttributeOverridesEmbed());
            Assert.assertEquals(findEntity.getIntegerFieldAccessEmbed(),
                                updatedFindEntity.getIntegerFieldAccessEmbed());
            Assert.assertEquals(findEntity.getEnumeratedFieldAccessEmbed(),
                                updatedFindEntity.getEnumeratedFieldAccessEmbed());
            Assert.assertEquals(findEntity.getTemporalFieldAccessEmbed(),
                                updatedFindEntity.getTemporalFieldAccessEmbed());
            Assert.assertEquals(findEntity.getLobFieldAccessEmbed(), updatedFindEntity.getLobFieldAccessEmbed());

            /*
             * If an Embeddable class has only one field and that field is transient, Ecipselink doesn't
             * create an object of type Embeddable class and set its transient field null.
             */
            if (JPAProviderImpl.ECLIPSELINK.equals(provider)) {
                Assert.assertNull(updatedFindEntity.getIntegerTransientEmbed());
            } else if (JPAProviderImpl.OPENJPA.equals(provider)) {
                Assert.assertNull(updatedFindEntity.getIntegerTransientEmbed().getTransientJavaValue());
                Assert.assertNull(updatedFindEntity.getIntegerTransientEmbed().getTransientValue());
            }

            Assert.assertEquals(findEntity.getIntegerPropertyAccessEmbed(),
                                updatedFindEntity.getIntegerPropertyAccessEmbed());
            Assert.assertEquals(findEntity.getEnumeratedPropertyAccessEmbed(),
                                updatedFindEntity.getEnumeratedPropertyAccessEmbed());
            Assert.assertEquals(findEntity.getTemporalPropertyAccessEmbed(),
                                updatedFindEntity.getTemporalPropertyAccessEmbed());
            Assert.assertEquals(findEntity.getLobPropertyAccessEmbed(),
                                updatedFindEntity.getLobPropertyAccessEmbed());
            Assert.assertEquals(findEntity.getCollectionIntegerEmbed(),
                                updatedFindEntity.getCollectionIntegerEmbed());
            Assert.assertEquals(findEntity.getCollectionEnumeratedEmbed(),
                                updatedFindEntity.getCollectionEnumeratedEmbed());
            Assert.assertEquals(findEntity.getCollectionTemporalEmbed(),
                                updatedFindEntity.getCollectionTemporalEmbed());
            Assert.assertEquals(findEntity.getCollectionLobEmbed(),
                                updatedFindEntity.getCollectionLobEmbed());
            Assert.assertEquals(findEntity.getListIntegerOrderColumnEmbed(),
                                updatedFindEntity.getListIntegerOrderColumnEmbed());
            Assert.assertEquals(ListIntegerOrderByEmbed.UPDATE_ORDERED,
                                updatedFindEntity.getListIntegerOrderByEmbed().getNotListIntegerOrderBy());
            Assert.assertEquals(findEntity.getListEnumeratedEmbed(),
                                updatedFindEntity.getListEnumeratedEmbed());
            Assert.assertEquals(findEntity.getListTemporalEmbed(),
                                updatedFindEntity.getListTemporalEmbed());
            Assert.assertEquals(findEntity.getListLobEmbed(),
                                updatedFindEntity.getListLobEmbed());

            // Delete the entity from the database
            System.out.println("Performing remove(" + updatedFindEntity + ") operation");
            em.remove(updatedFindEntity);

            System.out.println("Committing transaction...");
            if (tj.isTransactionActive()) {
                tj.commitTransaction();
            }

            em.clear();

            // Verify the entity remove was successful
            System.out.println("Performing find(" + JPAEmbeddableBasicEntity.class.getSimpleName() + ", " + id + ") operation");
            Object findRemovedEntity = em.find(JPAEmbeddableBasicEntity.class, id);
            Assert.assertNull("find(" + JPAEmbeddableBasicEntity.class.getSimpleName() + ", " + id + ") returned an entity.", findRemovedEntity);
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

    @SuppressWarnings({ "unchecked", "rawtypes", "deprecation" })
    public void testEmbeddableBasic02(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        JPAProviderImpl provider = getJPAProviderImpl(jpaResource);

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            int id = 2;

            XMLJPAEmbeddableBasicEntity newEntity = new XMLJPAEmbeddableBasicEntity();
            newEntity.setId(id);
            newEntity.setIntegerEmbed(new XMLIntegerEmbed(11));
            newEntity.setIntegerAttributeOverridesEmbed(new XMLIntegerAttributeOverridesEmbed(22));
            newEntity.setIntegerFieldAccessEmbed(new XMLIntegerFieldAccessEmbed(33));
            newEntity.setEnumeratedFieldAccessEmbed(new XMLEnumeratedFieldAccessEmbed(XMLEnumeratedFieldAccessEnum.ONE));
            newEntity.setTemporalFieldAccessEmbed(new XMLTemporalFieldAccessEmbed(new Date()));
            newEntity.setLobFieldAccessEmbed(new XMLLobFieldAccessEmbed("InitFA"));
            newEntity.setIntegerTransientEmbed(new XMLIntegerTransientEmbed(44, 55));
            newEntity.setIntegerPropertyAccessEmbed(new XMLIntegerPropertyAccessEmbed(66));
            newEntity.setEnumeratedPropertyAccessEmbed(new XMLEnumeratedPropertyAccessEmbed(XMLEnumeratedPropertyAccessEnum.TWO));
            newEntity.setTemporalPropertyAccessEmbed(new XMLTemporalPropertyAccessEmbed(new Date()));
            newEntity.setLobPropertyAccessEmbed(new XMLLobPropertyAccessEmbed("InitPA"));
            newEntity.setCollectionIntegerEmbed(new XMLCollectionIntegerEmbed(XMLCollectionIntegerEmbed.INIT));
            newEntity.setCollectionEnumeratedEmbed(new XMLCollectionEnumeratedEmbed(XMLCollectionEnumeratedEmbed.INIT));
            newEntity.setCollectionTemporalEmbed(new XMLCollectionTemporalEmbed(XMLCollectionTemporalEmbed.INIT));
            newEntity.setCollectionLobEmbed(new XMLCollectionLobEmbed(XMLCollectionLobEmbed.INIT));
            newEntity.setListIntegerOrderColumnEmbed(new XMLListIntegerOrderColumnEmbed(XMLListIntegerOrderColumnEmbed.INIT));
            newEntity.setListIntegerOrderByEmbed(new XMLListIntegerOrderByEmbed(XMLListIntegerOrderByEmbed.INIT));
            newEntity.setListEnumeratedEmbed(new XMLListEnumeratedEmbed(XMLListEnumeratedEmbed.INIT));
            newEntity.setListTemporalEmbed(new XMLListTemporalEmbed(XMLListTemporalEmbed.INIT));
            newEntity.setListLobEmbed(new XMLListLobEmbed(XMLListLobEmbed.INIT));
            newEntity.setSetIntegerEmbed(new XMLSetIntegerEmbed(XMLSetIntegerEmbed.INIT));
            newEntity.setSetEnumeratedEmbed(new XMLSetEnumeratedEmbed(XMLSetEnumeratedEmbed.INIT));
            newEntity.setSetTemporalEmbed(new XMLSetTemporalEmbed(XMLSetTemporalEmbed.INIT));
            newEntity.setSetLobEmbed(new XMLSetLobEmbed(XMLSetLobEmbed.INIT));
            newEntity.setMapKeyIntegerEmbed(new XMLMapKeyIntegerEmbed(XMLMapKeyIntegerEmbed.INIT));
            newEntity.setMapKeyIntegerValueTemporalEmbed(new XMLMapKeyIntegerValueTemporalEmbed(XMLMapKeyIntegerValueTemporalEmbed.INIT));
            newEntity.setMapKeyEnumeratedValueEnumeratedEmbed(new XMLMapKeyEnumeratedValueEnumeratedEmbed(XMLMapKeyEnumeratedValueEnumeratedEmbed.INIT));
            newEntity.setMapKeyTemporalValueTemporalEmbed(new XMLMapKeyTemporalValueTemporalEmbed(XMLMapKeyTemporalValueTemporalEmbed.INIT));
            newEntity.setMapKeyEnumeratedValueLobEmbed(new XMLMapKeyEnumeratedValueLobEmbed(XMLMapKeyEnumeratedValueLobEmbed.INIT));
            newEntity.setCollectionLobPropertyAccessEmbed(new ArrayList(XMLLobPropertyAccessEmbed.COLLECTION_INIT));
            newEntity.setListIntegerEmbedOrderColumn(new ArrayList(XMLIntegerEmbed.LIST_INIT));
            newEntity.setListIntegerAttributeOverridesEmbedOrderColumn(new ArrayList(XMLIntegerAttributeOverridesEmbed.LIST_INIT));
            newEntity.setListIntegerPropertyAccessEmbedOrderColumn(new ArrayList(XMLIntegerPropertyAccessEmbed.LIST_INIT));
            newEntity.setListEnumeratedPropertyAccessEmbedOrderColumn(new ArrayList(XMLEnumeratedPropertyAccessEmbed.LIST_INIT));
            newEntity.setListEnumeratedPropertyAccessEmbedOrderBy(new ArrayList(XMLEnumeratedPropertyAccessEmbed.LIST_INIT));
            newEntity.setListTemporalPropertyAccessEmbedOrderColumn(new ArrayList(XMLTemporalPropertyAccessEmbed.LIST_INIT));
            newEntity.setSetIntegerPropertyAccessEmbed(new HashSet(XMLIntegerPropertyAccessEmbed.SET_INIT));
//            newEntity.setMapKeyIntegerValueIntegerPropertyAccessEmbed(new HashMap(XMLIntegerPropertyAccessEmbed.MAP_INIT));
            newEntity.setMapKeyIntegerValueTemporalPropertyAccessEmbed(new HashMap(XMLTemporalPropertyAccessEmbed.MAP_INIT));
            newEntity.setMapKeyTemporalValueTemporalPropertyAccessEmbed(new HashMap(XMLTemporalPropertyAccessEmbed.MAP2_INIT));
//            newEntity.setMapKeyEnumeratedValueEnumeratedPropertyAccessEmbed(new HashMap(XMLEnumeratedPropertyAccessEmbed.MAP_INIT));
//            newEntity.setMapKeyIntegerEmbedValueIntegerEmbed(new HashMap(XMLIntegerEmbed.MAP_INIT2));
//            newEntity.setMapKeyLobEmbedValueLobEmbed(new HashMap(XMLLobPropertyAccessEmbed.MAP_INIT));

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
            System.out.println("Performing find(" + JPAEmbeddableBasicEntity.class.getSimpleName() + ", " + id + ") operation");
            XMLJPAEmbeddableBasicEntity findEntity = em.find(XMLJPAEmbeddableBasicEntity.class, id);
            Assert.assertNotNull("find(" + JPAEmbeddableBasicEntity.class.getSimpleName() + ", " + id + ") did not return an entity.", findEntity);

            // Perform content verifications
            Assert.assertTrue(newEntity != findEntity);
            Assert.assertTrue(findEntity + " is not managed by the persistence context.", em.contains(findEntity));
            Assert.assertEquals(newEntity.getId(), findEntity.getId());
            Assert.assertEquals(newEntity.getIntegerEmbed(), findEntity.getIntegerEmbed());
            Assert.assertEquals(newEntity.getIntegerAttributeOverridesEmbed(),
                                findEntity.getIntegerAttributeOverridesEmbed());
            Assert.assertEquals(newEntity.getIntegerFieldAccessEmbed(),
                                findEntity.getIntegerFieldAccessEmbed());
            Assert.assertEquals(newEntity.getEnumeratedFieldAccessEmbed(),
                                findEntity.getEnumeratedFieldAccessEmbed());
            Assert.assertEquals(newEntity.getTemporalFieldAccessEmbed(), findEntity.getTemporalFieldAccessEmbed());
            Assert.assertEquals(newEntity.getLobFieldAccessEmbed(), findEntity.getLobFieldAccessEmbed());

            /*
             * If an Embeddable class has only one field and that field is transient, Ecipselink doesn't
             * create an object of type Embeddable class and set its transient field null.
             */
            if (JPAProviderImpl.ECLIPSELINK.equals(provider)) {
                Assert.assertNull(findEntity.getIntegerTransientEmbed());
            } else if (JPAProviderImpl.OPENJPA.equals(provider)) {
                Assert.assertNull(findEntity.getIntegerTransientEmbed().getTransientJavaValue());
                Assert.assertNull(findEntity.getIntegerTransientEmbed().getTransientValue());
            }

            Assert.assertEquals(newEntity.getIntegerPropertyAccessEmbed(),
                                findEntity.getIntegerPropertyAccessEmbed());
            Assert.assertEquals(newEntity.getEnumeratedPropertyAccessEmbed(),
                                findEntity.getEnumeratedPropertyAccessEmbed());
            Assert.assertEquals(newEntity.getTemporalPropertyAccessEmbed(),
                                findEntity.getTemporalPropertyAccessEmbed());
            Assert.assertEquals(newEntity.getLobPropertyAccessEmbed(), findEntity.getLobPropertyAccessEmbed());
            Assert.assertEquals(newEntity.getCollectionIntegerEmbed(),
                                findEntity.getCollectionIntegerEmbed());
            Assert.assertEquals(newEntity.getCollectionEnumeratedEmbed(),
                                findEntity.getCollectionEnumeratedEmbed());
            Assert.assertEquals(newEntity.getCollectionTemporalEmbed(),
                                findEntity.getCollectionTemporalEmbed());
            Assert.assertEquals(newEntity.getCollectionLobEmbed(),
                                findEntity.getCollectionLobEmbed());
            Assert.assertEquals(newEntity.getListIntegerOrderColumnEmbed(),
                                findEntity.getListIntegerOrderColumnEmbed());
            Assert.assertEquals(XMLListIntegerOrderByEmbed.INIT_ORDERED, findEntity.getListIntegerOrderByEmbed().getNotListIntegerOrderBy());
            Assert.assertEquals(newEntity.getListEnumeratedEmbed(),
                                findEntity.getListEnumeratedEmbed());
            Assert.assertEquals(newEntity.getListTemporalEmbed(),
                                findEntity.getListTemporalEmbed());
            Assert.assertEquals(newEntity.getListLobEmbed(),
                                findEntity.getListLobEmbed());
            Assert.assertEquals(newEntity.getSetIntegerEmbed(), findEntity.getSetIntegerEmbed());
            Assert.assertEquals(newEntity.getSetEnumeratedEmbed(), findEntity.getSetEnumeratedEmbed());
            Assert.assertEquals(newEntity.getSetTemporalEmbed(), findEntity.getSetTemporalEmbed());
            Assert.assertEquals(newEntity.getSetLobEmbed(), findEntity.getSetLobEmbed());
            Assert.assertEquals(newEntity.getMapKeyIntegerEmbed(), findEntity.getMapKeyIntegerEmbed());
            Assert.assertEquals(newEntity.getMapKeyIntegerValueTemporalEmbed(),
                                findEntity.getMapKeyIntegerValueTemporalEmbed());
            Assert.assertEquals(newEntity.getMapKeyEnumeratedValueEnumeratedEmbed(),
                                findEntity.getMapKeyEnumeratedValueEnumeratedEmbed());
            Assert.assertEquals(newEntity.getMapKeyTemporalValueTemporalEmbed(),
                                findEntity.getMapKeyTemporalValueTemporalEmbed());
            Assert.assertEquals(newEntity.getMapKeyEnumeratedValueLobEmbed(),
                                findEntity.getMapKeyEnumeratedValueLobEmbed());
            Assert.assertEquals(newEntity.getCollectionLobPropertyAccessEmbed(),
                                findEntity.getCollectionLobPropertyAccessEmbed());
            Assert.assertEquals(newEntity.getListIntegerEmbedOrderColumn(),
                                findEntity.getListIntegerEmbedOrderColumn());
            Assert.assertEquals(newEntity.getListIntegerAttributeOverridesEmbedOrderColumn(), findEntity.getListIntegerAttributeOverridesEmbedOrderColumn());
            Assert.assertEquals(newEntity.getListIntegerPropertyAccessEmbedOrderColumn(), findEntity.getListIntegerPropertyAccessEmbedOrderColumn());
            Assert.assertEquals(newEntity.getListEnumeratedPropertyAccessEmbedOrderColumn(), findEntity.getListEnumeratedPropertyAccessEmbedOrderColumn());
            Assert.assertEquals(XMLEnumeratedPropertyAccessEmbed.LIST_INIT_ORDERED,
                                findEntity.getListEnumeratedPropertyAccessEmbedOrderBy());
            Assert.assertEquals(newEntity.getListTemporalPropertyAccessEmbedOrderColumn(), findEntity.getListTemporalPropertyAccessEmbedOrderColumn());
            Assert.assertEquals(newEntity.getSetIntegerPropertyAccessEmbed(),
                                findEntity.getSetIntegerPropertyAccessEmbed());
//            Assert.assertEquals(newEntity.getMapKeyIntegerValueIntegerPropertyAccessEmbed(), findEntity.getMapKeyIntegerValueIntegerPropertyAccessEmbed());
            Assert.assertEquals(newEntity.getMapKeyIntegerValueTemporalPropertyAccessEmbed(), findEntity.getMapKeyIntegerValueTemporalPropertyAccessEmbed());
            Assert.assertEquals(newEntity.getMapKeyTemporalValueTemporalPropertyAccessEmbed(),
                                findEntity.getMapKeyTemporalValueTemporalPropertyAccessEmbed());
//            Assert.assertEquals(newEntity.getMapKeyEnumeratedValueEnumeratedPropertyAccessEmbed(),
//                                findEntity.getMapKeyEnumeratedValueEnumeratedPropertyAccessEmbed());
//            Assert.assertEquals(newEntity.getMapKeyIntegerEmbedValueIntegerEmbed(),
//                                findEntity.getMapKeyIntegerEmbedValueIntegerEmbed());
//            Assert.assertEquals(newEntity.getMapKeyLobEmbedValueLobEmbed(),
//                                findEntity.getMapKeyLobEmbedValueLobEmbed());

            // Update the entity
            findEntity.getIntegerEmbed().setIntegerValue(111);
            findEntity.getIntegerAttributeOverridesEmbed().setNotIntegerValue(222);
            findEntity.getIntegerFieldAccessEmbed().setIntegerValueFieldAccessColumn(333);
            findEntity.getEnumeratedFieldAccessEmbed().setEnumeratedStringValueFA(XMLEnumeratedFieldAccessEnum.TWO);
            findEntity.getEnumeratedFieldAccessEmbed().setEnumeratedOrdinalValueFA(XMLEnumeratedFieldAccessEnum.TWO);
            findEntity.getTemporalFieldAccessEmbed().setTemporalValueFA(new Date(25000));
            findEntity.getLobFieldAccessEmbed().setClobValueFA("UpdateFA");

            /*
             * If an Embeddable class has only one field and that field is transient, Ecipselink doesn't
             * create an object of type Embeddable class and set its transient field null.
             */
            if (JPAProviderImpl.ECLIPSELINK.equals(provider)) {
                findEntity.setIntegerTransientEmbed(new XMLIntegerTransientEmbed());
            }
            findEntity.getIntegerTransientEmbed().setTransientJavaValue(444);
            findEntity.getIntegerTransientEmbed().setTransientValue(555);

            findEntity.getIntegerPropertyAccessEmbed().setIntegerValuePropertyAccessColumn(666);
            findEntity.getEnumeratedPropertyAccessEmbed().setEnumeratedStringValuePA(XMLEnumeratedPropertyAccessEnum.THREE);
            findEntity.getEnumeratedPropertyAccessEmbed().setEnumeratedOrdinalValuePA(XMLEnumeratedPropertyAccessEnum.THREE);
            findEntity.getTemporalPropertyAccessEmbed().setTemporalValuePA(new Date(25000));
            findEntity.getLobPropertyAccessEmbed().setClobValuePA("UpdatePA");
            findEntity.getCollectionIntegerEmbed().setCollectionInteger(XMLCollectionIntegerEmbed.UPDATE);
            findEntity.getCollectionEnumeratedEmbed().setCollectionEnumerated(XMLCollectionEnumeratedEmbed.UPDATE);
            findEntity.getCollectionTemporalEmbed().setCollectionDate(XMLCollectionTemporalEmbed.UPDATE);
            findEntity.getCollectionLobEmbed().setCollectionLob(XMLCollectionLobEmbed.UPDATE);
            findEntity.getListIntegerOrderColumnEmbed().setNotListIntegerOrderColumn(XMLListIntegerOrderColumnEmbed.UPDATE);
            findEntity.getListIntegerOrderByEmbed().setNotListIntegerOrderBy(XMLListIntegerOrderByEmbed.UPDATE);
            findEntity.getListEnumeratedEmbed().setListEnumerated(XMLListEnumeratedEmbed.UPDATE);
            findEntity.getListTemporalEmbed().setListDate(XMLListTemporalEmbed.UPDATE);
            findEntity.getListLobEmbed().setListLob(XMLListLobEmbed.UPDATE);
            findEntity.getSetIntegerEmbed().getNotSetInteger().remove(new Integer(1));
            findEntity.getSetIntegerEmbed().getNotSetInteger().add(new Integer(2)); // dup.
            findEntity.getSetEnumeratedEmbed().setSetEnumerated(XMLSetEnumeratedEmbed.UPDATE);
            findEntity.getSetTemporalEmbed().setSetDate(XMLSetTemporalEmbed.UPDATE);
            findEntity.getSetLobEmbed().setSetLob(XMLSetLobEmbed.UPDATE);
            findEntity.getMapKeyIntegerEmbed().getNotMapKeyInteger().put(new Integer(4), new Integer(400));
            findEntity.getMapKeyIntegerValueTemporalEmbed().setMapKeyIntegerValueTemporal(MapKeyIntegerValueTemporalEmbed.UPDATE);
            findEntity.getMapKeyEnumeratedValueEnumeratedEmbed().setMapKeyEnumeratedValueEnumerated(XMLMapKeyEnumeratedValueEnumeratedEmbed.UPDATE);
            findEntity.getMapKeyTemporalValueTemporalEmbed().setMapKeyTemporalValueTemporal(XMLMapKeyTemporalValueTemporalEmbed.UPDATE);
            findEntity.getMapKeyEnumeratedValueLobEmbed().setMapKeyEnumeratedValueLob(XMLMapKeyEnumeratedValueLobEmbed.UPDATE);
            findEntity.setCollectionLobPropertyAccessEmbed(new ArrayList(XMLLobPropertyAccessEmbed.COLLECTION_UPDATE));
            findEntity.setListIntegerEmbedOrderColumn(new ArrayList(XMLIntegerEmbed.LIST_UPDATE));
            findEntity.setListIntegerAttributeOverridesEmbedOrderColumn(new ArrayList(XMLIntegerAttributeOverridesEmbed.LIST_UPDATE));
            findEntity.setListIntegerPropertyAccessEmbedOrderColumn(new ArrayList(XMLIntegerPropertyAccessEmbed.LIST_UPDATE));
            findEntity.setListEnumeratedPropertyAccessEmbedOrderColumn(new ArrayList(XMLEnumeratedPropertyAccessEmbed.LIST_UPDATE));
            findEntity.setListEnumeratedPropertyAccessEmbedOrderBy(new ArrayList(XMLEnumeratedPropertyAccessEmbed.LIST_UPDATE));
            findEntity.setListTemporalPropertyAccessEmbedOrderColumn(new ArrayList(XMLTemporalPropertyAccessEmbed.LIST_UPDATE));
            findEntity.getSetIntegerPropertyAccessEmbed().remove(new XMLIntegerPropertyAccessEmbed(new Integer(1)));
            findEntity.getSetIntegerPropertyAccessEmbed().add(new XMLIntegerPropertyAccessEmbed(new Integer(2))); // dup.
//            findEntity.getMapKeyIntegerValueIntegerPropertyAccessEmbed().put(new Integer(4), new XMLIntegerPropertyAccessEmbed(new Integer(400)));
            findEntity.setMapKeyIntegerValueTemporalPropertyAccessEmbed(new HashMap(XMLTemporalPropertyAccessEmbed.MAP_UPDATE));
            findEntity.setMapKeyTemporalValueTemporalPropertyAccessEmbed(new HashMap(XMLTemporalPropertyAccessEmbed.MAP2_UPDATE));
//            findEntity.setMapKeyEnumeratedValueEnumeratedPropertyAccessEmbed(new HashMap(XMLEnumeratedPropertyAccessEmbed.MAP_UPDATE));
//            findEntity.getMapKeyIntegerEmbedValueIntegerEmbed().put(new XMLIntegerEmbed(new Integer(4)), new XMLIntegerEmbed(new Integer(400)));
//            findEntity.setMapKeyLobEmbedValueLobEmbed(new HashMap(XMLLobPropertyAccessEmbed.MAP_UPDATE));

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
            System.out.println("Performing find(" + XMLJPAEmbeddableBasicEntity.class.getSimpleName() + ", " + id + ") operation");
            XMLJPAEmbeddableBasicEntity updatedFindEntity = em.find(XMLJPAEmbeddableBasicEntity.class, id);
            Assert.assertNotNull("find(" + XMLJPAEmbeddableBasicEntity.class.getSimpleName() + ", " + id + ") did not return an entity.", updatedFindEntity);

            // Perform content verifications
            Assert.assertTrue(newEntity != updatedFindEntity);
            Assert.assertTrue(findEntity != updatedFindEntity);
            Assert.assertTrue(updatedFindEntity + " is not managed by the persistence context.", em.contains(updatedFindEntity));
            Assert.assertEquals(updatedFindEntity.getId(), findEntity.getId());
            Assert.assertEquals(findEntity.getIntegerEmbed(), updatedFindEntity.getIntegerEmbed());
            Assert.assertEquals(findEntity.getIntegerAttributeOverridesEmbed(),
                                updatedFindEntity.getIntegerAttributeOverridesEmbed());
            Assert.assertEquals(findEntity.getIntegerFieldAccessEmbed(),
                                updatedFindEntity.getIntegerFieldAccessEmbed());
            Assert.assertEquals(findEntity.getEnumeratedFieldAccessEmbed(),
                                updatedFindEntity.getEnumeratedFieldAccessEmbed());
            Assert.assertEquals(findEntity.getTemporalFieldAccessEmbed(),
                                updatedFindEntity.getTemporalFieldAccessEmbed());
            Assert.assertEquals(findEntity.getLobFieldAccessEmbed(),
                                updatedFindEntity.getLobFieldAccessEmbed());

            /*
             * If an Embeddable class has only one field and that field is transient, Ecipselink doesn't
             * create an object of type Embeddable class and set its transient field null.
             */
            if (JPAProviderImpl.ECLIPSELINK.equals(provider)) {
                Assert.assertNull(updatedFindEntity.getIntegerTransientEmbed());
            } else if (JPAProviderImpl.OPENJPA.equals(provider)) {
                Assert.assertNull(updatedFindEntity.getIntegerTransientEmbed().getTransientJavaValue());
                Assert.assertNull(updatedFindEntity.getIntegerTransientEmbed().getTransientValue());
            }

            Assert.assertEquals(findEntity.getIntegerPropertyAccessEmbed(),
                                updatedFindEntity.getIntegerPropertyAccessEmbed());
            Assert.assertEquals(findEntity.getEnumeratedPropertyAccessEmbed(),
                                updatedFindEntity.getEnumeratedPropertyAccessEmbed());
            Assert.assertEquals(findEntity.getTemporalPropertyAccessEmbed(),
                                updatedFindEntity.getTemporalPropertyAccessEmbed());
            Assert.assertEquals(findEntity.getLobPropertyAccessEmbed(),
                                updatedFindEntity.getLobPropertyAccessEmbed());
            Assert.assertEquals(findEntity.getCollectionIntegerEmbed(),
                                updatedFindEntity.getCollectionIntegerEmbed());
            Assert.assertEquals(findEntity.getCollectionEnumeratedEmbed(),
                                updatedFindEntity.getCollectionEnumeratedEmbed());
            Assert.assertEquals(findEntity.getCollectionTemporalEmbed(),
                                updatedFindEntity.getCollectionTemporalEmbed());
            Assert.assertEquals(findEntity.getCollectionLobEmbed(),
                                updatedFindEntity.getCollectionLobEmbed());
            Assert.assertEquals(findEntity.getListIntegerOrderColumnEmbed(),
                                updatedFindEntity.getListIntegerOrderColumnEmbed());
            Assert.assertEquals(XMLListIntegerOrderByEmbed.UPDATE_ORDERED,
                                updatedFindEntity.getListIntegerOrderByEmbed().getNotListIntegerOrderBy());
            Assert.assertEquals(findEntity.getListEnumeratedEmbed(),
                                updatedFindEntity.getListEnumeratedEmbed());
            Assert.assertEquals(findEntity.getListTemporalEmbed(),
                                updatedFindEntity.getListTemporalEmbed());
            Assert.assertEquals(findEntity.getListLobEmbed(),
                                updatedFindEntity.getListLobEmbed());
            Assert.assertEquals(findEntity.getSetIntegerEmbed(), updatedFindEntity.getSetIntegerEmbed());
            Assert.assertEquals(findEntity.getSetEnumeratedEmbed(), updatedFindEntity.getSetEnumeratedEmbed());
            Assert.assertEquals(findEntity.getSetTemporalEmbed(), updatedFindEntity.getSetTemporalEmbed());
            Assert.assertEquals(findEntity.getSetLobEmbed(), updatedFindEntity.getSetLobEmbed());
            Assert.assertEquals(findEntity.getMapKeyIntegerEmbed(),
                                updatedFindEntity.getMapKeyIntegerEmbed());
            Assert.assertEquals(findEntity.getMapKeyIntegerValueTemporalEmbed(),
                                updatedFindEntity.getMapKeyIntegerValueTemporalEmbed());
            Assert.assertEquals(findEntity.getMapKeyEnumeratedValueEnumeratedEmbed(), updatedFindEntity.getMapKeyEnumeratedValueEnumeratedEmbed());
            Assert.assertEquals(findEntity.getMapKeyTemporalValueTemporalEmbed(),
                                updatedFindEntity.getMapKeyTemporalValueTemporalEmbed());
            Assert.assertEquals(findEntity.getMapKeyEnumeratedValueLobEmbed(),
                                updatedFindEntity.getMapKeyEnumeratedValueLobEmbed());
            Assert.assertEquals(findEntity.getCollectionLobPropertyAccessEmbed(), updatedFindEntity.getCollectionLobPropertyAccessEmbed());
            Assert.assertEquals(findEntity.getListIntegerEmbedOrderColumn(),
                                updatedFindEntity.getListIntegerEmbedOrderColumn());
            Assert.assertEquals(findEntity.getListIntegerAttributeOverridesEmbedOrderColumn(), updatedFindEntity.getListIntegerAttributeOverridesEmbedOrderColumn());
            Assert.assertEquals(findEntity.getListIntegerPropertyAccessEmbedOrderColumn(), updatedFindEntity.getListIntegerPropertyAccessEmbedOrderColumn());
            Assert.assertEquals(findEntity.getListEnumeratedPropertyAccessEmbedOrderColumn(), updatedFindEntity.getListEnumeratedPropertyAccessEmbedOrderColumn());
            Assert.assertEquals(XMLEnumeratedPropertyAccessEmbed.LIST_UPDATE_ORDERED, updatedFindEntity.getListEnumeratedPropertyAccessEmbedOrderBy());
            Assert.assertEquals(findEntity.getListTemporalPropertyAccessEmbedOrderColumn(), updatedFindEntity.getListTemporalPropertyAccessEmbedOrderColumn());
            Assert.assertEquals(findEntity.getSetIntegerPropertyAccessEmbed(),
                                updatedFindEntity.getSetIntegerPropertyAccessEmbed());
//            Assert.assertEquals(findEntity.getMapKeyIntegerValueIntegerPropertyAccessEmbed(), updatedFindEntity.getMapKeyIntegerValueIntegerPropertyAccessEmbed());
            Assert.assertEquals(findEntity.getMapKeyIntegerValueTemporalPropertyAccessEmbed(),
                                updatedFindEntity.getMapKeyIntegerValueTemporalPropertyAccessEmbed());
            Assert.assertEquals(findEntity.getMapKeyTemporalValueTemporalPropertyAccessEmbed(),
                                updatedFindEntity.getMapKeyTemporalValueTemporalPropertyAccessEmbed());
//            Assert.assertEquals(findEntity.getMapKeyEnumeratedValueEnumeratedPropertyAccessEmbed(),
//                                updatedFindEntity.getMapKeyEnumeratedValueEnumeratedPropertyAccessEmbed());
//            Assert.assertEquals(findEntity.getMapKeyIntegerEmbedValueIntegerEmbed(), updatedFindEntity.getMapKeyIntegerEmbedValueIntegerEmbed());
//            Assert.assertEquals(findEntity.getMapKeyLobEmbedValueLobEmbed(),
//                                updatedFindEntity.getMapKeyLobEmbedValueLobEmbed());

            // Delete the entity from the database
            System.out.println("Performing remove(" + updatedFindEntity + ") operation");
            em.remove(updatedFindEntity);

            System.out.println("Committing transaction...");
            if (tj.isTransactionActive()) {
                tj.commitTransaction();
            }

            em.clear();

            // Verify the entity remove was successful
            System.out.println("Performing find(" + XMLJPAEmbeddableBasicEntity.class.getSimpleName() + ", " + id + ") operation");
            Object findRemovedEntity = em.find(XMLJPAEmbeddableBasicEntity.class, id);
            Assert.assertNull("find(" + XMLJPAEmbeddableBasicEntity.class.getSimpleName() + ", " + id + ") returned an entity.", findRemovedEntity);
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
     * Test Logic: testEmbeddableBasic03
     *
     * <p>
     * JPA 2.0 Specifications Tested (per V2 spec):
     * <ul>
     * <li>4.4.2 Identification Variables
     * <li>4.4.4 Path Expressions
     * <li>4.4.6 Collection Member Declarations
     * <li>4.6.3 Conditional Expressions - Path Expressions
     * <li>4.6.16 Subqueries
     * <li>4.8.4 Embeddables in the Query Result
     * </ul>
     *
     * <p>
     * Description: Validates embeddable path expressions using both queries and
     * subqueries with JDBC mode:
     * <ol>
     * <li>Create a new initialized entity, and persist it to the database.
     * <li>Verify the entity was saved to the database. This verification
     * ensures <b>both</b> annotations and XML on these navigations are
     * respected:
     * <ol>
     * <li>Embedded defaulted object.
     * <li>AttributeOverrides of embedded basic type.
     * <li>Embedded field access with Column override.
     * <li>Embedded property access with Column override (mixing field/property
     * access). CollectionTable, Column, and sorted by OrderColumn.
     * <li>Embedded Collection&lt;Integer&gt; using ElementCollection,
     * CollectionTable, Column, and sorted by OrderColumn.
     * <li>Embedded List&lt;Integer&gt; using ElementCollection,
     * CollectionTable, Column, and sorted by OrderColumn.
     * <li>Embedded Set&lt;Integer&gt; using ElementCollection, CollectionTable,
     * and Column.
     * <li>Embedded Map&lt;Integer,Integer&gt; using ElementCollection,
     * CollectionTable, MapKeyColumn and Column.
     * <li>List&lt;Embeddable.F/A Integer&gt; using ElementCollection,
     * CollectionTable, and sorted by OrderColumn.
     * <li>List&lt;Embeddable.F/A Integer&gt; using ElementCollection,
     * CollectionTable, AttributeOverrides, and sorted by OrderColumn.
     * <li>List&lt;Embeddable.P/A Integer w/ Column&gt; using ElementCollection,
     * CollectionTable, and sorted by OrderColumn (mixing field/property
     * access).
     * <li>Set&lt;Embeddable.P/A Integer&gt; using ElementCollection and
     * CollectionTable (mixing field/property access).
     * <li>Map&lt;Integer,Embeddable.P/A Integer&gt; using ElementCollection,
     * CollectionTable, MapKeyColumn, and AttributeOverrides on value w/ column
     * def (mixing field/property access).
     * <li>Map&lt;Embeddable.F/A Integer,Embeddable.F/A Integer&gt; using
     * ElementCollection, CollectionTable, and AttributeOverrides on key and
     * value.
     * </ol>
     * </ol>
     */
    @SuppressWarnings({ "unchecked", "rawtypes", "deprecation" })
    public void testEmbeddableBasic03(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            int id = 3;

            JPAEmbeddableBasicEntity newEntity = new JPAEmbeddableBasicEntity();
            newEntity.setId(id);
            newEntity.setIntegerEmbed(new IntegerEmbed(11));
            newEntity.setIntegerAttributeOverridesEmbed(new IntegerAttributeOverridesEmbed(22));
            newEntity.setIntegerFieldAccessEmbed(new IntegerFieldAccessEmbed(33));
            newEntity.setIntegerPropertyAccessEmbed(new IntegerPropertyAccessEmbed(66));
            newEntity.setCollectionIntegerEmbed(new CollectionIntegerEmbed(CollectionIntegerEmbed.INIT));
            newEntity.setListIntegerOrderColumnEmbed(new ListIntegerOrderColumnEmbed(ListIntegerOrderColumnEmbed.INIT));
            newEntity.setSetIntegerEmbed(new SetIntegerEmbed(SetIntegerEmbed.INIT));
            newEntity.setMapKeyIntegerEmbed(new MapKeyIntegerEmbed(MapKeyIntegerEmbed.INIT));
            newEntity.setListIntegerEmbedOrderColumn(new ArrayList(IntegerEmbed.LIST_INIT));
            newEntity.setListIntegerAttributeOverridesEmbedOrderColumn(new ArrayList(IntegerAttributeOverridesEmbed.LIST_INIT));
            newEntity.setListIntegerPropertyAccessEmbedOrderColumn(new ArrayList(IntegerPropertyAccessEmbed.LIST_INIT));
            newEntity.setSetIntegerPropertyAccessEmbed(new HashSet(IntegerPropertyAccessEmbed.SET_INIT));
            newEntity.setMapKeyIntegerValueIntegerPropertyAccessEmbed(new HashMap(IntegerPropertyAccessEmbed.MAP_INIT));
            newEntity.setMapKeyIntegerEmbedValueIntegerEmbed(new HashMap(IntegerEmbed.MAP_INIT2));

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

            // Validate integerEmbed
            // Implementation note: DISTINCT is required b/c an optimization is NOT provided to eliminate multiple SQLs. See CMVC 617252.
            String entityName = JPAEmbeddableBasicEntity.class.getSimpleName();
            Query queryEmbed = em.createQuery("SELECT DISTINCT e FROM " + entityName + " x JOIN x.integerEmbed e WHERE e.integerValue = 11");
            List resultEmbed = queryEmbed.getResultList();
            Assert.assertNotNull(resultEmbed);
            Assert.assertTrue(resultEmbed.size() == 1);
            Assert.assertEquals(newEntity.getIntegerEmbed(), resultEmbed.get(0));
            Query subqueryEmbed = em.createQuery("SELECT DISTINCT e FROM " + entityName + " x JOIN x.integerEmbed e WHERE EXISTS ( SELECT DISTINCT f.integerValue FROM "
                                                 + entityName + " y JOIN y.integerEmbed f WHERE f.integerValue = 11 )");
            resultEmbed = subqueryEmbed.getResultList();
            Assert.assertNotNull(resultEmbed);
            Assert.assertTrue(resultEmbed.size() == 1);
            Assert.assertEquals(newEntity.getIntegerEmbed(), resultEmbed.get(0));

            // Validate integerAttributeOverridesEmbed
            // Implementation note: DISTINCT is required b/c an optimization is NOT provided to eliminate multiple SQLs. See CMVC 617252.
            queryEmbed = em.createQuery("SELECT DISTINCT e FROM " + entityName + " x JOIN x.integerAttributeOverridesEmbed e WHERE e.notIntegerValue = 22");
            resultEmbed = queryEmbed.getResultList();
            Assert.assertNotNull(resultEmbed);
            Assert.assertTrue(resultEmbed.size() == 1);
            Assert.assertEquals(newEntity.getIntegerAttributeOverridesEmbed(), resultEmbed.get(0));
            subqueryEmbed = em.createQuery("SELECT DISTINCT e FROM " + entityName
                                           + " x JOIN x.integerAttributeOverridesEmbed e WHERE EXISTS ( SELECT DISTINCT f.notIntegerValue FROM " + entityName
                                           + " y JOIN y.integerAttributeOverridesEmbed f WHERE f.notIntegerValue = 22 )");
            resultEmbed = subqueryEmbed.getResultList();
            Assert.assertNotNull(resultEmbed);
            Assert.assertTrue(resultEmbed.size() == 1);
            Assert.assertEquals(newEntity.getIntegerAttributeOverridesEmbed(), resultEmbed.get(0));

            // Validate integerFieldAccessEmbed
            // Implementation note: DISTINCT is required b/c an optimization is NOT provided to eliminate multiple SQLs. See CMVC 617252.
            queryEmbed = em.createQuery("SELECT DISTINCT e FROM " + entityName + " x JOIN x.integerFieldAccessEmbed e WHERE e.integerValueFieldAccessColumn = 33");
            resultEmbed = queryEmbed.getResultList();
            Assert.assertNotNull(resultEmbed);
            Assert.assertTrue(resultEmbed.size() == 1);
            Assert.assertEquals(newEntity.getIntegerFieldAccessEmbed(), resultEmbed.get(0));
            subqueryEmbed = em.createQuery("SELECT DISTINCT e FROM " + entityName
                                           + " x JOIN x.integerFieldAccessEmbed e WHERE EXISTS ( SELECT DISTINCT f.integerValueFieldAccessColumn FROM " + entityName
                                           + " y JOIN y.integerFieldAccessEmbed f WHERE f.integerValueFieldAccessColumn = 33 )");
            resultEmbed = subqueryEmbed.getResultList();
            Assert.assertNotNull(resultEmbed);
            Assert.assertTrue(resultEmbed.size() == 1);
            Assert.assertEquals(newEntity.getIntegerFieldAccessEmbed(), resultEmbed.get(0));

            // Validate integerPropertyAccessEmbed
            // Implementation note: DISTINCT is required b/c an optimization is NOT provided to eliminate multiple SQLs. See CMVC 617252.
            queryEmbed = em.createQuery("SELECT DISTINCT e FROM " + entityName + " x , IN(x.integerPropertyAccessEmbed) e WHERE e.integerValuePropertyAccessColumn = 66");
            resultEmbed = queryEmbed.getResultList();
            Assert.assertNotNull(resultEmbed);
            Assert.assertTrue(resultEmbed.size() == 1);
            Assert.assertEquals(newEntity.getIntegerPropertyAccessEmbed(), resultEmbed.get(0));
            subqueryEmbed = em.createQuery("SELECT DISTINCT e FROM " + entityName
                                           + " x , IN(x.integerPropertyAccessEmbed) e WHERE EXISTS ( SELECT DISTINCT f.integerValuePropertyAccessColumn FROM " + entityName
                                           + " y , IN(y.integerPropertyAccessEmbed) f WHERE f.integerValuePropertyAccessColumn = 66 )");
            resultEmbed = subqueryEmbed.getResultList();
            Assert.assertNotNull(resultEmbed);
            Assert.assertTrue(resultEmbed.size() == 1);
            Assert.assertEquals(newEntity.getIntegerPropertyAccessEmbed(), resultEmbed.get(0));

            // Validate collectionIntegerEmbed
            // Implementation note: DISTINCT is required b/c an optimization is NOT provided to eliminate multiple SQLs. See CMVC 617252.
            queryEmbed = em.createQuery("SELECT DISTINCT e FROM " + entityName + " x JOIN x.collectionIntegerEmbed e JOIN e.collectionInteger i WHERE i = 2");
            resultEmbed = queryEmbed.getResultList();
            Assert.assertNotNull(resultEmbed);
            Assert.assertTrue(resultEmbed.size() == 1);
            Assert.assertEquals(newEntity.getCollectionIntegerEmbed(), resultEmbed.get(0));
            subqueryEmbed = em.createQuery("SELECT DISTINCT e FROM " + entityName
                                           + " x JOIN x.collectionIntegerEmbed e JOIN e.collectionInteger i WHERE EXISTS ( SELECT DISTINCT f.collectionInteger FROM "
                                           + entityName
                                           + " y JOIN y.collectionIntegerEmbed f JOIN f.collectionInteger j WHERE j = 2 )");
            resultEmbed = subqueryEmbed.getResultList();
            Assert.assertNotNull(resultEmbed);
            Assert.assertTrue(resultEmbed.size() == 1);
            Assert.assertEquals(newEntity.getCollectionIntegerEmbed(), resultEmbed.get(0));

            // Validate listIntegerOrderColumnEmbed
            // Implementation note: DISTINCT is required b/c an optimization is NOT provided to eliminate multiple SQLs. See CMVC 617252.
            queryEmbed = em.createQuery("SELECT DISTINCT e FROM " + entityName + " x , IN(x.listIntegerOrderColumnEmbed) e , IN(e.notListIntegerOrderColumn) i WHERE i = 3");
            resultEmbed = queryEmbed.getResultList();
            Assert.assertNotNull(resultEmbed);
            Assert.assertTrue(resultEmbed.size() == 1);
            Assert.assertEquals(newEntity.getListIntegerOrderColumnEmbed(), resultEmbed.get(0));
            subqueryEmbed = em.createQuery("SELECT DISTINCT e FROM "
                                           + entityName
                                           + " x , IN(x.listIntegerOrderColumnEmbed) e , IN(e.notListIntegerOrderColumn) i WHERE EXISTS ( SELECT DISTINCT f.notListIntegerOrderColumn FROM "
                                           + entityName
                                           + " y , IN(y.listIntegerOrderColumnEmbed) f , IN(f.notListIntegerOrderColumn) j WHERE j = 3 )");
            resultEmbed = subqueryEmbed.getResultList();
            Assert.assertNotNull(resultEmbed);
            Assert.assertTrue(resultEmbed.size() == 1);
            Assert.assertEquals(newEntity.getListIntegerOrderColumnEmbed(), resultEmbed.get(0));

            // Validate setIntegerEmbed
            // Implementation note: DISTINCT is required b/c an optimization is NOT provided to eliminate multiple SQLs. See CMVC 617252.
            queryEmbed = em.createQuery("SELECT DISTINCT e FROM " + entityName + " x JOIN  x.setIntegerEmbed e JOIN e.notSetInteger i WHERE i = 1");
            resultEmbed = queryEmbed.getResultList();
            Assert.assertNotNull(resultEmbed);
            Assert.assertTrue(resultEmbed.size() == 1);
            Assert.assertEquals(newEntity.getSetIntegerEmbed(), resultEmbed.get(0));
            subqueryEmbed = em.createQuery("SELECT DISTINCT e FROM " + entityName
                                           + " x JOIN  x.setIntegerEmbed e JOIN e.notSetInteger i WHERE EXISTS ( SELECT DISTINCT f.notSetInteger FROM " + entityName
                                           + " y JOIN  y.setIntegerEmbed f JOIN f.notSetInteger j WHERE j = 1 )");
            resultEmbed = subqueryEmbed.getResultList();
            Assert.assertNotNull(resultEmbed);
            Assert.assertTrue(resultEmbed.size() == 1);
            Assert.assertEquals(newEntity.getSetIntegerEmbed(), resultEmbed.get(0));

            // Validate mapKeyIntegerEmbed with KEY
            // Implementation note: DISTINCT is required b/c an optimization is NOT provided to eliminate multiple SQLs. See CMVC 617252.
            queryEmbed = em.createQuery("SELECT DISTINCT e FROM " + entityName + " x JOIN x.mapKeyIntegerEmbed e JOIN e.notMapKeyInteger i WHERE KEY(i) = 2");
            resultEmbed = queryEmbed.getResultList();
            Assert.assertNotNull(resultEmbed);
            Assert.assertTrue(resultEmbed.size() == 1);
            Assert.assertEquals(newEntity.getMapKeyIntegerEmbed(), resultEmbed.get(0));
            subqueryEmbed = em.createQuery("SELECT DISTINCT e FROM " + entityName
                                           + " x JOIN x.mapKeyIntegerEmbed e JOIN e.notMapKeyInteger i WHERE EXISTS ( SELECT DISTINCT f.notMapKeyInteger FROM " + entityName
                                           + " y JOIN y.mapKeyIntegerEmbed f JOIN f.notMapKeyInteger j WHERE KEY(j) = 2 )");
            resultEmbed = subqueryEmbed.getResultList();
            Assert.assertNotNull(resultEmbed);
            Assert.assertTrue(resultEmbed.size() == 1);
            Assert.assertEquals(newEntity.getMapKeyIntegerEmbed(), resultEmbed.get(0));

            // Validate mapKeyIntegerEmbed with VALUE
            // Implementation note: DISTINCT is required b/c an optimization is NOT provided to eliminate multiple SQLs. See CMVC 617252.
            queryEmbed = em.createQuery("SELECT DISTINCT e FROM " + entityName + " x JOIN x.mapKeyIntegerEmbed e JOIN e.notMapKeyInteger i WHERE VALUE(i) = 200");
            resultEmbed = queryEmbed.getResultList();
            Assert.assertNotNull(resultEmbed);
            Assert.assertTrue(resultEmbed.size() == 1);
            Assert.assertEquals(newEntity.getMapKeyIntegerEmbed(), resultEmbed.get(0));
            subqueryEmbed = em.createQuery("SELECT DISTINCT e FROM " + entityName
                                           + " x JOIN x.mapKeyIntegerEmbed e JOIN e.notMapKeyInteger i WHERE EXISTS ( SELECT DISTINCT f.notMapKeyInteger FROM " + entityName
                                           + " y JOIN y.mapKeyIntegerEmbed f JOIN f.notMapKeyInteger j WHERE VALUE(j) = 200 )");
            resultEmbed = subqueryEmbed.getResultList();
            Assert.assertNotNull(resultEmbed);
            Assert.assertTrue(resultEmbed.size() == 1);
            Assert.assertEquals(newEntity.getMapKeyIntegerEmbed(), resultEmbed.get(0));

            // Validate mapKeyIntegerEmbed with KEY and VALUE
            // Implementation note: DISTINCT is required b/c an optimization is NOT provided to eliminate multiple SQLs. See CMVC 617252.
            queryEmbed = em.createQuery("SELECT DISTINCT e FROM " + entityName + " x JOIN x.mapKeyIntegerEmbed e JOIN e.notMapKeyInteger i WHERE KEY(i) = 2 AND VALUE(i) = 200");
            resultEmbed = queryEmbed.getResultList();
            Assert.assertNotNull(resultEmbed);
            Assert.assertTrue(resultEmbed.size() == 1);
            Assert.assertEquals(newEntity.getMapKeyIntegerEmbed(), resultEmbed.get(0));
            subqueryEmbed = em.createQuery("SELECT DISTINCT e FROM " + entityName
                                           + " x JOIN x.mapKeyIntegerEmbed e JOIN e.notMapKeyInteger i WHERE EXISTS ( SELECT DISTINCT f.notMapKeyInteger FROM "
                                           + entityName
                                           + " y JOIN y.mapKeyIntegerEmbed f JOIN f.notMapKeyInteger j WHERE KEY(j) = 2 AND VALUE(j) = 200 )");
            resultEmbed = subqueryEmbed.getResultList();
            Assert.assertNotNull(resultEmbed);
            Assert.assertTrue(resultEmbed.size() == 1);
            Assert.assertEquals(newEntity.getMapKeyIntegerEmbed(), resultEmbed.get(0));

            // Validate listIntegerEmbedOrderColumn
            // Implementation note: DISTINCT is required b/c an optimization is NOT provided to eliminate multiple SQLs. See CMVC 617252.
            queryEmbed = em.createQuery("SELECT DISTINCT e FROM " + entityName + " x JOIN x.listIntegerEmbedOrderColumn e JOIN e.integerValue v WHERE v = 3");
            resultEmbed = queryEmbed.getResultList();
            Assert.assertNotNull(resultEmbed);
            Assert.assertTrue(resultEmbed.size() == 1);
            Assert.assertEquals(newEntity.getListIntegerEmbedOrderColumn().get(1), resultEmbed.get(0));
            subqueryEmbed = em.createQuery("SELECT DISTINCT e FROM " + entityName
                                           + " x JOIN x.listIntegerEmbedOrderColumn e JOIN e.integerValue v WHERE v = 3 AND EXISTS ( SELECT DISTINCT f.integerValue FROM "
                                           + entityName + " y JOIN y.listIntegerEmbedOrderColumn f JOIN f.integerValue w WHERE w = 3 )");
            resultEmbed = subqueryEmbed.getResultList();
            Assert.assertNotNull(resultEmbed);
            Assert.assertTrue(resultEmbed.size() == 1);
            Assert.assertEquals(newEntity.getListIntegerEmbedOrderColumn().get(1), resultEmbed.get(0));

            // Validate listIntegerAttributeOverridesEmbedOrderColumn
            // Implementation note: DISTINCT is required b/c an optimization is NOT provided to eliminate multiple SQLs. See CMVC 617252.
            queryEmbed = em.createQuery("SELECT DISTINCT e FROM " + entityName + " x JOIN x.listIntegerAttributeOverridesEmbedOrderColumn e JOIN e.notIntegerValue v WHERE v = 3");
            resultEmbed = queryEmbed.getResultList();
            Assert.assertNotNull(resultEmbed);
            Assert.assertTrue(resultEmbed.size() == 1);
            Assert.assertEquals(newEntity.getListIntegerAttributeOverridesEmbedOrderColumn().get(1), resultEmbed.get(0));
            subqueryEmbed = em.createQuery("SELECT DISTINCT e FROM "
                                           + entityName
                                           + " x JOIN x.listIntegerAttributeOverridesEmbedOrderColumn e JOIN e.notIntegerValue v WHERE v = 3 AND EXISTS ( SELECT DISTINCT f.notIntegerValue FROM "
                                           + entityName + " y JOIN y.listIntegerAttributeOverridesEmbedOrderColumn f JOIN f.notIntegerValue w WHERE w = 3 )");
            resultEmbed = subqueryEmbed.getResultList();
            Assert.assertNotNull(resultEmbed);
            Assert.assertTrue(resultEmbed.size() == 1);
            Assert.assertEquals(newEntity.getListIntegerAttributeOverridesEmbedOrderColumn().get(1), resultEmbed.get(0));

            // Validate listIntegerPropertyAccessEmbedOrderColumn
            // Implementation note: DISTINCT is required b/c an optimization is NOT provided to eliminate multiple SQLs. See CMVC 617252.
            queryEmbed = em.createQuery("SELECT DISTINCT e FROM " + entityName
                                        + " x JOIN x.listIntegerPropertyAccessEmbedOrderColumn e JOIN e.integerValuePropertyAccessColumn v WHERE v = 3");
            resultEmbed = queryEmbed.getResultList();
            Assert.assertNotNull(resultEmbed);
            Assert.assertTrue(resultEmbed.size() == 1);
            Assert.assertEquals(newEntity.getListIntegerPropertyAccessEmbedOrderColumn().get(1), resultEmbed.get(0));
            subqueryEmbed = em.createQuery("SELECT DISTINCT e FROM "
                                           + entityName
                                           + " x JOIN x.listIntegerPropertyAccessEmbedOrderColumn e JOIN e.integerValuePropertyAccessColumn v WHERE v = 3 AND EXISTS ( SELECT DISTINCT f.integerValuePropertyAccessColumn FROM "
                                           + entityName + " y JOIN y.listIntegerPropertyAccessEmbedOrderColumn f JOIN f.integerValuePropertyAccessColumn w WHERE w = 3 )");
            resultEmbed = subqueryEmbed.getResultList();
            Assert.assertNotNull(resultEmbed);
            Assert.assertTrue(resultEmbed.size() == 1);
            Assert.assertEquals(newEntity.getListIntegerPropertyAccessEmbedOrderColumn().get(1), resultEmbed.get(0));

            // Validate setIntegerPropertyAccessEmbed
            // Implementation note: DISTINCT is required b/c an optimization is NOT provided to eliminate multiple SQLs. See CMVC 617252.
            queryEmbed = em.createQuery("SELECT DISTINCT v FROM " + entityName
                                        + " x JOIN x.setIntegerPropertyAccessEmbed e JOIN e.integerValuePropertyAccessColumn v WHERE v = 3");
            resultEmbed = queryEmbed.getResultList();
            Assert.assertNotNull(resultEmbed);
            Assert.assertTrue(resultEmbed.size() == 1);
            Assert.assertEquals(new Integer(3), resultEmbed.get(0));
            subqueryEmbed = em.createQuery("SELECT DISTINCT v FROM "
                                           + entityName
                                           + " x JOIN x.setIntegerPropertyAccessEmbed e JOIN e.integerValuePropertyAccessColumn v WHERE v = 3 AND EXISTS ( SELECT DISTINCT w FROM "
                                           + entityName + " y JOIN y.setIntegerPropertyAccessEmbed f JOIN f.integerValuePropertyAccessColumn w WHERE w = 3 )");
            resultEmbed = subqueryEmbed.getResultList();
            Assert.assertNotNull(resultEmbed);
            Assert.assertTrue(resultEmbed.size() == 1);
            Assert.assertEquals(new Integer(3), resultEmbed.get(0));

            // Validate mapKeyIntegerValueIntegerPropertyAccessEmbed with KEY
            // Implementation note: DISTINCT is required b/c an optimization is NOT provided to eliminate multiple SQLs. See CMVC 617252.
            queryEmbed = em.createQuery("SELECT DISTINCT KEY(e) FROM " + entityName + " x JOIN x.mapKeyIntegerValueIntegerPropertyAccessEmbed e WHERE KEY(e) = 2");
            resultEmbed = queryEmbed.getResultList();
            Assert.assertNotNull(resultEmbed);
            Assert.assertTrue(resultEmbed.size() == 1);
            Assert.assertEquals(new Integer(2), resultEmbed.get(0));
            subqueryEmbed = em.createQuery("SELECT DISTINCT KEY(e) FROM " + entityName
                                           + " x JOIN x.mapKeyIntegerValueIntegerPropertyAccessEmbed e WHERE KEY(e) = 2 AND EXISTS ( SELECT DISTINCT KEY(f) FROM "
                                           + entityName + " y JOIN y.mapKeyIntegerValueIntegerPropertyAccessEmbed f WHERE KEY(f) = 2 )");
            resultEmbed = subqueryEmbed.getResultList();
            Assert.assertNotNull(resultEmbed);
            Assert.assertTrue(resultEmbed.size() == 1);
            Assert.assertEquals(new Integer(2), resultEmbed.get(0));

            // Validate mapKeyIntegerValueIntegerPropertyAccessEmbed with VALUE
            // Implementation note: DISTINCT is required b/c an optimization is NOT provided to eliminate multiple SQLs. See CMVC 617252.
            queryEmbed = em.createQuery("SELECT DISTINCT VALUE(e) FROM " + entityName
                                        + " x JOIN x.mapKeyIntegerValueIntegerPropertyAccessEmbed e WHERE VALUE(e).integerValuePropertyAccessColumn = 200");
            resultEmbed = queryEmbed.getResultList();
            Assert.assertNotNull(resultEmbed);
            Assert.assertTrue(resultEmbed.size() == 1);
            Assert.assertEquals(new IntegerPropertyAccessEmbed(new Integer(200)), resultEmbed.get(0));
            subqueryEmbed = em.createQuery("SELECT DISTINCT VALUE(e) FROM "
                                           + entityName
                                           + " x JOIN x.mapKeyIntegerValueIntegerPropertyAccessEmbed e WHERE VALUE(e).integerValuePropertyAccessColumn = 200 AND EXISTS ( SELECT DISTINCT VALUE(f).integerValuePropertyAccessColumn FROM "
                                           + entityName + " y JOIN y.mapKeyIntegerValueIntegerPropertyAccessEmbed f WHERE VALUE(f).integerValuePropertyAccessColumn = 200 )");
            resultEmbed = subqueryEmbed.getResultList();
            Assert.assertNotNull(resultEmbed);
            Assert.assertTrue(resultEmbed.size() == 1);
            Assert.assertEquals(new IntegerPropertyAccessEmbed(new Integer(200)), resultEmbed.get(0));

            // Validate mapKeyIntegerValueIntegerPropertyAccessEmbed with KEY and VALUE
            // Implementation note: DISTINCT is required b/c an optimization is NOT provided to eliminate multiple SQLs. See CMVC 617252.
            queryEmbed = em.createQuery("SELECT DISTINCT KEY(e) FROM " + entityName
                                        + " x JOIN x.mapKeyIntegerValueIntegerPropertyAccessEmbed e WHERE KEY(e) = 2 AND VALUE(e).integerValuePropertyAccessColumn = 200");
            resultEmbed = queryEmbed.getResultList();
            Assert.assertNotNull(resultEmbed);
            Assert.assertTrue(resultEmbed.size() == 1);
            Assert.assertEquals(new Integer(2), resultEmbed.get(0));
            subqueryEmbed = em.createQuery("SELECT DISTINCT KEY(e) FROM "
                                           + entityName
                                           + " x JOIN x.mapKeyIntegerValueIntegerPropertyAccessEmbed e WHERE KEY(e) = 2 AND VALUE(e).integerValuePropertyAccessColumn = 200 AND EXISTS ( SELECT DISTINCT KEY(f) FROM "
                                           + entityName
                                           + " y JOIN y.mapKeyIntegerValueIntegerPropertyAccessEmbed f WHERE KEY(f) = 2 AND VALUE(f).integerValuePropertyAccessColumn = 200 )");
            resultEmbed = subqueryEmbed.getResultList();
            Assert.assertNotNull(resultEmbed);
            Assert.assertTrue(resultEmbed.size() == 1);
            Assert.assertEquals(new Integer(2), resultEmbed.get(0));

            // Validate mapKeyIntegerEmbedValueIntegerEmbed with KEY
            // Implementation note: DISTINCT is required b/c an optimization is NOT provided to eliminate multiple SQLs. See CMVC 617252.
            queryEmbed = em.createQuery("SELECT DISTINCT KEY(e) FROM " + entityName + " x JOIN x.mapKeyIntegerEmbedValueIntegerEmbed e WHERE KEY(e).integerValue = 2");
            resultEmbed = queryEmbed.getResultList();
            Assert.assertNotNull(resultEmbed);
            Assert.assertTrue(resultEmbed.size() == 1);
            Assert.assertEquals(new IntegerEmbed(new Integer(2)), resultEmbed.get(0));
            subqueryEmbed = em.createQuery("SELECT DISTINCT KEY(e) FROM "
                                           + entityName
                                           + " x JOIN x.mapKeyIntegerEmbedValueIntegerEmbed e WHERE KEY(e).integerValue = 2 AND EXISTS ( SELECT DISTINCT KEY(f).integerValue FROM "
                                           + entityName + " y JOIN y.mapKeyIntegerEmbedValueIntegerEmbed f WHERE KEY(f).integerValue = 2 )");
            resultEmbed = subqueryEmbed.getResultList();
            Assert.assertNotNull(resultEmbed);
            Assert.assertTrue(resultEmbed.size() == 1);
            Assert.assertEquals(new IntegerEmbed(new Integer(2)), resultEmbed.get(0));

            // Validate mapKeyIntegerEmbedValueIntegerEmbed with VALUE
            // Implementation note: DISTINCT is required b/c an optimization is NOT provided to eliminate multiple SQLs. See CMVC 617252.
            queryEmbed = em.createQuery("SELECT DISTINCT VALUE(e) FROM " + entityName + " x JOIN x.mapKeyIntegerEmbedValueIntegerEmbed e WHERE VALUE(e).integerValue = 200");
            resultEmbed = queryEmbed.getResultList();
            Assert.assertNotNull(resultEmbed);
            Assert.assertTrue(resultEmbed.size() == 1);
            Assert.assertEquals(new IntegerEmbed(new Integer(200)), resultEmbed.get(0));
            subqueryEmbed = em.createQuery("SELECT DISTINCT VALUE(e) FROM "
                                           + entityName
                                           + " x JOIN x.mapKeyIntegerEmbedValueIntegerEmbed e WHERE VALUE(e).integerValue = 200 AND EXISTS ( SELECT DISTINCT VALUE(f).integerValue FROM "
                                           + entityName + " y JOIN y.mapKeyIntegerEmbedValueIntegerEmbed f WHERE VALUE(f).integerValue = 200 )");
            resultEmbed = subqueryEmbed.getResultList();
            Assert.assertNotNull(resultEmbed);
            Assert.assertTrue(resultEmbed.size() == 1);
            Assert.assertEquals(new IntegerEmbed(new Integer(200)), resultEmbed.get(0));

            // Validate mapKeyIntegerEmbedValueIntegerEmbed with KEY and VALUE
            // Implementation note: DISTINCT is required b/c an optimization is NOT provided to eliminate multiple SQLs. See CMVC 617252.
            queryEmbed = em.createQuery("SELECT DISTINCT KEY(e) FROM " + entityName
                                        + " x JOIN x.mapKeyIntegerEmbedValueIntegerEmbed e WHERE KEY(e).integerValue = 2 AND VALUE(e).integerValue = 200");
            resultEmbed = queryEmbed.getResultList();
            Assert.assertNotNull(resultEmbed);
            Assert.assertTrue(resultEmbed.size() == 1);
            Assert.assertEquals(new IntegerEmbed(new Integer(2)), resultEmbed.get(0));
            subqueryEmbed = em.createQuery("SELECT DISTINCT KEY(e) FROM "
                                           + entityName
                                           + " x JOIN x.mapKeyIntegerEmbedValueIntegerEmbed e WHERE KEY(e).integerValue = 2 AND VALUE(e).integerValue = 200 AND EXISTS ( SELECT DISTINCT KEY(f).integerValue FROM "
                                           + entityName
                                           + " y JOIN y.mapKeyIntegerEmbedValueIntegerEmbed f WHERE KEY(f).integerValue = 2 AND VALUE(f).integerValue = 200 )");
            resultEmbed = subqueryEmbed.getResultList();
            Assert.assertNotNull(resultEmbed);
            Assert.assertTrue(resultEmbed.size() == 1);
            Assert.assertEquals(new IntegerEmbed(new Integer(2)), resultEmbed.get(0));

            em.clear();

            System.out.println("Beginning new transaction...");
            tj.beginTransaction();
            if (tj.isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                em.joinTransaction();
            }

            // Delete the entity from the database
            System.out.println("Performing find(" + JPAEmbeddableBasicEntity.class.getSimpleName() + ", " + id + ") operation");
            JPAEmbeddableBasicEntity updatedFindEntity = em.find(JPAEmbeddableBasicEntity.class, id);
            Assert.assertNotNull("find(" + JPAEmbeddableBasicEntity.class.getSimpleName() + ", " + id + ") did not return an entity.", updatedFindEntity);

            System.out.println("Performing remove(" + updatedFindEntity + ") operation");
            em.remove(updatedFindEntity);

            System.out.println("Committing transaction...");
            if (tj.isTransactionActive()) {
                tj.commitTransaction();
            }

            em.clear();

            // Verify the entity remove was successful
            System.out.println("Performing find(" + JPAEmbeddableBasicEntity.class.getSimpleName() + ", " + id + ") operation");
            JPAEmbeddableBasicEntity findRemovedEntity = em.find(JPAEmbeddableBasicEntity.class, id);
            Assert.assertNull("find(" + JPAEmbeddableBasicEntity.class.getSimpleName() + ", " + id + ") returned an entity.", findRemovedEntity);
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

    @SuppressWarnings({ "unchecked", "rawtypes", "deprecation" })
    public void testEmbeddableBasic04(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            int id = 4;

            XMLJPAEmbeddableBasicEntity newEntity = new XMLJPAEmbeddableBasicEntity();
            newEntity.setId(id);

            newEntity.setIntegerEmbed(new XMLIntegerEmbed(11));
            newEntity.setIntegerAttributeOverridesEmbed(new XMLIntegerAttributeOverridesEmbed(22));

            newEntity.setIntegerFieldAccessEmbed(new XMLIntegerFieldAccessEmbed(33));
            newEntity.setEnumeratedFieldAccessEmbed(new XMLEnumeratedFieldAccessEmbed());
            newEntity.setTemporalFieldAccessEmbed(new XMLTemporalFieldAccessEmbed());
            newEntity.setLobFieldAccessEmbed(new XMLLobFieldAccessEmbed());

            newEntity.setIntegerTransientEmbed(new XMLIntegerTransientEmbed());

            newEntity.setIntegerPropertyAccessEmbed(new XMLIntegerPropertyAccessEmbed(66));
            newEntity.setEnumeratedPropertyAccessEmbed(new XMLEnumeratedPropertyAccessEmbed());
            newEntity.setTemporalPropertyAccessEmbed(new XMLTemporalPropertyAccessEmbed());
            newEntity.setLobPropertyAccessEmbed(new XMLLobPropertyAccessEmbed());

            newEntity.setCollectionIntegerEmbed(new XMLCollectionIntegerEmbed(XMLCollectionIntegerEmbed.INIT));
            newEntity.setCollectionEnumeratedEmbed(new XMLCollectionEnumeratedEmbed());
            newEntity.setCollectionTemporalEmbed(new XMLCollectionTemporalEmbed());
            newEntity.setCollectionLobEmbed(new XMLCollectionLobEmbed());

            newEntity.setListIntegerOrderColumnEmbed(new XMLListIntegerOrderColumnEmbed(XMLListIntegerOrderColumnEmbed.INIT));
            newEntity.setListIntegerOrderByEmbed(new XMLListIntegerOrderByEmbed());
            newEntity.setListEnumeratedEmbed(new XMLListEnumeratedEmbed());
            newEntity.setListTemporalEmbed(new XMLListTemporalEmbed());
            newEntity.setListLobEmbed(new XMLListLobEmbed());

            newEntity.setSetIntegerEmbed(new XMLSetIntegerEmbed(XMLSetIntegerEmbed.INIT));
            newEntity.setSetEnumeratedEmbed(new XMLSetEnumeratedEmbed());
            newEntity.setSetTemporalEmbed(new XMLSetTemporalEmbed());
            newEntity.setSetLobEmbed(new XMLSetLobEmbed());

            newEntity.setMapKeyIntegerEmbed(new XMLMapKeyIntegerEmbed(XMLMapKeyIntegerEmbed.INIT));
            newEntity.setMapKeyIntegerValueTemporalEmbed(new XMLMapKeyIntegerValueTemporalEmbed());
            newEntity.setMapKeyEnumeratedValueEnumeratedEmbed(new XMLMapKeyEnumeratedValueEnumeratedEmbed());
            newEntity.setMapKeyTemporalValueTemporalEmbed(new XMLMapKeyTemporalValueTemporalEmbed());
            newEntity.setMapKeyEnumeratedValueLobEmbed(new XMLMapKeyEnumeratedValueLobEmbed());

            newEntity.setListIntegerEmbedOrderColumn(new ArrayList(XMLIntegerEmbed.LIST_INIT));
            newEntity.setListIntegerAttributeOverridesEmbedOrderColumn(new ArrayList(XMLIntegerAttributeOverridesEmbed.LIST_INIT));
            newEntity.setListIntegerPropertyAccessEmbedOrderColumn(new ArrayList(XMLIntegerPropertyAccessEmbed.LIST_INIT));
            newEntity.setSetIntegerPropertyAccessEmbed(new HashSet(XMLIntegerPropertyAccessEmbed.SET_INIT));
//            newEntity.setMapKeyIntegerValueIntegerPropertyAccessEmbed(new HashMap(XMLIntegerPropertyAccessEmbed.MAP_INIT));
//            newEntity.setMapKeyIntegerEmbedValueIntegerEmbed(new HashMap(XMLIntegerEmbed.MAP_INIT2));

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

            // Validate integerEmbed
            // Implementation note: DISTINCT is required b/c an optimization is NOT provided to eliminate multiple SQLs. See CMVC 617252.
            String entityName = XMLJPAEmbeddableBasicEntity.class.getSimpleName();
            Query queryEmbed = em.createQuery("SELECT DISTINCT e FROM " + entityName + " x JOIN x.integerEmbed e WHERE e.integerValue = 11");
            List resultEmbed = queryEmbed.getResultList();
            Assert.assertNotNull(resultEmbed);
            Assert.assertTrue(resultEmbed.size() == 1);
            Assert.assertEquals(newEntity.getIntegerEmbed(), resultEmbed.get(0));
            Query subqueryEmbed = em.createQuery("SELECT DISTINCT e FROM " + entityName + " x JOIN x.integerEmbed e WHERE EXISTS ( SELECT DISTINCT f.integerValue FROM "
                                                 + entityName + " y JOIN y.integerEmbed f WHERE f.integerValue = 11 )");
            resultEmbed = subqueryEmbed.getResultList();
            Assert.assertNotNull(resultEmbed);
            Assert.assertTrue(resultEmbed.size() == 1);
            Assert.assertEquals(newEntity.getIntegerEmbed(), resultEmbed.get(0));

            // Validate integerAttributeOverridesEmbed
            // Implementation note: DISTINCT is required b/c an optimization is NOT provided to eliminate multiple SQLs. See CMVC 617252.
            queryEmbed = em.createQuery("SELECT DISTINCT e FROM " + entityName + " x JOIN x.integerAttributeOverridesEmbed e WHERE e.notIntegerValue = 22");
            resultEmbed = queryEmbed.getResultList();
            Assert.assertNotNull(resultEmbed);
            Assert.assertTrue(resultEmbed.size() == 1);
            Assert.assertEquals(newEntity.getIntegerAttributeOverridesEmbed(), resultEmbed.get(0));
            subqueryEmbed = em.createQuery("SELECT DISTINCT e FROM " + entityName
                                           + " x JOIN x.integerAttributeOverridesEmbed e WHERE EXISTS ( SELECT DISTINCT f.notIntegerValue FROM " + entityName
                                           + " y JOIN y.integerAttributeOverridesEmbed f WHERE f.notIntegerValue = 22 )");
            resultEmbed = subqueryEmbed.getResultList();
            Assert.assertNotNull(resultEmbed);
            Assert.assertTrue(resultEmbed.size() == 1);
            Assert.assertEquals(newEntity.getIntegerAttributeOverridesEmbed(), resultEmbed.get(0));

            // Validate integerFieldAccessEmbed
            // Implementation note: DISTINCT is required b/c an optimization is NOT provided to eliminate multiple SQLs. See CMVC 617252.
            queryEmbed = em.createQuery("SELECT DISTINCT e FROM " + entityName + " x JOIN x.integerFieldAccessEmbed e WHERE e.integerValueFieldAccessColumn = 33");
            resultEmbed = queryEmbed.getResultList();
            Assert.assertNotNull(resultEmbed);
            Assert.assertTrue(resultEmbed.size() == 1);
            Assert.assertEquals(newEntity.getIntegerFieldAccessEmbed(), resultEmbed.get(0));
            subqueryEmbed = em.createQuery("SELECT DISTINCT e FROM " + entityName
                                           + " x JOIN x.integerFieldAccessEmbed e WHERE EXISTS ( SELECT DISTINCT f.integerValueFieldAccessColumn FROM " + entityName
                                           + " y JOIN y.integerFieldAccessEmbed f WHERE f.integerValueFieldAccessColumn = 33 )");
            resultEmbed = subqueryEmbed.getResultList();
            Assert.assertNotNull(resultEmbed);
            Assert.assertTrue(resultEmbed.size() == 1);
            Assert.assertEquals(newEntity.getIntegerFieldAccessEmbed(), resultEmbed.get(0));

            // Validate integerPropertyAccessEmbed
            // Implementation note: DISTINCT is required b/c an optimization is NOT provided to eliminate multiple SQLs. See CMVC 617252.
            queryEmbed = em.createQuery("SELECT DISTINCT e FROM " + entityName + " x , IN(x.integerPropertyAccessEmbed) e WHERE e.integerValuePropertyAccessColumn = 66");
            resultEmbed = queryEmbed.getResultList();
            Assert.assertNotNull(resultEmbed);
            Assert.assertTrue(resultEmbed.size() == 1);
            Assert.assertEquals(newEntity.getIntegerPropertyAccessEmbed(), resultEmbed.get(0));
            subqueryEmbed = em.createQuery("SELECT DISTINCT e FROM " + entityName
                                           + " x , IN(x.integerPropertyAccessEmbed) e WHERE EXISTS ( SELECT DISTINCT f.integerValuePropertyAccessColumn FROM " + entityName
                                           + " y , IN(y.integerPropertyAccessEmbed) f WHERE f.integerValuePropertyAccessColumn = 66 )");
            resultEmbed = subqueryEmbed.getResultList();
            Assert.assertNotNull(resultEmbed);
            Assert.assertTrue(resultEmbed.size() == 1);
            Assert.assertEquals(newEntity.getIntegerPropertyAccessEmbed(), resultEmbed.get(0));

            // Validate collectionIntegerEmbed
            // Implementation note: DISTINCT is required b/c an optimization is NOT provided to eliminate multiple SQLs. See CMVC 617252.
            queryEmbed = em.createQuery("SELECT DISTINCT e FROM " + entityName + " x JOIN x.collectionIntegerEmbed e JOIN e.collectionInteger i WHERE i = 2");
            resultEmbed = queryEmbed.getResultList();
            Assert.assertNotNull(resultEmbed);
            Assert.assertTrue(resultEmbed.size() == 1);
            Assert.assertEquals(newEntity.getCollectionIntegerEmbed(), resultEmbed.get(0));
            subqueryEmbed = em.createQuery("SELECT DISTINCT e FROM " + entityName
                                           + " x JOIN x.collectionIntegerEmbed e JOIN e.collectionInteger i WHERE EXISTS ( SELECT DISTINCT f.collectionInteger FROM "
                                           + entityName
                                           + " y JOIN y.collectionIntegerEmbed f JOIN f.collectionInteger j WHERE j = 2 )");
            resultEmbed = subqueryEmbed.getResultList();
            Assert.assertNotNull(resultEmbed);
            Assert.assertTrue(resultEmbed.size() == 1);
            Assert.assertEquals(newEntity.getCollectionIntegerEmbed(), resultEmbed.get(0));

            // Validate listIntegerOrderColumnEmbed
            // Implementation note: DISTINCT is required b/c an optimization is NOT provided to eliminate multiple SQLs. See CMVC 617252.
            queryEmbed = em.createQuery("SELECT DISTINCT e FROM " + entityName + " x , IN(x.listIntegerOrderColumnEmbed) e , IN(e.notListIntegerOrderColumn) i WHERE i = 3");
            resultEmbed = queryEmbed.getResultList();
            Assert.assertNotNull(resultEmbed);
            Assert.assertTrue(resultEmbed.size() == 1);
            Assert.assertEquals(newEntity.getListIntegerOrderColumnEmbed(), resultEmbed.get(0));
            subqueryEmbed = em.createQuery("SELECT DISTINCT e FROM "
                                           + entityName
                                           + " x , IN(x.listIntegerOrderColumnEmbed) e , IN(e.notListIntegerOrderColumn) i WHERE EXISTS ( SELECT DISTINCT f.notListIntegerOrderColumn FROM "
                                           + entityName
                                           + " y , IN(y.listIntegerOrderColumnEmbed) f , IN(f.notListIntegerOrderColumn) j WHERE j = 3 )");
            resultEmbed = subqueryEmbed.getResultList();
            Assert.assertNotNull(resultEmbed);
            Assert.assertTrue(resultEmbed.size() == 1);
            Assert.assertEquals(newEntity.getListIntegerOrderColumnEmbed(), resultEmbed.get(0));

            // Validate setIntegerEmbed
            // Implementation note: DISTINCT is required b/c an optimization is NOT provided to eliminate multiple SQLs. See CMVC 617252.
            queryEmbed = em.createQuery("SELECT DISTINCT e FROM " + entityName + " x JOIN  x.setIntegerEmbed e JOIN e.notSetInteger i WHERE i = 1");
            resultEmbed = queryEmbed.getResultList();
            Assert.assertNotNull(resultEmbed);
            Assert.assertTrue(resultEmbed.size() == 1);
            Assert.assertEquals(newEntity.getSetIntegerEmbed(), resultEmbed.get(0));
            subqueryEmbed = em.createQuery("SELECT DISTINCT e FROM " + entityName
                                           + " x JOIN  x.setIntegerEmbed e JOIN e.notSetInteger i WHERE EXISTS ( SELECT DISTINCT f.notSetInteger FROM " + entityName
                                           + " y JOIN  y.setIntegerEmbed f JOIN f.notSetInteger j WHERE j = 1 )");
            resultEmbed = subqueryEmbed.getResultList();
            Assert.assertNotNull(resultEmbed);
            Assert.assertTrue(resultEmbed.size() == 1);
            Assert.assertEquals(newEntity.getSetIntegerEmbed(), resultEmbed.get(0));

            // Validate mapKeyIntegerEmbed with KEY
            // Implementation note: DISTINCT is required b/c an optimization is NOT provided to eliminate multiple SQLs. See CMVC 617252.
            queryEmbed = em.createQuery("SELECT DISTINCT e FROM " + entityName + " x JOIN x.mapKeyIntegerEmbed e JOIN e.notMapKeyInteger i WHERE KEY(i) = 2");
            resultEmbed = queryEmbed.getResultList();
            Assert.assertNotNull(resultEmbed);
            Assert.assertTrue(resultEmbed.size() == 1);
            Assert.assertEquals(newEntity.getMapKeyIntegerEmbed(), resultEmbed.get(0));
            subqueryEmbed = em.createQuery("SELECT DISTINCT e FROM " + entityName
                                           + " x JOIN x.mapKeyIntegerEmbed e JOIN e.notMapKeyInteger i WHERE EXISTS ( SELECT DISTINCT f.notMapKeyInteger FROM " + entityName
                                           + " y JOIN y.mapKeyIntegerEmbed f JOIN f.notMapKeyInteger j WHERE KEY(j) = 2 )");
            resultEmbed = subqueryEmbed.getResultList();
            Assert.assertNotNull(resultEmbed);
            Assert.assertTrue(resultEmbed.size() == 1);
            Assert.assertEquals(newEntity.getMapKeyIntegerEmbed(), resultEmbed.get(0));

            // Validate mapKeyIntegerEmbed with VALUE
            // Implementation note: DISTINCT is required b/c an optimization is NOT provided to eliminate multiple SQLs. See CMVC 617252.
            queryEmbed = em.createQuery("SELECT DISTINCT e FROM " + entityName + " x JOIN x.mapKeyIntegerEmbed e JOIN e.notMapKeyInteger i WHERE VALUE(i) = 200");
            resultEmbed = queryEmbed.getResultList();
            Assert.assertNotNull(resultEmbed);
            Assert.assertTrue(resultEmbed.size() == 1);
            Assert.assertEquals(newEntity.getMapKeyIntegerEmbed(), resultEmbed.get(0));
            subqueryEmbed = em.createQuery("SELECT DISTINCT e FROM " + entityName
                                           + " x JOIN x.mapKeyIntegerEmbed e JOIN e.notMapKeyInteger i WHERE EXISTS ( SELECT DISTINCT f.notMapKeyInteger FROM " + entityName
                                           + " y JOIN y.mapKeyIntegerEmbed f JOIN f.notMapKeyInteger j WHERE VALUE(j) = 200 )");
            resultEmbed = subqueryEmbed.getResultList();
            Assert.assertNotNull(resultEmbed);
            Assert.assertTrue(resultEmbed.size() == 1);
            Assert.assertEquals(newEntity.getMapKeyIntegerEmbed(), resultEmbed.get(0));

            // Validate mapKeyIntegerEmbed with KEY and VALUE
            // Implementation note: DISTINCT is required b/c an optimization is NOT provided to eliminate multiple SQLs. See CMVC 617252.
            queryEmbed = em.createQuery("SELECT DISTINCT e FROM " + entityName + " x JOIN x.mapKeyIntegerEmbed e JOIN e.notMapKeyInteger i WHERE KEY(i) = 2 AND VALUE(i) = 200");
            resultEmbed = queryEmbed.getResultList();
            Assert.assertNotNull(resultEmbed);
            Assert.assertTrue(resultEmbed.size() == 1);
            Assert.assertEquals(newEntity.getMapKeyIntegerEmbed(), resultEmbed.get(0));
            subqueryEmbed = em.createQuery("SELECT DISTINCT e FROM " + entityName
                                           + " x JOIN x.mapKeyIntegerEmbed e JOIN e.notMapKeyInteger i WHERE EXISTS ( SELECT DISTINCT f.notMapKeyInteger FROM "
                                           + entityName
                                           + " y JOIN y.mapKeyIntegerEmbed f JOIN f.notMapKeyInteger j WHERE KEY(j) = 2 AND VALUE(j) = 200 )");
            resultEmbed = subqueryEmbed.getResultList();
            Assert.assertNotNull(resultEmbed);
            Assert.assertTrue(resultEmbed.size() == 1);
            Assert.assertEquals(newEntity.getMapKeyIntegerEmbed(), resultEmbed.get(0));

            // Validate listIntegerEmbedOrderColumn
            // Implementation note: DISTINCT is required b/c an optimization is NOT provided to eliminate multiple SQLs. See CMVC 617252.
            queryEmbed = em.createQuery("SELECT DISTINCT e FROM " + entityName + " x JOIN x.listIntegerEmbedOrderColumn e JOIN e.integerValue v WHERE v = 3");
            resultEmbed = queryEmbed.getResultList();
            Assert.assertNotNull(resultEmbed);
            Assert.assertTrue(resultEmbed.size() == 1);
            Assert.assertEquals(newEntity.getListIntegerEmbedOrderColumn().get(1), resultEmbed.get(0));
            subqueryEmbed = em.createQuery("SELECT DISTINCT e FROM " + entityName
                                           + " x JOIN x.listIntegerEmbedOrderColumn e JOIN e.integerValue v WHERE v = 3 AND EXISTS ( SELECT DISTINCT f.integerValue FROM "
                                           + entityName + " y JOIN y.listIntegerEmbedOrderColumn f JOIN f.integerValue w WHERE w = 3 )");
            resultEmbed = subqueryEmbed.getResultList();
            Assert.assertNotNull(resultEmbed);
            Assert.assertTrue(resultEmbed.size() == 1);
            Assert.assertEquals(newEntity.getListIntegerEmbedOrderColumn().get(1), resultEmbed.get(0));

            // Validate listIntegerAttributeOverridesEmbedOrderColumn
            // Implementation note: DISTINCT is required b/c an optimization is NOT provided to eliminate multiple SQLs. See CMVC 617252.
            queryEmbed = em.createQuery("SELECT DISTINCT e FROM " + entityName + " x JOIN x.listIntegerAttributeOverridesEmbedOrderColumn e JOIN e.notIntegerValue v WHERE v = 3");
            resultEmbed = queryEmbed.getResultList();
            Assert.assertNotNull(resultEmbed);
            Assert.assertTrue(resultEmbed.size() == 1);
            Assert.assertEquals(newEntity.getListIntegerAttributeOverridesEmbedOrderColumn().get(1), resultEmbed.get(0));
            subqueryEmbed = em.createQuery("SELECT DISTINCT e FROM "
                                           + entityName
                                           + " x JOIN x.listIntegerAttributeOverridesEmbedOrderColumn e JOIN e.notIntegerValue v WHERE v = 3 AND EXISTS ( SELECT DISTINCT f.notIntegerValue FROM "
                                           + entityName
                                           + " y JOIN y.listIntegerAttributeOverridesEmbedOrderColumn f JOIN f.notIntegerValue w WHERE w = 3 )");
            resultEmbed = subqueryEmbed.getResultList();
            Assert.assertNotNull(resultEmbed);
            Assert.assertTrue(resultEmbed.size() == 1);
            Assert.assertEquals(newEntity.getListIntegerAttributeOverridesEmbedOrderColumn().get(1), resultEmbed.get(0));

            // Validate listIntegerPropertyAccessEmbedOrderColumn
            // Implementation note: DISTINCT is required b/c an optimization is NOT provided to eliminate multiple SQLs. See CMVC 617252.
            queryEmbed = em.createQuery("SELECT DISTINCT e FROM " + entityName
                                        + " x JOIN x.listIntegerPropertyAccessEmbedOrderColumn e JOIN e.integerValuePropertyAccessColumn v WHERE v = 3");
            resultEmbed = queryEmbed.getResultList();
            Assert.assertNotNull(resultEmbed);
            Assert.assertTrue(resultEmbed.size() == 1);
            Assert.assertEquals(newEntity.getListIntegerPropertyAccessEmbedOrderColumn().get(1), resultEmbed.get(0));
            subqueryEmbed = em.createQuery("SELECT DISTINCT e FROM "
                                           + entityName
                                           + " x JOIN x.listIntegerPropertyAccessEmbedOrderColumn e JOIN e.integerValuePropertyAccessColumn v WHERE v = 3 AND EXISTS ( SELECT DISTINCT f.integerValuePropertyAccessColumn FROM "
                                           + entityName
                                           + " y JOIN y.listIntegerPropertyAccessEmbedOrderColumn f JOIN f.integerValuePropertyAccessColumn w WHERE w = 3 )");
            resultEmbed = subqueryEmbed.getResultList();
            Assert.assertNotNull(resultEmbed);
            Assert.assertTrue(resultEmbed.size() == 1);
            Assert.assertEquals(newEntity.getListIntegerPropertyAccessEmbedOrderColumn().get(1), resultEmbed.get(0));

            // Validate setIntegerPropertyAccessEmbed
            // Implementation note: DISTINCT is required b/c an optimization is NOT provided to eliminate multiple SQLs. See CMVC 617252.
            queryEmbed = em.createQuery("SELECT DISTINCT v FROM " + entityName
                                        + " x JOIN x.setIntegerPropertyAccessEmbed e JOIN e.integerValuePropertyAccessColumn v WHERE v = 3");
            resultEmbed = queryEmbed.getResultList();
            Assert.assertNotNull(resultEmbed);
            Assert.assertTrue(resultEmbed.size() == 1);
            Assert.assertEquals(new Integer(3), resultEmbed.get(0));
            subqueryEmbed = em.createQuery("SELECT DISTINCT v FROM "
                                           + entityName
                                           + " x JOIN x.setIntegerPropertyAccessEmbed e JOIN e.integerValuePropertyAccessColumn v WHERE v = 3 AND EXISTS ( SELECT DISTINCT w FROM "
                                           + entityName
                                           + " y JOIN y.setIntegerPropertyAccessEmbed f JOIN f.integerValuePropertyAccessColumn w WHERE w = 3 )");
            resultEmbed = subqueryEmbed.getResultList();
            Assert.assertNotNull(resultEmbed);
            Assert.assertTrue(resultEmbed.size() == 1);
            Assert.assertEquals(new Integer(3), resultEmbed.get(0));

            // Validate mapKeyIntegerValueIntegerPropertyAccessEmbed with KEY
            // Implementation note: DISTINCT is required b/c an optimization is NOT provided to eliminate multiple SQLs. See CMVC 617252.
            queryEmbed = em.createQuery("SELECT DISTINCT KEY(e) FROM " + entityName + " x JOIN x.mapKeyIntegerValueIntegerPropertyAccessEmbed e WHERE KEY(e) = 2");
            resultEmbed = queryEmbed.getResultList();
            Assert.assertNotNull(resultEmbed);
            Assert.assertTrue(resultEmbed.size() == 1);
            Assert.assertEquals(new Integer(2), resultEmbed.get(0));
            subqueryEmbed = em.createQuery("SELECT DISTINCT KEY(e) FROM " + entityName
                                           + " x JOIN x.mapKeyIntegerValueIntegerPropertyAccessEmbed e WHERE KEY(e) = 2 AND EXISTS ( SELECT DISTINCT KEY(f) FROM "
                                           + entityName
                                           + " y JOIN y.mapKeyIntegerValueIntegerPropertyAccessEmbed f WHERE KEY(f) = 2 )");
            resultEmbed = subqueryEmbed.getResultList();
            Assert.assertNotNull(resultEmbed);
            Assert.assertTrue(resultEmbed.size() == 1);
            Assert.assertEquals(new Integer(2), resultEmbed.get(0));

            // Validate mapKeyIntegerValueIntegerPropertyAccessEmbed with VALUE
            // Implementation note: DISTINCT is required b/c an optimization is NOT provided to eliminate multiple SQLs. See CMVC 617252.
            queryEmbed = em.createQuery("SELECT DISTINCT VALUE(e) FROM " + entityName
                                        + " x JOIN x.mapKeyIntegerValueIntegerPropertyAccessEmbed e WHERE VALUE(e).integerValuePropertyAccessColumn = 200");
            resultEmbed = queryEmbed.getResultList();
            Assert.assertNotNull(resultEmbed);
            Assert.assertTrue(resultEmbed.size() == 1);
            Assert.assertEquals(new XMLIntegerPropertyAccessEmbed(new Integer(200)), resultEmbed.get(0));
            subqueryEmbed = em.createQuery("SELECT DISTINCT VALUE(e) FROM "
                                           + entityName
                                           + " x JOIN x.mapKeyIntegerValueIntegerPropertyAccessEmbed e WHERE VALUE(e).integerValuePropertyAccessColumn = 200 AND EXISTS ( SELECT DISTINCT VALUE(f).integerValuePropertyAccessColumn FROM "
                                           + entityName
                                           + " y JOIN y.mapKeyIntegerValueIntegerPropertyAccessEmbed f WHERE VALUE(f).integerValuePropertyAccessColumn = 200 )");
            resultEmbed = subqueryEmbed.getResultList();
            Assert.assertNotNull(resultEmbed);
            Assert.assertTrue(resultEmbed.size() == 1);
            Assert.assertEquals(new XMLIntegerPropertyAccessEmbed(new Integer(200)), resultEmbed.get(0));

            // Validate mapKeyIntegerValueIntegerPropertyAccessEmbed with KEY and VALUE
            // Implementation note: DISTINCT is required b/c an optimization is NOT provided to eliminate multiple SQLs. See CMVC 617252.
            queryEmbed = em.createQuery("SELECT DISTINCT KEY(e) FROM " + entityName
                                        + " x JOIN x.mapKeyIntegerValueIntegerPropertyAccessEmbed e WHERE KEY(e) = 2 AND VALUE(e).integerValuePropertyAccessColumn = 200");
            resultEmbed = queryEmbed.getResultList();
            Assert.assertNotNull(resultEmbed);
            Assert.assertTrue(resultEmbed.size() == 1);
            Assert.assertEquals(new Integer(2), resultEmbed.get(0));
            subqueryEmbed = em.createQuery("SELECT DISTINCT KEY(e) FROM "
                                           + entityName
                                           + " x JOIN x.mapKeyIntegerValueIntegerPropertyAccessEmbed e WHERE KEY(e) = 2 AND VALUE(e).integerValuePropertyAccessColumn = 200 AND EXISTS ( SELECT DISTINCT KEY(f) FROM "
                                           + entityName
                                           + " y JOIN y.mapKeyIntegerValueIntegerPropertyAccessEmbed f WHERE KEY(f) = 2 AND VALUE(f).integerValuePropertyAccessColumn = 200 )");
            resultEmbed = subqueryEmbed.getResultList();
            Assert.assertNotNull(resultEmbed);
            Assert.assertTrue(resultEmbed.size() == 1);
            Assert.assertEquals(new Integer(2), resultEmbed.get(0));

            // Validate mapKeyIntegerEmbedValueIntegerEmbed with KEY
            // Implementation note: DISTINCT is required b/c an optimization is NOT provided to eliminate multiple SQLs. See CMVC 617252.
            queryEmbed = em.createQuery("SELECT DISTINCT KEY(e) FROM " + entityName + " x JOIN x.mapKeyIntegerEmbedValueIntegerEmbed e WHERE KEY(e).integerValue = 2");
            resultEmbed = queryEmbed.getResultList();
            Assert.assertNotNull(resultEmbed);
            Assert.assertTrue(resultEmbed.size() == 1);
            Assert.assertEquals(new XMLIntegerEmbed(new Integer(2)), resultEmbed.get(0));
            subqueryEmbed = em.createQuery("SELECT DISTINCT KEY(e) FROM "
                                           + entityName
                                           + " x JOIN x.mapKeyIntegerEmbedValueIntegerEmbed e WHERE KEY(e).integerValue = 2 AND EXISTS ( SELECT DISTINCT KEY(f).integerValue FROM "
                                           + entityName
                                           + " y JOIN y.mapKeyIntegerEmbedValueIntegerEmbed f WHERE KEY(f).integerValue = 2 )");
            resultEmbed = subqueryEmbed.getResultList();
            Assert.assertNotNull(resultEmbed);
            Assert.assertTrue(resultEmbed.size() == 1);
            Assert.assertEquals(new XMLIntegerEmbed(new Integer(2)), resultEmbed.get(0));

            // Validate mapKeyIntegerEmbedValueIntegerEmbed with VALUE
            // Implementation note: DISTINCT is required b/c an optimization is NOT provided to eliminate multiple SQLs. See CMVC 617252.
            queryEmbed = em.createQuery("SELECT DISTINCT VALUE(e) FROM " + entityName + " x JOIN x.mapKeyIntegerEmbedValueIntegerEmbed e WHERE VALUE(e).integerValue = 200");
            resultEmbed = queryEmbed.getResultList();
            Assert.assertNotNull(resultEmbed);
            Assert.assertTrue(resultEmbed.size() == 1);
            Assert.assertEquals(new XMLIntegerEmbed(new Integer(200)), resultEmbed.get(0));
            subqueryEmbed = em.createQuery("SELECT DISTINCT VALUE(e) FROM "
                                           + entityName
                                           + " x JOIN x.mapKeyIntegerEmbedValueIntegerEmbed e WHERE VALUE(e).integerValue = 200 AND EXISTS ( SELECT DISTINCT VALUE(f).integerValue FROM "
                                           + entityName
                                           + " y JOIN y.mapKeyIntegerEmbedValueIntegerEmbed f WHERE VALUE(f).integerValue = 200 )");
            resultEmbed = subqueryEmbed.getResultList();
            Assert.assertNotNull(resultEmbed);
            Assert.assertTrue(resultEmbed.size() == 1);
            Assert.assertEquals(new XMLIntegerEmbed(new Integer(200)), resultEmbed.get(0));

            // Validate mapKeyIntegerEmbedValueIntegerEmbed with KEY and VALUE
            // Implementation note: DISTINCT is required b/c an optimization is NOT provided to eliminate multiple SQLs. See CMVC 617252.
            queryEmbed = em.createQuery("SELECT DISTINCT KEY(e) FROM " + entityName
                                        + " x JOIN x.mapKeyIntegerEmbedValueIntegerEmbed e WHERE KEY(e).integerValue = 2 AND VALUE(e).integerValue = 200");
            resultEmbed = queryEmbed.getResultList();
            Assert.assertNotNull(resultEmbed);
            Assert.assertTrue(resultEmbed.size() == 1);
            Assert.assertEquals(new XMLIntegerEmbed(new Integer(2)), resultEmbed.get(0));
            subqueryEmbed = em.createQuery("SELECT DISTINCT KEY(e) FROM "
                                           + entityName
                                           + " x JOIN x.mapKeyIntegerEmbedValueIntegerEmbed e WHERE KEY(e).integerValue = 2 AND VALUE(e).integerValue = 200 AND EXISTS ( SELECT DISTINCT KEY(f).integerValue FROM "
                                           + entityName
                                           + " y JOIN y.mapKeyIntegerEmbedValueIntegerEmbed f WHERE KEY(f).integerValue = 2 AND VALUE(f).integerValue = 200 )");
            resultEmbed = subqueryEmbed.getResultList();
            Assert.assertNotNull(resultEmbed);
            Assert.assertTrue(resultEmbed.size() == 1);
            Assert.assertEquals(new XMLIntegerEmbed(new Integer(2)), resultEmbed.get(0));

            em.clear();

            // Delete the entity from the database
            System.out.println("Performing find(" + XMLJPAEmbeddableBasicEntity.class.getSimpleName() + ", " + id + ") operation");
            XMLJPAEmbeddableBasicEntity updatedFindEntity = em.find(XMLJPAEmbeddableBasicEntity.class, id);
            Assert.assertNotNull("find(" + XMLJPAEmbeddableBasicEntity.class.getSimpleName() + ", " + id + ") did not return an entity.", updatedFindEntity);

            System.out.println("Beginning new transaction...");
            tj.beginTransaction();
            if (tj.isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                em.joinTransaction();
            }

            System.out.println("Performing remove(" + updatedFindEntity + ") operation");
            em.remove(updatedFindEntity);

            System.out.println("Committing transaction...");
            if (tj.isTransactionActive()) {
                tj.commitTransaction();
            }

            em.clear();

            // Verify the entity remove was successful
            System.out.println("Performing find(" + XMLJPAEmbeddableBasicEntity.class.getSimpleName() + ", " + id + ") operation");
            XMLJPAEmbeddableBasicEntity findRemovedEntity = em.find(XMLJPAEmbeddableBasicEntity.class, id);
            Assert.assertNull("find(" + XMLJPAEmbeddableBasicEntity.class.getSimpleName() + ", " + id + ") returned an entity.", findRemovedEntity);
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
     * Test Logic: testEmbeddableBasic05
     *
     * <p>
     * JPA 2.0 Specifications Tested (per V2 spec):
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
     * <li>Embedded defaulted object.
     * <li>AttributeOverrides of embedded basic type.
     * <li>Embedded field access with Column override.
     * <li>Embedded field access of Enumerated ordinal and string.
     * <li>Embedded field access of Temporal date.
     * <li>Embedded field access of CLOB.
     * <li>Embedded Java transient field.
     * <li>Embedded field access Transient.
     * <li>Embedded property access with Column override (mixing field/property
     * access).
     * <li>Embedded property access of Enumerated ordinal and string (mixing
     * field/property access).
     * <li>Embedded property access of Temporal date (mixing field/property
     * access).
     * <li>Embedded property access of CLOB (mixing field/property access).
     * <li>Embedded Collection&lt;Integer&gt; using ElementCollection,
     * CollectionTable, Column, and sorted by OrderColumn.
     * <li>Embedded Collection&lt;Enumerated.String&gt; using ElementCollection,
     * CollectionTable, Column, Enumerated, and sorted by OrderColumn.
     * <li>Embedded Collection&lt;Date&gt; using ElementCollection,
     * CollectionTable, Column, Temporal, and sorted by OrderColumn.
     * <li>Embedded Collection&lt;CLOB&gt; using ElementCollection,
     * CollectionTable, Column, Lob, and sorted by OrderColumn.
     * <li>Embedded List&lt;Integer&gt; using ElementCollection,
     * CollectionTable, Column, and sorted by OrderColumn.
     * <li>Embedded List&lt;Integer&gt; using ElementCollection,
     * CollectionTable, Column, and sorted by OrderBy.
     * <li>Embedded List&lt;Enumerated.String&gt; using ElementCollection,
     * CollectionTable, Column, Enumerated, and sorted by OrderColumn.
     * <li>Embedded List&lt;Date&gt; using ElementCollection, CollectionTable,
     * Column, Temporal, and sorted by OrderColumn.
     * <li>Embedded List&lt;CLOB&gt; using ElementCollection, CollectionTable,
     * Column, Lob, and sorted by OrderColumn.
     * <li>Embedded Set&lt;Integer&gt; using ElementCollection, CollectionTable,
     * and Column.
     * <li>Embedded Set&lt;Enumerated.String&gt; using ElementCollection,
     * CollectionTable, Column, and Enumerated.
     * <li>Embedded Set&lt;Date&gt; using ElementCollection, CollectionTable,
     * Column, and Temporal.
     * <li>Embedded Set&lt;CLOB&gt; using ElementCollection, CollectionTable,
     * Column, and Lob.
     * <li>Embedded Map&lt;Integer,Integer&gt; using ElementCollection,
     * CollectionTable, MapKeyColumn and Column.
     * <li>Embedded Map&lt;Integer,Date&gt; using ElementCollection,
     * CollectionTable, MapKeyColumn, Column, and Temporal.
     * <li>Embedded Map&lt;Enumerated.String,Enumerated.String&gt; using
     * ElementCollection, CollectionTable, MapKeyEnumerated, MapKeyColumn,
     * Column, and Enumerated.
     * <li>Embedded Map&lt;Date,Date&gt; using ElementCollection,
     * CollectionTable, MapKeyTemporal, MapKeyColumn, Column, and Temporal.
     * <li>Embedded Map&lt;Enumerated.String,CLOB&gt; using ElementCollection,
     * CollectionTable, MapKeyEnumerated, MapKeyColumn, Column, and Lob.
     * <li>Collection&lt;Embeddable.P/A CLOB&gt; using ElementCollection,
     * CollectionTable, and sorted by OrderColumn (mixing field/property
     * access).
     * <li>List&lt;Embeddable.F/A Integer&gt; using ElementCollection,
     * CollectionTable, and sorted by OrderColumn.
     * <li>List&lt;Embeddable.F/A Integer&gt; using ElementCollection,
     * CollectionTable, AttributeOverrides, and sorted by OrderColumn.
     * <li>List&lt;Embeddable.P/A Integer w/ Column&gt; using ElementCollection,
     * CollectionTable, and sorted by OrderColumn (mixing field/property
     * access).
     * <li>List&lt;Embeddable.P/A enumerations&gt; using ElementCollection,
     * CollectionTable, and sorted by OrderColumn (mixing field/property
     * access).
     * <li>List&lt;Embeddable.P/A enumerations&gt; using ElementCollection,
     * CollectionTable, and sorted by OrderBy (mixing field/property access).
     * <li>List&lt;Embeddable.P/A Date&gt; using ElementCollection,
     * CollectionTable, and sorted by OrderColumn (mixing field/property
     * access).
     * <li>Set&lt;Embeddable.P/A Integer&gt; using ElementCollection and
     * CollectionTable (mixing field/property access).
     * <li>Map&lt;Integer,Embeddable.P/A Integer&gt; using ElementCollection,
     * CollectionTable, MapKeyColumn, and AttributeOverrides (mixing
     * field/property access).
     * <li>Map&lt;Integer,Embeddable.P/A Date&gt; using ElementCollection,
     * CollectionTable, and MapKeyColumn, and sorted by OrderColumn (mixing
     * field/property access).
     * <li>Map&lt;Date,Embeddable.P/A Date&gt; using ElementCollection,
     * CollectionTable, MapKeyTemporal, and MapKeyColumn, and sorted by
     * OrderColumn (mixing field/property access).
     * <li>Map&lt;Enumerated.String,Embeddable.P/A Enumerated.String and
     * Enumerated.Ordinal&gt; using ElementCollection, CollectionTable,
     * MapKeyEnumerated, and MapKeyColumn, AttributeOverrides on both values,
     * and sorted by OrderColumn (mixing field/property access).
     * <li>Map&lt;Embeddable.F/A Integer,Embeddable.F/A Integer&gt; using
     * ElementCollection, CollectionTable, and AttributeOverrides on key and
     * value.
     * <li>Map&lt;Embeddable.P/A CLOB,Embeddable.P/A CLOB&gt; using
     * ElementCollection, CollectionTable, and AttributeOverrides on key and
     * value (mixing field/property access).
     * </ol>
     * <li>Detach the found entity, merge it to copy it to the persistence
     * context, update the original, and re-merge the original (which also
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
    @SuppressWarnings({ "unchecked", "rawtypes", "deprecation" })
    public void testEmbeddableBasic05(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        JPAProviderImpl provider = getJPAProviderImpl(jpaResource);

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            int id = 5;

            JPAEmbeddableBasicEntity newEntity = new JPAEmbeddableBasicEntity();
            newEntity.setId(id);
            newEntity.setIntegerEmbed(new IntegerEmbed(11));
            newEntity.setIntegerAttributeOverridesEmbed(new IntegerAttributeOverridesEmbed(22));
            newEntity.setIntegerFieldAccessEmbed(new IntegerFieldAccessEmbed(33));
            newEntity.setEnumeratedFieldAccessEmbed(new EnumeratedFieldAccessEmbed(EnumeratedFieldAccessEnum.ONE));
            newEntity.setTemporalFieldAccessEmbed(new TemporalFieldAccessEmbed(new Date()));
            newEntity.setLobFieldAccessEmbed(new LobFieldAccessEmbed("InitFA"));
            newEntity.setIntegerTransientEmbed(new IntegerTransientEmbed(44, 55));
            newEntity.setIntegerPropertyAccessEmbed(new IntegerPropertyAccessEmbed(66));
            newEntity.setEnumeratedPropertyAccessEmbed(new EnumeratedPropertyAccessEmbed(EnumeratedPropertyAccessEnum.TWO));
            newEntity.setTemporalPropertyAccessEmbed(new TemporalPropertyAccessEmbed(new Date()));
            newEntity.setLobPropertyAccessEmbed(new LobPropertyAccessEmbed("InitPA"));
            newEntity.setCollectionIntegerEmbed(new CollectionIntegerEmbed(CollectionIntegerEmbed.INIT));
            newEntity.setCollectionEnumeratedEmbed(new CollectionEnumeratedEmbed(CollectionEnumeratedEmbed.INIT));
            newEntity.setCollectionTemporalEmbed(new CollectionTemporalEmbed(CollectionTemporalEmbed.INIT));
            newEntity.setCollectionLobEmbed(new CollectionLobEmbed(CollectionLobEmbed.INIT));
            newEntity.setListIntegerOrderColumnEmbed(new ListIntegerOrderColumnEmbed(ListIntegerOrderColumnEmbed.INIT));
            newEntity.setListIntegerOrderByEmbed(new ListIntegerOrderByEmbed(ListIntegerOrderByEmbed.INIT));
            newEntity.setListEnumeratedEmbed(new ListEnumeratedEmbed(ListEnumeratedEmbed.INIT));
            newEntity.setListTemporalEmbed(new ListTemporalEmbed(ListTemporalEmbed.INIT));
            newEntity.setListLobEmbed(new ListLobEmbed(ListLobEmbed.INIT));
            newEntity.setSetIntegerEmbed(new SetIntegerEmbed(SetIntegerEmbed.INIT));
            newEntity.setSetEnumeratedEmbed(new SetEnumeratedEmbed(SetEnumeratedEmbed.INIT));
            newEntity.setSetTemporalEmbed(new SetTemporalEmbed(SetTemporalEmbed.INIT));
            newEntity.setSetLobEmbed(new SetLobEmbed(SetLobEmbed.INIT));
            newEntity.setMapKeyIntegerEmbed(new MapKeyIntegerEmbed(MapKeyIntegerEmbed.INIT));
            newEntity.setMapKeyIntegerValueTemporalEmbed(new MapKeyIntegerValueTemporalEmbed(MapKeyIntegerValueTemporalEmbed.INIT));
            newEntity.setMapKeyEnumeratedValueEnumeratedEmbed(new MapKeyEnumeratedValueEnumeratedEmbed(MapKeyEnumeratedValueEnumeratedEmbed.INIT));
            newEntity.setMapKeyTemporalValueTemporalEmbed(new MapKeyTemporalValueTemporalEmbed(MapKeyTemporalValueTemporalEmbed.INIT));
            newEntity.setMapKeyEnumeratedValueLobEmbed(new MapKeyEnumeratedValueLobEmbed(MapKeyEnumeratedValueLobEmbed.INIT));
            newEntity.setCollectionLobPropertyAccessEmbed(new ArrayList(LobPropertyAccessEmbed.COLLECTION_INIT));
            newEntity.setListIntegerEmbedOrderColumn(new ArrayList(IntegerEmbed.LIST_INIT));
            newEntity.setListIntegerAttributeOverridesEmbedOrderColumn(new ArrayList(IntegerAttributeOverridesEmbed.LIST_INIT));
            newEntity.setListIntegerPropertyAccessEmbedOrderColumn(new ArrayList(IntegerPropertyAccessEmbed.LIST_INIT));
            newEntity.setListEnumeratedPropertyAccessEmbedOrderColumn(new ArrayList(EnumeratedPropertyAccessEmbed.LIST_INIT));
            newEntity.setListEnumeratedPropertyAccessEmbedOrderBy(new ArrayList(EnumeratedPropertyAccessEmbed.LIST_INIT));
            newEntity.setListTemporalPropertyAccessEmbedOrderColumn(new ArrayList(TemporalPropertyAccessEmbed.LIST_INIT));
            newEntity.setSetIntegerPropertyAccessEmbed(new HashSet(IntegerPropertyAccessEmbed.SET_INIT));
            newEntity.setMapKeyIntegerValueIntegerPropertyAccessEmbed(new HashMap(IntegerPropertyAccessEmbed.MAP_INIT));
            newEntity.setMapKeyIntegerValueTemporalPropertyAccessEmbed(new HashMap(TemporalPropertyAccessEmbed.MAP_INIT));
            newEntity.setMapKeyTemporalValueTemporalPropertyAccessEmbed(new HashMap(TemporalPropertyAccessEmbed.MAP2_INIT));
            newEntity.setMapKeyEnumeratedValueEnumeratedPropertyAccessEmbed(new HashMap(EnumeratedPropertyAccessEmbed.MAP_INIT));
            newEntity.setMapKeyIntegerEmbedValueIntegerEmbed(new HashMap(IntegerEmbed.MAP_INIT2));
            newEntity.setMapKeyLobEmbedValueLobEmbed(new HashMap(LobPropertyAccessEmbed.MAP_INIT));

            System.out.println("Beginning new transaction...");
            tj.beginTransaction();
            if (tj.isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                em.joinTransaction();
            }

            System.out.println("Performing merge(" + newEntity + ") operation");
            JPAEmbeddableBasicEntity mergedNewEntity = em.merge(newEntity);
            Assert.assertFalse(newEntity + " is managed by the persistence context.", em.contains(newEntity));
            Assert.assertTrue(mergedNewEntity + " is not managed by the persistence context.", em.contains(mergedNewEntity));

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

            System.out.println("Performing find(" + JPAEmbeddableBasicEntity.class.getSimpleName() + ", " + id + ") operation");
            JPAEmbeddableBasicEntity findEntity = em.find(JPAEmbeddableBasicEntity.class, id);
            Assert.assertNotNull("find(" + JPAEmbeddableBasicEntity.class.getSimpleName() + ", " + id + ") did not return an entity.", findEntity);

            // Perform content verifications
            Assert.assertTrue(newEntity != findEntity);
            Assert.assertTrue(findEntity + " is not managed by the persistence context.", em.contains(findEntity));
            Assert.assertEquals(newEntity.getId(), findEntity.getId());
            Assert.assertEquals(newEntity.getIntegerEmbed(), findEntity.getIntegerEmbed());
            Assert.assertEquals(newEntity.getIntegerAttributeOverridesEmbed(),
                                findEntity.getIntegerAttributeOverridesEmbed());
            Assert.assertEquals(newEntity.getIntegerFieldAccessEmbed(),
                                findEntity.getIntegerFieldAccessEmbed());
            Assert.assertEquals(newEntity.getEnumeratedFieldAccessEmbed(),
                                findEntity.getEnumeratedFieldAccessEmbed());
            Assert.assertEquals(newEntity.getTemporalFieldAccessEmbed(), findEntity.getTemporalFieldAccessEmbed());
            Assert.assertEquals(newEntity.getLobFieldAccessEmbed(), findEntity.getLobFieldAccessEmbed());

            /*
             * If an Embeddable class has only one field and that field is transient, Ecipselink doesn't
             * create an object of type Embeddable class and set its transient field null.
             */
            if (JPAProviderImpl.ECLIPSELINK.equals(provider)) {
                Assert.assertNull(findEntity.getIntegerTransientEmbed());
            } else if (JPAProviderImpl.OPENJPA.equals(provider)) {
                Assert.assertNull(findEntity.getIntegerTransientEmbed().getTransientJavaValue());
                Assert.assertNull(findEntity.getIntegerTransientEmbed().getTransientValue());
            }

            Assert.assertEquals(newEntity.getIntegerPropertyAccessEmbed(),
                                findEntity.getIntegerPropertyAccessEmbed());
            Assert.assertEquals(newEntity.getEnumeratedPropertyAccessEmbed(),
                                findEntity.getEnumeratedPropertyAccessEmbed());
            Assert.assertEquals(newEntity.getTemporalPropertyAccessEmbed(),
                                findEntity.getTemporalPropertyAccessEmbed());
            Assert.assertEquals(newEntity.getLobPropertyAccessEmbed(), findEntity.getLobPropertyAccessEmbed());
            Assert.assertEquals(newEntity.getCollectionIntegerEmbed(),
                                findEntity.getCollectionIntegerEmbed());
            Assert.assertEquals(newEntity.getCollectionEnumeratedEmbed(),
                                findEntity.getCollectionEnumeratedEmbed());
            Assert.assertEquals(newEntity.getCollectionTemporalEmbed(),
                                findEntity.getCollectionTemporalEmbed());
            Assert.assertEquals(newEntity.getCollectionLobEmbed(),
                                findEntity.getCollectionLobEmbed());
            Assert.assertEquals(newEntity.getListIntegerOrderColumnEmbed(),
                                findEntity.getListIntegerOrderColumnEmbed());
            Assert.assertEquals(ListIntegerOrderByEmbed.INIT_ORDERED,
                                findEntity.getListIntegerOrderByEmbed().getNotListIntegerOrderBy());
            Assert.assertEquals(newEntity.getListEnumeratedEmbed(),
                                findEntity.getListEnumeratedEmbed());
            Assert.assertEquals(newEntity.getListTemporalEmbed(),
                                findEntity.getListTemporalEmbed());
            Assert.assertEquals(newEntity.getListLobEmbed(),
                                findEntity.getListLobEmbed());
            Assert.assertEquals(newEntity.getSetIntegerEmbed(), findEntity.getSetIntegerEmbed());
            Assert.assertEquals(newEntity.getSetEnumeratedEmbed(), findEntity.getSetEnumeratedEmbed());
            Assert.assertEquals(newEntity.getSetTemporalEmbed(), findEntity.getSetTemporalEmbed());
            Assert.assertEquals(newEntity.getSetLobEmbed(), findEntity.getSetLobEmbed());
            Assert.assertEquals(newEntity.getMapKeyIntegerEmbed(), findEntity.getMapKeyIntegerEmbed());
            Assert.assertEquals(newEntity.getMapKeyIntegerValueTemporalEmbed(),
                                findEntity.getMapKeyIntegerValueTemporalEmbed());
            Assert.assertEquals(newEntity.getMapKeyEnumeratedValueEnumeratedEmbed(),
                                findEntity.getMapKeyEnumeratedValueEnumeratedEmbed());
            Assert.assertEquals(newEntity.getMapKeyTemporalValueTemporalEmbed(),
                                findEntity.getMapKeyTemporalValueTemporalEmbed());
            Assert.assertEquals(newEntity.getMapKeyEnumeratedValueLobEmbed(),
                                findEntity.getMapKeyEnumeratedValueLobEmbed());
            Assert.assertEquals(newEntity.getCollectionLobPropertyAccessEmbed(),
                                findEntity.getCollectionLobPropertyAccessEmbed());
            Assert.assertEquals(newEntity.getListIntegerEmbedOrderColumn(),
                                findEntity.getListIntegerEmbedOrderColumn());
            Assert.assertEquals(newEntity.getListIntegerAttributeOverridesEmbedOrderColumn(), findEntity.getListIntegerAttributeOverridesEmbedOrderColumn());
            Assert.assertEquals(newEntity.getListIntegerPropertyAccessEmbedOrderColumn(), findEntity.getListIntegerPropertyAccessEmbedOrderColumn());
            Assert.assertEquals(newEntity.getListEnumeratedPropertyAccessEmbedOrderColumn(), findEntity.getListEnumeratedPropertyAccessEmbedOrderColumn());
            Assert.assertEquals(EnumeratedPropertyAccessEmbed.LIST_INIT_ORDERED,
                                findEntity.getListEnumeratedPropertyAccessEmbedOrderBy());
            Assert.assertEquals(newEntity.getListTemporalPropertyAccessEmbedOrderColumn(), findEntity.getListTemporalPropertyAccessEmbedOrderColumn());
            Assert.assertEquals(newEntity.getSetIntegerPropertyAccessEmbed(),
                                findEntity.getSetIntegerPropertyAccessEmbed());
            Assert.assertEquals(newEntity.getMapKeyIntegerValueIntegerPropertyAccessEmbed(), findEntity.getMapKeyIntegerValueIntegerPropertyAccessEmbed());
            Assert.assertEquals(newEntity.getMapKeyIntegerValueTemporalPropertyAccessEmbed(), findEntity.getMapKeyIntegerValueTemporalPropertyAccessEmbed());
            Assert.assertEquals(newEntity.getMapKeyTemporalValueTemporalPropertyAccessEmbed(),
                                findEntity.getMapKeyTemporalValueTemporalPropertyAccessEmbed());
            Assert.assertEquals(newEntity.getMapKeyEnumeratedValueEnumeratedPropertyAccessEmbed(),
                                findEntity.getMapKeyEnumeratedValueEnumeratedPropertyAccessEmbed());
            Assert.assertEquals(newEntity.getMapKeyIntegerEmbedValueIntegerEmbed(),
                                findEntity.getMapKeyIntegerEmbedValueIntegerEmbed());
            Assert.assertEquals(newEntity.getMapKeyLobEmbedValueLobEmbed(),
                                findEntity.getMapKeyLobEmbedValueLobEmbed());

            em.clear();

            System.out.println("Performing contains(" + findEntity + ") operation");
            Assert.assertFalse(findEntity + " is managed by the persistence context.", em.contains(findEntity));

            System.out.println("Performing merge(" + findEntity + ") operation");
            JPAEmbeddableBasicEntity mergedFindEntity = em.merge(findEntity);

            Assert.assertFalse(findEntity + " is managed by the persistence context.", em.contains(findEntity));
            Assert.assertTrue(mergedFindEntity + " is not managed by the persistence context.", em.contains(mergedFindEntity));

            // Update the entity
            // Don't touch ID.
            findEntity.getIntegerEmbed().setIntegerValue(111);
            findEntity.getIntegerAttributeOverridesEmbed().setNotIntegerValue(222);
            findEntity.getIntegerFieldAccessEmbed().setIntegerValueFieldAccessColumn(333);
            findEntity.getEnumeratedFieldAccessEmbed().setEnumeratedStringValueFA(EnumeratedFieldAccessEnum.TWO);
            findEntity.getEnumeratedFieldAccessEmbed().setEnumeratedOrdinalValueFA(EnumeratedFieldAccessEnum.TWO);
            findEntity.getTemporalFieldAccessEmbed().setTemporalValueFA(new Date(25000));
            findEntity.getLobFieldAccessEmbed().setClobValueFA("UpdateFA");

            /*
             * If an Embeddable class has only one field and that field is transient, Ecipselink doesn't
             * create an object of type Embeddable class and set its transient field null.
             */
            if (JPAProviderImpl.ECLIPSELINK.equals(provider)) {
                findEntity.setIntegerTransientEmbed(new IntegerTransientEmbed());
            }
            findEntity.getIntegerTransientEmbed().setTransientJavaValue(444);
            findEntity.getIntegerTransientEmbed().setTransientValue(555);

            findEntity.getIntegerPropertyAccessEmbed().setIntegerValuePropertyAccessColumn(666);
            findEntity.getEnumeratedPropertyAccessEmbed().setEnumeratedStringValuePA(EnumeratedPropertyAccessEnum.THREE);
            findEntity.getEnumeratedPropertyAccessEmbed().setEnumeratedOrdinalValuePA(EnumeratedPropertyAccessEnum.THREE);
            findEntity.getTemporalPropertyAccessEmbed().setTemporalValuePA(new Date(25000));
            findEntity.getLobPropertyAccessEmbed().setClobValuePA("UpdatePA");
            findEntity.getCollectionIntegerEmbed().setCollectionInteger(CollectionIntegerEmbed.UPDATE);
            findEntity.getCollectionEnumeratedEmbed().setCollectionEnumerated(CollectionEnumeratedEmbed.UPDATE);
            findEntity.getCollectionTemporalEmbed().setCollectionDate(CollectionTemporalEmbed.UPDATE);
            findEntity.getCollectionLobEmbed().setCollectionLob(CollectionLobEmbed.UPDATE);
            findEntity.getListIntegerOrderColumnEmbed().setNotListIntegerOrderColumn(ListIntegerOrderColumnEmbed.UPDATE);
            findEntity.getListIntegerOrderByEmbed().setNotListIntegerOrderBy(ListIntegerOrderByEmbed.UPDATE);
            findEntity.getListEnumeratedEmbed().setListEnumerated(ListEnumeratedEmbed.UPDATE);
            findEntity.getListTemporalEmbed().setListDate(ListTemporalEmbed.UPDATE);
            findEntity.getListLobEmbed().setListLob(ListLobEmbed.UPDATE);
            findEntity.getSetIntegerEmbed().getNotSetInteger().remove(new Integer(1));
            findEntity.getSetIntegerEmbed().getNotSetInteger().add(new Integer(2)); // dup.
            findEntity.getSetEnumeratedEmbed().setSetEnumerated(SetEnumeratedEmbed.UPDATE);
            findEntity.getSetTemporalEmbed().setSetDate(SetTemporalEmbed.UPDATE);
            findEntity.getSetLobEmbed().setSetLob(SetLobEmbed.UPDATE);
            findEntity.getMapKeyIntegerEmbed().getNotMapKeyInteger().put(new Integer(4), new Integer(400));
            findEntity.getMapKeyIntegerValueTemporalEmbed().setMapKeyIntegerValueTemporal(MapKeyIntegerValueTemporalEmbed.UPDATE);
            findEntity.getMapKeyEnumeratedValueEnumeratedEmbed().setMapKeyEnumeratedValueEnumerated(MapKeyEnumeratedValueEnumeratedEmbed.UPDATE);
            findEntity.getMapKeyTemporalValueTemporalEmbed().setMapKeyTemporalValueTemporal(MapKeyTemporalValueTemporalEmbed.UPDATE);
            findEntity.getMapKeyEnumeratedValueLobEmbed().setMapKeyEnumeratedValueLob(MapKeyEnumeratedValueLobEmbed.UPDATE);
            findEntity.setCollectionLobPropertyAccessEmbed(new ArrayList(LobPropertyAccessEmbed.COLLECTION_UPDATE));
            findEntity.setListIntegerEmbedOrderColumn(new ArrayList(IntegerEmbed.LIST_UPDATE));
            findEntity.setListIntegerAttributeOverridesEmbedOrderColumn(new ArrayList(IntegerAttributeOverridesEmbed.LIST_UPDATE));
            findEntity.setListIntegerPropertyAccessEmbedOrderColumn(new ArrayList(IntegerPropertyAccessEmbed.LIST_UPDATE));
            findEntity.setListEnumeratedPropertyAccessEmbedOrderColumn(new ArrayList(EnumeratedPropertyAccessEmbed.LIST_UPDATE));
            findEntity.setListEnumeratedPropertyAccessEmbedOrderBy(new ArrayList(EnumeratedPropertyAccessEmbed.LIST_UPDATE));
            findEntity.setListTemporalPropertyAccessEmbedOrderColumn(new ArrayList(TemporalPropertyAccessEmbed.LIST_UPDATE));
            findEntity.getSetIntegerPropertyAccessEmbed().remove(new IntegerPropertyAccessEmbed(new Integer(1)));
            findEntity.getSetIntegerPropertyAccessEmbed().add(new IntegerPropertyAccessEmbed(new Integer(2))); // dup.
            findEntity.getMapKeyIntegerValueIntegerPropertyAccessEmbed().put(new Integer(4), new IntegerPropertyAccessEmbed(new Integer(400)));
            findEntity.setMapKeyIntegerValueTemporalPropertyAccessEmbed(new HashMap(TemporalPropertyAccessEmbed.MAP_UPDATE));
            findEntity.setMapKeyTemporalValueTemporalPropertyAccessEmbed(new HashMap(TemporalPropertyAccessEmbed.MAP2_UPDATE));
            findEntity.setMapKeyEnumeratedValueEnumeratedPropertyAccessEmbed(new HashMap(EnumeratedPropertyAccessEmbed.MAP_UPDATE));
            findEntity.getMapKeyIntegerEmbedValueIntegerEmbed().put(new IntegerEmbed(new Integer(4)), new IntegerEmbed(new Integer(400)));
            findEntity.setMapKeyLobEmbedValueLobEmbed(new HashMap(LobPropertyAccessEmbed.MAP_UPDATE));

            System.out.println("Performing merge(" + findEntity + ") operation");
            JPAEmbeddableBasicEntity remergedFindEntity = em.merge(findEntity);

            Assert.assertFalse(findEntity + " is managed by the persistence context.", em.contains(findEntity));
            Assert.assertTrue(mergedFindEntity + " is not managed by the persistence context.", em.contains(mergedFindEntity));
            Assert.assertTrue(remergedFindEntity + " is not managed by the persistence context.", em.contains(remergedFindEntity));
            Assert.assertTrue(mergedFindEntity == remergedFindEntity);

            System.out.println("Committing transaction...");
            if (tj.isTransactionActive()) {
                tj.commitTransaction();
            }

            em.clear();

            System.out.println("Performing find(" + JPAEmbeddableBasicEntity.class.getSimpleName() + ", " + id + ") operation");
            JPAEmbeddableBasicEntity updatedFindEntity = em.find(JPAEmbeddableBasicEntity.class, id);
            Assert.assertNotNull("find(" + JPAEmbeddableBasicEntity.class.getSimpleName() + ", " + id + ") did not return an entity.", updatedFindEntity);

            // Perform content verifications
            Assert.assertTrue(newEntity != updatedFindEntity);
            Assert.assertTrue(findEntity != updatedFindEntity);
            Assert.assertTrue(updatedFindEntity + " is not managed by the persistence context.", em.contains(updatedFindEntity));
            Assert.assertEquals(updatedFindEntity.getId(), findEntity.getId());
            Assert.assertEquals(findEntity.getIntegerEmbed(), updatedFindEntity.getIntegerEmbed());
            Assert.assertEquals(findEntity.getIntegerAttributeOverridesEmbed(),
                                updatedFindEntity.getIntegerAttributeOverridesEmbed());
            Assert.assertEquals(findEntity.getIntegerFieldAccessEmbed(),
                                updatedFindEntity.getIntegerFieldAccessEmbed());
            Assert.assertEquals(findEntity.getEnumeratedFieldAccessEmbed(),
                                updatedFindEntity.getEnumeratedFieldAccessEmbed());
            Assert.assertEquals(findEntity.getTemporalFieldAccessEmbed(),
                                updatedFindEntity.getTemporalFieldAccessEmbed());
            Assert.assertEquals(findEntity.getLobFieldAccessEmbed(),
                                updatedFindEntity.getLobFieldAccessEmbed());

            /*
             * If an Embeddable class has only one field and that field is transient, Ecipselink doesn't
             * create an object of type Embeddable class and set its transient field null.
             */
            if (JPAProviderImpl.ECLIPSELINK.equals(provider)) {
                Assert.assertNull(updatedFindEntity.getIntegerTransientEmbed());
            } else if (JPAProviderImpl.OPENJPA.equals(provider)) {
                Assert.assertNull(updatedFindEntity.getIntegerTransientEmbed().getTransientJavaValue());
                Assert.assertNull(updatedFindEntity.getIntegerTransientEmbed().getTransientValue());
            }

            Assert.assertNull(updatedFindEntity.getIntegerTransientEmbed().getTransientJavaValue());
            Assert.assertNull(updatedFindEntity.getIntegerTransientEmbed().getTransientValue());
            Assert.assertEquals(findEntity.getIntegerPropertyAccessEmbed(),
                                updatedFindEntity.getIntegerPropertyAccessEmbed());
            Assert.assertEquals(findEntity.getEnumeratedPropertyAccessEmbed(),
                                updatedFindEntity.getEnumeratedPropertyAccessEmbed());
            Assert.assertEquals(findEntity.getTemporalPropertyAccessEmbed(),
                                updatedFindEntity.getTemporalPropertyAccessEmbed());
            Assert.assertEquals(findEntity.getLobPropertyAccessEmbed(),
                                updatedFindEntity.getLobPropertyAccessEmbed());
            Assert.assertEquals(findEntity.getCollectionIntegerEmbed(),
                                updatedFindEntity.getCollectionIntegerEmbed());
            Assert.assertEquals(findEntity.getCollectionEnumeratedEmbed(),
                                updatedFindEntity.getCollectionEnumeratedEmbed());
            Assert.assertEquals(findEntity.getCollectionTemporalEmbed(),
                                updatedFindEntity.getCollectionTemporalEmbed());
            Assert.assertEquals(findEntity.getCollectionLobEmbed(),
                                updatedFindEntity.getCollectionLobEmbed());
            Assert.assertEquals(findEntity.getListIntegerOrderColumnEmbed(),
                                updatedFindEntity.getListIntegerOrderColumnEmbed());
            Assert.assertEquals(ListIntegerOrderByEmbed.UPDATE_ORDERED,
                                updatedFindEntity.getListIntegerOrderByEmbed().getNotListIntegerOrderBy());
            Assert.assertEquals(findEntity.getListEnumeratedEmbed(),
                                updatedFindEntity.getListEnumeratedEmbed());
            Assert.assertEquals(findEntity.getListTemporalEmbed(),
                                updatedFindEntity.getListTemporalEmbed());
            Assert.assertEquals(findEntity.getListLobEmbed(),
                                updatedFindEntity.getListLobEmbed());
            Assert.assertEquals(findEntity.getSetIntegerEmbed(), updatedFindEntity.getSetIntegerEmbed());
            Assert.assertEquals(findEntity.getSetEnumeratedEmbed(), updatedFindEntity.getSetEnumeratedEmbed());
            Assert.assertEquals(findEntity.getSetTemporalEmbed(), updatedFindEntity.getSetTemporalEmbed());
            Assert.assertEquals(findEntity.getSetLobEmbed(), updatedFindEntity.getSetLobEmbed());
            Assert.assertEquals(findEntity.getMapKeyIntegerEmbed(),
                                updatedFindEntity.getMapKeyIntegerEmbed());
            Assert.assertEquals(findEntity.getMapKeyIntegerValueTemporalEmbed(),
                                updatedFindEntity.getMapKeyIntegerValueTemporalEmbed());
            Assert.assertEquals(findEntity.getMapKeyEnumeratedValueEnumeratedEmbed(), updatedFindEntity.getMapKeyEnumeratedValueEnumeratedEmbed());
            Assert.assertEquals(findEntity.getMapKeyTemporalValueTemporalEmbed(),
                                updatedFindEntity.getMapKeyTemporalValueTemporalEmbed());
            Assert.assertEquals(findEntity.getMapKeyEnumeratedValueLobEmbed(),
                                updatedFindEntity.getMapKeyEnumeratedValueLobEmbed());
            Assert.assertEquals(findEntity.getCollectionLobPropertyAccessEmbed(), updatedFindEntity.getCollectionLobPropertyAccessEmbed());
            Assert.assertEquals(findEntity.getListIntegerEmbedOrderColumn(),
                                updatedFindEntity.getListIntegerEmbedOrderColumn());
            Assert.assertEquals(findEntity.getListIntegerAttributeOverridesEmbedOrderColumn(), updatedFindEntity.getListIntegerAttributeOverridesEmbedOrderColumn());
            Assert.assertEquals(findEntity.getListIntegerPropertyAccessEmbedOrderColumn(), updatedFindEntity.getListIntegerPropertyAccessEmbedOrderColumn());
            Assert.assertEquals(findEntity.getListEnumeratedPropertyAccessEmbedOrderColumn(), updatedFindEntity.getListEnumeratedPropertyAccessEmbedOrderColumn());
            Assert.assertEquals(EnumeratedPropertyAccessEmbed.LIST_UPDATE_ORDERED,
                                updatedFindEntity.getListEnumeratedPropertyAccessEmbedOrderBy());
            Assert.assertEquals(findEntity.getListTemporalPropertyAccessEmbedOrderColumn(), updatedFindEntity.getListTemporalPropertyAccessEmbedOrderColumn());
            Assert.assertEquals(findEntity.getSetIntegerPropertyAccessEmbed(),
                                updatedFindEntity.getSetIntegerPropertyAccessEmbed());
            Assert.assertEquals(findEntity.getMapKeyIntegerValueIntegerPropertyAccessEmbed(), updatedFindEntity.getMapKeyIntegerValueIntegerPropertyAccessEmbed());
            Assert.assertEquals(findEntity.getMapKeyIntegerValueTemporalPropertyAccessEmbed(),
                                updatedFindEntity.getMapKeyIntegerValueTemporalPropertyAccessEmbed());
            Assert.assertEquals(findEntity.getMapKeyTemporalValueTemporalPropertyAccessEmbed(),
                                updatedFindEntity.getMapKeyTemporalValueTemporalPropertyAccessEmbed());
            Assert.assertEquals(findEntity.getMapKeyEnumeratedValueEnumeratedPropertyAccessEmbed(),
                                updatedFindEntity.getMapKeyEnumeratedValueEnumeratedPropertyAccessEmbed());
            Assert.assertEquals(findEntity.getMapKeyIntegerEmbedValueIntegerEmbed(), updatedFindEntity.getMapKeyIntegerEmbedValueIntegerEmbed());
            Assert.assertEquals(findEntity.getMapKeyLobEmbedValueLobEmbed(),
                                updatedFindEntity.getMapKeyLobEmbedValueLobEmbed());

            System.out.println("Beginning new transaction...");
            tj.beginTransaction();
            if (tj.isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                em.joinTransaction();
            }

            // Delete the entity from the database
            System.out.println("Performing find(" + JPAEmbeddableBasicEntity.class.getSimpleName() + ", " + id + ") operation");
            JPAEmbeddableBasicEntity findRemovedEntity = em.find(JPAEmbeddableBasicEntity.class, id);
            Assert.assertNotNull("find(" + JPAEmbeddableBasicEntity.class.getSimpleName() + ", " + id + ") did not return an entity.", findRemovedEntity);

            System.out.println("Performing remove(" + findRemovedEntity + ") operation");
            em.remove(findRemovedEntity);

            System.out.println("Committing transaction...");
            if (tj.isTransactionActive()) {
                tj.commitTransaction();
            }

            em.clear();

            // Verify the entity remove was successful
            System.out.println("Performing find(" + JPAEmbeddableBasicEntity.class.getSimpleName() + ", " + id + ") operation");
            findRemovedEntity = em.find(JPAEmbeddableBasicEntity.class, id);
            Assert.assertNull("find(" + JPAEmbeddableBasicEntity.class.getSimpleName() + ", " + id + ") returned an entity.", findRemovedEntity);
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

    @SuppressWarnings({ "unchecked", "rawtypes", "deprecation" })
    public void testEmbeddableBasic06(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        JPAProviderImpl provider = getJPAProviderImpl(jpaResource);

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            int id = 6;

            XMLJPAEmbeddableBasicEntity newEntity = new XMLJPAEmbeddableBasicEntity();
            newEntity.setId(id);
            newEntity.setIntegerEmbed(new XMLIntegerEmbed(11));
            newEntity.setIntegerAttributeOverridesEmbed(new XMLIntegerAttributeOverridesEmbed(22));
            newEntity.setIntegerFieldAccessEmbed(new XMLIntegerFieldAccessEmbed(33));
            newEntity.setEnumeratedFieldAccessEmbed(new XMLEnumeratedFieldAccessEmbed(XMLEnumeratedFieldAccessEnum.ONE));
            newEntity.setTemporalFieldAccessEmbed(new XMLTemporalFieldAccessEmbed(new Date()));
            newEntity.setLobFieldAccessEmbed(new XMLLobFieldAccessEmbed("InitFA"));
            newEntity.setIntegerTransientEmbed(new XMLIntegerTransientEmbed(44, 55));
            newEntity.setIntegerPropertyAccessEmbed(new XMLIntegerPropertyAccessEmbed(66));
            newEntity.setEnumeratedPropertyAccessEmbed(new XMLEnumeratedPropertyAccessEmbed(XMLEnumeratedPropertyAccessEnum.TWO));
            newEntity.setTemporalPropertyAccessEmbed(new XMLTemporalPropertyAccessEmbed(new Date()));
            newEntity.setLobPropertyAccessEmbed(new XMLLobPropertyAccessEmbed("InitPA"));
            newEntity.setCollectionIntegerEmbed(new XMLCollectionIntegerEmbed(XMLCollectionIntegerEmbed.INIT));
            newEntity.setCollectionEnumeratedEmbed(new XMLCollectionEnumeratedEmbed(XMLCollectionEnumeratedEmbed.INIT));
            newEntity.setCollectionTemporalEmbed(new XMLCollectionTemporalEmbed(XMLCollectionTemporalEmbed.INIT));
            newEntity.setCollectionLobEmbed(new XMLCollectionLobEmbed(XMLCollectionLobEmbed.INIT));
            newEntity.setListIntegerOrderColumnEmbed(new XMLListIntegerOrderColumnEmbed(XMLListIntegerOrderColumnEmbed.INIT));
            newEntity.setListIntegerOrderByEmbed(new XMLListIntegerOrderByEmbed(XMLListIntegerOrderByEmbed.INIT));
            newEntity.setListEnumeratedEmbed(new XMLListEnumeratedEmbed(XMLListEnumeratedEmbed.INIT));
            newEntity.setListTemporalEmbed(new XMLListTemporalEmbed(XMLListTemporalEmbed.INIT));
            newEntity.setListLobEmbed(new XMLListLobEmbed(XMLListLobEmbed.INIT));
            newEntity.setSetIntegerEmbed(new XMLSetIntegerEmbed(XMLSetIntegerEmbed.INIT));
            newEntity.setSetEnumeratedEmbed(new XMLSetEnumeratedEmbed(XMLSetEnumeratedEmbed.INIT));
            newEntity.setSetTemporalEmbed(new XMLSetTemporalEmbed(XMLSetTemporalEmbed.INIT));
            newEntity.setSetLobEmbed(new XMLSetLobEmbed(XMLSetLobEmbed.INIT));
            newEntity.setMapKeyIntegerEmbed(new XMLMapKeyIntegerEmbed(XMLMapKeyIntegerEmbed.INIT));
            newEntity.setMapKeyIntegerValueTemporalEmbed(new XMLMapKeyIntegerValueTemporalEmbed(XMLMapKeyIntegerValueTemporalEmbed.INIT));
            newEntity.setMapKeyEnumeratedValueEnumeratedEmbed(new XMLMapKeyEnumeratedValueEnumeratedEmbed(XMLMapKeyEnumeratedValueEnumeratedEmbed.INIT));
            newEntity.setMapKeyTemporalValueTemporalEmbed(new XMLMapKeyTemporalValueTemporalEmbed(XMLMapKeyTemporalValueTemporalEmbed.INIT));
            newEntity.setMapKeyEnumeratedValueLobEmbed(new XMLMapKeyEnumeratedValueLobEmbed(XMLMapKeyEnumeratedValueLobEmbed.INIT));
            newEntity.setCollectionLobPropertyAccessEmbed(new ArrayList(XMLLobPropertyAccessEmbed.COLLECTION_INIT));
            newEntity.setListIntegerEmbedOrderColumn(new ArrayList(XMLIntegerEmbed.LIST_INIT));
            newEntity.setListIntegerAttributeOverridesEmbedOrderColumn(new ArrayList(XMLIntegerAttributeOverridesEmbed.LIST_INIT));
            newEntity.setListIntegerPropertyAccessEmbedOrderColumn(new ArrayList(XMLIntegerPropertyAccessEmbed.LIST_INIT));
            newEntity.setListEnumeratedPropertyAccessEmbedOrderColumn(new ArrayList(XMLEnumeratedPropertyAccessEmbed.LIST_INIT));
            newEntity.setListEnumeratedPropertyAccessEmbedOrderBy(new ArrayList(XMLEnumeratedPropertyAccessEmbed.LIST_INIT));
            newEntity.setListTemporalPropertyAccessEmbedOrderColumn(new ArrayList(XMLTemporalPropertyAccessEmbed.LIST_INIT));
            newEntity.setSetIntegerPropertyAccessEmbed(new HashSet(XMLIntegerPropertyAccessEmbed.SET_INIT));
//            newEntity.setMapKeyIntegerValueIntegerPropertyAccessEmbed(new HashMap(XMLIntegerPropertyAccessEmbed.MAP_INIT));
            newEntity.setMapKeyIntegerValueTemporalPropertyAccessEmbed(new HashMap(XMLTemporalPropertyAccessEmbed.MAP_INIT));
            newEntity.setMapKeyTemporalValueTemporalPropertyAccessEmbed(new HashMap(XMLTemporalPropertyAccessEmbed.MAP2_INIT));
//            newEntity.setMapKeyEnumeratedValueEnumeratedPropertyAccessEmbed(new HashMap(XMLEnumeratedPropertyAccessEmbed.MAP_INIT));
//            newEntity.setMapKeyIntegerEmbedValueIntegerEmbed(new HashMap(XMLIntegerEmbed.MAP_INIT2));
//            newEntity.setMapKeyLobEmbedValueLobEmbed(new HashMap(XMLLobPropertyAccessEmbed.MAP_INIT));

            System.out.println("Beginning new transaction...");
            tj.beginTransaction();
            if (tj.isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                em.joinTransaction();
            }

            System.out.println("Performing merge(" + newEntity + ") operation");
            XMLJPAEmbeddableBasicEntity mergedNewEntity = em.merge(newEntity);
            Assert.assertFalse(newEntity + " is managed by the persistence context.", em.contains(newEntity));
            Assert.assertTrue(mergedNewEntity + " is not managed by the persistence context.", em.contains(mergedNewEntity));

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

            System.out.println("Performing find(" + XMLJPAEmbeddableBasicEntity.class.getSimpleName() + ", " + id + ") operation");
            XMLJPAEmbeddableBasicEntity findEntity = em.find(XMLJPAEmbeddableBasicEntity.class, id);
            Assert.assertNotNull("find(" + XMLJPAEmbeddableBasicEntity.class.getSimpleName() + ", " + id + ") did not return an entity.", findEntity);

            // Perform content verifications
            Assert.assertTrue(newEntity != findEntity);
            Assert.assertTrue(findEntity + " is not managed by the persistence context.", em.contains(findEntity));
            Assert.assertEquals(newEntity.getId(), findEntity.getId());
            Assert.assertEquals(newEntity.getIntegerEmbed(), findEntity.getIntegerEmbed());
            Assert.assertEquals(newEntity.getIntegerAttributeOverridesEmbed(),
                                findEntity.getIntegerAttributeOverridesEmbed());
            Assert.assertEquals(newEntity.getIntegerFieldAccessEmbed(),
                                findEntity.getIntegerFieldAccessEmbed());
            Assert.assertEquals(newEntity.getEnumeratedFieldAccessEmbed(),
                                findEntity.getEnumeratedFieldAccessEmbed());
            Assert.assertEquals(newEntity.getTemporalFieldAccessEmbed(), findEntity.getTemporalFieldAccessEmbed());
            Assert.assertEquals(newEntity.getLobFieldAccessEmbed(), findEntity.getLobFieldAccessEmbed());

            /*
             * If an Embeddable class has only one field and that field is transient, Ecipselink doesn't
             * create an object of type Embeddable class and set its transient field null.
             */
            if (JPAProviderImpl.ECLIPSELINK.equals(provider)) {
                Assert.assertNull(findEntity.getIntegerTransientEmbed());
            } else if (JPAProviderImpl.OPENJPA.equals(provider)) {
                Assert.assertNull(findEntity.getIntegerTransientEmbed().getTransientJavaValue());
                Assert.assertNull(findEntity.getIntegerTransientEmbed().getTransientValue());
            }

            Assert.assertNull(findEntity.getIntegerTransientEmbed().getTransientJavaValue());
            Assert.assertNull(findEntity.getIntegerTransientEmbed().getTransientValue());
            Assert.assertEquals(newEntity.getIntegerPropertyAccessEmbed(),
                                findEntity.getIntegerPropertyAccessEmbed());
            Assert.assertEquals(newEntity.getEnumeratedPropertyAccessEmbed(),
                                findEntity.getEnumeratedPropertyAccessEmbed());
            Assert.assertEquals(newEntity.getTemporalPropertyAccessEmbed(),
                                findEntity.getTemporalPropertyAccessEmbed());
            Assert.assertEquals(newEntity.getLobPropertyAccessEmbed(), findEntity.getLobPropertyAccessEmbed());
            Assert.assertEquals(newEntity.getCollectionIntegerEmbed(),
                                findEntity.getCollectionIntegerEmbed());
            Assert.assertEquals(newEntity.getCollectionEnumeratedEmbed(),
                                findEntity.getCollectionEnumeratedEmbed());
            Assert.assertEquals(newEntity.getCollectionTemporalEmbed(),
                                findEntity.getCollectionTemporalEmbed());
            Assert.assertEquals(newEntity.getCollectionLobEmbed(),
                                findEntity.getCollectionLobEmbed());
            Assert.assertEquals(newEntity.getListIntegerOrderColumnEmbed(),
                                findEntity.getListIntegerOrderColumnEmbed());
            Assert.assertEquals(XMLListIntegerOrderByEmbed.INIT_ORDERED,
                                findEntity.getListIntegerOrderByEmbed().getNotListIntegerOrderBy());
            Assert.assertEquals(newEntity.getListEnumeratedEmbed(),
                                findEntity.getListEnumeratedEmbed());
            Assert.assertEquals(newEntity.getListTemporalEmbed(),
                                findEntity.getListTemporalEmbed());
            Assert.assertEquals(newEntity.getListLobEmbed(),
                                findEntity.getListLobEmbed());
            Assert.assertEquals(newEntity.getSetIntegerEmbed(), findEntity.getSetIntegerEmbed());
            Assert.assertEquals(newEntity.getSetEnumeratedEmbed(), findEntity.getSetEnumeratedEmbed());
            Assert.assertEquals(newEntity.getSetTemporalEmbed(), findEntity.getSetTemporalEmbed());
            Assert.assertEquals(newEntity.getSetLobEmbed(), findEntity.getSetLobEmbed());
            Assert.assertEquals(newEntity.getMapKeyIntegerEmbed(), findEntity.getMapKeyIntegerEmbed());
            Assert.assertEquals(newEntity.getMapKeyIntegerValueTemporalEmbed(),
                                findEntity.getMapKeyIntegerValueTemporalEmbed());
            Assert.assertEquals(newEntity.getMapKeyEnumeratedValueEnumeratedEmbed(),
                                findEntity.getMapKeyEnumeratedValueEnumeratedEmbed());
            Assert.assertEquals(newEntity.getMapKeyTemporalValueTemporalEmbed(),
                                findEntity.getMapKeyTemporalValueTemporalEmbed());
            Assert.assertEquals(newEntity.getMapKeyEnumeratedValueLobEmbed(),
                                findEntity.getMapKeyEnumeratedValueLobEmbed());
            Assert.assertEquals(newEntity.getCollectionLobPropertyAccessEmbed(),
                                findEntity.getCollectionLobPropertyAccessEmbed());
            Assert.assertEquals(newEntity.getListIntegerEmbedOrderColumn(),
                                findEntity.getListIntegerEmbedOrderColumn());
            Assert.assertEquals(newEntity.getListIntegerAttributeOverridesEmbedOrderColumn(), findEntity.getListIntegerAttributeOverridesEmbedOrderColumn());
            Assert.assertEquals(newEntity.getListIntegerPropertyAccessEmbedOrderColumn(), findEntity.getListIntegerPropertyAccessEmbedOrderColumn());
            Assert.assertEquals(newEntity.getListEnumeratedPropertyAccessEmbedOrderColumn(), findEntity.getListEnumeratedPropertyAccessEmbedOrderColumn());
            Assert.assertEquals(XMLEnumeratedPropertyAccessEmbed.LIST_INIT_ORDERED, findEntity.getListEnumeratedPropertyAccessEmbedOrderBy());
            Assert.assertEquals(newEntity.getListTemporalPropertyAccessEmbedOrderColumn(), findEntity.getListTemporalPropertyAccessEmbedOrderColumn());
            Assert.assertEquals(newEntity.getSetIntegerPropertyAccessEmbed(),
                                findEntity.getSetIntegerPropertyAccessEmbed());
//            Assert.assertEquals(newEntity.getMapKeyIntegerValueIntegerPropertyAccessEmbed(), findEntity.getMapKeyIntegerValueIntegerPropertyAccessEmbed());
            Assert.assertEquals(newEntity.getMapKeyIntegerValueTemporalPropertyAccessEmbed(), findEntity.getMapKeyIntegerValueTemporalPropertyAccessEmbed());
            Assert.assertEquals(newEntity.getMapKeyTemporalValueTemporalPropertyAccessEmbed(),
                                findEntity.getMapKeyTemporalValueTemporalPropertyAccessEmbed());
//            Assert.assertEquals(newEntity.getMapKeyEnumeratedValueEnumeratedPropertyAccessEmbed(),
//                                findEntity.getMapKeyEnumeratedValueEnumeratedPropertyAccessEmbed());
//            Assert.assertEquals(newEntity.getMapKeyIntegerEmbedValueIntegerEmbed(),
//                                findEntity.getMapKeyIntegerEmbedValueIntegerEmbed());
//            Assert.assertEquals(newEntity.getMapKeyLobEmbedValueLobEmbed(),
//                                findEntity.getMapKeyLobEmbedValueLobEmbed());

            em.clear();

            System.out.println("Performing contains(" + findEntity + ") operation");
            Assert.assertFalse(findEntity + " is managed by the persistence context.", em.contains(findEntity));

            System.out.println("Performing merge(" + findEntity + ") operation");
            XMLJPAEmbeddableBasicEntity mergedFindEntity = em.merge(findEntity);

            Assert.assertFalse(findEntity + " is managed by the persistence context.", em.contains(findEntity));
            Assert.assertTrue(mergedFindEntity + " is not managed by the persistence context.", em.contains(mergedFindEntity));

            // Update the entity
            // Don't touch ID.
            findEntity.getIntegerEmbed().setIntegerValue(111);
            findEntity.getIntegerAttributeOverridesEmbed().setNotIntegerValue(222);
            findEntity.getIntegerFieldAccessEmbed().setIntegerValueFieldAccessColumn(333);
            findEntity.getEnumeratedFieldAccessEmbed().setEnumeratedStringValueFA(XMLEnumeratedFieldAccessEnum.TWO);
            findEntity.getEnumeratedFieldAccessEmbed().setEnumeratedOrdinalValueFA(XMLEnumeratedFieldAccessEnum.TWO);
            findEntity.getTemporalFieldAccessEmbed().setTemporalValueFA(new Date(25000));
            findEntity.getLobFieldAccessEmbed().setClobValueFA("UpdateFA");

            /*
             * If an Embeddable class has only one field and that field is transient, Ecipselink doesn't
             * create an object of type Embeddable class and set its transient field null.
             */
            if (JPAProviderImpl.ECLIPSELINK.equals(provider)) {
                findEntity.setIntegerTransientEmbed(new XMLIntegerTransientEmbed());
            }
            findEntity.getIntegerTransientEmbed().setTransientJavaValue(444);
            findEntity.getIntegerTransientEmbed().setTransientValue(555);

            findEntity.getIntegerPropertyAccessEmbed().setIntegerValuePropertyAccessColumn(666);
            findEntity.getEnumeratedPropertyAccessEmbed().setEnumeratedStringValuePA(XMLEnumeratedPropertyAccessEnum.THREE);
            findEntity.getEnumeratedPropertyAccessEmbed().setEnumeratedOrdinalValuePA(XMLEnumeratedPropertyAccessEnum.THREE);
            findEntity.getTemporalPropertyAccessEmbed().setTemporalValuePA(new Date(25000));
            findEntity.getLobPropertyAccessEmbed().setClobValuePA("UpdatePA");
            findEntity.getCollectionIntegerEmbed().setCollectionInteger(XMLCollectionIntegerEmbed.UPDATE);
            findEntity.getCollectionEnumeratedEmbed().setCollectionEnumerated(XMLCollectionEnumeratedEmbed.UPDATE);
            findEntity.getCollectionTemporalEmbed().setCollectionDate(XMLCollectionTemporalEmbed.UPDATE);
            findEntity.getCollectionLobEmbed().setCollectionLob(XMLCollectionLobEmbed.UPDATE);
            findEntity.getListIntegerOrderColumnEmbed().setNotListIntegerOrderColumn(XMLListIntegerOrderColumnEmbed.UPDATE);
            findEntity.getListIntegerOrderByEmbed().setNotListIntegerOrderBy(XMLListIntegerOrderByEmbed.UPDATE);
            findEntity.getListEnumeratedEmbed().setListEnumerated(XMLListEnumeratedEmbed.UPDATE);
            findEntity.getListTemporalEmbed().setListDate(XMLListTemporalEmbed.UPDATE);
            findEntity.getListLobEmbed().setListLob(XMLListLobEmbed.UPDATE);
            findEntity.getSetIntegerEmbed().getNotSetInteger().remove(new Integer(1));
            findEntity.getSetIntegerEmbed().getNotSetInteger().add(new Integer(2)); // dup.
            findEntity.getSetEnumeratedEmbed().setSetEnumerated(XMLSetEnumeratedEmbed.UPDATE);
            findEntity.getSetTemporalEmbed().setSetDate(XMLSetTemporalEmbed.UPDATE);
            findEntity.getSetLobEmbed().setSetLob(XMLSetLobEmbed.UPDATE);
            findEntity.getMapKeyIntegerEmbed().getNotMapKeyInteger().put(new Integer(4), new Integer(400));
            findEntity.getMapKeyIntegerValueTemporalEmbed().setMapKeyIntegerValueTemporal(XMLMapKeyIntegerValueTemporalEmbed.UPDATE);
            findEntity.getMapKeyEnumeratedValueEnumeratedEmbed().setMapKeyEnumeratedValueEnumerated(XMLMapKeyEnumeratedValueEnumeratedEmbed.UPDATE);
            findEntity.getMapKeyTemporalValueTemporalEmbed().setMapKeyTemporalValueTemporal(XMLMapKeyTemporalValueTemporalEmbed.UPDATE);
            findEntity.getMapKeyEnumeratedValueLobEmbed().setMapKeyEnumeratedValueLob(XMLMapKeyEnumeratedValueLobEmbed.UPDATE);
            findEntity.setCollectionLobPropertyAccessEmbed(new ArrayList(XMLLobPropertyAccessEmbed.COLLECTION_UPDATE));
            findEntity.setListIntegerEmbedOrderColumn(new ArrayList(XMLIntegerEmbed.LIST_UPDATE));
            findEntity.setListIntegerAttributeOverridesEmbedOrderColumn(new ArrayList(XMLIntegerAttributeOverridesEmbed.LIST_UPDATE));
            findEntity.setListIntegerPropertyAccessEmbedOrderColumn(new ArrayList(XMLIntegerPropertyAccessEmbed.LIST_UPDATE));
            findEntity.setListEnumeratedPropertyAccessEmbedOrderColumn(new ArrayList(XMLEnumeratedPropertyAccessEmbed.LIST_UPDATE));
            findEntity.setListEnumeratedPropertyAccessEmbedOrderBy(new ArrayList(XMLEnumeratedPropertyAccessEmbed.LIST_UPDATE));
            findEntity.setListTemporalPropertyAccessEmbedOrderColumn(new ArrayList(XMLTemporalPropertyAccessEmbed.LIST_UPDATE));
            findEntity.getSetIntegerPropertyAccessEmbed().remove(new XMLIntegerPropertyAccessEmbed(new Integer(1)));
            findEntity.getSetIntegerPropertyAccessEmbed().add(new XMLIntegerPropertyAccessEmbed(new Integer(2))); // dup.
//            findEntity.getMapKeyIntegerValueIntegerPropertyAccessEmbed().put(new Integer(4), new XMLIntegerPropertyAccessEmbed(new Integer(400)));
            findEntity.setMapKeyIntegerValueTemporalPropertyAccessEmbed(new HashMap(XMLTemporalPropertyAccessEmbed.MAP_UPDATE));
            findEntity.setMapKeyTemporalValueTemporalPropertyAccessEmbed(new HashMap(XMLTemporalPropertyAccessEmbed.MAP2_UPDATE));
//            findEntity.setMapKeyEnumeratedValueEnumeratedPropertyAccessEmbed(new HashMap(XMLEnumeratedPropertyAccessEmbed.MAP_UPDATE));
//            findEntity.getMapKeyIntegerEmbedValueIntegerEmbed().put(new XMLIntegerEmbed(new Integer(4)), new XMLIntegerEmbed(new Integer(400)));
//            findEntity.setMapKeyLobEmbedValueLobEmbed(new HashMap(XMLLobPropertyAccessEmbed.MAP_UPDATE));

            System.out.println("Performing merge(" + findEntity + ") operation");
            XMLJPAEmbeddableBasicEntity remergedFindEntity = em.merge(findEntity);

            Assert.assertFalse(findEntity + " is managed by the persistence context.", em.contains(findEntity));
            Assert.assertTrue(mergedFindEntity + " is not managed by the persistence context.", em.contains(mergedFindEntity));
            Assert.assertTrue(remergedFindEntity + " is not managed by the persistence context.", em.contains(remergedFindEntity));
            Assert.assertTrue(mergedFindEntity == remergedFindEntity);

            System.out.println("Committing transaction...");
            if (tj.isTransactionActive()) {
                tj.commitTransaction();
            }

            em.clear();

            System.out.println("Performing find(" + XMLJPAEmbeddableBasicEntity.class.getSimpleName() + ", " + id + ") operation");
            XMLJPAEmbeddableBasicEntity updatedFindEntity = em.find(XMLJPAEmbeddableBasicEntity.class, id);
            Assert.assertNotNull("find(" + XMLJPAEmbeddableBasicEntity.class.getSimpleName() + ", " + id + ") did not return an entity.", updatedFindEntity);

            // Perform content verifications
            Assert.assertTrue(newEntity != updatedFindEntity);
            Assert.assertTrue(findEntity != updatedFindEntity);
            Assert.assertTrue(updatedFindEntity + " is not managed by the persistence context.", em.contains(updatedFindEntity));
            Assert.assertEquals(updatedFindEntity.getId(), findEntity.getId());
            Assert.assertEquals(findEntity.getIntegerEmbed(), updatedFindEntity.getIntegerEmbed());
            Assert.assertEquals(findEntity.getIntegerAttributeOverridesEmbed(),
                                updatedFindEntity.getIntegerAttributeOverridesEmbed());
            Assert.assertEquals(findEntity.getIntegerFieldAccessEmbed(),
                                updatedFindEntity.getIntegerFieldAccessEmbed());
            Assert.assertEquals(findEntity.getEnumeratedFieldAccessEmbed(),
                                updatedFindEntity.getEnumeratedFieldAccessEmbed());
            Assert.assertEquals(findEntity.getTemporalFieldAccessEmbed(),
                                updatedFindEntity.getTemporalFieldAccessEmbed());
            Assert.assertEquals(findEntity.getLobFieldAccessEmbed(),
                                updatedFindEntity.getLobFieldAccessEmbed());

            /*
             * If an Embeddable class has only one field and that field is transient, Ecipselink doesn't
             * create an object of type Embeddable class and set its transient field null.
             */
            if (JPAProviderImpl.ECLIPSELINK.equals(provider)) {
                Assert.assertNull(updatedFindEntity.getIntegerTransientEmbed());
            } else if (JPAProviderImpl.OPENJPA.equals(provider)) {
                Assert.assertNull(updatedFindEntity.getIntegerTransientEmbed().getTransientJavaValue());
                Assert.assertNull(updatedFindEntity.getIntegerTransientEmbed().getTransientValue());
            }

            Assert.assertEquals(findEntity.getIntegerPropertyAccessEmbed(),
                                updatedFindEntity.getIntegerPropertyAccessEmbed());
            Assert.assertEquals(findEntity.getEnumeratedPropertyAccessEmbed(),
                                updatedFindEntity.getEnumeratedPropertyAccessEmbed());
            Assert.assertEquals(findEntity.getTemporalPropertyAccessEmbed(),
                                updatedFindEntity.getTemporalPropertyAccessEmbed());
            Assert.assertEquals(findEntity.getLobPropertyAccessEmbed(),
                                updatedFindEntity.getLobPropertyAccessEmbed());
            Assert.assertEquals(findEntity.getCollectionIntegerEmbed(),
                                updatedFindEntity.getCollectionIntegerEmbed());
            Assert.assertEquals(findEntity.getCollectionEnumeratedEmbed(),
                                updatedFindEntity.getCollectionEnumeratedEmbed());
            Assert.assertEquals(findEntity.getCollectionTemporalEmbed(),
                                updatedFindEntity.getCollectionTemporalEmbed());
            Assert.assertEquals(findEntity.getCollectionLobEmbed(),
                                updatedFindEntity.getCollectionLobEmbed());
            Assert.assertEquals(findEntity.getListIntegerOrderColumnEmbed(),
                                updatedFindEntity.getListIntegerOrderColumnEmbed());
            Assert.assertEquals(XMLListIntegerOrderByEmbed.UPDATE_ORDERED,
                                updatedFindEntity.getListIntegerOrderByEmbed().getNotListIntegerOrderBy());
            Assert.assertEquals(findEntity.getListEnumeratedEmbed(),
                                updatedFindEntity.getListEnumeratedEmbed());
            Assert.assertEquals(findEntity.getListTemporalEmbed(),
                                updatedFindEntity.getListTemporalEmbed());
            Assert.assertEquals(findEntity.getListLobEmbed(),
                                updatedFindEntity.getListLobEmbed());
            Assert.assertEquals(findEntity.getSetIntegerEmbed(), updatedFindEntity.getSetIntegerEmbed());
            Assert.assertEquals(findEntity.getSetEnumeratedEmbed(), updatedFindEntity.getSetEnumeratedEmbed());
            Assert.assertEquals(findEntity.getSetTemporalEmbed(), updatedFindEntity.getSetTemporalEmbed());
            Assert.assertEquals(findEntity.getSetLobEmbed(), updatedFindEntity.getSetLobEmbed());
            Assert.assertEquals(findEntity.getMapKeyIntegerEmbed(),
                                updatedFindEntity.getMapKeyIntegerEmbed());
            Assert.assertEquals(findEntity.getMapKeyIntegerValueTemporalEmbed(),
                                updatedFindEntity.getMapKeyIntegerValueTemporalEmbed());
            Assert.assertEquals(findEntity.getMapKeyEnumeratedValueEnumeratedEmbed(), updatedFindEntity.getMapKeyEnumeratedValueEnumeratedEmbed());
            Assert.assertEquals(findEntity.getMapKeyTemporalValueTemporalEmbed(),
                                updatedFindEntity.getMapKeyTemporalValueTemporalEmbed());
            Assert.assertEquals(findEntity.getMapKeyEnumeratedValueLobEmbed(),
                                updatedFindEntity.getMapKeyEnumeratedValueLobEmbed());
            Assert.assertEquals(findEntity.getCollectionLobPropertyAccessEmbed(), updatedFindEntity.getCollectionLobPropertyAccessEmbed());
            Assert.assertEquals(findEntity.getListIntegerEmbedOrderColumn(),
                                updatedFindEntity.getListIntegerEmbedOrderColumn());
            Assert.assertEquals(findEntity.getListIntegerAttributeOverridesEmbedOrderColumn(), updatedFindEntity.getListIntegerAttributeOverridesEmbedOrderColumn());
            Assert.assertEquals(findEntity.getListIntegerPropertyAccessEmbedOrderColumn(), updatedFindEntity.getListIntegerPropertyAccessEmbedOrderColumn());
            Assert.assertEquals(findEntity.getListEnumeratedPropertyAccessEmbedOrderColumn(), updatedFindEntity.getListEnumeratedPropertyAccessEmbedOrderColumn());
            Assert.assertEquals(XMLEnumeratedPropertyAccessEmbed.LIST_UPDATE_ORDERED, updatedFindEntity.getListEnumeratedPropertyAccessEmbedOrderBy());
            Assert.assertEquals(findEntity.getListTemporalPropertyAccessEmbedOrderColumn(), updatedFindEntity.getListTemporalPropertyAccessEmbedOrderColumn());
            Assert.assertEquals(findEntity.getSetIntegerPropertyAccessEmbed(),
                                updatedFindEntity.getSetIntegerPropertyAccessEmbed());
//            Assert.assertEquals(findEntity.getMapKeyIntegerValueIntegerPropertyAccessEmbed(), updatedFindEntity.getMapKeyIntegerValueIntegerPropertyAccessEmbed());
            Assert.assertEquals(findEntity.getMapKeyIntegerValueTemporalPropertyAccessEmbed(),
                                updatedFindEntity.getMapKeyIntegerValueTemporalPropertyAccessEmbed());
            Assert.assertEquals(findEntity.getMapKeyTemporalValueTemporalPropertyAccessEmbed(),
                                updatedFindEntity.getMapKeyTemporalValueTemporalPropertyAccessEmbed());
//            Assert.assertEquals(findEntity.getMapKeyEnumeratedValueEnumeratedPropertyAccessEmbed(),
//                                updatedFindEntity.getMapKeyEnumeratedValueEnumeratedPropertyAccessEmbed());
//            Assert.assertEquals(findEntity.getMapKeyIntegerEmbedValueIntegerEmbed(), updatedFindEntity.getMapKeyIntegerEmbedValueIntegerEmbed());
//            Assert.assertEquals(findEntity.getMapKeyLobEmbedValueLobEmbed(),
//                                updatedFindEntity.getMapKeyLobEmbedValueLobEmbed());

            System.out.println("Beginning new transaction...");
            tj.beginTransaction();
            if (tj.isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                em.joinTransaction();
            }

            // Delete the entity from the database
            System.out.println("Performing find(" + XMLJPAEmbeddableBasicEntity.class.getSimpleName() + ", " + id + ") operation");
            XMLJPAEmbeddableBasicEntity findRemovedEntity = em.find(XMLJPAEmbeddableBasicEntity.class, id);
            Assert.assertNotNull("find(" + XMLJPAEmbeddableBasicEntity.class.getSimpleName() + ", " + id + ") did not return an entity.", findRemovedEntity);

            System.out.println("Performing remove(" + findRemovedEntity + ") operation");
            em.remove(findRemovedEntity);

            System.out.println("Committing transaction...");
            if (tj.isTransactionActive()) {
                tj.commitTransaction();
            }

            em.clear();

            // Verify the entity remove was successful
            System.out.println("Performing find(" + XMLJPAEmbeddableBasicEntity.class.getSimpleName() + ", " + id + ") operation");
            findRemovedEntity = em.find(XMLJPAEmbeddableBasicEntity.class, id);
            Assert.assertNull("find(" + XMLJPAEmbeddableBasicEntity.class.getSimpleName() + ", " + id + ") returned an entity.", findRemovedEntity);
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
