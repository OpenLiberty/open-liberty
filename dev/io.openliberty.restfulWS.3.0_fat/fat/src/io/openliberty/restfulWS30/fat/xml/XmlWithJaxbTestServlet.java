/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
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
package io.openliberty.restfulWS30.fat.xml;

import static org.junit.Assert.assertEquals;

import java.net.HttpURLConnection;
import java.net.URI;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import jakarta.servlet.annotation.WebServlet;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/XmlWithJaxbTestServlet")
public class XmlWithJaxbTestServlet extends FATServlet {

    @Test
    public void testCanInvokeResourceWithApplicationXmlWithBuiltInJaxbProvider() throws Exception {
        URI uri = URI.create("http://localhost:" + System.getProperty("bvt.prop.HTTP_default") + "/xml/app/path/300");
        HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
        assertEquals(200, conn.getResponseCode());

        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = builder.parse(conn.getInputStream());
        Element rootElement = doc.getDocumentElement();
        assertEquals("entity", rootElement.getTagName());
        assertEquals("300", rootElement.getElementsByTagName("entityNumber").item(0).getTextContent());
        assertEquals("foo", rootElement.getElementsByTagName("entityName").item(0).getTextContent());
    }
}
