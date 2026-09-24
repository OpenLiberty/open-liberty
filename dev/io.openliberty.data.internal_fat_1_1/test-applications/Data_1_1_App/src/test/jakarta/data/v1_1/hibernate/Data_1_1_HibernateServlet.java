/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package test.jakarta.data.v1_1.hibernate;

import static org.junit.Assert.assertEquals;

import jakarta.annotation.Resource;
import jakarta.inject.Inject;
import jakarta.persistence.EntityAgent;
import jakarta.servlet.annotation.WebServlet;
import jakarta.transaction.UserTransaction;

import org.junit.Test;

import componenttest.app.FATServlet;
import test.jakarta.data.v1_1.web.Fractions;

@SuppressWarnings("serial")
@WebServlet("/Hibernate/*")
public class Data_1_1_HibernateServlet extends FATServlet {

    @Inject
    Fractions fractions;

    @Inject
    ResourceAccess resourceAccessor;

    @Resource
    UserTransaction tx;

    /**
     * Use a resource accessor method to obtain an EntityAgent from a stateless
     * repository. Use the EntityAgent to perform a query and assert that the
     * expected results are returned.
     */
    @Test
    public void testEntityAgentAccessor() {

        EntityAgent agent = resourceAccessor.agent();
        assertEquals(true,
                     agent.isOpen());

        assertEquals(true,
                     agent.isOpen());
        agent.close();
        assertEquals(false,
                     agent.isOpen());
    }

    /**
     * Use a default method that accesses a resource accessor method to obtain
     * an EntityAgent from a stateless repository. Use the EntityAgent to
     * perform a query and assert that the expected results are returned.
     */
    @Test
    public void testEntityAgentUsedByDefaultMethod() {

    }

}
