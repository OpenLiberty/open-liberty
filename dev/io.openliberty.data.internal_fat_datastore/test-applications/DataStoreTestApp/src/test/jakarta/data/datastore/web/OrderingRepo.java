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
 * A repository that shares a dataStore with other repositories.
 *
 * The datastore java:app/jdbc/DataSourceDef is used by
 * - DSDRepoEJB: primary entity DSDRepoEJB
 * - DSDRepo: primary entity DSDEntity
 * - DSDRepoWar: primary entity DSDEntityWar
 * - OrderingRepo (web2): primary entity DefaultDSEntityWar2
 *
 * Ensure that the asynchronous nature of the FutureEMBuilder does not result in a non-deterministic ordering
 * of statements in the the java:app/jdbc/DataSourceDef ddl file
 */
@Repository(dataStore = "java:app/jdbc/DataSourceDef")
public interface OrderingRepo {

    @Insert
    void insertDefault(DefaultDSEntity e);

}
