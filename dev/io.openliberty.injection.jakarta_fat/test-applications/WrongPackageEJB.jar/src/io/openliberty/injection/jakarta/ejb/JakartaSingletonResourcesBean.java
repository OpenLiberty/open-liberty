/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.injection.jakarta.ejb;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import jakarta.annotation.Resource;
import jakarta.ejb.EJBContext;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Singleton;

/**
 * Singleton bean using jakarta package annotations, except for
 * an incorrect use of javax.annotation.Resources.
 */
@Singleton
@javax.annotation.Resources(@javax.annotation.Resource(name = "SessionContext", type = SessionContext.class))
public class JakartaSingletonResourcesBean {
    @Resource
    EJBContext ctx;

    public void verifyInjection() {
        assertNotNull("jakarta Resource EJBContext is null", ctx);

        try {
            Context namingCtx = new InitialContext();
            SessionContext sctx = (SessionContext) namingCtx.lookup("java:comp/env/SessionContext");
            fail("lookup of javax Resrouce was successful : " + sctx);
        } catch (NamingException nex) {
            System.out.println("Expected NamingException occurred : " + nex);
        }
    }
}
