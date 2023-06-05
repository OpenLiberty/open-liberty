/*******************************************************************************
 * Copyright (c) 2016, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.openidconnect.server.fat.jaxrs.config;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.security.fat.common.utils.ldaputils.CommonAltRemoteLDAPServerSuite;

import componenttest.custom.junit.runner.AlwaysPassesTest;

@SuppressWarnings("restriction")
@RunWith(Suite.class)
@SuiteClasses({

        // All tests are run from the different jaxrs.config version FAT projects - this project is just the source of all of those tests
        AlwaysPassesTest.class

})
/**
 * Purpose: This suite collects and runs all known good test suites.
 */
public class FATSuite extends CommonAltRemoteLDAPServerSuite {

}
