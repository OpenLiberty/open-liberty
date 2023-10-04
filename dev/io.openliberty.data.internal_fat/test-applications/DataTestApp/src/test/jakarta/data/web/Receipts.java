/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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

import java.util.Collection;
import java.util.Optional;

import jakarta.data.repository.BasicRepository;
import jakarta.data.repository.Repository;

import io.openliberty.data.repository.Delete;
import io.openliberty.data.repository.Filter;

/**
 * Repository interface for the Receipt entity which is a record
 */
@Repository
public interface Receipts extends BasicRepository<Receipt, Long> {
    Optional<Receipt> deleteByPurchaseId(long purchaseId);

    @Delete
    @Filter(by = "customer")
    Collection<Receipt> deleteFor(String customer);
}
