/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.wim;

import com.ibm.ws.security.wim.env.ICacheUtil;
import com.ibm.ws.security.wim.env.IEncryptionUtil;
import com.ibm.ws.security.wim.env.ISSLUtil;

/**
 * Factory Manager class to return environment specific classes for WebSphere environment
 */
public class FactoryManager {

    public static ICacheUtil getCacheUtil() {
        return new com.ibm.ws.security.wim.env.was.Cache();
    }

    public static ISSLUtil getSSLUtil() {
        return new com.ibm.ws.security.wim.env.was.SSLUtilImpl();
    }

    public static IEncryptionUtil getEncryptionUtil() {
        return new com.ibm.ws.security.wim.env.was.EncryptionUtilImpl();
    }
}
