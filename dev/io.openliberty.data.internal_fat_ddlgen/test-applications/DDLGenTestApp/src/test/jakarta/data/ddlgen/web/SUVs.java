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
package test.jakarta.data.ddlgen.web;

import jakarta.data.repository.BasicRepository;
import jakarta.data.repository.Repository;

/**
 * Repository for datasource jndiName jdbc/TestDataSource
 */
@Repository(dataStore = "java:app/env/jdbc/TestDataSourceResourceRef")
public interface SUVs extends BasicRepository<SUV, String> {

}
