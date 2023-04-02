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
package io.openliberty.data.internal.persistence.cdi;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.LocalTransaction.LocalTransactionCurrent;
import com.ibm.ws.tx.embeddable.EmbeddableWebSphereTransactionManager;

import io.openliberty.cdi.spi.CDIExtensionMetadata;
import jakarta.enterprise.inject.spi.Extension;

/**
 * Simulates a provider for relational databases by delegating
 * JPQL queries to the Jakarta Persistence layer.
 */
@Component(configurationPid = "io.openliberty.data.internal.persistence.cdi.DataExtensionProvider",
           configurationPolicy = ConfigurationPolicy.IGNORE,
           service = { CDIExtensionMetadata.class, DataExtensionProvider.class })
public class DataExtensionProvider implements CDIExtensionMetadata {
    private static final Set<Class<? extends Extension>> extensions = Collections.singleton(DataExtension.class);

    @Reference(target = "(component.name=com.ibm.ws.threading)")
    protected ExecutorService executor;

    @Reference
    public LocalTransactionCurrent localTranCurrent;

    @Reference
    public EmbeddableWebSphereTransactionManager tranMgr;

    @Override
    @Trivial
    public Set<Class<? extends Extension>> getExtensions() {
        return extensions;
    }
}