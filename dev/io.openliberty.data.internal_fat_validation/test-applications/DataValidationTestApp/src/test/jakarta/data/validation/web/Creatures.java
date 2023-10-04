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

import jakarta.data.repository.BasicRepository;
import jakarta.data.repository.Repository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

/**
 * Repository for a Jakarta Persistence entity with bean validation annotations.
 */
@Repository(dataStore = "java:module/jdbc/DerbyDataSource")
public interface Creatures extends BasicRepository<@Valid Creature, @Positive Long> {

    int countById(Long id);

    boolean updateByIdSetWeight(long id, float newWeight);
}