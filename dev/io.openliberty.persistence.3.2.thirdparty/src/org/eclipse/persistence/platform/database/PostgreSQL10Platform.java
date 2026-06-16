/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation. All rights reserved.
 * Copyright (c) 2025 Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2026 IBM Corporation. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0,
 * or the Eclipse Distribution License v. 1.0 which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: EPL-2.0 OR BSD-3-Clause
 */

// Contributors:
//     13/01/2022-4.0.0 Tomas Kraus - 1391: JSON support in JPA
package org.eclipse.persistence.platform.database;

import org.eclipse.persistence.exceptions.ValidationException;
import org.eclipse.persistence.internal.localization.LoggingLocalization;
import org.eclipse.persistence.internal.sessions.AbstractSession;
import org.eclipse.persistence.logging.DefaultSessionLog;
import org.eclipse.persistence.logging.SessionLog;
import org.eclipse.persistence.tools.schemaframework.FieldDefinition;

import java.io.IOException;
import java.io.Writer;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;

/**
 * Postgres 10 database platform extension.
 * <p>
 * <b>Purpose</b>: Provides Postgres 10 specific behavior.
 * <p>
 * <b>Responsibilities</b>:
 * <ul>
 * <li>Native JSON support added in version 10.</li>
 * </ul>
 * This class requires Postgres JDBC driver on the classpath.
 */
public class PostgreSQL10Platform extends PostgreSQLPlatform {

    /**
     * Add extended JSON functionality dependent on PostgreSQL JDBC driver.
     */
    public interface PostgreSQL10JsonExtension {
        /**
         * Check whether provided instance is an instance of {@code PGobject}.
         *
         * @param parameter an instance to check
         * @return value of {@code true} when provided instance is an instance
         *         of {@code PGobject} or {@code false} otherwise
         */
        boolean isPgObjectInstance(final Object parameter);
    }

    // PostgreSQL10JsonExtension implementation: PostgreSQL10JsonPlatform instance if available or null
    private final PostgreSQL10JsonExtension postgreSQL10JsonExtension;

    /**
     * Creates an instance of Postgres 10 platform.
     */
    public PostgreSQL10Platform() {
        super();
        // Eager PostgreSQL10JsonExtension initialization from Postgres 10 specific platform
        // does not break the CORBA Extension tests.
        if (this.getJsonPlatform() instanceof PostgreSQL10JsonExtension) {
            postgreSQL10JsonExtension = (PostgreSQL10JsonExtension) this.getJsonPlatform();
            DefaultSessionLog.getLog().log(SessionLog.FINE, () -> LoggingLocalization.buildMessage("pgsql10_platform_with_json_extension"));
        // Missing PostgreSQL10JsonPlatform from org.eclipse.persistence.pgsql module.
        // This will cause JSON related functionality to fail.
        } else {
            postgreSQL10JsonExtension = null;
            DefaultSessionLog.getLog().log(SessionLog.FINE, () -> LoggingLocalization.buildMessage("pgsql10_platform_without_json_extension"));
        }
    }

    /**
     * INTERNAL
     * Set the parameter in the JDBC statement at the given index.
     * This support a wide range of different parameter types, and is heavily optimized for common types.
     * Handles Postgres specific PGobject instances.
     *
     * @param parameter the parameter to set
     * @param statement target {@code PreparedStatement} instance
     * @param index index of the parameter in the statement
     * @param session current database session
     */
    @Override
    public void setParameterValueInDatabaseCall(
            final Object parameter, final PreparedStatement statement,
            final int index, final AbstractSession session
    ) throws SQLException {
        if (postgreSQL10JsonExtension != null && postgreSQL10JsonExtension.isPgObjectInstance(parameter)) {
            statement.setObject(index, parameter);
        } else {
            super.setParameterValueInDatabaseCall(parameter, statement, index, session);
        }
    }

    /**
     * INTERNAL
     * Set the parameter in the JDBC statement at the given index.
     * This support a wide range of different parameter types, and is heavily optimized for common types.
     * Handles Postgres specific PGobject instances.
     *
     * @param parameter the parameter to set
     * @param statement target {@code CallableStatement} instance
     * @param name name of the parameter in the statement
     * @param session current database session
     */
    @Override
    public void setParameterValueInDatabaseCall(
            final Object parameter, final CallableStatement statement,
            final String name, final AbstractSession session
    ) throws SQLException {
        if (postgreSQL10JsonExtension != null && postgreSQL10JsonExtension.isPgObjectInstance(parameter)) {
            statement.setObject(name, parameter);
        } else {
            super.setParameterValueInDatabaseCall(parameter, statement, name, session);
        }
    }

    /*
                                 ____  ____  __
                                |    \|    \|  |
                                |  |  |  |  |  |__
                                |____/|____/|_____|
     */

    @Override
    protected Map<Class<?>, FieldDefinition.DatabaseType> buildDatabaseTypes() {
        final Map<Class<?>, FieldDefinition.DatabaseType> fieldTypeMapping = super.buildDatabaseTypes();
        // Mapping for JSON type.
        getJsonPlatform().updateFieldTypes(fieldTypeMapping);
        return fieldTypeMapping;
    }

    @Override
    protected Map<String, Class<?>> buildJavaTypes() {
        final Map<String, Class<?>> classTypeMapping = super.buildJavaTypes();
        // Mapping for JSON type.
        getJsonPlatform().updateClassTypes(classTypeMapping);
        return classTypeMapping;
    }

    @Override
    public void printFieldTypeSize(Writer writer, FieldDefinition field, FieldDefinition.DatabaseType databaseType, boolean shouldPrintFieldIdentityClause) throws IOException {
        super.printFieldTypeSize(writer, field, databaseType, false);
    }

    @Override
    public void printFieldIdentityClause(Writer writer) throws ValidationException {
        try {
            writer.write(" GENERATED BY DEFAULT AS IDENTITY");
        } catch (IOException ioException) {
            throw ValidationException.fileError(ioException);
        }
    }
}
