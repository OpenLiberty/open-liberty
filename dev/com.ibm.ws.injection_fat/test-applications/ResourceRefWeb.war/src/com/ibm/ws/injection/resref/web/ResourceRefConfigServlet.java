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
package com.ibm.ws.injection.resref.web;

import java.sql.Connection;
import java.sql.DatabaseMetaData;

import javax.annotation.Resource;
import javax.servlet.annotation.WebServlet;
import javax.sql.DataSource;

import org.junit.Test;

import componenttest.app.FATServlet;
import junit.framework.Assert;

@WebServlet("/ResourceRefConfigServlet")
public class ResourceRefConfigServlet extends FATServlet {
    private static final long serialVersionUID = 1L;

    // no lookup name, no jndi-binding-name, no binding config
    @Resource(name = "jdbc/TestDSNoAuthAlias")
    DataSource dsNoAuthAlias;

    // no lookup name, no jndi-binding-name, binding config with normalized jndi name
    @Resource(name = "jdbc/TestDSAuthAliasWithNormalizedJndiBinding")
    DataSource dsAuthAliasWithNormalizedJndiBinding;

    // no lookup name, no jndi-binding-name, binding config with denormalized jndi name
    @Resource(name = "jdbc/TestDSAuthAliasWithDenormalizedJndiBinding")
    DataSource dsAuthAliasWithDenormalizedJndiBinding;

    // lookup name, no jndi-binding-name, no binding config
    @Resource(name = "jdbc/TestDSNoAuthAliasWithLookup", lookup = "jdbc/TestDataSource")
    DataSource dsNoAuthAliasWithLookup;

    // lookup name, no jndi-binding-name, binding config with normalized jndi name
    @Resource(name = "jdbc/TestDSAuthAliasWithLookupWithNormalizedJndiBinding", lookup = "jdbc/TestDataSource")
    DataSource dsAuthAliasWithLookupWithNormalizedJndiBinding;

    // lookup name, no jndi-binding-name, binding config with denormalized jndi name
    @Resource(name = "jdbc/TestDSAuthAliasWithLookupWithDenormalizedJndiBinding", lookup = "jdbc/TestDataSource")
    DataSource dsAuthAliasWithLookupWithDenormalizedJndiBinding;

    /**
     * Tests injection of a DataSource that has no lookup name, no jndi-binding-name,
     * and no binding config with authentication-alias
     */
    @Test
    public void testDataSourceWithNoAuthAliasInjection() throws Exception {
        Connection con = dsNoAuthAlias.getConnection();
        try {
            DatabaseMetaData metadata = con.getMetaData();
            String user = metadata.getUserName();
            Assert.assertEquals("Unexpected user from datasource without authentication-alias", "APP", user);
        } finally {
            con.close();
        }
    }

    /**
     * Tests injection of a DataSource that has no lookup name, no jndi-binding-name,
     * and a normalized JNDI name binding config with authentication-alias
     */
    @Test
    public void testDataSourceWithAuthAliasWithNormalizedInjection() throws Exception {
        Connection con = dsAuthAliasWithNormalizedJndiBinding.getConnection();
        try {
            DatabaseMetaData metadata = con.getMetaData();
            String user = metadata.getUserName();
            Assert.assertEquals("Unexpected user from datasource with authentication-alias", "dbuser1", user);
        } finally {
            con.close();
        }
    }

    /**
     * Tests injection of a DataSource that has no lookup name, no jndi-binding-name,
     * and a denormalized JNDI name binding config with authentication-alias
     */
    @Test
    public void testDataSourceWithAuthAliasWithDenormalizedInjection() throws Exception {
        Connection con = dsAuthAliasWithDenormalizedJndiBinding.getConnection();
        try {
            DatabaseMetaData metadata = con.getMetaData();
            String user = metadata.getUserName();
            Assert.assertEquals("Unexpected user from datasource with authentication-alias", "dbuser2", user);
        } finally {
            con.close();
        }
    }

    /**
     * Tests injection of a DataSource that has a lookup name, no jndi-binding-name,
     * and no binding config with authentication-alias
     */
    @Test
    public void testDataSourceNoAuthAliasWithLookupInjection() throws Exception {
        Connection con = dsNoAuthAliasWithLookup.getConnection();
        try {
            DatabaseMetaData metadata = con.getMetaData();
            String user = metadata.getUserName();
            Assert.assertEquals("Unexpected user from datasource without authentication-alias", "APP", user);
        } finally {
            con.close();
        }
    }

    /**
     * Tests injection of a DataSource that has a lookup name, no jndi-binding-name,
     * and a normalized JNDI name binding config with authentication-alias
     */
    @Test
    public void testDataSourceWithAuthAliasWithLookupWithNormalizedInjection() throws Exception {
        Connection con = dsAuthAliasWithLookupWithNormalizedJndiBinding.getConnection();
        try {
            DatabaseMetaData metadata = con.getMetaData();
            String user = metadata.getUserName();
            Assert.assertEquals("Unexpected user from datasource with authentication-alias", "dbuser1", user);
        } finally {
            con.close();
        }
    }

    /**
     * Tests injection of a DataSource that has a lookup name, no jndi-binding-name,
     * and a denormalized JNDI name binding config with authentication-alias
     */
    @Test
    public void testDataSourceWithAuthAliasWithLookupWithDenormalizedInjection() throws Exception {
        Connection con = dsAuthAliasWithLookupWithDenormalizedJndiBinding.getConnection();
        try {
            DatabaseMetaData metadata = con.getMetaData();
            String user = metadata.getUserName();
            Assert.assertEquals("Unexpected user from datasource with authentication-alias", "dbuser2", user);
        } finally {
            con.close();
        }
    }
}