/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package jaxb.thirdparty.web;

import static org.junit.Assert.assertNull;

import java.security.CodeSource;

import javax.servlet.annotation.WebServlet;
import javax.xml.bind.JAXBContext;

import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/ThirdPartyJAXBTestServlet")
public class ThirdPartyJAXBTestServlet extends FATServlet {

    @Test
    public void testJaxbAPILoadedFromJDK() throws Exception {
        assertNull("System property 'javax.xml.bind.context.factory' effects the entire JVM and should not be set by the Liberty runtime!",
                   System.getProperty("javax.xml.bind.context.factory"));

        // Verify JAX-B API came from the JDK
        ClassLoader apiLoader = JAXBContext.class.getClassLoader();
        CodeSource apiSrc = JAXBContext.class.getProtectionDomain().getCodeSource();
        String apiLocation = apiSrc == null ? null : apiSrc.getLocation().toString();
        System.out.println("Got JAX-B API from loader=  " + apiLoader);
        System.out.println("Got JAX-B API from location=" + apiLocation);
        assertNull("Expected JAX-B API to come from JDK classloader, but it came from: " + apiLoader, apiLoader);
        assertNull("Expected JAX-B API to come from JDK, but it came from: " + apiLocation, apiLocation);
    }

    @Test
    public void testJaxbImplLoadedFromJDK() throws Exception {
        // Verify JAX-B impl came from the JDK
        JAXBContext ctx = JAXBContext.newInstance("jaxb.thirdparty.web", ObjectFactory.class.getClassLoader());
        ClassLoader implLoader = ctx.getClass().getClassLoader();
        CodeSource implSrc = ctx.getClass().getProtectionDomain().getCodeSource();
        String implLocation = implSrc == null ? null : implSrc.getLocation().toString();
        System.out.println("JAX-B impl is: " + ctx.getClass());
        System.out.println("Got JAX-B impl from loader=  " + implLoader);
        System.out.println("Got JAX-B impl from location=" + implLocation);
        assertNull("Expected JAX-B impl to come from JDK classloader, but it came from: " + implLoader, implLoader);
        assertNull("Expected JAX-B impl to come from JDK, but it came from: " + implLocation, implLocation);
    }

    @Test
    public void testActivationLoadedFromJDK() throws Exception {
        // Verify Activation API came from the JDK
        ClassLoader apiLoader = javax.activation.DataHandler.class.getClassLoader();
        CodeSource apiSrc = javax.activation.DataHandler.class.getProtectionDomain().getCodeSource();
        String apiLocation = apiSrc == null ? null : apiSrc.getLocation().toString();
        System.out.println("Got javax.activation from loader=  " + apiLoader);
        System.out.println("Got javax.activation from location=" + apiLocation);
        assertNull("Expected javax.activation to come from JDK classloader, but it came from: " + apiLoader, apiLoader);
        assertNull("Expected javax.activation to come from JDK, but it came from: " + apiLocation, apiLocation);
    }
}
