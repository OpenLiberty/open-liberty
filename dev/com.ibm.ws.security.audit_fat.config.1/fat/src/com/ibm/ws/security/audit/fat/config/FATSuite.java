/*******************************************************************************
 * Copyright (c) 2016, 2026 IBM Corporation and others.
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
package com.ibm.ws.security.audit.fat.config;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.custom.junit.runner.AlwaysPassesTest;


@RunWith(Suite.class)
/**
*
**/
@SuiteClasses({
	AlwaysPassesTest.class,
	SecureAuditLoggingTest.class
	/*
	 * This testsuite is closed, do not add new tests or it runs too long for automated
	 * testing. Add new tests to the next com.ibm.ws.security.audit_fat.config project
	 */

})
/**
 * Purpose: This suite collects and runs all known good test suites.
 */

public class FATSuite {
//	public class FATSuite extends CommonLocalLDAPServerSuite {

}
