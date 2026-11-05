/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2017
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
package com.ibm.ws.security.audit.fat.example;

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
	AuditExampleTest.class
})
/**
 * Purpose: This suite collects and runs all known good test suites.
 */

public class FATSuite {
//	public class FATSuite extends CommonLocalLDAPServerSuite {

}
