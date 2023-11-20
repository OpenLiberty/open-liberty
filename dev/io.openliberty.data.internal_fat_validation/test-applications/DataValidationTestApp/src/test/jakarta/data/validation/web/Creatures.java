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
package test.jakarta.data.validation.web;

import java.util.List;

import jakarta.data.repository.BasicRepository;
import jakarta.data.repository.OrderBy;
import jakarta.data.repository.Repository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Repository for a Jakarta Persistence entity with bean validation annotations.
 */
@Repository(dataStore = "java:module/jdbc/DerbyDataSource")
public interface Creatures extends BasicRepository<@Valid Creature, @Positive Long> {

    int countById(Long id);

    @OrderBy("id")
    @Size(min = 0, max = 3)
    List<Creature> findByScientificNameStartsWithAndWeightBetween(@NotBlank String genus,
                                                                  @Positive float minWeight,
                                                                  @Positive float maxWeight);

    boolean updateByIdSetWeight(long id, float newWeight);
}