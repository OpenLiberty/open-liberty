/*******************************************************************************
 * Copyright (c) 2018, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package jpahelper.databasemanagement;

import java.io.IOException;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import javax.transaction.UserTransaction;

import com.ibm.ws.testtooling.vehicle.web.AbstractDatabaseManagementServlet;

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
