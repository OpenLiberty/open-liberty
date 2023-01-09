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

package com.ibm.ws.jpa.fvt.packaging.xmlmappingfile.metadatacomplete.tests.ejb;

import java.util.HashMap;

import javax.annotation.PostConstruct;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.jpa.fvt.packaging.xmlmappingfile.metadatacomplete.testlogic.MetadataCompleteTestLogic;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.vehicle.web.EJBDBTestVehicleServlet;

import componenttest.annotation.ExpectedFFDC;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/TestMetadataCompleteEJBSFServlet")
public class TestMetadataCompleteEJBSFServlet extends EJBDBTestVehicleServlet {

    @PostConstruct
    private void initFAT() {
        testClassName = MetadataCompleteTestLogic.class.getName();
        ejbJNDIName = "ejb/MetadataCompleteSFEJB";

        jpaPctxMap.put("test-jpa-resource-amjta",
                       new JPAPersistenceContext("test-jpa-resource-amjta", PersistenceContextType.APPLICATION_MANAGED_JTA, PersistenceInjectionType.JNDI, "java:comp/env/jpa/MetadataComplete_AMJTA"));
        jpaPctxMap.put("test-jpa-resource-amrl",
                       new JPAPersistenceContext("test-jpa-resource-amrl", PersistenceContextType.APPLICATION_MANAGED_RL, PersistenceInjectionType.JNDI, "java:comp/env/jpa/MetadataComplete_AMRL"));
        jpaPctxMap.put("test-jpa-resource-cmts",
                       new JPAPersistenceContext("test-jpa-resource-cmts", PersistenceContextType.CONTAINER_MANAGED_TS, PersistenceInjectionType.JNDI, "java:comp/env/jpa/MetadataComplete_CMTS"));
    }

    /*
     * Verify that annotations in the specified entity are ignored.
     */

    @Test
    @ExpectedFFDC("javax.transaction.RollbackException")
    public void jpa10_Packaging_MetadataComplete_Test001_AMJTA_EJBSF() throws Exception {
        final String testName = "jpa10_Packaging_MetadataComplete_Test001_AMJTA_EJBSF";
        final String testMethod = "executeMetadataCompleteTest001";
        final String testResource = "test-jpa-resource-amjta";

        executeDDL("JPA10_PACKAGING_DELETE_${dbvendor}.ddl");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityAName", "MDCEntity");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Packaging_MetadataComplete_Test001_AMRL_EJBSF() throws Exception {
        final String testName = "jpa10_Packaging_MetadataComplete_Test001_AMRL_EJBSF";
        final String testMethod = "executeMetadataCompleteTest001";
        final String testResource = "test-jpa-resource-amrl";

        executeDDL("JPA10_PACKAGING_DELETE_${dbvendor}.ddl");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityAName", "MDCEntity");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    @ExpectedFFDC("javax.transaction.RollbackException")
    public void jpa10_Packaging_MetadataComplete_Test001_CMTS_EJBSF() throws Exception {
        final String testName = "jpa10_Packaging_MetadataComplete_Test001_CMTS_EJBSF";
        final String testMethod = "executeMetadataCompleteTest001";
        final String testResource = "test-jpa-resource-cmts";

        executeDDL("JPA10_PACKAGING_DELETE_${dbvendor}.ddl");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityAName", "MDCEntity");
        executeTest(testName, testMethod, testResource, properties);
    }

}
