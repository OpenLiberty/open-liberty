/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.injection.jakarta.webapp;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import componenttest.app.FATServlet;
import jakarta.annotation.Resource;
import jakarta.servlet.annotation.WebServlet;
import jakarta.transaction.UserTransaction;

/**
 * Servlet using Jakarta package annotations, except for one incorrect use
 * of javax.annotation.Resource.
 */
@SuppressWarnings("serial")
@WebServlet("/WrongPackageWebAppServlet")
public class WrongPackageWebAppServlet extends FATServlet {
    @Resource
    UserTransaction userTx;

    @javax.annotation.Resource
    UserTransaction userTx2;

    /**
     * This test verifies a warning is logged when the javax.annotation.Resource annotation
     * is used with Jakarta EE features and the annotation is ignored. There should be one
     * warning per module. The jakarta.annotation.Resource annotation works as expected.
     */
    public void testWrongPackageCommonAnnotations() throws Exception {
        assertNotNull("jakarta Resource UserTransaction is null", userTx);
        assertNull("javax Resource UserTransaction is not null", userTx2);
    }
}