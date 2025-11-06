/*******************************************************************************
 * Copyright (c) 2024,2025 IBM Corporation and others.
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

import jakarta.data.repository.Repository;

/**
 * A useless repository with no methods, no built-in repository super interface,
 * and no primary entity type. This can be used to test out if someone who is
 * just starting out developing can create a repository like this and inject it.
 */
@Repository(dataStore = "java:module/env/data/DataStoreRef")
public interface EmptyRepository {
}
