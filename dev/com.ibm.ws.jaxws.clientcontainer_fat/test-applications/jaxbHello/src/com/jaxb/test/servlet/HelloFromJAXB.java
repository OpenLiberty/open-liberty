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
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import com.ibm.jaxb.test.bean.Customer;

/**
 * Servlet implementation class HelloFromJAXB
 */

public class HelloFromJAXB {

    public static void main(String[] args) {

        Customer customer = null;
        XMLStreamReader xReader = null;
        try {

            /**
             * Using the javax.xml.bind.JAXBPermission to test whether use the JAXB-2.2 api
             */
            Class.forName("javax.xml.bind.JAXBPermission");

            JAXBContext jaxbContext = JAXBContext.newInstance(Customer.class);

            /**
             * Unmarshall the XML file to java object
             */
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            xReader = XMLInputFactory.newInstance()
                            .createXMLStreamReader(HelloFromJAXB.class.getClassLoader()
                                            .getResourceAsStream(
                                                                 ("/META-INF/resources/customer.xml")));
            JAXBElement<Customer> element = unmarshaller.unmarshal(xReader, Customer.class);

            customer = element.getValue();

            /**
             * Marshall the customer back and out put the xml to client
             */
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

            // output pretty printed
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

            jaxbMarshaller.marshal(customer, System.out);

        } catch (JAXBException e) {
            e.printStackTrace();
        } catch (Throwable t) {
            if (t instanceof ClassNotFoundException ||
                t instanceof NoClassDefFoundError) {
                System.out.println(t.getMessage());
            }
        } finally {

            System.out.flush();
            System.out.close();

        }
    }

}
