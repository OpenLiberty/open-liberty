/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.jakarta.data.jpa.hibernate.integration.web;

import jakarta.data.repository.Delete;
import jakarta.data.repository.Repository;
import jakarta.data.repository.Save;

/**
 * Repository for the County entity.
 */
@Repository(provider = "Hibernate",
            dataStore = "HibernateProvider") //NOTE: Hibernate requires use of PersistenceUnit name (not reference)
public interface Counties {

    @Delete
    void remove(County c);

    @Save
    void save(County c);

}
