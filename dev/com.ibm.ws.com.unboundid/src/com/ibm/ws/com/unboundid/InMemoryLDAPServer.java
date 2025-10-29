/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.com.unboundid;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Level;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import com.ibm.websphere.simplicity.log.Log;
import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;
import com.unboundid.ldap.listener.InMemoryListenerConfig;
import com.unboundid.ldap.sdk.DeleteRequest;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.Modification;
import com.unboundid.ldap.sdk.OperationType;
import com.unboundid.ldap.sdk.schema.Schema;
import com.unboundid.util.Debug;
import com.unboundid.util.ssl.KeyStoreKeyManager;
import com.unboundid.util.ssl.SSLUtil;
import com.unboundid.util.ssl.TrustAllTrustManager;

/**
 * An in memory UnboundID LDAP server. See ContextPoolTimeoutTest for an example on how to use this.
 *
 * <br><br>
 * Basic steps: Create an instance of this class passing in your base DNs, eg. o=ibm,c=us. Add users
 * and groups via entries. Note you will need to build the LDAP tree, so if you want to create
 * uid=user1,ou=users,o=ibm,c=us, then you need an entry for ou=users first.
 *
 * <br><br>
 * In addition to the standard classes, the schema has been extended with the following classes:
 * <ul>
 * <li>wimInetOrgPerson - an extension to the inetOrgPerson class that contains WIM PersonAccount attributes</li>
 * <li>wimGroupOfNames - an extension to the groupofNames class that contains WIM Group attributes.</li>
 * <li>simulatedMicrosoftSecurityPrincipal - a simulated Active Directory user containing samAccountName and memberOf</li>
 * </ul>
 *
 * You can view the WIM schema in wimschema.ldif.
 */
public class InMemoryLDAPServer {

    private static final Class<?> c = InMemoryLDAPServer.class;

    private InMemoryDirectoryServerConfig config = null;
    private InMemoryDirectoryServer ds = null;

    private static final String keystorePassword = "LDAPpassword";
    private String keystore;
    private static final String listenerName = "LDAP";
    private static final String secureListenerName = "LDAPS";

    static {
        // bridge java.util.Logger output to log4j
        //System.setProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager");
        //System.setProperty("java.util.logging.manager", "com.ibm.ws.kernel.boot.logging.WsLogManager");

        // setting the ldap debug level...
//        System.setProperty("com.unboundid.ldap.sdk.debug.enabled", "true");
//        System.setProperty("com.unboundid.ldap.sdk.debug.level", "FINEST");
//        System.setProperty("com.unboundid.ldap.sdk.debug.type", DebugType.getTypeNameList());

        //working debug start

        System.setProperty("com.unboundid.ldap.sdk.debug.enabled", "true");
        System.setProperty("com.unboundid.ldap.sdk.debug.level", "ALL");
        System.setProperty("com.unboundid.ldap.sdk.debug.file", "/tmp/ldapSdk.log");
        System.setProperty("javax.net.debug", "all");
        Debug.setEnabled(true);
        java.util.logging.Logger logger = Debug.getLogger();
        FileHandler cfileHandler = null;
        try {
            cfileHandler = new FileHandler("/tmp/cldapSdk.log");
        } catch (SecurityException | IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        cfileHandler.setLevel(Level.ALL);
        logger.addHandler(cfileHandler);

        //java.util.logging.Logger logger = Debug.getLogger();
        FileHandler sfileHandler = null;
        try {
            sfileHandler = new FileHandler("/tmp/sldapSdk.log");
        } catch (SecurityException | IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        sfileHandler.setLevel(Level.ALL);
        logger.addHandler(sfileHandler);
        // end
//        System.setProperty("unboundid.util.SSLUtil.enabledSSLProtocols", "TLSv1.2");
    }

    /**
     * Creates a new instance of the in memory LDAP server. It initializes the directory
     * service.
     *
     * <p/>
     * The LDAP and LDAPS ports will use available ephemeral ports and anonymous operations are allowed.
     *
     * @param useWimSchema Asking the user if they want to use the default WIM schema
     * @param baseEntries  The base entries to create for this in-memory LDAP servers
     * @throws Exception If something went wrong
     */
    public InMemoryLDAPServer(boolean useWimSchema, String... baseEntries) throws Exception {
        this(useWimSchema, 0, 0, baseEntries);
    }

    /**
     * Creates a new instance of the in memory LDAP server. It initializes the directory
     * service.
     *
     * <p/>
     * Anonymous operations are allowed.
     *
     * @param useWimSchema Asking the user if they want to use the default WIM schema
     * @param ldapPort     The LDAP port to use. 0 to indicate the server should choose an available port.
     * @param ldapsPort    The LDAPS port to use. 0 to indicate the server should choose an available port.
     * @param baseEntries  The base entries to create for this in-memory LDAP servers
     * @throws Exception If something went wrong
     */
    public InMemoryLDAPServer(boolean useWimSchema, int ldapPort, int ldapsPort, String... baseEntries) throws Exception {
        this(useWimSchema, ldapPort, ldapsPort, true, baseEntries);
    }

    /**
     * Creates a new instance of the in memory LDAP server. It initializes the directory
     * service.
     *
     * @param useWimSchema Asking the user if they want to use the default WIM schema
     * @param ldapPort     The LDAP port to use. 0 to indicate the server should choose an available port.
     * @param ldapsPort    The LDAPS port to use. 0 to indicate the server should choose an available port.
     * @param allowAnonOps Whether the server should allow anonymous operations.
     * @param baseEntries  The base entries to create for this in-memory LDAP servers
     * @throws Exception If something went wrong.
     */
    public InMemoryLDAPServer(boolean useWimSchema, int ldapPort, int ldapsPort, boolean allowAnonOps, String... baseEntries) throws Exception {

        System.setProperty("com.unboundid.ldap.sdk.debug.enabled", "true");
        System.setProperty("com.unboundid.ldap.sdk.debug.level", "FINEST");
        System.setProperty("com.unboundid.ldap.sdk.debug.file", "/tmp/ldapSdk.log");
        System.setProperty("javax.net.debug", "all");
        Debug.setEnabled(true);
        System.setProperty(Debug.PROPERTY_INCLUDE_CAUSE_IN_EXCEPTION_MESSAGES, "true");
        java.util.logging.Logger logger = Debug.getLogger();
        FileHandler cfileHandler = null;
        try {
            cfileHandler = new FileHandler("/tmp/cldapSdk.log");
        } catch (SecurityException | IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        cfileHandler.setLevel(Level.ALL);
        logger.addHandler(cfileHandler);

        //java.util.logging.Logger logger = Debug.getLogger();
        FileHandler sfileHandler = null;
        try {
            sfileHandler = new FileHandler("/tmp/sldapSdk.log");
        } catch (SecurityException | IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        sfileHandler.setLevel(Level.ALL);
        logger.addHandler(sfileHandler);
        /*
         * Enable required protocols and cipher suites.
         */
        SSLContext sslc = SSLContext.getDefault();
        SSLSocketFactory sslf = sslc.getSocketFactory();
        SSLSocket ssls = (SSLSocket) sslf.createSocket();
        List<String> protocols = Arrays.asList(ssls.getSupportedProtocols());
        Log.info(c, "InMemoryLDAPServer", "<<UTLE>> sslProtocols: " + protocols);
//        Log.info(c, "InMemoryLDAPServer", "<<UTLE>> hard code sslProtocols: TLSv1z.2");
//        protocols = Arrays.asList("TLSv1.2");
        SSLUtil.setEnabledSSLProtocols(protocols);
        //System.setProperty("com.unboundid.util.ssl.SSLUtil.ENABLED_SSL_PROTOCOLS", "TLSv1.3,TLSv1.2");

        String[] cipherSuits = ssls.getSupportedCipherSuites();
        Log.info(c, "InMemoryLDAPServer", "<<UTLE>> cipherSuits: " + convertCipherListToString(cipherSuits));
        SSLUtil.setEnabledSSLCipherSuites(Arrays.asList(cipherSuits));
        /*
         * Configure base entries and root credentials.
         */
        config = new InMemoryDirectoryServerConfig(baseEntries);
        config.addAdditionalBindCredentials(getBindDN(), getBindPassword());

        /*
         * Disable anonymous operations if requested.
         */
        if (!allowAnonOps) {
            config.setAuthenticationRequiredOperationTypes(OperationType.values());
        }

        /*
         * The keystore is stored within the JAR. Extract it to a temporary directory
         * so we can use the certificate within it at runtime.
         */
        keystore = extractResourceToFile("/resources/keystore.p12", "keystore", ".p12").getAbsolutePath();
        Log.info(c, "InMemoryLDAPServer", "<<UTLE>> keystore: " + keystore);

        KeyStoreKeyManager keystoreKeyManager = new KeyStoreKeyManager(keystore, keystorePassword.toCharArray(), "PKCS12", "cert-alias");
        String alias = keystoreKeyManager.getCertificateAlias();
        Log.info(c, "InMemoryLDAPServer", "<<UTLE>> keystore cert alias: " + alias);
        Log.info(c, "InMemoryLDAPServer", "<<UTLE>> keystore cert chain: " + keystoreKeyManager.getCertificateChain(alias));
        Log.info(c, "InMemoryLDAPServer", "<<UTLE>> keystore cert privateKey: " + keystoreKeyManager.getPrivateKey(alias));
        //        final SSLUtil serverSSLUtil = new SSLUtil(new KeyStoreKeyManager(keystore, keystorePassword
//                        .toCharArray(), "PKCS12", "cert-alias"), new TrustAllTrustManager());
        final SSLUtil serverSSLUtil = new SSLUtil(keystoreKeyManager, new TrustAllTrustManager());

        //SSLServerSocketFactory serverSocketFacotry = serverSSLUtil.createSSLServerSocketFactory();
        /*
         * Configure LDAPS.
         */
        ArrayList<InMemoryListenerConfig> configs = new ArrayList<InMemoryListenerConfig>();
//        InMemoryListenerConfig secure = InMemoryListenerConfig.createLDAPSConfig(secureListenerName, ldapsPort, serverSSLUtil.createSSLServerSocketFactory());
        InMemoryListenerConfig secure = InMemoryListenerConfig.createLDAPSConfig(secureListenerName, ldapsPort, serverSSLUtil.createSSLServerSocketFactory());
        configs.add(secure);

        /*
         * Configure LDAP.
         */
        InMemoryListenerConfig insecure = InMemoryListenerConfig.createLDAPConfig(listenerName, null, ldapPort, null);
        configs.add(insecure);
        config.setListenerConfigs(configs);

        /*
         * We can pre-load a schema if one is provided. The schema can either be passed in
         * or we can be directed to use our internal WIM'ified schema.
         */
        Schema schema = null;
        if (useWimSchema) {
            InputStream in = getClass().getResourceAsStream("/resources/wimschema.ldif");
            Schema wimschema = Schema.getSchema(in);
            schema = Schema.mergeSchemas(Schema.getDefaultStandardSchema(), wimschema);
        }
        config.setSchema(schema);

        /*
         * Configure and start listening for connections.
         */
        ds = new InMemoryDirectoryServer(config);
        ds.startListening();

        Log.info(c, "InMemoryLDAPServer", "<<UTLE>> about to call debugConnect");

        Debug.debugConnect("localhost", getLdapPort());

        Log.info(c, "InMemoryLDAPServer", "LDAP server started listenting on LDAP port " + getLdapPort() + " and LDAPS port " + getLdapsPort() + ".");
    }

    /**
     * Creates a new instance of the in memory LDAP server. It initializes the directory
     * service.
     *
     * @param bases The base entries to create for this in-memory LDAP servers
     * @throws Exception If something went wrong
     */
    public InMemoryLDAPServer(String... bases) throws Exception {
        /*
         * Merge the default schema with our WIM schema. The WIM schema adds wimInetOrgPerson,
         * wimGroupOfNames, and simulatedMicrosoftSecurityPrincipal. The wimInetOrgPerson
         * adds WIM properties as LDAP attributes so that we do not need to set up a mapping
         * between the WIM properties and other existing LDAP attributes.
         */
        this(true, bases);
    }

    /**
     * Extract a resource from the JAR to a temporary file on the file system. The
     * file is marked for deletion on JVM exit.
     *
     * @param resource The resource from the JAR to extract.
     * @param prefix   Prefix for the temporary file.
     * @param suffix   Suffix for the temporary file.
     * @return The {@link File} instance.
     * @throws IOException if there was an issue extracting the file.
     */
    protected File extractResourceToFile(String resource, String prefix, String suffix) throws IOException {
        File tempfile = File.createTempFile(prefix, suffix);
        Log.info(c, "InMemoryLDAPServer", "<<UTLE>> tempfile: " + tempfile.getCanonicalPath());

        InputStream src = getClass().getResourceAsStream(resource);
        Files.copy(src, Paths.get(tempfile.getAbsolutePath()), StandardCopyOption.REPLACE_EXISTING);
        return tempfile;
    }

    /**
     * Get the default administrative bind distinguished name.
     *
     * @return The default administrative bind distinguished name.
     * @see #getBindPassword()
     */
    public static String getBindDN() {
        return "uid=admin,ou=system";
    }

    /**
     * Get the default administrative bind password.
     *
     * @return The default administrative bind password.
     * @see #getBindDN()
     */
    public static String getBindPassword() {
        return "secret";
    }

    /**
     * Get the {@link InMemoryDirectoryServer} instance.
     *
     * @return The {@link InMemoryDirectoryServer} instance.
     */
    public InMemoryDirectoryServer getLdapServer() {
        return ds;
    }

    /**
     * Add an entry to the InMemoryDirectoryServer.
     *
     * @param entry The entry to be added.
     */
    public void add(Entry entry) throws LDAPException {
        ds.add(entry);
    }

    /**
     * Shut down the InMemoryDirectoryServer.
     */
    public void shutDown() {
        ds.shutDown(true);
    }

    /**
     * Shut down the InMemoryDirectoryServer.
     *
     * @param b true to close all existing connections, or false to stop accepting new connections.
     */
    public void shutDown(boolean b) {
        ds.shutDown(b);

    }

    /**
     * Retrieves the configured listen address for the first active listener, if defined.
     *
     * @return The configured listen address for the specified listener, or null if there is no such listener or the listener does not have an explicitly-configured listen address.
     */
    public InetAddress getListenAddress() {
        return ds.getListenAddress();
    }

    /**
     * Get the port the server is listening to.
     *
     * @return the port this directory server is listening to
     *
     * @deprecated This method has been replaced with {@link #getLdapPort()} and {@link #getLdapPort()}.
     */
    @Deprecated
    public int getListenPort() {
        return ds.getListenPort(listenerName);
    }

    /**
     * Get the port for the insecure LDAP port.
     *
     * @return LDAP port
     */
    public int getLdapPort() {
        return ds.getListenPort(listenerName);
    }

    /**
     * Get the secure LDAPS port.
     *
     * @return LDAPS port
     */
    public int getLdapsPort() {
        return ds.getListenPort(secureListenerName);
    }

    /**
     * @param userDn
     * @param modification
     * @throws LDAPException
     */
    public void modify(String userDn, Modification modification) throws LDAPException {
        ds.modify(userDn, modification);

    }

    /**
     * Delete an entry from the LDAP server, eg. delete("uid=user1,ou=users,o=ibm,c=us")
     *
     * @param dn the DN to remove from the directory server
     * @throws LDAPException
     */
    public void delete(String dn) throws LDAPException {
        DeleteRequest deleteRequest = new DeleteRequest(dn);
        delete(deleteRequest);
    }

    /**
     * Delete an entry from the LDAP server.
     *
     * @param dr the DeleteRequest to issue on the directory server
     * @throws LDAPException
     */
    public void delete(DeleteRequest dr) throws LDAPException {
        ds.delete(dr);
    }

    /**
     * Delete an entry from the LDAP server. Eat any exceptions.
     * Use on test clean up if the user may have already been deleted.
     *
     * @param dn
     */
    public void silentDelete(String dn) {
        try {
            ds.delete(dn);
        } catch (Exception e) {
            // if the user or group doesn't exist, that's fine.
        }
    }

    /**
     * Attempts to establish a client connection to the server.
     *
     * @param options The connection options to use when creating the connection. It may be null if a default set of options should be used.
     * @returns The client connection that has been established.
     * @throws LDAPException - If a problem is encountered while attempting to create the connection.
     */
    public LDAPConnection getConnection(String listenerName) throws LDAPException {
        return ds.getConnection(listenerName);
    }

    /*
     * Reads entries from the specified LDIF file and adds them to the server, optionally clearing any existing entries before beginning to add the new entries. If an error is
     * encountered while adding entries from LDIF then the server will remain populated with the data it held before the import attempt (even if the clear is given with a value of
     * true). This method may be used regardless of whether the server is listening for client connections.
     *
     * @param clear Indicates whether to remove all existing entries prior to adding entries read from LDIF.
     *
     * @param path The path to the LDIF file from which the entries should be read. It must not be null
     *
     * @returns The number of entries read from LDIF and added to the server.
     *
     * @throws LDAPException - If a problem occurs while reading entries or adding them to the server.
     */
    public int importFromLDIF(boolean clear, String path) throws LDAPException {
        return ds.importFromLDIF(clear, path);
    }

    private static String convertCipherListToString(String[] cipherList) {
        if (cipherList == null || cipherList.length == 0) {
            return "null";
        }

        StringBuilder sb = new StringBuilder();
        sb.append('(').append(cipherList.length).append(')');
        for (int i = 0; i < cipherList.length; i++) {
            sb.append(' ');
            sb.append(cipherList[i]);
        }

        return sb.toString();
    }
}
