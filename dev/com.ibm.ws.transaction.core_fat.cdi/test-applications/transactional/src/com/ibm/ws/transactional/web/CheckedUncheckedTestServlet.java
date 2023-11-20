/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package com.ibm.ws.transactional.web;

import static org.junit.Assert.fail;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;
import javax.transaction.RollbackException;
import javax.transaction.TransactionalException;

import org.junit.Test;

import componenttest.annotation.ExpectedFFDC;
import componenttest.app.FATServlet;

/**
 * Servlet implementation class TransactionalTest
 */
@WebServlet("/checkedunchecked")
public class CheckedUncheckedTestServlet extends FATServlet {
    private static final long serialVersionUID = 1L;

    @Inject
    private CheckedUncheckedTestBean bean;

    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
    public void testCheckedRequired() {
        try {
            bean.checkedRequired();
            fail();
        } catch (RollbackException e) {
            // pass
        }
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
    public void testUncheckedRequired() {
        try {
            bean.uncheckedRequired();
            fail();
        } catch (TransactionalException e) {
            // pass
        }
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
    public void testCheckedRequiresNew() {
        try {
            bean.checkedRequiresNew();
            fail();
        } catch (RollbackException e) {
            // pass
        }
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
    public void testUncheckedRequiresNew() {
        try {
            bean.uncheckedRequiresNew();
            fail();
        } catch (TransactionalException e) {
            // pass
        }
    }

    @Test
    @ExpectedFFDC(value = { "java.lang.RuntimeException" })
    public void testRTENotWrapped() {
        try {
            bean.throwRTE();
            fail();
        } catch (TransactionalException e) {
            fail();
        } catch (RuntimeException e) {
            // pass
        }
    }
}