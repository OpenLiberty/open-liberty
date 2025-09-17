/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package web;

import javax.annotation.Resource;
import javax.annotation.Resource.AuthenticationType;
import javax.naming.InitialContext;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import javax.transaction.UserTransaction;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/AuthServlet")
public class AuthServlet extends FATServlet {

    @Resource(name = "jdbc/derby", shareable = true, authenticationType = AuthenticationType.CONTAINER)
    DataSource ds;

    public void testUserTranLookup(HttpServletRequest request, HttpServletResponse response) throws Exception {
        final Object ut = new InitialContext().lookup("java:comp/UserTransaction");
        System.out.println("testUserTranLookup: Have looked up " + ut);
        if (ut instanceof javax.transaction.UserTransaction) {
            ((UserTransaction) ut).begin();
            ((UserTransaction) ut).commit();
        } else {
            if (ut == null) {
                throw new Exception("UserTransaction instance was null");
            } else {
                throw new Exception("UserTransaction lookup did not work: " + ut.getClass().getCanonicalName());
            }
        }
    }
}