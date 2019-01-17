/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.javaeesec.fat;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.custom.junit.runner.AlwaysPassesTest;

@RunWith(Suite.class)
@SuiteClasses({
                AlwaysPassesTest.class,
                CustomIdentityStoreHandlerTest.class,
                EJBModuleRealmTest.class,
                MultipleModuleDBRunAsTest.class,
                MultipleModuleGlobalClientCertFailOverTest.class,
                MultipleModuleGlobalClientCertTest.class,
                MultipleModuleGlobalLoginTest.class,
                MultipleModuleRunAsTest.class,
                NoIdentityStoreTest.class,
                SSOTest.class,
                ScopedTest.class,
                SecurityContextEJBTest.class,
                SecurityContextJaxRSTest.class,
                SecurityContextTest.class
})

/**
 * Purpose: This suite collects and runs all known good test suites.
 */
public class FATSuite {

}
