/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package test.jakarta.data.global.webapp;

import static jakarta.data.repository.By.ID;

import jakarta.data.repository.By;
import jakarta.data.repository.DataRepository;
import jakarta.data.repository.Delete;
import jakarta.data.repository.Insert;
import jakarta.data.repository.Repository;

/**
 * A Jakarta Data repository that requires a DataSource that is defined by a
 * different application.
 */
@Repository(dataStore = "java:global/jdbc/RestResourceDataSource")
public interface Dictionary extends DataRepository<Word, String> {

    @Insert
    void addWord(Word word);

    @Delete
    int deleteWord(@By(ID) String letters);

    boolean existsByIdIgnoreCase(String id);

    default boolean isWord(String letters) {
        return existsByIdIgnoreCase(letters);
    }
}
