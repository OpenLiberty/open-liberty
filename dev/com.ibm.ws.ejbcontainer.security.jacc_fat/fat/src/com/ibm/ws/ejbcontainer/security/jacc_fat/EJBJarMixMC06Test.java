/*******************************************************************************
 * Copyright (c) 2012, 2020 IBM Corporation and others.
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

package com.ibm.ws.ejbcontainer.security.jacc_fat;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * Performs testing where metadata-complete true is specified in ejb-jar.xml such that all annotations
 * in the EJB are ignored and only ejb-jar.xml is used to enforce security constraints.
 *
 * Performs testing of EJB with the ejb-jar.xml deployment descriptor mixed with annotations which are ignored.
 * The annotations which are ignored since metadata-complete is true are as follows:
 * At class level is @DenyAll annotation with a mix of annotations at
 * method level. Since metadata-complete is specified in ejb-jar.xml, all annotations are ignored.
 *
 * The ejb-jar.xml (version 3.0) for this test specifies a variety permissions to cover the following:
 *
 * 1) denyAll() - PermitAll method annotation with ejb-jar method-permission Employee, results in only Employee role allowed access since metadata-complete.
 * 2) denyAllwithParam() - PermitAll method annotation with ejb-jar exclude-list results in exclude-list since metadata-complete.
 * 3) permitAll() -PermitAll method annotation with no ejb-jar specification results in all permitted access because annotations are ignored.
 * 4) permitAllwithParam() - no method annotation with no ejb-jar specification results in all permitted access because annotations are ignored.
 * 5) manager() - RolesAllowed(Manager) method annotation with method-permission Employee in ejb-jar specification permits only Employee since metadata-complete.
 * 6) managerwithParam() - RolesAllowed(Manager) method annotation with method-permission Employee,Manager in ejb-jar permits both Employee, Manager since metadata-complete.
 * 7) employee() - no method annotation with Employee method-permission in ejb-jar permits only Employee.
 * 8) employeewithParam() - no method annotation with unchecked in ejb-jar permits any valid user.
 * 9) employeeAndManager() - RolesAllowed(Employee,Manager) annotation with DeclaredRole01 method permission in ejb-jar allows only DeclaredRole user.
 * 10) employeeAndManagerwithParam() - RolesAllowed(Employee,Manager) annotation with permission,unchecked and exclude-list in ejb-jar denies access to all.
 * 11) employeeAndManagerwithInt() - RolesAllowed(Employee,Manager) annotation with no ejb-jar allows all access since metadata-complete ignores annotations.
 * 12) employeeAndManagerwithIParams() - RolesAllowed(Employee,Manager) annotation with exclude-list in ejb-jar denies access to all.
 *
 * This test invokes Stateless SecurityEJBM06Bean methods with a variety of method signatures.
 */
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class EJBJarMixMC06Test extends EJBJarMixMC06Base {

    protected static Class<?> logClass = EJBJarMixMC06Test.class;

    @Rule
    public TestName name = new TestName();

    @BeforeClass
    public static void setUp() throws Exception {

        Log.info(logClass, "setUp", "Starting the server....");
        commonSetup(logClass, Constants.SERVER_EJBJAR_MC,
                    Constants.APPLICATION_SECURITY_EJB_JAR_MC, Constants.SERVLET_SECURITY_EJBMC, Constants.CONTEXT_ROOT_SECURITY_EJB_JAR_MC);

    }

    @Override
    protected TestName getName() {
        return name;
    }

}