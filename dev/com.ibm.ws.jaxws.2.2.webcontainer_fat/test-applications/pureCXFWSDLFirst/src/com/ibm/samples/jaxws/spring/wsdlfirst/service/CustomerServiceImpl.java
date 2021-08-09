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
package com.ibm.samples.jaxws.spring.wsdlfirst.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import javax.annotation.Resource;
import javax.xml.ws.WebServiceContext;

import com.ibm.samples.jaxws.spring.wsdlfirst.stub.Customer;
import com.ibm.samples.jaxws.spring.wsdlfirst.stub.CustomerService;
import com.ibm.samples.jaxws.spring.wsdlfirst.stub.CustomerType;
import com.ibm.samples.jaxws.spring.wsdlfirst.stub.NoSuchCustomer;
import com.ibm.samples.jaxws.spring.wsdlfirst.stub.NoSuchCustomerException;

public class CustomerServiceImpl implements CustomerService {

    /**
     * The WebServiceContext can be used to retrieve special attributes like the
     * user principal. Normally it is not needed
     */
    @Resource
    public WebServiceContext wsContext;

    @Override
    public List<Customer> getCustomersByName(String name) throws NoSuchCustomerException {
        if ("None".equals(name)) {
            NoSuchCustomer noSuchCustomer = new NoSuchCustomer();
            noSuchCustomer.setCustomerName(name);
            throw new NoSuchCustomerException("Did not find any matching customer for name=" + name, noSuchCustomer);
        }

        List<Customer> customers = new ArrayList<Customer>();
        for (int c = 0; c < 2; c++) {
            Customer cust = new Customer();
            cust.setName(name);
            cust.getAddress().add("Pine Street 200");
            Date bDate = new GregorianCalendar(2009, 01, 01).getTime();
            cust.setBirthDate(bDate);
            cust.setNumOrders(1);
            cust.setRevenue(10000);
            cust.setTest(new BigDecimal(1.5));
            cust.setType(CustomerType.BUSINESS);
            customers.add(cust);
        }

        return customers;
    }

    @Override
    public void updateCustomer(Customer customer) throws Exception {
        System.out.println("update request was received");
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            // Nothing to do here
        }
        if (isWSContextNull()) {
            throw new Exception("WebServiceContext is null");
        }
        System.out.println("Customer was updated");

    }

    public boolean isWSContextNull() {
        return wsContext == null;
    }

}
