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
package com.ibm.ws.jaxws.test.wsr.clientserviceresource;

import javax.annotation.Resource;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Service;

import com.ibm.ws.jaxws.test.wsr.client.TestUtils;
import com.ibm.ws.jaxws.test.wsr.server.stub.People;
import com.ibm.ws.jaxws.test.wsr.server.stub.PeopleService;

public class ServiceInjectionMemberLevel {

    // test normal service injection
    @Resource(name = "services/serviceInjectionMemberLevel")
    static Service service;

    public static void main(String[] args) {

        try {
            People bill = ((PeopleService) service).getBillPort();
            TestUtils.setEndpointAddressProperty((BindingProvider) bill, args[0], Integer.parseInt(args[1]));
            System.out.println(bill.hello("Response from ServiceInjectionMemberLevel"));
        } catch (Throwable t) {
            System.out.println("throw able: " + t);

        }

    }

}
