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
package com.ibm.ws.jpa.container.osgi.internal;

import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.classloading.ClassTransformer;

/**
 * Routes application class transformation to the first persistence unit which
 * lists the class in {@code PersistenceUnitInfo.getAllClassNames()}.
 */
final class PersistenceClassTransformerCoordinator implements ClassTransformer {
    private static final TraceComponent tc = Tr.register(PersistenceClassTransformerCoordinator.class);

    private final List<Registration> registrations = new CopyOnWriteArrayList<Registration>();

    void add(Object owner, List<String> allClassNames, ClassTransformer transformer) {
        for (Registration registration : registrations) {
            if (registration.owner == owner) {
                return;
            }
        }

        registrations.add(new Registration(owner, allClassNames, transformer));
    }

    void remove(Object owner) {
        for (Registration registration : registrations) {
            if (registration.owner == owner) {
                registrations.remove(registration);
                return;
            }
        }
    }

    boolean isEmpty() {
        return registrations.isEmpty();
    }

    @Override
    public byte[] transformClass(String name, byte[] bytes, CodeSource source, ClassLoader loader) {
        ProtectionDomain protectionDomain = new ProtectionDomain(source, null, loader, null);
        return transformClass(name, null, bytes, protectionDomain, loader);
    }

    @Override
    public byte[] transformClass(String name,
                                 Class<?> classBeingRedefined,
                                 byte[] bytes,
                                 ProtectionDomain protectionDomain,
                                 ClassLoader loader) {
        String binaryClassName = name.replace('/', '.');
        for (Registration registration : registrations) {
            if (registration.allClassNames.contains(binaryClassName)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Selected persistence-unit transformer owner " + registration.owner
                                 + " for class " + binaryClassName);
                }
                return registration.transformer.transformClass(name, classBeingRedefined, bytes, protectionDomain, loader);
            }
        }
        return bytes;
    }

    private static final class Registration {
        private final Object owner;
        private final Set<String> allClassNames;
        private final ClassTransformer transformer;

        private Registration(Object owner, List<String> allClassNames, ClassTransformer transformer) {
            this.owner = owner;
            this.allClassNames = Collections.unmodifiableSet(new HashSet<String>(allClassNames));
            this.transformer = transformer;
        }
    }
}
