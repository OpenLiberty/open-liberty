/*******************************************************************************
 * Copyright (c) 1997, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsp.tsx.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;

import com.ibm.ws.jsp.JspCoreException;

/**
 * This type was created in VisualAge.
 */
public class Query {

    private static final String icClass = "com.ibm.websphere.naming.WsnInitialContextFactory";
    private static final String icProvider = "iiop:///";
    private static Hashtable datasources = new Hashtable();
    private static InitialContext ic = null;

    private String queryString = null;
    private QueryResults results = null;
    private ConnectionProperties connProperties = null;
    private int maxRows = -1;

    /*
     * This method was created in VisualAge.
     * @param qs java.lang.String
     * @param qr com.ibm.websphere.jsp.QueryResults
     */
    protected Query() {
        // do nothing   
    }
    /**
     * This method was created in VisualAge.
     * @param cp com.ibm.websphere.jsp.ConnectionProperties
     */
    public Query(ConnectionProperties cp, String queryString) throws JspCoreException {
        setConnProperties(cp);
        setQueryString(queryString);
    }
    /**
     * This method was created in VisualAge.
     * @return com.ibm.websphere.jsp.QueryResults
     */
    public QueryResults execute() throws JspCoreException, SQLException {

        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        ;
        QueryResults qs = null;
        // verify all parameters
        verify();

        // invoke the compute method of QueryResults passing it the SQL resultset
        // the QueryResults will get all the data from the result set and cache it in
        // its private datastructures
        try {
            conn = getJdbcConnection();
            //conn = DriverManager.getConnection(url,user,passwd);
            stmt = conn.createStatement();
            int mrows = getMaxRows();
            if (mrows > 0)
                stmt.setMaxRows(mrows);
            rs = stmt.executeQuery(getQueryString());
            qs = new QueryResults();
            qs.compute(rs);
        }
        catch (SQLException e) {
            //com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.webcontainer.jsp.tsx.db.Query.execute", "73", this);
            throw e;
        }
        finally { // always free up database resources
            if (rs != null)
                rs.close();
            if (stmt != null)
                stmt.close();
            if (conn != null)
                conn.close();
        }
        setResults(qs);
        return qs;
    }

    public void executeUpdate() throws JspCoreException, SQLException {

        Connection conn = null;
        Statement stmt = null;
        // verify all parameters
        verify();

        try {
            //conn = DriverManager.getConnection(url,user,passwd);
            conn = getJdbcConnection();
            stmt = conn.createStatement();
            stmt.executeUpdate(getQueryString());
        }
        catch (SQLException e) {
            //com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.webcontainer.jsp.tsx.db.Query.executeUpdate", "101", this);
            throw e;
        }
        finally { // always free up database resources
            if (stmt != null)
                stmt.close();
            if (conn != null)
                conn.close();
        }
    } /**
         * This method was created in VisualAge.
         * @return com.ibm.websphere.jsp.ConnectionProperties
         */
    protected ConnectionProperties getConnProperties() {
        return connProperties;
    }
    /**
     * This method was created in VisualAge.
     * @return java.lang.String
     */
    protected String getQueryString() {
        return queryString;
    }
    /**
     * This method was created in VisualAge.
     * @return com.ibm.websphere.jsp.QueryResults
     */
    protected QueryResults getResults() {
        return results;
    }

    /**
     * This method was created in VisualAge.
     * @param newValue int
     */
    public void setMaxRows(int newValue) {
        if (newValue > 0)
            this.maxRows = newValue;
    }

    /**
     * This method was created in VisualAge.
     * @return int
     */
    protected int getMaxRows() {
        return this.maxRows;
    }

    /** Return a valid jdbc connection.
     *  if jndiName is specified then assume that we need to use a datasource 
     *    else use a drivermanager
     */
    private Connection getJdbcConnection() throws JspCoreException, SQLException {

        Connection conn = null;

        String url = getConnProperties().getUrl();
        String user = getConnProperties().getLoginUser();
        String passwd = getConnProperties().getLoginPasswd();
        String jndiName = getConnProperties().getJndiName();

        //System.out.println("***** Query.getJdbcConnection():  jndiname = " + jndiName);

        if (jndiName == null) { // use driver manager
            // load driver
            String dbDriver = (getConnProperties()).getDbDriver();
            try {
                Class.forName(dbDriver);
                conn = DriverManager.getConnection(url, user, passwd);
            } // try load driver
            catch (ClassNotFoundException e) {
                //com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.webcontainer.jsp.tsx.db.Query.getJdbcConnection", "174", this);
                throw new JspCoreException(JspConstants.InvalidDbDriver + dbDriver);
            } // catch
        } // if jndiname == null
        else { // use datasource
            DataSource ds;
            try {
                ds = getSingleton(jndiName);
            }
            catch (Throwable th) {
                //com.ibm.ws.ffdc.FFDCFilter.processException(th, "com.ibm.ws.webcontainer.jsp.tsx.db.Query.getJdbcConnection", "184", this);
                throw new JspCoreException(JspConstants.DatasourceException + th.getMessage());
            }
            conn = ds.getConnection(user, passwd);
        }

        return conn;
    }

    /*=========================================================================
     *      Private Utility functions
     *
     *======================================================================== */

    /** 
    */
    private static DataSource getSingleton(String datasourceName) throws JspCoreException {
        DataSource ds = (DataSource) datasources.get(datasourceName);
        if (ds != null) {
            return ds;
        }

        ds = findDataSource(datasourceName);

        return ds;
    }

    /** 
    */
    private static synchronized DataSource findDataSource(String datasourceName) throws JspCoreException {
        DataSource ds = (DataSource) datasources.get(datasourceName);
        if (ds != null) {
            //System.out.println("Datasource " + datasourceName + " found in cache");
            return ds;
        }
        InitialContext ic = getSingletonContext();
        DataSource dataSource = null;
        try {
            dataSource = (DataSource) ic.lookup(datasourceName);
        }
        catch (Throwable th) {
            throw new JspCoreException("Error looking up DataSource " + dataSource + " " + th.getMessage());
        }
        datasources.put(datasourceName, dataSource);
        //System.out.println("Datasource " + datasourceName + " put in cache");
        return dataSource;
    }

    /** 
    */
    private static InitialContext getSingletonContext() throws JspCoreException {
        if (ic == null) {
            getInitialContext();
        }
        return ic;
    }

    private static synchronized InitialContext getInitialContext() throws JspCoreException
    {
        if (ic != null) {
            return ic;
        }
        InitialContext initialContext = null;
//PQ84248 part 1 begin
        try {
        	initialContext = new InitialContext();
        }
        catch (Throwable th) {
        }

	 if (initialContext == null) { 
//PQ84248 part 1 end 
        Properties p = new Properties();
        p.put (Context.INITIAL_CONTEXT_FACTORY, icClass);
        p.put (Context.PROVIDER_URL,            icProvider);
        try {
            initialContext = new InitialContext(p);
            if (initialContext == null) {
                throw new JspCoreException("null pointer returned for InitalContext");
            }
        }
        catch (Throwable th) {
        	com.ibm.ws.ffdc.FFDCFilter.processException(th, "com.ibm.ws.webcontainer.jsp.tsx.db.Query.getInitialContext", "266");
            throw new JspCoreException("Error getting InitialContext, " + th.getMessage());
        }
       //PQ84248 part 2 begin 
       }
       //PQ84248 part 2 end
        ic = initialContext;
        return ic;
    }

    /**
    * This method was created in VisualAge.
    * @param args java.lang.String[]
    */
    public static void main(String args[]) {
        String dbDriver = "COM.ibm.db2.jdbc.app.DB2Driver";
        String url = "jdbc:db2:sample";
        String user = "Murali";
        String pass = "ibm";
        String querystring = "Select * from Murali.department";

        try {
            ConnectionProperties cp = new ConnectionProperties(dbDriver, url, user, pass);
            Query query = new Query(cp, querystring);
            QueryResults qs = query.execute();
            System.out.println("Number of rows = " + qs.size());
            for (int i = 0; i < qs.size(); i++) {
                String dept = qs.getValue("DEPTNAME", i);
                System.out.println("Department:" + dept);
            }
            /*Enumeration enum= qs.getRows();
            while (enum.hasMoreElements())
              {
              QueryRow qr = (QueryRow)enum.nextElement();
              String fn = qr.getValue("DEPT");
              String ln = qr.getValue("DEPTNAME");
            //  String bd = qr.getValue("BIRTHDATE");
              //String sal = qr.getValue("SALARY");
              System.out.println(fn + " " + ln);
            
              }*/ // while
        } // try
        catch (Exception e) {
            //com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.webcontainer.jsp.tsx.db.Query.main", "310");
            System.out.println("Exception:: " + e.getMessage());
        } //catch
    } //main
    /**
     * This method was created in VisualAge.
     * @param args java.lang.String[]
     */
    public static void main2(String args[]) {
        String dbDriver = "COM.ibm.db2.jdbc.app.DB2Driver";
        String url = "jdbc:db2:sample";
        String user = "batra";
        String pass = "varunbatra";
        String querystring = "Select * from batra.employee";
        String fn;
        String ln;
        String bd;
        String sal;

        try {
            ConnectionProperties cp = new ConnectionProperties(dbDriver, url, user, pass);
            Query query = new Query(cp, querystring);
            QueryResults qs = query.execute();
            System.out.println("Number of rows = " + qs.size());
            while (qs.next()) {
                fn = qs.getValue("FIRSTNME");
                ln = qs.getValue("LASTNAME");
                bd = qs.getValue("BIRTHDATE");
                sal = qs.getValue("SALARY");
                System.out.println(fn + " " + ln + " birthdate " + bd + " salary " + sal);
            }
        }
        catch (Exception e) {
            //com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.webcontainer.jsp.tsx.db.Query.main2", "348");
            System.out.println("Exception: " + e.getMessage());
        }
        System.out.println("All is Fine!");
    }
    /**
     * This method was created in VisualAge.
     * @param args java.lang.String[]
     */
    public static void main3(String args[]) {
        String dbDriver = "COM.ibm.db2.jdbc.app.DB2Driver";
        String url = "jdbc:db2:sample";
        String user = "batra";
        String pass = "varunbatra";
        String querystring = "Select * from batra.employee";
        String fn;
        String ln;
        String bd;
        String sal;

        try {
            ConnectionProperties cp = new ConnectionProperties(dbDriver, url, user, pass);
            Query query = new Query(cp, querystring);
            QueryResults qs = query.execute();
            System.out.println("Number of rows = " + qs.size());

            Enumeration e = qs.getRows();
            while (e.hasMoreElements()) {
                QueryRow qr = (QueryRow) e.nextElement();
                fn = qr.getValue("FIRSTNME");
                ln = qr.getValue("LASTNAME");
                bd = qr.getValue("BIRTHDATE");
                sal = qr.getValue("SALARY");
                System.out.println(fn + " " + ln + " birthdate " + bd + " salary " + sal);
            } // while

            /*
              while (qs.next())
                {
                fn = qs.getValue("FIRSTNME");
                ln = qs.getValue("LASTNAME");
                bd = qs.getValue("BIRTHDATE");
                sal = qs.getValue("SALARY");
                System.out.println(fn + " " + ln +
                                   " birthdate " + bd +
                                   " salary " + sal);
                }
                
              qs.setCurrRow(5);
              fn = qs.getValue("FIRSTNME");
              ln = qs.getValue("LASTNAME");
              bd = qs.getValue("BIRTHDATE");
              sal = qs.getValue("SALARY");
              System.out.println(fn + " " + ln +
                                   " birthdate " + bd +
                                   " salary " + sal);
            */

        } // try
        catch (Exception e) {
            //com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.webcontainer.jsp.tsx.db.Query.main3", "414");
            System.out.println("Exception:: " + e.getMessage());
        }
        System.out.println("All is Fine!!!!!");
    }
    /**
     * This method was created in VisualAge.
     * @param args java.lang.String[]
     */
    public static void mainold(String args[]) {
        String dbDriver = "COM.ibm.db2.jdbc.app.DB2Driver";
        String url = "jdbc:db2:sample";
        String user = "Murali";
        String pass = "ibm";
        String querystring = "Select FIRSTNME, LASTNAME, BIRTHDATE, SALARY from Murali.employee";

        try {
            ConnectionProperties cp = new ConnectionProperties(dbDriver, url, user, pass);
            Query query = new Query(cp, querystring);
            QueryResults qs = query.execute();
            System.out.println("Number of rows = " + qs.size());
            Enumeration e = qs.getRows();
            while (e.hasMoreElements()) {
                QueryRow qr = (QueryRow) e.nextElement();
                String fn = qr.getValue("FIRSTNME");
                String ln = qr.getValue("LASTNAME");
                String bd = qr.getValue("BIRTHDATE");
                String sal = qr.getValue("SALARY");
                System.out.println(fn + " " + ln + " birthdate " + bd + " salary " + sal);
            } // while
        } // try
        catch (Exception e) {
            //com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.webcontainer.jsp.tsx.db.Query.mainold", "450");
            System.out.println("Exception:: " + e.getMessage());
        } //catch
    } //main
    /**
     * This method was created in VisualAge.
     * @param newValue com.ibm.websphere.jsp.ConnectionProperties
     */
    public void setConnProperties(ConnectionProperties newValue) {
        this.connProperties = newValue;
    }
    /**
     * This method was created in VisualAge.
     * @param newValue java.lang.String
     */
    protected void setQueryString(String newValue) throws JspCoreException {
        if (newValue == null) {
            throw new JspCoreException(JspConstants.NullQueryString);
        }

        // fix query string i.e remove tabs and special characters
        // convert all whitespace to space
        int len = newValue.length();
        StringBuffer buff = new StringBuffer(len);
        for (int i = 0; i < len; i++) {
            char c = newValue.charAt(i);
            if (Character.isWhitespace(c)) {
                buff.append(" ");
            } // if
            else {
                buff.append(c);
            } //else
        } // for
        this.queryString = buff.toString().trim();
    }
    /**
     * This method was created in VisualAge.
     * @param newValue com.ibm.websphere.jsp.QueryResults
     */
    protected void setResults(QueryResults newValue) {
        this.results = newValue;
    }
    /**
     * This method was created in VisualAge.
     */
    protected void verify() throws JspCoreException {
        // do any more verification that may be necessary
    }
}
