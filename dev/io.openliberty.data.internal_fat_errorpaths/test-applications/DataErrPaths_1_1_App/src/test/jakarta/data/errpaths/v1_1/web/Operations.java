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
package test.jakarta.data.errpaths.v1_1.web;

import jakarta.data.page.Page;
import jakarta.data.page.PageRequest;
import jakarta.data.repository.DataRepository;
import jakarta.data.repository.Insert;
import jakarta.data.repository.NativeQuery;
import jakarta.data.repository.Repository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.PersistenceUnit;

/**
 * Repository with a valid entity.
 * Some methods are valid.
 * Others have errors, as indicated.
 */
// TODO Remove the PersistenceUnit annotation once Liberty's Jakarta
// Persistence container integration code implements the requirement to
// automatically bind the EntityManagerFactory into JNDI
@PersistenceUnit(name = "java:module/persistence/OpsPersistenceUnit/EntityManagerFactory",
                 unitName = "OpsPersistenceUnit")
@Repository(dataStore = "OpsPersistenceUnit")
public interface Operations extends DataRepository<Operation, String> {

    @NativeQuery("""
                    SELECT symbol
                      FROM Operation
                     WHERE COALESCE (numArgs, 0) = 2
                     ORDER BY symbol ASC
                    """)
    Page<Character> binaryOps(PageRequest req);

    @Insert
    void define(Operation op);

}
