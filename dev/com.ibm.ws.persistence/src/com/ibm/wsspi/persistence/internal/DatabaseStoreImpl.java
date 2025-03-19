/*******************************************************************************
 * Copyright (c) 2014, 2025 IBM Corporation and others.
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
package com.ibm.wsspi.persistence.internal;

import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import javax.sql.DataSource;
import javax.transaction.Transaction;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.auth.data.AuthData;
import com.ibm.ws.LocalTransaction.LocalTransactionCoordinator;
import com.ibm.ws.LocalTransaction.LocalTransactionCurrent;
import com.ibm.ws.jdbc.WSDataSource;
import com.ibm.ws.tx.embeddable.EmbeddableWebSphereTransactionManager;
import com.ibm.wsspi.persistence.DatabaseStore;
import com.ibm.wsspi.persistence.InMemoryMappingFile;
import com.ibm.wsspi.persistence.PersistenceService;
import com.ibm.wsspi.persistence.PersistenceServiceUnit;
import com.ibm.wsspi.persistence.PersistenceServiceUnitConfig;
import com.ibm.wsspi.resource.ResourceConfig;
import com.ibm.wsspi.resource.ResourceConfigFactory;
import com.ibm.wsspi.resource.ResourceFactory;

import io.openliberty.checkpoint.spi.CheckpointPhase;

/**
 * Uses a database as a persistent store.
 */
@Component(name = "com.ibm.ws.persistence.databaseStore",
           service = { DatabaseStore.class },
           configurationPolicy = ConfigurationPolicy.REQUIRE)
public class DatabaseStoreImpl implements DatabaseStore {
    private static final TraceComponent tc = Tr.register(DatabaseStoreImpl.class);
    private static final String EOLN = String.format("%n");

    private static final String BATCH_PKG_PREFIX = "com.ibm.jbatch.container.persistence.jpa.";
    private static final String PERSISTENT_EXECUTOR_PKG_PREFIX = "com.ibm.ws.concurrent.persistent.";

    private static enum SpecialEntitySet {
        BATCH, PERSISTENT_EXECUTOR, NONE
    };

    private SpecialEntitySet recognizeSpecialEntityPackage(String className) {
        if (className.startsWith(BATCH_PKG_PREFIX)) {
            return SpecialEntitySet.BATCH;
        } else if (className.startsWith(PERSISTENT_EXECUTOR_PKG_PREFIX)) {
            return SpecialEntitySet.PERSISTENT_EXECUTOR;
        } else {
            return SpecialEntitySet.NONE;
        }
    }

    /**
     * Reference to the default authentication data.
     */
    private final ServiceReference<?> authDataRef;

    /**
     * Resource factory for the data source.
     */
    private final ResourceFactory dataSourceFactory;

    /**
     * Indicates if this database store instance has been deactivated.
     */
    private volatile boolean deactivated;

    /**
     * A service that controls local transactions.
     */
    private final LocalTransactionCurrent localTranCurrent;

    /**
     * Resource factory for the non-transactional data source.
     */
    private final ResourceFactory nonJTADataSourceFactory;

    /**
     * The persistence service.
     */
    private final PersistenceService persistenceService;

    /**
     * Configuration properties for this instance.
     */
    private final Map<String, ?> properties;

    /**
     * Schema from config
     */
    private final String schema;

    /**
     * Table prefix from config
     */
    private final String tablePrefix;

    /**
     * Key generation strategy from config
     */
    private final String strategyConfig;

    /**
     * Length of schema String
     */
    final int schemaLength;

    /**
     * Length of tablePrefix String
     */
    final int tablePrefixLength;

    /**
     * Resource config factory.
     */
    private final ResourceConfigFactory resourceConfigFactory;

    /**
     * Transaction manager.
     */
    private final EmbeddableWebSphereTransactionManager tranMgr;

    @Activate
    public DatabaseStoreImpl(Map<String, ?> properties, //

                             // static mandatory references
                             @Reference(name = "DataSourceFactory", target = "(id=unbound)") ResourceFactory dataSourceFactory, //
                             @Reference LocalTransactionCurrent localTranCurrent, //
                             @Reference PersistenceService persistenceService, //
                             @Reference ResourceConfigFactory resourceConfigFactory, //
                             @Reference EmbeddableWebSphereTransactionManager tranMgr, //

                             // static optional refereences
                             @Reference(name = "AuthData",
                                 service = AuthData.class,
                                 cardinality = ReferenceCardinality.OPTIONAL,
                                 policy = ReferencePolicy.STATIC,
                                 target = "(id=unbound)",
                                 policyOption = ReferencePolicyOption.GREEDY) ServiceReference<AuthData> authDataRef, //
                             @Reference(name = "NonJTADataSourceFactory",
                                 cardinality = ReferenceCardinality.OPTIONAL,
                                 policy = ReferencePolicy.STATIC,
                                 policyOption = ReferencePolicyOption.GREEDY,
                                 target = "(id=unbound)") ResourceFactory nonJTADataSourceFactory
                    ) throws Exception {

        this.properties = properties;

        // static mandatory references
        this.dataSourceFactory = dataSourceFactory;
        this.localTranCurrent = localTranCurrent;
        this.persistenceService = persistenceService;
        this.resourceConfigFactory = resourceConfigFactory;
        this.tranMgr = tranMgr;

        // static optional references
        this.authDataRef = authDataRef;
        this.nonJTADataSourceFactory = nonJTADataSourceFactory;

        // The database store should be lazily initialized by the components using it, so there is no need
        // for the database store implementation to have its own lazy initialization.

        schema = (String) this.properties.get("schema");
        schemaLength = schema == null ? -1 : schema.length();
        tablePrefix = (String) this.properties.get("tablePrefix");
        tablePrefixLength = tablePrefix.length();
        strategyConfig = (String) this.properties.get("keyGenerationStrategy");
    }

    /**
     * @see com.ibm.wsspi.persistence.DatabaseStore#createPersistenceServiceUnit(java.lang.ClassLoader, java.lang.String[])
     */
    @Override
    public PersistenceServiceUnit createPersistenceServiceUnit(ClassLoader loader, String... entityClassNames) throws Exception {
        return createPersistenceServiceUnit(loader, new HashMap<String, Object>(), entityClassNames);
    }

    /**
     * @see com.ibm.wsspi.persistence.DatabaseStore#createPersistenceServiceUnit(java.lang.ClassLoader, java.util.Map properties, java.lang.String[])
     */
    @Override
    public PersistenceServiceUnit createPersistenceServiceUnit(ClassLoader loader, Map properties, String... entityClassNames) throws Exception {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "createPersistenceServiceUnit", loader, Arrays.asList(entityClassNames));

        Map<String, Object> puProps = new HashMap<String, Object>();
        for (int i = 0; i < tablePrefixLength; i++) {
            int codepoint = tablePrefix.codePointAt(i);
            if (!Character.isLetterOrDigit(codepoint) && codepoint != 95) { // alphanumeric or _ character
                Tr.error(tc, "ILLEGAL_IDENTIFIER_CWWKD0200E", tablePrefix);
                throw new IllegalArgumentException(Tr.formatMessage(tc, "ILLEGAL_IDENTIFIER_CWWKD0200E", tablePrefix));
            }
        }

        for (int i = 0; i < schemaLength; i++) {
            int codepoint = schema.codePointAt(i);
            if (!Character.isLetterOrDigit(codepoint) && codepoint != 95) { // alphanumeric or _ character
                Tr.error(tc, "ILLEGAL_IDENTIFIER_CWWKD0201E", schema);
                throw new IllegalArgumentException(Tr.formatMessage(tc, "ILLEGAL_IDENTIFIER_CWWKD0201E", schema));
            }
        }

        // Look for EclipseLink persistence properties
        if (this.properties.get("persistenceProperties.0.config.referenceType") != null) {
            for (String key : this.properties.keySet()) {
                if (key.startsWith("persistenceProperties.0") && !key.equals("persistenceProperties.0.config.referenceType")) {
                    Object value = this.properties.get(key);
                    key = key.substring(24);
                    puProps.put(key, value);
                }
            }
        }

        // Look for additional configuration properties
        int isolationLevel = Connection.TRANSACTION_READ_COMMITTED;
        if (properties.get("transactionIsolationLevel") != null) {
            Object configIsolationLevel = properties.get("transactionIsolationLevel");

            if(configIsolationLevel instanceof Number) {
                int newisolationLevel = ((Number) configIsolationLevel).intValue();
                if(supportsIsolationLevel(newisolationLevel)) {
                    isolationLevel = newisolationLevel;
                } else {
                    throw new UnsupportedOperationException(Tr.formatMessage(tc, "UNSUPPORTED_ISOLATION_LEVEL_CWWKD0293E", isolationLevel));
                }
            }
        }

        ResourceConfig resourceInfo = resourceConfigFactory.createResourceConfig(DataSource.class.getName());
        resourceInfo.setSharingScope(ResourceConfig.SHARING_SCOPE_SHAREABLE);
        resourceInfo.setIsolationLevel(isolationLevel);
        resourceInfo.setResAuthType(ResourceConfig.AUTH_CONTAINER);

        if (authDataRef != null) {
            String authDataId = (String) authDataRef.getProperty("id");
            resourceInfo.addLoginProperty("DefaultPrincipalMapping",
                                          authDataId.matches(".*(\\]/).*(\\[default-\\d*\\])")
                                                          ? (String) authDataRef.getProperty("config.displayId")
                                                          : authDataId);
            if (trace && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "DefaultPrincipalMapping " + resourceInfo);
                Tr.debug(this, tc, "AuthData id = " + authDataId);
            }
        }

        // NOTE the order is important for the calls to createResource below on
        // dataSourceFactory and nonJTADataSourceFactory.
        // DO NOT change the order they are called here without addressing the fact
        // that the sharing scope is modified to unshareable on the resourceInfo before calling
        // nonJTADataSourceFactory.createResource
        SpecialEntitySet entitySet = entityClassNames.length == 0 ? SpecialEntitySet.NONE : recognizeSpecialEntityPackage(entityClassNames[0]);
        DataSource dataSource = (DataSource) dataSourceFactory.createResource(resourceInfo);
        // Check the product name for support and compute strategy
        String strategy = checkSupportedDBProductComputeStrategy(dataSource, entitySet);

        DataSource nonJTADataSource;
        if (nonJTADataSourceFactory == null)
            nonJTADataSource = null;
        else {
            resourceInfo.setSharingScope(ResourceConfig.SHARING_SCOPE_UNSHAREABLE);
            nonJTADataSource = (DataSource) nonJTADataSourceFactory.createResource(resourceInfo);
        }

        if (deactivated) {
            Tr.error(tc, "DEACTIVATED_CWWKD0202E");
            String errMsg = Tr.formatMessage(tc, "DEACTIVATED_CWWKD0202E");

            if (trace && tc.isEntryEnabled())
                Tr.exit(this, tc, "createPersistenceServiceUnit", "deactivated");
            throw new IllegalStateException(errMsg);
        }

        // TODO: replace temporary code specific to persistentExecutor with general solution that applies the
        // table prefix, schema, and keyGenerationStrategy to all entities

        List<InMemoryMappingFile> inMemoryFiles;
        if (entitySet.equals(SpecialEntitySet.PERSISTENT_EXECUTOR)) {
            String ormFileContents = createOrmFileContentsForPersistentExecutor(strategy);
            inMemoryFiles = Collections.singletonList(new InMemoryMappingFile(ormFileContents.getBytes("UTF-8")));
        } else if (entitySet.equals(SpecialEntitySet.BATCH)) {
            String ormFileContents = createOrmFileContentsForBatch(entityClassNames, strategy);
            inMemoryFiles = Collections.singletonList(new InMemoryMappingFile(ormFileContents.getBytes("UTF-8")));
        } else {
            // hidden internal non-ship property for experimenting with Jakarta Data
            @SuppressWarnings("unchecked") // TODO if persistence service could read @Table from the entity class, we could remove this hack
            LinkedHashSet<String> tableNames = (LinkedHashSet<String>) properties.get("io.openliberty.persistence.internal.tableNames");
            String[] entityClassEntries = (String[]) properties.get("io.openliberty.persistence.internal.entityClassInfo");
            InMemoryMappingFile ormFile = createOrmFile(schema, tablePrefix, tableNames, entityClassNames, entityClassEntries);
            inMemoryFiles = (List<InMemoryMappingFile>) properties.get("io.openliberty.persistence.internal.generatedEntities");
            if (ormFile != null)
                if (inMemoryFiles == null) {
                    inMemoryFiles = Collections.singletonList(ormFile);
                } else {
                    inMemoryFiles = new ArrayList<>(inMemoryFiles);
                    inMemoryFiles.add(ormFile);
                }
        }

        PersistenceServiceUnitConfig config = new PersistenceServiceUnitConfig();
        config.setClasses(Arrays.asList(entityClassNames));
        config.setConsumerLoader(loader);
        config.setProperties(puProps);
        if (inMemoryFiles != null)
            config.setInMemoryMappingFiles(inMemoryFiles);
        config.setJtaDataSource(dataSource);
        if (nonJTADataSource != null)
            config.setNonJtaDataSource(nonJTADataSource);

        if (deactivated) {
            Tr.error(tc, "DEACTIVATED_CWWKD0202E");
            String errMsg = Tr.formatMessage(tc, "DEACTIVATED_CWWKD0202E");
            if (trace && tc.isEntryEnabled())
                Tr.exit(this, tc, "createPersistenceServiceUnit", "deactivated");
            throw new IllegalStateException(errMsg);
        }

        PersistenceServiceUnit persistenceServiceUnit = persistenceService.createPersistenceServiceUnit(config);
        boolean successful = false;
        try {
            if (deactivated) {
                if (trace && tc.isEntryEnabled())
                    Tr.exit(this, tc, "createPersistenceServiceUnit", "deactivated");
                String errMsg = Tr.formatMessage(tc, "DEACTIVATED_CWWKD0202E");
                throw new IllegalStateException(errMsg);
            }

            // ignore table creation for extra PersistenceServiceUnit that persistent executor creates to allow TRANSACTION_READ_UNCOMMITTED
            // TODO is there a better way to accomplish this?
            if (!(entitySet.equals(SpecialEntitySet.PERSISTENT_EXECUTOR) && entityClassNames.length == 1)) {
                boolean createTables = (Boolean) this.properties.get("createTables");
                boolean dropTables = (Boolean) this.properties.get("dropTables");
                if (createTables || dropTables) {
                    CheckpointPhase.onRestore(() -> dropAndOrCreateTables(persistenceServiceUnit, createTables, dropTables));
                }
            }

            if (deactivated) {
                if (trace && tc.isEntryEnabled())
                    Tr.exit(this, tc, "createPersistenceServiceUnit", "deactivated");
                String errMsg = Tr.formatMessage(tc, "DEACTIVATED_CWWKD0202E");
                throw new IllegalStateException(errMsg);
            }

            successful = true;
        } finally {
            if (!successful)
                persistenceServiceUnit.close();
        }

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "createPersistenceServiceUnit", persistenceServiceUnit);
        return persistenceServiceUnit;
    }

    private String checkSupportedDBProductComputeStrategy(DataSource dataSource, SpecialEntitySet entitySet) throws Exception {
        boolean isAutoStrategy = "AUTO".equals(strategyConfig);
        AtomicReference<String> strategy = new AtomicReference<>(isAutoStrategy ? "CHECKPOINT" : strategyConfig);
        CheckpointPhase.onRestore(() -> {
            String dbProductName = getDatabaseProductName(((WSDataSource) dataSource)).toLowerCase();
            if (dbProductName.contains("informix") || dbProductName.startsWith("ids/")) {
                // Defect 168450 - PersistenceService is currently disabled when running with Informix.
                // Once the informix issues are fixed, this exception will be removed.
                // When informix is using a DB2 driver, the product name is determined by db2 and is
                // similar to: ids/nt64
                throw new UnsupportedOperationException(dbProductName);
            }
            if (isAutoStrategy) {
                String autoStrategy = dbProductName.contains("oracle") ? "SEQUENCE"
                                : dbProductName.contains("adaptive server") || dbProductName.contains("sybase") ? "TABLE"
                                                : "IDENTITY";
                strategy.set(autoStrategy);
            }
        });
        String result = strategy.get();
        if ((entitySet  == SpecialEntitySet.BATCH || entitySet == SpecialEntitySet.PERSISTENT_EXECUTOR) && "CHECKPOINT".equals(result)) {
            // BATCH and PERSISTENT_EXECUTOR currently require a strategy so we must find one somehow
            // without requiring a connection to the DataSource

            // TODO need to add logic that checks the driver class name or other config
            // to determine a strategy without connecting to the DB here

            // TODO need to add an error message saying that for InstantOn (checkpoint)
            // a strategy must be configured with the 'keyGenerationStrategy' attribute
            throw new UnsupportedOperationException();
        }
        return result;
    }

    protected String createOrmFileContentsForPersistentExecutor(String strategy) {

        final boolean trace = TraceComponent.isAnyTracingEnabled();

        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "createOrmFileContentsForPersistentExecutor");

        StringBuilder orm = new StringBuilder(4 * tablePrefixLength + schemaLength + 703)
                        .append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>").append(EOLN)
                        .append("<entity-mappings xmlns=\"http://xmlns.jcp.org/xml/ns/persistence/orm\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://xmlns.jcp.org/xml/ns/persistence/orm http://xmlns.jcp.org/xml/ns/persistence/orm_2_1.xsd\" version=\"2.1\">").append(EOLN);
        if (schemaLength >= 0)
            orm.append(" <schema>").append(schema).append("</schema>").append(EOLN);
        orm
                        .append(" <entity class=\"com.ibm.ws.concurrent.persistent.db.Partition\">").append(EOLN)
                        .append("  <table name=\"").append(tablePrefix).append("PART\">").append(EOLN)
                        .append("   <unique-constraint>").append(EOLN)
                        .append("    <column-name>EXECUTOR</column-name>").append(EOLN)
                        .append("    <column-name>HOSTNAME</column-name>").append(EOLN)
                        .append("    <column-name>LSERVER</column-name>").append(EOLN)
                        .append("    <column-name>USERDIR</column-name>").append(EOLN)
                        .append("   </unique-constraint>").append(EOLN)
                        .append("  </table>").append(EOLN);
        if (!"IDENTITY".equals(strategy)) {
            orm
                            .append("  <attributes>").append(EOLN)
                            .append("   <id name=\"ID\">").append(EOLN)
                            .append("    <column name=\"ID\" nullable=\"false\"/>").append(EOLN)
                            .append("    <generated-value generator=\"IDGEN\" strategy=\"").append(strategy).append("\"/>").append(EOLN);
            if ("TABLE".equals(strategy)) {
                orm
                                .append("    <table-generator name=\"IDGEN\" table=\"").append(tablePrefix).append("GEN\"");
                if (schema != null)
                    orm.append(" schema=\"").append(schema).append('"');
                orm.append("/>").append(EOLN);
            } else
                orm
                                .append("    <sequence-generator name=\"IDGEN\" sequence-name=\"").append(tablePrefix).append("SEQ\"/>").append(EOLN);
            orm
                            .append("   </id>").append(EOLN)
                            .append("  </attributes>").append(EOLN);
        }
        orm
                        .append(" </entity>").append(EOLN)
                        .append(" <entity class=\"com.ibm.ws.concurrent.persistent.db.Property\">").append(EOLN)
                        .append("  <table name=\"").append(tablePrefix).append("PROP\"/>").append(EOLN)
                        .append(" </entity>").append(EOLN)
                        .append(" <entity class=\"com.ibm.ws.concurrent.persistent.db.Task\">").append(EOLN)
                        .append("  <table name=\"").append(tablePrefix).append("TASK\"/>").append(EOLN);
        if (!"IDENTITY".equals(strategy)) {
            orm
                            .append("  <attributes>").append(EOLN)
                            .append("   <id name=\"ID\">").append(EOLN)
                            .append("    <column name=\"ID\" nullable=\"false\"/>").append(EOLN)
                            .append("    <generated-value generator=\"IDGEN\" strategy=\"").append(strategy).append("\"/>").append(EOLN);
            if ("TABLE".equals(strategy)) {
                orm
                                .append("    <table-generator name=\"IDGEN\" table=\"").append(tablePrefix).append("GEN\"");
                if (schema != null)
                    orm.append(" schema=\"").append(schema).append('"');
                orm.append("/>").append(EOLN);
            } else
                orm
                                .append("    <sequence-generator name=\"IDGEN\" sequence-name=\"").append(tablePrefix).append("SEQ\"/>").append(EOLN);
            orm
                            .append("   </id>").append(EOLN)
                            .append("  </attributes>").append(EOLN);
        }
        orm
                        .append(" </entity>").append(EOLN)
                        .append("</entity-mappings>").append(EOLN);

        if (trace && tc.isDebugEnabled())
            Tr.debug(this, tc, "orm.xml generated for persistent executor", orm);

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "createOrmFileContentsForPersistentExecutor");

        return orm.toString();
    }

    protected String createOrmFileContentsForBatch(String[] entityClassNames, String strategy) {

        final boolean trace = TraceComponent.isAnyTracingEnabled();

        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "createOrmFileContentsForBatch");

        StringBuilder orm = new StringBuilder(4 * tablePrefixLength + schemaLength + 2000) // Wild guess.
        .append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>").append(EOLN)
                        .append("<entity-mappings xmlns=\"http://xmlns.jcp.org/xml/ns/persistence/orm\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://xmlns.jcp.org/xml/ns/persistence/orm http://xmlns.jcp.org/xml/ns/persistence/orm_2_1.xsd\" version=\"2.1\">").append(EOLN);
        if (schemaLength >= 0)
            orm.append(" <schema>").append(schema).append("</schema>").append(EOLN);

        // JOBEXECUTION - V1 entity - UNCONDITIONAL (v1, v2, or v3)
        orm
                        .append(" <entity class=\"com.ibm.jbatch.container.persistence.jpa.JobExecutionEntity\">").append(EOLN)
                        .append("  <table name=\"").append(tablePrefix).append("JOBEXECUTION\">").append(EOLN)
                        .append("    <index name=\"").append(tablePrefix).append("JE_FKINSTANCEID_IX\" column-list=\"FK_JOBINSTANCEID\" unique=\"false\"/>")
                        .append(EOLN).append("  </table>").append(EOLN)
                        .append("  <inheritance strategy=\"SINGLE_TABLE\"/>").append(EOLN)
                        .append("  <class-extractor class=\"com.ibm.jbatch.container.persistence.jpa.JobExecutionEntityExtractor\"/>").append(EOLN);
        if (!"IDENTITY".equals(strategy)) {
            orm
                            .append("  <attributes>").append(EOLN)
                            .append("   <id name=\"jobExecId\">").append(EOLN)
                            .append("    <column name=\"JOBEXECID\" nullable=\"false\"/>").append(EOLN)
                            .append("    <generated-value generator=\"JOBEXECIDGEN\" strategy=\"").append(strategy).append("\"/>").append(EOLN);
            if ("TABLE".equals(strategy)) {
                orm
                                .append("    <table-generator name=\"JOBEXECIDGEN\" table=\"").append(tablePrefix).append("GEN\"");
                if (schema != null)
                    orm.append(" schema=\"").append(schema).append('"');
                orm.append("/>").append(EOLN);
            } else
                orm
                                .append("    <sequence-generator name=\"JOBEXECIDGEN\" sequence-name=\"").append(tablePrefix).append("SEQ\"/>").append(EOLN);
            orm
                            .append("   </id>").append(EOLN)
                            .append("  </attributes>").append(EOLN);
        }
        orm
                        .append(" </entity>").append(EOLN);

        // JOBEXECUTION - V2 entity - CONDITIONAL (v2 or v3)
        if ( Arrays.asList(entityClassNames).contains("com.ibm.jbatch.container.persistence.jpa.JobExecutionEntityV2") ||
             Arrays.asList(entityClassNames).contains("com.ibm.jbatch.container.persistence.jpa.JobExecutionEntityV3") ) {
            orm
                            .append(" <entity class=\"com.ibm.jbatch.container.persistence.jpa.JobExecutionEntityV2\">").append(EOLN)
                            .append("  <table name=\"").append(tablePrefix).append("JOBEXECUTION\">").append(EOLN)
                            .append("  <index name=\"").append(tablePrefix).append("JE_FKINSTANCEID_IX\" column-list=\"FK_JOBINSTANCEID\" unique=\"false\"/>")
                            .append(EOLN).append("  </table>").append(EOLN)
                            .append("  <attributes>").append(EOLN)
                            .append("   <element-collection name=\"jobParameterElements\" target-class=\"com.ibm.jbatch.container.persistence.jpa.JobParameter\">").append(EOLN)
                            .append("    <collection-table name=\"").append(tablePrefix).append("JOBPARAMETER\">").append(EOLN)
                            .append("     <join-column name=\"FK_JOBEXECID\"/>").append(EOLN)
                            .append("     <index name=\"").append(tablePrefix).append("JP_FKJOBEXECID_IX\" column-list=\"FK_JOBEXECID\" unique=\"false\"/>").append(EOLN)
                            .append("    </collection-table>").append(EOLN)
                            .append("   </element-collection>").append(EOLN)
                            .append("  </attributes>").append(EOLN)
                            .append(" </entity>").append(EOLN);
        }
        // JOBEXECUTION - V3 entity - CONDITIONAL (v3 only)
        if ( Arrays.asList(entityClassNames).contains("com.ibm.jbatch.container.persistence.jpa.JobExecutionEntityV3")) {
            orm
                            .append(" <entity class=\"com.ibm.jbatch.container.persistence.jpa.JobExecutionEntityV3\">").append(EOLN)
                            .append("  <table name=\"").append(tablePrefix).append("JOBEXECUTION\">").append(EOLN)
                            .append("  </table>").append(EOLN)
                            .append(" </entity>").append(EOLN);

        }

        // JOBINSTANCE - V1 entity - UNCONDITIONAL (v1, v2, or v3)
        orm
                        .append(" <entity class=\"com.ibm.jbatch.container.persistence.jpa.JobInstanceEntity\">").append(EOLN)
                        .append("  <table name=\"").append(tablePrefix).append("JOBINSTANCE\"/>").append(EOLN)
                        .append("  <inheritance strategy=\"SINGLE_TABLE\"/>").append(EOLN)
                        .append("  <class-extractor class=\"com.ibm.jbatch.container.persistence.jpa.JobInstanceEntityExtractor\"/>").append(EOLN);
        if (!"IDENTITY".equals(strategy)) {
            orm
                            .append("  <attributes>").append(EOLN)
                            .append("   <id name=\"instanceId\">").append(EOLN)
                            .append("    <column name=\"JOBINSTANCEID\" nullable=\"false\"/>").append(EOLN)
                            .append("    <generated-value generator=\"JOBINSTANCEIDGEN\" strategy=\"").append(strategy).append("\"/>").append(EOLN);
            if ("TABLE".equals(strategy)) {
                orm
                                .append("    <table-generator name=\"JOBINSTANCEIDGEN\" table=\"").append(tablePrefix).append("GEN\"");
                if (schema != null)
                    orm.append(" schema=\"").append(schema).append('"');
                orm.append("/>").append(EOLN);
            } else
                orm
                                .append("    <sequence-generator name=\"JOBINSTANCEIDGEN\" sequence-name=\"").append(tablePrefix).append("SEQ\"/>").append(EOLN);
            orm
                            .append("   </id>").append(EOLN)
                            .append("  </attributes>").append(EOLN);
        }
        orm
                        .append(" </entity>").append(EOLN);

        // JOBINSTANCE - V2 entity - CONDITIONAL (v2 only)
        if (Arrays.asList(entityClassNames).contains("com.ibm.jbatch.container.persistence.jpa.JobInstanceEntityV2")) {
            orm
                            .append(" <entity class=\"com.ibm.jbatch.container.persistence.jpa.JobInstanceEntityV2\">").append(EOLN)
                            .append("  <table name=\"").append(tablePrefix).append("JOBINSTANCE\"/>").append(EOLN)
                            .append(" </entity>").append(EOLN);
        }
        // JOBINSTANCE - V2 and V3 entities - CONDITIONAL (v3 only)
        if (Arrays.asList(entityClassNames).contains("com.ibm.jbatch.container.persistence.jpa.JobInstanceEntityV3")) {
            orm
                            .append(" <entity class=\"com.ibm.jbatch.container.persistence.jpa.JobInstanceEntityV2\">").append(EOLN)
                            .append("  <table name=\"").append(tablePrefix).append("JOBINSTANCE\"/>").append(EOLN)
                            .append(" </entity>").append(EOLN)
                            .append(" <entity class=\"com.ibm.jbatch.container.persistence.jpa.JobInstanceEntityV3\">").append(EOLN)
                            .append("  <table name=\"").append(tablePrefix).append("JOBINSTANCE\"/>").append(EOLN)
                            .append("  <attributes>").append(EOLN)
                            .append("   <element-collection name=\"groupNames\" target-class=\"java.lang.String\">").append(EOLN)
                            .append("    <collection-table name=\"").append(tablePrefix).append("GROUPASSOCIATION\">").append(EOLN)
                            .append("     <join-column name=\"FK_JOBINSTANCEID\"/>").append(EOLN)
                            .append("     <index name=\"").append(tablePrefix).append("GA_FKINSTANCEID_IX\" column-list=\"FK_JOBINSTANCEID\" unique=\"false\"/>").append(EOLN)
                            .append("    </collection-table>").append(EOLN)
                            .append("   </element-collection>").append(EOLN)
                            .append("  </attributes>").append(EOLN)
                            .append(" </entity>").append(EOLN);

        }
        // STEPTHREADEXECUTION - V1 entity - UNCONDITIONAL (v1 or v2)
        orm
                        .append(" <entity class=\"com.ibm.jbatch.container.persistence.jpa.StepThreadExecutionEntity\">").append(EOLN)
                        .append("  <table name=\"").append(tablePrefix).append("STEPTHREADEXECUTION\">").append(EOLN)
                        .append("   <unique-constraint>").append(EOLN)
                        .append("    <column-name>FK_JOBEXECID</column-name>").append(EOLN)
                        .append("    <column-name>STEPNAME</column-name>").append(EOLN)
                        .append("    <column-name>PARTNUM</column-name>").append(EOLN)
                        .append("   </unique-constraint>").append(EOLN)
                        //adding indexes
                        .append(" <index name=\"").append(tablePrefix).append("STE_FKJOBEXECID_IX\" column-list=\"FK_JOBEXECID\" unique=\"false\"/>").append(EOLN)
                        .append(" <index name=\"").append(tablePrefix).append("STE_FKTLSTEPEID_IX\" column-list=\"FK_TOPLVL_STEPEXECID\" unique=\"false\"/>").append(EOLN)
                        //end indexes
                        .append("  </table>").append(EOLN);
        if (!"IDENTITY".equals(strategy)) {
            orm
                            .append("  <attributes>").append(EOLN)
                            .append("   <id name=\"stepExecutionId\">").append(EOLN)
                            .append("    <column name=\"STEPEXECID\" nullable=\"false\"/>").append(EOLN)
                            .append("    <generated-value generator=\"STEPEXECIDGEN\" strategy=\"").append(strategy).append("\"/>").append(EOLN);
            if ("TABLE".equals(strategy)) {
                orm
                                .append("    <table-generator name=\"STEPEXECIDGEN\" table=\"").append(tablePrefix).append("GEN\"");
                if (schema != null)
                    orm.append(" schema=\"").append(schema).append('"');
                orm.append("/>").append(EOLN);
            } else
                orm
                                .append("    <sequence-generator name=\"STEPEXECIDGEN\" sequence-name=\"").append(tablePrefix).append("SEQ\"/>").append(EOLN);
            orm
                            .append("   </id>").append(EOLN)
                            .append("  </attributes>").append(EOLN);
        }
        orm
                        .append(" </entity>").append(EOLN);

        // STEPTHREADEXECUTION - V2 entity - CONDITIONAL (v2 only)
        if (Arrays.asList(entityClassNames).contains("com.ibm.jbatch.container.persistence.jpa.StepThreadExecutionEntityV2")) {
            orm
                            .append(" <entity class=\"com.ibm.jbatch.container.persistence.jpa.StepThreadExecutionEntityV2\">").append(EOLN)
                            .append("  <table name=\"").append(tablePrefix).append("STEPTHREADEXECUTION\">").append(EOLN)
                            .append("  </table>").append(EOLN)
                            .append(" </entity>").append(EOLN);
        }

        // STEPTHREADINSTANCE - V1 entity - UNCONDITIONAL
        orm.append(" <entity class=\"com.ibm.jbatch.container.persistence.jpa.StepThreadInstanceEntity\">").append(EOLN)
                        .append("  <table name=\"").append(tablePrefix).append("STEPTHREADINSTANCE\">").append(EOLN)
                        //adding indexes
                        .append("   <index name=\"").append(tablePrefix).append("STI_FKINSTANCEID_IX\" column-list=\"FK_JOBINSTANCEID\" unique=\"false\"/>").append(EOLN)
                        .append("   <index name=\"").append(tablePrefix).append("STI_FKLATEST_SEI_IX\" column-list=\"FK_LATEST_STEPEXECID\" unique=\"false\"/>").append(EOLN)
                        //end indexes
                        .append("  </table>").append(EOLN)
                        .append(" </entity>").append(EOLN);

        // REMOTABLEPARTITION - V1 entity - CONDITIONAL (on presence of RemotablePartitionEntity only)
        if (Arrays.asList(entityClassNames).contains("com.ibm.jbatch.container.persistence.jpa.RemotablePartitionEntity")) {
            orm.append(" <entity class=\"com.ibm.jbatch.container.persistence.jpa.RemotablePartitionEntity\">").append(EOLN)
                               .append("  <table name=\"").append(tablePrefix).append("REMOTABLEPARTITION\">").append(EOLN)
                               .append("  </table>").append(EOLN)
                               .append(" </entity>").append(EOLN);
        }

        // ALL DONE
        orm.append("</entity-mappings>").append(EOLN);

        if (trace && tc.isDebugEnabled())
            Tr.debug(this, tc, "orm.xml generated for batch", orm);

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "createOrmFileContentsForBatch");

        return orm.toString();
    }

    /**
     * @return a generic ORM xml file using the given schema, tablePrefix, and entity classes.
     */
    protected InMemoryMappingFile createOrmFile(String schemaName,
                                                String tablePrefix,
                                                LinkedHashSet<String> tableNames, // entries correspond to entityClassNames
                                                String[] entityClassNames,
                                                String[] entityClassEntries)
                    throws UnsupportedEncodingException {
        return ((entityClassNames == null || entityClassNames.length == 0) && (entityClassEntries == null || entityClassEntries.length == 0))
                        ? null
                        : new InMemoryMappingFile(createOrm(schemaName, tablePrefix, tableNames, entityClassNames, entityClassEntries).getBytes("UTF-8"));
    }

    /**
     * @return a generic ORM xml file, in string form, using the given schema, tablePrefix, and entity classes.
     */
    protected String createOrm(String schemaName,
                               String tablePrefix,
                               LinkedHashSet<String> tableNames, // entries correspond to entityClassNames
                               String[] entityClassNames,
                               String[] entityClassEntries) {
        StringBuilder builder = new StringBuilder();

        // Add header information
        builder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + EOLN)
                        .append("<entity-mappings xmlns=\"http://xmlns.jcp.org/xml/ns/persistence/orm\"")
                        .append("                 xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"")
                        .append("                 xsi:schemaLocation=\"http://xmlns.jcp.org/xml/ns/persistence/orm http://xmlns.jcp.org/xml/ns/persistence/orm_2_1.xsd\"")
                        .append("                 version=\"2.1\">")
                        .append(EOLN);

        // Add schema if it exists
        if (schemaName != null && !schemaName.trim().isEmpty()) {
            builder.append(" <schema>" + schemaName + "</schema>" + EOLN);
        }

        // Add the entities and apply tablePrefix
        tablePrefix = (tablePrefix == null) ? "" : tablePrefix.trim();

        if (entityClassNames != null) {
            int i = 0;
            for (String tableName : tableNames) {
                builder.append(" <entity class=" + enquote(entityClassNames[i++]) + ">" + EOLN)
                                .append("  <table name=" + enquote(tablePrefix + tableName) + "/>" + EOLN)
                                .append(" </entity>" + EOLN);
            }
        }

        if (entityClassEntries != null)
            for (String entityClassEntry : entityClassEntries) {
                builder.append(entityClassEntry);
            }

        builder.append("</entity-mappings>" + EOLN);
        return builder.toString();
    }

    /**
     * @return the simple name (sans package) of the given class name
     */
    protected String parseSimpleName(String entityClassName) {
        return entityClassName.substring(entityClassName.lastIndexOf(".") + 1);
    }

    /**
     * @return "\"" + s + "\""
     */
    protected String enquote(String s) {
        return "\"" + s + "\"";
    }

    /**
     * Automatic table creation and/or deletion. If both creation and deletion are requested,
     * deletion is performed first.
     *
     * @param persistenceServiceUnit persistence service unit
     * @throws Exception if an error occurs creating tables.
     */
    private void dropAndOrCreateTables(PersistenceServiceUnit persistenceServiceUnit, boolean createTables, boolean dropTables) throws Exception {
        // Run under a new transaction and commit right away
        LocalTransactionCurrent localTranCurrent = this.localTranCurrent;
        LocalTransactionCoordinator suspendedLTC = localTranCurrent.suspend();
        EmbeddableWebSphereTransactionManager tranMgr = this.tranMgr;
        Transaction suspendedTran = suspendedLTC == null ? tranMgr.suspend() : null;
        boolean psuIsPUSI = (persistenceServiceUnit instanceof PersistenceServiceUnitImpl) ? true : false;

        synchronized (persistenceServiceUnit) {
            try {
                if (psuIsPUSI) {
                    ((PersistenceServiceUnitImpl) persistenceServiceUnit).setTransactionManager(tranMgr);
                }
                if (createTables) {
                    if (dropTables)
                        persistenceServiceUnit.dropAndCreateTables();
                    else
                        persistenceServiceUnit.createTables();
                } else if (dropTables) {
                    persistenceServiceUnit.dropTables();
                }
            } finally {
                // resume
                if (psuIsPUSI) {
                    ((PersistenceServiceUnitImpl) persistenceServiceUnit).setTransactionManager(null);
                }
                if (suspendedTran != null)
                    tranMgr.resume(suspendedTran);
                else if (suspendedLTC != null)
                    localTranCurrent.resume(suspendedLTC);
            }
        }
    }

    @Deactivate
    protected void deactivate(ComponentContext context) throws Exception {
        deactivated = true;
    }

    /**
     * Obtain the database product name from the database to which the data source connects.
     *
     * @param dataSource data source instance.
     * @return the database product name.
     * @throws Exception if unable to obtain the database product name.
     */
    private String getDatabaseProductName(WSDataSource dataSource) throws Exception {

        //Synchronized to avoid a race condition in tests where attempting to boot two
        //derby instances simultaneously causes tests to fail
        synchronized (DatabaseStoreImpl.class) {

            String dbProductName = dataSource.getDatabaseProductName();
            if (dbProductName == null) {
                // Query the metadata under a new transaction and commit right away
                LocalTransactionCurrent localTranCurrent = this.localTranCurrent;
                LocalTransactionCoordinator suspendedLTC = localTranCurrent.suspend();
                EmbeddableWebSphereTransactionManager tranMgr = this.tranMgr;
                Transaction suspendedTran = suspendedLTC == null ? tranMgr.suspend() : null;
                boolean tranStarted = false;
                try {
                    tranMgr.begin();
                    tranStarted = true;
                    Connection con = dataSource.getConnection();
                    try {
                        dbProductName = con.getMetaData().getDatabaseProductName();
                    } finally {
                        con.close();
                    }
                } finally {
                    try {
                        if (tranStarted)
                            tranMgr.commit();
                    } finally {
                        // resume
                        if (suspendedTran != null)
                            tranMgr.resume(suspendedTran);
                        else if (suspendedLTC != null)
                            localTranCurrent.resume(suspendedLTC);
                    }
                }
            }
            return dbProductName;
        }
    }

    /**
     * Attempt to ascertain if the given isolation level is supported for this DatabaseStore.
     * Creates a temporary DataSource and Connection in order to check database support.
     *<p>
     *{@link java.sql.DatabaseMetaData#supportsTransactionIsolationLevel()}
     *
     * @param isolationLevel valid isolation level value.
     * @return 'true' if the driver DatabaseMetaData indicates the given transaction isolation is supported; 'false' otherwise
     * @throws Exception if a failure occurs
     */
    private boolean supportsIsolationLevel(int isolationLevel) throws Exception {
        //Create a temporary DataSource to check the isolation level support
        ResourceConfig tempResourceInfo = resourceConfigFactory.createResourceConfig(DataSource.class.getName());
        tempResourceInfo.setSharingScope(ResourceConfig.SHARING_SCOPE_SHAREABLE);
        // Use READ_COMMITTED first to determine if the configured isolation level is supported
        tempResourceInfo.setIsolationLevel(Connection.TRANSACTION_READ_COMMITTED);
        tempResourceInfo.setResAuthType(ResourceConfig.AUTH_CONTAINER);

        if (authDataRef != null) {
            String authDataId = (String) authDataRef.getProperty("id");
            tempResourceInfo.addLoginProperty("DefaultPrincipalMapping",
                                          authDataId.matches(".*(\\]/).*(\\[default-\\d*\\])")
                                                          ? (String) authDataRef.getProperty("config.displayId")
                                                          : authDataId);
        }

        DataSource tempDataSource = (DataSource) dataSourceFactory.createResource(tempResourceInfo);

        // Query the metadata under a new transaction and commit right away
        LocalTransactionCurrent localTranCurrent = this.localTranCurrent;
        LocalTransactionCoordinator suspendedLTC = localTranCurrent.suspend();
        EmbeddableWebSphereTransactionManager tranMgr = this.tranMgr;
        Transaction suspendedTran = suspendedLTC == null ? tranMgr.suspend() : null;
        boolean tranStarted = false;
        try {
            tranMgr.begin();
            tranStarted = true;
            Connection con = tempDataSource.getConnection();
            try {
                return con.getMetaData().supportsTransactionIsolationLevel(isolationLevel);
            } finally {
                con.close();
            }
        } finally {
            try {
                if (tranStarted)
                    tranMgr.rollback();
            } finally {
                // resume
                if (suspendedTran != null)
                    tranMgr.resume(suspendedTran);
                else if (suspendedLTC != null)
                    localTranCurrent.resume(suspendedLTC);
            }
        }
    }

    @Override
    public String getSchema() {
        return schema;
    }

    @Override
    public String getTablePrefix() {
        return tablePrefix;
    }
}
