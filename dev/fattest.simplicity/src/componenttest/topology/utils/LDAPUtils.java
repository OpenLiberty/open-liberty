/*******************************************************************************
 * Copyright (c) 2011, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package componenttest.topology.utils;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.impl.LibertyServer;

/**
 *
 */
public class LDAPUtils {

    private static final Class<?> c = LDAPUtils.class;
    public static boolean USE_LOCAL_LDAP_SERVER = false;

    public static String LDAP_SERVER_1_NAME = "localhost";
    public static String LDAP_SERVER_2_NAME = "localhost";
    public static String LDAP_SERVER_3_NAME = "localhost";
    public static String LDAP_SERVER_4_NAME = "localhost";
    public static String LDAP_SERVER_5_NAME = "localhost";
    public static String LDAP_SERVER_6_NAME = "localhost";
    public static String LDAP_SERVER_7_NAME = "localhost";
    public static String LDAP_SERVER_8_NAME = "localhost";
    public static String LDAP_SERVER_9_NAME = "localhost";
    public static String LDAP_SERVER_10_NAME = "localhost";
    public static String LDAP_SERVER_11_NAME = "localhost";
    public static String LDAP_SERVER_12_NAME = "localhost";
    public static String LDAP_SERVER_13_NAME = "localhost";
    public static String LDAP_SERVER_1_PORT = "389";
    public static String LDAP_SERVER_2_PORT = "389";
    public static String LDAP_SERVER_2_SSL_PORT = "636";
    public static String LDAP_SERVER_3_PORT = "389";
    public static String LDAP_SERVER_3_SSL_PORT = "636";
    public static String LDAP_SERVER_4_PORT = "389";
    public static String LDAP_SERVER_4_SSL_PORT = "636";
    public static String LDAP_SERVER_5_PORT = "389";
    public static String LDAP_SERVER_6_PORT = "389";
    public static String LDAP_SERVER_6_SSL_PORT = "636";
    public static String LDAP_SERVER_7_PORT = "389";
    public static String LDAP_SERVER_7_SSL_PORT = "636";
    public static String LDAP_SERVER_8_PORT = "389";
    public static String LDAP_SERVER_8_SSL_PORT = "636";
    public static String LDAP_SERVER_9_PORT = "389";
    public static String LDAP_SERVER_10_PORT = "389";
    public static String LDAP_SERVER_11_PORT = "389";
    public static String LDAP_SERVER_12_PORT = "389";
    public static String LDAP_SERVER_13_PORT = "389";
    public static String LDAP_SERVER_13_SSL_PORT = "636";

    public static String LDAP_SERVER_2_BINDDN = "cn=testuser,cn=users,dc=secfvt2,dc=austin,dc=ibm,dc=com";
    public static String LDAP_SERVER_2_BINDPWD = "testuserpwd";
    public static String LDAP_SERVER_3_BINDDN = "cn=Directory Manager";
    public static String LDAP_SERVER_3_BINDPWD = "oracle1ldap";

    public static String LDAP_SERVER_4_BINDDN = "uid=admin,ou=system";
    public static String LDAP_SERVER_4_BINDPWD = "secret";
    public static String LDAP_SERVER_7_BINDDN = "uid=admin,ou=system";
    public static String LDAP_SERVER_7_BINDPWD = "secret";
    public static String LDAP_SERVER_8_BINDDN = "uid=admin,ou=system";
    public static String LDAP_SERVER_8_BINDPWD = "secret";
    public static String LDAP_SERVER_10_BINDDN = "uid=admin,ou=system";
    public static String LDAP_SERVER_10_BINDPWD = "secret";
    public static String LDAP_SERVER_11_BINDDN = "uid=admin,ou=system";
    public static String LDAP_SERVER_11_BINDPWD = "secret";
    public static String LDAP_SERVER_12_BINDDN = "uid=admin,ou=system";
    public static String LDAP_SERVER_12_BINDPWD = "secret";

    public static String LDAP_SERVER_13_BINDDN = "cn=Directory Manager";
    public static String LDAP_SERVER_13_BINDPWD = "oracle1ldap";

    /**
     * Adds the following LDAP variables to the bootstrap.properties file for
     * use in server.xml:
     *
     * <ul>
     * <li>ldap.server.1.name - the host of the first LDAP server</li>
     * <li>ldap.server.1.port - the port of the first LDAP server</li>
     * <li>ldap.server.2.name - the host of the second LDAP server</li>
     * <li>ldap.server.2.port - the port of the second LDAP server</li>
     * <li>ldap.server.3.name - the host of the third LDAP server</li>
     * <li>ldap.server.3.port - the port of the third LDAP server</li>
     * <li>ldap.server.4.name - the host of the fourth LDAP server</li>
     * <li>ldap.server.4.port - the port of the fourth LDAP server</li>
     * <li>ldap.server.5.name - the host of the fifth LDAP server</li>
     * <li>ldap.server.5.port - the port of the fifth LDAP server</li>
     * <li>ldap.server.6.name - the host of the sixth LDAP server</li>
     * <li>ldap.server.6.port - the port of the sixth LDAP server</li>
     * <li>ldap.server.7.name - the host of the seventh LDAP server</li>
     * <li>ldap.server.7.port - the port of the seventh LDAP server</li>
     * <li>ldap.server.8.name - the host of the eighth LDAP server</li>
     * <li>ldap.server.8.port - the port of the eighth LDAP server</li>
     * <li>ldap.server.9.name - the host of the ninth LDAP server</li>
     * <li>ldap.server.9.port - the port of the ninth LDAP server</li>
     * <li>ldap.server.10.name - the host of the tenth LDAP server</li>
     * <li>ldap.server.10.port - the port of the tenth LDAP server</li>
     * <li>ldap.server.11.name - the host of the eleventh LDAP server</li>
     * <li>ldap.server.11.port - the port of the eleventh LDAP server</li>
     * <li>ldap.server.12.name - the host of the twelfth LDAP server</li>
     * <li>ldap.server.12.port - the port of the twelfth LDAP server</li>
     * <li>ldap.server.13.name - the host of the thirteen LDAP server</li>
     * <li>ldap.server.13.port - the port of the thirteen LDAP server</li>
     * </ul>
     *
     * @param server
     *            server for which bootstrap properties file needs updating with LDAP server host/ports
     * @throws Exception
     */
    public static void addLDAPVariables(LibertyServer server) throws Exception {
        addLDAPVariables(server, true);
    }

    /**
     * Adds LDAP variables for various servers and ports to the bootstrap.properties file for use in server.xml.
     *
     * @param server
     * @param isInMemoryAllowed If false, physical LDAP servers and ports will be used as the property values.
     * @throws Exception
     */
    public static void addLDAPVariables(LibertyServer server, boolean isInMemoryAllowed) throws Exception {
        String method = "addLDAPVariables";
        Log.entering(c, method);

        // Read LDAP ports from system properties
        final String LDAP_1_PORT = AccessController.doPrivileged(new PrivilegedAction<String>() {
            @Override
            public String run() {
                Log.info(c, "<clinit>", "LDAP_1_PORT=" + System.getProperty("ldap.1.port"));
                return System.getProperty("ldap.1.port");
            }
        });
        final String LDAP_2_PORT = AccessController.doPrivileged(new PrivilegedAction<String>() {
            @Override
            public String run() {
                Log.info(c, "<clinit>", "LDAP_2_PORT=" + System.getProperty("ldap.2.port"));
                return System.getProperty("ldap.2.port");
            }
        });
        final String LDAP_3_PORT = AccessController.doPrivileged(new PrivilegedAction<String>() {
            @Override
            public String run() {
                Log.info(c, "<clinit>", "LDAP_3_PORT=" + System.getProperty("ldap.3.port"));
                return System.getProperty("ldap.3.port");
            }
        });

        final String LDAP_1_SSL_PORT = AccessController.doPrivileged(new PrivilegedAction<String>() {
            @Override
            public String run() {
                Log.info(c, "<clinit>", "LDAP_1_SSL_PORT=" + System.getProperty("ldap.1.ssl.port"));
                return System.getProperty("ldap.1.ssl.port");
            }
        });
        final String LDAP_2_SSL_PORT = AccessController.doPrivileged(new PrivilegedAction<String>() {
            @Override
            public String run() {
                Log.info(c, "<clinit>", "LDAP_2_SSL_PORT=" + System.getProperty("ldap.2.ssl.port"));
                return System.getProperty("ldap.2.ssl.port");
            }
        });
        final String LDAP_3_SSL_PORT = AccessController.doPrivileged(new PrivilegedAction<String>() {
            @Override
            public String run() {
                Log.info(c, "<clinit>", "LDAP_3_SSL_PORT=" + System.getProperty("ldap.3.ssl.port"));
                return System.getProperty("ldap.3.ssl.port");
            }
        });

        Properties props = new Properties();

        // Get bootstrap properties file from server
        RemoteFile bootstrapPropFile = null;
        try {
            bootstrapPropFile = server.getServerBootstrapPropertiesFile();
        } catch (Exception e) {
            Log.error(c, method, e, "Error while getting the bootstrap properties file from liberty server");
            throw new Exception("Error while getting the bootstrap properties file from liberty server");
        }

        // Open the remote file for reading
        FileInputStream in = null;
        try {
            in = (FileInputStream) bootstrapPropFile.openForReading();
        } catch (Exception e) {
            Log.error(c, method, e, "Error while reading the remote bootstrap properties file");
            throw new Exception("Error while reading the remote bootstrap properties file");
        }

        // Load properties from bootstrap
        try {
            props.load(in);
        } catch (IOException e) {
            Log.error(c, method, e, "Error while loading properties from file input stream");
            throw new IOException("Error while loading properties from file input stream");
        } finally {
            //Close the input stream
            try {
                in.close();
            } catch (IOException e1) {
                Log.error(c, method, e1, "Error while closing the input stream");
                throw new IOException("Error while closing the input stream");
            }
        }

        // Open the remote file for writing with append as false
        FileOutputStream out = null;
        try {
            out = (FileOutputStream) bootstrapPropFile.openForWriting(false);
        } catch (Exception e) {
            Log.error(c, method, e, "Error while writing to remote bootstrap properties file");
            throw new Exception("Error while writing to remote bootstrap properties file");
        }

        Log.info(c, "addLDAPVariables", "Setting in-memory LDAP server properties");
        props.setProperty("ldap.server.1.name", LDAP_SERVER_1_NAME);
        props.setProperty("ldap.server.2.name", LDAP_SERVER_2_NAME);
        props.setProperty("ldap.server.3.name", LDAP_SERVER_3_NAME);
        props.setProperty("ldap.server.4.name", LDAP_SERVER_4_NAME);
        props.setProperty("ldap.server.5.name", LDAP_SERVER_5_NAME);
        props.setProperty("ldap.server.6.name", LDAP_SERVER_6_NAME);
        props.setProperty("ldap.server.7.name", LDAP_SERVER_7_NAME);
        props.setProperty("ldap.server.8.name", LDAP_SERVER_8_NAME);
        props.setProperty("ldap.server.9.name", LDAP_SERVER_9_NAME);
        props.setProperty("ldap.server.10.name", LDAP_SERVER_10_NAME);
        props.setProperty("ldap.server.11.name", LDAP_SERVER_11_NAME);
        props.setProperty("ldap.server.12.name", LDAP_SERVER_12_NAME);
        props.setProperty("ldap.server.13.name", LDAP_SERVER_13_NAME);

        props.setProperty("ldap.server.1.port", LDAP_1_PORT);
        props.setProperty("ldap.server.2.port", LDAP_2_PORT);
        props.setProperty("ldap.server.2.ssl.port", LDAP_2_SSL_PORT);
        props.setProperty("ldap.server.3.port", LDAP_3_PORT);
        props.setProperty("ldap.server.3.ssl.port", LDAP_3_SSL_PORT);
        props.setProperty("ldap.server.4.port", LDAP_1_PORT);
        props.setProperty("ldap.server.4.ssl.port", LDAP_1_SSL_PORT);
        props.setProperty("ldap.server.5.port", LDAP_1_PORT);
        props.setProperty("ldap.server.6.port", LDAP_2_PORT);
        props.setProperty("ldap.server.6.ssl.port", LDAP_2_SSL_PORT);
        props.setProperty("ldap.server.7.port", LDAP_1_PORT);
        props.setProperty("ldap.server.7.ssl.port", LDAP_1_SSL_PORT);
        props.setProperty("ldap.server.8.port", LDAP_1_PORT);
        props.setProperty("ldap.server.8.ssl.port", LDAP_1_SSL_PORT);
        props.setProperty("ldap.server.9.port", LDAP_1_PORT);
        props.setProperty("ldap.server.10.port", LDAP_1_PORT);
        props.setProperty("ldap.server.11.port", LDAP_1_PORT);
        props.setProperty("ldap.server.12.port", LDAP_1_PORT);
        props.setProperty("ldap.server.13.port", LDAP_3_PORT);
        props.setProperty("ldap.server.13.ssl.port", LDAP_3_SSL_PORT);

        props.setProperty("ldap.server.4.bindDN", LDAP_SERVER_4_BINDDN);
        props.setProperty("ldap.server.4.bindPassword", LDAP_SERVER_4_BINDPWD);

        props.setProperty("ldap.server.7.bindDN", LDAP_SERVER_7_BINDDN);
        props.setProperty("ldap.server.7.bindPassword", LDAP_SERVER_7_BINDPWD);

        props.setProperty("ldap.server.8.bindDN", LDAP_SERVER_8_BINDDN);
        props.setProperty("ldap.server.8.bindPassword", LDAP_SERVER_8_BINDPWD);

        props.setProperty("ldap.server.10.bindDN", LDAP_SERVER_10_BINDDN);
        props.setProperty("ldap.server.10.bindPassword", LDAP_SERVER_10_BINDPWD);

        props.setProperty("ldap.server.11.bindDN", LDAP_SERVER_11_BINDDN);
        props.setProperty("ldap.server.11.bindPassword", LDAP_SERVER_11_BINDPWD);

        props.setProperty("ldap.server.12.bindDN", LDAP_SERVER_12_BINDDN);
        props.setProperty("ldap.server.12.bindPassword", LDAP_SERVER_12_BINDPWD);

        // Write above LDAP variables to remote bootstrap properties file
        try {
            props.store(out, null);
            Log.info(c, method, "added ldap variables to bootstrap file");
        } catch (IOException e) {
            Log.error(c, method, e, "Error while reading the remote bootstrap properties file");
            throw new Exception("Error while reading the remote bootstrap properties file");
        } finally {
            //Close the output stream
            try {
                out.close();
                Log.info(c, method, "closed output stream");
            } catch (IOException e) {
                Log.error(c, method, e, "Error while closing the output stream");
                throw new IOException("Error while closing the output stream");
            }
        }
        Log.info(c, method, "about to exit routine");

        Log.exiting(c, method);
    }

    public static boolean isLdapServerAvailable(String hostname, String port) throws Exception {
        return isLdapServerAvailable(hostname, port, false);
    }

    public static boolean isLdapServerAvailable(String hostname, String port, boolean useSsl) throws Exception {
        return isLdapServerAvailable(hostname, port, false, null, null);
    }

    public static boolean isLdapServerAvailable(String hostname, String port, boolean useSsl, String bindDn, String bindPwd) throws Exception {
        String method = "isLdapServerAvaialble";
        Log.entering(c, method);

        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, "ldap://" + hostname + ":" + port);
        env.put(Context.SECURITY_AUTHENTICATION, "none");
        if (useSsl) {
            env.put(Context.PROVIDER_URL, "ldaps://" + hostname + ":" + port);
            if (bindDn != null && bindPwd != null) {
                env.put(Context.SECURITY_AUTHENTICATION, "simple");
                env.put(Context.SECURITY_PRINCIPAL, bindDn);
                env.put(Context.SECURITY_CREDENTIALS, bindPwd);
            }
            // Use a custom SSLSocketFactory class that doesn't check the remote server's signer certificate
            env.put("java.naming.ldap.factory.socket", CustomSSLSocketFactory.class.getName());
        }
        DirContext ctx = null;
        boolean isLdapServerAvailable = false;
        // Create initial context
        try {
            ctx = new InitialDirContext(env);
            Log.info(c, method, "Successfully created context to ldap with hostname " + hostname + " and port " + port);

            isLdapServerAvailable = true;
        } catch (Exception e) {
            Log.error(c, method, e, "Error while creating context to ldap with hostname " + hostname + " and port " + port);
            throw new Exception("Error while creating context to ldap with hostname " + hostname + " and port " + port);
        } finally {
            try {
                if (ctx != null) {
                    ctx.close();
                }
            } catch (NamingException e) {
                Log.error(c, method, e, "Error while closing context to ldap with hostname " + hostname + " and port " + port);
            }
        }
        return isLdapServerAvailable;

    }

    public static boolean areAnyLdapServersAvailable(HashMap<String, String> servers) throws Exception {
        Set<Entry<String, String>> hostsAndPorts = servers.entrySet();
        ArrayList<Entry<String, String>> failedServers = new ArrayList<Entry<String, String>>();
        boolean atLeastOneIsRunning = false;
        for (Entry<String, String> e : hostsAndPorts) {
            try {
                if (isLdapServerAvailable(e.getKey(), e.getValue())) {
                    atLeastOneIsRunning = true;
                }
            } catch (Exception e1) {
                failedServers.add(e);
            }
        }
        if (!atLeastOneIsRunning) {
            String exceptionMessage = "Error while creating context to ldap with hostname(s) ";

            for (Entry<String, String> e : failedServers) {
                exceptionMessage = exceptionMessage + e.getKey() + ":" + e.getValue() + ", ";
            }

            throw new Exception(exceptionMessage);
        }
        return atLeastOneIsRunning;
    }

}
