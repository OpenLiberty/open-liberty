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
package web.cfd;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.annotation.Resource;
import javax.resource.ConnectionFactoryDefinition;
import javax.resource.ConnectionFactoryDefinitions;
import javax.servlet.annotation.WebServlet;
import javax.sql.DataSource;

import componenttest.app.FATServlet;

@ConnectionFactoryDefinitions({
                                @ConnectionFactoryDefinition(
                                                             name = "java:module/env/eis/ds6-auto-close-true",
                                                             interfaceName = "javax.sql.DataSource",
                                                             resourceAdapter = "DerbyRA",
                                                             properties = "autoCloseConnections=true"),
                                @ConnectionFactoryDefinition(
                                                             name = "java:module/env/eis/ds7-auto-close-false",
                                                             interfaceName = "javax.sql.DataSource",
                                                             resourceAdapter = "DerbyRA",
                                                             properties = "autoCloseConnections=false")
})
@WebServlet(urlPatterns = "/DerbyRACFDServlet")
public class DerbyRAConnectionFactoryDefinitionServlet extends FATServlet {
    private static final long serialVersionUID = 1L;

    // Intentionally left open by test after servlet method ends. Never do this in a real application.
    private static Connection cachedConnection;

    @Resource(lookup = "java:module/env/eis/ds6-auto-close-true", shareable = false)
    private DataSource ds6;

    @Resource(lookup = "java:module/env/eis/ds7-auto-close-false", shareable = false)
    private DataSource ds7;

    /**
     * First part of a test that leaves a connection handle open across the end of a servlet request
     * and expects the container to automatically close it.
     */
    public void testConnectionFactoryDefinitionLeakConnectionWithAutoCloseEnabled() throws Throwable {
        cachedConnection = ds6.getConnection();
    }

    /**
     * First part of a test that leaves a connection handle open across the end of a servlet request
     * and expects the container to leave it open.
     */
    public void testConnectionFactoryDefinitionLeakConnectionWithAutoCloseDisabled() throws Throwable {
        cachedConnection = ds7.getConnection();
    }

    /**
     * Second part of a test that leaves a connection handle open across the end of a servlet request
     * and expects the container to automatically close it.
     */
    public void testConnectionFactoryDefinitionLeakedConnectionWithAutoCloseEnabledClosed() throws Throwable {
        Connection con = cachedConnection;
        try {
            cachedConnection = null;
            assertTrue(con.isClosed());
        } finally {
            con.close();
        }
    }

    /**
     * Second part of a test that leaves a connection handle open across the end of a servlet request
     * and expects the container to leave it open. The connection must still be usable.
     */
    public void testConnectionFactoryDefinitionLeakedConnectionWithAutoCloseDisabledNotClosed() throws Throwable {
        Connection con = cachedConnection;
        try {
            ResultSet result = con.createStatement().executeQuery("VALUES(7)");
            assertTrue(result.next());
            assertEquals(7, result.getInt(1));
            Statement st = result.getStatement();
            result.close();
            st.close();
        } finally {
            cachedConnection = null;
            con.close();
        }
    }
}