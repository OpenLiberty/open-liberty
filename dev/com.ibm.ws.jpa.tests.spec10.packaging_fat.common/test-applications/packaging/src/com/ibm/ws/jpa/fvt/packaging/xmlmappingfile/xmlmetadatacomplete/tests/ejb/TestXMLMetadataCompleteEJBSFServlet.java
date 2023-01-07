/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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

package com.ibm.ws.jpa.fvt.packaging.xmlmappingfile.xmlmetadatacomplete.tests.ejb;

import java.util.HashMap;

import javax.annotation.PostConstruct;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.jpa.fvt.packaging.xmlmappingfile.xmlmetadatacomplete.testlogic.XMLMetadataCompleteTestLogic;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.vehicle.web.EJBDBTestVehicleServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/TestXMLMetadataCompleteEJBSFServlet")
public class TestXMLMetadataCompleteEJBSFServlet extends EJBDBTestVehicleServlet {
    @PostConstruct
    private void initFAT() {
        testClassName = XMLMetadataCompleteTestLogic.class.getName();
        ejbJNDIName = "ejb/XMLMetadataCompleteSFEJB";

        jpaPctxMap.put("test-jpa-resource-amjta",
                       new JPAPersistenceContext("test-jpa-resource-amjta", PersistenceContextType.APPLICATION_MANAGED_JTA, PersistenceInjectionType.JNDI, "java:comp/env/jpa/XMLMetadataComplete_AMJTA"));
        jpaPctxMap.put("test-jpa-resource-amrl",
                       new JPAPersistenceContext("test-jpa-resource-amrl", PersistenceContextType.APPLICATION_MANAGED_RL, PersistenceInjectionType.JNDI, "java:comp/env/jpa/XMLMetadataComplete_AMRL"));
        jpaPctxMap.put("test-jpa-resource-cmts",
                       new JPAPersistenceContext("test-jpa-resource-cmts", PersistenceContextType.CONTAINER_MANAGED_TS, PersistenceInjectionType.JNDI, "java:comp/env/jpa/XMLMetadataComplete_CMTS"));
    }

    /*
     * Verify that annotation in an entity is ignored by the persistence provider with entities that are also
     * defined in the XML Mapping File.
     */

    @Test
    public void jpa10_Packaging_XMLMetadataComplete_Test001_AMJTA_EJBSF() throws Exception {
        final String testName = "jpa10_Packaging_XMLMetadataComplete_Test001_AMJTA_EJBSF";
        final String testMethod = "executeXMLMetadataCompleteTest001";
        final String testResource = "test-jpa-resource-amjta";

        executeDDL("JPA10_PACKAGING_DELETE_${dbvendor}.ddl");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityAName", "XMLCompleteTestEntity");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Packaging_XMLMetadataComplete_Test001_AMRL_EJBSF() throws Exception {
        final String testName = "jpa10_Packaging_XMLMetadataComplete_Test001_AMRL_EJBSF";
        final String testMethod = "executeXMLMetadataCompleteTest001";
        final String testResource = "test-jpa-resource-amrl";

        executeDDL("JPA10_PACKAGING_DELETE_${dbvendor}.ddl");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityAName", "XMLCompleteTestEntity");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Packaging_XMLMetadataComplete_Test001_CMTS_EJBSF() throws Exception {
        final String testName = "jpa10_Packaging_XMLMetadataComplete_Test001_CMTS_EJBSF";
        final String testMethod = "executeXMLMetadataCompleteTest001";
        final String testResource = "test-jpa-resource-cmts";

        executeDDL("JPA10_PACKAGING_DELETE_${dbvendor}.ddl");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityAName", "XMLCompleteTestEntity");
        executeTest(testName, testMethod, testResource, properties);
    }

}
