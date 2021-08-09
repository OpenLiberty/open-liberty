/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.security.auth.callback;

import javax.security.auth.callback.Callback;

/**
 * <p>
 * The <code>WSAuthMechOidCallbackImpl</code> gathers the authentication mechanism universal object identifiers (OID)
 * from the constructor and pass it to the login module. The following list contains the OIDs for each authentication mechanism:<ol>
 * <li>The Kerberos authentication mechanism OID is "1.2.840.113554.1.2.2"</li>
 * <li>The LTPA authentication mechanism OID is "1.3.18.0.2.30.2"</li>
 * <li>The BasicAuth(GSSUP) authentication mechanism OID is "2.23.130.1.1.1"</li>
 * </ol>
 * </p>
 * 
 * @ibm-api
 * @author IBM Corporation
 * @version 1.0
 * @since 1.0
 * @ibm-spi
 */
public class WSAuthMechOidCallbackImpl implements Callback {

    private String defaultAuthMechOid;
    private String authMechOid;
    private final String prompt;

    /**
     * <p>
     * Construct a <code>WSAuthMechOidCallbackImpl</code> object with a prompt hint.
     * </p>
     * 
     * @param prompt The prompt hint.
     */
    public WSAuthMechOidCallbackImpl(String prompt) {
        this.prompt = prompt;
    }

    /**
     * <p>
     * Construct a <code>WSAuthMechOidCallbackImpl</code> object with a prompt hint and
     * an authentication mechanism OID.
     * </p>
     * 
     * @param prompt The prompt hint.
     * @param authMechOid The authentication mechanism OID.
     */
    public WSAuthMechOidCallbackImpl(String prompt, String authMechOid) {
        this.prompt = prompt;
        if (authMechOid == null || authMechOid.length() == 0) {
            this.authMechOid = this.getdefaultAuthMechOid();
        } else {
            this.authMechOid = authMechOid;
        }
    }

    /**
     * <p>
     * Set the authentication mechanism OID.
     * </p>
     * 
     * @param authMechOid The authentication mechanism OID.
     */
    public void setAuthMechOid(String authMechOid) {
        if (authMechOid == null || authMechOid.length() == 0) {
            this.authMechOid = this.getdefaultAuthMechOid();
        } else {
            this.authMechOid = authMechOid;
        }
    }

    /**
     * <p>
     * Return the authentication mechanism OID.
     * </p>
     * 
     * @return The authentication mechanism OID, could be <code>null</code>.
     */
    public String getAuthMechOid() {
        if (authMechOid == null || authMechOid.equals("")) {
            authMechOid = getdefaultAuthMechOid();
        }
        return authMechOid;
    }

    /**
     * <p>
     * Return the default realm name.
     * </p>
     * 
     * @return The default authentication mechanism OID depend on resource, could be <code>null</code>.
     */
    public String getdefaultAuthMechOid() {
        if (defaultAuthMechOid == null || defaultAuthMechOid.equals("")) {
            // Set to LTPA for now since there are currently no other mechanism available.
            defaultAuthMechOid = "1.3.18.0.2.30.2";
        }
        return defaultAuthMechOid;
    }

    /**
     * <p>
     * Return the prompt. If the prompt set in Constructor
     * is <code>null</code>, then <code>null</code> is returned.
     * </p>
     * 
     * @return The prompt, could be <code>null</code>.
     */
    public String getPrompt() {
        return prompt;
    }

    /**
     * <p>
     * Returns the name of the Callback. Typically, it is the name of the class.
     * </p>
     * 
     * @return The name of the Callback.
     */
    @Override
    public String toString() {
        return getClass().getName();
    }

}
