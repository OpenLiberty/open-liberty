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
package com.ibm.ws.cdi.extension.apps.xtorException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import javax.enterprise.inject.UnsatisfiedResolutionException;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.cdi.extension.spi.test.constructor.exception.ExtensionRegisteredBean;
import com.ibm.ws.cdi.extension.spi.test.constructor.exception.InterfaceRegisteredBean;

import componenttest.app.FATServlet;

@WebServlet("/")
public class ConstructorExceptionServlet extends FATServlet {

    @Inject
    InterfaceRegisteredBean bean;

    @Inject
    DummyBean db;

    private static final long serialVersionUID = 1L;

    @Test
    public void testCDIExtension() throws Exception {
        db.iExist(); //a simple injected bean that should always be there

        //since the Constructor of the CDI Extension class throws an exception, ExtensionRegisteredBean should not be found
        try {
            ExtensionRegisteredBean ub = CDI.current().select(ExtensionRegisteredBean.class).get();
            fail("Found unregistered bean: " + ub);
        } catch (UnsatisfiedResolutionException e) {
            //expected
        }
    }

    @Test
    public void testSPIExtension() throws Exception {
        db.iExist(); //a simple injected bean that should always be there

        //despite the CDI Extension class throwing an exception, the bean added via the SPI should still be found
        assertNotNull(bean);
        assertEquals("getBeans registered bean was injected", bean.toString());
    }
}
