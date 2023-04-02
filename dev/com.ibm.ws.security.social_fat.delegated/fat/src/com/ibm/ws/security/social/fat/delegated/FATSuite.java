/*******************************************************************************
 * Copyright (c) 2017, 2022 IBM Corporation and others.
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

package com.ibm.ws.security.social.fat.delegated;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.custom.junit.runner.AlwaysPassesTest;

@RunWith(Suite.class)
@SuiteClasses({
        AlwaysPassesTest.class,
        OidcDelegatedSocialLoginWithLibertyOPTests.class,
        OidcDelegatedSocialLoginWithProvidersTests.class,
        OidcDelegatedSocialLoginWithSelectionPageTests.class,
})

/**
 * Purpose: This suite collects and runs all known good test suites.
 */
public class FATSuite {

}
