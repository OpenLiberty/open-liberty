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
package jdbc.fat.v43.web;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;

import javax.servlet.ServletException;
import javax.servlet.SingleThreadModel;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import org.junit.Test;

import componenttest.annotation.ExpectedFFDC;
import componenttest.app.FATServlet;

@SuppressWarnings({ "serial", "deprecation" })
@WebServlet(urlPatterns = "/HandleListTestServlet")
public class HandleListTestServlet extends FATServlet implements SingleThreadModel {
    // SingleThreadModel prevents resource injection, and JNDI lookups aren't enabled, so we use these static
    // fields to store the data sources that were injected into the JDBC43TestServlet,
    static DataSource unsharablePool1DataSource;
    static DataSource unsharablePool2DataSource;

    // Asks the JDBC43TestServlet to populate the above fields
    private void populateDataSources(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        getServletContext()
                        .getRequestDispatcher("/JDBC43TestServlet?testMethod=populateDataSources")
                        .include(request, response);
    }

    /**
     * When HandleList is not enabled, an unshared connection cannot be used for another request which happens inline on the same thread.
     */
    @ExpectedFFDC("com.ibm.websphere.ce.j2c.ConnectionWaitTimeoutException")
    @Test
    public void testUnsharedConnectionNotReassociatedAcrossServletRequests(HttpServletRequest request, HttpServletResponse response) throws Exception {
        populateDataSources(request, response);

        Connection con = unsharablePool1DataSource.getConnection();
        try {
            PreparedStatement ps = con.prepareStatement("INSERT INTO STREETS VALUES(?, ?, ?)");
            ps.setString(1, "Hidden Hills Lane NE");
            ps.setString(2, "Rochester");
            ps.setString(3, "MN");
            ps.executeUpdate();

            // There is only one managed connection for the whole pool.
            // See if an inline servlet request can use it.
            // This would require parking the current handle before the inline servlet request and reassociating it back afterward.
            getServletContext()
                            .getRequestDispatcher("/JDBC43TestServlet?testMethod=testUnsharedConnectionNotReassociatedAcrossServletRequestsInnerRequest")
                            .include(request, response);

            ps.setString(1, "Haverhill Road NE");
            ps.executeUpdate();
        } finally {
            con.close();
        }
    }

    /**
     * When HandleList is enabled, an unshared connection can be used for another request which happens inline on the same thread.
     */
    // @Test TODO this will never work because webcontainer doesn't attempt a postInvoke/park before switching to the inline servlet request
    public void testUnsharedConnectionReassociatedAcrossServletRequests(HttpServletRequest request, HttpServletResponse response) throws Exception {
        // Use up both connections from the pool

        Connection con1 = unsharablePool2DataSource.getConnection();
        PreparedStatement ps1 = con1.prepareStatement("INSERT INTO STREETS VALUES(?, ?, ?)");
        ps1.setString(1, "Sunnydale Lane SE");
        ps1.setString(2, "Rochester");
        ps1.setString(3, "MN");
        ps1.executeUpdate();

        Connection con2 = unsharablePool2DataSource.getConnection();
        PreparedStatement ps2 = con1.prepareStatement("INSERT INTO STREETS VALUES(?, ?, ?)");
        ps2.setString(1, "Strathmore Lane SE");
        ps2.setString(2, "Rochester");
        ps2.setString(3, "MN");
        ps2.executeUpdate();

        // There are only two managed connection for the whole pool, both used above.
        // See if an inline servlet request can use them.
        // This would require parking the current handles before the inline servlet request and reassociating them back afterward.
        getServletContext()
                        .getRequestDispatcher("/JDBC43TestServlet?testMethod=testUnsharedConnectionReassociatedAcrossServletRequestsInnerRequest")
                        .include(request, response);

        // Connections must continue to be usable afterward
        ps1.setString(1, "Rose Drive SE");
        ps1.executeUpdate();
        ps2.setString(1, "College Drive SE");
        ps2.executeUpdate();

        con1.close();
        con2.close();
    }
}
