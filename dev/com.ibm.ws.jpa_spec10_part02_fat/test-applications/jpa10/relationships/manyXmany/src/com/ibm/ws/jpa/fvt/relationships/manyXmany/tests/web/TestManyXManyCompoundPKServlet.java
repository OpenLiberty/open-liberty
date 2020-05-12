/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.fvt.relationships.manyXmany.tests.web;

import java.util.HashMap;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.jpa.fvt.relationships.manyXmany.entities.compoundpk.annotated.EmbedIDMMEntityA;
import com.ibm.ws.jpa.fvt.relationships.manyXmany.entities.compoundpk.annotated.EmbedIDMMEntityB;
import com.ibm.ws.jpa.fvt.relationships.manyXmany.entities.compoundpk.annotated.IDClassMMEntityA;
import com.ibm.ws.jpa.fvt.relationships.manyXmany.entities.compoundpk.annotated.IDClassMMEntityB;
import com.ibm.ws.jpa.fvt.relationships.manyXmany.entities.compoundpk.xml.XMLEmbedIDMMEntityA;
import com.ibm.ws.jpa.fvt.relationships.manyXmany.entities.compoundpk.xml.XMLEmbedIDMMEntityB;
import com.ibm.ws.jpa.fvt.relationships.manyXmany.entities.compoundpk.xml.XMLIDClassMMEntityA;
import com.ibm.ws.jpa.fvt.relationships.manyXmany.entities.compoundpk.xml.XMLIDClassMMEntityB;
import com.ibm.ws.jpa.fvt.relationships.manyXmany.testlogic.ManyXManyCompoundPKTestLogic;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.vehicle.web.JPATestServlet;

@WebServlet(urlPatterns = "/TestManyXManyCompoundPKServlet")
public class TestManyXManyCompoundPKServlet extends JPATestServlet {
    private static final long serialVersionUID = 1L;

    // Container Managed Transaction Scope
    @PersistenceContext(unitName = "ManyXMany_CompoundPK_JTA")
    private EntityManager mxmCompoundPKEm;

    // Application Managed JTA
    @PersistenceUnit(unitName = "ManyXMany_CompoundPK_JTA")
    private EntityManagerFactory mxmCompoundPKEmf;

    // Application Managed Resource-Local
    @PersistenceUnit(unitName = "ManyXMany_CompoundPK_RL")
    private EntityManagerFactory mxmCompoundPKEmfRL;

    @PostConstruct
    private void initFAT() {
        testClassName = ManyXManyCompoundPKTestLogic.class.getName();

        jpaPctxMap.put("test-jpa-resource-amjta",
                       new JPAPersistenceContext("test-jpa-resource-amjta", PersistenceContextType.APPLICATION_MANAGED_JTA, PersistenceInjectionType.FIELD, "mxmCompoundPKEmf"));
        jpaPctxMap.put("test-jpa-resource-amrl",
                       new JPAPersistenceContext("test-jpa-resource-amrl", PersistenceContextType.APPLICATION_MANAGED_RL, PersistenceInjectionType.FIELD, "mxmCompoundPKEmfRL"));
        jpaPctxMap.put("test-jpa-resource-cmts",
                       new JPAPersistenceContext("test-jpa-resource-cmts", PersistenceContextType.CONTAINER_MANAGED_TS, PersistenceInjectionType.FIELD, "mxmCompoundPKEm"));
    }

/*
 * Verify that the JPA provider can manage ManyXMany relationships where the
 * entity on the inverse side of the relationship has a compound (Embeddable/ID Class) primary key.
 * Entities and IdClass are defined in annotation.
 *
 * Test Strategy:
 *
 * 1) Start a new transaction
 * 2) Create ICompoundPKManyXManyEntityA(id=1)
 * Create ICompoundPKManyXManyEntityB(id=1)
 * Set ICompoundPKManyXManyEntityA(id=1) to reference ICompoundPKManyXManyEntityB(id=1) in
 * a ManyXMany relationship
 * 3) Commit the transaction
 * 4) Clear the persistence context
 * 5) Find ICompoundPKManyXManyEntityA(id=1), access ICompoundPKManyXManyEntityB(id=1) from
 * ICompoundPKManyXManyEntityA(id=1)'s ManyXMany relationship field.
 *
 * Test passes if the relationship properly references ICompoundPKManyXManyEntityB(id=1)
 */

    @Test
    public void jpa10_Relationships_ManyXMany_CompoundPK_Embeddable_001_Ano_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_CompoundPK_Embeddable_001_Ano_AMJTA_Web";
        final String testMethod = "testManyXManyCompoundPK001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityAName", EmbedIDMMEntityA.class);
        properties.put("EntityBName", EmbedIDMMEntityB.class);

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Relationships_ManyXMany_CompoundPK_Embeddable_001_XML_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_CompoundPK_Embeddable_001_XML_AMJTA_Web";
        final String testMethod = "testManyXManyCompoundPK001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityAName", XMLEmbedIDMMEntityA.class);
        properties.put("EntityBName", XMLEmbedIDMMEntityB.class);

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Relationships_ManyXMany_CompoundPK_Embeddable_001_Ano_AMRL_Web() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_CompoundPK_Embeddable_001_Ano_AMRL_Web";
        final String testMethod = "testManyXManyCompoundPK001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityAName", EmbedIDMMEntityA.class);
        properties.put("EntityBName", EmbedIDMMEntityB.class);

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Relationships_ManyXMany_CompoundPK_Embeddable_001_XML_AMRL_Web() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_CompoundPK_Embeddable_001_XML_AMRL_Web";
        final String testMethod = "testManyXManyCompoundPK001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityAName", XMLEmbedIDMMEntityA.class);
        properties.put("EntityBName", XMLEmbedIDMMEntityB.class);

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Relationships_ManyXMany_CompoundPK_Embeddable_001_Ano_CMTS_Web() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_CompoundPK_Embeddable_001_Ano_CMTS_Web";
        final String testMethod = "testManyXManyCompoundPK001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityAName", EmbedIDMMEntityA.class);
        properties.put("EntityBName", EmbedIDMMEntityB.class);

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Relationships_ManyXMany_CompoundPK_Embeddable_001_XML_CMTS_Web() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_CompoundPK_Embeddable_001_XML_CMTS_Web";
        final String testMethod = "testManyXManyCompoundPK001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityAName", XMLEmbedIDMMEntityA.class);
        properties.put("EntityBName", XMLEmbedIDMMEntityB.class);

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Relationships_ManyXMany_CompoundPK_IDClass_001_Ano_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_CompoundPK_IDClass_001_Ano_AMJTA_Web";
        final String testMethod = "testManyXManyCompoundPK001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityAName", IDClassMMEntityA.class);
        properties.put("EntityBName", IDClassMMEntityB.class);

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Relationships_ManyXMany_CompoundPK_IDClass_001_XML_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_CompoundPK_IDClass_001_XML_AMJTA_Web";
        final String testMethod = "testManyXManyCompoundPK001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityAName", XMLIDClassMMEntityA.class);
        properties.put("EntityBName", XMLIDClassMMEntityB.class);

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Relationships_ManyXMany_CompoundPK_IDClass_001_Ano_AMRL_Web() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_CompoundPK_IDClass_001_Ano_AMRL_Web";
        final String testMethod = "testManyXManyCompoundPK001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityAName", IDClassMMEntityA.class);
        properties.put("EntityBName", IDClassMMEntityB.class);

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Relationships_ManyXMany_CompoundPK_IDClass_001_XML_AMRL_Web() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_CompoundPK_IDClass_001_XML_AMRL_Web";
        final String testMethod = "testManyXManyCompoundPK001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityAName", XMLIDClassMMEntityA.class);
        properties.put("EntityBName", XMLIDClassMMEntityB.class);

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Relationships_ManyXMany_CompoundPK_IDClass_001_Ano_CMTS_Web() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_CompoundPK_IDClass_001_Ano_CMTS_Web";
        final String testMethod = "testManyXManyCompoundPK001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityAName", IDClassMMEntityA.class);
        properties.put("EntityBName", IDClassMMEntityB.class);

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Relationships_ManyXMany_CompoundPK_IDClass_001_XML_CMTS_Web() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_CompoundPK_IDClass_001_XML_CMTS_Web";
        final String testMethod = "testManyXManyCompoundPK001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityAName", XMLIDClassMMEntityA.class);
        properties.put("EntityBName", XMLIDClassMMEntityB.class);

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

}
