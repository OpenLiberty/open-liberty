/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.concurrent.mp;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.concurrent.spi.ConcurrencyProvider;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.wsspi.threadcontext.ThreadContextDeserializationInfo;

/**
 * Context provider that clears MicroProfile context types when server config is used to
 * define EE Concurrency context types.
 */
@Component(name = "com.ibm.ws.concurrent.mp.cleared.context.provider",
           configurationPolicy = ConfigurationPolicy.IGNORE,
           property = "alwaysCaptureThreadContext:Boolean=true")
@Trivial
public class MicroProfileClearedContextProvider implements com.ibm.wsspi.threadcontext.ThreadContextProvider {
    /**
     * Reference to the concurrency provider.
     */
    @Reference
    private ConcurrencyProvider concurrencyProvider;

    @Override
    public com.ibm.wsspi.threadcontext.ThreadContext captureThreadContext(Map<String, String> execProps, Map<String, ?> threadContextConfig) {
        return createDefaultThreadContext(null);
    }

    @Override
    public com.ibm.wsspi.threadcontext.ThreadContext createDefaultThreadContext(Map<String, String> execProps) {
        return new MicroProfileClearedContextSnapshot((ConcurrencyManagerImpl) concurrencyProvider.getConcurrencyManager());
    }

    @Override
    public com.ibm.wsspi.threadcontext.ThreadContext deserializeThreadContext(ThreadContextDeserializationInfo info, byte[] bytes) throws ClassNotFoundException, IOException {
        return createDefaultThreadContext(null);
    }

    @Override
    public List<com.ibm.wsspi.threadcontext.ThreadContextProvider> getPrerequisites() {
        return null;
    }
}
