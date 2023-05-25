/*******************************************************************************
 * Copyright (c) 2020, 2023 IBM Corporation and others.
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
package commitPriority.web;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import javax.transaction.UserTransaction;

import commitPriority.common.CommitPriorityLocal;
import commitPriority.common.CommitPriorityTestUtils;
import componenttest.app.FATServlet;

/**
 * Servlet implementation class CommitOrderServlet
 */
@WebServlet("/commitPriority")
public class CommitPriorityServlet extends FATServlet {
    private static final long serialVersionUID = 1L;

    @Resource(name = "jdbc/derby7")
    DataSource ds1;

    @Resource(name = "jdbc/derby8")
    DataSource ds2;

    @Resource(name = "jdbc/derby9")
    DataSource ds3;

    @Resource
    UserTransaction ut;

    @EJB
    private CommitPriorityLocal ejb;

    @Inject
    private @Named("managedbeaninservlet") CommitPriorityLocal cdi;

    public void basicEJB(HttpServletRequest request, HttpServletResponse response) throws Exception {

        ejb.testMethod();
    }

    public void basicCDI(HttpServletRequest request, HttpServletResponse response) throws Exception {

        cdi.testMethod();
    }

    public void basicServlet(HttpServletRequest request, HttpServletResponse response) throws Exception {

        CommitPriorityTestUtils.test(ut, ds1, ds2, ds3);
    }
}