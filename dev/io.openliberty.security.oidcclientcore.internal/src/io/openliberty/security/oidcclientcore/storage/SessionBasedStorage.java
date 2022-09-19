/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.oidcclientcore.storage;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

public class SessionBasedStorage implements Storage {

    public static final TraceComponent tc = Tr.register(SessionBasedStorage.class);

    private final HttpServletRequest request;

    public SessionBasedStorage(HttpServletRequest request) {
        this.request = request;
    }

    @Override
    public void store(String name, String value) {
        store(name, value, null);
    }

    @Override
    public void store(String name, String value, StorageProperties properties) {
        // ignore storage properties, since http session attributes
        // can't set properties (e.g., expiration time) like cookies can
        HttpSession session = request.getSession();
        session.setAttribute(name, value);
    }

    @Override
    public String get(String name) {
        HttpSession session = request.getSession();
        Object value = session.getAttribute(name);
        if (value == null) {
            return null;
        }
        return (String) value;
    }

    @Override
    public void remove(String name) {
        HttpSession session = request.getSession();
        session.removeAttribute(name);
    }

}
