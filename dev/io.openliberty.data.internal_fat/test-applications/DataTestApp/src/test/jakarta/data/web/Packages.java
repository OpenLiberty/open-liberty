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

import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.KeysetAwarePage;
import jakarta.data.repository.Pageable;
import jakarta.data.repository.Repository;

/**
 *
 */
@Repository
public interface Packages extends CrudRepository<Package, Integer> {
    List<Package> findByHeightBetween(float minHeight, float maxHeight);

    KeysetAwarePage<Package> findByHeightGreaterThanOrderByLengthAscOrderByWidthDescOrderByHeightDescOrderByIdAsc(float minHeight, Pageable pagination);

    boolean updateByIdAddHeightMultiplyLengthDivideWidth(int id, float heightToAdd, float lengthMultiplier, float widthDivisor);

    void updateByIdDivideLengthDivideWidthDivideHeight(int id, float lengthDivisor, float widthDivisor, float heightDivisor);

    long updateByLengthLessThanEqualAndHeightBetweenMultiplyLengthMultiplyWidthSetHeight(float maxLength, float minHeight, float maxHeight,
                                                                                         float lengthMultiplier, float widthMultiplier, float newHeight);
}