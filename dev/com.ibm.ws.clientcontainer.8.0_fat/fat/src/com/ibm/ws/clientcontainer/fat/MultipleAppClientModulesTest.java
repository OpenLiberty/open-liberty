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

public class MultipleAppClientModulesTest extends AbstractTest {

    @Test
    public void testMultipleAppClientModulesFirst() throws Exception {
        client.copyFileToLibertyClientRoot("clients/MultipleAppClientModulesFirst/client.xml");
        String appClientMsg = "Hello Application Client.";
        startProcess();
        assertStartMessages();
        assertAppMessage(appClientMsg);
    }

    @Test
    public void testMultipleAppClientModulesSecond() throws Exception {
        client.copyFileToLibertyClientRoot("clients/MultipleAppClientModulesSecond/client.xml");
        String appClientMsg = "SystemExitClient main entry";
        startProcess();
        assertStartMessages();
        assertAppMessage(appClientMsg);
    }

    @Test
    public void testMultipleAppClientModulesNone() throws Exception {
        client.copyFileToLibertyClientRoot("clients/MultipleAppClientModulesNone/client.xml");
        String appClientMsg = "CWWKZ0127E:";
        startProcess();
        assertAppMessage(appClientMsg);
    }

    @Test
    public void testMultipleAppClientModulesUnknown() throws Exception {
        client.copyFileToLibertyClientRoot("clients/MultipleAppClientModulesUnknown/client.xml");
        String appClientMsg = "CWWKZ0129E:";
        startProcess();
        assertAppMessage(appClientMsg);
    }

    @Test
    public void testMultipleAppClientModulesNoAppDD() throws Exception {
        client.copyFileToLibertyClientRoot("clients/MultipleAppClientModulesNoAppDD/client.xml");
        String appClientMsg = "Hello Application Client.";
        startProcess();
        assertStartMessages();
        assertAppMessage(appClientMsg);
    }
}
