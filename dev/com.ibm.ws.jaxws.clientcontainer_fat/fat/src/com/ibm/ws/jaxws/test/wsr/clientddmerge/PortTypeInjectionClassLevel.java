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
package com.ibm.ws.jaxws.test.wsr.clientddmerge;

import javax.naming.InitialContext;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.WebServiceRef;

import com.ibm.ws.jaxws.test.wsr.client.TestUtils;
import com.ibm.ws.jaxws.test.wsr.server.stub.People;

//test Class level port type injection
@WebServiceRef(name = "services/portTypeInjectionClassLevel", type = People.class)
public class PortTypeInjectionClassLevel {

    public static void main(String[] args) {

        InitialContext context;
        try {
            context = new InitialContext();
            Object bill = context.lookup("java:comp/env/services/portTypeInjectionClassLevel");

            TestUtils.setEndpointAddressProperty((BindingProvider) bill, args[0], Integer.parseInt(args[1]));
            System.out.println(((People) bill).hello("Response from PortTypeInjectionClassLevel"));
        } catch (Throwable t) {
            System.out.println("throw able: " + t);

        }

    }

}