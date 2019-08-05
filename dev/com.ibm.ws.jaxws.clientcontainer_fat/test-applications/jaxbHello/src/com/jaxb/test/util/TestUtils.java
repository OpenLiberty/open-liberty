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
package com.jaxb.test.util;

import java.math.BigDecimal;

import com.ibm.jaxb.test.bean.Customer;
import com.ibm.jaxb.test.bean.UserAddress;

public class TestUtils {
    public static Customer createCustomer() {

        Customer customer = new Customer();
        customer.setId(1);
        customer.setName("customer");
        customer.setPhone("992-00356482");

        UserAddress uAddr0 = new UserAddress();
        uAddr0.setCity("City0");
        uAddr0.setName("customer");
        uAddr0.setState("state0");
        uAddr0.setStreet("street0");
        uAddr0.setZip(new BigDecimal("034952"));

        UserAddress uAddr1 = new UserAddress();
        uAddr1.setCity("City1");
        uAddr1.setName("customer");
        uAddr1.setState("state1");
        uAddr1.setStreet("street1");
        uAddr1.setZip(new BigDecimal("096843"));

        customer.getAddress().add(uAddr0);
        customer.getAddress().add(uAddr1);

        return customer;
    }
}
