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
package web;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.ejb.EJBException;
import javax.ejb.Remove;
import javax.ejb.Stateful;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

/**
 * This EJB caches a connection handle across method invocations (which are to be performed within a
 * the same transaction by the caller). The EJB remove method intentionally leaks the connection handle
 * so that we can verify that the HandleList cleans it up.
 */
@Stateful
public class DerbyConnectionCachingBean {
    private Connection con;

    public void connect() {
        try {
            DataSource ds = (DataSource) InitialContext.doLookup("eis/ds5"); // shareable
            con = ds.getConnection();
        } catch (NamingException | SQLException x) {
            throw new EJBException(x);
        }
    }

    public Integer find(String name) {
        try (Statement s = con.createStatement()) {
            ResultSet result = s.executeQuery("SELECT VAL FROM TESTTBL WHERE NAME='" + name + "'");
            return result.next() ? result.getInt(1) : null;
        } catch (SQLException x) {
            throw new EJBException(x);
        }
    }

    public Connection getCachedConnection() {
        return con;
    }

    public void insert(String name, int value) {
        try (Statement s = con.createStatement()) {
            s.executeUpdate("INSERT INTO TESTTBL VALUES('" + name + "', " + value + ")");
        } catch (SQLException x) {
            throw new EJBException(x);
        }
    }

    @Remove
    public void removeEJB() {
        System.out.println("EJB remove not closing connection handle: " + con);
    }
}
