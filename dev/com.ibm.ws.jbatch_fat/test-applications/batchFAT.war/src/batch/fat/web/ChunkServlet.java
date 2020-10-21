/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2014
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
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
