/*******************************************************************************
 * Copyright (c) 2011, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.artifact.zip.internal;

import org.osgi.framework.BundleContext;

import com.ibm.ws.artifact.zip.cache.ZipCachingService;
import com.ibm.wsspi.artifact.factory.ArtifactContainerFactory;

/**
 * Declarative services safe API for the zip file container factory
 * to link to the services defined root delegating container factory
 * and to the bundle context in which the zip file container is active.
 */
public interface ContainerFactoryHolder {
    /**
     * Answer the bundle context in which the zip file container is active.
     *
     * This will be null if the zip file container is not active.
     *
     * @return The bundle context in which the zip file container is active.
     */
    BundleContext getBundleContext();

    /**
     * Answer the root delegating container factory which is to be used by
     * the zip file container to convert entries to non-enclosed containers.
     *
     * An {@link IllegalStateException} is thrown if an attempt is made to
     * retrieve the root delegating container factory when the factory is unset.
     *
     * @return The root delegating container factory used by the zip file
     *     container.
     *
     * TODO: Want to rename this to 'getRootDelegatingContainerFactory',
     *       but cannot because of the dependency from the artifact tests
     *       in WS-CD-Open.
     */
    ArtifactContainerFactory getContainerFactory();

    /**
     * Answer the zip caching service used by the zip file container.
     *
     * An {@link IllegalStateException} is thrown if an attempt is made to
     * retrieve the zip caching service while the service is unset.
     *
     * @return The zip caching service used by the zip file container.
     */
    ZipCachingService getZipCachingService();

    /**
     * Answer true or false telling if the "jar" protocol is to be used in
     * archive URLs.  Normally, this should answer false, in which case the
     * "wsjar" protocol will be used.
     *
     * @return True or false telling if the "jar" protocol is to be used
     *     instead of the more usual "wsjar" protocol.
     */
    boolean useJarUrls();
}
