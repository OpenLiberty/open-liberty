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

import java.util.Optional;

import jakarta.data.constraint.EqualTo;
import jakarta.data.repository.By;
import jakarta.data.repository.Find;
import jakarta.data.repository.Is;
import jakarta.data.repository.Repository;
import jakarta.data.repository.stateful.Detach;

/**
 * Stateful repository for the Fraction entity
 */
@Repository(dataStore = "MyDataStore")
public interface StatefulFractions {

    @Detach
    void detach(Fraction entity);

    @Find
    Optional<Fraction> fetch//
    (@By(_Fraction.NUMERATOR) @Is(EqualTo.class) long num,
     @By(_Fraction.DENOMINATOR) @Is long den);
}
