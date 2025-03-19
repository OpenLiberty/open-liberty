/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package test.jakarta.data.web;

import java.util.Optional;

import jakarta.data.repository.DataRepository;
import jakarta.data.repository.Delete;
import jakarta.data.repository.Find;
import jakarta.data.repository.Insert;
import jakarta.data.repository.Repository;

/**
 * Repository for an entity where one of the attributes is a record that is in
 * this case used as an embeddable, but elsewhere used as an entity.
 */
@Repository
public interface Purchases extends DataRepository<Purchase, Long> {

    @Insert
    void buy(Purchase purchase);

    @Delete
    void clearAll();

    // Selects multiple entity attributes into a record, per the spec
    @Find
    Optional<Receipt> receiptFor(long purchaseId);

}
