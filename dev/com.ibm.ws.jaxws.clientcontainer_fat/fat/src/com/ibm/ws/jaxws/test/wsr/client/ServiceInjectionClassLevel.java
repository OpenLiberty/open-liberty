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
package com.ibm.ws.jaxws.test.wsr.client;

import javax.naming.InitialContext;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.WebServiceRef;

import com.ibm.ws.jaxws.test.wsr.server.stub.People;
import com.ibm.ws.jaxws.test.wsr.server.stub.PeopleService;

// test Class level service injection
@WebServiceRef(name = "services/serviceInjectionClassLevel", type = PeopleService.class)
public class ServiceInjectionClassLevel {

    public static void main(String[] args) {
        InitialContext context;
        try {

            context = new InitialContext();
            Object service = context.lookup("java:comp/env/services/serviceInjectionClassLevel");

            People bill = ((PeopleService) service).getBillPort();
            //workaround for the hard-coded server addr and port in wsdl

            TestUtils.setEndpointAddressProperty((BindingProvider) bill, args[0], Integer.parseInt(args[1]));
            System.out.println(bill.hello("Response from ServiceInjectionClassLevel"));
        } catch (Throwable t) {
            System.out.println("throw able: " + t);

        }

    }

}
