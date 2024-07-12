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
 * A repository that defaults to the default dataStore, but also has a qualifier
 * annotation on its resource accessor method for a DataSource.
 */
@Repository
public interface DSAccessorMethodQualifiedRepo {
    @ResourceQualifier // must be ignored
    DataSource dataSource();

    @Insert
    void add(List<DefaultDSEntity> entities);

    @Find
    @OrderBy("id")
    List<DefaultDSEntity> getAll(@By("value") String val);
}