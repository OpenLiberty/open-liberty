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

import java.io.Writer;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import com.ibm.jaxb.test.bean.Customer;
import com.jaxb.test.util.TestUtils;

/**
 * Servlet implementation class TestNotUsingBootstrapClassLoader
 */

public class TestNotUsingBootstrapClassLoader {

    public static void main(String[] args) {

        Writer out = null;

        Customer customer = TestUtils.createCustomer();

        try {

            JAXBContext jaxbContext = JAXBContext.newInstance(Customer.class);
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

            /**
             * Test if the JAXBContext API is from JDK
             */
            if (JAXBContext.class.getClassLoader() == null) {
                System.out.println("The JAXBContext.class is loaded from JDK");
                return;
            }
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
