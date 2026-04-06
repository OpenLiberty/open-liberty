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

import java.util.List;

import jakarta.data.repository.DataRepository;
import jakarta.data.repository.Repository;
import jakarta.data.repository.stateful.Persist;
import jakarta.transaction.Transactional;

/**
 * Stateful repository for the Fraction entity.
 * The repository inherits from the DataRepository interface.
 */
@Repository(dataStore = "MyDataStore")
public interface StatefulFractionRepository //
                extends DataRepository<Fraction, String> {

    @Persist
    @Transactional
    void persistAll(List<Fraction> fractions);

    @Persist
    void write(Fraction fraction);

}
