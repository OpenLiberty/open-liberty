/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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

package com.ibm.ws.jpa.fvt.derivedidentity.tests.ejb;

import javax.annotation.PostConstruct;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.jpa.fvt.derivedidentity.testlogic.DerivedIdentityTestLogic;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.vehicle.web.EJBDBTestVehicleServlet;

@WebServlet(urlPatterns = "/DerivedIdentityEJBSFEXTestServlet")
public class DerivedIdentityEJBSFEXTestServlet extends EJBDBTestVehicleServlet {
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

        executeTest(testName, testMethod, testResource);
        executeDDL("JPA20_DERIVEDIDENTITY_DEFAULT_DELETE_${dbvendor}.ddl");
    }

}
