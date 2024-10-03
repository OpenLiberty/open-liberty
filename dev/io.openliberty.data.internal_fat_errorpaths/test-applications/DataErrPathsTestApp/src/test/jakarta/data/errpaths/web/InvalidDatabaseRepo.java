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

import java.util.List;

import jakarta.data.repository.By;
import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.Find;
import jakarta.data.repository.Repository;

/**
 * Repository where the dataStore is configured to a DataSource that is configured
 * to use a database that does not exist.
 */
@Repository(dataStore = "java:comp/jdbc/InvalidDatabase")
public interface InvalidDatabaseRepo extends CrudRepository<Voter, Integer> {
    @Find
    List<Voter> livesAt(@By("address") String streetAddress);
}