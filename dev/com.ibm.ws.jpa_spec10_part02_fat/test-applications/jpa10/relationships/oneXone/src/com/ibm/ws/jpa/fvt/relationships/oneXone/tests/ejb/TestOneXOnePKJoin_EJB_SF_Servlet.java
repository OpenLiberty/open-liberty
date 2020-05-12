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

import com.ibm.ws.jpa.fvt.relationships.oneXone.entities.pkjoincolumn.annotation.PKJoinOOEntityA;
import com.ibm.ws.jpa.fvt.relationships.oneXone.entities.pkjoincolumn.annotation.PKJoinOOEntityB;
import com.ibm.ws.jpa.fvt.relationships.oneXone.entities.pkjoincolumn.xml.XMLPKJoinOOEnA;
import com.ibm.ws.jpa.fvt.relationships.oneXone.entities.pkjoincolumn.xml.XMLPKJoinOOEnB;
import com.ibm.ws.jpa.fvt.relationships.oneXone.testlogic.OneXOnePKJoinTestLogic;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.vehicle.web.EJBTestVehicleServlet;

@WebServlet(urlPatterns = "/TestOneXOnePKJoin_EJB_SF_Servlet")
public class TestOneXOnePKJoin_EJB_SF_Servlet extends EJBTestVehicleServlet {
    private static final long serialVersionUID = 1L;

    @PostConstruct
    private void initFAT() {
        testClassName = OneXOnePKJoinTestLogic.class.getName();
        ejbJNDIName = "ejb/OneXOneSFEJB";

        jpaPctxMap.put("test-jpa-resource-amjta",
                       new JPAPersistenceContext("test-jpa-resource-amjta", PersistenceContextType.APPLICATION_MANAGED_JTA, PersistenceInjectionType.JNDI, "java:comp/env/jpa/OneXOne_PKJoinColumn_AMJTA"));
        jpaPctxMap.put("test-jpa-resource-amrl",
                       new JPAPersistenceContext("test-jpa-resource-amrl", PersistenceContextType.APPLICATION_MANAGED_RL, PersistenceInjectionType.JNDI, "java:comp/env/jpa/OneXOne_PKJoinColumn_AMRL"));
        jpaPctxMap.put("test-jpa-resource-cmts",
                       new JPAPersistenceContext("test-jpa-resource-cmts", PersistenceContextType.CONTAINER_MANAGED_TS, PersistenceInjectionType.JNDI, "java:comp/env/jpa/OneXOne_PKJoinColumn_CMTS"));
    }

    @Test
    public void jpa10_Relationships_OneXOne_PKJoin_001_Ano_AMJTA_SF_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_PKJoin_001_Ano_AMJTA_SF_EJB";
        final String testMethod = "testPKJoin001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityAName", PKJoinOOEntityA.class);
        properties.put("EntityBName", PKJoinOOEntityB.class);

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Relationships_OneXOne_PKJoin_001_XML_AMJTA_SF_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_PKJoin_001_XML_AMJTA_SF_EJB";
        final String testMethod = "testPKJoin001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityAName", XMLPKJoinOOEnA.class);
        properties.put("EntityBName", XMLPKJoinOOEnB.class);

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Relationships_OneXOne_PKJoin_001_Ano_AMRL_SF_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_PKJoin_001_Ano_AMRL_SF_EJB";
        final String testMethod = "testPKJoin001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityAName", PKJoinOOEntityA.class);
        properties.put("EntityBName", PKJoinOOEntityB.class);

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Relationships_OneXOne_PKJoin_001_XML_AMRL_SF_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_PKJoin_001_XML_AMRL_SF_EJB";
        final String testMethod = "testPKJoin001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityAName", XMLPKJoinOOEnA.class);
        properties.put("EntityBName", XMLPKJoinOOEnB.class);

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Relationships_OneXOne_PKJoin_001_Ano_CMTS_SF_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_PKJoin_001_Ano_CMTS_SF_EJB";
        final String testMethod = "testPKJoin001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityAName", PKJoinOOEntityA.class);
        properties.put("EntityBName", PKJoinOOEntityB.class);

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Relationships_OneXOne_PKJoin_001_XML_CMTS_SF_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_PKJoin_001_XML_CMTS_SF_EJB";
        final String testMethod = "testPKJoin001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityAName", XMLPKJoinOOEnA.class);
        properties.put("EntityBName", XMLPKJoinOOEnB.class);

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }
}
