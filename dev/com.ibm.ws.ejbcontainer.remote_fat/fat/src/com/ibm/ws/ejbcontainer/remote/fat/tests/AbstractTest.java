/*
* IBM Confidential
*
* OCO Source Materials
*
* WLP Copyright IBM Corp. 2018
*
* The source code for this program is not published or otherwise divested
* of its trade secrets, irrespective of what has been deposited with the
* U.S. Copyright Office.
*/
package com.ibm.ws.ejbcontainer.remote.fat.tests;

import org.junit.Rule;
import org.junit.rules.TestName;

import componenttest.custom.junit.runner.RepeatTestFilter;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 *
 */
public abstract class AbstractTest {

    public abstract LibertyServer getServer();

    @Rule
    public TestName testName = new TestName();

    protected void runTest(String servlet) throws Exception {
        String test = testName.getMethodName().endsWith(RepeatTestFilter.CURRENT_REPEAT_ACTION) ? testName.getMethodName().substring(0,
                                                                                                                                     testName.getMethodName().length()
                                                                                                                                        - (RepeatTestFilter.CURRENT_REPEAT_ACTION.length()
                                                                                                                                           + 1)) : testName.getMethodName();
        FATServletClient.runTest(getServer(), servlet, test);
    }

    protected void runTest(String servlet, String testMethod) throws Exception {
        FATServletClient.runTest(getServer(), servlet, testMethod);
    }

}
