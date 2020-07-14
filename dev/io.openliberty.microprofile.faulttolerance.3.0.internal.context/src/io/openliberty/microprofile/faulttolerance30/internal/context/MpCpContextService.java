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
package io.openliberty.microprofile.faulttolerance30.internal.context;

import java.security.AccessController;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import org.eclipse.microprofile.context.ThreadContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.ws.kernel.service.util.SecureAction;
import com.ibm.ws.microprofile.faulttolerance.spi.context.ContextService;
import com.ibm.ws.microprofile.faulttolerance.spi.context.ContextSnapshot;

/**
 * Captures and applies context using MP Context Propagation
 * <p>
 * This class is configured with {@code service.ranking = 10} so that its service takes priority
 * over the default so that MPCP is used to apply context when available
 */
@Component(configurationPolicy = ConfigurationPolicy.IGNORE, property = "service.ranking:Integer=10")
public class MpCpContextService implements ContextService {

    private static final SecureAction secureAction = AccessController.doPrivileged(SecureAction.get());

    private final Map<ClassLoader, ThreadContext> threadContextMap = Collections.synchronizedMap(new WeakHashMap<>());

    @Override
    public ContextSnapshot capture() {
        ThreadContext threadContext = threadContextMap.computeIfAbsent(secureAction.getContextClassLoader(), c -> ThreadContext.builder().build());
        return new ExecutorContextSnapshot(threadContext.currentContextExecutor());
    }

}
