/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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

package com.ibm.ws.jpa.fvt.util.testlogic;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.PersistenceUnitUtil;
import javax.persistence.PersistenceUtil;
import javax.persistence.spi.LoadState;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceProviderResolver;
import javax.persistence.spi.PersistenceProviderResolverHolder;
import javax.persistence.spi.ProviderUtil;

import org.junit.Assert;

import com.ibm.ws.jpa.fvt.util.entities.Util1x1Lf;
import com.ibm.ws.jpa.fvt.util.entities.Util1x1Rt;
import com.ibm.ws.jpa.fvt.util.entities.Util1xmLf;
import com.ibm.ws.jpa.fvt.util.entities.Util1xmRt;
import com.ibm.ws.jpa.fvt.util.entities.UtilEmbEntity;
import com.ibm.ws.jpa.fvt.util.entities.UtilEmbeddable;
import com.ibm.ws.jpa.fvt.util.entities.UtilEmbeddable2;
import com.ibm.ws.jpa.fvt.util.entities.UtilEntity;
import com.ibm.ws.testtooling.jpaprovider.JPAPersistenceProvider;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.testlogic.AbstractTestLogic;
import com.ibm.ws.testtooling.vehicle.resources.JPAResource;
import com.ibm.ws.testtooling.vehicle.resources.TestExecutionResources;

/*
* Description: Test case to validate the following JPA 2.0 specification sections.
*
* JPA 2.0 Specifications Tested (date: FR-1/10/2009)
*    3.2.9 Load State
*       An entity is considered to be loaded if all attributes with FetchType.EAGER-whether explicitly
*       specified or by default-(including relationship and other collection-valued attributes) have been
*       loaded from the database or assigned by the application. Attributes with FetchType.LAZY may or
*       may not have been loaded. The available state of the entity instance and associated instances is as
*       described in section 3.2.7.
*
*       An attribute that is an embeddable is considered to be loaded if the embeddable attribute was loaded
*       from the database or assigned by the application, and, if the attribute references an embeddable instance
*       (i.e., is not null), the embeddable instance state is known to be loaded (i.e., all attributes of the
*       embeddable with FetchType.EAGER have been loaded from the database or assigned by the application).
*
*       A collection-valued attribute is considered to be loaded if the collection was loaded from the database
*       or the value of the attribute was assigned by the application, and, if the attribute references a collection
*       instance (i.e., is not null), each element of the collection (e.g. entity or embeddable) is considered to be
*       loaded.
*
*       A single-valued relationship attribute is considered to be loaded if the relationship attribute was loaded
*       from the database or assigned by the application, and, if the attribute references an entity instance (i.e.,
*       is not null), the entity instance state is known to be loaded.
*
*       A basic attribute is considered to be loaded if its state has been loaded from the database or assigned by
*       the application.
*
*       The PersistenceUtil.isLoaded methods can be used to determine the load state of an entity
*       and its attributes regardless of the persistence unit with which the entity is associated. The PersistenceUtil.
*       isLoaded methods return true if the above conditions hold, and false otherwise. If the
*       persistence unit is known, the PersistenceUnitUtil.isLoaded methods can be used instead.
*       See section 7.11.
*
*       Persistence provider contracts for determining the load state of an entity or entity attribute are described
*       in section 9.7.1.
*
*    7.11 PersistenceUnitUtil Interface
*       The PersistenceUnitUtil interface provides access to utility methods that can be invoked on
*       entities associated with the persistence unit. The behavior is undefined if these methods are invoked on
*       an entity instance that is not associated with the persistence unit from whose entity manager factory this
*       interface has been obtained.
*
*       Utility interface between the application and the persistence provider managing the persistence unit.
*          The methods of this interface should only be invoked on entity instances obtained from or managed by
*          entity managers for this persistence unit or on new entity instances.
*
*          public interface PersistenceUnitUtil extends PersistenceUtil {
*              public boolean isLoaded(Object entity, String attributeName);
*              public boolean isLoaded(Object entity);
*              public Object getIdentifier(Object entity);
*          }
*
*    9.4.2 javax.persistence.spi.ProviderUtil
*       The ProviderUtil interface is invoked by the PersistenceUtil implementation to determine
*       the load status of an entity or entity attribute. It is not intended to be invoked by the application.
*
*    9.6 javax.persistence.Persistence Class
*       The Persistence class is used to obtain a PersistenceUtil instance in both Java EE and Java SE environments.
*    9.7 PersistenceUtil Interface
*       The semantics of the methods of this interface are defined in section 9.7.1
*    9.7.1 Contracts for Determining the Load State of an Entity or Entity Attribute
*       The implementation of the PersistenceUtil.isLoaded(Object) method must determine the
*       list of persistence providers available in the runtime environment[87] and call the ProviderUtil.
*       isLoaded(Object) method on each of them until either:
*       - one provider returns LoadState.LOADED. In this case PersistenceUtil.isLoaded returns true.
*       - one provider returns LoadState.NOT_LOADED. In this case PersistenceUtil.isLoaded returns false.
*       - all providers return LoadState.UNKNOWN. In this case PersistenceUtil.isLoaded returns true.
*       If the PersistenceUtil implementation determines that only a single provider is available in the
*       environment, it is permitted to use provider-specific methods to determine the result of
*       isLoaded(Object) as long as the semantics defined in section 3.2.9 are observed.
*       The implementation of the PersistenceUtil.isLoaded(Object,String) method must
*       determine the list of persistence providers available in the environment and call the ProviderUtil.
*       isLoadedWithoutReference method on each of them until either:
*       - one provider returns LoadState.LOADED. In this case PersistenceUtil.isLoaded returns true.
*       - one provider returns LoadState.NOT_LOADED. In this case PersistenceUtil.isLoaded returns false.
*       - all providers return LoadState.UNKNOWN. In this case, the PersistenceUtil.isLoaded method then calls
*            ProviderUtil.isLoadedWithReference on each of the providers until:
*          - one provider returns LoadState.LOADED. In this case PersistenceUtil.isLoaded return true.
*          - one provider returns LoadState.NOT_LOADED. In this case, PersistenceUtil.isLoaded returns false.
*          - all providers return LoadState.UNKNOWN. In this case, PersistenceUtil.isLoaded returns true.
*       If the PersistenceUtil implementation determines that only a single provider is available in the
*       environment, it is permitted to use provider specific methods to determine the result of
*       isLoaded(Object, String) as long as the semantics defined in section 3.2.9 are observed.
*/
public class UtilTestLogic extends AbstractTestLogic {

    private EntityManagerFactory emf;
    private EntityManager em;
    private PersistenceProviderResolver resolver;
    private List<PersistenceProvider> providers;
    private ProviderUtil pvdrUtil;
    private PersistenceUtil pUtil;
    private PersistenceUnitUtil puUtil;

    public UtilTestLogic() {
    }

    /**
     * Test Logic: testUtilBasic
     *
     */
    public static final String TestUtilBasic = "testUtilBasic";
    public static final int Expected_TestUtilBasic_Points = 104;
    public static final int TestUtilBasic_TestRow_Id = 100;

    /**
     * Types that are used by the test case as non-entities.
     */
    private static final Set<Class<?>> NON_ENTITY_CLASSES = new HashSet<Class<?>>(Arrays.<Class<?>> asList(
                                                                                                           Object.class, String.class));

    public void testUtilBasic(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                              Object managedComponentObject) throws Exception {
        JPAResource jpaRW = testExecResources.getJpaResourceMap().get("test-jpa-resource");
//        JPAResource jpaCleanupResource = testExecResources.getJpaResourceMap().get("cleanup");
//        if (jpaCleanupResource == null) {
//            Assert.fail("Missing JPAResource 'cleanup').  Cannot execute the test.");
//            return;
//        }

        JPAPersistenceProvider jpaProvider = JPAPersistenceProvider.resolveJPAPersistenceProvider(jpaRW);

        System.out.println(TestUtilBasic +
                           ": Verify functions provided in ProviderUtil, PersistenceUtil and PersistenceUnitUtil interfaces "
                           + "against basic attribute type"
                           + testExecCtx);
        if (!initUtils(jpaRW, testExecCtx)) {
            return;
        }
        System.out.println("** Test logic start here..");
        try {
            // Cleanup the database before executing tests
            System.out.println("Cleaning up database before executing tests ...");
//            cleanupDatabase(jpaCleanupResource.getEm(), jpaCleanupResource.getTj(), UtilEntityEnum.values());
            setupTestData(jpaRW);
            System.out.println("Database cleanup complete.\n");

            EntityManager em = jpaRW.getEm();
            // Clear persistence context
            System.out.println("Clearing persistence context...");
            em.clear();
            Map<String, Object> hints = new TreeMap<String, Object>();
            if (!isUsingJPA20Feature()) {
                // For EclipseLink, use JPA 2.1 entity graphs.
                // Must access reflectively because this bucket compiles against JPA 2.0.
                Object entityGraph = EntityManager.class.getMethod("createEntityGraph", Class.class)
                                .invoke(em, UtilEntity.class);
                entityGraph.getClass()
                                .getMethod("addAttributeNodes", String[].class)
                                .invoke(entityGraph, (Object) new String[] { "name" });
                hints.put("javax.persistence.fetchgraph", entityGraph);
            }

            em.clear();
            // Test invalid entity type
            assertBasicInvalidEntityLoadState(
                                              "Test LoadState of null entity.",
                                              null,
                                              UtilEntity.class, LoadState.UNKNOWN, jpaProvider);
            assertBasicInvalidEntityLoadState(
                                              "Test LoadState of non-entity - Object.",
                                              new Object(),
                                              UtilEntity.class, LoadState.UNKNOWN, jpaProvider);
            assertBasicInvalidEntityLoadState("Test LoadState of non-entity - String",
                                              "Not Entity",
                                              UtilEntity.class, LoadState.UNKNOWN, jpaProvider);

            // Test "new entity instance" load state when not all attribute are loaded
            UtilEntity e1 = new UtilEntity();
            assertBasicNewInstanceLoadState(
                                            "Test 'new entity instance' load state when not all attributes are loaded",
                                            e1,
                                            UtilEntity.class, jpaProvider);

            // Test "new entity instance" load state when all attributes are loaded
            e1 = new UtilEntity();
            e1.setId(TestUtilBasic_TestRow_Id + 1);
            e1.setName("new instance name loaded");
            assertBasicNewInstanceLoadState(
                                            "Test 'new entity instance' load state when all attributes are loaded",
                                            e1,
                                            UtilEntity.class, jpaProvider);

            // Test basic attribute loaded state after fetch from DB.
            printTestDescription("Test basic attribute loaded state after fetch from DB. [d631756]");
            System.out.println("Begin a Tx");
            jpaRW.getTj().beginTransaction();
            if (jpaRW.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                jpaRW.getEm().joinTransaction();
            }
            e1 = em.find(UtilEntity.class, TestUtilBasic_TestRow_Id, hints);
            Assert.assertNotNull("Found UtilEntity(id=" + TestUtilBasic_TestRow_Id + ") = " + e1, e1);

            assertAttributeLoadState(
                                     // ProviderUtil.isLoaded() tests
                                     computeExpectedLoadedState(
                                                                e1, null, LoadState.LOADED,
                                                                e1, "name", LoadState.LOADED,
                                                                e1, "notLoaded", LoadState.NOT_LOADED,
                                                                e1, "unknown", JPAPersistenceProvider.ECLIPSELINK.equals(jpaProvider) ? LoadState.NOT_LOADED : LoadState.UNKNOWN),
                                     // PersistenceUtil.isLoaded() tests
                                     computeExpectedIsLoaded(
                                                             e1, null, true,
                                                             e1, "name", true,
                                                             e1, "notLoaded", false,
                                                             e1, "unknown", JPAPersistenceProvider.OPENJPA.equals(jpaProvider)),
                                     // PersistenceUnitUtil.isLoaded() tests
                                     computeExpectedIsLoaded(
                                                             e1, null, true,
                                                             e1, "name", true,
                                                             e1, "notLoaded", false,
                                                             e1, "unknown", false),
                                     e1, TestUtilBasic_TestRow_Id,
                                     UtilEntity.class, jpaProvider);

            // Test basic attribute loaded state after fetch from DB and assign new value by the application.
            printTestDescription("Test basic attribute loaded state after fetch from DB and assign new value by the application.");
            e1.setNotLoaded("new notLoaded field");
            assertAttributeLoadState(// ProviderUtil.isLoaded() tests
                                     computeExpectedLoadedState(
                                                                e1, null, LoadState.LOADED,
                                                                e1, "name", LoadState.LOADED,
                                                                e1, "notLoaded", LoadState.LOADED,
                                                                e1, "unknown", LoadState.UNKNOWN),
                                     // PersistenceUtil.isLoaded() tests
                                     computeExpectedIsLoaded(
                                                             e1, null, true,
                                                             e1, "name", true,
                                                             e1, "notLoaded", true,
                                                             e1, "unknown", true),
                                     // PersistenceUnitUtil.isLoaded() tests
                                     computeExpectedIsLoaded(
                                                             e1, null, true,
                                                             e1, "name", true,
                                                             e1, "notLoaded", true,
                                                             e1, "unknown", false),
                                     e1, TestUtilBasic_TestRow_Id, UtilEntity.class, jpaProvider);

            System.out.println("Rollback current Tx");
            jpaRW.getTj().rollbackTransaction();
        } finally {
            System.out.println(TestUtilBasic + ": End");
            clearUtils();
        }
    }

    /**
     * Test Logic: testUtilEmbeddable
     *
     */
    public static final String TestUtilEmbeddable = "testUtilEmbeddable";
    public static final int Expected_TestUtilEmbeddable_Points = 168;
    public static final int TestUtilEmbeddable_TestRow_Id = 200;

    public void testUtilEmbeddable(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                   Object managedComponentObject) {
        JPAResource jpaRW = testExecResources.getJpaResourceMap().get("test-jpa-resource");
//        JPAResource jpaCleanupResource = testExecResources.getJpaResourceMap().get("cleanup");
//        if (jpaCleanupResource == null) {
//            Assert.fail("Missing JPAResource 'cleanup').  Cannot execute the test.");
//            return;
//        }

        JPAPersistenceProvider jpaProvider = JPAPersistenceProvider.resolveJPAPersistenceProvider(jpaRW);

        System.out.println(TestUtilEmbeddable
                           + ": Verify functions provided in ProviderUtil, PersistenceUtil and PersistenceUnitUtil interfaces "
                           + "against embeddable attribute type "
                           + testExecCtx);
        if (!initUtils(jpaRW, testExecCtx)) {
            return;
        }
        System.out.println("********** Test logic start here **********");
        try {
            // Cleanup the database before executing tests
            System.out.println("Cleaning up database before executing tests ...");
//            cleanupDatabase(jpaCleanupResource.getEm(), jpaCleanupResource.getTj(), UtilEntityEnum.values());
            setupTestData(jpaRW);
            System.out.println("Database cleanup complete.\n");

            EntityManager em = jpaRW.getEm();
            // Clear persistence context
            System.out.println("Clearing persistence context...");
            em.clear();
            Map<String, Object> hints = new TreeMap<String, Object>();
            if (JPAPersistenceProvider.ECLIPSELINK.equals(jpaProvider)) {
                // For EclipseLink, use JPA 2.1 entity graphs.
                // Must access reflectively because this bucket compiles against JPA 2.0.

                // EntityGraph<UtilEmbEntity> graph = em.createEntityGraph(UtilEmbEntity.class);
                Object graph = EntityManager.class.getMethod("createEntityGraph", Class.class)
                                .invoke(em, UtilEmbEntity.class);

                // graph.addAttributeNodes("name", "emb", "emb1", "initNullEmb");
                graph.getClass()
                                .getMethod("addAttributeNodes", String[].class)
                                .invoke(graph, (Object) new String[] { "name", "emb", "emb1", "initNullEmb" });

                // *** The inclusion of initNullEmb above (and below) means we are changing the test for EclipseLink
                // because EclipseLink has no way of specifying LAZY for an embedded attribute itself.
                // By making it instead expect to load initNullEmb, we will be causing the least amount of interference
                // to the other tests.

                Method addSubgraph = graph.getClass().getMethod("addSubgraph", String.class, Class.class);
                Method addAttributeNodes = addSubgraph.getReturnType().getMethod("addAttributeNodes", String[].class);

                // TODO: the following might be needed for some tests that are disabled
                // graph.addSubgraph("emb", UtilEmbeddable.class).addAttributeNodes("embName");
                //Object subgraph = addSubgraph.invoke(graph, "emb", UtilEmbeddable.class);
                //addAttributeNodes.invoke(subgraph, (Object) new String[] { "embName" });

                // graph.addSubgraph("emb1", UtilEmbeddable.class).addAttributeNodes("embName");
                //subgraph = addSubgraph.invoke(graph, "emb1", UtilEmbeddable.class);
                //addAttributeNodes.invoke(subgraph, (Object) new String[] { "embName" });

                // graph.addSubgraph("initNullEmb", UtilEmbeddable2.class).addAttributeNodes("embName2");
                //subgraph = addSubgraph.invoke(graph, "initNullEmb", UtilEmbeddable2.class);
                //addAttributeNodes.invoke(subgraph, (Object) new String[] { "embName2" });

                hints.put("javax.persistence.fetchgraph", graph);
            }

            em.clear();
            // Test invalid entity type
            assertEmbeddableInvalidEntityLoadState(
                                                   "Test LoadState of null entity.",
                                                   null,
                                                   UtilEmbEntity.class, LoadState.UNKNOWN, jpaProvider);
            assertEmbeddableInvalidEntityLoadState(
                                                   "Test LoadState of non-entity - Object.",
                                                   new Object(),
                                                   UtilEmbEntity.class, LoadState.UNKNOWN, jpaProvider);
            assertEmbeddableInvalidEntityLoadState(
                                                   "Test LoadState of non-entity - String",
                                                   "Not Entity",
                                                   UtilEmbEntity.class, LoadState.UNKNOWN, jpaProvider);

            // Test "new entity instance" load state when not all attribute are loaded
            UtilEmbEntity e1 = new UtilEmbEntity();
            assertEmbeddableNewInstanceLoadState(
                                                 "Test 'new entity instance' load state when not all attributes are loaded",
                                                 e1,
                                                 UtilEmbEntity.class, jpaProvider);

            // Test "new entity instance" load state when all attributes are loaded
            e1 = new UtilEmbEntity();
            e1.setId(TestUtilEmbeddable_TestRow_Id + 1);
            e1.setName("new instance name loaded");
            UtilEmbeddable embeddable = new UtilEmbeddable();
            embeddable.setEmbName("new embbedable name");
            embeddable.setEmbNotLoaded("new embbedable notLoaded");
            assertEmbeddableNewInstanceLoadState(
                                                 "Test 'new entity instance' load state when all attributes are loaded",
                                                 e1,
                                                 UtilEmbEntity.class, jpaProvider);

            // Test basic attribute loaded state after fetch from DB.
            printTestDescription("Test basic attribute loaded state after fetch from DB. [d631756, OPENJPA-1430]");
            System.out.println("Begin a Tx");
            jpaRW.getTj().beginTransaction();
            if (jpaRW.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                jpaRW.getEm().joinTransaction();
            }
            e1 = em.find(UtilEmbEntity.class, TestUtilEmbeddable_TestRow_Id, hints);
            // Be careful NOT to touch e1 to trigger loading the lazily fetched fields. E.g.
            //  1) use property access to fetch field
            //  2) use field access to examine field state but no fetching content.
            //  3) toString() should not getLazyField() or directly access the field to avoid fetch content
            Assert.assertNotNull("Found UtilEmbEntity(id=" + TestUtilEmbeddable_TestRow_Id + ") = " + e1, e1);

            assertAttributeLoadState(
                                     // ProviderUtil.isLoaded() tests
                                     computeExpectedLoadedState(
                                                                e1, null, LoadState.LOADED, // all eager fields loaded
                                                                e1, "name", LoadState.LOADED, // all eager fields loaded
                                                                e1, "emb", LoadState.LOADED, // all eager fields loaded
//                        e1,  "emb.embName",          LoadState.LOADED,     // need OPENJPA-1430 feature
//                        e1,  "emb.embNotLoaded",     LoadState.NOT_LOADED, // need OPENJPA-1430 feature
                                                                e1, "emb1", LoadState.LOADED, // all eager fields loaded
                                                                e1, "initNullEmb", JPAPersistenceProvider.ECLIPSELINK.equals(jpaProvider) ? LoadState.LOADED : LoadState.NOT_LOADED, // initNullEmb == null.
                                                                e1, "unknown", LoadState.UNKNOWN),
                                     // PersistenceUtil.isLoaded() tests
                                     computeExpectedIsLoaded(
                                                             e1, null, true, // all eager fields loaded
                                                             e1, "name", true, // all eager fields loaded
                                                             e1, "emb", true, // all eager fields loaded
//                        e1,  "emb.embName",          false,                //
//                        e1,  "emb.embNotLoaded",     false,                //
                                                             e1, "emb1", true, // all eager fields loaded
                                                             e1, "initNullEmb", JPAPersistenceProvider.ECLIPSELINK.equals(jpaProvider), // initNullEmb == null
                                                             e1, "unknown", true), // All providers can't find "unknown", assume loaded
                                     // PersistenceUnitUtil.isLoaded() tests
                                     computeExpectedIsLoaded(
                                                             e1, null, true, // all eager fields loaded
                                                             e1, "name", true, // all eager fields loaded
                                                             e1, "emb", true, // all eager fields loaded
//                        e1,  "emb.embName",          false,                //
//                        e1,  "emb.embNotLoaded",     false,                //
                                                             e1, "emb1", true, // all eager fields loaded
                                                             e1, "initNullEmb", JPAPersistenceProvider.ECLIPSELINK.equals(jpaProvider), // initNullEmb == null
                                                             e1, "unknown", false), // this providers can't find "unknown", not loaded
                                     e1, TestUtilEmbeddable_TestRow_Id, UtilEmbEntity.class, jpaProvider);

            // Test basic attribute loaded state after fetch from DB and assign null to emb1.
            printTestDescription("Test basic attribute loaded state after fetch from DB and assign null to emb1. [d631756, OPENJPA-1430]");
            e1.setEmb1(null);

            assertAttributeLoadState(
                                     // ProviderUtil.isLoaded() tests
                                     computeExpectedLoadedState(
                                                                e1, null, LoadState.LOADED, // all eager fields loaded
                                                                e1, "name", LoadState.LOADED, // all eager fields loaded
                                                                e1, "emb", LoadState.LOADED, // all eager fields loaded
//                        e1,  "emb.embName",          LoadState.LOADED,     // need OPENJPA-1430 feature
//                        e1,  "emb.embNotLoaded",     LoadState.NOT_LOADED, // need OPENJPA-1430 feature
                                                                e1, "emb1", LoadState.LOADED, // emb1 assigned null
                                                                e1, "initNullEmb", JPAPersistenceProvider.ECLIPSELINK.equals(jpaProvider) ? LoadState.LOADED : LoadState.NOT_LOADED, // initNullEmb == null
                                                                e1, "unknown", LoadState.UNKNOWN),
                                     // PersistenceUtil.isLoaded() tests
                                     computeExpectedIsLoaded(
                                                             e1, null, true, // all eager fields loaded
                                                             e1, "name", true, // all eager fields loaded
                                                             e1, "emb", true, // all eager fields loaded
//                        e1,  "emb.embName",          false,                // need OPENJPA-1430 feature
//                        e1,  "emb.embNotLoaded",     false,                // need OPENJPA-1430 feature
                                                             e1, "emb1", true, // emb1 assigned null
                                                             e1, "initNullEmb", JPAPersistenceProvider.ECLIPSELINK.equals(jpaProvider), // initNullEmb == null.
                                                             e1, "unknown", true), // All providers can't find "unknown", assume loaded
                                     // PersistenceUnitUtil.isLoaded() tests
                                     computeExpectedIsLoaded(
                                                             e1, null, true, // all eager fields loaded
                                                             e1, "name", true, // all eager fields loaded
                                                             e1, "emb", true, // all eager fields loaded
//                        e1,  "emb.embName",          false,                // need OPENJPA-1430 feature
//                        e1,  "emb.embNotLoaded",     false,                // need OPENJPA-1430 feature
                                                             e1, "emb1", true, // emb1 assigned null
                                                             e1, "initNullEmb", JPAPersistenceProvider.ECLIPSELINK.equals(jpaProvider), // initNullEmb == null.
                                                             e1, "unknown", false), // this provider can't find "unknown", not loaded
                                     e1, TestUtilEmbeddable_TestRow_Id, UtilEmbEntity.class, jpaProvider);

            // Test basic attribute loaded state after fetch from DB and assign new value to emb.embNotLoaded. [d631756]
            printTestDescription("Test basic attribute loaded state after fetch from DB and assign new value to emb.embNotLoaded. [d631756,OPENJPA-1430]");
            e1.getEmb().setEmbNotLoaded("new embNotLoaded field");

            UtilEmbeddable newEmb1 = new UtilEmbeddable();
            newEmb1.setEmbName("new emb1.embName");
            newEmb1.setEmbNotLoaded("new emb1.embNotLoaded");
            e1.setEmb1(newEmb1);

            UtilEmbeddable2 newEmb = new UtilEmbeddable2();
            newEmb.setEmbName2("new initNullEmb.name");
            e1.setInitNullEmb(newEmb);

            assertAttributeLoadState(
                                     // ProviderUtil.isLoaded() tests
                                     computeExpectedLoadedState(
                                                                e1, null, LoadState.LOADED,
                                                                e1, "name", LoadState.LOADED,
                                                                e1, "emb", LoadState.LOADED,
//                        e1,  "emb.embName",          LoadState.LOADED,
//                        e1,  "embembNotLoaded",     LoadState.LOADED,
                                                                e1, "emb1", LoadState.LOADED,
                                                                e1, "initNullEmb", LoadState.LOADED,
                                                                e1, "unknown", LoadState.UNKNOWN),
                                     // PersistenceUtil.isLoaded() tests
                                     computeExpectedIsLoaded(
                                                             e1, null, true,
                                                             e1, "name", true,
                                                             e1, "emb", true,
//                        e1,  "emb.embName",          true,
//                        e1,  "emb.embNotLoaded",     true,
                                                             e1, "emb1", true,
                                                             e1, "initNullEmb", true,
                                                             e1, "unknown", true),
                                     // PersistenceUnitUtil.isLoaded() tests
                                     computeExpectedIsLoaded(
                                                             e1, null, true,
                                                             e1, "name", true,
                                                             e1, "emb", true,
//                        e1,  "emb.embName",          true,
//                        e1,  "emb.embNotLoaded",     true,
                                                             e1, "emb1", true,
                                                             e1, "initNullEmb", true,
                                                             e1, "unknown", false),
                                     e1, TestUtilEmbeddable_TestRow_Id, UtilEmbEntity.class, jpaProvider);

            System.out.println("Rollback current Tx");
            jpaRW.getTj().rollbackTransaction();
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        } finally {
            System.out.println(TestUtilEmbeddable + ": End");
            clearUtils();
        }
    }

    /**
     * Test Logic: testUtil1x1
     *
     */
    public static final String TestUtil1x1 = "testUtil1x1";
    public static final int Expected_TestUtil1x1_Points = 168;
    public static final int TestUtil1x1_TestRow_Id = 300;

    public void testUtil1x1(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                            Object managedComponentObject) {
        JPAResource jpaRW = testExecResources.getJpaResourceMap().get("test-jpa-resource");
//        JPAResource jpaCleanupResource = testExecResources.getJpaResourceMap().get("cleanup");
//        if (jpaCleanupResource == null) {
//            Assert.fail("Missing JPAResource 'cleanup').  Cannot execute the test.");
//            return;
//        }

        JPAPersistenceProvider jpaProvider = JPAPersistenceProvider.resolveJPAPersistenceProvider(jpaRW);

        System.out.println(TestUtil1x1
                           + ": Verify functions provided in ProviderUtil, PersistenceUtil and PersistenceUnitUtil interfaces "
                           + "against single-valued relationship attribute type "
                           + testExecCtx);
        if (!initUtils(jpaRW, testExecCtx)) {
            return;
        }
        System.out.println("********** Test logic start here **********");
        try {
            // Cleanup the database before executing tests
            System.out.println("Cleaning up database before executing tests ...");
//            cleanupDatabase(jpaCleanupResource.getEm(), jpaCleanupResource.getTj(), UtilEntityEnum.values());
            setupTestData(jpaRW);
            System.out.println("Database cleanup complete.\n");

            EntityManager em = jpaRW.getEm();
            // Clear persistence context
            System.out.println("Clearing persistence context...");
            em.clear();

            // Test invalid entity type
            assert1x1InvalidEntityLoadState(
                                            "Test LoadState of null entity.",
                                            null, null,
                                            Util1x1Lf.class, LoadState.UNKNOWN, jpaProvider);
            assert1x1InvalidEntityLoadState(
                                            "Test LoadState of non-entity - Object.",
                                            new Object(), null,
                                            Util1x1Lf.class, LoadState.UNKNOWN, jpaProvider);
            assert1x1InvalidEntityLoadState(
                                            "Test LoadState of non-entity - String",
                                            "Not Entity", null,
                                            Util1x1Lf.class, LoadState.UNKNOWN, jpaProvider);

            // Test "new entity instance" load state when not all attribute are loaded
            Util1x1Lf e1 = new Util1x1Lf();
            assert1x1NewInstanceLoadState(
                                          "Test 'new entity instance' load state when not all attributes are loaded",
                                          e1, null,
                                          Util1x1Lf.class, jpaProvider);

            // Test "new entity instance" load state when all attributes are loaded
            e1 = new Util1x1Lf();
            e1.setId(TestUtil1x1_TestRow_Id + 1);
            e1.setFirstName("new instance firstName");
            Util1x1Rt right = new Util1x1Rt();
            right.setId(TestUtil1x1_TestRow_Id + 11);
            right.setLastName("new instance lastName");
            e1.setUniRight(right);
            assert1x1NewInstanceLoadState(
                                          "Test 'new entity instance' load state when all attributes are loaded",
                                          e1, right,
                                          Util1x1Lf.class, jpaProvider);

            // Test basic attribute loaded state after fetch from DB.
            printTestDescription("Test basic attribute loaded state after fetch from DB. [d631756]");
            em.clear();
            System.out.println("Begin a Tx");
            jpaRW.getTj().beginTransaction();
            if (jpaRW.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                jpaRW.getEm().joinTransaction();
            }
            e1 = em.find(Util1x1Lf.class, TestUtil1x1_TestRow_Id);
            Assert.assertNotNull("Found Util1x1Lf(id=" + TestUtil1x1_TestRow_Id + ")", e1);
            Util1x1Rt eR = e1.uniRight;
            Util1x1Rt eZ = e1.uniRightLzy;
            Assert.assertNotNull("Util1x1Rt uniRight != null", eR);
            Assert.assertNull("Util1x1Rt uniRightLzy == null", eZ);

            assertAttributeLoadState(
                                     // ProviderUtil.isLoaded() tests
                                     computeExpectedLoadedState(
                                                                e1, null, LoadState.LOADED,
                                                                e1, "firstName", LoadState.LOADED,
                                                                e1, "uniRight", LoadState.LOADED,
                                                                e1, "uniRightLzy", LoadState.NOT_LOADED,
                                                                eR, "lastName", LoadState.LOADED,
                                                                eZ, "lastName", LoadState.UNKNOWN, // eZ = null
                                                                e1, "unknown", LoadState.UNKNOWN),
                                     // PersistenceUtil.isLoaded() tests
                                     computeExpectedIsLoaded(
                                                             e1, null, true,
                                                             e1, "firstName", true,
                                                             e1, "uniRight", true,
                                                             e1, "uniRightLzy", false,
                                                             eR, "lastName", true,
                                                             eZ, "lastName", true,
                                                             e1, "unknown", true),
                                     // PersistenceUnitUtil.isLoaded() tests
                                     computeExpectedIsLoaded(
                                                             e1, null, true,
                                                             e1, "firstName", true,
                                                             e1, "uniRight", true,
                                                             e1, "uniRightLzy", false,
                                                             eR, "lastName", true,
                                                             eZ, "lastName", false,
                                                             e1, "unknown", false),
                                     e1, TestUtil1x1_TestRow_Id, Util1x1Lf.class, jpaProvider);

            // Test basic attribute loaded state after fetch from DB and assign null to uniRight.
            printTestDescription("Test basic attribute loaded state after fetch from DB and assign null to uniRight. [d631756]");
            Util1x1Rt saveRight = e1.getUniRight();
            e1.setUniRight(null);

            eR = e1.uniRight;
            eZ = e1.uniRightLzy;
            Assert.assertNull("Util1x1Rt uniRight == null", eR);
            Assert.assertNull("Util1x1Rt uniRightLzy == null", eZ);

            assertAttributeLoadState(
                                     // ProviderUtil.isLoaded() tests
                                     computeExpectedLoadedState(
                                                                e1, null, LoadState.LOADED,
                                                                e1, "firstName", LoadState.LOADED,
                                                                e1, "uniRight", LoadState.LOADED,
                                                                e1, "uniRightLzy", LoadState.NOT_LOADED,
                                                                eR, "lastName", LoadState.UNKNOWN,
                                                                eZ, "lastName", LoadState.UNKNOWN,
                                                                e1, "unknown", LoadState.UNKNOWN),
                                     // PersistenceUtil.isLoaded() tests
                                     computeExpectedIsLoaded(
                                                             e1, null, true,
                                                             e1, "firstName", true,
                                                             e1, "uniRight", true,
                                                             e1, "uniRightLzy", false,
                                                             eR, "lastName", true,
                                                             eZ, "lastName", true,
                                                             e1, "unknown", true),
                                     // PersistenceUnitUtil.isLoaded() tests
                                     computeExpectedIsLoaded(
                                                             e1, null, true,
                                                             e1, "firstName", true,
                                                             e1, "uniRight", true,
                                                             e1, "uniRightLzy", false,
                                                             eR, "lastName", false,
                                                             eZ, "lastName", false,
                                                             e1, "unknown", false),
                                     e1, TestUtil1x1_TestRow_Id, Util1x1Lf.class, jpaProvider);

            // Test basic attribute loaded state after fetch from DB and assign new value to all fields.
            printTestDescription("Test basic attribute loaded state after fetch from DB and assign new value to all fields.");
            e1.setUniRight(saveRight);
            e1.getUniRightLzy().getLastName(); // force to fetch the lazy field

            eR = e1.uniRight;
            eZ = e1.uniRightLzy;
            Assert.assertNotNull("Util1x1Rt uniRight != null", eR);
            Assert.assertNotNull("Util1x1Rt uniRightLzy != null", eZ);

            assertAttributeLoadState(
                                     // ProviderUtil.isLoaded() tests
                                     computeExpectedLoadedState(
                                                                e1, null, LoadState.LOADED,
                                                                e1, "firstName", LoadState.LOADED,
                                                                e1, "uniRight", LoadState.LOADED,
                                                                e1, "uniRightLzy", LoadState.LOADED,
                                                                eR, "lastName", LoadState.LOADED,
                                                                eZ, "lastName", LoadState.LOADED,
                                                                e1, "unknown", LoadState.UNKNOWN),
                                     // PersistenceUtil.isLoaded() tests
                                     computeExpectedIsLoaded(
                                                             e1, null, true,
                                                             e1, "firstName", true,
                                                             e1, "uniRight", true,
                                                             e1, "uniRightLzy", true,
                                                             eR, "lastName", true,
                                                             eZ, "lastName", true,
                                                             e1, "unknown", true),
                                     // PersistenceUnitUtil.isLoaded() tests
                                     computeExpectedIsLoaded(
                                                             e1, null, true,
                                                             e1, "firstName", true,
                                                             e1, "uniRight", true,
                                                             e1, "uniRightLzy", true,
                                                             eR, "lastName", true,
                                                             eZ, "lastName", true,
                                                             e1, "unknown", false),
                                     e1, TestUtil1x1_TestRow_Id, Util1x1Lf.class, jpaProvider);

            System.out.println("Rollback current Tx");
            jpaRW.getTj().rollbackTransaction();

        } finally {
            System.out.println(TestUtil1x1 + ": End");
            clearUtils();
        }
    }

    /**
     * Test Logic: testUtil1xm
     *
     */
    public static final String TestUtil1xm = "testUtil1xm";
    public static final int Expected_TestUtil1xm_Points = 180;
    public static final int TestUtil1xm_TestRow_Id = 400;

    public void testUtil1xm(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                            Object managedComponentObject) {
        JPAResource jpaRW = testExecResources.getJpaResourceMap().get("test-jpa-resource");
//        JPAResource jpaCleanupResource = testExecResources.getJpaResourceMap().get("cleanup");
//        if (jpaCleanupResource == null) {
//            Assert.fail("Missing JPAResource 'cleanup').  Cannot execute the test.");
//            return;
//        }

        JPAPersistenceProvider jpaProvider = JPAPersistenceProvider.resolveJPAPersistenceProvider(jpaRW);

        System.out.println(TestUtil1xm
                           + ": Verify functions provided in ProviderUtil, PersistenceUtil and PersistenceUnitUtil interfaces "
                           + "against collection-valued relationship attribute type "
                           + testExecCtx);
        if (!initUtils(jpaRW, testExecCtx)) {
            return;
        }
        System.out.println("********** Test logic start here **********");
        try {
            // Cleanup the database before executing tests
            System.out.println("Cleaning up database before executing tests ...");
//            cleanupDatabase(jpaCleanupResource.getEm(), jpaCleanupResource.getTj(), UtilEntityEnum.values());
            setupTestData(jpaRW);
            System.out.println("Database cleanup complete.\n");

            EntityManager em = jpaRW.getEm();
            // Clear persistence context
            System.out.println("Clearing persistence context...");
            em.clear();

            // Test invalid entity type
            assert1xmInvalidEntityLoadState(
                                            "Test LoadState of null entity.",
                                            null, null,
                                            Util1xmLf.class, LoadState.UNKNOWN, jpaProvider);
            assert1xmInvalidEntityLoadState(
                                            "Test LoadState of non-entity - Object.",
                                            new Object(), null,
                                            Util1xmLf.class, LoadState.UNKNOWN, jpaProvider);
            assert1xmInvalidEntityLoadState(
                                            "Test LoadState of non-entity - String",
                                            "Not Entity", null,
                                            Util1xmLf.class, LoadState.UNKNOWN, jpaProvider);

            // Test "new entity instance" load state when not all attribute are loaded
            Util1xmLf e1 = new Util1xmLf();
            assert1xmNewInstanceLoadState(
                                          "Test 'new entity instance' load state when not all attributes are loaded",
                                          e1, null,
                                          Util1xmLf.class, jpaProvider);

            // Test "new entity instance" load state when all attributes are loaded
            e1 = new Util1xmLf();
            e1.setId(TestUtil1xm_TestRow_Id + 1);
            e1.setFirstName("new instance firstName");
            Util1xmRt right = new Util1xmRt();
            right.setId(TestUtil1xm_TestRow_Id + 11);
            right.setLastName("new instance lastName");
            e1.addUniRight(right);
            assert1xmNewInstanceLoadState("Test 'new entity instance' load state when all attributes are loaded",
                                          e1, right,
                                          Util1xmLf.class, jpaProvider);

            // Test basic attribute loaded state after fetch from DB.
            printTestDescription("Test basic attribute loaded state after fetch from DB. [d631756]");
            em.clear();
            System.out.println("Begin a Tx");
            jpaRW.getTj().beginTransaction();
            if (jpaRW.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                jpaRW.getEm().joinTransaction();
            }
            e1 = em.find(Util1xmLf.class, TestUtil1xm_TestRow_Id);
            Assert.assertNotNull("Found Util1xmLf(id=" + TestUtil1xm_TestRow_Id + ")", e1);
            if (JPAPersistenceProvider.OPENJPA.equals(jpaProvider)) {
                Collection<Util1xmRt> eRs = e1.uniRight;
                Assert.assertNull("Util1xmRt uniRight == null", eRs);
            } else {
//                results.assertPass("SKIPPED for non-OpenJPA because referencing uniRight field causes a lazy load and invalidates the test");
            }
            Collection<Util1xmRt> eEs = e1.uniRightEgr;
            Assert.assertNotNull("Util1xmRt uniRightEgr != null", eEs);
            if (eEs != null) {
                Assert.assertEquals("Util1xmRt uniRightEgr.size == 2", 2, eEs.size());
            }

            Iterator<Util1xmRt> itr;
            Util1xmRt eR1 = null;
            Util1xmRt eR2 = null;

            itr = eEs.iterator();
            Util1xmRt eE1 = itr.next();
            Util1xmRt eE2 = itr.next();

            assertAttributeLoadState(
                                     // ProviderUtil.isLoaded() tests
                                     computeExpectedLoadedState(
                                                                e1, null, LoadState.LOADED,
                                                                e1, "firstName", LoadState.LOADED,
                                                                e1, "uniRight", LoadState.NOT_LOADED,
                                                                e1, "uniRightEgr", LoadState.LOADED,
                                                                eR1, "lastName", LoadState.UNKNOWN, // eR = null
                                                                eE1, "lastName", LoadState.LOADED,
                                                                eE2, "lastName", LoadState.LOADED,
                                                                e1, "unknown", LoadState.UNKNOWN),
                                     // PersistenceUtil.isLoaded() tests
                                     computeExpectedIsLoaded(
                                                             e1, null, true,
                                                             e1, "firstName", true,
                                                             e1, "uniRight", false,
                                                             e1, "uniRightEgr", true,
                                                             eR1, "lastName", true, // uniRight = null -> eR1 = null -> UNKNOWN - true
                                                             eE1, "lastName", true,
                                                             eE2, "lastName", true,
                                                             e1, "unknown", true),
                                     // PersistenceUnitUtil.isLoaded() tests
                                     computeExpectedIsLoaded(
                                                             e1, null, true,
                                                             e1, "firstName", true,
                                                             e1, "uniRight", false,
                                                             e1, "uniRightEgr", true,
                                                             eR1, "lastName", false, // uniRight = null -> eR1 = null -> UNKNOWN - false
                                                             eE1, "lastName", true,
                                                             eE2, "lastName", true,
                                                             e1, "unknown", false),
                                     e1, TestUtil1xm_TestRow_Id, Util1xmLf.class, jpaProvider);

            // Test basic attribute loaded state after fetch from DB and assign null to uniRightEgr.
            printTestDescription(
                                 "Test basic attribute loaded state after fetch from DB and assign null to uniRightEgr. [d631756]");
            Collection<Util1xmRt> saveRightEgr = e1.getUniRightEgr();
            e1.setUniRightEgr(null);

            if (JPAPersistenceProvider.OPENJPA.equals(jpaProvider)) {
                Collection<Util1xmRt> eRs = e1.uniRight;
                Assert.assertNull("Util1xmRt uniRight == null. Observed " + eRs, eRs);
            } else {
//                results.assertPass("SKIPPED for non-OpenJPA because referencing uniRight field causes a lazy load and invalidates the test");
            }

            eEs = e1.uniRightEgr;
            Assert.assertNull("Util1xmRt uniRightEgr == null. Observed " + eEs, eEs);

            eR1 = null;
            eE1 = null;

            assertAttributeLoadState(
                                     // ProviderUtil.isLoaded() tests
                                     computeExpectedLoadedState(
                                                                e1, null, LoadState.LOADED,
                                                                e1, "firstName", LoadState.LOADED,
                                                                e1, "uniRight", LoadState.NOT_LOADED,
                                                                e1, "uniRightEgr", LoadState.LOADED,
                                                                eR1, "lastName", LoadState.UNKNOWN,
                                                                eE1, "lastName", LoadState.UNKNOWN,
                                                                e1, "unknown", LoadState.UNKNOWN),
                                     // PersistenceUtil.isLoaded() tests
                                     computeExpectedIsLoaded(
                                                             e1, null, true,
                                                             e1, "firstName", true,
                                                             e1, "uniRight", false,
                                                             e1, "uniRightEgr", true,
                                                             eR1, "lastName", true, // uniRight = null -> eR1 = null -> UNKNOWN - true
                                                             eE1, "lastName", true, // uniRighEgr = null -> eE1 = null -> UNKNOWN - true
                                                             e1, "unknown", true),
                                     // PersistenceUnitUtil.isLoaded() tests
                                     computeExpectedIsLoaded(
                                                             e1, null, true,
                                                             e1, "firstName", true,
                                                             e1, "uniRight", false,
                                                             e1, "uniRightEgr", true,
                                                             eR1, "lastName", false, // uniRight = null -> eR1 = null -> UNKNOWN - true
                                                             eE1, "lastName", false, // uniRighEgr = null -> eE1 = null -> UNKNOWN - true
                                                             e1, "unknown", false),
                                     e1, TestUtil1xm_TestRow_Id, Util1xmLf.class, jpaProvider);

            // Test basic attribute loaded state after fetch from DB and assign new value to all fields.
            printTestDescription(
                                 "Test basic attribute loaded state after fetch from DB and assign new value to all fields.");
            e1.setUniRightEgr(saveRightEgr);
            e1.getUniRight().iterator().next().getLastName(); // force to fetch the lazy field

            Collection<Util1xmRt> eRs = e1.getUniRight();
            Assert.assertNotNull("Util1xmRt uniRight != null", eRs);
            if (eRs != null) {
                Assert.assertEquals("Util1xmRt uniRight.size == 2", 2, eRs.size());
            }
            itr = eRs.iterator();
            eR1 = itr.next();
            eR2 = itr.next();

            eEs = e1.getUniRightEgr();
            Assert.assertNotNull("Util1xmRt uniRightEgr != null", eEs);
            if (eEs != null) {
                Assert.assertEquals("Util1xmRt uniRightEgr.size == 2", 2, eEs.size());
            }
            itr = eEs.iterator();
            eE1 = itr.next();
            eE2 = itr.next();

            assertAttributeLoadState(
                                     // ProviderUtil.isLoaded() tests
                                     computeExpectedLoadedState(
                                                                e1, null, LoadState.LOADED,
                                                                e1, "firstName", LoadState.LOADED,
                                                                e1, "uniRight", LoadState.LOADED,
                                                                e1, "uniRightEgr", LoadState.LOADED,
                                                                eR1, "lastName", LoadState.LOADED,
                                                                eR2, "lastName", LoadState.LOADED,
                                                                eE1, "lastName", LoadState.LOADED,
                                                                eE2, "lastName", LoadState.LOADED,
                                                                e1, "unknown", LoadState.UNKNOWN),
                                     // PersistenceUtil.isLoaded() tests
                                     computeExpectedIsLoaded(
                                                             e1, null, true,
                                                             e1, "firstName", true,
                                                             e1, "uniRight", true,
                                                             e1, "uniRightEgr", true,
                                                             eR1, "lastName", true,
                                                             eR2, "lastName", true,
                                                             eE1, "lastName", true,
                                                             eE2, "lastName", true,
                                                             e1, "unknown", true),
                                     // PersistenceUnitUtil.isLoaded() tests
                                     computeExpectedIsLoaded(
                                                             e1, null, true,
                                                             e1, "firstName", true,
                                                             e1, "uniRight", true,
                                                             e1, "uniRightEgr", true,
                                                             eR1, "lastName", true,
                                                             eR2, "lastName", true,
                                                             eE1, "lastName", true,
                                                             eE2, "lastName", true,
                                                             e1, "unknown", false),
                                     e1, TestUtil1xm_TestRow_Id, Util1xmLf.class, jpaProvider);

        } finally {
            System.out.println(TestUtil1xm + ": End");
            System.out.println("Rollback current Tx");
            jpaRW.getTj().rollbackTransaction();
            clearUtils();
        }
    }

    private boolean initUtils(JPAResource jpaRW, TestExecutionContext testExecCtx) {
        // Verify that testData was provided by the test client. Fail if it is not available.
        if (jpaRW == null) {
            // No testData object was passed to the test impl.
            Assert.fail(TestUtilBasic + ": The JPAResourceWrapper is missing (null). Cannot execute the test.");
            return false;
        }
        if (testExecCtx == null) {
            // No testData object was passed to the test impl.
            Assert.fail(TestUtilBasic + ": The testData is missing (null). Cannot execute the test.");
            return false;
        }

        emf = jpaRW.getEmf();
        Assert.assertNotNull("Verify factory exists.", emf);

        em = jpaRW.getEm();
        Assert.assertNotNull("Verify em exists.", em);

        resolver = PersistenceProviderResolverHolder.getPersistenceProviderResolver();
        Assert.assertNotNull("Verify PersistenceProviderResolver exists.", resolver);

        providers = resolver.getPersistenceProviders();
        System.out.println("Found " + providers.size() + " persistence provider(s)");
        System.out.println("      " + providers.toString());
        Assert.assertTrue("Did not find 1 or more persistence provider(s)", providers.size() > 0);
        System.out.println("Test ProviderUtil using persistence provider '" + providers.get(0).getClass().getName() + "'");

        pvdrUtil = providers.get(0).getProviderUtil();
        Assert.assertNotNull("Verify PersistenceUtil exists.", pvdrUtil);

        pUtil = Persistence.getPersistenceUtil();

        puUtil = emf.getPersistenceUnitUtil();
        Assert.assertNotNull("Verify PersistenceUnitUtil exists.", puUtil);

        return true;
    }

    private void clearUtils() {
        emf = null;
        em = null;
        resolver = null;
        providers = null;
        pvdrUtil = null;
        pUtil = null;
        puUtil = null;
    }

    private final void setupTestData(JPAResource jpaRW) {
        System.out.println("Begin a Tx");
        jpaRW.getTj().beginTransaction();
        if (jpaRW.getTj().isApplicationManaged()) {
            System.out.println("Joining entitymanager to JTA transaction...");
            jpaRW.getEm().joinTransaction();
        }

        // Initialize UtilEntity entity
        UtilEntity e = new UtilEntity();
        e.setId(TestUtilBasic_TestRow_Id);
        e.setName("loaded name");
        jpaRW.getEm().persist(e);

        // Initialize UtilEmbEntity entity
        UtilEmbEntity emb = new UtilEmbEntity();
        emb.setId(TestUtilEmbeddable_TestRow_Id);
        emb.setName("loaded embEntity name");
        UtilEmbeddable embeddable = new UtilEmbeddable();
        embeddable.setEmbName("embeddable name");
        embeddable.setEmbNotLoaded("embeddable notLoaded");
        emb.setEmb(embeddable);

        UtilEmbeddable embeddable1 = new UtilEmbeddable();
        embeddable1.setEmbName("embeddable name1");
        embeddable1.setEmbNotLoaded("embeddable notLoaded1");
        emb.setEmb1(embeddable1);

        emb.setInitNullEmb(null);
        em.persist(emb);

        // Initialize Util1x1Lf entity
        Util1x1Lf lf1x1 = new Util1x1Lf();
        lf1x1.setId(TestUtil1x1_TestRow_Id);
        lf1x1.setFirstName("loaded firstName");
        Util1x1Rt rt1x1 = new Util1x1Rt();
        rt1x1.setId(TestUtil1x1_TestRow_Id + 11);
        rt1x1.setLastName("loaded lastName");
        lf1x1.setUniRight(rt1x1);
        Util1x1Rt rt1x1Lzy = new Util1x1Rt();
        rt1x1Lzy.setId(TestUtil1x1_TestRow_Id + 21);
        rt1x1Lzy.setLastName("loaded lazy lastName");
        lf1x1.setUniRightLzy(rt1x1Lzy);

        em.persist(rt1x1);
        em.persist(rt1x1Lzy);
        em.persist(lf1x1);

        // Initialize Util1xmLf entity
        Util1xmLf lf1xm = new Util1xmLf();
        lf1xm.setId(TestUtil1xm_TestRow_Id);
        lf1xm.setFirstName("loaded firstName");

        Util1xmRt rt1xm1 = new Util1xmRt();
        rt1xm1.setId(TestUtil1xm_TestRow_Id + 11);
        rt1xm1.setLastName("loaded lastName1");
        lf1xm.addUniRight(rt1xm1);
        Util1xmRt rt1xm2 = new Util1xmRt();
        rt1xm2.setId(TestUtil1xm_TestRow_Id + 12);
        rt1xm2.setLastName("loaded lastName2");
        lf1xm.addUniRight(rt1xm2);

        Util1xmRt rt1xm3 = new Util1xmRt();
        rt1xm3.setId(TestUtil1xm_TestRow_Id + 21);
        rt1xm3.setLastName("loaded eager lastName3");
        lf1xm.addUniRightEgr(rt1xm3);
        Util1xmRt rt1xm4 = new Util1xmRt();
        rt1xm4.setId(TestUtil1xm_TestRow_Id + 22);
        rt1xm4.setLastName("loaded eager lastName4");
        lf1xm.addUniRightEgr(rt1xm4);

        em.persist(rt1xm1);
        em.persist(rt1xm2);
        em.persist(rt1xm3);
        em.persist(rt1xm4);
        em.persist(lf1xm);

        System.out.println("Commit current Tx");
        jpaRW.getTj().commitTransaction();
    }

    /**
     *
     */
    private void assertBasicNewInstanceLoadState(String desc, Object e1,
                                                 Class<?> expectedIdClass, JPAPersistenceProvider jpaProvider) {
        // EclipseLink considers new entities constructed by the application,
        // which have never been persisted, to be loaded.
        LoadState expectedLoadState = JPAPersistenceProvider.ECLIPSELINK.equals(jpaProvider) ? LoadState.LOADED : LoadState.UNKNOWN;

        assertBasicInvalidEntityLoadState(desc, e1, expectedIdClass, expectedLoadState, jpaProvider);
    }

    private void assertBasicInvalidEntityLoadState(String desc, Object e1,
                                                   Class<?> expectedIdClass, LoadState expectedLoadState, JPAPersistenceProvider jpaProvider) {
        assertInvalidEntityLoadState(desc,
                                     // ProviderUtil.isLoaded() tests
                                     computeExpectedLoadedState(
                                                                e1, null, expectedLoadState,
                                                                e1, "name", expectedLoadState,
                                                                e1, "notLoaded", expectedLoadState,
                                                                e1, "unknown", LoadState.UNKNOWN),
                                     // PersistenceUtil.isLoaded() tests
                                     computeExpectedIsLoaded(
                                                             e1, null, true,
                                                             e1, "name", true,
                                                             e1, "notLoaded", true,
                                                             e1, "unknown", true),
                                     // PersistenceUnitUtil.isLoaded() tests
                                     computeExpectedIsLoaded(
                                                             e1, null, expectedLoadState == LoadState.LOADED,
                                                             e1, "name", expectedLoadState == LoadState.LOADED,
                                                             e1, "notLoaded", expectedLoadState == LoadState.LOADED,
                                                             e1, "unknown", false),
                                     e1, expectedIdClass, jpaProvider);
    }

    /**
     *
     */
    private void assertEmbeddableNewInstanceLoadState(String desc, Object e1,
                                                      Class<?> expectedIdClass, JPAPersistenceProvider jpaProvider) {
        // EclipseLink considers new entities constructed by the application,
        // which have never been persisted, to be loaded.
        LoadState expectedLoadState = JPAPersistenceProvider.ECLIPSELINK.equals(jpaProvider) ? LoadState.LOADED : LoadState.UNKNOWN;
        assertEmbeddableInvalidEntityLoadState(desc, e1, expectedIdClass, expectedLoadState, jpaProvider);
    }

    private void assertEmbeddableInvalidEntityLoadState(String desc, Object e1,
                                                        Class<?> expectedIdClass, LoadState expectedLoadState, JPAPersistenceProvider jpaProvider) {
        assertInvalidEntityLoadState(desc,
                                     // ProviderUtil.isLoaded() tests
                                     computeExpectedLoadedState(
                                                                e1, null, expectedLoadState,
                                                                e1, "name", expectedLoadState,
                                                                e1, "emb", expectedLoadState,
//            e1, "emb.embName",          expectedLoadState,
//            e1, "emb.embNotLoaded",     expectedLoadState,
                                                                e1, "emb1", expectedLoadState,
                                                                e1, "initNullEmb", expectedLoadState,
                                                                e1, "unknown", LoadState.UNKNOWN),
                                     // PersistenceUtil.isLoaded() tests
                                     computeExpectedIsLoaded(
                                                             e1, null, true,
                                                             e1, "name", true,
                                                             e1, "emb", true,
//            e1,  "emb.embName",          true,
//            e1,  "emb.embNotLoaded",     true,
                                                             e1, "emb1", true,
                                                             e1, "initNullEmb", true,
                                                             e1, "unknown", true),
                                     // PersistenceUnitUtil.isLoaded() tests
                                     computeExpectedIsLoaded(
                                                             e1, null, expectedLoadState == LoadState.LOADED,
                                                             e1, "name", expectedLoadState == LoadState.LOADED,
                                                             e1, "emb", expectedLoadState == LoadState.LOADED,
//            e1,  "emb.embName",          expectedLoadState == LoadState.LOADED,
//            e1,  "emb.embNotLoaded",     expectedLoadState == LoadState.LOADED,
                                                             e1, "emb1", expectedLoadState == LoadState.LOADED,
                                                             e1, "initNullEmb", expectedLoadState == LoadState.LOADED,
                                                             e1, "unknown", false),
                                     e1, expectedIdClass, jpaProvider);
    }

    /**
     *
     */
    private void assert1x1NewInstanceLoadState(String desc, Object e1, Object e2,
                                               Class<?> expectedIdClass, JPAPersistenceProvider jpaProvider) {
        // EclipseLink considers new entities constructed by the application,
        // which have never been persisted, to be loaded.
        LoadState expectedLoadState = JPAPersistenceProvider.ECLIPSELINK.equals(jpaProvider) ? LoadState.LOADED : LoadState.UNKNOWN;
        assert1x1InvalidEntityLoadState(desc, e1, e2, expectedIdClass, expectedLoadState, jpaProvider);
    }

    private void assert1x1InvalidEntityLoadState(String desc, Object e1, Object e2,
                                                 Class<?> expectedIdClass, LoadState expectedLoadState, JPAPersistenceProvider jpaProvider) {
        assertInvalidEntityLoadState(desc,
                                     // ProviderUtil.isLoaded() tests
                                     computeExpectedLoadedState(
                                                                e1, null, expectedLoadState,
                                                                e1, "firstName", expectedLoadState,
                                                                e1, "uniRight", expectedLoadState,
                                                                e1, "uniRightLzy", expectedLoadState,
                                                                e1, "unknown", LoadState.UNKNOWN),
                                     // PersistenceUtil.isLoaded() tests
                                     computeExpectedIsLoaded(
                                                             e1, null, true,
                                                             e1, "firstName", true,
                                                             e1, "uniRight", true,
                                                             e1, "uniRightLzy", true,
                                                             e1, "unknown", true),
                                     // PersistenceUnitUtil.isLoaded() tests
                                     computeExpectedIsLoaded(
                                                             e1, null, expectedLoadState == LoadState.LOADED,
                                                             e1, "firstName", expectedLoadState == LoadState.LOADED,
                                                             e1, "uniRight", expectedLoadState == LoadState.LOADED,
                                                             e1, "uniRightLzy", expectedLoadState == LoadState.LOADED,
                                                             e1, "unknown", false),
                                     e1, expectedIdClass, jpaProvider);
    }

    /**
     *
     */
    private void assert1xmNewInstanceLoadState(String desc, Object e1, Object e2,
                                               Class<?> expectedIdClass, JPAPersistenceProvider jpaProvider) {
        // EclipseLink considers new entities constructed by the application,
        // which have never been persisted, to be loaded.
        LoadState expectedLoadState = JPAPersistenceProvider.ECLIPSELINK.equals(jpaProvider) ? LoadState.LOADED : LoadState.UNKNOWN;
        assert1xmInvalidEntityLoadState(desc, e1, e2, expectedIdClass, expectedLoadState, jpaProvider);
    }

    private void assert1xmInvalidEntityLoadState(String desc, Object e1, Object e2,
                                                 Class<?> expectedIdClass, LoadState expectedLoadState, JPAPersistenceProvider jpaProvider) {
        assertInvalidEntityLoadState(desc,
                                     // ProviderUtil.isLoaded() tests
                                     computeExpectedLoadedState(
                                                                e1, null, expectedLoadState,
                                                                e1, "firstName", expectedLoadState,
                                                                e1, "uniRight", expectedLoadState,
                                                                e1, "uniRightEgr", expectedLoadState,
                                                                e1, "unknown", LoadState.UNKNOWN),
                                     // PersistenceUtil.isLoaded() tests
                                     computeExpectedIsLoaded(
                                                             e1, null, true,
                                                             e1, "firstName", true,
                                                             e1, "uniRight", true,
                                                             e1, "uniRightEgr", true,
                                                             e1, "unknown", true),
                                     // PersistenceUnitUtil.isLoaded() tests
                                     computeExpectedIsLoaded(
                                                             e1, null, expectedLoadState == LoadState.LOADED,
                                                             e1, "firstName", expectedLoadState == LoadState.LOADED,
                                                             e1, "uniRight", expectedLoadState == LoadState.LOADED,
                                                             e1, "uniRightEgr", expectedLoadState == LoadState.LOADED,
                                                             e1, "unknown", false),
                                     e1, expectedIdClass, jpaProvider);
    }

    /**
     *
     * @param results
     * @param desc
     * @param e1
     * @param pvdrExpected
     * @param puExpected
     * @param puuExpected
     * @param expectedIdClass
     */
    private void assertInvalidEntityLoadState(String desc,
                                              ExpectedLoadState[] pvdrExpected,
                                              ExpectedIsLoaded[] puExpected,
                                              ExpectedIsLoaded[] puuExpected,
                                              Object eId, Class<?> expectedIdClass, JPAPersistenceProvider jpaProvider) {
        printTestDescription(desc);
        assertAttributeLoadState(
                                 pvdrExpected,
                                 puExpected,
                                 puuExpected,
                                 eId, null, expectedIdClass, jpaProvider);
    }

    class ExpectedLoadState {
        Object e;
        String name;
        LoadState state;

        ExpectedLoadState(Object e, String name, LoadState state) {
            this.e = e;
            this.name = name;
            this.state = state;
        }

        @Override
        public String toString() {
            return "ExpectedLoadState={e=" + e + ",name=" + name + ",LoadeState" + state + "}";
        }
    }

    class ExpectedIsLoaded {
        Object e;
        String name;
        boolean isLoaded;

        ExpectedIsLoaded(Object e, String name, boolean isLoaded) {
            this.e = e;
            this.name = name;
            this.isLoaded = isLoaded;
        }

        @Override
        public String toString() {
            return "ExpectedIsLoaded={e=" + e + ",name=" + name + ",isLoaded=" + isLoaded + "}";
        }
    }

    /**
     *
     */
    private void assertAttributeLoadState(
                                          ExpectedLoadState[] pvdrExpected,
                                          ExpectedIsLoaded[] puExpected,
                                          ExpectedIsLoaded[] puuExpected,
                                          Object eId, Object expectedId, Class<?> expectedIdClass, JPAPersistenceProvider jpaProvider) {

        System.out.println("  >>>----- ProviderUtil Tests -----");
        for (ExpectedLoadState loadState : pvdrExpected) {
            Object e1 = loadState.e;
            System.out.println("  * Entity(e)=" + e1);
            if (loadState.name == null || loadState.name.trim().length() == 0) {
                LoadState state = pvdrUtil.isLoaded(e1);
                Assert.assertEquals("  -> Test [" + loadState.state + "]=ProviderUtil.isLoaded(e) returned " + state, loadState.state,
                                    state);
            } else {
                LoadState state = pvdrUtil.isLoadedWithoutReference(e1, loadState.name);
                Assert.assertEquals("  -> Test [" + loadState.state + "]=ProviderUtil.isLoadedWithoutReference(e,\""
                                    + loadState.name + "\") returned " + state, loadState.state, state);
            }
        }

        System.out.println("  >>> ----- PersistenceUtil Tests -----");
        for (ExpectedIsLoaded loadState : puExpected) {
            Object e1 = loadState.e;
            System.out.println("  * Entity(e)=" + e1);
            if (loadState.name == null || loadState.name.trim().length() == 0) {
                Assert.assertEquals("  -> Test [" + loadState.isLoaded + "]=PersistenceUtil.isLoaded(e)",
                                    loadState.isLoaded, pUtil.isLoaded(e1));
            } else {
                Assert.assertEquals("  -> Test [" + loadState.isLoaded + "]=PersistenceUtil.isLoaded(e,\""
                                    + loadState.name + "\")", loadState.isLoaded, pUtil.isLoaded(e1, loadState.name));
            }
        }

        System.out.println("  >>> ----- PersistenceUnitUtil Tests -----");
        for (ExpectedIsLoaded loadState : puuExpected) {
            Object e1 = loadState.e;
            System.out.println("  * Entity(e)=" + e1);
            if (loadState.name == null || loadState.name.trim().length() == 0) {
                Assert.assertEquals("  -> Test [" + loadState.isLoaded + "]=PersistenceUnitUtil.isLoaded(e): " + e1,
                                    loadState.isLoaded, puUtil.isLoaded(e1));
            } else {
                Assert.assertEquals("  -> Test [" + loadState.isLoaded + "]=PersistenceUnitUtil.isLoaded(e,\""
                                    + loadState.name + "\"): " + e1, loadState.isLoaded, puUtil.isLoaded(e1, loadState.name));
            }
        }

        Object id = null;
        IllegalArgumentException expectedFailure;
        try {
            id = puUtil.getIdentifier(eId);
            expectedFailure = null;
        } catch (IllegalArgumentException x) {
            if (eId == null || NON_ENTITY_CLASSES.contains(eId.getClass())) // Per JavaDoc: Throws: IllegalArgumentException - if the object is found not to be an entity
                expectedFailure = x;
            else
                throw x;
        }
        if (expectedFailure != null) {
//            results.assertPass("  -> Test [null] PersistenceUnitUtil.getIdentifer(null) raises " + expectedFailure);
        } else if (expectedId == null && JPAPersistenceProvider.ECLIPSELINK.equals(jpaProvider)) {
            try {
                Object entityId = eId.getClass().getMethod("getId").invoke(eId);
                Assert.assertEquals("  -> Test [null]=PersistenceUnitUtil.getIdentifier(" + eId + ") returned " + id + " instead of " + entityId, id, entityId);
            } catch (Exception x) {
                Assert.fail("Unable to invoke getId on entity " + eId + ": " + (x instanceof InvocationTargetException ? x.getCause() : x));
            }
        } else if (expectedId == null) {
            Assert.assertNull("  -> Test [null]=PersistenceUnitUtil.getIdentifier(" + eId + ") returned " + id, id);
        } else {
            Assert.assertNotNull("  -> Test [!null]=PersistenceUnitUtil.getIdentifier()", id);
            {
//             results.assertPass("  -> Test PersistenceUnitUtil.getIdentifier() OpenJPAId test 1 skipped for " + jpaProvider);
//             results.assertPass("  -> Test PersistenceUnitUtil.getIdentifier() OpenJPAId test 2 skipped for " + jpaProvider);
//             results.assertPass("  -> Test PersistenceUnitUtil.getIdentifier() OpenJPAId test 3 skipped for " + jpaProvider);
            }
        }
    }

    private ExpectedLoadState[] computeExpectedLoadedState(Object... results) {
        int size = (results.length + 2) / 3;
        ExpectedLoadState rtn[] = new ExpectedLoadState[size];
        for (int i = 0; i < size; i++) {
            rtn[i] = new ExpectedLoadState(results[i * 3], (String) results[i * 3 + 1], (LoadState) results[i * 3 + 2]);
        }
        return rtn;
    }

    private ExpectedIsLoaded[] computeExpectedIsLoaded(Object... results) {
        int size = (results.length + 2) / 3;
        ExpectedIsLoaded rtn[] = new ExpectedIsLoaded[size];
        for (int i = 0; i < size; i++) {
            rtn[i] = new ExpectedIsLoaded(results[i * 3], (String) results[i * 3 + 1], (Boolean) results[i * 3 + 2]);
        }
        return rtn;
    }

    private void printTestDescription(String desc) {
        System.out.println("========== " + desc + " ==========");
    }
}
