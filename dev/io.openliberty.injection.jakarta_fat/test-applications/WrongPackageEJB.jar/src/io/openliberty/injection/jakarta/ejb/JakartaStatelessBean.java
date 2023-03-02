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
import static org.junit.Assert.assertNull;

import jakarta.annotation.Resource;
import jakarta.ejb.EJB;
import jakarta.ejb.EJBContext;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;

/**
 * Stateless bean using jakarta package annotations, except for
 * one incorrect use of javax.annotation.Resource.
 */
@Stateless
public class JakartaStatelessBean {
    @Resource
    EJBContext ctx;

    @javax.annotation.Resource
    SessionContext sctx;

    @EJB
    JakartaSingletonBean bean;

    public void verifyInjection() {
        assertNotNull("jakarta Resrouce EJBContext is null", ctx);
        assertNull("javax Resource SessionContext is not null", sctx);
        assertNotNull("jakarta EJB Singleton is null", bean);
    }
}
