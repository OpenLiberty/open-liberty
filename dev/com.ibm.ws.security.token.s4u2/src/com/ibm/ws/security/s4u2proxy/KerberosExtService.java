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
package com.ibm.ws.security.s4u2proxy;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;

import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.Oid;

/**
 * This interface should be implemented by service providers that
 * want to provide an impersonation of a client's Kerberos credential.
 */
public interface KerberosExtService {
    /**
     * The delegate service gets the client delegate GSSCredential behalf of the client using for self (S4U2self)
     * 
     * @param userPrincipalName - UserPrincipalName of the user for which the SPNEGO token will be generated.
     * @param targetServiceSpn - The target ServicePrincipalName of system for which SPNEGO token will be targeted.
     * @param gssNameType - GSSName type.
     * @param gssCredUsage - GSSCredential usage.
     * @param delegateServiceSpn - Delegate service principal name of the delegate service.
     * @param delegateServiceSubject - Delegate service subject that have the delegate SPN Kerberos ticket granting ticket (TGT).
     * @throws GSSException - thrown when SPNEGO token generation fails, when delegateServiceSpnSubject is null, when the delegateServiceSpnSubject
     *             does not contain the delegate service SPN TGT, when upn or targetServiceSpn are invalid.
     */

    public GSSCredential getDelegateGSSCredUsingS4U2self(String userPrincipalName,
                                                         String targetServiceSpn,
                                                         Oid gssNameType,
                                                         int gssCredUsage,
                                                         String delegateServiceSpn,
                                                         Subject delegateServiceSubject) throws GSSException;

    /**
     * The delegate service gets the client delegate GSSCredential behalf of the client using for proxy (S4U2proxy).
     * 
     * @param userPrincipalName - UserPrincipalName of the user for which the SPNEGO token will be generated.
     * @param delegateGSSContext - Delegate service GSSContext that is already established with the client.
     * @param delegateServiceSpn - Delegate service principal name.
     * @throws GSSException - thrown when SPNEGO token generation fails, when the delegateGSSContext is not established or nor extended GSSCredential
     *             or userPrincipalName is null.
     */

    public GSSCredential getDelegateGSSCredUsingS4U2proxy(String userPrincipalName,
                                                          GSSContext delegateGSSContext,
                                                          String delegateServiceSpn) throws GSSException;

    /**
     * This method return true if S4U2self is enabled; otherwise it returns false
     */
    public boolean isS4U2selfEnable();

    /**
     * This method return true if S4U2proxy is enabled; otherwise it returns false
     */
    public boolean isS4U2proxyEnable();

    /**
     * This method authenticate the delegate SPN to the KDC using the Kerberos keytab
     * 
     * @param delegateSpn
     * @param krb5Keytab
     * @return
     * @throws LoginException
     */
    Subject doKerberosLogin(String delegateSpn, String krb5Keytab) throws LoginException;

}
