/*
* IBM Confidential
*
* OCO Source Materials
*
* WLP Copyright IBM Corp. 2017
*
* The source code for this program is not published or otherwise divested
* of its trade secrets, irrespective of what has been deposited with the
* U.S. Copyright Office.
*/
package com.ibm.ws.security.javaeesec.fat_helper;

import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.http.impl.client.DefaultHttpClient;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.apacheds.EmbeddedApacheDS;

import componenttest.custom.junit.runner.OnlyRunInJava7Rule;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 *
 */
public class TestHelper {
    public static LibertyServer myServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.javaeesec.fat");
    protected static Class<?> logClass = TestHelper.class;
//    protected String queryString = "/JavaEESecBasicAuthServlet/JavaEESecBasic";
    protected static String[] warList = { "JavaEESecBasicAuthServlet.war", "JavaEESecAnnotatedBasicAuthServlet.war", "JavaEEsecBasicAuthServletMultipleIS.war" };
    protected static String urlBase;
    protected static String JAR_NAME = "JavaEESecBase.jar";
    private static EmbeddedApacheDS ldapServer = null;

    protected static DefaultHttpClient httpclient;

    public static void commonSetup() throws Exception {

        setupldapServer();

        WCApplicationHelper.addWarToServerApps(myServer, "JavaEESecBasicAuthServlet.war", true, JAR_NAME, false, "web.jar.base", "web.war.basic");
        WCApplicationHelper.addWarToServerApps(myServer, "JavaEESecAnnotatedBasicAuthServlet.war", true, JAR_NAME, false, "web.jar.base", "web.war.annotatedbasic");
        WCApplicationHelper.addWarToServerApps(myServer, "JavaEEsecFormAuth.war", true, JAR_NAME, false, "web.jar.base", "web.war.formlogin");
        WCApplicationHelper.addWarToServerApps(myServer, "JavaEEsecFormAuthRedirect.war", true, JAR_NAME, false, "web.jar.base", "web.war.redirectformlogin");
        myServer.copyFileToLibertyInstallRoot("lib/features", "internalFeatures/javaeesecinternals-1.0.mf");

        myServer.startServer(true);

        urlBase = "http://" + myServer.getHostname() + ":" + myServer.getHttpDefaultPort();

    }

    public static void commonTeardown() throws Exception {
        if (!OnlyRunInJava7Rule.IS_JAVA_7_OR_HIGHER)
            return; // skip the test teardown
        myServer.stopServer();

        if (ldapServer != null) {
            try {
                ldapServer.stopService();
            } catch (Exception e) {
                Log.error(logClass, "teardown", e, "LDAP server threw error while stopping. " + e.getMessage());
            }
        }

    }

    private static void setupldapServer() throws Exception {
        ldapServer = new EmbeddedApacheDS("HTTPAuthLDAP");
        ldapServer.addPartition("test", "o=ibm,c=us");
        ldapServer.startServer(Integer.parseInt(System.getProperty("ldap.1.port")));

        Entry entry = ldapServer.newEntry("o=ibm,c=us");
        entry.add("objectclass", "organization");
        entry.add("o", "ibm");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("uid=jaspildapuser1,o=ibm,c=us");
        entry.add("objectclass", "inetorgperson");
        entry.add("uid", "jaspildapuser1");
        entry.add("sn", "jaspildapuser1sn");
        entry.add("cn", "jaspiuser1");
        entry.add("userPassword", "s3cur1ty");
        ldapServer.add(entry);

    }

    public static String getURLBase() {
        return urlBase;
    }

    public static DefaultHttpClient getHttpClient() {
        return httpclient;
    }
}