/*******************************************************************************
 * Copyright (c) 2018, 2026 IBM Corporation and others.
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
package com.ibm.ws.cdi.thirdparty.tests;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.custom.junit.runner.AlwaysPassesTest;

/**
 * Tests specific to cdi-1.2+ and its integration with third party libraries
 */
@RunWith(Suite.class)
@SuiteClasses({
                HibernateSearchTestJavax.class,
                HibernateSearchTestJakarta.class,
                ThirdPartyTests.class,
                HibernateCDICompatibilityTest.class,
                AlwaysPassesTest.class
})
public class FATSuite {

}
