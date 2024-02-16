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

import java.lang.annotation.Annotation;
import java.util.Set;

import com.ibm.ws.resource.ResourceFactory;
import com.ibm.ws.runtime.metadata.MetaData;

/**
 * A subclass of ResourceFactory that allows for obtaining a list of
 * qualifier annotation instances that correspond to the qualifiers class
 * names that are specified on a resource factory definition.
 */
public interface QualifiedResourceFactory extends ResourceFactory {
    /**
     * Concurrency resource definition types where qualifiers can be specified.
     */
    enum Type {
        ContextService, ManagedExecutorService, ManagedScheduledExecutorService, ManagedThreadFactory
    };

    /**
     * Obtains the class loader of the application artifact that
     * defines the resource definition.
     *
     * @return the class loader.
     */
    ClassLoader getDeclaringClassLoader();

    /**
     * Obtains the metadata of the application artifact that
     * defines the resource definition.
     *
     * @return component metadata.
     */
    MetaData getDeclaringMetadata();

    /**
     * Returns instances of the qualifier annotations for this resource factory.
     *
     * @return qualifier annotations for this resource factory.
     *         Returns the empty set if there are no qualifier classes specified on the resource definition.
     */
    Set<Annotation> getQualifiers();
}