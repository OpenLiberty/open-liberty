/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.jakarta.data.web;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import jakarta.data.Pageable;
import jakarta.data.repository.Repository;

/**
 *
 */
@Repository
public interface Tariffs {

    int deleteByLeviedBy(String country);

    Stream<Tariff> findByLeviedAgainst(String country);

    Iterator<Tariff> findByLeviedAgainstLessThanOrderByKeyDesc(String countryNameBefore, Pageable pagination);

    Tariff findByLeviedByAndLeviedAgainstAndLeviedOn(String taxingCountry, String taxedCountry, String item);

    List<Tariff> findByLeviedByOrderByKey(String country, Pageable pagination);

    Tariff save(Tariff t);
}
