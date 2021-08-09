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
package com.ibm.ws.wsat.part.service;

import javax.jws.WebMethod;
import javax.jws.WebService;
import javax.naming.InitialContext;
import javax.transaction.UserTransaction;

/*
 * Simple web service to query and update a database. 
 */
@WebService(wsdlLocation = "WEB-INF/wsdl/WSATTestService.wsdl")
public class WSATTest {

    DBAccess localDB = new DBAccess("SERVER");

    @WebMethod
    public void init() {
        localDB.clearValues();
    }

    @WebMethod
    public String query() {
        return localDB.readValue();
    }

    @WebMethod
    public void set(String value) {
        localDB.writeValue(value);
        if (value.equals("REMOTE-FAIL")) {
            try {
                InitialContext ctx = new InitialContext();
                UserTransaction tx = (UserTransaction) ctx.lookup("java:comp/UserTransaction");
                tx.setRollbackOnly();
            } catch (Exception e) {
                System.out.println(e);
            }
        }
        if (value.equals("REMOTE-EX")) {
            throw new RuntimeException("Remote Exception");
        }

    }
}
