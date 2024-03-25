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
package test.jakarta.data.datastore.web;

import java.util.List;

import jakarta.data.repository.By;
import jakarta.data.repository.Find;
import jakarta.data.repository.Insert;
import jakarta.data.repository.OrderBy;
import jakarta.data.repository.Repository;

import javax.sql.DataSource;

/**
 * TODO enable this repository if we find a way to support qualified resource accessor methods
 * that are used to supply the data source.
 */
//@Repository
public interface QualifiedDSRepo {
    @ResourceQualifier
    DataSource dataSource();

    @Insert
    void add(List<ServerDSEntity> entities);

    @Find
    @OrderBy("id")
    List<ServerDSEntity> getAll(@By("value") int val);
}