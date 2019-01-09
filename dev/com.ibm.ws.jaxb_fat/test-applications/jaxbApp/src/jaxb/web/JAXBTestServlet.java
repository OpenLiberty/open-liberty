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
package jaxb.web;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.security.CodeSource;

import javax.servlet.annotation.WebServlet;
import javax.xml.bind.JAXBContext;

import org.junit.Test;

import componenttest.annotation.SkipForRepeat;
import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/JAXBTestServlet")
public class JAXBTestServlet extends FATServlet {

    @Test
    public void testJaxbAPILoadedFromLiberty() throws Exception {
        assertNull("System property 'javax.xml.bind.context.factory' effects the entire JVM and should not be set by the Liberty runtime!",
                   System.getProperty("javax.xml.bind.context.factory"));

        ClassLoader apiLoader = JAXBContext.class.getClassLoader();
        CodeSource apiSrc = JAXBContext.class.getProtectionDomain().getCodeSource();
        String apiLocation = apiSrc == null ? null : apiSrc.getLocation().toString();
        System.out.println("Got JAX-B API from loader=  " + apiLoader);
        System.out.println("Got JAX-B API from location=" + apiLocation);
        assertTrue("Expected JAX-B API to come from Liberty bundle, but it came from: " + apiLoader,
                   apiLoader != null && apiLoader.toString().contains("com.ibm.websphere.javaee.jaxb.2."));
        assertTrue("Expected JAX-B API to come from Liberty, but it came from: " + apiLocation,
                   apiLocation != null && apiLocation.contains("com.ibm.websphere.javaee.jaxb.2."));
    }

    @Test
    public void testJaxbImplLoadedFromLiberty() throws Exception {
        JAXBContext ctx = JAXBContext.newInstance("jaxb.web", ObjectFactory.class.getClassLoader());
        ClassLoader implLoader = ctx.getClass().getClassLoader();
        CodeSource implSrc = ctx.getClass().getProtectionDomain().getCodeSource();
        String implLocation = implSrc == null ? null : implSrc.getLocation().toString();
        System.out.println("JAX-B impl is: " + ctx.getClass());
        System.out.println("Got JAX-B impl from loader=  " + implLoader);
        System.out.println("Got JAX-B impl from location=" + implLocation);
        assertTrue("Expected JAX-B impl to come from JDK classloader, but it came from: " + implLoader,
                   implLoader != null && implLoader.toString().contains("com.ibm.ws."));
        assertTrue("Expected JAX-B impl to come from JDK, but it came from: " + implLocation,
                   implLocation != null && implLocation.contains("com.ibm.ws."));
    }

    @Test
    @SkipForRepeat("JAXB-2.3")
    public void testActivationLoaded_jaxb22() throws Exception {
        // Verify Activation API came from the JDK
        ClassLoader apiLoader = javax.activation.DataHandler.class.getClassLoader();
        CodeSource apiSrc = javax.activation.DataHandler.class.getProtectionDomain().getCodeSource();
        String apiLocation = apiSrc == null ? null : apiSrc.getLocation().toString();
        System.out.println("Got javax.activation from loader=  " + apiLoader);
        System.out.println("Got javax.activation from location=" + apiLocation);

        // On JDK 7/8 we will continue to load javax.activation from the JDK, but in JDK 9+ we will load it from a Liberty bundle
        if (System.getProperty("java.specification.version").startsWith("1.")) {
            assertNull("Expected javax.activation to come from JDK classloader, but it came from: " + apiLoader, apiLoader);
            assertNull("Expected javax.activation to come from JDK, but it came from: " + apiLocation, apiLocation);
        } else {
            assertTrue("Expected javax.activation to come from Liberty JDK classloader, but it came from: " + apiLoader,
                       apiLoader != null && apiLoader.toString().contains("com.ibm.websphere.javaee.activation.1.1"));
            assertTrue("Expected javax.activation to come from Liberty, but it came from: " + apiLocation,
                       apiLocation != null && apiLocation.contains("com.ibm.websphere.javaee.activation.1.1"));
        }
    }

    @Test
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    public void testActivationLoaded_jaxb23() throws Exception {
        // Verify Activation API came from the JDK
        ClassLoader apiLoader = javax.activation.DataHandler.class.getClassLoader();
        CodeSource apiSrc = javax.activation.DataHandler.class.getProtectionDomain().getCodeSource();
        String apiLocation = apiSrc == null ? null : apiSrc.getLocation().toString();
        System.out.println("Got javax.activation from loader=  " + apiLoader);
        System.out.println("Got javax.activation from location=" + apiLocation);

        assertTrue("Expected javax.activation to come from Liberty JDK classloader, but it came from: " + apiLoader,
                   apiLoader != null && apiLoader.toString().contains("com.ibm.websphere.javaee.activation.1.1"));
        assertTrue("Expected javax.activation to come from Liberty, but it came from: " + apiLocation,
                   apiLocation != null && apiLocation.contains("com.ibm.websphere.javaee.activation.1.1"));
    }
}
