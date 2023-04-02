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

package com.ibm.ws.ejbcontainer.security.jacc_fat;

import com.ibm.websphere.simplicity.log.Log;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

import jakarta.security.jacc.PolicyConfiguration;
import jakarta.security.jacc.PolicyConfigurationFactory;
import jakarta.security.jacc.EJBMethodPermission;

import componenttest.custom.junit.runner.FATRunner;


/**
 * Base class for EJBJakarta10. This class is extended for EJB in WAR testing.
 */
@Mode(TestMode.LITE)
@RunWith(FATRunner.class)
public class EJBJakarta10Test extends EJBJakarta10Base {

    protected static Class<?> logClass = EJBJakarta10Test.class;
    
    @Rule
    public TestName name = new TestName();

    @BeforeClass
    public static void setUp() throws Exception {

        commonSetup(logClass, Constants.SERVER_EJBJAR,
                    Constants.APPLICATION_SECURITY_EJB_JAR, Constants.SERVLET_SECURITY_EJBXML, Constants.CONTEXT_ROOT_SECURITY_EJB_JAR);

    }

    @Override
    protected TestName getName() {
        return name;
    }

}
