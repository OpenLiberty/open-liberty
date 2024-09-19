/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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

import jakarta.data.repository.Insert;
import jakarta.data.repository.Repository;

/**
 * A repository that handles multiple entities that are used in other repositories.
 * Ensure we do not create duplicate CREATE TABLE entries in the generated ddl file
 * for the databaseStore java.comp.DefaultDataSource.
 */
@Repository(dataStore = "java:comp/DefaultDataSource")
public interface MultiEntityRepo1 {

    @Insert
    void insertDefault1(DefaultDSEntity e);

    @Insert
    void insertDefault2(DefaultDSEntity2 e);
}
