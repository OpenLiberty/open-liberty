/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2016, 2022
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */

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
