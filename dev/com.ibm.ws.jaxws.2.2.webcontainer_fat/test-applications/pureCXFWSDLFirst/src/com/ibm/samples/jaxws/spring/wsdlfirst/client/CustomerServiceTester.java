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
package com.ibm.samples.jaxws.spring.wsdlfirst.client;

import java.io.IOException;
import java.util.List;

import javax.xml.ws.BindingProvider;

import com.ibm.samples.jaxws.spring.wsdlfirst.stub.Customer;
import com.ibm.samples.jaxws.spring.wsdlfirst.stub.CustomerService;
import com.ibm.samples.jaxws.spring.wsdlfirst.stub.NoSuchCustomerException;
import com.ibm.ws.jaxws.fat.util.TestUtils;

public final class CustomerServiceTester {

    // The CustomerService proxy will be injected either by spring or by a direct call to the setter
    CustomerService customerService;

    public CustomerService getCustomerService() {
        return customerService;
    }

    public void setCustomerService(CustomerService customerService) {
        this.customerService = customerService;
    }

    public String getCustomerByName(String name, String address, int port) throws NoSuchCustomerException, IOException {
        List<Customer> customers = null;

        // First we test the positive case where customers are found and we retrieve
        // a list of customers
        System.out.println("Sending request for customers named Smith");
        TestUtils.setEndpointAddressProperty((BindingProvider) customerService, address, port);

        customers = customerService.getCustomersByName(name);
        System.out.println("Response received");
        return customers.get(0).getName();
    }

    public void updateCustomer(String address, int port) throws Exception {
        //need to reset the address the correct one in case this case will run before getCustomerByName- defect 132213
        TestUtils.setEndpointAddressProperty((BindingProvider) customerService, address, port);
        Customer customer = new Customer();
        customer.setName("Smith");
        customerService.updateCustomer(customer);
    }

}
