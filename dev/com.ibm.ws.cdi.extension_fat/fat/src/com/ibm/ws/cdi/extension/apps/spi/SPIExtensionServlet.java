/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.extension.apps.spi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import javax.enterprise.inject.UnsatisfiedResolutionException;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.cdi.extension.spi.test.bundle.UnregisteredBean;
import com.ibm.ws.cdi.extension.spi.test.bundle.extension.MyExtensionString;
import com.ibm.ws.cdi.extension.spi.test.bundle.getclass.beaninjection.MyBeanInjectionString;
import com.ibm.ws.cdi.extension.spi.test.bundle.getclass.producer.MyProducedString;

import componenttest.app.FATServlet;

@WebServlet("/spi")
public class SPIExtensionServlet extends FATServlet {

    @Inject
    MyExtensionString extensionString;

    @Inject
    MyProducedString classString;

    @Inject
    MyBeanInjectionString beanInjectedString;

    @Inject
    AppBean appBean;

    @Inject
    CustomBDABean customBDABean;

    private static final long serialVersionUID = 1L;

    @Test
    public void testUnregisteredBean() {
        try {
            UnregisteredBean ub = CDI.current().select(UnregisteredBean.class).get();
            fail("Found unregistered bean: " + ub);
        } catch (UnsatisfiedResolutionException e) {
            //expected
        }

    }

    @Test
    public void testUnregisteredBDABean() {
        try {
            UnregisteredBDABean ub = CDI.current().select(UnregisteredBDABean.class).get();
            fail("Found unregistered bean: " + ub);
        } catch (UnsatisfiedResolutionException e) {
            //expected
        }
    }

    @Test
    public void testSPIProducer() {
        assertEquals("Injection from a producer registered in a CDI extension that was registered through the SPI", extensionString.toString());
    }

    @Test
    public void testGetBeanClasses() {
        assertEquals("An Interceptor registered via getBeanClasses in the SPI intercepted a normal scoped class registered via getBeanClasses Injection of a normal scoped class that was registered via getBeanClasses",
                     beanInjectedString.toString());
    }

    @Test
    public void testSPIInterceptorNormal() {
        assertEquals("Produced injection", classString.toString());
    }

    @Test
    public void testSPIInterceptorNormalAppWAR() {
        assertEquals("An Interceptor registered via getBeanClasses in the SPI intercepted a normal scoped class in the application WAR", appBean.toString());
    }

    @Test
    public void testBeanDefiningAnnotationClasses() {
        assertEquals("A Bean with an annotation registered via getBeanDefiningAnnotationClasses was successfully injected into a different bean with an annotation registered via getBeanDefiningAnnotationClasses",
                     customBDABean.toString());
    }

}
