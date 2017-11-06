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
package com.ibm.ws.clientcontainer.fat;

import org.junit.Test;

public class AppClientInvalidFeatureConfig extends AbstractTest {

    /*
     * Check if client.xml contains any other features than javaeeClient-7.0,
     * The timedExit and osgiConsole features are allowed.
     */
    @Test
    public void testInvalidFeatureInClient() throws Exception {
        client.copyFileToLibertyClientRoot("clients/HelloAppClientWithInvalidFeature/client.xml");
        String appClientMsg = "Hello Application Client.";
        startProcess();
        assertStartMessages();
        assertAppMessage(appClientMsg);
        assertAppMessage("CWWKF0040E:");
    }

}
