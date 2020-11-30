/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   IBM Corporation - initial API and implementation
 *******************************************************************************/
package batch.fat.web;

import java.io.IOException;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Note: make sure to create the tables needed for the Chunk tests.
 * see BatchFATHelper.java and ChunkTest.java
 */
@WebServlet(name = "Chunk", urlPatterns = { "/Chunk" })
public class ChunkServlet extends SelfValidatingJobServlet {

    /**
     * @return the datasource jndi to use for the app's INTABLE/OUT* tables
     */
    protected String getDataSourceJndi(HttpServletRequest req) {

        if (Boolean.parseBoolean(req.getParameter("sharedDB"))) {
            return "jdbc/batch"; // TODO - should be a better way to reflect this name (injection).
        } else {
            String dataSourceJndi = req.getParameter("dataSourceJndi");
            if (dataSourceJndi != null && dataSourceJndi.trim().length() > 0) {
                return dataSourceJndi;
            } else {
                return "jdbc/myds";
            }
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                    throws ServletException, IOException {

        String writeTableName = req.getParameter("writeTable");

        Properties jslParms = new Properties();
        jslParms.setProperty("writeTableProp", writeTableName);
        jslParms.setProperty("dsjndi", getDataSourceJndi(req));

        req.setAttribute(jslParmsAttr, jslParms);
        super.doGet(req, resp);
    }

}
