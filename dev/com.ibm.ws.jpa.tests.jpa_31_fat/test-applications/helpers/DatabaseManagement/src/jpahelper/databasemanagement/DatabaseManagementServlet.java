/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
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

package jpahelper.databasemanagement;

import java.io.IOException;

import javax.sql.DataSource;

import com.ibm.ws.testtooling.vehicle.web.AbstractDatabaseManagementServlet;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.UserTransaction;

@WebServlet(urlPatterns = { "/DMS" })
public class DatabaseManagementServlet extends AbstractDatabaseManagementServlet {
    @Resource
    private UserTransaction tx;

    @Resource(lookup = "jdbc/JPA_JTA_DS")
    private DataSource dsJta;

    @Resource(lookup = "jdbc/JPA_NJTA_DS")
    private DataSource dsRl;

    @PostConstruct
    private void initialise() {
        try {
            System.out.println("Initializing DatabaseManagementServlet");
            getInfo(this.dsRl);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void execRequest(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        final ClassLoader cl = DatabaseManagementServlet.class.getClassLoader();
        execRequest(this.dsJta, this.tx, cl, req, resp);
    }
}
