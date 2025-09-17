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
 * This test variation covers packaging of EJB in standalone WAR file with EJB and servlet
 * classes in WEB-INF/classes. There is no application.xml or ibm-application-bnd.xml file.
 *
 * Performs testing of EJB with only the ejb-jar.xml deployment descriptor and no annotations.
 *
 * The ejb-jar.xml (version 3.1) for this test specifies the following
 * 1) <role-link> of Employee role to Emp for use in isCallerInRole.
 * 2) <role-link> of Manager role to Mgr for use in isCallerInRole
 * 3) Method permitAll is unchecked
 * 4) Method denyAll is on exclude-list
 * 5) Method manager is protected by method-permission Manager
 * 6) Method employee is protected by method-permission Employee
 * 7) Method employeeAndManager is used to test overrides in permissions for various
 * method signatures.
 *
 * This test invokes SecurityEJBX02Bean methods with a variety of method signatures to insure that
 * annotations are processed correctly with methods of the same name and different signature.
 */
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class EJBJarX02InWarTest extends EJBJarX02Base {

    protected static Class<?> logClass = EJBJarX02InWarTest.class;

    @Rule
    public TestName name = new TestName();

    @BeforeClass
    public static void setUp() throws Exception {

        Log.info(logClass, "setUp", "Starting the server....");
        commonSetup(logClass, Constants.SERVER_EJBJAR_INWAR,
                    Constants.APPLICATION_SECURITY_EJB_JAR_INSTANDALONE_X02,
                    Constants.SERVLET_SECURITY_EJBXML, Constants.CONTEXT_ROOT_SECURITY_EJB_JAR_INSTANDALONE_X02);

    }

    @Override
    protected TestName getName() {
        return name;
    }

    @Override
    String getEJBString() {
        return "ejbx02w";
    }

}