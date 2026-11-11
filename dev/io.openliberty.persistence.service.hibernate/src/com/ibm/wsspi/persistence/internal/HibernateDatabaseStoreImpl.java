/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;
import jakarta.transaction.Transaction;

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
 * OSGi DS component for the {@code <databaseStore>} configuration element.
 * Backed by Hibernate 8.x (jakarta.persistence.*).
 *
 * Replaces DatabaseStoreImpl (EclipseLink) from com.ibm.ws.persistence.jakarta.
 */
@Component(name = "com.ibm.ws.persistence.databaseStore",
           service = { DatabaseStore.class },
           configurationPolicy = ConfigurationPolicy.REQUIRE)
public class HibernateDatabaseStoreImpl implements DatabaseStore {
    private static final TraceComponent tc = Tr.register(HibernateDatabaseStoreImpl.class);
    private static final String EOLN = String.format("%n");

    private static final String BATCH_PKG_PREFIX = "com.ibm.jbatch.container.persistence.jpa.";
    private static final String PERSISTENT_EXECUTOR_PKG_PREFIX = "com.ibm.ws.concurrent.persistent.";

    private enum SpecialEntitySet {
        BATCH, PERSISTENT_EXECUTOR, NONE
    }

    private SpecialEntitySet recognizeSpecialEntityPackage(String className) {
        if (className.startsWith(BATCH_PKG_PREFIX)) {
            return SpecialEntitySet.BATCH;
        } else if (className.startsWith(PERSISTENT_EXECUTOR_PKG_PREFIX)) {
            return SpecialEntitySet.PERSISTENT_EXECUTOR;
        }
        return SpecialEntitySet.NONE;
    }

    // ---- Injected references ----

    private final ServiceReference<?> authDataRef;
    private final ResourceFactory dataSourceFactory;
    private volatile boolean deactivated;
    private final LocalTransactionCurrent localTranCurrent;
    private final ResourceFactory nonJTADataSourceFactory;
    private final PersistenceService persistenceService;
    private final Map<String, ?> properties;
    private final ResourceConfigFactory resourceConfigFactory;
    private final EmbeddableWebSphereTransactionManager tranMgr;

    // ---- Config values ----

    private final String schema;
    private final String tablePrefix;
    private final String strategyConfig;
    final int schemaLength;
    final int tablePrefixLength;

    @Activate
    public HibernateDatabaseStoreImpl(Map<String, ?> properties,

                                      @Reference(name = "DataSourceFactory", target = "(id=unbound)") ResourceFactory dataSourceFactory,
                                      @Reference LocalTransactionCurrent localTranCurrent,
                                      @Reference PersistenceService persistenceService,
                                      @Reference ResourceConfigFactory resourceConfigFactory,
                                      @Reference EmbeddableWebSphereTransactionManager tranMgr,

                                      @Reference(name = "AuthData",
                                          service = AuthData.class,
                                          cardinality = ReferenceCardinality.OPTIONAL,
                                          policy = ReferencePolicy.STATIC,
                                          target = "(id=unbound)",
                                          policyOption = ReferencePolicyOption.GREEDY) ServiceReference<AuthData> authDataRef,
                                      @Reference(name = "NonJTADataSourceFactory",
                                          cardinality = ReferenceCardinality.OPTIONAL,
                                          policy = ReferencePolicy.STATIC,
                                          policyOption = ReferencePolicyOption.GREEDY,
                                          target = "(id=unbound)") ResourceFactory nonJTADataSourceFactory
                             ) throws Exception {

        this.properties = properties;
        this.dataSourceFactory = dataSourceFactory;
        this.localTranCurrent = localTranCurrent;
        this.persistenceService = persistenceService;
        this.resourceConfigFactory = resourceConfigFactory;
        this.tranMgr = tranMgr;
        this.authDataRef = authDataRef;
        this.nonJTADataSourceFactory = nonJTADataSourceFactory;

        schema = (String) this.properties.get("schema");
        schemaLength = schema == null ? -1 : schema.length();
        tablePrefix = (String) this.properties.get("tablePrefix");
        tablePrefixLength = tablePrefix.length();
        strategyConfig = (String) this.properties.get("keyGenerationStrategy");
    }

    @Deactivate
    protected void deactivate(ComponentContext context) throws Exception {
        deactivated = true;
    }

    // ---- DatabaseStore interface ----

    @Override
    public PersistenceServiceUnit createPersistenceServiceUnit(ClassLoader loader, String... entityClassNames) throws Exception {
        return createPersistenceServiceUnit(loader, new HashMap<>(), entityClassNames);
    }

    @Override
    public PersistenceServiceUnit createPersistenceServiceUnit(ClassLoader loader, Map properties, String... entityClassNames) throws Exception {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "createPersistenceServiceUnit", loader, Arrays.asList(entityClassNames));

        // Validate tablePrefix and schema — only alphanumeric and underscore allowed
        Map<String, Object> puProps = new HashMap<>();
        for (int i = 0; i < tablePrefixLength; i++) {
            int codepoint = tablePrefix.codePointAt(i);
            if (!Character.isLetterOrDigit(codepoint) && codepoint != 95) { // alphanumeric or _
                Tr.error(tc, "ILLEGAL_IDENTIFIER_CWWKD0200E", tablePrefix);
                throw new IllegalArgumentException(Tr.formatMessage(tc, "ILLEGAL_IDENTIFIER_CWWKD0200E", tablePrefix));
            }
        }

        for (int i = 0; i < schemaLength; i++) {
            int codepoint = schema.codePointAt(i);
            if (!Character.isLetterOrDigit(codepoint) && codepoint != 95) {
                Tr.error(tc, "ILLEGAL_IDENTIFIER_CWWKD0201E", schema);
                throw new IllegalArgumentException(Tr.formatMessage(tc, "ILLEGAL_IDENTIFIER_CWWKD0201E", schema));
            }
        }

        // Extract any Hibernate persistence properties passed via server.xml <persistenceProperties>
        if (this.properties.get("persistenceProperties.0.config.referenceType") != null) {
            for (String key : this.properties.keySet()) {
                if (key.startsWith("persistenceProperties.0") && !key.equals("persistenceProperties.0.config.referenceType")) {
                    Object value = this.properties.get(key);
                    key = key.substring(24);
                    puProps.put(key, value);
                }
            }
        }

        // Resolve isolation level from caller properties
        int isolationLevel = Connection.TRANSACTION_READ_COMMITTED;
        if (properties.get("transactionIsolationLevel") != null) {
            Object configIsolationLevel = properties.get("transactionIsolationLevel");
            if (configIsolationLevel instanceof Number) {
                int newIsolationLevel = ((Number) configIsolationLevel).intValue();
                if (supportsIsolationLevel(newIsolationLevel)) {
                    isolationLevel = newIsolationLevel;
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
        }

        SpecialEntitySet entitySet = entityClassNames.length == 0
                                     ? SpecialEntitySet.NONE
                                     : recognizeSpecialEntityPackage(entityClassNames[0]);
        DataSource dataSource = (DataSource) dataSourceFactory.createResource(resourceInfo);

        DataSource nonJTADataSource;
        if (nonJTADataSourceFactory == null) {
            nonJTADataSource = null;
        } else {
            resourceInfo.setSharingScope(ResourceConfig.SHARING_SCOPE_UNSHAREABLE);
            nonJTADataSource = (DataSource) nonJTADataSourceFactory.createResource(resourceInfo);
        }

        if (deactivated) {
            Tr.error(tc, "DEACTIVATED_CWWKD0202E");
            throw new IllegalStateException(Tr.formatMessage(tc, "DEACTIVATED_CWWKD0202E"));
        }

        // For BATCH and PERSISTENT_EXECUTOR: no ORM XML is generated.
        // hibernate.default_schema and HibernatePrefixingNamingStrategy replace it.
        // For NONE (Jakarta Data): generate ORM XML with JPA 4.0 namespace.
        List<InMemoryMappingFile> inMemoryFiles = null;

        if (entitySet == SpecialEntitySet.BATCH || entitySet == SpecialEntitySet.PERSISTENT_EXECUTOR) {
            // Schema and tablePrefix are passed as Hibernate properties directly
            if (schema != null) {
                puProps.put("hibernate.default_schema", schema);
            }
            puProps.put("hibernate.physical_naming_strategy",
                        new HibernatePrefixingNamingStrategy(tablePrefix));
            // Pass explicit key generation strategy if not AUTO
            if (!"AUTO".equals(strategyConfig)) {
                puProps.put("io.openliberty.persistence.keyGenerationStrategy", strategyConfig);
            }
        } else {
            // NONE path — Jakarta Data or generic entities
            @SuppressWarnings("unchecked")
            LinkedHashSet<String> tableNames = (LinkedHashSet<String>) properties.get(
                "io.openliberty.persistence.internal.tableNames");
            String[] entityClassEntries = (String[]) properties.get(
                "io.openliberty.persistence.internal.entityClassInfo");
            InMemoryMappingFile ormFile = createOrmFile(schema, tablePrefix, tableNames,
                                                        entityClassNames, entityClassEntries);
            @SuppressWarnings("unchecked")
            List<InMemoryMappingFile> generatedEntities = (List<InMemoryMappingFile>) properties.get(
                "io.openliberty.persistence.internal.generatedEntities");
            inMemoryFiles = generatedEntities;
            if (ormFile != null) {
                if (inMemoryFiles == null) {
                    inMemoryFiles = List.of(ormFile);
                } else {
                    inMemoryFiles = new ArrayList<>(inMemoryFiles);
                    inMemoryFiles.add(ormFile);
                }
            }
        }

        PersistenceServiceUnitConfig config = new PersistenceServiceUnitConfig();
        config.setClasses(Arrays.asList(entityClassNames));
        config.setConsumerLoader(loader);
        config.setProperties(puProps);
        if (inMemoryFiles != null) {
            config.setInMemoryMappingFiles(inMemoryFiles);
        }
        config.setJtaDataSource(dataSource);
        if (nonJTADataSource != null) {
            config.setNonJtaDataSource(nonJTADataSource);
        }

        if (deactivated) {
            Tr.error(tc, "DEACTIVATED_CWWKD0202E");
            throw new IllegalStateException(Tr.formatMessage(tc, "DEACTIVATED_CWWKD0202E"));
        }

        PersistenceServiceUnit persistenceServiceUnit = persistenceService.createPersistenceServiceUnit(config);
        boolean successful = false;
        try {
            if (deactivated) {
                throw new IllegalStateException(Tr.formatMessage(tc, "DEACTIVATED_CWWKD0202E"));
            }

            // Ignore table creation for the extra PersistenceServiceUnit that persistent executor
            // creates to allow TRANSACTION_READ_UNCOMMITTED
            if (!(entitySet == SpecialEntitySet.PERSISTENT_EXECUTOR && entityClassNames.length == 1)) {
                boolean createTables = (Boolean) this.properties.get("createTables");
                boolean dropTables = (Boolean) this.properties.get("dropTables");
                if (createTables || dropTables) {
                    CheckpointPhase.onRestore(() -> dropAndOrCreateTables(persistenceServiceUnit, createTables, dropTables));
                }
            }

            if (deactivated) {
                throw new IllegalStateException(Tr.formatMessage(tc, "DEACTIVATED_CWWKD0202E"));
            }

            successful = true;
        } finally {
            if (!successful) {
                persistenceServiceUnit.close();
            }
        }

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "createPersistenceServiceUnit", persistenceServiceUnit);
        return persistenceServiceUnit;
    }

    // ---- ORM XML generation for NONE / Jakarta Data path (JPA 4.0 namespace) ----

    /**
     * Creates an in-memory ORM file using JPA 4.0 namespace (jakarta.ee).
     * Used only for the NONE entity set (Jakarta Data and generic entity paths).
     */
    protected InMemoryMappingFile createOrmFile(String schemaName,
                                                String tablePrefix,
                                                LinkedHashSet<String> tableNames,
                                                String[] entityClassNames,
                                                String[] entityClassEntries) {
        return ((entityClassNames == null || entityClassNames.length == 0)
                && (entityClassEntries == null || entityClassEntries.length == 0))
                       ? null
                       : new InMemoryMappingFile(
                             createOrm(schemaName, tablePrefix, tableNames, entityClassNames, entityClassEntries)
                                 .getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Generates an ORM XML string using JPA 4.0 namespace.
     * Old version used http://xmlns.jcp.org/xml/ns/persistence/orm version "2.1".
     * New version uses https://jakarta.ee/xml/ns/persistence/orm version "3.2".
     */
    protected String createOrm(String schemaName,
                               String tablePrefix,
                               LinkedHashSet<String> tableNames,
                               String[] entityClassNames,
                               String[] entityClassEntries) {
        StringBuilder builder = new StringBuilder();

        builder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + EOLN)
               .append("<entity-mappings xmlns=\"https://jakarta.ee/xml/ns/persistence/orm\"")
               .append("                 xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"")
               .append("                 xsi:schemaLocation=\"https://jakarta.ee/xml/ns/persistence/orm")
               .append("                     https://jakarta.ee/xml/ns/persistence/orm/orm_3_2.xsd\"")
               .append("                 version=\"3.2\">")
               .append(EOLN);

        if (schemaName != null && !schemaName.trim().isEmpty()) {
            builder.append(" <schema>" + schemaName + "</schema>" + EOLN);
        }

        tablePrefix = (tablePrefix == null) ? "" : tablePrefix.trim();

        if (entityClassNames != null && tableNames != null) {
            int i = 0;
            for (String tableName : tableNames) {
                builder.append(" <entity class=" + enquote(entityClassNames[i++]) + ">" + EOLN)
                       .append("  <table name=" + enquote(tablePrefix + tableName) + "/>" + EOLN)
                       .append(" </entity>" + EOLN);
            }
        }

        if (entityClassEntries != null) {
            for (String entityClassEntry : entityClassEntries) {
                builder.append(entityClassEntry);
            }
        }

        builder.append("</entity-mappings>" + EOLN);
        return builder.toString();
    }

    protected String parseSimpleName(String entityClassName) {
        return entityClassName.substring(entityClassName.lastIndexOf(".") + 1);
    }

    protected String enquote(String s) {
        return "\"" + s + "\"";
    }

    // ---- Schema/table lifecycle ----

    private void dropAndOrCreateTables(PersistenceServiceUnit persistenceServiceUnit,
                                       boolean createTables, boolean dropTables) throws Exception {
        LocalTransactionCurrent localTranCurrent = this.localTranCurrent;
        LocalTransactionCoordinator suspendedLTC = localTranCurrent.suspend();
        EmbeddableWebSphereTransactionManager tranMgr = this.tranMgr;
        Transaction suspendedTran = suspendedLTC == null ? tranMgr.suspend() : null;

        synchronized (persistenceServiceUnit) {
            try {
                if (createTables) {
                    if (dropTables)
                        persistenceServiceUnit.dropAndCreateTables();
                    else
                        persistenceServiceUnit.createTables();
                } else if (dropTables) {
                    persistenceServiceUnit.dropTables();
                }
            } finally {
                if (suspendedTran != null)
                    tranMgr.resume(suspendedTran);
                else if (suspendedLTC != null)
                    localTranCurrent.resume(suspendedLTC);
            }
        }
    }

    private boolean supportsIsolationLevel(int isolationLevel) throws Exception {
        ResourceConfig tempResourceInfo = resourceConfigFactory.createResourceConfig(DataSource.class.getName());
        tempResourceInfo.setSharingScope(ResourceConfig.SHARING_SCOPE_SHAREABLE);
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

        LocalTransactionCurrent localTranCurrent = this.localTranCurrent;
        LocalTransactionCoordinator suspendedLTC = localTranCurrent.suspend();
        EmbeddableWebSphereTransactionManager tranMgr = this.tranMgr;
        Transaction suspendedTran = suspendedLTC == null ? tranMgr.suspend() : null;
        boolean tranStarted = false;
        try {
            tranMgr.begin();
            tranStarted = true;
            try (Connection con = tempDataSource.getConnection()) {
                return con.getMetaData().supportsTransactionIsolationLevel(isolationLevel);
            }
        } finally {
            try {
                if (tranStarted)
                    tranMgr.rollback();
            } finally {
                if (suspendedTran != null)
                    tranMgr.resume(suspendedTran);
                else if (suspendedLTC != null)
                    localTranCurrent.resume(suspendedLTC);
            }
        }
    }

    // ---- DatabaseStore getters ----

    @Override
    public String getSchema() {
        return schema;
    }

    @Override
    public String getTablePrefix() {
        return tablePrefix;
    }
}
