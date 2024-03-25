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
package com.ibm.ws.concurrent.cdi;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.resource.ResourceRefInfo;
import com.ibm.ws.runtime.metadata.MetaData;

/**
 * This class is used internally by the Concurrency component to
 * identify the class loader and metadata of the application artifact
 * that defines a managed thread factory definition.
 * This information is used to establish the context of
 * ManagedThreadFactory instances that are created as CDI beans.
 *
 * Do not use outside of the Concurrency component.
 */
@Trivial
public interface MTFBeanResourceInfo extends ResourceRefInfo {
    /**
     * Obtains the class loader of the application artifact that
     * defines the managed thread factory definition.
     *
     * @return the class loader.
     */
    ClassLoader getDeclaringClassLoader();

    /**
     * Obtains the metadata of the application artifact that
     * defines the managed thread factory definition.
     *
     * @return ComponentMetaData, ModuleMetaData, or ApplicationMetaData,
     *         at the most granular level available.
     */
    MetaData getDeclaringMetaData();
}
