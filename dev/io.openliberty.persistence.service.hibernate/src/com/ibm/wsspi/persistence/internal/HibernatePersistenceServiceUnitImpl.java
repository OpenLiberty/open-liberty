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

import java.io.Writer;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.spi.PersistenceUnitInfo;

import org.hibernate.jpa.HibernatePersistenceProvider;
import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;
import org.hibernate.jpa.boot.internal.PersistenceUnitInfoDescriptor;
import org.hibernate.jpa.boot.spi.EntityManagerFactoryBuilder;

import com.ibm.wsspi.persistence.PersistenceServiceUnit;
import com.ibm.wsspi.persistence.PersistenceServiceUnitConfig;

/**
 * PersistenceServiceUnit implementation backed by Hibernate 8.x.
 *
 * Replaces PersistenceServiceUnitImpl (EclipseLink) from com.ibm.ws.persistence.jakarta.
 *
 * Key differences from the EclipseLink version:
 * - No PsPersistenceProvider subclass needed — standard JPA SPI public method used directly
 * - No TARGET_SERVER / TargetServer property — Hibernate uses hibernate.transaction.jta.platform
 * - No UseNationalCharacterVaryingTypeForString — Hibernate handles NVARCHAR natively
 * - getDatabaseTerminationToken() unwraps Hibernate SessionFactory, not EclipseLink DatabaseSession
 * - No manual createSequences() transaction workaround — Hibernate handles sequences correctly
 */
public final class HibernatePersistenceServiceUnitImpl implements PersistenceServiceUnit {

    private static final String SCHEMA_GENERATION_DATABASE_ACTION =
        "jakarta.persistence.schema-generation.database.action";
    private static final String SCHEMA_GENERATION_SCRIPTS_ACTION =
        "jakarta.persistence.schema-generation.scripts.action";
    private static final String SCHEMA_GENERATION_SCRIPTS_CREATE_TARGET =
        "jakarta.persistence.schema-generation.scripts.create-target";
    private static final String DROP_AND_CREATE = "drop-and-create";
    private static final String CREATE = "create";
    private static final String DROP = "drop";

    private final EntityManagerFactory _emf;
    private final HibernatePUInfoImpl _pui;
    private final Map<String, Object> _serviceProperties;
    private final HibernateSchemaManager _schemaMgr;

    public HibernatePersistenceServiceUnitImpl(PersistenceServiceUnitConfig conf,
                                               InMemoryUrlStreamHandler inmemHandler,
                                               URL bundleRootUrl) {
        _pui = new HibernatePUInfoImpl(conf, inmemHandler, bundleRootUrl);

        _serviceProperties = new HashMap<>();
        // Schema-generation terminator — ensures DDL files have statement delimiters
        _serviceProperties.put("jakarta.persistence.schema-generation.scripts.create-source", "metadata");
        _serviceProperties.put("jakarta.persistence.schema-generation.scripts.drop-source", "metadata");

        // Build the EMF via EntityManagerFactoryBuilderImpl, supplying LibertyClassLoaderService
        // so that BytecodeProviderInitiator.loadJavaServices(BytecodeProvider.class) returns
        // empty and Hibernate falls back to the none/ no-op provider instead of ByteBuddy.
        // ByteBuddy's BytecodeProviderImpl has no public no-arg constructor and cannot be
        // loaded by ServiceLoader across OSGi bundle boundaries.
        LibertyClassLoaderService cls = new LibertyClassLoaderService(_pui.getClassLoader());
        EntityManagerFactoryBuilder builder = new EntityManagerFactoryBuilderImpl(
                new PersistenceUnitInfoDescriptor(_pui), new HashMap<>(_serviceProperties), cls);
        _emf = builder.build();

        _schemaMgr = new HibernateSchemaManager(_serviceProperties, _pui);
    }

    @Override
    public EntityManager createEntityManager() {
        return _emf.createEntityManager();
    }

    @Override
    public void createTables() {
        _schemaMgr.generateSchema(SCHEMA_GENERATION_DATABASE_ACTION, CREATE);
    }

    @Override
    public void dropAndCreateTables() {
        _schemaMgr.generateSchema(SCHEMA_GENERATION_DATABASE_ACTION, DROP_AND_CREATE);
    }

    @Override
    public void dropTables() {
        _schemaMgr.generateSchema(SCHEMA_GENERATION_DATABASE_ACTION, DROP);
    }

    @Override
    public void generateDDL(Writer out) {
        _schemaMgr.generateSchema(SCHEMA_GENERATION_SCRIPTS_ACTION, CREATE,
                                  SCHEMA_GENERATION_SCRIPTS_CREATE_TARGET, out);
    }

    @Override
    public void close() {
        _pui.close();
        _emf.close();
    }

    /**
     * Returns the SQL statement terminator for the database dialect.
     *
     * Hibernate 6+: JdbcServices.getDialect() was removed; use getJdbcEnvironment().getDialect().
     * Replaces EclipseLink: emf.unwrap(DatabaseSession).getPlatform().getStoredProcedureTerminationToken()
     */
    @Override
    public String getDatabaseTerminationToken() {
        // Hibernate 8 removed getSqlStatementDelimiter() from Dialect.
        // DDL scripts always use ";" as the statement terminator.
        return ";";
    }

}
