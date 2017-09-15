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
package com.ibm.ws.security.credentials;

import javax.security.auth.Subject;
import javax.security.auth.login.CredentialException;

/**
 * A CredentialProvider represents a service that the CredentialsService
 * should consume. A CredentialProvider sets the credentials that are to be
 * placed inside Subjects.
 */
public interface CredentialProvider {

    /**
     * Given a subject, the CredentialProvider must determine
     * whether it should take any action to set credentials
     * into the Subject's public and/or private credential sets.
     * <p>
     * If the creation of the credential fails, but the error is
     * non-critical, do not throw a CredentialException. Throwing
     * a CredentialException may cause the creating of the subject
     * to be terminated.
     * 
     * @param subject
     * @throws CredentialException If an attempt to create or set the
     *             credential encountered a problem.
     */
    void setCredential(Subject subject) throws CredentialException;

    /**
     * Determines if the subject is valid given the set of credentials.
     * <p>
     * If the provider determines the Subject to be valid, it shall return
     * true. If the provider can not determine the validity of the Subject
     * due to lack of information, the provider shall return true. If the
     * provider can determine the subject is not valid, it shall return false.
     * 
     * @param subject
     * @return {@code true} if the Subject is valid, or if there was
     *         insufficient to determine validity; {@code false} otherwise.
     */
    boolean isSubjectValid(Subject subject);
}
