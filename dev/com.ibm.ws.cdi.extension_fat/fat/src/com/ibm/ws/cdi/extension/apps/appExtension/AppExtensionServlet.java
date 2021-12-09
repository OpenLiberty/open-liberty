/*******************************************************************************
 * Copyright (c) 2015, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.extension.apps.appExtension;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.cdi.extension.apps.appExtension.jar.InLibJarBean;

import componenttest.app.FATServlet;

/**
 * Servlet implementation class TestServlet
 */
@WebServlet("/")
public class AppExtensionServlet extends FATServlet {
    private static final long serialVersionUID = 1L;

    @Inject
    InSameWarBean inSameWar;

    @Inject
    InLibJarBean inLibJar;

    @Test
    public void testAppServlet() throws Exception {
        assertNotNull(inSameWar);
        assertNotNull(inLibJar);
        assertTrue(inSameWar.toString().contains("created in"));
        assertTrue(inLibJar.toString().contains("created in"));
    }
}
