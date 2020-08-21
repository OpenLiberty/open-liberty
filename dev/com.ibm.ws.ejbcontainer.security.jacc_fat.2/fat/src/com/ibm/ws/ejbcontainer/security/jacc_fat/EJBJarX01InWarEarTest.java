/*******************************************************************************
 * Copyright (c) 2012, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
 * This test variation covers packaging of EJB in WAR with all packaged in an EAR file such
 * that the servlet classes are in WEB-INF/classes and EJB jar files are in WEB-INF/lib.
 *
 * Performs testing of EJB with only the ejb-jar.xml deployment descriptor and no annotations.
 *
 * The ejb-jar.xml (version 3.0) for this test specifies the following
 * 1) Method permitAll is unchecked and allows access to all
 * 2) Method denyAll is on exclude-list and denys access to all
 * 3) Method manager is protected by method-permission Manager
 * 4) Method employee is protected by method-permission Employee
 * 5) Method employeeAndManager is used to test overrides in permissions for various
 * method signatures which are specified using the <method-param> element in ejb-jar.
 * a) The method employeeAndManagerwithParams is listed as both unchecked and with a method-permission to verify
 * that unchecked overrides.
 * b) The method employeeAndManagerwithInt is listed on the exclude-list and with a method-permission to verify that
 * exclude-list overrides.
 *
 * This test invokes SecurityEJBX01Bean methods with a variety of method signatures to insure that
 * annotations are processed correctly with methods of the same name and different signature.
 */
@Mode(TestMode.LITE)
@RunWith(FATRunner.class)
public class EJBJarX01InWarEarTest extends EJBJarX01Base {

    protected static Class<?> logClass = EJBJarX01InWarEarTest.class;

    @Rule
    public TestName name = new TestName();

    @BeforeClass
    public static void setUp() throws Exception {

        Log.info(logClass, "setUp", "Starting the server....");
        commonSetup(logClass, Constants.SERVER_EJBJAR_INWAR,
                    Constants.APPLICATION_SECURITY_EJB_INWAR_EAR_X01,
                    Constants.SERVLET_SECURITY_EJBXML, Constants.CONTEXT_ROOT_SECURITY_EJB_INWAR_EAR_X01);

    }

    @Override
    protected TestName getName() {
        return name;
    }

}