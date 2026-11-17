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

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.spi.PersistenceUnitInfo;

import org.hibernate.dialect.OracleDialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;
import org.hibernate.jpa.boot.internal.PersistenceUnitInfoDescriptor;

import com.ibm.wsspi.persistence.DDLGenerationWriter;

/**
 * Manages all schema generation (DDL) operations for Hibernate 8.x.
 *
 * Replaces SchemaManager (EclipseLink) from com.ibm.ws.persistence.jakarta.
 *
 * What was removed compared to old SchemaManager:
 * - Static platform termination token map (8 entries hardcoded to EclipseLink class names):
 *   Hibernate's Dialect.getSqlStatementDelimiter() returns the correct token directly.
 * - EclipseLink.createSequences() bug workaround (Eclipse bug 356256):
 *   Hibernate handles sequence creation correctly without needing a manual transaction.
 * - PsPersistenceProvider.createContainerEMF() with requiresConnection=false:
 *   Hibernate's standard createContainerEntityManagerFactory() is used — it does not
 *   require a live DB connection if schema-generation source is "metadata".
 * - "copy with new name" workaround: EclipseLink silently ignored repeated schema-gen
 *   requests with the same PU name. Hibernate does not have this bug.
 *   The createCopyWithNewName() method is still called for safety but uses the same logic.
 *
 * What is kept:
 * - DDLGenerationWriter wrapping to intercept premature close() calls from Hibernate
 * - Oracle EXIT command postprocessing
 */
class HibernateSchemaManager {

    private static final String SCHEMA_GENERATION_SCRIPTS_CREATE_TARGET =
        "jakarta.persistence.schema-generation.scripts.create-target";

    // Care needs to be taken to not modify this map as it is shared with
    // HibernatePersistenceServiceUnitImpl.
    private final Map<String, Object> _serviceProperties;
    private final HibernatePUInfoImpl _pui;

    HibernateSchemaManager(Map<String, Object> props, HibernatePUInfoImpl pui) {
        _serviceProperties = props;
        _pui = pui;
    }

    void generateSchema(Object... schemaGenKeyValuePair) {
        Map<String, Object> props = new HashMap<>();
        for (int i = 0; i < schemaGenKeyValuePair.length; i += 2) {
            props.put((String) schemaGenKeyValuePair[i], schemaGenKeyValuePair[i + 1]);
        }
        props.putAll(_serviceProperties);

        boolean closeWriter = false;
        DDLGenerationWriter writer = null;
        Writer out = (Writer) props.get(SCHEMA_GENERATION_SCRIPTS_CREATE_TARGET);
        if (out != null) {
            if (out instanceof DDLGenerationWriter) {
                // The caller that provided this writer is responsible for closing it
                writer = (DDLGenerationWriter) out;
            } else {
                // Replace the provided writer with our own that suppresses close() calls from
                // Hibernate. This allows us to append an exit command before truly closing.
                writer = new DDLGenerationWriter(out);
                closeWriter = true;
            }
            props.put(SCHEMA_GENERATION_SCRIPTS_CREATE_TARGET, writer);
        }

        // Use a copy of the PUI with a new name for schema generation.
        // This matches the old EclipseLink workaround pattern; Hibernate does not need it
        // but it is harmless and kept for consistency.
        PersistenceUnitInfo puiCopy = _pui.createCopyWithNewName();

        EntityManagerFactory emf = null;
        try {
            LibertyClassLoaderService cls = new LibertyClassLoaderService(puiCopy.getClassLoader());
            emf = new EntityManagerFactoryBuilderImpl(
                    new PersistenceUnitInfoDescriptor(puiCopy), new HashMap<>(props), cls).build();

            if (writer != null) {
                postProcess(emf, writer);
            }
        } finally {
            if (emf != null) {
                emf.close();
            }
        }

        if (writer != null && closeWriter) {
            try {
                writer.writeExitAndClose();
            } catch (IOException e) {
                throw new PersistenceException(e);
            }
        }
    }

    /**
     * For Oracle, append EXIT; to the DDL script so it can be run directly from SQL*Plus.
     *
     * Uses SessionFactory.unwrap to access the Hibernate dialect. The Dialect class
     * is obtained via JdbcServices, which is available in Hibernate 6+.
     * In Hibernate 7/8.x the API is:
     *   SessionFactory.getJdbcServices().getJdbcEnvironment().getDialect()
     */
    private void postProcess(EntityManagerFactory emf, DDLGenerationWriter writer) {
        // getJdbcServices() is on SessionFactoryImplementor, not the SessionFactory public API
        SessionFactoryImplementor sf = emf.unwrap(SessionFactoryImplementor.class);
        org.hibernate.dialect.Dialect dialect =
            sf.getJdbcServices().getJdbcEnvironment().getDialect();
        if (dialect instanceof OracleDialect) {
            try {
                writer.setExitCommand("\nEXIT;");
            } catch (IOException e) {
                throw new PersistenceException(e);
            }
        }
    }
}
