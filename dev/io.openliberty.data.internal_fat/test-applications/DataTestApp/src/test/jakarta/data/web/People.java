/*******************************************************************************
 * Copyright (c) 2023, 2024 IBM Corporation and others.
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
package test.jakarta.data.web;

import jakarta.data.repository.DataRepository;
import jakarta.data.repository.Repository;

/**
 * This example only references the entity class as a parameterized type.
 * Do not add methods or inheritance that would allow the entity class
 * to be discovered another way.
 */
@Repository(dataStore = "java:module/env/data/DataStoreRef")
public interface People extends //
                CustomRepository<Person, Long>, //
                DataRepository<Person, Long> {
}
