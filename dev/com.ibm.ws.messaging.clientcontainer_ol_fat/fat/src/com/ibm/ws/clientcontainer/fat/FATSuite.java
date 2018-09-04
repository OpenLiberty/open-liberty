/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2014
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
package com.ibm.ws.clientcontainer.fat;


import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import componenttest.custom.junit.runner.AlwaysPassesTest;
@RunWith(Suite.class)
@SuiteClasses({
	AlwaysPassesTest.class,
	AsyncSendSAPITest.class,
	AsyncSendCAPITest.class,
	MessageListenerTest.class,
	ClientIDTest.class
})
/**
 * Purpose: This suite collects and runs all known good test suites.
 */
public class FATSuite {
}

