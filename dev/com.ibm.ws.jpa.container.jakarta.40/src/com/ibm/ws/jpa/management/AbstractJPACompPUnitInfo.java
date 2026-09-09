/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.jpa.management;

import jakarta.persistence.PersistenceUnitTransactionType;

/**
 * JPA 4.0 override of the pre-4.0 {@code AbstractJPACompPUnitInfo} that lives in
 * {@code com.ibm.ws.jpa.container.core}.
 *
 * <p>In JPA 4.0 (Jakarta EE 12), {@code PersistenceUnitTransactionType} was relocated from
 * {@code jakarta.persistence.spi} to the top-level {@code jakarta.persistence} package.
 * The transformer-produced base class still references
 * {@code jakarta.persistence.spi.PersistenceUnitTransactionType} because the 1:1 package rename
 * from {@code javax.persistence.spi} produces that path. That class no longer exists in the
 * spi package at JPA 4.0, causing a {@code NoClassDefFoundError} at runtime.
 *
 * <p>This replacement (same package, same name) is compiled directly against the JPA 4.0 API
 * ({@code io.openliberty.jakarta.persistence.4.0}) and is overlaid onto the transformed jar
 * by the {@code com.ibm.ws.jpa.container.jakarta.40} bundle. All delegation logic remains in
 * the transformed {@code JPACompPUnitInfo}; only {@code getTransactionType()} is here.
 */
abstract class AbstractJPACompPUnitInfo {

    // The common (real) PUnitInfo (non component specific).
    // Set by JPACompPUnitInfo constructor; declared here so getTransactionType() can access it.
    protected final JPAPUnitInfo ivPUnitInfo;

    AbstractJPACompPUnitInfo(JPAPUnitInfo puInfo) {
        ivPUnitInfo = puInfo;
    }

    /**
     * Returns the transaction type using the JPA 4.0 non-spi type location.
     *
     * @see jakarta.persistence.spi.PersistenceUnitInfo#getTransactionType()
     */
    public PersistenceUnitTransactionType getTransactionType() {
        return ivPUnitInfo.getTransactionType();
    }
}
