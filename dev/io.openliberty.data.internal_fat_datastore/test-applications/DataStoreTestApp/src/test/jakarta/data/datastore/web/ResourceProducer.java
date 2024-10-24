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

import jakarta.annotation.Resource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnit;

import javax.sql.DataSource;

/**
 * Producers of resources based on a persistence unit reference and resource reference.
 */
@ApplicationScoped
public class ResourceProducer {

    // A prefix of java:comp/env/ is implied for name
    @Produces
    @ResourceQualifier
    @PersistenceUnit(name = "persistence/MyPersistenceUnitRef", unitName = "MyPersistenceUnit")
    EntityManagerFactory emf;

    @Produces
    @ResourceQualifier
    @Resource(name = "java:comp/env/jdbc/ServerDataSourceRef", lookup = "jdbc/ServerDataSource")
    DataSource serverDataSource;
}
