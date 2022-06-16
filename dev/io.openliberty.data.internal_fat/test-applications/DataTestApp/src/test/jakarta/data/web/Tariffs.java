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

import java.util.List;
import java.util.stream.Stream;

import io.openliberty.data.Data;

/**
 *
 */
@Data
public interface Tariffs {

    int deleteByLeviedBy(String country);

    Stream<Tariff> findByLeviedAgainst(String country);

    Tariff findByLeviedByAndLeviedAgainstAndLeviedOn(String taxingCountry, String taxedCountry, String item);

    List<Tariff> findByLeviedByOrderByKey(String country);

    Tariff save(Tariff t);
}
