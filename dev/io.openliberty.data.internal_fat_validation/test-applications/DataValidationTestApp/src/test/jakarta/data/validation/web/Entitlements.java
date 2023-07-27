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
package test.jakarta.data.validation.web;

import jakarta.data.repository.DataRepository;
import jakarta.data.repository.Repository;

/**
 * Repository for a Jakarta Persistence entity with bean validation annotations.
 */
@Repository(dataStore = "java:module/jdbc/DerbyDataSource")
public interface Entitlements extends DataRepository<Entitlement, Long> {
    void save(Entitlement e);
}