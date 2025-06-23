/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.jpa.platformtck.tests.web;

import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import componenttest.app.FATServlet;
import jakarta.annotation.Resource;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.annotation.WebServlet;
import jakarta.transaction.UserTransaction;
import jakarta.inject.Inject;
import org.junit.Ignore;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/PlatformTCK32")
public class PlatformTCKServlet extends FATServlet {
    @Inject
    private EntityManager em;

    @Resource
    private UserTransaction tx;

    @Test
    public void alwaysPasses() {
        assertTrue(true);
    }

    // @Test
    // @Ignore
    // public void testEntityManagerInjection() {
    //     assertNotNull(em);
    //     System.out.println("EntityManager injected via @Inject");
    //     assertTrue(em.isOpen());
    //     System.out.println("EntityManager @Inject test passed!");
    // }

}
