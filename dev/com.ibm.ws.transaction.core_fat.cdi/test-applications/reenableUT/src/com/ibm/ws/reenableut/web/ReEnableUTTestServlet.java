/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package com.ibm.ws.reenableut.web;

import javax.inject.Inject;

import org.junit.Test;

import componenttest.app.FATServlet;

/**
 * Base class for testin UT reenablement
 * These tests should pass regardless of enableUserTransactionAsSpecified
 */
public abstract class ReEnableUTTestServlet extends FATServlet {

    /**
     *
     */
    public ReEnableUTTestServlet() {
        super();
    }

    @Inject
    ReEnableUserTranTestBean bean;

    @Test
    public void testNotSupportedFromNever() throws Exception {
        bean.checkReEnablementNotSupportedFromNever();
    }

    @Test
    public void testNotSupportedFromNotSupported() throws Exception {
        bean.checkReEnablementNotSupportedFromNotSupported();
    }

    @Test
    public void testNeverFromNotSupported() throws Exception {
        bean.checkReEnablementNeverFromNotSupported();
    }

    @Test
    public void testNeverFromNever() throws Exception {
        bean.checkReEnablementNeverFromNever();
    }
}