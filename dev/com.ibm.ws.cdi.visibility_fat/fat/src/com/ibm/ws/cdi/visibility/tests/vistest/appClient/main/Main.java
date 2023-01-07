/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
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
package com.ibm.ws.cdi.visibility.tests.vistest.appClient.main;

import javax.inject.Inject;

import com.ibm.ws.cdi.visibility.tests.vistest.appClient.AppClientTestingBean;
import com.ibm.ws.cdi.visibility.tests.vistest.appClientAsAppClientLib.AppClientAsAppClientLibTestingBean;
import com.ibm.ws.cdi.visibility.tests.vistest.appClientLib.AppClientLibTestingBean;
import com.ibm.ws.cdi.visibility.tests.vistest.framework.TestingBean;
import com.ibm.ws.cdi.visibility.tests.vistest.qualifiers.InRuntimeExtRegular;
import com.ibm.ws.cdi.visibility.tests.vistest.qualifiers.InRuntimeExtSeeApp;

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

    // Using a qualifier to look this up so that we don't have to have the runtime extension export this package as API
    @Inject
    @InRuntimeExtRegular
    private static TestingBean runtimeExtRegularTestingBean;

    // Using a qualifier to look this up so that we don't have to have the runtime extension export this package as API
    @Inject
    @InRuntimeExtSeeApp
    private static TestingBean runtimeExtSeeAppTestingBean;

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

        System.out.println("----");

        System.out.println("InRuntimeExtRegular");
        System.out.print(runtimeExtRegularTestingBean.doTest());

        System.out.println("----");

        System.out.println("InRuntimeExtSeeApp");
        System.out.print(runtimeExtSeeAppTestingBean.doTest());

        // We need a final line to separate the results from the shutdown messages
        System.out.println("----");

    }

}
