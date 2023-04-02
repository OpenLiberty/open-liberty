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
 * This test variation covers packaging of EJB in WAR with all packaged in an EAR file such
 * that the servlet classes are in WEB-INF/classes and EJB jar files are in WEB-INF/lib.
 *
 * Performs testing of EJB with the ejb-jar.xml deployment descriptor mixed with annotations at class and method level.
 *
 * The annotations specified at class level are @DeclareRoles and @RunAs(Employee) with method level
 * annotations and ejb-jar entries as described below.
 *
 * The ejb-jar.xml (version 3.0) for this test specifies a variety permissions to cover the following:
 * 1) DenyAll annotation method level with no ejb-jar specification results in DenyAll taking effect.
 * 2) DenyAll annotation method level with exclude-list in ejb-jar still results in excluding the method.
 * 3) DenyAll annotation method level with unchecked in ejb-jar results in unchecked overriding and allowing access.
 * 4) DenyAll annotation method level with Manager role method-permission results in Manager required.
 * 5) DenyAll annotation method level with Employee role method-permission results in Employee required.
 * 6) DenyAll annotation method level with Employee and Manager role method-permission results in Employee or Manager role required.
 * 7) DenyAll annotation method level with Employee and Manager role method-permission + unchecked + exclude-list results in exclude-list overriding.
 * 8) DeclaredRole annotation at class level and no ejb-jar entry results in user in role allowed access.
 * 9) RunAs annotation at class level takes effect when no ejb-jar run-as element.
 *
 *
 * This test invokes Stateless SecurityEJBM01Bean methods with a variety of method signatures to insure that
 * annotations are processed correctly with methods of the same name and different signature.
 */
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class EJBJarMixM01InWarEarTest extends EJBJarMixM01Base {

    protected static Class<?> logClass = EJBJarMixM01InWarEarTest.class;

    @Rule
    public TestName name = new TestName();

    @BeforeClass
    public static void setUp() throws Exception {

        commonSetup(logClass, Constants.SERVER_EJBJAR_INWAR,
                    Constants.APPLICATION_SECURITY_EJB_JAR_INWAR_EAR_M01,
                    Constants.SERVLET_SECURITY_EJBXML, Constants.CONTEXT_ROOT_SECURITY_EJB_JAR_INWAR_EAR_M01);

    }

    @Override
    protected TestName getName() {
        return name;
    }

    @Override
    String getEJBString() {
        return "ejbm01w";
    }

}