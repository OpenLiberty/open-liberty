/*******************************************************************************
 * Copyright (c) 2019, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.fvt.relationships.oneXmany.tests.ejb;

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

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/TestOneXManyUnidirectional_EJB_SL_Servlet")
public class TestOneXManyUnidirectional_EJB_SL_Servlet extends EJBTestVehicleServlet {
    private final String testLogicClassName = "com.ibm.ws.jpa.fvt.relationships.oneXmany.testlogic.OneXManyUnidirectionalTestLogic";

    private final HashMap<String, JPAPersistenceContext> jpaPctxMap = new HashMap<String, JPAPersistenceContext>();

    private final static String ejbJNDIName = "ejb/OneXManySLEJB";

    @PostConstruct
    private void initFAT() {
        jpaPctxMap.put("test-jpa-resource-amjta",
                       new JPAPersistenceContext("test-jpa-resource-amjta", PersistenceContextType.APPLICATION_MANAGED_JTA, PersistenceInjectionType.JNDI, "java:comp/env/jpa/OneXMany_Uni_AMJTA"));
        jpaPctxMap.put("test-jpa-resource-amrl",
                       new JPAPersistenceContext("test-jpa-resource-amrl", PersistenceContextType.APPLICATION_MANAGED_RL, PersistenceInjectionType.JNDI, "java:comp/env/jpa/OneXMany_Uni_AMRL"));
        jpaPctxMap.put("test-jpa-resource-cmts",
                       new JPAPersistenceContext("test-jpa-resource-cmts", PersistenceContextType.CONTAINER_MANAGED_TS, PersistenceInjectionType.JNDI, "java:comp/env/jpa/OneXMany_Uni_CMTS"));
    }

    /*
     * Verify basic One-to-Many service by creating UniEntityA and UniEntityB, and
     * add UniEntityB to the collection UniEntityA.defaultRelationship.
     *
     * Test verifies function performing the following:
     * 1) Create new UniEntityB(id=1)
     * 2) Create new UniEntityA(id=1), add UniEntityB(id=1) to defaultRelationship.
     * 3) Keep other (optional) OneXMany collections empty.
     * 4) Clear persistence context (if necessary), and find UniEntityA(id=1).
     * 5) Verify field data in UniEntityA(id=1)
     * 6) UniEntityA.defaultRelationship should contain UniEntityB(id=1). Extract it
     * and verify its field data.
     * 7) Test passes if all these conditions are met.
     *
     * This test case confirms the following function verification:
     * 1) Optional relational fields can be empty without persistence errors
     * (all fields except defaultRelationship are empty in UniEntityA(id=1))
     * 2) A traversable OneXMany association between UniEntityA to UniEntityB(id=1) is
     * established, and UniEntityB is accessible through the relational
     * reference defaultRelationship in UniEntityA.
     */

    @Test
    public void jpa10_Relationships_OneXMany_Unidirectional_001_Ano_AMJTA_EJB_SL() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_Unidirectional_001_Ano_AMJTA_EJB_SL";
        final String testMethod = "testOneXManyUni001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OMEntA");
        properties.put("EntityBName", "OMEntB_DR");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXMany_Unidirectional_001_XML_AMJTA_EJB_SL() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_Unidirectional_001_XML_AMJTA_EJB_SL";
        final String testMethod = "testOneXManyUni001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOMEntA");
        properties.put("EntityBName", "XMLOMEntB_DR");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXMany_Unidirectional_001_Ano_AMRL_EJB_SL() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_Unidirectional_001_Ano_AMRL_EJB_SL";
        final String testMethod = "testOneXManyUni001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OMEntA");
        properties.put("EntityBName", "OMEntB_DR");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXMany_Unidirectional_001_XML_AMRL_EJB_SL() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_Unidirectional_001_XML_AMRL_EJB_SL";
        final String testMethod = "testOneXManyUni001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOMEntA");
        properties.put("EntityBName", "XMLOMEntB_DR");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXMany_Unidirectional_001_Ano_CMTS_EJB_SL() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_Unidirectional_001_Ano_CMTS_EJB_SL";
        final String testMethod = "testOneXManyUni001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OMEntA");
        properties.put("EntityBName", "OMEntB_DR");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXMany_Unidirectional_001_XML_CMTS_EJB_SL() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_Unidirectional_001_XML_CMTS_EJB_SL";
        final String testMethod = "testOneXManyUni001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOMEntA");
        properties.put("EntityBName", "XMLOMEntB_DR");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXMany_Unidirectional_001_Ano_AMRL_Web() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_Unidirectional_001_Ano_AMRL_Web";
        final String testMethod = "testOneXManyUni001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OMEntA");
        properties.put("EntityBName", "OMEntB_DR");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXMany_Unidirectional_001_XML_AMRL_Web() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_Unidirectional_001_XML_AMRL_Web";
        final String testMethod = "testOneXManyUni001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOMEntA");
        properties.put("EntityBName", "XMLOMEntB_DR");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXMany_Unidirectional_001_Ano_CMTS_Web() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_Unidirectional_001_Ano_CMTS_Web";
        final String testMethod = "testOneXManyUni001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OMEntA");
        properties.put("EntityBName", "OMEntB_DR");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXMany_Unidirectional_001_XML_CMTS_Web() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_Unidirectional_001_XML_CMTS_Web";
        final String testMethod = "testOneXManyUni001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOMEntA");
        properties.put("EntityBName", "XMLOMEntB_DR");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
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
     * 1) Create an unpersisted UniEntityB(id=1).
     * 2) Create an unpersisted UniEntityA(id=1), add UniEntityB(id=1) to defaultRelationship.
     * 3) In a transaction, persist only UniEntityA(id=1) to the database.
     * Note that no persist command is invoked on UniEntityB(id=1), and since, by default,
     * persist operations are not cascaded across entity relationships, the persist operation
     * on UniEntityA(id=1) should not cause UniEntityB(id=1) to become persisted/managed.
     * 4) Clear persistence context, forcing all entities to become detached.
     * 5) Find UniEntityA(id=1), this should hit the database, returning an entity
     * with no entries in defaultRelationship because UniEntityB(id=1) was never persisted.
     *
     * Remove:
     *
     * By default, removing the entity on the owning side of the OneXMany relationship does
     * not cause the entity on the inverse side of the relationship to become removed as well.
     *
     * 1) Create Create UniEntityB(id=1)
     * 2) Create new UniEntityA(id=1), adding UniEntityB(id=1) to defaultRelationship.
     * 3) Remove UniEntityA(id=1), verify that UniEntityB(id=1) still exists. Test point passes if it does.
     *
     * If the entity on the inverse side of the relationship is removed, it should not remove the entity
     * on the owning of the relationship. Also, since the OneXMany relationship is optional, the relationship
     * field collection should be empty on fresh instances of the owning entity from find().
     *
     * 1) Create Create UniEntityB(id=1)
     * 2) Create new UniEntityA(id=1), add UniEntityB(id=1) to defaultRelationship.
     * 3) Remove UniEntityB(id=1)
     * 4) Clear the persistence context
     * 5) Find UniEntityA(id=1). It should still exist (especially given that it is a unidirectional
     * relationship), and defaultRelationship should be empty.
     *
     * Merge:
     *
     * Merge will attempt to update the managed entity to point to managed versions of entities referenced by
     * the detached entity.
     *
     * 1) Create UniEntityB(id=1)
     * 2) Create new UniEntityA(id=1), add UniEntityB(id=1) to defaultRelationship.
     * 3) Clear the persistence context, causing all entities to become detached
     * 4) Perform the following operations:
     * Modify the name field on UniEntityB(id=1)
     * Modify the name field on UniEntityA(id=1)
     * 5) Merge UniEntityA(id=1) into the persistence context.
     * 6) Verify the change to UniEntityA(id=1)'s name field. Verify that UniEntityA(id=1)'s defaultRelationship field references
     * a managed copy of UniEntityB(id=1) and that UniEntityB(id=1)'s name field contains the original value.
     * UniEntityB(id=1)'s name field should not have been changed because EntityManager.merge() was never called
     * on it directly, and merge by default does not cascade across entity relationships.
     *
     * If an entity being merged points to a removed entity, an IllegalArgumentException should be thrown.
     *
     * 1) Create Create UniEntityB(id=1)
     * 2) Create new UniEntityA(id=1), add UniEntityB(id=1) to defaultRelationship.
     * 3) Clear the persistence context
     * 4) In a single transaction, find UniEntityB(id=1) and remove it. This deletes UniEntityB(id=1) from the database.
     * Note: the original detached UniEntityB(id=1) reference still exists, and is referenced by the detached copy of
     * UniEntityA(id=1).
     * 5) Modify the name field of UniEntityA(id=1) and attempt to merge it. An IllegalArgumentException because the
     * detached entity still has a references in its defaultRelationship field to UniEntityB(id=1), which no longer
     * exists in the database.
     *
     * Refresh:
     *
     * Refresh operations are, by default, not cascaded across entity relationships. Without the REFRESH cascade
     * option, a refresh operation will stop at the source entity.
     *
     * 1) Create Create UniEntityB(id=1)
     * 2) Create new UniEntityA(id=1), add UniEntityB(id=1) to defaultRelationship.
     * 3) Start a new transaction
     * 4) With managed copies of the two entities, edit UniEntityA(id=1) and UniEntityA(id=1).defaultRelationship's name fields.
     * 5) Invoke EntityManager.refresh() on UniEntityA(id=1)
     * 6) Verify that UniEntityA(id=1)'s name field has been reverted to the value it had when it was created.
     * 7) Verify that UniEntityB(id=1) still has the new value
     */

    @Test
    @ExpectedFFDC("javax.transaction.RollbackException")
    public void jpa10_Relationships_OneXMany_Unidirectional_002_Ano_AMJTA_EJB_SL() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_Unidirectional_002_Ano_AMJTA_EJB_SL";
        final String testMethod = "testOneXManyUni002";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OMEntA");
        properties.put("EntityBName", "OMEntB_DR");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    @ExpectedFFDC("javax.transaction.RollbackException")
    public void jpa10_Relationships_OneXMany_Unidirectional_002_XML_AMJTA_EJB_SL() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_Unidirectional_002_XML_AMJTA_EJB_SL";
        final String testMethod = "testOneXManyUni002";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOMEntA");
        properties.put("EntityBName", "XMLOMEntB_DR");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXMany_Unidirectional_002_Ano_AMRL_EJB_SL() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_Unidirectional_002_Ano_AMRL_EJB_SL";
        final String testMethod = "testOneXManyUni002";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OMEntA");
        properties.put("EntityBName", "OMEntB_DR");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXMany_Unidirectional_002_XML_AMRL_EJB_SL() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_Unidirectional_002_XML_AMRL_EJB_SL";
        final String testMethod = "testOneXManyUni002";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOMEntA");
        properties.put("EntityBName", "XMLOMEntB_DR");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    @ExpectedFFDC("javax.transaction.RollbackException")
    public void jpa10_Relationships_OneXMany_Unidirectional_002_Ano_CMTS_EJB_SL() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_Unidirectional_002_Ano_CMTS_EJB_SL";
        final String testMethod = "testOneXManyUni002";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OMEntA");
        properties.put("EntityBName", "OMEntB_DR");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    @ExpectedFFDC("javax.transaction.RollbackException")
    public void jpa10_Relationships_OneXMany_Unidirectional_002_XML_CMTS_EJB_SL() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_Unidirectional_002_XML_CMTS_EJB_SL";
        final String testMethod = "testOneXManyUni002";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOMEntA");
        properties.put("EntityBName", "XMLOMEntB_DR");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
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
     * entity relationship. This means if UniEntityA is the target if an
     * EntityManager.persist() operation, the JPA implementation will automatically
     * invoke EntityManager.persist() on all of the UniEntityB entity relationships
     * that are marked with Cascade type ALL (or PERSIST).
     *
     * 1) Create an unpersisted UniEntityB(id=1)
     * 2) Create an unpersisted UniEntityA(id=1)
     * 3) Add UniEntityB(id=1) to cascadeAll.
     * 4) Start a new transaction, persist UniEntityA(id=1), and commit the transaction.
     * 5) Clear persistence context, forcing all entities to become detached.
     * 6) Find UniEntityA(id=1), this should hit the database, returning an entity
     * with a reference in cascadeAll to UniEntityB(id=1) because the persist operation
     * was cascaded.
     *
     * Remove:
     *
     * With cascade type set to ALL, remove operations are cascaded across the entity
     * relationship. This means if UniEntityA is the target of an EntityManager.remove()
     * operation, the JPA implementation will automatically invoke EntityManager.remove()
     * on all of the UniEntityB entity relationships that are marked with Cascade type ALL
     * (or REMOVE).
     *
     * 1) Create Create UniEntityB(id=1)
     * 2) Create new UniEntityA(id=1), add UniEntityB(id=1) to cascadeAll.
     * 3) Remove UniEntityA(id=1), verify that UniEntityB(id=1) also no longer exists. Test point passes if true.
     *
     * Merge:
     *
     * With cascade type set to ALL, merge operations will cascade across the entity relationship. So changes to
     * the target entity (UniEntityA(id=1)) and changes to all entities with relationship cascade attributes of
     * ALL (and MERGE) will be merged into the persistence context.
     *
     * 1) Create Create UniEntityB(id=1)
     * 2) Create new UniEntityA(id=1), add UniEntityB(id=1) to cascadeAll.
     * 3) Clear the persistence context, causing all entities to become detached
     * 4) Modify UniEntityA(id=1)'s name field. Modify UniEntityB(id=1)'s name field.
     * 5) Merge UniEntityA(id=1) into the persistence context. Examine the UniEntityB(id=1) referenced by the cascadeAll
     * field of the entity returned from the merge() operation. Both entities should be managed, and both
     * entities should contain the changes from step 4.
     *
     * Refresh:
     *
     * With cascade type set to ALL, refresh operations will cascade across entity relationships. So changes
     * to the target entity (UniEntityA(id=1)) and changes to all entities with relationship cascade attributes of
     * ALL (and REFRESH) will have their contents reset to match the data in the database.
     *
     * 1) Create Create UniEntityB(id=1)
     * 2) Create new UniEntityA(id=1), add UniEntityB(id=1) to cascadeAll.
     * 3) Start a new transaction
     * 4) With managed copies of the two entities, edit UniEntityA(id=1) and UniEntityA(id=1).cascadeAll's name fields.
     * 5) Invoke EntityManager.refresh() on UniEntityA(id=1)
     * 6) Verify that UniEntityA(id=1)'s name field has been reverted to the value it had when it was created.
     * 7) Verify that UniEntityB(id=1)'s name field has been reverted to the value it had when it was created.
     *
     */

    @Test
    public void jpa10_Relationships_OneXMany_Unidirectional_003_Ano_AMJTA_EJB_SL() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_Unidirectional_003_Ano_AMJTA_EJB_SL";
        final String testMethod = "testOneXManyUni003";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OMEntA");
        properties.put("EntityBName", "OMEntB_CA");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXMany_Unidirectional_003_XML_AMJTA_EJB_SL() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_Unidirectional_003_XML_AMJTA_EJB_SL";
        final String testMethod = "testOneXManyUni003";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOMEntA");
        properties.put("EntityBName", "XMLOMEntB_CA");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXMany_Unidirectional_003_Ano_AMRL_EJB_SL() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_Unidirectional_003_Ano_AMRL_EJB_SL";
        final String testMethod = "testOneXManyUni003";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OMEntA");
        properties.put("EntityBName", "OMEntB_CA");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXMany_Unidirectional_003_XML_AMRL_EJB_SL() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_Unidirectional_003_XML_AMRL_EJB_SL";
        final String testMethod = "testOneXManyUni003";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOMEntA");
        properties.put("EntityBName", "XMLOMEntB_CA");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXMany_Unidirectional_003_Ano_CMTS_EJB_SL() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_Unidirectional_003_Ano_CMTS_EJB_SL";
        final String testMethod = "testOneXManyUni003";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OMEntA");
        properties.put("EntityBName", "OMEntB_CA");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXMany_Unidirectional_003_XML_CMTS_EJB_SL() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_Unidirectional_003_XML_CMTS_EJB_SL";
        final String testMethod = "testOneXManyUni003";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOMEntA");
        properties.put("EntityBName", "XMLOMEntB_CA");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
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
     * entity relationship. This means if UniEntityA is the target if an
     * EntityManager.persist() operation, the JPA implementation will automatically
     * invoke EntityManager.persist() on all of the UniEntityB entity relationships
     * that are marked with Cascade type PERSIST.
     *
     * 1) Create an unpersisted UniEntityB(id=1)
     * 2) Create an unpersisted UniEntityA(id=1)
     * 3) Add UniEntityB(id=1) to cascadePersist.
     * 4) Start a new transaction, persist UniEntityA(id=1), and commit the transaction.
     * 5) Clear persistence context, forcing all entities to become detached.
     * 6) Find UniEntityA(id=1), this should hit the database, returning an entity
     * with a reference in cascadePersist to UniEntityB(id=1) because the persist operation
     * was cascaded.
     *
     */

    @Test
    public void jpa10_Relationships_OneXMany_Unidirectional_004_Ano_AMJTA_EJB_SL() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_Unidirectional_004_Ano_AMJTA_EJB_SL";
        final String testMethod = "testOneXManyUni004";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OMEntA");
        properties.put("EntityBName", "OMEntB_CP");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXMany_Unidirectional_004_XML_AMJTA_EJB_SL() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_Unidirectional_004_XML_AMJTA_EJB_SL";
        final String testMethod = "testOneXManyUni004";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOMEntA");
        properties.put("EntityBName", "XMLOMEntB_CP");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXMany_Unidirectional_004_Ano_AMRL_EJB_SL() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_Unidirectional_004_Ano_AMRL_EJB_SL";
        final String testMethod = "testOneXManyUni004";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OMEntA");
        properties.put("EntityBName", "OMEntB_CP");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXMany_Unidirectional_004_XML_AMRL_EJB_SL() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_Unidirectional_004_XML_AMRL_EJB_SL";
        final String testMethod = "testOneXManyUni004";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOMEntA");
        properties.put("EntityBName", "XMLOMEntB_CP");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXMany_Unidirectional_004_Ano_CMTS_EJB_SL() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_Unidirectional_004_Ano_CMTS_EJB_SL";
        final String testMethod = "testOneXManyUni004";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OMEntA");
        properties.put("EntityBName", "OMEntB_CP");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXMany_Unidirectional_004_XML_CMTS_EJB_SL() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_Unidirectional_004_XML_CMTS_EJB_SL";
        final String testMethod = "testOneXManyUni004";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOMEntA");
        properties.put("EntityBName", "XMLOMEntB_CP");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
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
     * relationship. This means if UniEntityA is the target of an EntityManager.remove()
     * operation, the JPA implementation will automatically invoke EntityManager.remove()
     * on all of the UniEntityB entity relationships that are marked with Cascade type REMOVE.
     *
     * 1) Create Create UniEntityB(id=1)
     * 2) Create new UniEntityA(id=1), add UniEntityB(id=1) to cascadeRemove.
     * 3) Remove UniEntityA(id=1), verify that UniEntityB(id=1) also no longer exists. Test point passes if true.
     *
     *
     */

    @Test
    @ExpectedFFDC("javax.transaction.RollbackException")
    public void jpa10_Relationships_OneXMany_Unidirectional_005_Ano_AMJTA_EJB_SL() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_Unidirectional_005_Ano_AMJTA_EJB_SL";
        final String testMethod = "testOneXManyUni005";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OMEntA");
        properties.put("EntityBName", "OMEntB_CRM");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    @ExpectedFFDC("javax.transaction.RollbackException")
    public void jpa10_Relationships_OneXMany_Unidirectional_005_XML_AMJTA_EJB_SL() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_Unidirectional_005_XML_AMJTA_EJB_SL";
        final String testMethod = "testOneXManyUni005";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOMEntA");
        properties.put("EntityBName", "XMLOMEntB_CRM");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXMany_Unidirectional_005_Ano_AMRL_EJB_SL() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_Unidirectional_005_Ano_AMRL_EJB_SL";
        final String testMethod = "testOneXManyUni005";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OMEntA");
        properties.put("EntityBName", "OMEntB_CRM");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXMany_Unidirectional_005_XML_AMRL_EJB_SL() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_Unidirectional_005_XML_AMRL_EJB_SL";
        final String testMethod = "testOneXManyUni005";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOMEntA");
        properties.put("EntityBName", "XMLOMEntB_CRM");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    @ExpectedFFDC("javax.transaction.RollbackException")
    public void jpa10_Relationships_OneXMany_Unidirectional_005_Ano_CMTS_EJB_SL() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_Unidirectional_005_Ano_CMTS_EJB_SL";
        final String testMethod = "testOneXManyUni005";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OMEntA");
        properties.put("EntityBName", "OMEntB_CRM");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    @ExpectedFFDC("javax.transaction.RollbackException")
    public void jpa10_Relationships_OneXMany_Unidirectional_005_XML_CMTS_EJB_SL() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_Unidirectional_005_XML_CMTS_EJB_SL";
        final String testMethod = "testOneXManyUni005";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOMEntA");
        properties.put("EntityBName", "XMLOMEntB_CRM");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
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
     * the target entity (UniEntityA(id=1)) and changes to all entities with relationship cascade attributes of
     * MERGE will be merged into the persistence context.
     *
     * 1) Create Create UniEntityB(id=1)
     * 2) Create new UniEntityA(id=1), add UniEntityB(id=1) to cascadeMerge.
     * 3) Clear the persistence context, causing all entities to become detached
     * 4) Modify UniEntityA(id=1)'s name field. Modify UniEntityB(id=1)'s name field.
     * 5) Merge UniEntityA(id=1) into the persistence context. Examine the UniEntityB(id=1) referenced by the cascadeMerge
     * field of the entity returned from the merge() operation. Both entities should be managed, and both
     * entities should contain the changes from step 4.
     */

    @Test
    @ExpectedFFDC("javax.transaction.RollbackException")
    public void jpa10_Relationships_OneXMany_Unidirectional_006_Ano_AMJTA_EJB_SL() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_Unidirectional_006_Ano_AMJTA_EJB_SL";
        final String testMethod = "testOneXManyUni006";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OMEntA");
        properties.put("EntityBName", "OMEntB_CM");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    @ExpectedFFDC("javax.transaction.RollbackException")
    public void jpa10_Relationships_OneXMany_Unidirectional_006_XML_AMJTA_EJB_SL() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_Unidirectional_006_XML_AMJTA_EJB_SL";
        final String testMethod = "testOneXManyUni006";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOMEntA");
        properties.put("EntityBName", "XMLOMEntB_CM");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXMany_Unidirectional_006_Ano_AMRL_EJB_SL() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_Unidirectional_006_Ano_AMRL_EJB_SL";
        final String testMethod = "testOneXManyUni006";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OMEntA");
        properties.put("EntityBName", "OMEntB_CM");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXMany_Unidirectional_006_XML_AMRL_EJB_SL() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_Unidirectional_006_XML_AMRL_EJB_SL";
        final String testMethod = "testOneXManyUni006";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOMEntA");
        properties.put("EntityBName", "XMLOMEntB_CM");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    @ExpectedFFDC("javax.transaction.RollbackException")
    public void jpa10_Relationships_OneXMany_Unidirectional_006_Ano_CMTS_EJB_SL() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_Unidirectional_006_Ano_CMTS_EJB_SL";
        final String testMethod = "testOneXManyUni006";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OMEntA");
        properties.put("EntityBName", "OMEntB_CM");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    @ExpectedFFDC("javax.transaction.RollbackException")
    public void jpa10_Relationships_OneXMany_Unidirectional_006_XML_CMTS_EJB_SL() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_Unidirectional_006_XML_CMTS_EJB_SL";
        final String testMethod = "testOneXManyUni006";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOMEntA");
        properties.put("EntityBName", "XMLOMEntB_CM");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
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
     * to the target entity (UniEntityA(id=1)) and changes to all entities with relationship cascade attributes of
     * REFRESH will have their contents reset to match the data in the database.
     *
     * 1) Create Create UniEntityB(id=1)
     * 2) Create new UniEntityA(id=1), add UniEntityB(id=1) to cascadeRefresh.
     * 3) Start a new transaction
     * 4) With managed copies of the two entities, edit UniEntityA(id=1) and UniEntityA(id=1).cascadeRefresh's name fields.
     * 5) Invoke EntityManager.refresh() on UniEntityA(id=1)
     * 6) Verify that UniEntityA(id=1)'s name field has been reverted to the value it had when it was created.
     * 7) Verify that UniEntityB(id=1)'s name field has been reverted to the value it had when it was created.
     *
     */

    @Test
    @ExpectedFFDC("javax.transaction.RollbackException")
    public void jpa10_Relationships_OneXMany_Unidirectional_007_Ano_AMJTA_EJB_SL() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_Unidirectional_007_Ano_AMJTA_EJB_SL";
        final String testMethod = "testOneXManyUni007";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OMEntA");
        properties.put("EntityBName", "OMEntB_CRF");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    @ExpectedFFDC("javax.transaction.RollbackException")
    public void jpa10_Relationships_OneXMany_Unidirectional_007_XML_AMJTA_EJB_SL() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_Unidirectional_007_XML_AMJTA_EJB_SL";
        final String testMethod = "testOneXManyUni007";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOMEntA");
        properties.put("EntityBName", "XMLOMEntB_CRF");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXMany_Unidirectional_007_Ano_AMRL_EJB_SL() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_Unidirectional_007_Ano_AMRL_EJB_SL";
        final String testMethod = "testOneXManyUni007";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OMEntA");
        properties.put("EntityBName", "OMEntB_CRF");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXMany_Unidirectional_007_XML_AMRL_EJB_SL() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_Unidirectional_007_XML_AMRL_EJB_SL";
        final String testMethod = "testOneXManyUni007";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOMEntA");
        properties.put("EntityBName", "XMLOMEntB_CRF");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    @ExpectedFFDC("javax.transaction.RollbackException")
    public void jpa10_Relationships_OneXMany_Unidirectional_007_Ano_CRFTS_EJB_SL() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_Unidirectional_007_Ano_CRFTS_EJB_SL";
        final String testMethod = "testOneXManyUni007";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OMEntA");
        properties.put("EntityBName", "OMEntB_CRF");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    @ExpectedFFDC("javax.transaction.RollbackException")
    public void jpa10_Relationships_OneXMany_Unidirectional_007_XML_CRFTS_EJB_SL() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_Unidirectional_007_XML_CRFTS_EJB_SL";
        final String testMethod = "testOneXManyUni007";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOMEntA");
        properties.put("EntityBName", "XMLOMEntB_CRF");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    /*
     * Cardinality Test: Verify that multiple entities can be stored in the relational collection.
     *
     * 1) Create UniEntityB(id=1), save to database.
     * 2) Create UniEntityB(id=2), save to database.
     * 3) Create new UniEntityA(id=1), add UniEntityB(id=1) and UniEntityB(id=2) to defaultRelationship, and save to database.
     * 4) Create new UniEntityA(id=2), add UniEntityB(id=1) and UniEntityB(id=2) to defaultRelationship, and save to database.
     * 5) Clear the persistence context, and verify EntityA's database state. Test passes if EntityA's
     * defaultRelationship collections have references to both UniEntityB.
     *
     */

    @Test
    public void jpa10_Relationships_OneXMany_Unidirectional_Cardinality_Ano_AMJTA_EJB_SL() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_Unidirectional_Cardinality_Ano_AMJTA_EJB_SL";
        final String testMethod = "testCardinality001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OMEntA");
        properties.put("EntityBName", "OMEntB_DR");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXMany_Unidirectional_Cardinality_XML_AMJTA_EJB_SL() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_Unidirectional_Cardinality_XML_AMJTA_EJB_SL";
        final String testMethod = "testCardinality001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOMEntA");
        properties.put("EntityBName", "XMLOMEntB_DR");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXMany_Unidirectional_Cardinality_Ano_AMRL_EJB_SL() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_Unidirectional_Cardinality_Ano_AMRL_EJB_SL";
        final String testMethod = "testCardinality001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OMEntA");
        properties.put("EntityBName", "OMEntB_DR");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXMany_Unidirectional_Cardinality_XML_AMRL_EJB_SL() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_Unidirectional_Cardinality_XML_AMRL_EJB_SL";
        final String testMethod = "testCardinality001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOMEntA");
        properties.put("EntityBName", "XMLOMEntB_DR");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXMany_Unidirectional_Cardinality_Ano_CRFTS_EJB_SL() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_Unidirectional_Cardinality_Ano_CRFTS_EJB_SL";
        final String testMethod = "testCardinality001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OMEntA");
        properties.put("EntityBName", "OMEntB_DR");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXMany_Unidirectional_Cardinality_XML_CRFTS_EJB_SL() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_Unidirectional_Cardinality_XML_CRFTS_EJB_SL";
        final String testMethod = "testCardinality001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOMEntA");
        properties.put("EntityBName", "XMLOMEntB_DR");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }
}
