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
package test.jakarta.data.jpa.web;

import java.util.List;

import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.KeysetAwareSlice;
import jakarta.data.repository.OrderBy;
import jakarta.data.repository.Pageable;
import jakarta.data.repository.Repository;

/**
 *
 */
@Repository
public interface Businesses extends CrudRepository<Business, Integer> {

    // embeddable 1 level deep
    List<Business> findByLatitudeBetweenOrderByLongitudeDesc(float min, float max);

    // embeddable 3 levels deep where @Column resolves name conflict
    Business[] findByLocation_Address_Street_NameIgnoreCaseEndsWithOrderByLocation_Address_Street_DirectionIgnoreCaseAscNameAsc(String streetName);

    // embeddable 2 levels deep
    @OrderBy(value = "city", descending = true)
    @OrderBy("location.address.zip")
    @OrderBy("houseNum")
    @OrderBy("id")
    KeysetAwareSlice<Business> findByZipIn(Iterable<Integer> zipCodes, Pageable pagination);
}