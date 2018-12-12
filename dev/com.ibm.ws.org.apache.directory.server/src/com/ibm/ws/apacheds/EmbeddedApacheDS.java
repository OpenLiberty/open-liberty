/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.apacheds;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.directory.api.ldap.codec.controls.search.pagedSearch.PagedResultsFactory;
import org.apache.directory.api.ldap.codec.standalone.StandaloneLdapApiService;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Modification;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.ldap.model.schema.registries.SchemaLoader;
import org.apache.directory.api.ldap.schemaextractor.SchemaLdifExtractor;
import org.apache.directory.api.ldap.schemaextractor.impl.DefaultSchemaLdifExtractor;
import org.apache.directory.api.ldap.schemaloader.LdifSchemaLoader;
import org.apache.directory.api.ldap.schemamanager.impl.DefaultSchemaManager;
import org.apache.directory.api.util.exception.Exceptions;
import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.DefaultDirectoryService;
import org.apache.directory.server.core.api.CacheService;
import org.apache.directory.server.core.api.CoreSession;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.InstanceLayout;
import org.apache.directory.server.core.api.partition.Partition;
import org.apache.directory.server.core.api.schema.SchemaPartition;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmPartition;
import org.apache.directory.server.core.partition.ldif.LdifPartition;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.protocol.shared.transport.TcpTransport;

/**
 * An embedded ApacheDS LDAP server. This implementation is stateless between
 * initializations.
 */
public class EmbeddedApacheDS {

    /** The LDAP server */
    private LdapServer server;

    /** The directory service */
    private DirectoryService service;

    /** The name for this server instance. */
    private final String name;

    static {
        /*
         * Set the following property to enable paged searches. See the following for details:
         *
         * https://issues.apache.org/jira/browse/DIRSERVER-1917
         */
        System.setProperty(StandaloneLdapApiService.CONTROLS_LIST, PagedResultsFactory.class.getName());
    }

    /**
     * Creates a new instance of EmbeddedADS. It initializes the directory
     * service.
     *
     * @param instance The name of this ADS instance.
     * @throws Exception
     *             If something went wrong
     */
    public EmbeddedApacheDS(String instance) throws Exception {
        this.name = instance;

        File workDir = new File("apacheDS/instances/" + instance);

        /*
         * Start fresh each time.
         */
        if (workDir.exists()) {
            /*
             * Can replace with java.nio.file.Files.delete(...) when we stop compiling with
             * Java 6 compliance.
             */
            FileUtils.deleteDirectory(workDir);
        }

        workDir.mkdirs();

        this.initDirectoryService(workDir);
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
     * Get a free port.
     *
     * @return The free port.
     * @throws IOException If a free port could not be found.
     */
    private static int getOpenPort() throws IOException {
        ServerSocket s = new ServerSocket(0);
        int port = s.getLocalPort();
        s.close();
        return port;
    }

    /**
     * Convenience method to add an entry using the admin session.
     *
     * @param entry The entry to add.
     * @throws LdapException If there was an error adding the entry.
     */
    public void add(Entry entry) throws LdapException {
        CoreSession session = this.service.getAdminSession();
        session.add(entry);

    }

    public void modify(Dn dn, Modification mod) throws LdapException {
        CoreSession session = this.service.getAdminSession();
        session.modify(dn, mod);
    }

    /**
     * Add a new partition to the server
     *
     * @param partitionId The partition ID
     * @param partitionDn The partition DN
     * @return The newly added partition
     * @throws Exception If the partition can't be added
     */
    public Partition addPartition(String partitionId, String partitionDn) throws Exception {
        JdbmPartition partition = new JdbmPartition(this.service.getSchemaManager());
        partition.setId(partitionId);
        partition.setPartitionPath(new File(this.service.getInstanceLayout().getPartitionsDirectory(), partitionId).toURI());
        partition.setSuffixDn(new Dn(partitionDn));
        service.addPartition(partition);

        return partition;
    }

    /**
     * Get the name of this LDAP server instance.
     *
     * @return The name.
     */
    public String getInstanceName() {
        return name;
    }

    /**
     * Get the {@link LdapServer} intance.
     *
     * @return The {@link LdapServer} instance.
     */
    public LdapServer getLdapServer() {
        return server;
    }

    /**
     * Get the backing {@link DirectoryService} for this instance.
     *
     * @return The {@link DirectoryService} instance.
     */
    public DirectoryService getService() {
        return service;
    }

    /**
     * Initialize the server. It creates the partition, adds the index, and
     * injects the context entries for the created partitions.
     *
     * @param workDir
     *            the directory to be used for storing the data
     * @throws Exception
     *             if there were some problems while initializing the system
     */
    private void initDirectoryService(final File workDir) throws Exception {
        // Initialize the LDAP service
        this.service = new DefaultDirectoryService();
        this.service.setInstanceLayout(new InstanceLayout(workDir));

        final CacheService cacheService = new CacheService();
        cacheService.initialize(this.service.getInstanceLayout());

        this.service.setCacheService(cacheService);

        /*
         * First load the schema
         */
        this.initSchemaPartition();

        /*
         * Then the system partition. This is a MANDATORY partition.
         * DO NOT add this via addPartition() method, trunk code complains about
         * duplicate partition while initializing.
         */
        final JdbmPartition systemPartition = new JdbmPartition(this.service.getSchemaManager());
        systemPartition.setId("system");
        systemPartition.setPartitionPath(new File(this.service.getInstanceLayout().getPartitionsDirectory(), systemPartition.getId()).toURI());
        systemPartition.setSuffixDn(new Dn(ServerDNConstants.SYSTEM_DN));
        systemPartition.setSchemaManager(this.service.getSchemaManager());

        // mandatory to call this method to set the system partition
        // Note: this system partition might be removed from trunk
        this.service.setSystemPartition(systemPartition);

        /*
         * Disable the ChangeLog system
         */
        this.service.getChangeLog().setEnabled(false);
        this.service.setDenormalizeOpAttrsEnabled(true);

        /*
         * Start the service.
         */
        this.service.startup();

        /*
         * Add Microsoft schema for sAMAccountName and memberOf.
         * These two attributes are not defined in ApacheDS.
         */
        Entry entry = newEntry("cn=microsoft, ou=schema");
        entry.add("objectclass", "metaSchema");
        entry.add("objectclass", "top");
        entry.add("cn", "microsoft");
        add(entry);

        entry = newEntry("ou=attributetypes, cn=microsoft, ou=schema");
        entry.add("objectclass", "organizationalUnit");
        entry.add("objectclass", "top");
        entry.add("ou", "attributetypes");
        add(entry);

        entry = newEntry("m-oid=1.2.840.113556.1.4.221, ou=attributetypes, cn=microsoft, ou=schema");
        entry.add("objectclass", "metaAttributeType");
        entry.add("objectclass", "metaTop");
        entry.add("objectclass", "top");
        entry.add("m-oid", "1.2.840.113556.1.4.221");
        entry.add("m-name", "sAMAccountName");
        entry.add("m-equality", "caseIgnoreMatch");
        entry.add("m-syntax", "1.3.6.1.4.1.1466.115.121.1.15");
        entry.add("m-singleValue", "TRUE");
        add(entry);

        entry = newEntry("m-oid=1.2.840.113556.1.4.222, ou=attributetypes, cn=microsoft, ou=schema");
        entry.add("objectclass", "metaAttributeType");
        entry.add("objectclass", "metaTop");
        entry.add("objectclass", "top");
        entry.add("m-oid", "1.2.840.113556.1.4.222");
        entry.add("m-name", "memberOf");
        entry.add("m-equality", "caseIgnoreMatch");
        entry.add("m-syntax", "1.3.6.1.4.1.1466.115.121.1.15");
        entry.add("m-singleValue", "FALSE");
        add(entry);

        entry = newEntry("ou=objectclasses, cn=microsoft, ou=schema");
        entry.add("objectclass", "organizationalUnit");
        entry.add("objectclass", "top");
        entry.add("ou", "objectClasses");
        add(entry);

        entry = newEntry("m-oid=1.2.840.113556.1.5.6, ou=objectclasses, cn=microsoft, ou=schema");
        entry.add("objectclass", "metaObjectClass");
        entry.add("objectclass", "metaTop");
        entry.add("objectclass", "top");
        entry.add("m-oid", "1.2.840.113556.1.5.6");
        entry.add("m-name", "simulatedMicrosoftSecurityPrincipal");
        entry.add("m-supObjectClass", "top");
        entry.add("m-typeObjectClass", "AUXILIARY");
        entry.add("m-must", "sAMAccountName");
        entry.add("m-may", "memberOf");
        add(entry);

        /*
         * Initialize some WIM specific schema.
         */
        this.initWimAttributes();
        this.initWimObjectClasses();
    }

    /**
     * initialize the schema manager and add the schema partition to diectory
     * service
     *
     * @throws Exception
     *             if the schema LDIF files are not found on the classpath
     */
    private void initSchemaPartition() throws Exception {
        final InstanceLayout instanceLayout = this.service.getInstanceLayout();

        final File schemaPartitionDirectory = new File(instanceLayout.getPartitionsDirectory(), "schema");

        /*
         * Extract the schema on disk (a brand new one) and load the registries
         */
        if (schemaPartitionDirectory.exists()) {
            System.out.println("schema partition already exists, skipping schema extraction");
        } else {
            final SchemaLdifExtractor extractor = new DefaultSchemaLdifExtractor(instanceLayout.getPartitionsDirectory());
            extractor.extractOrCopy();
        }

        final SchemaLoader loader = new LdifSchemaLoader(schemaPartitionDirectory);
        final SchemaManager schemaManager = new DefaultSchemaManager(loader);

        /*
         * We have to load the schema now, otherwise we won't be able
         * to initialize the Partitions, as we won't be able to parse
         * and normalize their suffix Dn
         */
        schemaManager.loadAllEnabled();

        final List<Throwable> errors = schemaManager.getErrors();

        if (errors.size() != 0) {
            throw new Exception(I18n.err(I18n.ERR_317,
                                         Exceptions.printErrors(errors)));
        }

        this.service.setSchemaManager(schemaManager);

        /*
         * Init the LdifPartition with schema
         */
        final LdifPartition schemaLdifPartition = new LdifPartition(schemaManager);
        schemaLdifPartition.setPartitionPath(schemaPartitionDirectory.toURI());

        /*
         * The schema partition
         */
        final SchemaPartition schemaPartition = new SchemaPartition(schemaManager);
        schemaPartition.setWrappedPartition(schemaLdifPartition);
        this.service.setSchemaPartition(schemaPartition);
    }

    /**
     * Convenience method to call {@link CoreSession#lookup(Dn, String...)}.
     *
     * @param dn The distinguished name to lookup.
     * @return The {@link Entry} if it exists.
     * @throws LdapException If there was an issue doing the lookup.
     */
    public Entry lookup(String dn) throws LdapException {
        return this.service.getAdminSession().lookup(new Dn(dn));
    }

    /**
     * Convenience method to create a new entry.
     *
     * @param dn The distinguished name of the new entry.
     * @return The new {@link Entry} instance.
     * @throws LdapInvalidDnException
     * @throws LdapException If there was an issue creating the new entry.
     */
    public Entry newEntry(String dn) throws LdapException {
        return this.service.newEntry(new Dn(dn));
    }

    /**
     * Starts the LdapServer
     *
     * @throws Exception
     */
    public void startServer() throws Exception {
        startServer(getOpenPort());
    }

    public void startServer(int port) throws Exception {
        this.server = new LdapServer();
        this.server.setTransports(new TcpTransport(port));
        this.server.setDirectoryService(this.service);
        this.server.start();
        System.out.println("The server '" + this.name + "' is running on TCP port: " + server.getPort());
    }

    /**
     * Stop the LdapServer. Use stopService for JUnit teardown.
     */
    public void stopServer() {
        this.server.stop();
    }

    /**
     * Stop the LdapServer and the Directory service. Use for JUnit teardown.
     * If the service is not stopped, then future instances with the same name
     * cannot clean up the file directory.
     */
    public void stopService() throws Exception {
        this.server.stop();
        service.shutdown();
    }

    public boolean isStarted() {
        return server.isStarted();
    }

    /**
     * Initialize some WIM specific attributes in the LDAP schema.
     *
     * @throws LdapException If there was a failure initialize the attributes.
     */
    private void initWimAttributes() throws LdapException {

        /*
         * Initialize some branches to hold the attributes.
         */
        Entry entry = newEntry("cn=ibm, ou=schema");
        entry.add("objectclass", "metaSchema");
        entry.add("objectclass", "top");
        entry.add("cn", "ibm");
        add(entry);

        entry = newEntry("ou=attributetypes, cn=ibm, ou=schema");
        entry.add("objectclass", "organizationalUnit");
        entry.add("objectclass", "top");
        entry.add("ou", "attributetypes");
        add(entry);

        /*
         * Attributes will start with the OID 1.3.6.1.4.1.18060.0.4.3.2.1 and increment
         * for each attribute. This OID is one that was assigned to Apache and that they
         * use for their examples.
         */
        entry = newEntry("m-oid=1.3.6.1.4.1.18060.0.4.3.2.1, ou=attributetypes, cn=ibm, ou=schema");
        entry.add("objectclass", "metaAttributeType");
        entry.add("objectclass", "metaTop");
        entry.add("objectclass", "top");
        entry.add("m-oid", "1.3.6.1.4.1.18060.0.4.3.2.1");
        entry.add("m-name", "photoUrl");
        entry.add("m-equality", "caseIgnoreMatch");
        entry.add("m-syntax", "1.3.6.1.4.1.1466.115.121.1.15");
        entry.add("m-singleValue", "TRUE");
        add(entry);

        entry = newEntry("m-oid=1.3.6.1.4.1.18060.0.4.3.2.2, ou=attributetypes, cn=ibm, ou=schema");
        entry.add("objectclass", "metaAttributeType");
        entry.add("objectclass", "metaTop");
        entry.add("objectclass", "top");
        entry.add("m-oid", "1.3.6.1.4.1.18060.0.4.3.2.2");
        entry.add("m-name", "photoURLThumbnail");
        entry.add("m-equality", "caseIgnoreMatch");
        entry.add("m-syntax", "1.3.6.1.4.1.1466.115.121.1.15");
        entry.add("m-singleValue", "TRUE");
        add(entry);

        entry = newEntry("m-oid=1.3.6.1.4.1.18060.0.4.3.2.3, ou=attributetypes, cn=ibm, ou=schema");
        entry.add("objectclass", "metaAttributeType");
        entry.add("objectclass", "metaTop");
        entry.add("objectclass", "top");
        entry.add("m-oid", "1.3.6.1.4.1.18060.0.4.3.2.3");
        entry.add("m-name", "homeStreet");
        entry.add("m-equality", "caseIgnoreMatch");
        entry.add("m-syntax", "1.3.6.1.4.1.1466.115.121.1.15");
        entry.add("m-singleValue", "TRUE");
        add(entry);

        entry = newEntry("m-oid=1.3.6.1.4.1.18060.0.4.3.2.4, ou=attributetypes, cn=ibm, ou=schema");
        entry.add("objectclass", "metaAttributeType");
        entry.add("objectclass", "metaTop");
        entry.add("objectclass", "top");
        entry.add("m-oid", "1.3.6.1.4.1.18060.0.4.3.2.4");
        entry.add("m-name", "homeCity");
        entry.add("m-equality", "caseIgnoreMatch");
        entry.add("m-syntax", "1.3.6.1.4.1.1466.115.121.1.15");
        entry.add("m-singleValue", "TRUE");
        add(entry);

        entry = newEntry("m-oid=1.3.6.1.4.1.18060.0.4.3.2.5, ou=attributetypes, cn=ibm, ou=schema");
        entry.add("objectclass", "metaAttributeType");
        entry.add("objectclass", "metaTop");
        entry.add("objectclass", "top");
        entry.add("m-oid", "1.3.6.1.4.1.18060.0.4.3.2.5");
        entry.add("m-name", "homeStateOrProvinceName");
        entry.add("m-equality", "caseIgnoreMatch");
        entry.add("m-syntax", "1.3.6.1.4.1.1466.115.121.1.15");
        entry.add("m-singleValue", "TRUE");
        add(entry);

        entry = newEntry("m-oid=1.3.6.1.4.1.18060.0.4.3.2.6, ou=attributetypes, cn=ibm, ou=schema");
        entry.add("objectclass", "metaAttributeType");
        entry.add("objectclass", "metaTop");
        entry.add("objectclass", "top");
        entry.add("m-oid", "1.3.6.1.4.1.18060.0.4.3.2.6");
        entry.add("m-name", "homePostalCode");
        entry.add("m-equality", "caseIgnoreMatch");
        entry.add("m-syntax", "1.3.6.1.4.1.1466.115.121.1.15");
        entry.add("m-singleValue", "TRUE");
        add(entry);

        entry = newEntry("m-oid=1.3.6.1.4.1.18060.0.4.3.2.7, ou=attributetypes, cn=ibm, ou=schema");
        entry.add("objectclass", "metaAttributeType");
        entry.add("objectclass", "metaTop");
        entry.add("objectclass", "top");
        entry.add("m-oid", "1.3.6.1.4.1.18060.0.4.3.2.7");
        entry.add("m-name", "homeCountryName");
        entry.add("m-equality", "caseIgnoreMatch");
        entry.add("m-syntax", "1.3.6.1.4.1.1466.115.121.1.15");
        entry.add("m-singleValue", "TRUE");
        add(entry);

        entry = newEntry("m-oid=1.3.6.1.4.1.18060.0.4.3.2.8, ou=attributetypes, cn=ibm, ou=schema");
        entry.add("objectclass", "metaAttributeType");
        entry.add("objectclass", "metaTop");
        entry.add("objectclass", "top");
        entry.add("m-oid", "1.3.6.1.4.1.18060.0.4.3.2.8");
        entry.add("m-name", "businessStreet");
        entry.add("m-equality", "caseIgnoreMatch");
        entry.add("m-syntax", "1.3.6.1.4.1.1466.115.121.1.15");
        entry.add("m-singleValue", "TRUE");
        add(entry);

        entry = newEntry("m-oid=1.3.6.1.4.1.18060.0.4.3.2.9, ou=attributetypes, cn=ibm, ou=schema");
        entry.add("objectclass", "metaAttributeType");
        entry.add("objectclass", "metaTop");
        entry.add("objectclass", "top");
        entry.add("m-oid", "1.3.6.1.4.1.18060.0.4.3.2.9");
        entry.add("m-name", "businessCity");
        entry.add("m-equality", "caseIgnoreMatch");
        entry.add("m-syntax", "1.3.6.1.4.1.1466.115.121.1.15");
        entry.add("m-singleValue", "TRUE");
        add(entry);

        entry = newEntry("m-oid=1.3.6.1.4.1.18060.0.4.3.2.10, ou=attributetypes, cn=ibm, ou=schema");
        entry.add("objectclass", "metaAttributeType");
        entry.add("objectclass", "metaTop");
        entry.add("objectclass", "top");
        entry.add("m-oid", "1.3.6.1.4.1.18060.0.4.3.2.10");
        entry.add("m-name", "businessStateOrProvinceName");
        entry.add("m-equality", "caseIgnoreMatch");
        entry.add("m-syntax", "1.3.6.1.4.1.1466.115.121.1.15");
        entry.add("m-singleValue", "TRUE");
        add(entry);

        entry = newEntry("m-oid=1.3.6.1.4.1.18060.0.4.3.2.11, ou=attributetypes, cn=ibm, ou=schema");
        entry.add("objectclass", "metaAttributeType");
        entry.add("objectclass", "metaTop");
        entry.add("objectclass", "top");
        entry.add("m-oid", "1.3.6.1.4.1.18060.0.4.3.2.11");
        entry.add("m-name", "businessPostalCode");
        entry.add("m-equality", "caseIgnoreMatch");
        entry.add("m-syntax", "1.3.6.1.4.1.1466.115.121.1.15");
        entry.add("m-singleValue", "TRUE");
        add(entry);

        entry = newEntry("m-oid=1.3.6.1.4.1.18060.0.4.3.2.12, ou=attributetypes, cn=ibm, ou=schema");
        entry.add("objectclass", "metaAttributeType");
        entry.add("objectclass", "metaTop");
        entry.add("objectclass", "top");
        entry.add("m-oid", "1.3.6.1.4.1.18060.0.4.3.2.12");
        entry.add("m-name", "businessCountryName");
        entry.add("m-equality", "caseIgnoreMatch");
        entry.add("m-syntax", "1.3.6.1.4.1.1466.115.121.1.15");
        entry.add("m-singleValue", "TRUE");
        add(entry);

        entry = newEntry("m-oid=1.3.6.1.4.1.18060.0.4.3.2.13, ou=attributetypes, cn=ibm, ou=schema");
        entry.add("objectclass", "metaAttributeType");
        entry.add("objectclass", "metaTop");
        entry.add("objectclass", "top");
        entry.add("m-oid", "1.3.6.1.4.1.18060.0.4.3.2.13");
        entry.add("m-name", "middleName");
        entry.add("m-equality", "caseIgnoreMatch");
        entry.add("m-syntax", "1.3.6.1.4.1.1466.115.121.1.15");
        entry.add("m-singleValue", "TRUE");
        add(entry);

        entry = newEntry("m-oid=1.3.6.1.4.1.18060.0.4.3.2.14, ou=attributetypes, cn=ibm, ou=schema");
        entry.add("objectclass", "metaAttributeType");
        entry.add("objectclass", "metaTop");
        entry.add("objectclass", "top");
        entry.add("m-oid", "1.3.6.1.4.1.18060.0.4.3.2.14");
        entry.add("m-name", "honorificSuffix");
        entry.add("m-equality", "caseIgnoreMatch");
        entry.add("m-syntax", "1.3.6.1.4.1.1466.115.121.1.15");
        entry.add("m-singleValue", "TRUE");
        add(entry);

        entry = newEntry("m-oid=1.3.6.1.4.1.18060.0.4.3.2.15, ou=attributetypes, cn=ibm, ou=schema");
        entry.add("objectclass", "metaAttributeType");
        entry.add("objectclass", "metaTop");
        entry.add("objectclass", "top");
        entry.add("m-oid", "1.3.6.1.4.1.18060.0.4.3.2.15");
        entry.add("m-name", "honorificPrefix");
        entry.add("m-equality", "caseIgnoreMatch");
        entry.add("m-syntax", "1.3.6.1.4.1.1466.115.121.1.15");
        entry.add("m-singleValue", "TRUE");
        add(entry);

        entry = newEntry("m-oid=1.3.6.1.4.1.18060.0.4.3.2.16, ou=attributetypes, cn=ibm, ou=schema");
        entry.add("objectclass", "metaAttributeType");
        entry.add("objectclass", "metaTop");
        entry.add("objectclass", "top");
        entry.add("m-oid", "1.3.6.1.4.1.18060.0.4.3.2.16");
        entry.add("m-name", "nickName");
        entry.add("m-equality", "caseIgnoreMatch");
        entry.add("m-syntax", "1.3.6.1.4.1.1466.115.121.1.15");
        entry.add("m-singleValue", "TRUE");
        add(entry);

        entry = newEntry("m-oid=1.3.6.1.4.1.18060.0.4.3.2.17, ou=attributetypes, cn=ibm, ou=schema");
        entry.add("objectclass", "metaAttributeType");
        entry.add("objectclass", "metaTop");
        entry.add("objectclass", "top");
        entry.add("m-oid", "1.3.6.1.4.1.18060.0.4.3.2.17");
        entry.add("m-name", "profileUrl");
        entry.add("m-equality", "caseIgnoreMatch");
        entry.add("m-syntax", "1.3.6.1.4.1.1466.115.121.1.15");
        entry.add("m-singleValue", "TRUE");
        add(entry);

        entry = newEntry("m-oid=1.3.6.1.4.1.18060.0.4.3.2.18, ou=attributetypes, cn=ibm, ou=schema");
        entry.add("objectclass", "metaAttributeType");
        entry.add("objectclass", "metaTop");
        entry.add("objectclass", "top");
        entry.add("m-oid", "1.3.6.1.4.1.18060.0.4.3.2.18");
        entry.add("m-name", "locale");
        entry.add("m-equality", "caseIgnoreMatch");
        entry.add("m-syntax", "1.3.6.1.4.1.1466.115.121.1.15");
        entry.add("m-singleValue", "TRUE");
        add(entry);

        entry = newEntry("m-oid=1.3.6.1.4.1.18060.0.4.3.2.19, ou=attributetypes, cn=ibm, ou=schema");
        entry.add("objectclass", "metaAttributeType");
        entry.add("objectclass", "metaTop");
        entry.add("objectclass", "top");
        entry.add("m-oid", "1.3.6.1.4.1.18060.0.4.3.2.19");
        entry.add("m-name", "timezone");
        entry.add("m-equality", "caseIgnoreMatch");
        entry.add("m-syntax", "1.3.6.1.4.1.1466.115.121.1.15");
        entry.add("m-singleValue", "TRUE");
        add(entry);

        entry = newEntry("m-oid=1.3.6.1.4.1.18060.0.4.3.2.20, ou=attributetypes, cn=ibm, ou=schema");
        entry.add("objectclass", "metaAttributeType");
        entry.add("objectclass", "metaTop");
        entry.add("objectclass", "top");
        entry.add("m-oid", "1.3.6.1.4.1.18060.0.4.3.2.20");
        entry.add("m-name", "active");
        entry.add("m-equality", "booleanMatch");
        entry.add("m-syntax", "1.3.6.1.4.1.1466.115.121.1.7");
        entry.add("m-singleValue", "TRUE");
        add(entry);

        entry = newEntry("m-oid=1.3.6.1.4.1.18060.0.4.3.2.21, ou=attributetypes, cn=ibm, ou=schema");
        entry.add("objectclass", "metaAttributeType");
        entry.add("objectclass", "metaTop");
        entry.add("objectclass", "top");
        entry.add("m-oid", "1.3.6.1.4.1.18060.0.4.3.2.21");
        entry.add("m-name", "ims");
        entry.add("m-equality", "caseIgnoreMatch");
        entry.add("m-syntax", "1.3.6.1.4.1.1466.115.121.1.15");
        entry.add("m-singleValue", "FALSE");
        add(entry);

        entry = newEntry("m-oid=1.3.6.1.4.1.18060.0.4.3.2.22, ou=attributetypes, cn=ibm, ou=schema");
        entry.add("objectclass", "metaAttributeType");
        entry.add("objectclass", "metaTop");
        entry.add("objectclass", "top");
        entry.add("m-oid", "1.3.6.1.4.1.18060.0.4.3.2.22");
        entry.add("m-name", "extendedProperty1");
        entry.add("m-equality", "caseIgnoreMatch");
        entry.add("m-syntax", "1.3.6.1.4.1.1466.115.121.1.15");
        entry.add("m-singleValue", "TRUE");
        add(entry);

        entry = newEntry("m-oid=1.3.6.1.4.1.18060.0.4.3.2.23, ou=attributetypes, cn=ibm, ou=schema");
        entry.add("objectclass", "metaAttributeType");
        entry.add("objectclass", "metaTop");
        entry.add("objectclass", "top");
        entry.add("m-oid", "1.3.6.1.4.1.18060.0.4.3.2.23");
        entry.add("m-name", "extendedProperty2");
        entry.add("m-equality", "caseIgnoreMatch");
        entry.add("m-syntax", "1.3.6.1.4.1.1466.115.121.1.15");
        entry.add("m-singleValue", "FALSE");
        add(entry);

    }

    /**
     * Initialize some WIM specific object classes in the WIM schema. These classes include an
     * extension of inetOrgPerson (wimInetOrgPerson) and groupOfNames (wimGroupOfNames). These
     * extensions contain extra attributes that generally match up with the WIM property names
     * for PersonAccounta and Group respectively as well as some extended properties.
     *
     * @throws LdapException If there was a failure initialize the object classes.
     */
    private void initWimObjectClasses() throws LdapException {

        /*
         * Create the ou=objectclasses,cn=ibm,ou=schema branch.
         */
        Entry entry = newEntry("ou=objectclasses, cn=ibm, ou=schema");
        entry.add("objectclass", "organizationalUnit");
        entry.add("objectclass", "top");
        entry.add("ou", "objectClasses");
        add(entry);

        /*
         * Object classes will start with the OID 1.3.6.1.4.1.18060.0.4.3.1.1 and increment
         * for each object class. This OID is one that was assigned to Apache and that they
         * use for their examples.
         */

        /**
         * <pre>
         * ObjectClass:
         *    wimInetOrgPerson            - 1.3.6.1.4.1.18060.0.4.3.1.1
         *
         * Attributes:
         *    photoUrl                    - 1.3.6.1.4.1.18060.0.4.3.2.1
         *    photoURLThumbnail           - 1.3.6.1.4.1.18060.0.4.3.2.2
         *    homeStreet                  - 1.3.6.1.4.1.18060.0.4.3.2.3
         *    homeCity                    - 1.3.6.1.4.1.18060.0.4.3.2.4
         *    homeStateOrProvinceName     - 1.3.6.1.4.1.18060.0.4.3.2.5
         *    homePostalCode              - 1.3.6.1.4.1.18060.0.4.3.2.6
         *    homeCountryName             - 1.3.6.1.4.1.18060.0.4.3.2.7
         *    businessStreet              - 1.3.6.1.4.1.18060.0.4.3.2.8
         *    businessCity                - 1.3.6.1.4.1.18060.0.4.3.2.9
         *    businessStateOrProvinceName - 1.3.6.1.4.1.18060.0.4.3.2.10
         *    businessPostalCode          - 1.3.6.1.4.1.18060.0.4.3.2.11
         *    businessCountryName         - 1.3.6.1.4.1.18060.0.4.3.2.12
         *    middleName                  - 1.3.6.1.4.1.18060.0.4.3.2.13
         *    honorificSuffix             - 1.3.6.1.4.1.18060.0.4.3.2.14
         *    honorificPrefix             - 1.3.6.1.4.1.18060.0.4.3.2.15
         *    nickName                    - 1.3.6.1.4.1.18060.0.4.3.2.16
         *    profileUrl                  - 1.3.6.1.4.1.18060.0.4.3.2.17
         *    locale                      - 1.3.6.1.4.1.18060.0.4.3.2.18
         *    timezone                    - 1.3.6.1.4.1.18060.0.4.3.2.19
         *    active                      - 1.3.6.1.4.1.18060.0.4.3.2.20
         *    ims                         - 1.3.6.1.4.1.18060.0.4.3.2.21
         *    extendedProperty1           - 1.3.6.1.4.1.18060.0.4.3.2.22
         *    extendedProperty2           - 1.3.6.1.4.1.18060.0.4.3.2.23
         * </pre>
         */
        entry = newEntry("m-oid=1.3.6.1.4.1.18060.0.4.3.1.1, ou=objectclasses, cn=ibm, ou=schema");
        entry.add("objectclass", "metaObjectClass");
        entry.add("objectclass", "metaTop");
        entry.add("objectclass", "top");
        entry.add("m-oid", "1.3.6.1.4.1.18060.0.4.3.1.1");
        entry.add("m-name", "wimInetOrgPerson");
        entry.add("m-supObjectClass", "inetOrgPerson");
        entry.add("m-typeObjectClass", "STRUCTURAL");
        entry.add("m-may", "photoUrl");
        entry.add("m-may", "photoURLThumbnail");
        entry.add("m-may", "homeStreet");
        entry.add("m-may", "homeCity");
        entry.add("m-may", "homeStateOrProvinceName");
        entry.add("m-may", "homePostalCode");
        entry.add("m-may", "homeCountryName");
        entry.add("m-may", "businessStreet");
        entry.add("m-may", "businessCity");
        entry.add("m-may", "businessStateOrProvinceName");
        entry.add("m-may", "businessPostalCode");
        entry.add("m-may", "businessCountryName");
        entry.add("m-may", "middleName");
        entry.add("m-may", "honorificSuffix");
        entry.add("m-may", "honorificPrefix");
        entry.add("m-may", "nickName");
        entry.add("m-may", "profileUrl");
        entry.add("m-may", "locale");
        entry.add("m-may", "timezone");
        entry.add("m-may", "active");
        entry.add("m-may", "ims");
        entry.add("m-may", "extendedProperty1");
        entry.add("m-may", "extendedProperty2");
        add(entry);

        /**
         * <pre>
         * ObjectClass:
         *    wimGroupOfNames             - 1.3.6.1.4.1.18060.0.4.3.1.2
         *
         * Attributes:
         *    extendedProperty1           - 1.3.6.1.4.1.18060.0.4.3.2.22
         *    extendedProperty2           - 1.3.6.1.4.1.18060.0.4.3.2.23
         * </pre>
         */
        entry = newEntry("m-oid=1.3.6.1.4.1.18060.0.4.3.1.2, ou=objectclasses, cn=ibm, ou=schema");
        entry.add("objectclass", "metaObjectClass");
        entry.add("objectclass", "metaTop");
        entry.add("objectclass", "top");
        entry.add("m-oid", "1.3.6.1.4.1.18060.0.4.3.1.2");
        entry.add("m-name", "wimGroupOfNames");
        entry.add("m-supObjectClass", "groupOfNames");
        entry.add("m-typeObjectClass", "STRUCTURAL");
        entry.add("m-may", "extendedProperty1");
        entry.add("m-may", "extendedProperty2");
        add(entry);
    }
}
