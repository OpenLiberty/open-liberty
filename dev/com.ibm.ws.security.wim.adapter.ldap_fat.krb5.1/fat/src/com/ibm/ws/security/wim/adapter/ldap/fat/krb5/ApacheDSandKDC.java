/*******************************************************************************
 * Copyright (c) 2021, 2022 IBM Corporation and others.
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
package com.ibm.ws.security.wim.adapter.ldap.fat.krb5;

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.directory.api.ldap.model.constants.SupportedSaslMechanisms;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.message.ModifyRequest;
import org.apache.directory.api.ldap.model.message.ModifyRequestImpl;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.kerberos.client.KdcConfig;
import org.apache.directory.kerberos.client.KdcConnection;
import org.apache.directory.kerberos.client.Kinit;
import org.apache.directory.kerberos.client.TgTicket;
import org.apache.directory.kerberos.client.TgtRequest;
import org.apache.directory.kerberos.credentials.cache.Credentials;
import org.apache.directory.kerberos.credentials.cache.CredentialsCache;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.partition.Partition;
import org.apache.directory.server.core.factory.DefaultDirectoryServiceFactory;
import org.apache.directory.server.core.factory.DirectoryServiceFactory;
import org.apache.directory.server.core.factory.PartitionFactory;
import org.apache.directory.server.core.kerberos.KeyDerivationInterceptor;
import org.apache.directory.server.kerberos.KerberosConfig;
import org.apache.directory.server.kerberos.kdc.KdcServer;
import org.apache.directory.server.kerberos.shared.crypto.encryption.KerberosKeyFactory;
import org.apache.directory.server.kerberos.shared.keytab.Keytab;
import org.apache.directory.server.kerberos.shared.keytab.KeytabEntry;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.ldap.handlers.sasl.MechanismHandler;
import org.apache.directory.server.ldap.handlers.sasl.gssapi.GssapiMechanismHandler;
import org.apache.directory.server.protocol.shared.transport.TcpTransport;
import org.apache.directory.server.protocol.shared.transport.Transport;
import org.apache.directory.shared.kerberos.KerberosTime;
import org.apache.directory.shared.kerberos.codec.types.EncryptionType;
import org.apache.directory.shared.kerberos.codec.types.PrincipalNameType;
import org.apache.directory.shared.kerberos.components.EncryptionKey;
import org.apache.directory.shared.kerberos.components.PrincipalName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.wim.adapter.ldap.fat.krb5.utils.LdapKerberosUtils;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.utils.FileUtils;

@RunWith(FATRunner.class)
public class ApacheDSandKDC {

    private static final Class<?> c = ApacheDSandKDC.class;

    protected static boolean FAT_TEST_LOCALRUN = Boolean.getBoolean("fat.test.localrun");

    public static String BASE_DN = LdapKerberosUtils.BASE_DN; // default, override in extending class

    /* The Domain needs to be capitalized for Kerberos, but not necessarily for LDAP. */
    public static String DOMAIN = LdapKerberosUtils.DOMAIN; // default, override in extending class

    public static String WHICH_FAT = "LDAP"; // default, override in extending class. options: {LDAP, SPNEGO}

    protected static String bindPassword = LdapKerberosUtils.BIND_PASSWORD; // default, override in extending class

    protected static String bindUserName = LdapKerberosUtils.BIND_USER; // default, override in extending class

    protected static String bindPrincipalName = LdapKerberosUtils.BIND_PRINCIPAL_NAME; // default, override in extending class

    private static KdcConnection conn;

    private static String serverPrincipal;

    public static String ldapServerHostName = LdapKerberosUtils.HOSTNAME;

    protected static String ldapUser = "ldap"; // default, override in extending class

    private static String ldapPrincipal;

    private static String ldapUserDN;

    protected static String krbtgtUser = "krbtgt"; // default, override in extending class

    private static String krbtgtUserDN;

    private static String krbtgtPrincipal;

    protected static String ticketCacheFile = null;

    protected static String keytabFile = null;

    protected static String configFile = null;

    public static long MIN_LIFE = 6 * 60000;

    static int DEFAULT_KDC_PORT = 88;

    private static boolean initialised;
    private static int savedKdcPort = -1;

    private static DirectoryService directoryService;
    private static LdapServer ldapServer;
    private static KdcServer kdcServer;

    private static void startLdapServer() throws Exception {
        DirectoryServiceFactory dsf = new DefaultDirectoryServiceFactory();
        dsf.init(c.toString());
        directoryService = dsf.getDirectoryService();
        directoryService.addLast(new KeyDerivationInterceptor()); // Required for Kerberos
        directoryService.getChangeLog().setEnabled(false);
        //   directoryService.setAllowAnonymousAccess(true);
        SchemaManager schemaManager = directoryService.getSchemaManager();

        createPartition(dsf, schemaManager, "example", BASE_DN);

        directoryService.startup();

        int ldapPort = getOpenPort(-1);

        ldapServer = new LdapServer();
        ldapServer.setServiceName("DefaultLDAP");
        Transport ldap = new TcpTransport(ldapServerHostName, ldapPort, 3, 5);
        ldapServer.addTransports(ldap);
        ldapServer.setDirectoryService(directoryService);
        ldapServer.setSearchBaseDn(BASE_DN);

        ldapServer.setSaslHost(ldapServerHostName);
        ldapServer.setSaslPrincipal(ldapPrincipal);
        Map<String, MechanismHandler> saslMechanismHandlers = new HashMap<String, MechanismHandler>();
        GssapiMechanismHandler gssapiMechanismHandler = new GssapiMechanismHandler();
        saslMechanismHandlers.put(SupportedSaslMechanisms.GSSAPI, gssapiMechanismHandler);
        ldapServer.setSaslMechanismHandlers(saslMechanismHandlers);

        ldapServer.start();
    }

    private static void createPartition(final DirectoryServiceFactory dsf, final SchemaManager schemaManager, final String id,
                                        final String suffix) throws Exception {
        final String methodName = "createPartition";

        Log.info(c, methodName, "Creating partition for " + suffix);
        PartitionFactory pf = dsf.getPartitionFactory();
        Log.info(c, methodName, "workingDir " + directoryService.getInstanceLayout().getPartitionsDirectory());
        Partition p = pf.createPartition(schemaManager, directoryService.getDnFactory(), id, suffix, 1000,
                                         new File(directoryService.getInstanceLayout().getPartitionsDirectory(), "example"));
        pf.addIndex(p, "krb5PrincipalName", 10);

        try {
            directoryService.addPartition(p);
        } catch (Throwable e) {
            Log.error(c, methodName, e, "Partition creation failed, trying a second time");
            Thread.sleep(5000);
            directoryService.addPartition(p);

            /*
             * Added the retry above, but still seeing rare exceptions from addPartition on z/OS: java.lang.Error: ERR_545 couldn't obtain free translation
             * Throwing an exception here if we do succeed with a retry so we can add more tries if it turns out that a retry helps.
             */

            throw new Exception("Retrying directoryService.addPartion does help in some cases, add more retries to ApacheDSandKDC class.");
        }

        Entry entry = directoryService.newEntry(new Dn(BASE_DN));
        entry.add("objectclass", "domain");
        entry.add("objectclass", "top");
        entry.add("dc", "example");
        directoryService.getAdminSession().add(entry);

        Log.info(c, methodName, "Created partition for " + suffix);
    }

    private static void startKDC() throws Exception {
        final String methodName = "startKDC";

        if (savedKdcPort == -1) {
            savedKdcPort = getOpenPort(DEFAULT_KDC_PORT); // attempt to get the normal default KDC port
        }

        Log.info(c, methodName, "Creating krb.conf file");

        configFile = createConfigFile(bindUserName + "krb5-", savedKdcPort, true, false); // Default config file.
        System.setProperty("java.security.krb5.conf", configFile);
        Log.info(c, methodName, "krb.conf file: " + configFile);

        Log.info(c, methodName, "Starting KDC");

        kdcServer = new KdcServer();
        kdcServer.setServiceName("Test KDC");
        kdcServer.setSearchBaseDn(BASE_DN);

        KerberosConfig config = kdcServer.getConfig();
        config.setServicePrincipal(krbtgtPrincipal);
        config.setPrimaryRealm(DOMAIN);
        config.setPaEncTimestampRequired(false); // Required to avoid GSSAPI errors
        config.setBodyChecksumVerified(false); // Required to avoid GSSAPI errors

        TcpTransport tcpTransport = new TcpTransport(ldapServerHostName, savedKdcPort);
        kdcServer.addTransports(tcpTransport);
        kdcServer.setDirectoryService(directoryService);
        kdcServer.start();

        Log.info(c, methodName, "Started KDC");
    }

    //  @BeforeClass
    public static void setupService() throws Exception {
        final String methodName = "setupService";

        // Uncomment to add more logging, will log a ton to output.txt, use only when needed
//        Logger root = Logger.getLogger("");
//        root.setLevel(Level.FINEST);
//        for (Handler handler : root.getHandlers()) {
//            handler.setLevel(Level.FINEST);
//        }
        ldapPrincipal = ldapUser + "/" + ldapServerHostName + "@" + DOMAIN;

        ldapUserDN = "uid=" + ldapUser + "," + BASE_DN;

        krbtgtUserDN = "uid=" + krbtgtUser + "," + BASE_DN;

        krbtgtPrincipal = krbtgtUser + "/" + DOMAIN + "@" + DOMAIN;

        if (initialised) {
            Log.info(c, methodName, "ApacheDS already marked as started.");
            return;
        }
        startLdapServer();
        startKDC();

        initialised = true;

        createKerberosUserEntries();

        if (conn == null) {
            KdcConfig config = KdcConfig.getDefaultConfig();
            config.setUseUdp(false);
            config.setKdcPort(kdcServer.getTcpPort());
            config.setEncryptionTypes(kdcServer.getConfig().getEncryptionTypes());
            config.setTimeout(Integer.MAX_VALUE);
            conn = new KdcConnection(config);
        }
        if (serverPrincipal == null) {
            serverPrincipal = fixServicePrincipalName(ldapPrincipal,
                                                      new Dn(ldapUserDN), ldapServer);
        }

        createTicketCacheFile();

        createKeyTabFile();

        Log.info(c, methodName, "KDC Port: " + kdcServer.getTcpPort() + ", LDAP Port: " + ldapServer.getPort());

    }

    public static void tearDownService() throws Exception {
        stopAllServers();
        initialised = false;
    }

    /**
     * Create a default TicketCache file
     *
     * @throws Exception
     */
    public static void createTicketCacheFile() throws Exception {
        final String methodName = "createTicketCacheFile";

        Log.info(c, methodName, "Creating ticket cache for " + bindPrincipalName);
        File ccFile = File.createTempFile(bindUserName + "Cache-", ".cc");
        if (!FAT_TEST_LOCALRUN) {
            ccFile.deleteOnExit();
        }
        Kinit kinit = new Kinit(conn);
        kinit.setCredCacheFile(ccFile);

        kinit.kinit(bindPrincipalName, bindPassword);

        CredentialsCache credCache = CredentialsCache.load(ccFile);
        assertNotNull("TicketCache is null", credCache);

        ticketCacheFile = ccFile.getAbsolutePath();

        Log.info(c, methodName, "Created ticket cache: " + ticketCacheFile);
        Log.info(c, methodName, "Ticket cache contents: " + FileUtils.readFile(ticketCacheFile));
    }

    /**
     * Create the principal that will be used for Kerberos bind
     *
     * @param krb5User
     * @param userPassword
     * @param principalName
     * @return
     * @throws Exception
     */
    public static String createPrincipal(String krb5User, String krb5UserPwd) throws Exception {
        final String methodName = "createPrincipal";

        String principalName = krb5User + "@" + DOMAIN;
        Entry entry = new DefaultEntry(directoryService.getSchemaManager());
        entry.add("userPassword", krb5UserPwd);
        entry.add("krb5PrincipalName", principalName);
        entry.add("krb5KeyVersionNumber", "0");

        //The following attributes are required to complete the valid Directory Service User Entry:
        entry.setDn("uid=" + krb5User + "," + BASE_DN);
        entry.add("objectClass", "top", "person", "inetOrgPerson", "krb5principal", "krb5kdcentry");
        entry.add("cn", krb5User);
        entry.add("sn", krb5User);
        entry.add("uid", krb5User);

        directoryService.getAdminSession().add(entry);

        if ("LDAP".equals(WHICH_FAT)) {
            Log.info(c, methodName, "Created " + entry.getDn());
        } else {
            //SPNEGO FAT
            Log.info(c, methodName, "Created " + entry.get("krb5PrincipalName"));
        }

        return entry.getDn().getName();
    }

    /**
     * Create the Kerberos and related entities
     *
     * @throws Exception
     */
    public static void createKerberosUserEntries() throws Exception {
        Log.info(c, "startAllServers", "Creating KDC user entries");

        Entry entry = new DefaultEntry(directoryService.getSchemaManager());
        entry.setDn(krbtgtUserDN);
        entry.add("objectClass", "top", "person", "inetOrgPerson", "krb5principal", "krb5kdcentry");
        entry.add("cn", "KDC Service");
        entry.add("sn", "Service");
        entry.add("uid", krbtgtUser);
        entry.add("userPassword", "secret");
        entry.add("krb5PrincipalName", krbtgtPrincipal);
        entry.add("krb5KeyVersionNumber", "0");
        directoryService.getAdminSession().add(entry);

        Log.info(c, "createPrincipal", "Created " + entry.getDn());

        // app service
        entry = new DefaultEntry(directoryService.getSchemaManager());
        entry.setDn(ldapUserDN);
        entry.add("objectClass", "top", "person", "inetOrgPerson", "krb5principal", "krb5kdcentry");
        entry.add("cn", ldapUser.toUpperCase());
        entry.add("sn", "Service");
        entry.add("uid", ldapUser);
        entry.add("userPassword", "secret");
        entry.add("krb5PrincipalName", ldapPrincipal);
        entry.add("krb5KeyVersionNumber", "0");
        directoryService.getAdminSession().add(entry);

        Log.info(c, "createPrincipal", "Created " + entry.getDn());

        createPrincipal(bindUserName, bindPassword);

        Log.info(c, "startAllServers", "Created KDC user entries");

    }

    public static String fixServicePrincipalName(String servicePrincipalName, Dn serviceEntryDn, LdapServer ldapServer) throws LdapException {
        KerberosPrincipal servicePrincipal = new KerberosPrincipal(servicePrincipalName, KerberosPrincipal.KRB_NT_SRV_HST);
        servicePrincipalName = servicePrincipal.getName();

        ldapServer.setSaslHost(servicePrincipalName.substring(servicePrincipalName.indexOf("/") + 1,
                                                              servicePrincipalName.indexOf("@")));
        ldapServer.setSaslPrincipal(servicePrincipalName);

        if (serviceEntryDn != null) {
            ModifyRequest modifyRequest = new ModifyRequestImpl();
            modifyRequest.setName(serviceEntryDn);
            modifyRequest.replace("userPassword", "randall");
            modifyRequest.replace("krb5PrincipalName", servicePrincipalName);
            ldapServer.getDirectoryService().getAdminSession().modify(modifyRequest);
        }

        return servicePrincipalName;
    }

    public static String getDefaultTicketCacheFile() {
        return ticketCacheFile;
    }

    public static String getDefaultConfigFile() {
        return configFile;
    }

    public static String getDefaultKeytabFile() {
        return keytabFile;
    }

    /**
     * Stop all the servers related to ApacheDS
     *
     * @throws Exception
     */
    public static void stopAllServers() throws Exception {
        String methodName = "stopAllServers";
        Log.info(c, methodName, "Stopping all ApacheDS servers");
        if (kdcServer != null) {
            kdcServer.stop();
        }

        if (ldapServer != null) {
            ldapServer.stop();
        }

        if (directoryService != null) {
            directoryService.shutdown();
        }

        Log.info(c, methodName, "Stopped all ApacheDS servers, double checking ports");

        // Having some weird issues on Windows where servers fail to start, HTTP team suspects artifacts from this class
        ServerSocket s = null;
        for (int i = 0; i <= 3; i++) {
            try {
                Log.info(c, methodName, "Checking if KDC port is freed.");
                s = new ServerSocket(kdcServer.getTcpPort());
                s.setReuseAddress(true);
                Log.info(c, methodName, "KDC port is free.");
                break;
            } catch (Throwable t) {
                Log.info(c, methodName, "KDC port not free. Other tests may fail to start.");
                Thread.sleep(1000);
            } finally {
                if (s != null) {
                    s.close();
                }
            }
        }

        s = null;
        for (int i = 0; i <= 3; i++) {
            try {
                Log.info(c, methodName, "Checking if LdapServer port is freed.");
                s = new ServerSocket(ldapServer.getPort());
                s.setReuseAddress(true);
                Log.info(c, methodName, "LdapServer port is free.");
                break;
            } catch (Throwable t) {
                Log.info(c, methodName, "LdapServer port not free. Other tests may fail to start.");
                Thread.sleep(1000);
            } finally {
                if (s != null) {
                    s.close();
                }
            }
        }
    }

    /**
     * Start all the servers related to ApacheDS
     *
     * @throws Exception
     */
    public static void startAllServers() throws Exception {
        assertNotNull("KdcServer is null and cannot be started", kdcServer);
        assertNotNull("LdapServer is null and cannot be started", ldapServer);
        assertNotNull("DirectoryService is null and cannot be started", directoryService);

        Log.info(c, "startAllServers", "Starting all ApacheDS servers");
        directoryService.startup();
        ldapServer.start();
        kdcServer.start();
        Log.info(c, "startAllServers", "Started all ApacheDS servers");
    }

    /**
     * Get a free port.
     *
     * @return The free port.
     * @throws IOException If a free port could not be found.
     */
    private static int getOpenPort(int preferredPort) throws IOException {
        final String methodName = "getOpenPort";

        Log.info(c, methodName, "Preferred port: " + preferredPort);
        ServerSocket s = null;
        if (FAT_TEST_LOCALRUN && System.getProperty("os.name").toLowerCase().startsWith("mac") && preferredPort < 100) {
            Log.info(c, methodName, "Running on local Mac, get random port instead of requested " + preferredPort);
        } else if (preferredPort > 0) {
            try {
                s = new ServerSocket(preferredPort);
                s.setReuseAddress(true);
                Log.info(c, methodName, "Got preferred port " + s);
                return preferredPort;
            } catch (Throwable t) {
                // get a random port instead
                Log.info(c, methodName, "Unabled to get preferred port, get a random port.");
            } finally {
                if (s != null) {
                    s.close();
                }
            }
        }
        s = new ServerSocket(0);
        s.setReuseAddress(true);
        Log.info(c, methodName, "Got socket.");
        int port = s.getLocalPort();
        Log.info(c, methodName, "Got port " + port);
        s.close();
        Log.info(c, methodName, "Close socket");
        return port;
    }

    /**
     * Get the {@link DirectoryService} for the LDAP server.
     *
     * @return the {@link DirectoryService}.
     */
    public static DirectoryService getDirectoryService() {
        return directoryService;
    }

    /**
     * Create the krb config file with a custom KDC port
     *
     * @return
     * @throws IOException
     */
    public static String createConfigFile(String name, int port, boolean includeRealm, boolean includeKeytab) throws IOException {
        final String methodName = "createConfigFile";

        Log.info(c, methodName, "Creating config file: " + name);
        File configFile = File.createTempFile(name, ".conf");
        if (!FAT_TEST_LOCALRUN) {
            configFile.deleteOnExit();
        }

        Path outputPath = Paths.get(configFile.getAbsolutePath());
        Log.info(c, methodName, "Created krb.conf file: " + configFile.getAbsolutePath());
        String conf = "[libdefaults]\n" +
                      "        rdns = false\n" +
                      "        dns_lookup_realm = false\n" +
                      "        udp_preference_limit = 1\n" +
                      "        dns_lookup_kdc = false\n" +
                      "        renew_lifetime = 7d\n" +
                      "        forwardable = true\n";
        if (includeRealm) {
            conf = conf + "        default_realm = " + DOMAIN.toUpperCase() + "\n";
        }
        if (includeKeytab) {
            if (keytabFile == null) {
                Log.info(c, methodName, "Default keytab is null, test may fail with default_keytab_name set to null.");
            }
            conf = conf + "        default_keytab_name = " + keytabFile + "\n";
        }
        conf = conf +
               "\n" +
               "[realms]\n" +
               "        " + DOMAIN.toUpperCase() + " = {\n" +
               "                kdc = " + ldapServerHostName + ":" + port + "\n" +
               "        }\n" +
               "\n" +
               "[domain_realm]\n" +
               "        ." + DOMAIN.toLowerCase() + " = " + DOMAIN.toUpperCase() + "\n" +
               "        " + DOMAIN.toLowerCase() + " = " + DOMAIN.toUpperCase() + "\n";
        outputPath.getParent().toFile().mkdirs();
        Files.write(outputPath, conf.getBytes(StandardCharsets.UTF_8));

        Log.info(c, methodName, "krb.conf file contents: \n\n" + conf);
        return configFile.getAbsolutePath();
    }

    /**
     * Create the default krb config file with a custom KDC port
     *
     * @return
     * @throws IOException
     */
    public static String createInvalidConfigFile(String name, int port) throws IOException {
        final String methodName = "createInvalidConfigFile";

        Log.info(c, methodName, "Creating invalid config file: " + name);
        File ccFile = File.createTempFile(name, ".conf");
        if (!FAT_TEST_LOCALRUN) {
            ccFile.deleteOnExit();
        }

        Path outputPath = Paths.get(ccFile.getAbsolutePath());
        Log.info(c, methodName, "creating krb.conf file.");
        String conf = "[libdefaults]\n" +
                      "        default_realm = NOT.REALM" + "\n" +
                      "\n" +
                      "[realms]\n" +
                      "        NOT.REALM = {\n" +
                      "                kdc = " + ldapServerHostName + ":" + port + "\n" +
                      "        }\n" +
                      "\n" +
                      "[domain_realm]\n" +
                      "        .not.realm = NOT.REALM\n" +
                      "        not.realm = NOT.REALM\n";
        outputPath.getParent().toFile().mkdirs();
        Files.write(outputPath, conf.getBytes(StandardCharsets.UTF_8));

        Log.info(c, methodName, "krb.conf file contents: " + conf);
        return ccFile.getAbsolutePath();
    }

    /**
     * Create a temp ticketCache with a short lifetime
     *
     * @param renew
     * @return
     * @throws Exception
     */
    public static String createTicketCacheShortLife(boolean renew) throws Exception {
        final String methodName = "createTicketCacheShortLife";

        Log.info(c, methodName, "Creating short life ticket cache. Renew: " + renew);
        KerberosConfig kdcConfig = kdcServer.getConfig();
        long prevMaxTicket = kdcConfig.getMaximumTicketLifetime();
        long prevMin = kdcConfig.getMinimumTicketLifetime();
        boolean previousRenew = kdcConfig.isRenewableAllowed();
        File ccFile = null;
        try {
            kdcConfig.setMaximumTicketLifetime(MIN_LIFE); // Though forever, this is the minimum max allowed by ApacheDS
            kdcConfig.setRenewableAllowed(renew);
            kdcConfig.setMinimumTicketLifetime(1);
            Log.info(c, methodName, "Creating ticket cache for " + bindPrincipalName);
            ccFile = File.createTempFile("credCacheShortLife-", ".cc");
            if (!FAT_TEST_LOCALRUN) {
                ccFile.deleteOnExit();
            }

            CredentialsCache credCache = null;
            if (renew) {
                /**
                 * Apache Kinit doesn't pick up the renewable setting from the krb.config, do it manually
                 */
                TgtRequest clientTgtReq = new TgtRequest();
                clientTgtReq.setClientPrincipal(bindPrincipalName);
                clientTgtReq.setPassword(bindPassword);
                clientTgtReq.setRenewable(true);

                TgTicket tgt = conn.getTgt(clientTgtReq);

                credCache = new CredentialsCache();
                PrincipalName princ = new PrincipalName(bindPrincipalName, PrincipalNameType.KRB_NT_PRINCIPAL);
                princ.setRealm(tgt.getRealm());
                credCache.setPrimaryPrincipalName(princ);
                Credentials cred = new Credentials(tgt);
                credCache.addCredentials(cred);
                CredentialsCache.store(ccFile, credCache);
            } else {
                Kinit kinit = new Kinit(conn);
                kinit.setCredCacheFile(ccFile);

                kinit.kinit(bindPrincipalName, bindPassword);

                credCache = CredentialsCache.load(ccFile);
            }

            assertNotNull("TicketCache failed to create", credCache);

        } finally {
            kdcConfig.setMaximumTicketLifetime(prevMaxTicket);
            kdcConfig.setRenewableAllowed(previousRenew);
            kdcConfig.setMinimumTicketLifetime(prevMin);
        }

        Log.info(c, methodName, "Created ticket cache: " + ticketCacheFile);

        return ccFile.getAbsolutePath();

    }

    /**
     * Create a keytab file with the default bindUser and password
     *
     * @throws Exception If there was an error creating the keytab file.
     */
    public static void createKeyTabFile() throws Exception {
        keytabFile = createKeyTabFile(bindUserName, bindPrincipalName, bindPassword);
    }

    /**
     * Create a keytab file with the specified userName, principal name and password.
     *
     * @param userName      The user name that will be used to create a unique file name.
     * @param principalName The principal name that will inserted into the keytab.
     * @param password      The password that will be inserted into the keytab.
     * @return The path to the newly created keytab file.
     * @throws Exception If there was an error creating the keytab file.
     */
    public static String createKeyTabFile(String userName, String principalName, String password) throws Exception {
        final String methodName = "createKeyTabFile";

        Log.info(c, methodName, "Creating keytab for " + principalName);
        File keyTabTemp = File.createTempFile(userName + "-", ".keytab");
        if (!FAT_TEST_LOCALRUN) {
            keyTabTemp.deleteOnExit();
        }

        Keytab keytab = Keytab.getInstance();

        List<KeytabEntry> entries = addKerberosKeysToKeytab(principalName, password);

        keytab.setEntries(entries);
        keytab.write(keyTabTemp);

        String file = keyTabTemp.getAbsolutePath();

        Log.info(c, methodName, "Created keytab: " + keytabFile);
        Log.info(c, methodName, "Keytab actual contents: " + FileUtils.readFile(file));
        return file;
    }

    /**
     * @param entries
     */
    protected static List<KeytabEntry> addKerberosKeysToKeytab(String principalName, String principalPwd) {
        List<KeytabEntry> entries = new ArrayList<KeytabEntry>();
        KerberosTime timeStamp = new KerberosTime();

        addKerberosKeys(principalName, principalPwd, entries, timeStamp);

        // KeytabEntry doesn't have a nice toString
        for (KeytabEntry key : entries) {
            StringBuffer strBuf = new StringBuffer();
            strBuf.append(" PrincipalName: " + key.getPrincipalName());
            //strBuf.append(" PrincipalType: " + key.getPrincipalType());
            strBuf.append(" Key: " + key.getKey());
            strBuf.append(" KeyVersion: " + key.getKeyVersion());
            //strBuf.append(" TimeStamp: " + key.getTimeStamp());

            Log.info(c, "addKerberosKeysToKeytab", "Keytab entry: " + strBuf.toString());
        }

        return entries;
    }

    /**
     * @param principalName
     * @param principalPwd
     * @param entries
     */
    private static void addKerberosKeys(String principalName, String principalPwd, List<KeytabEntry> entries, KerberosTime timeStamp) {
        int principalType = 1; // KRB5_NT_PRINCIPAL

        for (Map.Entry<EncryptionType, EncryptionKey> keyEntry : KerberosKeyFactory.getKerberosKeys(principalName, principalPwd).entrySet()) {
            final EncryptionKey key = keyEntry.getValue();
            final byte keyVersion = (byte) key.getKeyVersion();
            entries.add(new KeytabEntry(principalName, principalType, timeStamp, keyVersion, key));
        }
    }

    /**
     * Create the Spnego(HTTP) SPN service/user in the KDCServer DS
     *
     * @param dn          The distinguished name for the entry.
     * @param spn         The SPN, or krb5 principal name.
     * @param spnPassword The pasword.
     * @throws Exception If there was an error creating the SPN entry.
     */
    public static void createSpnegoSPNEntry(String dn, String spn, String spnPassword) throws Exception {
        String methodName = "createSpnegoSPNEntry";
        Log.info(c, methodName, "Creating KDC user entries");

        // spnego HTTP service
        Entry entry = new DefaultEntry(directoryService.getSchemaManager());
        entry.setDn(dn);
        entry.add("objectClass", "top", "person", "inetOrgPerson", "krb5principal", "krb5kdcentry");
        entry.add("cn", "HTTP");
        entry.add("sn", "Service");
        entry.add("uid", "HTTP");
        entry.add("userPassword", spnPassword);
        entry.add("krb5PrincipalName", spn);
        entry.add("krb5KeyVersionNumber", "0");
        directoryService.getAdminSession().add(entry);

        Log.info(c, methodName, "Created " + entry.getDn());
    }

    /**
     * Create a SPNEGO SPN keytab.
     *
     * @param canonicalHostname The canonical hostname.
     * @param spn               The SPN.
     * @param spnPassword       The SPN password.
     * @return The path to the keytab file.
     * @throws Exception If there was an error creating the keytab.
     */
    public static String createSpnegoSPNKeytab(String canonicalHostname, String spn, String spnPassword) throws Exception {
        String methodName = "createSpnegoSPNKeytab";
        Log.info(c, methodName, "Creating keytab for " + spn);

        File keyTabTemp = File.createTempFile(canonicalHostname + "_http", ".keytab");
        if (!FAT_TEST_LOCALRUN) {
            keyTabTemp.deleteOnExit();
        }

        Keytab keytab = Keytab.getInstance();

        List<KeytabEntry> entries = addKerberosKeysToKeytab(spn, spnPassword);

        keytab.setEntries(entries);
        keytab.write(keyTabTemp);

        String keytabFile = keyTabTemp.getAbsolutePath();

        Log.info(c, methodName, "Created keytab: " + keytabFile);
        Log.info(c, methodName, "Keytab actual contents: " + FileUtils.readFile(keytabFile));
        return keytabFile;
    }

    /**
     * Get the KDC port. This should be called after the KDC is started / initialized.
     *
     * @return The KDC port or -1 is the KDC server is not started.
     */
    public static int getKdcPort() {
        return (kdcServer != null) ? kdcServer.getTcpPort() : -1;
    }

    /**
     * Get the LDAP port. This sould be called after the LDAP server is started / initialized.
     *
     * @return The LDAP port or -1 if the LDAP server is not started.
     */
    public static int getLdapPort() {
        return (ldapServer != null) ? ldapServer.getPort() : -1;
    }
}
