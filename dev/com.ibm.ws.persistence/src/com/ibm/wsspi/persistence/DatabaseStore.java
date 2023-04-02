/*******************************************************************************
 * Copyright (c) 2015, 2021 IBM Corporation and others.
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
package com.ibm.wsspi.persistence;

import java.util.Map;

/**
 * Enables configuration of a database as a persistent store that can be shared by multiple components.
 */
public interface DatabaseStore {

    /**
     * Create a persistence service unit for the specified entity classes.
     * The invoker of this method is responsible for closing the persistence service unit
     * and for participating in DDL generation.
     *
     * @param loader class loader for the entity classes.
     * @param entityClasses list of entity classes.
     * @return the persistence service unit.
     * @throws Exception if a failure occurs.
     * @throws UnsupportedOperationException for Informix
     */
    PersistenceServiceUnit createPersistenceServiceUnit(ClassLoader loader, String... entityClassNames) throws Exception;

    /**
     * Create a persistence service unit for the specified entity classes.
     * Allows the invoker to pass configuration properties.
     * The invoker of this method is responsible for closing the persistence service unit
     * and for participating in DDL generation.
     *
     * The persistence service feature is currently not supported for informix.
     *
     * @param loader class loader for the entity classes.
     * @param properties map of configuration properties
     * @param entityClasses list of entity classes.
     * @return the persistence service unit.
     * @throws Exception if a failure occurs.
     * @throws UnsupportedOperationException for Informix
     * @throws UnsupportedOperationException if a configuration property is not supported
     */
    PersistenceServiceUnit createPersistenceServiceUnit(ClassLoader loader, Map properties, String... entityClassNames) throws Exception;

    /**
     * Get the configured schema name.
     */
    public String getSchema();

    /**
     * Get the configured table prefix.
     */
    public String getTablePrefix();
}
