/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
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
package io.openliberty.data.internal;

import java.util.List;

import jakarta.data.Template;
import jakarta.data.exceptions.MappingException;

/**
 * Implementations of this interface are made available in the OSGi service registry
 * and made mandatory if and only if the providing feature is enabled.
 * This is accomplished by the feature providing a defaultInstances.xml file
 * to overlay the configuration with a target and minimum cardinality of 1.
 */
public interface LibertyDataProvider {
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
     * Obtains a repository instance for the specified repository interface and entity class.
     *
     * @param repositoryInterface the repository interface.
     * @param entityClass         the main entity class that the repository is for.
     * @return repository instance.
     */
    <R> R getRepository(Class<R> repositoryInterface, Class<?> entityClass) throws MappingException;

    /**
     * Obtains the Template implementation for this type of provider.
     *
     * @return the Template implementation.
     */
    Template getTemplate();

    /**
     * Invoked when the bean for the repository is disposed.
     *
     * @param repository instance that was previously obtained via getRepository.
     */
    void repositoryBeanDisposed(Object repository);
}