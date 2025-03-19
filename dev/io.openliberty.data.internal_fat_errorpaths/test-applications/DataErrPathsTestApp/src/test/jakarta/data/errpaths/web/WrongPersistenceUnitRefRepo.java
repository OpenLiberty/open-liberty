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
package test.jakarta.data.errpaths.web;

import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.Repository;

/**
 * Repository with a valid entity, but uses a persistence unit that does not
 * have the entity.
 */
@Repository(dataStore = "java:app/env/WrongPersistenceUnitRef")
public interface WrongPersistenceUnitRefRepo //
                extends CrudRepository<Volunteer, String> {

}