/*******************************************************************************
 * Copyright (c) 2003, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/*
 * Created on Dec 18, 2003
 *
 * To change this generated comment go to 
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package com.ibm.ws.security.core;

import java.util.Iterator;
import java.util.Set;

import javax.security.auth.Subject;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.WSSecurityException;
import com.ibm.websphere.security.auth.WSSubject;
import com.ibm.websphere.security.cred.WSCredential;

/*
 * NOTE: This is a minimal implementation to provide compatibility with 
 * the tWas version of this class. 
 */
public class SecurityContext {

    private static final TraceComponent tc =
                    Tr.register(SecurityContext.class, null, null);

    public static final String REALM_SEPARATOR = "/";

    /**
     * Return the security name of the current subject on the thread
     * 
     * @return the security name id, or null if there is no subject or no WSCredential
     */
    public static String getName() {
        String secname = null;
        WSCredential credential = getCallerWSCredential();
        try {
            if (credential != null && !credential.isUnauthenticated()) {
                String realmSecname = credential.getRealmSecurityName();
                if (realmSecname != null && !realmSecname.isEmpty()) {
                    secname = realmSecname.substring(realmSecname.lastIndexOf(REALM_SEPARATOR) + 1);
                }
            }
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Internal error: " + e);
        }
        return secname;
    }

    /**
     * Return the accessid of the current subject on the thread
     * 
     * @return the access id, or null if there is no subject or no WSCredential
     */
    public static String getUser() {
        String accessid = null;
        WSCredential credential = getCallerWSCredential();
        try {
            if (credential != null && !credential.isUnauthenticated())
                accessid = credential.getAccessId();
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Internal error: " + e);
        }
        return accessid;
    }

    private static WSCredential getCallerWSCredential() {
        WSCredential wsCredential = null;
        Subject subject;
        try {
            subject = WSSubject.getCallerSubject();
            if (subject != null) {
                Set<WSCredential> wsCredentials = subject.getPublicCredentials(WSCredential.class);
                Iterator<WSCredential> wsCredentialsIterator = wsCredentials.iterator();
                if (wsCredentialsIterator.hasNext()) {
                    wsCredential = wsCredentialsIterator.next();
                }
            }
        } catch (WSSecurityException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Internal error: " + e);
        }
        return wsCredential;
    }
}
