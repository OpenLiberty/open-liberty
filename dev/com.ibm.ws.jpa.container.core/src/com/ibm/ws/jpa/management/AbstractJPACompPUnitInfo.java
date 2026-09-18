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

import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.PersistenceUnitTransactionType;

/**
 * Version-sensitive base for component-specific PersistenceUnitInfo. <p>
 *
 * Contains only the method whose return-type descriptor differs across JPA versions:
 * {@code getTransactionType()}.  In JPA/Jakarta EE up to 3.2, the return type is
 * {@code javax.persistence.spi.PersistenceUnitTransactionType} (transformed to
 * {@code jakarta.persistence.spi.PersistenceUnitTransactionType}).  In JPA 4.0 the
 * type moved to the top-level {@code jakarta.persistence} package. <p>
 *
 * All other delegation logic lives in {@link JPACompPUnitInfo} which extends this class.
 * The JPA 4.0 overlay in {@code com.ibm.ws.jpa.container.jakarta.40} supplies a
 * compiled-from-source replacement of this class that returns
 * {@code jakarta.persistence.PersistenceUnitTransactionType}.
 */
abstract class AbstractJPACompPUnitInfo implements PersistenceUnitInfo {

    // The common (real) PUnitInfo (non component specific).
    // Declared here so getTransactionType() can delegate to it without
    // referencing ivPUnitInfo across the class hierarchy boundary.
    protected final JPAPUnitInfo ivPUnitInfo;

    AbstractJPACompPUnitInfo(JPAPUnitInfo puInfo) {
        ivPUnitInfo = puInfo;
    }

    /**
     * @see javax.persistence.spi.PersistenceUnitInfo#getTransactionType()
     */
    @Override
    public PersistenceUnitTransactionType getTransactionType() {
        return ivPUnitInfo.getTransactionType();
    }
}
