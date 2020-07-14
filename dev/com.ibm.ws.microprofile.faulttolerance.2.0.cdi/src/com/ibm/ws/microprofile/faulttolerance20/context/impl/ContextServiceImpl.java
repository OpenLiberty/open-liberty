/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.faulttolerance20.context.impl;

import java.util.Collections;
import java.util.Map;

import javax.enterprise.context.control.RequestContextController;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.CDI;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

import com.ibm.ws.microprofile.faulttolerance.spi.context.ContextService;
import com.ibm.ws.microprofile.faulttolerance.spi.context.ContextSnapshot;
import com.ibm.wsspi.threadcontext.WSContextService;

/**
 * Implements ContextService using the liberty context API and the CDI RequestContextController
 * <p>
 * Captures and applies the following context:
 * <ul>
 * <li>Classloader
 * <li>JEE Metadata
 * <li>Security
 * <li>CDI Request Context
 * </ul>
 */
@Component(configurationPolicy = ConfigurationPolicy.IGNORE)
public class ContextServiceImpl implements ContextService {

    /**
     * The collection of contexts to capture under createThreadContext.
     * Classloader, JeeMetadata, and security.
     */
    @SuppressWarnings("unchecked")
    private static final Map<String, ?>[] THREAD_CONTEXT_PROVIDERS = new Map[] {
                                                                                 Collections.singletonMap(WSContextService.THREAD_CONTEXT_PROVIDER,
                                                                                                          "com.ibm.ws.classloader.context.provider"),
                                                                                 Collections.singletonMap(WSContextService.THREAD_CONTEXT_PROVIDER,
                                                                                                          "com.ibm.ws.javaee.metadata.context.provider"),
                                                                                 Collections.singletonMap(WSContextService.THREAD_CONTEXT_PROVIDER,
                                                                                                          "com.ibm.ws.security.context.provider"),
    };

    @Reference
    private WSContextService wsContextService;

    /** {@inheritDoc} */
    @Override
    public ContextSnapshot capture() {
        Instance<RequestContextController> requestContextInstance = CDI.current().select(RequestContextController.class);
        RequestContextController requestContextController = requestContextInstance.isResolvable() ? requestContextInstance.get() : null;
        return new ContextSnapshotImpl(wsContextService.captureThreadContext(null, THREAD_CONTEXT_PROVIDERS), requestContextController);
    }

}
