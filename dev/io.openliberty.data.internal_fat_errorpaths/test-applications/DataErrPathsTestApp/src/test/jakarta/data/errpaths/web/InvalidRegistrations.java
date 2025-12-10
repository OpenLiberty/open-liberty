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

import jakarta.annotation.Resource;
import jakarta.data.repository.BasicRepository;
import jakarta.data.repository.Repository;

/**
 * Repository for an invalid record entity that lacks an identifier attribute.
 */
@Repository(dataStore = "java:comp/jdbc/DataSourceForInvalidRecordEntityRef")
@Resource(name = "java:comp/jdbc/DataSourceForInvalidRecordEntityRef",
          lookup = "java:module/jdbc/DataSourceForInvalidEntity")
public interface InvalidRegistrations extends BasicRepository<VoterRegistration, String> {

}