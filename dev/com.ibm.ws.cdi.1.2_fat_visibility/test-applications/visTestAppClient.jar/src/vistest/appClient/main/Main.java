/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
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
