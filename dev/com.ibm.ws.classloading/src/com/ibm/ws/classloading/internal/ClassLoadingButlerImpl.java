/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.classloading.internal;

import java.util.HashSet;
import java.util.Set;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.classloading.ClassLoadingButler;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Notifier.Notification;

public class ClassLoadingButlerImpl implements ClassLoadingButler {
    private final static TraceComponent tc = Tr.register(ClassLoadingButlerImpl.class);
    private final Set<ContainerClassLoader> classLoaders = new HashSet<ContainerClassLoader>();

    ClassLoadingButlerImpl(Container appContainer) {}

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.app.manager.monitor.ClassLoadingButler#setClassLoader(com.ibm.ws.classloading.internal.ContainerClassLoader)
     */
    @Override
    @Trivial
    public void addClassLoader(ClassLoader classLoader) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "addClassLoader - " + classLoader);
        }

        if (classLoader instanceof ContainerClassLoader) {
            synchronized (classLoaders) {
                classLoaders.add((ContainerClassLoader) classLoader);
            }
        } else {
            throw new IllegalArgumentException("classLoader is not a ContainerClassLoader");
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.app.manager.monitor.ClassLoadingButler#redefineClasses(com.ibm.wsspi.adaptable.module.Notifier.Notification)
     */
    @Override
    public boolean redefineClasses(Notification notification) {
        // if there are no paths to process, this is a minor update
        if (notification.getPaths().isEmpty()) {
            return true;
        }

        boolean success;
        synchronized (classLoaders) {
            if (classLoaders.isEmpty()) {
                success = false;
            } else {
                success = true;
                for (ContainerClassLoader loader : classLoaders) {
                    if (!loader.redefineClasses(notification)) {
                        success = false;
                        break;
                    }
                }
            }
        }

        return success;
    }

}
