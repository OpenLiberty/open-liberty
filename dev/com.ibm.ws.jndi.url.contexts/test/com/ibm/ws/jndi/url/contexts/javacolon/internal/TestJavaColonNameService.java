/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jndi.url.contexts.javacolon.internal;

import javax.naming.NamingException;

import com.ibm.ws.container.service.naming.NamingConstants;

public class TestJavaColonNameService extends JavaColonNameService {
    String moduleName;
    String appName;

    void setModuleName(String name) {
        moduleName = name;
    }

    void setAppName(String name) {
        appName = name;
    }

    @Override
    protected String getModuleName(NamingConstants.JavaColonNamespace namespace, String name) throws NamingException {
        return moduleName;
    }

    @Override
    protected String getAppName(NamingConstants.JavaColonNamespace namespace, String name) throws NamingException {
        return appName;
    }
}
