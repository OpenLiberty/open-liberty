/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
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

import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import jakarta.data.page.PageRequest;
import jakarta.data.repository.DataRepository;
import jakarta.data.repository.Insert;
import jakarta.data.repository.Repository;

/**
 *
 */
@Repository
public interface Tariffs extends DataRepository<Tariff, Long> {

    int deleteByLeviedBy(String country);

    Stream<Tariff> findByLeviedAgainst(String country);

    Iterator<Tariff> findByLeviedAgainstLessThanOrderByKeyDesc(String countryNameBefore, PageRequest pagination);

    Tariff findByLeviedByAndLeviedAgainstAndLeviedOn(String taxingCountry, String taxedCountry, String item);

    List<Tariff> findByLeviedByOrderByKey(String country, PageRequest pagination);

    @Insert
    Tariff save(Tariff t);
}
