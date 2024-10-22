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
package test.jakarta.data.config.web;

import jakarta.data.repository.BasicRepository;
import jakarta.data.repository.Repository;

import javax.sql.DataSource;

/**
 *
 */
@Repository(dataStore = "MyDataStore")
public interface Employees extends BasicRepository<Employee, Integer> {
    DataSource getDataSource();
}