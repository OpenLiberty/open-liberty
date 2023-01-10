/*******************************************************************************
 * Copyright (c) 2018,2022 IBM Corporation and others.
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
package io.openliberty.microprofile.context.cleared.internal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.context.ThreadContext;
import org.eclipse.microprofile.context.spi.ContextManagerProvider;
import org.eclipse.microprofile.context.spi.ThreadContextProvider;
import org.eclipse.microprofile.context.spi.ThreadContextSnapshot;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.wsspi.threadcontext.ThreadContextDeserializationInfo;

/**
 * Context provider that clears MicroProfile context types when server config is used to
 * define EE Concurrency context types.
 * This should only be done up through MP Context Propagation 1.3, corresponding to Jakarta EE 9.
 * In Jakarta EE 10, the Concurrency 3.0 specification defines its own mechanism for providing
 * third-party context, and so we should avoid automatically clearing context via the MicroProfile
 * mechanism which might interfere with that.
 */
@Component(name = "com.ibm.ws.concurrent.mp.cleared.context.provider",
           configurationPolicy = ConfigurationPolicy.IGNORE,
           property = "alwaysCaptureThreadContext:Boolean=true")
@SuppressWarnings("deprecation")
@Trivial
public class MicroProfileClearedContextProvider implements com.ibm.wsspi.threadcontext.ThreadContextProvider {
    /**
     * Most of these are skipped because Liberty already provides most these (they inherit from
     * ContainerContextProvider). CDI is also skipped for compatibility purposes.
     */
    private static final HashSet<String> DO_NOT_CLEAR = new HashSet<String>(Arrays.asList //
    (
     ThreadContext.APPLICATION,
     ThreadContext.CDI,
     "Classification", // z/OS WLM context
     "EmptyHandleList",
     ThreadContext.SECURITY,
     "SyncToOSThread",
     ThreadContext.TRANSACTION //
    ));

    /**
     * Reference to the context manager provider.
     */
    @Reference
    private ContextManagerProvider cmProvider;

    @Override
    public com.ibm.wsspi.threadcontext.ThreadContext captureThreadContext(Map<String, String> execProps, Map<String, ?> threadContextConfig) {
        return createDefaultThreadContext(null);
    }

    @Override
    public com.ibm.wsspi.threadcontext.ThreadContext createDefaultThreadContext(Map<String, String> execProps) {
        ArrayList<ThreadContextSnapshot> contextSnapshots = new ArrayList<ThreadContextSnapshot>();

        @SuppressWarnings("unchecked")
        Iterable<ThreadContextProvider> providers = (Iterable<ThreadContextProvider>) cmProvider.getContextManager();

        for (ThreadContextProvider provider : providers)
            if (!DO_NOT_CLEAR.contains(provider.getThreadContextType()))
                contextSnapshots.add(provider.clearedContext(Collections.emptyMap()));

        return new MicroProfileClearedContextSnapshot(contextSnapshots);
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
