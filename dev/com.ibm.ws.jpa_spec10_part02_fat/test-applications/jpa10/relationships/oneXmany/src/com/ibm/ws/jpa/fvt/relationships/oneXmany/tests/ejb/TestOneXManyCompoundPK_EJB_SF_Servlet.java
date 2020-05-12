/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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

import com.ibm.ws.jpa.fvt.relationships.oneXmany.entities.compoundpk.annotated.EmbedIDOMEntityA;
import com.ibm.ws.jpa.fvt.relationships.oneXmany.entities.compoundpk.annotated.EmbedIDOMEntityB;
import com.ibm.ws.jpa.fvt.relationships.oneXmany.entities.compoundpk.annotated.IDClassOMEntityA;
import com.ibm.ws.jpa.fvt.relationships.oneXmany.entities.compoundpk.annotated.IDClassOMEntityB;
import com.ibm.ws.jpa.fvt.relationships.oneXmany.entities.compoundpk.xml.XMLEmbedIDOMEntityA;
import com.ibm.ws.jpa.fvt.relationships.oneXmany.entities.compoundpk.xml.XMLEmbedIDOMEntityB;
import com.ibm.ws.jpa.fvt.relationships.oneXmany.entities.compoundpk.xml.XMLIDClassOMEntityA;
import com.ibm.ws.jpa.fvt.relationships.oneXmany.entities.compoundpk.xml.XMLIDClassOMEntityB;
import com.ibm.ws.jpa.fvt.relationships.oneXmany.testlogic.OneXManyCompoundPKTestLogic;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.vehicle.web.EJBTestVehicleServlet;

@WebServlet(urlPatterns = "/TestOneXManyCompoundPK_EJB_SF_Servlet")
public class TestOneXManyCompoundPK_EJB_SF_Servlet extends EJBTestVehicleServlet {
    private static final long serialVersionUID = 1L;

    @PostConstruct
    private void initFAT() {
        testClassName = OneXManyCompoundPKTestLogic.class.getName();
        ejbJNDIName = "ejb/OneXManySFEJB";

        jpaPctxMap.put("test-jpa-resource-amjta",
                       new JPAPersistenceContext("test-jpa-resource-amjta", PersistenceContextType.APPLICATION_MANAGED_JTA, PersistenceInjectionType.JNDI, "java:comp/env/jpa/OneXMany_CompoundPK_AMJTA"));
        jpaPctxMap.put("test-jpa-resource-amrl",
                       new JPAPersistenceContext("test-jpa-resource-amrl", PersistenceContextType.APPLICATION_MANAGED_RL, PersistenceInjectionType.JNDI, "java:comp/env/jpa/OneXMany_CompoundPK_AMRL"));
        jpaPctxMap.put("test-jpa-resource-cmts",
                       new JPAPersistenceContext("test-jpa-resource-cmts", PersistenceContextType.CONTAINER_MANAGED_TS, PersistenceInjectionType.JNDI, "java:comp/env/jpa/OneXMany_CompoundPK_CMTS"));
    }

    /*
     * Verify that the JPA provider can manage OneXMany relationships where the
     * entity on the inverse side of the relationship has a compound (Embeddable/ID Class) primary key.
     * Entities and IdClass are defined in annotation.
     *
     * Test Strategy:
     *
     * 1) Start a new transaction
     * 2) Create ICompoundPKOneXManyEntityA(id=1)
     * Create ICompoundPKOneXManyEntityB(id=1)
     * Set ICompoundPKOneXManyEntityA(id=1) to reference ICompoundPKOneXManyEntityB(id=1) in
     * a OneXMany relationship
     * 3) Commit the transaction
     * 4) Clear the persistence context
     * 5) Find ICompoundPKOneXManyEntityA(id=1), access ICompoundPKOneXManyEntityB(id=1) from
     * ICompoundPKOneXManyEntityA(id=1)'s OneXMany relationship field.
     *
     * Test passes if the relationship properly references ICompoundPKOneXManyEntityB(id=1)
     */

    @Test
    public void jpa10_Relationships_OneXMany_CompoundPK_Embeddable_001_Ano_AMJTA_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_CompoundPK_Embeddable_001_Ano_AMJTA_EJB_SF";
        final String testMethod = "testOneXManyCompoundPK001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityAName", EmbedIDOMEntityA.class);
        properties.put("EntityBName", EmbedIDOMEntityB.class);

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Relationships_OneXMany_CompoundPK_Embeddable_001_XML_AMJTA_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_CompoundPK_Embeddable_001_XML_AMJTA_EJB_SF";
        final String testMethod = "testOneXManyCompoundPK001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityAName", XMLEmbedIDOMEntityA.class);
        properties.put("EntityBName", XMLEmbedIDOMEntityB.class);

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Relationships_OneXMany_CompoundPK_Embeddable_001_Ano_AMRL_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_CompoundPK_Embeddable_001_Ano_AMRL_EJB_SF";
        final String testMethod = "testOneXManyCompoundPK001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityAName", EmbedIDOMEntityA.class);
        properties.put("EntityBName", EmbedIDOMEntityB.class);

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Relationships_OneXMany_CompoundPK_Embeddable_001_XML_AMRL_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_CompoundPK_Embeddable_001_XML_AMRL_EJB_SF";
        final String testMethod = "testOneXManyCompoundPK001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityAName", XMLEmbedIDOMEntityA.class);
        properties.put("EntityBName", XMLEmbedIDOMEntityB.class);

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Relationships_OneXMany_CompoundPK_Embeddable_001_Ano_CMTS_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_CompoundPK_Embeddable_001_Ano_CMTS_EJB_SF";
        final String testMethod = "testOneXManyCompoundPK001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityAName", EmbedIDOMEntityA.class);
        properties.put("EntityBName", EmbedIDOMEntityB.class);

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Relationships_OneXMany_CompoundPK_Embeddable_001_XML_CMTS_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_CompoundPK_Embeddable_001_XML_CMTS_EJB_SF";
        final String testMethod = "testOneXManyCompoundPK001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityAName", XMLEmbedIDOMEntityA.class);
        properties.put("EntityBName", XMLEmbedIDOMEntityB.class);

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Relationships_OneXMany_CompoundPK_IDClass_001_Ano_AMJTA_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_CompoundPK_IDClass_001_Ano_AMJTA_EJB_SF";
        final String testMethod = "testOneXManyCompoundPK001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityAName", IDClassOMEntityA.class);
        properties.put("EntityBName", IDClassOMEntityB.class);

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Relationships_OneXMany_CompoundPK_IDClass_001_XML_AMJTA_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_CompoundPK_IDClass_001_XML_AMJTA_EJB_SF";
        final String testMethod = "testOneXManyCompoundPK001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityAName", XMLIDClassOMEntityA.class);
        properties.put("EntityBName", XMLIDClassOMEntityB.class);

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Relationships_OneXMany_CompoundPK_IDClass_001_Ano_AMRL_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_CompoundPK_IDClass_001_Ano_AMRL_EJB_SF";
        final String testMethod = "testOneXManyCompoundPK001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityAName", IDClassOMEntityA.class);
        properties.put("EntityBName", IDClassOMEntityB.class);

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Relationships_OneXMany_CompoundPK_IDClass_001_XML_AMRL_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_CompoundPK_IDClass_001_XML_AMRL_EJB_SF";
        final String testMethod = "testOneXManyCompoundPK001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityAName", XMLIDClassOMEntityA.class);
        properties.put("EntityBName", XMLIDClassOMEntityB.class);

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Relationships_OneXMany_CompoundPK_IDClass_001_Ano_CMTS_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_CompoundPK_IDClass_001_Ano_CMTS_EJB_SF";
        final String testMethod = "testOneXManyCompoundPK001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityAName", IDClassOMEntityA.class);
        properties.put("EntityBName", IDClassOMEntityB.class);

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Relationships_OneXMany_CompoundPK_IDClass_001_XML_CMTS_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_CompoundPK_IDClass_001_XML_CMTS_EJB_SF";
        final String testMethod = "testOneXManyCompoundPK001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityAName", XMLIDClassOMEntityA.class);
        properties.put("EntityBName", XMLIDClassOMEntityB.class);

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }
}
