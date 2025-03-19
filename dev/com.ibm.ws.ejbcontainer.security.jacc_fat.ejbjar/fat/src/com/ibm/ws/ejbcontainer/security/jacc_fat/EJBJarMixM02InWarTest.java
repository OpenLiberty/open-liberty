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

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * This test variation covers packaging of EJB in standalone WAR file with EJB and servlet
 * classes in WEB-INF/classes. There is no application.xml or ibm-application-bnd.xml file.
 *
 * Performs testing of EJB with the ejb-jar.xml deployment descriptor mixed with class and method level annotations.
 *
 * The annotations specified at class level are @PermitAll and @RunAs(Employee) with a mix of overriding annotations at
 * method level and in ejb-jar.xml as described below.
 *
 * The ejb-jar.xml (version 3.0) for this test specifies a variety permissions to cover the following:
 * 1) DenyAll method annotation with no ejb-jar specification overrides class level @PermitAll
 * 2) DenyAll method annotation with ejb-jar method-permission Manager, results in only Manager role allowed access.
 * 3) No method annotation and no ejb-jar specification results in PermitAll.
 * 4) No method annotation and ejb-jar exclude-list results in exclude-list overriding.
 * 5) DenyAll method annotation with ejb-jar unchecked results in unchecked overriding to permit access.
 * 6) RolesAllowed(Employee) method annotation with no ejb-jar specification results in only Employee allowed access.
 * 7) RolesAllowed(Employee) method annotation with no ejb-jar Manager permission results in only Manager allowed access.
 * 8) RolesAllowed(Employee,Manager) with ejb-jar unchecked results in unchecked to permit all access.
 * 9) RolesAllowed(Employee,Manager) with ejb-jar exclude-list results in exclude-list denying all access.
 * 10) RolesAllowed(Employee,Manager) with ejb-jar Employee results in only Employee role access.
 * 11) RolesAllowed(Employee,Manager) with ejb-jar Employee results in only Employee role access.
 * 12) Class level RunAs(Employee) is overridden by ejb-jar use-caller-identity.
 *
 * This test invokes Stateless SecurityEJBM02Bean methods with a variety of method signatures to insure that
 * annotations are processed correctly with methods of the same name and different signature.
 */
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class EJBJarMixM02InWarTest extends EJBJarMixM02Base {

    protected static Class<?> logClass = EJBJarMixM02InWarTest.class;

    @Rule
    public TestName name = new TestName();

    @BeforeClass
    public static void setUp() throws Exception {

        commonSetup(logClass, Constants.SERVER_EJBJAR_INWAR,
                    Constants.APPLICATION_SECURITY_EJB_JAR_INSTANDALONE_M02,
                    Constants.SERVLET_SECURITY_EJBXML, Constants.CONTEXT_ROOT_SECURITY_EJB_JAR_INSTANDALONE_M02);

    }

    @Override
    protected TestName getName() {
        return name;
    }

}