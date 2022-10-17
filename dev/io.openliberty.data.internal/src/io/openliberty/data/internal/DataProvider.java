/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.data.internal;

import java.util.List;

import jakarta.data.Template;

/**
 * Implementations of this interface are made available in the OSGi service registry
 * and made mandatory if and only if the providing feature is enabled.
 * This is accomplished by the feature providing a defaultInstances.xml file
 * to overlay the configuration with a target and minimum cardinality of 1.
 */
public interface DataProvider {
    /**
     * Provides the implementation of the specified repository interface.
     *
     * @param <R>                 interface class that defines the data repository
     * @param repositoryInterface the repository interface
     * @param entityClass         type of entity that the repository persists
     * @return repository instance
     */
    <R> R createRepository(Class<R> repositoryInterface, Class<?> entityClass);

    /**
     * Notifies the provider of the entity classes that it is expected to handle.
     *
     * @param databaseId identifier for database-related configuration
     * @param loader     class loader
     * @param classList  entity classes
     * @throws Exception // TODO remove this?
     */
    void entitiesFound(String databaseId, ClassLoader loader, List<Class<?>> entities) throws Exception;

    /**
     * Obtains the Template implementation for this type of provider.
     *
     * @return the Template implementation.
     */
    Template getTemplate();
}