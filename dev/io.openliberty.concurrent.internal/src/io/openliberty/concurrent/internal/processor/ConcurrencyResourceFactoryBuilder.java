/*******************************************************************************
 * Copyright (c) 2024,2025 IBM Corporation and others.
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
import java.util.Arrays;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.resource.ResourceFactoryBuilder;
import com.ibm.ws.runtime.metadata.ApplicationMetaData;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.runtime.metadata.MetaData;
import com.ibm.ws.runtime.metadata.ModuleMetaData;

/**
 * Superclass for all ResourceFactoryBuilders that are provided by the
 * Concurrency component.
 */
abstract class ConcurrencyResourceFactoryBuilder //
                implements ResourceFactoryBuilder {
    private static final TraceComponent tc = //
                    Tr.register(ConcurrencyResourceFactoryBuilder.class);

    /**
     * Create an exception for the error condition where a java:global name is
     * configured on an application-defined resource that also configures
     * qualifiers.
     *
     * @param jndiName   JNDI name of the resource definition.
     * @param qualifiers qualifier names.
     * @param appName    application name.
     * @param metadata   metadata of the application artifact.
     * @return UnsupportedOperationException.
     */
    UnsupportedOperationException excJavaGlobalWithQualifiers(String jndiName,
                                                              String[] qualifiers,
                                                              String appName,
                                                              MetaData metadata) {
        String artifactName;
        if (metadata instanceof ComponentMetaData)
            artifactName = ((ComponentMetaData) metadata).getJ2EEName().toString();
        else if (metadata instanceof ModuleMetaData)
            artifactName = ((ModuleMetaData) metadata).getJ2EEName().toString();
        else if (metadata instanceof ApplicationMetaData)
            artifactName = ((ApplicationMetaData) metadata).getJ2EEName().toString();
        else
            artifactName = appName;

        return new UnsupportedOperationException(Tr //
                        .formatMessage(tc,
                                       "CWWKC1208.global.with.qualifiers",
                                       artifactName,
                                       getDefinitionAnnotationClass().getSimpleName(),
                                       getDDElementName(),
                                       jndiName,
                                       Arrays.toString(qualifiers)));
    }

    /**
     * Returns the type of deployment descriptor element that this builder handles.
     * For example: managed-executor
     *
     * @return the type of deployment descriptor element that this builder handles.
     */
    abstract String getDDElementName();

    /**
     * Returns the type of resource definition annotation that this builder handles.
     * For example: ManagedExecutorDefinition
     *
     * @return the type of resource definition annotation that this builder handles.
     */
    abstract Class<? extends Annotation> getDefinitionAnnotationClass();
}