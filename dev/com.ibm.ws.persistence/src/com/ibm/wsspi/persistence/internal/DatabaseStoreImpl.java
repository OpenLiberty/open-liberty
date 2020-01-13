/*******************************************************************************
 * Copyright (c) 2014, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.persistence.internal;

import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

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
import com.ibm.websphere.ras.annotation.Trivial;
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
        BATCH, PERSISTENT_EXECUTOR
    };

    private SpecialEntitySet recognizeSpecialEntityPackage(String className) {
        if (className.startsWith(BATCH_PKG_PREFIX)) {
            return SpecialEntitySet.BATCH;
        } else if (className.startsWith(PERSISTENT_EXECUTOR_PKG_PREFIX)) {
            return SpecialEntitySet.PERSISTENT_EXECUTOR;
        } else {
            return null;
        }
    }

    /**
     * Reference to the default authentication data.
     */
    private ServiceReference<?> authDataRef;

    /**
     * Resource factory for the data source.
     */
    private ResourceFactory dataSourceFactory;

    /**
     * Indicates if this database store instance has been deactivated.
     */
    private volatile boolean deactivated;

    /**
     * A service that controls local transactions.
     */
    private LocalTransactionCurrent localTranCurrent;

    /**
     * Resource factory for the non-transactional data source.
     */
    private ResourceFactory nonJTADataSourceFactory;

    /**
     * The persistence service.
     */
    private PersistenceService persistenceService;

    /**
     * Configuration properties for this instance.
     */
    private Dictionary<String, ?> properties;

    /**
     * Schema from config
     */
    private String schema;

    /**
     * Table prefix from config
     */
    private String tablePrefix;

    /**
     * Key generation strategy from config
     */
    private String strategy;

    /**
     * Length of schema String
     */
    int schemaLength;

    /**
     * Length of tablePrefix String
     */
    int tablePrefixLength;

    /**
     * Resource config factory.
     */
    private ResourceConfigFactory resourceConfigFactory;

    /**
     * Transaction manager.
     */
    private EmbeddableWebSphereTransactionManager tranMgr;

    @Activate
    @Trivial
    protected void activate(ComponentContext context) throws Exception {
        properties = context.getProperties();
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "activate", properties);

        // The database store should be lazily initialized by the components using it, so there is no need
        // for the database store implementation to have its own lazy initialization.

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "activate");
    }

    /**
     * @see com.ibm.wsspi.persistence.DatabaseStore#createPersistenceServiceUnit(java.lang.ClassLoader, java.lang.String[])
     */
    @Override
    public PersistenceServiceUnit createPersistenceServiceUnit(ClassLoader loader, String... entityClassNames) throws Exception {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "createPersistenceServiceUnit", loader, Arrays.asList(entityClassNames));

        Map<String, Object> puProps = new HashMap<String, Object>();
        tablePrefix = (String) properties.get("tablePrefix");
        tablePrefixLength = tablePrefix.length();
        for (int i = 0; i < tablePrefixLength; i++) {
            int codepoint = tablePrefix.codePointAt(i);
            if (!Character.isLetterOrDigit(codepoint) && codepoint != 95) { // alphanumeric or _ character
                Tr.error(tc, "ILLEGAL_IDENTIFIER_CWWKD0200E", tablePrefix);
                throw new IllegalArgumentException(Tr.formatMessage(tc, "ILLEGAL_IDENTIFIER_CWWKD0200E", tablePrefix));
            }
        }

        schema = (String) properties.get("schema");
        schemaLength = schema == null ? -1 : schema.length();
        for (int i = 0; i < schemaLength; i++) {
            int codepoint = schema.codePointAt(i);
            if (!Character.isLetterOrDigit(codepoint) && codepoint != 95) { // alphanumeric or _ character
                Tr.error(tc, "ILLEGAL_IDENTIFIER_CWWKD0201E", schema);
                throw new IllegalArgumentException(Tr.formatMessage(tc, "ILLEGAL_IDENTIFIER_CWWKD0201E", schema));
            }
        }

        // Look for EclipseLink persistence properties
        if (properties.get("persistenceProperties.0.config.referenceType") != null) {
            for (String key : Collections.list(properties.keys())) {
                if (key.startsWith("persistenceProperties.0") && !key.equals("persistenceProperties.0.config.referenceType")) {
                    Object value = properties.get(key);
                    key = key.substring(24);
                    puProps.put(key, value);
                }
            }
        }

        ResourceConfig resourceInfo = resourceConfigFactory.createResourceConfig(DataSource.class.getName());
        resourceInfo.setSharingScope(ResourceConfig.SHARING_SCOPE_SHAREABLE);
        resourceInfo.setIsolationLevel(Connection.TRANSACTION_READ_COMMITTED);
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

        DataSource dataSource = (DataSource) dataSourceFactory.createResource(resourceInfo);
        String dbProductName = getDatabaseProductName(((WSDataSource) dataSource)).toLowerCase();
        if (dbProductName.contains("informix") || dbProductName.startsWith("ids/")) {
            // Defect 168450 - PersistenceService is currently disabled when running with Informix.
            // Once the informix issues are fixed, this exception will be removed.
            // When informix is using a DB2 driver, the product name is determined by db2 and is
            // similar to: ids/nt64
            throw new UnsupportedOperationException(dbProductName);
        }

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

        strategy = (String) properties.get("keyGenerationStrategy");
        if ("AUTO".equals(strategy)) {
            strategy = dbProductName.contains("oracle") ? "SEQUENCE"
                            : dbProductName.contains("adaptive server") || dbProductName.contains("sybase") ? "TABLE"
                                            : "IDENTITY";
        }

        // TODO: replace temporary code specific to persistentExecutor with general solution that applies the
        // table prefix, schema, and keyGenerationStrategy to all entities

        InMemoryMappingFile ormFile = null;
        SpecialEntitySet entitySet = recognizeSpecialEntityPackage(entityClassNames[0]);
        if (entitySet.equals(SpecialEntitySet.PERSISTENT_EXECUTOR)) {
            String ormFileContents = createOrmFileContentsForPersistentExecutor();
            ormFile = new InMemoryMappingFile(ormFileContents.getBytes("UTF-8"));
        } else if (entitySet.equals(SpecialEntitySet.BATCH)) {
            String ormFileContents = createOrmFileContentsForBatch(entityClassNames);
            ormFile = new InMemoryMappingFile(ormFileContents.getBytes("UTF-8"));
        } else {
            ormFile = createOrmFile(schema, tablePrefix, entityClassNames);
        }

        PersistenceServiceUnitConfig config = new PersistenceServiceUnitConfig();
        config.setClasses(Arrays.asList(entityClassNames));
        config.setConsumerLoader(loader);
        config.setProperties(puProps);
        if (ormFile != null)
            config.setInMemoryMappingFiles(Collections.singletonList(ormFile));
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
                throw new IllegalStateException();
            }

            if ((Boolean) properties.get("createTables"))
                if (entitySet.equals(SpecialEntitySet.PERSISTENT_EXECUTOR) && entityClassNames.length == 1)
                    ; // ignore table creation for extra PersistenceServiceUnit that persistent executor creates to allow TRANSACTION_READ_UNCOMMITTED
                      // TODO is there a better way to accomplish this?
                else
                    createTables(persistenceServiceUnit);

            if (deactivated) {
                if (trace && tc.isEntryEnabled())
                    Tr.exit(this, tc, "createPersistenceServiceUnit", "deactivated");
                throw new IllegalStateException();
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

    protected String createOrmFileContentsForPersistentExecutor() {

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

    protected String createOrmFileContentsForBatch(String[] entityClassNames) {

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
                                                String[] entityClassNames)
                    throws UnsupportedEncodingException {
        return (entityClassNames == null || entityClassNames.length == 0)
                        ? null
                        : new InMemoryMappingFile(createOrm(schemaName, tablePrefix, entityClassNames).getBytes("UTF-8"));
    }

    /**
     * @return a generic ORM xml file, in string form, using the given schema, tablePrefix, and entity classes.
     */
    protected String createOrm(String schemaName, String tablePrefix, String[] entityClassNames) {
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

        for (String entityClassName : entityClassNames) {
            String simpleName = parseSimpleName(entityClassName);

            builder.append(" <entity class=" + enquote(entityClassName) + ">" + EOLN)
                            .append("  <table name=" + enquote(tablePrefix + simpleName) + "/>" + EOLN)
                            .append(" </entity>" + EOLN);
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
     * Automatic table creation.
     *
     * @param persistenceServiceUnit persistence service unit
     * @throws Exception if an error occurs creating tables.
     */
    private void createTables(PersistenceServiceUnit persistenceServiceUnit) throws Exception {
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
                persistenceServiceUnit.createTables();
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

    /**
     * Declarative Services method for setting the service reference for the default auth data
     *
     * @param ref reference to the service
     */

    @Reference(service = AuthData.class,
               cardinality = ReferenceCardinality.OPTIONAL,
               policy = ReferencePolicy.STATIC,
               target = "(id=unbound)",
               policyOption = ReferencePolicyOption.GREEDY)
    protected void setAuthData(ServiceReference<AuthData> ref) {
        authDataRef = ref;
    }

    /**
     * Declarative Services method for setting the resource factory for the data source.
     *
     * @param svc the service
     */
    @Reference(target = "(id=unbound)")
    protected void setDataSourceFactory(ResourceFactory svc) {
        dataSourceFactory = svc;
    }

    /**
     * Declarative Services method for setting the LocalTransactionCurrent.
     *
     * @param ref the service
     */
    @Reference
    protected void setLocalTransactionCurrent(LocalTransactionCurrent svc) {
        localTranCurrent = svc;
    }

    /**
     * Declarative Services method for setting the resource factory for the non-transactional data source.
     *
     * @param svc the service
     */
    @Reference(cardinality = ReferenceCardinality.OPTIONAL,
               policy = ReferencePolicy.STATIC,
               policyOption = ReferencePolicyOption.GREEDY,
               target = "(id=unbound)")
    protected void setNonJTADataSourceFactory(ResourceFactory svc) {
        nonJTADataSourceFactory = svc;
    }

    /**
     * Declarative Services method for setting the persistence service.
     *
     * @param svc the service
     */
    @Reference
    protected void setPersistenceService(PersistenceService svc) {
        persistenceService = svc;
    }

    /**
     * Declarative services method for setting the resource config factory.
     *
     * @param svc the service
     */
    @Reference(service = ResourceConfigFactory.class)
    protected void setResourceConfigFactory(ResourceConfigFactory svc) {
        resourceConfigFactory = svc;
    }

    /**
     * Declarative Services method for setting the transaction manager
     *
     * @param svc the service
     */
    @Reference
    protected void setTransactionManager(EmbeddableWebSphereTransactionManager svc) {
        tranMgr = svc;
    }

    /**
     * Declarative Services method for unsetting the service reference for default auth data
     *
     * @param ref reference to the service
     */

    protected void unsetAuthData(ServiceReference<AuthData> ref) {
        authDataRef = null;
    }

    /**
     * Declarative Services method for unsetting the resource factory for the data source.
     *
     * @param svc the service
     */
    protected void unsetDataSourceFactory(ResourceFactory svc) {
        dataSourceFactory = null;
    }

    /**
     * Declarative Services method for unsetting the LocalTransactionCurrent.
     *
     * @param svc the service
     */
    protected void unsetLocalTransactionCurrent(LocalTransactionCurrent svc) {
        localTranCurrent = null;
    }

    /**
     * Declarative Services method for unsetting the resource factory for the non-transactional data source.
     *
     * @param svc the service
     */
    protected void unsetNonJTADataSourceFactory(ResourceFactory svc) {
        nonJTADataSourceFactory = null;
    }

    /**
     * Declarative Services method for unsetting the persistence service.
     *
     * @param svc the service
     */
    protected void unsetPersistenceService(PersistenceService svc) {
        persistenceService = null;
    }

    /**
     * Declarative Services method for unsetting the resource config factory.
     *
     * @param svc the service
     */
    protected void unsetResourceConfigFactory(ResourceConfigFactory svc) {
        resourceConfigFactory = null;
    }

    /**
     * Declarative Services method for unsetting the transaction manager
     *
     * @param svc the service
     */
    protected void unsetTransactionManager(EmbeddableWebSphereTransactionManager svc) {
        tranMgr = null;
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
