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
package io.openliberty.concurrent.internal.cdi;

import java.util.List;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.concurrent.cdi.MTFBeanResourceInfo;
import com.ibm.ws.runtime.metadata.MetaData;

/**
 * This class is used internally by the Concurrency component to
 * identify the class loader and metadata of the application artifact
 * that defines a managed thread factory definition.
 * This information is used to establish the context of
 * ManagedThreadFactory instances that are created as CDI beans.
 */
@SuppressWarnings("restriction")
@Trivial
class MTFBeanResourceInfoImpl implements MTFBeanResourceInfo {
    private static final TraceComponent tc = Tr.register(MTFBeanResourceInfoImpl.class);

    private final ClassLoader declaringClassLoader;

    private final MetaData declaringMetadata;

    MTFBeanResourceInfoImpl(ClassLoader declaringClassLoader, MetaData declaringMetadata) {
        this.declaringClassLoader = declaringClassLoader;
        this.declaringMetadata = declaringMetadata;
    }

    @Override
    public int getAuth() {
        return 0;
    }

    @Override
    public int getBranchCoupling() {
        return 0;
    }

    @Override
    public int getCommitPriority() {
        return 0;
    }

    /**
     * Obtains the class loader of the application artifact that
     * defines the managed thread factory definition.
     *
     * @return the class loader.
     */
    @Override
    public ClassLoader getDeclaringClassLoader() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "getDeclaringClassLoader: " + declaringClassLoader);

        return declaringClassLoader;
    }

    /**
     * Obtains the metadata of the application artifact that
     * defines the managed thread factory definition.
     *
     * @return metadata.
     */
    @Override
    public MetaData getDeclaringMetaData() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "getDeclaringMetadata: " + declaringMetadata);

        return declaringMetadata;
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public int getIsolationLevel() {
        return 0;
    }

    @Override
    public String getJNDIName() {
        return null;
    }

    @Override
    public String getLoginConfigurationName() {
        return null;
    }

    @Override
    public List<? extends Property> getLoginPropertyList() {
        return null;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public int getSharingScope() {
        return 0;
    }

    @Override
    public String getType() {
        return null;
    }
}