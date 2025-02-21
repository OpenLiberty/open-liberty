/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package jaxb.xmlnsform.servlet;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

/**
 *
 */
@WebServlet("/JaxbSpecTest")
public class JaxbSpecTestServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doWorker(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doWorker(req, resp);
    }

    private void doWorker(HttpServletRequest req, HttpServletResponse resp) {
        PrintWriter writer = null;
        try {
            writer = resp.getWriter();
            File personXml = new File("Person.xml");

            if (!personXml.exists()) {
                writer.write("XML not found");
                return;
            }
            JAXBContext jaxbContextQualified = JAXBContext.newInstance(jaxb.xmlnsform.qualified.Person.class);
            Unmarshaller unmarshallerQualified = jaxbContextQualified.createUnmarshaller();
            jaxb.xmlnsform.qualified.Person qualifiedPerson = (jaxb.xmlnsform.qualified.Person) unmarshallerQualified.unmarshal(personXml);
            writeNullStatus(writer, "qpwp", qualifiedPerson.getFirstNameWithPrefix()); // qpwp: Qualified Person With Prefix
            writeNullStatus(writer, "qpnp", qualifiedPerson.getLastNameWithOutPrefix()); // qpnp: Qualified Person No Prefix

            JAXBContext jaxbContextUnQualified = JAXBContext.newInstance(jaxb.xmlnsform.unqualified.Person.class);
            Unmarshaller unmarshallerUnQualified = jaxbContextUnQualified.createUnmarshaller();
            jaxb.xmlnsform.unqualified.Person unQualifiedPerson = (jaxb.xmlnsform.unqualified.Person) unmarshallerUnQualified.unmarshal(personXml);
            writeNullStatus(writer, "upwp", unQualifiedPerson.getFirstNameWithPrefix()); // upwp: Unqualified Person With Prefix
            writeNullStatus(writer, "upnp", unQualifiedPerson.getLastNameWithOutPrefix()); // upnp: Unqualified Person No Prefix

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     *
     * @param key   key to mark which assertion it's related to
     * @param value value to test if it's null
     * @return
     */
    private void writeNullStatus(PrintWriter writer, String key, String value) {
        writer.write(key + (value == null ? ":null |" : ":value |"));
    }

}
