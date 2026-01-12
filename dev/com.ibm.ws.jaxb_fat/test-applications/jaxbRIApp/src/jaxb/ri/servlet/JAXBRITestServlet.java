/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package jaxb.ri.servlet;

import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.util.logging.Logger;

import javax.servlet.annotation.WebServlet;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.junit.Test;

import componenttest.app.FATServlet;
import jaxb.ri.object.Book;

@WebServlet("/JAXBRITestServlet")
public class JAXBRITestServlet extends FATServlet {

    Logger LOG = Logger.getLogger("jaxb.ri.servlet.JAXBRITestServlet");

    @Test
    public void testOldGmonthMapping() throws JAXBException {
        String xmlOldGmonthMapping = "<book id='1123121234'><gdate>--12--</gdate></book>";
        ByteArrayInputStream xmlInputOld = new ByteArrayInputStream(xmlOldGmonthMapping.getBytes());

        JAXBContext ctx;
        ctx = JAXBContext.newInstance(Book.class);
        Unmarshaller unMarshaller = ctx.createUnmarshaller();
        Book bookOld = (Book) unMarshaller.unmarshal(xmlInputOld);

        assertNotNull("Old Gregorian month mapping(jaxb.ri.useOldGmonthMapping) is not honored!", bookOld.getgDate());
    }

    @Test
    public void testGmonthMapping() throws JAXBException {
        String xmlNewGmonthMapping = "<book id='1123121234'><title>The one about JAXB</title><gdate>--12</gdate></book>";
        ByteArrayInputStream xmlInputNew = new ByteArrayInputStream(xmlNewGmonthMapping.getBytes());

        JAXBContext ctx;
        ctx = JAXBContext.newInstance(Book.class);
        Unmarshaller unMarshaller = ctx.createUnmarshaller();
        Book bookNew = (Book) unMarshaller.unmarshal(xmlInputNew);

        assertNotNull("When Old Gregorian month mapping(jaxb.ri.useOldGmonthMapping)is activated, new month mapping failed!", bookNew.getgDate());
    }

}
