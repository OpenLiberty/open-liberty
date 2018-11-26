/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.boot.classloader;

import java.lang.reflect.Method;

public class ClassLoaderHookFactory {
    private static final boolean doesJDKSupportSharedClasses;
    private static final boolean isSharingEnabled;
    static {
        boolean supported = false;
        boolean sharingEnabled = false;
        try {
            Class<?> sharedClass = Class.forName("com.ibm.oti.shared.Shared");
            supported = true;
            Method m = sharedClass.getDeclaredMethod("isSharingEnabled");
            Boolean isSharing = (Boolean) m.invoke(sharedClass);
            sharingEnabled = isSharing.booleanValue();
        } catch (Exception e) {
            // default to false if we catch exceptions.  If ClassNotFoundException supported will remain false.
        }

        doesJDKSupportSharedClasses = supported;
        isSharingEnabled = sharingEnabled;
    }

    public static ClassLoaderHook getClassLoaderHook(ClassLoader loader) {
        return doesJDKSupportSharedClasses && isSharingEnabled ? SharedClassCacheHook.newInstance(loader) : null;
    }
}
