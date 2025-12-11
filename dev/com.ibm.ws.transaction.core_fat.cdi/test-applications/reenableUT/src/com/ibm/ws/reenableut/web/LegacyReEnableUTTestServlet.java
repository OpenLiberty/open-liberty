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

import static org.junit.Assert.assertTrue;

import javax.annotation.Resource;
import javax.servlet.annotation.WebServlet;
import javax.transaction.UserTransaction;

import org.junit.Test;

@WebServlet("/legacyreenableut")
public class LegacyReEnableUTTestServlet extends ReEnableUTTestServlet {

    @Resource
    private UserTransaction ut;

    @Test
    public void testNotSupportedFromRequired() throws Exception {
        assertTrue(bean.checkReEnablementNotSupportedFromRequired(false));
    }

    @Test
    public void testNotSupportedFromRequiresNew() throws Exception {
        assertTrue(bean.checkReEnablementNotSupportedFromRequiresNew(false));
    }

    @Test
    public void testNotSupportedFromSupports() throws Exception {
        assertTrue(bean.checkReEnablementNotSupportedFromSupports(false));
    }

    @Test
    public void testNotSupportedFromMandatory() throws Exception {
        ut.begin();
        assertTrue(bean.checkReEnablementNotSupportedFromMandatory(false));
    }

    @Test
    public void testNeverFromSupports() throws Exception {
        assertTrue(bean.checkReEnablementNeverFromSupports(false));
    }
}