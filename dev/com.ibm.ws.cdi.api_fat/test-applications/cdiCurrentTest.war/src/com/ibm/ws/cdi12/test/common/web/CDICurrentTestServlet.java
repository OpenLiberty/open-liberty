/*******************************************************************************
 * Copyright (c) 2015, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi12.test.common.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.enterprise.inject.spi.CDI;
import javax.servlet.annotation.WebServlet;

import com.ibm.ws.cdi12.test.current.extension.MyDeploymentVerifier;

import componenttest.app.FATServlet;

@WebServlet("/")
public class CDICurrentTestServlet extends FATServlet {

    private static final long serialVersionUID = 1L;

    public void testCDICurrent() {

        String message = MyDeploymentVerifier.getMessage();
        assertEquals(message, MyDeploymentVerifier.SUCCESS, message);

        SimpleBean sb = CDI.current().select(SimpleBean.class).get();
        assertNotNull("SimpleBean was null", sb);
        String msg = sb.test();
        assertEquals("SimpleBean message was: " + msg, SimpleBean.MSG, msg);

    }

}
