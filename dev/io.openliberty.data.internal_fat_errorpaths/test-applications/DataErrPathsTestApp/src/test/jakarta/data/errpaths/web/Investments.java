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
package test.jakarta.data.errpaths.web;

import jakarta.data.repository.DataRepository;
import jakarta.data.repository.Insert;
import jakarta.data.repository.Repository;

/**
 * Repository for an invalid recprd entity that tries to put a Jakarta Persistence
 * annotation on a record component.
 */
@Repository(dataStore = "java:app/jdbc/env/DSForInvalidEntityRecordWithJPAAnnoRef")
public interface Investments extends DataRepository<Investment, Long> {
    @Insert
    void invest(Investment inv);
}