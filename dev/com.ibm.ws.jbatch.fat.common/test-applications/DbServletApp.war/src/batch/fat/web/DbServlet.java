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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

/**
 *
 */
@WebServlet(urlPatterns = { "/DbServlet/*" })
public class DbServlet extends HttpServlet {
	
	private HashSet<String> ignorableDerbyStates = new HashSet<String>( Arrays.asList("42Y55", "42Y07", "42X86") );
	private HashSet<Integer> ignorableDB2Codes = new HashSet<Integer>( Arrays.asList(-204) );
	private HashSet<String> ignorableDB2States = new HashSet<String>( Arrays.asList("42704") );

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
                    throws ServletException, IOException {

        log("doPost:  request.getPathInfo(): " + request.getPathInfo());
        log("doPost:  request.getContentLength(): " + request.getContentLength());
        log("doPost:  request.getQueryString(): " + request.getQueryString());

        try {
            getClass().getMethod(getTestMethod(request), HttpServletRequest.class, HttpServletResponse.class)
                            .invoke(this, request, response);

        } catch (Exception e) {
            log("doPost: ERROR! ", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().println("Unexpected Exception:" + e);
            e.printStackTrace(response.getWriter());
        }

        response.getWriter().flush();
        response.getWriter().close();
    }

    /**
     * 
     * The test method is given by the url path (minus the app+servlet context);
     * e.g: for url "/<appContext>/DbServlet/doSql", the test method is "doSql"
     * 
     * @return the test method to invoke (parsed from the URI).
     */
    protected String getTestMethod(HttpServletRequest req) {
        return req.getPathInfo().substring(1); // skip the leading "/"
    }

    /**
     * 
     */
    public void doSql(HttpServletRequest request, HttpServletResponse response) throws Exception {
        executeSqlStatements(getDataSourceConnection(request),
                             parseSqlStatements(StringUtils.read(request.getInputStream())));
    }

    public void querySql(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String queryResult = executeSqlQuery(getDataSourceConnection(request),
                                             parseSqlStatements(StringUtils.read(request.getInputStream())));
        response.getWriter().println(queryResult);
    }

    /**
     * @return a connection to the dataSource described by the request parms.
     */
    protected Connection getDataSourceConnection(HttpServletRequest request) throws SQLException, NamingException {
        log("getDataSourceConnection: from dataSourceJndi: " + getQueryParm(request, "dataSourceJndi")
            + ", dataSourceUser: " + getQueryParm(request, "dataSourceUser")
            + ", dataSourcePassword: " + getQueryParm(request, "dataSourcePassword"));

        return getDataSource(getQueryParm(request, "dataSourceJndi"))
                        .getConnection(getQueryParm(request, "dataSourceUser"),
                                       getQueryParm(request, "dataSourcePassword"));
    }

    /**
     * Gotta parse the query string myself since getParameter() won't work because
     * I'm also passing POST data.
     */
    protected String getQueryParm(HttpServletRequest request, String parmName) {

        String queryString = request.getQueryString();
        List<String> parms = StringUtils.split(queryString, "&");
        for (String parm : parms) {
            List<String> parmAndValue = StringUtils.split(parm, "=");
            if (parmAndValue.size() >= 2 && parmAndValue.get(0).equals(parmName)) {
                log("getQueryParm: " + parmName + "=" + parmAndValue.get(1));
                return parmAndValue.get(1);
            }
        }

        log("getQueryParm: " + parmName + "=null");
        return null;
    }

    /**
     * @return the DataSource with the given jndi name.
     */
    protected DataSource getDataSource(String dataSourceJndi) throws NamingException {
        log("getDataSource: from jndi: " + dataSourceJndi);
        return DataSource.class.cast(new InitialContext().lookup(dataSourceJndi));
    }

    /**
     * @return true if the SQLException is ignorable (e.g a DROP failure cuz the
     *         table doesn't exist).
     */
    protected boolean isIgnorable(SQLException sqle, Connection connection) {
    	// Pull out the Database Product Name
    	String databaseProductName = null;
    	try {
			databaseProductName = connection.getMetaData().getDatabaseProductName();
		} catch (SQLException e) {
			return false;
		}
    	
    	// Check for Derby exceptions
    	if( "apache derby".equals(databaseProductName.toLowerCase()) && 
    			ignorableDerbyStates.contains( sqle.getSQLState() ) ) {
    		return true;
    	}
    	
    	// Check for DB2 exceptions
    	//  E.g.:  Database product name : DB2/LINUXX8664
    	if( databaseProductName.toLowerCase().startsWith("db2") && 
    			ignorableDB2Codes.contains( sqle.getErrorCode() ) && 
    			ignorableDB2States.contains( sqle.getSQLState() ) ) {
    		return true;
    	}
    	
    	return false;
    }

    /**
     * Note: the connection is closed after all statements have executed.
     */
    protected void executeSqlStatements(Connection connection, List<String> sqlStatements) throws SQLException {

        try {
            for (String sql : sqlStatements) {
                if (!StringUtils.isEmpty(sql)) {
                    log("executeSqlStatements: executing statement: " + sql);

                    PreparedStatement statement = null;
                    try {
                        statement = connection.prepareStatement(sql);
                        statement.execute();
                    } catch (SQLException sqle) {

                        if (isIgnorable(sqle, connection)) {
                            log("executeSqlStatements: ignoring failure: " + sqle);
                        } else {
                            throw sqle;
                        }
                    } finally {
                        if (statement != null) {
                            statement.close();
                        }
                    }
                }
            }
        } finally {
            connection.close();
        }
    }

    protected String executeSqlQuery(Connection connection, List<String> sqlStatements) throws SQLException {
        String resultString = "";
        ResultSet result = null;
        try {
            for (String sql : sqlStatements) {
                if (!StringUtils.isEmpty(sql)) {
                    log("executeSqlQuery: executing query: " + sql);

                    PreparedStatement statement = connection.prepareStatement(sql);
                    result = statement.executeQuery();
                    if (result != null) {
                        resultString = resultString.concat(readResultSet(result));
                    }
                    statement.close();
                }
            }
        } finally {
            connection.close();
        }
        return resultString;
    }

    private String readResultSet(ResultSet result) throws SQLException {
        StringBuilder sb = new StringBuilder();
        int columns = result.getMetaData().getColumnCount();
        int curColumn;
        String delim = "";
        while (result.next()) {
            sb.append(delim);
            sb.append("[");
            String sep = "";
            for (curColumn = 1; curColumn <= columns; curColumn++) {
                sb.append(sep);
                sb.append(result.getString(curColumn));
                sep = "|";
            }
            sb.append("]");
            delim = ",";
        }
        return sb.toString();
    }

    /**
     * Parse the bulk sql data for sql statements, which are delimited by ';'.
     * 
     * @return the sql statements parsed from the given bulk sql data.
     */
    protected List<String> parseSqlStatements(List<String> bulkSqlData) throws IOException {
        if (bulkSqlData.isEmpty()) {
            throw new IOException("Failed to read SQL data from request");
        }
        log("parseSqlStatements: from bulkSqlData: " + bulkSqlData);
        return StringUtils.split(StringUtils.join(bulkSqlData, ""), ";");
    }

}
