/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package com.ibm.ws.jpa.management;

import jakarta.persistence.PersistenceUnitTransactionType;

/**
 * JPA 4.0 override of the pre-4.0 {@code AbstractJPAPUnitInfo} that lives in
 * {@code com.ibm.ws.jpa.container.core}.
 *
 * <p>In JPA 4.0 (Jakarta EE 12), {@code PersistenceUnitTransactionType} was relocated from
 * {@code jakarta.persistence.spi} to the top-level {@code jakarta.persistence} package.
 * The transformer-produced base class still references
 * {@code jakarta.persistence.spi.PersistenceUnitTransactionType} because the 1:1 package rename
 * from {@code javax.persistence.spi} produces that path.  That class no longer exists in the
 * spi package at JPA 4.0, causing a {@code NoClassDefFoundError} at {@code JPAPUnitInfo.<init>}.
 *
 * <p>This replacement (same package, same name, same method signatures) is compiled directly
 * against the JPA 4.0 API ({@code io.openliberty.jakarta.persistence.4.0}) and is overlaid
 * onto the transformed jar by the {@code com.ibm.ws.jpa.container.jakarta.40} bundle so that
 * the correct class is resolved at runtime when the JPA 4.0 API bundle is wired.
 *
 * <p>The public API is exactly the same as the pre-4.0 version because
 * {@link JPAPUnitInfo#setTransactionType(javax.persistence.spi.PersistenceUnitTransactionType)}
 * bridges via the name-based {@link #setTransactionTypeByName(String, boolean)} rather than
 * calling the typed setter directly.
 */
public abstract class AbstractJPAPUnitInfo {

    // Transaction Type, i.e. JTA or ResourceLocal
    private PersistenceUnitTransactionType ivTxType = null;

    /**
     * Initialises the transaction type to JTA as the default for a new persistence unit.
     * Called by {@link JPAPUnitInfo#JPAPUnitInfo(JPAApplInfo, JPAPuId, ClassLoader)}.
     */
    protected final void initTxType() {
        ivTxType = PersistenceUnitTransactionType.JTA;
    }

    /**
     * Returns the transaction type of this persistence unit.
     *
     * @see jakarta.persistence.spi.PersistenceUnitInfo#getTransactionType()
     */
    public final PersistenceUnitTransactionType getTransactionType() {
        return ivTxType;
    }

    /**
     * Returns {@code true} if the transaction type is JTA.
     * Descriptor is {@code ()Z} — safe across all JPA versions.
     */
    public final boolean isJtaTransactionType() {
        return ivTxType == null || PersistenceUnitTransactionType.JTA == ivTxType;
    }

    /**
     * Sets the transaction type from the enum constant name so that the call site on
     * {@link JPAPUnitInfo} never has to pass a typed enum value across the class hierarchy.
     *
     * @param name            the enum constant name ({@code "JTA"}, {@code "RESOURCE_LOCAL"}),
     *                        or {@code null} to use the runtime default
     * @param isServerRuntime {@code true} when running in a server (not an app client)
     */
    final void setTransactionTypeByName(String name, boolean isServerRuntime) {
        if (name == null) {
            ivTxType = isServerRuntime ? PersistenceUnitTransactionType.JTA : PersistenceUnitTransactionType.RESOURCE_LOCAL;
        } else if ("JTA".equals(name)) {
            ivTxType = PersistenceUnitTransactionType.JTA;
        } else {
            ivTxType = PersistenceUnitTransactionType.RESOURCE_LOCAL;
        }
    }
}
