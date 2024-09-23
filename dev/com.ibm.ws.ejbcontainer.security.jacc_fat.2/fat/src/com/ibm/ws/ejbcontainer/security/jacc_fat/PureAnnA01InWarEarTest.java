/*******************************************************************************
 * Copyright (c) 2011, 2024 IBM Corporation and others.
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
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.CheckpointTest;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.RepeatTests;

/**
 * This test variation covers packaging of EJB in WAR with all packaged in an EAR file such
 * that the servlet classes are in WEB-INF/classes and EJB jar files are in WEB-INF/lib.
 *
 * Performs testing of EJB pure annotations with a Stateless bean and application-bnd role mappings in server.xml.
 * This tests the class level annotations RunAs (Employee), RolesAllowed (Manager) and DeclareRoles(DeclaredRole01) with a
 * variety of method level annotations. This test invokes SecurityEJBA01Bean methods with a variety of method signatures to insure that
 * annotations are processed correctly with methods of the same name and different signature.
 *
 * This test covers the application-bnd role mappings in server.xml while PureAnnAppBndXMLBindingsTest covers
 * application-bnd mappings in ibm-application-bnd.xml.
 *
 * This test covers the positive RunAs testing while PureAnnA05Test covers the negative scenarios.
 */
@RunWith(FATRunner.class)
@CheckpointTest(alwaysRun = true)
public class PureAnnA01InWarEarTest extends PureAnnA01Base {

    protected static Class<?> logClass = PureAnnA01InWarEarTest.class;

    @Rule
    public TestName name = new TestName();

    @ClassRule
    public static RepeatTests r = FATSuite.defaultAndCheckpointRepeat(Constants.SERVER_EJB);

    @BeforeClass
    public static void setUp() throws Exception {

        Log.info(logClass, "setUp", "Starting the server....");
        commonSetup(logClass, Constants.SERVER_EJB,
                    Constants.APPLICATION_SECURITY_EJB_IN_WAR, Constants.SERVLET_SECURITY_EJB, Constants.CONTEXT_ROOT_SECURITY_EJB_IN_WAR);

    }

    @Override
    protected TestName getName() {
        return name;
    }

}