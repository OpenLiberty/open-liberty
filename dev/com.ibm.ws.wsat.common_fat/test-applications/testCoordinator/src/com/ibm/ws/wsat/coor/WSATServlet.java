/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.wsat.coor;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLDecoder;

import javax.naming.InitialContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.UserTransaction;
import javax.xml.ws.BindingProvider;

import com.ibm.ws.wsat.coor.client.WSATTest;
import com.ibm.ws.wsat.coor.client.WSATTestService;

/*
 * Test servlet for WS-AT end-to-end test
 */
public class WSATServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    DBAccess localDB;
    WSATTest remoteDB;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        localDB = new DBAccess("CLIENT");

        String url = URLDecoder.decode(request.getParameter("remote"), "UTF-8");
        WSATTestService ws = new WSATTestService();
        remoteDB = ws.getWSATTestPort();
        ((BindingProvider) remoteDB).getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, url);

        String op = request.getParameter("op");
        String result = "";
        if (op.equals("init")) {
            initOp();
        } else if (op.equals("commit")) {
            String value = request.getParameter("value");
            updateOp(value, true);
        } else if (op.equals("rollback")) {
            String value = request.getParameter("value");
            updateOp(value, false);
        } else if (op.equals("query")) {
            result = queryOp();
        }

        PrintWriter writer = response.getWriter();
        writer.println(result);
        writer.flush();
        writer.close();
    }

    private void initOp() {
        localDB.clearValues();
        remoteDB.init();
    }

    private String queryOp() {
        String localValue = localDB.readValue();
        String remoteValue = remoteDB.query();
        return localValue + "/" + remoteValue;
    }

    private void updateOp(String value, boolean commit) throws ServletException {
        UserTransaction tx = null;
        try {
            InitialContext ctx = new InitialContext();
            tx = (UserTransaction) ctx.lookup("java:comp/UserTransaction");

            tx.begin();
            localDB.writeValue(value);
            try {
                remoteDB.set(value);
            } catch (RuntimeException e) {
                System.out.println(e); // Ignore exceptions from the webservice call itself
            }
            if (value.equals("LOCAL-FAIL")) {
                tx.setRollbackOnly();
            }

            if (commit) {
                tx.commit();
            } else {
                tx.rollback();
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}
