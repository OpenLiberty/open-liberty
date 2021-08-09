/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/*
 * Some of the code was derived from code supplied by the Apache Software Foundation licensed under the Apache License, Version 2.0.
 */
package com.ibm.ws.transport.iiop.security.config.ssl;

import java.util.Hashtable;
import java.util.Map;

import javax.net.ssl.SSLSession;

/**
 * Stores requests' SSL sessions so that they may be shared amongst portable
 * interceptors. We use this singleton instead of using a ThreadLocal
 * because we cannot guarantee that interceptors will be called under
 * the same thread for a single request.
 * <p/>
 * TODO: There may be an error where the interceptor does not remove the
 * registered session. We should have a daemon that cleans up old requests.
 * 
 * @version $Revision: 451417 $ $Date: 2006-09-29 13:13:22 -0700 (Fri, 29 Sep 2006) $
 */
public final class SSLSessionManager {
    private final static Map requestSSLSessions = new Hashtable();

    public static SSLSession getSSLSession(int requestId) {
        return (SSLSession) requestSSLSessions.get(requestId);
    }

    public static void setSSLSession(int requestId, SSLSession session) {
        requestSSLSessions.put(requestId, session);
    }

    public static SSLSession clearSSLSession(int requestId) {
        return (SSLSession) requestSSLSessions.remove(requestId);
    }
}
