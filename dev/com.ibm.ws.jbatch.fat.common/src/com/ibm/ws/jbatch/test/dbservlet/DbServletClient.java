package com.ibm.ws.jbatch.test.dbservlet;


import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

/**
 * For making calls into the DbServletApp.
 * 
 * For example, to build the batch runtime DB (assuming the DDL is located 
 * in publish/files/batch-derby.ddl):
 * 
 * new DbServletClient()
 *          .setDataSourceJndi("jdbc/batch")
 *          .setDataSourceUser("user", "pass")
 *          .setHostAndPort(server.getHostname(), server.getHttpDefaultPort())
 *          .loadSql(server.pathToAutoFVTTestFiles + "batch-derby.ddl", "JBATCH", "")
 *          .executeUpdate();
 */
public class DbServletClient {

    private static final Logger logger = Logger.getLogger(DbServletClient.class.getName());
    
    /**
     * helper for simple logging.
     */
    private static void log(String method, String msg) {
        logger.info(method + ": " + msg);
    }

    /**
     * Datasource props.
     */
    private String dataSourceJndi;
    private String dataSourceUser;
    private String dataSourcePassword;

    /**
     * The target server's host:port
     */
    private String hostAndPort;

    /**
     * The SQL to execute.
     */
    private String sql;

    /**
     * @param dataSourceJndi The jndi name of the datasource that represents the DB.
     * 
     * @return this
     */
    public DbServletClient setDataSourceJndi(String dataSourceJndi) {
        this.dataSourceJndi = dataSourceJndi;
        return this;
    }

    /**
     * @param dataSourceUser The user that will access the datasource
     * @param dataSourcePassword The user's password
     * 
     * @return this
     */
    public DbServletClient setDataSourceUser(String dataSourceUser, String dataSourcePassword) {
        this.dataSourceUser = dataSourceUser;
        this.dataSourcePassword = dataSourcePassword;
        return this;
    }
    
    /**
     * @param host the target server's http host
     * @param port the target server's http port
     * 
     * @return this
     */
    public DbServletClient setHostAndPort(String host, int port) {
        this.hostAndPort = host + ":" + port;
        return this;
    }
    
    /**
     * @return the target server's http host:port
     */
    public String getHostAndPort() {
        return hostAndPort;
    }

    /**
     * @param sql
     * 
     * @return this
     */
    public DbServletClient setSql(String sql) {
        this.sql = sql;
        return this;
    }

    /**
     * @param sql
     * @param schema Substituted for {schema} in sql
     * @param tablePrefix Substituted for {tablePrefix} in sql
     * 
     * @return this
     */
    public DbServletClient setSql(String sql, String schema, String tablePrefix) {
        return setSql(resolveSql(sql, schema, tablePrefix));
    }

    /**
     * Load the SQL from the given file and substitute {schema} and {tablePrefix}
     * with the given values.
     * 
     * @param sqlFileName contains SQL statements
     * @param schema for resolving {schema}
     * @param tablePrefix for resolving {tablePrefix}
     * 
     * @return the sql
     */
    public DbServletClient loadSql(String sqlFileName,
                                   String schema,
                                   String tablePrefix) throws IOException {

        this.sql = loadSql(new File(sqlFileName),
                           schema,
                           tablePrefix);

        return this;
    }

    /**
     * @return a URL for running updates
     */
    protected URL buildUpdateUrl() throws IOException {
        StringBuilder sb = new StringBuilder("http://" + getHostAndPort()
                       + "/DbServletApp"
                       + "/DbServlet"
                       + "/doSql"
                       + "?dataSourceJndi=" + dataSourceJndi);
        if (dataSourceUser != null) {
        	sb.append("&dataSourceUser=" + dataSourceUser
                       + "&dataSourcePassword=" + dataSourcePassword);
        }
        return new URL(sb.toString());
    }

    /**
     * @return a URL for running queries
     */
    protected URL buildQueryUrl() throws IOException {
    	StringBuilder sb = new StringBuilder("http://" + getHostAndPort()
                       + "/DbServletApp"
                       + "/DbServlet"
                       + "/querySql"
                       + "?dataSourceJndi=" + dataSourceJndi);
        if (dataSourceUser != null) {
        	sb.append("&dataSourceUser=" + dataSourceUser
                       + "&dataSourcePassword=" + dataSourcePassword);
        }
        return new URL(sb.toString());
    }

    /**
     * Execute the SQL as an update statement.
     * 
     * @return the HttpURLConnection
     */
    public HttpURLConnection executeUpdate() throws IOException {
        return HttpUtils.getHttpConnection("POST", buildUpdateUrl(), sql);
    }

    /**
     * Execute the SQL as a query statement.
     * 
     * @return the query results.
     */
    public String executeQuery() throws IOException {
        HttpURLConnection conn = HttpUtils.getHttpConnection("GET", buildQueryUrl(), sql);

        String retMe = StringUtils.join(StringUtils.readLines(conn.getInputStream()), System.getProperty("line.separator"));
        log("executeQuery", "results: " + retMe);
        return retMe;
    }

    /**
     * Load the SQL from the given file and substitute {schema} and {tablePrefix}
     * with the given values.
     * 
     * @param sqlFile contains the SQL statements
     * @param schema for resolving {schema}
     * @param tablePrefix for resolving {tablePrefix}
     * 
     * @return the sql with {schema} and {tablePrefix} substituted.
     */
    public static String loadSql(File sqlFile, String schema, String tablePrefix) throws IOException {

        log("loadSql", "File: " + sqlFile.getCanonicalPath() +
                       ", schema = " + schema + ", tablePrefix =" + tablePrefix);

        // Load the sql
        List<String> sqlLines = filterOutComments(StringUtils.readLines(new FileInputStream(sqlFile)));

        String sql = resolveSql(StringUtils.join(sqlLines, ""), schema, tablePrefix);

        log("loadSql", "sql: " + sql);

        return sql;
    }

    /**
     * @return the given sqlLines with all comment lines (start with "--") filtered out.
     */
    public static List<String> filterOutComments(List<String> sqlLines) {
        return StringUtils.filter(sqlLines, new StringUtils.FilterClause() {
            @Override
            public boolean filter(String line) {
                return (!line.startsWith("--"));
            }
        });
    }

    /**
     * @return the given sql with {schema} and {tablePrefix} substituted.
     */
    public static String resolveSql(String sql, String schema, String tablePrefix) {
        return sql.replaceAll("\\{schema\\}", schema.trim())
                        .replaceAll("\\{tablePrefix\\}", tablePrefix.trim());
    }

    /**
     * Assorted StringUtils needed by DbServletClient.
     * 
     * Note: I've embedded this inside DbServletClient so that the DbServletClient
     * code is easily portable.
     */
    protected static class StringUtils {

        /**
         * @return true if the given string is null or empty or only whitespace.
         */
        public static boolean isEmpty(String s) {
            return (s == null || s.trim().isEmpty());
        }
        
        /**
         * @return the given strs joined on the given delim.
         */
        public static String join(Collection<String> strs, String delim) {
            StringBuffer retMe = new StringBuffer();
            String d = "";
            for (String str : ((strs != null) ? strs : new ArrayList<String>())) {
                retMe.append(d).append(str);
                d = delim;
            }
            return retMe.toString();
        }

        /**
         * Assumes InputStream is encoded in UTF-8.
         */
        public static List<String> readLines(InputStream inputStream) throws IOException {
            if (inputStream == null) {
                return new ArrayList<String>();
            }
            return readLines(new BufferedReader(new InputStreamReader(inputStream, "UTF-8")));
        }

        /**
         * @param reader read from here (note: the reader is closed at end-of-method).
         * 
         * @return the given buffer as a List<String>, one String per line.
         */
        public static List<String> readLines(BufferedReader reader) throws IOException {
            List<String> retMe = new ArrayList<String>();
            String lineIn = null;
            while ((lineIn = reader.readLine()) != null) {
                retMe.add(lineIn);
            }
            reader.close();
            return retMe;
        }

        /**
         * 
         * @return the subset of strs for which filterClause returns true
         */
        public static List<String> filter(List<String> strs, FilterClause filterClause) {
            List<String> retMe = new ArrayList<String>();
            for (String str : strs) {
                if (filterClause.filter(str)) {
                    retMe.add(str);
                }
            }
            return retMe;
        }

        /**
         * Used with the filter method.
         */
        public static interface FilterClause {
            public boolean filter(String item);
        };
    }
    
    /**
     * 
     */
    protected static class IOUtils {

        /**
         * Copy the given InputStream to the given OutputStream.
         * 
         * Note: the InputStream is closed when the copy is complete.  The OutputStream 
         *       is left open.
         */
        public static void copyStream(InputStream from, OutputStream to) throws IOException {
            byte buffer[] = new byte[2048];
            int bytesRead;
            while ((bytesRead = from.read(buffer)) != -1) {
                to.write(buffer, 0, bytesRead);
            }
            from.close();
        }
    }    
    
    /**
     * 
     */
    protected static class HttpUtils {
        
        /**
         * This gets an HttpURLConnection to the requested address
         * 
         * @param requestMethod The HTTP request method (GET, POST, HEAD, OPTIONS, PUT, DELETE, TRACE)
         * @param url The URL to get a connection to
         * @param timeout
         * 
         * @return the HttpURLConnection
         * 
         * @throws IOException
         */
        protected static HttpURLConnection getHttpConnection(String requestMethod, URL url, int timeout) throws IOException {
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
            con.setRequestMethod(requestMethod);
            con.setConnectTimeout(timeout);

            if (con instanceof HttpsURLConnection) {
                ((HttpsURLConnection) con).setHostnameVerifier(new HostnameVerifier() {
                    @Override
                    public boolean verify(String hostname, SSLSession session) {
                        return true;
                    }
                });
            }

            return con;
        }
        
        /**
         * @param requestMethod "GET", "POST", etc.
         * @param url the http url
         * @param payload message content
         * 
         * @return con
         */
        public static HttpURLConnection getHttpConnection(String requestMethod, 
                                                          URL url, 
                                                          String payload) throws IOException {
            log("getHttpConnection", requestMethod + " " + url);
            log("getHttpConnection", "payload: " + payload);
            HttpURLConnection con = getHttpConnection(requestMethod, url, 0);
            
            if (payload != null) {
                IOUtils.copyStream( new ByteArrayInputStream(payload.getBytes("UTF-8")),
                                    con.getOutputStream() );
            }
                    
            try {
                con.connect();
                int rc = con.getResponseCode(); // Forces the request.
                log("getHttpConnection", "response code: " + rc);
            } catch (IOException ioe) {
                
                // Read the error stream and include it in the IOException.
                String errMsg = StringUtils.join(StringUtils.readLines(con.getErrorStream()), System.getProperty("line.separator"));
                if ( !StringUtils.isEmpty(errMsg) ) {
                    throw new IOException(errMsg, ioe);
                } else {
                    throw ioe;
                }
            }
            
            return con;
        }
    }

}
