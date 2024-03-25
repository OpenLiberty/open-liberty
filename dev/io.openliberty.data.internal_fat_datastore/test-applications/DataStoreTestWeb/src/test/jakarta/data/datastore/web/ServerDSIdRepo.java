/*******************************************************************************
 * Copyright (c) 2023,2024 IBM Corporation and others.
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

import jakarta.data.repository.Delete;
import jakarta.data.repository.Repository;

@Repository(dataStore = "ServerDataSource") // id of dataSource in server.xml
public interface ServerDSIdRepo {

    boolean existsById(String id);

    // Entity type is inferred from the Delete annotation.
    // Do not add other methods or inheritance to this class.
    @Delete
    void remove(ServerDSEntity e);
}