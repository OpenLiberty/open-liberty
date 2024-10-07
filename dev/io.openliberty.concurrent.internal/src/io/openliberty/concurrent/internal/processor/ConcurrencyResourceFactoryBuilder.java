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
package io.openliberty.concurrent.internal.processor;

import java.lang.annotation.Annotation;

import com.ibm.ws.resource.ResourceFactoryBuilder;

/**
 * Super interface for all ResourceFcatoryBuilders that are provided by the
 * Concurrency component.
 */
public interface ConcurrencyResourceFactoryBuilder extends ResourceFactoryBuilder {
    /**
     * Returns the type of deployment descriptor element that this builder handles.
     * For example: managed-executor
     *
     * @return the type of deployment descriptor element that this builder handles.
     */
    String getDDElementName();

    /**
     * Returns the type of resource definition annotation that this builder handles.
     * For example: ManagedExecutorDefinition
     *
     * @return the type of resource definition annotation that this builder handles.
     */
    Class<? extends Annotation> getDefinitionAnnotationClass();
}