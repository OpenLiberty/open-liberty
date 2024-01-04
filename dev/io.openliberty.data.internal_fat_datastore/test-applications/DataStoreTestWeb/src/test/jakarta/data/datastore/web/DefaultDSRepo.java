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
package test.jakarta.data.datastore.web;

import jakarta.data.repository.DataRepository;
import jakarta.data.repository.Insert;
import jakarta.data.repository.Repository;

@Repository(dataStore = "java:comp/DefaultDataSource")
public interface DefaultDSRepo extends DataRepository<DefaultDSEntity, Long> {

    @Insert
    void insert(DefaultDSEntity e);

    boolean existsByIdAndValue(long id, String value);
}