/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
@WebServlet(urlPatterns = "/TestMetadataCompleteEJBSFEXServlet")
public class TestMetadataCompleteEJBSFEXServlet extends EJBDBTestVehicleServlet {

    @PostConstruct
    private void initFAT() {
        testClassName = MetadataCompleteTestLogic.class.getName();
        ejbJNDIName = "ejb/MetadataCompleteSFExEJB";

        jpaPctxMap.put("test-jpa-resource-cmex",
                       new JPAPersistenceContext("test-jpa-resource-cmex", PersistenceContextType.CONTAINER_MANAGED_ES, PersistenceInjectionType.JNDI, "java:comp/env/jpa/MetadataComplete_CMEX"));
    }

    /*
     * Verify that annotations in the specified entity are ignored.
     */

    @Test
    @ExpectedFFDC("javax.transaction.RollbackException")
    public void jpa10_Packaging_MetadataComplete_Test001_CMEX_EJBSF() throws Exception {
        final String testName = "jpa10_Packaging_MetadataComplete_Test001_CMEX_EJBSF";
        final String testMethod = "executeMetadataCompleteTest001";
        final String testResource = "test-jpa-resource-cmex";

        executeDDL("JPA10_PACKAGING_DELETE_${dbvendor}.ddl");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityAName", "MDCEntity");
        executeTest(testName, testMethod, testResource, properties);
    }

}
