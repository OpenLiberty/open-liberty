/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.app.manager.fat.datasource.ejb;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.LocalBean;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

/**
 * Session Bean implementation class DataSourceBean
 */

@Singleton
@LocalBean
@Startup
public class DataSourceBean {

    private final static String CREATE_AUDIT_TBL_SQL = "create table AUDIT (METHOD VARCHAR(10), TIMESTAMP BIGINT)";
    private final static String DELETE_AUDIT_TBL_SQL = "delete from AUDIT";
    private final static String AUDIT_INSERT_SQL = "insert into AUDIT values (?, ?)";
    private final static String DATA_SOURCE_JNDI_NAME = "jdbc/dsfat5";

    //@Resource(name=DATA_SOURCE_JNDI_NAME)
    private DataSource dataSource;

    public DataSourceBean() {}

    @PostConstruct
    public void init() {
        System.out.println("DataSourceBean init enter");

        //DataSource dataSource;
        try {
            Context ctx = new InitialContext();
            dataSource = (DataSource) ctx.lookup(DATA_SOURCE_JNDI_NAME);
        } catch (NamingException ex) {
            System.out.println("DataSourceBean init ERROR Unable to lookup datasource");
            return;
        }

        Connection conn = null;
        try {
            conn = dataSource.getConnection();
            PreparedStatement createStmt = conn.prepareStatement(CREATE_AUDIT_TBL_SQL);
            createStmt.execute();
        } catch (SQLException ex) {
            // this could occur if the table already exists - in that case, we should clear the table
            try {
                PreparedStatement deleteStmt = conn.prepareStatement(DELETE_AUDIT_TBL_SQL);
                deleteStmt.execute();
            } catch (SQLException ex2) {
                System.out.println("DataSourceBean init ERROR - could not create nor clear AUDIT table");
                ex2.printStackTrace();
            }
            // if it is something else, then the next statement should show the same failure
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException ex) {
                }
            }
        }

        try {
            conn = dataSource.getConnection();
            PreparedStatement ps = conn.prepareStatement(AUDIT_INSERT_SQL);
            ps.setString(1, "init");
            ps.setLong(2, System.currentTimeMillis());
            int rows = ps.executeUpdate();
            System.out.println("DataSourceBean init inserted " + rows + " rows");
        } catch (SQLException ex) {
            System.out.println("DataSourceBean init failed to insert row");
            ex.printStackTrace();
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException ex) {
                }
            }
            System.out.println("DataSourceBean init exit");
        }
    }

    @PreDestroy
    public void destroy() {
        System.out.println("DataSourceBean destroy enter");

//    	new Exception("Getting Stack Trace").printStackTrace();

//    	DataSource dataSource;
//    	try {
//    		Context ctx = new InitialContext();
//    		dataSource = (DataSource) ctx.lookup(DATA_SOURCE_JNDI_NAME);
//    	} catch (NamingException ex) {
//    		System.out.println("DataSourceBean destroy ERROR Unable to lookup datasource");
//    		return;
//    	}

        Connection conn = null;
        try {
            conn = dataSource.getConnection();
            PreparedStatement ps = conn.prepareStatement(AUDIT_INSERT_SQL);
            ps.setString(1, "destroy");
            ps.setLong(2, System.currentTimeMillis());
            int rows = ps.executeUpdate();
            System.out.println("DataSourceBean destroy inserted " + rows + " rows");
        } catch (SQLException ex) {
            System.out.println("DataSourceBean destroy failed to insert row");
            ex.printStackTrace();
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException ex) {
                }
            }
            System.out.println("DataSourceBean destroy exit");
        }
    }
}
