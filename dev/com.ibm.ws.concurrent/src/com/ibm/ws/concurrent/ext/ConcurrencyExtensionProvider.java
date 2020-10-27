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
package com.ibm.ws.concurrent.ext;

import com.ibm.ws.concurrent.WSManagedExecutorService;
import com.ibm.wsspi.resource.ResourceInfo;

/**
 * Interface by which a single provider implementation can be plugged in
 * to intercept and replace resource reference lookups for
 * <code>managedExecutorService</code> and
 * <code>managedScheduledExecutorService</code>.
 *
 * At most one provider implementation can be supplied across the entire system.
 * A feature that provides this extension point makes itself incompatible
 * with every other feature that provides this extension point.
 * Do not implement if this restriction is unacceptable.
 */
public interface ConcurrencyExtensionProvider {
    /**
     * Invoked by ResourceFactory.createResource as an extension point,
     * whereby the implementer of this method can supply the managed executor
     * instance that is returned to the application for resource reference lookups.
     *
     * @param executor     managed executor instance that would normally be used for the
     *                         resource reference lookup.
     * @param resourceInfo resource reference information.
     * @return managed executor instance to use instead as the result of the
     *         resource reference lookup.
     */
    ManagedExecutorExtension provide(WSManagedExecutorService executor, ResourceInfo resourceInfo);
}
