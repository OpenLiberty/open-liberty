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

package com.ibm.ws.jpa.fvt.derivedidentity.tests.ejb;

import javax.annotation.PostConstruct;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.jpa.fvt.derivedidentity.testlogic.DerivedIdentityTestLogic;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.vehicle.web.EJBTestVehicleServlet;

@WebServlet(urlPatterns = "/DerivedIdentityEJBSFEXTestServlet")
public class DerivedIdentityEJBSFEXTestServlet extends EJBTestVehicleServlet {
    @PostConstruct
    private void initFAT() {
        testClassName = DerivedIdentityTestLogic.class.getName();
        ejbJNDIName = "ejb/DerivedIdentityTestSFEXEJB";

        jpaPctxMap.put("test-jpa-resource-cmex",
                       new JPAPersistenceContext("test-jpa-resource-cmex", PersistenceContextType.CONTAINER_MANAGED_ES, PersistenceInjectionType.JNDI, "java:comp/env/jpa/DerivedIdentity_CMES"));
    }

    @Test
    public void jpa20_derivedidentity_001_CMEX_EJB_SF() throws Exception {
        final String testName = "jpa20_derivedidentity_001_CMEX_EJB_SF";
        final String testMethod = "testScenario01";
        final String testResource = "test-jpa-resource-cmex";

        executeDDL("JPA20_DERIVEDIDENTITY_DELETE_${dbvendor}.ddl");

        executeTest(testName, testMethod, testResource);
    }

}
