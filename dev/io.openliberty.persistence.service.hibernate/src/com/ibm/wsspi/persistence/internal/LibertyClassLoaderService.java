/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package com.ibm.wsspi.persistence.internal;

import java.util.Collection;
import java.util.Collections;

import org.hibernate.boot.registry.classloading.internal.ClassLoaderServiceImpl;
import org.hibernate.bytecode.spi.BytecodeProvider;

/**
 * Hibernate ClassLoaderService for Liberty's persistenceService-3.0.
 *
 * Overrides {@link #loadJavaServices(Class)} to return an empty collection
 * for {@link BytecodeProvider}, preventing Hibernate's
 * {@code BytecodeProviderInitiator} from loading
 * {@code BytecodeProviderImpl} via Java {@code ServiceLoader}.
 *
 * {@code BytecodeProviderImpl} (ByteBuddy) has no public no-arg constructor
 * and cannot be instantiated by {@code ServiceLoader} across OSGi bundle
 * boundaries. When this method returns empty, Hibernate's initiator falls
 * back to {@code org.hibernate.bytecode.internal.none.BytecodeProviderImpl}
 * — the built-in no-op — which is correct for Liberty where entity classes
 * are pre-compiled Liberty internals, not user application classes requiring
 * runtime bytecode enhancement.
 *
 * All other service lookups delegate unchanged to {@link ClassLoaderServiceImpl}.
 */
class LibertyClassLoaderService extends ClassLoaderServiceImpl {

    LibertyClassLoaderService(ClassLoader appClassLoader) {
        super(appClassLoader);
    }

    @Override
    public <S> Collection<S> loadJavaServices(Class<S> serviceContract) {
        if (BytecodeProvider.class.equals(serviceContract)) {
            // Return empty — BytecodeProviderInitiator.getBytecodeProvider() will
            // fall back to the none/ no-op provider when the collection is empty.
            return Collections.emptyList();
        }
        return super.loadJavaServices(serviceContract);
    }
}
