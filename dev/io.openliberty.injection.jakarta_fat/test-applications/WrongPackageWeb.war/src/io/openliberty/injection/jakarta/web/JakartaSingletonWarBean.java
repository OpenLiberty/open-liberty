/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.injection.jakarta.web;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import jakarta.annotation.Resource;
import jakarta.ejb.EJB;
import jakarta.ejb.EJBContext;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Singleton;

/**
 * Singleton bean packaged in a WAR module using jakarta package annotations,
 * except for one incorrect use of javax.annotation.Resource.
 */
@Singleton
public class JakartaSingletonWarBean {
    @Resource
    EJBContext ctx;

    @javax.annotation.Resource
    SessionContext sctx;

    @EJB
    JakartaStatelessWarBean bean;

    public void verifyInjection() {
        assertNotNull("jakarta Resrouce EJBContext is null", ctx);
        assertNull("javax Resource SessionContext is not null", sctx);
        assertNotNull("jakarta EJB Stateless is null", bean);
    }
}
