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

import com.ibm.ws.cdi.misplaced.spi.test.bundle.extension.MyExtensionString;
import com.ibm.ws.cdi.misplaced.spi.test.bundle.getclass.beaninjection.MyBeanInjectionString;
import com.ibm.ws.cdi.misplaced.spi.test.bundle.getclass.producer.MyProducedString;

import componenttest.app.FATServlet;

@WebServlet("/misplaced")
public class MisplacedTestServlet extends FATServlet {

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
            //This will fail because while the extension will run as expected and add MyExtensionString to the BDA, it will be filtered out later because it cannot be found in the bundle.
            MyExtensionString ub = CDI.current().select(MyExtensionString.class).get();
            fail("Bean registered via an extension when both the bean and the extension are in a different bundle to the SPI impl class. This is unexpected: " + ub);
        } catch (UnsatisfiedResolutionException e) {
            //expected
        }
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
