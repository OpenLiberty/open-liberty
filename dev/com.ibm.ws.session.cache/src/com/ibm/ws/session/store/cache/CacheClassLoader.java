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
package com.ibm.ws.session.store.cache;

import com.ibm.ws.session.store.cache.serializable.SessionData;
import com.ibm.ws.session.store.cache.serializable.SessionKey;
import com.ibm.ws.session.store.cache.serializable.SessionPropertyKey;

/**
 * Allows the JCache provider to deserialize specific classes from the com.ibm.ws.session.cache bundle.
 */
public class CacheClassLoader extends ClassLoader {
    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        Class<?> c;

        if (name.equals(SessionKey.class.getName()))
            c = SessionKey.class;
        else if (name.equals(SessionPropertyKey.class.getName()))
            c = SessionPropertyKey.class;
        else if (name.equals(SessionData.class.getName()))
            c = SessionData.class;
        else
            c = super.findClass(name);

        return c;
    }
}
