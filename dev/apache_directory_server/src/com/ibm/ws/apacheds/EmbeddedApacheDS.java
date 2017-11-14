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

        File workDir = new File("apacheDS/" + instance);

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
        this.service.getAdminSession().add(entry);
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
     * Stop the LdapServer.
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
}
