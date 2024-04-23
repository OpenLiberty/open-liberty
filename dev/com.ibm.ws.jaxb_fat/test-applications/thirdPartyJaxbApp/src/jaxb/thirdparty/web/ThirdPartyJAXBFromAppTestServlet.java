/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package jaxb.thirdparty.web;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.StringWriter;
import java.security.CodeSource;
import java.util.Date;
import java.util.logging.Logger;

import javax.servlet.annotation.WebServlet;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

import org.junit.Test;

import componenttest.app.FATServlet;

/**
 * This series of tests verify that a third party JAXB implementation can be packaged with an application
 * and not conflict with any Liberty feature that uses the internal version of jaxb-2.2.
 *
 * For these tests we use the jms-2.0 feature, though no jms-2.0 code is included in the app.
 *
 * These tests require the classloader delegation in the server.xml be set to "parentLast"
 *
 */
@SuppressWarnings("serial")
@WebServlet("/ThirdPartyJAXBFromAppTestServlet")
public class ThirdPartyJAXBFromAppTestServlet extends FATServlet {

    Logger LOG = Logger.getLogger("jaxb.thirdparty.web.ThirdPartyJAXBFromAppTestServlet");

    // The app class loader, used to verify that's where the API is loaded from.
    private static String API_APP_CLASSLOADER = "com.ibm.ws.classloading.internal.ParentLastClassLoader";

    private static String IMPL_LOCATION = "WEB-INF/lib";

    /**
     * This test verifies that a app packaged JAXB 2.2 spec API is loaded from the ParentLastClassLoader classloader and WEB-INF/lib directory of the
     * application rather from our internal classloader, and bundle location.
     */
    @Test
    public void testJaxbAPILoadedFromApp() throws Exception {
        assertNull("System property 'javax.xml.bind.context.factory' effects the entire JVM and should not be set by the Liberty runtime!",
                   System.getProperty("javax.xml.bind.context.factory"));

        // Verify JAX-B API came from the application dependencies
        ClassLoader apiLoader = JAXBContext.class.getClassLoader();
        CodeSource apiSrc = JAXBContext.class.getProtectionDomain().getCodeSource();
        String apiLocation = apiSrc == null ? null : apiSrc.getLocation().toString();
        LOG.info("Got JAX-B API from loader=  " + apiLoader);
        LOG.info("Got JAX-B API from location=" + apiLocation);
        assertTrue("Expected JAX-B API to come from ParentLastClassLoader classloader, but it came from: " + apiLoader.getClass().getName(),
                   apiLoader.getClass().getName().contains(API_APP_CLASSLOADER));
        assertTrue("Expected JAX-B API to come from the application, but it came from: " + apiLocation, apiLocation.contains(IMPL_LOCATION));
    }

    /**
     * This test verifies that the third party JAXB 2.2 impl is loaded and is not null and is loaded from the WEB-INF/lib directory of the
     * application rather from our internal classloader, and bundle location.
     */
    @Test
    public void testJaxbImplLoadedFromApp() throws Exception {
        // Verify JAX-B impl came from the application dependencies
        JAXBContext ctx = JAXBContext.newInstance("jaxb.thirdparty.web", ObjectFactory.class.getClassLoader());
        ClassLoader implLoader = ctx.getClass().getClassLoader();
        CodeSource implSrc = ctx.getClass().getProtectionDomain().getCodeSource();
        String implLocation = implSrc == null ? null : implSrc.getLocation().toString();
        LOG.info("JAX-B impl is: " + ctx.getClass());
        LOG.info("Got JAX-B impl from loader=  " + implLoader);
        LOG.info("Got JAX-B impl from location=" + implLocation);
        assertNotNull("Expected JAX-B impl to come from app ParentLastClassLoader, but it came from: " + implLoader, implLoader);
        assertTrue("Expected JAX-B impl to come from the application, but it came from: " + implLocation, implLocation.contains(IMPL_LOCATION));
    }

    /**
     * This test verifies that a third-party JAXB 2.2 Context can be created and produce an marshaller that can create a non-null output
     * when marshalling a jaxb.thirdparty.web.Book object.
     */
    @Test
    public void testJAXBContextLoadedFromApp() throws Exception {
        // Instantiate a Book object
        Book book = new Book();
        book.setAuthor("Libby Bot");
        book.setDate(new Date());
        book.setId(Long.parseLong("1123121234"));
        book.setName("The one about JAXB");

        // Create a new JAXBContext and marshall it too a StringWriter
        JAXBContext ctx = JAXBContext.newInstance(Book.class);
        Marshaller marshaller = ctx.createMarshaller();
        StringWriter stringWriter = new StringWriter();
        marshaller.marshal(book, stringWriter);

        LOG.info("Marshaller returned: " + stringWriter.toString());

        // Verify contents are not null.
        assertNotNull("Expected marshalled Book to be non-null", stringWriter.toString());
    }

}
