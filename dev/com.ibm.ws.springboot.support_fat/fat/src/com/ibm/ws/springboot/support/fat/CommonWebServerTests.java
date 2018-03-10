/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.springboot.support.fat;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import componenttest.topology.utils.HttpUtils;

public abstract class CommonWebServerTests extends AbstractSpringTests {
    @Test
    public void testBasicSpringBootApplication() throws Exception {
        assertNotNull("The application was not installed", server
                        .waitForStringInLog("CWWKZ0001I:.*"));

        // NOTE we set the port to the expected port according to the test application.properties
        server.setHttpDefaultPort(8081);
        HttpUtils.findStringInUrl(server, "", "HELLO SPRING BOOT!!");
    }

}
