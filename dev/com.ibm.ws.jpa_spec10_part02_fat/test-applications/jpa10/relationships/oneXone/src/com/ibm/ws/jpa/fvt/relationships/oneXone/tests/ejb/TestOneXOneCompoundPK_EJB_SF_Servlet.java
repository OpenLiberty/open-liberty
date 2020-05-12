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

package com.ibm.ws.jpa.fvt.relationships.oneXone.tests.ejb;

import java.util.HashMap;

import javax.annotation.PostConstruct;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.jpa.fvt.relationships.oneXone.entities.complexpk.annotation.EmbedIDOOEntA;
import com.ibm.ws.jpa.fvt.relationships.oneXone.entities.complexpk.annotation.EmbedIDOOEntB;
import com.ibm.ws.jpa.fvt.relationships.oneXone.entities.complexpk.annotation.IDClassOOEntityA;
import com.ibm.ws.jpa.fvt.relationships.oneXone.entities.complexpk.annotation.IDClassOOEntityB;
import com.ibm.ws.jpa.fvt.relationships.oneXone.entities.complexpk.xml.XMLEmbedIDOOEntA;
import com.ibm.ws.jpa.fvt.relationships.oneXone.entities.complexpk.xml.XMLEmbedIDOOEntB;
import com.ibm.ws.jpa.fvt.relationships.oneXone.entities.complexpk.xml.XMLIDClassOOEntityA;
import com.ibm.ws.jpa.fvt.relationships.oneXone.entities.complexpk.xml.XMLIDClassOOEntityB;
import com.ibm.ws.jpa.fvt.relationships.oneXone.testlogic.OneXOneCompoundPKTestLogic;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.vehicle.web.EJBTestVehicleServlet;

@WebServlet(urlPatterns = "/TestOneXOneCompoundPK_EJB_SF_Servlet")
public class TestOneXOneCompoundPK_EJB_SF_Servlet extends EJBTestVehicleServlet {
    private static final long serialVersionUID = 1L;

    @PostConstruct
    private void initFAT() {
        testClassName = OneXOneCompoundPKTestLogic.class.getName();
        ejbJNDIName = "ejb/OneXOneSFEJB";

        jpaPctxMap.put("test-jpa-resource-amjta",
                       new JPAPersistenceContext("test-jpa-resource-amjta", PersistenceContextType.APPLICATION_MANAGED_JTA, PersistenceInjectionType.JNDI, "java:comp/env/jpa/OneXOne_CompoundPK_AMJTA"));
        jpaPctxMap.put("test-jpa-resource-amrl",
                       new JPAPersistenceContext("test-jpa-resource-amrl", PersistenceContextType.APPLICATION_MANAGED_RL, PersistenceInjectionType.JNDI, "java:comp/env/jpa/OneXOne_CompoundPK_AMRL"));
        jpaPctxMap.put("test-jpa-resource-cmts",
                       new JPAPersistenceContext("test-jpa-resource-cmts", PersistenceContextType.CONTAINER_MANAGED_TS, PersistenceInjectionType.JNDI, "java:comp/env/jpa/OneXOne_CompoundPK_CMTS"));
    }

    /*
     * Verify that the JPA provider can manage OneXOne relationships where the
     * entity on the inverse side of the relationship has a compound (Embeddable/ID Class) primary key.
     * Entities and IdClass are defined in annotation.
     *
     * Test Strategy:
     *
     * 1) Start a new transaction
     * 2) Create ICompoundPKOneXOneEntityA(id=1)
     * Create ICompoundPKOneXOneEntityB(id=1)
     * Set ICompoundPKOneXOneEntityA(id=1) to reference ICompoundPKOneXOneEntityB(id=1) in
     * a OneXOne relationship
     * 3) Commit the transaction
     * 4) Clear the persistence context
     * 5) Find ICompoundPKOneXOneEntityA(id=1), access ICompoundPKOneXOneEntityB(id=1) from
     * ICompoundPKOneXOneEntityA(id=1)'s OneXOne relationship field.
     *
     * Test passes if the relationship properly references ICompoundPKOneXOneEntityB(id=1)
     */

    @Test
    public void jpa10_Relationships_OneXOne_CompoundPK_Embeddable_001_Ano_AMJTA_SF_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_CompoundPK_Embeddable_001_Ano_AMJTA_SF_EJB";
        final String testMethod = "testOneXOneCompoundPK001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityAName", EmbedIDOOEntA.class);
        properties.put("EntityBName", EmbedIDOOEntB.class);

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Relationships_OneXOne_CompoundPK_Embeddable_001_XML_AMJTA_SF_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_CompoundPK_Embeddable_001_XML_AMJTA_SF_EJB";
        final String testMethod = "testOneXOneCompoundPK001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityAName", XMLEmbedIDOOEntA.class);
        properties.put("EntityBName", XMLEmbedIDOOEntB.class);

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Relationships_OneXOne_CompoundPK_Embeddable_001_Ano_AMRL_SF_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_CompoundPK_Embeddable_001_Ano_AMRL_SF_EJB";
        final String testMethod = "testOneXOneCompoundPK001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityAName", EmbedIDOOEntA.class);
        properties.put("EntityBName", EmbedIDOOEntB.class);

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Relationships_OneXOne_CompoundPK_Embeddable_001_XML_AMRL_SF_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_CompoundPK_Embeddable_001_XML_AMRL_SF_EJB";
        final String testMethod = "testOneXOneCompoundPK001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityAName", XMLEmbedIDOOEntA.class);
        properties.put("EntityBName", XMLEmbedIDOOEntB.class);

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Relationships_OneXOne_CompoundPK_Embeddable_001_Ano_CMTS_SF_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_CompoundPK_Embeddable_001_Ano_CMTS_SF_EJB";
        final String testMethod = "testOneXOneCompoundPK001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityAName", EmbedIDOOEntA.class);
        properties.put("EntityBName", EmbedIDOOEntB.class);

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Relationships_OneXOne_CompoundPK_Embeddable_001_XML_CMTS_SF_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_CompoundPK_Embeddable_001_XML_CMTS_SF_EJB";
        final String testMethod = "testOneXOneCompoundPK001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityAName", XMLEmbedIDOOEntA.class);
        properties.put("EntityBName", XMLEmbedIDOOEntB.class);

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Relationships_OneXOne_CompoundPK_IDClass_001_Ano_AMJTA_SF_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_CompoundPK_IDClass_001_Ano_AMJTA_SF_EJB";
        final String testMethod = "testOneXOneCompoundPK001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityAName", IDClassOOEntityA.class);
        properties.put("EntityBName", IDClassOOEntityB.class);

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Relationships_OneXOne_CompoundPK_IDClass_001_XML_AMJTA_SF_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_CompoundPK_IDClass_001_XML_AMJTA_SF_EJB";
        final String testMethod = "testOneXOneCompoundPK001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityAName", XMLIDClassOOEntityA.class);
        properties.put("EntityBName", XMLIDClassOOEntityB.class);

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Relationships_OneXOne_CompoundPK_IDClass_001_Ano_AMRL_SF_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_CompoundPK_IDClass_001_Ano_AMRL_SF_EJB";
        final String testMethod = "testOneXOneCompoundPK001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityAName", IDClassOOEntityA.class);
        properties.put("EntityBName", IDClassOOEntityB.class);

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Relationships_OneXOne_CompoundPK_IDClass_001_XML_AMRL_SF_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_CompoundPK_IDClass_001_XML_AMRL_SF_EJB";
        final String testMethod = "testOneXOneCompoundPK001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityAName", XMLIDClassOOEntityA.class);
        properties.put("EntityBName", XMLIDClassOOEntityB.class);

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Relationships_OneXOne_CompoundPK_IDClass_001_Ano_CMTS_SF_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_CompoundPK_IDClass_001_Ano_CMTS_SF_EJB";
        final String testMethod = "testOneXOneCompoundPK001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityAName", IDClassOOEntityA.class);
        properties.put("EntityBName", IDClassOOEntityB.class);

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Relationships_OneXOne_CompoundPK_IDClass_001_XML_CMTS_SF_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_CompoundPK_IDClass_001_XML_CMTS_SF_EJB";
        final String testMethod = "testOneXOneCompoundPK001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityAName", XMLIDClassOOEntityA.class);
        properties.put("EntityBName", XMLIDClassOOEntityB.class);

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }
}
