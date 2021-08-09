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
package com.ibm.wsspi.security.tai;

import javax.security.auth.Subject;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.security.WebTrustAssociationFailedException;

/**
 * <P>
 * This is the result returned from
 * <code>com.ibm.wsspi.security.tai.TrustAssociationInterceptor.negotiateValidateandEstablishTrust</code> method. It contains
 * the result of trust association interceptor negotiation, validation and establishing trust. If the trust is validated and
 * established, then status code with value <code>HttpServletResponse.SC_OK</code> and the authenticated principal must be
 * set in the <CODE>TAIResult</CODE>. The JAAS Subject is optionally, this is only used if trust association interceptor wants
 * to pass additional security information to WebSphere Application Server security runtime (please security attribute
 * propagation documentation for details).
 * </P>
 * 
 * <P>
 * Please see <code>com.ibm.wsspi.security.tai.TrustAssociationInterceptor</code> for details on the status code returned.
 * </P>
 * 
 * @author author International Business Machines Corp.
 * @version 1.0
 * @see javax.security.auth.Subject
 * @see com.ibm.wsspi.security.tai.TrustAssociationInterceptor
 * @ibm-spi
 */
public final class TAIResult {
    /**
     * <P>
     * Create an instance of the result with status, if trust is validated
     * and established (should return status <CODE>HttpServletResponse.SC_OK</CODE>,
     * if any other status code is return, WebSphere Application Server will stop
     * the normal processing and send status code back to caller.), the authenticated
     * principal and JAAS Subject contains additional security information from
     * third party security service.
     * <P>
     * 
     * @param status status code, please use <CODE>HttpServletResponse.SC_OK</CODE> if trust is validated and established
     * @param principal authenticated principal
     * @param subject JAAS Subject contains additional security information
     * @return TAIResult
     * @exception WebTrustAssociationFailedException
     *                Thrown if there is no authenticated principal when status is <CODE>HttpServletResponse.SC_OK</CODE>
     * @see javax.security.auth.Subject
     */
    public static TAIResult create(int status, String principal, Subject subject) throws WebTrustAssociationFailedException {
        return new TAIResult(status, principal, subject);
    }

    /**
     * <P>
     * Create an instance of the result with status, if trust is validated
     * and established (should return status <CODE>HttpServletResponse.SC_OK</CODE>,
     * if any other status code is return, WebSphere Application Server will stop the
     * normal processing and send status code back to caller.) and the authenticated principal.
     * <P>
     * 
     * @param status status code, please use <CODE>HttpServletResponse.SC_OK</CODE> if trust is validated and established
     * @param principal authenticated principal
     * @return TAIResult
     * @exception WebTrustAssociationFailedException
     *                Thrown if there is no authenticated principal when status is <CODE>HttpServletResponse.SC_OK</CODE>
     */
    public static TAIResult create(int status, String principal) throws WebTrustAssociationFailedException {
        return new TAIResult(status, principal, null);
    }

    /**
     * <P>
     * Create an instance of the result with status code other than <CODE>HttpServletResponse.SC_OK</CODE>.
     * This is for failure case.
     * <P>
     * 
     * @param status status code other than <CODE>HttpServletResponse.SC_OK</CODE>, for negotiation, or failure
     * @return TAIResult
     * @exception WebTrustAssociationFailedException
     */
    public static TAIResult create(int status) throws WebTrustAssociationFailedException {
        return new TAIResult(status, null, null);
    }

    /**
     * <P>
     * If trust is validated and established, then <CODE>HttpServletResponse.SC_OK</CODE> should be returned.
     * If any other status code is return, WebSphere Application Server will stop the normal processing and
     * send status code back to caller.
     * </P>
     * 
     * @return The status of the trust association interceptor processing.
     */
    public final int getStatus() {
        return status;
    }

    /**
     * <P>
     * If trust is validated and established and status is <CODE>HttpServletResponse.SC_OK</CODE>, then this
     * method return the authenticated principal.
     * </P>
     * 
     * @return The authenticated principal.
     */
    public final String getAuthenticatedPrincipal() {
        return principal;
    }

    /**
     * <P>
     * If trust is validated and established and status is <CODE>HttpServletResponse.SC_OK</CODE>, then this return
     * the JAAS Subject that contains the other security information that can be used to create the WebSphere Application
     * Server credential. Please refer to the security token propagation documentation for details.
     * </P>
     * 
     * <P>
     * This is optional and <CODE>null</CODE> could be return if there is no additional security information.
     * </P>
     * 
     * @return The JAAS Subject contains additional security information. <CODE>null</CODE> could be returned.
     * @see javax.security.auth.Subject
     */
    public final Subject getSubject() {
        return subject;
    }

    public TAIResult(int status, String principal, Subject subject) throws WebTrustAssociationFailedException {
        init(status, principal, subject);
    }

    private void init(int status, String principal, Subject subject) throws WebTrustAssociationFailedException {
        if ((status == HttpServletResponse.SC_OK)) {
            if ((principal == null) || (principal.length() == 0)) {
                throw new WebTrustAssociationFailedException("No principal in Trust Association Result");
            }
        }

        this.status = status;
        this.principal = principal;
        this.subject = subject;
    }

    private int status;
    private String principal;
    private Subject subject;
}
