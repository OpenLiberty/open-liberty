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

@WebServlet(urlPatterns = "/TestOneXOnePKJoin_EJB_SFEX_Servlet")
public class TestOneXOnePKJoin_EJB_SFEX_Servlet extends EJBTestVehicleServlet {
    private static final long serialVersionUID = 1L;

    @PostConstruct
    private void initFAT() {
        testClassName = OneXOnePKJoinTestLogic.class.getName();
        ejbJNDIName = "ejb/OneXOneSFExEJB";

        jpaPctxMap.put("test-jpa-resource-cmex",
                       new JPAPersistenceContext("test-jpa-resource-cmex", PersistenceContextType.CONTAINER_MANAGED_ES, PersistenceInjectionType.JNDI, "java:comp/env/jpa/OneXOne_PKJoinColumn_CMEX"));
    }

    @Test
    public void jpa10_Relationships_OneXOne_PKJoin_001_Ano_CMEX_SF_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_PKJoin_001_Ano_CMEX_SF_EJB";
        final String testMethod = "testPKJoin001";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityAName", PKJoinOOEntityA.class);
        properties.put("EntityBName", PKJoinOOEntityB.class);

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Relationships_OneXOne_PKJoin_001_XML_CMEX_SF_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_PKJoin_001_XML_CMEX_SF_EJB";
        final String testMethod = "testPKJoin001";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityAName", XMLPKJoinOOEnA.class);
        properties.put("EntityBName", XMLPKJoinOOEnB.class);

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }
}
