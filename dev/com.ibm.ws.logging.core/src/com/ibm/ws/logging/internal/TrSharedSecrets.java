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
package com.ibm.ws.logging.internal;

import java.lang.reflect.Constructor;

import com.ibm.websphere.ras.TraceComponent;

/**
 * Access to the internals of the com.ibm.websphere.ras package.
 */
public abstract class TrSharedSecrets {
    private static TrSharedSecrets instance = createInstance();

    private static TrSharedSecrets createInstance() {
        try {
            Class<? extends TrSharedSecrets> implClass = Class.forName("com.ibm.websphere.ras.TrSharedSecretsImpl").asSubclass(TrSharedSecrets.class);
            Constructor<? extends TrSharedSecrets> constructor = implClass.getDeclaredConstructor((Class<?>[]) null);
            constructor.setAccessible(true);
            return constructor.newInstance((Object[]) null);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static TrSharedSecrets getInstance() {
        return instance;
    }

    public abstract void addGroup(TraceComponent tc, String group);
}
