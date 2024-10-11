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

import java.time.LocalDate;
import java.util.List;

import jakarta.data.repository.By;
import jakarta.data.repository.DataRepository;
import jakarta.data.repository.Find;
import jakarta.data.repository.Repository;

/**
 * Repository where the dataStore is configured to a JNDI name that does not exist.
 */
@Repository(dataStore = "java:module/env/DoesNotExist")
public interface InvalidJNDIRepo extends DataRepository<Voter, Integer> {
    @Find
    List<Voter> bornOn(@By("birthday") LocalDate d);
}