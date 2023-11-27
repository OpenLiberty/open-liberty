/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.cdi40.internal.utils;

import org.jboss.weld.proxy.WeldClientProxy;

public class CDI40Utils {
    /**
     * WARNING: USE OF THIS METHOD IS DISCOURAGED.
     * Contextual information about the instance is lost.
     * Any interceptors associated with the instance will not be invoked.
     * Storing the underlying instance will mean that the lifecycle is not properly managed and may lead to a memory leak.
     * You may end up using the instance outside of the intended scope.
     *
     * If an object is an instance of WeldClientProxy then attempt to unwrap the proxy to return the underlying instance.
     * Otherwise just return the original object.
     *
     * @param instance The instance to unwrap
     * @return The underlying instance
     * @deprecated
     */
    @Deprecated
    public static Object getContextualInstanceFromProxy(Object instance) {
        Object contextualInstance = instance;
        if (instance != null && instance instanceof WeldClientProxy) {
            contextualInstance = ((WeldClientProxy) instance).getMetadata().getContextualInstance();
        }
        return contextualInstance;
    }
}
