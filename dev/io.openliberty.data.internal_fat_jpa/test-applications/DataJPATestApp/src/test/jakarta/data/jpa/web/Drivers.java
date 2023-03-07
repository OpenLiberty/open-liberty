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
package test.jakarta.data.jpa.web;

import java.util.stream.Stream;

import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.OrderBy;
import jakarta.data.repository.Repository;

/**
 * Repository for testing OneToOne relationship between Driver and DriversLicense entities.
 */
@Repository
public interface Drivers extends CrudRepository<Driver, Integer> {

    int deleteByFullNameEndsWith(String ending);

    @OrderBy("birthday")
    Stream<DriversLicense> findByFullNameEndsWith(String ending);
}