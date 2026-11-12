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

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * Hibernate PhysicalNamingStrategy that prepends a configured table prefix to all table names.
 *
 * Replaces the ORM XML stanza:
 *   {@code <table name="WLP_JOBEXECUTION"/>}
 * which was generated at runtime by createOrmFileContentsForBatch() / createOrmFileContentsForPersistentExecutor()
 * in the EclipseLink (2.0) implementation.
 *
 * The prefix is only applied to table names. Column, sequence, catalog, and schema names
 * are passed through unchanged.
 *
 * Thread-safe — all fields are final.
 */
@Trivial
public class HibernatePrefixingNamingStrategy implements PhysicalNamingStrategy {

    private final String prefix;

    /**
     * @param prefix the table name prefix to prepend (e.g. {@code "WLP_"}). Must not be null.
     */
    public HibernatePrefixingNamingStrategy(String prefix) {
        this.prefix = prefix == null ? "" : prefix;
    }

    @Override
    public Identifier toPhysicalTableName(Identifier name, JdbcEnvironment context) {
        if (name == null) {
            return null;
        }
        return Identifier.toIdentifier(prefix + name.getText(), name.isQuoted());
    }

    @Override
    public Identifier toPhysicalCatalogName(Identifier name, JdbcEnvironment context) {
        return name;
    }

    @Override
    public Identifier toPhysicalSchemaName(Identifier name, JdbcEnvironment context) {
        return name;
    }

    @Override
    public Identifier toPhysicalSequenceName(Identifier name, JdbcEnvironment context) {
        return name;
    }

    @Override
    public Identifier toPhysicalColumnName(Identifier name, JdbcEnvironment context) {
        return name;
    }
}
