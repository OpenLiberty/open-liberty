/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.test.overriddenuri.client.servlet;

import javax.xml.ws.BindingProvider;
import javax.xml.ws.WebServiceRef;

import com.ibm.ws.jaxws.test.wsr.client.TestUtils;
import com.ibm.ws.test.overriddenuri.client.SimpleEcho;
import com.ibm.ws.test.overriddenuri.client.SimpleEchoService;

/**
 *
 */

public class TestOverriddenEndpointUri {

    @WebServiceRef(name = "service/SimpleEchoService")
    private static SimpleEchoService defaultSimpleEchoService;

    public static void main(String[] args) {

        String result = null;

        try {
            SimpleEcho simpleEcho = defaultSimpleEchoService.getSimpleEchoPort();
            TestUtils.setEndpointAddressProperty((BindingProvider) simpleEcho, args[0], Integer.parseInt(args[1]));

            result = simpleEcho.echo("Hello");
        } catch (Exception e) {
            result = e.getMessage();
        }

        System.out.println(result);

    }
}
