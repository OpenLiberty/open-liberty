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
package com.ibm.ws.jpa.management;

import javax.persistence.spi.PersistenceUnitTransactionType;

/**
 * Base class that isolates the version-specific {@code PersistenceUnitTransactionType} reference
 * from the rest of {@link JPAPUnitInfo}.
 *
 * <p>In JPA 1.0–3.2 (Jakarta EE 9–11) the type lives at
 * {@code javax.persistence.spi.PersistenceUnitTransactionType} which the Jakarta-EE transformer
 * rewrites to {@code jakarta.persistence.spi.PersistenceUnitTransactionType} — the class still
 * exists in the spi package for those API versions.
 *
 * <p>In JPA 4.0 (Jakarta EE 12) the type was moved to the top-level
 * {@code jakarta.persistence.PersistenceUnitTransactionType} and removed from the spi package.
 * The JPA 4.0 overlay project (com.ibm.ws.jpa.container.jakarta.40) supplies a replacement
 * version of this class compiled against the non-spi import so that the 4.0 container bundle
 * wires correctly when the JPA 4.0 API is active.
 *
 * <p>{@link JPAPUnitInfo#setTransactionType(javax.persistence.spi.PersistenceUnitTransactionType)}
 * bridges to {@link #setTransactionTypeByName(String, boolean)} using only the enum constant name
 * so that the call never crosses the javax/jakarta namespace boundary at the method signature level.
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
     * @see javax.persistence.spi.PersistenceUnitInfo#getTransactionType()
     */
    public final PersistenceUnitTransactionType getTransactionType() {
        return ivTxType;
    }

    /**
     * Sets the transaction type from the enum constant name so that the call site on
     * {@link JPAPUnitInfo} never has to pass a typed enum value across the class hierarchy.
     * This avoids a {@code NoSuchMethodError} when the 4.0 overlay replaces this class with
     * a version that stores {@code jakarta.persistence.PersistenceUnitTransactionType} instead.
     *
     * @param name        the {@link PersistenceUnitTransactionType#name()} value, or {@code null}
     *                        to use the runtime default
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
