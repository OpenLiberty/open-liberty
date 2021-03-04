/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
import org.apache.directory.server.core.api.CoreSession;
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
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.wim.adapter.ldap.fat.krb5.utils.LdapKerberosUtils;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.utils.FileUtils;

@RunWith(FATRunner.class)
public class ApacheDSandKDC {

    private static final Class<?> c = ApacheDSandKDC.class;

    private static boolean FAT_TEST_LOCALRUN = Boolean.getBoolean("fat.test.localrun");

    public static final String BASE_DN = LdapKerberosUtils.BASE_DN;

    public static final String DOMAIN = LdapKerberosUtils.DOMAIN;

    private static CoreSession session;

    private static KdcConnection conn;

    private static String bindPassword = LdapKerberosUtils.BIND_PASSWORD;

    private static String bindUserName = LdapKerberosUtils.BIND_USER;

    public static String bindPrincipalName = LdapKerberosUtils.BIND_PRINCIPAL_NAME;

    private static String serverPrincipal;

    public static String ldapServerHostName = LdapKerberosUtils.HOSTNAME;

    private static String ldapUser = "ldap";

    private static String ldapPrincipal = ldapUser + "/" + ldapServerHostName + "@" + DOMAIN;

    private static String ldapUserDN = "uid=" + ldapUser + "," + BASE_DN;

    private static String krbtgtUser = "krbtgt";

    private static String krbtgtUserDN = "uid=" + krbtgtUser + "," + BASE_DN;

    private static String krbtgtPrincipal = krbtgtUser + "/" + DOMAIN + "@" + DOMAIN;

    protected static String ticketCacheFile = null;

    protected static String keytabFile = null;

    protected static String configFile = null;

    public static long MIN_LIFE = 6 * 60000;

    // regular user/group
    protected static final String vmmUser1 = "user1";
    protected static final String vmmUser1DN = "uid=vmmUser1," + BASE_DN;
    protected static final String vmmUser1pwd = "password";
    protected static final String vmmGroup1 = "vmmGroup1";
    protected static final String vmmGroup1DN = "cn=" + vmmGroup1 + "," + BASE_DN;

    static int LDAP_PORT = -1;
    static int KDC_PORT = -1;

    private static final String DIRECTORY_NAME = c.toString();
    private static boolean initialised;

    private static DirectoryService directoryService;
    private static LdapServer ldapServer;
    private static KdcServer kdcServer;

    private static void startLdapServer() throws Exception {
        DirectoryServiceFactory dsf = new DefaultDirectoryServiceFactory();
        dsf.init(DIRECTORY_NAME);
        directoryService = dsf.getDirectoryService();
        directoryService.addLast(new KeyDerivationInterceptor()); // Required for Kerberos
        directoryService.getChangeLog().setEnabled(false);
        //   directoryService.setAllowAnonymousAccess(true);
        SchemaManager schemaManager = directoryService.getSchemaManager();

        createPartition(dsf, schemaManager, "example", BASE_DN);

        directoryService.startup();

        LDAP_PORT = getOpenPort();

        ldapServer = new LdapServer();
        ldapServer.setServiceName("DefaultLDAP");
        Transport ldap = new TcpTransport(ldapServerHostName, LDAP_PORT, 3, 5);
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
        Log.info(c, "createPartition", "Creating partition for " + suffix);
        PartitionFactory pf = dsf.getPartitionFactory();
        Log.info(c, "setUp", "workingDir " + directoryService.getInstanceLayout().getPartitionsDirectory());
        Partition p = pf.createPartition(schemaManager, directoryService.getDnFactory(), id, suffix, 1000,
                                         new File(directoryService.getInstanceLayout().getPartitionsDirectory(), "example"));
        pf.addIndex(p, "krb5PrincipalName", 10);

        directoryService.addPartition(p);

        Entry entry = directoryService.newEntry(new Dn(BASE_DN));
        entry.add("objectclass", "domain");
        entry.add("objectclass", "top");
        entry.add("dc", "example");
        directoryService.getAdminSession().add(entry);

        Log.info(c, "createPartition", "Created partition for " + suffix);
    }

    private static void startKDC() throws Exception {
        KDC_PORT = getOpenPort();

        Log.info(c, "startKDC", "Creating krb.conf file");

        configFile = createDefaultConfigFile();
        System.setProperty("java.security.krb5.conf", configFile);
        Log.info(c, "startKDC", "krb.conf file: " + configFile);

        Log.info(c, "startKDC", "Starting KDC");

        kdcServer = new KdcServer();
        kdcServer.setServiceName("Test KDC");
        kdcServer.setSearchBaseDn(BASE_DN);

        KerberosConfig config = kdcServer.getConfig();
        config.setServicePrincipal(krbtgtPrincipal);
        config.setPrimaryRealm(DOMAIN);
        config.setPaEncTimestampRequired(false); // Required to avoid GSSAPI errors
        config.setBodyChecksumVerified(false); // Required to avoid GSSAPI errors

        TcpTransport udp = new TcpTransport(ldapServerHostName, KDC_PORT);
        kdcServer.addTransports(udp);

        kdcServer.setDirectoryService(directoryService);
        kdcServer.start();

        Log.info(c, "startKDC", "Started KDC");
    }

    @BeforeClass
    public static void setup() throws Exception {
        // Uncomment to add more logging, will log a ton to output.txt, use only when needed
//        Logger root = Logger.getLogger("");
//        root.setLevel(Level.FINEST);
//        for (Handler handler : root.getHandlers()) {
//            handler.setLevel(Level.FINEST);
//        }

        if (initialised) {
            Log.info(c, "start", "ApacheDS already marked as started.");
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

        addBasicUserAndGroup();

        createTicketCacheFile();

        createKeyTabFile();

        Log.info(c, "setup ", "Ports: " + kdcServer.getTcpPort() + " " + ldapServer.getPort());

    }

    @After
    public void tearDown() throws Exception {
        stopAllServers();
        initialised = false;
    }

    /**
     * Add a starter user and group for general servlet login and searches
     *
     */
    public static void addBasicUserAndGroup() throws Exception {
        Log.info(c, "addBasicUserAndGroup", "Adding basic user and group");
        Entry entry = directoryService.newEntry(new Dn(vmmUser1DN));
        entry.add("objectclass", "inetorgperson");
        entry.add("uid", vmmUser1);
        entry.add("sn", "user1");
        entry.add("cn", "user1");
        entry.add("userPassword", vmmUser1pwd);
        session.add(entry);

        entry = directoryService.newEntry(new Dn(vmmGroup1DN));
        entry.add("objectclass", "groupOfNames");
        entry.add("member", vmmUser1DN);
        session.add(entry);
        Log.info(c, "addBasicUserAndGroup", "Adding basic user and group");
    }

    /**
     * Create a default TicketCache file
     *
     * @throws Exception
     */
    public static void createTicketCacheFile() throws Exception {
        Log.info(c, "createTicketCacheFile", "Creating ticket cache for " + bindPrincipalName);
        File ccFile = File.createTempFile("user17Cache-", ".cc");
        if (!FAT_TEST_LOCALRUN) {
            ccFile.deleteOnExit();
        }
        Kinit kinit = new Kinit(conn);
        kinit.setCredCacheFile(ccFile);

        kinit.kinit(bindPrincipalName, bindPassword);

        CredentialsCache credCache = CredentialsCache.load(ccFile);
        assertNotNull("TicketCache is null", credCache);

        ticketCacheFile = ccFile.getAbsolutePath();

        Log.info(c, "createTicketCacheFile", "Created ticket cache: " + ticketCacheFile);
        Log.info(c, "createTicketCacheFile", "Ticket cache contents: " + FileUtils.readFile(ticketCacheFile));
    }

    /**
     * Create the principal that will be used for Kerberos bind
     *
     * @param uid
     * @param userPassword
     * @param principalName
     * @return
     * @throws Exception
     */
    public static String createPrincipal(String uid, String userPassword, String principalName) throws Exception {
        Entry entry = new DefaultEntry(session.getDirectoryService().getSchemaManager());
        entry.setDn("uid=" + uid + "," + BASE_DN);
        entry.add("objectClass", "top", "person", "inetOrgPerson", "krb5principal", "krb5kdcentry");
        entry.add("cn", uid);
        entry.add("sn", uid);
        entry.add("uid", uid);
        entry.add("userPassword", userPassword);
        entry.add("krb5PrincipalName", principalName);
        entry.add("krb5KeyVersionNumber", "0");
        session.add(entry);

        Log.info(c, "createPrincipal", "Created " + entry.getDn());

        return entry.getDn().getName();
    }

    /**
     * Create the Kerberos and related entities
     *
     * @throws Exception
     */
    public static void createKerberosUserEntries() throws Exception {
        Log.info(c, "startAllServers", "Creating KDC user entries");

        session = kdcServer.getDirectoryService().getAdminSession();

        Entry entry = new DefaultEntry(session.getDirectoryService().getSchemaManager());
        entry.setDn(krbtgtUserDN);
        entry.add("objectClass", "top", "person", "inetOrgPerson", "krb5principal", "krb5kdcentry");
        entry.add("cn", "KDC Service");
        entry.add("sn", "Service");
        entry.add("uid", krbtgtUser);
        entry.add("userPassword", "secret");
        entry.add("krb5PrincipalName", krbtgtPrincipal);
        entry.add("krb5KeyVersionNumber", "0");
        session.add(entry);

        Log.info(c, "createPrincipal", "Created " + entry.getDn());

        // app service
        entry = new DefaultEntry(session.getDirectoryService().getSchemaManager());
        entry.setDn(ldapUserDN);
        entry.add("objectClass", "top", "person", "inetOrgPerson", "krb5principal", "krb5kdcentry");
        entry.add("cn", ldapUser.toUpperCase());
        entry.add("sn", "Service");
        entry.add("uid", ldapUser);
        entry.add("userPassword", "secret");
        entry.add("krb5PrincipalName", ldapPrincipal);
        entry.add("krb5KeyVersionNumber", "0");
        session.add(entry);

        Log.info(c, "createPrincipal", "Created " + entry.getDn());

        createPrincipal(bindUserName, bindPassword, bindPrincipalName);

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
        Log.info(c, "stopAllServers", "Stopping all ApacheDS servers");
        if (kdcServer != null) {
            kdcServer.stop();
        }

        if (ldapServer != null) {
            ldapServer.stop();
        }

        if (directoryService != null) {
            directoryService.shutdown();
        }
        Log.info(c, "stopAllServers", "Stopped all ApacheDS servers");
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
    private static int getOpenPort() throws IOException {
        Log.info(c, "getOpenPort", "Entry.");
        ServerSocket s = new ServerSocket(0);
        Log.info(c, "getOpenPort", "Got socket.");
        int port = s.getLocalPort();
        Log.info(c, "getOpenPort", "Got port " + s);
        s.close();
        Log.info(c, "getOpenPort", "Close socket");
        return port;
    }

    public static DirectoryService getDirectoryService() {
        return directoryService;
    }

    /**
     * Create the default krb config file.
     *
     * @return
     * @throws IOException
     */
    public static String createDefaultConfigFile() throws IOException {
        return createConfigFile(bindUserName + "krb5-", KDC_PORT);
    }

    /**
     * Create the default krb config file with a custom KDC port
     *
     * @return
     * @throws IOException
     */
    public static String createConfigFile(String name, int port) throws IOException {
        Log.info(c, "createConfigFile", "Creating config file: " + name);
        File ccFile = File.createTempFile(name, ".conf");
        if (!FAT_TEST_LOCALRUN) {
            ccFile.deleteOnExit();
        }

        Path outputPath = Paths.get(ccFile.getAbsolutePath());
        Log.info(c, "generateConf", "creating krb.conf file.");
        String conf = "[libdefaults]\n" +
                      "        rdns = false\n" +
                      "        dns_lookup_realm = false\n" +
                      "        udp_preference_limit = 1\n" +
                      "        dns_lookup_kdc = false\n" +
                      "        renew_lifetime = 7d\n" +
                      "        forwardable = true\n" +
                      "        default_realm = " + DOMAIN.toUpperCase() + "\n" +
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

        Log.info(c, "createConfigFile", "krb.conf file contents: " + conf);
        return ccFile.getAbsolutePath();
    }

    /**
     * Create the default krb config file with a custom KDC port
     *
     * @return
     * @throws IOException
     */
    public static String createInvalidConfigFile(String name, int port) throws IOException {
        Log.info(c, "createInvalidConfigFile", "Creating invalid config file: " + name);
        File ccFile = File.createTempFile(name, ".conf");
        if (!FAT_TEST_LOCALRUN) {
            ccFile.deleteOnExit();
        }

        Path outputPath = Paths.get(ccFile.getAbsolutePath());
        Log.info(c, "generateConf", "creating krb.conf file.");
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

        Log.info(c, "createInvalidConfigFile", "krb.conf file contents: " + conf);
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
        Log.info(c, "createTicketCacheShortLife", "Creating short life ticket cache. Renew: " + renew);
        KerberosConfig kdcConfig = kdcServer.getConfig();
        long prevMaxTicket = kdcConfig.getMaximumTicketLifetime();
        long prevMin = kdcConfig.getMinimumTicketLifetime();
        boolean previousRenew = kdcConfig.isRenewableAllowed();
        File ccFile = null;
        try {
            kdcConfig.setMaximumTicketLifetime(MIN_LIFE); // Though forever, this is the minimum max allowed by ApacheDS
            kdcConfig.setRenewableAllowed(renew);
            kdcConfig.setMinimumTicketLifetime(1);
            Log.info(c, "createTicketCacheShortLife", "Creating ticket cache for " + bindPrincipalName);
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

        Log.info(c, "createTicketCacheShortLife", "Created ticket cache: " + ticketCacheFile);

        return ccFile.getAbsolutePath();

    }

    /**
     * Create a keytab file with the default bindUser and password
     *
     * @throws Exception
     */
    public static void createKeyTabFile() throws Exception {
        Log.info(c, "createKeyTabFile", "Creating keytab for " + bindPrincipalName);
        File keyTabTemp = File.createTempFile(bindUserName + "-", ".keytab");
        if (!FAT_TEST_LOCALRUN) {
            keyTabTemp.deleteOnExit();
        }

        KerberosTime timeStamp = new KerberosTime();
        int principalType = 1; // KRB5_NT_PRINCIPAL

        Keytab keytab = Keytab.getInstance();

        List<KeytabEntry> entries = new ArrayList<KeytabEntry>();

        for (Map.Entry<EncryptionType, EncryptionKey> keyEntry : KerberosKeyFactory.getKerberosKeys(
                                                                                                    bindPrincipalName, bindPassword)
                        .entrySet()) {
            final EncryptionKey key = keyEntry.getValue();
            final byte keyVersion = (byte) key.getKeyVersion();
            entries.add(new KeytabEntry(bindPrincipalName, principalType, timeStamp, keyVersion, key));
        }

        keytab.setEntries(entries);
        keytab.write(keyTabTemp);

        keytabFile = keyTabTemp.getAbsolutePath();

        Log.info(c, "createKeyTabFile", "Created keytab: " + keytabFile);
        Log.info(c, "createKeyTabFile", "Keytab actual contents: " + FileUtils.readFile(keytabFile));
        // KeytabEntry doesn't have a nice toString
        for (KeytabEntry key : entries) {
            StringBuffer strBuf = new StringBuffer();
            strBuf.append(" PrincipalName: " + key.getPrincipalName());
            strBuf.append(" PrincipalType: " + key.getPrincipalType());
            strBuf.append(" Key: " + key.getKey());
            strBuf.append(" KeyVersion: " + key.getKeyVersion());
            strBuf.append(" TimeStamp: " + key.getTimeStamp());

            Log.info(c, "createKeyTabFile", "Keytab entry: " + strBuf.toString());
        }

    }
}
