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
package io.openliberty.concurrent.internal.qualified;

import java.util.List;
import java.util.Map;

/**
 * Maintains associations of qualifiers to resource factory for
 * each type of resource and for each JEE name.
 *
 * JEEName -> [qualifiers -> ResourceFactory for ContextService,
 * . . . . . . qualifiers -> ResourceFactory for ManagedExecutorService,
 * . . . . . . qualifiers -> ResourceFactory for ManagedScheduledExecutorService,
 * . . . . . . qualifiers -> ResourceFactory for ManagedThreadFactory ]
 *
 * List index positions correspond to the ordinal value of the respective Type
 * enumeration constant.
 *
 * The *ResourceFactoryBuilder classes populate it with the resource factories
 * that they create. The ConcurrencyExtension removes all entries for the
 * JEE name that is present on the thread and registers beans with the qualifiers.
 *
 * This interface is available as an OSGi service component for the above purpose.
 */
public interface QualifiedResourceFactories {

    /**
     * The resource factory builder invokes this method to add a
     * resource factory with qualifiers to be processed by the
     * concurrency CDI extension.
     *
     * @param jeeName         JEE name of the form APP#MODULE or APP.
     *                            // TODO EJBs and component level
     * @param resourceType    type of resource definition
     * @param qualifierNames  names of qualifier annotation classes
     * @param resourceFactory the resource factory
     */
    void add(String jeeName,
             QualifiedResourceFactory.Type resourceType,
             List<String> qualifierNames,
             QualifiedResourceFactory resourceFactory);

    /**
     * The concurrency CDI extension invokes this method to obtain all
     * of the resource factories so it can register them as beans with
     * their respective qualifiers.
     *
     * @param jeeName JEE name of the form APP#MODULE or APP.
     *                    // TODO EJBs and component level
     * @return list of the form [qualifiers -> ResourceFactory for ContextService,
     *         . . . . . . . . . qualifiers -> ResourceFactory for ManagedExecutorService,
     *         . . . . . . . . . qualifiers -> ResourceFactory for ManagedScheduledExecutorService,
     *         . . . . . . . . . qualifiers -> ResourceFactory for ManagedThreadFactory ]
     */
    List<Map<List<String>, QualifiedResourceFactory>> removeAll(String jeeName);
}