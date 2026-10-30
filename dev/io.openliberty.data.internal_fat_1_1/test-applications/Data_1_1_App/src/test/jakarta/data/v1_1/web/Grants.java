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
package test.jakarta.data.v1_1.web;

import static jakarta.data.repository.By.ID;

import java.util.List;
import java.util.Optional;

import jakarta.data.Order;
import jakarta.data.repository.By;
import jakarta.data.repository.DataRepository;
import jakarta.data.repository.Delete;
import jakarta.data.repository.Fetching;
import jakarta.data.repository.Find;
import jakarta.data.repository.Insert;
import jakarta.data.repository.Repository;
import jakarta.data.restrict.Restriction;
import jakarta.persistence.query.QueryOptions;

/**
 * Repository for the Grant entity, used to test the Fetching annotation
 * and QueryOptions with entityGraph.
 *
 * <ul>
 * <li>{@link #withGiver} uses {@code @Fetching} to eagerly load the lazy
 *     {@code giver} association.</li>
 * <li>{@link #withRecipient} uses {@code @QueryOptions(entityGraph=...)} to
 *     eagerly load the lazy {@code recipient} association.</li>
 * <li>{@link #find} loads neither lazy association nor {@code reportingDates},
 *     allowing tests to verify all three remain uninitialized.</li>
 * </ul>
 */
@Repository(dataStore = "MyDataStore")
public interface Grants extends DataRepository<Grant, Long> {

    @Insert
    void issue(Grant grant);

    @Insert
    void registerGiver(Giver grantee);

    @Insert
    void registerRecipient(Grantee recipient);

    /**
     * Finds a grant by id with no lazy attributes eagerly loaded.
     * All three lazy attributes (giver, recipient, reportingDates)
     * remain uninitialized.
     */
    @Find
    List<Grant> show(Restriction<Grant> filter,
                     Order<Grant> sorts);

    @Delete
    void terminateAll();

    /**
     * Finds a grant by id, eagerly loading the recipient and reportingDates,
     * but not the giver association, which remains lazy.
     */
    @Find
    @Fetching(_Grant.REPORTING_DATES)
    @QueryOptions(entityGraph = "Grant.withRecipient")
    Optional<Grant> withRecipientAndReportingDates(@By(ID) long id);

}
