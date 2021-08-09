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
package com.jaxb.test.servlet;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import com.ibm.jaxb.test.bean.Customer;
import com.jaxb.test.util.TestUtils;

public class TestUsingIBMImpl {

    public static void main(String[] args) {

        Customer customer = TestUtils.createCustomer();

        try {

            JAXBContext jaxbContext = JAXBContext.newInstance(Customer.class);

            /**
             * Test if using IBM fast path implementation
             */
            if (!jaxbContext.getClass().getName().contains("com.ibm.xml.xlxp2")) {
                System.out.println("The jaxb implementation is not from IBM");
                return;
            }

            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

            // output pretty printed
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

            jaxbMarshaller.marshal(customer, System.out);

        } catch (JAXBException e) {
            e.printStackTrace();
        } finally {

            System.out.flush();
            System.out.close();

        }
    }

}
