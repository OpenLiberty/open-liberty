/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2015
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
package vistest.appClient.main;

import javax.inject.Inject;

import vistest.appClient.AppClientTestingBean;
import vistest.appClientAsAppClientLib.AppClientAsAppClientLibTestingBean;
import vistest.appClientLib.AppClientLibTestingBean;

/**
 * Called by the test client to test the visibility of beans from different BDAs which are only accessible in a client application.
 */
public class Main {

    @Inject
    private static AppClientTestingBean appClientTestingBean;

    @Inject
    private static AppClientLibTestingBean appClientLibTestingBean;

    @Inject
    private static AppClientAsAppClientLibTestingBean appClientAsAppClientLibTestingBean;

    /**
     * Print the results of each testing bean in turn, separated by a line of "----"
     * 
     * @param args ignored
     */
    public static void main(String[] args) {

        // We need an initial line to separate the startup messages from the application output
        System.out.println("----");

        System.out.println("InAppClient");
        System.out.print(appClientTestingBean.doTest());

        System.out.println("----");

        System.out.println("InAppClientLib");
        System.out.print(appClientLibTestingBean.doTest());

        System.out.println("----");

        System.out.println("InAppClientAsAppClientLib");
        System.out.print(appClientAsAppClientLibTestingBean.doTest());

        // We need a final line to separate the results from the shutdown messages
        System.out.println("----");

    }

}
