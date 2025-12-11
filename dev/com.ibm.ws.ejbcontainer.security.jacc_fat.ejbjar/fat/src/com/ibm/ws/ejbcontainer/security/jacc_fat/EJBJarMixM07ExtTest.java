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
 * Performs testing of EJB with the extensions file ibm-ejb-jar-ext.xml along with
 * ejb-jar.xml deployment descriptor mixed with annotations.
 *
 * The annotations contain @RunAs(Employee), @PermitAll and @DeclareRoles(DeclaredRole01,
 * ejb-jar.xml contains run-as use-caller-identity and ibm-ejb-jar-ext.xml contains method
 * level run-as settings which should override the ejb-jar use-caller-identity which in turn overrides annotations.
 *
 * Permissions are set up to test that ibm-ejb-jar-ext.xml extensions
 * for run-as will override those in ejb-jar.xml when there are conflicts in ejb-jar.xml.
 * The run-as settings in ibm-ejb-jar-ext.xml take effect at the specified
 * method level such that the methods listed in the extensions file
 * will override the run-as use-caller-identity in ejb-jar.xml.
 *
 * The ibm-ejb-jar-ext.xml tests method level run-as settings for CALLER_IDENTITY, SPECIFIED_IDENTITY,
 * and SYSTEM_IDENTITY (not supported).
 *
 * The test also verifies that security constraints in annotations and ejb-jar.xml are still enforced when
 * an extensions file is present.
 *
 * This test invokes Stateless SecurityEJBM07Bean methods with a variety of method signatures to insure that
 * ibm-ejb-jar-ext.xml method level run-as settings are processed correctly with methods of the same name and different signature.
 * The SecurityEJBM07Bean invokes the SecurityEJBRunAsExtBean methods from within its methods based on the run-as user specified.
 */
@Mode(TestMode.LITE)
@RunWith(FATRunner.class)
public class EJBJarMixM07ExtTest extends EJBJarMixM07ExtBase {

    protected static Class<?> logClass = EJBJarMixM07ExtTest.class;

    @Rule
    public TestName name = new TestName();

    @BeforeClass
    public static void setUp() throws Exception {

        Log.info(logClass, "setUp", "Starting the server....");
        commonSetup(logClass, Constants.SERVER_EJBJAR,
                    Constants.APPLICATION_SECURITY_EJB_JAR, Constants.SERVLET_SECURITY_EJBXML, Constants.CONTEXT_ROOT_SECURITY_EJB_JAR);

    }

    @Override
    protected TestName getName() {
        return name;
    }

}