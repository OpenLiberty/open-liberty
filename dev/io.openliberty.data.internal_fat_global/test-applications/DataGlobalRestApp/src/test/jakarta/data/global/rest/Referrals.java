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
package test.jakarta.data.global.rest;

import jakarta.data.repository.BasicRepository;
import jakarta.data.repository.Repository;

import javax.sql.DataSource;

/**
 * A repository that relies on a DataSource with a java:global/env
 * resource reference that is defined in another application.
 */
@Repository(dataStore = "java:global/env/jdbc/WebAppDataSourceRef")
public interface Referrals extends BasicRepository<Referral, String> {

    DataSource getDataSource();
}
