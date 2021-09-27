/*******************************************************************************
 * Copyright (c) 2018, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.fvt.relationships.manyXmany.testlogic.tests.ejb;

import java.util.HashMap;

import javax.annotation.PostConstruct;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.vehicle.web.EJBTestVehicleServlet;

import componenttest.annotation.ExpectedFFDC;

@WebServlet(urlPatterns = "/TestManyXManyBidirectional_EJB_SF_Servlet")
public class TestManyXManyBidirectional_EJB_SF_Servlet extends EJBTestVehicleServlet {
    private static final long serialVersionUID = -6684407686587457314L;

    private final String testLogicClassName = "com.ibm.ws.jpa.fvt.relationships.manyXmany.testlogic.ManyXManyBidirectionalTestLogic";

    private final HashMap<String, JPAPersistenceContext> jpaPctxMap = new HashMap<String, JPAPersistenceContext>();

    private final static String ejbJNDIName = "ejb/ManyXManySFEJB";

    @PostConstruct
    private void initFAT() {
        jpaPctxMap.put("test-jpa-resource-amjta",
                       new JPAPersistenceContext("test-jpa-resource-amjta", PersistenceContextType.APPLICATION_MANAGED_JTA, PersistenceInjectionType.JNDI, "java:comp/env/jpa/ManyXMany_Bi_AMJTA"));
        jpaPctxMap.put("test-jpa-resource-amrl",
                       new JPAPersistenceContext("test-jpa-resource-amrl", PersistenceContextType.APPLICATION_MANAGED_RL, PersistenceInjectionType.JNDI, "java:comp/env/jpa/ManyXMany_Bi_AMRL"));
        jpaPctxMap.put("test-jpa-resource-cmts",
                       new JPAPersistenceContext("test-jpa-resource-cmts", PersistenceContextType.CONTAINER_MANAGED_TS, PersistenceInjectionType.JNDI, "java:comp/env/jpa/ManyXMany_Bi_CMTS"));
    }

/*
 * Verify basic Many-to-Many service by creating BiEntityA and BiEntityB, and
 * add BiEntityB to the collection BiEntityA.defaultRelationship.
 *
 * Test verifies function performing the following:
 * 1) Create new BiEntityB(id=1)
 * 2) Create new BiEntityA(id=1), add BiEntityB(id=1) to defaultRelationship.
 * 3) Keep other (optional) ManyXMany collections empty.
 * 4) Clear persistence context (if necessary), and find BiEntityA(id=1).
 * 5) Verify field data in BiEntityA(id=1)
 * 6) BiEntityA.defaultRelationship should contain BiEntityB(id=1). Extract it
 * and verify its field data.
 * 7) Test passes if all these conditions are met.
 *
 * This test case confirms the following function verification:
 * 1) Optional relational fields can be empty without persistence errors
 * (all fields except defaultRelationship are empty in BiEntityA(id=1))
 * 2) A traversable ManyXMany association between BiEntityA to BiEntityB(id=1) is
 * established, and BiEntityB is accessible through the relational
 * reference defaultRelationship in BiEntityA.
 */

    @Test
    public void jpa10_Relationships_ManyXMany_Bidirectional_001_Ano_AMJTA_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_Bidirectional_001_Ano_AMJTA_EJB_SF";
        final String testMethod = "testManyXManyUni001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "MMBiEntA");
        properties.put("EntityBName", "MMBiEntB_DR");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_ManyXMany_Bidirectional_001_XML_AMJTA_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_Bidirectional_001_XML_AMJTA_EJB_SF";
        final String testMethod = "testManyXManyUni001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLMMBiEntA");
        properties.put("EntityBName", "XMLMMBiEntB_DR");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_ManyXMany_Bidirectional_001_Ano_AMRL_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_Bidirectional_001_Ano_AMRL_EJB_SF";
        final String testMethod = "testManyXManyUni001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "MMBiEntA");
        properties.put("EntityBName", "MMBiEntB_DR");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_ManyXMany_Bidirectional_001_XML_AMRL_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_Bidirectional_001_XML_AMRL_EJB_SF";
        final String testMethod = "testManyXManyUni001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLMMBiEntA");
        properties.put("EntityBName", "XMLMMBiEntB_DR");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_ManyXMany_Bidirectional_001_Ano_CMTS_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_Bidirectional_001_Ano_CMTS_EJB_SF";
        final String testMethod = "testManyXManyUni001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "MMBiEntA");
        properties.put("EntityBName", "MMBiEntB_DR");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_ManyXMany_Bidirectional_001_XML_CMTS_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_Bidirectional_001_XML_CMTS_EJB_SF";
        final String testMethod = "testManyXManyUni001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLMMBiEntA");
        properties.put("EntityBName", "XMLMMBiEntB_DR");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

/*
 * Test Cascade, Type: Default (no cascade options specified) Verification
 *
 * Verify cascade: none behavior (default). By this:
 *
 * Persist:
 *
 * Both entities in the relationship need to have persist() invoked on them
 * in order to be stored on the database. By default, persisting the owning side
 * of the relationship does not automatically persist the entity on the inverse
 * side, and vice versa.
 *
 * 1) Create an unpersisted BiEntityB(id=1).
 * 2) Create an unpersisted BiEntityA(id=1), add BiEntityB(id=1) to defaultRelationship.
 * 3) In a transaction, persist only BiEntityA(id=1) to the database.
 * Note that no persist command is invoked on BiEntityB(id=1), and since, by default,
 * persist operations are not cascaded across entity relationships, the persist operation
 * on BiEntityA(id=1) should not cause BiEntityB(id=1) to become persisted/managed.
 * 4) Clear persistence context, forcing all entities to become detached.
 * 5) Find BiEntityA(id=1), this should hit the database, returning an entity
 * with no entries in defaultRelationship because BiEntityB(id=1) was never persisted.
 *
 * Remove:
 *
 * By default, removing the entity on the owning side of the ManyXMany relationship does
 * not cause the entity on the inverse side of the relationship to become removed as well.
 *
 * 1) Create Create BiEntityB(id=1)
 * 2) Create new BiEntityA(id=1), adding BiEntityB(id=1) to defaultRelationship.
 * 3) Remove BiEntityA(id=1), verify that BiEntityB(id=1) still exists. Test point passes if it does.
 *
 * If the entity on the inverse side of the relationship is removed, it should not remove the entity
 * on the owning of the relationship. Also, since the ManyXMany relationship is optional, the relationship
 * field collection should be empty on fresh instances of the owning entity from find().
 *
 * 1) Create Create BiEntityB(id=1)
 * 2) Create new BiEntityA(id=1), add BiEntityB(id=1) to defaultRelationship.
 * 3) Remove BiEntityB(id=1)
 * 4) Clear the persistence context
 * 5) Find BiEntityA(id=1). It should still exist (especially given that it is a unidirectional
 * relationship), and defaultRelationship should be empty.
 *
 * Merge:
 *
 * Merge will attempt to update the managed entity to point to managed versions of entities referenced by
 * the detached entity.
 *
 * 1) Create BiEntityB(id=1)
 * 2) Create new BiEntityA(id=1), add BiEntityB(id=1) to defaultRelationship.
 * 3) Clear the persistence context, causing all entities to become detached
 * 4) Perform the following operations:
 * Modify the name field on BiEntityB(id=1)
 * Modify the name field on BiEntityA(id=1)
 * 5) Merge BiEntityA(id=1) into the persistence context.
 * 6) Verify the change to BiEntityA(id=1)'s name field. Verify that BiEntityA(id=1)'s defaultRelationship field references
 * a managed copy of BiEntityB(id=1) and that BiEntityB(id=1)'s name field contains the original value.
 * BiEntityB(id=1)'s name field should not have been changed because EntityManager.merge() was never called
 * on it directly, and merge by default does not cascade across entity relationships.
 *
 * If an entity being merged points to a removed entity, an IllegalArgumentException should be thrown.
 *
 * 1) Create Create BiEntityB(id=1)
 * 2) Create new BiEntityA(id=1), add BiEntityB(id=1) to defaultRelationship.
 * 3) Clear the persistence context
 * 4) In a single transaction, find BiEntityB(id=1) and remove it. This deletes BiEntityB(id=1) from the database.
 * Note: the original detached BiEntityB(id=1) reference still exists, and is referenced by the detached copy of
 * BiEntityA(id=1).
 * 5) Modify the name field of BiEntityA(id=1) and attempt to merge it. An IllegalArgumentException because the
 * detached entity still has a references in its defaultRelationship field to BiEntityB(id=1), which no longer
 * exists in the database.
 *
 * Refresh:
 *
 * Refresh operations are, by default, not cascaded across entity relationships. Without the REFRESH cascade
 * option, a refresh operation will stop at the source entity.
 *
 * 1) Create Create BiEntityB(id=1)
 * 2) Create new BiEntityA(id=1), add BiEntityB(id=1) to defaultRelationship.
 * 3) Start a new transaction
 * 4) With managed copies of the two entities, edit BiEntityA(id=1) and BiEntityA(id=1).defaultRelationship's name fields.
 * 5) Invoke EntityManager.refresh() on BiEntityA(id=1)
 * 6) Verify that BiEntityA(id=1)'s name field has been reverted to the value it had when it was created.
 * 7) Verify that BiEntityB(id=1) still has the new value
 */

    @Test
    @ExpectedFFDC("javax.transaction.RollbackException")
    public void jpa10_Relationships_ManyXMany_Bidirectional_002_Ano_AMJTA_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_Bidirectional_002_Ano_AMJTA_EJB_SF";
        final String testMethod = "testManyXManyUni002";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "MMBiEntA");
        properties.put("EntityBName", "MMBiEntB_DR");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    @ExpectedFFDC("javax.transaction.RollbackException")
    public void jpa10_Relationships_ManyXMany_Bidirectional_002_XML_AMJTA_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_Bidirectional_002_XML_AMJTA_EJB_SF";
        final String testMethod = "testManyXManyUni002";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLMMBiEntA");
        properties.put("EntityBName", "XMLMMBiEntB_DR");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_ManyXMany_Bidirectional_002_Ano_AMRL_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_Bidirectional_002_Ano_AMRL_EJB_SF";
        final String testMethod = "testManyXManyUni002";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "MMBiEntA");
        properties.put("EntityBName", "MMBiEntB_DR");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_ManyXMany_Bidirectional_002_XML_AMRL_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_Bidirectional_002_XML_AMRL_EJB_SF";
        final String testMethod = "testManyXManyUni002";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLMMBiEntA");
        properties.put("EntityBName", "XMLMMBiEntB_DR");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    @ExpectedFFDC("javax.transaction.RollbackException")
    public void jpa10_Relationships_ManyXMany_Bidirectional_002_Ano_CMTS_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_Bidirectional_002_Ano_CMTS_EJB_SF";
        final String testMethod = "testManyXManyUni002";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "MMBiEntA");
        properties.put("EntityBName", "MMBiEntB_DR");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    @ExpectedFFDC("javax.transaction.RollbackException")
    public void jpa10_Relationships_ManyXMany_Bidirectional_002_XML_CMTS_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_Bidirectional_002_XML_CMTS_EJB_SF";
        final String testMethod = "testManyXManyUni002";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLMMBiEntA");
        properties.put("EntityBName", "XMLMMBiEntB_DR");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

/*
 * Test Cascade, Type: ALL (all cascade options specified) Verification
 *
 * Verify cascade: all. By this:
 *
 * Persist:
 *
 * With cascade type set to ALL, persist operations are cascaded across the
 * entity relationship. This means if BiEntityA is the target if an
 * EntityManager.persist() operation, the JPA implementation will automatically
 * invoke EntityManager.persist() on all of the BiEntityB entity relationships
 * that are marked with Cascade type ALL (or PERSIST).
 *
 * 1) Create an unpersisted BiEntityB(id=1)
 * 2) Create an unpersisted BiEntityA(id=1)
 * 3) Add BiEntityB(id=1) to cascadeAll.
 * 4) Start a new transaction, persist BiEntityA(id=1), and commit the transaction.
 * 5) Clear persistence context, forcing all entities to become detached.
 * 6) Find BiEntityA(id=1), this should hit the database, returning an entity
 * with a reference in cascadeAll to BiEntityB(id=1) because the persist operation
 * was cascaded.
 *
 * Remove:
 *
 * With cascade type set to ALL, remove operations are cascaded across the entity
 * relationship. This means if BiEntityA is the target of an EntityManager.remove()
 * operation, the JPA implementation will automatically invoke EntityManager.remove()
 * on all of the BiEntityB entity relationships that are marked with Cascade type ALL
 * (or REMOVE).
 *
 * 1) Create Create BiEntityB(id=1)
 * 2) Create new BiEntityA(id=1), add BiEntityB(id=1) to cascadeAll.
 * 3) Remove BiEntityA(id=1), verify that BiEntityB(id=1) also no longer exists. Test point passes if true.
 *
 * Merge:
 *
 * With cascade type set to ALL, merge operations will cascade across the entity relationship. So changes to
 * the target entity (BiEntityA(id=1)) and changes to all entities with relationship cascade attributes of
 * ALL (and MERGE) will be merged into the persistence context.
 *
 * 1) Create Create BiEntityB(id=1)
 * 2) Create new BiEntityA(id=1), add BiEntityB(id=1) to cascadeAll.
 * 3) Clear the persistence context, causing all entities to become detached
 * 4) Modify BiEntityA(id=1)'s name field. Modify BiEntityB(id=1)'s name field.
 * 5) Merge BiEntityA(id=1) into the persistence context. Examine the BiEntityB(id=1) referenced by the cascadeAll
 * field of the entity returned from the merge() operation. Both entities should be managed, and both
 * entities should contain the changes from step 4.
 *
 * Refresh:
 *
 * With cascade type set to ALL, refresh operations will cascade across entity relationships. So changes
 * to the target entity (BiEntityA(id=1)) and changes to all entities with relationship cascade attributes of
 * ALL (and REFRESH) will have their contents reset to match the data in the database.
 *
 * 1) Create Create BiEntityB(id=1)
 * 2) Create new BiEntityA(id=1), add BiEntityB(id=1) to cascadeAll.
 * 3) Start a new transaction
 * 4) With managed copies of the two entities, edit BiEntityA(id=1) and BiEntityA(id=1).cascadeAll's name fields.
 * 5) Invoke EntityManager.refresh() on BiEntityA(id=1)
 * 6) Verify that BiEntityA(id=1)'s name field has been reverted to the value it had when it was created.
 * 7) Verify that BiEntityB(id=1)'s name field has been reverted to the value it had when it was created.
 */

    @Test
    public void jpa10_Relationships_ManyXMany_Bidirectional_003_Ano_AMJTA_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_Bidirectional_003_Ano_AMJTA_EJB_SF";
        final String testMethod = "testManyXManyUni003";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "MMBiEntA");
        properties.put("EntityBName", "MMBiEntB_CA");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_ManyXMany_Bidirectional_003_XML_AMJTA_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_Bidirectional_003_XML_AMJTA_EJB_SF";
        final String testMethod = "testManyXManyUni003";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLMMBiEntA");
        properties.put("EntityBName", "XMLMMBiEntB_CA");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_ManyXMany_Bidirectional_003_Ano_AMRL_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_Bidirectional_003_Ano_AMRL_EJB_SF";
        final String testMethod = "testManyXManyUni003";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "MMBiEntA");
        properties.put("EntityBName", "MMBiEntB_CA");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_ManyXMany_Bidirectional_003_XML_AMRL_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_Bidirectional_003_XML_AMRL_EJB_SF";
        final String testMethod = "testManyXManyUni003";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLMMBiEntA");
        properties.put("EntityBName", "XMLMMBiEntB_CA");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_ManyXMany_Bidirectional_003_Ano_CMTS_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_Bidirectional_003_Ano_CMTS_EJB_SF";
        final String testMethod = "testManyXManyUni003";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "MMBiEntA");
        properties.put("EntityBName", "MMBiEntB_CA");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_ManyXMany_Bidirectional_003_XML_CMTS_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_Bidirectional_003_XML_CMTS_EJB_SF";
        final String testMethod = "testManyXManyUni003";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLMMBiEntA");
        properties.put("EntityBName", "XMLMMBiEntB_CA");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

/*
 * Test Cascade, Type: PERSIST Verification
 *
 * Verify cascade: persist. By this:
 *
 * Persist:
 *
 * With cascade type set to PERSIST, persist operations are cascaded across the
 * entity relationship. This means if BiEntityA is the target if an
 * EntityManager.persist() operation, the JPA implementation will automatically
 * invoke EntityManager.persist() on all of the BiEntityB entity relationships
 * that are marked with Cascade type PERSIST.
 *
 * 1) Create an unpersisted BiEntityB(id=1)
 * 2) Create an unpersisted BiEntityA(id=1)
 * 3) Add BiEntityB(id=1) to cascadePersist.
 * 4) Start a new transaction, persist BiEntityA(id=1), and commit the transaction.
 * 5) Clear persistence context, forcing all entities to become detached.
 * 6) Find BiEntityA(id=1), this should hit the database, returning an entity
 * with a reference in cascadePersist to BiEntityB(id=1) because the persist operation
 * was cascaded.
 */

    @Test
    public void jpa10_Relationships_ManyXMany_Bidirectional_004_Ano_AMJTA_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_Bidirectional_004_Ano_AMJTA_EJB_SF";
        final String testMethod = "testManyXManyUni004";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "MMBiEntA");
        properties.put("EntityBName", "MMBiEntB_CP");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_ManyXMany_Bidirectional_004_XML_AMJTA_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_Bidirectional_004_XML_AMJTA_EJB_SF";
        final String testMethod = "testManyXManyUni004";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLMMBiEntA");
        properties.put("EntityBName", "XMLMMBiEntB_CP");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_ManyXMany_Bidirectional_004_Ano_AMRL_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_Bidirectional_004_Ano_AMRL_EJB_SF";
        final String testMethod = "testManyXManyUni004";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "MMBiEntA");
        properties.put("EntityBName", "MMBiEntB_CP");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_ManyXMany_Bidirectional_004_XML_AMRL_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_Bidirectional_004_XML_AMRL_EJB_SF";
        final String testMethod = "testManyXManyUni004";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLMMBiEntA");
        properties.put("EntityBName", "XMLMMBiEntB_CP");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_ManyXMany_Bidirectional_004_Ano_CMTS_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_Bidirectional_004_Ano_CMTS_EJB_SF";
        final String testMethod = "testManyXManyUni004";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "MMBiEntA");
        properties.put("EntityBName", "MMBiEntB_CP");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_ManyXMany_Bidirectional_004_XML_CMTS_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_Bidirectional_004_XML_CMTS_EJB_SF";
        final String testMethod = "testManyXManyUni004";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLMMBiEntA");
        properties.put("EntityBName", "XMLMMBiEntB_CP");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

/*
 * Test Cascade, Type: REMOVE Verification
 *
 * Verify cascade: remove. By this:
 *
 * Remove:
 *
 * With cascade type set to REMOVE, remove operations are cascaded across the entity
 * relationship. This means if BiEntityA is the target of an EntityManager.remove()
 * operation, the JPA implementation will automatically invoke EntityManager.remove()
 * on all of the BiEntityB entity relationships that are marked with Cascade type REMOVE.
 *
 * 1) Create Create BiEntityB(id=1)
 * 2) Create new BiEntityA(id=1), add BiEntityB(id=1) to cascadeRemove.
 * 3) Remove BiEntityA(id=1), verify that BiEntityB(id=1) also no longer exists. Test point passes if true.
 */

    @Test
    @ExpectedFFDC("javax.transaction.RollbackException")
    public void jpa10_Relationships_ManyXMany_Bidirectional_005_Ano_AMJTA_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_Bidirectional_005_Ano_AMJTA_EJB_SF";
        final String testMethod = "testManyXManyUni005";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "MMBiEntA");
        properties.put("EntityBName", "MMBiEntB_CRM");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    @ExpectedFFDC("javax.transaction.RollbackException")
    public void jpa10_Relationships_ManyXMany_Bidirectional_005_XML_AMJTA_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_Bidirectional_005_XML_AMJTA_EJB_SF";
        final String testMethod = "testManyXManyUni005";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLMMBiEntA");
        properties.put("EntityBName", "XMLMMBiEntB_CRM");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_ManyXMany_Bidirectional_005_Ano_AMRL_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_Bidirectional_005_Ano_AMRL_EJB_SF";
        final String testMethod = "testManyXManyUni005";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "MMBiEntA");
        properties.put("EntityBName", "MMBiEntB_CRM");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_ManyXMany_Bidirectional_005_XML_AMRL_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_Bidirectional_005_XML_AMRL_EJB_SF";
        final String testMethod = "testManyXManyUni005";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLMMBiEntA");
        properties.put("EntityBName", "XMLMMBiEntB_CRM");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    @ExpectedFFDC("javax.transaction.RollbackException")
    public void jpa10_Relationships_ManyXMany_Bidirectional_005_Ano_CMTS_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_Bidirectional_005_Ano_CMTS_EJB_SF";
        final String testMethod = "testManyXManyUni005";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "MMBiEntA");
        properties.put("EntityBName", "MMBiEntB_CRM");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    @ExpectedFFDC("javax.transaction.RollbackException")
    public void jpa10_Relationships_ManyXMany_Bidirectional_005_XML_CMTS_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_Bidirectional_005_XML_CMTS_EJB_SF";
        final String testMethod = "testManyXManyUni005";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLMMBiEntA");
        properties.put("EntityBName", "XMLMMBiEntB_CRM");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

/*
 * Test Cascade, Type: MERGE Verification
 *
 * Verify cascade: merge. By this:
 *
 * Merge:
 *
 * With cascade type set to MERGE, merge operations will cascade across the entity relationship. So changes to
 * the target entity (BiEntityA(id=1)) and changes to all entities with relationship cascade attributes of
 * MERGE will be merged into the persistence context.
 *
 * 1) Create Create BiEntityB(id=1)
 * 2) Create new BiEntityA(id=1), add BiEntityB(id=1) to cascadeMerge.
 * 3) Clear the persistence context, causing all entities to become detached
 * 4) Modify BiEntityA(id=1)'s name field. Modify BiEntityB(id=1)'s name field.
 * 5) Merge BiEntityA(id=1) into the persistence context. Examine the BiEntityB(id=1) referenced by the cascadeMerge
 * field of the entity returned from the merge() operation. Both entities should be managed, and both
 * entities should contain the changes from step 4.
 */

    @Test
    @ExpectedFFDC("javax.transaction.RollbackException")
    public void jpa10_Relationships_ManyXMany_Bidirectional_006_Ano_AMJTA_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_Bidirectional_006_Ano_AMJTA_EJB_SF";
        final String testMethod = "testManyXManyUni006";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "MMBiEntA");
        properties.put("EntityBName", "MMBiEntB_CM");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    @ExpectedFFDC("javax.transaction.RollbackException")
    public void jpa10_Relationships_ManyXMany_Bidirectional_006_XML_AMJTA_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_Bidirectional_006_XML_AMJTA_EJB_SF";
        final String testMethod = "testManyXManyUni006";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLMMBiEntA");
        properties.put("EntityBName", "XMLMMBiEntB_CM");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_ManyXMany_Bidirectional_006_Ano_AMRL_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_Bidirectional_006_Ano_AMRL_EJB_SF";
        final String testMethod = "testManyXManyUni006";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "MMBiEntA");
        properties.put("EntityBName", "MMBiEntB_CM");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_ManyXMany_Bidirectional_006_XML_AMRL_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_Bidirectional_006_XML_AMRL_EJB_SF";
        final String testMethod = "testManyXManyUni006";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLMMBiEntA");
        properties.put("EntityBName", "XMLMMBiEntB_CM");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    @ExpectedFFDC("javax.transaction.RollbackException")
    public void jpa10_Relationships_ManyXMany_Bidirectional_006_Ano_CMTS_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_Bidirectional_006_Ano_CMTS_EJB_SF";
        final String testMethod = "testManyXManyUni006";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "MMBiEntA");
        properties.put("EntityBName", "MMBiEntB_CM");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    @ExpectedFFDC("javax.transaction.RollbackException")
    public void jpa10_Relationships_ManyXMany_Bidirectional_006_XML_CMTS_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_Bidirectional_006_XML_CMTS_EJB_SF";
        final String testMethod = "testManyXManyUni006";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLMMBiEntA");
        properties.put("EntityBName", "XMLMMBiEntB_CM");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

/*
 * Test Cascade, Type: REFRESH Verification
 *
 * Verify cascade: refresh. By this:
 *
 * Refresh:
 *
 * With cascade type set to REFRESH, refresh operations will cascade across entity relationships. So changes
 * to the target entity (BiEntityA(id=1)) and changes to all entities with relationship cascade attributes of
 * REFRESH will have their contents reset to match the data in the database.
 *
 * 1) Create Create BiEntityB(id=1)
 * 2) Create new BiEntityA(id=1), add BiEntityB(id=1) to cascadeRefresh.
 * 3) Start a new transaction
 * 4) With managed copies of the two entities, edit BiEntityA(id=1) and BiEntityA(id=1).cascadeRefresh's name fields.
 * 5) Invoke EntityManager.refresh() on BiEntityA(id=1)
 * 6) Verify that BiEntityA(id=1)'s name field has been reverted to the value it had when it was created.
 * 7) Verify that BiEntityB(id=1)'s name field has been reverted to the value it had when it was created.
 */

    @Test
    @ExpectedFFDC("javax.transaction.RollbackException")
    public void jpa10_Relationships_ManyXMany_Bidirectional_007_Ano_AMJTA_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_Bidirectional_007_Ano_AMJTA_EJB_SF";
        final String testMethod = "testManyXManyUni007";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "MMBiEntA");
        properties.put("EntityBName", "MMBiEntB_CRF");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    @ExpectedFFDC("javax.transaction.RollbackException")
    public void jpa10_Relationships_ManyXMany_Bidirectional_007_XML_AMJTA_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_Bidirectional_007_XML_AMJTA_EJB_SF";
        final String testMethod = "testManyXManyUni007";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLMMBiEntA");
        properties.put("EntityBName", "XMLMMBiEntB_CRF");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_ManyXMany_Bidirectional_007_Ano_AMRL_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_Bidirectional_007_Ano_AMRL_EJB_SF";
        final String testMethod = "testManyXManyUni007";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "MMBiEntA");
        properties.put("EntityBName", "MMBiEntB_CRF");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_ManyXMany_Bidirectional_007_XML_AMRL_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_Bidirectional_007_XML_AMRL_EJB_SF";
        final String testMethod = "testManyXManyUni007";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLMMBiEntA");
        properties.put("EntityBName", "XMLMMBiEntB_CRF");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    @ExpectedFFDC("javax.transaction.RollbackException")
    public void jpa10_Relationships_ManyXMany_Bidirectional_007_Ano_CMTS_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_Bidirectional_007_Ano_CMTS_EJB_SF";
        final String testMethod = "testManyXManyUni007";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "MMBiEntA");
        properties.put("EntityBName", "MMBiEntB_CRF");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    @ExpectedFFDC("javax.transaction.RollbackException")
    public void jpa10_Relationships_ManyXMany_Bidirectional_007_XML_CMTS_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_Bidirectional_007_XML_CMTS_EJB_SF";
        final String testMethod = "testManyXManyUni007";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLMMBiEntA");
        properties.put("EntityBName", "XMLMMBiEntB_CRF");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

/*
 * Cardinality Test: Verify that multiple entities can be stored in the relational collection. (15 POINTS)
 *
 * 1) Create BiEntityB(id=1), save to database.
 * 2) Create BiEntityB(id=2), save to database.
 * 3) Create new BiEntityA(id=1), add BiEntityB(id=1) and BiEntityB(id=2) to defaultRelationship, and save to database.
 * 4) Create new BiEntityA(id=2), add BiEntityB(id=1) and BiEntityB(id=2) to defaultRelationship, and save to database.
 * 5) Clear the persistence context, and verify that both entities' database state. Test passes if both
 * instances of BiEntityA's defaultRelationship collections have references to both BiEntityB.
 */

    @Test
    public void jpa10_Relationships_ManyXMany_Bidirectional_TestCardinality001_Ano_AMJTA_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_Bidirectional_TestCardinality001_Ano_AMJTA_EJB_SF";
        final String testMethod = "testCardinality001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "MMBiEntA");
        properties.put("EntityBName", "MMBiEntB_DR");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_ManyXMany_Bidirectional_TestCardinality001_XML_AMJTA_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_Bidirectional_TestCardinality001_XML_AMJTA_EJB_SF";
        final String testMethod = "testCardinality001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLMMBiEntA");
        properties.put("EntityBName", "XMLMMBiEntB_DR");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_ManyXMany_Bidirectional_TestCardinality001_Ano_AMRL_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_Bidirectional_TestCardinality001_Ano_AMRL_EJB_SF";
        final String testMethod = "testCardinality001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "MMBiEntA");
        properties.put("EntityBName", "MMBiEntB_DR");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_ManyXMany_Bidirectional_TestCardinality001_XML_AMRL_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_Bidirectional_TestCardinality001_XML_AMRL_EJB_SF";
        final String testMethod = "testCardinality001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLMMBiEntA");
        properties.put("EntityBName", "XMLMMBiEntB_DR");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_ManyXMany_Bidirectional_TestCardinality001_Ano_CMTS_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_Bidirectional_TestCardinality001_Ano_CMTS_EJB_SF";
        final String testMethod = "testCardinality001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "MMBiEntA");
        properties.put("EntityBName", "MMBiEntB_DR");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_ManyXMany_Bidirectional_TestCardinality001_XML_CMTS_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_Bidirectional_TestCardinality001_XML_CMTS_EJB_SF";
        final String testMethod = "testCardinality001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLMMBiEntA");
        properties.put("EntityBName", "XMLMMBiEntB_DR");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

/*
 * Verify basic Many-to-Many service by creating UniEntityA and UniEntityB, and
 * add BiEntityA to the collection BiEntityB.entityA. (9 POINTS)
 *
 * Test verifies function performing the following:
 * 1) Create new BiEntityA(id=1)
 * 2) Create new BiEntityB(id=1), add BiEntityB(id=1) to its EntityA Collection.
 * 3) Keep other (optional) ManyXMany collections empty.
 * 4) Clear persistence context (if necessary), and find BiEntityB(id=1).
 * 5) Verify field data in BiEntityB(id=1)
 * 6) BiEntityBB's EntityA Collection should contain BiEntityA(id=1). Extract it
 * and verify its field data.
 * 7) Test passes if all these conditions are met.
 *
 * This test case confirms the following function verification:
 * 2) A traversable ManyXMany association between BiEntityB to BiEntityA(id=1) is
 * established, and BiEntityA is accessible through the relational
 * reference EntityA Collection in BiEntityB.
 */

    @Test
    public void jpa10_Relationships_ManyXMany_Bidirectional_Inverse_001_Ano_AMJTA_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_Bidirectional_Inverse_001_Ano_AMJTA_EJB_SF";
        final String testMethod = "testManyXManyBi001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "MMBiEntA");
        properties.put("EntityBName", "MMBiEntB_DR");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_ManyXMany_Bidirectional_Inverse_001_XML_AMJTA_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_Bidirectional_Inverse_001_XML_AMJTA_EJB_SF";
        final String testMethod = "testManyXManyBi001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLMMBiEntA");
        properties.put("EntityBName", "XMLMMBiEntB_DR");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_ManyXMany_Bidirectional_Inverse_001_Ano_AMRL_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_Bidirectional_Inverse_001_Ano_AMRL_EJB_SF";
        final String testMethod = "testManyXManyBi001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "MMBiEntA");
        properties.put("EntityBName", "MMBiEntB_DR");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_ManyXMany_Bidirectional_Inverse_001_XML_AMRL_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_Bidirectional_Inverse_001_XML_AMRL_EJB_SF";
        final String testMethod = "testManyXManyBi001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLMMBiEntA");
        properties.put("EntityBName", "XMLMMBiEntB_DR");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_ManyXMany_Bidirectional_Inverse_001_Ano_CMTS_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_Bidirectional_Inverse_001_Ano_CMTS_EJB_SF";
        final String testMethod = "testManyXManyBi001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "MMBiEntA");
        properties.put("EntityBName", "MMBiEntB_DR");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_ManyXMany_Bidirectional_Inverse_001_XML_CMTS_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_Bidirectional_Inverse_001_XML_CMTS_EJB_SF";
        final String testMethod = "testManyXManyBi001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLMMBiEntA");
        properties.put("EntityBName", "XMLMMBiEntB_DR");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

/*
 * Test Cascade, Type: Default (no cascade options specified) Verification
 *
 * Verify cascade: none behavior (default). By this:
 *
 * Persist:
 *
 * Both entities in the relationship need to have persist() invoked on them
 * in order to be stored on the database. By default, persisting the inverse side
 * of the bidirectional relationship does not automatically persist the entity on the owning
 * side, and vice versa.
 *
 * 1) In a new transaction, create an persisted BiEntityB_DR(id=1)
 * and an unpersisted BiEntityA(id=1), setting BiEntityB_DR(id=1)'s entityA Collection to BiEntityA(id=1).
 * 2) Commit the transaction. An IllegalStateException should be thrown, because
 * entityAField is referencing an entity that is not managed and does not exist on the database.
 *
 * Remove:
 *
 * By default, removing the entity on the inverse side of the manyXmany bidirectional relationship does
 * not cause the entity on the owning side of the relationship to become removed as well.
 *
 * 1) Create BiEntityB_DR(id=1)
 * 2) Create new BiEntityA(id=1), set BiEntityB_DR(id=1)'s entityA Collection to BiEntityA(id=1).
 * 3) Remove BiEntityB_DR(id=1)), verify that BiEntityA(id=1) still exists. Test point passes if it does.
 *
 * If the entity on the owning side of the bidirectional relationship is removed, it should not remove the entity
 * on the inverse of the relationship. Also, since the manyXmany relationship is optional, the relationship
 * field should be set null on fresh instances of the inverse entity from find().
 *
 * 1) Create BiEntityB_DR(id=1)
 * 2) Create new BiEntityA(id=1), set BiEntityB_DR(id=1)'s entityA Collection to BiEntityA(id=1).
 * 3) Remove BiEntityA(id=1)
 * 4) Clear the persistence context
 * 5) Find BiEntityB_DR(id=1). It should still exist, and entityAField should be null.
 *
 * Merge:
 *
 * Merge will attempt to update the managed entity to point to managed versions of entities referenced by
 * the detached entity.
 *
 * 1) Create BiEntityB_DR(id=1)
 * 2) Create new BiEntityA(id=1), set BiEntityB_DR(id=1)'s entityA Collection to BiEntityA(id=1).
 * 3) Clear the persistence context, causing all entities to become detached
 * 4) Perform the following operations:
 * Modify the name field on BiEntityB_DR(id=1)
 * Modify the name field on BiEntityA(id=1)
 * 5) Merge BiEntityB_DR(id=1) into the persistence context.
 * 6) Verify the change to BiEntityB_DR(id=1)'s name field. Verify that BiEntityB_DR(id=1)'s entityAField field references
 * a managed copy of BiEntityA(id=1) and that BiEntityA(id=1)'s name field contains the original value.
 * BiEntityA(id=1)'s name field should not have been changed because EntityManager.merge() was never called
 * on it directly, and merge by default does not cascade across entity relationships.
 *
 * If an entity being merged points to a removed entity, an IllegalArgumentException should be thrown.
 *
 * 1) Create BiEntityB_DR(id=1)
 * 2) Create new BiEntityA(id=1), set BiEntityB_DR(id=1)'s entityA Collection to BiEntityA(id=1).
 * 3) Clear the persistence context
 * 4) In a single transaction, find BiEntityA(id=1) and remove it. This deletes BiEntityA(id=1) from the database.
 * Note: the original detached BiEntityA(id=1) reference still exists, and is referenced by the detached copy of
 * BiEntityB_DR(id=1).
 * 5) Modify the name field of BiEntityB_DR(id=1) and attempt to merge it. An IllegalArgumentException because the
 * detached entity still has a references in its entityAField field to BiEntityA(id=1), which no longer exists in the database.
 *
 * Refresh:
 *
 * Refresh operations are, by default, not cascaded across entity relationships. Without the REFRESH cascade
 * option, a refresh operation will stop at the source entity.
 *
 * 1) Create BiEntityB_DR(id=1)
 * 2) Create new BiEntityA(id=1), set BiEntityB_DR(id=1)'s entityA Collection to BiEntityA(id=1).
 * 3) Start a new transaction
 * 4) With managed copies of the two entities, edit BiEntityB_DR(id=1) and BiEntityB_DR(id=1).BiEntityB_DR(id=1)'s name fields.
 * 5) Invoke EntityManager.refresh() on BiEntityB_DR(id=1)
 * 6) Verify that BiEntityB_DR(id=1)'s name field has been reverted to the value it had when it was created.
 * 7) Verify that BiEntityA(id=1) still has the new value
 */

    @Test
    @ExpectedFFDC("javax.transaction.RollbackException")
    public void jpa10_Relationships_ManyXMany_Bidirectional_Inverse_002_Ano_AMJTA_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_Bidirectional_Inverse_002_Ano_AMJTA_EJB_SF";
        final String testMethod = "testManyXManyBi002";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "MMBiEntA");
        properties.put("EntityBName", "MMBiEntB_DR");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    @ExpectedFFDC("javax.transaction.RollbackException")
    public void jpa10_Relationships_ManyXMany_Bidirectional_Inverse_002_XML_AMJTA_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_Bidirectional_Inverse_002_XML_AMJTA_EJB_SF";
        final String testMethod = "testManyXManyBi002";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLMMBiEntA");
        properties.put("EntityBName", "XMLMMBiEntB_DR");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_ManyXMany_Bidirectional_Inverse_002_Ano_AMRL_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_Bidirectional_Inverse_002_Ano_AMRL_EJB_SF";
        final String testMethod = "testManyXManyBi002";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "MMBiEntA");
        properties.put("EntityBName", "MMBiEntB_DR");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_ManyXMany_Bidirectional_Inverse_002_XML_AMRL_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_Bidirectional_Inverse_002_XML_AMRL_EJB_SF";
        final String testMethod = "testManyXManyBi002";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLMMBiEntA");
        properties.put("EntityBName", "XMLMMBiEntB_DR");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    @ExpectedFFDC("javax.transaction.RollbackException")
    public void jpa10_Relationships_ManyXMany_Bidirectional_Inverse_002_Ano_CMTS_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_Bidirectional_Inverse_002_Ano_CMTS_EJB_SF";
        final String testMethod = "testManyXManyBi002";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "MMBiEntA");
        properties.put("EntityBName", "MMBiEntB_DR");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    @ExpectedFFDC("javax.transaction.RollbackException")
    public void jpa10_Relationships_ManyXMany_Bidirectional_Inverse_002_XML_CMTS_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_Bidirectional_Inverse_002_XML_CMTS_EJB_SF";
        final String testMethod = "testManyXManyBi002";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLMMBiEntA");
        properties.put("EntityBName", "XMLMMBiEntB_DR");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

/*
 * Cascade:all declaration verification.
 *
 * Cascade type overrides are unidirectional, meaning that in a bidirectional relationship, a @ManyToMany relational
 * marker with CascadeType=ALL only applies to the direction of the relationship the tag modifies, and not also
 * to the inverse direction. So therefore, if CascadeType=ALL is applied to the relationship on the owning side,
 * only operations originating on the owning relationship are cascaded to the entity on the inverse side of the
 * relationship. Unless the relational field on the entity of the inverse side is also marked CascadeType=ALL,
 * then operations originating on the inverse entity are not cascaded across to the entity on the owning side of
 * the relationship.
 *
 *
 * Verify cascade:all behavior on the owning side does not override default cascade:none behavior on the inverse side.
 *
 * Persist:
 *
 * Both entities in the relationship need to have persist() invoked on them
 * in order to be stored on the database. By default, persisting the inverse side
 * of the bidirectional relationship does not automatically persist the entity on the owning
 * side, and vice versa.
 *
 * 1) In a new transaction, create an persisted BiEntityB_CA(id=1)
 * and an unpersisted BiEntityA(id=1), adding BiEntityA(id=1) to BiEntityB_CA(id=1)'s EntityA Collection.
 * 2) Commit the transaction. An IllegalStateException should be thrown, because
 * entityAField is referencing an entity that is not managed and does not exist on the database.
 *
 * Remove:
 *
 * By default, removing the entity on the inverse side of the oneXone bidirectional relationship does
 * not cause the entity on the owning side of the relationship to become removed as well.
 *
 * 1) Create BiEntityB_CA(id=1)
 * 2) Create new BiEntityA(id=1), add BiEntityA(id=1) to BiEntityB_CA(id=1)'s EntityA Collection.
 * 3) Remove BiEntityB_CA(id=1)), verify that BiEntityA(id=1) still exists. Test point passes if it does.
 *
 * Merge:
 *
 * Merge will attempt to update the managed entity to point to managed versions of entities referenced by
 * the detached entity.
 *
 * 1) Create BiEntityB_CA(id=1)
 * 2) Create new BiEntityA(id=1), add BiEntityA(id=1) to BiEntityB_CA(id=1)'s EntityA Collection.
 * 3) Clear the persistence context, causing all entities to become detached
 * 4) Perform the following operations:
 * Modify the name field on BiEntityB_CA(id=1)
 * Modify the name field on BiEntityA(id=1)
 * 5) Merge BiEntityB_CA(id=1) into the persistence context.
 * 6) Verify the change to BiEntityB_CA(id=1)'s name field. Verify that BiEntityB_CA(id=1)'s entityAField collection contains
 * a managed copy of BiEntityA(id=1) and that BiEntityA(id=1)'s name field contains the original value.
 * BiEntityA(id=1)'s name field should not have been changed because EntityManager.merge() was never called
 * on it directly, and merge by default does not cascade across entity relationships.
 *
 * Refresh:
 *
 * Refresh operations are, by default, not cascaded across entity relationships. Without the REFRESH cascade
 * option, a refresh operation will stop at the source entity.
 *
 * 1) Create BiEntityB_CA(id=1)
 * 2) Create new BiEntityA(id=1), add BiEntityB_CA(id=1)'s EntityA Collection to BiEntityA(id=1).
 * 3) Start a new transaction
 * 4) With managed copies of the two entities, edit BiEntityB_CA(id=1) and BiEntityB_CA(id=1)->BiEntityA(id=1)'s name fields.
 * 5) Invoke EntityManager.refresh() on BiEntityB_CA(id=1)
 * 6) Verify that BiEntityB_CA(id=1)'s name field has been reverted to the value it had when it was created.
 * 7) Verify that BiEntityA(id=1) still has the new value
 */

    @Test
    @ExpectedFFDC("javax.transaction.RollbackException")
    public void jpa10_Relationships_ManyXMany_Bidirectional_Inverse_003_Ano_AMJTA_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_Bidirectional_Inverse_003_Ano_AMJTA_EJB_SF";
        final String testMethod = "testManyXManyBi003";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "MMBiEntA");
        properties.put("EntityBName", "MMBiEntB_CA");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    @ExpectedFFDC("javax.transaction.RollbackException")
    public void jpa10_Relationships_ManyXMany_Bidirectional_Inverse_003_XML_AMJTA_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_Bidirectional_Inverse_003_XML_AMJTA_EJB_SF";
        final String testMethod = "testManyXManyBi003";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLMMBiEntA");
        properties.put("EntityBName", "XMLMMBiEntB_CA");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_ManyXMany_Bidirectional_Inverse_003_Ano_AMRL_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_Bidirectional_Inverse_003_Ano_AMRL_EJB_SF";
        final String testMethod = "testManyXManyBi003";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "MMBiEntA");
        properties.put("EntityBName", "MMBiEntB_CA");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_ManyXMany_Bidirectional_Inverse_003_XML_AMRL_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_Bidirectional_Inverse_003_XML_AMRL_EJB_SF";
        final String testMethod = "testManyXManyBi003";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLMMBiEntA");
        properties.put("EntityBName", "XMLMMBiEntB_CA");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    @ExpectedFFDC("javax.transaction.RollbackException")
    public void jpa10_Relationships_ManyXMany_Bidirectional_Inverse_003_Ano_CMTS_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_Bidirectional_Inverse_003_Ano_CMTS_EJB_SF";
        final String testMethod = "testManyXManyBi003";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "MMBiEntA");
        properties.put("EntityBName", "MMBiEntB_CA");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    @ExpectedFFDC("javax.transaction.RollbackException")
    public void jpa10_Relationships_ManyXMany_Bidirectional_Inverse_003_XML_CMTS_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_Bidirectional_Inverse_003_XML_CMTS_EJB_SF";
        final String testMethod = "testManyXManyBi003";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLMMBiEntA");
        properties.put("EntityBName", "XMLMMBiEntB_CA");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

/*
 * Cascade type overrides are unidirectional, meaning that in a bidirectional relationship, a @ManyToMany relational
 * marker with CascadeType=PERSIST only applies to the direction of the relationship the tag modifies, and not also
 * to the inverse direction. So therefore, if CascadeType=PERSIST is applied to the relationship on the owning side,
 * only operations originating on the owning relationship are cascaded to the entity on the inverse side of the
 * relationship. Unless the relational field on the entity of the inverse side is also marked CascadeType=PERSIST,
 * then operations originating on the inverse entity are not cascaded across to the entity on the owning side of
 * the relationship.
 *
 * Verify cascade:persist behavior on the owning side does not override default cascade:none behavior on the inverse side.
 *
 * Both entities in the relationship need to have persist() invoked on them
 * in order to be stored on the database. By default, persisting the inverse side
 * of the bidirectional relationship does not automatically persist the entity on the owning
 * side, and vice versa.
 *
 * 1) In a new transaction, create an persisted BiEntityB_CP(id=1)
 * and an unpersisted BiEntityA(id=1), add BiEntityA(id=1) to BiEntityB_CP(id=1)'s EntityA Collection.
 * 2) Commit the transaction. An IllegalStateException should be thrown, because
 * the EntityA in BiEntityB_CP is an entity that is not managed and does not exist on the database.
 */

    @Test
    @ExpectedFFDC("javax.transaction.RollbackException")
    public void jpa10_Relationships_ManyXMany_Bidirectional_Inverse_004_Ano_AMJTA_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_Bidirectional_Inverse_004_Ano_AMJTA_EJB_SF";
        final String testMethod = "testManyXManyBi004";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "MMBiEntA");
        properties.put("EntityBName", "MMBiEntB_CP");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    @ExpectedFFDC("javax.transaction.RollbackException")
    public void jpa10_Relationships_ManyXMany_Bidirectional_Inverse_004_XML_AMJTA_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_Bidirectional_Inverse_004_XML_AMJTA_EJB_SF";
        final String testMethod = "testManyXManyBi004";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLMMBiEntA");
        properties.put("EntityBName", "XMLMMBiEntB_CP");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_ManyXMany_Bidirectional_Inverse_004_Ano_AMRL_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_Bidirectional_Inverse_004_Ano_AMRL_EJB_SF";
        final String testMethod = "testManyXManyBi004";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "MMBiEntA");
        properties.put("EntityBName", "MMBiEntB_CP");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_ManyXMany_Bidirectional_Inverse_004_XML_AMRL_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_Bidirectional_Inverse_004_XML_AMRL_EJB_SF";
        final String testMethod = "testManyXManyBi004";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLMMBiEntA");
        properties.put("EntityBName", "XMLMMBiEntB_CP");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    @ExpectedFFDC("javax.transaction.RollbackException")
    public void jpa10_Relationships_ManyXMany_Bidirectional_Inverse_004_Ano_CMTS_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_Bidirectional_Inverse_004_Ano_CMTS_EJB_SF";
        final String testMethod = "testManyXManyBi004";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "MMBiEntA");
        properties.put("EntityBName", "MMBiEntB_CP");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    @ExpectedFFDC("javax.transaction.RollbackException")
    public void jpa10_Relationships_ManyXMany_Bidirectional_Inverse_004_XML_CMTS_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_Bidirectional_Inverse_004_XML_CMTS_EJB_SF";
        final String testMethod = "testManyXManyBi004";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLMMBiEntA");
        properties.put("EntityBName", "XMLMMBiEntB_CP");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

/*
 * Cascade:Remove declaration verification.
 *
 * Cascade type overrides are unidirectional, meaning that in a bidirectional relationship, a @ManyToMany relational
 * marker with CascadeType=REMOVE only applies to the direction of the relationship the tag modifies, and not also
 * to the inverse direction. So therefore, if CascadeType=REMOVE is applied to the relationship on the owning side,
 * only operations originating on the owning relationship are cascaded to the entity on the inverse side of the
 * relationship. Unless the relational field on the entity of the inverse side is also marked CascadeType=REMOVE,
 * then operations originating on the inverse entity are not cascaded across to the entity on the owning side of
 * the relationship.
 *
 * Verify cascade:remove behavior on the owning side does not override default cascade:none behavior on the inverse side.
 *
 * By default, removing the entity on the inverse side of the oneXone bidirectional relationship does
 * not cause the entity on the owning side of the relationship to become removed as well.
 *
 * 1) Create BiEntityB_CRM(id=1)
 * 2) Create new BiEntityA(id=1), add BiEntityA(id=1) to BiEntityB_CRM(id=1)'s EntityA Collection.
 * 3) Remove BiEntityB_CRM(id=1)), verify that BiEntityA(id=1) still exists. Test point passes if it does.
 *
 */

    @Test
    public void jpa10_Relationships_ManyXMany_Bidirectional_Inverse_005_Ano_AMJTA_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_Bidirectional_Inverse_005_Ano_AMJTA_EJB_SF";
        final String testMethod = "testManyXManyBi005";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "MMBiEntA");
        properties.put("EntityBName", "MMBiEntB_CRM");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_ManyXMany_Bidirectional_Inverse_005_XML_AMJTA_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_Bidirectional_Inverse_005_XML_AMJTA_EJB_SF";
        final String testMethod = "testManyXManyBi005";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLMMBiEntA");
        properties.put("EntityBName", "XMLMMBiEntB_CRM");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_ManyXMany_Bidirectional_Inverse_005_Ano_AMRL_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_Bidirectional_Inverse_005_Ano_AMRL_EJB_SF";
        final String testMethod = "testManyXManyBi005";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "MMBiEntA");
        properties.put("EntityBName", "MMBiEntB_CRM");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_ManyXMany_Bidirectional_Inverse_005_XML_AMRL_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_Bidirectional_Inverse_005_XML_AMRL_EJB_SF";
        final String testMethod = "testManyXManyBi005";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLMMBiEntA");
        properties.put("EntityBName", "XMLMMBiEntB_CRM");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_ManyXMany_Bidirectional_Inverse_005_Ano_CMTS_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_Bidirectional_Inverse_005_Ano_CMTS_EJB_SF";
        final String testMethod = "testManyXManyBi005";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "MMBiEntA");
        properties.put("EntityBName", "MMBiEntB_CRM");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_ManyXMany_Bidirectional_Inverse_005_XML_CMTS_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_Bidirectional_Inverse_005_XML_CMTS_EJB_SF";
        final String testMethod = "testManyXManyBi005";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLMMBiEntA");
        properties.put("EntityBName", "XMLMMBiEntB_CRM");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

/*
 * Cascade:Merge declaration verification.
 *
 * Cascade type overrides are unidirectional, meaning that in a bidirectional relationship, a @ManyToMany relational
 * marker with CascadeType=MERGE only applies to the direction of the relationship the tag modifies, and not also
 * to the inverse direction. So therefore, if CascadeType=MERGE is applied to the relationship on the owning side,
 * only operations originating on the owning relationship are cascaded to the entity on the inverse side of the
 * relationship. Unless the relational field on the entity of the inverse side is also marked CascadeType=MERGE,
 * then operations originating on the inverse entity are not cascaded across to the entity on the owning side of
 * the relationship.
 *
 * Verify cascade:merge behavior on the owning side does not override default cascade:none behavior on the inverse side.
 *
 * Merge will attempt to update the managed entity to point to managed versions of entities referenced by
 * the detached entity.
 *
 * 1) Create BiEntityB_CM(id=1)
 * 2) Create new BiEntityA(id=1), add BiEntityA(id=1) to BiEntityB_CM(id=1)'s EntityA Collection.
 * 3) Clear the persistence context, causing all entities to become detached
 * 4) Perform the following operations:
 * Modify the name field on BiEntityB_CM(id=1)
 * Modify the name field on BiEntityA(id=1)
 * 5) Merge BiEntityB_CM(id=1) into the persistence context.
 * 6) Verify the change to BiEntityB_CM(id=1)'s name field. Verify that BiEntityB_CM(id=1)'s entityAField field references
 * a managed copy of BiEntityA(id=1) and that BiEntityA(id=1)'s name field contains the original value.
 * BiEntityA(id=1)'s name field should not have been changed because EntityManager.merge() was never called
 * on it directly, and merge by default does not cascade across entity relationships.
 *
 */

    @Test
    public void jpa10_Relationships_ManyXMany_Bidirectional_Inverse_006_Ano_AMJTA_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_Bidirectional_Inverse_006_Ano_AMJTA_EJB_SF";
        final String testMethod = "testManyXManyBi006";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "MMBiEntA");
        properties.put("EntityBName", "MMBiEntB_CM");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_ManyXMany_Bidirectional_Inverse_006_XML_AMJTA_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_Bidirectional_Inverse_006_XML_AMJTA_EJB_SF";
        final String testMethod = "testManyXManyBi006";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLMMBiEntA");
        properties.put("EntityBName", "XMLMMBiEntB_CM");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_ManyXMany_Bidirectional_Inverse_006_Ano_AMRL_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_Bidirectional_Inverse_006_Ano_AMRL_EJB_SF";
        final String testMethod = "testManyXManyBi006";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "MMBiEntA");
        properties.put("EntityBName", "MMBiEntB_CM");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_ManyXMany_Bidirectional_Inverse_006_XML_AMRL_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_Bidirectional_Inverse_006_XML_AMRL_EJB_SF";
        final String testMethod = "testManyXManyBi006";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLMMBiEntA");
        properties.put("EntityBName", "XMLMMBiEntB_CM");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_ManyXMany_Bidirectional_Inverse_006_Ano_CMTS_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_Bidirectional_Inverse_006_Ano_CMTS_EJB_SF";
        final String testMethod = "testManyXManyBi006";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "MMBiEntA");
        properties.put("EntityBName", "MMBiEntB_CM");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_ManyXMany_Bidirectional_Inverse_006_XML_CMTS_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_Bidirectional_Inverse_006_XML_CMTS_EJB_SF";
        final String testMethod = "testManyXManyBi006";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLMMBiEntA");
        properties.put("EntityBName", "XMLMMBiEntB_CM");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

/*
 * Cascade:refresh declaration verification.
 *
 * Cascade type overrides are unidirectional, meaning that in a bidirectional relationship, a @ManyToMany relational
 * marker with CascadeType=REFRESH only applies to the direction of the relationship the tag modifies, and not also
 * to the inverse direction. So therefore, if CascadeType=REFRESH is applied to the relationship on the owning side,
 * only operations originating on the owning relationship are cascaded to the entity on the inverse side of the
 * relationship. Unless the relational field on the entity of the inverse side is also marked CascadeType=REFRESH,
 * then operations originating on the inverse entity are not cascaded across to the entity on the owning side of
 * the relationship.
 *
 * Verify cascade:refresh behavior on the owning side does not override default cascade:none behavior on the inverse side.
 *
 * Refresh operations are, by default, not cascaded across entity relationships. Without the REFRESH cascade
 * option, a refresh operation will stop at the source entity.
 *
 * 1) Create BiEntityB_CRF(id=1)
 * 2) Create new BiEntityA(id=1), add BiEntityA(id=1) to BiEntityB_CRF(id=1)'s EntityA Collection.
 * 3) Start a new transaction
 * 4) With managed copies of the two entities, edit BiEntityB_B5RF(id=1) and BiEntityB_CRF(id=1)->BiEntityA(id=1)'s name fields.
 * 5) Invoke EntityManager.refresh() on BiEntityB_CRF(id=1)
 * 6) Verify that BiEntityB_CRF(id=1)'s name field has been reverted to the value it had when it was created.
 * 7) Verify that BiEntityA(id=1) still has the new value
 */

    @Test
    public void jpa10_Relationships_ManyXMany_Bidirectional_Inverse_007_Ano_AMJTA_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_Bidirectional_Inverse_007_Ano_AMJTA_EJB_SF";
        final String testMethod = "testManyXManyBi007";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "MMBiEntA");
        properties.put("EntityBName", "MMBiEntB_CRF");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_ManyXMany_Bidirectional_Inverse_007_XML_AMJTA_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_Bidirectional_Inverse_007_XML_AMJTA_EJB_SF";
        final String testMethod = "testManyXManyBi007";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLMMBiEntA");
        properties.put("EntityBName", "XMLMMBiEntB_CRF");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_ManyXMany_Bidirectional_Inverse_007_Ano_AMRL_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_Bidirectional_Inverse_007_Ano_AMRL_EJB_SF";
        final String testMethod = "testManyXManyBi007";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "MMBiEntA");
        properties.put("EntityBName", "MMBiEntB_CRF");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_ManyXMany_Bidirectional_Inverse_007_XML_AMRL_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_Bidirectional_Inverse_007_XML_AMRL_EJB_SF";
        final String testMethod = "testManyXManyBi007";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLMMBiEntA");
        properties.put("EntityBName", "XMLMMBiEntB_CRF");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_ManyXMany_Bidirectional_Inverse_007_Ano_CMTS_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_Bidirectional_Inverse_007_Ano_CMTS_EJB_SF";
        final String testMethod = "testManyXManyBi007";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "MMBiEntA");
        properties.put("EntityBName", "MMBiEntB_CRF");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_ManyXMany_Bidirectional_Inverse_007_XML_CMTS_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_Bidirectional_Inverse_007_XML_CMTS_EJB_SF";
        final String testMethod = "testManyXManyBi007";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLMMBiEntA");
        properties.put("EntityBName", "XMLMMBiEntB_CRF");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

/*
 * Cardinality Test: Verify that no more then one entity on the inverse side of the relationship can have a relationship
 * with a specific instance of an entity on the owning side of the relationship.
 *
 * 1) Create BiEntityA(id=1), save to database.
 * 2) Create BiEntityA(id=2), save to database.
 * 3) Create new BiEntity_DC(id=1), add BiEntityA(id=1) and BiEntityA(id=2) to EntityA Collection, and save to database.
 * 4) Create new BiEntity_DC(id=2), add BiEntityA(id=1) and BiEntityA(id=2) to EntityA Collection, and save to database.
 * 5) Clear the persistence context, and verify that both entities' database state. Test passes if both
 * instances of UniEntityB's EntityA collections have references to both UniEntityA entities.
 */

    @Test
    public void jpa10_Relationships_ManyXMany_Bidirectional_Inverse_TestCardinality001_Ano_AMJTA_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_Bidirectional_Inverse_TestCardinality001_Ano_AMJTA_EJB_SF";
        final String testMethod = "testBiCardinality001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "MMBiEntA");
        properties.put("EntityBName", "MMBiEntB_DR");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_ManyXMany_Bidirectional_Inverse_TestCardinality001_XML_AMJTA_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_Bidirectional_Inverse_TestCardinality001_XML_AMJTA_EJB_SF";
        final String testMethod = "testBiCardinality001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLMMBiEntA");
        properties.put("EntityBName", "XMLMMBiEntB_DR");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_ManyXMany_Bidirectional_Inverse_TestCardinality001_Ano_AMRL_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_Bidirectional_Inverse_TestCardinality001_Ano_AMRL_EJB_SF";
        final String testMethod = "testBiCardinality001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "MMBiEntA");
        properties.put("EntityBName", "MMBiEntB_DR");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_ManyXMany_Bidirectional_Inverse_TestCardinality001_XML_AMRL_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_Bidirectional_Inverse_TestCardinality001_XML_AMRL_EJB_SF";
        final String testMethod = "testBiCardinality001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLMMBiEntA");
        properties.put("EntityBName", "XMLMMBiEntB_DR");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_ManyXMany_Bidirectional_Inverse_TestCardinality001_Ano_CMTS_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_Bidirectional_Inverse_TestCardinality001_Ano_CMTS_EJB_SF";
        final String testMethod = "testBiCardinality001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "MMBiEntA");
        properties.put("EntityBName", "MMBiEntB_DR");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_ManyXMany_Bidirectional_Inverse_TestCardinality001_XML_CMTS_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_Bidirectional_Inverse_TestCardinality001_XML_CMTS_EJB_SF";
        final String testMethod = "testBiCardinality001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLMMBiEntA");
        properties.put("EntityBName", "XMLMMBiEntB_DR");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

}
